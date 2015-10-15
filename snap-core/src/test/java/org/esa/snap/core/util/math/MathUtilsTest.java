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

package org.esa.snap.core.util.math;

import junit.framework.TestCase;

import java.awt.Dimension;
import java.awt.Rectangle;


public class MathUtilsTest extends TestCase {

    public void testEqualValues() {

        assertTrue(MathUtils.equalValues(1.0F, 1.0F));
        assertTrue(MathUtils.equalValues(1.0F, 1.0F + 0.9F * MathUtils.EPS_F));
        assertTrue(MathUtils.equalValues(1.0F, 1.0F - 0.9F * MathUtils.EPS_F));
        assertTrue(!MathUtils.equalValues(1.0F, 2.0F));
        assertTrue(!MathUtils.equalValues(1.0F, 1.0F + 1.1F * MathUtils.EPS_F));
        assertTrue(!MathUtils.equalValues(1.0F, 1.0F - 1.1F * MathUtils.EPS_F));
        assertTrue(MathUtils.equalValues(1.0F, 1.0F, 1e-4F));
        assertTrue(MathUtils.equalValues(1.0F, 1.0F + 0.9e-4F, 1e-4F));
        assertTrue(MathUtils.equalValues(1.0F, 1.0F - 0.9e-4F, 1e-4F));
        assertTrue(!MathUtils.equalValues(1.0F, 1.0F + 1.1e-4F, 1e-5F));
        assertTrue(!MathUtils.equalValues(1.0F, 1.0F - 1.1e-4F, 1e-5F));

        assertTrue(MathUtils.equalValues(1.0, 1.0));
        assertTrue(MathUtils.equalValues(1.0, 1.0 + 0.9 * MathUtils.EPS));
        assertTrue(MathUtils.equalValues(1.0, 1.0 - 0.9 * MathUtils.EPS));
        assertTrue(!MathUtils.equalValues(1.0, 2.0));
        assertTrue(!MathUtils.equalValues(1.0, 1.0 + 1.1 * MathUtils.EPS));
        assertTrue(!MathUtils.equalValues(1.0, 1.0 - 1.1 * MathUtils.EPS));
        assertTrue(MathUtils.equalValues(1.0, 1.0, 1e-4));
        assertTrue(MathUtils.equalValues(1.0, 1.0 + 0.9e-4, 1e-4));
        assertTrue(MathUtils.equalValues(1.0, 1.0 - 0.9e-4, 1e-4));
        assertTrue(!MathUtils.equalValues(1.0, 1.0 + 1.1e-4, 1e-5));
        assertTrue(!MathUtils.equalValues(1.0, 1.0 - 1.1e-4, 1e-5));
    }

    public void testInterpolate2D() {
        assertEquals(0.0F, MathUtils.interpolate2D(0.0F, 0.0F, 0.0F, 1.0F, 1.0F, 2.0F), MathUtils.EPS_F);
        assertEquals(0.5F, MathUtils.interpolate2D(0.5F, 0.0F, 0.0F, 1.0F, 1.0F, 2.0F), MathUtils.EPS_F);
        assertEquals(1.0F, MathUtils.interpolate2D(1.0F, 0.0F, 0.0F, 1.0F, 1.0F, 2.0F), MathUtils.EPS_F);
        assertEquals(0.5F, MathUtils.interpolate2D(0.0F, 0.5F, 0.0F, 1.0F, 1.0F, 2.0F), MathUtils.EPS_F);
        assertEquals(1.0F, MathUtils.interpolate2D(0.5F, 0.5F, 0.0F, 1.0F, 1.0F, 2.0F), MathUtils.EPS_F);
        assertEquals(1.5F, MathUtils.interpolate2D(1.0F, 0.5F, 0.0F, 1.0F, 1.0F, 2.0F), MathUtils.EPS_F);
        assertEquals(1.0F, MathUtils.interpolate2D(0.0F, 1.0F, 0.0F, 1.0F, 1.0F, 2.0F), MathUtils.EPS_F);
        assertEquals(1.5F, MathUtils.interpolate2D(0.5F, 1.0F, 0.0F, 1.0F, 1.0F, 2.0F), MathUtils.EPS_F);
        assertEquals(2.0F, MathUtils.interpolate2D(1.0F, 1.0F, 0.0F, 1.0F, 1.0F, 2.0F), MathUtils.EPS_F);

        assertEquals(0.0, MathUtils.interpolate2D(0.0, 0.0, 0.0, 1.0, 1.0, 2.0), MathUtils.EPS);
        assertEquals(0.5, MathUtils.interpolate2D(0.5, 0.0, 0.0, 1.0, 1.0, 2.0), MathUtils.EPS);
        assertEquals(1.0, MathUtils.interpolate2D(1.0, 0.0, 0.0, 1.0, 1.0, 2.0), MathUtils.EPS);
        assertEquals(0.5, MathUtils.interpolate2D(0.0, 0.5, 0.0, 1.0, 1.0, 2.0), MathUtils.EPS);
        assertEquals(1.0, MathUtils.interpolate2D(0.5, 0.5, 0.0, 1.0, 1.0, 2.0), MathUtils.EPS);
        assertEquals(1.5, MathUtils.interpolate2D(1.0, 0.5, 0.0, 1.0, 1.0, 2.0), MathUtils.EPS);
        assertEquals(1.0, MathUtils.interpolate2D(0.0, 1.0, 0.0, 1.0, 1.0, 2.0), MathUtils.EPS);
        assertEquals(1.5, MathUtils.interpolate2D(0.5, 1.0, 0.0, 1.0, 1.0, 2.0), MathUtils.EPS);
        assertEquals(2.0, MathUtils.interpolate2D(1.0, 1.0, 0.0, 1.0, 1.0, 2.0), MathUtils.EPS);
    }

    /*public void testExp() {
        boolean oldDebugState = Debug.setEnabled(true);

        final int maxIter = 1000000;
        StopWatch sw = new StopWatch();

        sw.start();
        testMathExp(maxIter);
        sw.stop();
        Debug.trace(maxIter + " x 4 x Math.exp(): " + sw.getTimeDiffString());

        sw.start();
        testStrictMathExp(maxIter);
        sw.stop();
        Debug.trace(maxIter + " x 4 x StrictMath.exp(): " + sw.getTimeDiffString());

        Debug.setEnabled(oldDebugState);
    }


    void testMathExp(int maxIter) {
        double x = 1.0F, y;
        for (int i = 0; i < maxIter; i++) {
            y = Math.exp(x);
            y = Math.exp(y);
            y = Math.exp(y);
            y = Math.exp(y);
        }
    }

    void testStrictMathExp(int maxIter) {
        double x = 1.0F, y;
        for (int i = 0; i < maxIter; i++) {
            y = StrictMath.exp(x);
            y = StrictMath.exp(y);
            y = StrictMath.exp(y);
            y = StrictMath.exp(y);
        }
    }  */

    public void testRounding() {
        assertEquals(1.0, MathUtils.computeRoundFactor(0.0, 100.0, 0), MathUtils.EPS);
        assertEquals(10.0, MathUtils.computeRoundFactor(0.0, 100.0, 1), MathUtils.EPS);
        assertEquals(100.0, MathUtils.computeRoundFactor(0.0, 100.0, 2), MathUtils.EPS);
        assertEquals(1000.0, MathUtils.computeRoundFactor(0.0, 100.0, 3), MathUtils.EPS);

        assertEquals(1000.0, MathUtils.computeRoundFactor(0.001, 0.002, 0), MathUtils.EPS);
        assertEquals(10000.0, MathUtils.computeRoundFactor(0.001, 0.002, 1), MathUtils.EPS);
        assertEquals(100000.0, MathUtils.computeRoundFactor(0.001, 0.002, 2), MathUtils.EPS);
        assertEquals(1000000.0, MathUtils.computeRoundFactor(0.001, 0.002, 3), MathUtils.EPS);
        assertEquals(10000000.0, MathUtils.computeRoundFactor(0.001, 0.002, 4), MathUtils.EPS);

        assertEquals(10.0, MathUtils.round(10.0, 1.0), MathUtils.EPS);
        assertEquals(10.0, MathUtils.round(10.0, 10.0), MathUtils.EPS);
        assertEquals(10.0, MathUtils.round(10.0, 100.0), MathUtils.EPS);
        assertEquals(10.0, MathUtils.round(10.0, 1000.0), MathUtils.EPS);

        assertEquals(10.0, MathUtils.round(10.1234567, 1.0), MathUtils.EPS);
        assertEquals(10.1, MathUtils.round(10.1234567, 10.0), MathUtils.EPS);
        assertEquals(10.12, MathUtils.round(10.1234567, 100.0), MathUtils.EPS);
        assertEquals(10.123, MathUtils.round(10.1234567, 1000.0), MathUtils.EPS);
        assertEquals(10.1235, MathUtils.round(10.1234567, 10000.0), MathUtils.EPS);
        assertEquals(10.12346, MathUtils.round(10.1234567, 100000.0), MathUtils.EPS);
        assertEquals(10.123457, MathUtils.round(10.1234567, 1000000.0), MathUtils.EPS);
        assertEquals(10.1234567, MathUtils.round(10.1234567, 10000000.0), MathUtils.EPS);
    }

    /**
     * Tests the correct functionality of the degreesToRadians method
     */
    public void testDegToRad() {
        float[] degrees = {0.f, 10.f, 23.f, 45.f, 89.f, 90.f, 134.f, 265.f, 312.f};
        float[] radians = {
                    0.f,
                    0.17453293f,
                    0.40142573f,
                    0.78539816f,
                    1.55334303f,
                    1.57079633f,
                    2.33874119f,
                    4.62512252f,
                    5.44542727f
        };

        for (int n = 0; n < degrees.length; n++) {
            assertEquals(radians[n], MathUtils.DTOR_F * degrees[n], 1e-6);
        }
    }

    /**
     * Tests the correct functionality of the radians to degree conversion
     */
    public void testRadToDeg() {
        float[] radians = {0.1f, 0.3f, 0.6f, 0.9f, 1.12f, 1.6f, 2.7f, 3.8f};
        float[] degrees = {
                    5.72957795f,
                    17.18873385f,
                    34.37746771f,
                    51.56620156f,
                    64.17127305f,
                    91.67324722f,
                    154.69860469f,
                    217.72396215f
        };

        for (int n = 0; n < radians.length; n++) {
            assertEquals(degrees[n], MathUtils.RTOD_F * radians[n], 1e-6);
        }
    }

    public void testFloorInt() {
        assertEquals(1, MathUtils.floorInt(1.0d));
        assertEquals(0, MathUtils.floorInt(0.99999999999d));
        assertEquals(0, MathUtils.floorInt(0.5d));
        assertEquals(0, MathUtils.floorInt(0.49999999999d));
        assertEquals(0, MathUtils.floorInt(0.0d));
        assertEquals(-1, MathUtils.floorInt(-0.00000000001d));
        assertEquals(-1, MathUtils.floorInt(-0.49999999999d));
        assertEquals(-1, MathUtils.floorInt(-0.5d));
        assertEquals(-1, MathUtils.floorInt(-0.99999999999d));
        assertEquals(-1, MathUtils.floorInt(-1.0d));
        assertEquals(-2, MathUtils.floorInt(-1.00000000001d));
    }

    public void testFloorlong() {
        assertEquals(1, MathUtils.floorLong(1.0d));
        assertEquals(0, MathUtils.floorLong(0.99999999999d));
        assertEquals(0, MathUtils.floorLong(0.5d));
        assertEquals(0, MathUtils.floorLong(0.49999999999d));
        assertEquals(0, MathUtils.floorLong(0.0d));
        assertEquals(-1, MathUtils.floorLong(-0.00000000001d));
        assertEquals(-1, MathUtils.floorLong(-0.49999999999d));
        assertEquals(-1, MathUtils.floorLong(-0.5d));
        assertEquals(-1, MathUtils.floorLong(-0.99999999999d));
        assertEquals(-1, MathUtils.floorLong(-1.0d));
        assertEquals(-2, MathUtils.floorLong(-1.00000000001d));
    }

    public void testRoundAndCropInt() {
        assertEquals(4L, MathUtils.roundAndCrop(4.3f, 1, 7));
        assertEquals(5L, MathUtils.roundAndCrop(4.3f, 5, 7));
        assertEquals(3L, MathUtils.roundAndCrop(4.3f, 1, 3));
    }

    public void testRoundAndCropLong() {
        assertEquals(4L, MathUtils.roundAndCrop(4.3d, 1L, 7L));
        assertEquals(5L, MathUtils.roundAndCrop(4.3d, 5L, 7L));
        assertEquals(3L, MathUtils.roundAndCrop(4.3d, 1L, 3L));
    }

    public void testCropByte() {
        byte min = 12;
        byte max = 28;
        byte val;

        val = 15;
        assertEquals(15, MathUtils.crop(val, min, max));
        val = 7;
        assertEquals(min, MathUtils.crop(val, min, max));
        val = 34;
        assertEquals(max, MathUtils.crop(val, min, max));
    }

    public void testCropShort() {
        short min = 12;
        short max = 28;
        short val;

        val = 15;
        assertEquals(15, MathUtils.crop(val, min, max));
        val = 7;
        assertEquals(min, MathUtils.crop(val, min, max));
        val = 34;
        assertEquals(max, MathUtils.crop(val, min, max));
    }

    public void testCropInt() {
        int min = 12;
        int max = 28;
        int val;

        val = 15;
        assertEquals(15, MathUtils.crop(val, min, max));
        val = 7;
        assertEquals(min, MathUtils.crop(val, min, max));
        val = 34;
        assertEquals(max, MathUtils.crop(val, min, max));
    }

    public void testCropLong() {
        long min = 12;
        long max = 28;
        long val;

        val = 15;
        assertEquals(15, MathUtils.crop(val, min, max));
        val = 7;
        assertEquals(min, MathUtils.crop(val, min, max));
        val = 34;
        assertEquals(max, MathUtils.crop(val, min, max));
    }

    public void testCropFloat() {
        float min = 12.5f;
        float max = 28.7f;
        float val;

        val = 15.4f;
        assertEquals(15.4f, MathUtils.crop(val, min, max), 1e-5f);
        val = 7.4f;
        assertEquals(min, MathUtils.crop(val, min, max), 1e-5f);
        val = 34.6f;
        assertEquals(max, MathUtils.crop(val, min, max), 1e-5f);
    }

    public void testCropDouble() {
        double min = 120.3;
        double max = 286.4;
        double val;

        val = 150.5;
        assertEquals(150.5, MathUtils.crop(val, min, max), 1e-10);
        val = 70.4;
        assertEquals(min, MathUtils.crop(val, min, max), 1e-10);
        val = 347.12;
        assertEquals(max, MathUtils.crop(val, min, max), 1e-10);
    }

    public void testFitDimension() {
        assertEquals(new Dimension(0, 0), MathUtils.fitDimension(0, 1.0, 0.5));
        assertEquals(new Dimension(2, 1), MathUtils.fitDimension(2, 1.0, 0.5));
        assertEquals(new Dimension(4, 2), MathUtils.fitDimension(8, 1.0, 0.5));

        assertEquals(new Dimension(0, 0), MathUtils.fitDimension(0, 0.5, 1.0));
        assertEquals(new Dimension(1, 2), MathUtils.fitDimension(2, 0.5, 1.0));
        assertEquals(new Dimension(2, 4), MathUtils.fitDimension(8, 0.5, 1.0));

        assertEquals(new Dimension(0, 0), MathUtils.fitDimension(0, 1.0, 1.0));
        assertEquals(new Dimension(1, 1), MathUtils.fitDimension(1, 1.0, 1.0));
        assertEquals(new Dimension(2, 2), MathUtils.fitDimension(4, 1.0, 1.0));
        assertEquals(new Dimension(3, 3), MathUtils.fitDimension(9, 1.0, 1.0));


        assertEquals(new Dimension(2, 1), MathUtils.fitDimension(3, 1.0, 0.5));
        assertEquals(new Dimension(4, 2), MathUtils.fitDimension(5, 1.0, 0.5));
        assertEquals(new Dimension(4, 2), MathUtils.fitDimension(7, 1.0, 0.5));
        assertEquals(new Dimension(4, 2), MathUtils.fitDimension(9, 1.0, 0.5));
        assertEquals(new Dimension(4, 2), MathUtils.fitDimension(11, 1.0, 0.5));
        assertEquals(new Dimension(6, 3), MathUtils.fitDimension(13, 1.0, 0.5));
        assertEquals(new Dimension(6, 3), MathUtils.fitDimension(15, 1.0, 0.5));
        assertEquals(new Dimension(6, 3), MathUtils.fitDimension(17, 1.0, 0.5));
        assertEquals(new Dimension(6, 3), MathUtils.fitDimension(19, 1.0, 0.5));
        assertEquals(new Dimension(6, 3), MathUtils.fitDimension(21, 1.0, 0.5));
        assertEquals(new Dimension(6, 3), MathUtils.fitDimension(23, 1.0, 0.5));
        assertEquals(new Dimension(8, 4), MathUtils.fitDimension(25, 1.0, 0.5));
    }

    public static void testSudivideRectangle() {
        Rectangle[] rectangles;

        rectangles = MathUtils.subdivideRectangle(100, 40, 4, 2, 0);
        assertEquals(4 * 2, rectangles.length);
        assertEquals(new Rectangle(0, 0, 25, 20), rectangles[0]);
        assertEquals(new Rectangle(25, 0, 25, 20), rectangles[1]);
        assertEquals(new Rectangle(50, 0, 25, 20), rectangles[2]);
        assertEquals(new Rectangle(75, 0, 25, 20), rectangles[3]);
        assertEquals(new Rectangle(0, 20, 25, 20), rectangles[4]);
        assertEquals(new Rectangle(25, 20, 25, 20), rectangles[5]);
        assertEquals(new Rectangle(50, 20, 25, 20), rectangles[6]);
        assertEquals(new Rectangle(75, 20, 25, 20), rectangles[7]);

        rectangles = MathUtils.subdivideRectangle(100, 40, 4, 2, 1);
        assertEquals(4 * 2, rectangles.length);
        assertEquals(new Rectangle(0, 0, 26, 21), rectangles[0]);
        assertEquals(new Rectangle(24, 0, 27, 21), rectangles[1]);
        assertEquals(new Rectangle(49, 0, 27, 21), rectangles[2]);
        assertEquals(new Rectangle(74, 0, 26, 21), rectangles[3]);
        assertEquals(new Rectangle(0, 19, 26, 21), rectangles[4]);
        assertEquals(new Rectangle(24, 19, 27, 21), rectangles[5]);
        assertEquals(new Rectangle(49, 19, 27, 21), rectangles[6]);
        assertEquals(new Rectangle(74, 19, 26, 21), rectangles[7]);
    }
}

