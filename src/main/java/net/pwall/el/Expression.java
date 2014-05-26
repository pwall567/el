/*
 * @(#) Expression.java v1.5
 *
 * JSTL Expression Language Parser / Evaluator
 * Copyright (C) 2003, 2005, 2006, 2007, 2014  Peter Wall
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.ParsePosition;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;

/**
 * Expression evaluation system for the JSTL Expression Language (EL).  This
 * class is the base class for the various expression elements, and it also
 * contains nested classes for the parser and the expression elements, and
 * static methods to simplify access to the parser.
 *
 * <p>Expression evaluation is a two-stage process.  First the source string is
 * parsed into an expression tree, where the lowest-priority operator is the
 * root node of the tree.  Then the tree is evaluated, by calling the
 * <code>evaluate</code> method on the root node.  This will recursively call
 * <code>evaluate</code> on its operands, and then apply the operation.</p>
 *
 * <p>The tree resulting from the <code>parse</code> operation may be saved and
 * evaluated repeatedly, with the input variables possibly changing with each
 * evaluation.</p>
 *
 * <p>Variable names in the expression must be resolved by a
 * <code>Resolver</code> object supplied to the <code>Parser</code>, or passed
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
 *     Expression exp = parser.parseExpression("4 > 3 ? 'a' : 'b'", null);
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
     * Parse an entire string to an <code>Expression</code> tree.
     *
     * @param str      text to parse (not including ${ })
     * @param resolver a <code>Resolver</code> object to resolve names in the
     *                 expression
     * @return         the <code>Expression</code> tree
     * @throws ParseException on any errors
     */
    public static Expression parseExpression(CharSequence str,
            Resolver resolver) throws ParseException {
        return defaultParser.parseExpression(str, resolver);
    }

    /**
     * Replace all occurrences of <code>$&#123;...&#125;</code> in a string
     * with the contents of the braces evaluated as an expression.
     *
     * @param source   the input text
     * @param resolver a <code>Resolver</code> object to resolve names in the
     *                 expression
     * @return         the text following substitutions
     * @throws ExpressionException on any errors
     */
    public static String substitute(String source, Resolver resolver)
            throws ExpressionException {
        return defaultParser.substitute(source, resolver);
    }

    /**
     * Parse a string and return an expression which will replace all
     * occurrences of <code>$&#123;...&#125;</code> in the string with the
     * contents of the braces evaluated as an expression.
     *
     * @param str      the text to be parsed
     * @param resolver a <code>Resolver</code> object to resolve names in the
     *                 expression
     * @return         the <code>Expression</code> tree
     * @throws ParseException on any errors
     */
    public static Expression parseSubstitution(CharSequence str,
            Resolver resolver) throws ParseException {
        return defaultParser.parseSubstitution(str, resolver);
    }

    /**
     * Optimize the expression - evaluate constant sub-expressions and remove
     * parentheses.  For an expression that is parsed once and used repeatedly,
     * optimization is recommended.  For an expression that is evaluated only
     * once this is unnecessary.
     *
     * <p>Subclasses must override this method to perform any specific
     * optimizations; the default behavior is to return the object itself
     * (perform no optimizations).</p>
     *
     * @return  the current expression object
     */
    public Expression optimize() {
        return this;
    }

    /**
     * Get the result type of this expression.  The default is
     * <code>{@link Object}</code>.
     *
     * @return  the result type of the expression
     */
    public Class<?> getType() {
        return Object.class;
    }

    /**
     * Evaluate the expression.
     *
     * @return the result of the expression
     * @throws EvaluationException on any errors
     */
    public abstract Object evaluate() throws EvaluationException;

    /**
     * Return <code>true</code> if the expression is constant.  The default is
     * <code>false</false>.
     *
     * @return <code>true</code> if the expression is constant
     */
    public boolean isConstant() {
        return false;
    }

    /**
     * Evaluate the expression and return the result as a <code>boolean</code>.
     *
     * @return the result of the expression as a <code>boolean</code>
     * @throws EvaluationException on any errors
     */
    public boolean asBoolean() throws EvaluationException {
        return asBoolean(evaluate());
    }

    /**
     * Convert an <code>Object</code> to boolean using the coercion rules in the
     * specification document.
     *
     * @param  object    the object
     * @return           a boolean
     * @throws BooleanCoercionException on any errors
     */
    public static boolean asBoolean(Object object)
            throws BooleanCoercionException {
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
     * Evaluate the expression and return the result as an <code>int</code>.
     *
     * @return the result of the expression as an <code>int</code>
     * @throws EvaluationException on any errors
     */
    public int asInt() throws EvaluationException {
        return asInt(evaluate());
    }

    /**
     * Convert an <code>Object</code> to int using the coercion rules in the
     * specification document.
     *
     * @param  object    the object
     * @return           an int
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
     * Evaluate the expression and return the result as a <code>long</code>.
     *
     * @return the result of the expression as a <code>long</code>
     * @throws EvaluationException on any errors
     */
    public long asLong() throws EvaluationException {
        return asLong(evaluate());
    }

    /**
     * Convert an <code>Object</code> to long using the coercion rules in the
     * specification document.
     *
     * @param  object    the object
     * @return           a long
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
     * Evaluate the expression and return the result as a <code>double</code>.
     *
     * @return the result of the expression as a <code>double</code>
     * @throws EvaluationException on any errors
     */
    public double asDouble() throws EvaluationException {
        return asDouble(evaluate());
    }

    /**
     * Convert an <code>Object</code> to double using the coercion rules in the
     * specification document.
     *
     * @param  object    the object
     * @return           a double
     * @throws DoubleCoercionException on any errors
     */
    public static double asDouble(Object object)
            throws DoubleCoercionException {
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
     * Evaluate the expression and return the result as a <code>String</code>.
     *
     * @return the result of the expression as a <code>String</code>
     * @throws EvaluationException on any errors
     */
    public String asString() throws EvaluationException {
        return asString(evaluate());
    }

    /**
     * Convert an <code>Object</code> to String using the coercion rules in the
     * specification document.
     *
     * @param  object    the object
     * @return           a String
     * @throws StringCoercionException on any errors
     */
    public static String asString(Object object)
            throws StringCoercionException {
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
     * @param left   the object on the left of the expression
     * @param right  the object on the right of the dot or within the brackets
     * @return  the result
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
     * @param left   the object on the left of the expression
     * @param right  the object on the right of the dot or within the brackets
     * @param value  the new value
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
     * @param ch  the character
     * @return    true if character is valid
     */
    public static boolean isIdentifierStart(char ch) {
        return Character.isJavaIdentifierStart(ch);
    }

    /**
     * Check whether character is valid as a part of an identifier.
     *
     * @param ch  the character
     * @return    true if character is valid
     */
    public static boolean isIdentifierPart(char ch) {
        return Character.isJavaIdentifierPart(ch);
    }

    /**
     * Check whether string is valid as an identifier.
     *
     * @param str  the string (any <code>CharSequence</code> is allowed)
     * @return     true if string is valid
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
     * Tests whether an operand object is floating point - a <code>Float</code>,
     * a <code>Double</code> or a string that could be converted to
     * a floating point number.
     *
     * @param a    the operand object to test
     * @return     true if the operand is floating point
     */
    protected static boolean floatOrStringOperand(Object a) {
        return a instanceof Float || a instanceof Double || floatString(a);
    }

    /**
     * Tests whether an operand object is a floating point object.
     *
     * @param a    the operand object to test
     * @return     true if the operand is a floating point object
     */
    protected static boolean floatOperand(Object a) {
        return a instanceof Float || a instanceof Double;
    }

    /**
     * Tests whether an operand object is a string that should be treated as a
     * floating point number.  Note that it does not confirm that the string
     * contains a valid number, only that any attempt to convert it to a number
     * should use a floating point conversion.
     *
     * @param a    the operand object to test
     * @return     true if the operand is a floating point string
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
     * Tests whether an operand object is an integer (<code>Long</code>) object.
     *
     * @param a    the operand object to test
     * @return     true if the operand is an integer object
     */
    protected static boolean longOperand(Object a) {
        return a instanceof Byte || a instanceof Short || a instanceof Long ||
                a instanceof Integer || a instanceof Character;
    }

    /**
     * Find a public getter method for the named property.  The class may
     * not be public, or the getter method in that class may not be public,
     * so we need to go up the class hierarchy or the implemented interfaces
     * to locate a public method on a public class or interface.
     *
     * @param cls   the class of the object
     * @param name  the getter method name
     * @return      the <code>Method</code> object.
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
     * Find a public setter method for the named property.  The class may
     * not be public, or the setter method in that class may not be public,
     * so we need to go up the class hierarchy or the implemented interfaces
     * to locate a public method on a public class or interface.
     *
     * @param leftClass  the class of the object
     * @param name       the setter method name
     * @param valueClass the class of the value
     * @return           the <code>Method</code> object.
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
     * <code>Expression</code> class which will translate the standard form of
     * the Expression Language; non-standard uses (e.g. the use of the assign
     * operator) will require a specific parser to be instantiated and
     * customized before use.
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
         * Get the state of the "assign allowed" flag.  When this flag is set
         * to <code>true</code> the assignment operator is recognized by the
         * parser (this is an extension to the JSTL 1.0 EL specification).
         *
         * @return the state of the "assign allowed" flag
         */
        public boolean isAssignAllowed() {
            return assignAllowed;
        }

        /**
         * Set the new state of the "assign allowed" flag.
         *
         * @param assignAllowed  the new state of the "assign allowed" flag
         * @see #isAssignAllowed()
         */
        public void setAssignAllowed(boolean assignAllowed) {
            this.assignAllowed = assignAllowed;
        }

        /**
         * Get the state of the "conditional allowed" flag.  When this flag is
         * set to <code>true</code> the conditional (triadic) operator is
         * recognized by the parser (this is an extension to the JSTL 1.0 EL
         * specification).
         *
         * @return the state of the "conditional allowed" flag
         */
        public boolean isConditionalAllowed() {
            return conditionalAllowed;
        }

        /**
         * Set the new state of the "conditional allowed" flag.
         *
         * @param conditionalAllowed  the new state of the "conditional
         *                            allowed" flag
         * @see #isConditionalAllowed()
         */
        public void setConditionalAllowed(boolean conditionalAllowed) {
            this.conditionalAllowed = conditionalAllowed;
        }

        /**
         * Get the state of the "case convert allowed" flag.  When this flag is
         * set to <code>true</code> the <code>toupper</code> and
         * <code>tolower</code> case conversion operators are recognized by the
         * parser (this is an extension to the JSTL 1.0 EL specification).
         *
         * @return the state of the "case convert allowed" flag
         */
        public boolean isCaseConvertAllowed() {
            return caseConvertAllowed;
        }

        /**
         * Set the new state of the "case convert allowed" flag.
         *
         * @param caseConvertAllowed  the new state of the "case convert
         *                            allowed" flag
         * @see #isCaseConvertAllowed()
         */
        public void setCaseConvertAllowed(boolean caseConvertAllowed) {
            this.caseConvertAllowed = caseConvertAllowed;
        }

        /**
         * Get the state of the "match allowed" flag.  When this flag is set to
         * <code>true</code> the pattern match operator is recognized by the
         * parser (this is an extension to the JSTL 1.0 EL specification).
         *
         * @return the state of the "match allowed" flag
         */
        public boolean isMatchAllowed() {
            return matchAllowed;
        }

        /**
         * Set the new state of the "match allowed" flag.
         *
         * @param matchAllowed  the new state of the "match allowed" flag
         * @see #isMatchAllowed()
         */
        public void setMatchAllowed(boolean matchAllowed) {
            this.matchAllowed = matchAllowed;
        }

        /**
         * Get the state of the "join allowed" flag.  When this flag is set to
         * <code>true</code> the string concatenation (join) operator is
         * recognized by the parser (this is an extension to the JSTL 1.0 EL
         * specification).
         *
         * @return the state of the "join allowed" flag
         */
        public boolean isJoinAllowed() {
            return joinAllowed;
        }

        /**
         * Set the new state of the "join allowed" flag.
         *
         * @param joinAllowed  the new state of the "join allowed" flag
         * @see #isJoinAllowed()
         */
        public void setJoinAllowed(boolean joinAllowed) {
            this.joinAllowed = joinAllowed;
        }

        /**
         * Get the state of the "array operation allowed" flag.  When this flag
         * is set to <code>true</code> the array/list length and sum operators
         * are recognized by the parser (this is an extension to the JSTL 1.0 EL
         * specification).
         *
         * @return the state of the "array operation allowed" flag
         */
        public boolean isArrayOperationAllowed() {
            return arrayOperationAllowed;
        }

        /**
         * Set the new state of the "array operation allowed" flag.
         *
         * @param arrayOperationAllowed  the new state of the "array operation
         *                               allowed" flag
         * @see #isArrayOperationAllowed()
         */
        public void setArrayOperationAllowed(boolean arrayOperationAllowed) {
            this.arrayOperationAllowed = arrayOperationAllowed;
        }

        /**
         * Parse an expression from text to an <code>Expression</code> tree.
         * The parse operation will start at the position indicated by the
         * <code>ParsePosition</code> object supplied as a parameter, and will
         * stop on the first unrecognized element.  The
         * <code>ParsePosition</code> will be updated to point to the location
         * where the parse stopped, and the caller must confirm that this is the
         * end of the expression.
         *
         * @param str      text to parse (not including ${ })
         * @param pp       <code>ParsePosition</code> object
         * @param resolver a <code>Resolver</code> object to resolve names in
         *                 the expression
         * @return         the <code>Expression</code> tree
         * @throws ParseException on any errors
         */
        public Expression parseExpression(CharSequence str, ParsePosition pp,
                Resolver resolver) throws ParseException {
            Operator expression = new Parentheses(null);
            Operator current = expression;
            int strLen = str.length();
            for (;;) {
                skipSpaces(str, pp);
                int tokenStart = pp.getIndex();
                if (tokenStart >= strLen)
                    throw new UnexpectedEndException();
                char ch = str.charAt(tokenStart);
                // first look for a prefix operator (-, !, not, empty)
                if (ch == '-') {
                    // check if start of numeric literal
                    int pos = tokenStart + 1;
                    if (!(pos < strLen && isDigitOrDot(str.charAt(pos)))) {
                        Operator op = new Negate(null);
                        current.setRight(op);
                        current = op;
                        pp.setIndex(pos);
                        continue;
                    }
                }
                if (match(str, pp, '!') || match(str, pp, "not")) {
                    Operator op = new Not(null);
                    current.setRight(op);
                    current = op;
                    continue;
                }
                if (match(str, pp, "empty")) {
                    Operator op = new Empty(null);
                    current.setRight(op);
                    current = op;
                    continue;
                }
                if (isCaseConvertAllowed() && match(str, pp, "toupper")) {
                    Operator op = new ToUpper(null);
                    current.setRight(op);
                    current = op;
                    continue;
                }
                if (isCaseConvertAllowed() && match(str, pp, "tolower")) {
                    Operator op = new ToLower(null);
                    current.setRight(op);
                    current = op;
                    continue;
                }
                if (isArrayOperationAllowed() && match(str, pp, "length")) {
                    Operator op = new Length(null);
                    current.setRight(op);
                    current = op;
                    continue;
                }
                if (isArrayOperationAllowed() && match(str, pp, "sum")) {
                    Operator op = new Sum(null);
                    current.setRight(op);
                    current = op;
                    continue;
                }
                // check for parentheses and a subexpression (recursion)
                if (ch == '(') {
                    pp.setIndex(tokenStart + 1);
                    Expression nested = parseExpression(str, pp, resolver);
                    int newPos = pp.getIndex();
                    if (newPos >= strLen || str.charAt(newPos) != ')') {
                        pp.setErrorIndex(newPos);
                        throw new UnmatchedParenthesisException();
                    }
                    if (!(nested instanceof Parentheses))
                        nested = new Parentheses(nested);
                    current.setRight(nested);
                    pp.setIndex(newPos + 1);
                }
                else if (ch >= '0' && ch <= '9' || ch == '.' || ch == '+' ||
                        ch == '-') {
                    // numeric literal
                    current.setRight(new Constant(parseNumber(str, pp)));
                }
                else if (ch == '"' || ch == '\'') {
                    // string literal
                    current.setRight(new Constant(parseString(str, pp)));
                }
                else if (match(str, pp, "null")) {
                    // null literal
                    current.setRight(new Constant(null));
                }
                else if (match(str, pp, "true")) {
                    // boolean literal
                    current.setRight(trueConstant);
                }
                else if (match(str, pp, "false")) {
                    // boolean literal
                    current.setRight(falseConstant);
                }
                else if (match(str, pp, "and") || match(str, pp, "div") ||
                        match(str, pp, "eq") || match(str, pp, "ge") ||
                        match(str, pp, "gt") || match(str, pp, "instanceof") ||
                        match(str, pp, "le") || match(str, pp, "lt") ||
                        match(str, pp, "mod") || match(str, pp, "ne") ||
                        match(str, pp, "or")) {
                    // other reserved word - error
                    pp.setErrorIndex(tokenStart);
                    throw new ReservedWordException();
                }
                else if (isIdentifierStart(ch)) {
                    // lookup identifier using external name resolver
                    String identString = parseIdentifier(str, pp);
                    Expression identifier = null;
                    if (resolver != null)
                        identifier = resolver.resolve(identString);
                    if (identifier == null) {
                        pp.setErrorIndex(tokenStart);
                        throw new IdentifierException(identString);
                    }
                    current.setRight(identifier);
                }
                else {
                    // none of the above - error
                    pp.setErrorIndex(tokenStart);
                    throw new UnexpectedElementException();
                }
                skipSpaces(str, pp);
                // now check for . and [] (possibly multiple)
                Expression currentRight = current.getRight();
                if (currentRight instanceof Parentheses ||
                        currentRight instanceof Variable) {
                    while (pp.getIndex() < strLen) {
                        if (match(str, pp, '.')) {
                            // . property
                            skipSpaces(str, pp);
                            int i = pp.getIndex();
                            if (i >= strLen ||
                                    !isIdentifierStart(str.charAt(i))) {
                                pp.setErrorIndex(i);
                                throw new PropertyException();
                            }
                            Indexed indexed = new Indexed(currentRight,
                                    new Constant(parseIdentifier(str, pp)));
                            current.setRight(indexed);
                            currentRight = indexed;
                            skipSpaces(str, pp);
                        }
                        else if (match(str, pp, '[')) {
                            // [ index ]
                            int start = pp.getIndex() - 1;
                            Expression nested =
                                    parseExpression(str, pp, resolver);
                            int i = pp.getIndex();
                            if (i >= strLen || str.charAt(i) != ']') {
                                pp.setErrorIndex(start);
                                throw new UnmatchedBracketException();
                            }
                            Indexed indexed = new Indexed(currentRight, nested);
                            current.setRight(indexed);
                            currentRight = indexed;
                            pp.setIndex(i + 1);
                            skipSpaces(str, pp);
                        }
                        else
                            break;
                    }
                }
                if (pp.getIndex() == strLen)
                    break;
                // now check for a diadic operator (like + or ==)
                Diadic diadic = null;
                if (match(str, pp, "and") || match(str, pp, "&&"))
                    diadic = new And(null, null);
                else if (match(str, pp, "or") || match(str, pp, "||"))
                    diadic = new Or(null, null);
                else if (match(str, pp, '+'))
                    diadic = new Plus(null, null);
                else if (match(str, pp, '-'))
                    diadic = new Minus(null, null);
                else if (match(str, pp, '*'))
                    diadic = new Multiply(null, null);
                else if (match(str, pp, '/') || match(str, pp, "div"))
                    diadic = new Divide(null, null);
                else if (match(str, pp, '%') || match(str, pp, "mod"))
                    diadic = new Modulo(null, null);
                else if (match(str, pp, ">=") || match(str, pp, "ge"))
                    diadic = new GreaterEqual(null, null);
                else if (match(str, pp, "<=") || match(str, pp, "le"))
                    diadic = new LessEqual(null, null);
                else if (match(str, pp, '>') || match(str, pp, "gt"))
                    diadic = new GreaterThan(null, null);
                else if (match(str, pp, '<') || match(str, pp, "lt"))
                    diadic = new LessThan(null, null);
                else if (match(str, pp, "==") || match(str, pp, "eq"))
                    diadic = new Equal(null, null);
                else if (match(str, pp, "!=") || match(str, pp, "ne"))
                    diadic = new NotEqual(null, null);
                else if (isConditionalAllowed() && match(str, pp, '?'))
                    diadic = new Conditional(null, null);
                else if (isConditionalAllowed() && match(str, pp, ':'))
                    diadic = new Else(null, null);
                else if (isAssignAllowed() && match(str, pp, '='))
                    diadic = new Assign(null, null);
                else if (isMatchAllowed() && match(str, pp, "~="))
                    diadic = new Match(null, null);
                else if (isJoinAllowed() && match(str, pp, '#'))
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
         * Check that every conditional expression has a matching else
         * subexpression, and that no else expressions appear other than as
         * subexpressions of a conditional expression.
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
         * Parse an entire string to an <code>Expression</code> tree.
         *
         * @param str      text to parse (not including ${ })
         * @param resolver a <code>Resolver</code> object to resolve names in
         *                 the expression
         * @return         the <code>Expression</code> tree
         * @throws ParseException on any errors
         */
        public Expression parseExpression(CharSequence str,
                Resolver resolver) throws ParseException {
            ParsePosition pp = new ParsePosition(0);
            Expression expression = parseExpression(str, pp, resolver);
            if (pp.getIndex() < str.length()) {
                pp.setErrorIndex(pp.getIndex());
                throw new UnparsedStringException();
            }
            return expression;
        }

        /**
         * Replace all occurrences of <code>$&#123;...&#125;</code> in a string
         * with the contents of the braces evaluated as an expression.
         *
         * @param source   the input text
         * @param resolver a <code>Resolver</code> object to resolve names in
         *                 the expression
         * @return         the text following substitutions
         * @throws ExpressionException on any errors
         */
        public String substitute(String source, Resolver resolver)
                throws ExpressionException {
            int offset = source.indexOf("${");
            if (offset < 0)
                return source;
            ParsePosition pp = new ParsePosition(2);
            int current = 0;
            StringBuffer buf = null;
            int len = source.length();
            if (offset == 0) {
                // optimize case where source is a single substitution
                String result = parseExpression(source, pp, resolver).
                        asString();
                current = pp.getIndex();
                if (!(current < len && source.charAt(current) == '}'))
                    throw new UnmatchedBraceException();
                if (++current == len)
                    return result;
                buf = new StringBuffer(result);
                offset = source.indexOf("${", current);
                if (offset < 0) {
                    buf.append(source.substring(current));
                    return buf.toString();
                }
            }
            else
                buf = new StringBuffer();
            for (;;) {
                if (offset > current)
                    buf.append(source.substring(current, offset));
                pp.setIndex(offset + 2);
                buf.append(parseExpression(source, pp, resolver).asString());
                current = pp.getIndex();
                if (!(current < len && source.charAt(current) == '}'))
                    throw new UnmatchedBraceException();
                ++current;
                offset = source.indexOf("${", current);
                if (offset < 0)
                    break;
            }
            if (current < len)
                buf.append(source.substring(current));
            return buf.toString();
        }

        /**
         * Parse a string and return an expression which will replace all
         * occurrences of <code>$&#123;...&#125;</code> in the string with the
         * contents of the braces evaluated as an expression.
         *
         * @param str      the text to be parsed
         * @param resolver a <code>Resolver</code> object to resolve names in
         *                 the expression
         * @return         the <code>Expression</code> tree
         * @throws ParseException on any errors
         */
        public Expression parseSubstitution(CharSequence str, Resolver resolver)
                throws ParseException {
            Concat concat = new Concat();
            int len = str.length();
            int i = 0;
            for (;;) {
                if (i == len)
                    break;
                if (str.charAt(i) == '$' && i + 1 < len &&
                        str.charAt(i + 1) == '{') {
                    ParsePosition pp = new ParsePosition(i + 2);
                    concat.addExpression(parseExpression(str, pp, resolver));
                    i = pp.getIndex();
                    if (!(i < len && str.charAt(i) == '}'))
                        throw new UnmatchedBraceException();
                    ++i;
                }
                else {
                    int start = i;
                    do {
                        ++i;
                    } while (i < len && !(str.charAt(i) == '$' && i + 1 < len &&
                            str.charAt(i + 1) == '{'));
                    concat.addExpression(
                            new Constant(str.subSequence(start, i).toString()));
                }
            }
            return concat.singleExpression();
        }

        /**
         * Parse an expression from text to an <code>Expression</code> tree.
         * This method is identical to {@link #parseExpression(CharSequence,
         * ParsePosition, Expression.Resolver)} above except that it uses a
         * <code>Resolver</code> set by the
         * {@link #setResolver(Expression.Resolver)} method.
         *
         * @param str      text to parse (not including ${ })
         * @param pp       <code>ParsePosition</code> object
         * @return         the <code>Expression</code> tree
         * @throws ParseException on any errors
         */
        public Expression parseExpression(CharSequence str, ParsePosition pp)
                throws ParseException {
            return parseExpression(str, pp, resolver);
        }

        /**
         * Parse an entire string to an <code>Expression</code> tree.  This
         * method is identical to {@link #parseExpression(CharSequence,
         * Expression.Resolver)} above except that it uses a
         * <code>Resolver</code> set by the
         * {@link #setResolver(Expression.Resolver)} method.
         *
         * @param str      text to parse (not including ${ })
         * @return         the <code>Expression</code> tree
         * @throws ParseException on any errors
         */
        public Expression parseExpression(CharSequence str)
                throws ParseException {
            return parseExpression(str, resolver);
        }

        /**
         * Replace all occurrences of <code>$&#123;...&#125;</code> in a string
         * with the contents of the braces evaluated as an expression.
         * This method is identical to {@link #substitute(String,
         * Expression.Resolver)} above except that it uses a
         * <code>Resolver</code> set by the
         * {@link #setResolver(Expression.Resolver)} method.
         *
         * @param source   the input text
         * @return         the text following substitutions
         * @throws ExpressionException on any errors
         */
        public String substitute(String source) throws ExpressionException {
            return substitute(source, resolver);
        }

        /**
         * Parse a string and return an expression which will replace all
         * occurrences of <code>$&#123;...&#125;</code> in the string with the
         * contents of the braces evaluated as an expression.
         * This method is identical to {@link #parseSubstitution(CharSequence,
         * Expression.Resolver)} above except that it uses a
         * <code>Resolver</code> set by the
         * {@link #setResolver(Expression.Resolver)} method.
         *
         * @param str      the text to be parsed
         * @return         the <code>Expression</code> tree
         * @throws ParseException on any errors
         */
        public Expression parseSubstitution(CharSequence str)
                throws ParseException {
            return parseSubstitution(str, resolver);
        }

        /**
         * Get the current <code>Resolver</code>.
         *
         * @return the current <code>Resolver</code>
         */
        public Resolver getResolver() {
            return resolver;
        }

        /**
         * Set the <code>Resolver</code> for use when no <code>Resolver</code>
         * is specified on a method call.
         *
         * @param resolver the <code>Resolver</code>
         */
        public void setResolver(Resolver resolver) {
            this.resolver = resolver;
        }

        /**
         * Extracts an identifier from the current position in the character
         * sequence.  This assumes that the first character has already been
         * confirmed to be a valid identifier start character.
         *
         * @param str   the character sequence
         * @param pp    a <code>ParsePosition</code> object containing the
         *              current index into the character sequence - this will be
         *              updated to reflect the position after the identifier
         * @return      the identifier
         */
        private static String parseIdentifier(CharSequence str,
                ParsePosition pp) {
            int start = pp.getIndex();
            int strLen = str.length();
            int i = start;
            do {
                ++i;
            } while (i < strLen && isIdentifierPart(str.charAt(i)));
            pp.setIndex(i);
            return str.subSequence(start, i).toString();
        }

        /**
         * Extracts a number from the current position in the character
         * sequence.
         *
         * @param str   the character sequence
         * @param pp    a <code>ParsePosition</code> object containing the
         *              current index into the character sequence - this will be
         *              updated to reflect the position after the number
         * @return      the identifier
         * @throws NumberException on any errors 
         */
        private static Number parseNumber(CharSequence str, ParsePosition pp)
                throws NumberException {
            int strLen = str.length();
            int i = pp.getIndex();
            int start = i;
            char ch = str.charAt(i);
            boolean negative = false;
            if (ch == '+')
                ++i;
            else if (ch == '-') {
                negative = true;
                ++i;
            }
            if (i >= strLen) {
                pp.setIndex(i);
                pp.setErrorIndex(start);
                throw new NumberException();
            }
            ch = str.charAt(i);
            long longResult = 0L;
            for (;;) {
                if (ch >= '0' && ch <= '9') {
                    longResult = longResult * 10 + ch - '0';
                    if (++i >= strLen)
                        break;
                    ch = str.charAt(i);
                }
                else if (ch == '.' || ch == 'e' || ch == 'E') {
                    double doubleResult = longResult;
                    if (ch == '.') {
                        if (++i >= strLen) {
                            pp.setIndex(i);
                            pp.setErrorIndex(start);
                            throw new NumberException();
                        }
                        double increment = 0.1;
                        while ((ch = str.charAt(i)) >= '0' && ch <= '9') {
                            doubleResult += increment * (ch - '0');
                            increment *= 0.1;
                            if (++i >= strLen)
                                break;
                        }
                    }
                    if (i < strLen) {
                        ch = str.charAt(i);
                        if (ch == 'e' || ch == 'E') {
                            if (++i >= strLen ||
                                    !((ch = str.charAt(i)) >= '0' &&
                                            ch <= '9')) {
                                pp.setIndex(i);
                                pp.setErrorIndex(start);
                                throw new NumberException();
                            }
                            int exponent = 0;
                            do {
                                exponent = exponent * 10 + ch - '0';
                                if (++i >= strLen)
                                    break;
                            } while ((ch = str.charAt(i)) >= '0' && ch <= '9');
                            for (; exponent > 0; --exponent)
                                doubleResult *= 10.0;
                        }
                    }
                    pp.setIndex(i);
                    return new Double(negative ? -doubleResult : doubleResult);
                }
                else
                    break;
            }
            pp.setIndex(i);
            return new Long(negative ? -longResult : longResult);
        }

        /**
         * Extracts a string from the current position in the character
         * sequence.  This assumes that the first character has already been
         * confirmed to be a valid opening quote character.
         *
         * @param str   the character sequence
         * @param pp    a <code>ParsePosition</code> object containing the
         *              current index into the character sequence - this will be
         *              updated to reflect the position after the string
         * @return      the identifier
         * @throws StringException on any errors
         */
        private static String parseString(CharSequence str, ParsePosition pp)
                throws StringException {
            StringBuffer buf = new StringBuffer();
            int n = str.length();
            int i = pp.getIndex();
            char quote = str.charAt(i++);
            boolean escape = false;
            for (;;) {
                if (i >= n) {
                    pp.setIndex(i);
                    pp.setErrorIndex(i);
                    throw new StringException();
                }
                char ch = str.charAt(i);
                if (escape) {
                    if (ch != '\\' && ch != '"' && ch != '\'')
                        buf.append('\\');
                    buf.append(ch);
                    escape = false;
                }
                else {
                    if (ch == quote)
                        break;
                    if (ch == '\\')
                        escape = true;
                    else
                        buf.append(ch);
                }
                ++i;
            }
            pp.setIndex(i + 1);
            return buf.toString();
        }

        /**
         * Skip past spaces in the character sequence.
         *
         * @param str  the character sequence
         * @param pp   a <code>ParsePosition</code> object containing the
         *             current index into the character sequence - this will be
         *             updated to reflect the position of the next non-space
         *             character
         */
        private static void skipSpaces(CharSequence str, ParsePosition pp) {
            int i = pp.getIndex();
            int n = str.length();
            while (i < n && isSpace(str.charAt(i)))
                ++i;
            pp.setIndex(i);
        }

        /**
         * Compare the characters at the current position in a character
         * sequence with a given string.  If the string to be matched is an
         * identifier name, the match is successful only if the next character
         * is not a valid name character.
         *
         * @param str     the character sequence
         * @param pp      a <code>ParsePosition</code> object containing the
         *                current index into the character sequence - this will
         *                be updated to reflect the position after the matched
         *                string if the match was successful
         * @param target  the target string
         * @return        true if the string matches
         */
        private static boolean match(CharSequence str, ParsePosition pp,
                CharSequence target) {
            int pos = pp.getIndex();
            for (int i = 0; i < target.length(); )
                if (pos >= str.length() || str.charAt(pos++) !=
                        target.charAt(i++))
                    return false;
            if (isIdentifierStart(target.charAt(0)) && pos < str.length() &&
                    isIdentifierPart(str.charAt(pos)))
                return false;
            pp.setIndex(pos);
            return true;
        }

        /**
         * Compare the character at the current position in a character sequence
         * with a given character.
         *
         * @param str     the character sequence
         * @param pp      a <code>ParsePosition</code> object containing the
         *                current index into the character sequence - this will
         *                be updated to reflect the position after the matched
         *                character if the match was successful
         * @param target  the target character
         * @return        true if the character matches
         */
        private static boolean match(CharSequence str, ParsePosition pp,
                char target) {
            int pos = pp.getIndex();
            if (pos >= str.length() || str.charAt(pos) != target)
                return false;
            pp.setIndex(pos + 1);
            return true;
        }

        /**
         * Test character for space - includes HT (tab), NL (LF) and CR.
         *
         * @param ch    the character to test
         * @return      true if character is a space
         */
        private static boolean isSpace(char ch) {
            return ch == ' ' || ch == '\t' || ch == '\n' || ch == '\r';
        }

        /**
         * Test character for digit or dot.
         *
         * @param ch    the character to test
         * @return      true if character is a digit or dot (period)
         */
        private static boolean isDigitOrDot(char ch) {
            return ch >= '0' && ch <= '9' || ch == '.';
        }

    }

    /**
     * An interface to represent assignable expressions.  This is an extension
     * to the JSTL 1.0 EL specification.
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
     * class are generally returned by the <code>Resolver</code>.
     *
     * <p>This class contains no functionality; the <code>Resolver</code> must
     * return an implementation class which provides <code>evaluate()</code>
     * and <code>assign()</code> methods.</p>
     */
    public static abstract class Variable extends Expression
            implements Assignable {

    }

    /**
     * A class to represent a constant in an expression tree.  Objects of this
     * class are created for constants in the expression.  The
     * <code>optimize()</code> method will pre-evaluate operators with
     * constant operands where possible.
     */
    public static class Constant extends Expression {

        private Object value;

        /**
         * Construct a <code>Constant</code> with a given object value.
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
         * Return <code>true</code> to indicate the expression is constant.
         *
         * @return <code>true</code> - the expression is constant
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
         * Test for equality.  Return true only if the other object is a
         * constant with the same value.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
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
         * Convert to string.  This returns the string representation of the
         * constant in the Expression Language.  String constants require
         * special handling to ensure that quotes are used and special
         * characters are escaped.
         *
         * @return  a string representation of the constant
         */
        @Override
        public String toString() {
            if (value == null)
                return "null";
            if (value instanceof String) {
                StringBuffer sb = new StringBuffer();
                String str = (String)value;
                char quote = str.indexOf('\'') >= 0 && str.indexOf('"') < 0 ?
                        '"' : '\'';
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
            // note - for all other forms of constant generated by the parser,
            // the standard toString() conversion on the object will produce the
            // correct form of string; any constants generated by a Resolver
            // must provide their own formatting, probably by subclassing
            // Constant
            return value.toString();
        }

    }

    /**
     * A base class for all operators, both monadic (one operand) and diadic
     * (two operands).
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
         * Optimize the subexpression.  The default behavior is to optimize the
         * operand, then if it is constant, execute the operation.
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
         * @return <code>true</code> if the operand is constant
         */
        public boolean optimizeRightOperand() {
            Expression optRight = right.optimize();
            right = optRight;
            return optRight.isConstant();
        }

        /**
         * Get the priority of the operator.  The priority is used to determine
         * the order of evaluation of an expression with multiple operations.
         * For example, a + b * c is evaluated as (a + (b * c)).
         *
         * @return  the priority
         */
        public abstract int getPriority();

        /**
         * Get the name of the operator.  The name is the token which represents
         * the operator in the Expression Language.
         *
         * @return  the name
         */
        public abstract String getName();

        /**
         * Test for equality.  Return true only if the other object is an
         * <code>Operator</code> with an equal operand.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
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
            StringBuffer sb = new StringBuffer();
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
         * Optimize the subexpression.  The default behavior is to optimize the
         * operands, then if both are constant, execute the operation.
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
         * @return <code>true</code> if both operands are constant
         */
        public boolean optimizeOperands() {
            boolean rightConstant = optimizeRightOperand();
            Expression optLeft = left.optimize();
            left = optLeft;
            return rightConstant && optLeft.isConstant();
        }

        /**
         * Return true if the operation is commutative.  The default is false.
         *
         * @return false
         */
        public boolean isCommutative() {
            return false;
        }

        /**
         * Return true if the operation is associative.  The default is false.
         *
         * @return false
         */
        public boolean isAssociative() {
            return false;
        }

        /**
         * Return true if the operation is left-associative.  The default is
         * true.
         *
         * @return true
         */
        public boolean isLeftAssociative() {
            return true;
        }

        /**
         * Test for equality.  Return true only if the other object is a
         * <code>Diadic</code> operation with equal operands, or is
         * commutative with equal operands reversed, or is associative and all
         * operands match.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
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
            StringBuffer sb = new StringBuffer();
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
     * A class to represent the indexing operation.  The expression language
     * converts property references of the form 'object.property' to
     * 'object["property"]', so both of these are handled by this operator.
     * 
     * <p>Much of the behavior of this class is implemented as static methods
     * on the <code>Expression</code> class so that they may be accessed more
     * easily from outside the class.</p>
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
         * Evaluate the subexpression.  The rules for this operation are given
         * in the specification document referenced at the start of the
         * <code>Expression</code> class.
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
         * Test for equality.  Return true only if the other object is an
         * <code>Indexed</code> operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Indexed && super.equals(o);
        }

        /**
         * Convert subexpression to string.  This operator requires special
         * handling because of the syntax of indexing operations.
         *
         * @return  a string representation of the subexpression
         */
        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
            Expression expr = getLeft();
            if (expr instanceof Operator &&
                    ((Operator)expr).getPriority() < priority) {
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
     * A class to represent the assignment operation.  This is an extension to
     * the JSTL 1.0 EL specification.
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
         * Return false - the operation is right-associative.
         *
         * @return false
         */
        @Override
        public boolean isLeftAssociative() {
            return false;
        }

        /**
         * Get the result type of this operator.  The result type is the type of
         * the right operand.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return getRight().getType();
        }

        /**
         * Evaluate the subexpression.  The result of an assignment operation is
         * the value assigned.
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
         * Optimize the subexpression.  The assignment operation can not be
         * optimized, and the left operand can only be partially optimized - for
         * example, if the left operand is <code>array[2+2]</code> the addition
         * can be optimized but the indexing operation can not.  The right
         * operand can be fully optimized.
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
         * Test for equality.  Return true only if the other object is an
         * <code>Assign</code> operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Assign && super.equals(o);
        }

    }

    /**
     * A class to represent the conditional expression ( <code>&#63;
     * &#58;</code> ).  This is an extension to the JSTL 1.0 EL specification.
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
         * Return false - the operation is right-associative.
         *
         * @return false
         */
        @Override
        public boolean isLeftAssociative() {
            return false;
        }

        /**
         * Get the result type of this operator.  This is determined from the
         * types of the operands of the <code>Else</code> operator.
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
         * Evaluate the conditional expression.  The result of the conditional
         * expression is the left or the right operand of the
         * <code>ElseExpression</code> which must be in the right operand
         * position of this expression, depending on the boolean value of the
         * left operand of this expression.
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
         * Optimize the conditional expression.  If the left operand is
         * constant, the expression is optimized to the subexpression in either
         * the left or the right operand of the <code>ElseExpression</code>
         * which must be in the right operand position of this expression.
         *
         * @return the true or the false result, or the expression itself
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
         * Convert subexpression to string.  This operator requires special
         * handling to ensure that the else pseudo-operator is not enclosed in
         * parentheses.
         *
         * @return  a string representation of the subexpression
         */
        @Override
        public String toString() {
            StringBuffer sb = new StringBuffer();
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
     * A class to represent the else portion of a conditional expression
     * ( <code>&#63; &#58;</code> ).  It is not so much an expression as a
     * structure to hold two alternative results from the conditional
     * expression, of which this must be the right operand.  This is an
     * extension to the JSTL 1.0 EL specification.
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
         * Return false - the operation is right-associative.
         *
         * @return false
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
         * Evaluate the subexpression.  This method determines the type of
         * operands supplied, then calls the appropriate <code>execute()</code>
         * method in the implementing class to apply the operation.
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
         * Get the result type of this operator.  The result type of an
         * arithmetic operation is always <code>{@link Number}</code> or a
         * subclass.
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
         * Apply the operation to <code>long</code> operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         * @return       the result
         */
        public abstract long execute(long left, long right);

        /**
         * Apply the operation to <code>double</code> operands.
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
         * Return true - the operation is commutative.
         *
         * @return true
         */
        @Override
        public boolean isCommutative() {
            return true;
        }

        /**
         * Return true - the operation is associative.
         *
         * @return true
         */
        @Override
        public boolean isAssociative() {
            return true;
        }

        /**
         * Apply the operation to <code>long</code> operands.
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
         * Apply the operation to <code>double</code> operands.
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
         * Optimize the subexpression.  In addition to the default optimization,
         * check for multiply by 0 or 1.
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
         * Test for equality.  Return true only if the other object is a
         * <code>Multiply</code> operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Multiply && super.equals(o);
        }

    }

    /**
     * An implementation class for the divide operation.  This does not use the
     * <code>Arithmetic</code> base class because the rules for the divide
     * operator are different from those for the other operators.
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
         * Get the result type of this operator.  The result type of a
         * divide operation is always <code>{@link Double}</code>.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Double.class;
        }

        /**
         * Evaluate the subexpression.  This method converts both operands to
         * <code>double</double> and performs the operation.
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
         * Optimize the subexpression.  In addition to the default optimization,
         * check for divisor of 1 or dividend of 0.
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
         * Test for equality.  Return true only if the other object is a
         * <code>Divide</code> operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
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
         * Apply the operation to <code>long</code> operands.
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
         * Apply the operation to <code>double</code> operands.
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
         * Test for equality.  Return true only if the other object is a
         * <code>Modulo</code> operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
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
         * Return true - the operation is commutative.
         *
         * @return true
         */
        @Override
        public boolean isCommutative() {
            return true;
        }

        /**
         * Return true - the operation is associative.
         *
         * @return true
         */
        @Override
        public boolean isAssociative() {
            return true;
        }

        /**
         * Apply the operation to <code>long</code> operands.
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
         * Apply the operation to <code>double</code> operands.
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
         * Optimize the subexpression.  In addition to the default optimization,
         * check for addition of 0.
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
         * Test for equality.  Return true only if the other object is a
         * <code>Plus</code> operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
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
         * Apply the operation to <code>long</code> operands.
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
         * Apply the operation to <code>double</code> operands.
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
         * Optimize the subexpression.  In addition to the default optimization,
         * check for subtraction of or from 0.
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
         * Test for equality.  Return true only if the other object is a
         * <code>Minus</code> operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Minus && super.equals(o);
        }

    }

    /**
     * An interface to represent logical operations (including comparison and
     * equality).
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
         * Get the result type of this operator.  The result type of an
         * AND operation is always <code>{@link Boolean}</code>.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Boolean.class;
        }

        /**
         * Evaluate the subexpression.  This method evaluates the left operand,
         * and only if it is TRUE evaluates the right operand.
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
         * Optimize the subexpression.  Optimize the operands, then, if either
         * operand is constant, return FALSE if it is FALSE, otherwise the other
         * operand.
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
         * Test for equality.  Return true only if the other object is an
         * <code>And</code> operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
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
         * Get the result type of this operator.  The result type of an
         * OR operation is always <code>{@link Boolean}</code>.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Boolean.class;
        }

        /**
         * Evaluate the subexpression.  This method evaluates the left operand,
         * and only if it is FALSE evaluates the right operand.
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
         * Optimize the subexpression.  Optimize the operands, then, if either
         * operand is constant, return TRUE if it is TRUE, otherwise the other
         * operand.
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
         * Test for equality.  Return true only if the other object is an
         * <code>Or</code> operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
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
         * Get the result type of this operator.  The result type of a
         * comparison operation is always <code>{@link Boolean}</code>.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Boolean.class;
        }

        /**
         * Evaluate the subexpression.  This method delegates the bulk of the
         * work to the <code>compare()</code> method.
         *
         * @return  the boolean object resulting from the comparison operation
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            return compare() ? Boolean.TRUE : Boolean.FALSE;
        }

        /**
         * Compare the operands.  This method performs the comparison and calls
         * the appropriate method on the implementing class to determine whether
         * the comparison result is TRUE or FALSE in this case.
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
         * Optimize the subexpression.  Optimize the operands, and if both are
         * constant, execute the operation.
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
         * The result if the left operand is less than the right.
         *
         * @return  false
         */
        @Override
        public boolean less() {
            return false;
        }

        /**
         * The result if the operands are equal.
         *
         * @return  false
         */
        @Override
        public boolean equal() {
            return false;
        }

        /**
         * The result if the left operand is greater than the right.
         *
         * @return  true
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
         * Test for equality.  Return true only if the other object is a
         * <code>GreaterThan</code> operation with equal operands, or a
         * <code>LessThan</code> with equal operands reversed.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof GreaterThan && super.equals(o))
                return true;
            if (!(o instanceof LessThan))
                return false;
            LessThan op = (LessThan)o;
            return getLeft().equals(op.getRight()) &&
                            getRight().equals(op.getLeft());
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
         * The result if the left operand is less than the right.
         *
         * @return  true
         */
        @Override
        public boolean less() {
            return true;
        }

        /**
         * The result if the operands are equal.
         *
         * @return  false
         */
        @Override
        public boolean equal() {
            return false;
        }

        /**
         * The result if the left operand is greater than the right.
         *
         * @return  false
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
         * Test for equality.  Return true only if the other object is a
         * <code>LessThan</code> operation with equal operands, or a
         * <code>GreaterThan</code> with equal operands reversed.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof LessThan && super.equals(o))
                return true;
            if (!(o instanceof GreaterThan))
                return false;
            GreaterThan op = (GreaterThan)o;
            return getLeft().equals(op.getRight()) &&
                            getRight().equals(op.getLeft());
        }

    }

    /**
     * An implementation class for the 'greater than or equal' operator.
     */
    public static class GreaterEqual extends Relative implements Logical {

        public static final String name = ">=";

        /**
         * Construct a 'greater than or equal' operation with the given
         * operands.
         *
         * @param left   the left operand
         * @param right  the right operand
         */
        public GreaterEqual(Expression left, Expression right) {
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
         * The result if the left operand is less than the right.
         *
         * @return  false
         */
        @Override
        public boolean less() {
            return false;
        }

        /**
         * The result if the operands are equal.
         *
         * @return  true
         */
        @Override
        public boolean equal() {
            return true;
        }

        /**
         * The result if the left operand is greater than the right.
         *
         * @return  true
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
         * Test for equality.  Return true only if the other object is a
         * <code>GreaterEqual</code> operation with equal operands, or a
         * <code>LessEqual</code> with equal operands reversed.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof GreaterEqual && super.equals(o))
                return true;
            if (!(o instanceof LessEqual))
                return false;
            LessEqual op = (LessEqual)o;
            return getLeft().equals(op.getRight()) &&
                            getRight().equals(op.getLeft());
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
         * The result if the left operand is less than the right.
         *
         * @return  true
         */
        @Override
        public boolean less() {
            return true;
        }

        /**
         * The result if the operands are equal.
         *
         * @return  true
         */
        @Override
        public boolean equal() {
            return true;
        }

        /**
         * The result if the left operand is greater than the right.
         *
         * @return  false
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
         * Test for equality.  Return true only if the other object is a
         * <code>LessEqual</code> operation with equal operands, or a
         * <code>GreaterEqual</code> with equal operands reversed.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            if (o instanceof LessEqual && super.equals(o))
                return true;
            if (!(o instanceof GreaterEqual))
                return false;
            GreaterEqual op = (GreaterEqual)o;
            return getLeft().equals(op.getRight()) &&
                            getRight().equals(op.getLeft());
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
         * Return true - the operation is commutative.
         *
         * @return true
         */
        @Override
        public boolean isCommutative() {
            return true;
        }

        /**
         * Get the result type of this operator.  The result type of an
         * equality operation is always <code>{@link Boolean}</code>.
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
         * @return  true if the operands are equal
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
         * Evaluate the subexpression.  This method delegates the bulk of the
         * work to the <code>compare()</code> method.
         *
         * @return  the boolean object resulting from the comparison operation
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            return compare() ? Boolean.TRUE : Boolean.FALSE;
        }

        /**
         * Optimize the subexpression.  Optimize the operands, and if both are
         * constant, execute the operation.
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
         * Test for equality.  Return true only if the other object is an
         * <code>Equal</code> operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
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
         * Evaluate the subexpression.  This method delegates the bulk of the
         * work to the <code>compare()</code> method.
         *
         * @return  the boolean object resulting from the comparison operation
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            return compare() ? Boolean.FALSE : Boolean.TRUE;
        }

        /**
         * Optimize the subexpression.  Optimize the operands, and if both are
         * constant, execute the operation.
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
         * Test for equality.  Return true only if the other object is a
         * <code>NotEqual</code> operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof NotEqual && super.equals(o);
        }

    }

    /**
     * A class to represent the "wildcard pattern match" operation.  This is an
     * extension to the JSTL 1.0 EL specification.
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
         * Get the result type of this operator.  The result type of a match
         * operation is always <code>{@link Boolean}</code>.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Boolean.class;
        }

        /**
         * Evaluate the subexpression.  Match the left operand string against
         * the right operand pattern.
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
         * Test for equality.  Return true only if the other object is a
         * <code>Match</code> operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
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
         * @return         true if the string matches the pattern
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
     * A class to represent the string concatenation (join) operation.  This is
     * an extension to the JSTL 1.0 EL specification.
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
         * Get the result type of this operator.  The result type of a join
         * operation is always <code>{@link String}</code>.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return String.class;
        }

        /**
         * Optimize the subexpression.  In addition to the default optimization,
         * check for join with null or zero-length string.
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
         * Evaluate the subexpression.  Concatenate the left and right operands
         * (as strings).
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
         * Test for equality.  Return true only if the other object is a
         * <code>Join</code> operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Join && super.equals(o);
        }

    }

    /**
     * A dummy operator to represent parentheses in an expression.  By making
     * the contents of the parentheses the right operand of the expression,
     * the operator precedence search is prevented from scanning the
     * sub-expression.
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
         * Get the result type of this operator.  The result type of this
         * operator is the result type of its right operand.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return getRight().getType();
        }

        /**
         * Evaluate the subexpression.  This just returns the evaluation of the
         * operand.
         *
         * @return  the result of the evaluation of the operand
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            return getRight().evaluate();
        }

        /**
         * Optimize the subexpression.  This method calls the parent class to
         * optimize the operand, then returns the operand, because the dummy
         * operator no longer has any value.
         *
         * @return  the operand
         */
        @Override
        public Expression optimize() {
            optimizeRightOperand();
            return getRight();
        }

        /**
         * Test for equality.  Return true only if the other object is a
         * <code>Parentheses</code> with an equal operand.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
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
            StringBuffer sb = new StringBuffer();
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
         * Get the result type of this operator.  The result type of an
         * arithmetic operation is always <code>{@link Number}</code>.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Number.class;
        }

        /**
         * Evaluate the subexpression.  Evaluate the operand and apply the
         * negate operation.
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
         * Test for equality.  Return true only if the other object is a
         * <code>Negate</code> operation with an equal operand.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
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
         * Get the result type of this operator.  The result type of a
         * logical operation is always <code>{@link Boolean}</code>.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Boolean.class;
        }

        /**
         * Evaluate the subexpression.  Evaluate the operand and return the
         * boolean object corresponding to its logical inversion.
         *
         * @return  the object resulting from the NOT operation
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            return getRight().asBoolean() ? Boolean.FALSE : Boolean.TRUE;
        }

        /**
         * Optimize the subexpression.  This method calls the parent class to
         * optimize the operand, and attempts to logically invert comparison,
         * equality and logical operations.  Then, if the operand is constant,
         * it attempts to apply the operation.  If this fails the optimization
         * is dropped.
         *
         * @return  the inverted operation, the constant result, or the operator
         *          itself
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
         * Test for equality.  Return true only if the other object is a
         * <code>Not</code> operation with an equal operand.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
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
         * Get the result type of this operator.  The result type of a test for
         * empty operation is always <code>{@link Boolean}</code>.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Boolean.class;
        }

        /**
         * Evaluate the subexpression.  Evaluate the operand and apply the
         * rules for testing for empty.
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
         * Test for equality.  Return true only if the other object is a
         * <code>Empty</code> operation with an equal operand.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Empty && super.equals(o);
        }

    }

    /**
     * A class to represent the 'convert to upper case' operation.  This is an
     * extension to the JSTL 1.0 EL specification.
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
         * Get the result type of this operator.  The result type of a case
         * conversion operation is always <code>{@link String}</code>.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return String.class;
        }

        /**
         * Evaluate the subexpression.  Evaluate the operand as a
         * <code>String</code> and convert it to upper case.
         *
         * @return  the converted string
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            return getRight().asString().toUpperCase();
        }

        /**
         * Test for equality.  Return true only if the other object is a
         * <code>ToUpper</code> operation with an equal operand.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof ToUpper && super.equals(o);
        }

    }

    /**
     * A class to represent the 'convert to lower case' operation.  This is an
     * extension to the JSTL 1.0 EL specification.
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
         * Get the result type of this operator.  The result type of a case
         * conversion operation is always <code>{@link String}</code>.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return String.class;
        }

        /**
         * Evaluate the subexpression.  Evaluate the operand as a
         * <code>String</code> and convert it to lower case.
         *
         * @return  the converted string
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            return getRight().asString().toLowerCase();
        }

        /**
         * Test for equality.  Return true only if the other object is a
         * <code>ToUpper</code> operation with an equal operand.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof ToLower && super.equals(o);
        }

    }

    /**
     * A class to represent the <code>length</code> operation.  This is an
     * extension to the JSTL 1.0 EL specification.
     */
    public static class Length extends Operator {

        public static final String name = "length";
        public static final int priority = 7;

        /**
         * Construct a <code>length</code> operator with the given operand.
         *
         * @param operand  the right operand
         */
        public Length(Expression operand) {
            super(operand);
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
         * Get the result type of this operator.  The result type of a length
         * operation is always <code>{@link Integer}</code>.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Integer.class;
        }

        /**
         * Optimize the subexpression.  The operand is optimized, but no attempt
         * is made to optimize this operation.
         *
         * @return  the operator itself
         */
        @Override
        public Expression optimize() {
            optimizeRightOperand();
            return this;
        }

        /**
         * Evaluate the subexpression.  Evaluate the operand and, if it is a
         * <code>Map</code>, <code>List</code> or array, return the number of
         * elements.
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
         * Test for equality.  Return true only if the other object is a
         * <code>Length</code> operation with an equal operand.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
         * @see Object#equals(Object)
         */
        @Override
        public boolean equals(Object o) {
            return o instanceof Length && super.equals(o);
        }

    }

    /**
     * A class to represent the <code>sum</code> operation.  This is an
     * extension to the JSTL 1.0 EL specification.
     */
    public static class Sum extends Operator {

        public static final String name = "sum";
        public static final int priority = 7;

        /**
         * Construct a <code>sum</code> operator with the given operand.
         *
         * @param operand  the right operand
         */
        public Sum(Expression operand) {
            super(operand);
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
         * Get the result type of this operator.  The result type of a sum
         * operation is <code>{@link Number}</code>.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return Number.class;
        }

        /**
         * Optimize the subexpression.  The operand is optimized, but no attempt
         * is made to optimize this operation.
         *
         * @return  the operator itself
         */
        @Override
        public Expression optimize() {
            optimizeRightOperand();
            return this;
        }

        /**
         * Evaluate the subexpression.  Evaluate the operand and, if it is a
         * <code>Map</code>, <code>List</code> or array, return the sum of the
         * elements.
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
         * Calculate the sum.  The sum is returned as a <code>Long</code> if all
         * the values are integral, or a <code>Double</code> otherwise.
         *
         * @param i an <code>Iterator</code> over the array or <code>List</code>
         *          or <code>Map</code>
         * @return a <code>Long</code> or <code>Double</code> containing the sum
         * @throws ClassCastException if an element is not a <code>Number</code>
         *         or <code>String</code>
         * @throws NumberFormatException if a <code>String</code> element can
         *         not be converted to a number
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
         * Test for equality.  Return true only if the other object is a
         * <code>Sum</code> operation with an equal operand.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
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
         * Construct an <code>ArrayIterator</code> with the given array.
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
         * Return true if the iterator has more values.
         *
         * @return <code>true</code> if the iterator has more values
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
     * <code>parseSubstitution()</code> method.
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
         * Get the result type of this operator.  The result type of a
         * concatenation operation is always <code>{@link String}</code>.
         *
         * @return  the result type of the operator
         */
        @Override
        public Class<?> getType() {
            return String.class;
        }

        /**
         * Evaluate the subexpression.  Evaluate each element and add it to an
         * output string.
         *
         * @return  the string concatenation of all the elements
         * @throws  EvaluationException on any errors
         */
        @Override
        public Object evaluate() throws EvaluationException {
            StringBuffer sb = new StringBuffer();
            for (int i = 0; i < num; ++i)
                sb.append(array[i].asString());
            return sb.toString();
        }

        /**
         * Optimize the subexpression.  Each element is individually optimized,
         * constant empty strings are dropped and adjacent constant strings are
         * combined.
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
         * Get the result of the concatenation as a single expression.  If the
         * concatenation resulted from the parse of a string consisting of a
         * single expression, return that expression; otherwise return the
         * concatenation expression.
         *
         * @return the result of the concatenation as a single expression
         */
        public Expression singleExpression() {
            if (num == 1)
                return array[0];
            return this;
        }

        /**
         * Test for equality.  Return true only if the other object is a
         * <code>Concat</code> operation with equal operands.
         *
         * @param o  the object for comparison
         * @return   true if expressions are equal
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
     * A class to represent all exceptions in the expression system, both
     * parsing and evaluation exceptions.
     */
    public static class ExpressionException extends Exception {

        private static final long serialVersionUID = 1L;

        /**
         * Construct an <code>ExpressionException</code> with the given message.
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

        private static final long serialVersionUID = 1L;

        /**
         * Construct a <code>ParseException</code> with the given message.
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

        private static final long serialVersionUID = 1L;

        /**
         * Construct a <code>UnmatchedParenthesisException</code>.
         */
        public UnmatchedParenthesisException() {
            super("unmatched.parenthesis");
        }

    }

    /**
     * An exception class for unmatched brackets.
     */
    public static class UnmatchedBracketException extends ParseException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct a <code>UnmatchedBracketException</code>.
         */
        public UnmatchedBracketException() {
            super("unmatched.bracket");
        }

    }

    /**
     * An exception class for unmatched braces.
     */
    public static class UnmatchedBraceException extends ParseException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct a <code>UnmatchedBraceException</code>.
         */
        public UnmatchedBraceException() {
            super("unmatched.brace");
        }

    }

    /**
     * An exception class for undefined identifiers.
     */
    public static class IdentifierException extends ParseException {

        private static final long serialVersionUID = 1L;

        private String identifier;

        /**
         * Construct a <code>IdentifierException</code>.
         *
         * @param identifier  the identifier that was not recognized
         */
        public IdentifierException(String identifier) {
            super("identifier");
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

        private static final long serialVersionUID = 1L;

        /**
         * Construct a <code>ReservedWordException</code>.
         */
        public ReservedWordException() {
            super("reserved");
        }

    }

    /**
     * An exception class for the illegal use of the property
     * ('object.property') syntax.
     */
    public static class PropertyException extends ParseException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct a <code>PropertyException</code>.
         */
        public PropertyException() {
            super("property");
        }

    }

    /**
     * An exception class for illegal numbers.
     */
    public static class NumberException extends ParseException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct a <code>NumberException</code>.
         */
        public NumberException() {
            super("number");
        }

    }

    /**
     * An exception class for illegal strings.
     */
    public static class StringException extends ParseException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct a <code>StringException</code>.
         */
        public StringException() {
            super("string");
        }

    }

    /**
     * An exception class to indicate unparsed text at the end of an expression.
     */
    public static class UnparsedStringException extends ParseException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct an <code>UnparsedStringException</code>.
         */
        public UnparsedStringException() {
            super("unparsed");
        }

    }

    /**
     * An exception class for general syntax errors.
     */
    public static class UnexpectedElementException extends ParseException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct an <code>UnexpectedElementException</code>.
         */
        public UnexpectedElementException() {
            super("unexpected");
        }

    }

    /**
     * An exception class for unexpected end of string.
     */
    public static class UnexpectedEndException extends ParseException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct an <code>UnexpectedEndException</code>.
         */
        public UnexpectedEndException() {
            super("end");
        }

    }

    /**
     * An exception class for conditional expression errors.  These occur when
     * a conditional expression does not have a matching else expression (the
     * use of conditional expressions is an extension to the JSTL 1.0 EL
     * specification).
     */
    public static class ConditionalException extends ParseException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct a <code>ConditionalException</code>.
         */
        public ConditionalException() {
            super("conditional");
        }

    }

    /**
     * An exception class for else expression errors.  These occur when
     * an else expression appears without a matching conditional expression (the
     * use of conditional expressions is an extension to the JSTL 1.0 EL
     * specification).
     */
    public static class ElseException extends ParseException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct an <code>ElseException</code>.
         */
        public ElseException() {
            super("else");
        }

    }

    /**
     * A class to represent evaluation exceptions.
     */
    public static class EvaluationException extends ExpressionException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct an <code>EvaluationException</code> with the given message.
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

        private static final long serialVersionUID = 1L;

        /**
         * Construct a <code>CoercionException</code> for the given type.
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

        private static final long serialVersionUID = 1L;

        /**
         * Construct a <code>BooleanCoercionException</code>.
         */
        public BooleanCoercionException() {
            super("boolean");
        }

    }

    /**
     * An exception class for int coercion errors.
     */
    public static class IntCoercionException extends CoercionException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct an <code>IntCoercionException</code>.
         */
        public IntCoercionException() {
            super("int");
        }

    }

    /**
     * An exception class for long coercion errors.
     */
    public static class LongCoercionException extends CoercionException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct a <code>LongCoercionException</code>.
         */
        public LongCoercionException() {
            super("long");
        }

    }

    /**
     * An exception class for double coercion errors.
     */
    public static class DoubleCoercionException extends CoercionException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct a <code>DoubleCoercionException</code>.
         */
        public DoubleCoercionException() {
            super("double");
        }

    }

    /**
     * An exception class for string coercion errors.
     */
    public static class StringCoercionException extends CoercionException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct a <code>StringCoercionException</code>.
         */
        public StringCoercionException() {
            super("string");
        }

    }

    /**
     * An exception class for indexing errors.
     */
    public static class IndexException extends EvaluationException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct an <code>IndexException</code>.
         */
        public IndexException() {
            super("index");
        }

    }

    /**
     * An exception class for assignment errors.
     */
    public static class AssignException extends EvaluationException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct an <code>AssignException</code>.
         */
        public AssignException() {
            super("assign");
        }

    }

    /**
     * An exception class for length operation errors.
     */
    public static class LengthException extends EvaluationException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct an <code>LengthException</code>.
         */
        public LengthException() {
            super("length");
        }

    }

    /**
     * An exception class for sum operation errors.
     */
    public static class SumException extends EvaluationException {

        private static final long serialVersionUID = 1L;

        /**
         * Construct an <code>SumException</code>.
         */
        public SumException() {
            super("sum");
        }

    }

    /**
     * Inner interface representing an object which will resolve names for the
     * expression parser.
     */
    public static interface Resolver {

        /**
         * Resolve an identifier.  The result may be any type of
         * <code>Expression</code>, but it will usually be a
         * <code>Variable</code> or a <code>Constant</code>.
         *
         * @param identifier  the identifier to be resolved
         * @return            an expression, or null if the name can not be
         *                    resolved
         */
        Expression resolve(String identifier);

    }

}
