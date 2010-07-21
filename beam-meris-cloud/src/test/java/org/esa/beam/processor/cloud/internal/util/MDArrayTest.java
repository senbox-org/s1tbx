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
package org.esa.beam.processor.cloud.internal.util;

import java.lang.reflect.Array;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class MDArrayTest extends TestCase {

    public static final double IDX_OFFSET = 0.34;
    public static final double IDX_FACTOR = 2.15;

    public MDArrayTest(final String s) {
        super(s);
    }

    public static Test suite() {
        return new TestSuite(MDArrayTest.class);
    }

    public void testConstructor() {
        MDArray array;
        Object elements;

        elements = new short[5];
        array = new MDArray(elements);
        assertSame(elements, array.getJavaArray());
        assertEquals(1, array.getRank());
        assertEquals(Short.TYPE, array.getElementType());
        assertEquals(5, array.getElementCount());
        assertEquals(5, array.getDimSize(0));

        elements = new double[5][3][9];
        array = new MDArray(elements);
        assertSame(elements, array.getJavaArray());
        assertEquals(3, array.getRank());
        assertEquals(Double.TYPE, array.getElementType());
        assertEquals(5 * 3 * 9, array.getElementCount());
        assertEquals(5, array.getDimSize(0));
        assertEquals(3, array.getDimSize(1));
        assertEquals(9, array.getDimSize(2));

        try {
            elements = new int[0][1];
            array = new MDArray(elements);
            fail("IllegalArgumentException expected, zero dim size not supported");
        } catch (IllegalArgumentException e) {
        }

        try {
            elements = new int[1][0];
            array = new MDArray(elements);
            fail("IllegalArgumentException expected, zero dim size not supported");
        } catch (IllegalArgumentException e) {
        }

        try {
            elements = "Hallo!";
            array = new MDArray(elements);
            fail("IllegalArgumentException expected, not an array");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testCreateLayout() {
        final MDArray.Layout descriptor;

        final Object array = new double[3][23][5][43];
        descriptor = MDArray.createLayout(array);
        assertEquals(4, descriptor.getRank());
        assertEquals(3, descriptor.getDimSize(0));
        assertEquals(23, descriptor.getDimSize(1));
        assertEquals(5, descriptor.getDimSize(2));
        assertEquals(43, descriptor.getDimSize(3));
        assertEquals(double.class, descriptor.getElementType());
        assertEquals(14835, descriptor.getElementCount());
    }

    public void testCreateDoubleArrayFromFlatFloat() {
        final int[] sizes = new int[]{2, 3, 4};
        final float[] flatArray = new float[sizes[0] * sizes[1] * sizes[2]];
        for (int i = 0; i < flatArray.length; i++) {
            flatArray[i] = (float) getTestDoubleValue(i);
        }
        testCreateDoubleArrayFromFlat(sizes, flatArray);
    }

    public void testCreateDoubleArrayFromFlatDouble() {
        final int[] sizes = new int[]{2, 3, 4};
        final double[] flatArray = new double[sizes[0] * sizes[1] * sizes[2]];
        for (int i = 0; i < flatArray.length; i++) {
            flatArray[i] = getTestDoubleValue(i);
        }
        testCreateDoubleArrayFromFlat(sizes, flatArray);
    }

    private void testCreateDoubleArrayFromFlat(final int[] sizes, final Object flatArray) {
        final MDArray array = new MDArray(double.class, sizes, flatArray);
        assertEquals(sizes.length, array.getRank());
        assertEquals(double.class, array.getElementType());
        assertEquals(sizes[0] * sizes[1] * sizes[2], array.getElementCount());
        assertEquals(sizes[0], array.getDimSize(0));
        assertEquals(sizes[1], array.getDimSize(1));
        assertEquals(sizes[2], array.getDimSize(2));

        assertEquals(double[][][].class, array.getJavaArray().getClass());
        final double[][][] aaa = (double[][][]) array.getJavaArray();
        int n = 0;
        for (int i = 0; i < array.getDimSize(0); i++) {
            for (int j = 0; j < array.getDimSize(1); j++) {
                for (int k = 0; k < array.getDimSize(2); k++) {
                    final double expected = getTestDoubleValue(n);
                    final double actual = aaa[i][j][k];
                    assertEquals(expected, actual, 1e-5);
                    n++;
                }
            }
        }
    }

    private double getTestDoubleValue(final int i) {
        return i * IDX_FACTOR + IDX_OFFSET;
    }

    public void testJavaArrayPrerequisites() {
        final Object javaArray = Array.newInstance(double.class, new int[]{4, 3, 2});
        final Class arrayType = javaArray.getClass();
        assertEquals(double[][][].class, arrayType);
        assertEquals("[[[D", arrayType.getName());
        assertEquals(double.class, arrayType.getComponentType().getComponentType().getComponentType());
        assertEquals("double", arrayType.getComponentType().getComponentType().getComponentType().getName());
    }


    public void testGetElementCount() {
        assertEquals(3L, MDArray.getElementCount(new int[]{3}));
        assertEquals(12L, MDArray.getElementCount(new int[]{3, 4}));
        assertEquals(60L, MDArray.getElementCount(new int[]{3, 4, 5}));
        assertEquals(120L, MDArray.getElementCount(new int[]{3, 4, 5, 2}));
        assertEquals(0L, MDArray.getElementCount(new int[]{3, 4, 0, 2}));

        try {
            MDArray.getElementCount(new int[]{});
            fail("RuntimeException expected");
        } catch (RuntimeException e) {
            // ok
        }
    }

    public void testGetElementOffset() {
        assertEquals(0L, MDArray.getElementOffset(new int[]{3}, new int[]{0}));
        assertEquals(1L, MDArray.getElementOffset(new int[]{3}, new int[]{1}));
        assertEquals(2L, MDArray.getElementOffset(new int[]{3}, new int[]{2}));
        assertEquals(3L, MDArray.getElementOffset(new int[]{3}, new int[]{3}));

        assertEquals(20939469952L,
                MDArray.getElementOffset(new int[]{473, 64, 276, 844, 6},
                        new int[]{234, 5, 210, 354, 4}));

    }

}

