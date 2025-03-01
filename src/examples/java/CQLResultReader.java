import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;

import java.nio.charset.StandardCharsets;
import java.util.List;

public class CQLResultReader
{
    /**
     * Читает метаданные результата из ByteBuf.
     *
     * @param byteBuf буфер, содержащий данные
     * @return список имён столбцов
     */
    public void readMetadata(ByteBuf byteBuf) {
        System.out.println(ByteBufUtil.prettyHexDump(byteBuf));
    }

    /**
     * Читает строку из ByteBuf.
     *
     * @param byteBuf буфер, содержащий данные
     * @return строка
     */
    private static String readString(ByteBuf byteBuf) {
        int length = readStringLength(byteBuf);
        byte[] bytes = new byte[length];
        byteBuf.readBytes(bytes);
        return new String(bytes, StandardCharsets.UTF_8);
    }

    /**
     * Читает длину строки из ByteBuf.
     *
     * @param byteBuf буфер, содержащий данные
     * @return длина строки
     */
    private static int readStringLength(ByteBuf byteBuf) {
        return byteBuf.readShort() & 0xFFFF; // Беззнаковое значение
    }

    /**
     * Читает строки данных из ByteBuf.
     *
     * @param byteBuf буфер, содержащий данные
     * @param columns список имён столбцов
     */
    public void readRows(ByteBuf byteBuf, List<String> columns) {
        // Читаем количество строк (4 байта)
        int rowsCount = byteBuf.readInt();

        for (int i = 0; i < rowsCount; i++) {
            System.out.println("Row " + (i + 1) + ":");
            for (String column : columns) {
                // Читаем значение (длина + данные)
                int length = byteBuf.readInt();
                if (length < 0) {
                    System.out.println(column + ": null"); // NULL значение
                } else {
                    byte[] valueBytes = new byte[length];
                    byteBuf.readBytes(valueBytes);
                    String value = new String(valueBytes, StandardCharsets.UTF_8);
                    System.out.println(column + ": " + value);
                }
            }
        }
    }
}
