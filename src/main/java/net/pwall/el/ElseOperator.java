/*
 * @(#) ElseOperator.java
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
 * A class to represent the else portion of a conditional expression ( {@code ? :} ).  It is
 * not so much an expression as a structure to hold two alternative results from the
 * conditional expression, of which this must be the right operand.  This is an extension to
 * the JSTL 1.0 EL specification.
 *
 * @author  Peter Wall
 */
public class ElseOperator extends DiadicOperator {

    public static final String name = ":";
    public static final int priority = 0;

    /**
     * Construct an else subexpression with the given operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     */
    public ElseOperator(Expression left, Expression right) {
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
     * Throw an exception - this subexpression should never be executed.
     *
     * @return nothing
     * @throws EvaluationException always
     */
    @Override
    public Object evaluate() throws EvaluationException {
        throw new EvaluationException("else");
    }

}
