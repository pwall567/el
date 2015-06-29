/*
 * @(#) Expression.java v2.0
 *
 * JSTL Expression Language Parser / Evaluator
 * Copyright (C) 2003, 2005, 2006, 2007, 2012, 2013  Peter Wall
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 */

package net.pwall.el;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

import net.pwall.util.ParseText;

/**
 * Expression evaluation system for the JSTL Expression Language (EL).  This
 * class is the base class for the various expression elements, and it also
 * contains nested classes for the parser and the expression elements, and
 * static methods to simplify access to the parser.
 *
 * <p>Expression evaluation is a two-stage process.  First the source string is
 * parsed into an expression tree, where the lowest-priority operator is the
 * root node of the tree.  Then the tree is evaluated, by calling the
 * {@link #evaluate()} method on the root node.  This will recursively call
 * {@code evaluate()} on its operands, and then apply the operation.</p>
 *
 * <p>The tree resulting from the <code>parse</code> operation may be saved and
 * evaluated repeatedly, with the input variables possibly changing with each
 * evaluation.</p>
 *
 * <p>Variable names in the expression must be resolved by a
 * {@link Resolver} object supplied to the {@link Parser}, or passed
 * to the parse method as a parameter.</p>
 *
 * <p>The specification of the JSTL Expression Language is contained in the
 * document "JavaServer Pages&#x2122; Standard Tag Library" published by Sun
 * Microsystems, Inc.; this version of the parser / evaluator conforms to
 * Version 1.0 of that document, with the following extensions (which must be
 * explicitly enabled):
 *   <ul>
 *     <li>Conditional expressions ( <code>? :</code> )</li>
 *     <li>Assignment operations</li>
 *     <li><code>toupper</code> and <code>tolower</code> conversions</li>
 *     <li>Wildcard match operation</li>
 *     <li>String concatenation (join) operation</li>
 *     <li>Array/list <code>length</code> and <code>sum</code> operations</li>
 *   </ul>
 * </p>
 *
 * <p>There are many ways to use the system.  The simplest is to use the static
 * methods of this class:
 * <pre>
 *     Expression exp = Expression.parseExpression("4+3*2", null);
 *     Object result = exp.evaluate(); // result will contain Integer(10)
 * </pre>
 * There is also a convenience method to perform substitution of
 * <code>$&#123;...&#125;</code> sequences in a string:
 * <pre>
 *     String result = Expression.substitute("4+3*2=${4+3*2}", null);
 *     // result will contain "4+3*2=10"
 * </pre>
 * To allow the use of JSP variables, a {@link JspResolver} may be used:
 * <pre>
 *     JspResolver resolver = new JspResolver(pageContext);
 *     String output = Expression.substitute(input, resolver);
 * </pre>
 * More complex uses of the system, for example conditional expressions, may
 * require a {@link Parser} to be instantiated:
 * <pre>
 *     Expression.Parser parser = new Expression.Parser();
 *     parser.setConditionalAllowed(true);
 *     Expression exp = Expression.parseExpression("4 > 3 ? 'a' : 'b'", null);
 *     Object result = exp.evaluate(); // result will contain 'a'
 * </pre>
 * This is only a brief introduction to the facilities of the system; see the
 * documentation for the methods of this class and its children for more
 * information.</p>
 *
 * @author Peter Wall
 */
public abstract class Expression {

    private static final Parser defaultParser = new Parser();
    private static final String nullString = "";
    public static final Constant trueConstant = new Constant(Boolean.TRUE);
    public static final Constant falseConstant = new Constant(Boolean.FALSE);
    public static final Constant nullStringConstant = new Constant(nullString);

    /**
     * Parse an entire string to an {@code Expression} tree.
     *
     * @param str       text to parse (not including ${ })
     * @param resolver  a {@link Resolver} object to resolve names in the expression
     * @return          the {@code Expression} tree
     * @throws ParseException on any errors
     */
    public static Expression parseExpression(CharSequence str, Resolver resolver)
            throws ParseException {
        return defaultParser.parseExpression(str, resolver);
    }

    /**
     * Replace all occurrences of <code>$&#123;...&#125;</code> in a string with the contents of
     * the braces evaluated as an expression.
     *
     * @param source    the input text
     * @param resolver  a {@link Resolver} object to resolve names in the expression
     * @return          the text following substitutions
     * @throws ExpressionException on any errors
     */
    public static String substitute(String source, Resolver resolver)
            throws ExpressionException {
        return defaultParser.substitute(source, resolver);
    }

    /**
     * Parse a string and return an expression which will replace all occurrences of
     * <code>$&#123;...&#125;</code> in the string with the contents of the braces evaluated as
     * an expression.
     *
     * @param str       the text to be parsed
     * @param resolver  a {@link Resolver} object to resolve names in the expression
     * @return          the {@code Expression} tree
     * @throws ParseException on any errors
     */
    public static Expression parseSubstitution(CharSequence str, Resolver resolver)
            throws ParseException {
        return defaultParser.parseSubstitution(str, resolver);
    }

    /**
     * Optimize the expression - evaluate constant sub-expressions and remove parentheses.  For
     * an expression that is parsed once and used repeatedly, optimization is recommended.  For
     * an expression that is evaluated only once this is unnecessary.
     *
     * <p>Subclasses must override this method to perform any specific optimizations; the
     * default behavior is to return the object itself (perform no optimizations).</p>
     *
     * @return  the current expression object
     */
    public Expression optimize() {
        return this;
    }

    /**
     * Get the result type of this expression.  The default is {@link Object}.
     *
     * @return  the result type of the expression
     */
    public Class<?> getType() {
        return Object.class;
    }

    /**
     * Evaluate the expression.
     *
     * @return  the result of the expression
     * @throws EvaluationException on any errors
     */
    public abstract Object evaluate() throws EvaluationException;

    /**
     * Return {@code true} if the expression is constant.  The default is {@code false}.
     *
     * @return  {@code true} if the expression is constant
     */
    public boolean isConstant() {
        return false;
    }

    /**
     * Evaluate the expression and return the result as a {@code boolean}.
     *
     * @return  the result of the expression as a {@code boolean}
     * @throws EvaluationException on any errors
     */
    public boolean asBoolean() throws EvaluationException {
        return asBoolean(evaluate());
    }

    /**
     * Convert an {@link Object} to {@code boolean} using the coercion rules in the
     * specification document.
     *
     * @param  object   the object
     * @return          a {@code boolean}
     * @throws BooleanCoercionException on any errors
     */
    public static boolean asBoolean(Object object) throws BooleanCoercionException {
        if (object == null)
            return false;
        if (object instanceof Boolean)
            return ((Boolean)object).booleanValue();
        if (object instanceof String) {
            String string = (String)object;
            if (string.length() == 0)
                return false;
            try {
                return Boolean.valueOf(string).booleanValue();
            }
            catch (Exception e) {
            }
        }
        throw new BooleanCoercionException();
    }

    /**
     * Evaluate the expression and return the result as an {@code int}.
     *
     * @return  the result of the expression as an {@code int}
     * @throws EvaluationException on any errors
     */
    public int asInt() throws EvaluationException {
        return asInt(evaluate());
    }

    /**
     * Convert an {@link Object} to {@code int} using the coercion rules in the specification
     * document.
     *
     * @param  object   the object
     * @return          an {@code int}
     * @throws IntCoercionException on any errors
     */
    public static int asInt(Object object) throws IntCoercionException {
        if (object == null)
            return 0;
        if (object instanceof Character)
            return ((Character)object).charValue();
        if (object instanceof Number)
            return ((Number)object).intValue();
        if (object instanceof String) {
            String string = (String)object;
            if (string.length() == 0)
                return 0;
            try {
                return Integer.parseInt(string, 10);
            }
            catch (Exception e) {
            }
        }
        throw new IntCoercionException();
    }

    /**
     * Evaluate the expression and return the result as a {@code long}.
     *
     * @return  the result of the expression as a {@code long}
     * @throws EvaluationException on any errors
     */
    public long asLong() throws EvaluationException {
        return asLong(evaluate());
    }

    /**
     * Convert an {@link Object} to {@code long} using the coercion rules in the specification
     * document.
     *
     * @param  object   the object
     * @return          a {@code long}
     * @throws LongCoercionException on any errors
     */
    public static long asLong(Object object) throws LongCoercionException {
        if (object == null)
            return 0;
        if (object instanceof Character)
            return ((Character)object).charValue();
        if (object instanceof Number)
            return ((Number)object).longValue();
        if (object instanceof String) {
            String string = (String)object;
            if (string.length() == 0)
                return 0;
            try {
                return Long.parseLong(string, 10);
            }
            catch (Exception e) {
            }
        }
        throw new LongCoercionException();
    }

    /**
     * Evaluate the expression and return the result as a {@code double}.
     *
     * @return  the result of the expression as a {@code double}
     * @throws EvaluationException on any errors
     */
    public double asDouble() throws EvaluationException {
        return asDouble(evaluate());
    }

    /**
     * Convert an {@link Object} to {@code double} using the coercion rules in the specification
     * document.
     *
     * @param  object   the object
     * @return          a {@code double}
     * @throws DoubleCoercionException on any errors
     */
    public static double asDouble(Object object) throws DoubleCoercionException {
        if (object == null)
            return 0.0;
        if (object instanceof Character)
            return ((Character)object).charValue();
        if (object instanceof Number)
            return ((Number)object).doubleValue();
        if (object instanceof String) {
            String string = (String)object;
            if (string.length() == 0)
                return 0;
            try {
                return Double.parseDouble(string);
            }
            catch (Exception e) {
            }
        }
        throw new DoubleCoercionException();
    }

    /**
     * Evaluate the expression and return the result as a {@link String}.
     *
     * @return  the result of the expression as a {@link String}
     * @throws EvaluationException on any errors
     */
    public String asString() throws EvaluationException {
        return asString(evaluate());
    }

    /**
     * Convert an {@link Object} to {@link String} using the coercion rules in the specification
     * document.
     *
     * @param  object   the object
     * @return          a {@link String}
     * @throws StringCoercionException on any errors
     */
    public static String asString(Object object) throws StringCoercionException {
        if (object == null)
            return nullString;
        try {
            return object.toString();
        }
        catch (Exception e) {
            throw new StringCoercionException();
        }
    }

    /**
     * Get the result of an indexing operation - either a [] or dot operation.
     *
     * @param left      the object on the left of the expression
     * @param right     the object on the right of the dot or within the brackets
     * @return          the result
     * @throws EvaluationException on any errors
     */
    public static Object getIndexed(Object left, Object right) throws EvaluationException {
        if (left == null || right == null)
            return null;
        if (left instanceof Map)
            return ((Map<?, ?>)left).get(right);
        if (left instanceof List) {
            try {
                return ((List<?>)left).get(asInt(right));
            }
            catch (IndexOutOfBoundsException ioobe) {
                return null;
            }
        }
        if (left instanceof Object[]) {
            try {
                return ((Object[])left)[asInt(right)];
            }
            catch (IndexOutOfBoundsException ioobe) {
                return null;
            }
        }
        Class<?> leftClass = left.getClass();
        if (leftClass.isArray()) {
            try {
                return Array.get(left, asInt(right));
            }
            catch (IndexOutOfBoundsException ioobe) {
                return null;
            }
        }
        String rightString = asString(right);
        StringBuilder sb = new StringBuilder("get");
        try {
            sb.append(Character.toUpperCase(rightString.charAt(0)));
            if (rightString.length() > 1)
                sb.append(rightString.substring(1));
            Method method = findPublicGetter(leftClass, sb.toString());
            return method.invoke(left, (Object[])null);
        }
        catch (Exception e) {
        }
        sb.setLength(0);
        sb.append("is");
        try {
            sb.append(Character.toUpperCase(rightString.charAt(0)));
            if (rightString.length() > 1)
                sb.append(rightString.substring(1));
            Method method = findPublicGetter(leftClass, sb.toString());
            return method.invoke(left, (Object[])null);
        }
        catch (Exception e) {
            throw new IndexException();
        }
    }

    /**
     * Set the result of an indexing operation - either a [] or dot operation.
     *
     * @param left      the object on the left of the expression
     * @param right     the object on the right of the dot or within the brackets
     * @param value     the new value
     * @throws EvaluationException on any errors
     */
    @SuppressWarnings("unchecked")
    public static void setIndexed(Object left, Object right, Object value)
            throws EvaluationException {
        if (left == null || right == null)
            throw new AssignException();
        if (left instanceof Map) {
            ((Map<Object, Object>)left).put(right, value);
            return;
        }
        if (left instanceof List) {
            try {
                ((List<Object>)left).set(asInt(right), value);
                return;
            }
            catch (IndexOutOfBoundsException ioobe) {
                throw new AssignException();
            }
        }
        if (left instanceof Object[]) {
            try {
                ((Object[])left)[asInt(right)] = value;
                return;
            }
            catch (IndexOutOfBoundsException ioobe) {
                throw new AssignException();
            }
        }
        Class<?> leftClass = left.getClass();
        if (leftClass.isArray()) {
            try {
                Array.set(left, asInt(right), value);
                return;
            }
            catch (IndexOutOfBoundsException ioobe) {
                throw new AssignException();
            }
        }
        String rightString = asString(right);
        StringBuilder sb = new StringBuilder("set");
        try {
            sb.append(Character.toUpperCase(rightString.charAt(0)));
            if (rightString.length() > 1)
                sb.append(rightString.substring(1));
            Class<?> valueClass = value == null ? null : value.getClass();
            Method method = findPublicSetter(leftClass, sb.toString(), valueClass);
            method.invoke(left, new Object[] { value });
            return;
        }
        catch (Exception e) {
            throw new IndexException();
        }
    }

    /**
     * Check whether character is valid as the start of an identifier.
     *
     * @param ch    the character
     * @return      {@code true} if character is valid
     */
    public static boolean isIdentifierStart(char ch) {
        return Character.isJavaIdentifierStart(ch);
    }

    /**
     * Check whether character is valid as a part of an identifier.
     *
     * @param ch    the character
     * @return      {@code true} if character is valid
     */
    public static boolean isIdentifierPart(char ch) {
        return Character.isJavaIdentifierPart(ch);
    }

    /**
     * Check whether string is valid as an identifier.
     *
     * @param str   the string (any {@link CharSequence} is allowed)
     * @return      {@code true} if string is valid
     */
    public static boolean isValidIdentifier(CharSequence str) {
        if (str == null || str.length() == 0)
            return false;
        if (!isIdentifierStart(str.charAt(0)))
            return false;
        for (int i = 1; i < str.length(); ++i)
            if (!isIdentifierPart(str.charAt(i)))
                return false;
        return true;
    }

    /**
     * Tests whether an operand object is floating point - a {@code Float}, a {@code Double} or
     * a string that could be converted to a floating point number.
     *
     * @param a     the operand object to test
     * @return      {@code true} if the operand is floating point
     */
    protected static boolean floatOrStringOperand(Object a) {
        return a instanceof Float || a instanceof Double || floatString(a);
    }

    /**
     * Tests whether an operand object is a floating point object.
     *
     * @param a     the operand object to test
     * @return      {@code true} if the operand is a floating point object
     */
    protected static boolean floatOperand(Object a) {
        return a instanceof Float || a instanceof Double;
    }

    /**
     * Tests whether an operand object is a string that should be treated as a floating point
     * number.  Note that it does not confirm that the string contains a valid number, only that
     * any attempt to convert it to a number should use a floating point conversion.
     *
     * @param a     the operand object to test
     * @return      {@code true} if the operand is a floating point string
     */
    protected static boolean floatString(Object a) {
        if (a instanceof String) {
            String string = (String)a;
            if (string.indexOf('.') >= 0 || string.indexOf('e') >= 0 ||
                    string.indexOf('E') >= 0)
                return true;
        }
        return false;
    }

    /**
     * Tests whether an operand object is an integer (e.g. {@code Long}) object.
     *
     * @param a     the operand object to test
     * @return      {@code true} if the operand is an integer object
     */
    protected static boolean longOperand(Object a) {
        return a instanceof Byte || a instanceof Short || a instanceof Long ||
                a instanceof Integer || a instanceof Character;
    }

    /**
     * Find a public getter method for the named property.  The class may not be public, or the
     * getter method in that class may not be public, so we need to go up the class hierarchy or
     * the implemented interfaces to locate a public method on a public class or interface.
     *
     * @param cls   the class of the object
     * @param name  the getter method name
     * @return      the {@link Method} object.
     */
    private static Method findPublicGetter(Class<?> cls, String name) {
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            if (Modifier.isPublic(c.getModifiers())) {
                try {
                    Method method = c.getMethod(name, (Class<?>[])null);
                    if (Modifier.isPublic(method.getModifiers()))
                        return method;
                }
                catch (SecurityException e) {
                }
                catch (NoSuchMethodException e) {
                }
            }
        }
        Class<?>[] ifaces = cls.getInterfaces();
        for (int i = 0, n = ifaces.length; i < n; ++i) {
            Method method = findPublicGetter(ifaces[i], name);
            if (method != null)
                return method;
        }
        return null;
    }

    /**
     * Find a public setter method for the named property.  The class may not be public, or the
     * setter method in that class may not be public, so we need to go up the class hierarchy or
     * the implemented interfaces to locate a public method on a public class or interface.
     *
     * @param leftClass     the class of the object
     * @param name          the setter method name
     * @param valueClass    the class of the value
     * @return              the {@link Method} object.
     */
    private static Method findPublicSetter(Class<?> leftClass, String name,
            Class<?> valueClass) {
        for (Class<?> lc = leftClass; lc != null; lc = lc.getSuperclass()) {
            if (Modifier.isPublic(lc.getModifiers())) {
                try {
                    Method[] methods = lc.getMethods();
                    for (int i = 0, n = methods.length; i < n; ++i) {
                        Method method = methods[i];
                        if (method.getName().equals(name) &&
                                Modifier.isPublic(method.getModifiers())) {
                            Class<?>[] params = method.getParameterTypes();
                            if (params.length == 1 && params[0].isAssignableFrom(valueClass))
                            return method;
                        }
                    }
                }
                catch (SecurityException e) {
                }
            }
        }
        Class<?>[] ifaces = leftClass.getInterfaces();
        for (int i = 0, n = ifaces.length; i < n; ++i) {
            Method method = findPublicSetter(ifaces[i], name, valueClass);
            if (method != null)
                return method;
        }
        return null;
    }

    /**
     * The expression parser.  There is a static instance of this class in the
     * {@code Expression} class which will translate the standard form of the Expression
     * Language; non-standard uses (e.g. the use of the assign operator) will require a specific
     * parser to be instantiated and customized before use.
     */
    public static class Parser {

        private Resolver resolver = null;
        private boolean assignAllowed = false;
        private boolean conditionalAllowed = false;
        private boolean caseConvertAllowed = false;
        private boolean matchAllowed = false;
        private boolean joinAllowed = false;
        private boolean arrayOperationAllowed = false;

        /**
         * Get the state of the "assign allowed" flag.  When this flag is set to {@code true}
         * the assignment operator is recognized by the parser (this is an extension to the
         * JSTL 1.0 EL specification).
         *
         * @return  the state of the "assign allowed" flag
         */
        public boolean isAssignAllowed() {
            return assignAllowed;
        }

        /**
         * Set the new state of the "assign allowed" flag.
         *
         * @param assignAllowed     the new state of the "assign allowed" flag
         * @see #isAssignAllowed()
         */
        public void setAssignAllowed(boolean assignAllowed) {
            this.assignAllowed = assignAllowed;
        }

        /**
         * Get the state of the "conditional allowed" flag.  When this flag is set to
         * {@code true} the conditional (triadic) operator is recognized by the parser (this is
         * an extension to the JSTL 1.0 EL specification).
         *
         * @return  the state of the "conditional allowed" flag
         */
        public boolean isConditionalAllowed() {
            return conditionalAllowed;
        }

        /**
         * Set the new state of the "conditional allowed" flag.
         *
         * @param conditionalAllowed    the new state of the "conditional allowed" flag
         * @see #isConditionalAllowed()
         */
        public void setConditionalAllowed(boolean conditionalAllowed) {
            this.conditionalAllowed = conditionalAllowed;
        }

        /**
         * Get the state of the "case convert allowed" flag.  When this flag is set to
         * {@code true} the {@code toupper} and {@code tolower} case conversion operators are
         * recognized by the parser (this is an extension to the JSTL 1.0 EL specification).
         *
         * @return  the state of the "case convert allowed" flag
         */
        public boolean isCaseConvertAllowed() {
            return caseConvertAllowed;
        }

        /**
         * Set the new state of the "case convert allowed" flag.
         *
         * @param caseConvertAllowed    the new state of the "case convert allowed" flag
         * @see #isCaseConvertAllowed()
         */
        public void setCaseConvertAllowed(boolean caseConvertAllowed) {
            this.caseConvertAllowed = caseConvertAllowed;
        }

        /**
         * Get the state of the "match allowed" flag.  When this flag is set to {@code true} the
         * pattern match operator is recognized by the parser (this is an extension to the
         * JSTL 1.0 EL specification).
         *
         * @return  the state of the "match allowed" flag
         */
        public boolean isMatchAllowed() {
            return matchAllowed;
        }

        /**
         * Set the new state of the "match allowed" flag.
         *
         * @param matchAllowed      the new state of the "match allowed" flag
         * @see #isMatchAllowed()
         */
        public void setMatchAllowed(boolean matchAllowed) {
            this.matchAllowed = matchAllowed;
        }

        /**
         * Get the state of the "join allowed" flag.  When this flag is set to {@code true} the
         * string concatenation (join) operator is recognized by the parser (this is an
         * extension to the JSTL 1.0 EL specification).
         *
         * @return  the state of the "join allowed" flag
         */
        public boolean isJoinAllowed() {
            return joinAllowed;
        }

        /**
         * Set the new state of the "join allowed" flag.
         *
         * @param joinAllowed   the new state of the "join allowed" flag
         * @see #isJoinAllowed()
         */
        public void setJoinAllowed(boolean joinAllowed) {
            this.joinAllowed = joinAllowed;
        }

        /**
         * Get the state of the "array operation allowed" flag.  When this flag is set to
         * {@code true} the array/list length and sum operators are recognized by the parser
         * (this is an extension to the JSTL 1.0 EL specification).
         *
         * @return  the state of the "array operation allowed" flag
         */
        public boolean isArrayOperationAllowed() {
            return arrayOperationAllowed;
        }

        /**
         * Set the new state of the "array operation allowed" flag.
         *
         * @param arrayOperationAllowed     the new state of the "array operation allowed" flag
         * @see #isArrayOperationAllowed()
         */
        public void setArrayOperationAllowed(boolean arrayOperationAllowed) {
            this.arrayOperationAllowed = arrayOperationAllowed;
        }

        /**
         * Parse an expression from text to an {@code Expression} tree.  The parse operation
         * will start at the current index position of the {@link ELParseText} object, and will
         * stop either at the end of the text or on the first unrecognized element.  The
         * {@link ELParseText} will be updated past the text translated, and the caller must
         * confirm that this is the end of the expression.
         *
         * @param test      an {@link ELParseText} object containing the text to parse
         *                  (not including ${ })
         * @param resolver  a {@link Resolver} object to resolve names in
         *                  the expression
         * @return          the {@code Expression} tree
         * @throws ParseException on any errors
         */
        public Expression parseExpression(ELParseText text, Resolver resolver)
                throws ParseException {
            Operator expression = new Parentheses(null);
            Operator current = expression;
            for (;;) {
                text.skipSpaces();
                if (text.isExhausted())
                    throw new UnexpectedEndException();
                // first look for a prefix operator (-, !, not, empty)
                if (text.match('-')) {
                    if (text.match('.') || text.matchDecFixed(1)) {
                        // it's a numeric literal - backspace and leave it
                        text.back(2);
                    }
                    else {
                        Operator op = new Negate(null);
                        current.setRight(op);
                        current = op;
                        continue;
                    }
                }
                if (text.match('!') || text.matchName("not")) {
                    Operator op = new Not(null);
                    current.setRight(op);
                    current = op;
                    continue;
                }
                if (text.matchName("empty")) {
                    Operator op = new Empty(null);
                    current.setRight(op);
                    current = op;
                    continue;
                }
                if (isCaseConvertAllowed() && text.matchName("toupper")) {
                    Operator op = new ToUpper(null);
                    current.setRight(op);
                    current = op;
                    continue;
                }
                if (isCaseConvertAllowed() && text.matchName("tolower")) {
                    Operator op = new ToLower(null);
                    current.setRight(op);
                    current = op;
                    continue;
                }
                if (isArrayOperationAllowed() && text.matchName("length")) {
                    Operator op = new Length(null);
                    current.setRight(op);
                    current = op;
                    continue;
                }
                if (isArrayOperationAllowed() && text.matchName("sum")) {
                    Operator op = new Sum(null);
                    current.setRight(op);
                    current = op;
                    continue;
                }
                // check for parentheses and a subexpression (recursion)
                if (text.match('(')) {
                    Expression nested = parseExpression(text, resolver);
                    if (!text.match(')'))
                        throw new UnmatchedParenthesisException();
                    if (!(nested instanceof Parentheses))
                        nested = new Parentheses(nested);
                    current.setRight(nested);
                }
                else if (text.matchNumber()) {
                    // numeric literal
                    current.setRight(new Constant(text.getResultNumber()));
                }
                else if (text.matchStringLiteral()) {
                    // string literal
                    current.setRight(new Constant(text.getResultStringLiteral()));
                }
                else if (text.matchName("null")) {
                    // null literal
                    current.setRight(new Constant(null));
                }
                else if (text.matchName("true")) {
                    // boolean literal
                    current.setRight(trueConstant);
                }
                else if (text.matchName("false")) {
                    // boolean literal
                    current.setRight(falseConstant);
                }
                else if (text.matchName("and") || text.matchName("div") ||
                        text.matchName("eq") || text.matchName("ge") ||
                        text.matchName("gt") || text.matchName("instanceof") ||
                        text.matchName("le") || text.matchName("lt") ||
                        text.matchName("mod") || text.matchName("ne") ||
                        text.matchName("or")) {
                    // other reserved word - error
                    throw new ReservedWordException();
                }
                else if (text.matchName()) {
                    // might be name, or might be function call
                    int currentIndex = text.getIndex();
                    String identString = text.getResultString();
                    if (resolver instanceof ExtendedResolver && text.match(':') &&
                            text.matchName()) {
                        ExtendedResolver extResolver = (ExtendedResolver)resolver;
                        String namespace = extResolver.resolvePrefix(identString);
                        if (namespace == null)
                            throw new FunctionParseException();
                        String classname = extResolver.resolveNamespace(namespace);
                        if (classname == null)
                            throw new FunctionParseException();
                        FunctionCall functionCall =
                                new FunctionCall(classname, text.getResultString());
                        text.skipSpaces();
                        if (!text.match('('))
                            throw new FunctionParseException();
                        text.skipSpaces();
                        if (!text.match(')')) {
                            for (;;) {
                                Expression nested = parseExpression(text, resolver);
                                functionCall.addArgument(nested);
                                if (text.isExhausted())
                                    throw new FunctionParseException();
                                if (text.match(')'))
                                    break;
                                if (!text.match(','))
                                    throw new FunctionParseException();
                            }
                        }
                        current.setRight(functionCall);
                    }
                    else {
                        text.setIndex(currentIndex);
                        // lookup identifier using external name resolver
                        Expression identifier = null;
                        if (resolver != null)
                            identifier = resolver.resolve(identString);
                        if (identifier == null)
                            throw new IdentifierException(identString);
                        current.setRight(identifier);
                    }
                }
                else {
                    // none of the above - error
                    throw new UnexpectedElementException();
                }
                text.skipSpaces();
                // now check for . and [] (possibly multiple)
                Expression currentRight = current.getRight();
                if (currentRight instanceof Parentheses || currentRight instanceof Variable ||
                        currentRight instanceof FunctionCall) {
                    // TODO is the above test necessary?
                    // should indexing be allowed after any type of expression?
                    while (!text.isExhausted()) {
                        if (text.match('.')) {
                            // . property
                            text.skipSpaces();
                            if (!text.matchName())
                                throw new PropertyException();
                            Indexed indexed = new Indexed(currentRight,
                                    new Constant(text.getResultString()));
                            current.setRight(indexed);
                            currentRight = indexed;
                            text.skipSpaces();
                        }
                        else if (text.match('[')) {
                            // [ index ]
                            Expression nested = parseExpression(text, resolver);
                            if (!text.match(']'))
                                throw new UnmatchedBracketException();
                            Indexed indexed = new Indexed(currentRight, nested);
                            current.setRight(indexed);
                            currentRight = indexed;
                            text.skipSpaces();
                        }
                        else
                            break;
                    }
                }
                if (text.isExhausted())
                    break;
                // now check for a diadic operator (like + or ==)
                Diadic diadic = null;
                if (text.matchName("and") || text.match("&&"))
                    diadic = new And(null, null);
                else if (text.matchName("or") || text.match("||"))
                    diadic = new Or(null, null);
                else if (text.match('+'))
                    diadic = new Plus(null, null);
                else if (text.match('-'))
                    diadic = new Minus(null, null);
                else if (text.match('*'))
                    diadic = new Multiply(null, null);
                else if (text.match('/') || text.matchName("div"))
                    diadic = new Divide(null, null);
                else if (text.match('%') || text.matchName("mod"))
                    diadic = new Modulo(null, null);
                else if (text.match(">=") || text.matchName("ge"))
                    diadic = new GreaterEqual(null, null);
                else if (text.match("<=") || text.matchName("le"))
                    diadic = new LessEqual(null, null);
                else if (text.match('>') || text.matchName("gt"))
                    diadic = new GreaterThan(null, null);
                else if (text.match('<') || text.matchName("lt"))
                    diadic = new LessThan(null, null);
                else if (text.match("==") || text.matchName("eq"))
                    diadic = new Equal(null, null);
                else if (text.match("!=") || text.matchName("ne"))
                    diadic = new NotEqual(null, null);
                else if (isConditionalAllowed() && text.match('?'))
                    diadic = new Conditional(null, null);
                else if (isConditionalAllowed() && text.match(':'))
                    diadic = new Else(null, null);
                else if (isAssignAllowed() && text.match('='))
                    diadic = new Assign(null, null);
                else if (isMatchAllowed() && text.match("~="))
                    diadic = new Match(null, null);
                else if (isJoinAllowed() && text.match('#'))
                    diadic = new Join(null, null);
                else
                    break;
                // find the right point in the expression to add operator
                // (based on priority and left-associativity)
                int prio = diadic.getPriority();
                Operator x = expression;
                for (;;) {
                    Expression right = x.getRight();
                    if (!(right instanceof Diadic))
                        break;
                    Diadic rightOp = (Diadic)right;
                    int rightPrio = rightOp.getPriority();
                    if (rightPrio > prio)
                        break;
                    if (rightPrio == prio && rightOp.isLeftAssociative())
                        break;
                    x = rightOp;
                }
                diadic.setLeft(x.getRight());
                x.setRight(diadic);
                current = diadic;
            }
            Expression result = expression.getRight();
            if (isConditionalAllowed())
                checkConditional(result);
            return result;
        }

        /**
         * Check that every conditional expression has a matching else subexpression, and that
         * no else expressions appear other than as subexpressions of a conditional expression.
         *
         * @param expr  the expression to be checked
         * @throws ParseException on any errors
         */
        private void checkConditional(Expression expr) throws ParseException {
            if (expr instanceof Conditional) {
                Conditional cond = (Conditional)expr;
                checkConditional(cond.getLeft());
                Expression right = cond.getRight();
                if (!(right instanceof Else))
                    throw new ConditionalException();
                Else elseExpr = (Else)right;
                checkConditional(elseExpr.getLeft());
                checkConditional(elseExpr.getRight());
            }
            else if (expr instanceof Else) {
                throw new ElseException();
            }
            else if (expr instanceof Diadic) {
                Diadic diadic = (Diadic)expr;
                checkConditional(diadic.getLeft());
                checkConditional(diadic.getRight());
            }
            else if (expr instanceof Operator) {
                Operator operator = (Operator)expr;
                checkConditional(operator.getRight());
            }
        }

        /**
         * Parse an entire string to an {@code Expression} tree.
         *
         * @param str       text to parse (not including ${ })
         * @param resolver  a {@link Resolver} object to resolve names in the expression
         * @return          the {@code Expression} tree
         * @throws ParseException on any errors
         */
        public Expression parseExpression(CharSequence str, Resolver resolver)
                throws ParseException {
            ELParseText text = new ELParseText(str);
            Expression expression = parseExpression(text, resolver);
            if (!text.isExhausted())
                throw new UnparsedStringException();
            return expression;
        }

        /**
         * Replace all occurrences of <code>$&#123;...&#125;</code> in a string with the
         * contents of the braces evaluated as an expression.
         *
         * @param source    the input text
         * @param resolver  a {@link Resolver} object to resolve names in the expression
         * @return          the text following substitutions
         * @throws ExpressionException on any errors
         */
        public String substitute(String source, Resolver resolver) throws ExpressionException {
            ELParseText text = new ELParseText(source);
            text.skipTo("${");
            if (text.isExhausted())
                return source;
            StringBuilder sb = null;
            if (text.getIndex() == 0) {
                // optimize case where source is a single substitution
                text.skip(2);
                String result = parseExpression(text, resolver).asString();
                if (!text.match('}'))
                    throw new UnmatchedBraceException();
                if (text.isExhausted())
                    return result;
                sb = new StringBuilder(result);
                text.skipTo("${");
                if (text.isExhausted()) {
                    text.appendResultTo(sb);
                    return sb.toString();
                }
            }
            else
                sb = new StringBuilder();
            for (;;) {
                text.appendResultTo(sb);
                text.skip(2); // to get here, we must have matched "${"
                sb.append(parseExpression(text, resolver).asString());
                if (!text.match('}'))
                    throw new UnmatchedBraceException();
                text.skipTo("${");
                if (text.isExhausted())
                    break;
            }
            text.appendResultTo(sb);
            return sb.toString();
        }

        /**
         * Parse a string and return an expression which will replace all occurrences of
         * <code>$&#123;...&#125;</code> in the string with the contents of the braces evaluated
         * as an expression.
         *
         * @param str       the text to be parsed
         * @param resolver  a {@link Resolver} object to resolve names in the expression
         * @return          the {@code Expression} tree
         * @throws ParseException on any errors
         */
        public Expression parseSubstitution(CharSequence str, Resolver resolver)
                throws ParseException {
            Concat concat = new Concat();
            ELParseText text = new ELParseText(str);
            for (;;) {
                if (text.isExhausted())
                    break;
                if (text.match("${")) {
                    concat.addExpression(parseExpression(text, resolver));
                    if (!text.match('}'))
                        throw new UnmatchedBraceException();
                }
                else {
                    text.skipTo("${");
                    concat.addExpression(new Constant(text.getResultString()));
                }
            }
            return concat.singleExpression();
        }

        /**
         * Parse an expression from text to an {@code Expression} tree.  This method is
         * identical to {@link #parseExpression(ELParseText, Resolver)} above except that it
         * uses a {@link Resolver} set by the {@link #setResolver(Resolver)} method.
         *
         * @param test      an {@link ELParseText} object containing the text to parse
         *                  (not including ${ })
         * @return          the {@code Expression} tree
         * @throws ParseException on any errors
         */
        public Expression parseExpression(ELParseText text) throws ParseException {
            return parseExpression(text, resolver);
        }

        /**
         * Parse an entire string to an {@code Expression} tree.  This method is identical to
         * {@link #parseExpression(CharSequence, Resolver)} above except that it uses a
         * {@link Resolver} set by the {@link #setResolver(Resolver)} method.
         *
         * @param str       text to parse (not including ${ })
         * @return          the {@code Expression} tree
         * @throws ParseException on any errors
         */
        public Expression parseExpression(CharSequence str) throws ParseException {
            return parseExpression(str, resolver);
        }

        /**
         * Replace all occurrences of <code>$&#123;...&#125;</code> in a string with the
         * contents of the braces evaluated as an expression.  This method is identical to
         * {@link #substitute(String, Resolver)} above except that it uses a
         * {@link Resolver} set by the {@link #setResolver(Resolver)} method.
         *
         * @param source    the input text
         * @return          the text following substitutions
         * @throws ExpressionException on any errors
         */
        public String substitute(String source) throws ExpressionException {
            return substitute(source, resolver);
        }

        /**
         * Parse a string and return an expression which will replace all occurrences of
         * <code>$&#123;...&#125;</code> in the string with the contents of the braces evaluated
         * as an expression. This method is identical to {@link #parseSubstitution(CharSequence,
         * Resolver)} above except that it uses a {@link Resolver} set by the
         * {@link #setResolver(Resolver)} method.
         *
         * @param str       the text to be parsed
         * @return          the {@code Expression} tree
         * @throws ParseException on any errors
         */
        public Expression parseSubstitution(CharSequence str) throws ParseException {
            return parseSubstitution(str, resolver);
        }

        /**
         * Get the current {@link Resolver}.
         *
         * @return the current {@link Resolver}
         */
        public Resolver getResolver() {
            return resolver;
        }

        /**
         * Set the {@link Resolver} for use when no {@link Resolver} is specified on a method
         * call.
         *
         * @param resolver the {@link Resolver}
         */
        public void setResolver(Resolver resolver) {
            this.resolver = resolver;
        }

    }

    /**
     * An interface to represent assignable expressions.  This is an extension to the JSTL 1.0
     * EL specification.
     */
    public static interface Assignable {

        /**
         * Assign a value to this variable.
         *
         * @param value the value to be stored
         * @throws EvaluationException on any errors
         */
        void assign(Object value) throws EvaluationException;

    }

    /**
     * A class to represent a variable in an expression tree.  Objects of this
     * class are generally returned by the {@link Resolver}.
     *
     * <p>This class contains no functionality; the {@link Resolver} must
     * return an implementation class which provides <code>evaluate()</code>
     * and <code>assign()</code> methods.</p>
     */
    public static abstract class Variable extends Expression
            implements Assignable {

    }

    /**
     * A class to represent a constant in an expression tree.  Objects of this class are created
     * for constants in the expression.  The {@link #optimize()} method will pre-evaluate
     * operators with constant operands where possible.
     */
    public static class Constant extends Expression {

        private Object value;

        /**
         * Construct a {@code Constant} with a given object value.
         *
         * @param value  the object
         */
        public Constant(Object value) {
            this.value = value;
        }

        /**
         * Get the value represented by the constant.
         *
         * @return  the value represented by the constant
         */
        public Object getValue() {
            return value;
        }

        /**
         * Evaluate the subexpression.
         *
         * @return  the value represented by the constant
         */
        @Override
        public Object evaluate() {
            return value;
        }

        /**
         * Return {@code true} to indicate the expression is constant.
         *
         * @return {@code true} - the expression is constant
         */
        @Override
        public boolean isConstant() {
            return true;
        }

        /**
         * Get the result type of this constant.
         *
         * @return  the result type of the constant
         */
        @Override
        public Class<?> getType() {
            return value == null ? null : value.getClass();
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a constant with
         * the same value.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Constant))
                return false;
            Constant c = (Constant)o;
            if (value == null)
                return c.value == null;
            return value.equals(c.value);
        }

        /**
         * Ensure that objects which compare as equal return the same hash code.
         *
         * @return the hash code
         * @see Object#hashCode()
         */
        @Override
        public int hashCode() {
            return value == null ? 0 : value.hashCode();
        }

        /**
         * Convert to string.  This returns the string representation of the constant in the
         * Expression Language.  String constants require special handling to ensure that quotes
         * are used and special characters are escaped.
         *
         * @return  a string representation of the constant
         */
        @Override
        public String toString() {
            if (value == null)
                return "null";
            if (value instanceof String) {
                StringBuilder sb = new StringBuilder();
                String str = (String)value;
                char quote = str.indexOf('\'') >= 0 && str.indexOf('"') < 0 ? '"' : '\'';
                sb.append(quote);
                for (int i = 0, n = str.length(); i < n; ++i) {
                    char ch = str.charAt(i);
                    if (ch == quote || ch == '\\')
                        sb.append('\\');
                    sb.append(ch);
                }
                sb.append(quote);
                return sb.toString();
            }
            // note - for all other forms of constant generated by the parser, the standard
            // toString() conversion on the object will produce the correct form of string; any
            // constants generated by a Resolver must provide their own formatting, probably by
            // subclassing Constant
            return value.toString();
        }

    }

    /**
     * A base class for all operators, both monadic (one operand) and diadic (two operands).
     */
    public static abstract class Operator extends Expression {

        private Expression right;

        /**
         * Construct an <code>Operator</code> with a given operand.
         *
         * @param right  the right or only operand
         */
        public Operator(Expression right) {
            this.right = right;
        }

        /**
         * Get the right operand.
         *
         * @return  the right operand
         */
        public Expression getRight() {
            return right;
        }

        /**
         * Set the right operand.
         *
         * @param right  the new value for the right operand
         */
        void setRight(Expression right) {
            this.right = right;
        }

        /**
         * Optimize the subexpression.  The default behavior is to optimize the operand, then if
         * it is constant, execute the operation.
         *
         * @return  the constant result or the operator itself
         */
        @Override
        public Expression optimize() {
            if (optimizeRightOperand()) {
                try {
                    return new Constant(evaluate());
                }
                catch (EvaluationException ee) {
                    // ignore
                }
            }
            return this;
        }

        /**
         * Optimize the operand.
         *
         * @return {@code true} if the operand is constant
         */
        public boolean optimizeRightOperand() {
            Expression optRight = right.optimize();
            right = optRight;
            return optRight.isConstant();
        }

        /**
         * Get the priority of the operator.  The priority is used to determine the order of
         * evaluation of an expression with multiple operations.  For example, a + b * c is
         * evaluated as (a + (b * c)).
         *
         * @return  the priority
         */
        public abstract int getPriority();

        /**
         * Get the name of the operator.  The name is the token which represents the operator in
         * the Expression Language.
         *
         * @return  the name
         */
        public abstract String getName();

        /**
         * Test for equality.  Return {@code true} only if the other object is an
         * {@link Operator} with an equal operand.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Operator &&
                    getRight().equals(((Operator)o).getRight());
        }

        /**
         * Ensure that objects which compare as equal return the same hash code.
         *
         * @return the hash code
         * @see Object#hashCode()
         */
        @Override
        public int hashCode() {
            return getRight().hashCode();
        }

        /**
         * Convert subexpression to string.
         *
         * @return  a string representation of the subexpression
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            String name = getName();
            sb.append(name);
            Expression expr = getRight();
            if (expr instanceof Operator) {
                sb.append('(');
                sb.append(expr);
                sb.append(')');
            }
            else {
                if (isIdentifierStart(name.charAt(0)))
                    sb.append(' ');
                sb.append(expr);
            }
            return sb.toString();
        }

    }

    /**
     * A base class for all diadic (two operand) operators.
     */
    public static abstract class Diadic extends Operator {

        private Expression left;

        /**
         * Construct a <code>Diadic</code> with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public Diadic(Expression left, Expression right) {
            super(right);
            this.left = left;
        }

        /**
         * Get the left operand.
         *
         * @return  the left operand
         */
        public Expression getLeft() {
            return left;
        }

        /**
         * Set the left operand.
         *
         * @param left  the new value for the left operand
         */
        void setLeft(Expression left) {
            this.left = left;
        }

        /**
         * Optimize the subexpression.  The default behavior is to optimize the operands, then
         * if both are constant, execute the operation.
         *
         * @return  the constant result or the operator itself
         */
        @Override
        public Expression optimize() {
            if (optimizeOperands()) {
                try {
                    return new Constant(evaluate());
                }
                catch (EvaluationException ee) {
                    // ignore
                }
            }
            return this;
        }

        /**
         * Optimize the operands.
         *
         * @return {@code true} if both operands are constant
         */
        public boolean optimizeOperands() {
            boolean rightConstant = optimizeRightOperand();
            Expression optLeft = left.optimize();
            left = optLeft;
            return rightConstant && optLeft.isConstant();
        }

        /**
         * Return {@code true} if the operation is commutative.  The default is {@code false}.
         *
         * @return {@code false}
         */
        public boolean isCommutative() {
            return false;
        }

        /**
         * Return {@code true} if the operation is associative.  The default is {@code false}.
         *
         * @return {@code false}
         */
        public boolean isAssociative() {
            return false;
        }

        /**
         * Return {@code true} if the operation is left-associative.  The default is
         * {@code true}.
         *
         * @return {@code true}
         */
        public boolean isLeftAssociative() {
            return true;
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a {@link Diadic}
         * operation with equal operands, or is commutative with equal operands reversed, or is
         * associative and all operands match.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Diadic))
                return false;
            Diadic op = (Diadic)o;
            if (getLeft().equals(op.getLeft()) &&
                    getRight().equals(op.getRight()))
                return true;
            if (isCommutative() && getLeft().equals(op.getRight()) &&
                    getRight().equals(op.getLeft()))
                return true;
            if (!isAssociative())
                return false;
            List<Expression> operands = new ArrayList<>();
            accumOperands(operands, getLeft());
            accumOperands(operands, getRight());
            if (!checkOperands(operands, op.getLeft()))
                return false;
            if (!checkOperands(operands, op.getRight()))
                return false;
            return operands.isEmpty();
        }

        private void accumOperands(List<Expression> operands, Expression expr) {
            if (expr.getClass().equals(getClass())) {
                Diadic op = (Diadic)expr;
                accumOperands(operands, op.getLeft());
                accumOperands(operands, op.getRight());
            }
            else
                operands.add(expr);
        }

        private boolean checkOperands(List<Expression> operands, Expression expr) {
            if (expr.getClass().equals(getClass())) {
                Diadic op = (Diadic)expr;
                if (!checkOperands(operands, op.getLeft()))
                    return false;
                if (!checkOperands(operands, op.getRight()))
                    return false;
                return true;
            }
            for (int i = 0, n = operands.size(); i < n; ++i) {
                if (expr.equals(operands.get(i))) {
                    operands.remove(i);
                    return true;
                }
            }
            return false;
        }

        /**
         * Ensure that objects which compare as equal return the same hash code.
         *
         * @return the hash code
         * @see Object#hashCode()
         */
        @Override
        public int hashCode() {
            return getLeft().hashCode() ^ getRight().hashCode();
        }

        /**
         * Convert subexpression to string.
         *
         * @return  a string representation of the subexpression
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            Expression expr = getLeft();
            if (expr instanceof Operator) {
                sb.append('(');
                sb.append(expr);
                sb.append(')');
            }
            else
                sb.append(expr);
            sb.append(' ');
            sb.append(getName());
            sb.append(' ');
            expr = getRight();
            if (expr instanceof Operator) {
                sb.append('(');
                sb.append(expr);
                sb.append(')');
            }
            else
                sb.append(expr);
            return sb.toString();
        }

    }

    /**
     * A class to represent the indexing operation.  The expression language converts property
     * references of the form {@code object.property} to {@code object["property"]}, so both of
     * these are handled by this operator.
     *
     * <p>Much of the behavior of this class is implemented as static methods on the
     * {@code Expression} class so that they may be accessed more easily from outside the
     * class.</p>
     */
    public static class Indexed extends Diadic implements Assignable {

        public static final String name = ".";
        public static final int priority = 9;

        /**
         * Construct an <code>Indexed</code> operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand (or the one in brackets)
         */
        public Indexed(Expression left, Expression right) {
            super(left, right);
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
         * Get the name of the operator (the token for this operator in an expression).
         *
         * @return the name
         */
        @Override
        public String getName() {
            return name;
        }

        /**
         * Evaluate the subexpression.  The rules for this operation are given in the
         * specification document referenced at the start of the {@code Expression} class.
         *
         * @return  the object resulting from the indexing operation
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            Object left = getLeft().evaluate();
            if (left == null)
                return null;
            return getIndexed(left, getRight().evaluate());
        }

        /**
         * Assign a value to the object addressed by this indexing operation.
         *
         * @param value the value to be stored
         * @throws EvaluationException on any errors
         */
        @Override
        public void assign(Object value) throws EvaluationException {
            Object left = getLeft().evaluate();
            if (left == null)
                throw new AssignException();
            setIndexed(left, getRight().evaluate(), value);
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is an
         * {@code Indexed} operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Indexed && super.equals(o);
        }

        /**
         * Convert subexpression to string.  This operator requires special handling because of
         * the syntax of indexing operations.
         *
         * @return  a string representation of the subexpression
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            Expression expr = getLeft();
            if (expr instanceof Operator && ((Operator)expr).getPriority() < priority) {
                sb.append('(');
                sb.append(expr);
                sb.append(')');
            }
            else
                sb.append(expr);
            expr = getRight();
            if (isPropertyName(expr)) {
                sb.append('.');
                sb.append(((Constant)expr).evaluate());
            }
            else {
                sb.append('[');
                sb.append(expr);
                sb.append(']');
            }
            return sb.toString();
        }

        private boolean isPropertyName(Expression expr) {
            if (!(expr instanceof Constant))
                return false;
            Object value = ((Constant)expr).evaluate();
            if (!(value instanceof String))
                return false;
            String str = (String)value;
            int n = str.length();
            if (n == 0 || !isIdentifierStart(str.charAt(0)))
                return false;
            for (int i = 1; i < n; ++i)
                if (!isIdentifierPart(str.charAt(i)))
                    return false;
            return true;
        }

    }

    /**
     * A class to represent the assignment operation.  This is an extension to the JSTL 1.0 EL
     * specification.
     */
    public static class Assign extends Diadic {

        public static final String name = "=";
        public static final int priority = -1;

        /**
         * Construct an <code>Assign</code> operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public Assign(Expression left, Expression right) {
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
         * Return {@code false} - the operation is right-associative.
         *
         * @return {@code false}
         */
        @Override
        public boolean isLeftAssociative() {
            return false;
        }

        /**
         * Get the result type of this operator.  The result type is the type of the right
         * operand.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return getRight().getType();
        }

        /**
         * Evaluate the subexpression.  The result of an assignment operation is the value
         * assigned.
         *
         * @return  the object resulting from the assignment operation
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            Expression left = getLeft();
            if (!(left instanceof Assignable))
                throw new AssignException();
            Object right = getRight().evaluate();
            ((Assignable)left).assign(right);
            return right;
        }

        /**
         * Optimize the subexpression.  The assignment operation can not be optimized, and the
         * left operand can only be partially optimized - for example, if the left operand is
         * {@code array[2+2]} the addition can be optimized but the indexing operation can not.
         * The right operand can be fully optimized.
         *
         * @return  the operator itself
         */
        @Override
        public Expression optimize() {
            optimizeRightOperand();
            Expression left = getLeft();
            if (left instanceof Operator)
                ((Operator)left).optimizeRightOperand();
            return this;
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is an {@link Assign}
         * operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Assign && super.equals(o);
        }

    }

    /**
     * A class to represent the conditional expression ( {@code ? :} ).  This is an extension to
     * the JSTL 1.0 EL specification.
     */
    public static class Conditional extends Diadic {

        public static final String name = "?";
        public static final int priority = 0;

        /**
         * Construct a conditional expression with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public Conditional(Expression left, Expression right) {
            super(left, right);
        }

        /**
         * Get the name of the operator (the token for this operator in an
         * expression).
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
         * Return {@code false} - the operation is right-associative.
         *
         * @return {@code false}
         */
        @Override
        public boolean isLeftAssociative() {
            return false;
        }

        /**
         * Get the result type of this operator.  This is determined from the types of the
         * operands of the {@link Else} operator.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            Else elseOp = (Else)getRight();
            Class<?> leftType = elseOp.getLeft().getType();
            Class<?> rightType = elseOp.getRight().getType();
            if (leftType == rightType)
                return leftType;
            if (Number.class.isAssignableFrom(leftType) &&
                    Number.class.isAssignableFrom(rightType))
                return Number.class;
            return Object.class;
        }

        /**
         * Evaluate the conditional expression.  The result of the conditional expression is the
         * left or the right operand of the {@link Else} expression which must be in the right
         * operand position of this expression, depending on the boolean value of the left
         * operand of this expression.
         *
         * @return  the object resulting from the conditional expression
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            Expression right = getRight();
            if (!(right instanceof Else))
                throw new EvaluationException("conditional");
            Else elseExpr = (Else)right;
            Expression result = getLeft().asBoolean() ? elseExpr.getLeft() :
                    elseExpr.getRight();
            return result.evaluate();
        }

        /**
         * Optimize the conditional expression.  If the left operand is constant, the expression
         * is optimized to the subexpression in either the left or the right operand of the
         * {@link Else} expression which must be in the right operand position of this
         * expression.
         *
         * @return the {@code true} or the {@code false} result, or the expression itself
         */
        @Override
        public Expression optimize() {
            Expression optLeft = getLeft().optimize();
            setLeft(optLeft);
            Expression right = getRight();
            if (right instanceof Else) {
                Else elseExpr = (Else)right;
                if (optLeft.isConstant()) {
                    try {
                        return optLeft.asBoolean() ?
                                elseExpr.getLeft().optimize() :
                                elseExpr.getRight().optimize();
                    }
                    catch (EvaluationException e) {
                        // ignore
                    }
                }
                elseExpr.optimizeOperands();
            }
            return this;
        }

        /**
         * Convert subexpression to string.  This operator requires special handling to ensure
         * that the else pseudo-operator is not enclosed in parentheses.
         *
         * @return  a string representation of the subexpression
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            Expression expr = getLeft();
            if (expr instanceof Operator) {
                sb.append('(');
                sb.append(expr);
                sb.append(')');
            }
            else
                sb.append(expr);
            sb.append(' ');
            sb.append(getName());
            sb.append(' ');
            sb.append(getRight());
            return sb.toString();
        }

    }

    /**
     * A class to represent the else portion of a conditional expression ( {@code ? :} ).  It is
     * not so much an expression as a structure to hold two alternative results from the
     * conditional expression, of which this must be the right operand.  This is an extension to
     * the JSTL 1.0 EL specification.
     */
    public static class Else extends Diadic {

        public static final String name = ":";
        public static final int priority = 0;

        /**
         * Construct an else subexpression with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public Else(Expression left, Expression right) {
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
         * Return {@code false} - the operation is right-associative.
         *
         * @return {@code false}
         */
        @Override
        public boolean isLeftAssociative() {
            return false;
        }

        /**
         * Throw an exception - this subexpression should never be executed.
         *
         * @return nothing
         * @throws EvaluationException always
         */
        @Override
        public Object evaluate() throws EvaluationException {
            throw new EvaluationException("else");
        }

    }

    /**
     * A base class to represent most arithmetic operations.
     */
    public static abstract class Arithmetic extends Diadic {

        /**
         * Construct an arithmetic operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public Arithmetic(Expression left, Expression right) {
            super(left, right);
        }

        /**
         * Evaluate the subexpression.  This method determines the type of operands supplied,
         * then calls the appropriate {@code execute()} method in the implementing class to
         * apply the operation.
         *
         * @return  the object resulting from the arithmetic operation
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            Object left = getLeft().evaluate();
            Object right = getRight().evaluate();
            if (left == null && right == null)
                return new Integer(0);
            if (floatOrStringOperand(left) || floatOrStringOperand(right))
                return new Double(execute(asDouble(left), asDouble(right)));
            return new Long(execute(asLong(left), asLong(right)));
        }

        /**
         * Get the result type of this operator.  The result type of an arithmetic operation is
         * always {@link Number} or a subclass.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            Class<?> leftType = getLeft().getType();
            Class<?> rightType = getRight().getType();
            if (leftType == Double.class || leftType == Float.class ||
                    rightType == Double.class || rightType == Float.class)
                return Double.class;
            if ((leftType == Integer.class || leftType == Long.class ||
                    leftType == Short.class|| leftType == Byte.class) &&
                    (rightType == Integer.class || rightType == Long.class ||
                    rightType == Short.class || rightType == Byte.class))
                return Long.class;
            return Number.class; // indeterminate, but must be a number
        }

        /**
         * Apply the operation to {@code long} operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         * @return       the result
         */
        public abstract long execute(long left, long right);

        /**
         * Apply the operation to {@code double} operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         * @return       the result
         */
        public abstract double execute(double left, double right);

    }

    /**
     * An implementation class for the multiply operation.
     */
    public static class Multiply extends Arithmetic {

        public static final String name = "*";
        public static final int priority = 6;

        /**
         * Construct a multiply operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public Multiply(Expression left, Expression right) {
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
         * Return {@code true} - the operation is commutative.
         *
         * @return {@code true}
         */
        @Override
        public boolean isCommutative() {
            return true;
        }

        /**
         * Return {@code true} - the operation is associative.
         *
         * @return {@code true}
         */
        @Override
        public boolean isAssociative() {
            return true;
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
            return left * right;
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
            return left * right;
        }

        /**
         * Optimize the subexpression.  In addition to the default optimization, check for
         * multiply by 0 or 1.
         *
         * @return  the constant result or the operator itself
         */
        @Override
        public Expression optimize() {
            if (optimizeOperands()) {
                try {
                    return new Constant(evaluate());
                }
                catch (EvaluationException ee) {
                    // ignore
                }
            }
            Expression left = getLeft();
            Expression right = getRight();
            if (right.isConstant()) {
                Object val = ((Constant)right).getValue();
                if (val instanceof Integer) {
                    int intVal = ((Integer)val).intValue();
                    if (intVal == 0)
                        return right;
                    if (intVal == 1)
                        return left;
                }
                else if (val instanceof Long) {
                    long longVal = ((Long)val).longValue();
                    if (longVal == 0)
                        return right;
                    if (longVal == 1)
                        return left;
                }
                else if (val instanceof Number) {
                    double doubleVal = ((Number)val).doubleValue();
                    if (doubleVal == 0.0)
                        return right;
                    if (doubleVal == 1.0)
                        return left;
                }
            }
            if (left.isConstant()) {
                Object val = ((Constant)left).getValue();
                if (val instanceof Integer) {
                    int intVal = ((Integer)val).intValue();
                    if (intVal == 0)
                        return left;
                    if (intVal == 1)
                        return right;
                }
                else if (val instanceof Long) {
                    long longVal = ((Long)val).longValue();
                    if (longVal == 0)
                        return left;
                    if (longVal == 1)
                        return right;
                }
                else if (val instanceof Number) {
                    double doubleVal = ((Number)val).doubleValue();
                    if (doubleVal == 0.0)
                        return left;
                    if (doubleVal == 1.0)
                        return right;
                }
            }
            return this;
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a
         * {@code Multiply} operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Multiply && super.equals(o);
        }

    }

    /**
     * An implementation class for the divide operation.  This does not use the
     * {@link Arithmetic} base class because the rules for the divide operator are different
     * from those for the other operators.
     */
    public static class Divide extends Diadic {

        public static final String name = "/";
        public static final int priority = 6;

        /**
         * Construct a divide operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public Divide(Expression left, Expression right) {
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
         * Get the result type of this operator.  The result type of a divide operation is
         * always {@link Double}.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Double.class;
        }

        /**
         * Evaluate the subexpression.  This method converts both operands to {@code double} and
         * performs the operation.
         *
         * @return  the object resulting from the divide operation
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            Object left = getLeft().evaluate();
            Object right = getRight().evaluate();
            if (left == null && right == null)
                return new Integer(0);
            try {
                return new Double(asDouble(left) / asDouble(right));
            }
            catch (EvaluationException ee) {
                throw ee;
            }
            catch (Exception e) {
                throw new EvaluationException("divide");
            }
        }

        /**
         * Optimize the subexpression.  In addition to the default optimization, check for
         * divisor of 1 or dividend of 0.
         *
         * @return  the constant result or the operator itself
         */
        @Override
        public Expression optimize() {
            if (optimizeOperands()) {
                try {
                    return new Constant(evaluate());
                }
                catch (EvaluationException ee) {
                    // ignore
                }
            }
            Expression left = getLeft();
            Expression right = getRight();
            if (right.isConstant()) {
                Object val = ((Constant)right).getValue();
                if (val instanceof Integer) {
                    if (((Integer)val).intValue() == 1)
                        return left;
                }
                else if (val instanceof Long) {
                    if (((Long)val).longValue() == 1)
                        return left;
                }
                else if (val instanceof Number) {
                    if (((Number)val).doubleValue() == 1.0)
                        return left;
                }
            }
            if (left.isConstant()) {
                Object val = ((Constant)left).getValue();
                if (val instanceof Integer) {
                    if (((Integer)val).intValue() == 0)
                        return left;
                }
                else if (val instanceof Long) {
                    if (((Long)val).longValue() == 0)
                        return left;
                }
                else if (val instanceof Number) {
                    if (((Number)val).doubleValue() == 0.0)
                        return left;
                }
            }
            return this;
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a {@code Divide}
         * operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Divide && super.equals(o);
        }

    }

    /**
     * An implementation class for the modulo operation.
     */
    public static class Modulo extends Arithmetic {

        public static final String name = "%";
        public static final int priority = 6;

        /**
         * Construct a modulo operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public Modulo(Expression left, Expression right) {
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
            return o instanceof Modulo && super.equals(o);
        }

    }

    /**
     * An implementation class for the plus operation.
     */
    public static class Plus extends Arithmetic {

        public static final String name = "+";
        public static final int priority = 5;

        /**
         * Construct a plus operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public Plus(Expression left, Expression right) {
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
         * Return {@code true} - the operation is commutative.
         *
         * @return {@code true}
         */
        @Override
        public boolean isCommutative() {
            return true;
        }

        /**
         * Return {@code true} - the operation is associative.
         *
         * @return {@code true}
         */
        @Override
        public boolean isAssociative() {
            return true;
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
            return left + right;
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
            return left + right;
        }

        /**
         * Optimize the subexpression.  In addition to the default optimization, check for
         * addition of 0.
         *
         * @return  the constant result or the operator itself
         */
        @Override
        public Expression optimize() {
            if (optimizeOperands()) {
                try {
                    return new Constant(evaluate());
                }
                catch (EvaluationException ee) {
                    // ignore
                }
            }
            Expression left = getLeft();
            Expression right = getRight();
            if (right.isConstant()) {
                Object val = ((Constant)right).getValue();
                if (val instanceof Integer) {
                    if (((Integer)val).intValue() == 0)
                        return left;
                }
                else if (val instanceof Long) {
                    if (((Long)val).longValue() == 0)
                        return left;
                }
                else if (val instanceof Number) {
                    if (((Number)val).doubleValue() == 0.0)
                        return left;
                }
            }
            if (left.isConstant()) {
                Object val = ((Constant)left).getValue();
                if (val instanceof Integer) {
                    if (((Integer)val).intValue() == 0)
                        return right;
                }
                else if (val instanceof Long) {
                    if (((Long)val).longValue() == 0)
                        return right;
                }
                else if (val instanceof Number) {
                    if (((Number)val).doubleValue() == 0.0)
                        return right;
                }
            }
            return this;
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a {@code Plus}
         * operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Plus && super.equals(o);
        }

    }

    /**
     * An implementation class for the minus operation.
     */
    public static class Minus extends Arithmetic {

        public static final String name = "-";
        public static final int priority = 5;

        /**
         * Construct a minus operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public Minus(Expression left, Expression right) {
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
            return left - right;
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
            return left - right;
        }

        /**
         * Optimize the subexpression.  In addition to the default optimization, check for
         * subtraction of or from 0.
         *
         * @return  the constant result or the operator itself
         */
        @Override
        public Expression optimize() {
            if (optimizeOperands()) {
                try {
                    return new Constant(evaluate());
                }
                catch (EvaluationException ee) {
                    // ignore
                }
            }
            Expression left = getLeft();
            Expression right = getRight();
            if (right.isConstant()) {
                Object val = ((Constant)right).getValue();
                if (val instanceof Integer) {
                    if (((Integer)val).intValue() == 0)
                        return left;
                }
                else if (val instanceof Long) {
                    if (((Long)val).longValue() == 0)
                        return left;
                }
                else if (val instanceof Number) {
                    if (((Number)val).doubleValue() == 0.0)
                        return left;
                }
            }
            if (left.isConstant()) {
                Object val = ((Constant)left).getValue();
                if (val instanceof Integer) {
                    if (((Integer)val).intValue() == 0)
                        return new Negate(right);
                }
                else if (val instanceof Long) {
                    if (((Long)val).longValue() == 0)
                        return new Negate(right);
                }
                else if (val instanceof Number) {
                    if (((Number)val).doubleValue() == 0.0)
                        return new Negate(right);
                }
            }
            return this;
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a {@code Minus}
         * operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Minus && super.equals(o);
        }

    }

    /**
     * An interface to represent logical operations (including comparison and equality).
     */
    public static interface Logical {

        /**
         * Logically invert the operation.
         *
         * @return   the logical inversion of the operation
         */
        Expression invert();

    }

    /**
     * A class to represent the logical AND operation.
     */
    public static class And extends Diadic implements Logical {

        public static final String name = "&&";
        public static final int priority = 2;

        /**
         * Construct an AND operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public And(Expression left, Expression right) {
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
         * Get the result type of this operator.  The result type of an AND operation is always
         * {@link Boolean}.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Boolean.class;
        }

        /**
         * Evaluate the subexpression.  This method evaluates the left operand, and only if it
         * is {@code true} evaluates the right operand.
         *
         * @return  the boolean object resulting from the AND operation
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            if (!getLeft().asBoolean())
                return Boolean.FALSE;
            return getRight().asBoolean() ? Boolean.TRUE : Boolean.FALSE;
        }

        /**
         * Optimize the subexpression.  Optimize the operands, then, if either operand is
         * constant, return {@code false} if it is {@code false}, otherwise the other operand.
         *
         * @return  the constant result or the operator itself
         */
        @Override
        public Expression optimize() {
            optimizeOperands();
            Expression left = getLeft();
            Expression right = getRight();
            if (left.isConstant()) {
                try {
                    return left.asBoolean() ? right : falseConstant;
                }
                catch (EvaluationException ee) {
                }
            }
            if (right.isConstant()) {
                try {
                    return right.asBoolean() ? left : falseConstant;
                }
                catch (EvaluationException ee) {
                }
            }
            return this;
        }

        /**
         * Logically invert the operation.
         *
         * @return   the logical inversion of the operation
         */
        @Override
        public Expression invert() {
            Expression notLeft = new Not(getLeft());
            Expression notRight = new Not(getRight());
            return new Or(notLeft.optimize(), notRight.optimize());
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is an {@code And}
         * operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof And && super.equals(o);
        }

    }

    /**
     * A class to represent the logical OR operation.
     */
    public static class Or extends Diadic implements Logical {

        public static final String name = "||";
        public static final int priority = 1;

        /**
         * Construct an OR operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public Or(Expression left, Expression right) {
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
         * Get the result type of this operator.  The result type of an OR operation is always
         * {@link Boolean}.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Boolean.class;
        }

        /**
         * Evaluate the subexpression.  This method evaluates the left operand, and only if it
         * is {@code false} evaluates the right operand.
         *
         * @return  the boolean object resulting from the OR operation
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            if (getLeft().asBoolean())
                return Boolean.TRUE;
            return getRight().asBoolean() ? Boolean.TRUE : Boolean.FALSE;
        }

        /**
         * Optimize the subexpression.  Optimize the operands, then, if either operand is
         * constant, return {@code true} if it is {@code true}, otherwise the other operand.
         *
         * @return  the constant result or the operator itself
         */
        @Override
        public Expression optimize() {
            optimizeOperands();
            Expression left = getLeft();
            Expression right = getRight();
            if (left.isConstant()) {
                try {
                    return left.asBoolean() ? trueConstant : right;
                }
                catch (EvaluationException ee) {
                }
            }
            if (right.isConstant()) {
                try {
                    return right.asBoolean() ? trueConstant : left;
                }
                catch (EvaluationException ee) {
                }
            }
            return this;
        }

        /**
         * Logically invert the operation.
         *
         * @return   the logical inversion of the operation
         */
        @Override
        public Expression invert() {
            Expression notLeft = new Not(getLeft());
            Expression notRight = new Not(getRight());
            return new And(notLeft.optimize(), notRight.optimize());
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is an {@code Or}
         * operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Or && super.equals(o);
        }

    }

    /**
     * A base class for relative comparison operations.
     */
    public static abstract class Relative extends Diadic {

        public static final int priority = 4;

        /**
         * Construct a relative comparison operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public Relative(Expression left, Expression right) {
            super(left, right);
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
         * Get the result type of this operator.  The result type of a comparison operation is
         * always {@link Boolean}.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Boolean.class;
        }

        /**
         * Evaluate the subexpression.  This method delegates the bulk of the work to the
         * {@code compare()} method.
         *
         * @return  the boolean object resulting from the comparison operation
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            return compare() ? Boolean.TRUE : Boolean.FALSE;
        }

        /**
         * Compare the operands.  This method performs the comparison and calls the appropriate
         * method on the implementing class to determine whether the comparison result is
         * {@code true} or {@code false} in this case.
         *
         * @return  the boolean result of the comparison
         * @throws  EvaluationException on any errors
         */
        public boolean compare() throws EvaluationException {
            Object left = getLeft().evaluate();
            Object right = getRight().evaluate();
            try {
                if (left == right)
                    return equal();
                if (left == null)
                    return false;
                if (left.equals(right))
                    return equal();
                if (right == null)
                    return false;
                if (floatOperand(left) || floatOperand(right)) {
                    double leftDouble = asDouble(left);
                    double rightDouble = asDouble(right);
                    if (leftDouble == rightDouble)
                        return equal();
                    if (leftDouble < rightDouble)
                        return less();
                    return greater();
                }
                if (longOperand(left) || longOperand(right)) {
                    long leftLong = asLong(left);
                    long rightLong = asLong(right);
                    if (leftLong == rightLong)
                        return equal();
                    if (leftLong < rightLong)
                        return less();
                    return greater();
                }
                if (left instanceof String || right instanceof String) {
                    int comparison = asString(left).compareTo(asString(right));
                    if (comparison == 0)
                        return equal();
                    if (comparison < 0)
                        return less();
                    return greater();
                }
                if (left instanceof Comparable) {
                    @SuppressWarnings("unchecked")
                    int comparison = ((Comparable<Object>)left).compareTo(right);
                    if (comparison == 0)
                        return equal();
                    if (comparison < 0)
                        return less();
                    return greater();
                }
                if (right instanceof Comparable) {
                    @SuppressWarnings("unchecked")
                    int comparison = ((Comparable<Object>)right).compareTo(left);
                    if (comparison == 0)
                        return equal();
                    if (comparison > 0)
                        return less();
                    return greater();
                }
            }
            catch (EvaluationException ee) {
                throw ee;
            }
            catch (Exception e) {
            }
            throw new EvaluationException("compare");
        }

        /**
         * The result if the left operand is less than the right.
         *
         * @return  the boolean result of the comparison
         */
        public abstract boolean less();

        /**
         * The result if the operands are equal.
         *
         * @return  the boolean result of the comparison
         */
        public abstract boolean equal();

        /**
         * The result if the left operand is greater than the right.
         *
         * @return  the boolean result of the comparison
         */
        public abstract boolean greater();

        /**
         * Optimize the subexpression.  Optimize the operands, and if both are constant, execute
         * the operation.
         *
         * @return  the constant result or the operator itself
         */
        @Override
        public Expression optimize() {
            if (optimizeOperands()) {
                try {
                    return compare() ? trueConstant : falseConstant;
                }
                catch (EvaluationException ee) {
                    // ignore
                }
            }
            return this;
        }

    }

    /**
     * An implementation class for the 'greater than' operator.
     */
    public static class GreaterThan extends Relative implements Logical {

        public static final String name = ">";

        /**
         * Construct a 'greater than' operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public GreaterThan(Expression left, Expression right) {
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
         * The result if the left operand is less than the right.
         *
         * @return  {@code false}
         */
        @Override
        public boolean less() {
            return false;
        }

        /**
         * The result if the operands are equal.
         *
         * @return  {@code false}
         */
        @Override
        public boolean equal() {
            return false;
        }

        /**
         * The result if the left operand is greater than the right.
         *
         * @return  {@code true}
         */
        @Override
        public boolean greater() {
            return true;
        }

        /**
         * Logically invert the operation.
         *
         * @return   the logical inversion of the operation
         */
        @Override
        public Expression invert() {
            return new LessEqual(getLeft(), getRight());
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a
         * {@code GreaterThan} operation with equal operands, or a {@link LessThan} with equal
         * operands reversed.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof GreaterThan && super.equals(o))
                return true;
            if (!(o instanceof LessThan))
                return false;
            LessThan op = (LessThan)o;
            return getLeft().equals(op.getRight()) && getRight().equals(op.getLeft());
        }

    }

    /**
     * An implementation class for the 'less than' operator.
     */
    public static class LessThan extends Relative implements Logical {

        public static final String name = "<";

        /**
         * Construct a 'less than' operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public LessThan(Expression left, Expression right) {
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
         * The result if the left operand is less than the right.
         *
         * @return  {@code true}
         */
        @Override
        public boolean less() {
            return true;
        }

        /**
         * The result if the operands are equal.
         *
         * @return  {@code false}
         */
        @Override
        public boolean equal() {
            return false;
        }

        /**
         * The result if the left operand is greater than the right.
         *
         * @return  {@code false}
         */
        @Override
        public boolean greater() {
            return false;
        }

        /**
         * Logically invert the operation.
         *
         * @return   the logical inversion of the operation
         */
        @Override
        public Expression invert() {
            return new GreaterEqual(getLeft(), getRight());
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a
         * {@code LessThan} operation with equal operands, or a {@link GreaterThan} with equal
         * operands reversed.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof LessThan && super.equals(o))
                return true;
            if (!(o instanceof GreaterThan))
                return false;
            GreaterThan op = (GreaterThan)o;
            return getLeft().equals(op.getRight()) && getRight().equals(op.getLeft());
        }

    }

    /**
     * An implementation class for the 'greater than or equal' operator.
     */
    public static class GreaterEqual extends Relative implements Logical {

        public static final String name = ">=";

        /**
         * Construct a 'greater than or equal' operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public GreaterEqual(Expression left, Expression right) {
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
         * The result if the left operand is less than the right.
         *
         * @return  {@code false}
         */
        @Override
        public boolean less() {
            return false;
        }

        /**
         * The result if the operands are equal.
         *
         * @return  {@code true}
         */
        @Override
        public boolean equal() {
            return true;
        }

        /**
         * The result if the left operand is greater than the right.
         *
         * @return  {@code true}
         */
        @Override
        public boolean greater() {
            return true;
        }

        /**
         * Logically invert the operation.
         *
         * @return   the logical inversion of the operation
         */
        @Override
        public Expression invert() {
            return new LessThan(getLeft(), getRight());
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a
         * {@code GreaterEqual} operation with equal operands, or a {@link LessEqual} with equal
         * operands reversed.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof GreaterEqual && super.equals(o))
                return true;
            if (!(o instanceof LessEqual))
                return false;
            LessEqual op = (LessEqual)o;
            return getLeft().equals(op.getRight()) && getRight().equals(op.getLeft());
        }

    }

    /**
     * An implementation class for the 'less than or equal' operator.
     */
    public static class LessEqual extends Relative implements Logical {

        public static final String name = "<=";

        /**
         * Construct a 'less than or equal' operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public LessEqual(Expression left, Expression right) {
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
         * The result if the left operand is less than the right.
         *
         * @return  {@code true}
         */
        @Override
        public boolean less() {
            return true;
        }

        /**
         * The result if the operands are equal.
         *
         * @return  {@code true}
         */
        @Override
        public boolean equal() {
            return true;
        }

        /**
         * The result if the left operand is greater than the right.
         *
         * @return  {@code false}
         */
        @Override
        public boolean greater() {
            return false;
        }

        /**
         * Logically invert the operation.
         *
         * @return   the logical inversion of the operation
         */
        @Override
        public Expression invert() {
            return new GreaterThan(getLeft(), getRight());
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a
         * {@code LessEqual} operation with equal operands, or a {@link GreaterEqual} with equal
         * operands reversed.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof LessEqual && super.equals(o))
                return true;
            if (!(o instanceof GreaterEqual))
                return false;
            GreaterEqual op = (GreaterEqual)o;
            return getLeft().equals(op.getRight()) && getRight().equals(op.getLeft());
        }

    }

    /**
     * A base class for equality comparison operations.
     */
    public abstract static class Equality extends Diadic {

        public static final int priority = 3;

        /**
         * Construct a equality comparison operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public Equality(Expression left, Expression right) {
            super(left, right);
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
         * Return {@code true} - the operation is commutative.
         *
         * @return {@code true}
         */
        @Override
        public boolean isCommutative() {
            return true;
        }

        /**
         * Get the result type of this operator.  The result type of an equality operation is
         * always {@link Boolean}.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Boolean.class;
        }

        /**
         * Compare the two operands for equality.
         *
         * @return  {@code true} if the operands are equal
         * @throws  EvaluationException on any errors
         */
        public boolean compare() throws EvaluationException {
            Object left = getLeft().evaluate();
            Object right = getRight().evaluate();
            try {
                if (left == null) {
                    if (right == null)
                        return true;
                    return false;
                }
                if (left.equals(right))
                    return true;
                if (right == null)
                    return false;
                if (floatOperand(left) || floatOperand(right)) {
                    return asDouble(left) == asDouble(right);
                }
                if (longOperand(left) || longOperand(right)) {
                    return asLong(left) == asLong(right);
                }
                if (left instanceof String || right instanceof String) {
                    return asString(left).equals(asString(right));
                }
                return left.equals(right);
            }
            catch (EvaluationException ee) {
                throw ee;
            }
            catch (Exception e) {
            }
            throw new EvaluationException("compare");
        }

    }

    /**
     * An implementation class for the 'equal' operator.
     */
    public static class Equal extends Equality implements Logical {

        public static final String name = "==";

        /**
         * Construct an 'equal' operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public Equal(Expression left, Expression right) {
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
         * Evaluate the subexpression.  This method delegates the bulk of the work to the
         * {@code compare()} method.
         *
         * @return  the boolean object resulting from the comparison operation
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            return compare() ? Boolean.TRUE : Boolean.FALSE;
        }

        /**
         * Optimize the subexpression.  Optimize the operands, and if both are constant, execute
         * the operation.
         *
         * @return  the constant result or the operator itself
         */
        @Override
        public Expression optimize() {
            if (optimizeOperands()) {
                try {
                    return compare() ? trueConstant : falseConstant;
                }
                catch (EvaluationException ee) {
                    // ignore
                }
            }
            return this;
        }

        /**
         * Logically invert the operation.
         *
         * @return   the logical inversion of the operation
         */
        @Override
        public Expression invert() {
            return new NotEqual(getLeft(), getRight());
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is an {@code Equal}
         * operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Equal && super.equals(o);
        }

    }

    /**
     * An implementation class for the 'not equal' operator.
     */
    public static class NotEqual extends Equality implements Logical {

        public static final String name = "!=";

        /**
         * Construct a 'not equal' operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public NotEqual(Expression left, Expression right) {
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
         * Evaluate the subexpression.  This method delegates the bulk of the work to the
         * {@code compare()} method.
         *
         * @return  the boolean object resulting from the comparison operation
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            return compare() ? Boolean.FALSE : Boolean.TRUE;
        }

        /**
         * Optimize the subexpression.  Optimize the operands, and if both are constant, execute
         * the operation.
         *
         * @return  the constant result or the operator itself
         */
        @Override
        public Expression optimize() {
            if (optimizeOperands()) {
                try {
                    return compare() ? falseConstant : trueConstant;
                }
                catch (EvaluationException ee) {
                    // ignore
                }
            }
            return this;
        }

        /**
         * Logically invert the operation.
         *
         * @return   the logical inversion of the operation
         */
        @Override
        public Expression invert() {
            return new Equal(getLeft(), getRight());
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a
         * {@code NotEqual} operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof NotEqual && super.equals(o);
        }

    }

    /**
     * A class to represent the "wildcard pattern match" operation.  This is an extension to the
     * JSTL 1.0 EL specification.
     */
    public static class Match extends Diadic {

        public static final String name = "~=";
        public static final int priority = 3;
        public static final char multi = '*';
        public static final char single = '?';
        public static final char escape = '\\';

        /**
         * Construct a 'match' operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public Match(Expression left, Expression right) {
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
         * Get the result type of this operator.  The result type of a match operation is always
         * {@link Boolean}.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Boolean.class;
        }

        /**
         * Evaluate the subexpression.  Match the left operand string against the right operand
         * pattern.
         *
         * @return the boolean result of the pattern match operation
         * @throws EvaluationException if the operands are not strings or null
         */
        @Override
        public Object evaluate() throws EvaluationException {
            Object left = getLeft().evaluate();
            Object right = getRight().evaluate();
            if (!(left == null || left instanceof String) ||
                    !(right == null || right instanceof String))
                throw new EvaluationException("match");
            if (left == null || right == null)
                return Boolean.FALSE;
            return match((String)left, 0, (String)right, 0) ? Boolean.TRUE :
                    Boolean.FALSE;
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a {@code Match}
         * operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Match && super.equals(o);
        }

        /**
         * Match a string against a pattern.
         *
         * @param str      the string
         * @param strIndex the offset within the string
         * @param pat      the pattern
         * @param patIndex the offset within the pattern
         * @return         {@code true} if the string matches the pattern
         */
        private static boolean match(String str, int strIndex, String pat,
                int patIndex) {
            int strLen = str.length();
            int patLen = pat.length();
            while (patIndex < patLen) {
                char patChar = pat.charAt(patIndex++);
                if (patChar == multi) {
                    if (patIndex >= patLen) // end of pattern
                        return true;
                    for (;;) {
                        if (match(str, strIndex, pat, patIndex))
                            return true;
                        if (strIndex >= strLen)
                            return false;
                        strIndex++;
                    }
                }
                if (patChar == single) {
                    if (strIndex >= strLen)
                        return false;
                    strIndex++;
                }
                else if (patChar == escape) {
                    if (patIndex >= patLen)
                        return false; // error - escape at end of pattern
                    if (strIndex >= strLen ||
                            str.charAt(strIndex++) != pat.charAt(patIndex++))
                        return false;
                }
                else {
                    if (strIndex >= strLen || str.charAt(strIndex++) != patChar)
                        return false;
                }
            }
            return strIndex == strLen;
        }

    }

    /**
     * A class to represent the string concatenation (join) operation.  This is an extension to
     * the JSTL 1.0 EL specification.
     */
    public static class Join extends Diadic {

        public static final String name = "#";
        public static final int priority = 5;

        /**
         * Construct a 'join' operation with the given operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public Join(Expression left, Expression right) {
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
         * Get the result type of this operator.  The result type of a join operation is always
         * {@link String}.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return String.class;
        }

        /**
         * Optimize the subexpression.  In addition to the default optimization, check for join
         * with null or zero-length string.
         *
         * @return  the constant result or the operator itself
         */
        @Override
        public Expression optimize() {
            if (optimizeOperands()) {
                try {
                    return new Constant(evaluate());
                }
                catch (EvaluationException ee) {
                    // ignore
                }
            }
            Expression left = getLeft();
            Expression right = getRight();
            if (right.isConstant()) {
                Object val = ((Constant)right).getValue();
                if (val == null ||
                        val instanceof String && ((String)val).length() == 0)
                    return left;
            }
            if (left.isConstant()) {
                Object val = ((Constant)left).getValue();
                if (val == null ||
                        val instanceof String && ((String)val).length() == 0)
                    return right;
            }
            return this;
        }

        /**
         * Evaluate the subexpression.  Concatenate the left and right operands (as strings).
         *
         * @return the string result of the join operation
         * @throws EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            Object left = getLeft().evaluate();
            Object right = getRight().evaluate();
            if (left == null)
                return right == null ? nullString : asString(right);
            return right == null ? asString(left) :
                    asString(left) + asString(right);
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a {@code Join}
         * operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Join && super.equals(o);
        }

    }

    /**
     * A dummy operator to represent parentheses in an expression.  By making the contents of
     * the parentheses the right operand of the expression, the operator precedence search is
     * prevented from scanning the sub-expression.
     */
    public static class Parentheses extends Operator {

        public static final String name = "()";
        public static final int priority = 8;

        /**
         * Construct a dummy parentheses operator with the given operand.
         *
         * @param operand  the right operand
         */
        public Parentheses(Expression operand) {
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
         * Get the result type of this operator.  The result type of this operator is the result
         * type of its right operand.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return getRight().getType();
        }

        /**
         * Evaluate the subexpression.  This just returns the evaluation of the operand.
         *
         * @return  the result of the evaluation of the operand
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            return getRight().evaluate();
        }

        /**
         * Optimize the subexpression.  This method calls the parent class to optimize the
         * operand, then returns the operand, because the dummy operator no longer has any
         * value.
         *
         * @return  the operand
         */
        @Override
        public Expression optimize() {
            optimizeRightOperand();
            return getRight();
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a
         * {@code Parentheses} with an equal operand.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Parentheses && super.equals(o);
        }

        /**
         * Convert subexpression to string.
         *
         * @return  a string representation of the subexpression
         */
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder();
            sb.append('(');
            sb.append(getRight());
            sb.append(')');
            return sb.toString();
        }

    }

    /**
     * A class to represent the negate operation.
     */
    public static class Negate extends Operator {

        public static final String name = "-";
        public static final int priority = 7;

        /**
         * Construct a negate operator with the given operand.
         *
         * @param operand  the right operand
         */
        public Negate(Expression operand) {
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
                return new Integer(0);
            try {
                if (operand instanceof String) {
                    if (floatString(operand))
                        return new Double(-asDouble(operand));
                    return new Long(-asLong(operand));
                }
                if (operand instanceof Byte)
                    return new Byte((byte)(-((Byte)operand).byteValue()));
                if (operand instanceof Short)
                    return new Short((short)(-((Short)operand).shortValue()));
                if (operand instanceof Integer)
                    return new Integer(-((Integer)operand).intValue());
                if (operand instanceof Long)
                    return new Long(-((Long)operand).longValue());
                if (operand instanceof Float)
                    return new Float(-((Float)operand).floatValue());
                if (operand instanceof Double)
                    return new Double(-((Double)operand).doubleValue());
            }
            catch (EvaluationException ee) {
                throw ee;
            }
            catch (Exception e) {
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
            return o instanceof Negate && super.equals(o);
        }

    }

    /**
     * A class to represent the logical NOT operation.
     */
    public static class Not extends Operator implements Logical {

        public static final String name = "!";
        public static final int priority = 7;

        /**
         * Construct a NOT operator with the given operand.
         *
         * @param operand  the right operand
         */
        public Not(Expression operand) {
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
         * Get the result type of this operator.  The result type of a logical operation is
         * always {@link Boolean}.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Boolean.class;
        }

        /**
         * Evaluate the subexpression.  Evaluate the operand and return the boolean object
         * corresponding to its logical inversion.
         *
         * @return  the object resulting from the NOT operation
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            return getRight().asBoolean() ? Boolean.FALSE : Boolean.TRUE;
        }

        /**
         * Optimize the subexpression.  This method calls the parent class to optimize the
         * operand, and attempts to logically invert comparison, equality and logical
         * operations.  Then, if the operand is constant, it attempts to apply the operation.
         * If this fails the optimization is dropped.
         *
         * @return  the inverted operation, the constant result, or the operator itself
         */
        @Override
        public Expression optimize() {
            optimizeRightOperand();
            Expression right = getRight();
            if (right instanceof Logical)
                return ((Logical)right).invert();
            if (right.isConstant()) {
                try {
                    return right.asBoolean() ? falseConstant : trueConstant;
                }
                catch (EvaluationException ee) {
                    // ignore
                }
            }
            return this;
        }

        /**
         * Logically invert the operation.
         *
         * @return   the logical inversion of the operation
         */
        @Override
        public Expression invert() {
            return getRight();
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a {@code Not}
         * operation with an equal operand.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Not && super.equals(o);
        }

    }

    /**
     * A class to represent the 'test for empty' operation.
     */
    public static class Empty extends Operator {

        public static final String name = "empty";
        public static final int priority = 7;

        /**
         * Construct a 'test for empty' operator with the given operand.
         *
         * @param operand  the right operand
         */
        public Empty(Expression operand) {
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
         * Get the result type of this operator.  The result type of a test for empty operation
         * is always {@link Boolean}.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Boolean.class;
        }

        /**
         * Evaluate the subexpression.  Evaluate the operand and apply the rules for testing for
         * empty.
         *
         * @return  the object resulting from the 'test for empty' operation
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            Object operand = getRight().evaluate();
            if (operand == null)
                return Boolean.TRUE;
            if (operand instanceof String && ((String)operand).length() == 0)
                return Boolean.TRUE;
            if (operand instanceof Object[] && ((Object[])operand).length == 0)
                return Boolean.TRUE;
            if (operand.getClass().isArray() && Array.getLength(operand) == 0)
                return Boolean.TRUE;
            if (operand instanceof Map && ((Map<?, ?>)operand).isEmpty())
                return Boolean.TRUE;
            if (operand instanceof List && ((List<?>)operand).isEmpty())
                return Boolean.TRUE;
            return Boolean.FALSE;
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is an {@code Empty}
         * operation with an equal operand.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Empty && super.equals(o);
        }

    }

    /**
     * A class to represent the 'convert to upper case' operation.  This is an extension to the
     * JSTL 1.0 EL specification.
     */
    public static class ToUpper extends Operator {

        public static final String name = "toupper";
        public static final int priority = 7;

        /**
         * Construct a 'convert to upper case' operator with the given operand.
         *
         * @param operand  the right operand
         */
        public ToUpper(Expression operand) {
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
         * Get the result type of this operator.  The result type of a case conversion operation
         * is always {@link String}.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return String.class;
        }

        /**
         * Evaluate the subexpression.  Evaluate the operand as a {@link String} and convert it
         * to upper case.
         *
         * @return  the converted string
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            return getRight().asString().toUpperCase();
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a {@code ToUpper}
         * operation with an equal operand.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof ToUpper && super.equals(o);
        }

    }

    /**
     * A class to represent the 'convert to lower case' operation.  This is an extension to the
     * JSTL 1.0 EL specification.
     */
    public static class ToLower extends Operator {

        public static final String name = "tolower";
        public static final int priority = 7;

        /**
         * Construct a 'convert to lower case' operator with the given operand.
         *
         * @param operand  the right operand
         */
        public ToLower(Expression operand) {
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
         * Get the result type of this operator.  The result type of a case conversion operation
         * is always {@link String}.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return String.class;
        }

        /**
         * Evaluate the subexpression.  Evaluate the operand as a {@link String} and convert it
         * to lower case.
         *
         * @return  the converted string
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            return getRight().asString().toLowerCase();
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a {@code ToUpper}
         * operation with an equal operand.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof ToLower && super.equals(o);
        }

    }

    /**
     * A class to represent the {@code length} operation.  This is an extension to the JSTL 1.0
     * EL specification.
     */
    public static class Length extends Operator {

        public static final String name = "length";
        public static final int priority = 7;

        /**
         * Construct a {@code length} operator with the given operand.
         *
         * @param operand  the right operand
         */
        public Length(Expression operand) {
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
         * Get the result type of this operator.  The result type of a length operation is
         * always {@link Integer}.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Integer.class;
        }

        /**
         * Optimize the subexpression.  The operand is optimized, but no attempt is made to
         * optimize this operation.
         *
         * @return  the operator itself
         */
        @Override
        public Expression optimize() {
            optimizeRightOperand();
            return this;
        }

        /**
         * Evaluate the subexpression.  Evaluate the operand and, if it is a {@link Map},
         * {@link List} or array, return the number of elements.
         *
         * @return  the length of the array
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            Object right = getRight().evaluate();
            if (right instanceof Map)
                return new Integer(((Map<?, ?>)right).size());
            if (right instanceof List)
                return new Integer(((List<?>)right).size());
            if (right instanceof Object[])
                return new Integer(((Object[])right).length);
            if (right.getClass().isArray())
                return new Integer(Array.getLength(right));
            throw new LengthException();
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a {@code Length}
         * operation with an equal operand.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Length && super.equals(o);
        }

    }

    /**
     * A class to represent the <code>sum</code> operation.  This is an extension to the JSTL
     * 1.0 EL specification.
     */
    public static class Sum extends Operator {

        public static final String name = "sum";
        public static final int priority = 7;

        /**
         * Construct a {@code sum} operator with the given operand.
         *
         * @param operand  the right operand
         */
        public Sum(Expression operand) {
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
         * Get the result type of this operator.  The result type of a sum operation is
         * {@link Number}.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Number.class;
        }

        /**
         * Optimize the subexpression.  The operand is optimized, but no attempt is made to
         * optimize this operation.
         *
         * @return  the operator itself
         */
        @Override
        public Expression optimize() {
            optimizeRightOperand();
            return this;
        }

        /**
         * Evaluate the subexpression.  Evaluate the operand and, if it is a {@link Map},
         * {@link List} or array, return the sum of the elements.
         *
         * @return  the length of the array
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            Object right = getRight().evaluate();
            try {
                if (right instanceof Map)
                    return calculateSum(((Map<?, ?>)right).values().iterator());
                if (right instanceof List)
                    return calculateSum(((List<?>)right).iterator());
                if (right.getClass().isArray())
                    return calculateSum(new ArrayIterator(right));
            }
            catch (RuntimeException e) {
            }
            throw new SumException();
        }

        /**
         * Calculate the sum.  The sum is returned as a {@code Long} if all the values are
         * integral, or a {@code Double} otherwise.
         *
         * @param i an {@link Iterator} over the array or {@link List} or {@link Map}
         * @return a {@code Long} or {@code Double} containing the sum
         * @throws ClassCastException if an element is not a {@link Number} or {@link String}
         * @throws NumberFormatException if a {@link String} element can not be converted to a
         *         number
         */
        private Object calculateSum(Iterator<?> i) {
            long longTotal = 0L;
            while (i.hasNext()) {
                Object value = i.next();
                if (value != null) {
                    if (value instanceof Long || value instanceof Integer ||
                            value instanceof Short || value instanceof Byte)
                        longTotal += ((Number)value).longValue();
                    else if (value instanceof String && !floatString(value))
                        longTotal += Long.parseLong((String)value);
                    else {
                        double doubleTotal = longTotal;
                        for (;;) {
                            if (value != null)
                                doubleTotal += value instanceof String ?
                                        Double.parseDouble((String)value) :
                                        ((Number)value).doubleValue();
                            if (!i.hasNext())
                                break;
                            value = i.next();
                        }
                        return new Double(doubleTotal);
                    }
                }
            }
            return new Long(longTotal);
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a {@code Sum}
         * operation with an equal operand.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Sum && super.equals(o);
        }

    }

    /**
     * A simple array iterator class to assist with array operations.
     */
    public static class ArrayIterator implements Iterator<Object> {

        private Object array;
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

    /**
     * A class to represent the concatenation pseudo-expression created by the
     * {@code parseSubstitution()} method.
     */
    public static class Concat extends Expression {

        private static final int increment = 5;

        private Expression[] array;
        private int num = 0;

        /**
         * Create an empty concatenation.
         */
        public Concat() {
            array = new Expression[increment];
            num = 0;
        }

        /**
         * Add an element to the concatenation.
         *
         * @param expr  the element to be added
         */
        public void addExpression(Expression expr) {
            if (num == array.length) {
                Expression[] newArray = new Expression[num + increment];
                System.arraycopy(array, 0, newArray, 0, num);
                array = newArray;
            }
            array[num++] = expr;
        }

        /**
         * Get the result type of this operator.  The result type of a concatenation operation
         * is always {@link String}.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return String.class;
        }

        /**
         * Evaluate the subexpression.  Evaluate each element and add it to an output string.
         *
         * @return  the string concatenation of all the elements
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < num; ++i)
                sb.append(array[i].asString());
            return sb.toString();
        }

        /**
         * Optimize the subexpression.  Each element is individually optimized, constant empty
         * strings are dropped and adjacent constant strings are combined.
         *
         * @return  the optimized concatenation operation
         */
        @Override
        public Expression optimize() {
            int j = 0;
            for (int i = 0; i < num; ++i) {
                Expression expr = array[i].optimize();
                if (expr instanceof Constant) {
                    Object val = ((Constant)expr).evaluate();
                    if (val != null && val instanceof String) {
                        String str = (String)val;
                        if (str.length() == 0)
                            continue;
                        if (j > 0) {
                            Expression expr2 = array[j - 1];
                            if (expr2 instanceof Constant) {
                                Object val2 = ((Constant)expr2).evaluate();
                                if (val2 != null && val2 instanceof String) {
                                    array[j - 1] =
                                            new Constant(((String)val2) + str);
                                    continue;
                                }
                            }
                        }
                    }
                }
                array[j++] = expr;
            }
            if (j == 0)
                return nullStringConstant;
            if (j == 1) {
                Expression expr = array[0];
                if (expr instanceof Constant) {
                    Object val = ((Constant)expr).evaluate();
                    if (val != null && val instanceof String)
                        return expr;
                    try {
                        return new Constant(expr.asString());
                    }
                    catch (EvaluationException ee) {
                    }
                }
            }
            num = j;
            while (j < array.length)
                array[j++] = null;
            return this;
        }

        /**
         * Get the result of the concatenation as a single expression.  If the concatenation
         * resulted from the parse of a string consisting of a single expression, return that
         * expression; otherwise return the concatenation expression.
         *
         * @return the result of the concatenation as a single expression
         */
        public Expression singleExpression() {
            if (num == 1)
                return array[0];
            return this;
        }

        /**
         * Test for equality.  Return {@code true} only if the other object is a {@code Concat}
         * operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   {@code true} if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            if (!(o instanceof Concat))
                return false;
            Concat op = (Concat)o;
            if (num != op.num)
                return false;
            for (int i = 0; i < num; ++i)
                if (!array[i].equals(op.array[i]))
                    return false;
            return true;
        }

        /**
         * Ensure that objects which compare as equal return the same hash code.
         *
         * @return the hash code
         * @see Object#hashCode()
         */
        @Override
        public int hashCode() {
            int result = 0;
            for (int i = 0; i < num; ++i)
                result ^= array[i].hashCode();
            return result;
        }

    }

    /**
     * A class to represent the function call operation.
     */
    public static class FunctionCall extends Expression {

        private static Map<String, Object> classMap = new HashMap<>();

        private String classname;
        private String functionName;
        private List<Expression> arguments;

        /**
         * Create a function call operation.
         *
         * @param   classname       the classname of the class to instantiate
         * @param   functionName    the name of the method to execute
         */
        public FunctionCall(String classname, String functionName) {
            this.classname = classname;
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
            Object functionObject = classMap.get(classname);
            if (functionObject == null) {
                try {
                    Class<?> functionObjectClass = Class.forName(classname);
                    functionObject = functionObjectClass.newInstance();
                    classMap.put(classname, functionObject);
                }
                catch (ClassNotFoundException e) { // handle these differently?
                    throw new FunctionEvaluationException();
                }
                catch (InstantiationException e) {
                    throw new FunctionEvaluationException();
                }
                catch (IllegalAccessException e) {
                    throw new FunctionEvaluationException();
                }
            }
            int n = arguments.size();
            for (Method method : functionObject.getClass().getMethods()) {
                if (method.getName().equals(functionName) &&
                        method.getParameterTypes().length == n) {
                    Object[] array = new Object[n];
                    for (int i = 0; i < n; i++)
                        array[i] = arguments.get(i).evaluate();
                    try {
                        return method.invoke(null, array);
                    }
                    catch (IllegalAccessException e) {
                        throw new FunctionEvaluationException();
                    }
                    catch (IllegalArgumentException e) {
                        throw new FunctionEvaluationException();
                    }
                    catch (InvocationTargetException e) {
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
            if (!(o instanceof FunctionCall))
                return false;
            FunctionCall fc = (FunctionCall)o;
            if (!classname.equals(fc.classname) || !functionName.equals(fc.functionName))
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
            int result = classname.hashCode() ^ functionName.hashCode();
            for (int i = 0, n = arguments.size(); i < n; ++i)
                result ^= arguments.get(i).hashCode();
            return result;
        }

    }

    /**
     * A class to represent all exceptions in the expression system, both parsing and evaluation
     * exceptions.
     */
    public static class ExpressionException extends Exception {

        private static final long serialVersionUID = -3641819526563992578L;

        /**
         * Construct an {@code ExpressionException} with the given message.
         *
         * @param message  the message
         */
        public ExpressionException(String message) {
            super(message);
        }

    }

    /**
     * A class to represent parse exceptions.
     */
    public static class ParseException extends ExpressionException {

        private static final long serialVersionUID = -3286750214318116469L;

        /**
         * Construct a {@code ParseException} with the given message.
         *
         * @param message  the message
         */
        public ParseException(String message) {
            super(message);
        }

    }

    /**
     * An exception class for unmatched parentheses.
     */
    public static class UnmatchedParenthesisException extends ParseException {

        private static final long serialVersionUID = 1661428115028450303L;

        /**
         * Construct an {@code UnmatchedParenthesisException}.
         */
        public UnmatchedParenthesisException() {
            super("unmatched.parenthesis");
        }

    }

    /**
     * An exception class for unmatched brackets.
     */
    public static class UnmatchedBracketException extends ParseException {

        private static final long serialVersionUID = -8072205275825609727L;

        /**
         * Construct an {@code UnmatchedBracketException}.
         */
        public UnmatchedBracketException() {
            super("unmatched.bracket");
        }

    }

    /**
     * An exception class for unmatched braces.
     */
    public static class UnmatchedBraceException extends ParseException {

        private static final long serialVersionUID = -2011622722123078308L;

        /**
         * Construct an {@code UnmatchedBraceException}.
         */
        public UnmatchedBraceException() {
            super("unmatched.brace");
        }

    }

    /**
     * An exception class for undefined identifiers.
     */
    public static class IdentifierException extends ParseException {

        private static final long serialVersionUID = -2050423148965843481L;

        private String identifier;

        /**
         * Construct an {@code IdentifierException}.
         *
         * @param identifier  the identifier that was not recognized
         */
        public IdentifierException(String identifier) {
            super("identifier:" + identifier);
            this.identifier = identifier;
        }

        /**
         * Get the identifer.
         *
         * @return the identifier
         */
        public String getIdentifier() {
            return identifier;
        }

    }

    /**
     * An exception class for the use of reserved words.
     */
    public static class ReservedWordException extends ParseException {

        private static final long serialVersionUID = -1281977658799986894L;

        /**
         * Construct a {@code ReservedWordException}.
         */
        public ReservedWordException() {
            super("reserved");
        }

    }

    /**
     * An exception class for the illegal use of the property ({@code object.property}) syntax.
     */
    public static class PropertyException extends ParseException {

        private static final long serialVersionUID = -134506457895527215L;

        /**
         * Construct a {@code PropertyException}.
         */
        public PropertyException() {
            super("property");
        }

    }

    /**
     * An exception class for illegal numbers.
     */
    public static class NumberException extends ParseException {

        private static final long serialVersionUID = -3541897520713384932L;

        /**
         * Construct a {@code NumberException}.
         */
        public NumberException() {
            super("number");
        }

    }

    /**
     * An exception class for illegal strings.
     */
    public static class StringException extends ParseException {

        private static final long serialVersionUID = -663228289734382357L;

        /**
         * Construct a {@code StringException}.
         */
        public StringException() {
            super("string");
        }

    }

    /**
     * An exception class to indicate unparsed text at the end of an expression.
     */
    public static class UnparsedStringException extends ParseException {

        private static final long serialVersionUID = 1312390447752373903L;

        /**
         * Construct an {@code UnparsedStringException}.
         */
        public UnparsedStringException() {
            super("unparsed");
        }

    }

    /**
     * An exception class for general syntax errors.
     */
    public static class UnexpectedElementException extends ParseException {

        private static final long serialVersionUID = -3186327291206035634L;

        /**
         * Construct an {@code UnexpectedElementException}.
         */
        public UnexpectedElementException() {
            super("unexpected");
        }

    }

    /**
     * An exception class for unexpected end of string.
     */
    public static class UnexpectedEndException extends ParseException {

        private static final long serialVersionUID = 515741523591116116L;

        /**
         * Construct an {@code UnexpectedEndException}.
         */
        public UnexpectedEndException() {
            super("end");
        }

    }

    /**
     * An exception class for conditional expression errors.  These occur when a conditional
     * expression does not have a matching else expression (the use of conditional expressions
     * is an extension to the JSTL 1.0 EL specification).
     */
    public static class ConditionalException extends ParseException {

        private static final long serialVersionUID = -2881382265664362226L;

        /**
         * Construct a {@code ConditionalException}.
         */
        public ConditionalException() {
            super("conditional");
        }

    }

    /**
     * An exception class for else expression errors.  These occur when an else expression
     * appears without a matching conditional expression (the use of conditional expressions is
     * an extension to the JSTL 1.0 EL specification).
     */
    public static class ElseException extends ParseException {

        private static final long serialVersionUID = 6388695354388009372L;

        /**
         * Construct an {@code ElseException}.
         */
        public ElseException() {
            super("else");
        }

    }

    /**
     * An exception class for function call errors.
     */
    public static class FunctionParseException extends ParseException {

        private static final long serialVersionUID = 3048458551293937043L;

        /**
         * Construct a {@code FunctionParseException}.
         */
        public FunctionParseException() {
            super("parse.function");
        }

    }

    /**
     * A class to represent evaluation exceptions.
     */
    public static class EvaluationException extends ExpressionException {

        private static final long serialVersionUID = 7286375754096923754L;

        /**
         * Construct an {@code EvaluationException} with the given message.
         *
         * @param message  the message
         */
        public EvaluationException(String message) {
            super(message);
        }

    }

    /**
     * An exception class for coercion errors.
     */
    public static class CoercionException extends EvaluationException {

        private static final long serialVersionUID = -1702285297144783306L;

        /**
         * Construct a {@code CoercionException} for the given type.
         *
         * @param type  the type of the failed coercion
         */
        public CoercionException(String type) {
            super("coercion." + type);
        }

    }

    /**
     * An exception class for boolean coercion errors.
     */
    public static class BooleanCoercionException extends CoercionException {

        private static final long serialVersionUID = 1441587294848534710L;

        /**
         * Construct a {@code BooleanCoercionException}.
         */
        public BooleanCoercionException() {
            super("boolean");
        }

    }

    /**
     * An exception class for int coercion errors.
     */
    public static class IntCoercionException extends CoercionException {

        private static final long serialVersionUID = -4442530503193693699L;

        /**
         * Construct an {@code IntCoercionException}.
         */
        public IntCoercionException() {
            super("int");
        }

    }

    /**
     * An exception class for long coercion errors.
     */
    public static class LongCoercionException extends CoercionException {

        private static final long serialVersionUID = -2943338656778121853L;

        /**
         * Construct a {@code LongCoercionException}.
         */
        public LongCoercionException() {
            super("long");
        }

    }

    /**
     * An exception class for double coercion errors.
     */
    public static class DoubleCoercionException extends CoercionException {

        private static final long serialVersionUID = 7189427432254746387L;

        /**
         * Construct a {@code DoubleCoercionException}.
         */
        public DoubleCoercionException() {
            super("double");
        }

    }

    /**
     * An exception class for string coercion errors.
     */
    public static class StringCoercionException extends CoercionException {

        private static final long serialVersionUID = 3794199809625613318L;

        /**
         * Construct a {@code StringCoercionException}.
         */
        public StringCoercionException() {
            super("string");
        }

    }

    /**
     * An exception class for indexing errors.
     */
    public static class IndexException extends EvaluationException {

        private static final long serialVersionUID = -689989317127661110L;

        /**
         * Construct an {@code IndexException}.
         */
        public IndexException() {
            super("index");
        }

    }

    /**
     * An exception class for assignment errors.
     */
    public static class AssignException extends EvaluationException {

        private static final long serialVersionUID = 4258277443617101984L;

        /**
         * Construct an {@code AssignException}.
         */
        public AssignException() {
            super("assign");
        }

    }

    /**
     * An exception class for length operation errors.
     */
    public static class LengthException extends EvaluationException {

        private static final long serialVersionUID = -6836549488697678977L;

        /**
         * Construct a {@code LengthException}.
         */
        public LengthException() {
            super("length");
        }

    }

    /**
     * An exception class for sum operation errors.
     */
    public static class SumException extends EvaluationException {

        private static final long serialVersionUID = 2385081765272547293L;

        /**
         * Construct a {@code SumException}.
         */
        public SumException() {
            super("sum");
        }

    }

    /**
     * An exception class for sum operation errors.
     */
    public static class FunctionEvaluationException extends EvaluationException {

        private static final long serialVersionUID = 3505224066470817874L;

        /**
         * Construct a {@code FunctionEvaluationException}.
         */
        public FunctionEvaluationException() {
            super("evaluate.function");
        }

    }

    /**
     * Inner interface representing an object which will resolve names for the
     * expression parser.
     */
    public static interface Resolver {

        /**
         * Resolve an identifier.  The result may be any type of {@code Expression}, but it will
         * usually be a {@code Variable} or a {@code Constant}.
         *
         * @param identifier  the identifier to be resolved
         * @return            an expression, or {@code null} if the name can not be resolved
         */
        Expression resolve(String identifier);

    }

    public static interface ExtendedResolver extends Resolver {

        /**
         * Resolve a namespace prefix.  This is used in the case of prefixed function calls.
         *
         * @param   prefix  the prefix
         * @return  the URI for the namespace, or {@code null} if the prefix can not be resolved
         */
        String resolvePrefix(String prefix);

        /**
         * Resolve a namespace URI to the classname of a class that implements the
         * functionality.
         *
         * @param   uri     the URI
         * @return  the classname of the implementing class, or {@code null} if the URI can not
         *          be resolved
         */
        String resolveNamespace(String uri);

    }

    /**
     * Subclass of {@link ParseText} to handle Expression Language name rules, and number and
     * string conventions.
     */
    public static class ELParseText extends ParseText {

        public ELParseText(CharSequence s) {
            super(s);
        }

        @Override
        public boolean isNameStart(char ch) {
            return isIdentifierStart(ch);
        }

        @Override
        public boolean isNameContinuation(char ch) {
            return isIdentifierPart(ch);
        }

        public boolean matchNumber() {
            int i = getIndex();
            int x = getTextLength();
            if (i >= x)
                return false;
            char ch = charAt(i);
            if (ch == '-' || ch == '+') {
                if (++i >= x)
                    return false;
                ch = charAt(i);
            }
            if (!(isDigit(ch) || ch == '.'))
                return false;
        ok:
            for (;;) {
                while (isDigit(ch)) {
                    if (++i >= x)
                        break ok;
                    ch = charAt(i);
                }
                if (ch == '.') {
                    if (++i >= x)
                        break ok;
                    ch = charAt(i);
                    while (isDigit(ch)) {
                        if (++i >= x)
                            break ok;
                        ch = charAt(i);
                    }
                }
                if (ch == 'E' || ch == 'e') {
                    if (++i >= x)
                        return false;
                    ch = charAt(i);
                    if (!isDigit(ch))
                        return false;
                    do {
                        if (++i >= x)
                            break ok;
                    } while (isDigit(charAt(i)));
                }
                // check for name character immediately following?
                break ok;
            }
            return matchSuccess(i);
        }

        public Number getResultNumber() {
            String str = getResultString();
            if (str.indexOf('.') >= 0 || str.indexOf('e') >= 0 || str.indexOf('E') >= 0)
                return new Double(str);
            long i = Long.parseLong(str);
            if (i <= Integer.MAX_VALUE && i >= Integer.MIN_VALUE)
                return new Integer((int)i);
            return new Long(i);
        }

        public boolean matchStringLiteral() {
            int i = getIndex();
            int x = getTextLength();
            if (i >= x)
                return false;
            char ch = charAt(i);
            if (ch != '"' && ch != '\'')
                return false;
            char delimiter = ch;
            for (;;) {
                if (++i >= x)
                    return false;
                ch = charAt(i);
                if (ch == delimiter)
                    break;
                if (ch == '\\') {
                    if (++i >= x)
                        return false;
                    // check character following backslash?
                }
            }
            return matchSuccess(i + 1);
        }

        public String getResultStringLiteral() {
            int i = getStart();
            int x = getIndex();
            if (x - i < 2)
                throw new IllegalStateException();
            char ch = charAt(i++);
            if (!(ch == '"' || ch == '\'') || ch != charAt(--x))
                throw new IllegalStateException();
            StringBuilder sb = new StringBuilder();
            while (i < x) {
                ch = charAt(i++);
                if (ch == '\\')
                    ch = charAt(i++);
                sb.append(ch);
            }
            return sb.toString();
        }

    }

}
