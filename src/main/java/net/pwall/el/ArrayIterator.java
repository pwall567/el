/*
 * @(#) ArrayIterator.java
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

import java.lang.reflect.Array;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * A simple array iterator class to assist with array operations.
 *
 * @author  Peter Wall
 */
public class ArrayIterator implements Iterator<Object> {

    private final Object array;
    private int index;

    /**
     * Construct an {@code ArrayIterator} with the given array.
     *
     * @param  array  the array
     * @throws IllegalArgumentException if the argument is not an array
     */
    public ArrayIterator(Object array) {
        if (!array.getClass().isArray())
            throw new IllegalArgumentException();
        this.array = array;
        index = 0;
    }

    /**
     * Return {@code true} if the iterator has more values.
     *
     * @return {@code true} if the iterator has more values
     */
    @Override
    public boolean hasNext() {
        return index < Array.getLength(array);
    }

    /**
     * Return the next value.
     *
     * @return the next value
     * @throws NoSuchElementException if the iterator is already at end
     */
    @Override
    public Object next() {
        if (!hasNext())
            throw new NoSuchElementException();
        return Array.get(array, index++);
    }

    /**
     * Unsupported operation - remove not allowed.
     *
     * @throws UnsupportedOperationException always
     */
    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

}
