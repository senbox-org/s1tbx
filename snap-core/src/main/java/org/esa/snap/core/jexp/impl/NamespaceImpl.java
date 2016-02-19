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
import org.esa.snap.core.jexp.Symbol;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.WritableNamespace;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Provides an implementation of the {@code {@link org.esa.snap.core.jexp.Namespace}} interface.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 */
public class NamespaceImpl implements WritableNamespace {

    private final Namespace defaultNamespace;
    private final Map<String, Symbol> symbols;
    private final Map<String, Function[]> functions;

    public NamespaceImpl() {
        this(null);
    }

    public NamespaceImpl(final Namespace defaultNamespace) {
        this.defaultNamespace = defaultNamespace;
        symbols = new HashMap<>(32);
        functions = new HashMap<>(16);
    }

    public final Namespace getDefaultNamespace() {
        return defaultNamespace;
    }

    public final void registerSymbol(final Symbol symbol) {
        symbols.put(symbol.getName(), symbol);
    }

    public final void deregisterSymbol(final Symbol symbol) {
        symbols.remove(symbol.getName());
    }

    public final Symbol resolveSymbol(final String name) {
        Symbol symbol = symbols.get(name);
        if (symbol == null && defaultNamespace != null) {
            symbol = defaultNamespace.resolveSymbol(name);
        }
        return symbol;
    }

    public final void registerFunction(final Function function) {
        Function[] array = getFunctions(function.getName());
        if (array != null) {
            for (Function anArray : array) {
                if (anArray == function) {
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
        functions.put(function.getName(), array);
    }

    public final void deregisterFunction(final Function function) {
        Function[] array = getFunctions(function.getName());
        if (array != null) {
            final ArrayList<Function> functionList = new ArrayList<>(Arrays.asList(array));
            if (!functionList.remove(function)) {
                return;
            }
            array = functionList.toArray(new Function[functionList.size()]);
            functions.put(function.getName(), array);
        }
    }

    public final Function resolveFunction(final String name, final Term[] args) {
        final Function[] functions = getFunctions(name);
        if (functions != null) {
            Function bestFunction = null;
            int bestValidity = -1;
            for (Function function : functions) {
                int definedNumArgs = function.getNumArgs();
                if (definedNumArgs == args.length || definedNumArgs == -1) {
                    int validity = definedNumArgs == args.length ? 10 : 0;
                    for (int i = 0; i < args.length; i++) {
                        Term arg = args[i];
                        int argType = function.getArgType(i);
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
        if (defaultNamespace != null) {
            return defaultNamespace.resolveFunction(name, args);
        }
        return null;
    }

    public Symbol[] getAllSymbols() {
        final ArrayList<Symbol> symbolList = new ArrayList<>(symbols.values());
        if (defaultNamespace instanceof WritableNamespace) {
            final WritableNamespace writableNamespace = (WritableNamespace) defaultNamespace;
            final Symbol[] defaultSymbols = writableNamespace.getAllSymbols();
            symbolList.addAll(Arrays.asList(defaultSymbols));
        }
        return symbolList.toArray(new Symbol[symbolList.size()]);
    }

    public final Function[] getAllFunctions() {
        final Collection<Function[]> collection = functions.values();
        final ArrayList<Function> functionList = new ArrayList<>(32 + 2 * collection.size());
        for (Function[] functions : collection) {
            functionList.addAll(Arrays.asList(functions));
        }
        if (defaultNamespace instanceof WritableNamespace) {
            final WritableNamespace writableNamespace = (WritableNamespace) defaultNamespace;
            final Function[] defaultFunctions = writableNamespace.getAllFunctions();
            functionList.addAll(Arrays.asList(defaultFunctions));
        }
        return functionList.toArray(new Function[functionList.size()]);
    }

    private Function[] getFunctions(final String name) {
        return functions.get(name);
    }
}
