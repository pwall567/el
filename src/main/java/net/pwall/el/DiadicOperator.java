/*
 * @(#) DiadicOperator.java
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

import java.util.ArrayList;
import java.util.List;

/**
 * A base class for all diadic (two operand) operators.
 *
 * @author  Peter Wall
 */
public abstract class DiadicOperator extends Operator {

    private Expression left;

    /**
     * Construct a <code>Diadic</code> with the given operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     */
    public DiadicOperator(Expression left, Expression right) {
        super(right);
        this.left = left;
    }

    /**
     * Get the left operand.
     *
     * @return  the left operand
     */
    public Expression getLeft() {
        return left;
    }

    /**
     * Set the left operand.
     *
     * @param left  the new value for the left operand
     */
    void setLeft(Expression left) {
        this.left = left;
    }

    /**
     * Optimize the subexpression.  The default behavior is to optimize the operands, then
     * if both are constant, execute the operation.
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
        return this;
    }

    /**
     * Optimize the operands.
     *
     * @return {@code true} if both operands are constant
     */
    public boolean optimizeOperands() {
        boolean rightConstant = optimizeRightOperand();
        Expression optLeft = left.optimize();
        left = optLeft;
        return rightConstant && optLeft.isConstant();
    }

    /**
     * Return {@code true} if the operation is commutative.  The default is {@code false}.
     *
     * @return {@code false}
     */
    public boolean isCommutative() {
        return false;
    }

    /**
     * Return {@code true} if the operation is associative.  The default is {@code false}.
     *
     * @return {@code false}
     */
    public boolean isAssociative() {
        return false;
    }

    /**
     * Return {@code true} if the operation is left-associative.  The default is
     * {@code true}.
     *
     * @return {@code true}
     */
    public boolean isLeftAssociative() {
        return true;
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is a {@link DiadicOperator}
     * operation with equal operands, or is commutative with equal operands reversed, or is
     * associative and all operands match.
     *
     * @param o  the object for comparison
     * @return   {@code true} if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof DiadicOperator))
            return false;
        DiadicOperator op = (DiadicOperator)o;
        if (getLeft().equals(op.getLeft()) &&
                getRight().equals(op.getRight()))
            return true;
        if (isCommutative() && getLeft().equals(op.getRight()) &&
                getRight().equals(op.getLeft()))
            return true;
        if (!isAssociative())
            return false;
        List<Expression> operands = new ArrayList<>();
        accumOperands(operands, getLeft());
        accumOperands(operands, getRight());
        if (!checkOperands(operands, op.getLeft()))
            return false;
        if (!checkOperands(operands, op.getRight()))
            return false;
        return operands.isEmpty();
    }

    private void accumOperands(List<Expression> operands, Expression expr) {
        if (expr.getClass().equals(getClass())) {
            DiadicOperator op = (DiadicOperator)expr;
            accumOperands(operands, op.getLeft());
            accumOperands(operands, op.getRight());
        }
        else
            operands.add(expr);
    }

    private boolean checkOperands(List<Expression> operands, Expression expr) {
        if (expr.getClass().equals(getClass())) {
            DiadicOperator op = (DiadicOperator)expr;
            if (!checkOperands(operands, op.getLeft()))
                return false;
            if (!checkOperands(operands, op.getRight()))
                return false;
            return true;
        }
        for (int i = 0, n = operands.size(); i < n; ++i) {
            if (expr.equals(operands.get(i))) {
                operands.remove(i);
                return true;
            }
        }
        return false;
    }

    /**
     * Ensure that objects which compare as equal return the same hash code.
     *
     * @return the hash code
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return getLeft().hashCode() ^ getRight().hashCode();
    }

    /**
     * Convert subexpression to string.
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
        expr = getRight();
        if (expr instanceof Operator) {
            sb.append('(');
            sb.append(expr);
            sb.append(')');
        }
        else
            sb.append(expr);
        return sb.toString();
    }

}
