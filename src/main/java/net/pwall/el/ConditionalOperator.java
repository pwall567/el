/*
 * @(#) ConditionalOperator.java
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
 * A class to represent the conditional expression ( {@code ? :} ).  This is an extension to
 * the JSTL 1.0 EL specification.
 *
 * @author  Peter Wall
 */
public class ConditionalOperator extends DiadicOperator {

    public static final String name = "?";
    public static final int priority = 0;

    /**
     * Construct a conditional expression with the given operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     */
    public ConditionalOperator(Expression left, Expression right) {
        super(left, right);
    }

    /**
     * Get the name of the operator (the token for this operator in an
     * expression).
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
     * Get the result type of this operator.  This is determined from the types of the
     * operands of the {@link ElseOperator} operator.
     *
     * @return  the result type of the operator
     */
    @Override
    public Class<?> getType() {
        ElseOperator elseOp = (ElseOperator)getRight();
        Class<?> leftType = elseOp.getLeft().getType();
        Class<?> rightType = elseOp.getRight().getType();
        if (leftType == rightType)
            return leftType;
        if (Number.class.isAssignableFrom(leftType) &&
                Number.class.isAssignableFrom(rightType))
            return Number.class;
        return Object.class;
    }

    /**
     * Evaluate the conditional expression.  The result of the conditional expression is the
     * left or the right operand of the {@link ElseOperator} expression which must be in the right
     * operand position of this expression, depending on the boolean value of the left
     * operand of this expression.
     *
     * @return  the object resulting from the conditional expression
     * @throws  EvaluationException on any errors
     */
    @Override
    public Object evaluate() throws EvaluationException {
        Expression right = getRight();
        if (!(right instanceof ElseOperator))
            throw new EvaluationException("conditional");
        ElseOperator elseExpr = (ElseOperator)right;
        Expression result = getLeft().asBoolean() ? elseExpr.getLeft() :
                elseExpr.getRight();
        return result.evaluate();
    }

    /**
     * Optimize the conditional expression.  If the left operand is constant, the expression
     * is optimized to the subexpression in either the left or the right operand of the
     * {@link ElseOperator} expression which must be in the right operand position of this
     * expression.
     *
     * @return the {@code true} or the {@code false} result, or the expression itself
     */
    @Override
    public Expression optimize() {
        Expression optLeft = getLeft().optimize();
        setLeft(optLeft);
        Expression right = getRight();
        if (right instanceof ElseOperator) {
            ElseOperator elseExpr = (ElseOperator)right;
            if (optLeft.isConstant()) {
                try {
                    return optLeft.asBoolean() ?
                            elseExpr.getLeft().optimize() :
                            elseExpr.getRight().optimize();
                }
                catch (EvaluationException e) {
                    // ignore
                }
            }
            elseExpr.optimizeOperands();
        }
        return this;
    }

    /**
     * Convert subexpression to string.  This operator requires special handling to ensure
     * that the else pseudo-operator is not enclosed in parentheses.
     *
     * @return  a string representation of the subexpression
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Expression expr = getLeft();
        if (expr instanceof Operator) {
            sb.append('(');
            sb.append(expr);
            sb.append(')');
        }
        else
            sb.append(expr);
        sb.append(' ');
        sb.append(getName());
        sb.append(' ');
        sb.append(getRight());
        return sb.toString();
    }

}
