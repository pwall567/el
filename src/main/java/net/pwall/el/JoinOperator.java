/*
 * @(#) JoinOperator.java
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
 * A class to represent the string concatenation (join) operation.  This is an extension to
 * the JSTL 1.0 EL specification.
 *
 * @author  Peter Wall
 */
public class JoinOperator extends DiadicOperator {

    public static final String name = "#";
    public static final int priority = 5;

    /**
     * Construct a 'join' operation with the given operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     */
    public JoinOperator(Expression left, Expression right) {
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
     * Get the result type of this operator.  The result type of a join operation is always
     * {@link String}.
     *
     * @return  the result type of the operator
     */
    @Override
    public Class<?> getType() {
        return String.class;
    }

    /**
     * Optimize the subexpression.  In addition to the default optimization, check for join
     * with null or zero-length string.
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
        Expression left = getLeft();
        Expression right = getRight();
        if (right.isConstant()) {
            Object val = ((Constant)right).getValue();
            if (val == null ||
                    val instanceof String && ((String)val).length() == 0)
                return left;
        }
        if (left.isConstant()) {
            Object val = ((Constant)left).getValue();
            if (val == null ||
                    val instanceof String && ((String)val).length() == 0)
                return right;
        }
        return this;
    }

    /**
     * Evaluate the subexpression.  Concatenate the left and right operands (as strings).
     *
     * @return the string result of the join operation
     * @throws EvaluationException on any errors
     */
    @Override
    public Object evaluate() throws EvaluationException {
        Object left = getLeft().evaluate();
        Object right = getRight().evaluate();
        if (left == null)
            return right == null ? Constant.nullString : asString(right);
        return right == null ? asString(left) :
                asString(left) + asString(right);
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is a {@code Join}
     * operation with equal operands.
     *
     * @param o  the object for comparison
     * @return   {@code true} if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof JoinOperator && super.equals(o);
    }

}
