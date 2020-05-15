/*
 * @(#) LessOrEqualOperator.java
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
 * An implementation class for the 'less than or equal' operator.
 *
 * @author  Peter Wall
 */
public class LessOrEqualOperator extends RelativeOperator implements LogicalOperator {

    public static final String name = "<=";

    /**
     * Construct a 'less than or equal' operation with the given operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     */
    public LessOrEqualOperator(Expression left, Expression right) {
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
     * The result if the left operand is less than the right.
     *
     * @return  {@code true}
     */
    @Override
    public boolean less() {
        return true;
    }

    /**
     * The result if the operands are equal.
     *
     * @return  {@code true}
     */
    @Override
    public boolean equal() {
        return true;
    }

    /**
     * The result if the left operand is greater than the right.
     *
     * @return  {@code false}
     */
    @Override
    public boolean greater() {
        return false;
    }

    /**
     * Logically invert the operation.
     *
     * @return   the logical inversion of the operation
     */
    @Override
    public Expression invert() {
        return new GreaterThanOperator(getLeft(), getRight());
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is a
     * {@code LessEqual} operation with equal operands, or a {@link GreaterOrEqualOperator} with equal
     * operands reversed.
     *
     * @param o  the object for comparison
     * @return   {@code true} if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        if (o instanceof LessOrEqualOperator && super.equals(o))
            return true;
        if (!(o instanceof GreaterOrEqualOperator))
            return false;
        GreaterOrEqualOperator op = (GreaterOrEqualOperator)o;
        return getLeft().equals(op.getRight()) && getRight().equals(op.getLeft());
    }

}
