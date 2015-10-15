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

/**
 * The <code>Tokenizer</code> class is used to split a given text source
 * into lexicographical tokens which are regognized by the parser.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 * @see ParserImpl
 */
public final class Tokenizer {

    /**
     * Token type which indicates the end-of-stream.
     */
    public static final int TT_EOS = -1;

    /**
     * Token type which indicates an unknown token type.
     */
    public static final int TT_UNKNOWN = -2;

    /**
     * Token type which indicates a numeric constant.
     */
    public static final int TT_INT = -11;

    /**
     * Token type which indicates an hexadecimal integer constant.
     */
    public static final int TT_HEX_INT = -12;

    /**
     * Token type which indicates an hexadecimal integer constant.
     */
    public static final int TT_OCT_INT = -13;

    /**
     * Token type which indicates a numeric constant.
     */
    public static final int TT_DOUBLE = -14;

    /**
     * Token type which indicates a string constant.
     */
    public static final int TT_STRING = -21;

    /**
     * Token type which indicates a name (e.g. variable or function identifier).
     */
    public static final int TT_NAME = -31;

    /**
     * Token type which indicates a name (e.g. variable or function identifier).
     */
    public static final int TT_KEYWORD = -32;

    /**
     * Token type which indicates a name (e.g. variable or function identifier).
     */
    public static final int TT_ESCAPED_NAME = -33;

    /**
     * Token type which indicates a character (-sequence) with a special meaning.
     */
    public static final int TT_SPECIAL = -41;

    /**
     * The keywords regognized by this tokenizer.
     */
    private final static String[] keywords = new String[]{
            "and", "or", "not", "true", "false", "if", "then", "else",
    };

    /**
     * The special tokens regognized by this tokenizer.
     */
    private final static String[] specialTokens = new String[]{
            "==", "<=", ">=", "!=", "&&", "||", "<<", ">>"
    };

    /**
     * The ordinary characters regognized by this tokenizer.
     */
    private final static char[] ordinaryChars = new char[]{
            '(', ')', '{', '}', '[', ']',
            ',', ':',
            '<', '>',
            '=',
            '!', '?',
            '"',
            '|', '&', '%', '$',
            '+', '-', '*', '/',
            '^', '~'
    };


    /**
     * The text source as character array.
     */
    private final char[] source;

    /**
     * The current token.
     */
    private final StringBuilder token;

    /**
     * Determines whether the last token has been pushed back.
     */
    private boolean pushedBack;

    /**
     * The type of the current token.
     */
    private int type;

    /**
     * The current position within the text source.
     */
    private int pos;

    /**
     * The current line number within the text source.
     */
    private int line;

    /**
     * The current column number within the text source.
     */
    private int column;

    /**
     * Determines if tokenizing is performed case sensitive or not
     */
    private boolean caseSensitive;

    /**
     * Constructs a new tokenizer for the given text source.
     *
     * @param source        the text source to split into tokens
     * @param caseSensitive determines if tokenizing is performed case sensitive or not
     */
    public Tokenizer(final String source, final boolean caseSensitive) {
        this.source = source.toCharArray();
        line = 1;
        column = 0;
        pos = 0;
        type = TT_UNKNOWN;
        pushedBack = false;
        token = new StringBuilder();
        this.caseSensitive = caseSensitive;
    }

    /**
     * Constructs a new tokenizer for the given text source.
     * Tokenizing is performed case insensitive.
     *
     * @param source the text source to split into tokens
     */
    public Tokenizer(final String source) {
        this(source, false);
    }

    /**
     * Returns the current token type.
     */
    public int getType() {
        return type;
    }

    /**
     * Gets the current source line number.
     */
    public int getLine() {
        return line;
    }

    /**
     * Gets the current source column number.
     */
    public int getColumn() {
        return column;
    }

    /**
     * Gets the current token wich was read in due to the last
     * <code>next()</code> call.
     *
     * @return the current token string
     */
    public String getToken() {
        return token.toString();
    }

    /**
     * Pushes back the token read by the last <code>next()</code> call, so
     * that it is returned again by a following <code>next()</code> call.
     */
    public void pushBack() {
        pushedBack = true;
    }

    /**
     * Reads the next token from the text source passed to the constructor of
     * this tokenizer. The value returned is one of the regognized token types
     * or for ordinary characters, the character code itself.
     *
     * @return the type of the current token or for ordinary characters, the
     *         character code itself
     */
    public int next() {

        if (pushedBack) {
            pushedBack = false;
            return type;
        }

        type = TT_UNKNOWN;
        token.setLength(0);

        eatWhite();

        if (isEos()) {
            type = TT_EOS;
        } else if (isNameStart()) {
            eatName();
        } else if (isEscapedNameStart()) {
            eatEscapedName();
        } else if (isStringStart()) {
            eatString();
        } else if (isHexNumberStart()) {
            eatHexNumber();
        } else if (isNumberStart()) {
            eatNumber();
        } else {
            for (String specialToken : specialTokens) {
                final int n = specialToken.length();
                int i = pos;
                for (; i < pos + n; i++) {
                    if (isEos(i) || specialToken.charAt(i - pos) != peek(i)) {
                        break;
                    }
                }
                if (i == pos + n) {
                    type = TT_SPECIAL;
                    eat(n);
                    break;
                }
            }

            if (type != TT_SPECIAL) {
                for (char ordinaryChar : ordinaryChars) {
                    if (ordinaryChar == peek()) {
                        type = peek();
                        eat();
                        break;
                    }
                }
            }
        }

        return type;
    }

    /**
     * Tests whether or not the given name is a valid external name. Valid names have a length greater than zero,
     * start with a letter or underscore followed by letters, digits or underscores.
     * The keywords are not allowed as external names.
     *
     * @param name the name to test
     * @return <code>true</code> if the name is a valid external name, <code>false</code> otherwise
     */
    public static boolean isExternalName(final String name) {
        if (name == null || name.length() == 0) {
            return false;
        }
        for (final String keyword : keywords) {
            if (keyword.equalsIgnoreCase(name)) {
                return false;
            }
        }
        return name.charAt(0) == '\'' && name.charAt(name.length() - 1) == '\''
               || name.matches("[a-z_$A-Z][a-z_$A-Z0-9\\.]*");
    }

    /**
     * Create an external name from the given name.
     * If the given name contains character which are not valid in an external name
     * the name is escaped with single quotes.
     *
     * @param name the name
     * @return a valid external name
     */
    public static String createExternalName(final String name) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("The name to be externalized must at least contain one character");
        }
        if (isExternalName(name)) {
            return name;
        }
        return "\'" + name.replaceAll("[\\']", "\\'\\'") + "\'";
    }

    /**
     * Appends all subsequential white space characters to the current token
     * and increments the current source position.
     */
    private void eatWhite() {
        while (isWhite()) {
            if (peek() == '\n') {
                line++;
                column = 0;
            }
            incPos();
        }
    }

    /**
     * Appends all subsequential characters belonging to a name to the current token
     * and increments the current source position.
     */
    private void eatName() {
        type = TT_NAME;

        do {
            eat();
        } while (isNamePart());

        final String name = token.toString();
        for (String keyword : keywords) {
            final boolean isEqual;
            if (caseSensitive) {
                isEqual = name.equals(keyword);
            } else {
                isEqual = name.equalsIgnoreCase(keyword);
            }
            if (isEqual) {
                type = TT_KEYWORD;
                break;
            }
        }
    }

    /**
     * Appends all subsequential characters belonging to a string constant
     * to the current token and increments the current source position.
     */
    private void eatString() {
        type = TT_STRING;
        incPos(); // skip leading (")
        while (!isStringEnd()) {
            eat();
        }
        if (!isEos()) {
            incPos(); // skip trailing (")
        }
    }

    /**
     * Appends all subsequential characters belonging to a string constant
     * to the current token and increments the current source position.
     */
    private void eatEscapedName() {
        type = TT_ESCAPED_NAME;
        incPos(); // skip leading (')
        while (!isEscapedNameEnd()) {
            if (isEscapedEscape()) {
                incPos();
            }
            eat();
        }
        if (!isEos()) {
            incPos(); // skip trailing (')
        }
    }

    /**
     * Appends all subsequent characters belonging to a number constant
     * to the current token and increments the current source position.
     */
    private void eatNumber() {

        while (isDigit()) {
            eat(); // digit
        }
        if (isDot()) {
            type = TT_DOUBLE;
            eat(); // '.'
            while (isDigit()) {
                eat(); // digit
            }
        }
        if (isExpPartStart()) {
            type = TT_DOUBLE;
            eat(); // 'e' or 'E'
            eat(); // '-' or '+' or digit
            while (isDigit()) {
                eat(); // digit
            }
        }
        if (type != TT_DOUBLE) {
            type = TT_INT;
            if (token.charAt(0) == '0' && token.length() > 1) {
                type = TT_OCT_INT;
                for (int i = 0; i < token.length(); i++) {
                    if (isOctDigit(token.charAt(i))) {
                        type = TT_INT;
                        break;
                    }
                }
            }
        }
    }

    /**
     * Appends all subsequential characters belonging to a hexadecimal number
     * constant to the current token and increments the current source position.
     */
    private void eatHexNumber() {
        type = TT_HEX_INT;
        eat(); // '0'
        eat(); // 'x' or 'X'
        while (isHexDigit()) {
            eat();
        }
    }

    /**
     * Determines whether the character at the current position is a white space.
     */
    private boolean isWhite() {
        return isWhite(pos);
    }

    /**
     * Determines whether the character at the given position is a white space.
     */
    private boolean isWhite(final int i) {
        return !isEos(i) && Character.isWhitespace(peek(i));
    }

    /**
     * Determines whether the character at the current position is
     * a single quote used to mark the end of an escaped name (').
     */
    private boolean isEscapedNameStart() {
        return isChar('\'');
    }

    /**
     * Determines whether the character at the current position is
     * a single quote used to mark the end of an escaped name (').
     */
    private boolean isEscapedNameEnd() {
        return isEos() || (isChar('\'') && !isChar(pos + 1, '\''));
    }

    /**
     * Determines whether the character at the current position and
     * the following character are both single quotes.
     */
    private boolean isEscapedEscape() {
        return (isChar('\'') && isChar(pos + 1, '\''));
    }

    /**
     * Determines whether the character at the current position is
     * a quote used to mark the beginning of a string constant (").
     */
    private boolean isStringStart() {
        return isChar('"');
    }

    /**
     * Determines whether the character at the current position is
     * a double quote used to mark the end of a string constant (").
     */
    private boolean isStringEnd() {
        return isEos() || isChar('"');
    }

    /**
     * Determines whether the character at the current position
     * marks the beginning of a number (a digit or a '.').
     */
    private boolean isNumberStart() {
        return isDigit() || isDotAndDigit();
    }

    private boolean isHexNumberStart() {
        return isHexNumberStart(pos);
    }

    private boolean isHexNumberStart(final int i) {
        return isChar(i, '0') && (isChar(i + 1, 'x') || isChar(i + 1, 'X') || isChar(i + 1, 'm'));
    }

    private boolean isDotAndDigit() {
        return isDotAndDigit(pos);
    }

    private boolean isDotAndDigit(final int i) {
        return isDot(i) && isDigit(i + 1);
    }

    private boolean isSignAndDigit(final int i) {
        return isSign(i) && isDigit(i + 1);
    }

    private boolean isExpPartStart() {
        return (isChar('e') || isChar('E'))
                && (isDigit(pos + 1) || isSignAndDigit(pos + 1));
    }

    private boolean isDot() {
        return isDot(pos);
    }

    private boolean isDot(final int i) {
        return isChar(i, '.');
    }

    private boolean isSign(final int i) {
        return isMinus(i) || isPlus(i);
    }

    private boolean isPlus(int i) {
        return isChar(i, '+');
    }

    private boolean isMinus(int i) {
        return isChar(i, '-');
    }

    private boolean isChar(final char ch) {
        return peek() == ch;
    }

    private boolean isChar(final int i, final char ch) {
        return peek(i) == ch;
    }

    private boolean isEos() {
        return isEos(pos);
    }

    private boolean isEos(final int i) {
        return i >= source.length;
    }

    private boolean isDigit() {
        return isDigit(pos);
    }

    private boolean isDigit(final int i) {
        return !isEos(i) && Character.isDigit(peek(i));
    }

    private boolean isHexDigit() {
        return isHexDigit(pos);
    }

    private boolean isHexDigit(final int i) {
        return !isEos(i)
                && (Character.isDigit(peek(i))
                || (peek(i) >= 'a' && peek(i) <= 'f')
                || (peek(i) >= 'A' && peek(i) <= 'F'));
    }

    private static boolean isOctDigit(final char ch) {
        return ch >= '0' && ch <= '8';
    }

    private boolean isNameStart() {
        return isNameStart(pos);
    }

    /**
     * Determines whether the character at the current position
     * marks the beginning of a name (a letter or '_').
     */
    private boolean isNameStart(final int i) {
        return !isEos(i) && (Character.isLetter(source[i]) || source[i] == '_' || source[i] == '$');
    }

    /**
     * Determines whether the character at the current position is
     * part of a name (a letter, a digit or '_').
     */
    private boolean isNamePart() {
        return isNamePart(pos);
    }

    /**
     * Determines whether the character at the given position i is
     * part of a name (a letter, a digit, the '_' or '.').
     */
    private boolean isNamePart(final int i) {
        return isNameStart(i) || isDigit(i) || isDot(i);
    }

    /**
     * Returns the character at the current position.
     */
    private char peek() {
        return peek(pos);
    }

    /**
     * Returns the character at the given position i.
     */
    private char peek(final int i) {
        return isEos(i) ? '\0' : source[i];
    }


    /**
     * Appends a single character to the current token and increments the
     * current source position.
     */
    private void eat() {
        token.append(peek());
        incPos();
    }

    /**
     * Appends n characters to the current token and increments the
     * current source position by n.
     */
    private void eat(final int n) {
        token.append(source, pos, n);
        incPos(n);
    }

    /**
     * Increments the current source position.
     */
    private void incPos() {
        pos++;
        column++;
    }

    /**
     * Increments the current source position by n.
     */
    private void incPos(final int n) {
        pos += n;
        column += n;
    }
}
