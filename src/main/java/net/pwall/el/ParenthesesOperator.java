/*
 * @(#) ParenthesesOperator.java
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
 * A dummy operator to represent parentheses in an expression.  By making the contents of
 * the parentheses the right operand of the expression, the operator precedence search is
 * prevented from scanning the sub-expression.
 *
 * @author  Peter Wall
 */
public class ParenthesesOperator extends Operator {

    public static final String name = "()";
    public static final int priority = 8;

    /**
     * Construct a dummy parentheses operator with the given operand.
     *
     * @param operand  the right operand
     */
    public ParenthesesOperator(Expression operand) {
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
     * Get the result type of this operator.  The result type of this operator is the result
     * type of its right operand.
     *
     * @return  the result type of the operator
     */
    @Override
    public Class<?> getType() {
        return getRight().getType();
    }

    /**
     * Evaluate the subexpression.  This just returns the evaluation of the operand.
     *
     * @return  the result of the evaluation of the operand
     * @throws  EvaluationException on any errors
     */
    @Override
    public Object evaluate() throws EvaluationException {
        return getRight().evaluate();
    }

    /**
     * Optimize the subexpression.  This method calls the parent class to optimize the
     * operand, then returns the operand, because the dummy operator no longer has any
     * value.
     *
     * @return  the operand
     */
    @Override
    public Expression optimize() {
        optimizeRightOperand();
        return getRight();
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is a
     * {@code Parentheses} with an equal operand.
     *
     * @param o  the object for comparison
     * @return   {@code true} if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof ParenthesesOperator && super.equals(o);
    }

    /**
     * Convert subexpression to string.
     *
     * @return  a string representation of the subexpression
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append('(');
        sb.append(getRight());
        sb.append(')');
        return sb.toString();
    }

}
