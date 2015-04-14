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

import org.csa.rstb.gpf.PolarimetricSpeckleFilterOp;
import org.esa.s1tbx.dataio.PolBandUtils;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.gpf.TileIndex;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

/**
 * Polarimetric Speckle Filter
 */
public class IDAN implements SpeckleFilter {

    private final PolarimetricSpeckleFilterOp operator;
    private final Product sourceProduct;
    private final Product targetProduct;
    private final PolBandUtils.MATRIX sourceProductType;
    private final PolBandUtils.PolSourceBand[] srcBandList;
    private final int filterSize;

    private final int anSize;
    private final double sigmaV;
    private final double sigmaVSqr;

    public IDAN(final PolarimetricSpeckleFilterOp op, final Product srcProduct, final Product trgProduct,
                final PolBandUtils.MATRIX sourceProductType, final PolBandUtils.PolSourceBand[] srcBandList,
                final int anSize, final int numLooks) {
        this.operator = op;
        this.sourceProduct = srcProduct;
        this.targetProduct = trgProduct;
        this.sourceProductType = sourceProductType;
        this.srcBandList = srcBandList;
        this.filterSize = anSize * 2;  // filterSize in this case is used only in generating source rectangle

        this.anSize = anSize;
        sigmaV = 1.0 / Math.sqrt(numLooks);
        sigmaVSqr = sigmaV * sigmaV;
    }

    public void computeTiles(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle, final Rectangle sourceRectangle) {
        if (sourceProductType == PolBandUtils.MATRIX.FULL ||
                sourceProductType == PolBandUtils.MATRIX.C3 ||
                sourceProductType == PolBandUtils.MATRIX.T3) {
            idanFilter(targetTiles, targetRectangle, sourceRectangle);
        } else if(sourceProductType == PolBandUtils.MATRIX.C2 ||
                sourceProductType == PolBandUtils.MATRIX.DUAL_HH_HV ||
                sourceProductType == PolBandUtils.MATRIX.DUAL_VH_VV ||
                sourceProductType == PolBandUtils.MATRIX.DUAL_HH_VV) {
            idanFilterC2(targetTiles, targetRectangle, sourceRectangle);
        } else {
            throw new OperatorException("For IDAN filtering, only C2, C3 and T3 are currently supported");
        }
    }

    /**
     * Filter full polarimetric data with IDAN filter for given tile.
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @param sourceRectangle The area in the source product
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during computation of the filtered value.
     */
    private void idanFilterC2(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle,
                              final Rectangle sourceRectangle) {

        final int x0 = targetRectangle.x, y0 = targetRectangle.y;
        final int w = targetRectangle.width,  h = targetRectangle.height;
        final int maxY = y0 + h, maxX = x0 + w;
        // System.out.println("idanFilter x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final int sx0 = sourceRectangle.x, sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width, sh = sourceRectangle.height;

        final double[][] data11Real = new double[sh][sw];
        final double[][] data12Real = new double[sh][sw];
        final double[][] data12Imag = new double[sh][sw];
        final double[][] data22Real = new double[sh][sw];
        final double[][] span = new double[sh][sw];

        final TileIndex trgIndex = new TileIndex(targetTiles.get(targetProduct.getBandAt(0)));

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

            for (int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for (int x = x0; x < maxX; ++x) {
                    final int idx = trgIndex.getIndex(x);

                    final Seed seed = getInitialSeed(x, y, sx0, sy0, sw, sh, data11Real, data22Real);

                    final Pix[] anPixelList = getIDANPixels(x, y, sx0, sy0, sw, sh, data11Real, data22Real, seed);

                    final double b = computeFilterScaleParam(sx0, sy0, anPixelList, span);

                    for (final Band targetBand : bandList.targetBands) {
                        final String targetBandName = targetBand.getName();
                        final ProductData dataBuffer = targetTiles.get(targetBand).getDataBuffer();

                        if (targetBandName.contains("C11")) {
                            double value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data11Real, b);
                            dataBuffer.setElemFloatAt(idx, (float) value);
                        } else if (targetBandName.contains("C12_real")) {
                            double value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data12Real, b);
                            dataBuffer.setElemFloatAt(idx, (float) value);
                        } else if (targetBandName.contains("C12_imag")) {
                            double value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data12Imag, b);
                            dataBuffer.setElemFloatAt(idx, (float) value);
                        } else if (targetBandName.contains("C22")) {
                            double value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data22Real, b);
                            dataBuffer.setElemFloatAt(idx, (float) value);
                        }
                    }
                }
            }
        }
    }

    /**
     * Compute the initial seed value for given pixel. The marginal median in a 3x3 neighborhood of the given pixel
     * is computed and used as the seed value.
     *
     * @param xc         X coordinate of the given pixel
     * @param yc         Y coordinate of the given pixel
     * @param sx0        X coordinate of the pixel at the upper left corner of the source rectangle
     * @param sy0        Y coordinate of the pixel at the upper left corner of the source rectangle
     * @param sw         Width of the source rectangle
     * @param sh         Height of the source rectangle
     * @param data11Real Data of the 1st diagonal element in covariance matrix for all pixels in source rectangle
     * @param data22Real Data of the 2nd diagonal element in covariance matrix for all pixels in source rectangle
     * @return seed The computed initial seed value
     */
    private static Seed getInitialSeed(final int xc, final int yc, final int sx0, final int sy0, final int sw,
                                       final int sh, final double[][] data11Real, final double[][] data22Real) {

        // define vector p = [d11 d22], then the seed is the marginal median of all vectors in the 3x3 window
        final double[] d11 = new double[9];
        final double[] d22 = new double[9];

        int r, c;
        int k = 0;
        for (int y = yc - 1; y <= yc + 1; y++) {
            for (int x = xc - 1; x <= xc + 1; x++) {
                if (x >= sx0 && x < sx0 + sw && y >= sy0 && y < sy0 + sh) {
                    r = y - sy0;
                    c = x - sx0;
                    d11[k] = data11Real[r][c];
                    d22[k] = data22Real[r][c];
                    k++;
                }
            }
        }

        Arrays.sort(d11, 0, k);
        Arrays.sort(d22, 0, k);

        final int med = k / 2;
        final Seed seed = new Seed();
        seed.value[0] = d11[med];
        seed.value[1] = d22[med];
        seed.calculateAbsolutes();
        return seed;
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
     * @param data11Real Data of the 1st diagonal element in coherency matrix for all pixels in source rectangle
     * @param data22Real Data of the 2nd diagonal element in coherency matrix for all pixels in source rectangle
     * @param seed       The initial seed value
     * @return anPixelList List of pixels in the adaptive neighbourhood
     */
    private Pix[] getIDANPixels(final int xc, final int yc, final int sx0, final int sy0, final int sw, final int sh,
                                final double[][] data11Real, final double[][] data22Real, final Seed seed) {

        // 1st run of region growing with IDAN50 threshold and initial seed, qualified pixel goes to anPixelList,
        // non-qualified pixel goes to "background pixels" list
        final double threshold50 = 4 / 3 * sigmaV;
        final java.util.List<Pix> anPixelList = new ArrayList<Pix>(anSize);
        final Pix[] bgPixelList = regionGrowing(
                xc, yc, sx0, sy0, sw, sh, data11Real, data22Real, seed, threshold50, anPixelList);

        // update seed with the pixels in AN
        final Seed newSeed = new Seed();
        if (!anPixelList.isEmpty()) {
            for (Pix pixel : anPixelList) {
                newSeed.value[0] += data11Real[pixel.y - sy0][pixel.x - sx0];
                newSeed.value[1] += data22Real[pixel.y - sy0][pixel.x - sx0];
            }
            newSeed.value[0] /= anPixelList.size();
            newSeed.value[1] /= anPixelList.size();
        } else {
            newSeed.value[0] = seed.value[0];
            newSeed.value[1] = seed.value[1];
        }
        newSeed.calculateAbsolutes();

        // 2nd run of region growing with IDAN95 threshold, the new seed and "background pixels" i.e. pixels rejected
        // in the 1st run of region growing are checked and added to AN
        final double threshold95 = 4 * sigmaV;
        reExamBackgroundPixels(sx0, sy0, data11Real, data22Real, newSeed, threshold95, anPixelList, bgPixelList);

        if (anPixelList.isEmpty()) {
            return new Pix[]{new Pix(xc, yc)};
        }
        return anPixelList.toArray(new Pix[anPixelList.size()]);
    }

    /**
    * Re-exam the pixels that are rejected in the region growing process and add them to AN if qualified.
    *
    * @param sx0         X coordinate of the pixel at the upper left corner of the source rectangle
    * @param sy0         Y coordinate of the pixel at the upper left corner of the source rectangle
    * @param data11Real  Data of the 1st diagonal element in coherency matrix for all pixels in source rectangle
    * @param data22Real  Data of the 2nd diagonal element in coherency matrix for all pixels in source rectangle
    * @param seed        The seed value for AN
    * @param threshold   Threshold used in searching for pixels in AN
    * @param anPixelList List of pixels in AN
    * @param bgPixelList List of pixels rejected in searching for AN pixels
    */
    private static void reExamBackgroundPixels(final int sx0, final int sy0, final double[][] data11Real,
                                               final double[][] data22Real, final Seed seed, final double threshold,
                                               final java.util.List<Pix> anPixelList, final Pix[] bgPixelList) {
        int r, c;
        for (final Pix pixel : bgPixelList) {
            r = pixel.y - sy0;
            c = pixel.x - sx0;
            if (distance(data11Real[r][c], data22Real[r][c], seed) < threshold) {
                anPixelList.add(new Pix(pixel.x, pixel.y));
            }
        }
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
     * @param data11Real  Data of the 1st diagonal element in coherency matrix for all pixels in source rectangle
     * @param data22Real  Data of the 2nd diagonal element in coherency matrix for all pixels in source rectangle
     * @param seed        The initial seed value for AN
     * @param threshold   Threshold used in searching for pixels in AN
     * @param anPixelList List of pixels in AN
     * @return bgPixelList List of pixels rejected in searching for AN pixels
     */
    private Pix[] regionGrowing(final int xc, final int yc, final int sx0, final int sy0, final int sw, final int sh,
                                final double[][] data11Real, final double[][] data22Real, final Seed seed,
                                final double threshold, final java.util.List<Pix> anPixelList) {

        final int rc = yc - sy0;
        final int cc = xc - sx0;
        final Map<Integer, Boolean> visited = new HashMap<Integer, Boolean>(anSize + 8);
        final java.util.List<Pix> bgPixelList = new ArrayList<Pix>(anSize);

        if (distance(data11Real[rc][cc], data22Real[rc][cc], seed) < threshold) {
            anPixelList.add(new Pix(xc, yc));
        } else {
            bgPixelList.add(new Pix(xc, yc));
        }
        visited.put(rc * sw + cc, true);

        final java.util.List<Pix> front = new ArrayList<Pix>(anSize);
        front.add(new Pix(xc, yc));
        final java.util.List<Pix> newfront = new ArrayList<Pix>(anSize);

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
                            if (distance(data11Real[r][c], data22Real[r][c], seed) < threshold) {
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

    /**
     * Filter full polarimetric data with IDAN filter for given tile.
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @param sourceRectangle The area in the source product
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during computation of the filtered value.
     */
    private void idanFilter(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle,
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

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {
            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
            for (int i = 0; i < bandList.srcBands.length; ++i) {
                sourceTiles[i] = operator.getSourceTile(bandList.srcBands[i], sourceRectangle);
                dataBuffers[i] = sourceTiles[i].getDataBuffer();
            }

            final Tile srcTile = operator.getSourceTile(bandList.srcBands[0], sourceRectangle);
            createT3SpanImage(srcTile, sourceProductType, sourceRectangle, dataBuffers,
                              data11Real, data12Real, data12Imag, data13Real, data13Imag,
                              data22Real, data23Real, data23Imag, data33Real, span);

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

            for (int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for (int x = x0; x < maxX; ++x) {
                    final int idx = trgIndex.getIndex(x);

                    final Seed seed = getInitialSeed(x, y, sx0, sy0, sw, sh, data11Real, data22Real, data33Real);

                    final Pix[] anPixelList = getIDANPixels(x, y, sx0, sy0, sw, sh,
                                                            data11Real, data22Real, data33Real, seed);

                    final double b = computeFilterScaleParam(sx0, sy0, anPixelList, span);

                    int i = 0;
                    double value = 0.0;
                    for (final T3Elem elem : T3Elem.values()) {
                        switch (elem) {
                            case T11:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data11Real, b);
                                i = 0;
                                break;

                            case T12_real:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data12Real, b);
                                i = 1;
                                break;

                            case T12_imag:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data12Imag, b);
                                i = 2;
                                break;

                            case T13_real:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data13Real, b);
                                i = 3;
                                break;

                            case T13_imag:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data13Imag, b);
                                i = 4;
                                break;

                            case T22:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data22Real, b);
                                i = 5;
                                break;

                            case T23_real:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data23Real, b);
                                i = 6;
                                break;

                            case T23_imag:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data23Imag, b);
                                i = 7;
                                break;

                            case T33:
                                value = getIDANFilteredValue(x, y, sx0, sy0, anPixelList, data33Real, b);
                                i = 8;
                                break;

                            default:
                                break;
                        }

                        targetDataBuffers[i].setElemFloatAt(idx, (float) value);
                    }
                }
            }
        }
    }

    /**
     * Compute the initial seed value for given pixel. The marginal median in a 3x3 neighborhood of the given pixel
     * is computed and used as the seed value.
     *
     * @param xc         X coordinate of the given pixel
     * @param yc         Y coordinate of the given pixel
     * @param sx0        X coordinate of the pixel at the upper left corner of the source rectangle
     * @param sy0        Y coordinate of the pixel at the upper left corner of the source rectangle
     * @param sw         Width of the source rectangle
     * @param sh         Height of the source rectangle
     * @param data11Real Data of the 1st diagonal element in coherency matrix for all pixels in source rectangle
     * @param data22Real Data of the 2nd diagonal element in coherency matrix for all pixels in source rectangle
     * @param data33Real Data of the 3rd diagonal element in coherency matrix for all pixels in source rectangle
     * @return seed The computed initial seed value
     */
    private static Seed getInitialSeed(final int xc, final int yc, final int sx0, final int sy0, final int sw, final int sh,
                                       final double[][] data11Real, final double[][] data22Real, final double[][] data33Real) {

        // define vector p = [d11 d22 d33], then the seed is the marginal median of all vectors in the 3x3 window
        final double[] d11 = new double[9];
        final double[] d22 = new double[9];
        final double[] d33 = new double[9];

        int r, c;
        int k = 0;
        for (int y = yc - 1; y <= yc + 1; y++) {
            for (int x = xc - 1; x <= xc + 1; x++) {
                if (x >= sx0 && x < sx0 + sw && y >= sy0 && y < sy0 + sh) {
                    r = y - sy0;
                    c = x - sx0;
                    d11[k] = data11Real[r][c];
                    d22[k] = data22Real[r][c];
                    d33[k] = data33Real[r][c];
                    k++;
                }
            }
        }

        Arrays.sort(d11, 0, k);
        Arrays.sort(d22, 0, k);
        Arrays.sort(d33, 0, k);

        final int med = k / 2;
        final Seed seed = new Seed();
        seed.value[0] = d11[med];
        seed.value[1] = d22[med];
        seed.value[2] = d33[med];
        seed.calculateAbsolutes();
        return seed;
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
     * @param data11Real Data of the 1st diagonal element in coherency matrix for all pixels in source rectangle
     * @param data22Real Data of the 2nd diagonal element in coherency matrix for all pixels in source rectangle
     * @param data33Real Data of the 3rd diagonal element in coherency matrix for all pixels in source rectangle
     * @param seed       The initial seed value
     * @return anPixelList List of pixels in the adaptive neighbourhood
     */
    private Pix[] getIDANPixels(final int xc, final int yc, final int sx0, final int sy0, final int sw, final int sh,
                                final double[][] data11Real, final double[][] data22Real, final double[][] data33Real,
                                final Seed seed) {

        // 1st run of region growing with IDAN50 threshold and initial seed, qualified pixel goes to anPixelList,
        // non-qualified pixel goes to "background pixels" list
        final double threshold50 = 2 * sigmaV;
        final java.util.List<Pix> anPixelList = new ArrayList<>(anSize);
        final Pix[] bgPixelList = regionGrowing(xc, yc, sx0, sy0, sw, sh, data11Real, data22Real, data33Real,
                                                seed, threshold50, anPixelList);

        // update seed with the pixels in AN
        final Seed newSeed = new Seed();
        if (!anPixelList.isEmpty()) {
            for (Pix pixel : anPixelList) {
                newSeed.value[0] += data11Real[pixel.y - sy0][pixel.x - sx0];
                newSeed.value[1] += data22Real[pixel.y - sy0][pixel.x - sx0];
                newSeed.value[2] += data33Real[pixel.y - sy0][pixel.x - sx0];
            }
            newSeed.value[0] /= anPixelList.size();
            newSeed.value[1] /= anPixelList.size();
            newSeed.value[2] /= anPixelList.size();
        } else {
            newSeed.value[0] = seed.value[0];
            newSeed.value[1] = seed.value[1];
            newSeed.value[2] = seed.value[2];
        }
        newSeed.calculateAbsolutes();

        // 2nd run of region growing with IDAN95 threshold, the new seed and "background pixels" i.e. pixels rejected
        // in the 1st run of region growing are checked and added to AN
        final double threshold95 = 6 * sigmaV;
        reExamBackgroundPixels(sx0, sy0, data11Real, data22Real, data33Real, newSeed, threshold95,
                               anPixelList, bgPixelList);

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
     * @param data11Real  Data of the 1st diagonal element in coherency matrix for all pixels in source rectangle
     * @param data22Real  Data of the 2nd diagonal element in coherency matrix for all pixels in source rectangle
     * @param data33Real  Data of the 3rd diagonal element in coherency matrix for all pixels in source rectangle
     * @param seed        The initial seed value for AN
     * @param threshold   Threshold used in searching for pixels in AN
     * @param anPixelList List of pixels in AN
     * @return bgPixelList List of pixels rejected in searching for AN pixels
     */
    private Pix[] regionGrowing(final int xc, final int yc, final int sx0, final int sy0, final int sw, final int sh,
                                final double[][] data11Real, final double[][] data22Real, final double[][] data33Real,
                                final Seed seed, final double threshold, final java.util.List<Pix> anPixelList) {

        final int rc = yc - sy0;
        final int cc = xc - sx0;
        final Map<Integer, Boolean> visited = new HashMap<>(anSize + 8);
        final java.util.List<Pix> bgPixelList = new ArrayList<>(anSize);

        if (distance(data11Real[rc][cc], data22Real[rc][cc], data33Real[rc][cc], seed) < threshold) {
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
                            if (distance(data11Real[r][c], data22Real[r][c], data33Real[r][c], seed) < threshold) {
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
     * Cmpute distance between vector p and a given seed vector.
     *
     * @param p0   Vector
     * @param p1   Vector
     * @param seed Vector
     * @return Distance
     */
    private static double distance(final double p0, final double p1, final Seed seed) {
        return Math.abs(p0 - seed.value[0]) / seed.absValue[0] +
                Math.abs(p1 - seed.value[1]) / seed.absValue[1];
    }

    /**
     * Cmpute distance between vector p and a given seed vector.
     *
     * @param p0   Vector
     * @param p1   Vector
     * @param p2   Vector
     * @param seed Vector
     * @return Distance
     */
    private static double distance(final double p0, final double p1, final double p2, final Seed seed) {
        return Math.abs(p0 - seed.value[0]) / seed.absValue[0] +
                Math.abs(p1 - seed.value[1]) / seed.absValue[1] +
                Math.abs(p2 - seed.value[2]) / seed.absValue[2];
    }

    /**
     * Re-exam the pixels that are rejected in the region growing process and add them to AN if qualified.
     *
     * @param sx0         X coordinate of the pixel at the upper left corner of the source rectangle
     * @param sy0         Y coordinate of the pixel at the upper left corner of the source rectangle
     * @param data11Real  Data of the 1st diagonal element in coherency matrix for all pixels in source rectangle
     * @param data22Real  Data of the 2nd diagonal element in coherency matrix for all pixels in source rectangle
     * @param data33Real  Data of the 3rd diagonal element in coherency matrix for all pixels in source rectangle
     * @param seed        The seed value for AN
     * @param threshold   Threshold used in searching for pixels in AN
     * @param anPixelList List of pixels in AN
     * @param bgPixelList List of pixels rejected in searching for AN pixels
     */
    private static void reExamBackgroundPixels(final int sx0, final int sy0, final double[][] data11Real,
                                               final double[][] data22Real, final double[][] data33Real,
                                               final Seed seed, final double threshold,
                                               final java.util.List<Pix> anPixelList, final Pix[] bgPixelList) {
        int r, c;
        for (final Pix pixel : bgPixelList) {
            r = pixel.y - sy0;
            c = pixel.x - sx0;
            if (distance(data11Real[r][c], data22Real[r][c], data33Real[r][c], seed) < threshold) {
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
     * @param span        Span image in source rectangle
     * @return The scale parameter b
     */
    private double computeFilterScaleParam(
            final int sx0, final int sy0, final Pix[] anPixelList, final double[][] span) {

        final double[] spanPixels = new double[anPixelList.length];
        int k = 0;
        for (Pix pixel : anPixelList) {
            spanPixels[k++] = span[pixel.y - sy0][pixel.x - sx0];
        }

        return computeMMSEWeight(spanPixels, sigmaVSqr);
    }

    /**
     * Compute MMSE filtered value for given pixel.
     *
     * @param x           X coordinate of the given pixel
     * @param y           Y  coordinate of the given pixel
     * @param sx0         X coordinate of the pixel at the upper left corner of the source rectangle
     * @param sy0         Y coordinate of the pixel at the upper left corner of the source rectangle
     * @param anPixelList List of pixels in AN
     * @param data        Data in source rectangle
     * @param b           The scale parameter
     * @return The filtered value
     */
    private static double getIDANFilteredValue(final int x, final int y, final int sx0, final int sy0,
                                               final Pix[] anPixelList, final double[][] data, final double b) {

        double mean = 0.0;
        for (final Pix pixel : anPixelList) {
            mean += data[pixel.y - sy0][pixel.x - sx0];
        }
        mean /= anPixelList.length;

        return mean + b * (data[y - sy0][x - sx0] - mean);
    }

    private static class Seed {
        final double[] value = new double[3];
        final double[] absValue = new double[3];

        public void calculateAbsolutes() {
            absValue[0] = Math.abs(value[0]);
            absValue[1] = Math.abs(value[1]);
            absValue[2] = Math.abs(value[2]);
        }
    }
}
