package com.github.adamorgan.internal.utils;

import javax.annotation.Nullable;
import java.math.BigDecimal;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.temporal.TemporalAccessor;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * This class has major inspiration from <a href="https://commons.apache.org/proper/commons-lang/" target="_blank">Lang 3</a>
 *
 * <p>Specifically StringUtils.java and ExceptionUtils.java
 */
public final class Helpers
{
    private static final ZoneOffset OFFSET = ZoneOffset.of("+00:00");
    @SuppressWarnings("rawtypes")
    private static final Consumer EMPTY_CONSUMER = (v) ->
    {
    };

    public static void setContent(StringBuilder builder, String content)
    {
        if (content != null)
        {
            content = content.trim();
            builder.setLength(0);
            builder.append(content);
        }
        else
        {
            builder.setLength(0);
        }
    }

    public static boolean isDecimal(String number)
    {
        try
        {
            new BigDecimal(number);
            return true;
        }
        catch (NumberFormatException failure)
        {
            return false;
        }
    }

    @SuppressWarnings("unchecked")
    public static <T> Consumer<T> emptyConsumer()
    {
        return (Consumer<T>) EMPTY_CONSUMER;
    }

    public static OffsetDateTime toOffset(long instant)
    {
        return OffsetDateTime.ofInstant(Instant.ofEpochMilli(instant), OFFSET);
    }

    public static long toTimestamp(String iso8601String)
    {
        TemporalAccessor joinedAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.parse(iso8601String);
        return Instant.from(joinedAt).toEpochMilli();
    }

    public static OffsetDateTime toOffsetDateTime(@Nullable TemporalAccessor temporal)
    {
        if (temporal == null)
        {
            return null;
        }
        else
        {
            if (temporal instanceof OffsetDateTime)
            {
                return (OffsetDateTime) temporal;
            }
            else
            {
                ZoneOffset offset;
                try
                {
                    offset = ZoneOffset.from(temporal);
                }
                catch (DateTimeException ignore)
                {
                    offset = ZoneOffset.UTC;
                }
                try
                {
                    LocalDateTime ldt = LocalDateTime.from(temporal);
                    return OffsetDateTime.of(ldt, offset);
                }
                catch (DateTimeException ignore)
                {
                    try
                    {
                        Instant instant = Instant.from(temporal);
                        return OffsetDateTime.ofInstant(instant, offset);
                    }
                    catch (DateTimeException ex)
                    {
                        throw new DateTimeException("Unable to obtain OffsetDateTime from TemporalAccessor: " + temporal + " of type " + temporal.getClass().getName(), ex);
                    }
                }
            }
        }
    }

    // locale-safe String#format

    public static String format(String format, Object... args)
    {
        return String.format(Locale.ROOT, format, args);
    }

    // ## StringUtils ##

    public static boolean isEmpty(final CharSequence seq)
    {
        return seq == null || seq.length() == 0;
    }

    public static boolean containsWhitespace(final CharSequence seq)
    {
        if (isEmpty(seq))
        {
            return false;
        }
        for (int i = 0; i < seq.length(); i++)
        {
            if (Character.isWhitespace(seq.charAt(i)))
            {
                return true;
            }
        }
        return false;
    }

    public static boolean isBlank(final CharSequence seq)
    {
        if (isEmpty(seq))
        {
            return true;
        }
        for (int i = 0; i < seq.length(); i++)
        {
            if (!Character.isWhitespace(seq.charAt(i)))
            {
                return false;
            }
        }
        return true;
    }

    public static int countMatches(final CharSequence seq, final char c)
    {
        if (isEmpty(seq))
        {
            return 0;
        }
        int count = 0;
        for (int i = 0; i < seq.length(); i++)
        {
            if (seq.charAt(i) == c)
            {
                count++;
            }
        }
        return count;
    }

    public static String truncate(final String input, final int maxWidth)
    {
        if (input == null)
        {
            return null;
        }
        Checks.notNegative(maxWidth, "maxWidth");
        if (input.length() <= maxWidth)
        {
            return input;
        }
        if (maxWidth == 0)
        {
            return "";
        }
        return input.substring(0, maxWidth);
    }

    public static String rightPad(final String input, final int size)
    {
        int pads = size - input.length();
        if (pads <= 0)
        {
            return input;
        }
        StringBuilder out = new StringBuilder(input);
        for (int i = pads; i > 0; i--)
        {
            out.append(' ');
        }
        return out.toString();
    }

    public static String leftPad(final String input, final int size)
    {
        int pads = size - input.length();
        if (pads <= 0)
        {
            return input;
        }
        StringBuilder out = new StringBuilder();
        for (int i = pads; i > 0; i--)
        {
            out.append(' ');
        }
        return out.append(input).toString();
    }

    public static boolean isNumeric(final String input)
    {
        if (isEmpty(input))
        {
            return false;
        }
        for (char c : input.toCharArray())
        {
            if (!Character.isDigit(c))
            {
                return false;
            }
        }
        return true;
    }

    public static int codePointLength(final CharSequence string)
    {
        return (int) string.codePoints().count();
    }

    public static String[] split(String input, String match)
    {
        List<String> out = new ArrayList<>();
        int i = 0;
        while (i < input.length())
        {
            int j = input.indexOf(match, i);
            if (j == -1)
            {
                out.add(input.substring(i));
                break;
            }

            out.add(input.substring(i, j));
            i = j + match.length();
        }

        return out.toArray(new String[0]);
    }

    public static boolean equals(String a, String b, boolean ignoreCase)
    {
        return ignoreCase ? a == b || (a != null && b != null && a.equalsIgnoreCase(b)) : Objects.equals(a, b);
    }

    // ## CollectionUtils ##

    public static boolean deepEquals(Collection<?> first, Collection<?> second)
    {
        if (first == second)
        {
            return true;
        }
        if (first == null || second == null || first.size() != second.size())
        {
            return false;
        }
        for (Iterator<?> itFirst = first.iterator(), itSecond = second.iterator(); itFirst.hasNext(); )
        {
            Object elementFirst = itFirst.next();
            Object elementSecond = itSecond.next();
            if (!Objects.equals(elementFirst, elementSecond))
            {
                return false;
            }
        }
        return true;
    }

    public static boolean deepEqualsUnordered(Collection<?> first, Collection<?> second)
    {
        if (first == second)
        {
            return true;
        }
        if (first == null || second == null)
        {
            return false;
        }
        return first.size() == second.size() && second.containsAll(first);
    }

    public static <E extends Enum<E>> EnumSet<E> copyEnumSet(Class<E> clazz, Collection<E> col)
    {
        return col == null || col.isEmpty() ? EnumSet.noneOf(clazz) : EnumSet.copyOf(col);
    }

    @SafeVarargs
    public static <T> Set<T> setOf(T... elements)
    {
        Set<T> set = new HashSet<>(elements.length);
        Collections.addAll(set, elements);
        return set;
    }

    @SafeVarargs
    public static <T> List<T> listOf(T... elements)
    {
        return Collections.unmodifiableList(Arrays.asList(elements));
    }

    // ## ExceptionUtils ##

    public static <T extends Throwable> T appendCause(T throwable, Throwable cause)
    {
        Throwable t = throwable;
        while (t.getCause() != null)
        {
            t = t.getCause();
        }
        t.initCause(cause);
        return throwable;
    }

    public static boolean hasCause(Throwable throwable, Class<? extends Throwable> cause)
    {
        Throwable cursor = throwable;
        while (cursor != null)
        {
            if (cause.isInstance(cursor))
            {
                return true;
            }
            cursor = cursor.getCause();
        }
        return false;
    }

    public static <T> Collector<T, ?, List<T>> toUnmodifiableList()
    {
        return Collectors.collectingAndThen(Collectors.toList(), Collections::unmodifiableList);
    }

    public static <E extends Enum<E>> Collector<E, ?, Set<E>> toUnmodifiableEnumSet(Class<E> enumType)
    {
        return Collectors.collectingAndThen(Collectors.toCollection(() -> EnumSet.noneOf(enumType)), Collections::unmodifiableSet);
    }

    @SafeVarargs
    public static <E extends Enum<E>> Set<E> unmodifiableEnumSet(E first, E... rest)
    {
        return Collections.unmodifiableSet(EnumSet.of(first, rest));
    }

    public static String durationToString(Duration duration, TimeUnit resolutionUnit)
    {
        long actual = resolutionUnit.convert(duration.getSeconds(), TimeUnit.SECONDS);
        String raw = actual + " " + resolutionUnit.toString().toLowerCase(Locale.ROOT);

        long days = duration.toDays();
        long hours = duration.toHours() % 24;
        long minutes = duration.toMinutes() % 60;
        long seconds = duration.getSeconds() - TimeUnit.DAYS.toSeconds(days) - TimeUnit.HOURS.toSeconds(hours) - TimeUnit.MINUTES.toSeconds(minutes);

        StringJoiner joiner = new StringJoiner(" ");
        if (days > 0)
        {
            joiner.add(days + " days");
        }
        if (hours > 0)
        {
            joiner.add(hours + " hours");
        }
        if (minutes > 0)
        {
            joiner.add(minutes + " minutes");
        }
        if (seconds > 0)
        {
            joiner.add(seconds + " seconds");
        }

        return raw + " (" + joiner + ")";
    }
}
