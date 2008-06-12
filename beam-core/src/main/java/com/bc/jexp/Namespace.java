/*
 * $Id: Namespace.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
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
 * Namespaces are used by a <code>{@link com.bc.jexp.Parser}</code> in order
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
