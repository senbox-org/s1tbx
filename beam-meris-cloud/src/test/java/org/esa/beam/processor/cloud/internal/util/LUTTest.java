/*
 * $Id: LUTTest.java,v 1.1 2006/10/10 14:47:34 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.processor.cloud.internal.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class LUTTest extends TestCase {

    public LUTTest(final String s) {
        super(s);
    }

    public static Test suite() {
        return new TestSuite(LUTTest.class);
    }


    public void testLUTProperties() {
        final float[][] javaArray = new float[][]{{10f, 12f, 13f}, {20f, 22f, 23f}};
        final double[] tab1 = new double[]{1, 2};
        final double[] tab2 = new double[]{3, 4, 5};

        final LUT lut = new LUT(javaArray);
        assertSame(javaArray, lut.getJavaArray());
        assertEquals(2, lut.getRank());
        assertEquals(2, lut.getDimSize(0));
        assertEquals(3, lut.getDimSize(1));
        assertEquals(6, lut.getElementCount());
        assertEquals(null, lut.getTab(0));
        assertEquals(null, lut.getTab(1));
        lut.setTab(0, tab1);
        lut.setTab(1, tab2);
        assertSame(tab1, lut.getTab(0));
        assertSame(tab2, lut.getTab(1));
    }

    public void testThatNullTabsAreAllowed() {
        final LUT lut = create2x3Lut();
        assertEquals(null, lut.getTab(0));
        try {
            lut.setTab(0, null);
        } catch (RuntimeException notExpected) {
            fail();
        }
    }

    public void testThatSetTabChecksArgs() {
        final LUT lut = new LUT(new float[][]{{10f, 12f, 13f}, {20f, 22f, 23f}});
        try {
            lut.setTab(0, new double[]{1});
            fail("tab size wrong");
        } catch (RuntimeException expected) {
        }
        try {
            lut.setTab(2, new double[]{1, 2});
            fail("tab index wrong");
        } catch (RuntimeException expected) {
        }
    }

    private LUT create2x3Lut() {
        return new LUT(new float[][]{{10f, 12f, 13f}, {20f, 22f, 23f}});
    }

}

