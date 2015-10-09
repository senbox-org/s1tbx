/*
 * $Id: FormattedReaderTest.java,v 1.3 2006/07/24 14:40:25 marcop Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.snap.core.nn;

import junit.framework.TestCase;

import java.io.IOException;
import java.io.StringReader;

public class FormattedStringReaderTest extends TestCase {

    private static final double EPS = 1.0e-8;

    private FormattedStringReader r;

    @Override
    protected void setUp() throws Exception {
        r = new FormattedStringReader(new StringReader("ranges repeated for easier input\n" +
                                                               "#\n" +
                                                               "6\n" +
                                                               "-1.610930\n" +
                                                               "3.998400\n" +
                                                               "-5.928960 3.881530\n" +
                                                               "-4.234020 8.997880\n" +
                                                               "198134 4493 -72345\n" +
                                                               "$\n" +
                                                               "#planes=3 6 50 1\n" +
                                                               "bias 1 50\n" +
                                                               "7.890334 8.897559 8.359957\n" +
                                                               "-23.919953 44.968232 4.968232"));
    }

    @Override
    protected void tearDown() throws Exception {
        r = null;
    }

    public void testX() throws IOException {

        assertEquals("ranges repeated for easier input", r.rString());
        assertEquals(6, r.rlong());
        assertEquals(-1.61093, r.rdouble(), EPS);
        assertEquals(3.9984, r.rdouble(), EPS);

        final double[] d4 = r.rdouble(4);
        assertEquals(4, d4.length);
        assertEquals(-5.92896, d4[0], EPS);
        assertEquals(3.88153, d4[1], EPS);
        assertEquals(-4.23402, d4[2], EPS);
        assertEquals(8.99788, d4[3], EPS);

        final long[] l3 = r.rlong(3);
        assertEquals(3, l3.length);
        assertEquals(198134, l3[0]);
        assertEquals(4493, l3[1]);
        assertEquals(-72345, l3[2]);

        assertEquals("$", r.rString());
        assertEquals("bias 1 50", r.rString());
        final double[][] d23 = r.rdoubleAll();
        assertEquals(2, d23.length);
        assertEquals(3, d23[0].length);
        assertEquals(3, d23[1].length);
        assertEquals(7.890334, d23[0][0], EPS);
        assertEquals(8.897559, d23[0][1], EPS);
        assertEquals(8.359957, d23[0][2], EPS);
        assertEquals(-23.919953, d23[1][0], EPS);
        assertEquals(44.968232, d23[1][1], EPS);
        assertEquals(4.968232, d23[1][2], EPS);
    }
}
