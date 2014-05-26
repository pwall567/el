/*
 * @(#) SimpleVariable.java
 *
 * JSTL Expression Language Parser / Evaluator - Simple Name Resolver
 * Copyright (C) 2003, 2006, 2014  Peter Wall
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
 * A simple implementation of the <code>{@link Expression.Variable}</code>
 * abstract class.  This class is never used by the expression parser; it is
 * provided as a convenience for writers of
 * <code>{@link Expression.Resolver}</code> classes.
 *
 * @author Peter Wall
 */
public class SimpleVariable extends Expression.Variable {

    private String name;
    private Object object;

    /**
     * Construct a <code>SimpleVariable</code> which returns an object.
     *
     * @param object  the object to be returned
     */
    public SimpleVariable(String name, Object object) {
        this.name = name;
        this.object = object;
    }

    /**
     * Get the name of the variable.
     *
     * @return the variable name
     */
    public String getName() {
        return name;
    }

    /**
     * Modify the value represented by the variable.
     *
     * @param object  the new object value
     */
    @Override
    public void assign(Object object) {
        this.object = object;
    }

    /**
     * Evaluate the subexpression.
     *
     * @return  the object represented by the variable
     */
    @Override
    public Object evaluate() {
        return object;
    }

    /**
     * Test for equality.  Return true only if the other object is a
     * variable referring to the same object (names are not considered).
     *
     * @param o  the object for comparison
     * @return   true if expressions are equal
     * @see Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        return o instanceof SimpleVariable &&
                ((SimpleVariable)o).object == object;
    }

    /**
     * Ensure that objects which compare as equal return the same hash code.
     *
     * @return the hash code
     * @see Object#hashCode()
     */
    @Override
    public int hashCode() {
        return object == null ? 0 : object.hashCode();
    }

    /**
     * Return the name of the variable.
     *
     * @return the name of the variable
     */
    @Override
    public String toString() {
        return name;
    }

}
