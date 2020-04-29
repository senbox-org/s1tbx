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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Intensity-Driven Adaptive-Neighbourhood (IDAN) Speckle Filter
 */
public class IDAN implements SpeckleFilter {

    private final Operator operator;
    private final Product sourceProduct;
    private final Product targetProduct;
    private Map<String, String[]> targetBandNameToSourceBandName;
    private final int sourceImageWidth;
    private final int sourceImageHeight;
    private final int halfSizeX;
    private final int halfSizeY;

    private final int anSize;
    private final double sigmaV;
    private final double sigmaVSqr;

    public IDAN(final Operator op, final Product srcProduct, final Product trgProduct,
                final Map<String, String[]> targetBandNameToSourceBandName, final String numLooksStr,
                final int anSize) {

        this.operator = op;
        this.sourceProduct = srcProduct;
        this.targetProduct = trgProduct;
        this.targetBandNameToSourceBandName = targetBandNameToSourceBandName;
        this.halfSizeX = anSize;  // filter size in this case is used only in generating source rectangle
        this.halfSizeY = anSize;
        this.anSize = anSize;
        sourceImageWidth = srcProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();

        sigmaV = 1.0 / Math.sqrt(Integer.parseInt(numLooksStr));
        sigmaVSqr = sigmaV * sigmaV;
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
            OperatorUtils.catchOperatorException("IDAN", e);
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

        final double[][] srcTileIntensity = getSourceTileIntensity(
                sx0, sy0, sw, sh, sourceData1, sourceData2, srcIndex, noDataValue, bandUnit);

        final int xMax = x0 + w;
        final int yMax = y0 + h;
        for (int y = y0; y < yMax; ++y) {
            final int yy = y - y0;
            for (int x = x0; x < xMax; ++x) {
                final int xx = x - x0;

                final double seed = getInitialSeed(x, y, sx0, sy0, sw, sh, srcTileIntensity, noDataValue);

                final Pix[] anPixelList = getIDANPixels(x, y, sx0, sy0, sw, sh, srcTileIntensity, noDataValue, seed);

                final double b = computeFilterScaleParam(sx0, sy0, anPixelList, srcTileIntensity);

                filteredTile[yy][xx] = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, srcTileIntensity, bandUnit, b);
            }
        }

        return filteredTile;
    }

    private static double[][] getSourceTileIntensity(
            final int sx0, final int sy0, final int sw, final int sh, final ProductData srcData1,
            final ProductData srcData2, final TileIndex srcIndex, final double noDataValue, final Unit.UnitType unit) {

        final double[][] srcTileData = new double[sh][sw];
        final int yMax = sy0 + sh;
        final int xMax = sx0 + sw;

        if (unit == Unit.UnitType.REAL || unit == Unit.UnitType.IMAGINARY) {
            for (int y = sy0; y < yMax; ++y) {
                srcIndex.calculateStride(y);
                final int yy = y - sy0;
                for (int x = sx0; x < xMax; ++x) {
                    final int idx = srcIndex.getIndex(x);
                    final double I = srcData1.getElemDoubleAt(idx);
                    final double Q = srcData2.getElemDoubleAt(idx);
                    if (Double.compare(I, noDataValue) != 0 && Double.compare(Q, noDataValue) != 0) {
                        srcTileData[yy][x - sx0] = I * I + Q * Q;
                    } else {
                        srcTileData[yy][x - sx0] = noDataValue;
                    }
                }
            }
        } else if (unit == Unit.UnitType.AMPLITUDE) {
            for (int y = sy0; y < yMax; ++y) {
                srcIndex.calculateStride(y);
                final int yy = y - sy0;
                for (int x = sx0; x < xMax; ++x) {
                    final double v = srcData1.getElemDoubleAt(srcIndex.getIndex(x));
                    srcTileData[yy][x - sx0] = v * v;
                }
            }
        } else {
            for (int y = sy0; y < yMax; ++y) {
                srcIndex.calculateStride(y);
                final int yy = y - sy0;
                for (int x = sx0; x < xMax; ++x) {
                    srcTileData[yy][x - sx0] = srcData1.getElemDoubleAt(srcIndex.getIndex(x));
                }
            }
        }

        return srcTileData;
    }

    /**
     * Compute the initial seed value for given pixel. The marginal median in a 3x3 neighborhood of the given pixel
     * is computed and used as the seed value.
     *
     * @param tx               X coordinate of a given pixel.
     * @param ty               Y coordinate of a given pixel.
     * @param sx0              X coordinate of the pixel at the upper left corner of the source rectangle
     * @param sy0              Y coordinate of the pixel at the upper left corner of the source rectangle
     * @param sw               Width of the source rectangle
     * @param sh               Height of the source rectangle
     * @param srcTileIntensity Source tile intensity.
     * @param noDataValue      Place holder for no data value.
     * @return                 The computed initial seed value
     */
    private static double getInitialSeed(
            final int tx, final int ty, final int sx0, final int sy0, final int sw, final int sh,
            final double[][] srcTileIntensity, final double noDataValue) {

        final int xMin = Math.max(tx - 1, sx0);
        final int xMax = Math.min(tx + 1, sx0 + sw - 1);
        final int yMin = Math.max(ty - 1, sy0);
        final int yMax = Math.min(ty + 1, sy0 + sh - 1);
        final double[] validSamples = new double[9];

        int k = 0;
        for (int y = yMin; y <= yMax; y++) {
            final int yy = y - sy0;
            for (int x = xMin; x <= xMax; x++) {
                final double v = srcTileIntensity[yy][x - sx0];
                if (Double.compare(v, noDataValue) != 0) {
                    validSamples[k] = v;
                    k++;
                }
            }
        }

        Arrays.sort(validSamples, 0, k);

        return validSamples[k / 2];
    }

    /**
     * Find all pixels in the adaptive neighbourhood of a given pixel.
     *
     * @param xc         X coordinate of the given pixel
     * @param yc         Y coordinate of the given pixel
     * @param sx0        X coordinate of the pixel at the upper left corner of the source rectangle
     * @param sy0        Y coordinate of the pixel at the upper left corner of the source rectangle
     * @param sw         Width of the source rectangle
     * @param sh         Height of the source rectangle
     * @param srcTileIntensity Source tile intensity.
     * @param noDataValue      Place holder for no data value.
     * @param seed       The initial seed value
     * @return anPixelList List of pixels in the adaptive neighbourhood
     */
    private Pix[] getIDANPixels(final int xc, final int yc, final int sx0, final int sy0, final int sw, final int sh,
                                final double[][] srcTileIntensity, final double noDataValue,
                                final double seed) {

        // 1st run of region growing with IDAN50 threshold and initial seed, qualified pixel goes to anPixelList,
        // non-qualified pixel goes to "background pixels" list
        final double threshold50 = (2.0 / 3.0) * sigmaV;
        final java.util.List<Pix> anPixelList = new ArrayList<>(anSize);
        final Pix[] bgPixelList = regionGrowing(
                xc, yc, sx0, sy0, sw, sh, srcTileIntensity, noDataValue, seed, threshold50, anPixelList);

        // update seed with the pixels in AN
        double newSeed = 0.0;
        if (!anPixelList.isEmpty()) {
            for (Pix pixel : anPixelList) {
                newSeed += srcTileIntensity[pixel.y - sy0][pixel.x - sx0];
            }
            newSeed /= anPixelList.size();
        } else {
            newSeed = seed;
        }

        // 2nd run of region growing with IDAN95 threshold, the new seed and "background pixels" i.e. pixels rejected
        // in the 1st run of region growing are checked and added to AN
        final double threshold95 = 2.0 * sigmaV;
        reExamBackgroundPixels(
                sx0, sy0, srcTileIntensity, noDataValue, newSeed, threshold95, anPixelList, bgPixelList);

        if (anPixelList.isEmpty()) {
            return new Pix[]{new Pix(xc, yc)};
        }
        return anPixelList.toArray(new Pix[anPixelList.size()]);
    }

    /**
     * Find pixels in the adaptive neighbourhood (AN) of a given pixel using region growing method.
     *
     * @param xc          X coordinate of the given pixel
     * @param yc          Y coordinate of the given pixel
     * @param sx0         X coordinate of the pixel at the upper left corner of the source rectangle
     * @param sy0         Y coordinate of the pixel at the upper left corner of the source rectangle
     * @param sw          Width of the source rectangle
     * @param sh          Height of the source rectangle
     * @param srcTileIntensity Source tile intensity.
     * @param noDataValue      Place holder for no data value.
     * @param seed        The initial seed value for AN
     * @param threshold   Threshold used in searching for pixels in AN
     * @param anPixelList List of pixels in AN
     * @return bgPixelList List of pixels rejected in searching for AN pixels
     */
    private Pix[] regionGrowing(final int xc, final int yc, final int sx0, final int sy0, final int sw, final int sh,
                                final double[][] srcTileIntensity, final double noDataValue,
                                final double seed, final double threshold, final java.util.List<Pix> anPixelList) {

        final int rc = yc - sy0;
        final int cc = xc - sx0;
        final Map<Integer, Boolean> visited = new HashMap<>(anSize + 8);
        final java.util.List<Pix> bgPixelList = new ArrayList<>(anSize);

        if (Double.compare(srcTileIntensity[rc][cc], noDataValue) != 0 &&
                Math.abs((srcTileIntensity[rc][cc] - seed) / seed) < threshold) {
            anPixelList.add(new Pix(xc, yc));
        } else {
            bgPixelList.add(new Pix(xc, yc));
        }
        visited.put(rc * sw + cc, true);

        final java.util.List<Pix> front = new ArrayList<>(anSize);
        front.add(new Pix(xc, yc));
        final java.util.List<Pix> newfront = new ArrayList<>(anSize);

        final int width = sx0 + sw;
        final int height = sy0 + sh;
        int r, c;
        Integer index;

        while (anPixelList.size() < anSize && !front.isEmpty()) {
            newfront.clear();

            for (final Pix p : front) {

                final int[] x = {p.x - 1, p.x, p.x + 1, p.x - 1, p.x + 1, p.x - 1, p.x, p.x + 1};
                final int[] y = {p.y - 1, p.y - 1, p.y - 1, p.y, p.y, p.y + 1, p.y + 1, p.y + 1};

                for (int i = 0; i < 8; i++) {

                    if (x[i] >= sx0 && x[i] < width && y[i] >= sy0 && y[i] < height) {
                        r = y[i] - sy0;
                        c = x[i] - sx0;
                        index = r * sw + c;
                        if (visited.get(index) == null) {
                            visited.put(index, true);
                            final Pix newPos = new Pix(x[i], y[i]);
                            if (Double.compare(srcTileIntensity[r][c], noDataValue) != 0 &&
                                    Math.abs((srcTileIntensity[r][c] - seed) / seed) < threshold) {
                                anPixelList.add(newPos);
                                newfront.add(newPos);
                            } else {
                                bgPixelList.add(newPos);
                            }
                        }
                    }
                }
                if (anPixelList.size() > anSize) {
                    break;
                }
            }
            front.clear();
            front.addAll(newfront);
        }
        return bgPixelList.toArray(new Pix[bgPixelList.size()]);
    }

    private final static class Pix {
        final int x, y;

        public Pix(final int xx, final int yy) {
            x = xx;
            y = yy;
        }
    }

    /**
     * Re-exam the pixels that are rejected in the region growing process and add them to AN if qualified.
     *
     * @param sx0         X coordinate of the pixel at the upper left corner of the source rectangle
     * @param sy0         Y coordinate of the pixel at the upper left corner of the source rectangle
     * @param srcTileIntensity  Source tile intensity
     * @param noDataValue  No dat value
     * @param seed        The seed value for AN
     * @param threshold   Threshold used in searching for pixels in AN
     * @param anPixelList List of pixels in AN
     * @param bgPixelList List of pixels rejected in searching for AN pixels
     */
    private static void reExamBackgroundPixels(
            final int sx0, final int sy0, final double[][] srcTileIntensity, final double noDataValue,
            final double seed, final double threshold, final java.util.List<Pix> anPixelList,
            final Pix[] bgPixelList) {

        int r, c;
        for (final Pix pixel : bgPixelList) {
            r = pixel.y - sy0;
            c = pixel.x - sx0;
            if (Math.abs((srcTileIntensity[r][c] - seed) / seed) < threshold) {
                anPixelList.add(new Pix(pixel.x, pixel.y));
            }
        }
    }

    /**
     * Compute scale parameter b for MMSE filter.
     *
     * @param sx0         X coordinate of the pixel at the upper left corner of the source rectangle
     * @param sy0         Y coordinate of the pixel at the upper left corner of the source rectangle
     * @param anPixelList List of pixels in AN
     * @param srcTileIntensity Source tile intensity
     * @return The scale parameter b
     */
    private double computeFilterScaleParam(
            final int sx0, final int sy0, final Pix[] anPixelList, final double[][] srcTileIntensity) {

        final double[] anPixels = new double[anPixelList.length];
        int k = 0;
        for (Pix pixel : anPixelList) {
            anPixels[k++] = srcTileIntensity[pixel.y - sy0][pixel.x - sx0];
        }

        return computeMMSEWeight(anPixels, sigmaVSqr);
    }

    /**
     * Compute MMSE filtered value for given pixel.
     *
     * @param x           X coordinate of the given pixel
     * @param y           Y  coordinate of the given pixel
     * @param sx0         X coordinate of the pixel at the upper left corner of the source rectangle
     * @param sy0         Y coordinate of the pixel at the upper left corner of the source rectangle
     * @param anPixelList List of pixels in AN
     * @param srcTileIntensity Source tile intensity
     * @param b           The scale parameter
     * @return The filtered value
     */
    private static double getIDANFilteredValue(
            final int x, final int y, final int sx0, final int sy0, final Pix[] anPixelList,
            final double[][] srcTileIntensity, final Unit.UnitType bandUnit, final double b) {

        if (bandUnit == Unit.UnitType.AMPLITUDE) {
            double mean = 0.0;

            for (final Pix pixel : anPixelList) {
                mean += Math.sqrt(srcTileIntensity[pixel.y - sy0][pixel.x - sx0]);
            }
            mean /= anPixelList.length;

            return mean + b * (Math.sqrt(srcTileIntensity[y - sy0][x - sx0]) - mean);
        } else { // intensity
            double mean = 0.0;

            for (final Pix pixel : anPixelList) {
                mean += srcTileIntensity[pixel.y - sy0][pixel.x - sx0];
            }
            mean /= anPixelList.length;

            return mean + b * (srcTileIntensity[y - sy0][x - sx0] - mean);
        }
    }
}
