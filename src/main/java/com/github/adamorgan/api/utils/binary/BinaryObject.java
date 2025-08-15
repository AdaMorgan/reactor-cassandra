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

package com.github.adamorgan.api.utils.binary;

import com.github.adamorgan.internal.utils.EncodingUtils;
import com.github.adamorgan.internal.utils.LibraryLogger;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import org.slf4j.Logger;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.InetAddress;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.BiFunction;

/**
 * Represents an implement Byte Buffer used in communication with the responses Apache Cassandra.
 */
public class BinaryObject implements SerializableBinary
{
    public static final Logger LOG = LibraryLogger.getLog(BinaryObject.class);

    private final ByteBuf obj;
    private final int raw;
    private final int length;
    public final BinaryArray.Path path;

    public BinaryObject(@Nonnull ByteBuf obj, final BinaryArray.Path path, final int length)
    {
        this.obj = obj;
        this.path = path;
        this.raw = 1 << path.offset;
        this.length = length;
    }

    public int getRawValue()
    {
        return raw;
    }

    @Nonnull
    public BinaryType getType()
    {
        return BinaryType.fromValue(path.offset);
    }

    @Nullable
    public <T> T get(@Nonnull Class<T> clazz, @Nonnull BiFunction<ByteBuf, Integer, T> parser)
    {
        return this.length >= 0 ? parser.apply(obj, length) : null;
    }

    @Nullable
    public String getString()
    {
        return get(String.class, EncodingUtils::unpackUTF);
    }

    @Nonnull
    public String getString(@Nonnull String defaultValue)
    {
        String value = get(String.class, EncodingUtils::unpackUTF);
        return value == null ? defaultValue : value;
    }

    @Nullable
    public Boolean getBoolean()
    {
        return get(Boolean.class, EncodingUtils::unpackBoolean);
    }

    @Nonnull
    public Boolean getBoolean(boolean defaultValue)
    {
        Boolean value = get(Boolean.class, EncodingUtils::unpackBoolean);
        return value == null ? defaultValue : value;
    }

    @Nullable
    public Integer getInt()
    {
        return get(Integer.class, EncodingUtils::unpackInt);
    }

    @Nonnull
    public Integer getInt(int defaultValue)
    {
        Integer value = get(Integer.class, EncodingUtils::unpackInt);
        return value == null ? defaultValue : value;
    }

    @Nullable
    public Long getLong()
    {
        return get(Long.class, EncodingUtils::unpackLong);
    }

    @Nonnull
    public Long getLong(long defaultValue)
    {
        Long value = get(Long.class, EncodingUtils::unpackLong);
        return value == null ? defaultValue : value;
    }

    @Nullable
    public Double getDouble()
    {
        return get(Double.class, EncodingUtils::unpackDouble);
    }

    @Nonnull
    public Double getDouble(double defaultValue)
    {
        Double value = get(Double.class, EncodingUtils::unpackDouble);
        return value == null ? defaultValue : value;
    }

    @Nullable
    public Float getFloat()
    {
        return get(Float.class, EncodingUtils::unpackFloat);
    }

    @Nonnull
    public Float getFloat(float defaultValue)
    {
        Float value = get(Float.class, EncodingUtils::unpackFloat);
        return value == null ? defaultValue : value;
    }

    @Nullable
    public UUID getUUID()
    {
        return get(UUID.class, EncodingUtils::unpackUUID);
    }

    @Nonnull
    public UUID getUUID(UUID defaultValue)
    {
        UUID value = get(UUID.class, EncodingUtils::unpackUUID);
        return value == null ? defaultValue : value;
    }

    @Nullable
    public OffsetDateTime getTime()
    {
        return get(OffsetDateTime.class, EncodingUtils::unpackDate);
    }

    @Nonnull
    public OffsetDateTime getTime(OffsetDateTime defaultValue)
    {
        OffsetDateTime value = get(OffsetDateTime.class, EncodingUtils::unpackDate);
        return value == null ? defaultValue : value;
    }

    @Nullable
    public InetAddress getInetAddress()
    {
        return get(InetAddress.class, EncodingUtils::unpackInet);
    }

    @Nonnull
    public InetAddress getInetAddress(InetAddress defaultValue)
    {
        InetAddress value = get(InetAddress.class, EncodingUtils::unpackInet);
        return value == null ? defaultValue : value;
    }

    @Nullable
    public byte[] getBytes()
    {
        return get(byte[].class, EncodingUtils::unpackBytes);
    }

    @Nonnull
    public byte[] getBytes(byte[] defaultValue)
    {
        byte[] value = get(byte[].class, EncodingUtils::unpackBytes);
        return value == null ? defaultValue : value;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <V> Set<V> getSet()
    {
        return (Set<V>) get(Set.class, EncodingUtils::unpackSet);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public <V> Set<V> getSet(Set<V> defaultValue)
    {
        Set<V> set = get(Set.class, EncodingUtils::unpackSet);
        return set == null ? defaultValue : set;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <V> List<V> getList()
    {
        return get(List.class, EncodingUtils::unpackList);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public <V> List<V> getList(List<V> defaultValue)
    {
        List<V> list = get(List.class, EncodingUtils::unpackList);
        return list == null ? defaultValue : list;
    }

    @Nullable
    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getMap()
    {
        return get(Map.class, EncodingUtils::unpackMap);
    }

    @Nonnull
    @SuppressWarnings("unchecked")
    public <K, V> Map<K, V> getMap(Map<K, V> defaultValue)
    {
        Map<K, V> map = get(Map.class, EncodingUtils::unpackMap);
        return map == null ? defaultValue : map;
    }

    @Override
    public boolean equals(Object obj)
    {
        if (!(obj instanceof BinaryObject))
            return false;
        if (obj == this)
            return true;
        BinaryObject other = (BinaryObject) obj;
        return ByteBufUtil.equals(this.obj, other.obj);
    }

    @Nonnull
    @Override
    public BinaryObject toBinary()
    {
        return this;
    }

    @Nonnull
    @Override
    public ByteBuf asByteBuf()
    {
        return obj;
    }

    @Override
    public String toString()
    {
        return ByteBufUtil.prettyHexDump(obj);
    }
}
