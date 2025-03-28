# CQL BINARY PROTOCOL v4

The native protocol defines the format of the binary messages exchanged between the driver and Cassandra over TCP. 
As a driver user, you don’t need to know the fine details (although the protocol spec is in [the Cassandra codebase](https://github.com/apache/cassandra/tree/trunk/doc) if you’re curious); 
the most visible aspect is that some features are only available with specific protocol versions.

## Compatibility matrix

Coming soon...

## Table Contents

* [Overview](#-Overview)
* [Frame header](#-frame-header)
  * [version](#version)
  * [flags](#flags)
  * [stream](#stream)
  * [opcode](#opcode)
  * [length](#length)
* [Notations](#-notations)
* [Messages](#-messages)
  * [Requests](#requests)
    * [STARTUP](#startup)
    * [AUTH_RESPONSE](#auth_response)
    * [OPTIONS](#options)
    * [QUERY](#query)
    * [PREPARE](#prepare)
    * [EXECUTE](#execute)
    * [BATCH](#batch)
    * [REGISTER](#register)
  * [Responses](#responses)
    * [ERROR](#error)
    * [READY](#ready)
    * [AUTHENTICATE](#authenticate)
    * [SUPPORTED](#supported)
    * [RESULT](#result)
      * [Void](#void)
      * [Rows](#rows)
      * [Set_keyspace](#set_keyspace)
      * [Prepared](#prepared)
      * [Schema_change](#schema_change)
    * [EVENT](#event)
    * [AUTH_CHALLENGE](#auth_challenge)
    * [AUTH_SUCCESS](#auth_success)
* [Compression](#-compression)
* [Data Type Serialization Formats](#-data-type-serialization-formats)
  * [Data Type Serialization](#data-type-specifications)
  * [Varint Encoding Examples](#varint-encoding-examples)
* [User Defined Type Serialization](#-user-defined-type-serialization)
* [Result paging](#-result-paging)
* [Error codes](#-error-codes)
* [Changes from v3](#-changes-from-v3)

## 🔍 Overview

The CQL binary protocol is a frame based protocol. Frames are defined as:

```githubexpressionlanguage
0         8        16        24        32         40
+---------+---------+---------+---------+---------+
| version |  flags  |      stream       | opcode  |
+---------+---------+---------+---------+---------+
|                length                 |
+---------+---------+---------+---------+
|                                       |
.            ...  body ...              .
.                                       .
.                                       .
+----------------------------------------
```

The protocol is big-endian (network byte order).

Each frame contains a fixed size header (9 bytes) followed by a variable size
body. The header is described in Section 2. The content of the body depends
on the header opcode value (the body can in particular be empty for some
opcode values). The list of allowed opcodes is defined in Section 2.4 and the
details of each corresponding message are described Section 4.

The protocol distinguishes two types of frames: requests and responses. Requests
are those frames sent by the client to the server. Responses are those frames sent
by the server to the client. Note, however, that the protocol supports server pushes
(events) so a response does not necessarily come right after a client request.

Note to client implementors: client libraries should always assume that the
body of a given frame may contain more data than what is described in this
document. It will however always be safe to ignore the remainder of the frame
body in such cases. The reason is that this may enable extending the protocol
with optional features without needing to change the protocol version.

## 📜 Frame header

### version

The version is a single byte that indicates both the direction of the message
(request or response) and the version of the protocol in use. The most
significant bit of version is used to define the direction of the message:
0 indicates a request, 1 indicates a response. This can be useful for protocol
analyzers to distinguish the nature of the packet from the direction in which
it is moving. The rest of that byte is the protocol version (4 for the protocol
defined in this document). In other words, for this version of the protocol,
version will be one of:
0x04    Request frame for this protocol version
0x84    Response frame for this protocol version

Please note that while every message ships with the version, only one version
of messages is accepted on a given connection. In other words, the first message
exchanged (STARTUP) sets the version for the connection for the lifetime of this
connection.

This document describes version 4 of the protocol. For the changes made since
version 3, see Section 10.

### flags

Flags applying to this frame. The flags have the following meaning (described
by the mask that allows selecting them):
- 0x01: Compression flag. If set, the frame body is compressed. The actual compression to use should have been set up beforehand through the Startup message.
- 0x02: Tracing flag. For a request frame, indicates the client requires tracing of the request. Only QUERY, PREPARE and EXECUTE queries support tracing.
- 0x04: Custom payload flag. Indicates that a generic key-value custom payload for a custom QueryHandler implementation is present in the frame.
- 0x08: Warning flag. The response contains warnings which were generated by the server.

The rest of flags is currently unused and ignored.

### stream

A frame has a stream id (a [short] value). When sending request messages, this
stream id must be set by the client to a non-negative value (negative stream ids
are reserved for streams initiated by the server). If a client sends a request message
with the stream id X, it is guaranteed that the stream id of the response to
that message will be X.

This helps to enable the asynchronous nature of the protocol. There can be up to
32768 different simultaneous streams.

### opcode

| Hex Value | Message Type   | Description                         |
|-----------|----------------|-------------------------------------|
| `0x00`    | ERROR          | Error response message              |
| `0x01`    | STARTUP        | Initialize connection               |
| `0x02`    | READY          | Connection ready notification       |
| `0x03`    | AUTHENTICATE   | Authentication challenge request    |
| `0x05`    | OPTIONS        | Request supported options           |
| `0x06`    | SUPPORTED      | Supported options response          |
| `0x07`    | QUERY          | CQL query execution                 |
| `0x08`    | RESULT         | Query result response               |
| `0x09`    | PREPARE        | Prepare statement request           |
| `0x0A`    | EXECUTE        | Execute prepared statement          |
| `0x0B`    | REGISTER       | Register for event notifications    |
| `0x0C`    | EVENT          | Server-pushed event notification    |
| `0x0D`    | BATCH          | Batch query execution               |
| `0x0E`    | AUTH_CHALLENGE | Authentication challenge            |
| `0x0F`    | AUTH_RESPONSE  | Authentication response             |
| `0x10`    | AUTH_SUCCESS   | Authentication success notification |

**Notes:**
- All opcodes are 1 byte in size
- `0x04` is intentionally unused in this protocol version
- Message direction is determined by the frame header's version byte
- Error responses use opcode `0x00` (ERROR)

(Note that there is no 0x04 message in this version of the protocol)

### length

A 4 byte integer representing the length of the body of the frame (note:
currently a frame is limited to 256MB in length).

## 📝 Notations

To describe the layout of the frame body for the messages, we define the following:

| Type                | Length                 | Description                                                                                                                  |
|---------------------|------------------------|------------------------------------------------------------------------------------------------------------------------------|
| `[int]`             | 4 bytes                | Signed integer (big-endian)                                                                                                  |
| `[long]`            | 8 bytes                | Signed integer (big-endian)                                                                                                  |
| `[short]`           | 2 bytes                | Unsigned integer (big-endian)                                                                                                |
| `[string]`          | 2 + n bytes            | `[short] n` (length), followed by `n` UTF-8 bytes                                                                            |
| `[long string]`     | 4 + n bytes            | `[int] n` (length), followed by `n` UTF-8 bytes                                                                              |
| `[uuid]`            | 16 bytes               | UUID (RFC 4122)                                                                                                              |
| `[string list]`     | 2 + Σ                  | `[short] n` (count), followed by `n` `[string]` elements                                                                     |
| `[bytes]`           | 4 + n bytes (if n ≥ 0) | `[int] n` (length), followed by `n` bytes if `n >= 0` (null if `n < 0`)                                                      |
| `[value]`           | 4 + n bytes (if n ≥ 0) | `[int] n` (length):<br>- `n >= 0`: `n` bytes follow<br>- `n == -1`: null value (4 bytes)<br>- `n == -2`: "not set" (4 bytes) |
| `[short bytes]`     | 2 + n bytes            | `[short] n` (length), followed by `n` bytes if `n >= 0`                                                                      |
| `[option]`          | 2 + variable           | Pair of `<id><value>`:<br>- `id`: `[short]`<br>- `value`: type-dependent                                                     |
| `[option list]`     | 2 + Σ                  | `[short] n` (count), followed by `n` `[option]` elements                                                                     |
| `[inet]`            | 1 + n + 4 bytes        | `[byte]` (address size), `n` IP bytes (4 or 16), `[int]` port                                                                |
| `[consistency]`     | 2 bytes                | `[short]` representing consistency level                                                                                     |
| `[string map]`      | 2 + Σ                  | `[short] n` (count), followed by `n` key-value pairs (`[string]` keys and values)                                            |
| `[string multimap]` | 2 + Σ                  | `[short] n` (count), followed by `n` pairs with `[string]` keys and `[string list]` values                                   |
| `[bytes map]`       | 2 + Σ                  | `[short] n` (count), followed by `n` pairs with `[string]` keys and `[bytes]` values                                         |

**Notes:**
- Σ indicates variable length depending on contained elements
- All numeric values are big-endian
- String values are UTF-8 encoded
- Negative lengths indicate special values (null/not set)

## 📤 Messages

Dependant on the flags specified in the header, the layout of the message body must be:
`[<tracing_id>][<warnings>][<custom_payload>][<message>]`

where:
- tracing_id - is a UUID tracing ID, present if this is a request message and the Tracing flag is set
- warnings - is a string list of warnings (if this is a request message and the Warning flag is set
- custom_payload - is bytes map for the serialised custom payload present if this is one of the message types which support custom payloads (QUERY, PREPARE, EXECUTE and BATCH) and the Custom payload flag is set
- message - as defined below through sections 4 and 5.

### Requests

Note that outside of their normal responses, all requests can get an ERROR message as response.

#### STARTUP

Initialize the connection. The server will respond by either a READY message or an AUTHENTICATE message.

This must be the first message of the connection, except for OPTIONS that can be sent before to find out the options supported by the server.

The body is a [string map] of options. Possible options are:
- **CQL_VERSION** - the version of CQL to use (currently only "3.0.0" is supported)
- **COMPRESSION** - the compression algorithm to use for frames
- **NO_COMPACT** - whether to establish connection in compatibility mode
- **THROW_ON_OVERLOAD** - If true, server will send OverloadedException error message back to client instead of applying back pressure

#### AUTH_RESPONSE

Answers a server authentication challenge. The body is a single [bytes] token.

#### OPTIONS

Asks the server to return which STARTUP options are supported. The body should be empty and the server will respond with a SUPPORTED message.

#### QUERY

Performs a CQL query. The body must be:
<query><query_parameters>
where <query> is a [long string] and <query_parameters> contains consistency level, flags, and optional parameters like values, page size, paging state, etc.

#### PREPARE

Prepare a query for later execution. The body consists of the CQL query to prepare as a [long string].

#### EXECUTE

Executes a prepared query. The body must be:
`<id><query_parameters>`
where `<id>` is the prepared query ID and `<query_parameters>` has the same definition as in QUERY.

#### BATCH

Allows executing a list of queries (prepared or not) as a batch. The body must be:
`<type><n><query_1>...<query_n><consistency><flags>[<serial_consistency>][<timestamp>]`
where <type> indicates the type of batch (logged, unlogged, or counter).

#### REGISTER

Register this connection to receive some types of events. The body is a [string list] representing the event types to register for.

### Responses

#### ERROR

Indicates an error processing a request. The body will be an error code ([int]) followed by a [string] error message, and possibly more content depending on the exception.

#### READY

Indicates that the server is ready to process queries. The body is empty.

#### AUTHENTICATE

Indicates that the server requires authentication. The body consists of a single [string] indicating the full class name of the IAuthenticator in use.

#### SUPPORTED

Indicates which startup options are supported by the server. The body is a [string multimap] giving for each supported option the list of supported values.

#### RESULT

The response to query operations (QUERY, PREPARE, EXECUTE, or BATCH requests). The message body begins with an [int] field indicating the result type (kind), followed by type-specific content.

Supported result kinds and their corresponding hex codes:

| Kind Value | Name          | Description                                                  |
|------------|---------------|--------------------------------------------------------------|
| 0x0001     | Void          | Indicates successful query execution with no additional data |
| 0x0002     | Rows          | Contains result set data from SELECT queries                 |
| 0x0003     | Set_keyspace  | Response to USE queries, confirms keyspace change            |                                                                                                                
| 0x0004     | Prepared      | Contains prepared statement metadata                         |                                                                                                                              
| 0x0005     | Schema_change | Notification of DDL operation completion                     |

The remainder of the message body varies according to the result kind:

* Void: Empty body
* Rows: Contains query result metadata and row data
* Set_keyspace: Single [string] with the activated keyspace name
* Prepared: Includes statement ID and parameter metadata 
* Schema_change: Contains DDL operation details (3 [string] fields)

##### Void
The rest of the body is empty.

##### Rows
Indicates a set of rows. Contains metadata, row count, and row content.
An `[int]` where bits represent formatting information. Supported flags:

###### flags
| Mask     | Name               | Description                                                         |
|----------|--------------------|---------------------------------------------------------------------|
| `0x0001` | Global_tables_spec | If set, contains single `<global_table_spec>`                       |
| `0x0002` | Has_more_pages     | If set, contains `<paging_state>` (bytes for pagination)            |
| `0x0004` | No_metadata        | If set, contains only flags, column count and optional paging state |

###### Components

| Component             | Type/Size                              | Presence Condition                         | Description                  |
|-----------------------|----------------------------------------|--------------------------------------------|------------------------------|
| **flags**             | 4 bytes                                | Always present                             | Bitmask flags                |
| **columns_count**     | 4 bytes                                | Always present                             | Column count                 |
| **paging_state**      | 4 + N bytes                            | When flags.0x0002 = 1                      | Length (4B) + data (N bytes) |
| **global_table_spec** | 4 + X + 4 + Y bytes                    | When flags.0x0001 = 1 AND flags.0x0004 = 0 | Keyspace (X) + Table (Y)     |
| **col_spec**          | [see breakdown](#column-specification) | When flags.0x0004 = 0                      | Column specifications        |
| **rows_count**        | 4 bytes                                | Always present                             | Row count                    |
| **rows_content**      | rows * cols                            | rows_count × columns_count                 | Cell values                  |

###### Column Specification

| Part          | Type/Size                              | Presence Condition                   |
|---------------|----------------------------------------|--------------------------------------|
| `key_space`   | `string` (4 + L bytes)                 | When flags.0x0001 = 0                |
| `table_name`  | `string` (4 + M bytes)                 | When flags.0x0001 = 0                |
| `column_name` | `string` (4 + N bytes)                 | Always present                       |
| `column_type` | `option` (1-2 + size bytes)            | Always present                       |

##### Set_keyspace
The body is a single [string] indicating the name of the keyspace that has been set.

##### Prepared
The body contains prepared query ID, metadata, and result metadata.

##### Schema_change
The body is the same as for a "SCHEMA_CHANGE" event (3 strings: change_type, target, options).

#### EVENT
An event pushed by the server. The body starts with a [string] representing the event type. Valid event types are:
- "TOPOLOGY_CHANGE": cluster topology changes
- "STATUS_CHANGE": node status changes
- "SCHEMA_CHANGE": schema changes

#### AUTH_CHALLENGE
A server authentication challenge. The body is a single [bytes] token.

#### AUTH_SUCCESS
Indicates the success of the authentication phase. The body is a single [bytes] token.

## 🗜️ Compression

Frame compression is supported by the protocol, but only the frame body is compressed. Compression must be agreed upon in the STARTUP message. Supported compression algorithms:
- lz4
- snappy

## 🔠 Data Type Serialization Formats

This section describes the serialization formats for all CQL data types supported by Cassandra. Client drivers should use these formats when encoding values for EXECUTE messages, and Cassandra uses them when returning values in RESULT messages.

**General Notes:**
- All values are transmitted as `[bytes]` (length-prefixed byte sequences)
- Empty values (zero length) are distinct from NULL (negative length)
- All encodings use big-endian byte order
- Length prefixes are not included in the type-specific formats below

### Data Type Specifications

| Type          | Size          | Format                                   | Notes                                  |
|---------------|---------------|------------------------------------------|----------------------------------------|
| **ascii**     | n bytes       | ASCII characters [0-127]                 | Invalid if contains bytes >127         |
| **bigint**    | 8 bytes       | Two's complement integer                 |                                        |
| **blob**      | n bytes       | Raw byte sequence                        |                                        |
| **boolean**   | 1 byte        | 0 = false, 1 = true (recommended)        | Any non-zero = true                    |
| **date**      | 4 bytes       | Unsigned integer (days since epoch)      | Epoch center at 2^31 (1970-01-01)      |
| **decimal**   | variable      | `[int]` scale + `varint` unscaled value  | Value = unscaled × 10<sup>-scale</sup> |
| **double**    | 8 bytes       | IEEE 754 binary64                        |                                        |
| **float**     | 4 bytes       | IEEE 754 binary32                        |                                        |
| **inet**      | 4 or 16 bytes | IPv4 (4B) or IPv6 (16B) address          |                                        |
| **int**       | 4 bytes       | Two's complement integer                 |                                        |
| **list**      | variable      | `[int]` count + elements                 | Each element as `[bytes]`              |
| **map**       | variable      | `[int]` count + key-value pairs          | Each key/value as `[bytes]`            |
| **set**       | variable      | `[int]` count + elements                 | Each element as `[bytes]`              |
| **smallint**  | 2 bytes       | Two's complement integer                 |                                        |
| **text**      | n bytes       | UTF-8 encoded string                     | Alias: varchar                         |
| **time**      | 8 bytes       | Nanoseconds since midnight               | Range: 0-86399999999999                |
| **timestamp** | 8 bytes       | Milliseconds since Unix epoch            | Negative = pre-epoch                   |
| **timeuuid**  | 16 bytes      | Version 1 UUID                           | RFC 4122 compliant                     |
| **tinyint**   | 1 byte        | Two's complement integer                 |                                        |
| **tuple**     | variable      | Sequence of `[bytes]` elements           | Null elements use length -1            |
| **uuid**      | 16 bytes      | Any valid UUID                           | RFC 4122 compliant                     |
| **varint**    | variable      | Two's complement variable-length integer |                                        |

### Varint Encoding Examples

| Value | Encoding |
|-------|----------|
| 0     | 0x00     |
| 1     | 0x01     |
| 127   | 0x7F     |
| 128   | 0x0080   |
| 129   | 0x0081   |
| -1    | 0xFF     |
| -128  | 0x80     |
| -129  | 0xFF7F   |

**Important Notes for Varint:**
- Positive values must have MSB < 0x80
- Values with MSB ≥ 0x80 should be padded with leading 0x00
- Negative values are represented using two's complement

## 👤 User Defined Type Serialization

A UDT value is composed of successive [bytes] values, one for each field of the UDT value (in the order defined by the type).

## 📃 Result paging

The protocol allows for paging the result of queries. QUERY and EXECUTE messages can specify a page size. If more results are available, the RESULT message will have the Has_more_pages flag set and contain a paging_state value to retrieve the next page.

## 🚨 Error codes

ERROR messages contain an error code and message. Some errors include additional information. Error codes include:

| Hex Code | Name                  | Additional Information Format                                                               | Description                           |
|----------|-----------------------|---------------------------------------------------------------------------------------------|---------------------------------------|
| 0x0000   | Server Error          |                                                                                             | Internal server error                 |
| 0x000A   | Protocol Error        |                                                                                             | Protocol violation                    |
| 0x0100   | Authentication Error  |                                                                                             | Authentication failure                |
| 0x1000   | Unavailable Exception | `[consistency]<cl> [int]<required> [int]<alive>`                                            | Not enough replicas available         |
| 0x1001   | Overloaded            |                                                                                             | Coordinator overloaded                |
| 0x1002   | Is Bootstrapping      |                                                                                             | Coordinator bootstrapping             |
| 0x1003   | Truncate Error        |                                                                                             | Truncation operation failed           |
| 0x1100   | Write Timeout         | `[consistency]<cl> [int]<received> [int]<blockfor> [string]<writeType>`                     | Write operation timeout               |
| 0x1200   | Read Timeout          | `[consistency]<cl> [int]<received> [int]<blockfor> [byte]<data_present>`                    | Read operation timeout                |
| 0x1300   | Read Failure          | `[consistency]<cl> [int]<received> [int]<blockfor> [int]<numfailures> [byte]<data_present>` | Non-timeout read failure              |
| 0x1400   | Function Failure      | `[string]<keyspace> [string]<function> [string list]<arg_types>`                            | User-defined function execution error |
| 0x1500   | Write Failure         | `[consistency]<cl> [int]<received> [int]<blockfor> [int]<numfailures> [string]<writeType>`  | Non-timeout write failure             |
| 0x2000   | Syntax Error          |                                                                                             | Query syntax error                    |
| 0x2100   | Unauthorized          |                                                                                             | Permission denied                     |
| 0x2200   | Invalid               |                                                                                             | Invalid request                       |
| 0x2300   | Config Error          |                                                                                             | Configuration issue                   |
| 0x2400   | Already Exists        | `[string]<ks> [string]<table>`                                                              | Keyspace/table already exists         |
| 0x2500   | Unprepared            | `[short bytes]<unknown_id>`                                                                 | Unknown prepared statement ID         |

### Write Types (for Write Timeout/Failure)

| Type           | Description                   |
|----------------|-------------------------------|
| SIMPLE         | Non-batched non-counter write |
| BATCH          | Logged batch write            |
| UNLOGGED_BATCH | Unlogged batch write          |
| COUNTER        | Counter write                 |
| BATCH_LOG      | Batch log write failure       |
| CAS            | Compare-and-set operation     |
| VIEW           | Materialized view update      |
| CDC            | Change Data Capture operation |

### Important Notes

1. All error codes are 4-byte integers
2. The message string is always present after the error code
3. Additional information (when present) follows the message string
4. Consistency levels in additional info use the standard [consistency] encoding
5. For Already Exists errors:
  - Empty table string indicates keyspace creation conflict
  - Non-empty table string indicates table creation conflict

## 🆕 Changes from v3

- Prepared responses now include partition-key bind indexes
- Modified format of "SCHEMA_CHANGE" events to include changes related to UDFs and UDAs
- Added Read_failure and Function_failure error codes
- Added custom payload to frames
- Added warnings to frames
- Added date, time, tinyint and smallint data types
- <paging_state> is not compatible between v3 and v4
- Added THROW_ON_OVERLOAD startup option