/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.gpf;

import Jama.Matrix;
import org.esa.s1tbx.dataio.PolBandUtils;
import org.esa.snap.eo.Constants;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.gpf.TileIndex;


/**
 * Common dual pol code used by polarimetric operators
 */
public final class DualPolOpUtils {

    public static final double EPS = Constants.EPS;

    /**
     * Get scatter matrix for given pixel.
     *
     * @param index           X,Y coordinate of the given pixel
     * @param dataBuffers     Source tiles dataBuffers for all 4 (dual pol) or 8 (full pol) source bands
     * @param scatterMatrix_i Real part of the scatter matrix
     * @param scatterMatrix_q Imaginary part of the scatter matrix
     */
    public static void getComplexScatterMatrix(final int index, final ProductData[] dataBuffers,
                                               final double[][] scatterMatrix_i, final double[][] scatterMatrix_q) {

        // Dual pol: Case 1 is HH HV
        //           Case 2 is VH VV
        //           Case 3 is HH VV

        // If quad pol or dual pol Cases 1 or 3 then this is HH; else it is dual pol Case 2 then this is VH
        scatterMatrix_i[0][0] = dataBuffers[0].getElemDoubleAt(index); // real
        scatterMatrix_q[0][0] = dataBuffers[1].getElemDoubleAt(index); // imag

        // If quad pol or dual pol Case 1 then this is HV; else it is dual pol Cases 2 or 3 then this is VV
        scatterMatrix_i[0][1] = dataBuffers[2].getElemDoubleAt(index); // real
        scatterMatrix_q[0][1] = dataBuffers[3].getElemDoubleAt(index); // imag

        if (dataBuffers.length > 4) {

            // Must be quad pol
            scatterMatrix_i[1][0] = dataBuffers[4].getElemDoubleAt(index); // VH - real
            scatterMatrix_q[1][0] = dataBuffers[5].getElemDoubleAt(index); // VH - imag

            scatterMatrix_i[1][1] = dataBuffers[6].getElemDoubleAt(index); // VV - real
            scatterMatrix_q[1][1] = dataBuffers[7].getElemDoubleAt(index); // VV - imag
        }
    }

    /**
     * Get mean covariance matrix C2 for given pixel.
     *
     * @param x                 X coordinate of the given pixel.
     * @param y                 Y coordinate of the given pixel.
     * @param halfWindowSizeX   The sliding window width /2
     * @param halfWindowSizeY   The sliding window height /2
     * @param sourceImageWidth  Source image width.
     * @param sourceImageHeight Source image height.
     * @param sourceProductType The source product type.
     * @param sourceTiles       The source tiles for all bands.
     * @param dataBuffers       Source tile data buffers.
     * @param Cr                The real part of the mean covariance matrix.
     * @param Ci                The imaginary part of the mean covariance matrix.
     */
    public static void getMeanCovarianceMatrixC2(
            final int x, final int y, final int halfWindowSizeX, final int halfWindowSizeY, final int sourceImageWidth,
            final int sourceImageHeight, final PolBandUtils.MATRIX sourceProductType,
            final Tile[] sourceTiles, final ProductData[] dataBuffers, final double[][] Cr, final double[][] Ci) {

        final double[][] tempCr = new double[2][2];
        final double[][] tempCi = new double[2][2];

        final int xSt = Math.max(x - halfWindowSizeX, 0);
        final int xEd = Math.min(x + halfWindowSizeX, sourceImageWidth - 1);
        final int ySt = Math.max(y - halfWindowSizeY, 0);
        final int yEd = Math.min(y + halfWindowSizeY, sourceImageHeight - 1);
        final int num = (yEd - ySt + 1) * (xEd - xSt + 1);

        final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

        final Matrix CrMat = new Matrix(2, 2);
        final Matrix CiMat = new Matrix(2, 2);

        if (sourceProductType == PolBandUtils.MATRIX.C2) {

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    getCovarianceMatrixC2(srcIndex.getIndex(xx), dataBuffers, tempCr, tempCi);
                    CrMat.plusEquals(new Matrix(tempCr));
                    CiMat.plusEquals(new Matrix(tempCi));
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.LCHCP || sourceProductType == PolBandUtils.MATRIX.RCHCP) {
            final double[] tempKr = new double[2];
            final double[] tempKi = new double[2];

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    getCompactPolScatterVector(srcIndex.getIndex(xx), dataBuffers, tempKr, tempKi);
                    DualPolOpUtils.computeCovarianceMatrixC2(tempKr, tempKi, tempCr, tempCi);
                    CrMat.plusEquals(new Matrix(tempCr));
                    CiMat.plusEquals(new Matrix(tempCi));
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.DUAL_HH_HV ||
                sourceProductType == PolBandUtils.MATRIX.DUAL_VH_VV ||
                sourceProductType == PolBandUtils.MATRIX.DUAL_HH_VV) {

            final double[][] Sr = new double[1][2];
            final double[][] Si = new double[1][2];

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    PolOpUtils.getComplexScatterMatrix(srcIndex.getIndex(xx), dataBuffers, Sr, Si);
                    computeCovarianceMatrixC2(Sr[0], Si[0], tempCr, tempCi);
                    CrMat.plusEquals(new Matrix(tempCr));
                    CiMat.plusEquals(new Matrix(tempCi));
                }
            }

        } else {
            throw new OperatorException("getMeanCovarianceMatrixC2 not implemented for raw dual pol");
        }

        CrMat.timesEquals(1.0 / num);
        CiMat.timesEquals(1.0 / num);
        for (int i = 0; i < 2; i++) {
            Cr[i][0] = CrMat.get(i, 0);
            Ci[i][0] = CiMat.get(i, 0);

            Cr[i][1] = CrMat.get(i, 1);
            Ci[i][1] = CiMat.get(i, 1);
        }
    }

    /**
     * Get covariance matrix C2 for a given pixel in the input C2 product.
     *
     * @param index       X,Y coordinate of the given pixel
     * @param dataBuffers Source tile data buffers for all 4 source bands
     * @param Cr          Real part of the 2x2 covariance matrix
     * @param Ci          Imaginary part of the 2x2 covariance matrix
     */
    public static void getCovarianceMatrixC2(final int index, final ProductData[] dataBuffers,
                                             final double[][] Cr, final double[][] Ci) {

        Cr[0][0] = dataBuffers[0].getElemDoubleAt(index); // C11 - real
        Ci[0][0] = 0.0;                                   // C11 - imag

        Cr[0][1] = dataBuffers[1].getElemDoubleAt(index); // C12 - real
        Ci[0][1] = dataBuffers[2].getElemDoubleAt(index); // C12 - imag

        Cr[1][1] = dataBuffers[3].getElemDoubleAt(index); // C22 - real
        Ci[1][1] = 0.0;                                   // C22 - imag

        Cr[1][0] = Cr[0][1];
        Ci[1][0] = -Ci[0][1];
    }

    /**
     * Get covariance matrix C2 for a given pixel.
     *
     * @param index       X,Y coordinate of the given pixel
     * @param dataBuffers Source tile data buffers for all 4 source bands
     * @param Cr          Real part of the 2x2 covariance matrix
     * @param Ci          Imaginary part of the 2x2 covariance matrix
     */
    public static void getCovarianceMatrixC2(final int index, final PolBandUtils.MATRIX sourceProductType,
                                             final ProductData[] dataBuffers, final double[][] Cr,
                                             final double[][] Ci) {

        if (sourceProductType == PolBandUtils.MATRIX.LCHCP || sourceProductType == PolBandUtils.MATRIX.RCHCP) {

            final double[] kr = new double[2];
            final double[] ki = new double[2];
            getCompactPolScatterVector(index, dataBuffers, kr, ki);
            DualPolOpUtils.computeCovarianceMatrixC2(kr, ki, Cr, Ci);

        } else if (sourceProductType == PolBandUtils.MATRIX.C2) {
            getCovarianceMatrixC2(index, dataBuffers, Cr, Ci);

        } else if (sourceProductType == PolBandUtils.MATRIX.DUAL_HH_HV ||
                sourceProductType == PolBandUtils.MATRIX.DUAL_VH_VV ||
                sourceProductType == PolBandUtils.MATRIX.DUAL_HH_VV) {

            final double[][] Sr = new double[1][2];
            final double[][] Si = new double[1][2];

            PolOpUtils.getComplexScatterMatrix(index, dataBuffers, Sr, Si);
            computeCovarianceMatrixC2(Sr[0], Si[0], Cr, Ci);
        }
    }

    /**
     * Get compact pol scatter vector for a given pixel in the input compact pol product.
     *
     * @param index       X,Y coordinate of the given pixel
     * @param dataBuffers Source tiles dataBuffers for all 4 source bands
     * @param kr          Real part of the scatter vector
     * @param ki          Imaginary part of the scatter vector
     */
    public static void getCompactPolScatterVector(final int index, final ProductData[] dataBuffers,
                                                  final double[] kr, final double[] ki) {

        kr[0] = dataBuffers[0].getElemDoubleAt(index); // RH - real
        ki[0] = dataBuffers[1].getElemDoubleAt(index); // RH - imag

        kr[1] = dataBuffers[2].getElemDoubleAt(index); // RV - real
        ki[1] = dataBuffers[3].getElemDoubleAt(index); // RV - imag
    }

    /**
     * Compute covariance matrix c2 for given dual pol or complex compact pol 2x1 scatter vector.
     *
     * For dual pol product:
     *
     * Case 1) k_DP1 = [S_HH
     *                  S_HV]
     *         kr[0] = i_hh, ki[0] = q_hh, kr[1] = i_hv, ki[1] = q_hv
     *
     * Case 2) k_DP2 = [S_VH
     *                  S_VV]
     *         kr[0] = i_vh, ki[0] = q_vh, kr[1] = i_vv, ki[1] = q_vv
     *
     * Case 3) k_DP3 = [S_HH
     *                  S_VV]
     *         kr[0] = i_hh, ki[0] = q_hh, kr[1] = i_vv, ki[1] = q_vv
     *
     * @param kr Real part of the scatter vector
     * @param ki Imaginary part of the scatter vector
     * @param Cr Real part of the covariance matrix
     * @param Ci Imaginary part of the covariance matrix
     */
    public static void computeCovarianceMatrixC2(final double[] kr, final double[] ki,
                                                 final double[][] Cr, final double[][] Ci) {

        Cr[0][0] = kr[0] * kr[0] + ki[0] * ki[0];
        Ci[0][0] = 0.0;

        Cr[0][1] = kr[0] * kr[1] + ki[0] * ki[1];
        Ci[0][1] = ki[0] * kr[1] - kr[0] * ki[1];

        Cr[1][1] = kr[1] * kr[1] + ki[1] * ki[1];
        Ci[1][1] = 0.0;

        Cr[1][0] = Cr[0][1];
        Ci[1][0] = -Ci[0][1];
    }
}
