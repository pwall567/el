/*
 * @(#) SimpleResolver.java
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

import java.util.HashMap;
import java.util.Map;

/**
 * Name Resolver for Expression Language.  It has no built-in names, but it
 * will allow any number of new names to be created.
 *
 * @author Peter Wall
 */
public class SimpleResolver implements Expression.Resolver {

    private Map<String, Object> map;

    /**
     * Construct the <code>SimpleResolver</code>
     */
    public SimpleResolver() {
        map = new HashMap<>();
    }

    /**
     * Create a variable, or modify an existing one.
     *
     * @param identifier  the identifier of the variable
     * @param object      the value of the variable
     */
    public void set(String identifier, Object object) {
        map.put(identifier, object);
    }

    /**
     * Resolve an identifier to a variable.
     *
     * @param identifier  the identifier to be resolved
     * @return            a variable, or null if the name can not be resolved
     */
    @Override
    public Expression resolve(String identifier) {
        Object variable = map.get(identifier);
        if (variable == null)
            return null;
        return new SimpleVariable(identifier, variable);
    }

}
