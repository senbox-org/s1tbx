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
public class BoxCar implements SpeckleFilter {

    private final PolarimetricSpeckleFilterOp operator;
    private final Product sourceProduct;
    private final Product targetProduct;
    private final PolBandUtils.MATRIX sourceProductType;
    private final PolBandUtils.PolSourceBand[] srcBandList;
    private final int filterSize, halfFilterSize;

    public BoxCar(final PolarimetricSpeckleFilterOp op, final Product srcProduct, final Product trgProduct,
                  PolBandUtils.MATRIX sourceProductType, final PolBandUtils.PolSourceBand[] srcBandList, final int filterSize) {
        this.operator = op;
        this.sourceProduct = srcProduct;
        this.targetProduct = trgProduct;
        this.sourceProductType = sourceProductType;
        this.srcBandList = srcBandList;
        this.filterSize = filterSize;
        this.halfFilterSize = filterSize/2;
    }

    public void computeTiles(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle, final Rectangle sourceRectangle) {
        if (PolBandUtils.isFullPol(sourceProductType)) {
            boxcarFilterFullPol(targetTiles, targetRectangle, sourceRectangle);
        } else if (PolBandUtils.isQuadPol(sourceProductType)) {
            boxcarFilterC3T3C4T4(targetTiles, targetRectangle, sourceRectangle);
        } else if (PolBandUtils.isDualPol(sourceProductType)) {
            boxcarFilterC2(targetTiles, targetRectangle, sourceRectangle);
        } else {
            throw new OperatorException("For Boxcar filtering, only C2, C3, T3, C4 and T4 are supported");
        }
    }

    /**
     * Filter compact polarimetric data with Box Car filter for given tile.
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @param sourceRectangle The area in the source product
     * @throws org.esa.snap.framework.gpf.OperatorException If an error occurs during computation of the filtered value.
     */
    private void boxcarFilterC2(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle,
                                final Rectangle sourceRectangle) {

        final int x0 = targetRectangle.x, y0 = targetRectangle.y;
        final int w = targetRectangle.width,  h = targetRectangle.height;
        final int maxY = y0 + h, maxX = x0 + w;
        final int sourceImageWidth = sourceProduct.getSceneRasterWidth();
        final int sourceImageHeight = sourceProduct.getSceneRasterHeight();
        //System.out.println("boxcar x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final TileIndex trgIndex = new TileIndex(targetTiles.get(targetProduct.getBandAt(0)));

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {
            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
            for (int i = 0; i < bandList.srcBands.length; ++i) {
                sourceTiles[i] = operator.getSourceTile(bandList.srcBands[i], sourceRectangle);
                dataBuffers[i] = sourceTiles[i].getDataBuffer();
            }

            final double[][] Cr = new double[2][2];
            final double[][] Ci = new double[2][2];

            for (int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for (int x = x0; x < maxX; ++x) {
                    final int idx = trgIndex.getIndex(x);

                    DualPolOpUtils.getMeanCovarianceMatrixC2(x, y, halfFilterSize, halfFilterSize, sourceImageWidth,
                            sourceImageHeight, sourceProductType, sourceTiles, dataBuffers, Cr, Ci);

                    for (Band targetBand : bandList.targetBands) {
                        final String targetBandName = targetBand.getName();
                        final ProductData dataBuffer = targetTiles.get(targetBand).getDataBuffer();
                        if (targetBandName.equals("C11")) {
                            dataBuffer.setElemFloatAt(idx, (float) Cr[0][0]);
                        } else if (targetBandName.contains("C12_real")) {
                            dataBuffer.setElemFloatAt(idx, (float) Cr[0][1]);
                        } else if (targetBandName.contains("C12_imag")) {
                            dataBuffer.setElemFloatAt(idx, (float) Ci[0][1]);
                        } else if (targetBandName.equals("C22")) {
                            dataBuffer.setElemFloatAt(idx, (float) Cr[1][1]);
                        }
                    }
                }
            }
        }
    }

    /**
     * Filter full polarimetric data with Box Car filter for given tile.
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @param sourceRectangle The area in the source product
     * @throws org.esa.snap.framework.gpf.OperatorException If an error occurs during computation of the filtered value.
     */
    private void boxcarFilterFullPol(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle,
                                     final Rectangle sourceRectangle) {

        final int x0 = targetRectangle.x, y0 = targetRectangle.y;
        final int w = targetRectangle.width,  h = targetRectangle.height;
        final int maxY = y0 + h, maxX = x0 + w;
        final int sourceImageWidth = sourceProduct.getSceneRasterWidth();
        final int sourceImageHeight = sourceProduct.getSceneRasterHeight();
        //System.out.println("boxcar x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final TileIndex trgIndex = new TileIndex(targetTiles.get(targetProduct.getBandAt(0)));

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {
            final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
            final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];

            for (int i = 0; i < bandList.srcBands.length; ++i) {
                sourceTiles[i] = operator.getSourceTile(bandList.srcBands[i], sourceRectangle);
                dataBuffers[i] = sourceTiles[i].getDataBuffer();
            }

            final TileIndex srcIndex = new TileIndex(sourceTiles[0]);
            final double[][] Tr = new double[3][3];
            final double[][] Ti = new double[3][3];

            for (int y = y0; y < maxY; ++y) {
                trgIndex.calculateStride(y);
                for (int x = x0; x < maxX; ++x) {
                    final int idx = trgIndex.getIndex(x);

                    // todo: Here for every pixel T3 is computed 5 times if the filter size is 5, should save some result
                    PolOpUtils.getMeanCoherencyMatrix(x, y, halfFilterSize, halfFilterSize, sourceImageWidth, sourceImageHeight,
                                                      sourceProductType, srcIndex, dataBuffers, Tr, Ti);

                    for (Band targetBand : bandList.targetBands) {
                        final String targetBandName = targetBand.getName();
                        final ProductData dataBuffer = targetTiles.get(targetBand).getDataBuffer();
                        if (targetBandName.equals("T11") || targetBandName.contains("T11_"))
                            dataBuffer.setElemFloatAt(idx, (float) Tr[0][0]);
                        else if (targetBandName.contains("T12_real"))
                            dataBuffer.setElemFloatAt(idx, (float) Tr[0][1]);
                        else if (targetBandName.contains("T12_imag"))
                            dataBuffer.setElemFloatAt(idx, (float) Ti[0][1]);
                        else if (targetBandName.contains("T13_real"))
                            dataBuffer.setElemFloatAt(idx, (float) Tr[0][2]);
                        else if (targetBandName.contains("T13_imag"))
                            dataBuffer.setElemFloatAt(idx, (float) Ti[0][2]);
                        else if (targetBandName.equals("T22") || targetBandName.contains("T22_"))
                            dataBuffer.setElemFloatAt(idx, (float) Tr[1][1]);
                        else if (targetBandName.contains("T23_real"))
                            dataBuffer.setElemFloatAt(idx, (float) Tr[1][2]);
                        else if (targetBandName.contains("T23_imag"))
                            dataBuffer.setElemFloatAt(idx, (float) Ti[1][2]);
                        else if (targetBandName.equals("T33") || targetBandName.contains("T33_"))
                            dataBuffer.setElemFloatAt(idx, (float) Tr[2][2]);
                    }
                }
            }
        }
    }

    /**
     * Filter C3, T3, C4 or T4 data with Box Car filter for given tile.
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed.
     * @param sourceRectangle The area in the source product
     * @throws org.esa.snap.framework.gpf.OperatorException If an error occurs during computation of the filtered value.
     */
    private void boxcarFilterC3T3C4T4(final Map<Band, Tile> targetTiles, final Rectangle targetRectangle,
                                      final Rectangle sourceRectangle) {

        final int x0 = targetRectangle.x, y0 = targetRectangle.y;
        final int w = targetRectangle.width,  h = targetRectangle.height;
        final int maxY = y0 + h, maxX = x0 + w;
        //System.out.println("boxcar x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final int sx0 = sourceRectangle.x;
        final int sy0 = sourceRectangle.y;
        final int sw = sourceRectangle.width;
        final int sh = sourceRectangle.height;

        final double[] neighborValues = new double[filterSize * filterSize];
        Tile targetTile, sourceTile;

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {
            for (final Band targetBand : bandList.targetBands) {
                targetTile = targetTiles.get(targetBand);
                final ProductData dataBuffer = targetTile.getDataBuffer();
                sourceTile = operator.getSourceTile(sourceProduct.getBand(targetBand.getName()), sourceRectangle);

                for (int y = y0; y < maxY; ++y) {
                    for (int x = x0; x < maxX; ++x) {

                        final int idx = targetTile.getDataBufferIndex(x, y);

                        getNeighborValues(x, y, sx0, sy0, sw, sh, sourceTile, neighborValues);

                        dataBuffer.setElemFloatAt(idx, (float) getMeanValue(neighborValues));
                    }
                }
            }
        }
    }

    /**
     * Get pixel values in a filter size rectanglar region centered at the given pixel.
     *
     * @param x              X coordinate of a given pixel.
     * @param y              Y coordinate of a given pixel.
     * @param sx0            X coordinate of pixel at upper left corner of source tile.
     * @param sy0            Y coordinate of pixel at upper left corner of source tile.
     * @param sw             Source tile width.
     * @param sh             Source tile height.
     * @param sourceTile     The source tile.
     * @param neighborValues Array holding the pixel values.
     * @throws org.esa.snap.framework.gpf.OperatorException If an error occurs in obtaining the pixel values.
     */
    private void getNeighborValues(final int x, final int y, final int sx0, final int sy0, final int sw, final int sh,
                                   final Tile sourceTile, final double[] neighborValues) {

        final ProductData sourceData = sourceTile.getDataBuffer();

        for (int i = 0; i < filterSize; ++i) {

            int xi = x - halfFilterSize + i;
            if (xi < sx0) {
                xi = sx0;
            } else if (xi >= sx0 + sw) {
                xi = sx0 + sw - 1;
            }

            final int stride = i * filterSize;
            for (int j = 0; j < filterSize; ++j) {

                int yj = y - halfFilterSize + j;
                if (yj < sy0) {
                    yj = sy0;
                } else if (yj >= sy0 + sh) {
                    yj = sy0 + sh - 1;
                }

                neighborValues[j + stride] = sourceData.getElemDoubleAt(sourceTile.getDataBufferIndex(xi, yj));
            }
        }
    }
}
