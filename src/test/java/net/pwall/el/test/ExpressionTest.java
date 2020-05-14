/*
 * @(#) ExpressionTest.java
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

import net.pwall.el.Expression;
import net.pwall.el.SimpleResolver;

public class ExpressionTest {

    @Test
    public void shouldParseAndEvaluateSimpleExpression() throws Exception {
        Expression exp = Expression.parseExpression("4+3*2", null);
        Object result = exp.evaluate(); // result will contain Integer(10)
        assertEquals(10, result);
    }

    @Test
    public void shouldParseAndEvaluateSimpleSubstitution() throws Exception {
        SimpleResolver resolver =  new SimpleResolver();
        resolver.set("text", "Hello");
        resolver.set("number", 3);
        String result = Expression.substitute("text = ${text}, number = ${number * 2}.", resolver);
        assertEquals("text = Hello, number = 6.", result);
    }

    @Test
    public void shouldParseAndSeparatelyEvaluateSimpleSubstitution() throws Exception {
        SimpleResolver resolver =  new SimpleResolver();
        resolver.set("text", "Hello again");
        resolver.set("number", 99);
        Expression exp = Expression.parseSubstitution("text = ${text}, number = ${number * 2}.", resolver);
        String result = exp.asString();
        assertEquals("text = Hello again, number = 198.", result);
    }

    @Test
    public void shouldParseAndEvaluateExpressionAndConvertToInt() throws Exception {
        Expression exp = Expression.parseExpression("4+3*2", null);
        int result = exp.asInt();
        assertEquals(10, result);
    }

    @Test
    public void shouldParseAndEvaluateExpressionAndConvertToLong() throws Exception {
        Expression exp = Expression.parseExpression("4+3*2", null);
        long result = exp.asLong();
        assertEquals(10, result);
    }

    @Test
    public void shouldParseAndEvaluateExpressionAndConvertToDouble() throws Exception {
        Expression exp = Expression.parseExpression("4+3*2", null);
        double result = exp.asDouble();
        assertEquals(10.0, result, 1e-8);
    }

    @Test
    public void shouldTestForValidIdentifier() {
        assertTrue(Expression.isValidIdentifier("test"));
        assertFalse(Expression.isValidIdentifier("999"));
    }

}
