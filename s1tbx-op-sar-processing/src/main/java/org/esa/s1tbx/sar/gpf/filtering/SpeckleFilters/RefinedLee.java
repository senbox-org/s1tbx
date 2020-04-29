/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.sar.gpf.filtering.SpeckleFilters;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.util.Arrays;
import java.util.Map;

/**
 * RefinedLee Speckle Filter
 */
public class RefinedLee implements SpeckleFilter {

    private final Operator operator;
    private final Product sourceProduct;
    private final Product targetProduct;
    private final int windowSizeX;
    private final int windowSizeY;
    private final int halfWindowSizeX;
    private final int halfWindowSizeY;
    private final int sourceImageWidth;
    private final int sourceImageHeight;
    private Map<String, String[]> targetBandNameToSourceBandName;

    public RefinedLee(final Operator op, final Product srcProduct, final Product trgProduct,
                      final Map<String, String[]> targetBandNameToSourceBandName) {

        this.operator = op;
        this.sourceProduct = srcProduct;
        this.targetProduct = trgProduct;
        this.windowSizeX = 7;
        this.windowSizeY = 7;
        this.halfWindowSizeX = windowSizeX / 2;
        this.halfWindowSizeY = windowSizeY / 2;
        this.sourceImageWidth = srcProduct.getSceneRasterWidth();
        this.sourceImageHeight = srcProduct.getSceneRasterHeight();
        this.targetBandNameToSourceBandName = targetBandNameToSourceBandName;
    }

    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) {

        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int x0 = targetTileRectangle.x;
            final int y0 = targetTileRectangle.y;
            final int w = targetTileRectangle.width;
            final int h = targetTileRectangle.height;
            final int xMax = x0 + w;
            final int yMax = y0 + h;
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final String[] srcBandNames = targetBandNameToSourceBandName.get(targetBand.getName());
            final double[][] filteredTile = performFiltering(x0, y0, w, h, srcBandNames);

            final ProductData tgtData = targetTile.getDataBuffer();
            final TileIndex tgtIndex = new TileIndex(targetTile);
            for (int y = y0; y < yMax; ++y) {
                tgtIndex.calculateStride(y);
                final int yy = y - y0;
                for (int x = x0; x < xMax; ++x) {
                    tgtData.setElemDoubleAt(tgtIndex.getIndex(x), filteredTile[yy][x - x0]);
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("RefinedLee", e);
        } finally {
            pm.done();
        }
    }

    public double[][] performFiltering(
            final int x0, final int y0, final int w, final int h, final String[] srcBandNames) {

        final double[][] filteredTile = new double[h][w];

        final Rectangle sourceTileRectangle = getSourceTileRectangle(
                x0, y0, w, h, halfWindowSizeX, halfWindowSizeY, sourceImageWidth, sourceImageHeight);

        Band sourceBand1 = null;
        Band sourceBand2 = null;
        Tile sourceTile1 = null;
        Tile sourceTile2 = null;
        ProductData sourceData1 = null;
        ProductData sourceData2 = null;
        if (srcBandNames.length == 1) {
            sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
            sourceTile1 = operator.getSourceTile(sourceBand1, sourceTileRectangle);
            sourceData1 = sourceTile1.getDataBuffer();
        } else {
            sourceBand1 = sourceProduct.getBand(srcBandNames[0]);
            sourceBand2 = sourceProduct.getBand(srcBandNames[1]);
            sourceTile1 = operator.getSourceTile(sourceBand1, sourceTileRectangle);
            sourceTile2 = operator.getSourceTile(sourceBand2, sourceTileRectangle);
            sourceData1 = sourceTile1.getDataBuffer();
            sourceData2 = sourceTile2.getDataBuffer();
        }
        final Unit.UnitType bandUnit = Unit.getUnitType(sourceBand1);
        final double noDataValue = sourceBand1.getNoDataValue();
        final TileIndex srcIndex = new TileIndex(sourceTile1);
        final double[][] neighborPixelValues = new double[windowSizeX][windowSizeY];
        final int xMax = x0 + w;
        final int yMax = y0 + h;

        for (int y = y0; y < yMax; ++y) {
            final int yy = y - y0;
            for (int x = x0; x < xMax; ++x) {
                final int xx = x - x0;

                final int numSamples = getNeighborValuesWithoutBorderExt(
                        x, y, sourceData1, sourceData2, srcIndex, noDataValue, bandUnit, sourceTileRectangle,
                        windowSizeX, windowSizeY, neighborPixelValues);

                if (numSamples > 0) {
                    filteredTile[yy][xx] = getRefinedLeeValueUsingEdgeThreshold(
                            windowSizeX, windowSizeY, numSamples, noDataValue, neighborPixelValues);
                } else {
                    filteredTile[yy][xx] = noDataValue;
                }
            }
        }
        return filteredTile;
    }

    /**
     * Compute filtered pixel value using refined Lee filter.
     *
     * @param numSamples          The number of valid pixel in the neighborhood.
     * @param noDataValue         The place holder for no data.
     * @param neighborPixelValues The neighbor pixel values.
     * @return The filtered pixel value.
     */
    public double getRefinedLeeValueUsingEdgeThreshold(
            final int filterSizeX, final int filterSizeY, final int numSamples, final double noDataValue,
            final double[][] neighborPixelValues) {

        if (numSamples < filterSizeX * filterSizeY) {
            return computePixelValueUsingLocalStatistics(neighborPixelValues, noDataValue);
        }

        /*
        final double var = getLocalVarianceValue(
                getLocalMeanValue(neighborPixelValues, noDataValue), neighborPixelValues, noDataValue);
        if (var < edgeThreshold) {
            return computePixelValueUsingLocalStatistics(neighborPixelValues, noDataValue);
        }*/

        final double[][] subAreaMeans = new double[3][3];
        computeSubAreaMeans(neighborPixelValues, subAreaMeans);

        final double[] gradients = new double[4];
        computeGradients(subAreaMeans, gradients);

        return computePixelValueUsingEdgeDetection(neighborPixelValues, noDataValue, subAreaMeans, gradients);
    }

    /**
     * Compute filtered pixel value using Local Statistics filter.
     *
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @param noDataValue         The place holder for no data.
     * @return The filtered pixel value.
     */
    private double computePixelValueUsingLocalStatistics(
            final double[][] neighborPixelValues, final Double noDataValue) {

        if (noDataValue.equals(neighborPixelValues[neighborPixelValues.length/2][neighborPixelValues[0].length/2])) {
            return noDataValue;
        }

        // y is the pixel amplitude or intensity and x is the pixel reflectance before degradation
        final double meanY = getLocalMeanValue(neighborPixelValues, noDataValue);
        if (noDataValue.equals(meanY)) {
            return noDataValue;
        }

        final double varY = getLocalVarianceValue(meanY, neighborPixelValues, noDataValue);
        if (varY == 0.0) {
            return meanY;
        }

        if (noDataValue.equals(varY)) {
            return noDataValue;
        }

        final double sigmaV = getLocalNoiseVarianceValue(neighborPixelValues, noDataValue);
        double varX = (varY - meanY * meanY * sigmaV) / (1 + sigmaV);
        if (varX < 0) {
            varX = 0.0;
        }
        final double b = varX / varY;
        return meanY + b * (neighborPixelValues[3][3] - meanY);
    }

    /**
     * Compute local mean for pixels in the neighborhood.
     *
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @param noDataValue         The place holder for no data.
     * @return The local mean.
     */
    private static double getLocalMeanValue(final double[][] neighborPixelValues, final double noDataValue) {

        int k = 0;
        double mean = 0;
        for (double[] neighborPixelValue : neighborPixelValues) {
            for (int i = 0; i < neighborPixelValues[0].length; ++i) {
                if (Double.compare(neighborPixelValue[i], noDataValue) != 0) {
                    mean += neighborPixelValue[i];
                    k++;
                }
            }
        }

        if (k > 0) {
            return mean / k;
        }

        return noDataValue;
    }

    /**
     * Compute local variance for pixels in the neighborhood.
     *
     * @param mean                The mean value for pixels in the neighborhood.
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @param noDataValue         The place holder for no data.
     * @return The local variance.
     */
    private static double getLocalVarianceValue(
            final double mean, final double[][] neighborPixelValues, final double noDataValue) {

        int k = 0;
        double var = 0.0;
        for (double[] neighborPixelValue : neighborPixelValues) {
            for (int i = 0; i < neighborPixelValues[0].length; ++i) {
                if (Double.compare(neighborPixelValue[i], noDataValue) != 0) {
                    final double diff = neighborPixelValue[i] - mean;
                    var += diff * diff;
                    k++;
                }
            }
        }

        if (k > 1) {
            return var / (k - 1);
        }

        return noDataValue;
    }

    /**
     * Compute mean values for the 9 3x3 sub-areas in the 7x7 neighborhood.
     *
     * @param neighborPixelValues The pixel values in the 7x7 neighborhood.
     * @param subAreaMeans        The 9 mean values.
     */
    private static void computeSubAreaMeans(
            final double[][] neighborPixelValues, double[][] subAreaMeans) {

        for (int j = 0; j < 3; j++) {
            final int y0 = 2 * j;
            for (int i = 0; i < 3; i++) {
                final int x0 = 2 * i;

                int k = 0;
                double mean = 0.0;
                for (int y = y0; y < y0 + 3; y++) {
                    for (int x = x0; x < x0 + 3; x++) {
                        mean += neighborPixelValues[y][x];
                        k++;
                    }
                }

                subAreaMeans[j][i] = mean / k;
            }
        }
    }

    private static void computeGradients(final double[][] subAreaMeans, final double[] gradients) {

        gradients[0] = Math.abs(subAreaMeans[1][0] - subAreaMeans[1][2]);
        gradients[1] = Math.abs(subAreaMeans[0][2] - subAreaMeans[2][0]);
        gradients[2] = Math.abs(subAreaMeans[0][1] - subAreaMeans[2][1]);
        gradients[3] = Math.abs(subAreaMeans[0][0] - subAreaMeans[2][2]);
    }

    /**
     * Compute filtered pixel value using refined Lee filter.
     *
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @param noDataValue         The place holder for no data.
     * @return The filtered pixel value.
     */
    private double computePixelValueUsingEdgeDetection(
            final double[][] neighborPixelValues, final double noDataValue,
            final double[][] subAreaMeans, final double[] gradients) {

        int direction = 0;
        double maxGradient = -Double.MAX_VALUE;
        for (int i = 0; i < gradients.length; i++) {
            if (maxGradient < gradients[i]) {
                maxGradient = gradients[i];
                direction = i;
            }
        }

        int d = 0;
        if (direction == 0) {

            if (Math.abs(subAreaMeans[1][0] - subAreaMeans[1][1]) < Math.abs(subAreaMeans[1][1] - subAreaMeans[1][2])) {
                d = 4;
            } else {
                d = 0;
            }

        } else if (direction == 1) {

            if (Math.abs(subAreaMeans[0][2] - subAreaMeans[1][1]) < Math.abs(subAreaMeans[1][1] - subAreaMeans[2][0])) {
                d = 1;
            } else {
                d = 5;
            }

        } else if (direction == 2) {

            if (Math.abs(subAreaMeans[0][1] - subAreaMeans[1][1]) < Math.abs(subAreaMeans[1][1] - subAreaMeans[2][1])) {
                d = 2;
            } else {
                d = 6;
            }

        } else if (direction == 3) {

            if (Math.abs(subAreaMeans[0][0] - subAreaMeans[1][1]) < Math.abs(subAreaMeans[1][1] - subAreaMeans[2][2])) {
                d = 3;
            } else {
                d = 7;
            }
        }

        final double[] pixels = new double[28];
        getNonEdgeAreaPixelValues(neighborPixelValues, d, pixels);

        final double meanY = getMeanValue(pixels, pixels.length, noDataValue);
        final double varY = getVarianceValue(pixels, pixels.length, meanY, noDataValue);
        if (varY == 0.0) {
            return 0.0;
        }
        final double sigmaV = getLocalNoiseVarianceValue(neighborPixelValues, noDataValue);
        double varX = (varY - meanY * meanY * sigmaV) / (1 + sigmaV);
        if (varX < 0) {
            varX = 0.0;
        }
        final double b = varX / varY;
        return meanY + b * (neighborPixelValues[3][3] - meanY);
    }

    /**
     * Compute local noise variance for pixels in the neighborhood.
     *
     * @param neighborPixelValues The pixel values in the neighborhood.
     * @param noDataValue         The place holder for no data.
     * @return The local noise variance.
     */
    private double getLocalNoiseVarianceValue(final double[][] neighborPixelValues, final double noDataValue) {

        final double[] subAreaVariances = new double[9];
        final double[] subArea = new double[9];
        int numSubArea = 0;
        for (int j = 0; j < 3; j++) {
            final int y0 = 2 * j;
            for (int i = 0; i < 3; i++) {
                final int x0 = 2 * i;

                int k = 0;
                for (int y = y0; y < y0 + 3; y++) {
                    final int yy = (y - y0) * 3;
                    for (int x = x0; x < x0 + 3; x++) {
                        if (Double.compare(neighborPixelValues[y][x], noDataValue) != 0) {
                            subArea[yy + (x - x0)] = neighborPixelValues[y][x];
                            k++;
                        }
                    }
                }

                if (k == 9) {
                    final double subAreaMean = getMeanValue(subArea, k, noDataValue);
                    if (subAreaMean > 0) {
                        subAreaVariances[numSubArea] =
                                getVarianceValue(subArea, k, subAreaMean, noDataValue) / (subAreaMean * subAreaMean);
                    } else {
                        subAreaVariances[numSubArea] = 0.0;
                    }
                    numSubArea++;
                }
            }
        }

        if (numSubArea < 1) {
            return 0.0;
        }
        Arrays.sort(subAreaVariances, 0, numSubArea - 1);
        final int numSubAreaForAvg = Math.min(5, numSubArea);
        double avg = 0.0;
        for (int n = 0; n < numSubAreaForAvg; n++) {
            avg += subAreaVariances[n];
        }
        return avg / numSubAreaForAvg;
    }

    /**
     * Get pixel values from the non-edge area indicated by the given direction.
     *
     * @param neighborPixelValues The pixel values in the 7x7 neighborhood.
     * @param d                   The direction index.
     * @param pixels              The array of pixels.
     */
    private static void getNonEdgeAreaPixelValues(final double[][] neighborPixelValues, final int d,
                                                  final double[] pixels) {
        switch (d) {
            case 0: {

                int k = 0;
                for (int y = 0; y < 7; y++) {
                    for (int x = 3; x < 7; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 1: {

                int k = 0;
                for (int y = 0; y < 7; y++) {
                    for (int x = y; x < 7; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 2: {

                int k = 0;
                for (int y = 0; y < 4; y++) {
                    for (int x = 0; x < 7; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 3: {

                int k = 0;
                for (int y = 0; y < 7; y++) {
                    for (int x = 0; x < 7 - y; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 4: {

                int k = 0;
                for (int y = 0; y < 7; y++) {
                    for (int x = 0; x < 4; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 5: {

                int k = 0;
                for (int y = 0; y < 7; y++) {
                    for (int x = 0; x < y + 1; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 6: {

                int k = 0;
                for (int y = 3; y < 7; y++) {
                    for (int x = 0; x < 7; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
            case 7: {

                int k = 0;
                for (int y = 0; y < 7; y++) {
                    for (int x = 6 - y; x < 7; x++) {
                        pixels[k] = neighborPixelValues[y][x];
                        k++;
                    }
                }
                break;
            }
        }
    }

    /*
    public double getRefinedLeeValueUsingGradientThreshold(
            final int filterSizeX, final int filterSizeY, final double gradThreshold,
            final int numSamples, final double noDataValue, final double[][] neighborPixelValues) {

        if (numSamples < filterSizeX * filterSizeY) {
            return getMeanValue(neighborPixelValues, noDataValue);
        }

        final double[][] subAreaMeans = new double[3][3];
        computeSubAreaMeans(neighborPixelValues, subAreaMeans);

        final double[] gradients = new double[4];
        computeGradients(subAreaMeans, gradients);

        if (gradients[0] < gradThreshold && gradients[1] < gradThreshold &&
                gradients[2] < gradThreshold && gradients[3] < gradThreshold) {
            return getMeanValue(neighborPixelValues, noDataValue);
        }

        return computePixelValueUsingEdgeDetection(neighborPixelValues, noDataValue, subAreaMeans, gradients);
    }*/
}
