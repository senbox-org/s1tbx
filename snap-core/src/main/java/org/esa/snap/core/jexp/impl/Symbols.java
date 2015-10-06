package org.esa.snap.core.jexp.impl;

import org.esa.snap.core.jexp.Symbol;

import java.util.Arrays;
import java.util.List;

/**
 * Common symbol definitions.
 *
 * @author Norman Fomferra
 */
public class Symbols {
    public static final Symbol PI = SymbolFactory.createConstant("PI", Math.PI);
    public static final Symbol E = SymbolFactory.createConstant("E", Math.E);

    public static List<Symbol> getAll() {
        return Arrays.asList(PI, E);
    }
}
