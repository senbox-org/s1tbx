/*
 * Copyright (C) 2021 SkyWatch Space Applications Inc. https://www.skywatch.com
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
package org.esa.s1tbx;

import Jama.Matrix;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.jblas.DoubleMatrix;
import org.junit.Ignore;
import org.junit.Test;

/**
 * Unit test for PolOpUtils.
 */
@Ignore
public class TestMatrices {

    private static final int iterations = 1;//200000;
    private static final int t3 = 3;
    private static final int large = 100;

    @Test
    public void testMatrixAddT3Commons() {
        matrixAddCommons(t3);
    }

    @Test
    public void testMatrixAddLargeCommons() {
        matrixAddCommons(large);
    }

    public void matrixAddCommons(final int size) {
        final double[][] tempTr = new double[size][size];
        final RealMatrix TrMat = new Array2DRowRealMatrix(size, size);

        for (int i = 0; i < iterations; ++i) {
            TrMat.add(new Array2DRowRealMatrix(tempTr));
        }
    }

    @Test
    public void testMatrixAddT3Jama() {
        matrixAddJama(t3);
    }

    @Test
    public void testMatrixAddLargeJama() {
        matrixAddJama(large);
    }

    public void matrixAddJama(final int size) {
        final double[][] tempTr = new double[size][size];
        final Matrix TrMat = new Matrix(size, size);

        for (int i = 0; i < iterations; ++i) {
            TrMat.plus(new Matrix(tempTr));
        }
    }

    @Test
    public void testMatrixAddT3JBlas() {
        matrixAddJBlas(t3);
    }

    @Test
    public void testMatrixAddLargeJBlas() {
        matrixAddJBlas(large);
    }

    public void matrixAddJBlas(final int size) {
        final double[][] tempTr = new double[size][size];
        final DoubleMatrix TrMat = new DoubleMatrix(size, size);

        for (int i = 0; i < iterations; ++i) {
            TrMat.add(new DoubleMatrix(tempTr));
        }
    }

    @Test
    public void testMatrixMultT3Commons() {
        matrixMultCommons(t3);
    }

    @Test
    public void testMatrixMultLargeCommons() {
        matrixMultCommons(large);
    }

    public void matrixMultCommons(final int size) {
        final double[][] tempTr = new double[size][size];
        final RealMatrix TrMat = new Array2DRowRealMatrix(size, size);

        for (int i = 0; i < iterations; ++i) {
            TrMat.multiply(new Array2DRowRealMatrix(tempTr));
        }
    }

    @Test
    public void testMatrixMultT3Jama() {
        matrixMultJama(t3);
    }

    @Test
    public void testMatrixMultLargeJama() {
        matrixMultJama(large);
    }

    public void matrixMultJama(final int size) {
        final double[][] tempTr = new double[size][size];
        final Matrix TrMat = new Matrix(size, size);

        for (int i = 0; i < iterations; ++i) {
            TrMat.times(new Matrix(tempTr));
        }
    }

    @Test
    public void testMatrixMultT3JBlas() {
        matrixMultJBlas(t3);
    }

    @Test
    public void testMatrixMultLargeJBlas() {
        matrixMultJBlas(large);
    }

    public void matrixMultJBlas(final int size) {
        final double[][] tempTr = new double[size][size];
        final DoubleMatrix TrMat = new DoubleMatrix(size, size);

        for (int i = 0; i < iterations; ++i) {
            TrMat.mul(new DoubleMatrix(tempTr));
        }
    }
}
