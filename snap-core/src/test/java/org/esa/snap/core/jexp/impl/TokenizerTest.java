package org.esa.snap.core.jexp.impl;

import org.junit.Test;

import static org.junit.Assert.*;

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
        assertEquals('-', tokenizer.next());
        assertEquals('-', tokenizer.getType());
        assertEquals("-", tokenizer.getToken());
        assertEquals(Tokenizer.TT_INT, tokenizer.next());
        assertEquals(Tokenizer.TT_INT, tokenizer.getType());
        assertEquals("4", tokenizer.getToken());
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
        assertEquals('-', tokenizer.next());
        assertEquals('-', tokenizer.getType());
        assertEquals("-", tokenizer.getToken());
        assertEquals(Tokenizer.TT_INT, tokenizer.next());
        assertEquals(Tokenizer.TT_INT, tokenizer.getType());
        assertEquals("4", tokenizer.getToken());
        assertEquals(Tokenizer.TT_EOS, tokenizer.next());
    }

    @Test
    public void testTokenizerWithBandNames() throws Exception {
        Tokenizer tokenizer = new Tokenizer("algal");
        assertEquals(Tokenizer.TT_NAME, tokenizer.next());
        assertEquals("algal", tokenizer.getToken());
        tokenizer = new Tokenizer("'250m_16_days_EVI'");
        assertEquals(Tokenizer.TT_ESCAPED_NAME, tokenizer.next());
        assertEquals("250m_16_days_EVI", tokenizer.getToken());
    }

    @Test
    public void testParseInt() {
        assertEquals(4, Integer.parseInt("4"));
        assertEquals(-4, Integer.parseInt("-4"));
        assertEquals(4, Integer.parseInt("+4"));
    }

    @Test
    public void testParseLong() {
        assertEquals(4, Long.parseLong("4"));
        assertEquals(-4, Long.parseLong("-4"));
        assertEquals(4, Long.parseLong("+4"));
    }
}
