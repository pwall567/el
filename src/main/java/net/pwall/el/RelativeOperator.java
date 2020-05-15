/*
 * @(#) RelativeOperator.java
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
 * A base class for relative comparison operations.
 *
 * @author  Peter Wall
 */
public abstract class RelativeOperator extends DiadicOperator {

    public static final int priority = 4;

    /**
     * Construct a relative comparison operation with the given operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     */
    public RelativeOperator(Expression left, Expression right) {
        super(left, right);
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
     * Get the result type of this operator.  The result type of a comparison operation is
     * always {@link Boolean}.
     *
     * @return  the result type of the operator
     */
    @Override
    public Class<?> getType() {
        return Boolean.class;
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
     * Compare the operands.  This method performs the comparison and calls the appropriate
     * method on the implementing class to determine whether the comparison result is
     * {@code true} or {@code false} in this case.
     *
     * @return  the boolean result of the comparison
     * @throws  EvaluationException on any errors
     */
    public boolean compare() throws EvaluationException {
        Object left = getLeft().evaluate();
        Object right = getRight().evaluate();
        try {
            if (left == right)
                return equal();
            if (left == null)
                return false;
            if (left.equals(right))
                return equal();
            if (right == null)
                return false;
            if (floatOperand(left) || floatOperand(right)) {
                double leftDouble = asDouble(left);
                double rightDouble = asDouble(right);
                if (leftDouble == rightDouble)
                    return equal();
                if (leftDouble < rightDouble)
                    return less();
                return greater();
            }
            if (longOperand(left) || longOperand(right)) {
                long leftLong = asLong(left);
                long rightLong = asLong(right);
                if (leftLong == rightLong)
                    return equal();
                if (leftLong < rightLong)
                    return less();
                return greater();
            }
            if (left instanceof String || right instanceof String) {
                int comparison = asString(left).compareTo(asString(right));
                if (comparison == 0)
                    return equal();
                if (comparison < 0)
                    return less();
                return greater();
            }
            if (left instanceof Comparable) {
                @SuppressWarnings("unchecked")
                int comparison = ((Comparable<Object>)left).compareTo(right);
                if (comparison == 0)
                    return equal();
                if (comparison < 0)
                    return less();
                return greater();
            }
            if (right instanceof Comparable) {
                @SuppressWarnings("unchecked")
                int comparison = ((Comparable<Object>)right).compareTo(left);
                if (comparison == 0)
                    return equal();
                if (comparison > 0)
                    return less();
                return greater();
            }
        }
        catch (EvaluationException ee) {
            throw ee;
        }
        catch (Exception ignored) {
        }
        throw new EvaluationException("compare");
    }

    /**
     * The result if the left operand is less than the right.
     *
     * @return  the boolean result of the comparison
     */
    public abstract boolean less();

    /**
     * The result if the operands are equal.
     *
     * @return  the boolean result of the comparison
     */
    public abstract boolean equal();

    /**
     * The result if the left operand is greater than the right.
     *
     * @return  the boolean result of the comparison
     */
    public abstract boolean greater();

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

}
