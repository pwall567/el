/*
 * @(#) IndexedOperator.java
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
 * A class to represent the indexing operation.  The expression language converts property
 * references of the form {@code object.property} to {@code object["property"]}, so both of
 * these are handled by this operator.
 *
 * <p>Much of the behavior of this class is implemented as static methods on the
 * {@code Expression} class so that they may be accessed more easily from outside the
 * class.</p>
 *
 * @author  Peter Wall
 */
public class IndexedOperator extends DiadicOperator implements AssignableExpression {

    public static final String name = ".";
    public static final int priority = 9;

    /**
     * Construct an <code>Indexed</code> operation with the given operands.
     *
     * @param left   the left operand
     * @param right  the right operand (or the one in brackets)
     */
    public IndexedOperator(Expression left, Expression right) {
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
     * Get the name of the operator (the token for this operator in an expression).
     *
     * @return the name
     */
    @Override
    public String getName() {
        return name;
    }

    /**
     * Evaluate the subexpression.  The rules for this operation are given in the
     * specification document referenced at the start of the {@code Expression} class.
     *
     * @return  the object resulting from the indexing operation
     * @throws  EvaluationException on any errors
     */
    @Override
    public Object evaluate() throws EvaluationException {
        Object left = getLeft().evaluate();
        if (left == null)
            return null;
        return getIndexed(left, getRight().evaluate());
    }

    /**
     * Assign a value to the object addressed by this indexing operation.
     *
     * @param value the value to be stored
     * @throws EvaluationException on any errors
     */
    @Override
    public void assign(Object value) throws EvaluationException {
        Object left = getLeft().evaluate();
        if (left == null)
            throw new AssignException();
        setIndexed(left, getRight().evaluate(), value);
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is an
     * {@code Indexed} operation with equal operands.
     *
     * @param o  the object for comparison
     * @return   {@code true} if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof IndexedOperator && super.equals(o);
    }

    /**
     * Convert subexpression to string.  This operator requires special handling because of
     * the syntax of indexing operations.
     *
     * @return  a string representation of the subexpression
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        Expression expr = getLeft();
        if (expr instanceof Operator && ((Operator)expr).getPriority() < priority) {
            sb.append('(');
            sb.append(expr);
            sb.append(')');
        }
        else
            sb.append(expr);
        expr = getRight();
        if (isPropertyName(expr)) {
            sb.append('.');
            sb.append(((Constant)expr).evaluate());
        }
        else {
            sb.append('[');
            sb.append(expr);
            sb.append(']');
        }
        return sb.toString();
    }

    private boolean isPropertyName(Expression expr) {
        if (!(expr instanceof Constant))
            return false;
        Object value = ((Constant)expr).evaluate();
        if (!(value instanceof String))
            return false;
        String str = (String)value;
        int n = str.length();
        if (n == 0 || !isIdentifierStart(str.charAt(0)))
            return false;
        for (int i = 1; i < n; ++i)
            if (!isIdentifierPart(str.charAt(i)))
                return false;
        return true;
    }

}
