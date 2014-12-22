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
package org.csa.rstb.gpf.specklefilters;

import org.csa.rstb.gpf.DualPolOpUtils;
import org.csa.rstb.gpf.PolOpUtils;
import org.csa.rstb.gpf.PolarimetricSpeckleFilterOp;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.nest.dataio.PolBandUtils;
import org.esa.snap.gpf.TileIndex;

import java.awt.*;
import java.util.Arrays;
import java.util.Map;

/**
 * Polarimetric Speckle Filter
 */
public class LeeSigma implements SpeckleFilter {

    public static final String SIGMA_50_PERCENT = "0.5";
    public static final String SIGMA_60_PERCENT = "0.6";
    public static final String SIGMA_70_PERCENT = "0.7";
    public static final String SIGMA_80_PERCENT = "0.8";
    public static final String SIGMA_90_PERCENT = "0.9";

    private final PolarimetricSpeckleFilterOp operator;
    private final Product sourceProduct;
    private final Product targetProduct;
    private final PolBandUtils.MATRIX sourceProductType;
    private final PolBandUtils.PolSourceBand[] srcBandList;
    private final int filterSize, halfFilterSize;

    // parameters for Lee sigma filter
    private final double sigmaV;
    private final double sigmaVSqr;

    private int numLooks;
    private double I1, I2; // sigma range
    private int sigma;
    private double sigmaVP; // revised sigmaV used in MMSE filter
    private double sigmaVPSqr;
    private int targetWindowSize = 0;
    private int halfTargetWindowSize = 0;
    private int targetSize = 5;

    public LeeSigma(final PolarimetricSpeckleFilterOp op, final Product srcProduct, final Product trgProduct,
                    PolBandUtils.MATRIX sourceProductType, final PolBandUtils.PolSourceBand[] srcBandList,
                    final int filterSize, final int numLooks, final String sigmaStr, final String targetWindowSizeStr) {
        this.operator = op;
        this.sourceProduct = srcProduct;
        this.targetProduct = trgProduct;
        this.sourceProductType = sourceProductType;
        this.srcBandList = srcBandList;
        this.filterSize = filterSize;
        this.halfFilterSize = filterSize/2;

        this.numLooks = numLooks;

        switch (targetWindowSizeStr) {
            case PolarimetricSpeckleFilterOp.WINDOW_SIZE_3x3:
                targetWindowSize = 3;
                break;
            case PolarimetricSpeckleFilterOp.WINDOW_SIZE_5x5:
                targetWindowSize = 5;
                break;
            default:
                throw new OperatorException("Unknown target window size: " + targetWindowSizeStr);
        }

        halfTargetWindowSize = targetWindowSize / 2;
        sigmaV = 1.0 / Math.sqrt(numLooks);
        sigmaVSqr = sigmaV * sigmaV;

        setSigmaRange(sigmaStr);
    }

    public void computeTiles(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle, final Rectangle sourceRectangle) {
        //System.out.println("LeeSigma.computeTile: sourceProductType = " + sourceProductType);
        if (sourceProductType == PolBandUtils.MATRIX.FULL ||
                sourceProductType == PolBandUtils.MATRIX.C3 ||
                sourceProductType == PolBandUtils.MATRIX.T3) {
            leeSigmaFilter(targetTiles, targetRectangle, sourceRectangle);
        } else if (sourceProductType == PolBandUtils.MATRIX.C2 ||
                sourceProductType == PolBandUtils.MATRIX.DUAL_HH_HV ||
                sourceProductType == PolBandUtils.MATRIX.DUAL_VH_VV ||
                sourceProductType == PolBandUtils.MATRIX.DUAL_HH_VV) {
            leeSigmaFilterC2(targetTiles, targetRectangle, sourceRectangle);
        } else {
            throw new OperatorException("For Lee Sigma filter, only C2, C3 and T3 are supported currently");
        }
    }

    private void setSigmaRange(final String sigmaStr) throws OperatorException {

        switch (sigmaStr) {
            case SIGMA_50_PERCENT:
                sigma = 5;
                break;
            case SIGMA_60_PERCENT:
                sigma = 6;
                break;
            case SIGMA_70_PERCENT:
                sigma = 7;
                break;
            case SIGMA_80_PERCENT:
                sigma = 8;
                break;
            case SIGMA_90_PERCENT:
                sigma = 9;
                break;
            default:
                throw new OperatorException("Unknown sigma: " + sigmaStr);
        }

        if (numLooks == 1) {

            if (sigma == 5) {
                I1 = 0.436;
                I2 = 1.920;
                sigmaVP = 0.4057;
            } else if (sigma == 6) {
                I1 = 0.343;
                I2 = 2.210;
                sigmaVP = 0.4954;
            } else if (sigma == 7) {
                I1 = 0.254;
                I2 = 2.582;
                sigmaVP = 0.5911;
            } else if (sigma == 8) {
                I1 = 0.168;
                I2 = 3.094;
                sigmaVP = 0.6966;
            } else if (sigma == 9) {
                I1 = 0.084;
                I2 = 3.941;
                sigmaVP = 0.8191;
            }

        } else if (numLooks == 2) {

            if (sigma == 5) {
                I1 = 0.582;
                I2 = 1.584;
                sigmaVP = 0.2763;
            } else if (sigma == 6) {
                I1 = 0.501;
                I2 = 1.755;
                sigmaVP = 0.3388;
            } else if (sigma == 7) {
                I1 = 0.418;
                I2 = 1.972;
                sigmaVP = 0.4062;
            } else if (sigma == 8) {
                I1 = 0.327;
                I2 = 2.260;
                sigmaVP = 0.4810;
            } else if (sigma == 9) {
                I1 = 0.221;
                I2 = 2.744;
                sigmaVP = 0.5699;
            }

        } else if (numLooks == 3) {

            if (sigma == 5) {
                I1 = 0.652;
                I2 = 1.458;
                sigmaVP = 0.2222;
            } else if (sigma == 6) {
                I1 = 0.580;
                I2 = 1.586;
                sigmaVP = 0.2736;
            } else if (sigma == 7) {
                I1 = 0.505;
                I2 = 1.751;
                sigmaVP = 0.3280;
            } else if (sigma == 8) {
                I1 = 0.419;
                I2 = 1.965;
                sigmaVP = 0.3892;
            } else if (sigma == 9) {
                I1 = 0.313;
                I2 = 2.320;
                sigmaVP = 0.4624;
            }

        } else if (numLooks == 4) {

            if (sigma == 5) {
                I1 = 0.694;
                I2 = 1.385;
                sigmaVP = 0.1921;
            } else if (sigma == 6) {
                I1 = 0.630;
                I2 = 1.495;
                sigmaVP = 0.2348;
            } else if (sigma == 7) {
                I1 = 0.560;
                I2 = 1.627;
                sigmaVP = 0.2825;
            } else if (sigma == 8) {
                I1 = 0.480;
                I2 = 1.804;
                sigmaVP = 0.3354;
            } else if (sigma == 9) {
                I1 = 0.378;
                I2 = 2.094;
                sigmaVP = 0.3991;
            }
        }

        sigmaVPSqr = sigmaVP * sigmaVP;
    }

    private void leeSigmaFilterC2(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle,
                                  final Rectangle sourceRectangle) {

        final int x0 = targetRectangle.x, y0 = targetRectangle.y;
        final int w = targetRectangle.width,  h = targetRectangle.height;
        final int maxY = y0 + h, maxX = x0 + w;
        // System.out.println("refinedLee x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final int sx0 = sourceRectangle.x, sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width,  sh = sourceRectangle.height;

        final TileIndex trgIndex = new TileIndex(targetTiles.get(targetProduct.getBandAt(0)));

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {

            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];

            for (int i = 0; i < bandList.srcBands.length; ++i) {
                sourceTiles[i] = operator.getSourceTile(bandList.srcBands[i], sourceRectangle);
                dataBuffers[i] = sourceTiles[i].getDataBuffer();
            }
            final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

            final ProductData[] targetDataBuffers = new ProductData[4];
            for (final Band targetBand : bandList.targetBands) {
                final String targetBandName = targetBand.getName();
                final ProductData dataBuffer = targetTiles.get(targetBand).getDataBuffer();

                if (targetBandName.contains("C11")) {
                    targetDataBuffers[0] = dataBuffer;
                } else if (targetBandName.contains("C12_real")) {
                    targetDataBuffers[1] = dataBuffer;
                } else if (targetBandName.contains("C12_imag")) {
                    targetDataBuffers[2] = dataBuffer;
                } else if (targetBandName.contains("C22")) {
                    targetDataBuffers[3] = dataBuffer;
                }
            }

            Z98 z98 = new Z98();
            computeZ98ValuesC2(sourceTiles[0], sourceRectangle, dataBuffers, z98);

            double[][] Cr = new double[2][2];
            double[][] Ci = new double[2][2];

            int xx, yy, trgIdx, srcIdx;
            boolean[][] isPointTarget = new boolean[h][w];
            C2[][] filterWindowC2 = null;
            C2[][] targetWindowC2 = null;

            for (int y = y0; y < maxY; ++y) {
                yy = y - y0;
                trgIndex.calculateStride(y);
                srcIndex.calculateStride(y);

                for (int x = x0; x < maxX; ++x) {
                    xx = x - x0;
                    trgIdx = trgIndex.getIndex(x);
                    srcIdx = srcIndex.getIndex(x);

                    DualPolOpUtils.getCovarianceMatrixC2(srcIdx, sourceProductType, dataBuffers, Cr, Ci);

                    if (isPointTarget[yy][xx]) {
                        saveC2(Cr, Ci, trgIdx, targetDataBuffers);
                        continue;
                    }

                    if (y - halfFilterSize < sy0 || y + halfFilterSize > sy0 + sh - 1 ||
                            x - halfFilterSize < sx0 || x + halfFilterSize > sx0 + sw - 1) {

                        filterWindowC2 = new C2[filterSize][filterSize];
                        getWindowPixelC2s(x, y, dataBuffers, sx0, sy0, sw, sh, sourceTiles[0], filterWindowC2);
                        final int n = setPixelsInSigmaRange(filterWindowC2);
                        computeFilteredC2(filterWindowC2, n, sigmaVSqr, Cr, Ci);
                        saveC2(Cr, Ci, trgIdx, targetDataBuffers);
                        continue;
                    }

                    targetWindowC2 = new C2[targetWindowSize][targetWindowSize];
                    getWindowPixelC2s(x, y, dataBuffers, sx0, sy0, sw, sh, sourceTiles[0], targetWindowC2);

                    if (checkPointTarget(z98, targetWindowC2, isPointTarget, x0, y0, w, h)) {
                        saveC2(Cr, Ci, trgIdx, targetDataBuffers);
                        continue;
                    }

                    double[] sigmaRangeC11 = new double[2];
                    double[] sigmaRangeC22 = new double[2];
                    computeSigmaRange(targetWindowC2, 0, sigmaRangeC11);
                    computeSigmaRange(targetWindowC2, 1, sigmaRangeC22);

                    filterWindowC2 = new C2[filterSize][filterSize];
                    getWindowPixelC2s(x, y, dataBuffers, sx0, sy0, sw, sh, sourceTiles[0], filterWindowC2);

                    final int n = selectPixelsInSigmaRange(sigmaRangeC11, sigmaRangeC22, filterWindowC2);
                    if (n == 0) {
                        saveC2(Cr, Ci, trgIdx, targetDataBuffers);
                        continue;
                    }

                    computeFilteredC2(filterWindowC2, n, sigmaVPSqr, Cr, Ci);
                    saveC2(Cr, Ci, trgIdx, targetDataBuffers);
                }
            }
        }
    }

    private void computeSigmaRange(C2[][] targetWindowC2, final int elemIdx, double[] sigmaRange) {

        final double[] data = new double[targetWindowSize * targetWindowSize];
        int k = 0;
        double mean = 0.0;
        for (int j = 0; j < targetWindowSize; j++) {
            for (int i = 0; i < targetWindowSize; i++) {
                data[k] = targetWindowC2[j][i].Cr[elemIdx][elemIdx];
                mean += data[k];
                k++;
            }
        }
        mean /= k;

        final double b = computeMMSEWeight(data, sigmaVSqr);
        final double filtered = mean + b * (data[k / 2] - mean);

        sigmaRange[0] = filtered * I1;
        sigmaRange[1] = filtered * I2;
    }

    private int setPixelsInSigmaRange(final C2[][] filterWindowC2) {
        int n = 0;
        for (int j = 0; j < filterSize; j++) {
            for (int i = 0; i < filterSize; i++) {
                if (filterWindowC2[j][i] != null) {
                    filterWindowC2[j][i].inSigmaRange = true;
                    n++;
                }
            }
        }
        return n;
    }

    private boolean checkPointTarget(final Z98 z98, final C2[][] targetWindowC2, boolean[][] isPointTarget,
                                     final int x0, final int y0, final int w, final int h) {

        if (targetWindowC2[halfTargetWindowSize][halfTargetWindowSize].Cr[0][0] > z98.c11) {
            if (getClusterSize(z98.c11, targetWindowC2, 0) > targetSize) {
                markClusterPixels(isPointTarget, z98.c11, targetWindowC2, x0, y0, w, h, 0);
                return true;
            }
        }

        if (targetWindowC2[halfTargetWindowSize][halfTargetWindowSize].Cr[1][1] > z98.c22) {
            if (getClusterSize(z98.c22, targetWindowC2, 1) > targetSize) {
                markClusterPixels(isPointTarget, z98.c22, targetWindowC2, x0, y0, w, h, 1);
                return true;
            }
        }

        return false;
    }

    private int getClusterSize(final double threshold, final C2[][] targetWindowC2, final int elemIdx) {

        int clusterSize = 0;
        for (int j = 0; j < targetWindowSize; j++) {
            for (int i = 0; i < targetWindowSize; i++) {
                if (targetWindowC2[j][i].Cr[elemIdx][elemIdx] > threshold) {
                    clusterSize++;
                }
            }
        }
        return clusterSize;
    }

    private void markClusterPixels(
            boolean[][] isPointTarget, final double threshold, final C2[][] targetWindowC2,
            final int x0, final int y0, final int w, final int h, final int elemIdx) {

        for (int j = 0; j < targetWindowSize; j++) {
            for (int i = 0; i < targetWindowSize; i++) {
                if (targetWindowC2[j][i].Cr[elemIdx][elemIdx] > threshold &&
                        targetWindowC2[j][i].y >= y0 && targetWindowC2[j][i].y < y0 + h &&
                        targetWindowC2[j][i].x >= x0 && targetWindowC2[j][i].x < x0 + w) {

                    isPointTarget[targetWindowC2[j][i].y - y0][targetWindowC2[j][i].x - x0] = true;
                }
            }
        }
    }

    private int selectPixelsInSigmaRange(final double[] sigmaRangeC11, final double[] sigmaRangeC22,
                                         C2[][] filterWindowC2) {

        int numPixelsInSigmaRange = 0;
        for (int j = 0; j < filterSize; j++) {
            for (int i = 0; i < filterSize; i++) {
                if (filterWindowC2[j][i] != null &&
                        filterWindowC2[j][i].Cr[0][0] >= sigmaRangeC11[0] &&
                        filterWindowC2[j][i].Cr[0][0] <= sigmaRangeC11[1] &&
                        filterWindowC2[j][i].Cr[1][1] >= sigmaRangeC22[0] &&
                        filterWindowC2[j][i].Cr[1][1] <= sigmaRangeC22[1]) {

                    filterWindowC2[j][i].inSigmaRange = true;
                    numPixelsInSigmaRange++;
                }
            }
        }
        return numPixelsInSigmaRange;
    }

    private static void saveC2(final double[][] Cr, final double[][] Ci,
                               final int idx, final ProductData[] targetDataBuffers) {

        targetDataBuffers[0].setElemFloatAt(idx, (float) Cr[0][0]); // C11
        targetDataBuffers[1].setElemFloatAt(idx, (float) Cr[0][1]); // C12_real
        targetDataBuffers[2].setElemFloatAt(idx, (float) Ci[0][1]); // C12_imag
        targetDataBuffers[3].setElemFloatAt(idx, (float) Cr[1][1]); // C22
    }

    private void getWindowPixelC2s(final int x, final int y, final ProductData[] sourceDataBuffers,
                                   final int sx0, final int sy0, final int sw, final int sh,
                                   final Tile sourceTile, C2[][] windowPixelC2) {

        final TileIndex srcIndex = new TileIndex(sourceTile);
        final int windowSize = windowPixelC2.length;
        final int halfWindowSize = windowSize / 2;

        final double[][] Cr = new double[2][2];
        final double[][] Ci = new double[2][2];

        int yy, xx;
        for (int j = 0; j < windowSize; j++) {
            yy = y - halfWindowSize + j;
            srcIndex.calculateStride(yy);
            for (int i = 0; i < windowSize; i++) {
                xx = x - halfWindowSize + i;
                if (yy >= sy0 && yy <= sy0 + sh - 1 && xx >= sx0 && xx <= sx0 + sw - 1) {
                    final int srcIdx = srcIndex.getIndex(xx);
                    DualPolOpUtils.getCovarianceMatrixC2(srcIdx, sourceProductType, sourceDataBuffers, Cr, Ci);
                    windowPixelC2[j][i] = new C2(xx, yy, Cr, Ci);
                }
            }
        }
    }

    private void computeFilteredC2(final C2[][] filterWindowC2, final int n, final double sigmaVSqr,
                                   double[][] Cr, double[][] Ci) {

        double[] span = new double[n];
        getSpan(filterWindowC2, span);
        final double b = computeMMSEWeight(span, sigmaVSqr);
        filterC2(filterWindowC2, b, n, Cr, Ci);
    }

    private void getSpan(final C2[][] filterWindowC2, double[] span) {

        int k = 0;
        for (int j = 0; j < filterSize; j++) {
            for (int i = 0; i < filterSize; i++) {
                if (filterWindowC2[j][i] != null && filterWindowC2[j][i].inSigmaRange) {
                    span[k++] = filterWindowC2[j][i].Cr[0][0] + filterWindowC2[j][i].Cr[1][1];
                }
            }
        }
    }

    private void filterC2(final C2[][] filterWindowC2, final double b, final int numPixelsInSigmaRange,
                          double[][] filteredTr, double[][] filteredTi) {

        final double[][] meanCr = new double[2][2];
        final double[][] meanCi = new double[2][2];

        for (int j = 0; j < filterSize; j++) {
            for (int i = 0; i < filterSize; i++) {
                if (filterWindowC2[j][i] != null && filterWindowC2[j][i].inSigmaRange) {

                    for (int m = 0; m < 2; m++) {
                        for (int n = 0; n < 2; n++) {
                            meanCr[m][n] += filterWindowC2[j][i].Cr[m][n];
                            meanCi[m][n] += filterWindowC2[j][i].Ci[m][n];
                        }
                    }
                }
            }
        }

        for (int m = 0; m < 2; m++) {
            for (int n = 0; n < 2; n++) {
                meanCr[m][n] /= numPixelsInSigmaRange;
                meanCi[m][n] /= numPixelsInSigmaRange;
            }
        }

        for (int m = 0; m < 2; m++) {
            for (int n = 0; n < 2; n++) {
                filteredTr[m][n] = (1 - b) * meanCr[m][n] + b * filterWindowC2[halfFilterSize][halfFilterSize].Cr[m][n];
                filteredTi[m][n] = (1 - b) * meanCi[m][n] + b * filterWindowC2[halfFilterSize][halfFilterSize].Ci[m][n];
            }
        }
    }

    public final static class C2 {
        public int x = -1;
        public int y = -1;
        public final double[][] Cr = new double[2][2];
        public final double[][] Ci = new double[2][2];
        public boolean inSigmaRange = false;

        public C2(final int x, final int y, final double[][] Cr, final double[][] Ci) {
            this.x = x;
            this.y = y;
            for (int a = 0; a < Cr.length; a++) {
                System.arraycopy(Cr[a], 0, this.Cr[a], 0, Cr[a].length);
                System.arraycopy(Ci[a], 0, this.Ci[a], 0, Ci[a].length);
            }
        }

        public C2() {
        }
    }

    /**
     * Filter the given tile of image with Improved Lee Sigma filter.
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @param sourceRectangle The area in the source product
     */
    private void leeSigmaFilter(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle,
                                final Rectangle sourceRectangle) {
        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        // System.out.println("refinedLee x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final int sx0 = sourceRectangle.x;
        final int sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;

        final TileIndex trgIndex = new TileIndex(targetTiles.get(targetProduct.getBandAt(0)));
        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {

            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] sourceDataBuffers = new ProductData[bandList.srcBands.length];

            for (int i = 0; i < bandList.srcBands.length; ++i) {
                sourceTiles[i] = operator.getSourceTile(bandList.srcBands[i], sourceRectangle);
                sourceDataBuffers[i] = sourceTiles[i].getDataBuffer();
            }
            final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

            final ProductData[] targetDataBuffers = new ProductData[9];
            for (final Band targetBand : bandList.targetBands) {
                final String targetBandName = targetBand.getName();
                final ProductData dataBuffer = targetTiles.get(targetBand).getDataBuffer();
                if (PolBandUtils.isBandForMatrixElement(targetBandName, "11"))
                    targetDataBuffers[0] = dataBuffer;
                else if (PolBandUtils.isBandForMatrixElement(targetBandName, "12_real"))
                    targetDataBuffers[1] = dataBuffer;
                else if (PolBandUtils.isBandForMatrixElement(targetBandName, "12_imag"))
                    targetDataBuffers[2] = dataBuffer;
                else if (PolBandUtils.isBandForMatrixElement(targetBandName, "13_real"))
                    targetDataBuffers[3] = dataBuffer;
                else if (PolBandUtils.isBandForMatrixElement(targetBandName, "13_imag"))
                    targetDataBuffers[4] = dataBuffer;
                else if (PolBandUtils.isBandForMatrixElement(targetBandName, "22"))
                    targetDataBuffers[5] = dataBuffer;
                else if (PolBandUtils.isBandForMatrixElement(targetBandName, "23_real"))
                    targetDataBuffers[6] = dataBuffer;
                else if (PolBandUtils.isBandForMatrixElement(targetBandName, "23_imag"))
                    targetDataBuffers[7] = dataBuffer;
                else if (PolBandUtils.isBandForMatrixElement(targetBandName, "33"))
                    targetDataBuffers[8] = dataBuffer;
            }

            Z98 z98 = new Z98();
            computeZ98Values(sourceTiles[0], sourceRectangle, sourceDataBuffers, z98);

            final double[][] Tr = new double[3][3];
            final double[][] Ti = new double[3][3];

            int xx, yy, trgIdx, srcIdx;
            final boolean[][] isPointTarget = new boolean[h][w];
            T3[][] filterWindowT3 = null;
            T3[][] targetWindowT3 = null;

            for (int y = y0; y < maxY; ++y) {
                yy = y - y0;
                trgIndex.calculateStride(y);
                srcIndex.calculateStride(y);

                for (int x = x0; x < maxX; ++x) {
                    xx = x - x0;
                    trgIdx = trgIndex.getIndex(x);
                    srcIdx = srcIndex.getIndex(x);

                    PolOpUtils.getT3(srcIdx, sourceProductType, sourceDataBuffers, Tr, Ti);

                    if (isPointTarget[yy][xx]) {
                        saveT3(Tr, Ti, trgIdx, targetDataBuffers);
                        continue;
                    }

                    if (y - halfFilterSize < sy0 || y + halfFilterSize > sy0 + sh - 1 ||
                            x - halfFilterSize < sx0 || x + halfFilterSize > sx0 + sw - 1) {

                        filterWindowT3 = new T3[filterSize][filterSize];
                        getWindowPixelT3s(x, y, sourceDataBuffers, sx0, sy0, sw, sh, sourceTiles[0], filterWindowT3);
                        final int n = setPixelsInSigmaRange(filterWindowT3);
                        computeFilteredT3(filterWindowT3, n, sigmaVSqr, Tr, Ti);
                        saveT3(Tr, Ti, trgIdx, targetDataBuffers);
                        continue;
                    }

                    targetWindowT3 = new T3[targetWindowSize][targetWindowSize];
                    getWindowPixelT3s(x, y, sourceDataBuffers, sx0, sy0, sw, sh, sourceTiles[0], targetWindowT3);

                    if (checkPointTarget(z98, targetWindowT3, isPointTarget, x0, y0, w, h)) {
                        saveT3(Tr, Ti, trgIdx, targetDataBuffers);
                        continue;
                    }

                    double[] sigmaRangeT11 = new double[2];
                    double[] sigmaRangeT22 = new double[2];
                    double[] sigmaRangeT33 = new double[2];
                    computeSigmaRange(targetWindowT3, 0, sigmaRangeT11);
                    computeSigmaRange(targetWindowT3, 1, sigmaRangeT22);
                    computeSigmaRange(targetWindowT3, 2, sigmaRangeT33);

                    filterWindowT3 = new T3[filterSize][filterSize];
                    getWindowPixelT3s(x, y, sourceDataBuffers, sx0, sy0, sw, sh, sourceTiles[0], filterWindowT3);

                    final int n = selectPixelsInSigmaRange(sigmaRangeT11, sigmaRangeT22, sigmaRangeT33, filterWindowT3);
                    if (n == 0) {
                        saveT3(Tr, Ti, trgIdx, targetDataBuffers);
                        continue;
                    }

                    computeFilteredT3(filterWindowT3, n, sigmaVPSqr, Tr, Ti);
                    saveT3(Tr, Ti, trgIdx, targetDataBuffers);
                }
            }
        }
    }

    private void computeZ98ValuesC2(final Tile sourceTile, final Rectangle sourceRectangle,
                                  final ProductData[] sourceDataBuffers, Z98 z98) {

        final TileIndex srcIndex = new TileIndex(sourceTile);
        final int sx0 = sourceRectangle.x;
        final int sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;
        final int maxY = sy0 + sh;
        final int maxX = sx0 + sw;
        final int z98Index = (int) (sw * sh * 0.98) - 1;

        double[] c11 = new double[sw * sh];
        double[] c22 = new double[sw * sh];

        final double[][] Cr = new double[2][2];
        final double[][] Ci = new double[2][2];

        int k = 0;
        for (int y = sy0; y < maxY; y++) {
            srcIndex.calculateStride(y);
            for (int x = sx0; x < maxX; x++) {
                final int index = srcIndex.getIndex(x);
                DualPolOpUtils.getCovarianceMatrixC2(index, sourceProductType, sourceDataBuffers, Cr, Ci);
                c11[k] = Cr[0][0];
                c22[k] = Cr[1][1];
                k++;
            }
        }

        Arrays.sort(c11);
        Arrays.sort(c22);

        z98.c11 = c11[z98Index];
        z98.c22 = c22[z98Index];
    }

    private void computeZ98Values(final Tile sourceTile, final Rectangle sourceRectangle,
                                  final ProductData[] sourceDataBuffers, Z98 z98) {

        final TileIndex srcIndex = new TileIndex(sourceTile);
        final int sx0 = sourceRectangle.x;
        final int sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;
        final int maxY = sy0 + sh;
        final int maxX = sx0 + sw;
        final int z98Index = (int) (sw * sh * 0.98) - 1;

        double[] t11 = new double[sw * sh];
        double[] t22 = new double[sw * sh];
        double[] t33 = new double[sw * sh];

        final double[][] Tr = new double[3][3];
        final double[][] Ti = new double[3][3];

        int k = 0;
        for (int y = sy0; y < maxY; y++) {
            srcIndex.calculateStride(y);
            for (int x = sx0; x < maxX; x++) {
                final int index = srcIndex.getIndex(x);
                PolOpUtils.getT3(index, sourceProductType, sourceDataBuffers, Tr, Ti);
                t11[k] = Tr[0][0];
                t22[k] = Tr[1][1];
                t33[k] = Tr[2][2];
                k++;
            }
        }

        Arrays.sort(t11);
        Arrays.sort(t22);
        Arrays.sort(t33);

        z98.t11 = t11[z98Index];
        z98.t22 = t22[z98Index];
        z98.t33 = t33[z98Index];
    }

    private static void saveT3(final double[][] Tr, final double[][] Ti,
                               final int idx, final ProductData[] targetDataBuffers) {

        targetDataBuffers[0].setElemFloatAt(idx, (float) Tr[0][0]); // T11
        targetDataBuffers[1].setElemFloatAt(idx, (float) Tr[0][1]); // T12_real
        targetDataBuffers[2].setElemFloatAt(idx, (float) Ti[0][1]); // T12_imag
        targetDataBuffers[3].setElemFloatAt(idx, (float) Tr[0][2]); // T13_real
        targetDataBuffers[4].setElemFloatAt(idx, (float) Ti[0][2]); // T13_imag
        targetDataBuffers[5].setElemFloatAt(idx, (float) Tr[1][1]); // T22
        targetDataBuffers[6].setElemFloatAt(idx, (float) Tr[1][2]); // T23_real
        targetDataBuffers[7].setElemFloatAt(idx, (float) Ti[1][2]); // T23_imag
        targetDataBuffers[8].setElemFloatAt(idx, (float) Tr[2][2]); // T33
    }

    private void getWindowPixelT3s(final int x, final int y, final ProductData[] sourceDataBuffers,
                                   final int sx0, final int sy0, final int sw, final int sh,
                                   final Tile sourceTile, T3[][] windowPixelT3) {

        final TileIndex srcIndex = new TileIndex(sourceTile);
        final int windowSize = windowPixelT3.length;
        final int halfWindowSize = windowSize / 2;

        final double[][] Tr = new double[3][3];
        final double[][] Ti = new double[3][3];

        int yy, xx;
        for (int j = 0; j < windowSize; j++) {
            yy = y - halfWindowSize + j;
            srcIndex.calculateStride(yy);
            for (int i = 0; i < windowSize; i++) {
                xx = x - halfWindowSize + i;
                if (yy >= sy0 && yy <= sy0 + sh - 1 && xx >= sx0 && xx <= sx0 + sw - 1) {
                    final int srcIdx = srcIndex.getIndex(xx);
                    PolOpUtils.getT3(srcIdx, sourceProductType, sourceDataBuffers, Tr, Ti);
                    windowPixelT3[j][i] = new T3(xx, yy, Tr, Ti);
                }
            }
        }
    }

    private boolean checkPointTarget(final Z98 z98, final T3[][] targetWindowT3, boolean[][] isPointTarget,
                                     final int x0, final int y0, final int w, final int h) {

        if (targetWindowT3[halfTargetWindowSize][halfTargetWindowSize].Tr[0][0] > z98.t11) {
            if (getClusterSize(z98.t11, targetWindowT3, 0) > targetSize) {
                markClusterPixels(isPointTarget, z98.t11, targetWindowT3, x0, y0, w, h, 0);
                return true;
            }
        }

        if (targetWindowT3[halfTargetWindowSize][halfTargetWindowSize].Tr[1][1] > z98.t22) {
            if (getClusterSize(z98.t22, targetWindowT3, 1) > targetSize) {
                markClusterPixels(isPointTarget, z98.t22, targetWindowT3, x0, y0, w, h, 1);
                return true;
            }
        }

        if (targetWindowT3[halfTargetWindowSize][halfTargetWindowSize].Tr[2][2] > z98.t33) {
            if (getClusterSize(z98.t33, targetWindowT3, 2) > targetSize) {
                markClusterPixels(isPointTarget, z98.t33, targetWindowT3, x0, y0, w, h, 2);
                return true;
            }
        }

        return false;
    }

    private int getClusterSize(final double threshold, final T3[][] targetWindowT3, final int elemIdx) {

        int clusterSize = 0;
        for (int j = 0; j < targetWindowSize; j++) {
            for (int i = 0; i < targetWindowSize; i++) {
                if (targetWindowT3[j][i].Tr[elemIdx][elemIdx] > threshold) {
                    clusterSize++;
                }
            }
        }
        return clusterSize;
    }

    private void markClusterPixels(
            boolean[][] isPointTarget, final double threshold, final T3[][] targetWindowT3,
            final int x0, final int y0, final int w, final int h, final int elemIdx) {

        for (int j = 0; j < targetWindowSize; j++) {
            for (int i = 0; i < targetWindowSize; i++) {
                if (targetWindowT3[j][i].Tr[elemIdx][elemIdx] > threshold &&
                        targetWindowT3[j][i].y >= y0 && targetWindowT3[j][i].y < y0 + h &&
                        targetWindowT3[j][i].x >= x0 && targetWindowT3[j][i].x < x0 + w) {

                    isPointTarget[targetWindowT3[j][i].y - y0][targetWindowT3[j][i].x - x0] = true;
                }
            }
        }
    }

    private void computeSigmaRange(T3[][] targetWindowT3, final int elemIdx, double[] sigmaRange) {

        final double[] data = new double[targetWindowSize * targetWindowSize];
        int k = 0;
        double mean = 0.0;
        for (int j = 0; j < targetWindowSize; j++) {
            for (int i = 0; i < targetWindowSize; i++) {
                data[k] = targetWindowT3[j][i].Tr[elemIdx][elemIdx];
                mean += data[k];
                k++;
            }
        }
        mean /= k;

        final double b = computeMMSEWeight(data, sigmaVSqr);
        final double filtered = mean + b * (data[k / 2] - mean);

        sigmaRange[0] = filtered * I1;
        sigmaRange[1] = filtered * I2;
    }

    private int setPixelsInSigmaRange(final T3[][] filterWindowT3) {
        int n = 0;
        for (int j = 0; j < filterSize; j++) {
            for (int i = 0; i < filterSize; i++) {
                if (filterWindowT3[j][i] != null) {
                    filterWindowT3[j][i].inSigmaRange = true;
                    n++;
                }
            }
        }
        return n;
    }

    private int selectPixelsInSigmaRange(final double[] sigmaRangeT11, final double[] sigmaRangeT22,
                                         final double[] sigmaRangeT33, T3[][] filterWindowT3) {

        int numPixelsInSigmaRange = 0;
        for (int j = 0; j < filterSize; j++) {
            for (int i = 0; i < filterSize; i++) {
                if (filterWindowT3[j][i] != null &&
                        filterWindowT3[j][i].Tr[0][0] >= sigmaRangeT11[0] &&
                        filterWindowT3[j][i].Tr[0][0] <= sigmaRangeT11[1] &&
                        filterWindowT3[j][i].Tr[1][1] >= sigmaRangeT22[0] &&
                        filterWindowT3[j][i].Tr[1][1] <= sigmaRangeT22[1] &&
                        filterWindowT3[j][i].Tr[2][2] >= sigmaRangeT33[0] &&
                        filterWindowT3[j][i].Tr[2][2] <= sigmaRangeT33[1]) {

                    filterWindowT3[j][i].inSigmaRange = true;
                    numPixelsInSigmaRange++;
                }
            }
        }
        return numPixelsInSigmaRange;
    }

    private void computeFilteredT3(final T3[][] filterWindowT3, final int n, final double sigmaVSqr,
                                   double[][] Tr, double[][] Ti) {

        double[] span = new double[n];
        getSpan(filterWindowT3, span);
        final double b = computeMMSEWeight(span, sigmaVSqr);
        filterT3(filterWindowT3, b, n, Tr, Ti);
    }

    private void getSpan(final T3[][] filterWindowT3, double[] span) {

        int k = 0;
        for (int j = 0; j < filterSize; j++) {
            for (int i = 0; i < filterSize; i++) {
                if (filterWindowT3[j][i] != null && filterWindowT3[j][i].inSigmaRange) {
                    span[k++] = filterWindowT3[j][i].Tr[0][0] +
                            filterWindowT3[j][i].Tr[1][1] +
                            filterWindowT3[j][i].Tr[2][2];
                }
            }
        }
    }

    private void filterT3(final T3[][] filterWindowT3, final double b, final int numPixelsInSigmaRange,
                          double[][] filteredTr, double[][] filteredTi) {

        final double[][] meanTr = new double[3][3];
        final double[][] meanTi = new double[3][3];

        for (int j = 0; j < filterSize; j++) {
            for (int i = 0; i < filterSize; i++) {
                if (filterWindowT3[j][i] != null && filterWindowT3[j][i].inSigmaRange) {

                    for (int m = 0; m < 3; m++) {
                        for (int n = 0; n < 3; n++) {
                            meanTr[m][n] += filterWindowT3[j][i].Tr[m][n];
                            meanTi[m][n] += filterWindowT3[j][i].Ti[m][n];
                        }
                    }
                }
            }
        }

        for (int m = 0; m < 3; m++) {
            for (int n = 0; n < 3; n++) {
                meanTr[m][n] /= numPixelsInSigmaRange;
                meanTi[m][n] /= numPixelsInSigmaRange;
            }
        }

        for (int m = 0; m < 3; m++) {
            for (int n = 0; n < 3; n++) {
                filteredTr[m][n] = (1 - b) * meanTr[m][n] + b * filterWindowT3[halfFilterSize][halfFilterSize].Tr[m][n];
                filteredTi[m][n] = (1 - b) * meanTi[m][n] + b * filterWindowT3[halfFilterSize][halfFilterSize].Ti[m][n];
            }
        }
    }


    public final static class Z98 {
        public double c11;
        public double c22;

        public double t11;
        public double t22;
        public double t33;
    }

    public final static class T3 {
        public int x = -1;
        public int y = -1;
        public final double[][] Tr = new double[3][3];
        public final double[][] Ti = new double[3][3];
        public boolean inSigmaRange = false;

        public T3(final int x, final int y, final double[][] Tr, final double[][] Ti) {
            this.x = x;
            this.y = y;
            for (int a = 0; a < Tr.length; a++) {
                System.arraycopy(Tr[a], 0, this.Tr[a], 0, Tr[a].length);
                System.arraycopy(Ti[a], 0, this.Ti[a], 0, Ti[a].length);
            }
        }

        public T3() {
        }
    }
}
