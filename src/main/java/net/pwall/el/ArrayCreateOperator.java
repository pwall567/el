/*
 * @(#) ArrayCreateOperator.java
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
 * A class to represent the array create operation.
 *
 * @author  Peter Wall
 */
public class ArrayCreateOperator extends Expression {

    private final List<Expression> items;

    public ArrayCreateOperator() {
        items = new ArrayList<>();
    }

    public void addItem(Expression item) {
        items.add(item);
    }

    @Override
    public Object evaluate() throws EvaluationException {
        int n = items.size();
        Object[] array = new Object[n];
        for (int i = 0; i < n; i++)
            array[i] = items.get(i).evaluate();
        return array;
    }

    /**
     * Get the result type of this operator.  The result type of an array create operation
     * is always {@code Object[]}.
     *
     * @return  the result type of the operator
     */
    @Override
    public Class<?> getType() {
        return Object[].class;
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is an
     * {@code ArrayCreate} operation with equal operands.
     *
     * @param   o   the object for comparison
     * @return      {@code true} if expressions are equal
     * @see     Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ArrayCreateOperator))
            return false;
        ArrayCreateOperator ac = (ArrayCreateOperator)o;
        int n = items.size();
        if (n != ac.items.size())
            return false;
        for (int i = 0; i < n; i++)
            if (!items.get(i).equals(ac))
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
        int result = 0;
        for (int i = 0, n = items.size(); i < n; ++i)
            result ^= items.get(i).hashCode();
        return result;
    }

}
