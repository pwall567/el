/*
 * @(#) ExtendedResolver.java
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
 * An extended {@link Resolver} that also resolves namespaces.
 *
 * @author  Peter Wall
 */
public interface ExtendedResolver extends Resolver {

    /**
     * Resolve a namespace prefix.  This is used in the case of prefixed function calls.
     *
     * @param   prefix  the prefix
     * @return  the URI for the namespace, or {@code null} if the prefix can not be resolved
     */
    String resolvePrefix(String prefix);

    /**
     * Resolve a namespace URI to an object that implements the functionality.
     *
     * @param   uri     the URI
     * @return  the implementing object, or {@code null} if the URI can not be resolved
     */
    Object resolveNamespace(String uri);

}
