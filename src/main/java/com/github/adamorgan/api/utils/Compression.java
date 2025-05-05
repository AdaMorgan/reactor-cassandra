package com.github.adamorgan.api.utils;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4SafeDecompressor;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.function.Function;

public enum Compression
{
    LZ4("lz4", Compression::packLZ4, Compression::unpackLZ4),
    SNAPPY("snappy", Compression::packSnappy, Compression::unpackSnappy);

    private final String key;
    private final Function<ByteBuf, ByteBuf> pack, unpack;

    Compression(String key, Function<ByteBuf, ByteBuf> pack, Function<ByteBuf, ByteBuf> unpack)
    {
        this.key = key;
        this.pack = pack;
        this.unpack = unpack;
    }

    @Override
    public String toString()
    {
        return key;
    }

    public ByteBuf pack(ByteBuf body)
    {
        return pack.apply(body);
    }

    public ByteBuf unpack(ByteBuf body)
    {
        return unpack.apply(body);
    }

    private static ByteBuf packLZ4(ByteBuf body)
    {
        byte[] input = new byte[body.readableBytes()];
        body.getBytes(body.readerIndex(), input);

        LZ4Compressor compressor = LZ4Factory.fastestInstance().fastCompressor();
        int maxLength = compressor.maxCompressedLength(input.length);
        byte[] compressed = new byte[maxLength];

        int compressedLength = compressor.compress(input, 0, input.length, compressed, 0);

        ByteBuf out = body.alloc().buffer(4 + compressedLength);
        out.writeInt(input.length);
        out.writeBytes(compressed, 0, compressedLength);

        return out;
    }

    private static ByteBuf unpackLZ4(ByteBuf input)
    {
        int uncompressedLength = input.readInt();
        int compressedLength = input.readableBytes();

        byte[] compressed = new byte[compressedLength];
        input.readBytes(compressed);

        LZ4SafeDecompressor decompressor = LZ4Factory.fastestInstance().safeDecompressor();
        byte[] restored = new byte[uncompressedLength];

        decompressor.decompress(compressed, 0, compressedLength, restored, 0);

        return Unpooled.wrappedBuffer(restored);
    }

    private static ByteBuf packSnappy(ByteBuf input)
    {
        return null;
    }

    private static ByteBuf unpackSnappy(ByteBuf input)
    {
        ByteBuffer in = inputNioBuffer(input);
        input.readerIndex(input.writerIndex());

        ByteBuf output = null;
        try
        {
            if (!Snappy.isValidCompressedBuffer(in))
            {
                throw new IllegalArgumentException("Provided frame does not appear to be Snappy compressed");
            }

            output = input.alloc()
                    .directBuffer(Snappy.uncompressedLength(in));
            ByteBuffer out = outputNioBuffer(output);

            int size = Snappy.uncompress(in, out);
            output.writerIndex(output.writerIndex() + size);
            return output;
        }
        catch (IOException e)
        {
            if (output != null)
            {
                output.release();
            }
            throw new RuntimeException(e);
        }
    }

    private static ByteBuffer inputNioBuffer(ByteBuf buf)
    {
        int index = buf.readerIndex();
        int len = buf.readableBytes();
        return buf.nioBufferCount() == 1 ? buf.internalNioBuffer(index, len) : buf.nioBuffer(index, len);
    }

    private static ByteBuffer outputNioBuffer(ByteBuf buf)
    {
        int index = buf.writerIndex();
        int len = buf.writableBytes();
        return buf.nioBufferCount() == 1 ? buf.internalNioBuffer(index, len) : buf.nioBuffer(index, len);
    }
}
