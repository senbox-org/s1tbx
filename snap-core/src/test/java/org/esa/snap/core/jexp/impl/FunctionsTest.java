package org.esa.snap.core.jexp.impl;

import org.esa.snap.core.jexp.EvalEnv;
import org.esa.snap.core.jexp.Parser;
import org.esa.snap.core.jexp.Term;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class FunctionsTest {

    private EvalEnv env;
    private Parser parser;

    @Before
    public void setUp() throws Exception {
        env = new EvalEnv() {};
        parser = new ParserImpl();
    }

    @Test
    public void testGetAll() throws Exception {
        int size = Functions.getAll().size();
        assertTrue(size >= 40);
    }

    @Test
    public void testBitSet() throws Exception {
        assertTrue(Functions.BIT_SET.evalB(env, new Term[]{
                parser.parse("1"),
                parser.parse("0")
        }));

        assertTrue(Functions.BIT_SET.evalB(env, new Term[]{
                parser.parse("0x20"),
                parser.parse("5")
        }));
        assertFalse(Functions.BIT_SET.evalB(env, new Term[]{
                parser.parse("0x20"),
                parser.parse("4")
        }));

        assertTrue(Functions.BIT_SET.evalB(env, new Term[]{
                parser.parse("-2147483647 - 1"), // Actually this -2147483648 (0x80000000) but due to bug SNAP-389 the workaround must be used
                parser.parse("31")
        }));

    }
}
