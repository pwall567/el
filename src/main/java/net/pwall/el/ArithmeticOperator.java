/*
 * @(#) ArithmeticOperator.java
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
 * A base class to represent most arithmetic operations.
 *
 * @author  Peter Wall
 */
public abstract class ArithmeticOperator extends DiadicOperator {

    /**
     * Construct an arithmetic operation with the given operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     */
    public ArithmeticOperator(Expression left, Expression right) {
        super(left, right);
    }

    /**
     * Evaluate the subexpression.  This method determines the type of operands supplied,
     * then calls the appropriate {@code execute()} method in the implementing class to
     * apply the operation.
     *
     * @return  the object resulting from the arithmetic operation
     * @throws  EvaluationException on any errors
     */
    @Override
    public Object evaluate() throws EvaluationException {
        Object left = getLeft().evaluate();
        Object right = getRight().evaluate();
        if (left == null && right == null)
            return 0;
        if (floatOrStringOperand(left) || floatOrStringOperand(right))
            return execute(asDouble(left), asDouble(right));
        long result = execute(asLong(left), asLong(right));
        if (result >= Integer.MIN_VALUE && result <= Integer.MAX_VALUE)
            return (int)result;
        return result;
    }

    /**
     * Get the result type of this operator.  The result type of an arithmetic operation is
     * always {@link Number} or a subclass.
     *
     * @return  the result type of the operator
     */
    @Override
    public Class<?> getType() {
        Class<?> leftType = getLeft().getType();
        Class<?> rightType = getRight().getType();
        if (leftType == Double.class || leftType == Float.class ||
                rightType == Double.class || rightType == Float.class)
            return Double.class;
        if ((leftType == Integer.class || leftType == Long.class ||
                leftType == Short.class|| leftType == Byte.class) &&
                (rightType == Integer.class || rightType == Long.class ||
                        rightType == Short.class || rightType == Byte.class))
            return Long.class;
        return Number.class; // indeterminate, but must be a number
    }

    /**
     * Apply the operation to {@code long} operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     * @return       the result
     */
    public abstract long execute(long left, long right);

    /**
     * Apply the operation to {@code double} operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     * @return       the result
     */
    public abstract double execute(double left, double right);

}
