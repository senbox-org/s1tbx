package com.bc.jexp.impl;

import org.junit.Test;

import static org.junit.Assert.assertTrue;

/**
 * @author Norman
 */
public class FunctionsTest {
    @Test
    public void testGetAll() throws Exception {
        int size = Functions.getAll().size();
        assertTrue(size >= 40);
    }
}
