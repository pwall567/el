/*
 * @(#) Functions.java
 *
 * JSTL Expression Language Parser / Evaluator
 * Copyright (C) 2015, 2020  Peter Wall
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.pwall.el;

import java.util.List;
import java.util.Map;

import net.pwall.util.Strings;
import net.pwall.xml.XML;

/**
 * Functions for JSTL Expression Language.
 *
 * @author  Peter Wall
 */
public class Functions {

    /**
     * Tests if an input string contains the specified substring.
     *
     * @param   str1    the input string
     * @param   str2    the string to test for
     * @return  {@code true} if the second string is contained in the first
     */
    public static boolean contains(String str1, String str2) {
        return str1.contains(str2);
    }

    /**
     * Tests if an input string contains the specified substring in a case insensitive way.
     *
     * @param   str1    the input string
     * @param   str2    the string to test for
     * @return  {@code true} if the second string is contained in the first
     */
    public static boolean containsIgnoreCase(String str1, String str2) {
        return str1.toUpperCase().contains(str2.toUpperCase());
    }

    /**
     * Tests if an input string ends with the specified suffix.
     *
     * @param   str1    the input string
     * @param   str2    the suffix
     * @return  {@code true} if the input string ends with the suffix
     */
    public static boolean endsWith(String str1, String str2) {
        return str1.endsWith(str2);
    }

    /**
     * Escapes characters that could be interpreted as XML markup.  For example, &lt; will be
     * converted to &amp;lt;.
     *
     * @param   str     the string to escape
     * @return  the escaped string
     */
    public static String escapeXML(String str) {
        return XML.escape(str);
    }

    /**
     * Returns the index within a string of the first occurrence of a specified substring.
     *
     * @param   str1    the input string
     * @param   str2    the substring
     * @return  the index, or -1 if the substring is not found
     */
    public static int indexOf(String str1, String str2) {
        return str1.indexOf(str2);
    }

    /**
     * Joins all elements of an array into a string.
     *
     * @param   strings         the array of strings
     * @param   separator       the separator
     * @return  a single string consisting of the array, joined by the separator
     */
    public static String join(String[] strings, String separator) {
        return Strings.join(strings, separator);
    }

    /**
     * Returns the number of items in a collection, or the number of characters in a string.
     *
     * @param   object  the input object
     * @return  the number of items or characters
     */
    public static int length(Object object) {
        if (object instanceof CharSequence)
            return ((CharSequence)object).length();
        if (object instanceof Map)
            return ((Map<?, ?>)object).size();
        if (object instanceof List)
            return ((List<?>)object).size();
        if (object instanceof Object[])
            return ((Object[])object).length;
        return 0;
    }

    /**
     * Returns a string resulting from replacing in an input string all occurrences of a
     * "before" string into an "after" substring.
     *
     * @param   input   the input string
     * @param   before  the string to be replaced
     * @param   after   the replacement
     * @return  the string after replacement
     */
    public static String replace(String input, String before, String after) {
        return input.replace(before, after);
    }

    /**
     * Splits a string into an array of substrings.
     *
     * @param   str1    the string to be split
     * @param   str2    the separator
     * @return  the array
     */
    public static String[] split(String str1, String str2) {
        return Strings.split(str1, str2);
    }

    /**
     * Tests if an input string starts with the specified prefix.
     *
     * @param   str1    the input string
     * @param   str2    the prefix
     * @return  {@code true} if the input string starts with the prefix
     */
    public static boolean startsWith(String str1, String str2) {
        return str1.startsWith(str2);
    }

    /**
     * Returns a subset of a string.
     *
     * @param   str     the input string
     * @param   start   the start index (inclusive)
     * @param   end     the end index (exclusive)
     * @return  the substring
     */
    public static String substring(String str, int start, int end) {
        return str.substring(start, end);
    }

    /**
     * Returns a subset of a string following a specific substring.
     *
     * @param   str1    the input string
     * @param   str2    the separator
     * @return  the substring
     */
    public static String substringAfter(String str1, String str2) {
        int i = str1.indexOf(str2);
        return i < 0 ? "" : str1.substring(i + str2.length());
    }

    /**
     * Returns a subset of a string before a specific substring.
     *
     * @param   str1    the input string
     * @param   str2    the separator
     * @return  the substring
     */
    public static String substringBefore(String str1, String str2) {
        int i = str1.indexOf(str2);
        return i < 0 ? str1 : str1.substring(0, i);
    }

    /**
     * Converts all of the characters of a string to lower case.
     *
     * @param   str    the input string
     * @return  the converted string
     */
    public static String toLowerCase(String str) {
        return str.toLowerCase();
    }

    /**
     * Converts all of the characters of a string to upper case.
     *
     * @param   str    the input string
     * @return  the converted string
     */
    public static String toUpperCase(String str) {
        return str.toUpperCase();
    }

    /**
     * Removes white spaces from both ends of a string.
     *
     * @param   str    the input string
     * @return  the trimmed string
     */
    public static String trim(String str) {
        return str.trim();
    }

}
