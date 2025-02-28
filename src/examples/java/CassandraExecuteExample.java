import com.datastax.internal.requests.SocketCode;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.buffer.Unpooled;

import java.nio.charset.StandardCharsets;

public class CassandraExecuteExample
{
    public ByteBuf prepare(int version, int streamId, String query) {
        ByteBuf buffer = Unpooled.buffer();

        // Заголовок сообщения
        buffer.writeByte(version); // Версия протокола
        buffer.writeByte(0x00);             // Флаги
        buffer.writeShort(streamId);        // Идентификатор потока
        buffer.writeByte(SocketCode.PREPARE);   // Код операции (PREPARE)

        // Место для длины тела сообщения (заполним позже)
        int bodyLengthIndex = buffer.writerIndex();
        buffer.writeInt(0);

        // Тело сообщения
        int bodyStartIndex = buffer.writerIndex();

        // Текст запроса (long string)
        SocketClient.Writer.writeLongString(query, buffer);

        // Вычисляем длину тела сообщения
        int bodyLength = buffer.writerIndex() - bodyStartIndex;
        buffer.setInt(bodyLengthIndex, bodyLength);

        return buffer;
    }

    public ByteBuf execute(ByteBuf byteBuf, int shardId) {
        ByteBuf buffer = Unpooled.buffer();

        byte[] preparedQueryId = readPreparedQueryId(byteBuf);

        // Заголовок сообщения
        buffer.writeByte(0x04); // Версия протокола (Cassandra v4)
        buffer.writeByte(0x00); // Флаги
        buffer.writeShort(0x00); // Идентификатор потока
        buffer.writeByte(0x0A); // Код операции (EXECUTE)

        // Место для длины тела сообщения (заполним позже)
        int bodyLengthIndex = buffer.writerIndex();
        buffer.writeInt(0);

        // Тело сообщения
        int bodyStartIndex = buffer.writerIndex();

        // 1. ID подготовленного запроса (short bytes)
        buffer.writeShort(preparedQueryId.length); // Длина ID (2 байта)
        buffer.writeBytes(preparedQueryId); // Сам ID

        // 2. Параметры запроса
        // Флаги (1 байт): 0x01 — значения передаются
        buffer.writeByte(0x01);

        // Уровень согласованности (1 байт): QUORUM (0x04)
        buffer.writeByte(0x04); // QUORUM

        // Флаги пакета (1 байт): 0x00 — по умолчанию
        buffer.writeByte(0x00);

        // Количество значений (4 байта): у нас 1 параметр (shard_id)
        buffer.writeInt(1);

        // 3. Добавляем значение параметра shard_id (int)
        buffer.writeInt(4); // Длина значения: 4 байта (int)
        buffer.writeInt(shardId); // Само значение

        // Вычисляем длину тела сообщения
        int bodyLength = buffer.writerIndex() - bodyStartIndex;
        buffer.setInt(bodyLengthIndex, bodyLength);

        return buffer;
    }

    private byte[] readPreparedQueryId(ByteBuf byteBuf) {
        int idLength = byteBuf.readShort(); // Длина ID (2 байта)
        byte[] id = new byte[idLength];
        byteBuf.readBytes(id); // Сам ID (байтовый массив)

        // Логирование для отладки
        System.out.println("Prepared Query ID Length: " + idLength);
        System.out.println("Prepared Query ID: " + bytesToHex(id));

        return id;
    }

    // Вспомогательный метод для преобразования байтов в hex-строку
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x ", b));
        }
        return sb.toString();
    }
}