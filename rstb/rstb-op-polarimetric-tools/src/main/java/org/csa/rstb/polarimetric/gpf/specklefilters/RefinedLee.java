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

import org.csa.rstb.polarimetric.gpf.PolarimetricSpeckleFilterOp;
import org.esa.s1tbx.dataio.PolBandUtils;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.gpf.TileIndex;

import java.awt.Rectangle;
import java.util.Map;

/**
 * Polarimetric Speckle Filter
 */
public class RefinedLee implements SpeckleFilter {

    private final PolarimetricSpeckleFilterOp operator;
    private final Product sourceProduct;
    private final Product targetProduct;
    private final PolBandUtils.MATRIX sourceProductType;
    private final PolBandUtils.PolSourceBand[] srcBandList;

    private final int filterSize, halfFilterSize;
    private final int convSize;
    private final int stride;
    private final int subWindowSize;
    private final double sigmaV;
    private final double sigmaVSqr;

    private static final double NonValidPixelValue = -1.0;

    public RefinedLee(final PolarimetricSpeckleFilterOp op, final Product srcProduct, final Product trgProduct,
                      PolBandUtils.MATRIX sourceProductType, final PolBandUtils.PolSourceBand[] srcBandList,
                      final int filterSize, final int numLooks) {
        this.operator = op;
        this.sourceProduct = srcProduct;
        this.targetProduct = trgProduct;
        this.sourceProductType = sourceProductType;
        this.srcBandList = srcBandList;

        this.filterSize = filterSize;
        this.halfFilterSize = filterSize/2;

        switch (filterSize) {
            case 5:
                subWindowSize = 3;
                stride = 1;
                break;
            case 7:
                subWindowSize = 3;
                stride = 2;
                break;
            case 9:
                subWindowSize = 5;
                stride = 2;
                break;
            case 11:
                subWindowSize = 5;
                stride = 3;
                break;
            default:
                throw new OperatorException("Unknown window size: " + filterSize);
        }

        convSize = filterSize * (halfFilterSize + 1);
        sigmaV = 1.0 / Math.sqrt(numLooks);
        sigmaVSqr = sigmaV * sigmaV;
    }

    public void computeTiles(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle, final Rectangle sourceRectangle) {
        if (PolBandUtils.isFullPol(sourceProductType)) {
            refinedLeeFilterFullPol(targetTiles, targetRectangle, sourceRectangle);
        } else if (PolBandUtils.isQuadPol(sourceProductType)) {
            refinedLeeFilterC3T3C4T4(targetTiles, targetRectangle, sourceRectangle);
        } else if (PolBandUtils.isDualPol(sourceProductType)) {
            refinedLeeFilterC2(targetTiles, targetRectangle, sourceRectangle);
        } else {
            throw new OperatorException("For Refined Lee filtering, only C2, C3, T3, C4 and T4 are supported");
        }
    }

    /**
     * Filter compact data for the given tile with refined Lee filter.
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @param sourceRectangle The area in the source product
     */
    private void refinedLeeFilterC2(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle,
                                    final Rectangle sourceRectangle) {

        final int x0 = targetRectangle.x, y0 = targetRectangle.y;
        final int w = targetRectangle.width,  h = targetRectangle.height;
        final int maxY = y0 + h, maxX = x0 + w;
        //System.out.println("refinedLee x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;

        final double[][] data11Real = new double[sh][sw];
        final double[][] data12Real = new double[sh][sw];
        final double[][] data12Imag = new double[sh][sw];
        final double[][] data22Real = new double[sh][sw];
        final double[][] span = new double[sh][sw];

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {

            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
            for (int i = 0; i < bandList.srcBands.length; ++i) {
                sourceTiles[i] = operator.getSourceTile(bandList.srcBands[i], sourceRectangle);
                dataBuffers[i] = sourceTiles[i].getDataBuffer();
            }

            final Tile srcTile = operator.getSourceTile(bandList.srcBands[0], sourceRectangle);
            createC2SpanImage(srcTile, sourceProductType, sourceRectangle, dataBuffers,
                    data11Real, data12Real, data12Imag, data22Real, span);

            for (Band targetBand : bandList.targetBands) {

                final Tile targetTile = targetTiles.get(targetBand);
                final TileIndex trgIndex = new TileIndex(targetTile);
                final String trgBandName = targetBand.getName();
                final ProductData dataBuffer = targetTiles.get(targetBand).getDataBuffer();

                if (trgBandName.equals("C11")) {
                    computeFilteredTile(x0, y0, maxX, maxY, sourceRectangle, data11Real, span, trgIndex, dataBuffer);
                } else if (trgBandName.contains("C12_real")) {
                    computeFilteredTile(x0, y0, maxX, maxY, sourceRectangle, data12Real, span, trgIndex, dataBuffer);
                } else if (trgBandName.contains("C12_imag")) {
                    computeFilteredTile(x0, y0, maxX, maxY, sourceRectangle, data12Imag, span, trgIndex, dataBuffer);
                } else if (trgBandName.equals("C22")) {
                    computeFilteredTile(x0, y0, maxX, maxY, sourceRectangle, data22Real, span, trgIndex, dataBuffer);
                }
            }
        }
    }

    private void computeFilteredTile(final int x0, final int y0, final int maxX, final int maxY,
                                     final Rectangle sourceRectangle, final double[][] data, final double[][] span,
                                     final TileIndex trgIndex, final ProductData dataBuffer) {

        final int filterSize2 = filterSize * filterSize;
        final double[][] neighborSpanValues = new double[filterSize][filterSize];
        final double[][] neighborPixelValues = new double[filterSize][filterSize];

        for (int y = y0; y < maxY; ++y) {
            trgIndex.calculateStride(y);
            for (int x = x0; x < maxX; ++x) {

                final int n = getLocalData(x, y, sourceRectangle, data, span, neighborPixelValues, neighborSpanValues);

                double v;
                if (n < filterSize2) {
                    v = computePixelValueUsingLocalStatistics(neighborPixelValues);
                } else {
                    v = computePixelValueUsingEdgeDetection(neighborPixelValues, neighborSpanValues);
                }

                dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float) v);
            }
        }
    }

    /**
     * Filter the given tile of image with refined Lee filter.
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @param sourceRectangle The area in the source product
     */
    private void refinedLeeFilterFullPol(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle,
                                         final Rectangle sourceRectangle) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("refinedLee x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;

        final double[][] data11Real = new double[sh][sw];
        final double[][] data12Real = new double[sh][sw];
        final double[][] data12Imag = new double[sh][sw];
        final double[][] data13Real = new double[sh][sw];
        final double[][] data13Imag = new double[sh][sw];
        final double[][] data22Real = new double[sh][sw];
        final double[][] data23Real = new double[sh][sw];
        final double[][] data23Imag = new double[sh][sw];
        final double[][] data33Real = new double[sh][sw];
        final double[][] span = new double[sh][sw];

        final TileIndex trgIndex = new TileIndex(targetTiles.get(targetProduct.getBandAt(0)));
        final int filterSize2 = filterSize * filterSize;

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {
            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
            for (int i = 0; i < bandList.srcBands.length; ++i) {
                sourceTiles[i] = operator.getSourceTile(bandList.srcBands[i], sourceRectangle);
                dataBuffers[i] = sourceTiles[i].getDataBuffer();
            }

            final Tile srcTile = operator.getSourceTile(bandList.srcBands[0], sourceRectangle);
            createT3SpanImage(srcTile, sourceProductType, sourceRectangle, dataBuffers, data11Real, data12Real, data12Imag,
                              data13Real, data13Imag, data22Real, data23Real, data23Imag, data33Real, span);

            final double[][] neighborSpanValues = new double[filterSize][filterSize];
            final double[][] neighborPixelValues = new double[filterSize][filterSize];

            final ProductData[] targetDataBuffers = new ProductData[9];

            for (final Band targetBand : bandList.targetBands) {
                final String trgBandName = targetBand.getName();
                final ProductData dataBuffer = targetTiles.get(targetBand).getDataBuffer();
                if (targetDataBuffers[0] == null && (trgBandName.equals("T11") || trgBandName.contains("T11_")))
                    targetDataBuffers[0] = dataBuffer;
                else if (targetDataBuffers[1] == null && trgBandName.contains("T12_real"))
                    targetDataBuffers[1] = dataBuffer;
                else if (targetDataBuffers[2] == null && trgBandName.contains("T12_imag"))
                    targetDataBuffers[2] = dataBuffer;
                else if (targetDataBuffers[3] == null && trgBandName.contains("T13_real"))
                    targetDataBuffers[3] = dataBuffer;
                else if (targetDataBuffers[4] == null && trgBandName.contains("T13_imag"))
                    targetDataBuffers[4] = dataBuffer;
                else if (targetDataBuffers[5] == null && (trgBandName.equals("T22") || trgBandName.contains("T22_")))
                    targetDataBuffers[5] = dataBuffer;
                else if (targetDataBuffers[6] == null && trgBandName.contains("T23_real"))
                    targetDataBuffers[6] = dataBuffer;
                else if (targetDataBuffers[7] == null && trgBandName.contains("T23_imag"))
                    targetDataBuffers[7] = dataBuffer;
                else if (targetDataBuffers[8] == null && (trgBandName.equals("T33") || trgBandName.contains("T33_")))
                    targetDataBuffers[8] = dataBuffer;
            }

            int i = 0;
            for (T3Elem elem : T3Elem.values()) {
                for (int y = y0; y < maxY; ++y) {
                    trgIndex.calculateStride(y);
                    for (int x = x0; x < maxX; ++x) {
                        final int idx = trgIndex.getIndex(x);

                        int n = 0;
                        switch (elem) {
                            case T11:
                                n = getLocalData(x, y, sourceRectangle, data11Real, span, neighborPixelValues, neighborSpanValues);
                                i = 0;
                                break;

                            case T12_real:
                                n = getLocalData(x, y, sourceRectangle, data12Real, span, neighborPixelValues, neighborSpanValues);
                                i = 1;
                                break;

                            case T12_imag:
                                n = getLocalData(x, y, sourceRectangle, data12Imag, span, neighborPixelValues, neighborSpanValues);
                                i = 2;
                                break;

                            case T13_real:
                                n = getLocalData(x, y, sourceRectangle, data13Real, span, neighborPixelValues, neighborSpanValues);
                                i = 3;
                                break;

                            case T13_imag:
                                n = getLocalData(x, y, sourceRectangle, data13Imag, span, neighborPixelValues, neighborSpanValues);
                                i = 4;
                                break;

                            case T22:
                                n = getLocalData(x, y, sourceRectangle, data22Real, span, neighborPixelValues, neighborSpanValues);
                                i = 5;
                                break;

                            case T23_real:
                                n = getLocalData(x, y, sourceRectangle, data23Real, span, neighborPixelValues, neighborSpanValues);
                                i = 6;
                                break;

                            case T23_imag:
                                n = getLocalData(x, y, sourceRectangle, data23Imag, span, neighborPixelValues, neighborSpanValues);
                                i = 7;
                                break;

                            case T33:
                                n = getLocalData(x, y, sourceRectangle, data33Real, span, neighborPixelValues, neighborSpanValues);
                                i = 8;
                                break;

                            default:
                                break;
                        }

                        if (n < filterSize2) {
                            targetDataBuffers[i].setElemFloatAt(
                                    idx, (float) computePixelValueUsingLocalStatistics(neighborPixelValues));
                        } else {
                            targetDataBuffers[i].setElemFloatAt(
                                    idx, (float) computePixelValueUsingEdgeDetection(neighborPixelValues, neighborSpanValues));
                        }
                    }
                }
            }
        }
    }

    /**
     * Compute filtered pixel value using Local Statistics filter.
     *
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @return The filtered pixel value.
     */
    private double computePixelValueUsingLocalStatistics(final double[][] neighborPixelValues) {

        // here y is the pixel amplitude or intensity and x is the pixel reflectance before degradation
        final double meanY = getLocalMeanValue(neighborPixelValues);
        final double varY = getLocalVarianceValue(meanY, neighborPixelValues);
        if (varY == 0.0) {
            return 0.0;
        }

        double varX = (varY - meanY * meanY * sigmaVSqr) / (1 + sigmaVSqr);
        if (varX < 0.0) {
            varX = 0.0;
        }
        final double b = varX / varY;
        return meanY + b * (neighborPixelValues[halfFilterSize][halfFilterSize] - meanY);
    }

    /**
     * Compute filtered pixel value using refined Lee filter.
     *
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @param neighborSpanValues  The span image pixel values in the neighborhood.
     * @return The filtered pixel value.
     */
    private double computePixelValueUsingEdgeDetection(final double[][] neighborPixelValues,
                                                       final double[][] neighborSpanValues) {

        final double[][] subAreaMeans = new double[3][3];
        computeSubAreaMeans(stride, subWindowSize, neighborSpanValues, subAreaMeans);

        int d = getDirection(subAreaMeans);

        final double[] spanPixels = new double[convSize];
        getNonEdgeAreaPixelValues(neighborSpanValues, d, spanPixels);

        final double meanY = getMeanValue(spanPixels);
        final double varY = getVarianceValue(spanPixels, meanY);
        if (varY == 0.0) {
            return 0.0;
        }

        double varX = (varY - meanY * meanY * sigmaVSqr) / (1 + sigmaVSqr);
        if (varX < 0.0) {
            varX = 0.0;
        }
        final double b = varX / varY;

        final double[] covElemPixels = new double[convSize];
        getNonEdgeAreaPixelValues(neighborPixelValues, d, covElemPixels);
        final double meanZ = getMeanValue(covElemPixels);

        return meanZ + b * (neighborPixelValues[halfFilterSize][halfFilterSize] - meanZ);
    }

    /**
     * Comppute local mean for pixels in the neighborhood.
     *
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @return The local mean.
     */
    private double getLocalMeanValue(final double[][] neighborPixelValues) {
        int k = 0;
        double mean = 0;
        for (int j = 0; j < filterSize; ++j) {
            for (int i = 0; i < filterSize; ++i) {
                if (neighborPixelValues[j][i] != NonValidPixelValue) {
                    mean += neighborPixelValues[j][i];
                    k++;
                }
            }
        }
        return mean / k;
    }

    /**
     * Comppute local variance for pixels in the neighborhood.
     *
     * @param mean                The mean value for pixels in the neighborhood.
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @return The local variance.
     */
    private double getLocalVarianceValue(final double mean, final double[][] neighborPixelValues) {
        int k = 0;
        double var = 0.0;
        for (int j = 0; j < filterSize; ++j) {
            for (int i = 0; i < filterSize; ++i) {
                if (neighborPixelValues[j][i] != NonValidPixelValue) {
                    final double diff = neighborPixelValues[j][i] - mean;
                    var += diff * diff;
                    k++;
                }
            }
        }
        return var / (k - 1);
    }

    /**
     * Compute mean values for the 3x3 sub-areas in the sliding window.
     *
     * @param stride              Stride for shifting sub-window within the sliding window.
     * @param subWindowSize       Size of sub-area.
     * @param neighborPixelValues The pixel values in the sliding window.
     * @param subAreaMeans        The 9 mean values.
     */
    private static void computeSubAreaMeans(final int stride, final int subWindowSize,
                                            final double[][] neighborPixelValues, double[][] subAreaMeans) {

        final double subWindowSizeSqr = subWindowSize * subWindowSize;
        for (int j = 0; j < 3; j++) {
            final int y0 = j * stride;
            for (int i = 0; i < 3; i++) {
                final int x0 = i * stride;

                double mean = 0.0;
                for (int y = y0; y < y0 + subWindowSize; y++) {
                    for (int x = x0; x < x0 + subWindowSize; x++) {
                        mean += neighborPixelValues[y][x];
                    }
                }
                subAreaMeans[j][i] = mean / subWindowSizeSqr;
            }
        }
    }

    /**
     * Get gradient direction.
     *
     * @param subAreaMeans The mean values for the 3x3 sub-areas in the sliding window.
     * @return The direction.
     */
    private static int getDirection(final double[][] subAreaMeans) {

        final double[] gradient = new double[4];
        gradient[0] = subAreaMeans[0][2] + subAreaMeans[1][2] + subAreaMeans[2][2] -
                subAreaMeans[0][0] - subAreaMeans[1][0] - subAreaMeans[2][0];

        gradient[1] = subAreaMeans[0][1] + subAreaMeans[0][2] + subAreaMeans[1][2] -
                subAreaMeans[1][0] - subAreaMeans[2][0] - subAreaMeans[2][1];

        gradient[2] = subAreaMeans[0][0] + subAreaMeans[0][1] + subAreaMeans[0][2] -
                subAreaMeans[2][0] - subAreaMeans[2][1] - subAreaMeans[2][2];

        gradient[3] = subAreaMeans[0][0] + subAreaMeans[0][1] + subAreaMeans[1][0] -
                subAreaMeans[1][2] - subAreaMeans[2][1] - subAreaMeans[2][2];

        int direction = 0;
        double maxGradient = -1.0;
        for (int i = 0; i < 4; i++) {
            double absGrad = Math.abs(gradient[i]);
            if (maxGradient < absGrad) {
                maxGradient = absGrad;
                direction = i;
            }
        }

        if (gradient[direction] > 0.0) {
            direction += 4;
        }

        return direction;
    }

    /**
     * Get pixel values from the non-edge area indicated by the given direction.
     *
     * @param neighborPixelValues The pixel values in the filterSize by filterSize neighborhood.
     * @param d                   The direction index.
     * @param pixels              The array of pixels.
     */
    private void getNonEdgeAreaPixelValues(final double[][] neighborPixelValues, final int d, double[] pixels) {

        switch (d) {
            case 0: {

                int k = 0;
                for (int y = 0; y < filterSize; y++) {
                    for (int x = halfFilterSize; x < filterSize; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 1: {

                int k = 0;
                for (int y = 0; y < filterSize; y++) {
                    for (int x = y; x < filterSize; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 2: {

                int k = 0;
                for (int y = 0; y <= halfFilterSize; y++) {
                    for (int x = 0; x < filterSize; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 3: {

                int k = 0;
                for (int y = 0; y < filterSize; y++) {
                    for (int x = 0; x < filterSize - y; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 4: {

                int k = 0;
                for (int y = 0; y < filterSize; y++) {
                    for (int x = 0; x <= halfFilterSize; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 5: {

                int k = 0;
                for (int y = 0; y < filterSize; y++) {
                    for (int x = 0; x < y + 1; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 6: {

                int k = 0;
                for (int y = halfFilterSize; y < filterSize; y++) {
                    for (int x = 0; x < filterSize; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 7: {

                int k = 0;
                for (int y = 0; y < filterSize; y++) {
                    for (int x = filterSize - 1 - y; x < filterSize; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
        }
    }

    /**
     * Filter C3, T3, C4 or T4 data for the given tile with refined Lee filter.
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @param sourceRectangle The area in the source product
     */
    private void refinedLeeFilterC3T3C4T4(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle,
                                          final Rectangle sourceRectangle) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("refinedLee x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final int sx0 = sourceRectangle.x;
        final int sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;
        final int filterSize2 = filterSize * filterSize;

        final double[][] neighborSpanValues = new double[filterSize][filterSize];
        final double[][] neighborPixelValues = new double[filterSize][filterSize];

        final int syMax = sy0 + sh;
        final int sxMax = sx0 + sw;

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {

            final double[][] span = new double[sh][sw];
            createSpanImage(bandList.srcBands, sourceRectangle, span);

            for (Band targetBand : bandList.targetBands) {
                final Tile targetTile = targetTiles.get(targetBand);
                final Tile sourceTile = operator.getSourceTile(sourceProduct.getBand(targetBand.getName()), sourceRectangle);
                final TileIndex trgIndex = new TileIndex(targetTile);
                final TileIndex srcIndex = new TileIndex(sourceTile);
                final ProductData dataBuffer = targetTile.getDataBuffer();

                final float[] srcData = sourceTile.getDataBufferFloat();

                for (int y = y0; y < maxY; ++y) {
                    trgIndex.calculateStride(y);
                    final int yhalf = y - halfFilterSize;

                    for (int x = x0; x < maxX; ++x) {
                        final int xhalf = x - halfFilterSize;

                        final int n = getNeighborValuesWithoutBorderExt
                                (xhalf, yhalf, sx0, sy0, syMax, sxMax, neighborPixelValues, span, neighborSpanValues,
                                 srcIndex, srcData);

                        double v;
                        if (n < filterSize2) {
                            v = computePixelValueUsingLocalStatistics(neighborPixelValues);
                        } else {
                            v = computePixelValueUsingEdgeDetection(neighborPixelValues, neighborSpanValues);
                        }
                        dataBuffer.setElemFloatAt(trgIndex.getIndex(x), (float) v);

                    }
                }
            }
        }
    }

    private int getLocalData(final int xc, final int yc, final Rectangle sourceRectangle, final double[][] data,
                             final double[][] span, double[][] neighborPixelValues, double[][] neighborSpanValues) {

        final int sx0 = sourceRectangle.x;
        final int sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;
        final int syMax = sy0 + sh;
        final int sxMax = sx0 + sw;
        final int yhalf = yc - halfFilterSize;
        final int xhalf = xc - halfFilterSize;

        int k = 0;
        for (int j = 0; j < filterSize; ++j) {
            final int yj = yhalf + j;

            if (yj < sy0 || yj >= syMax) {
                for (int i = 0; i < filterSize; ++i) {
                    neighborPixelValues[j][i] = NonValidPixelValue;
                    neighborSpanValues[j][i] = NonValidPixelValue;
                }
                continue;
            }

            final int spanY = yj - sy0;
            for (int i = 0; i < filterSize; ++i) {
                final int xi = xhalf + i;

                if (xi < sx0 || xi >= sxMax) {
                    neighborPixelValues[j][i] = NonValidPixelValue;
                    neighborSpanValues[j][i] = NonValidPixelValue;
                } else {
                    neighborPixelValues[j][i] = data[spanY][xi - sx0];
                    neighborSpanValues[j][i] = span[spanY][xi - sx0];
                    k++;
                }
            }
        }

        return k;
    }

    /**
     * Create Span image.
     *
     * @param sourceBands         the input bands
     * @param sourceTileRectangle The source tile rectangle.
     * @param span                The span image.
     */
    private void createSpanImage(final Band[] sourceBands, final Rectangle sourceTileRectangle, final double[][] span) {

        // The pixel value of the span image is given by the trace of the covariance or coherence matrix for the pixel.
        Tile[] sourceTiles;
        if (sourceProductType == PolBandUtils.MATRIX.C3 || sourceProductType == PolBandUtils.MATRIX.T3) {
            sourceTiles = new Tile[3];
        } else if (sourceProductType == PolBandUtils.MATRIX.C4 || sourceProductType == PolBandUtils.MATRIX.T4) {
            sourceTiles = new Tile[4];
        } else {
            throw new OperatorException("Polarimetric Matrix not supported");
        }

        for (final Band band : sourceBands) {
            final String bandName = band.getName();
            if (PolBandUtils.isBandForMatrixElement(bandName, "11")) {
                sourceTiles[0] = operator.getSourceTile(band, sourceTileRectangle);
            } else if (PolBandUtils.isBandForMatrixElement(bandName, "22")) {
                sourceTiles[1] = operator.getSourceTile(band, sourceTileRectangle);
            } else if (PolBandUtils.isBandForMatrixElement(bandName, "33")) {
                sourceTiles[2] = operator.getSourceTile(band, sourceTileRectangle);
            } else if (PolBandUtils.isBandForMatrixElement(bandName, "44")) {
                sourceTiles[3] = operator.getSourceTile(band, sourceTileRectangle);
            }
        }

        final int sx0 = sourceTileRectangle.x;
        final int sy0 = sourceTileRectangle.y;
        final int sw = sourceTileRectangle.width;
        final int sh = sourceTileRectangle.height;
        final int maxY = sy0 + sh;
        final int maxX = sx0 + sw;

        final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

        for (int y = sy0; y < maxY; ++y) {
            srcIndex.calculateStride(y);
            final int spanY = y - sy0;
            for (int x = sx0; x < maxX; ++x) {
                final int index = srcIndex.getIndex(x);

                double sum = 0.0;
                for (Tile srcTile : sourceTiles) {
                    sum += srcTile.getDataBuffer().getElemDoubleAt(index);
                }
                span[spanY][x - sx0] = sum / 4;
            }
        }
    }

    /**
     * Get span image pixel values in a filter size rectanglar region centered at the given pixel.
     *
     * @param xhalf               X coordinate of the given pixel.
     * @param yhalf               Y coordinate of the given pixel.
     * @param sx0                 X coordinate of pixel at upper left corner of source tile.
     * @param sy0                 Y coordinate of pixel at upper left corner of source tile.
     * @param neighborPixelValues 2-D array holding the pixel valuse
     * @param span                The span image.
     * @param neighborSpanValues  2-D array holding the span image pixel valuse.
     * @return The number of valid pixels.
     * @throws org.esa.snap.framework.gpf.OperatorException If an error occurs in obtaining the pixel values.
     */
    private int getNeighborValuesWithoutBorderExt(
            final int xhalf, final int yhalf, final int sx0, final int sy0, final int syMax, final int sxMax,
            final double[][] neighborPixelValues, final double[][] span, double[][] neighborSpanValues,
            final TileIndex srcIndex, final float[] srcData) {

        int k = 0;
        for (int j = 0; j < filterSize; ++j) {
            final int yj = yhalf + j;

            if (yj < sy0 || yj >= syMax) {
                for (int i = 0; i < filterSize; ++i) {
                    neighborPixelValues[j][i] = NonValidPixelValue;
                    neighborSpanValues[j][i] = NonValidPixelValue;
                }
                continue;
            }

            final int spanY = yj - sy0;
            srcIndex.calculateStride(yj);
            for (int i = 0; i < filterSize; ++i) {
                final int xi = xhalf + i;

                if (xi < sx0 || xi >= sxMax) {
                    neighborPixelValues[j][i] = NonValidPixelValue;
                    neighborSpanValues[j][i] = NonValidPixelValue;
                } else {
                    neighborPixelValues[j][i] = srcData[srcIndex.getIndex(xi)];
                    neighborSpanValues[j][i] = span[spanY][xi - sx0];
                    k++;
                }
            }
        }

        return k;
    }
}
