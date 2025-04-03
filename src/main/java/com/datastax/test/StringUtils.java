package com.datastax.test;

import org.example.data.DataArray;
import org.example.data.DataObject;

import java.util.LinkedList;

public class StringUtils
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
                builder.append("-".repeat(width + 2)).append("|");
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
                sb.append(" ".repeat(widths[i] - cell.length() + 1)).append("|");
            }
            sb.append("\n");
        }
    }
}
