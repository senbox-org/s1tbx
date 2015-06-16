package com.bc.jexp.impl;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Norman
 */
public class SymbolsTest {
    @Test
    public void testGetAll() throws Exception {
        int size = Symbols.getAll().size();
        assertTrue(size >= 2);
    }
}
