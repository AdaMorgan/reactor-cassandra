package com.github.adamorgan.internal.utils.compress;

import com.github.adamorgan.api.utils.Compression;
import io.netty.buffer.ByteBuf;
import org.xerial.snappy.Snappy;

import java.io.IOException;
import java.nio.ByteBuffer;

/**
 * @implNote The Snappy protocol already encodes the uncompressed length in the compressed payload,
 * so {@link #pack(ByteBuf)} and {@link #packWithoutLength(ByteBuf)} produce the same
 * output for this compressor. The corresponding parameters {@code
 * prependWithUncompressedLength} and {@code uncompressedLength} are ignored by their respective
 * methods.
 */
public class SnappyCompressor extends AbstractCompressor
{
    public SnappyCompressor()
    {
        super(Compression.SNAPPY);
    }
    
    @Override
    protected ByteBuf packDirect(ByteBuf body, /*ignored*/ boolean prependWithUncompressedLength)
    {
        int maxCompressedLength = Snappy.maxCompressedLength(body.readableBytes());
        // If the input is direct we will allocate a direct output buffer as well as this will allow us
        // to use Snappy.compress(ByteBuffer, ByteBuffer) and so eliminate memory copies.
        ByteBuf output = body.alloc().directBuffer(maxCompressedLength);

        try
        {
            ByteBuffer in = inputNioBuffer(body);
            // Increase reader index.
            body.readerIndex(body.writerIndex());

            ByteBuffer out = outputNioBuffer(output);
            int written = Snappy.compress(in, out);
            // Set the writer index so the amount of written bytes is reflected
            output.writerIndex(output.writerIndex() + written);
            return output;
        }
        catch (IOException e)
        {
            // release output buffer so we not leak and rethrow exception.
            output.release();
            throw new RuntimeException(e);
        }
    }

    @Override
    protected ByteBuf packHeap(ByteBuf body, /*ignored*/ boolean prependWithUncompressedLength)
    {
        int maxCompressedLength = Snappy.maxCompressedLength(body.readableBytes());
        int inOffset = body.arrayOffset() + body.readerIndex();
        byte[] in = body.array();
        int len = body.readableBytes();
        // Increase reader index.
        body.readerIndex(body.writerIndex());

        // Allocate a heap buffer from the ByteBufAllocator as we may use a PooledByteBufAllocator and
        // so can eliminate the overhead of allocate a new byte[].
        ByteBuf output = body.alloc()
                .heapBuffer(maxCompressedLength);
        try
        {
            // Calculate the correct offset.
            int offset = output.arrayOffset() + output.writerIndex();
            byte[] out = output.array();
            int written = Snappy.compress(in, inOffset, len, out, offset);

            // Increase the writerIndex with the written bytes.
            output.writerIndex(output.writerIndex() + written);
            return output;
        }
        catch (IOException e)
        {
            // release output buffer so we not leak and rethrow exception.
            output.release();
            throw new RuntimeException(e);
        }
    }

    @Override
    protected int readUncompressedLength(ByteBuf body)
    {
        // Since compress methods don't actually prepend with a length, we have nothing to read here.
        // Return a bogus length (it will be ignored by the decompress methods, so the actual value
        // doesn't matter).
        return -1;
    }

    @Override
    protected ByteBuf unpackDirect(ByteBuf input, /*ignored*/ int length)
    {
        ByteBuffer in = inputNioBuffer(input);
        // Increase reader index.
        input.readerIndex(input.writerIndex());

        ByteBuf output = null;
        try
        {
            if (!Snappy.isValidCompressedBuffer(in))
            {
                throw new IllegalArgumentException("Provided frame does not appear to be Snappy compressed");
            }
            // If the input is direct we will allocate a direct output buffer as well as this will allow
            // us to use Snappy.compress(ByteBuffer, ByteBuffer) and so eliminate memory copies.
            output = input.alloc().directBuffer(Snappy.uncompressedLength(in));
            ByteBuffer out = outputNioBuffer(output);

            int size = Snappy.uncompress(in, out);
            // Set the writer index so the amount of written bytes is reflected
            output.writerIndex(output.writerIndex() + size);
            return output;
        }
        catch (IOException e)
        {
            // release output buffer so we not leak and rethrow exception.
            if (output != null)
            {
                output.release();
            }
            throw new RuntimeException(e);
        }
    }

    @Override
    protected ByteBuf unpackHeap(ByteBuf input, /*ignored*/ int length)
    {
        // Not a direct buffer so use byte arrays...
        int inOffset = input.arrayOffset() + input.readerIndex();
        byte[] in = input.array();
        int len = input.readableBytes();
        // Increase reader index.
        input.readerIndex(input.writerIndex());

        ByteBuf output = null;
        try
        {
            if (!Snappy.isValidCompressedBuffer(in, inOffset, len))
            {
                throw new IllegalArgumentException("Provided frame does not appear to be Snappy compressed");
            }
            // Allocate a heap buffer from the ByteBufAllocator as we may use a PooledByteBufAllocator and
            // so can eliminate the overhead of allocate a new byte[].
            output = input.alloc().heapBuffer(Snappy.uncompressedLength(in, inOffset, len));
            // Calculate the correct offset.
            int offset = output.arrayOffset() + output.writerIndex();
            byte[] out = output.array();
            int written = Snappy.uncompress(in, inOffset, len, out, offset);

            // Increase the writerIndex with the written bytes.
            output.writerIndex(output.writerIndex() + written);
            return output;
        }
        catch (IOException e)
        {
            // release output buffer so we not leak and rethrow exception.
            if (output != null)
            {
                output.release();
            }
            throw new RuntimeException(e);
        }
    }
}
