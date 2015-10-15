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

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import org.esa.snap.core.util.Debug;

import java.util.Arrays;

public class FXYSumTest extends TestCase {

    public final static double EPS = 1.0e-10;
    public final static int N = 72 * 72;

    public FXYSumTest(String s) {
        super(s);
    }

    public static Test suite() {
        return new TestSuite(FXYSumTest.class);
    }

    public void testCreateFXYSum() {
        FXYSum fxySum;

        fxySum = FXYSum.createFXYSum(1, createArray(3));
        assertTrue(fxySum instanceof FXYSum.Linear);

        fxySum = FXYSum.createFXYSum(2, createArray(4));
        assertTrue(fxySum instanceof FXYSum.BiLinear);

        fxySum = FXYSum.createFXYSum(2, createArray(6));
        assertTrue(fxySum instanceof FXYSum.Quadric);

        fxySum = FXYSum.createFXYSum(4, createArray(9));
        assertTrue(fxySum instanceof FXYSum.BiQuadric);

        fxySum = FXYSum.createFXYSum(3, createArray(10));
        assertTrue(fxySum instanceof FXYSum.Cubic);

        fxySum = FXYSum.createFXYSum(6, createArray(16));
        assertTrue(fxySum instanceof FXYSum.BiCubic);

        // not able to create
        fxySum = FXYSum.createFXYSum(5, createArray(12));
        assertTrue(fxySum == null);
    }

    public void testLinearOptimization() {
        FXYSum fxyRaw = new FXYSum(FXYSum.FXY_LINEAR);
        FXYSum fxyOpt = new FXYSum.Linear();
        testRawAgainstOptimized(fxyRaw, fxyOpt);
    }

    public void testBiLinearOptimization() {
        FXYSum fxyRaw = new FXYSum(FXYSum.FXY_BI_LINEAR);
        FXYSum fxyOpt = new FXYSum.BiLinear();
        testRawAgainstOptimized(fxyRaw, fxyOpt);
    }

    public void testQuadricOptimization() {
        FXYSum fxyRaw = new FXYSum(FXYSum.FXY_QUADRATIC);
        FXYSum fxyOpt = new FXYSum.Quadric();
        testRawAgainstOptimized(fxyRaw, fxyOpt);
    }

    public void testBiQuadricOptimization() {
        FXYSum fxyRaw = new FXYSum(FXYSum.FXY_BI_QUADRATIC);
        FXYSum fxyOpt = new FXYSum.BiQuadric();
        testRawAgainstOptimized(fxyRaw, fxyOpt);
    }

    public void testCubicOptimization() {
        FXYSum fxyRaw = new FXYSum(FXYSum.FXY_CUBIC);
        FXYSum fxyOpt = new FXYSum.Cubic();
        testRawAgainstOptimized(fxyRaw, fxyOpt);
    }

    public void testBiCubicOptimization() {
        FXYSum fxyRaw = new FXYSum(FXYSum.FXY_BI_CUBIC);
        FXYSum fxyOpt = new FXYSum.BiCubic();
        testRawAgainstOptimized(fxyRaw, fxyOpt);
    }

    public void testRawAgainstOptimized(FXYSum fxyRaw, FXYSum fxyOpt) {
        final int m = 100;
        double[][] data = new double[m][3];
        double x, y, z;
        for (int i = 0; i < m; i++) {
            x = Math.PI * random(-0.5, +0.5);
            y = Math.PI * random(-0.5, +0.5);
            z = Math.sin((x * y) / 4.0 + (x + y) / 2.0);
            data[i][0] = x;
            data[i][1] = y;
            data[i][2] = z;
        }

        fxyRaw.approximate(data, null);
        fxyOpt.approximate(data, null);

        final boolean oldState = Debug.setEnabled(true);
        try {
            assertEquals(fxyRaw.getRootMeanSquareError(), fxyOpt.getRootMeanSquareError(), EPS);

            double zRaw, zOpt;

            for (int i = 0; i < 10; i++) {
                x = Math.PI * random(-0.5, +0.5);
                y = Math.PI * random(-0.5, +0.5);
                zRaw = fxyRaw.computeZ(x, y);
                zOpt = fxyOpt.computeZ(x, y);
                assertEquals(zRaw, zOpt, EPS);
            }
        } finally {
            Debug.setEnabled(oldState);
        }
    }

    private static double random(double x1, double x2) {
        return x1 + Math.random() * (x2 - x1);
    }

    private double[] createArray(final int length) {
        final double[] linearCoeffs = new double[length];
        Arrays.fill(linearCoeffs, 0.67);
        return linearCoeffs;
    }
}
