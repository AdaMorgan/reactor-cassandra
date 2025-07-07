package com.github.assertions.checks;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.function.ThrowingConsumer;

import java.util.regex.Pattern;

import static com.github.ChecksHelper.*;

public class StringChecksAssertions extends AbstractChecksAssertions<String, StringChecksAssertions>
{
    public StringChecksAssertions(String name, ThrowingConsumer<String> callable)
    {
        super(name, callable);
    }

    public StringChecksAssertions checksNotEmpty()
    {
        throwsFor(null, isNullError(name));
        throwsFor("", isEmptyError(name));
        return this;
    }

    public StringChecksAssertions checksNotBlank()
    {
        return checksNotBlank(true);
    }

    public StringChecksAssertions checksNotBlank(boolean checkNull)
    {
        if (checkNull)
            throwsFor(null, isNullError(name));
        throwsFor("", isBlankError(name));
        throwsFor(" ", isBlankError(name));
        return this;
    }

    public StringChecksAssertions checksNotLonger(int maxLength)
    {
        String invalidInput = StringUtils.repeat("s", maxLength + 1);
        throwsFor(invalidInput, tooLongError(name, maxLength, invalidInput));
        return this;
    }

    public StringChecksAssertions checksLowercaseOnly()
    {
        throwsFor("InvalidCasing", isNotLowercase(name, "InvalidCasing"));
        return this;
    }

    public StringChecksAssertions checksRange(int minLength, int maxLength)
    {
        String tooLong = StringUtils.repeat("s", maxLength + 1);
        String tooShort = StringUtils.repeat("s", minLength - 1);
        throwsFor(tooShort, notInRangeError(name, minLength, maxLength, tooShort));
        throwsFor(tooLong, notInRangeError(name, minLength, maxLength, tooLong));
        return this;
    }

    public StringChecksAssertions checksRegex(String input, Pattern regex)
    {
        throwsFor(input, notRegexMatch(name, regex, input));
        return this;
    }

    public StringChecksAssertions checksNoWhitespace()
    {
        String input = "hello world";
        throwsFor(input, containsWhitespaceError(name, input));
        return this;
    }
}
