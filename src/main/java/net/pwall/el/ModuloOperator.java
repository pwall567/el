/*
 * @(#) ModuloOperator.java
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
 * An implementation class for the modulo operation.
 *
 * @author  Peter Wall
 */
public class ModuloOperator extends ArithmeticOperator {

    public static final String name = "%";
    public static final int priority = 6;

    /**
     * Construct a modulo operation with the given operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     */
    public ModuloOperator(Expression left, Expression right) {
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
     * Apply the operation to {@code long} operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     * @return       the result
     */
    @Override
    public long execute(long left, long right) {
        return left % right;
    }

    /**
     * Apply the operation to {@code double} operands.
     *
     * @param left   the left operand
     * @param right  the right operand
     * @return       the result
     */
    @Override
    public double execute(double left, double right) {
        return left % right;
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is a {@code Modulo}
     * operation with equal operands.
     *
     * @param o  the object for comparison
     * @return   {@code true} if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof ModuloOperator && super.equals(o);
    }

}
