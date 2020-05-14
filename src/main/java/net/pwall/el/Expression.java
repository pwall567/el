/*
 * @(#) Expression.java
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
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Expression evaluation system for the JSTL Expression Language (EL).  This
 * class is the base class for the various expression elements, and it also
 * contains static methods to simplify access to the parser.
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
 * explicitly enabled):</p>
 *
 * <ul>
 *   <li>Conditional expressions ( <code>? :</code> )</li>
 *   <li>Assignment operations</li>
 *   <li><code>toupper</code> and <code>tolower</code> conversions</li>
 *   <li>Wildcard match operation</li>
 *   <li>String concatenation (join) operation</li>
 *   <li>Array/list <code>length</code> and <code>sum</code> operations</li>
 * </ul>
 *
 * <p>There are many ways to use the system.  The simplest is to use the static
 * methods of this class:</p>
 * <pre>
 *     Expression exp = Expression.parseExpression("4+3*2", null);
 *     Object result = exp.evaluate(); // result will contain Integer(10)
 * </pre>
 * <p>There is also a convenience method to perform substitution of
 * <code>$&#123;...&#125;</code> sequences in a string:</p>
 * <pre>
 *     String result = Expression.substitute("4+3*2=${4+3*2}", null);
 *     // result will contain "4+3*2=10"
 * </pre>
 * <p>More complex uses of the system, for example conditional expressions, may
 * require a {@link Parser} to be instantiated:</p>
 * <pre>
 *     Expression.Parser parser = new Expression.Parser();
 *     parser.setConditionalAllowed(true);
 *     Expression exp = Expression.parseExpression("4 &gt; 3 ? 'a' : 'b'", null);
 *     Object result = exp.evaluate(); // result will contain 'a'
 * </pre>
 * <p>This is only a brief introduction to the facilities of the system; see the
 * documentation for the methods of this class and its children for more
 * information.</p>
 *
 * @author  Peter Wall
 */
public abstract class Expression {

    private static final Parser defaultParser = new Parser();
    private static final Parser restrictedParser = new Parser();

    static {
        defaultParser.setArrayOperationAllowed(true);
        defaultParser.setAssignAllowed(true);
        defaultParser.setCaseConvertAllowed(true);
        defaultParser.setConditionalAllowed(true);
        defaultParser.setJoinAllowed(true);
        defaultParser.setMatchAllowed(true);
    }

    /**
     * Get the default {@link Parser}.
     *
     * @return      the default {@link Parser}
     */
    public static Parser getDefaultParser() {
        return defaultParser;
    }

    /**
     * Get the "restricted" {@link Parser} (this has none of the extensions enabled).
     *
     * @return      the default {@link Parser}
     */
    public static Parser getRestrictedParser() {
        return restrictedParser;
    }

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
            return (Boolean)object;
        if (object instanceof String) {
            String string = (String)object;
            if (string.length() == 0)
                return false;
            try {
                return Boolean.parseBoolean(string);
            }
            catch (Exception ignored) {
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
            return (Character)object;
        if (object instanceof Number)
            return ((Number)object).intValue();
        if (object instanceof String) {
            String string = (String)object;
            if (string.length() == 0)
                return 0;
            try {
                return Integer.parseInt(string, 10);
            }
            catch (Exception ignored) {
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
            return (Character)object;
        if (object instanceof Number)
            return ((Number)object).longValue();
        if (object instanceof String) {
            String string = (String)object;
            if (string.length() == 0)
                return 0;
            try {
                return Long.parseLong(string, 10);
            }
            catch (Exception ignored) {
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
            return (Character)object;
        if (object instanceof Number)
            return ((Number)object).doubleValue();
        if (object instanceof String) {
            String string = (String)object;
            if (string.length() == 0)
                return 0;
            try {
                return Double.parseDouble(string);
            }
            catch (Exception ignored) {
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
            return Constant.nullString;
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
            //noinspection ConstantConditions
            return method.invoke(left, (Object[])null);
        }
        catch (Exception ignored) { // includes NPE on method
        }
        sb.setLength(0);
        sb.append("is");
        try {
            sb.append(Character.toUpperCase(rightString.charAt(0)));
            if (rightString.length() > 1)
                sb.append(rightString.substring(1));
            Method method = findPublicGetter(leftClass, sb.toString());
            //noinspection ConstantConditions
            return method.invoke(left, (Object[])null);
        }
        catch (Exception e) { // includes NPE on method
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
            //noinspection ConstantConditions
            method.invoke(left, value);
        }
        catch (Exception e) { // includes NPE on method
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
            return string.indexOf('.') >= 0 || string.indexOf('e') >= 0 || string.indexOf('E') >= 0;
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
        Objects.requireNonNull(cls);
        for (Class<?> c = cls; c != null; c = c.getSuperclass()) {
            if (Modifier.isPublic(c.getModifiers())) {
                try {
                    Method method = c.getMethod(name, (Class<?>[])null);
                    if (Modifier.isPublic(method.getModifiers()))
                        return method;
                }
                catch (SecurityException | NoSuchMethodException ignored) {
                }
            }
        }
        Class<?>[] ifaces = cls.getInterfaces();
        for (Class<?> iface : ifaces) {
            Method method = findPublicGetter(iface, name);
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
    private static Method findPublicSetter(Class<?> leftClass, String name, Class<?> valueClass) {
        Objects.requireNonNull(leftClass);
        for (Class<?> lc = leftClass; lc != null; lc = lc.getSuperclass()) {
            if (Modifier.isPublic(lc.getModifiers())) {
                try {
                    Method[] methods = lc.getMethods();
                    for (Method method : methods) {
                        if (method.getName().equals(name) &&
                                Modifier.isPublic(method.getModifiers())) {
                            Class<?>[] params = method.getParameterTypes();
                            if (params.length == 1 && params[0].isAssignableFrom(valueClass))
                                return method;
                        }
                    }
                }
                catch (SecurityException ignored) {
                }
            }
        }
        Class<?>[] ifaces = leftClass.getInterfaces();
        for (Class<?> iface : ifaces) {
            Method method = findPublicSetter(iface, name, valueClass);
            if (method != null)
                return method;
        }
        return null;
    }

}
