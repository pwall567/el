/*
 * @(#) Parser.java
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

import net.pwall.util.ParseText;

/**
 * The expression parser.  There is a static instance of this class in the
 * {@code Expression} class which will translate the standard form of the Expression
 * Language; non-standard uses (e.g. the use of the assign operator) will require a specific
 * parser to be instantiated and customized before use.
 *
 * @author  Peter Wall
 */
public class Parser {

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
     * @param text      an {@link ELParseText} object containing the text to parse
     *                  (not including ${ })
     * @param resolver  a {@link Resolver} object to resolve names in
     *                  the expression
     * @return          the {@code Expression} tree
     * @throws          ParseException on any errors
     */
    public Expression parseExpression(ELParseText text, Resolver resolver)
            throws ParseException {
        Operator expression = new ParenthesesOperator(null);
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
                    Operator op = new NegateOperator(null);
                    current.setRight(op);
                    current = op;
                    continue;
                }
            }
            if (text.match('!') || text.matchName("not")) {
                Operator op = new NotOperator(null);
                current.setRight(op);
                current = op;
                continue;
            }
            if (text.matchName("empty")) {
                Operator op = new EmptyOperator(null);
                current.setRight(op);
                current = op;
                continue;
            }
            if (isCaseConvertAllowed() && text.matchName("toupper")) {
                Operator op = new ToUpperOperator(null);
                current.setRight(op);
                current = op;
                continue;
            }
            if (isCaseConvertAllowed() && text.matchName("tolower")) {
                Operator op = new ToLowerOperator(null);
                current.setRight(op);
                current = op;
                continue;
            }
            if (isArrayOperationAllowed() && text.matchName("length")) {
                Operator op = new LengthOperator(null);
                current.setRight(op);
                current = op;
                continue;
            }
            if (isArrayOperationAllowed() && text.matchName("sum")) {
                Operator op = new SumOperator(null);
                current.setRight(op);
                current = op;
                continue;
            }
            // check for parentheses and a subexpression (recursion)
            if (text.match('(')) {
                Expression nested = parseExpression(text, resolver);
                if (!text.match(')'))
                    throw new UnmatchedParenthesisException();
                if (!(nested instanceof ParenthesesOperator))
                    nested = new ParenthesesOperator(nested);
                current.setRight(nested);
            }
            else if (text.match('[')) {
                // array
                ArrayCreateOperator arrayCreate = new ArrayCreateOperator();
                if (!text.skipSpaces().match(']')) {
                    for (;;) {
                        Expression nested = parseExpression(text, resolver);
                        arrayCreate.addItem(nested);
                        if (text.skipSpaces().isExhausted())
                            throw new UnexpectedEndException();
                        if (text.match(']'))
                            break;
                        if (!text.match(','))
                            throw new UnexpectedElementException();
                    }
                }
                current.setRight(arrayCreate);
            }
            else if (text.match('{')) {
                // object
                ObjectCreateOperator objectCreate = new ObjectCreateOperator();
                if (!text.skipSpaces().match('}')) {
                    for (;;) {
                        if (text.isExhausted())
                            throw new UnexpectedEndException();
                        String identifier;
                        if (text.matchName())
                            identifier = text.getResultString();
                        else if (text.matchStringLiteral())
                            identifier = text.getResultStringLiteral();
                        else
                            throw new UnexpectedElementException();
                        if (text.skipSpaces().isExhausted())
                            throw new UnexpectedEndException();
                        if (!text.match(':'))
                            throw new UnexpectedElementException();
                        if (text.skipSpaces().isExhausted())
                            throw new UnexpectedEndException();
                        Expression nested = parseExpression(text, resolver);
                        objectCreate.addItem(identifier, nested);
                        if (text.skipSpaces().isExhausted())
                            throw new UnexpectedEndException();
                        if (text.match('}'))
                            break;
                        if (!text.match(','))
                            throw new UnexpectedElementException();
                        text.skipSpaces();
                    }
                }
                current.setRight(objectCreate);
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
                current.setRight(Constant.trueConstant);
            }
            else if (text.matchName("false")) {
                // boolean literal
                current.setRight(Constant.falseConstant);
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
                    Object functionImpl = extResolver.resolveNamespace(namespace);
                    if (functionImpl == null)
                        throw new FunctionParseException();
                    FunctionCallOperator functionCall =
                            new FunctionCallOperator(functionImpl, text.getResultString());
                    text.skipSpaces();
                    if (!text.match('('))
                        throw new FunctionParseException();
                    text.skipSpaces();
                    if (!text.match(')')) {
                        for (;;) {
                            Expression nested = parseExpression(text, resolver);
                            functionCall.addArgument(nested);
                            if (text.skipSpaces().isExhausted())
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
            while (!text.isExhausted()) {
                if (text.match('.')) {
                    // . property
                    text.skipSpaces();
                    if (!text.matchName())
                        throw new PropertyException();
                    IndexedOperator indexed = new IndexedOperator(currentRight,
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
                    IndexedOperator indexed = new IndexedOperator(currentRight, nested);
                    current.setRight(indexed);
                    currentRight = indexed;
                    text.skipSpaces();
                }
                else
                    break;
            }
            if (text.isExhausted())
                break;
            // now check for a diadic operator (like + or ==)
            DiadicOperator diadic;
            if (text.matchName("and") || text.match("&&"))
                diadic = new AndOperator(null, null);
            else if (text.matchName("or") || text.match("||"))
                diadic = new OrOperator(null, null);
            else if (text.match('+'))
                diadic = new PlusOperator(null, null);
            else if (text.match('-'))
                diadic = new MinusOperator(null, null);
            else if (text.match('*'))
                diadic = new MultiplyOperator(null, null);
            else if (text.match('/') || text.matchName("div"))
                diadic = new DivideOperator(null, null);
            else if (text.match('%') || text.matchName("mod"))
                diadic = new ModuloOperator(null, null);
            else if (text.match(">=") || text.matchName("ge"))
                diadic = new GreaterOrEqualOperator(null, null);
            else if (text.match("<=") || text.matchName("le"))
                diadic = new LessOrEqualOperator(null, null);
            else if (text.match('>') || text.matchName("gt"))
                diadic = new GreaterThanOperator(null, null);
            else if (text.match('<') || text.matchName("lt"))
                diadic = new LessThanOperator(null, null);
            else if (text.match("==") || text.matchName("eq"))
                diadic = new EqualOperator(null, null);
            else if (text.match("!=") || text.matchName("ne"))
                diadic = new NotEqualOperator(null, null);
            else if (isConditionalAllowed() && text.match('?'))
                diadic = new ConditionalOperator(null, null);
            else if (isConditionalAllowed() && text.match(':'))
                diadic = new ElseOperator(null, null);
            else if (isAssignAllowed() && text.match('='))
                diadic = new AssignOperator(null, null);
            else if (isMatchAllowed() && text.match("~="))
                diadic = new MatchOperator(null, null);
            else if (isJoinAllowed() && text.match('#'))
                diadic = new JoinOperator(null, null);
            else
                break;
            // find the right point in the expression to add operator
            // (based on priority and left-associativity)
            int prio = diadic.getPriority();
            Operator x = expression;
            for (;;) {
                Expression right = x.getRight();
                if (!(right instanceof DiadicOperator))
                    break;
                DiadicOperator rightOp = (DiadicOperator)right;
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
     * @throws      ParseException on any errors
     */
    private void checkConditional(Expression expr) throws ParseException {
        if (expr instanceof ConditionalOperator) {
            ConditionalOperator cond = (ConditionalOperator)expr;
            checkConditional(cond.getLeft());
            Expression right = cond.getRight();
            if (!(right instanceof ElseOperator))
                throw new ConditionalException();
            ElseOperator elseExpr = (ElseOperator)right;
            checkConditional(elseExpr.getLeft());
            checkConditional(elseExpr.getRight());
        }
        else if (expr instanceof ElseOperator) {
            throw new ElseException();
        }
        else if (expr instanceof DiadicOperator) {
            DiadicOperator diadic = (DiadicOperator)expr;
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
     * @throws          ParseException on any errors
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
     * @throws          ExpressionException on any errors
     */
    public String substitute(String source, Resolver resolver) throws ExpressionException {
        ELParseText text = new ELParseText(source);
        text.skipTo("${");
        if (text.isExhausted())
            return source;
        StringBuilder sb;
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
     * @throws          ParseException on any errors
     */
    public Expression parseSubstitution(CharSequence str, Resolver resolver)
            throws ParseException {
        ConcatOperator concat = new ConcatOperator();
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
     * @param text      an {@link ELParseText} object containing the text to parse
     *                  (not including ${ })
     * @return          the {@code Expression} tree
     * @throws          ParseException on any errors
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
     * @throws          ParseException on any errors
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
     * @throws          ExpressionException on any errors
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
     * @throws          ParseException on any errors
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
            return Expression.isIdentifierStart(ch);
        }

        @Override
        public boolean isNameContinuation(char ch) {
            return Expression.isIdentifierPart(ch);
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
                return (int)i;
            return i;
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
