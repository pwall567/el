/*
 * @(#) EmptyOperator.java
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

import java.lang.reflect.Array;
import java.util.List;
import java.util.Map;

/**
 * A class to represent the 'test for empty' operation.
 *
 * @author  Peter Wall
 */
public class EmptyOperator extends Operator {

    public static final String name = "empty";
    public static final int priority = 7;

    /**
     * Construct a 'test for empty' operator with the given operand.
     *
     * @param operand  the right operand
     */
    public EmptyOperator(Expression operand) {
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
     * Get the result type of this operator.  The result type of a test for empty operation
     * is always {@link Boolean}.
     *
     * @return  the result type of the operator
     */
    @Override
    public Class<?> getType() {
        return Boolean.class;
    }

    /**
     * Evaluate the subexpression.  Evaluate the operand and apply the rules for testing for
     * empty.
     *
     * @return  the object resulting from the 'test for empty' operation
     * @throws  EvaluationException on any errors
     */
    @Override
    public Object evaluate() throws EvaluationException {
        Object operand = getRight().evaluate();
        if (operand == null)
            return Boolean.TRUE;
        if (operand instanceof String && ((String)operand).length() == 0)
            return Boolean.TRUE;
        if (operand instanceof Object[] && ((Object[])operand).length == 0)
            return Boolean.TRUE;
        if (operand.getClass().isArray() && Array.getLength(operand) == 0)
            return Boolean.TRUE;
        if (operand instanceof Map && ((Map<?, ?>)operand).isEmpty())
            return Boolean.TRUE;
        if (operand instanceof List && ((List<?>)operand).isEmpty())
            return Boolean.TRUE;
        return Boolean.FALSE;
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is an {@code Empty}
     * operation with an equal operand.
     *
     * @param o  the object for comparison
     * @return   {@code true} if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof EmptyOperator && super.equals(o);
    }

}
