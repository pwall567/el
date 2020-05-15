/*
 * @(#) EqualityOperator.java
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
 * A base class for equality comparison operations.
 *
 * @author  Peter Wall
 */
public abstract class EqualityOperator extends DiadicOperator {

    public static final int priority = 3;

    /**
     * Construct a equality comparison operation with the given operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     */
    public EqualityOperator(Expression left, Expression right) {
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
     * Return {@code true} - the operation is commutative.
     *
     * @return {@code true}
     */
    @Override
    public boolean isCommutative() {
        return true;
    }

    /**
     * Get the result type of this operator.  The result type of an equality operation is
     * always {@link Boolean}.
     *
     * @return  the result type of the operator
     */
    @Override
    public Class<?> getType() {
        return Boolean.class;
    }

    /**
     * Compare the two operands for equality.
     *
     * @return  {@code true} if the operands are equal
     * @throws  EvaluationException on any errors
     */
    public boolean compare() throws EvaluationException {
        Object left = getLeft().evaluate();
        Object right = getRight().evaluate();
        try {
            if (left == null) {
                if (right == null)
                    return true;
                return false;
            }
            if (left.equals(right))
                return true;
            if (right == null)
                return false;
            if (floatOperand(left) || floatOperand(right)) {
                return asDouble(left) == asDouble(right);
            }
            if (longOperand(left) || longOperand(right)) {
                return asLong(left) == asLong(right);
            }
            if (left instanceof String || right instanceof String) {
                return asString(left).equals(asString(right));
            }
            return left.equals(right);
        }
        catch (EvaluationException ee) {
            throw ee;
        }
        catch (Exception e) {
        }
        throw new EvaluationException("compare");
    }

}
