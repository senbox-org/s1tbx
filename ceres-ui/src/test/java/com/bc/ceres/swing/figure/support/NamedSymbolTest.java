package com.bc.ceres.swing.figure.support;

import org.junit.Test;

import static org.junit.Assert.*;

/**
 * Created by IntelliJ IDEA.
 * User: nfomferra
 * Date: 5/27/11
 * Time: 5:40 PM
 */
public class NamedSymbolTest {

    @Test
    public void testNames() throws Exception {
        assertSame(NamedSymbol.CROSS, NamedSymbol.getSymbol("cross"));
        assertSame(NamedSymbol.CROSS, NamedSymbol.getSymbol("Cross"));
        assertSame(NamedSymbol.PLUS, NamedSymbol.getSymbol("plus"));
        assertSame(NamedSymbol.PLUS, NamedSymbol.getSymbol("PLUS"));
        assertSame(NamedSymbol.STAR, NamedSymbol.getSymbol("STAR"));
        assertSame(NamedSymbol.STAR, NamedSymbol.getSymbol("star"));
        assertSame(NamedSymbol.PIN, NamedSymbol.getSymbol("PIN"));
        assertSame(NamedSymbol.PIN, NamedSymbol.getSymbol("Pin"));
        assertSame(NamedSymbol.SQUARE, NamedSymbol.getSymbol("square"));
        assertSame(NamedSymbol.SQUARE, NamedSymbol.getSymbol("Square"));
        assertSame(NamedSymbol.CIRCLE, NamedSymbol.getSymbol("circle"));
        assertSame(NamedSymbol.CIRCLE, NamedSymbol.getSymbol("CIRCLE"));
        assertSame(null, NamedSymbol.getSymbol("Unknown"));
    }
}
