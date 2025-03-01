import com.datastax.internal.requests.SocketCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

public class CassandraExecuteExample
{
    private final SocketClient.Initializer initializer;

    public CassandraExecuteExample(SocketClient.Initializer initializer)
    {
        this.initializer = initializer;
    }

    public ByteBuf prepare(String query)
    {
        ByteBuf buffer = Unpooled.buffer();

        buffer.writeByte(0x04);
        buffer.writeByte(0x00);
        buffer.writeShort(0x00);
        buffer.writeByte(SocketCode.PREPARE);

        int bodyLengthIndex = buffer.writerIndex();
        buffer.writeInt(0);

        int bodyStartIndex = buffer.writerIndex();

        SocketClient.Writer.writeLongString(query, buffer);
        buffer.writeShort(0x0001);
        buffer.writeByte(0x00);

        int bodyLength = buffer.writerIndex() - bodyStartIndex;
        buffer.setInt(bodyLengthIndex, bodyLength);

        return buffer;
    }


    /*
        public ByteBuf createStartupMessage()
        {
            ByteBuf buffer = Unpooled.buffer();

            buffer.writeByte(PROTOCOL_VERSION);
            buffer.writeByte(0x00);
            buffer.writeShort(0x00);
            buffer.writeByte(SocketCode.STARTUP);

            Map<String, String> options = new HashMap<>();
            options.put(CQL_VERSION_OPTION, CQL_VERSION);

            //options.put(COMPRESSION_OPTION, "");
            //options.put(NO_COMPACT_OPTION, "true");

            options.put(DRIVER_VERSION_OPTION, DRIVER_VERSION);
            options.put(DRIVER_NAME_OPTION, DRIVER_NAME);

            ByteBuf body = Unpooled.buffer();
            Writer.writeStringMap(options, body);

            buffer.writeInt(body.readableBytes());
            buffer.writeBytes(body);

            return buffer;
        }
     */
    public ByteBuf execute(ByteBuf byteBuf, int shardId)
    {
        ByteBuf buffer = Unpooled.buffer();

        // Заголовок сообщения
        buffer.writeByte(0x04); // Версия протокола (Cassandra v4)
        buffer.writeByte(0x00); // Флаги
        buffer.writeShort(1);   // Идентификатор потока
        buffer.writeByte(0x0A); // Код операции (EXECUTE)

        // Место для длины тела сообщения (заполним позже)
        int bodyLengthIndex = buffer.writerIndex();
        buffer.writeInt(0);

        // Тело сообщения
        int bodyStartIndex = buffer.writerIndex();

        byte[] preparedQueryId = this.readPreparedQueryId(byteBuf);

        // 1. ID подготовленного запроса (short bytes)
        buffer.writeShort(preparedQueryId.length); // Длина ID (2 байта)
        buffer.writeBytes(preparedQueryId);        // Сам ID

        // 2. Параметры запроса
        buffer.writeByte(0x01); // Флаги: значения передаются
        buffer.writeByte(0x04); // Уровень согласованности: QUORUM (0x04)
        buffer.writeByte(0x00); // Флаги пакета: по умолчанию

        buffer.writeInt(1);     // Количество значений: 1
        buffer.writeInt(4);     // Длина значения: 4 байта (int)
        buffer.writeInt(shardId); // Значение параметра shard_id

        // Вычисляем длину тела сообщения
        int bodyLength = buffer.writerIndex() - bodyStartIndex;
        buffer.setInt(bodyLengthIndex, bodyLength);

        byte[] bytes1 = ByteBufUtil.decodeHexDump("040001000a0000002b");

        byte[] bytes2 = ByteBufUtil.decodeHexDump("00106ca274e8e9fa379db3edec41652e9bc7000a27000100000004000000000000138800062f39cb789e68");

        return Unpooled.wrappedBuffer(bytes1, bytes2);
    }

    private byte[] readPreparedQueryId(ByteBuf byteBuf)
    {
        // Чтение длины ID (2 байта, big-endian)
        int idLength = byteBuf.readShort(); // Убедимся, что длина положительная

        // Проверка, что в буфере достаточно данных для чтения
        if (byteBuf.readableBytes() < idLength)
        {
            throw new IllegalArgumentException("Not enough data in ByteBuf to read query ID");
        }

        // Чтение самого ID (байтовый массив)
        byte[] id = new byte[idLength];
        byteBuf.readBytes(id);

        return id;
    }
}