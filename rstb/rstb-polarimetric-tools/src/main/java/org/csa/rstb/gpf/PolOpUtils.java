/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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

import Jama.EigenvalueDecomposition;
import Jama.Matrix;
import org.apache.commons.math.util.FastMath;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Tile;
import org.esa.nest.gpf.PolBandUtils;
import org.esa.nest.gpf.TileIndex;
import org.esa.nest.eo.Constants;

/*
import Jama.Matrix;
import Jama.SingularValueDecomposition;

import Jama.EigenvalueDecomposition;
import Jampack.Eig;
*/

/**
 * Common code used by polarimetric operators
 */
public final class PolOpUtils {

    private static final double sqrt2 = Math.sqrt(2);
    public static final double EPS = Constants.EPS;

    public static final String quarterPi = "PI/4 Mode";
    public static final String rch = "Right Circular Hybrid Mode";
    public static final String lch = "Left Circular Hybrid Mode";
    public static final String dcp = "Dual Circular Polarimetric Mode";
    public static final String dualHHHV = "Dual HH/HV";
    public static final String dualVVVH = "Dual VV/VH";

    /**
     * Get scatter matrix for given pixel.
     * @param index X,Y coordinate of the given pixel
     * @param dataBuffers Source tiles dataBuffers for all 8 source bands
     * @param scatterMatrix_i Real part of the scatter matrix
     * @param scatterMatrix_q Imaginary part of the scatter matrix
     */
    public static void getComplexScatterMatrix(final int index, final ProductData[] dataBuffers,
                                        final double[][] scatterMatrix_i, final double[][] scatterMatrix_q){

        scatterMatrix_i[0][0] = dataBuffers[0].getElemDoubleAt(index); // HH - real
        scatterMatrix_q[0][0] = dataBuffers[1].getElemDoubleAt(index); // HH - imag

        scatterMatrix_i[0][1] = dataBuffers[2].getElemDoubleAt(index); // HV - real
        scatterMatrix_q[0][1] = dataBuffers[3].getElemDoubleAt(index); // HV - imag

        scatterMatrix_i[1][0] = dataBuffers[4].getElemDoubleAt(index); // VH - real
        scatterMatrix_q[1][0] = dataBuffers[5].getElemDoubleAt(index); // VH - imag

        scatterMatrix_i[1][1] = dataBuffers[6].getElemDoubleAt(index); // VV - real
        scatterMatrix_q[1][1] = dataBuffers[7].getElemDoubleAt(index); // VV - imag
    }

    /**
     * Get covariance matrix C3 for given pixel.
     * @param index X,Y coordinate of the given pixel
     * @param dataBuffers Source tiles dataBuffers for all 9 source bands
     * @param Cr Real part of the covariance matrix
     * @param Ci Imaginary part of the covariance matrix
     */
    public static void getCovarianceMatrixC3(final int index, final ProductData[] dataBuffers,
                                             final double[][] Cr, final double[][] Ci){

        Cr[0][0] = dataBuffers[0].getElemDoubleAt(index); // C11 - real
        Ci[0][0] = 0.0;                                   // C11 - imag

        Cr[0][1] = dataBuffers[1].getElemDoubleAt(index); // C12 - real
        Ci[0][1] = dataBuffers[2].getElemDoubleAt(index); // C12 - imag

        Cr[0][2] = dataBuffers[3].getElemDoubleAt(index); // C13 - real
        Ci[0][2] = dataBuffers[4].getElemDoubleAt(index); // C13 - imag

        Cr[1][1] = dataBuffers[5].getElemDoubleAt(index); // C22 - real
        Ci[1][1] = 0.0;                                   // C22 - imag

        Cr[1][2] = dataBuffers[6].getElemDoubleAt(index); // C23 - real
        Ci[1][2] = dataBuffers[7].getElemDoubleAt(index); // C23 - imag

        Cr[2][2] = dataBuffers[8].getElemDoubleAt(index); // C33 - real
        Ci[2][2] = 0.0;                                   // C33 - imag

        Cr[1][0] = Cr[0][1]; Ci[1][0] = -Ci[0][1];
        Cr[2][0] = Cr[0][2]; Ci[2][0] = -Ci[0][2];
        Cr[2][1] = Cr[1][2]; Ci[2][1] = -Ci[1][2];
    }

    /**
     * Get covariance matrix C4 for given pixel.
     * @param index X,Y coordinate of the given pixel
     * @param dataBuffers Source tiles dataBuffers for all 16 source bands
     * @param Cr Real part of the covariance matrix
     * @param Ci Imaginary part of the covariance matrix
     */
    public static void getCovarianceMatrixC4(final int index, final ProductData[] dataBuffers,
                                             final double[][] Cr, final double[][] Ci){

        Cr[0][0] = dataBuffers[0].getElemDoubleAt(index); // C11 - real
        Ci[0][0] = 0.0;                                   // C11 - imag

        Cr[0][1] = dataBuffers[1].getElemDoubleAt(index); // C12 - real
        Ci[0][1] = dataBuffers[2].getElemDoubleAt(index); // C12 - imag

        Cr[0][2] = dataBuffers[3].getElemDoubleAt(index); // C13 - real
        Ci[0][2] = dataBuffers[4].getElemDoubleAt(index); // C13 - imag

        Cr[0][3] = dataBuffers[5].getElemDoubleAt(index); // C14 - real
        Ci[0][3] = dataBuffers[6].getElemDoubleAt(index); // C14 - imag

        Cr[1][1] = dataBuffers[7].getElemDoubleAt(index); // C22 - real
        Ci[1][1] = 0.0;                                   // C22 - imag

        Cr[1][2] = dataBuffers[8].getElemDoubleAt(index); // C23 - real
        Ci[1][2] = dataBuffers[9].getElemDoubleAt(index); // C23 - imag

        Cr[1][3] = dataBuffers[10].getElemDoubleAt(index); // C24 - real
        Ci[1][3] = dataBuffers[11].getElemDoubleAt(index); // C24 - imag

        Cr[2][2] = dataBuffers[12].getElemDoubleAt(index); // C33 - real
        Ci[2][2] = 0.0;                                    // C33 - imag

        Cr[2][3] = dataBuffers[13].getElemDoubleAt(index); // C34 - real
        Ci[2][3] = dataBuffers[14].getElemDoubleAt(index); // C34 - imag

        Cr[3][3] = dataBuffers[15].getElemDoubleAt(index); // C44 - real
        Ci[3][3] = 0.0;                                    // C44 - imag

        Cr[1][0] = Cr[0][1]; Ci[1][0] = -Ci[0][1];
        Cr[2][0] = Cr[0][2]; Ci[2][0] = -Ci[0][2];
        Cr[2][1] = Cr[1][2]; Ci[2][1] = -Ci[1][2];
        Cr[3][0] = Cr[0][3]; Ci[3][0] = -Ci[0][3];
        Cr[3][1] = Cr[1][3]; Ci[3][1] = -Ci[1][3];
        Cr[3][2] = Cr[2][3]; Ci[3][2] = -Ci[2][3];
    }

    /**
     * Get coherency matrix T3 for given pixel.
     * @param index X,Y coordinate of the given pixel
     * @param dataBuffers Source tile buffers for all 9 source bands
     * @param Tr Real part of the coherency matrix
     * @param Ti Imaginary part of the coherency matrix
     */
    public static void getCoherencyMatrixT3(final int index, final ProductData[] dataBuffers,
                                            final double[][] Tr, final double[][] Ti){

        Tr[0][0] = dataBuffers[0].getElemDoubleAt(index); // T11 - real
        Ti[0][0] = 0.0;                                                   // T11 - imag

        Tr[0][1] = dataBuffers[1].getElemDoubleAt(index); // T12 - real
        Ti[0][1] = dataBuffers[2].getElemDoubleAt(index); // T12 - imag

        Tr[0][2] = dataBuffers[3].getElemDoubleAt(index); // T13 - real
        Ti[0][2] = dataBuffers[4].getElemDoubleAt(index); // T13 - imag

        Tr[1][1] = dataBuffers[5].getElemDoubleAt(index); // T22 - real
        Ti[1][1] = 0.0;                                                   // T22 - imag

        Tr[1][2] = dataBuffers[6].getElemDoubleAt(index); // T23 - real
        Ti[1][2] = dataBuffers[7].getElemDoubleAt(index); // T23 - imag

        Tr[2][2] = dataBuffers[8].getElemDoubleAt(index); // T33 - real
        Ti[2][2] = 0.0;                                                   // T33 - imag

        Tr[1][0] = Tr[0][1]; Ti[1][0] = -Ti[0][1];
        Tr[2][0] = Tr[0][2]; Ti[2][0] = -Ti[0][2];
        Tr[2][1] = Tr[1][2]; Ti[2][1] = -Ti[1][2];
    }

    /**
     * Get coherency matrix T4 for given pixel.
     * @param index X,Y coordinate of the given pixel
     * @param dataBuffers Source tile buffers for all 16 source bands
     * @param Tr Real part of the coherency matrix
     * @param Ti Imaginary part of the coherency matrix
     */
    public static void getCoherencyMatrixT4(final int index, final ProductData[] dataBuffers,
                                            final double[][] Tr, final double[][] Ti){

        Tr[0][0] = dataBuffers[0].getElemDoubleAt(index); // T11 - real
        Ti[0][0] = 0.0;                                   // T11 - imag

        Tr[0][1] = dataBuffers[1].getElemDoubleAt(index); // T12 - real
        Ti[0][1] = dataBuffers[2].getElemDoubleAt(index); // T12 - imag

        Tr[0][2] = dataBuffers[3].getElemDoubleAt(index); // T13 - real
        Ti[0][2] = dataBuffers[4].getElemDoubleAt(index); // T13 - imag

        Tr[0][3] = dataBuffers[5].getElemDoubleAt(index); // T14 - real
        Ti[0][3] = dataBuffers[6].getElemDoubleAt(index); // T14 - imag

        Tr[1][1] = dataBuffers[7].getElemDoubleAt(index); // T22 - real
        Ti[1][1] = 0.0;                                   // T22 - imag

        Tr[1][2] = dataBuffers[8].getElemDoubleAt(index); // T23 - real
        Ti[1][2] = dataBuffers[9].getElemDoubleAt(index); // T23 - imag

        Tr[1][3] = dataBuffers[10].getElemDoubleAt(index); // T24 - real
        Ti[1][3] = dataBuffers[11].getElemDoubleAt(index); // T24 - imag

        Tr[2][2] = dataBuffers[12].getElemDoubleAt(index); // T33 - real
        Ti[2][2] = 0.0;                                    // T33 - imag

        Tr[2][3] = dataBuffers[13].getElemDoubleAt(index); // T34 - real
        Ti[2][3] = dataBuffers[14].getElemDoubleAt(index); // T34 - imag

        Tr[3][3] = dataBuffers[15].getElemDoubleAt(index); // T44 - real
        Ti[3][3] = 0.0;                                    // T44 - imag

        Tr[1][0] = Tr[0][1]; Ti[1][0] = -Ti[0][1];
        Tr[2][0] = Tr[0][2]; Ti[2][0] = -Ti[0][2];
        Tr[2][1] = Tr[1][2]; Ti[2][1] = -Ti[1][2];
        Tr[3][0] = Tr[0][3]; Ti[3][0] = -Ti[0][3];
        Tr[3][1] = Tr[1][3]; Ti[3][1] = -Ti[1][3];
        Tr[3][2] = Tr[2][3]; Ti[3][2] = -Ti[2][3];
    }

    /**
     * Compute covariance matrix for given scatter matrix.
     * @param scatterRe Real part of the scatter matrix
     * @param scatterIm Imaginary part of the scatter matrix
     * @param Cr Real part of the covariance matrix
     * @param Ci Imaginary part of the covariance matrix
     */
    public static void computeCovarianceMatrixC3(final double[][] scatterRe, final double[][] scatterIm,
                                               final double[][] Cr, final double[][] Ci) {

        final double sHHr = scatterRe[0][0];
        final double sHHi = scatterIm[0][0];
        final double sHVr = scatterRe[0][1];
        final double sHVi = scatterIm[0][1];
        final double sVHr = scatterRe[1][0];
        final double sVHi = scatterIm[1][0];
        final double sVVr = scatterRe[1][1];
        final double sVVi = scatterIm[1][1];

        final double k1r = sHHr;
        final double k1i = sHHi;
        final double k2r = (sHVr + sVHr) / sqrt2;
        final double k2i = (sHVi + sVHi) / sqrt2;
        final double k3r = sVVr;
        final double k3i = sVVi;

        Cr[0][0] = k1r*k1r + k1i*k1i;
        Ci[0][0] = 0.0;

        Cr[0][1] = k1r*k2r + k1i*k2i;
        Ci[0][1] = k1i*k2r - k1r*k2i;

        Cr[0][2] = k1r*k3r + k1i*k3i;
        Ci[0][2] = k1i*k3r - k1r*k3i;

        Cr[1][1] = k2r*k2r + k2i*k2i;
        Ci[1][1] = 0.0;

        Cr[1][2] = k2r*k3r + k2i*k3i;
        Ci[1][2] = k2i*k3r - k2r*k3i;

        Cr[2][2] = k3r*k3r + k3i*k3i;
        Ci[2][2] = 0.0;

        Cr[1][0] = Cr[0][1]; Ci[1][0] = -Ci[0][1];
        Cr[2][0] = Cr[0][2]; Ci[2][0] = -Ci[0][2];
        Cr[2][1] = Cr[1][2]; Ci[2][1] = -Ci[1][2];
    }

    /**
     * Compute covariance matrix C4 for given scatter matrix.
     * @param scatterRe Real part of the scatter matrix
     * @param scatterIm Imaginary part of the scatter matrix
     * @param Cr Real part of the covariance matrix
     * @param Ci Imaginary part of the covariance matrix
     */
    public static void computeCovarianceMatrixC4(final double[][] scatterRe, final double[][] scatterIm,
                                                 final double[][] Cr, final double[][] Ci) {

        final double sHHr = scatterRe[0][0];
        final double sHHi = scatterIm[0][0];
        final double sHVr = scatterRe[0][1];
        final double sHVi = scatterIm[0][1];
        final double sVHr = scatterRe[1][0];
        final double sVHi = scatterIm[1][0];
        final double sVVr = scatterRe[1][1];
        final double sVVi = scatterIm[1][1];

        final double k1r = sHHr;
        final double k1i = sHHi;
        final double k2r = sHVr;
        final double k2i = sHVi;
        final double k3r = sVHr;
        final double k3i = sVHi;
        final double k4r = sVVr;
        final double k4i = sVVi;

        Cr[0][0] = k1r*k1r + k1i*k1i;
        Ci[0][0] = 0.0;

        Cr[0][1] = k1r*k2r + k1i*k2i;
        Ci[0][1] = k1i*k2r - k1r*k2i;

        Cr[0][2] = k1r*k3r + k1i*k3i;
        Ci[0][2] = k1i*k3r - k1r*k3i;

        Cr[0][3] = k1r*k4r + k1i*k4i;
        Ci[0][3] = k1i*k4r - k1r*k4i;

        Cr[1][1] = k2r*k2r + k2i*k2i;
        Ci[1][1] = 0.0;

        Cr[1][2] = k2r*k3r + k2i*k3i;
        Ci[1][2] = k2i*k3r - k2r*k3i;

        Cr[1][3] = k2r*k4r + k2i*k4i;
        Ci[1][3] = k2i*k4r - k2r*k4i;

        Cr[2][2] = k3r*k3r + k3i*k3i;
        Ci[2][2] = 0.0;

        Cr[2][3] = k3r*k4r + k3i*k4i;
        Ci[2][3] = k3i*k4r - k3r*k4i;

        Cr[3][3] = k4r*k4r + k4i*k4i;
        Ci[3][3] = 0.0;

        Cr[1][0] = Cr[0][1]; Ci[1][0] = -Ci[0][1];
        Cr[2][0] = Cr[0][2]; Ci[2][0] = -Ci[0][2];
        Cr[2][1] = Cr[1][2]; Ci[2][1] = -Ci[1][2];
        Cr[3][0] = Cr[0][3]; Ci[3][0] = -Ci[0][3];
        Cr[3][1] = Cr[1][3]; Ci[3][1] = -Ci[1][3];
        Cr[3][2] = Cr[2][3]; Ci[3][2] = -Ci[2][3];
    }

    /**
     * Compute coherency matrix T3 for given scatter matrix.
     * @param scatterRe Real part of the scatter matrix
     * @param scatterIm Imaginary part of the scatter matrix
     * @param Tr Real part of the coherency matrix
     * @param Ti Imaginary part of the coherency matrix
     */
    public static void computeCoherencyMatrixT3(final double[][] scatterRe, final double[][] scatterIm,
                                                final double[][] Tr, final double[][] Ti) {

        final double sHHr = scatterRe[0][0];
        final double sHHi = scatterIm[0][0];
        final double sHVr = scatterRe[0][1];
        final double sHVi = scatterIm[0][1];
        final double sVHr = scatterRe[1][0];
        final double sVHi = scatterIm[1][0];
        final double sVVr = scatterRe[1][1];
        final double sVVi = scatterIm[1][1];

        final double k1r = (sHHr + sVVr) / sqrt2;
        final double k1i = (sHHi + sVVi) / sqrt2;
        final double k2r = (sHHr - sVVr) / sqrt2;
        final double k2i = (sHHi - sVVi) / sqrt2;
        final double k3r = (sHVr + sVHr) / sqrt2;
        final double k3i = (sHVi + sVHi) / sqrt2;

        Tr[0][0] = k1r*k1r + k1i*k1i;
        Ti[0][0] = 0.0;

        Tr[0][1] = k1r*k2r + k1i*k2i;
        Ti[0][1] = k1i*k2r - k1r*k2i;

        Tr[0][2] = k1r*k3r + k1i*k3i;
        Ti[0][2] = k1i*k3r - k1r*k3i;

        Tr[1][1] = k2r*k2r + k2i*k2i;
        Ti[1][1] = 0.0;

        Tr[1][2] = k2r*k3r + k2i*k3i;
        Ti[1][2] = k2i*k3r - k2r*k3i;

        Tr[2][2] = k3r*k3r + k3i*k3i;
        Ti[2][2] = 0.0;

        Tr[1][0] = Tr[0][1]; Ti[1][0] = -Ti[0][1];
        Tr[2][0] = Tr[0][2]; Ti[2][0] = -Ti[0][2];
        Tr[2][1] = Tr[1][2]; Ti[2][1] = -Ti[1][2];
    }

    /**
     * Compute coherency matrix T4 for given scatter matrix.
     * @param scatterRe Real part of the scatter matrix
     * @param scatterIm Imaginary part of the scatter matrix
     * @param Tr Real part of the coherency matrix
     * @param Ti Imaginary part of the coherency matrix
     */
    public static void computeCoherencyMatrixT4(final double[][] scatterRe, final double[][] scatterIm,
                                                final double[][] Tr, final double[][] Ti) {

        final double sHHr = scatterRe[0][0];
        final double sHHi = scatterIm[0][0];
        final double sHVr = scatterRe[0][1];
        final double sHVi = scatterIm[0][1];
        final double sVHr = scatterRe[1][0];
        final double sVHi = scatterIm[1][0];
        final double sVVr = scatterRe[1][1];
        final double sVVi = scatterIm[1][1];

        final double k1r = (sHHr + sVVr) / sqrt2;
        final double k1i = (sHHi + sVVi) / sqrt2;
        final double k2r = (sHHr - sVVr) / sqrt2;
        final double k2i = (sHHi - sVVi) / sqrt2;
        final double k3r = (sHVr + sVHr) / sqrt2;
        final double k3i = (sHVi + sVHi) / sqrt2;
        final double k4r = (sVHi - sHVi) / sqrt2;
        final double k4i = (sHVr - sVHr) / sqrt2;

        Tr[0][0] = k1r*k1r + k1i*k1i;
        Ti[0][0] = 0.0;

        Tr[0][1] = k1r*k2r + k1i*k2i;
        Ti[0][1] = k1i*k2r - k1r*k2i;

        Tr[0][2] = k1r*k3r + k1i*k3i;
        Ti[0][2] = k1i*k3r - k1r*k3i;

        Tr[0][3] = k1r*k4r + k1i*k4i;
        Ti[0][3] = k1i*k4r - k1r*k4i;

        Tr[1][1] = k2r*k2r + k2i*k2i;
        Ti[1][1] = 0.0;

        Tr[1][2] = k2r*k3r + k2i*k3i;
        Ti[1][2] = k2i*k3r - k2r*k3i;

        Tr[1][3] = k2r*k4r + k2i*k4i;
        Ti[1][3] = k2i*k4r - k2r*k4i;

        Tr[2][2] = k3r*k3r + k3i*k3i;
        Ti[2][2] = 0.0;

        Tr[2][3] = k3r*k4r + k3i*k4i;
        Ti[2][3] = k3i*k4r - k3r*k4i;

        Tr[3][3] = k4r*k4r + k4i*k4i;
        Ti[3][3] = 0.0;

        Tr[1][0] = Tr[0][1]; Ti[1][0] = -Ti[0][1];
        Tr[2][0] = Tr[0][2]; Ti[2][0] = -Ti[0][2];
        Tr[2][1] = Tr[1][2]; Ti[2][1] = -Ti[1][2];
        Tr[3][0] = Tr[0][3]; Ti[3][0] = -Ti[0][3];
        Tr[3][1] = Tr[1][3]; Ti[3][1] = -Ti[1][3];
        Tr[3][2] = Tr[2][3]; Ti[3][2] = -Ti[2][3];
    }

    /**
     * Compute 3x3 correlation matrix for two given scatter matrices.
     * @param scatter1Re Real part of the 1st scatter matrix
     * @param scatter1Im Imaginary part of the 1st scatter matrix
     * @param scatter2Re Real part of the 2nd scatter matrix
     * @param scatter2Im Imaginary part of the 2nd scatter matrix
     * @param Tr Real part of the correlation matrix
     * @param Ti Imaginary part of the correlation matrix
     */
    public static void computeCorrelationMatrix(final double[][] scatter1Re, final double[][] scatter1Im,
                                                final double[][] scatter2Re, final double[][] scatter2Im,
                                                final double[][] Tr, final double[][] Ti) {

        final double s1HHr = scatter1Re[0][0];
        final double s1HHi = scatter1Im[0][0];
        final double s1HVr = scatter1Re[0][1];
        final double s1HVi = scatter1Im[0][1];
        final double s1VHr = scatter1Re[1][0];
        final double s1VHi = scatter1Im[1][0];
        final double s1VVr = scatter1Re[1][1];
        final double s1VVi = scatter1Im[1][1];

        final double s2HHr = scatter2Re[0][0];
        final double s2HHi = scatter2Im[0][0];
        final double s2HVr = scatter2Re[0][1];
        final double s2HVi = scatter2Im[0][1];
        final double s2VHr = scatter2Re[1][0];
        final double s2VHi = scatter2Im[1][0];
        final double s2VVr = scatter2Re[1][1];
        final double s2VVi = scatter2Im[1][1];

        final double k11r = (s1HHr + s1VVr) / sqrt2;
        final double k11i = (s1HHi + s1VVi) / sqrt2;
        final double k12r = (s1HHr - s1VVr) / sqrt2;
        final double k12i = (s1HHi - s1VVi) / sqrt2;
        final double k13r = (s1HVr + s1VHr) / sqrt2;
        final double k13i = (s1HVi + s1VHi) / sqrt2;

        final double k21r = (s2HHr + s2VVr) / sqrt2;
        final double k21i = (s2HHi + s2VVi) / sqrt2;
        final double k22r = (s2HHr - s2VVr) / sqrt2;
        final double k22i = (s2HHi - s2VVi) / sqrt2;
        final double k23r = (s2HVr + s2VHr) / sqrt2;
        final double k23i = (s2HVi + s2VHi) / sqrt2;

        Tr[0][0] = k11r*k21r + k11i*k21i;
        Ti[0][0] = k11i*k21r - k11r*k21i;

        Tr[0][1] = k11r*k22r + k11i*k22i;
        Ti[0][1] = k11i*k22r - k11r*k22i;

        Tr[0][2] = k11r*k23r + k11i*k23i;
        Ti[0][2] = k11i*k23r - k11r*k23i;

        Tr[1][0] = k12r*k21r + k12i*k21i;
        Ti[1][0] = k12i*k21r - k12r*k21i;

        Tr[1][1] = k12r*k22r + k12i*k22i;
        Ti[1][1] = k12i*k22r - k12r*k22i;

        Tr[1][2] = k12r*k23r + k12i*k23i;
        Ti[1][2] = k12i*k23r - k12r*k23i;

        Tr[2][0] = k13r*k21r + k13i*k21i;
        Ti[2][0] = k13i*k21r - k13r*k21i;

        Tr[2][1] = k13r*k22r + k13i*k22i;
        Ti[2][1] = k13i*k22r - k13r*k22i;

        Tr[2][2] = k13r*k23r + k13i*k23i;
        Ti[2][2] = k13i*k23r - k13r*k23i;
    }

    /**
     * Convert covariance matrix C4 to coherency matrix T4
     * @param c4Re Real part of C4 matrix
     * @param c4Im Imaginary part of C4 matrix
     * @param t4Re Real part of T4 matrix
     * @param t4Im Imaginary part of T4 matrix
     */
    public static void c4ToT4(final double[][] c4Re, final double[][] c4Im,
                              final double[][] t4Re, final double[][] t4Im) {

        t4Re[0][0] = 0.5*(c4Re[0][0] + 2*c4Re[0][3] + c4Re[3][3]);
        t4Im[0][0] = 0.5*(c4Im[0][0] + c4Im[3][3]);

        t4Re[0][1] = 0.5*(c4Re[0][0] - c4Re[3][3]);
        t4Im[0][1] = 0.5*(c4Im[0][0] - 2*c4Im[0][3] - c4Im[3][3]);

        t4Re[0][2] = 0.5*(c4Re[0][1] + c4Re[1][3] + c4Re[0][2] + c4Re[2][3]);
        t4Im[0][2] = 0.5*(c4Im[0][1] - c4Im[1][3] + c4Im[0][2] - c4Im[2][3]);

        t4Re[0][3] = 0.5*(c4Im[0][1] - c4Im[1][3] - c4Im[0][2] + c4Im[2][3]);
        t4Im[0][3] = 0.5*(-c4Re[0][1] - c4Re[1][3] + c4Re[0][2] + c4Re[2][3]);

        t4Re[1][0] = t4Re[0][1];
        t4Im[1][0] = -t4Im[0][1];

        t4Re[1][1] = 0.5*(c4Re[0][0] - 2*c4Re[0][3] + c4Re[3][3]);
        t4Im[1][1] = 0.5*(c4Im[0][0] + c4Im[3][3]);

        t4Re[1][2] = 0.5*(c4Re[0][1] - c4Re[1][3] + c4Re[0][2] - c4Re[2][3]);
        t4Im[1][2] = 0.5*(c4Im[0][1] + c4Im[1][3] + c4Im[0][2] + c4Im[2][3]);

        t4Re[1][3] = 0.5*(c4Im[0][1] + c4Im[1][3] - c4Im[0][2] - c4Im[2][3]);
        t4Im[1][3] = 0.5*(-c4Re[0][1] + c4Re[1][3] + c4Re[0][2] - c4Re[2][3]);

        t4Re[2][0] = t4Re[0][2];
        t4Im[2][0] = -t4Im[0][2];

        t4Re[2][1] = t4Re[1][2];
        t4Im[2][1] = -t4Im[1][2];

        t4Re[2][2] = 0.5*(c4Re[1][1] + 2*c4Re[1][2] + c4Re[2][2]);
        t4Im[2][2] = 0.5*(c4Im[1][1] + c4Im[2][2]);

        t4Re[2][3] = 0.5*(c4Im[1][1] - 2*c4Im[1][2] - c4Im[2][2]);
        t4Im[2][3] = 0.5*(-c4Re[1][1] + c4Re[2][2]);

        t4Re[3][0] = t4Re[0][3];
        t4Im[3][0] = -t4Im[0][3];

        t4Re[3][1] = t4Re[1][3];
        t4Im[3][1] = -t4Im[1][3];

        t4Re[3][2] = t4Re[2][3];
        t4Im[3][2] = -t4Im[2][3];

        t4Re[3][3] = 0.5*(c4Re[1][1] - 2*c4Re[1][2] + c4Re[2][2]);
        t4Im[3][3] = 0.5*(c4Im[1][1] + c4Im[2][2]);
    }

    /**
     * Convert coherency matrix T4 to covariance matrix C4
     * @param t4Re Real part of T4 matrix
     * @param t4Im Imaginary part of T4 matrix
     * @param c4Re Real part of C4 matrix
     * @param c4Im Imaginary part of C4 matrix
     */
    public static void t4ToC4(final double[][] t4Re, final double[][] t4Im,
                              final double[][] c4Re, final double[][] c4Im) {

        c4Re[0][0] = 0.5*(t4Re[0][0] + t4Re[0][1] + t4Re[1][0] + t4Re[1][1]);
        c4Im[0][0] = 0.0;

        c4Re[0][1] = 0.5*(t4Re[0][2] - t4Im[0][3] + t4Re[1][2] - t4Im[1][3]);
        c4Im[0][1] = 0.5*(t4Im[0][2] + t4Re[0][3] + t4Im[1][2] + t4Re[1][3]);

        c4Re[0][2] = 0.5*(t4Re[0][2] + t4Im[0][3] + t4Re[1][2] + t4Im[1][3]);
        c4Im[0][2] = 0.5*(t4Im[0][2] - t4Re[0][3] + t4Im[1][2] - t4Re[1][3]);

        c4Re[0][3] = 0.5*(t4Re[0][0] - t4Re[0][1] + t4Re[1][0] - t4Re[1][1]);
        c4Im[0][3] = 0.5*(t4Im[0][0] - t4Im[0][1] + t4Im[1][0] - t4Im[1][1]);

        c4Re[1][0] = c4Re[0][1];
        c4Im[1][0] = -c4Im[0][1];

        c4Re[1][1] = 0.5*(t4Re[2][2] - t4Im[2][3] + t4Im[3][2] + t4Re[3][3]);
        c4Im[1][1] = 0.0;

        c4Re[1][2] = 0.5*(t4Re[2][2] + t4Im[2][3] + t4Im[3][2] - t4Re[3][3]);
        c4Im[1][2] = 0.5*(t4Im[2][2] - t4Re[2][3] - t4Re[3][2] - t4Im[3][3]);

        c4Re[1][3] = 0.5*(t4Re[2][0] - t4Re[2][1] + t4Im[3][0] - t4Im[3][1]);
        c4Im[1][3] = 0.5*(t4Im[2][0] - t4Im[2][1] - t4Re[3][0] + t4Re[3][1]);

        c4Re[2][0] = c4Re[0][2];
        c4Im[2][0] = -c4Im[0][2];

        c4Re[2][1] = c4Re[1][2];
        c4Im[2][1] = -c4Im[1][2];

        c4Re[2][2] = 0.5*(t4Re[2][2] + t4Im[2][3] - t4Im[3][2] + t4Re[3][3]);
        c4Im[2][2] = 0.0;

        c4Re[2][3] = 0.5*(t4Re[2][0] - t4Re[2][1] - t4Im[3][0] + t4Im[3][1]);
        c4Im[2][3] = 0.5*(t4Im[2][0] - t4Im[2][1] + t4Re[3][0] - t4Re[3][1]);

        c4Re[3][0] = c4Re[0][3];
        c4Im[3][0] = -c4Im[0][3];

        c4Re[3][1] = c4Re[1][3];
        c4Im[3][1] = -c4Im[1][3];

        c4Re[3][2] = c4Re[2][3];
        c4Im[3][2] = -c4Im[2][3];

        c4Re[3][3] = 0.5*(t4Re[0][0] - t4Re[0][1] - t4Re[1][0] + t4Re[1][1]);
        c4Im[3][3] = 0.0;
    }

    /**
     * Convert covariance matrix C3 to coherency matrix T3
     * @param c3Re Real part of C3 matrix
     * @param c3Im Imaginary part of C3 matrix
     * @param t3Re Real part of T3 matrix
     * @param t3Im Imaginary part of T3 matrix
     */
    public static void c3ToT3(final double[][] c3Re, final double[][] c3Im,
                              final double[][] t3Re, final double[][] t3Im) {

        t3Re[0][0] = (c3Re[0][0] + 2*c3Re[0][2] + c3Re[2][2]) / 2;
        t3Im[0][0] = 0.0;
        t3Re[0][1] = (c3Re[0][0] - c3Re[2][2]) / 2;
        t3Im[0][1] = -c3Im[0][2];
        t3Re[0][2] = (c3Re[0][1] + c3Re[1][2]) / sqrt2;
        t3Im[0][2] = (c3Im[0][1] - c3Im[1][2]) / sqrt2;

        t3Re[1][0] = t3Re[0][1];
        t3Im[1][0] = -t3Im[0][1];
        t3Re[1][1] = (c3Re[0][0] - 2*c3Re[0][2] + c3Re[2][2]) / 2;
        t3Im[1][1] = 0.0;
        t3Re[1][2] = (c3Re[0][1] - c3Re[1][2]) / sqrt2;
        t3Im[1][2] = (c3Im[0][1] + c3Im[1][2]) / sqrt2;

        t3Re[2][0] = t3Re[0][2];
        t3Im[2][0] = -t3Im[0][2];
        t3Re[2][1] = t3Re[1][2];
        t3Im[2][1] = -t3Im[1][2];
        t3Re[2][2] = c3Re[1][1];
        t3Im[2][2] = 0.0;
    }

    /**
     * Convert coherency matrix T3 to covariance matrix C3
     * @param t3Re Real part of T3 matrix
     * @param t3Im Imaginary part of T3 matrix
     * @param c3Re Real part of C3 matrix
     * @param c3Im Imaginary part of C3 matrix
     */
    public static void t3ToC3(final double[][] t3Re, final double[][] t3Im,
                              final double[][] c3Re, final double[][] c3Im) {

        c3Re[0][0] = 0.5*(t3Re[0][0] + t3Re[0][1] + t3Re[1][0] + t3Re[1][1]);
        c3Im[0][0] = 0.0;

        c3Re[0][1] = (t3Re[0][2] + t3Re[1][2]) / sqrt2;
        c3Im[0][1] = (t3Im[0][2] + t3Im[1][2]) / sqrt2;

        c3Re[0][2] = 0.5*(t3Re[0][0] - t3Re[0][1] + t3Re[1][0] - t3Re[1][1]);
        c3Im[0][2] = 0.5*(t3Im[0][0] - t3Im[0][1] + t3Im[1][0] - t3Im[1][1]);

        c3Re[1][0] = c3Re[0][1];
        c3Im[1][0] = -c3Im[0][1];

        c3Re[1][1] = t3Re[2][2];
        c3Im[1][1] = 0.0;

        c3Re[1][2] = (t3Re[2][0] - t3Re[2][1]) / sqrt2;
        c3Im[1][2] = (t3Im[2][0] - t3Im[2][1]) / sqrt2;

        c3Re[2][0] = c3Re[0][2];
        c3Im[2][0] = -c3Im[0][2];

        c3Re[2][1] = c3Re[1][2];
        c3Im[2][1] = -c3Im[1][2];

        c3Re[2][2] = 0.5*(t3Re[0][0] - t3Re[0][1] - t3Re[1][0] + t3Re[1][1]);
        c3Im[2][2] = 0.0;
    }

    /**
     * Convert coherency matrix T4 to coherency matrix T3
     * @param t4Re Real part of T4 matrix
     * @param t4Im Imaginary part of T4 matrix
     * @param t3Re Real part of T3 matrix
     * @param t3Im Imaginary part of T3 matrix
     */
    public static void t4ToT3(final double[][] t4Re, final double[][] t4Im,
                              final double[][] t3Re, final double[][] t3Im) {

        // loop unwrapping
        System.arraycopy(t4Re[0],0,t3Re[0],0,t3Re[0].length);
        System.arraycopy(t4Im[0],0,t3Im[0],0,t3Im[0].length);

        System.arraycopy(t4Re[1],0,t3Re[1],0,t3Re[1].length);
        System.arraycopy(t4Im[1],0,t3Im[1],0,t3Im[1].length);

        System.arraycopy(t4Re[2],0,t3Re[2],0,t3Re[2].length);
        System.arraycopy(t4Im[2],0,t3Im[2],0,t3Im[2].length);
    }

    /**
     * Get mean coherency matrix for given pixel.
     * @param x X coordinate of the given pixel.
     * @param y Y coordinate of the given pixel.
     * @param halfWindowSize The sliding window size / 2.
     * @param sourceProductType The source product type.
     * @param sourceImageWidth The source image width.
     * @param sourceImageHeight The source image height.
     * @param srcIndex The TileIndex of the first source tile
     * @param dataBuffers Source tile data buffers.
     * @param Tr The real part of the mean coherency matrix.
     * @param Ti The imaginary part of the mean coherency matrix.
     */
    public static void getMeanCoherencyMatrix(
            final int x, final int y, final int halfWindowSize, final int sourceImageWidth, final int sourceImageHeight,
            final PolBandUtils.MATRIX sourceProductType, final TileIndex srcIndex, final ProductData[] dataBuffers,
            final double[][] Tr, final double[][] Ti) {

        final double[][] tempSr = new double[2][2];
        final double[][] tempSi = new double[2][2];
        final double[][] tempCr = new double[3][3];
        final double[][] tempCi = new double[3][3];
        final double[][] tempTr = new double[3][3];
        final double[][] tempTi = new double[3][3];

        final int xSt = FastMath.max(x - halfWindowSize, 0);
        final int xEd = FastMath.min(x + halfWindowSize, sourceImageWidth - 1);
        final int ySt = FastMath.max(y - halfWindowSize, 0);
        final int yEd = FastMath.min(y + halfWindowSize, sourceImageHeight - 1);
        final int num = (yEd - ySt + 1)*(xEd - xSt + 1);

        final Matrix TrMat = new Matrix(3,3);
        final Matrix TiMat = new Matrix(3,3);

        if (sourceProductType == PolBandUtils.MATRIX.T3) {

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    getCoherencyMatrixT3(srcIndex.getIndex(xx), dataBuffers, tempTr, tempTi);
                    TrMat.plusEquals(new Matrix(tempTr));
                    TiMat.plusEquals(new Matrix(tempTi));
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.C3) {

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    getCovarianceMatrixC3(srcIndex.getIndex(xx), dataBuffers, tempCr, tempCi);
                    c3ToT3(tempCr, tempCi, tempTr, tempTi);
                    TrMat.plusEquals(new Matrix(tempTr));
                    TiMat.plusEquals(new Matrix(tempTi));
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.FULL) {

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    getComplexScatterMatrix(srcIndex.getIndex(xx), dataBuffers, tempSr, tempSi);
                    computeCoherencyMatrixT3(tempSr, tempSi, tempTr, tempTi);
                    TrMat.plusEquals(new Matrix(tempTr));
                    TiMat.plusEquals(new Matrix(tempTi));
                }
            }
        }

        TrMat.timesEquals(1.0/num);
        TiMat.timesEquals(1.0/num);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Tr[i][j] = TrMat.get(i,j);
                Ti[i][j] = TiMat.get(i,j);
            }
        }
    }

    /**
     * Get mean covariance matrix for given pixel.
     * @param x X coordinate of the given pixel.
     * @param y Y coordinate of the given pixel.
     * @param halfWindowSize The sliding window size / 2
     * @param sourceProductType The source product type.
     * @param sourceImageWidth The source image width.
     * @param sourceImageHeight The source image height.
     * @param sourceTiles The source tiles for all bands.
     * @param dataBuffers Source tile data buffers.
     * @param Cr The real part of the mean covariance matrix.
     * @param Ci The imaginary part of the mean covariance matrix.
     */
    public static void getMeanCovarianceMatrix(
            final int x, final int y, final int halfWindowSize, final int sourceImageWidth, final int sourceImageHeight,
            final PolBandUtils.MATRIX sourceProductType, final Tile[] sourceTiles, final ProductData[] dataBuffers,
            final double[][] Cr, final double[][] Ci) {

        final double[][] tempSr = new double[2][2];
        final double[][] tempSi = new double[2][2];
        final double[][] tempCr = new double[3][3];
        final double[][] tempCi = new double[3][3];
        final double[][] tempTr = new double[3][3];
        final double[][] tempTi = new double[3][3];

        final int xSt = FastMath.max(x - halfWindowSize, sourceTiles[0].getMinX());
        final int xEd = FastMath.min(x + halfWindowSize, sourceTiles[0].getMaxX() - 1);
        final int ySt = FastMath.max(y - halfWindowSize, sourceTiles[0].getMinY());
        final int yEd = FastMath.min(y + halfWindowSize, sourceTiles[0].getMaxY() - 1);
        final int num = (yEd - ySt + 1)*(xEd - xSt + 1);

        final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

        final Matrix CrMat = new Matrix(3,3);
        final Matrix CiMat = new Matrix(3,3);

        if (sourceProductType == PolBandUtils.MATRIX.C3) {

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    getCovarianceMatrixC3(srcIndex.getIndex(xx), dataBuffers, tempCr, tempCi);
                    CrMat.plusEquals(new Matrix(tempCr));
                    CiMat.plusEquals(new Matrix(tempCi));
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.T3) {

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    getCoherencyMatrixT3(srcIndex.getIndex(xx), dataBuffers, tempTr, tempTi);
                    t3ToC3(tempTr, tempTi, tempCr, tempCi);
                    CrMat.plusEquals(new Matrix(tempCr));
                    CiMat.plusEquals(new Matrix(tempCi));
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.FULL) {

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    getComplexScatterMatrix(srcIndex.getIndex(xx), dataBuffers, tempSr, tempSi);
                    computeCovarianceMatrixC3(tempSr, tempSi, tempCr, tempCi);
                    CrMat.plusEquals(new Matrix(tempCr));
                    CiMat.plusEquals(new Matrix(tempCi));
                }
            }
        }

        CrMat.timesEquals(1.0/num);
        CiMat.timesEquals(1.0/num);
        for (int i = 0; i < 3; i++) {
            for (int j = 0; j < 3; j++) {
                Cr[i][j] = CrMat.get(i,j);
                Ci[i][j] = CiMat.get(i,j);
            }
        }
    }


    public static void getT3(final int index, final PolBandUtils.MATRIX sourceProductType, final ProductData[] dataBuffers,
                       final double[][] Tr, final double[][] Ti) {

        if (sourceProductType == PolBandUtils.MATRIX.FULL) {

            final double[][] Sr = new double[2][2];
            final double[][] Si = new double[2][2];
            getComplexScatterMatrix(index, dataBuffers, Sr, Si);
            computeCoherencyMatrixT3(Sr, Si, Tr, Ti);

        } else if (sourceProductType == PolBandUtils.MATRIX.T3) {

            getCoherencyMatrixT3(index, dataBuffers, Tr, Ti);

        } else if (sourceProductType == PolBandUtils.MATRIX.C3) {

            final double[][] Cr = new double[3][3];
            final double[][] Ci = new double[3][3];
            getCovarianceMatrixC3(index, dataBuffers, Cr, Ci);
            c3ToT3(Cr, Ci, Tr, Ti);
        }
    }

    /**
     * Perform eigenvalue decomposition for a given Hermitian matrix
     * @param n Matrix dimension
     * @param HMr Real part of the Hermitian matrix
     * @param HMi Imaginary part of the Hermitian matrix
     * @param EigenVectRe Real part of the eigenvector matrix
     * @param EigenVectIm Imaginary part of the eigenvector matrix
     * @param EigenVal Eigenvalue vector
     */
    public static void eigenDecomposition(final int n, final double[][] HMr, final double[][] HMi,
                                          final double[][] EigenVectRe, final double[][] EigenVectIm, final double[] EigenVal) {

        final double[][] ar = new double[n][n];
        final double[][] ai = new double[n][n];
        final double[][] vr = new double[n][n];
        final double[][] vi = new double[n][n];
        final double[] d = new double[n];
        final double[] z = new double[n];
        final double[] w = new double[2];
        final double[] s = new double[2];
        final double[] c = new double[2];
        final double[] titi = new double[2];
        final double[] gc = new double[2];
        final double[] hc = new double[2];
        double sm, tresh, x, toto, e, f, g, h, r, d1, d2;
        int p, q, ii, i, j, k;
        int n2 = n*n;

        for (i = 0; i < n; i++) {
            for (j = 0; j < n; j++) {
                ar[i][j] = HMr[i][j];
                ai[i][j] = HMi[i][j];
                vr[i][j] = 0.0;
                vi[i][j] = 0.0;
            }
            vr[i][i] = 1.;
            vi[i][i] = 0.;

            d[i] = ar[i][i];
            z[i] = 0.;
        }

        final int iiMax = 1000*n2;
        for (ii = 1; ii < iiMax; ii++) {

            sm = 0.;
            for (p = 0; p < n-1; p++) {
                for (q = p + 1; q < n; q++) {
                    sm += 2.0*Math.sqrt(ar[p][q]*ar[p][q] + ai[p][q]*ai[p][q]);
                }
            }
            sm /= (n2 - n);

            if (sm < 1.E-16) {
                break;
            }

            tresh = 1.E-17;
            if (ii < 4) {
                tresh = (long) 0.2*sm / n2;
            }

            x = -1.E-15;
            p = 0;
            q = 0;
            for (i = 0; i < n-1; i++) {
                for (j = i + 1; j < n; j++) {
                    toto = Math.sqrt(ar[i][j]*ar[i][j] + ai[i][j]*ai[i][j]);
                    if (x < toto) {
                        x = toto;
                        p = i;
                        q = j;
                    }
                }
            }
            toto = Math.sqrt(ar[p][q]*ar[p][q] + ai[p][q]*ai[p][q]);
            if (toto > tresh) {
                e = d[p] - d[q];
                w[0] = ar[p][q];
                w[1] = ai[p][q];
                g = Math.sqrt(w[0]*w[0] + w[1]*w[1]);
                g = g*g;
                f = Math.sqrt(e*e + 4.0*g);
                d1 = e + f;
                d2 = e - f;
                if (Math.abs(d2) > Math.abs(d1)) {
                    d1 = d2;
                }
                r = Math.abs(d1) / Math.sqrt(d1*d1 + 4.0*g);
                s[0] = r;
                s[1] = 0.0;
                titi[0] = 2.0*r/d1;
                titi[1] = 0.0;
                c[0] = titi[0]*w[0] - titi[1]*w[1];
                c[1] = titi[0]*w[1] + titi[1]*w[0];
                r = Math.sqrt(s[0]*s[0] + s[1]*s[1]);
                r = r*r;
                h = (d1/2.0 + 2.0*g/d1)*r;
                d[p] = d[p] - h;
                z[p] = z[p] - h;
                d[q] = d[q] + h;
                z[q] = z[q] + h;
                ar[p][q] = 0.0;
                ai[p][q] = 0.0;

                for (j = 0; j < p; j++) {
                    gc[0] = ar[j][p];
                    gc[1] = ai[j][p];
                    hc[0] = ar[j][q];
                    hc[1] = ai[j][q];
                    ar[j][p] = c[0]*gc[0] - c[1]*gc[1] - s[0]*hc[0] - s[1]*hc[1];
                    ai[j][p] = c[0]*gc[1] + c[1]*gc[0] - s[0]*hc[1] + s[1]*hc[0];
                    ar[j][q] = s[0]*gc[0] - s[1]*gc[1] + c[0]*hc[0] + c[1]*hc[1];
                    ai[j][q] = s[0]*gc[1] + s[1]*gc[0] + c[0]*hc[1] - c[1]*hc[0];
                }
                for (j = p+1; j < q; j++) {
                    gc[0] = ar[p][j];
                    gc[1] = ai[p][j];
                    hc[0] = ar[j][q];
                    hc[1] = ai[j][q];
                    ar[p][j] = c[0]*gc[0] + c[1]*gc[1] - s[0]*hc[0] - s[1]*hc[1];
                    ai[p][j] = c[0]*gc[1] - c[1]*gc[0] + s[0]*hc[1] - s[1]*hc[0];
                    ar[j][q] = s[0]*gc[0] + s[1]*gc[1] + c[0]*hc[0] + c[1]*hc[1];
                    ai[j][q] = -s[0]*gc[1] + s[1]*gc[0] + c[0]*hc[1] - c[1]*hc[0];
                }
                for (j = q+1; j < n; j++) {
                    gc[0] = ar[p][j];
                    gc[1] = ai[p][j];
                    hc[0] = ar[q][j];
                    hc[1] = ai[q][j];
                    ar[p][j] = c[0]*gc[0] + c[1]*gc[1] - s[0]*hc[0] + s[1]*hc[1];
                    ai[p][j] = c[0]*gc[1] - c[1]*gc[0] - s[0]*hc[1] - s[1]*hc[0];
                    ar[q][j] = s[0]*gc[0] + s[1]*gc[1] + c[0]*hc[0] - c[1]*hc[1];
                    ai[q][j] = s[0]*gc[1] - s[1]*gc[0] + c[0]*hc[1] + c[1]*hc[0];
                }
                for (j = 0; j < n; j++) {
                    gc[0] = vr[j][p];
                    gc[1] = vi[j][p];
                    hc[0] = vr[j][q];
                    hc[1] = vi[j][q];
                    vr[j][p] = c[0] * gc[0] - c[1] * gc[1] - s[0] * hc[0] - s[1] * hc[1];
                    vi[j][p] = c[0] * gc[1] + c[1] * gc[0] - s[0] * hc[1] + s[1] * hc[0];
                    vr[j][q] = s[0] * gc[0] - s[1] * gc[1] + c[0] * hc[0] + c[1] * hc[1];
                    vi[j][q] = s[0] * gc[1] + s[1] * gc[0] + c[0] * hc[1] - c[1] * hc[0];
                }
            }
        }

        for (k = 0; k < n; k++) {
            d[k] = 0;
            for (i = 0; i < n; i++) {
                for (j = 0; j < n; j++) {
                    d[k] = d[k] + vr[i][k]*(HMr[i][j]*vr[j][k] - HMi[i][j]*vi[j][k]);
                    d[k] = d[k] + vi[i][k]*(HMr[i][j]*vi[j][k] + HMi[i][j]*vr[j][k]);
                }
            }
        }

        double tmp_r, tmp_i;
        for (i = 0; i < n; i++) {
            for (j = i + 1; j < n; j++) {
                if (d[j] > d[i]) {
                    x = d[i];
                    d[i] = d[j];
                    d[j] = x;
                    for (k = 0; k < n; k++) {
                        tmp_r = vr[k][i];
                        tmp_i = vi[k][i];
                        vr[k][i] = vr[k][j];
                        vi[k][i] = vi[k][j];
                        vr[k][j] = tmp_r;
                        vi[k][j] = tmp_i;
                    }
                }
            }
        }

        for (i = 0; i < n; i++) {
            EigenVal[i] = d[i];
            for (j = 0; j < n; j++) {
                EigenVectRe[i][j] = vr[i][j];
                EigenVectIm[i][j] = vi[i][j];
            }
        }

    }
    /*
    // Eigenvalue decomposition using JAMA svd function
    public static void eigenDecomposition(int n, double[][] HMr, double[][] HMi,
                                           double[][] EigenVectRe, double[][] EigenVectIm, double[] EigenVal) {

        final double[][] H = new double[2*n][2*n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                H[i][j] = HMr[i][j];
                H[n+i][n+j] = HMr[i][j];
                H[i][n+j] = -HMi[i][j];
                H[n+i][j] = HMi[i][j];
            }
        }

        final Matrix M = new Matrix(H);
        final SingularValueDecomposition Svd = M.svd(); // M = U*S*V'
        final Matrix U = Svd.getU();
        final Matrix D = Svd.getS();

        for (int i = 0; i < n; i++) {
            final int i2 = i * 2;
            EigenVal[i] = D.get(i2, i2);
            for (int j = 0; j < n; j++) {
                EigenVectRe[j][i] = U.get(j, i2);
                EigenVectIm[j][i] = U.get(n+j, i2);
            }
        }
    }
    */

    /**
     * Eigenvalue decomposition of general complex matrix using JAMA eig function
      */
    public static void eigenDecompGeneral(int n, double[][] HMr, double[][] HMi,
                                          double[][] EigenVectRe, double[][] EigenVectIm, double[] EigenVal) {

        final double[][] H = new double[2*n][2*n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                H[i][j] = HMr[i][j];
                H[n+i][n+j] = HMr[i][j];
                H[i][n+j] = -HMi[i][j];
                H[n+i][j] = HMi[i][j];
            }
        }

        final Matrix M = new Matrix(H);
        final EigenvalueDecomposition Evd = M.eig(); // M = V*D*V'
        final Matrix V = Evd.getV();
        final Matrix D = Evd.getD();

        final int n2 = n*2;
        double[][] d = D.getArray();
        double[][] v = V.getArray();
        double x;
        for (int i = 0; i < n2; i++) {
            for (int j = i + 1; j < n2; j++) {
                if (Math.abs(d[j][j]) > Math.abs(d[i][i])) {
                    x = d[i][i];
                    d[i][i] = d[j][j];
                    d[j][j] = x;
                    for (int k = 0; k < n2; k++) {
                        x = v[k][i];
                        v[k][i] = v[k][j];
                        v[k][j] = x;
                    }
                }
            }
        }

        for (int i = 0; i < n; i++) {
            final int i2 = i*2;
            EigenVal[i] = d[i2][i2];
            for (int j = 0; j < n; j++) {
                EigenVectRe[j][i] = v[j][i2];
                EigenVectIm[j][i] = v[n+j][i2];
            }
        }
    }


    public static void inverseComplexMatrix(
            final int n, final double[][] Mr, final double[][] Mi, double[][] invMr, double[][] invMi) {

        final int n2 = n*2;
        final double[][] M = new double[n2][n2];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                M[i][j] = Mr[i][j];
                M[i+n][j+n] = Mr[i][j];
                M[i][j+n] = -Mi[i][j];
                M[i+n][j] = Mi[i][j];
            }
        }

        final double[][] I = new double[n2][n];
        for (int i = 0; i < n2; i++) {
            for (int j = 0; j < n; j++) {
                if (i == j) {
                    I[i][j] = 1;
                } else {
                    I[i][j] = 0;
                }
            }
        }

        final Matrix MMat = new Matrix(M);
        final Matrix IMat = new Matrix(I);
        final Matrix invMMat = MMat.inverse().times(IMat);

        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                invMr[i][j] = invMMat.get(i,j);
                invMi[i][j] = invMMat.get(i+n,j);
            }
        }
    }

    //===================== Compact Pol functions ============================

    /**
     * Get mean covariance matrix C2 for given pixel.
     * @param x X coordinate of the given pixel.
     * @param y Y coordinate of the given pixel.
     * @param halfWindowSize The sliding window size / 2
     * @param sourceProductType The source product type.
     * @param sourceTiles The source tiles for all bands.
     * @param dataBuffers Source tile data buffers.
     * @param Cr The real part of the mean covariance matrix.
     * @param Ci The imaginary part of the mean covariance matrix.
     */
    public static void getMeanCovarianceMatrixC2(
            final int x, final int y, final int halfWindowSize, final PolBandUtils.MATRIX sourceProductType,
            final Tile[] sourceTiles, final ProductData[] dataBuffers, final double[][] Cr, final double[][] Ci) {

        final double[] tempKr = new double[2];
        final double[] tempKi = new double[2];
        final double[][] tempCr = new double[2][2];
        final double[][] tempCi = new double[2][2];

        final int xSt = FastMath.max(x - halfWindowSize, sourceTiles[0].getMinX());
        final int xEd = FastMath.min(x + halfWindowSize, sourceTiles[0].getMaxX() - 1);
        final int ySt = FastMath.max(y - halfWindowSize, sourceTiles[0].getMinY());
        final int yEd = FastMath.min(y + halfWindowSize, sourceTiles[0].getMaxY() - 1);
        final int num = (yEd - ySt + 1)*(xEd - xSt + 1);

        final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

        final Matrix CrMat = new Matrix(2,2);
        final Matrix CiMat = new Matrix(2,2);

        if (sourceProductType == PolBandUtils.MATRIX.C2) {

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    getCovarianceMatrixC2(srcIndex.getIndex(xx), dataBuffers, tempCr, tempCi);
                    CrMat.plusEquals(new Matrix(tempCr));
                    CiMat.plusEquals(new Matrix(tempCi));
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.COMPACT) {

            for (int yy = ySt; yy <= yEd; ++yy) {
                srcIndex.calculateStride(yy);
                for (int xx = xSt; xx <= xEd; ++xx) {
                    getCompactPolScatterVector(srcIndex.getIndex(xx), dataBuffers, tempKr, tempKi);
                    computeCovarianceMatrixC2(tempKr, tempKi, tempCr, tempCi);
                    CrMat.plusEquals(new Matrix(tempCr));
                    CiMat.plusEquals(new Matrix(tempCi));
                }
            }
        }

        CrMat.timesEquals(1.0/num);
        CiMat.timesEquals(1.0/num);
        for (int i = 0; i < 2; i++) {
            for (int j = 0; j < 2; j++) {
                Cr[i][j] = CrMat.get(i,j);
                Ci[i][j] = CiMat.get(i,j);
            }
        }
    }

    /**
     * Get covariance matrix C2 for a given pixel in the input C2 product.
     * @param index X,Y coordinate of the given pixel
     * @param dataBuffers Source tile data buffers for all 4 source bands
     * @param Cr Real part of the 2x2 covariance matrix
     * @param Ci Imaginary part of the 2x2 covariance matrix
     */
    public static void getCovarianceMatrixC2(final int index, final ProductData[] dataBuffers,
                                             final double[][] Cr, final double[][] Ci){

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
     * Get compact pol scatter vector for a given pixel in the input compact pol product.
     * @param index X,Y coordinate of the given pixel
     * @param dataBuffers Source tiles dataBuffers for all 4 source bands
     * @param kr Real part of the scatter vector
     * @param ki Imaginary part of the scatter vector
     */
    public static void getCompactPolScatterVector(final int index, final ProductData[] dataBuffers,
                                                  final double[] kr, final double[] ki){

        kr[0] = dataBuffers[0].getElemDoubleAt(index); // RH - real
        ki[0] = dataBuffers[1].getElemDoubleAt(index); // RH - imag

        kr[1] = dataBuffers[2].getElemDoubleAt(index); // RV - real
        ki[1] = dataBuffers[3].getElemDoubleAt(index); // RV - imag
    }

    /**
     * Compute covariance matrix c2 for given complex compact pol 2x1 scatter vector.
     * @param kr Real part of the scatter vector
     * @param ki Imaginary part of the scatter vector
     * @param Cr Real part of the covariance matrix
     * @param Ci Imaginary part of the covariance matrix
     */
    public static void computeCovarianceMatrixC2(final double[] kr, final double[] ki,
                                                 final double[][] Cr, final double[][] Ci) {

        Cr[0][0] = kr[0]*kr[0] + ki[0]*ki[0];
        Ci[0][0] = 0.0;

        Cr[0][1] = kr[0]*kr[1] + ki[0]*ki[1];
        Ci[0][1] = ki[0]*kr[1] - kr[0]*ki[1];

        Cr[1][1] = kr[1]*kr[1] + ki[1]*ki[1];
        Ci[1][1] = 0.0;

        Cr[1][0] = Cr[0][1];
        Ci[1][0] = -Ci[0][1];
    }

    /**
     * Simulate compact pol 2x1 complex scattering vector using scatter matrix for a given pixel in a full pol product.
     * @param compactMode Compact polarimetric mode
     * @param scatterRe Real part of the scatter matrix
     * @param scatterIm Imaginary part of the scatter matrix
     * @param kr Real part of the scattering vector
     * @param ki Imaginary part of the scattering vector
     */
    public static void computeCompactPolScatteringVector(
            final String compactMode, final double[][] scatterRe, final double[][] scatterIm,
            final double[] kr, final double[] ki) {

        final double sHHr = scatterRe[0][0];
        final double sHHi = scatterIm[0][0];
        final double sHVr = scatterRe[0][1];
        final double sHVi = scatterIm[0][1];
        final double sVHr = scatterRe[1][0];
        final double sVHi = scatterIm[1][0];
        final double sVVr = scatterRe[1][1];
        final double sVVi = scatterIm[1][1];

        if (compactMode.equals(quarterPi)) {
            kr[0] = (sHHr + sHVr)/sqrt2;
            ki[0] = (sHHi + sHVi)/sqrt2;
            kr[1] = (sVVr + sVHr)/sqrt2;
            ki[1] = (sVVi + sVHi)/sqrt2;
        } else if (compactMode.equals(rch)) {
            kr[0] = (sHHr + sHVi)/sqrt2;
            ki[0] = (sHHi - sHVr)/sqrt2;
            kr[1] = (sVHr + sVVi)/sqrt2;
            ki[1] = (sVHi - sVVr)/sqrt2;
        } else if (compactMode.equals(lch)) {
            kr[0] = (sHHr - sHVi)/sqrt2;
            ki[0] = (sHHi + sHVr)/sqrt2;
            kr[1] = (sVHr - sVVi)/sqrt2;
            ki[1] = (sVHi + sVVr)/sqrt2;
        } else if (compactMode.equals(dcp)) {
            kr[0] = (sHHr - sVVr + sHVi + sVHi)/2.0;
            ki[0] = (sHHi - sVVi - sHVr - sVHr)/2.0;
            kr[1] = (-sHHi - sVVi + sHVr - sVHr)/2.0;
            ki[1] = (sHHr + sHHi + sHVi - sVHi)/2.0;
        } else if (compactMode.equals(dualHHHV)) {
            kr[0] = sHHr;
            ki[0] = sHHi;
            kr[1] = sHVr;
            ki[1] = sHVi;
        } else if (compactMode.equals(dualVVVH)) {
            kr[0] = sVHr;
            ki[0] = sVHi;
            kr[1] = sVVr;
            ki[1] = sVVi;
        }
    }

    /**
     * Simulate 2x2 compact pol covariance matrix using scatter matrix for a given pixel in a full pol product.
     * @param compactMode Compact polarimetric mode
     * @param scatterRe Real part of the scatter matrix
     * @param scatterIm Imaginary part of the scatter matrix
     * @param Cr Real part of the covariance matrix
     * @param Ci Imaginary part of the covariance matrix
     */
    public static void computeCompactPolCovarianceMatrixC2(
            final String compactMode, final double[][] scatterRe, final double[][] scatterIm,
            final double[][] Cr, final double[][] Ci) {

        final double[] kr = new double[2];
        final double[] ki = new double[2];

        computeCompactPolScatteringVector(compactMode, scatterRe, scatterIm, kr, ki);

        computeCovarianceMatrixC2(kr, ki, Cr, Ci);
    }

    /**
     * Compute 4x1 compact pol Stokes vector for given mean covariance matrix C2.
     * @param Cr Real part of the mean covariance matrix.
     * @param Ci Imaginary part of the mean covariance matrix.
     * @param g Stokes vector.
     */
    public static void computeCompactPolStokesVector(final double[][] Cr, final double[][] Ci, final double[] g) {

        g[0] = Cr[0][0] + Cr[1][1];
        g[1] = Cr[0][0] - Cr[1][1];
        g[2] = 2*Cr[0][1];
        g[3] = -2*Ci[0][1];
    }

    /**
     * Compute 4x1 compact pol Stokes vector for given 2x1 complex scatter vector.
     * @param kr Real part of the scatter vector
     * @param ki Imaginary part of the scatter vector
     * @param g The Stokes vector
     */
    public static void computeCompactPolStokesVector(final double[] kr, final double[] ki, final double[] g) {

        g[0] = kr[0]*kr[0] + ki[0]*ki[0] + kr[1]*kr[1] + ki[1]*ki[1];
        g[1] = kr[0]*kr[0] + ki[0]*ki[0] - kr[1]*kr[1] - ki[1]*ki[1];
        g[2] = 2*(kr[0]*kr[1] + ki[0]*ki[1]);
        g[3] = -2*(ki[0]*kr[0] - kr[0]*ki[1]);
    }

    /**
     * Compute degree of polarization for given 4x1 compact pol Stokes vector.
     * @param g Stokes vector
     * @return degree of polarization
     */
    public static double computeDegreeOfPolarization(final double[] g) {

        if (g[0] == 0.0) {
            return -1;
        }

        return Math.sqrt(g[1]*g[1] + g[2]*g[2] + g[3]*g[3])/g[0];
    }

    /**
     * Compute Stokes parameters for given 4x1 compact pol Stokes vector.
     * @param g Stokes vector
     * @return The Stokes parameters.
     */
    public static StokesParameters computeStokesParameters(final double[] g) {

        StokesParameters parameters = new StokesParameters();

        parameters.DegreeOfPolarization = Math.sqrt(g[1]*g[1] + g[2]*g[2] + g[3]*g[3])/g[0];
        parameters.DegreeOfDepolarization = 1 - parameters.DegreeOfPolarization;
        parameters.DegreeOfCircularity = -g[3]/(g[0]*parameters.DegreeOfPolarization);
        parameters.DegreeOfEllipticity = Math.tan(0.5*Math.asin(parameters.DegreeOfCircularity));
        parameters.circularPolarizationRatio = (g[0] - g[3])/(g[0] + g[3]);
        parameters.linearPolarizationRatio = (g[0] - g[1])/(g[0] + g[1]);
        parameters.RelativePhase = Math.atan(g[3]/g[2]);

        return parameters;
    }

    /**
     * Reconstruct full pol coherency matrix T3 using compact pol data. The random volume over ground
     * (RVOG) model is assumed.
     * @param sourceProductType The compact pol source product type
     * @param idx Pixel index in source product
     * @param dataBuffers Source band data buffers
     * @param Tr Real part of the reconstructed T3 matrix
     * @param Ti Imaginary part of the reconstructed T3 matrix
     */
    public static void reconstructCoherencyMatrixT3(
            final PolBandUtils.MATRIX sourceProductType, final int idx, final ProductData[] dataBuffers,
            final double[][] Tr, final double[][] Ti) {

        final double[][] Cr = new double[2][2];
        final double[][] Ci = new double[2][2];
        final double[] kr = new double[2];
        final double[] ki = new double[2];
        final double[] g = new double[4];

        // todo: should use averaged c2 in computing g?
        if (sourceProductType == PolBandUtils.MATRIX.C2) {

            getCovarianceMatrixC2(idx, dataBuffers, Cr, Ci);
            computeCompactPolStokesVector(Cr, Ci, g);

        } else if (sourceProductType == PolBandUtils.MATRIX.COMPACT) {

            getCompactPolScatterVector(idx, dataBuffers, kr, ki);
            computeCompactPolStokesVector(kr, ki, g);
        }

        if (g[0] <= 0 || g[3] == 0) {
            return;
        }

        final double m = Math.sqrt(g[1]*g[1] + g[2]*g[2] + g[3]*g[3])/g[0];
        final double mv = 0.5*g[0]*(1 - m);
        final double ms = 2.0*g[0]*m;
        // todo: should check compact mode for LH or RH to decide the sign before g[3]
        final double alpha = 0.5*Math.atan(Math.sqrt(g[1]*g[1] + g[2]*g[2])/(-g[3])); // RHC
        final double phi = Math.acos(g[1]/Math.sqrt(g[1]*g[1] + g[2]*g[2]));          // RHC
        //final double alpha = 0.5*Math.atan(Math.sqrt(g[1]*g[1] + g[2]*g[2])/g[3]);  // LHC
        //final double phi = -Math.acos(g[1]/Math.sqrt(g[1]*g[1] + g[2]*g[2]));       // LHC

        final double cosAlpha = Math.cos(alpha);
        final double sinAlpha = Math.sin(alpha);
        final double cosPhi = Math.cos(phi);
        final double sinPhi = Math.sin(phi);

        Tr[0][0] = ms*cosAlpha*cosAlpha + 2.0*mv;
        Ti[0][0] = 0.0;

        Tr[0][1] = ms*cosAlpha*sinAlpha*cosPhi;
        Ti[0][1] = ms*cosAlpha*sinAlpha*sinPhi;

        Tr[0][2] = 0.0;
        Ti[0][2] = 0.0;

        Tr[1][0] = Tr[0][1];
        Ti[1][0] = -Ti[0][1];

        Tr[1][1] = ms*sinAlpha*sinAlpha + mv;
        Ti[1][1] = 0.0;

        Tr[1][2] = 0.0;
        Ti[1][2] = 0.0;

        Tr[2][0] = 0.0;
        Ti[2][0] = 0.0;

        Tr[2][1] = 0.0;
        Ti[2][1] = 0.0;

        Tr[2][2] = mv;
        Ti[2][2] = 0.0;
    }

    public static class StokesParameters {

        public double DegreeOfPolarization;
        public double DegreeOfDepolarization;
        public double DegreeOfCircularity;
        public double DegreeOfEllipticity;
        public double circularPolarizationRatio;
        public double linearPolarizationRatio;
        public double RelativePhase;
    }

}
