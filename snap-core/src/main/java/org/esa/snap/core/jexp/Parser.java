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
package org.esa.snap.core.jexp;

/**
 * Instances of the <code>Parser</code> interface are used to convert a code
 * string representing an arithmetic expression in a tree of terms
 * which can then be executed by using one of the evaluation methods of
 * the <code>{@link Term}</code> class.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public interface Parser {

    /**
     * Gets this parser's default namespace.
     *
     * @return the default environment used to resolve names.
     */
    Namespace getDefaultNamespace();

    /**
     * Parses the expression given in the code string.
     * Names in the code string are resolved using the default namespace.
     *
     * @param code the code string, for the syntax of valid expressions refer
     *             to the class description
     * @return the the parsed code as {@link Term}
     *
     * @throws ParseException if a parse reportError occurs
     */
    Term parse(String code) throws ParseException;


    /**
     * Parses the expression given in the code string.
     * Names in the code string are resolved using the given namespace.
     *
     * @param code      the code string, for the syntax of valid expressions refer
     *                  to the class description
     * @param namespace the environment which is used to resolve names
     * @return the the parsed code as {@link Term}
     *
     * @throws ParseException if a parse error occurs
     */
    Term parse(String code, Namespace namespace) throws ParseException;
}
