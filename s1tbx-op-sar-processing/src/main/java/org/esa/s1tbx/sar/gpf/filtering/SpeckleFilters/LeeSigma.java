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
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.FilterWindow;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * LeeSigma Speckle Filter
 */
public class LeeSigma implements SpeckleFilter {

    private final Operator operator;
    private final Product sourceProduct;
    private final Product targetProduct;
    private final int sourceImageWidth;
    private final int sourceImageHeight;
    private Map<String, String[]> targetBandNameToSourceBandName;
    private final String numLooksStr;
    private final String windowSize;
    private final String targetWindowSizeStr;
    private final String sigmaStr;

    private int halfSizeX;
    private int halfSizeY;
    private int numLooks = 0;
    private int filterSize = 0;
    private double A1, A2; // sigma range for amplitude
    private double I1, I2; // sigma range for intensity
    private int sigma;
    private double ISigmaV;
    private double ISigmaVSqr;
    private double ISigmaVP; // revised sigmaV used in MMSE filter
    private double ISigmaVPSqr;
    private double ASigmaV;
    private double ASigmaVSqr;
    private double ASigmaVP; // revised sigmaV used in MMSE filter
    private double ASigmaVPSqr;
    private int targetWindowSize = 0;
    private int halfTargetWindowSize = 0;
    private int targetSize = 5;

    private static final String SIGMA_50_PERCENT = "0.5";
    private static final String SIGMA_60_PERCENT = "0.6";
    private static final String SIGMA_70_PERCENT = "0.7";
    private static final String SIGMA_80_PERCENT = "0.8";
    private static final String SIGMA_90_PERCENT = "0.9";

    public LeeSigma(final Operator op, final Product srcProduct, final Product trgProduct,
                    final Map<String, String[]> targetBandNameToSourceBandName, final String numLooksStr,
                    final String windowSize, final String targetWindowSizeStr, final String sigmaStr) {

        this.operator = op;
        this.sourceProduct = srcProduct;
        this.targetProduct = trgProduct;
        this.sourceImageWidth = srcProduct.getSceneRasterWidth();
        this.sourceImageHeight = srcProduct.getSceneRasterHeight();
        this.targetBandNameToSourceBandName = targetBandNameToSourceBandName;
        this.numLooksStr = numLooksStr;
        this.windowSize = windowSize;
        this.targetWindowSizeStr = targetWindowSizeStr;
        this.sigmaStr = sigmaStr;

        setLeeSigmaParameters();
    }

    private void setLeeSigmaParameters() {

        numLooks = Integer.parseInt(numLooksStr);
        filterSize = FilterWindow.parseWindowSize(windowSize);

        halfSizeX = filterSize / 2;
        halfSizeY = halfSizeX;

        targetWindowSize = FilterWindow.parseWindowSize(targetWindowSizeStr);

        halfTargetWindowSize = targetWindowSize / 2;

        ISigmaV = 1.0 / Math.sqrt(numLooks);
        ISigmaVSqr = ISigmaV * ISigmaV;

        ASigmaV = 0.5227 / Math.sqrt(numLooks);
        ASigmaVSqr = ASigmaV * ASigmaV;

        setSigmaRange(sigmaStr);
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
                ISigmaVP = 0.4057;
            } else if (sigma == 6) {
                I1 = 0.343;
                I2 = 2.210;
                ISigmaVP = 0.4954;
            } else if (sigma == 7) {
                I1 = 0.254;
                I2 = 2.582;
                ISigmaVP = 0.5911;
            } else if (sigma == 8) {
                I1 = 0.168;
                I2 = 3.094;
                ISigmaVP = 0.6966;
            } else if (sigma == 9) {
                I1 = 0.084;
                I2 = 3.941;
                ISigmaVP = 0.8191;
            }

        } else if (numLooks == 2) {

            if (sigma == 5) {
                I1 = 0.582;
                I2 = 1.584;
                ISigmaVP = 0.2763;
            } else if (sigma == 6) {
                I1 = 0.501;
                I2 = 1.755;
                ISigmaVP = 0.3388;
            } else if (sigma == 7) {
                I1 = 0.418;
                I2 = 1.972;
                ISigmaVP = 0.4062;
            } else if (sigma == 8) {
                I1 = 0.327;
                I2 = 2.260;
                ISigmaVP = 0.4810;
            } else if (sigma == 9) {
                I1 = 0.221;
                I2 = 2.744;
                ISigmaVP = 0.5699;
            }

        } else if (numLooks == 3) {

            if (sigma == 5) {
                I1 = 0.652;
                I2 = 1.458;
                ISigmaVP = 0.2222;
            } else if (sigma == 6) {
                I1 = 0.580;
                I2 = 1.586;
                ISigmaVP = 0.2736;
            } else if (sigma == 7) {
                I1 = 0.505;
                I2 = 1.751;
                ISigmaVP = 0.3280;
            } else if (sigma == 8) {
                I1 = 0.419;
                I2 = 1.965;
                ISigmaVP = 0.3892;
            } else if (sigma == 9) {
                I1 = 0.313;
                I2 = 2.320;
                ISigmaVP = 0.4624;
            }

        } else if (numLooks == 4) {

            if (sigma == 5) {
                I1 = 0.694;
                I2 = 1.385;
                ISigmaVP = 0.1921;
            } else if (sigma == 6) {
                I1 = 0.630;
                I2 = 1.495;
                ISigmaVP = 0.2348;
            } else if (sigma == 7) {
                I1 = 0.560;
                I2 = 1.627;
                ISigmaVP = 0.2825;
            } else if (sigma == 8) {
                I1 = 0.480;
                I2 = 1.804;
                ISigmaVP = 0.3354;
            } else if (sigma == 9) {
                I1 = 0.378;
                I2 = 2.094;
                ISigmaVP = 0.3991;
            }
        }

        ISigmaVPSqr = ISigmaVP * ISigmaVP;

        if (numLooks == 1) {

            if (sigma == 5) {
                A1 = 0.653997;
                A2 = 1.40002;
                ASigmaVP = 0.208349;
            } else if (sigma == 6) {
                A1 = 0.578998;
                A2 = 1.50601;
                ASigmaVP = 0.255358;
            } else if (sigma == 7) {
                A1 = 0.496999;
                A2 = 1.63201;
                ASigmaVP = 0.305303;
            } else if (sigma == 8) {
                A1 = 0.403999;
                A2 = 1.79501;
                ASigmaVP = 0.361078;
            } else if (sigma == 9) {
                A1 = 0.286;
                A2 = 2.04301;
                ASigmaVP = 0.426375;
            }

        } else if (numLooks == 2) {

            if (sigma == 5) {
                A1 = 0.76;
                A2 = 1.263;
                ASigmaVP = 0.139021;
            } else if (sigma == 6) {
                A1 = 0.705;
                A2 = 1.332;
                ASigmaVP = 0.169777;
            } else if (sigma == 7) {
                A1 = 0.643;
                A2 = 1.412;
                ASigmaVP = 0.206675;
            } else if (sigma == 8) {
                A1 = 0.568;
                A2 = 1.515;
                ASigmaVP = 0.244576;
            } else if (sigma == 9) {
                A1 = 0.467;
                A2 = 1.673;
                ASigmaVP = 0.29107;
            }

        } else if (numLooks == 3) {

            if (sigma == 5) {
                A1 = 0.806;
                A2 = 1.21;
                ASigmaVP = 0.109732;
            } else if (sigma == 6) {
                A1 = 0.76;
                A2 = 1.263;
                ASigmaVP = 0.138001;
            } else if (sigma == 7) {
                A1 = 0.708;
                A2 = 1.327;
                ASigmaVP = 0.163686;
            } else if (sigma == 8) {
                A1 = 0.645;
                A2 = 1.408;
                ASigmaVP = 0.19597;
            } else if (sigma == 9) {
                A1 = 0.557;
                A2 = 1.531;
                ASigmaVP = 0.234219;
            }

        } else if (numLooks == 4) {

            if (sigma == 5) {
                A1 = 0.832;
                A2 = 1.179;
                ASigmaVP = 0.0894192;
            } else if (sigma == 6) {
                A1 = 0.793;
                A2 = 1.226;
                ASigmaVP = 0.112018;
            } else if (sigma == 7) {
                A1 = 0.747;
                A2 = 1.279;
                ASigmaVP = 0.139243;
            } else if (sigma == 8) {
                A1 = 0.691;
                A2 = 1.347;
                ASigmaVP = 0.167771;
            } else if (sigma == 9) {
                A1 = 0.613;
                A2 = 1.452;
                ASigmaVP = 0.839;
            }
        }
        ASigmaVPSqr = ASigmaVP * ASigmaVP;
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
            OperatorUtils.catchOperatorException("LeeSigma", e);
        } finally {
            pm.done();
        }
    }

    public double[][] performFiltering(
            final int x0, final int y0, final int w, final int h, final String[] srcBandNames) {

        final double[][] filteredTile = new double[h][w];

        final Rectangle sourceTileRectangle = getSourceTileRectangle(
                x0, y0, w, h, halfSizeX, halfSizeY, sourceImageWidth, sourceImageHeight);

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

        final int sx0 = sourceTileRectangle.x;
        final int sy0 = sourceTileRectangle.y;
        final int sw = sourceTileRectangle.width;
        final int sh = sourceTileRectangle.height;

        double sigmaVSqr, sigmaVPSqr, sigmaRangeLow, sigmaRangeHigh;
        if (bandUnit == Unit.UnitType.AMPLITUDE) {
            sigmaVSqr = ASigmaVSqr;
            sigmaVPSqr = ASigmaVPSqr;
            sigmaRangeLow = A1;
            sigmaRangeHigh = A2;
        } else {
            sigmaVSqr = ISigmaVSqr;
            sigmaVPSqr = ISigmaVPSqr;
            sigmaRangeLow = I1;
            sigmaRangeHigh = I2;
        }

        final double z98 = computeZ98Values(
                srcIndex, sourceTileRectangle, noDataValue, bandUnit, sourceData1, sourceData2);

        final boolean[][] isPointTarget = new boolean[h][w];
        final double[][] targetWindow = new double[targetWindowSize][targetWindowSize];
        final double[][] filterWindow = new double[filterSize][filterSize];
        double[] pixelsSelected;

        final int xMax = x0 + w;
        final int yMax = y0 + h;
        for (int y = y0; y < yMax; ++y) {
            final int yy = y - y0;
            srcIndex.calculateStride(y);

            for (int x = x0; x < xMax; ++x) {
                final int xx = x - x0;
                final int srcIdx = srcIndex.getIndex(x);

                final double v = getPixelValue(srcIdx, noDataValue, bandUnit, sourceData1, sourceData2);

                if (isPointTarget[yy][xx]) {
                    filteredTile[yy][xx] = v;
                    continue;
                }

                if (y - halfSizeY < sy0 || y + halfSizeY > sy0 + sh - 1 ||
                        x - halfSizeX < sx0 || x + halfSizeX > sx0 + sw - 1) {

                    getWindowPixels(x, y, sx0, sy0, sw, sh, sourceTile1, noDataValue, bandUnit,
                                    sourceData1, sourceData2, filterWindow);

                    pixelsSelected = getValidPixels(filterWindow, noDataValue);

                    final double vEst = computeMMSEEstimate(v, pixelsSelected, sigmaVSqr, noDataValue);

                    filteredTile[yy][xx] = vEst;
                    continue;
                }

                getWindowPixels(x, y, sx0, sy0, sw, sh, sourceTile1, noDataValue, bandUnit,
                                sourceData1, sourceData2, targetWindow);

                if (checkPointTarget(x, y, z98, targetWindow, isPointTarget, x0, y0, w, h, noDataValue)) {
                    filteredTile[yy][xx] = v;
                    continue;
                }

                pixelsSelected = getValidPixels(targetWindow, noDataValue);
                final double meanEst = computeMMSEEstimate(v, pixelsSelected, sigmaVSqr, noDataValue);
                double[] sigmaRange = {meanEst * sigmaRangeLow, meanEst * sigmaRangeHigh};

                getWindowPixels(x, y, sx0, sy0, sw, sh, sourceTile1, noDataValue, bandUnit,
                                sourceData1, sourceData2, filterWindow);

                pixelsSelected = selectPixelsInSigmaRange(sigmaRange, filterWindow, noDataValue);
                if (pixelsSelected.length == 0) {
                    filteredTile[yy][xx] = v;
                    continue;
                }

                final double vEst = computeMMSEEstimate(v, pixelsSelected, sigmaVPSqr, noDataValue);
                filteredTile[yy][xx] = vEst;
            }
        }

        return filteredTile;
    }

    private static double computeZ98Values(final TileIndex srcIndex, final Rectangle sourceRectangle,
                                           final double noDataValue, final Unit.UnitType unit,
                                           final ProductData srcData1, final ProductData srcData2) {

        final int sx0 = sourceRectangle.x;
        final int sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;
        final int maxY = sy0 + sh;
        final int maxX = sx0 + sw;
        final int z98Index = (int) (sw * sh * 0.98) - 1;

        double[] pixelValues = new double[sw * sh];
        int k = 0;
        for (int y = sy0; y < maxY; y++) {
            srcIndex.calculateStride(y);
            for (int x = sx0; x < maxX; x++) {
                pixelValues[k++] = getPixelValue(srcIndex.getIndex(x), noDataValue, unit, srcData1, srcData2);
            }
        }

        Arrays.sort(pixelValues);
        return pixelValues[z98Index];
    }

    private static double getPixelValue(final int index, final double noDataValue, final Unit.UnitType unit,
                                        final ProductData srcData1, final ProductData srcData2) {

        if (unit == Unit.UnitType.REAL || unit == Unit.UnitType.IMAGINARY) {
            final double I = srcData1.getElemDoubleAt(index);
            final double Q = srcData2.getElemDoubleAt(index);
            if (Double.compare(I, noDataValue) != 0 && Double.compare(Q, noDataValue) != 0) {
                return I * I + Q * Q;
            } else {
                return noDataValue;
            }
        } else {
            return srcData1.getElemDoubleAt(index);
        }
    }

    private static void getWindowPixels(final int x, final int y, final int sx0, final int sy0, final int sw, final int sh,
                                        final Tile sourceTile, final double noDataValue, final Unit.UnitType unit,
                                        final ProductData srcData1, final ProductData srcData2, final double[][] windowPixel) {

        final TileIndex srcIndex = new TileIndex(sourceTile);
        final int windowSize = windowPixel.length;
        final int halfWindowSize = windowSize / 2;

        int yy, xx;
        for (int j = 0; j < windowSize; j++) {
            yy = y - halfWindowSize + j;
            srcIndex.calculateStride(yy);
            for (int i = 0; i < windowSize; i++) {
                xx = x - halfWindowSize + i;
                if (yy >= sy0 && yy <= sy0 + sh - 1 && xx >= sx0 && xx <= sx0 + sw - 1) {
                    windowPixel[j][i] = getPixelValue(srcIndex.getIndex(xx), noDataValue, unit, srcData1, srcData2);
                } else {
                    windowPixel[j][i] = noDataValue;
                }
            }
        }
    }

    private static double[] getValidPixels(final double[][] filterWindow, final double noDataValue) {

        final List<Double> pixelsSelected = new ArrayList<>();
        final int nCols = filterWindow[0].length;
        for (double[] aFilterWindow : filterWindow) {
            for (int i = 0; i < nCols; i++) {
                if (Double.compare(aFilterWindow[i], noDataValue) != 0) {
                    pixelsSelected.add(aFilterWindow[i]);
                }
            }
        }
        final double[] result = new double[pixelsSelected.size()];
        for(int i = 0; i < pixelsSelected.size(); ++i) {
            result[i] = pixelsSelected.get(i);
        }
        return result;
    }

    private boolean checkPointTarget(final int x, final int y, final double z98, final double[][] targetWindow,
                                     final boolean[][] isPointTarget, final int x0, final int y0, final int w,
                                     final int h, final double noDataValue) {

        if (targetWindow[halfTargetWindowSize][halfTargetWindowSize] > z98) {
            if (getClusterSize(z98, targetWindow, noDataValue) > targetSize) {
                markClusterPixels(x, y, isPointTarget, z98, targetWindow, x0, y0, w, h, noDataValue);
                return true;
            }
        }

        return false;
    }

    private int getClusterSize(final double threshold, final double[][] targetWindow, final double noDataValue) {

        int clusterSize = 0;
        for (int j = 0; j < targetWindowSize; j++) {
            for (int i = 0; i < targetWindowSize; i++) {
                if (Double.compare(targetWindow[j][i], noDataValue) != 0 && targetWindow[j][i] > threshold) {
                    clusterSize++;
                }
            }
        }
        return clusterSize;
    }

    private void markClusterPixels(final int x, final int y,
                                   boolean[][] isPointTarget, final double threshold, final double[][] targetWindow,
                                   final int x0, final int y0, final int w, final int h, final double noDataValue) {

        final int windowSize = targetWindow.length;
        final int halfWindowSize = windowSize / 2;

        int yy, xx;
        for (int j = 0; j < targetWindowSize; j++) {
            yy = y - halfWindowSize + j;
            for (int i = 0; i < targetWindowSize; i++) {
                xx = x - halfWindowSize + i;
                if (Double.compare(targetWindow[j][i], noDataValue) != 0 && targetWindow[j][i] > threshold &&
                        yy >= y0 && yy < y0 + h && xx >= x0 && xx < x0 + w) {
                    isPointTarget[yy - y0][xx - x0] = true;
                }
            }
        }
    }

    private double computeMMSEWeight(
            final double[] dataArray, final double meanY, final double sigmaVSqr, final double noDataValue) {

        final double varY = getVarianceValue(dataArray, dataArray.length, meanY, noDataValue);
        if (varY == 0.0) {
            return 0.0;
        }

        double varX = (varY - meanY * meanY * sigmaVSqr) / (1 + sigmaVSqr);
        if (varX < 0.0) {
            varX = 0.0;
        }

        return varX / varY;
    }

    private double[] selectPixelsInSigmaRange(
            final double[] sigmaRange, final double[][] filterWindow, final double noDataValue) {

        final List<Double> pixelsSelected = new ArrayList<>();
        for (int j = 0; j < filterSize; j++) {
            for (int i = 0; i < filterSize; i++) {
                if (Double.compare(filterWindow[j][i], noDataValue) != 0 && filterWindow[j][i] >= sigmaRange[0] &&
                        filterWindow[j][i] <= sigmaRange[1]) {
                    pixelsSelected.add(filterWindow[j][i]);
                }
            }
        }
        final double[] result = new double[pixelsSelected.size()];
        for(int i = 0; i < pixelsSelected.size(); ++i) {
            result[i] = pixelsSelected.get(i);
        }
        return result;
    }

    private double computeMMSEEstimate(final double centerPixelValue, final double[] dataArray,
                                       final double sigmaVSqr, final double noDataValue) {

        final double mean = getMeanValue(dataArray, dataArray.length, noDataValue);

        final double b = computeMMSEWeight(dataArray, mean, sigmaVSqr, noDataValue);

        return (1 - b) * mean + b * centerPixelValue;
    }
}
