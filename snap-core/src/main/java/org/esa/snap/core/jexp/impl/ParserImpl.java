/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.core.jexp.impl;

import org.esa.snap.core.jexp.Function;
import org.esa.snap.core.jexp.Namespace;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Parser;
import org.esa.snap.core.jexp.Symbol;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.WritableNamespace;


/**
 * A default implementation for the {@link org.esa.snap.core.jexp.Parser} interface.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public final class ParserImpl implements Parser {

    /**
     * The environment used to resolve names.
     */
    private Namespace defaultNamespace;

    /**
     * The tokenizer used by this parser.
     */
    private Tokenizer tokenizer;

    /**
     * If true, the parser performs type checking on expressions.
     */
    private boolean typeChecking;


    /**
     * Constructs a new parser instance which uses a {@link DefaultNamespace} and type-checking enabled.
     */
    public ParserImpl() {
        this(new DefaultNamespace(), true);
    }

    /**
     * Constructs a new parser instance which uses a {@link DefaultNamespace} and the given type-checking setting.
     *
     * @param typeChecking if true, the parser performs strong type-checking on expressions
     */
    public ParserImpl(boolean typeChecking) {
        this(new DefaultNamespace(), typeChecking);
    }

    /**
     * Constructs a new parser instance which uses the given namespace and type-checking enabled.
     *
     * @param namespace the environment used to resolve names.
     */
    public ParserImpl(final Namespace namespace) {
        this(namespace, true);
    }

    /**
     * Constructs a new parser instance which uses the given namespace and the given type-checking setting.
     *
     * @param namespace    the environment used to resolve names.
     * @param typeChecking if true, the parser performs type checking on expressions
     */
    public ParserImpl(final Namespace namespace, boolean typeChecking) {
        defaultNamespace = namespace;
        this.typeChecking = typeChecking;
        tokenizer = null;
    }

    /**
     * Gets this parser's default namespace.
     *
     * @return the default environment used to resolve names.
     */
    public final Namespace getDefaultNamespace() {
        return defaultNamespace;
    }

    /**
     * Gets whether or not this parser performs type checking on expressions.
     *
     * @return if true, the parser performs type checking on expressions
     */
    public boolean isTypeChecking() {
        return typeChecking;
    }

    /**
     * Parses the expression given in the code string.
     * Names in the code string are resolved using the default namespace.
     *
     * @param code the code string, for the syntax of valid expressions refer
     *             to the class description
     * @throws ParseException if a parse reportError occurs
     */
    public final Term parse(final String code) throws ParseException {
        return parse(code, defaultNamespace);
    }

    /**
     * Parses the expression given in the code string.
     * Names in the code string are resolved using the given namespace.
     *
     * @param code      the code string, for the syntax of valid expressions refer
     *                  to the class description
     * @param namespace the environment which is used to resolve names
     * @throws ParseException if a parse error occurs
     */
    public final Term parse(final String code, final Namespace namespace) throws ParseException {
        if (code == null) {
            throw new IllegalArgumentException("code is null");
        }
        Namespace defaultNamespace = this.defaultNamespace;
        if (namespace != null && namespace != defaultNamespace) {
            this.defaultNamespace = new NamespaceImpl(namespace);
        }
        tokenizer = new Tokenizer(code);
        Term term = parseImpl();
        tokenizer = null;
        this.defaultNamespace = defaultNamespace;
        return term;
    }

    /**
     * Implements the <code>parse</code> method. Calls <code>parseTerm(false)</code>
     * and throws an exception if the next token is not the end-of-string.
     *
     * @return The generated term.
     * @throws ParseException if a parse error occurs
     */
    private Term parseImpl() throws ParseException {
        final Term expr = parseTerm(false);
        final int tt = tokenizer.next();
        if (tt != Tokenizer.TT_EOS) {
            reportError("Incomplete expression."); /*I18N*/
        }
        return expr;
    }

    /**
     * Parses a complete expression jex.term. Simply a wrapper for
     * a <code>parseAssign</code> method in order to signal that the assignment
     * operator '=' has the highest operator precedence.
     *
     * @param required true, if the expression is required.
     * @return The generated term.
     * @throws ParseException if a parse error occurs
     */
    private Term parseTerm(final boolean required) throws ParseException {
        return parseAssign(required);
    }

    /**
     * Parses an assignment expression <i>x '=' y</i>.
     *
     * @param required true, if the expression is required.
     * @return The generated term.
     * @throws ParseException if a parse error occurs
     */
    private Term parseAssign(final boolean required) throws ParseException {
        Term t1 = parseConditional(required);
        while (t1 != null) {
            int tt = tokenizer.next();
            if (tt == '=') {
                Term t2 = parseAssign(true);
                if (t1 instanceof Term.Ref && ((Term.Ref) t1).getVariable() != null) {
                    t1 = new Term.Assign(t1, t2);
                } else {
                    reportError("Variable expected on the left side of assignment '='.");
                }
            } else {
                tokenizer.pushBack();
                break;
            }
        }
        return t1;
    }

    /**
     * Parses a conditional expression (not implemented).
     *
     * @param required true, if the expression is required.
     * @return The generated term.
     * @throws ParseException if a parse error occurs
     */
    private Term parseConditional(final boolean required) throws ParseException {
        tokenizer.next();
        if (isKeyword("if")) {
            Term t1 = parseTerm(true);
            tokenizer.next();
            if (isKeyword("then")) {
                Term t2 = parseTerm(true);
                tokenizer.next();
                if (isKeyword("else")) {
                    if (isTypeChecking() && !t1.isB()) {
                        reportError("Boolean operand expected after 'if' in conditional term.");
                    }
                    Term t3 = parseTerm(true);
                    return createConditionTerm(t1, t2, t3);
                } else {
                    tokenizer.pushBack();
                    reportError("Missing 'else' in a conditional 'if' term.");
                }
            } else {
                tokenizer.pushBack();
                reportError("Missing 'then' in a conditional 'if' term.");
            }
        } else {
            tokenizer.pushBack();
        }

        Term t1 = parseLogicalOr(required);
        int tt = tokenizer.next();
        if (tt == '?') {
            Term t2 = parseTerm(true);
            tt = tokenizer.next();
            if (tt == ':') {
                if (isTypeChecking() && !t1.isB()) {
                    reportError("Boolean operand expected before '?' in conditional term.");
                }
                Term t3 = parseTerm(true);
                t1 = createConditionTerm(t1, t2, t3);
            } else {
                tokenizer.pushBack();
                reportError("Missing ':' part of conditional '?:' term.");
            }
        } else {
            tokenizer.pushBack();
        }
        return t1;
    }

    /**
     * Parses a logical OR expression <i>x '||' y</i>.
     *
     * @param required true, if the expression is required.
     * @return The generated term.
     * @throws ParseException if a parse error occurs
     */
    private Term parseLogicalOr(final boolean required) throws ParseException {
        Term t1 = parseLogicalAnd(required);
        while (t1 != null) {
            /*int tt =*/
            tokenizer.next();
            if (isSpecial("||") || isKeyword("or")) {
                Term t2 = parseLogicalAnd(true);
                if ((isB(t1, t2)) || !isTypeChecking()) {
                    t1 = new Term.OrB(t1, t2);
                } else {
                    reportTypeErrorB2("'||' or 'or'");
                }
            } else {
                tokenizer.pushBack();
                break;
            }
        }
        return t1;
    }

    /**
     * Parses a logical AND expression <i>x '&&' y</i>.
     *
     * @param required true, if the expression is required.
     * @return The generated term.
     * @throws ParseException if a parse error occurs
     */
    private Term parseLogicalAnd(final boolean required) throws ParseException {
        Term t1 = parseComparison(required);
        while (t1 != null) {
            /*int tt =*/
            tokenizer.next();
            if (isSpecial("&&") || isKeyword("and")) {
                Term t2 = parseComparison(true);
                if ((isB(t1, t2)) || !isTypeChecking()) {
                    t1 = new Term.AndB(t1, t2);
                } else {
                    reportTypeErrorB2("'&&' or 'and'");
                }
            } else {
                tokenizer.pushBack();
                break;
            }
        }
        return t1;
    }

    /**
     * Parses a Comparison expression
     * <i>x '==' y</i>,
     * <i>x '!=' y</i>,
     * <i>x '<' y</i>,
     * <i>x '<=' y</i>.
     * <i>x '>' y</i>,
     * <i>x '>=' y</i>.
     *
     * @param required true, if the expression is required.
     * @return The generated term.
     * @throws ParseException if a parse error occurs
     */
    private Term parseComparison(final boolean required) throws ParseException {
        Term t1 = parseBitwiseOr(required);
        while (t1 != null) {
            int tt = tokenizer.next();
            if (tt == '<') {
                Term t2 = parseBitwiseOr(true);
                if (isD(t1, t2)) {
                    t1 = new Term.LtD(t1, t2);
                } else if (isI(t1, t2)) {
                    t1 = new Term.LtI(t1, t2);
                } else if (!isTypeChecking()) {
                    t1 = new Term.LtD(t1, t2);
                } else {
                    reportTypeErrorN2("'<'");
                }
            } else if (tt == '>') {
                Term t2 = parseBitwiseOr(true);
                if (isD(t1, t2)) {
                    t1 = new Term.GtD(t1, t2);
                } else if (isI(t1, t2)) {
                    t1 = new Term.GtI(t1, t2);
                } else if (!isTypeChecking()) {
                    t1 = new Term.GtD(t1, t2);
                } else {
                    reportTypeErrorN2("'>'");
                }
            } else if (isSpecial("==")) {
                Term t2 = parseBitwiseOr(true);
                if (isD(t1, t2)) {
                    t1 = new Term.EqD(t1, t2);
                } else if (isB(t1, t2)) {
                    t1 = new Term.EqB(t1, t2);
                } else if ((t1.isI() || t1.isB()) && (t2.isI() || t2.isB())) {
                    t1 = new Term.EqI(t1, t2);
                } else if (!isTypeChecking()) {
                    t1 = new Term.EqD(t1, t2);
                } else {
                    reportTypeErrorN2("'=='");
                }
            } else if (isSpecial("!=")) {
                Term t2 = parseBitwiseOr(true);
                if (isD(t1, t2)) {
                    t1 = new Term.NEqD(t1, t2);
                } else if (isB(t1, t2)) {
                    t1 = new Term.NEqB(t1, t2);
                } else if ((t1.isI() || t1.isB()) && (t2.isI() || t2.isB())) {
                    t1 = new Term.NEqI(t1, t2);
                } else if (!isTypeChecking()) {
                    t1 = new Term.NEqD(t1, t2);
                } else {
                    reportTypeErrorN2("'!='");
                }
            } else if (isSpecial("<=")) {
                Term t2 = parseBitwiseOr(true);
                if (isD(t1, t2)) {
                    t1 = new Term.LeD(t1, t2);
                } else if (isI(t1, t2)) {
                    t1 = new Term.LeI(t1, t2);
                } else if (!isTypeChecking()) {
                    t1 = new Term.LeD(t1, t2);
                } else {
                    reportTypeErrorN2("'<='");
                }
            } else if (isSpecial(">=")) {
                Term t2 = parseBitwiseOr(true);
                if (isD(t1, t2)) {
                    t1 = new Term.GeD(t1, t2);
                } else if (isI(t1, t2)) {
                    t1 = new Term.GeI(t1, t2);
                } else if (!isTypeChecking()) {
                    t1 = new Term.GeD(t1, t2);
                } else {
                    reportTypeErrorN2("'>='");
                }
            } else {
                tokenizer.pushBack();
                break;
            }
        }
        return t1;
    }

    /**
     * Parses a bitwise OR expression <i>x '|' y</i>.
     *
     * @param required true, if the expression is required.
     * @return The generated term.
     * @throws ParseException if a parse error occurs
     */
    private Term parseBitwiseOr(final boolean required) throws ParseException {
        Term t1 = parseBitwiseXOr(required);
        while (t1 != null) {
            int tt = tokenizer.next();
            if (tt == '|') {
                Term t2 = parseBitwiseXOr(true);
                if ((isI(t1, t2)) || !isTypeChecking()) {
                    t1 = new Term.OrI(t1, t2);
                } else {
                    reportTypeErrorI2("'|'");
                }
            } else {
                tokenizer.pushBack();
                break;
            }
        }
        return t1;
    }

    /**
     * Parses a bitwise XOR expression <i>x '^' y</i>.
     *
     * @param required true, if the expression is required.
     * @return The generated term.
     * @throws ParseException if a parse error occurs
     */
    private Term parseBitwiseXOr(final boolean required) throws ParseException {
        Term t1 = parseBitwiseAnd(required);
        while (t1 != null) {
            int tt = tokenizer.next();
            if (tt == '^') {
                Term t2 = parseBitwiseAnd(true);
                if ((isI(t1, t2)) || !isTypeChecking()) {
                    t1 = new Term.XOrI(t1, t2);
                } else {
                    reportTypeErrorI2("'^'");
                }
            } else {
                tokenizer.pushBack();
                break;
            }
        }
        return t1;
    }


    /**
     * Parses a bitwise AND expression <i>x '&' y</i>.
     *
     * @param required true, if the expression is required.
     * @return The generated term.
     * @throws ParseException if a parse error occurs
     */
    private Term parseBitwiseAnd(final boolean required) throws ParseException {
        Term t1 = parseAdd(required);
        while (t1 != null) {
            int tt = tokenizer.next();
            if (tt == '&') {
                Term t2 = parseAdd(true);
                if ((isI(t1, t2)) || !isTypeChecking()) {
                    t1 = new Term.AndI(t1, t2);
                } else {
                    reportTypeErrorI2("'&'");
                }
            } else {
                tokenizer.pushBack();
                break;
            }
        }
        return t1;
    }

    /**
     * Parses an additive expression <i>x '+' y</i> or <i>x '-' y</i>.
     *
     * @param required true, if the expression is required.
     * @return The generated term.
     * @throws ParseException if a parse error occurs
     */
    private Term parseAdd(final boolean required) throws ParseException {
        Term t1 = parseMul(required);
        while (t1 != null) {
            int tt = tokenizer.next();
            if (tt == '+') {
                Term t2 = parseMul(true);
                if (isD(t1, t2)) {
                    t1 = new Term.Add(Term.TYPE_D, t1, t2);
                } else if (isI(t1, t2)) {
                    t1 = new Term.Add(Term.TYPE_I, t1, t2);
                } else if (!isTypeChecking()) {
                    t1 = new Term.Add(t1, t2);
                } else {
                    reportTypeErrorN2("'+'");
                }
            } else if (tt == '-') {
                Term t2 = parseMul(true);
                if (isD(t1, t2)) {
                    t1 = new Term.Sub(Term.TYPE_D, t1, t2);
                } else if (isI(t1, t2)) {
                    t1 = new Term.Sub(Term.TYPE_I, t1, t2);
                } else if (!isTypeChecking()) {
                    t1 = new Term.Sub(t1, t2);
                } else {
                    reportTypeErrorN2("'-'");
                }
            } else {
                tokenizer.pushBack();
                break;
            }
        }
        return t1;
    }


    /**
     * Parses a multiplicative expression <i>x '*' y</i>, <i>x '/' y</i>
     * or <i>x '%' y</i> (modulo).
     *
     * @param required true, if the expression is required.
     * @return The generated term.
     * @throws ParseException if a parse error occurs
     */
    private Term parseMul(final boolean required) throws ParseException {
        Term t1 = parseUnary(required);
        while (t1 != null) {
            int tt = tokenizer.next();
            if (tt == '*') {
                Term t2 = parseUnary(true);
                if (isD(t1, t2)) {
                    t1 = new Term.Mul(Term.TYPE_D, t1, t2);
                } else if (isI(t1, t2)) {
                    t1 = new Term.Mul(Term.TYPE_I, t1, t2);
                } else if (!isTypeChecking()) {
                    t1 = new Term.Mul(t1, t2);
                } else {
                    reportTypeErrorN2("'*'");
                }
            } else if (tt == '/') {
                Term t2 = parseUnary(true);
                if (isD(t1, t2)) {
                    t1 = new Term.Div(Term.TYPE_D, t1, t2);
                } else if (isI(t1, t2)) {
                    t1 = new Term.Div(Term.TYPE_I, t1, t2);
                } else if (!isTypeChecking()) {
                    t1 = new Term.Div(t1, t2);
                } else {
                    reportTypeErrorN2("'/'");
                }
            } else if (tt == '%') {
                Term t2 = parseUnary(true);
                if (isD(t1, t2)) {
                    t1 = new Term.Mod(Term.TYPE_D, t1, t2);
                } else if (isI(t1, t2)) {
                    t1 = new Term.Mod(Term.TYPE_I, t1, t2);
                } else if (!isTypeChecking()) {
                    t1 = new Term.Mod(t1, t2);
                } else {
                    reportTypeErrorN2("'%'");
                }
            } else {
                tokenizer.pushBack();
                break;
            }
        }
        return t1;
    }

    /**
     * Parses an unary expression <i>'+' x</i>, <i>'-' x</i>, <i>'!' x</i>, <i>'~' x</i>.
     *
     * @param required true, if the expression is required.
     * @return The generated term.
     * @throws ParseException if a parse error occurs
     */
    private Term parseUnary(final boolean required) throws ParseException {
        Term t1 = null;
        final int tt = tokenizer.next();
        if (tt == '+') {
            Term t2 = parseUnary(true);
            if (t2.isI() || t2.isD()) {
                t1 = t2;
            } else if (!isTypeChecking()) {
                t1 = t2;
            } else {
                reportTypeErrorN1("'+'");
            }
        } else if (tt == '-') {
            Term t2 = parseUnary(true);
            if (t2 instanceof Term.ConstI) {
                t1 = new Term.ConstI(-((Term.ConstI) t2).getValue());
            } else if (t2 instanceof Term.ConstD) {
                t1 = new Term.ConstD(-((Term.ConstD) t2).getValue());
            } else if (t2.isI()) {
                t1 = new Term.Neg(Term.TYPE_I, t2);
            } else if (t2.isD()) {
                t1 = new Term.Neg(Term.TYPE_D, t2);
            } else if (!isTypeChecking()) {
                t1 = new Term.Neg(t2);
            } else {
                reportTypeErrorN1("'-'");
            }
        } else if (tt == '!' || isKeyword("not")) {
            Term t2 = parseUnary(true);
            if (t2.isB() || !isTypeChecking()) {
                t1 = new Term.NotB(t2);
            } else {
                reportTypeErrorB1("'!' or 'not'");
            }
        } else if (tt == '~') {
            Term t2 = parseUnary(true);
            if (t2.isI() || !isTypeChecking()) {
                t1 = new Term.NotI(t2);
            } else {
                reportTypeErrorI1("'~'");
            }
        } else {
            tokenizer.pushBack();
            t1 = parsePostfix(required);
        }
        return t1;
    }

    /**
     * Parses an postfix expression.
     *
     * @param required true, if the expression is required.
     * @return The generated term.
     * @throws ParseException if a parse error occurs
     */
    private Term parsePostfix(final boolean required) throws ParseException {
        return parsePrimary(required);
    }

    /**
     * Parses a primary expression: a constant number <i>num</i>,
     * a constant vector of numbers <i>'{' num1 ',' num2 ',' ... '}'</i>,
     * a constant string <i>'"'ASCII-characters'"'</i>, a variable reference
     * <i>identifier</i> or a function call <i>identifier '(' arg1 ',' arg2 ','
     * arg3 ',' ...')'</i>.
     *
     * @param required true, if the expression is required.
     * @return The generated term.
     * @throws ParseException if a parse error occurs
     */
    private Term parsePrimary(final boolean required) throws ParseException {
        Term t1 = null;
        int tt = tokenizer.next();
        if (tt == Tokenizer.TT_DOUBLE) {
            t1 = new Term.ConstD(convertDoubleToken());
        } else if (tt == Tokenizer.TT_INT) {
            t1 = new Term.ConstI(convertIntToken());
        } else if (tt == Tokenizer.TT_HEX_INT) {
            t1 = new Term.ConstI(convertHexIntToken());
        } else if (tt == Tokenizer.TT_OCT_INT) {
            t1 = new Term.ConstI(convertOctIntToken());
        } else if (tt == Tokenizer.TT_STRING) {
            t1 = new Term.ConstS(convertStringToken());
        } else if (tt == Tokenizer.TT_KEYWORD) {
            String keyword = tokenizer.getToken();
            if (keyword.equalsIgnoreCase("true")) {
                t1 = Term.ConstB.TRUE;
            } else if (keyword.equalsIgnoreCase("false")) {
                t1 = Term.ConstB.FALSE;
            } else {
                reportError("Unexpected keyword '" + keyword + "'.");
            }
        } else if (tt == Tokenizer.TT_NAME || tt == Tokenizer.TT_ESCAPED_NAME) {
            String name = tokenizer.getToken();
            if ("NaN".equals(name)) {
                t1 = Term.ConstD.NAN;
            } else {
                t1 = parseCallOrRef(name);
            }
        } else if (tt == '(') {
            t1 = parseTerm(true);
            tt = tokenizer.next();
            if (tt != ')') {
                tokenizer.pushBack();
                reportError("Missing ')'."); /*I18N*/
            }
        } else {
            if (required) {
                reportError("Term expected."); /*I18N*/
            }
            tokenizer.pushBack();
        }
        return t1;
    }

    private String convertStringToken() {
        return tokenizer.getToken();
    }

    private Term parseCallOrRef(final String name) throws ParseException {
        final int tt;
        Term t1 = null;
        tt = tokenizer.next();
        if (tt == '(') {
            Term[] args = parseArgumentList();
            if ("var".equals(name)) {
                return parseVar(args);
            } else {
                Function function = defaultNamespace.resolveFunction(name, args);
                if (function != null) {
                    t1 = new Term.Call(function, args);
                } else {
                    reportError("Undefined function '" + getFunctionCallString(name, args) + "'."); /*I18N*/
                }
            }
        } else {
            tokenizer.pushBack();
            Symbol symbol = defaultNamespace.resolveSymbol(name);
            if (symbol != null) {
                t1 = new Term.Ref(symbol);
            } else {
                reportError("Undefined symbol '" + name + "'."); /*I18N*/
            }
        }
        return t1;
    }

    /**
     * Parses a variable definition <i>'var' '(' name ',' value [',' min ','  max [','  step]] ')'</i>
     *
     * @return The generated term.
     * @throws ParseException if a parse error occurs
     */
    private Term parseVar(Term[] args) throws ParseException {

        String varName = null;
        Object varVal = null;
        Number varMin = null;
        Number varMax = null;
        Number varStep = null;

        Term arg0 = args[0];
        if (arg0 instanceof Term.ConstS) {
            varName = ((Term.ConstS) arg0).getValue();
        } else {
            reportError("<name> must be a string constant: var(<name>, <value> [, <min>, <max> [, <step>]])");
        }

        int intCount = 0;

        if (args.length > 1) {
            Term arg1 = args[1];
            if (arg1 instanceof Term.ConstI) {
                varVal = ((Term.ConstI) arg1).getValue();
                intCount++;
                if (args.length != 2 && args.length != 4 && args.length != 5) {
                    reportError("Wrong number of arguments: var(<name>, <value> [, <min>, <max> [, <step>]])");
                }
            } else if (arg1 instanceof Term.ConstD) {
                varVal = ((Term.ConstD) arg1).getValue();
                if (args.length != 2 && args.length != 4 && args.length != 5) {
                    reportError("Wrong number of arguments: var(<name>, <value> [, <min>, <max> [, <step>]])");
                }
            } else if (arg1 instanceof Term.ConstB) {
                varVal = ((Term.ConstB) arg1).getValue();
                if (args.length != 2) {
                    reportError("Wrong number of arguments: var(<name>, <boolean>)");
                }
            } else {
                reportError("2nd arg of 'var' must be a numeric or boolean constant");
            }
        } else {
            reportError("Wrong number of arguments: var(<name>, <val> [, <min>, <max> [, <step>]])");
        }

        if (args.length >= 4) {
            Term arg2 = args[2];
            if (arg2 instanceof Term.ConstI) {
                varMin = ((Term.ConstI) arg2).getValue();
                intCount++;
            } else if (arg2 instanceof Term.ConstD) {
                varMin = ((Term.ConstD) arg2).getValue();
            } else {
                reportError("<min> must be numeric: var(<name>, <val> [, <min>, <max> [, <step>]])");
            }

            Term arg3 = args[3];
            if (arg3 instanceof Term.ConstI) {
                varMax = ((Term.ConstI) arg3).getValue();
                intCount++;
            } else if (arg3 instanceof Term.ConstD) {
                varMax = ((Term.ConstD) arg3).getValue();
            } else {
                reportError("<max> must be numeric: var(<name>, <val> [, <min>, <max> [, <step>]])");
            }
        }

        if (args.length == 5) {
            Term arg4 = args[4];
            if (arg4 instanceof Term.ConstI) {
                varStep = ((Term.ConstI) arg4).getValue();
                intCount++;
            } else if (arg4 instanceof Term.ConstD) {
                varStep = ((Term.ConstD) arg4).getValue();
            } else {
                reportError("<step> must be numeric: var(<name>, <val> [, <min>, <max> [, <step>]])");
            }
        }

        Symbol var;
        if (varVal instanceof Boolean) {
            var = new SymbolFactory.VariableB(varName, (Boolean) varVal);
        } else if (intCount == args.length - 1) {
            var = new SymbolFactory.VariableI(varName,
                                              ((Number) varVal).intValue(),
                                              varMin != null ? varMin.intValue() : 0,
                                              varMax != null ? varMax.intValue() : 100,
                                              varStep != null ? varStep.intValue() : 1);
        } else {
            var = new SymbolFactory.VariableD(varName,
                                              ((Number) varVal).doubleValue(),
                                              varMin != null ? varMin.doubleValue() : 0.0,
                                              varMax != null ? varMax.doubleValue() : 1.0,
                                              varStep != null ? varStep.doubleValue() : 0.1);
        }
        if (defaultNamespace instanceof WritableNamespace) {
            WritableNamespace writableNamespace = (WritableNamespace) defaultNamespace;
            writableNamespace.registerSymbol(var);
        }
        return new Term.Ref(var);
    }


    /**
     * Parses a function argument list <i>'(' arg1 ',' arg2 ',' arg3 ',' ... ')'</i>
     *
     * @return The generated term.
     * @throws ParseException if a parse error occurs
     */
    private Term[] parseArgumentList() throws ParseException {

        final Term[] args = parseTermList();
        final int tt = tokenizer.next();
        if (tt != ')') {
            tokenizer.pushBack();
            reportError("Missing ')' or ','."); /*I18N*/
        }

        return args;
    }

    /**
     * Parses a term list <i>t1 ',' t2 ',' t3 ',' ... ',' tN</i>
     *
     * @return The array of generated terms.
     * @throws ParseException if a parse error occurs
     */
    private Term[] parseTermList() throws ParseException {

        Term[] terms = new Term[1];
        int count = 0;

        Term term = parseTerm(false);

        while (term != null) {

            if (count >= terms.length) {
                Term[] temp = new Term[2 * terms.length];
                System.arraycopy(terms, 0, temp, 0, terms.length);
                terms = temp;
            }
            terms[count++] = term;

            int tt = tokenizer.next();
            if (tt == ',') {
                term = parseTerm(true);
            } else {
                tokenizer.pushBack();
                term = null;
            }
        }

        if (count < terms.length) {
            Term[] temp = new Term[count];
            System.arraycopy(terms, 0, temp, 0, count);
            terms = temp;
        }

        return terms;
    }

//    /**
//     * Parses a constant array of numbers
//     * <i>'{' num1 ',' num2 ',' num3 ','  ... ',' numN '}'</i>,
//     * @throws ParseException if a parse error occurs
//     */
//    private Term parseArray() throws ParseException {
//
//        int tt = _tokenizer.next(); // skip '['
//        double[] values = new double[3];
//        int count = 0;
//        String number;
//
//        while (tt != '}') {
//
//            if (tt == Tokenizer.TT_REALNUM || tt == Tokenizer.TT_HEXNUM) {
//                number = _tokenizer.getToken();
//                if (count >= values.length) {
//                    double[] temp = new double[2 * values.length];
//                    System.arraycopy(values, 0, temp, 0, values.length);
//                    values = temp;
//                }
//            } else {
//                _tokenizer.pushBack();
//                reportError("Number expected as vector element."); /*I18N*/
//                break;
//            }
//
//            if (tt == Tokenizer.TT_REALNUM) {
//                values[count++] = convertDoubleToken();
//            } else if (tt == Tokenizer.TT_HEXNUM) {
//                values[count++] = (double) convertHexIntToken();
//            }
//
//            tt = _tokenizer.next();
//            if (tt == ',') {
//                tt = _tokenizer.next();
//            }
//        }
//        if (count < values.length) {
//            double[] temp = new double[count];
//            System.arraycopy(values, 0, temp, 0, count);
//            values = temp;
//        }
//
//        return null;
//
//    }

    private double convertDoubleToken() throws ParseException {
        final String token = tokenizer.getToken();
        try {
            return Double.parseDouble(token);
        } catch (NumberFormatException e) {
            reportError("Token '" + token + "' is not a valid numeric constant."); /*I18N*/
        }
        return 0.0;
    }

    private int convertHexIntToken() throws ParseException {
        final String token = tokenizer.getToken();
        try {
            String hexPart = token.substring(2); // skip '0x' prefix
            long l = Long.parseLong(hexPart, 16);
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                return (int) l;
            } else {
                reportError("Hexadecimal constant '" + token + "' is out of range."); /*I18N*/
            }
        } catch (NumberFormatException e) {
            reportError("Token '" + token + "' is not a valid hexadecimal constant."); /*I18N*/
        }
        return 0;
    }

    private int convertOctIntToken() throws ParseException {
        final String token = tokenizer.getToken();
        try {
            String octPart = token.substring(1); // skip '0' prefix
            long l = Long.parseLong(octPart, 8);
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                return (int) l;
            } else {
                reportError("Octal constant '" + token + "' is out of range."); /*I18N*/
            }
        } catch (NumberFormatException e) {
            reportError("Token '" + token + "' is not a valid octal constant."); /*I18N*/
        }
        return 0;
    }

    private int convertIntToken() throws ParseException {
        String token = tokenizer.getToken();
        try {
            long l = Long.parseLong(token);
            if (l >= Integer.MIN_VALUE && l <= Integer.MAX_VALUE) {
                return (int) l;
            } else {
                reportError("Integer constant '" + token + "' is out of range."); /*I18N*/
            }
        } catch (NumberFormatException e) {
            reportError("Token  '" + token + "' is not a valid integer constant."); /*I18N*/
        }
        return 0;
    }

    private boolean isSpecial(final String special) {
        if (tokenizer.getType() == Tokenizer.TT_SPECIAL) {
            String token = tokenizer.getToken();
            if (token.equals(special)) {
                return true;
            }
        }
        return false;
    }

    private boolean isKeyword(final String keyword) {
        if (tokenizer.getType() == Tokenizer.TT_KEYWORD) {
            String token = tokenizer.getToken();
            if (token.equalsIgnoreCase(keyword)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isB(Term t1, Term t2) {
        return t1.isB() && t2.isB();
    }

    private static boolean isI(Term t1, Term t2) {
        return t1.isI() && t2.isI();
    }

    private static boolean isD(Term t1, Term t2) {
        return t1.isD() && t2.isN() || t1.isN() && t2.isD();
    }

    /**
     * Throws a <code>ParseException</code> with the given message
     *
     * @param message Error message.
     * @throws ParseException always
     */
    private void reportError(final String message) throws ParseException {
        throw new ParseException(tokenizer.getLine(), tokenizer.getColumn(), message);
    }

    private void reportTypeErrorB1(final String operator) throws ParseException {
        reportError("Boolean operand expected for unary " + operator + " operator.");
    }

    private void reportTypeErrorI1(final String operator) throws ParseException {
        reportError("Integer operand expected for unary " + operator + " operator.");
    }

    private void reportTypeErrorN1(final String operator) throws ParseException {
        reportError("Numeric operand expected for unary " + operator + " operator.");
    }

    private void reportTypeErrorB2(final String operator) throws ParseException {
        reportError("Boolean operands expected for binary " + operator + " operator.");
    }

    private void reportTypeErrorI2(final String operator) throws ParseException {
        reportError("Integer operands expected for binary " + operator + " operator.");
    }

    private void reportTypeErrorN2(final String operator) throws ParseException {
        reportError("Numeric operands expected for binary " + operator + " operator.");
    }

    private static String getFunctionCallString(final String name, final Term[] args) {
        final StringBuffer sb = new StringBuffer();
        sb.append(name);
        sb.append('(');
        for (int i = 0; i < args.length; i++) {
            if (i > 0) {
                sb.append(',');
            }
            sb.append(getParamTypeString(args[i].getRetType()));
        }
        sb.append(')');
        return sb.toString();
    }

    private static String getParamTypeString(final int type) {
        if (type == Term.TYPE_B) {
            return "boolean";
        } else if (type == Term.TYPE_I) {
            return "int";
        } else if (type == Term.TYPE_D) {
            return "double";
        } else {
            return "?";
        }
    }

    private Term createConditionTerm(Term t1, Term t2, Term t3) throws ParseException {
        if (isB(t2, t3)) {
            return new Term.Cond(Term.TYPE_B, t1, t2, t3);
        } else if ((isD(t2, t3))) {
            return new Term.Cond(Term.TYPE_D, t1, t2, t3);
        } else if ((isI(t2, t3))) {
            return new Term.Cond(Term.TYPE_I, t1, t2, t3);
        } else if (!isTypeChecking()) {
            return new Term.Cond(Term.TYPE_D, t1, t2, t3);
        } else {
            reportError("Boolean or numeric operands expected in conditional term.");
            return null;
        }
    }

}


