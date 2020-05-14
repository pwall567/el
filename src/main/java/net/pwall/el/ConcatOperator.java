/*
 * @(#) ConcatOperator.java
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
 * A class to represent the concatenation pseudo-expression created by the
 * {@code parseSubstitution()} method.
 *
 * @author  Peter Wall
 */
public class ConcatOperator extends Expression {

    private static final int increment = 5;

    private Expression[] array;
    private int num = 0;

    /**
     * Create an empty concatenation.
     */
    public ConcatOperator() {
        array = new Expression[increment];
        num = 0;
    }

    /**
     * Add an element to the concatenation.
     *
     * @param expr  the element to be added
     */
    public void addExpression(Expression expr) {
        if (num == array.length) {
            Expression[] newArray = new Expression[num + increment];
            System.arraycopy(array, 0, newArray, 0, num);
            array = newArray;
        }
        array[num++] = expr;
    }

    /**
     * Get the result type of this operator.  The result type of a concatenation operation
     * is always {@link String}.
     *
     * @return  the result type of the operator
     */
    @Override
    public Class<?> getType() {
        return String.class;
    }

    /**
     * Evaluate the subexpression.  Evaluate each element and add it to an output string.
     *
     * @return  the string concatenation of all the elements
     * @throws  EvaluationException on any errors
     */
    @Override
    public Object evaluate() throws EvaluationException {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < num; ++i)
            sb.append(array[i].asString());
        return sb.toString();
    }

    /**
     * Optimize the subexpression.  Each element is individually optimized, constant empty
     * strings are dropped and adjacent constant strings are combined.
     *
     * @return  the optimized concatenation operation
     */
    @Override
    public Expression optimize() {
        int j = 0;
        for (int i = 0; i < num; ++i) {
            Expression expr = array[i].optimize();
            if (expr instanceof Constant) {
                Object val = ((Constant)expr).evaluate();
                if (val instanceof String) {
                    String str = (String)val;
                    if (str.length() == 0)
                        continue;
                    if (j > 0) {
                        Expression expr2 = array[j - 1];
                        if (expr2 instanceof Constant) {
                            Object val2 = ((Constant)expr2).evaluate();
                            if (val2 instanceof String) {
                                array[j - 1] =
                                        new Constant(((String)val2) + str);
                                continue;
                            }
                        }
                    }
                }
            }
            array[j++] = expr;
        }
        if (j == 0)
            return Constant.nullStringConstant;
        if (j == 1) {
            Expression expr = array[0];
            if (expr instanceof Constant) {
                Object val = ((Constant)expr).evaluate();
                if (val instanceof String)
                    return expr;
                try {
                    return new Constant(expr.asString());
                }
                catch (EvaluationException ignored) {
                }
            }
        }
        num = j;
        while (j < array.length)
            array[j++] = null;
        return this;
    }

    /**
     * Get the result of the concatenation as a single expression.  If the concatenation
     * resulted from the parse of a string consisting of a single expression, return that
     * expression; otherwise return the concatenation expression.
     *
     * @return the result of the concatenation as a single expression
     */
    public Expression singleExpression() {
        if (num == 1)
            return array[0];
        return this;
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is a {@code Concat}
     * operation with equal operands.
     *
     * @param o  the object for comparison
     * @return   {@code true} if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ConcatOperator))
            return false;
        ConcatOperator op = (ConcatOperator)o;
        if (num != op.num)
            return false;
        for (int i = 0; i < num; ++i)
            if (!array[i].equals(op.array[i]))
                return false;
        return true;
    }

    /**
     * Ensure that objects which compare as equal return the same hash code.
     *
     * @return the hash code
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        int result = 0;
        for (int i = 0; i < num; ++i)
            result ^= array[i].hashCode();
        return result;
    }

}
