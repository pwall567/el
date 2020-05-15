/*
 * @(#) FunctionCallOperator.java
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

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;

/**
 * A class to represent the function call operation.
 *
 * @author  Peter Wall
 */
public class FunctionCallOperator extends Expression {

    private final Object impl;
    private final String functionName;
    private final List<Expression> arguments;

    /**
     * Create a function call operation.
     *
     * @param   impl            the implementation object
     * @param   functionName    the name of the method to execute
     */
    public FunctionCallOperator(Object impl, String functionName) {
        this.impl = impl;
        this.functionName = functionName;
        arguments = new ArrayList<>();
    }

    /**
     * Add an argument to the function call.
     *
     * @param   argument    the argument
     */
    public void addArgument(Expression argument) {
        arguments.add(argument);
    }

    @Override
    public Object evaluate() throws EvaluationException {
        int n = arguments.size();
        for (Method method : impl.getClass().getMethods()) {
            if (method.getName().equals(functionName) &&
                    method.getParameterTypes().length == n) {
                Object[] array = new Object[n];
                for (int i = 0; i < n; i++)
                    array[i] = arguments.get(i).evaluate();
                try {
                    return method.invoke(impl, array);
                }
                catch (IllegalAccessException | IllegalArgumentException | InvocationTargetException e) {
                    throw new FunctionEvaluationException();
                }
            }
        }
        throw new FunctionEvaluationException(); // no matching method found
    }

    @Override
    public Expression optimize() {
        for (int i = 0, n = arguments.size(); i < n; i++)
            arguments.set(i, arguments.get(i).optimize());
        return this;
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is a
     * {@code FunctionCall} operation with equal operands.
     *
     * @param   o   the object for comparison
     * @return      {@code true} if expressions are equal
     * @see     Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof FunctionCallOperator))
            return false;
        FunctionCallOperator fc = (FunctionCallOperator)o;
        if (!impl.equals(fc.impl) || !functionName.equals(fc.functionName))
            return false;
        int n = arguments.size();
        if (n != fc.arguments.size())
            return false;
        for (int i = 0; i < n; i++)
            if (!arguments.get(i).equals(fc.arguments.get(i)))
                return false;
        return true;
    }

    /**
     * Ensure that objects which compare as equal return the same hash code.
     *
     * @return  the hash code
     * @see     Object#hashCode()
     */
    @Override
    public int hashCode() {
        int result = impl.hashCode() ^ functionName.hashCode();
        for (int i = 0, n = arguments.size(); i < n; ++i)
            result ^= arguments.get(i).hashCode();
        return result;
    }

}
