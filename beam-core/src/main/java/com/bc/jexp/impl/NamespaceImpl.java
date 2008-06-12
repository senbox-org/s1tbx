/*
 * $Id: NamespaceImpl.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
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

package com.bc.jexp.impl;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import com.bc.jexp.Function;
import com.bc.jexp.Namespace;
import com.bc.jexp.Symbol;
import com.bc.jexp.Term;
import com.bc.jexp.WritableNamespace;

/**
 * Provides an implementation of the <code>{@link com.bc.jexp.Namespace}</code> interface.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
 */
public class NamespaceImpl implements WritableNamespace {

    private final Namespace _defaultNamespace;
    private final Map _symbols;
    private final Map _functions;

    public NamespaceImpl() {
        this(null);
    }

    public NamespaceImpl(final Namespace defaultNamespace) {
        _defaultNamespace = defaultNamespace;
        _symbols = new HashMap(32);
        _functions = new HashMap(16);
    }

    public final Namespace getDefaultNamespace() {
        return _defaultNamespace;
    }

    public final void registerSymbol(final Symbol symbol) {
        _symbols.put(symbol.getName(), symbol);
    }

    public final void deregisterSymbol(final Symbol symbol) {
        _symbols.remove(symbol.getName());
    }

    public final Symbol resolveSymbol(final String name) {
        Symbol symbol = (Symbol) _symbols.get(name);
        if (symbol == null && _defaultNamespace != null) {
            symbol = _defaultNamespace.resolveSymbol(name);
        }
        return symbol;
    }

    public final void registerFunction(final Function function) {
        Function[] array = getFunctions(function.getName());
        if (array != null) {
            for (int i = 0; i < array.length; i++) {
                if (array[i] == function) {
                    return;
                }
            }
            Function[] arrayNew = new Function[array.length + 1];
            System.arraycopy(array, 0, arrayNew, 0, array.length);
            arrayNew[array.length] = function;
            array = arrayNew;
        } else {
            array = new Function[]{function};
        }
        _functions.put(function.getName(), array);
    }

    public final void deregisterFunction(final Function function) {
        Function[] array = getFunctions(function.getName());
        if (array != null) {
            final ArrayList functionList = new ArrayList(Arrays.asList(array));
            if (!functionList.remove(function)) {
                return;
            }
            array = (Function[]) functionList.toArray(new Function[functionList.size()]);
            _functions.put(function.getName(), array);
        }
    }

    public final Function resolveFunction(final String name, final Term[] args) {
        final Function[] functions = getFunctions(name);
        if (functions != null) {
            Function bestFunction = null;
            int bestValidity = -1;
            for (int i = 0; i < functions.length; i++) {
                Function function = functions[i];
                if (function.getNumArgs() == args.length) {
                    int validity = 0;
                    for (int j = 0; j < args.length; j++) {
                        Term arg = args[j];
                        int argType = function.getArgTypes()[j];
                        if (argType == arg.getRetType()) {
                            validity += 4;
                        } else if (argType == Term.TYPE_D && arg.isN()) {
                            validity += 2;
                        } else if (argType == Term.TYPE_I && arg.isN()) {
                            validity += 1;
                        }
                    }
                    if (validity > bestValidity) {
                        bestFunction = function;
                        bestValidity = validity;
                    }
                }
            }

            if (bestFunction != null) {
                return bestFunction;
            }
        }
        if (_defaultNamespace != null) {
            return _defaultNamespace.resolveFunction(name, args);
        }
        return null;
    }

    public Symbol[] getAllSymbols() {
        final ArrayList symbolList = new ArrayList(_symbols.values());
        if (_defaultNamespace instanceof WritableNamespace) {
            final WritableNamespace writableNamespace = (WritableNamespace) _defaultNamespace;
            final Symbol[] defaultSymbols = writableNamespace.getAllSymbols();
            symbolList.addAll(Arrays.asList(defaultSymbols));
        }
        return (Symbol[]) symbolList.toArray(new Symbol[0]);
    }

    public final Function[] getAllFunctions() {
        final Collection collection = _functions.values();
        final ArrayList functionList = new ArrayList(32 + 2 * collection.size());
        for (Iterator iterator = collection.iterator(); iterator.hasNext();) {
            Function[] functions = (Function[]) iterator.next();
            functionList.addAll(Arrays.asList(functions));
        }
        if (_defaultNamespace instanceof WritableNamespace) {
            final WritableNamespace writableNamespace = (WritableNamespace) _defaultNamespace;
            final Function[] defaultFunctions = writableNamespace.getAllFunctions();
            functionList.addAll(Arrays.asList(defaultFunctions));
        }
        return (Function[]) functionList.toArray(new Function[0]);
    }

    private Function[] getFunctions(final String name) {
        return (Function[]) _functions.get(name);
    }
}
