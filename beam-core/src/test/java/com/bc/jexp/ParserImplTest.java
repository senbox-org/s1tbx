package com.bc.jexp;

import junit.framework.TestCase;
import com.bc.jexp.impl.ParserImpl;

public class ParserImplTest extends TestCase {

    public void testTokens() throws ParseException {
        final EvalEnv env = new EvalEnv() {
        };
        final ParserImpl parser = new ParserImpl();

        Term term;

        term = parser.parse("true");
        assertEquals(true, term.evalB(env));
        assertEquals(1, term.evalI(env));
        assertEquals(1.0, term.evalD(env));
        assertEquals("true", term.evalS(env));
        assertEquals("true", term.toString());

        term = parser.parse("85321");
        assertEquals(true, term.evalB(env));
        assertEquals(85321, term.evalI(env));
        assertEquals(85321.0, term.evalD(env));
        assertEquals("85321", term.evalS(env));
        assertEquals("85321", term.toString());

        term = parser.parse("1.79");
        assertEquals(true, term.evalB(env));
        assertEquals(1, term.evalI(env));
        assertEquals(1.79, term.evalD(env));
        assertEquals("1.79", term.evalS(env));
        assertEquals("1.79", term.toString());

        term = parser.parse("\"Some text.\"");
        try {
            assertEquals(false, term.evalB(env));
            fail("EvalException?");
        } catch (EvalException e) {
        }
        try {
            assertEquals(0, term.evalI(env));
            fail("EvalException?");
        } catch (EvalException e) {
        }
        try {
            assertEquals(0.0, term.evalD(env));
            fail("EvalException?");
        } catch (EvalException e) {
        }
        assertEquals("Some text.", term.evalS(env));
        assertEquals("\"Some text.\"", term.toString());

        term = parser.parse("\"374\"");
        try {
            assertEquals(true, term.evalB(env));
            fail("EvalException?");
        } catch (EvalException e) {
        }
        assertEquals(374, term.evalI(env));
        assertEquals(374.0, term.evalD(env));
        assertEquals("374", term.evalS(env));
        assertEquals("\"374\"", term.toString());
    }
}
