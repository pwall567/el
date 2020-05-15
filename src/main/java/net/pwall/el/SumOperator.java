/*
 * @(#) SumOperator.java
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

import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * A class to represent the <code>sum</code> operation.  This is an extension to the JSTL
 * 1.0 EL specification.
 *
 * @author  Peter Wall
 */
public class SumOperator extends Operator {

    public static final String name = "sum";
    public static final int priority = 7;

    /**
     * Construct a {@code sum} operator with the given operand.
     *
     * @param operand  the right operand
     */
    public SumOperator(Expression operand) {
        super(operand);
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
     * Get the result type of this operator.  The result type of a sum operation is
     * {@link Number}.
     *
     * @return  the result type of the operator
     */
    @Override
    public Class<?> getType() {
        return Number.class;
    }

    /**
     * Optimize the subexpression.  The operand is optimized, but no attempt is made to
     * optimize this operation.
     *
     * @return  the operator itself
     */
    @Override
    public Expression optimize() {
        optimizeRightOperand();
        return this;
    }

    /**
     * Evaluate the subexpression.  Evaluate the operand and, if it is a {@link Map},
     * {@link List} or array, return the sum of the elements.
     *
     * @return  the length of the array
     * @throws  EvaluationException on any errors
     */
    @Override
    public Object evaluate() throws EvaluationException {
        Object right = getRight().evaluate();
        try {
            if (right instanceof Map)
                return calculateSum(((Map<?, ?>)right).values().iterator());
            if (right instanceof List)
                return calculateSum(((List<?>)right).iterator());
            if (right.getClass().isArray())
                return calculateSum(new ArrayIterator(right));
        }
        catch (RuntimeException ignored) {
        }
        throw new SumException();
    }

    /**
     * Calculate the sum.  The sum is returned as a {@code Long} if all the values are
     * integral, or a {@code Double} otherwise.
     *
     * @param i an {@link Iterator} over the array or {@link List} or {@link Map}
     * @return a {@code Long} or {@code Double} containing the sum
     * @throws ClassCastException if an element is not a {@link Number} or {@link String}
     * @throws NumberFormatException if a {@link String} element can not be converted to a
     *         number
     */
    private Object calculateSum(Iterator<?> i) {
        long longTotal = 0L;
        while (i.hasNext()) {
            Object value = i.next();
            if (value != null) {
                if (value instanceof Long || value instanceof Integer ||
                        value instanceof Short || value instanceof Byte)
                    longTotal += ((Number)value).longValue();
                else if (value instanceof String && !floatString(value))
                    longTotal += Long.parseLong((String)value);
                else {
                    double doubleTotal = longTotal;
                    for (;;) {
                        if (value != null)
                            doubleTotal += value instanceof String ?
                                    Double.parseDouble((String)value) :
                                    ((Number)value).doubleValue();
                        if (!i.hasNext())
                            break;
                        value = i.next();
                    }
                    return doubleTotal;
                }
            }
        }
        return longTotal;
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is a {@code Sum}
     * operation with an equal operand.
     *
     * @param o  the object for comparison
     * @return   {@code true} if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof SumOperator && super.equals(o);
    }

}
