/*
 * @(#) MatchOperator.java
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

package net.pwall.el;

/**
 * A class to represent the "wildcard pattern match" operation.  This is an extension to the
 * JSTL 1.0 EL specification.
 *
 * @author  Peter Wall
 */
public class MatchOperator extends DiadicOperator {

    public static final String name = "~=";
    public static final int priority = 3;
    public static final char multi = '*';
    public static final char single = '?';
    public static final char escape = '\\';

    /**
     * Construct a 'match' operation with the given operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     */
    public MatchOperator(Expression left, Expression right) {
        super(left, right);
    }

    /**
     * Get the name of the operator (the token for this operator in an expression).
     *
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Get the priority of the operator.
     *
     * @return  the priority
     */
    @Override
    public int getPriority() {
        return priority;
    }

    /**
     * Get the result type of this operator.  The result type of a match operation is always
     * {@link Boolean}.
     *
     * @return  the result type of the operator
     */
    @Override
    public Class<?> getType() {
        return Boolean.class;
    }

    /**
     * Evaluate the subexpression.  Match the left operand string against the right operand
     * pattern.
     *
     * @return the boolean result of the pattern match operation
     * @throws EvaluationException if the operands are not strings or null
     */
    @Override
    public Object evaluate() throws EvaluationException {
        Object left = getLeft().evaluate();
        Object right = getRight().evaluate();
        if (!(left == null || left instanceof String) ||
                !(right == null || right instanceof String))
            throw new EvaluationException("match");
        if (left == null || right == null)
            return Boolean.FALSE;
        return match((String)left, 0, (String)right, 0) ? Boolean.TRUE :
                Boolean.FALSE;
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is a {@code Match}
     * operation with equal operands.
     *
     * @param o  the object for comparison
     * @return   {@code true} if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof MatchOperator && super.equals(o);
    }

    /**
     * Match a string against a pattern.
     *
     * @param str      the string
     * @param strIndex the offset within the string
     * @param pat      the pattern
     * @param patIndex the offset within the pattern
     * @return         {@code true} if the string matches the pattern
     */
    private static boolean match(String str, int strIndex, String pat,
                                 int patIndex) {
        int strLen = str.length();
        int patLen = pat.length();
        while (patIndex < patLen) {
            char patChar = pat.charAt(patIndex++);
            if (patChar == multi) {
                if (patIndex >= patLen) // end of pattern
                    return true;
                for (;;) {
                    if (match(str, strIndex, pat, patIndex))
                        return true;
                    if (strIndex >= strLen)
                        return false;
                    strIndex++;
                }
            }
            if (patChar == single) {
                if (strIndex >= strLen)
                    return false;
                strIndex++;
            }
            else if (patChar == escape) {
                if (patIndex >= patLen)
                    return false; // error - escape at end of pattern
                if (strIndex >= strLen ||
                        str.charAt(strIndex++) != pat.charAt(patIndex++))
                    return false;
            }
            else {
                if (strIndex >= strLen || str.charAt(strIndex++) != patChar)
                    return false;
            }
        }
        return strIndex == strLen;
    }

}
