/*
 * @(#) PlusOperator.java
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
 * An implementation class for the plus operation.
 *
 * @author  Peter Wall
 */
public class PlusOperator extends ArithmeticOperator {

    public static final String name = "+";
    public static final int priority = 5;

    /**
     * Construct a plus operation with the given operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     */
    public PlusOperator(Expression left, Expression right) {
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
     * Return {@code true} - the operation is commutative.
     *
     * @return {@code true}
     */
    @Override
    public boolean isCommutative() {
        return true;
    }

    /**
     * Return {@code true} - the operation is associative.
     *
     * @return {@code true}
     */
    @Override
    public boolean isAssociative() {
        return true;
    }

    /**
     * Apply the operation to {@code long} operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     * @return       the result
     */
    @Override
    public long execute(long left, long right) {
        return left + right;
    }

    /**
     * Apply the operation to {@code double} operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     * @return       the result
     */
    @Override
    public double execute(double left, double right) {
        return left + right;
    }

    /**
     * Optimize the subexpression.  In addition to the default optimization, check for
     * addition of 0.
     *
     * @return  the constant result or the operator itself
     */
    @Override
    public Expression optimize() {
        if (optimizeOperands()) {
            try {
                return new Constant(evaluate());
            }
            catch (EvaluationException ee) {
                // ignore
            }
        }
        Expression left = getLeft();
        Expression right = getRight();
        if (right.isConstant()) {
            Object val = ((Constant)right).getValue();
            if (val instanceof Integer) {
                if ((Integer)val == 0)
                    return left;
            }
            else if (val instanceof Long) {
                if ((Long)val == 0)
                    return left;
            }
            else if (val instanceof Number) {
                if (((Number)val).doubleValue() == 0.0)
                    return left;
            }
        }
        if (left.isConstant()) {
            Object val = ((Constant)left).getValue();
            if (val instanceof Integer) {
                if ((Integer)val == 0)
                    return right;
            }
            else if (val instanceof Long) {
                if ((Long)val == 0)
                    return right;
            }
            else if (val instanceof Number) {
                if (((Number)val).doubleValue() == 0.0)
                    return right;
            }
        }
        return this;
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is a {@code Plus}
     * operation with equal operands.
     *
     * @param o  the object for comparison
     * @return   {@code true} if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof PlusOperator && super.equals(o);
    }

}
