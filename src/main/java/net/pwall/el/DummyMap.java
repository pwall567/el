/*
 * @(#) DummyMap.java
 *
 * JSTL Expression Language Parser / Evaluator - Dummy Map implementation
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

import java.util.Collection;
import java.util.Map;
import java.util.Set;

/**
 * A default implementation of most of the <code>Map</code> interface.  This
 * allows <code>Map</code> objects to be created by providing only the
 * <code>get()</code> method.
 *
 * <p>This class is provided as part of the JSTL Expression Language Parser /
 * Evaluator package because the Expression Language "<code>.</code>" and
 * "<code>[]</code>" operations can extract values from <code>Map</code>
 * objects, and it can be useful for <code>{@link Expression.Resolver}</code>
 * implementations to return objects that implement this interface.</p>
 *
 * @author Peter Wall
 */
public abstract class DummyMap implements Map<Object, Object> {

    /**
     * Unsupported operation.
     */
    @Override
    public int size() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public boolean isEmpty() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public boolean containsKey(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public boolean containsValue(Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public Object put(Object key, Object value) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public Object remove(Object key) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public void putAll(Map<? extends Object, ? extends Object> t) {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public void clear() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public Set<Object> keySet() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public Collection<Object> values() {
        throw new UnsupportedOperationException();
    }

    /**
     * Unsupported operation.
     */
    @Override
    public Set<Map.Entry<Object, Object>> entrySet() {
        throw new UnsupportedOperationException();
    }

}
