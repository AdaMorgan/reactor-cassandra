package com.github.adamorgan.api.requests;

import com.github.adamorgan.internal.utils.Checks;

import javax.annotation.Nonnull;
import java.util.Collection;
import java.util.EnumSet;

public enum NetworkIntent
{
    TOPOLOGY_CHANGE(1),
    STATUS_CHANGE(2),
    SCHEMA_CHANGE(3);

    public static final int DEFAULT = 0;

    private final int rawValue;
    private final int offset;

    NetworkIntent(int offset)
    {
        this.offset = offset;
        this.rawValue = 1 << offset;
    }

    /**
     * The raw bitmask value for this intent
     *
     * @return The raw bitmask value
     */
    public int getRawValue()
    {
        return rawValue;
    }

    /**
     * The offset of the intent flag within a bitmask
     * <br>This means {@code getRawValue() == 1 << getOffset()}
     *
     * @return The offset
     */
    public int getOffset()
    {
        return offset;
    }

    /**
     * Converts a bitmask into an {@link EnumSet} of enum values.
     *
     * @param raw The raw bitmask
     * @return {@link EnumSet} of intents
     */
    @Nonnull
    public static EnumSet<NetworkIntent> getIntents(int raw)
    {
        EnumSet<NetworkIntent> set = EnumSet.noneOf(NetworkIntent.class);
        for (NetworkIntent intent : values())
        {
            if ((intent.getRawValue() & raw) != 0)
            {
                set.add(intent);
            }
        }
        return set;
    }

    /**
     * Converts the given intents to a bitmask
     *
     * @param set The {@link Collection} of intents
     * @return The bitmask for this set of intents
     *
     * @throws IllegalArgumentException If null is provided
     */
    public static int getRaw(@Nonnull Collection<NetworkIntent> set)
    {
        int raw = 0;
        for (NetworkIntent intent : set)
        {
            raw |= intent.rawValue;
        }
        return raw;
    }

    /**
     * Converts the given intents to a bitmask
     *
     * @param intent The first intent
     * @param set    The remaining intents
     * @return The bitmask for this set of intents
     *
     * @throws IllegalArgumentException If null is provided
     */
    public static int getRaw(@Nonnull NetworkIntent intent, @Nonnull NetworkIntent... set)
    {
        Checks.notNull(intent, "Intent");
        Checks.notNull(set, "Intent");
        return getRaw(EnumSet.of(intent, set));
    }
}
