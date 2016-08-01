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

package org.esa.snap.core.jexp.impl;

import org.esa.snap.core.jexp.EvalEnv;
import org.esa.snap.core.jexp.EvalException;
import org.esa.snap.core.jexp.ParseException;
import org.esa.snap.core.jexp.Symbol;
import org.esa.snap.core.jexp.Term;
import org.esa.snap.core.jexp.Variable;
import org.esa.snap.core.jexp.WritableNamespace;
import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.*;

public class ParserImplTest {

    private EvalEnv env;
    private ParserImpl parser;

    @Before
    public void setUp() throws Exception {
        env = new EvalEnv() {
        };
        parser = new ParserImpl();
    }

    @Test
    public void testBooleanConst() throws ParseException {
        Term term = parser.parse("true");
        assertEquals(true, term.evalB(env));
        assertEquals(1, term.evalI(env));
        assertEquals(1.0, term.evalD(env), 0.0);
        assertEquals("true", term.evalS(env));
        assertEquals("true", term.toString());
    }

    @Test
    public void testIntegerConst() throws ParseException {
        Term term = parser.parse("85321");
        assertEquals(true, term.evalB(env));
        assertEquals(85321, term.evalI(env));
        assertEquals(85321.0, term.evalD(env), 0.0);
        assertEquals("85321", term.evalS(env));
        assertEquals("85321", term.toString());
    }

    @Test
    public void testIntegerMaxValueConst() throws ParseException {
        final int maxValue = Integer.MAX_VALUE;
        final String maxValueString = Integer.toString(maxValue);
        Term term = parser.parse(maxValueString);
        assertEquals(true, term.evalB(env));
        assertEquals(maxValue, term.evalI(env));
        assertEquals(maxValue, term.evalD(env), 0.0);
        assertEquals(maxValueString, term.evalS(env));
        assertEquals(maxValueString, term.toString());
    }

    @Test
    public void testIntegerMinValueConst() throws ParseException {
        final int minValue = Integer.MIN_VALUE;
        final String minValueString = Integer.toString(minValue);
        try {
            parser.parse(minValueString);
            fail("ParseException expected");
        } catch (ParseException e) {
            assertEquals("Integer constant '2147483648' is out of range.", e.getMessage());
        }

        /*
        assertEquals(true, term.evalB(env));
        assertEquals(minValue, term.evalI(env));
        assertEquals(minValue, term.evalD(env), 0.0);
        assertEquals(minValueString, term.evalS(env));
        assertEquals(minValueString, term.toString());
        */
    }

    @Test
    public void testPositiveNumber() throws ParseException {
        Term term = parser.parse("+4");
        assertEquals(true, term instanceof Term.ConstI);
        assertEquals(4, term.evalI(env));
        assertEquals("4", term.evalS(env));
        assertEquals("4", term.toString());

        term = parser.parse("4");
        assertEquals(true, term instanceof Term.ConstI);
        assertEquals(4, term.evalI(env));
        assertEquals("4", term.evalS(env));
        assertEquals("4", term.toString());

        term = parser.parse("+0.4");
        assertEquals(true, term instanceof Term.ConstD);
        assertEquals(0.4, term.evalD(env), 1e-15);
        assertEquals("0.4", term.evalS(env));
        assertEquals("0.4", term.toString());

        term = parser.parse("0.4");
        assertEquals(true, term instanceof Term.ConstD);
        assertEquals(0.4, term.evalD(env), 1e-15);
        assertEquals("0.4", term.evalS(env));
        assertEquals("0.4", term.toString());

        term = parser.parse("+.4");
        assertEquals(true, term instanceof Term.ConstD);
        assertEquals(0.4, term.evalD(env), 1e-15);
        assertEquals("0.4", term.evalS(env));
        assertEquals("0.4", term.toString());

        term = parser.parse(".4");
        assertEquals(true, term instanceof Term.ConstD);
        assertEquals(0.4, term.evalD(env), 1e-15);
        assertEquals("0.4", term.evalS(env));
        assertEquals("0.4", term.toString());
    }

    @Test
    public void testNegativeNumber() throws ParseException {
        Term term = parser.parse("-4");
        assertEquals(true, term instanceof Term.ConstI);
        assertEquals(-4, term.evalI(env));
        assertEquals("-4", term.evalS(env));
        assertEquals("-4", term.toString());

        term = parser.parse("-0.4");
        assertEquals(true, term instanceof Term.ConstD);
        assertEquals(-0.4, term.evalD(env), 1e-15);
        assertEquals("-0.4", term.evalS(env));
        assertEquals("-0.4", term.toString());

        term = parser.parse("-.4");
        assertEquals(true, term instanceof Term.ConstD);
        assertEquals(-0.4, term.evalD(env), 1e-15);
        assertEquals("-0.4", term.evalS(env));
        assertEquals("-0.4", term.toString());
    }

    @Test
    public void testAddition() throws ParseException {
        final Term term = parser.parse("24 + 4");
        assertEquals(28, term.evalI(env));
    }

    @Test
    public void testSubstraction() throws ParseException {
        final Term term = parser.parse("24 - 4");
        assertEquals(20, term.evalI(env));
    }

    @Test
    public void testAdditionNumber() throws ParseException {
        final Term term = parser.parse("24 +4");
        assertEquals(28, term.evalI(env));
    }

    @Test
    public void testSubstractionNumber() throws ParseException {
        final Term term = parser.parse("24 -4");
        assertEquals(20, term.evalI(env));
    }

    @Test
    public void testSubstractionNegative() throws ParseException {
        Term term = parser.parse("24 - -4");
        assertEquals(28, term.evalI(env));

        term = parser.parse("24--4");
        assertEquals(28, term.evalI(env));

        term = parser.parse("24-(-4)");
        assertEquals(28, term.evalI(env));

        term = parser.parse("(24--4)");
        assertEquals(28, term.evalI(env));

        term = parser.parse("24.0--------------------++++++++++++++++++++++(4)");
        assertEquals(28, term.evalI(env));
    }

    @Test
    public void testSubstractionPositive() throws ParseException {
        final Term term = parser.parse("24 - +4");
        assertEquals(20, term.evalI(env));
    }

    @Test
    public void testSubstractionWithoutSpaces() throws ParseException {
        final Term term = parser.parse("24-4");
        assertEquals(20, term.evalI(env));
    }

    @Test
    public void testAdditionWithoutSpaces() throws ParseException {
        final Term term = parser.parse("24+4");
        assertEquals(28, term.evalI(env));
    }

    @Test
    public void testFloatConst() throws ParseException {
        Term term = parser.parse("1.79");
        assertEquals(true, term.evalB(env));
        assertEquals(1, term.evalI(env));
        assertEquals(1.79, term.evalD(env), 1e-10);
        assertEquals("1.79", term.evalS(env));
        assertEquals("1.79", term.toString());
    }

    @Test
    public void testStringConst() throws ParseException {
        Term term = parser.parse("\"Some text.\"");
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
            assertEquals(0.0, term.evalD(env), 0.0);
            fail("EvalException?");
        } catch (EvalException e) {
        }
        assertEquals("Some text.", term.evalS(env));
        assertEquals("\"Some text.\"", term.toString());
    }

    @Test
    public void testNumberString() throws ParseException {
        Term term = parser.parse("\"374\"");
        try {
            assertEquals(true, term.evalB(env));
            fail("EvalException?");
        } catch (EvalException e) {
        }
        assertEquals(374, term.evalI(env));
        assertEquals(374.0, term.evalD(env), 1e-10);
        assertEquals("374", term.evalS(env));
        assertEquals("\"374\"", term.toString());
    }

    @Test
    public void testNaNConst() throws ParseException {
        Term term = parser.parse("NaN");
        assertTrue(Double.isNaN(term.evalD(env)));
    }

    @Test
    public void testNaNAndInf() throws ParseException {

        final Variable x = SymbolFactory.createVariable("x", 0.0);
        ((WritableNamespace) parser.getDefaultNamespace()).registerSymbol(x);

        Term term = parser.parse("inf(x) || nan(x)");

        x.assignD(env, 0.0);
        assertEquals(false, term.evalB(env));

        x.assignD(env, Math.log(0));
        assertEquals(true, term.evalB(env));

        x.assignD(env, (double) (float) Math.log(0));
        assertEquals(true, term.evalB(env));

        x.assignD(env, 0.0);
        assertEquals(false, term.evalB(env));

        x.assignD(env, Math.sqrt(-1));
        assertEquals(true, term.evalB(env));

        x.assignD(env, (double) (float) Math.sqrt(-1));
        assertEquals(true, term.evalB(env));
    }

    @Test
    public void testConditional() throws ParseException {
        testConditional("x > 0 ? 1 : 0");
        testConditional("IF x > 0 THEN 1 ELSE 0");
    }

    private void testConditional(String code) throws ParseException {
        final Variable x = SymbolFactory.createVariable("x", 0.0);
        try {
            ((WritableNamespace) parser.getDefaultNamespace()).registerSymbol(x);
            Term term = parser.parse(code);

            assertNotNull(term);
            assertNotNull(term.getChildren());
            int i = term.getChildren().length;
            assertEquals(3, i);

            x.assignI(null, -10);
            assertEquals(0, term.evalI(env));

            x.assignI(null, 0);
            assertEquals(0, term.evalI(env));

            x.assignI(null, 10);
            assertEquals(1, term.evalI(env));
        } finally {
            ((WritableNamespace) parser.getDefaultNamespace()).deregisterSymbol(x);
        }
    }

    @Test
    public void testSymbolStartingWithNumber() throws ParseException {
        final Variable band = SymbolFactory.createVariable("250m_16_days_EVI", 0.0);
        try {
            ((WritableNamespace) parser.getDefaultNamespace()).registerSymbol(band);
            parser.parse("'250m_16_days_EVI'");
        } finally {
            ((WritableNamespace) parser.getDefaultNamespace()).deregisterSymbol(band);
        }
    }

    @Test
    public void testValidVariableDExpression() throws ParseException {
        Term term;

        term = parser.parse("var(\"a\", 0.9)");
        assertEquals(0.9, term.evalD(env), 1.e-10);

        term = parser.parse("var(\"a\", 0.2, 0.0, 1.0)");
        assertEquals(0.2, term.evalD(env), 1.e-10);

        term = parser.parse("var(\"a\", 0.5, 0.0, 1.0, 0.1)");
        assertEquals(0.5, term.evalD(env), 1.e-10);

        term = parser.parse("a");
        assertEquals(0.5, term.evalD(env), 1.e-10);

        Symbol a = parser.getDefaultNamespace().resolveSymbol("a");
        assertTrue(a instanceof Variable);
        ((Variable) a).assignD(null, 0.1);

        assertEquals(0.1, term.evalD(env), 1.e-10);
    }

    @Test
    public void testInvalidVariableDExpression() {

        try {
            parser.parse("var(2, 0.9, 0.1)");
        } catch (ParseException e) {
            assertEquals("<name> must be a string constant: var(<name>, <value> [, <min>, <max> [, <step>]])", e.getMessage());
        }

        try {
            parser.parse("var(\"a\", 0.9, 0.1)");
        } catch (ParseException e) {
            assertEquals("Wrong number of arguments: var(<name>, <value> [, <min>, <max> [, <step>]])", e.getMessage());
        }

        try {
            parser.parse("var(\"a\", 0.2, 0.0, true)");
        } catch (ParseException e) {
            assertEquals("<max> must be numeric: var(<name>, <val> [, <min>, <max> [, <step>]])", e.getMessage());
        }

        try {
            parser.parse("var(\"a\", 0.5, 0.0, 1.0, 0.1, 2)");
        } catch (ParseException e) {
            assertEquals("Wrong number of arguments: var(<name>, <value> [, <min>, <max> [, <step>]])", e.getMessage());
        }
    }

    @Test
    public void testNestedConditionalIfThenElse() throws ParseException {
        testNestedConditional("x > 0 ? 1 : x < 0 ? -1 : 0");
        testNestedConditional("x <= 0 ? (x==0?0:-1):1");
        testNestedConditional("IF x > 0 THEN 1 ELSE (if x < 0 then -1 else 0)");
        testNestedConditional("IF x > 0 THEN 1 ELSE IF x < 0 THEN -1 ELSE 0");
        testNestedConditional("IF x <= 0 THEN (if x == 0 then 0 else -1) ELSE 1");
    }

    private void testNestedConditional(String code) throws ParseException {
        final Variable x = SymbolFactory.createVariable("x", 0.0);
        try {
            ((WritableNamespace) parser.getDefaultNamespace()).registerSymbol(x);

            Term term = parser.parse(code);
            assertNotNull(term);
            assertNotNull(term.getChildren());
            int i = term.getChildren().length;
            assertEquals(3, i);

            x.assignI(null, -10);
            assertEquals(-1, term.evalI(env));

            x.assignI(null, 0);
            assertEquals(0, term.evalI(env));

            x.assignI(null, 10);
            assertEquals(1, term.evalI(env));
        } finally {
            ((WritableNamespace) parser.getDefaultNamespace()).deregisterSymbol(x);
        }
    }

    @Test
    public void testDistanceFunction() throws ParseException {
        Term term = parser.parse("distance(0.1, 0.2, 0.3, 0.4, 0.3, 0.1)");
        assertNotNull(term);
        assertNotNull(term.getChildren());
        assertEquals(6, term.getChildren().length);

        double d1 = 0.1 - 0.4;
        double d2 = 0.2 - 0.3;
        double d3 = 0.3 - 0.1;
        assertEquals(Math.sqrt(d1 * d1 + d2 * d2 + d3 * d3), term.evalD(env), 1.e-10);
    }

    @Test
    public void testDistanceDerivFunction() throws ParseException {
        Term term = parser.parse("distance_deriv(0.1, 0.2, 0.3, 1.4, 1.3, 1.1)");
        assertNotNull(term);
        assertNotNull(term.getChildren());
        assertEquals(6, term.getChildren().length);

        double d1 = (0.2 - 0.1) - (1.3 - 1.4);
        double d2 = (0.3 - 0.2) - (1.1 - 1.3);
        assertEquals(Math.sqrt(d1 * d1 + d2 * d2), term.evalD(env), 1.e-10);
    }

    @Test
    public void testDistanceIntegFunction() throws ParseException {
        Term term = parser.parse("distance_integ(0.1, 0.2, 0.3, 1.4, 1.3, 1.1)");
        assertNotNull(term);
        assertNotNull(term.getChildren());
        assertEquals(6, term.getChildren().length);

        double d1 = (0.1) - (1.4);
        double d2 = (0.1 + 0.2) - (1.4 + 1.3);
        double d3 = (0.1 + 0.2 + 0.3) - (1.4 + 1.3 + 1.1);
        assertEquals(Math.sqrt(d1 * d1 + d2 * d2 + d3 * d3), term.evalD(env), 1.e-10);
    }

    int ix;
    double dx;

    @Test
    public void testPow() throws ParseException {
        NamespaceImpl namespace = new NamespaceImpl(new DefaultNamespace());
        namespace.registerSymbol(new AbstractSymbol.I("ix") {
            @Override
            public int evalI(EvalEnv env) throws EvalException {
                return ix;
            }
        });
        namespace.registerSymbol(new AbstractSymbol.D("dx") {
            @Override
            public double evalD(EvalEnv env) throws EvalException {
                return dx;
            }
        });
        ParserImpl parser = new ParserImpl(namespace);
        ix = 1;
        dx = 1.0;
        assertEquals(10.0, parser.parse("pow(10, ix)").evalD(null), 1E-10);
        assertEquals(10.0, parser.parse("pow(10, ix)").evalD(null), 1E-10);
        assertEquals(10.0, parser.parse("pow(10, dx)").evalD(null), 1E-10);
        assertEquals(10.0, parser.parse("pow(10, dx)").evalD(null), 1E-10);
        ix = 2;
        dx = 2.0;
        assertEquals(100.0, parser.parse("pow(10.0, ix)").evalD(null), 1E-10);
        assertEquals(100.0, parser.parse("pow(10.0, ix)").evalD(null), 1E-10);
        assertEquals(100.0, parser.parse("pow(10.0, dx)").evalD(null), 1E-10);
        assertEquals(100.0, parser.parse("pow(10.0, dx)").evalD(null), 1E-10);
    }

}
