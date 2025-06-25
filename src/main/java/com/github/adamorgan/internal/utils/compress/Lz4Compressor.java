/*
 * Copyright 2025 Ada Morgan, John Regan
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */ 

package com.github.adamorgan.internal.utils.compress;

import com.github.adamorgan.api.utils.Compression;
import io.netty.buffer.ByteBuf;
import net.jpountz.lz4.LZ4Compressor;
import net.jpountz.lz4.LZ4Factory;
import net.jpountz.lz4.LZ4FastDecompressor;

import java.nio.ByteBuffer;

public class Lz4Compressor extends AbstractCompressor
{
    private final LZ4Compressor compressor;
    private final LZ4FastDecompressor decompressor;
    private final LZ4Factory lz4Factory;

    public Lz4Compressor()
    {
        super(Compression.LZ4);
        this.lz4Factory = LZ4Factory.fastestInstance();
        this.compressor = lz4Factory.fastCompressor();
        this.decompressor = lz4Factory.fastDecompressor();
    }

    public String getName()
    {
        return this.compression.name();
    }

    @Override
    protected ByteBuf packDirect(ByteBuf body, boolean prependWithUncompressedLength)
    {
        int maxCompressedLength = compressor.maxCompressedLength(body.readableBytes());
        // If the input is direct we will allocate a direct output buffer as well as this will allow us
        // to use LZ4Compressor.compress and so eliminate memory copies.
        ByteBuf output = body.alloc().directBuffer((prependWithUncompressedLength ? 4 : 0) + maxCompressedLength);
        try
        {
            ByteBuffer in = inputNioBuffer(body);
            // Increase reader index.
            body.readerIndex(body.writerIndex());

            if (prependWithUncompressedLength)
            {
                output.writeInt(in.remaining());
            }

            ByteBuffer out = outputNioBuffer(output);
            int written = compressor.compress(in, in.position(), in.remaining(), out, out.position(), out.remaining());
            // Set the writer index so the amount of written bytes is reflected
            output.writerIndex(output.writerIndex() + written);
        }
        catch (Exception e)
        {
            // release output buffer so we not leak and rethrow exception.
            output.release();
            throw e;
        }
        return output;
    }

    @Override
    protected ByteBuf packHeap(ByteBuf body, boolean prependWithUncompressedLength)
    {
        int maxCompressedLength = compressor.maxCompressedLength(body.readableBytes());

        // Not a direct buffer so use byte arrays...
        int inOffset = body.arrayOffset() + body.readerIndex();
        byte[] in = body.array();
        int len = body.readableBytes();
        // Increase reader index.
        body.readerIndex(body.writerIndex());

        // Allocate a heap buffer from the ByteBufAllocator as we may use a PooledByteBufAllocator and
        // so can eliminate the overhead of allocate a new byte[].
        ByteBuf output = body.alloc().heapBuffer((prependWithUncompressedLength ? 4 : 0) + maxCompressedLength);
        try
        {
            if (prependWithUncompressedLength)
            {
                output.writeInt(len);
            }
            // calculate the correct offset.
            int offset = output.arrayOffset() + output.writerIndex();
            byte[] out = output.array();
            int written = compressor.compress(in, inOffset, len, out, offset);

            // Set the writer index so the amount of written bytes is reflected
            output.writerIndex(output.writerIndex() + written);
        }
        catch (Exception failException)
        {
            // release output buffer so we not leak and rethrow exception.
            output.release();
            throw failException;
        }
        return output;
    }

    @Override
    protected int readUncompressedLength(ByteBuf body)
    {
        return body.readInt();
    }

    @Override
    protected ByteBuf unpackDirect(ByteBuf input, int length)
    {
        // If the input is direct we will allocate a direct output buffer as well as this will allow us
        // to use LZ4Compressor.decompress and so eliminate memory copies.
        int readable = input.readableBytes();
        ByteBuffer in = inputNioBuffer(input);
        // Increase reader index.
        input.readerIndex(input.writerIndex());
        ByteBuf output = input.alloc()
                .directBuffer(length);
        try
        {
            ByteBuffer out = outputNioBuffer(output);
            int read = decompressor.decompress(in, in.position(), out, out.position(), out.remaining());
            if (read != readable)
            {
                throw new IllegalArgumentException("Compressed lengths mismatch");
            }

            // Set the writer index so the amount of written bytes is reflected
            output.writerIndex(output.writerIndex() + length);
        }
        catch (Exception failException)
        {
            // release output buffer so we not leak and rethrow exception.
            output.release();
            throw failException;
        }
        return output;
    }

    @Override
    protected ByteBuf unpackHeap(ByteBuf input, int length)
    {
        // Not a direct buffer so use byte arrays...
        byte[] in = input.array();
        int len = input.readableBytes();
        int inOffset = input.arrayOffset() + input.readerIndex();
        // Increase reader index.
        input.readerIndex(input.writerIndex());

        // Allocate a heap buffer from the ByteBufAllocator as we may use a PooledByteBufAllocator and
        // so can eliminate the overhead of allocate a new byte[].
        ByteBuf output = input.alloc().heapBuffer(length);
        try
        {
            int offset = output.arrayOffset() + output.writerIndex();
            byte[] out = output.array();
            int read = decompressor.decompress(in, inOffset, out, offset, length);
            if (read != len)
            {
                throw new IllegalArgumentException("Compressed lengths mismatch");
            }

            // Set the writer index so the amount of written bytes is reflected
            output.writerIndex(output.writerIndex() + length);
        }
        catch (Exception failException)
        {
            // release output buffer so we not leak and rethrow exception.
            output.release();
            throw failException;
        }
        return output;
    }
}
