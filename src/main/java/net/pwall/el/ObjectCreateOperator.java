/*
 * @(#) ObjectCreateOperator.java
 *
 * JSTL Expression Language Parser / Evaluator
 * Copyright (C) 2014, 2020  Peter Wall
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * A class to represent the object create operation.
 *
 * @author  Peter Wall
 */
public class ObjectCreateOperator extends Expression {

    private final List<ObjectCreateItem> items;

    public ObjectCreateOperator() {
        items = new ArrayList<>();
    }

    public void addItem(String identifier, Expression expression) {
        items.add(new ObjectCreateItem(identifier, expression));
    }

    @Override
    public Object evaluate() throws EvaluationException {
        int n = items.size();
        Map<String, Object> object = new HashMap<>();
        for (int i = 0; i < n; i++)
            object.put(items.get(i).getIdentifier(),
                    items.get(i).getExpression().evaluate());
        return object;
    }

    @Override
    public Expression optimize() {
        for (int i = 0, n = items.size(); i < n; i++) {
            ObjectCreateItem item = items.get(i);
            item.setExpression(item.getExpression().optimize());
        }
        return this;
    }

    /**
     * Get the result type of this operator.  The result type of an object create operation
     * is always {@link Map}.
     *
     * @return  the result type of the operator
     */
    @Override
    public Class<?> getType() {
        return Map.class;
    }

    /**
     * Test for equality.  Return {@code true} only if the other object is an
     * {@code ObjectCreate} operation with equal operands.
     *
     * @param   o   the object for comparison
     * @return      {@code true} if expressions are equal
     * @see     Object#equals(Object)
     */
    @Override
    public boolean equals(Object o) {
        if (!(o instanceof ObjectCreateOperator))
            return false;
        ObjectCreateOperator oc = (ObjectCreateOperator)o;
        int n = items.size();
        if (n != oc.items.size())
            return false;
        for (int i = 0; i < n; i++)
            if (!items.get(i).equals(oc.items.get(i)))
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

    public static class ObjectCreateItem {

        private final String identifier;
        private Expression expression;

        public ObjectCreateItem(String identifier, Expression expression) {
            this.identifier = identifier;
            this.expression = expression;
        }

        public String getIdentifier() {
            return identifier;
        }

        public Expression getExpression() {
            return expression;
        }

        public void setExpression(Expression expression) {
            this.expression = expression;
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is an
         * {@code ObjectCreateItem} operation with equal contents.
         *
         * @param   o   the object for comparison
         * @return      {@code true} if expressions are equal
         * @see     Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof ObjectCreateItem))
                return false;
            ObjectCreateItem oci = (ObjectCreateItem)o;
            return identifier.equals(oci.identifier) && expression.equals(oci.expression);
        }

        /**
         * Ensure that objects which compare as equal return the same hash code.
         *
         * @return  the hash code
         * @see     Object#hashCode()
         */
        @Override
        public int hashCode() {
            return identifier.hashCode() ^ expression.hashCode();
        }

    }

}
