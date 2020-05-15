/*
 * @(#) AssignOperator.java
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
 * A class to represent the assignment operation.  This is an extension to the JSTL 1.0 EL
 * specification.
 *
 * @author  Peter Wall
 */
public class AssignOperator extends DiadicOperator {

    public static final String name = "=";
    public static final int priority = -1;

    /**
     * Construct an <code>Assign</code> operation with the given operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     */
    public AssignOperator(Expression left, Expression right) {
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
     * Return {@code false} - the operation is right-associative.
     *
     * @return {@code false}
     */
    @Override
    public boolean isLeftAssociative() {
        return false;
    }

    /**
     * Get the result type of this operator.  The result type is the type of the right
     * operand.
     *
     * @return  the result type of the operator
     */
    @Override
    public Class<?> getType() {
        return getRight().getType();
    }

    /**
     * Evaluate the subexpression.  The result of an assignment operation is the value
     * assigned.
     *
     * @return  the object resulting from the assignment operation
     * @throws  EvaluationException on any errors
     */
    @Override
    public Object evaluate() throws EvaluationException {
        Expression left = getLeft();
        if (!(left instanceof AssignableExpression))
            throw new AssignException();
        Object right = getRight().evaluate();
        ((AssignableExpression)left).assign(right);
        return right;
    }

    /**
     * Optimize the subexpression.  The assignment operation can not be optimized, and the
     * left operand can only be partially optimized - for example, if the left operand is
     * {@code array[2+2]} the addition can be optimized but the indexing operation can not.
     * The right operand can be fully optimized.
     *
     * @return  the operator itself
     */
    @Override
    public Expression optimize() {
        optimizeRightOperand();
        Expression left = getLeft();
        if (left instanceof Operator)
            ((Operator)left).optimizeRightOperand();
        return this;
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is an {@link AssignOperator}
     * operation with equal operands.
     *
     * @param o  the object for comparison
     * @return   {@code true} if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof AssignOperator && super.equals(o);
    }

}
