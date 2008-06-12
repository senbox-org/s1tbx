/*
 * $Id: Parser.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
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
package com.bc.jexp;

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
     * @return the default environment used to resolve names.
     */
    Namespace getDefaultNamespace();

    /**
     * Parses the expression given in the code string.
     * Names in the code string are resolved using the default namespace.
     *
     * @param code the code string, for the syntax of valid expressions refer
     *             to the class description
     * @throws ParseException if a parse reportError occurs
     */
    Term parse(String code) throws ParseException;


    /**
     * Parses the expression given in the code string.
     * Names in the code string are resolved using the given namespace.
     *
     * @param code the code string, for the syntax of valid expressions refer
     *             to the class description
     * @param namespace the environment which is used to resolve names
     * @throws ParseException if a parse error occurs
     */
    Term parse(String code, Namespace namespace) throws ParseException;
}
