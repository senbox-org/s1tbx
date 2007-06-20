/*
 * $Id: BitmaskExpressionParser.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */

package org.esa.beam.framework.dataop.bitmask;

import java.io.IOException;
import java.io.Reader;
import java.io.StreamTokenizer;
import java.io.StringReader;

import org.esa.beam.util.Guardian;

/**
 * The <code>BitmaskExpressionParser</code> class is used to create {@link BitmaskTerm bit-mask terms} from bit-mask
 * expressions given as text. The bit-mask expressions recognized by this parser must have the following syntax:
 * <p/>
 * <blockquote> <p><i>bit-mask-expression :=</i><br> &nbsp;&nbsp;&nbsp;&nbsp;<i>or-expression</i>
 * <p/>
 * <p><i>or-expression :=</i><br> &nbsp;&nbsp;&nbsp;&nbsp;<i>and-expression</i><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;<i>or-expression</i> <b><code>or</code></b> <i>and-expression</i>
 * <p/>
 * <p><i>and-expression :=</i><br> &nbsp;&nbsp;&nbsp;&nbsp;<i>not-expression</i><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;<i>and-expression</i> <b><code>and</code></b> <i>not-expression</i>
 * <p/>
 * <p><i>not-expression :=</i><br> &nbsp;&nbsp;&nbsp;&nbsp;<i>primary-expression</i><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;<b><code>not</code></b> <i>not-expression</i>
 * <p/>
 * <p><i>primary-expression :=</i><br> &nbsp;&nbsp;&nbsp;&nbsp;<i>flag-reference</i><br>
 * &nbsp;&nbsp;&nbsp;&nbsp;<b><code>(</code></b> <i>bit-mask-expression</i> <b><code>)</code></b>
 * <p/>
 * <p><i>flag-reference :=</i><br> &nbsp;&nbsp;&nbsp;&nbsp;<i>dataset-name</i><b><code>.</code></b><i>flag-name</i></b>
 * </blockquote>
 * <p/>
 * <p>Where <i>dataset-name</i> and <i>flag-name</i> are names specific for a particular data product. Names are in
 * general resolved case-insenitively. The parser also accepts an alternate notation for the boolean operators:
 * <blockquote> The <b><code>|</code></b> character for the <b><code>or</code></b> operator,<br> the
 * <b><code>&</code></b> character for the <b><code>and</code></b> operator and finally<br> the <b><code>!</code></b>
 * character for the <b><code>not</code></b> operator. </blockquote>
 * <p/>
 * <p>For example, the following parseBitmaskExpression request will perform without errors:
 * <pre>
 *     BitmaskTerm term = BitmaskExpressionParser.parse("l2_flags.LAND and not l2_flags.DDV");
 * </pre>
 * <p>Another example for a valid expression in alternatate notation is:
 * <pre>
 *     BitmaskTerm term = BitmaskExpressionParser.parse("l2_flags.LAND | (l2_flags.COASTLINE & !l2_flags.CLOUD)");
 * </pre>
 * <p/>
 * <p>The terms created in the examples above could successfully be evaluated in an evaluation context provided by an
 * ENVISAT MERIS Level 2 data product.
 *
 * @author Norman Fomferra
 * @version $Revision: 1.1.1.1 $ $Date: 2006/09/11 08:16:45 $
 * @see #parse(String)
 * @see #parse(Reader)
 * @see BitmaskTerm
 */
public class BitmaskExpressionParser {

    /**
     * The tokenizer used to tokenize the input stream, before it is parsed.
     */
    private StreamTokenizer _st;

    /**
     * Constructs a new parser for the given input stream.
     *
     */
    private BitmaskExpressionParser(Reader reader) {
        _st = new StreamTokenizer(reader);
        _st.resetSyntax();
        _st.whitespaceChars(0, 32);
        _st.wordChars('a', 'z');
        _st.wordChars('A', 'Z');
        _st.wordChars('_', '_');
        _st.wordChars('0', '9');
    }

    /**
     * Parses the bit-mask expression given as character string.
     *
     * @param code the bit-mask expression given as character string
     *
     * @return the bit-mask term tree representing the bit-mask expression
     *
     * @throws BitmaskExpressionParseException
     *          if the given code could not be parsed
     */
    public static BitmaskTerm parse(String code) throws BitmaskExpressionParseException {
        Guardian.assertNotNull("code", code);
        try {
            return parse(new StringReader(code));
        } catch (IOException e) {
            throw new BitmaskExpressionParseException("invalid bitmap expression: '" + code + "'");
        }
    }

    /**
     * Parses the bit-mask expression given as input stream.
     *
     * @param reader the bit-mask expression given as input stream
     *
     * @return the bit-mask term tree representing the bit-mask expression
     *
     * @throws BitmaskExpressionParseException
     *                     if the given code could not be parsed
     * @throws IOException if an I/O error occurs
     */
    public static BitmaskTerm parse(Reader reader) throws BitmaskExpressionParseException,
                                                          IOException {
        Guardian.assertNotNull("reader", reader);
        BitmaskExpressionParser parser = new BitmaskExpressionParser(reader);
        return parser.parseBitmaskExpression();
    }

    private BitmaskTerm parseBitmaskExpression() throws BitmaskExpressionParseException,
                                                        IOException {
        BitmaskTerm term = parseBitmaskExpression(false);
        if (!isEOF()) {
            raiseUnexpectedTokenFoundError(null);
        }
        return term;
    }

    private BitmaskTerm parseBitmaskExpression(boolean termRequired) throws BitmaskExpressionParseException,
                                                                            IOException {
        return parseOrExpression(termRequired);
    }

    private BitmaskTerm parseOrExpression(boolean termRequired) throws BitmaskExpressionParseException,
                                                                       IOException {
        BitmaskTerm term1 = parseAndExpression(termRequired);
        if (term1 == null) {
            return null;
        }

        while (true) {
            nextToken();
            if (isOrKeyword() || isOrOperator()) {
                BitmaskTerm term2 = parseOrExpression(true);
                term1 = new BitmaskTerm.Or(term1, term2);
            } else {
                pushBackToken();
                break;
            }
        }

        return term1;
    }

    private BitmaskTerm parseAndExpression(boolean termRequired) throws BitmaskExpressionParseException,
                                                                        IOException {
        BitmaskTerm term1 = parseUnaryExpression(termRequired);
        if (term1 == null) {
            return null;
        }

        while (true) {
            nextToken();
            if (isAndKeyword() || isAndOperator()) {
                BitmaskTerm term2 = parseAndExpression(true);
                term1 = new BitmaskTerm.And(term1, term2);
            } else {
                pushBackToken();
                break;
            }
        }

        return term1;
    }


    private BitmaskTerm parseUnaryExpression(boolean termRequired) throws BitmaskExpressionParseException,
                                                                          IOException {

        BitmaskTerm term = null;

        nextToken();
        if (isNotKeyword() || isNotOperator()) {
            term = parseUnaryExpression(true);
            term = new BitmaskTerm.Not(term);
        } else {
            pushBackToken();
            term = parsePrimaryTerm(termRequired);
        }

        return term;
    }

    private BitmaskTerm parsePrimaryTerm(boolean termRequired) throws BitmaskExpressionParseException,
                                                                      IOException {

        BitmaskTerm term = null;

        nextToken();
        if (getTokenType() == '(') {
            term = parseBitmaskExpression(true);
            nextToken();
            if (getTokenType() != ')') {
                raiseUnexpectedTokenFoundError("')' expected");
            }
        } else if (isNameToken()) {
            String bandName = getToken();
            nextToken();
            if (getTokenType() == '.') {
                nextToken();
                if (isNameToken()) {
                    String flagName = getToken();
                    term = new BitmaskTerm.FlagReference(bandName, flagName);
                } else {
                    raiseUnexpectedTokenFoundError("flag name expected");
                }
            } else {
                raiseUnexpectedTokenFoundError("'.' expected");
            }
        } else if (isEOF()) {
            if (termRequired) {
                raiseUnexpectedTokenFoundError("operator or flag name expected");
            }
        } else {
            raiseUnexpectedTokenFoundError("operator or flag name expected");
        }

        return term;
    }

    private void nextToken() throws IOException {
        _st.nextToken();
    }

    private boolean isOrKeyword() {
        return isNameToken() && getToken().equalsIgnoreCase("or");
    }

    private boolean isAndKeyword() {
        return isNameToken() && getToken().equalsIgnoreCase("and");
    }

    private boolean isNotKeyword() {
        return isNameToken() && getToken().equalsIgnoreCase("not");
    }

    private boolean isAndOperator() {
        return getTokenType() == '&';
    }

    private boolean isOrOperator() {
        return getTokenType() == '|';
    }

    private boolean isNotOperator() {
        return getTokenType() == '!';
    }

    private boolean isNameToken() {
        return getTokenType() == StreamTokenizer.TT_WORD
               && _st.sval != null
               && _st.sval.length() > 0
               && (Character.isLetter(_st.sval.charAt(0)) || _st.sval.charAt(0) == '_');
    }

    private boolean isNumberToken() {
        return getTokenType() == StreamTokenizer.TT_NUMBER;
    }

    private boolean isEOF() {
        return getTokenType() == StreamTokenizer.TT_EOF;
    }

    private int getTokenType() {
        return _st.ttype;
    }

    private String getToken() {
        if (isNameToken()) {
            return _st.sval;
        } else if (isNumberToken()) {
            return String.valueOf(_st.nval);
        } else if (_st.sval != null && _st.sval.length() >= 0) {
            return _st.sval;
        } else {
            return String.valueOf((char) _st.ttype);
        }
    }

    private void pushBackToken() {
        _st.pushBack();
    }

    private void raiseUnexpectedTokenFoundError(String message) throws BitmaskExpressionParseException {
        pushBackToken();
        String m;
        if (message != null) {
            if (!isEOF()) {
                m = message + ", but found token '" + getToken() + "'";
            } else {
                m = message + ", but found 'end-of-string'";
            }
        } else {
            if (!isEOF()) {
                m = "unexpected token '" + getToken() + "' found";
            } else {
                m = "unexpected 'end-of-stream' found";
            }
        }
        throw new BitmaskExpressionParseException(m);
    }

}

