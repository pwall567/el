/*
 * @(#) FunctionsTest.java
 *
 * JSTL Expression Language Parser / Evaluator
 * Copyright (C) 2003, 2005, 2006, 2007, 2012, 2013, 2020  Peter Wall
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

package net.pwall.el.test;

import org.junit.Test;
import static org.junit.Assert.*;

import net.pwall.el.Functions;

public class FunctionsTest {

    @Test
    public void shouldPerformContainsFunction() {
        String str1 = "abcdef";
        String str2 = "bc";
        assertTrue(Functions.contains(str1, str2));
        assertFalse(Functions.contains(str2, str1));
    }

    @Test
    public void shouldPerformContainsIgnoreCaseFunction() {
        String str1 = "abcdef";
        assertTrue(Functions.containsIgnoreCase(str1, "bc"));
        assertTrue(Functions.containsIgnoreCase(str1, "BC"));
        assertFalse(Functions.containsIgnoreCase(str1, "XY"));
    }

    @Test
    public void shouldPerformEndsWithFunction() {
        String str1 = "abcdef";
        assertTrue(Functions.endsWith(str1, "ef"));
        assertFalse(Functions.endsWith(str1, "ee"));
    }

}
