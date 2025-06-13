package com.github.adamorgan.api.utils.binary;

import com.github.adamorgan.internal.utils.EncodingUtils;
import io.netty.buffer.ByteBuf;

import javax.annotation.Nonnull;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.function.*;
import java.util.stream.*;

public class BinaryCollector implements Collector<ByteBuf, BinaryArray, Stream<BinaryObject>>
{
    private final BinaryArray array;
    private final ByteBuf rawArray;

    public BinaryCollector(BinaryArray array)
    {
        this.array = array;
        this.rawArray = array.raw;
    }

    @Override
    @Nonnull
    public Supplier<BinaryArray> supplier()
    {
        return () -> array;
    }

    @Override
    public BiConsumer<BinaryArray, ByteBuf> accumulator()
    {
        return null;
    }

    @Override
    public BinaryOperator<BinaryArray> combiner()
    {
        return null;
    }

    @Override
    @Nonnull
    public Function<BinaryArray, Stream<BinaryObject>> finisher()
    {
        return array ->
        {
            List<BinaryPath> paths = IntStream.range(0, array.columnsCount).mapToObj(this::mapper).collect(Collectors.toList());
            return IntStream.range(0, rawArray.readInt()).boxed().flatMap(i -> paths.stream().map(this::mapper));
        };
    }

    @Nonnull
    public BinaryPath mapper(int expensive)
    {
        String x = !array.isGlobal ? EncodingUtils.unpackUTF84(rawArray) : array.keyspace;
        String y = !array.isGlobal ? EncodingUtils.unpackUTF84(rawArray) : array.table;
        return new BinaryPath(x, y, EncodingUtils.unpackUTF84(rawArray), rawArray.readUnsignedShort());
    }

    @Nonnull
    public BinaryObject mapper(BinaryPath path)
    {
        int length = rawArray.readInt();
        ByteBuf rawData = rawArray.readSlice(length).retain();
        return new BinaryObject(rawData, path, length);
    }

    @Override
    @Nonnull
    public Set<Characteristics> characteristics()
    {
        return Collections.emptySet();
    }
}
