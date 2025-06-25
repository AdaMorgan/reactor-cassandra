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

package com.github.adamorgan.internal.utils;

import java.math.BigDecimal;

public final class Helpers
{
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

    public static <T extends Throwable> T appendCause(T throwable, Throwable cause)
    {
        Throwable t = throwable;
        while (t.getCause() != null)
            t = t.getCause();
        t.initCause(cause);
        return throwable;
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

    public static int codePointLength(final CharSequence string)
    {
        return (int) string.codePoints().count();
    }
}
