/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.jexp;

import junit.framework.TestCase;
import com.bc.jexp.impl.ParserImpl;
import com.bc.jexp.impl.SymbolFactory;

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


        term = parser.parse("NaN");
        assertTrue(Double.isNaN(term.evalD(env)));

        // Band arithmetic: y = log(x), pixel value x=0
        term = parser.parse("log(0)");
        double d = term.evalD(env);

        final Variable x = SymbolFactory.createVariable("x", 0.0);
        ((WritableNamespace)parser.getDefaultNamespace()).registerSymbol(x);
        term = parser.parse("inf(x) || nan(x)");

        x.assignD(env, 0.0);
        assertEquals(false, term.evalB(env));

        x.assignD(env, Math.log(0));
        assertEquals(true, term.evalB(env));

        x.assignD(env, (double)(float)Math.log(0));
        assertEquals(true, term.evalB(env));

        x.assignD(env, 0.0);
        assertEquals(false, term.evalB(env));

        x.assignD(env, Math.sqrt(-1));
        assertEquals(true, term.evalB(env));

        x.assignD(env, (double)(float)Math.sqrt(-1));
        assertEquals(true, term.evalB(env));
    }
}
