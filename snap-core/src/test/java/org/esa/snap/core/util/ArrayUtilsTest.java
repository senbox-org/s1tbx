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
package org.esa.snap.core.util;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import java.util.Arrays;

public class ArrayUtilsTest extends TestCase {

    public ArrayUtilsTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ArrayUtilsTest.class);
    }

    public void testEqualFloatArrays() {
        float[] a = {2.4f, 3.7f, 0.0005423f, -424.6f};
        float[] b = {2.4f, 3.7f, 0.0005423f, -424.6f};
        float[] c = {2.4f, 3.7f, 0.0005657f, -424.6f};
        float[] d = {2.4f, 3.7f, 0.0005423f};
        float[] e = a;
        assertEquals(true, ArrayUtils.equalArrays(a, b, 1e-5f));
        assertEquals(false, ArrayUtils.equalArrays(a, c, 1e-5f));
        assertEquals(false, ArrayUtils.equalArrays(a, d, 1e-5f));
        assertEquals(true, ArrayUtils.equalArrays(a, e, 1e-5f));
        assertEquals(true, ArrayUtils.equalArrays((float[]) null, (float[]) null, 1e-5f));
        assertEquals(false, ArrayUtils.equalArrays(a, null, 1e-5f));
        assertEquals(false, ArrayUtils.equalArrays(null, a, 1e-5f));
    }

    public void testEqualDoubleArrays() {
        double[] a = {2.4, 3.7, 0.0005423, -424.6};
        double[] b = {2.4, 3.7, 0.0005423, -424.6};
        double[] c = {2.4, 3.7, 0.0005657, -424.6};
        double[] d = {2.4, 3.7, 0.0005423};
        double[] e = a;
        assertEquals(true, ArrayUtils.equalArrays(a, b, 1e-10));
        assertEquals(false, ArrayUtils.equalArrays(a, c, 1e-10));
        assertEquals(false, ArrayUtils.equalArrays(a, d, 1e-10));
        assertEquals(true, ArrayUtils.equalArrays(a, e, 1e-10));
        assertEquals(true, ArrayUtils.equalArrays(null, null, 1e-10));
        assertEquals(false, ArrayUtils.equalArrays(a, null, 1e-10));
        assertEquals(false, ArrayUtils.equalArrays(null, a, 1e-10));
    }

    public void testEqualArrays() {
        Object[] a = {"A", "B", "C"};
        Object[] b = {"A", "B", "C"};
        Object[] c = {"1", "2", "3"};
        Object[] d = {"A", "B", "C", "D"};
        Object[] e = a;
        assertEquals(ArrayUtils.equalArrays(a, b), true);
        assertEquals(ArrayUtils.equalArrays(a, c), false);
        assertEquals(ArrayUtils.equalArrays(a, d), false);
        assertEquals(ArrayUtils.equalArrays(a, e), true);
        assertEquals(ArrayUtils.equalArrays(null, null), true);
        assertEquals(ArrayUtils.equalArrays(a, null), false);
        assertEquals(ArrayUtils.equalArrays(null, a), false);
    }

    public void testGetElementIndexAndIsMemberOf() {
        Object[] array1 = {"A", "B", "C"};
        assertEquals(0, ArrayUtils.getElementIndex("A", array1));
        assertEquals(1, ArrayUtils.getElementIndex("B", array1));
        assertEquals(2, ArrayUtils.getElementIndex("C", array1));
        assertEquals(-1, ArrayUtils.getElementIndex("D", array1));
        assertTrue(ArrayUtils.isMemberOf("A", array1));
        assertTrue(ArrayUtils.isMemberOf("B", array1));
        assertTrue(ArrayUtils.isMemberOf("C", array1));
        assertTrue(!ArrayUtils.isMemberOf("D", array1));

        Object[] array2 = {"A", new Object[]{"B1", "B2"}, "C"};
        assertEquals(0, ArrayUtils.getElementIndex("A", array2));
        assertEquals(1, ArrayUtils.getElementIndex(new Object[]{"B1", "B2"}, array2));
        assertEquals(2, ArrayUtils.getElementIndex("C", array2));
        assertEquals(-1, ArrayUtils.getElementIndex("D", array2));
        assertTrue(ArrayUtils.isMemberOf("A", array2));
        assertTrue(ArrayUtils.isMemberOf(new Object[]{"B1", "B2"}, array2));
        assertTrue(ArrayUtils.isMemberOf("C", array2));
        assertTrue(!ArrayUtils.isMemberOf("D", array2));
    }

    public void testSwapByteArray() {
        final byte[] array0 = {};
        final byte[] array1 = {1};
        final byte[] array2 = {1, 2};
        final byte[] array3 = {1, 2, 3};
        final byte[] array10 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        final byte[] array0Rev = {};
        final byte[] array1Rev = {1};
        final byte[] array2Rev = {2, 1};
        final byte[] array3Rev = {3, 2, 1};
        final byte[] array10Rev = {10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        ArrayUtils.swapArray(array0);
        assertTrue(Arrays.equals(array0Rev, array0));
        ArrayUtils.swapArray(array1);
        assertTrue(Arrays.equals(array1Rev, array1));
        ArrayUtils.swapArray(array2);
        assertTrue(Arrays.equals(array2Rev, array2));
        ArrayUtils.swapArray(array3);
        assertTrue(Arrays.equals(array3Rev, array3));
        ArrayUtils.swapArray(array10);
        assertTrue(Arrays.equals(array10Rev, array10));
    }

    public void testSwapCharArray() {
        final char[] array0 = {};
        final char[] array1 = {1};
        final char[] array2 = {1, 2};
        final char[] array3 = {1, 2, 3};
        final char[] array10 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        final char[] array0Rev = {};
        final char[] array1Rev = {1};
        final char[] array2Rev = {2, 1};
        final char[] array3Rev = {3, 2, 1};
        final char[] array10Rev = {10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        ArrayUtils.swapArray(array0);
        assertTrue(Arrays.equals(array0Rev, array0));
        ArrayUtils.swapArray(array1);
        assertTrue(Arrays.equals(array1Rev, array1));
        ArrayUtils.swapArray(array2);
        assertTrue(Arrays.equals(array2Rev, array2));
        ArrayUtils.swapArray(array3);
        assertTrue(Arrays.equals(array3Rev, array3));
        ArrayUtils.swapArray(array10);
        assertTrue(Arrays.equals(array10Rev, array10));
    }

    public void testSwapShortArray() {
        final short[] array0 = {};
        final short[] array1 = {1};
        final short[] array2 = {1, 2};
        final short[] array3 = {1, 2, 3};
        final short[] array10 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        final short[] array0Rev = {};
        final short[] array1Rev = {1};
        final short[] array2Rev = {2, 1};
        final short[] array3Rev = {3, 2, 1};
        final short[] array10Rev = {10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        ArrayUtils.swapArray(array0);
        assertTrue(Arrays.equals(array0Rev, array0));
        ArrayUtils.swapArray(array1);
        assertTrue(Arrays.equals(array1Rev, array1));
        ArrayUtils.swapArray(array2);
        assertTrue(Arrays.equals(array2Rev, array2));
        ArrayUtils.swapArray(array3);
        assertTrue(Arrays.equals(array3Rev, array3));
        ArrayUtils.swapArray(array10);
        assertTrue(Arrays.equals(array10Rev, array10));
    }

    public void testSwapIntArray() {
        final int[] array0 = {};
        final int[] array1 = {1};
        final int[] array2 = {1, 2};
        final int[] array3 = {1, 2, 3};
        final int[] array10 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        final int[] array0Rev = {};
        final int[] array1Rev = {1};
        final int[] array2Rev = {2, 1};
        final int[] array3Rev = {3, 2, 1};
        final int[] array10Rev = {10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        ArrayUtils.swapArray(array0);
        assertTrue(Arrays.equals(array0Rev, array0));
        ArrayUtils.swapArray(array1);
        assertTrue(Arrays.equals(array1Rev, array1));
        ArrayUtils.swapArray(array2);
        assertTrue(Arrays.equals(array2Rev, array2));
        ArrayUtils.swapArray(array3);
        assertTrue(Arrays.equals(array3Rev, array3));
        ArrayUtils.swapArray(array10);
        assertTrue(Arrays.equals(array10Rev, array10));
    }

    public void testSwapLongArray() {
        final long[] array0 = {};
        final long[] array1 = {1};
        final long[] array2 = {1, 2};
        final long[] array3 = {1, 2, 3};
        final long[] array10 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        final long[] array0Rev = {};
        final long[] array1Rev = {1};
        final long[] array2Rev = {2, 1};
        final long[] array3Rev = {3, 2, 1};
        final long[] array10Rev = {10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        ArrayUtils.swapArray(array0);
        assertTrue(Arrays.equals(array0Rev, array0));
        ArrayUtils.swapArray(array1);
        assertTrue(Arrays.equals(array1Rev, array1));
        ArrayUtils.swapArray(array2);
        assertTrue(Arrays.equals(array2Rev, array2));
        ArrayUtils.swapArray(array3);
        assertTrue(Arrays.equals(array3Rev, array3));
        ArrayUtils.swapArray(array10);
        assertTrue(Arrays.equals(array10Rev, array10));
    }

    public void testSwapFloatArray() {
        final float[] array0 = {};
        final float[] array1 = {1};
        final float[] array2 = {1, 2};
        final float[] array3 = {1, 2, 3};
        final float[] array10 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        final float[] array0Rev = {};
        final float[] array1Rev = {1};
        final float[] array2Rev = {2, 1};
        final float[] array3Rev = {3, 2, 1};
        final float[] array10Rev = {10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        ArrayUtils.swapArray(array0);
        assertTrue(Arrays.equals(array0Rev, array0));
        ArrayUtils.swapArray(array1);
        assertTrue(Arrays.equals(array1Rev, array1));
        ArrayUtils.swapArray(array2);
        assertTrue(Arrays.equals(array2Rev, array2));
        ArrayUtils.swapArray(array3);
        assertTrue(Arrays.equals(array3Rev, array3));
        ArrayUtils.swapArray(array10);
        assertTrue(Arrays.equals(array10Rev, array10));
    }

    public void testSwapDoubleArray() {
        final double[] array0 = {};
        final double[] array1 = {1};
        final double[] array2 = {1, 2};
        final double[] array3 = {1, 2, 3};
        final double[] array10 = {1, 2, 3, 4, 5, 6, 7, 8, 9, 10};
        final double[] array0Rev = {};
        final double[] array1Rev = {1};
        final double[] array2Rev = {2, 1};
        final double[] array3Rev = {3, 2, 1};
        final double[] array10Rev = {10, 9, 8, 7, 6, 5, 4, 3, 2, 1};
        ArrayUtils.swapArray(array0);
        assertTrue(Arrays.equals(array0Rev, array0));
        ArrayUtils.swapArray(array1);
        assertTrue(Arrays.equals(array1Rev, array1));
        ArrayUtils.swapArray(array2);
        assertTrue(Arrays.equals(array2Rev, array2));
        ArrayUtils.swapArray(array3);
        assertTrue(Arrays.equals(array3Rev, array3));
        ArrayUtils.swapArray(array10);
        assertTrue(Arrays.equals(array10Rev, array10));
    }

    public void testSwapObjectrray() {
        final Object[] array0 = {};
        final Object[] array1 = {"1"};
        final Object[] array2 = {"1", "2"};
        final Object[] array3 = {"1", "2", "3"};
        final Object[] array10 = {"1", "2", "3", "4", "5", "6", "7", "8", "9", "10"};
        final Object[] array0Rev = {};
        final Object[] array1Rev = {"1"};
        final Object[] array2Rev = {"2", "1"};
        final Object[] array3Rev = {"3", "2", "1"};
        final Object[] array10Rev = {"10", "9", "8", "7", "6", "5", "4", "3", "2", "1"};
        ArrayUtils.swapArray(array0);
        assertTrue(Arrays.equals(array0Rev, array0));
        ArrayUtils.swapArray(array1);
        assertTrue(Arrays.equals(array1Rev, array1));
        ArrayUtils.swapArray(array2);
        assertTrue(Arrays.equals(array2Rev, array2));
        ArrayUtils.swapArray(array3);
        assertTrue(Arrays.equals(array3Rev, array3));
        ArrayUtils.swapArray(array10);
        assertTrue(Arrays.equals(array10Rev, array10));
    }

    public void testAddToArrayForInts() {
        try {
            ArrayUtils.addToArray(null, 23);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException expected
        }

        final int[] oldArray = new int[]{12, 45, 2, 4};
        final int[] expArray = new int[]{12, 45, 2, 4, 17};

        final int[] newArray = ArrayUtils.addToArray(oldArray, 17);

        assertEquals(true, Arrays.equals(expArray, newArray));
    }

    public void testAddArraysForInts() {
        try {
            ArrayUtils.addArrays(null, new int[3]);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException expected
        }

        try {
            ArrayUtils.addArrays(new int[3], null);
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
            // IllegalArgumentException expected
        }

        /* spacer */ final int[] secondArray = new int[]{27, 32, 1};
        final int[] firstArray = new int[]{12, 45, 2, 4};
        final int[] expecArray = new int[]{12, 45, 2, 4, 27, 32, 1};

        final int[] newArray = ArrayUtils.addArrays(firstArray, secondArray);

        assertEquals(true, Arrays.equals(expecArray, newArray));
    }

    public void testCreateIntArray() {
        assertEquals(true, Arrays.equals(new int[]{18, 19, 20, 21, 22, 23}, ArrayUtils.createIntArray(18, 23)));
        assertEquals(true, Arrays.equals(new int[]{18, 19, 20, 21, 22, 23}, ArrayUtils.createIntArray(23, 18)));

        assertEquals(true, Arrays.equals(new int[]{-2, -1, 0, 1, 2, 3}, ArrayUtils.createIntArray(-2, 3)));
        assertEquals(true, Arrays.equals(new int[]{-2, -1, 0, 1, 2, 3}, ArrayUtils.createIntArray(3, -2)));
    }
}

