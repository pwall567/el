/*
 * @(#) ToLowerOperator.java
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
 * A class to represent the 'convert to lower case' operation.  This is an extension to the
 * JSTL 1.0 EL specification.
 *
 * @author  Peter Wall
 */
public class ToLowerOperator extends Operator {

    public static final String name = "tolower";
    public static final int priority = 7;

    /**
     * Construct a 'convert to lower case' operator with the given operand.
     *
     * @param operand  the right operand
     */
    public ToLowerOperator(Expression operand) {
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
     * Get the result type of this operator.  The result type of a case conversion operation
     * is always {@link String}.
     *
     * @return  the result type of the operator
     */
    @Override
    public Class<?> getType() {
        return String.class;
    }

    /**
     * Evaluate the subexpression.  Evaluate the operand as a {@link String} and convert it
     * to lower case.
     *
     * @return  the converted string
     * @throws  EvaluationException on any errors
     */
    @Override
    public Object evaluate() throws EvaluationException {
        return getRight().asString().toLowerCase();
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is a {@code ToUpper}
     * operation with an equal operand.
     *
     * @param o  the object for comparison
     * @return   {@code true} if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof ToLowerOperator && super.equals(o);
    }

}
