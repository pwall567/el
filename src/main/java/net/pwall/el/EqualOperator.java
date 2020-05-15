/*
 * @(#) EqualOperator.java
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
 * An implementation class for the 'equal' operator.
 *
 * @author  Peter Wall
 */
public class EqualOperator extends EqualityOperator implements LogicalOperator {

    public static final String name = "==";

    /**
     * Construct an 'equal' operation with the given operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     */
    public EqualOperator(Expression left, Expression right) {
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
     * Evaluate the subexpression.  This method delegates the bulk of the work to the
     * {@code compare()} method.
     *
     * @return  the boolean object resulting from the comparison operation
     * @throws  EvaluationException on any errors
     */
    @Override
    public Object evaluate() throws EvaluationException {
        return compare() ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Optimize the subexpression.  Optimize the operands, and if both are constant, execute
     * the operation.
     *
     * @return  the constant result or the operator itself
     */
    @Override
    public Expression optimize() {
        if (optimizeOperands()) {
            try {
                return compare() ? Constant.trueConstant : Constant.falseConstant;
            }
            catch (EvaluationException ee) {
                // ignore
            }
        }
        return this;
    }

    /**
     * Logically invert the operation.
     *
     * @return   the logical inversion of the operation
     */
    @Override
    public Expression invert() {
        return new NotEqualOperator(getLeft(), getRight());
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is an {@code Equal}
     * operation with equal operands.
     *
     * @param o  the object for comparison
     * @return   {@code true} if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof EqualOperator && super.equals(o);
    }

}
