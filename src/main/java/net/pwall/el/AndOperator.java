/*
 * @(#) AndOperator.java
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
 * A class to represent the logical AND operation.
 *
 * @author  Peter Wall
 */
public class AndOperator extends DiadicOperator implements LogicalOperator {

    public static final String name = "&&";
    public static final int priority = 2;

    /**
     * Construct an AND operation with the given operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     */
    public AndOperator(Expression left, Expression right) {
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
     * Get the result type of this operator.  The result type of an AND operation is always
     * {@link Boolean}.
     *
     * @return  the result type of the operator
     */
    @Override
    public Class<?> getType() {
        return Boolean.class;
    }

    /**
     * Evaluate the subexpression.  This method evaluates the left operand, and only if it
     * is {@code true} evaluates the right operand.
     *
     * @return  the boolean object resulting from the AND operation
     * @throws  EvaluationException on any errors
     */
    @Override
    public Object evaluate() throws EvaluationException {
        if (!getLeft().asBoolean())
            return Boolean.FALSE;
        return getRight().asBoolean() ? Boolean.TRUE : Boolean.FALSE;
    }

    /**
     * Optimize the subexpression.  Optimize the operands, then, if either operand is
     * constant, return {@code false} if it is {@code false}, otherwise the other operand.
     *
     * @return  the constant result or the operator itself
     */
    @Override
    public Expression optimize() {
        optimizeOperands();
        Expression left = getLeft();
        Expression right = getRight();
        if (left.isConstant()) {
            try {
                return left.asBoolean() ? right : Constant.falseConstant;
            }
            catch (EvaluationException ignored) {
            }
        }
        if (right.isConstant()) {
            try {
                return right.asBoolean() ? left : Constant.falseConstant;
            }
            catch (EvaluationException ignored) {
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
        Expression notLeft = new NotOperator(getLeft());
        Expression notRight = new NotOperator(getRight());
        return new OrOperator(notLeft.optimize(), notRight.optimize());
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is an {@code And}
     * operation with equal operands.
     *
     * @param o  the object for comparison
     * @return   {@code true} if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof AndOperator && super.equals(o);
    }

}
