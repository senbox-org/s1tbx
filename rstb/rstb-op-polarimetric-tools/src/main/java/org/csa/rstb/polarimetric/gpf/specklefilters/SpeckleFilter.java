/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.csa.rstb.polarimetric.gpf.specklefilters;

import org.csa.rstb.polarimetric.gpf.DualPolOpUtils;
import org.csa.rstb.polarimetric.gpf.PolOpUtils;
import org.esa.s1tbx.io.PolBandUtils;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.gpf.TileIndex;

import java.awt.Rectangle;
import java.util.Map;

/**
 * Interface for Polarimetric Speckle Filters
 */
public interface SpeckleFilter {

    enum T3Elem {
        T11, T12_real, T12_imag, T13_real, T13_imag, T22, T23_real, T23_imag, T33
    }

    void computeTiles(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle,
                             final Rectangle sourceRectangle);

    /**
     * Get the mean value of pixel intensities in a given rectangular region.
     *
     * @param neighborValues The pixel values in the given rectangular region.
     * @return mean The mean value.
     * @throws org.esa.snap.framework.gpf.OperatorException If an error occurs in computation of the mean value.
     */
    default double getMeanValue(final double[] neighborValues) {

        double mean = 0.0;
        for (double neighborValue : neighborValues) {
            mean += neighborValue;
        }
        mean /= neighborValues.length;

        return mean;
    }

    /**
     * Get the variance of pixel intensities in a given rectanglar region.
     *
     * @param neighborValues The pixel values in the given rectanglar region.
     * @param mean           of neighbourhood
     * @return var The variance value.
     * @throws org.esa.snap.framework.gpf.OperatorException If an error occurs in computation of the variance.
     */
    default  double getVarianceValue(final double[] neighborValues, final double mean) {

        double var = 0.0;
        if (neighborValues.length > 1) {

            for (double neighborValue : neighborValues) {
                final double diff = neighborValue - mean;
                var += diff * diff;
            }
            var /= (neighborValues.length - 1);
        }

        return var;
    }

    default double computeMMSEWeight(final double[] dataArray, final double sigmaVSqr) {

        final double meanY = getMeanValue(dataArray);
        final double varY = getVarianceValue(dataArray, meanY);
        if (varY == 0.0) {
            return 0.0;
        }

        double varX = (varY - meanY * meanY * sigmaVSqr) / (1 + sigmaVSqr);
        if (varX < 0.0) {
            varX = 0.0;
        }
        return varX / varY;
    }

    /**
     * Create Span image.
     *
     * @param sourceRectangle The source tile rectangle.
     * @param span            The span image.
     */
    default void createC2SpanImage(final Tile srcTile, final PolBandUtils.MATRIX sourceProductType,
                                   final Rectangle sourceRectangle, final ProductData[] dataBuffers,
                                   final double[][] data11Real, final double[][] data12Real, final double[][] data12Imag,
                                   final double[][] data22Real, final double[][] span) {

        // The pixel value of the span image is given by the trace of the covariance or coherence matrix for the pixel.
        final int sx0 = sourceRectangle.x;
        final int sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;
        final int maxY = sy0 + sh;
        final int maxX = sx0 + sw;

        final TileIndex srcIndex = new TileIndex(srcTile);

        final double[][] Cr = new double[2][2];
        final double[][] Ci = new double[2][2];

        if (sourceProductType == PolBandUtils.MATRIX.LCHCP || sourceProductType == PolBandUtils.MATRIX.RCHCP) {

            final double[] Kr = new double[2];
            final double[] Ki = new double[2];

            for (int y = sy0; y < maxY; ++y) {
                final int j = y - sy0;
                srcIndex.calculateStride(y);
                for (int x = sx0; x < maxX; ++x) {
                    final int i = x - sx0;
                    final int index = srcIndex.getIndex(x);

                    DualPolOpUtils.getCompactPolScatterVector(index, dataBuffers, Kr, Ki);
                    DualPolOpUtils.computeCovarianceMatrixC2(Kr, Ki, Cr, Ci);

                    data11Real[j][i] = Cr[0][0];
                    data12Real[j][i] = Cr[0][1];
                    data12Imag[j][i] = Ci[0][1];
                    data22Real[j][i] = Cr[1][1];
                    span[j][i] = (Cr[0][0] + Cr[1][1]) / 2.0;
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.C2) {

            for (int y = sy0; y < maxY; ++y) {
                final int j = y - sy0;
                srcIndex.calculateStride(y);
                for (int x = sx0; x < maxX; ++x) {
                    final int i = x - sx0;
                    final int index = srcIndex.getIndex(x);

                    DualPolOpUtils.getCovarianceMatrixC2(index, dataBuffers, Cr, Ci);

                    data11Real[j][i] = Cr[0][0];
                    data12Real[j][i] = Cr[0][1];
                    data12Imag[j][i] = Ci[0][1];
                    data22Real[j][i] = Cr[1][1];
                    span[j][i] = (Cr[0][0] + Cr[1][1]) / 2.0;
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.DUAL_HH_HV ||
                sourceProductType == PolBandUtils.MATRIX.DUAL_VH_VV ||
                sourceProductType == PolBandUtils.MATRIX.DUAL_HH_VV) {

            final double[][] Sr = new double[1][2];
            final double[][] Si = new double[1][2];

            for (int y = sy0; y < maxY; ++y) {
                final int j = y - sy0;
                srcIndex.calculateStride(y);
                for (int x = sx0; x < maxX; ++x) {
                    final int i = x - sx0;
                    final int index = srcIndex.getIndex(x);

                    PolOpUtils.getComplexScatterMatrix(index, dataBuffers, Sr, Si);
                    DualPolOpUtils.computeCovarianceMatrixC2(Sr[0], Si[0], Cr, Ci);

                    data11Real[j][i] = Cr[0][0];
                    data12Real[j][i] = Cr[0][1];
                    data12Imag[j][i] = Ci[0][1];
                    data22Real[j][i] = Cr[1][1];
                    span[j][i] = (Cr[0][0] + Cr[1][1]) / 2.0;
                }
            }

        } else {
            throw new OperatorException("Cp or dual pol product is expected.");
        }
    }

    /**
     * Create Span image.
     *
     * @param sourceRectangle The source tile rectangle.
     * @param span            The span image.
     */
    default void createT3SpanImage(final Tile srcTile, final PolBandUtils.MATRIX sourceProductType,
                                          final Rectangle sourceRectangle, final ProductData[] dataBuffers, final double[][] data11Real,
                                          final double[][] data12Real, final double[][] data12Imag, final double[][] data13Real,
                                          final double[][] data13Imag, final double[][] data22Real, final double[][] data23Real,
                                          final double[][] data23Imag, final double[][] data33Real, final double[][] span) {

        // The pixel value of the span image is given by the trace of the covariance or coherence matrix for the pixel.
        final int sx0 = sourceRectangle.x;
        final int sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;
        final int maxY = sy0 + sh;
        final int maxX = sx0 + sw;

        final TileIndex srcIndex = new TileIndex(srcTile);

        final double[][] Mr = new double[3][3];
        final double[][] Mi = new double[3][3];

        if (sourceProductType == PolBandUtils.MATRIX.FULL) {

            final double[][] Sr = new double[2][2];
            final double[][] Si = new double[2][2];

            for (int y = sy0; y < maxY; ++y) {
                final int j = y - sy0;
                srcIndex.calculateStride(y);
                for (int x = sx0; x < maxX; ++x) {
                    final int i = x - sx0;

                    final int index = srcIndex.getIndex(x);
                    PolOpUtils.getComplexScatterMatrix(index, dataBuffers, Sr, Si);
                    PolOpUtils.computeCoherencyMatrixT3(Sr, Si, Mr, Mi);

                    data11Real[j][i] = Mr[0][0];
                    data12Real[j][i] = Mr[0][1];
                    data12Imag[j][i] = Mi[0][1];
                    data13Real[j][i] = Mr[0][2];
                    data13Imag[j][i] = Mi[0][2];
                    data22Real[j][i] = Mr[1][1];
                    data23Real[j][i] = Mr[1][2];
                    data23Imag[j][i] = Mi[1][2];
                    data33Real[j][i] = Mr[2][2];
                    span[j][i] = (Mr[0][0] + Mr[1][1] + Mr[2][2]) / 4.0;
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.T3) {

            for (int y = sy0; y < maxY; ++y) {
                final int j = y - sy0;
                srcIndex.calculateStride(y);
                for (int x = sx0; x < maxX; ++x) {
                    final int i = x - sx0;

                    final int index = srcIndex.getIndex(x);
                    PolOpUtils.getCoherencyMatrixT3(index, dataBuffers, Mr, Mi);

                    data11Real[j][i] = Mr[0][0];
                    data12Real[j][i] = Mr[0][1];
                    data12Imag[j][i] = Mi[0][1];
                    data13Real[j][i] = Mr[0][2];
                    data13Imag[j][i] = Mi[0][2];
                    data22Real[j][i] = Mr[1][1];
                    data23Real[j][i] = Mr[1][2];
                    data23Imag[j][i] = Mi[1][2];
                    data33Real[j][i] = Mr[2][2];
                    span[j][i] = (Mr[0][0] + Mr[1][1] + Mr[2][2]) / 4.0;
                }
            }

        } else if (sourceProductType == PolBandUtils.MATRIX.C3) {

            for (int y = sy0; y < maxY; ++y) {
                final int j = y - sy0;
                srcIndex.calculateStride(y);
                for (int x = sx0; x < maxX; ++x) {
                    final int i = x - sx0;

                    final int index = srcIndex.getIndex(x);
                    PolOpUtils.getCovarianceMatrixC3(index, dataBuffers, Mr, Mi);

                    data11Real[j][i] = Mr[0][0];
                    data12Real[j][i] = Mr[0][1];
                    data12Imag[j][i] = Mi[0][1];
                    data13Real[j][i] = Mr[0][2];
                    data13Imag[j][i] = Mi[0][2];
                    data22Real[j][i] = Mr[1][1];
                    data23Real[j][i] = Mr[1][2];
                    data23Imag[j][i] = Mi[1][2];
                    data33Real[j][i] = Mr[2][2];
                    span[j][i] = (Mr[0][0] + Mr[1][1] + Mr[2][2]) / 4.0;
                }
            }

        } else {
            throw new OperatorException("Polarimetric Matrix not supported");
        }
    }
}
