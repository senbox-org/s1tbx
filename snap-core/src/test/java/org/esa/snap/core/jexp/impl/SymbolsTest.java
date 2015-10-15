package org.esa.snap.core.jexp.impl;

import org.junit.Test;

import static org.junit.Assert.*;

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
