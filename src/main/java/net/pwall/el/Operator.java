/*
 * @(#) Operator.java
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
 * A base class for all operators, both monadic (one operand) and diadic (two operands).
 *
 * @author  Peter Wall
 */
public abstract class Operator extends Expression {

    private Expression right;

    /**
     * Construct an <code>Operator</code> with a given operand.
     *
     * @param right  the right or only operand
     */
    public Operator(Expression right) {
        this.right = right;
    }

    /**
     * Get the right operand.
     *
     * @return  the right operand
     */
    public Expression getRight() {
        return right;
    }

    /**
     * Set the right operand.
     *
     * @param right  the new value for the right operand
     */
    void setRight(Expression right) {
        this.right = right;
    }

    /**
     * Optimize the subexpression.  The default behavior is to optimize the operand, then if
     * it is constant, execute the operation.
     *
     * @return  the constant result or the operator itself
     */
    @Override
    public Expression optimize() {
        if (optimizeRightOperand()) {
            try {
                return new Constant(evaluate());
            }
            catch (EvaluationException ee) {
                // ignore
            }
        }
        return this;
    }

    /**
     * Optimize the operand.
     *
     * @return {@code true} if the operand is constant
     */
    public boolean optimizeRightOperand() {
        Expression optRight = right.optimize();
        right = optRight;
        return optRight.isConstant();
    }

    /**
     * Get the priority of the operator.  The priority is used to determine the order of
     * evaluation of an expression with multiple operations.  For example, a + b * c is
     * evaluated as (a + (b * c)).
     *
     * @return  the priority
     */
    public abstract int getPriority();

    /**
     * Get the name of the operator.  The name is the token which represents the operator in
     * the Expression Language.
     *
     * @return  the name
     */
    public abstract String getName();

    /**
     * Test for equality.  Return {@code true} only if the other object is an
     * {@link Operator} with an equal operand.
     *
     * @param o  the object for comparison
     * @return   {@code true} if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof Operator &&
                getRight().equals(((Operator)o).getRight());
    }

    /**
     * Ensure that objects which compare as equal return the same hash code.
     *
     * @return the hash code
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getRight().hashCode();
    }

    /**
     * Convert subexpression to string.
     *
     * @return  a string representation of the subexpression
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        String name = getName();
        sb.append(name);
        Expression expr = getRight();
        if (expr instanceof Operator) {
            sb.append('(');
            sb.append(expr);
            sb.append(')');
        }
        else {
            if (isIdentifierStart(name.charAt(0)))
                sb.append(' ');
            sb.append(expr);
        }
        return sb.toString();
    }

}
