/*
 * @(#) NegateOperator.java
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
 * A class to represent the negate operation.
 *
 * @author  Peter Wall
 */
public class NegateOperator extends Operator {

    public static final String name = "-";
    public static final int priority = 7;

    /**
     * Construct a negate operator with the given operand.
     *
     * @param operand  the right operand
     */
    public NegateOperator(Expression operand) {
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
     * Get the result type of this operator.  The result type of an arithmetic operation is
     * always {@link Number}.
     *
     * @return  the result type of the operator
     */
    @Override
    public Class<?> getType() {
        return Number.class;
    }

    /**
     * Evaluate the subexpression.  Evaluate the operand and apply the negate operation.
     *
     * @return  the object resulting from the negate operation
     * @throws  EvaluationException on any errors
     */
    @Override
    public Object evaluate() throws EvaluationException {
        Object operand = getRight().evaluate();
        if (operand == null)
            return 0;
        try {
            if (operand instanceof String) {
                if (floatString(operand))
                    return -asDouble(operand);
                return -asLong(operand);
            }
            if (operand instanceof Byte)
                return (byte)(-(Byte)operand);
            if (operand instanceof Short)
                return (short)(-(Short)operand);
            if (operand instanceof Integer)
                return -(Integer)operand;
            if (operand instanceof Long)
                return -(Long)operand;
            if (operand instanceof Float)
                return -(Float)operand;
            if (operand instanceof Double)
                return -(Double)operand;
        }
        catch (EvaluationException ee) {
            throw ee;
        }
        catch (Exception ignored) {
        }
        throw new EvaluationException("negate");
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is a {@code Negate}
     * operation with an equal operand.
     *
     * @param o  the object for comparison
     * @return   {@code true} if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof NegateOperator && super.equals(o);
    }

}
