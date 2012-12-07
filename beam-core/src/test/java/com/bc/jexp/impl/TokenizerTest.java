package com.bc.jexp.impl;

import static org.junit.Assert.*;

import org.junit.*;

public class TokenizerTest {

    @Test
    public void testPlusFour() {
        final Tokenizer tokenizer = new Tokenizer("+4");
        assertEquals('+', tokenizer.next());
        assertEquals('+', tokenizer.getType());
        assertEquals("+", tokenizer.getToken());
        assertEquals(Tokenizer.TT_INT, tokenizer.next());
        assertEquals(Tokenizer.TT_INT, tokenizer.getType());
        assertEquals("4", tokenizer.getToken());
        assertEquals(Tokenizer.TT_EOS, tokenizer.next());
    }

    @Test
    public void testMinusFour() {
        final Tokenizer tokenizer = new Tokenizer("-4");
        assertEquals(Tokenizer.TT_INT, tokenizer.next());
        assertEquals(Tokenizer.TT_INT, tokenizer.getType());
        assertEquals("-4", tokenizer.getToken());
        assertEquals(Tokenizer.TT_EOS, tokenizer.next());
    }

    @Test
    public void testThreePlusFour() {
        final Tokenizer tokenizer = new Tokenizer("3+4");
        assertEquals(Tokenizer.TT_INT, tokenizer.next());
        assertEquals(Tokenizer.TT_INT, tokenizer.getType());
        assertEquals("3", tokenizer.getToken());
        assertEquals('+', tokenizer.next());
        assertEquals('+', tokenizer.getType());
        assertEquals("+", tokenizer.getToken());
        assertEquals(Tokenizer.TT_INT, tokenizer.next());
        assertEquals(Tokenizer.TT_INT, tokenizer.getType());
        assertEquals("4", tokenizer.getToken());
        assertEquals(Tokenizer.TT_EOS, tokenizer.next());
    }

    @Test
    public void testThreeMinusFour() {
        final Tokenizer tokenizer = new Tokenizer("3-4");
        assertEquals(Tokenizer.TT_INT, tokenizer.next());
        assertEquals(Tokenizer.TT_INT, tokenizer.getType());
        assertEquals("3", tokenizer.getToken());
        assertEquals(Tokenizer.TT_INT, tokenizer.next());
        assertEquals(Tokenizer.TT_INT, tokenizer.getType());
        assertEquals("-4", tokenizer.getToken());
        assertEquals(Tokenizer.TT_EOS, tokenizer.next());
    }

    @Test
    public void testParseInt() {
        Integer.parseInt("4");
        Integer.parseInt("-4");
        try {
            Integer.parseInt("+4");
            fail("Should not come here.");
        } catch (NumberFormatException ignore) {
        }
    }

    @Test
    public void testParseLong() {
        Long.parseLong("4");
        Long.parseLong("-4");
        try {
            Long.parseLong("+4");
            fail("Should not come here.");
        } catch (NumberFormatException ignore) {
        }
    }
}
