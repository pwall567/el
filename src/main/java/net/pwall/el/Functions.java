/*
 * @(#) Functions.java
 *
 * JSTL Expression Language Parser / Evaluator
 * Copyright (C) 2015 Peter Wall
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package net.pwall.el;

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

import net.pwall.util.Strings;
import net.pwall.xml.XML;

/**
 * Functions for JSTL Expression Language.
 * 
 * @author Peter Wall
 */
public class Functions {

    /**
     * Tests if an input string contains the specified substring.
     *
     * @param   str1    the input string
     * @param   str2    the string to test for
     * @return  {@code true} if the second string is contained in the first
     */
    public boolean contains(String str1, String str2) {
        return str1.indexOf(str2) >= 0;
    }

    /**
     * Tests if an input string contains the specified substring in a case insensitive way.
     *
     * @param   str1    the input string
     * @param   str2    the string to test for
     * @return  {@code true} if the second string is contained in the first
     */
    public boolean containsIgnoreCase(String str1, String str2) {
        return str1.toUpperCase().indexOf(str2.toUpperCase()) >= 0;
    }

    public boolean endsWith(String str1, String str2) {
        return str1.endsWith(str2);
    }

    public String escapeXML(String str) {
        return XML.escape(str);
    }

    public int indexOf(String str1, String str2) {
        return str1.indexOf(str2);
    }

    public String join(String[] strings, String separator) {
        return Strings.join(strings, separator);
    }

    public int length(Object object) {
        if (object instanceof CharSequence)
            return ((CharSequence)object).length();
        if (object instanceof Map)
            return ((Map<?, ?>)object).size();
        if (object instanceof List)
            return ((List<?>)object).size();
        if (object instanceof Object[])
            return ((Object[])object).length;
        if (object.getClass().isArray())
            return Array.getLength(object);
        return 0;
    }

    public String replace(String str1, String str2, String str3) {
        return str1.replace(str2, str3);
    }

    public String[] split(String str1, String str2) {
        if (str2.length() == 1)
            return Strings.split(str1, str2.charAt(0));
        // TODO
        return null;
    }

    public boolean startsWith(String str1, String str2) {
        return str1.startsWith(str2);
    }

    public String substring(String str, int start, int end) {
        return str.substring(start, end);
    }

    public String substringAfter(String str1, String str2) {
        int i = str1.indexOf(str2);
        if (i < 0)
            return str1; // TODO check
        return str1.substring(i + str2.length());
    }

    public String substringBefore(String str1, String str2) {
        int i = str1.indexOf(str2);
        if (i < 0)
            return str1; // TODO check
        return str1.substring(0, i);
    }

    public String toLowerCase(String str) {
        return str.toLowerCase();
    }

    public String toUpperCase(String str) {
        return str.toUpperCase();
    }

    public String trim(String str) {
        return str.trim();
    }

}
