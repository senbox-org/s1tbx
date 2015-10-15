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
 * Namespaces are used by a <code>{@link org.esa.snap.core.jexp.Parser}</code> in order
 * to resolve to symbol references and function calls.
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public interface Namespace {

    /**
     * Resolves the given name in order to find a matching symbol.
     * @param name a symbol name
     * @return the symbol or <code>null</code> if this namespace does not
     *         contain a corresponding symbol
     */
    Symbol resolveSymbol(String name);

    /**
     * Resolves the given name and argument list in order to find a matching function.
     * @param name a function name
     * @param args the argument list
     * @return the function or <code>null</code> if this namespace does not
     *         contain a corresponding function
     */
    Function resolveFunction(String name, Term[] args);
}
