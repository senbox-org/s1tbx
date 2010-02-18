/*
 * $Id: WritableNamespace.java,v 1.1.1.1 2006/09/11 08:16:43 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package com.bc.jexp;


public interface WritableNamespace extends Namespace {

    void registerSymbol(Symbol symbol);

    void deregisterSymbol(Symbol symbol);

    void registerFunction(Function function);

    void deregisterFunction(Function function);

    Symbol[] getAllSymbols();

    Function[] getAllFunctions();
}
