package com.github.adamorgan.test;

import io.netty.buffer.ByteBuf;
import org.apache.commons.lang3.StringUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.util.LinkedList;
import java.util.stream.Collectors;

public class RowsResultImpl
{
    private final ByteBuf buffer;
    private final LinkedList<ColumnImpl> columns = new LinkedList<>();
    private final LinkedList<RowImpl> rows = new LinkedList<>();

    public RowsResultImpl(@Nonnull final ByteBuf buffer)
    {
        this.buffer = buffer;
    }

    @Override
    public String toString()
    {
        int rowsFlag = buffer.readInt();

        boolean hasGlobalTableSpec = (rowsFlag & 0x0001) != 0;
        boolean hasMorePages = (rowsFlag & 0x0002) != 0;
        boolean noMetadata = (rowsFlag & 0x0004) != 0;
        //#
        String keyspace = "";
        String table = "";

        int columnsCount = buffer.readInt();

        if (hasGlobalTableSpec) {
            keyspace = readString(buffer);
            table = readString(buffer);
        }

        if (hasMorePages)
        {
            buffer.skipBytes(buffer.readInt());
        }

        if (!noMetadata) {
            for (int i = 0; i < columnsCount; i++) {
                if (!hasGlobalTableSpec) {
                    keyspace = readString(buffer);
                    table = readString(buffer);
                }

                String name = readString(buffer);
                int type = buffer.readShort();

                columns.add(new ColumnImpl(keyspace, table, name, type));
            }
        }

        int rowsCount = buffer.readInt();

        for (int rowNumber = 0; rowNumber < rowsCount; rowNumber++) {
            for (ColumnImpl column : columns) {
                Serializable value = readValue(buffer, column.getType());
                RowImpl row = new RowImpl(column, value);
                this.rows.addLast(row);
            }
        }

        LinkedList<String> columns = this.columns.stream().map(ColumnImpl::toString).collect(Collectors.toCollection(LinkedList::new));
        LinkedList<String> rows = this.rows.stream().map(RowImpl::toString).collect(Collectors.toCollection(LinkedList::new));

        return new StringTableUtils.Table(columns, rows).toString();
    }

    private static String readString(@Nonnull ByteBuf buffer)
    {
        int length = buffer.readUnsignedShort();
        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    private static Serializable readValue(@Nonnull ByteBuf buffer, int type)
    {
        int length = buffer.readInt();
        if (length < 0)
        {
            return Void.class;
        }

        byte[] bytes = new byte[length];
        buffer.readBytes(bytes);

        return new String(bytes, StandardCharsets.UTF_8);
    }

    public static class ColumnImpl
    {
        private final String keyspace;
        private final String tableName;
        private final String name;
        private final int type;

        public ColumnImpl(String keyspace, String tableName, String name, int type)
        {
            this.keyspace = keyspace;
            this.tableName = tableName;
            this.name = name;
            this.type = type;
        }

        @Nonnull
        public String getName()
        {
            return name;
        }

        public int getType()
        {
            return type;
        }

        @Override
        public String toString()
        {
            return String.format("%s.%s.%s [%s]", keyspace, tableName, name, type);
        }
    }

    public static class RowImpl
    {
        private final ColumnImpl column;
        private final Serializable value;

        public RowImpl(@Nonnull ColumnImpl column, @Nullable Serializable value)
        {
            this.column = column;
            this.value = value;
        }

        @Override
        public String toString()
        {
            return value != null ? value.toString() : "null";
        }
    }

    public static class StringTableUtils
    {
        public static class Table
        {
            private final int columnCount;
            private final LinkedList<String> headers;
            private final LinkedList<String> rows;

            public Table(LinkedList<String> headers, LinkedList<String> rows)
            {
                this.headers = headers;
                this.rows = rows;
                this.columnCount = headers.size();
            }

            @Override
            public String toString() {
                LinkedList<String> allData = new LinkedList<>();
                allData.addAll(headers);
                allData.addAll(rows);

                int[] colWidths = new int[columnCount];
                for (int i = 0; i < allData.size(); i++) {
                    int col = i % columnCount;
                    colWidths[col] = Math.max(colWidths[col], allData.get(i).length());
                }

                StringBuilder builder = new StringBuilder();

                appendFormattedRow(builder, headers, colWidths);

                builder.append("|");
                for (int width : colWidths) {
                    builder.append(StringUtils.repeat("-", width + 2)).append("|");
                }
                builder.append("\n");

                for (int i = 0; i < rows.size(); i += columnCount) {
                    LinkedList<String> row = new LinkedList<>();
                    for (int j = 0; j < columnCount; j++) {
                        row.add(rows.get(i + j));
                    }
                    appendFormattedRow(builder, row, colWidths);
                }

                return builder.toString();
            }

            private void appendFormattedRow(StringBuilder sb, LinkedList<String> cells, int[] widths) {
                sb.append("|");
                for (int i = 0; i < columnCount; i++) {
                    String cell = cells.get(i);
                    sb.append(" ").append(cell);
                    String repeat = StringUtils.repeat(" ", widths[i] - cell.length() + 1);
                    sb.append(repeat).append("|");
                }
                sb.append("\n");
            }
        }
    }
}
