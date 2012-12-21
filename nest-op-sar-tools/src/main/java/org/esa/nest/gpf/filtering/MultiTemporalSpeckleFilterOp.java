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
package org.esa.nest.gpf.filtering;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Applies Multitemporal Speckle Filtering to multitemporal images.
 *
 * For a sequence of n registered multitemporal PRI images, with intensity at position (x, y) in image k
 * denoted by Ik(x, y), the goal of temporal filtering is to combine them linearly such that the n output
 * images Jk(x, y) meeting the following two conditions:
 *
 * 1. Jk is unbiased (i.e. E[Jk] = E[Ik], where E[] denotes expected value, so that the filtering does not
 *    distort the sigma0 values).
 *
 * 2. Jk has minimum variance, so that speckle is minimized. 
 *
 * The following equation has been implemented:
 *
 *    Jk(x, y) = E[Ik]*(I1(x, y)/E[I1] + ... + In(x, y)/E[In])/n
 *
 * where E[I] is the local mean value of pixels in a user selected window centered at (x, y) in image I.
 * The window size can be 3x3, 5x5, 7x7, 9x9 or 11x11.
 * 
 * The operator has the following two preprocessing steps:
 *
 * 1. The first step is calibration in which ?0 is derived from the digital number at each pixel. This
 *    ensures that values of from different times and in different parts of the image are comparable.
 *
 * 2. The second is registration of the images in the multitemporal sequence.
 *
 * Here it is assumed that preprocessing has been performed before applying this operator. The input to
 * the operator is assumed to be a product with multiple calibrated and co-registrated bands.
 *
 * Reference:
 * [1] S. Quegan, T. L. Toan, J. J. Yu, F. Ribbes and N. Floury, “Multitemporal ERS SAR Analysis Applied to
 * Forest Mapping”, IEEE Transactions on Geoscience and Remote Sensing, vol. 38, no. 2, March 2000.
 */

@OperatorMetadata(alias="Multi-Temporal-Speckle-Filter",
                  category = "SAR Tools\\Speckle Filtering",
                  description = "Speckle Reduction using Multitemporal Filtering")
public class MultiTemporalSpeckleFilterOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct = null;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band", 
            rasterDataNodeType = Band.class, label="Source Bands")
    private String[] sourceBandNames;

    @Parameter(valueSet = {WINDOW_SIZE_3x3, WINDOW_SIZE_5x5, WINDOW_SIZE_7x7, WINDOW_SIZE_9x9, WINDOW_SIZE_11x11},
               defaultValue = WINDOW_SIZE_3x3, label="Window Size")
    private String windowSize = WINDOW_SIZE_3x3;

    private int halfWindowWidth = 0;
    private int halfWindowHeight = 0;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;

    private static final String WINDOW_SIZE_3x3 = "3x3";
    private static final String WINDOW_SIZE_5x5 = "5x5";
    private static final String WINDOW_SIZE_7x7 = "7x7";
    private static final String WINDOW_SIZE_9x9 = "9x9";
    private static final String WINDOW_SIZE_11x11 = "11x11";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public MultiTemporalSpeckleFilterOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            targetProduct = new Product(sourceProduct.getName(),
                                        sourceProduct.getProductType(),
                                        sourceProduct.getSceneRasterWidth(),
                                        sourceProduct.getSceneRasterHeight());

            OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

            addSelectedBands();

            // The tile width has to be the image width, otherwise the index calculation in the last tile is not correct.
            //targetProduct.setPreferredTileSize(targetProduct.getSceneRasterWidth(), 50);

            int windowWidth = 0;
            int windowHeight = 0;
            if (windowSize.equals(WINDOW_SIZE_3x3)) {
                windowWidth = 3;
                windowHeight = 3;
            } else if (windowSize.equals(WINDOW_SIZE_5x5)) {
                windowWidth = 5;
                windowHeight = 5;
            } else if (windowSize.equals(WINDOW_SIZE_7x7)) {
                windowWidth = 7;
                windowHeight = 7;
            } else if (windowSize.equals(WINDOW_SIZE_9x9)) {
                windowWidth = 9;
                windowHeight = 9;
            } else if (windowSize.equals(WINDOW_SIZE_11x11)) {
                windowWidth = 11;
                windowHeight = 11;
            } else {
                throw new OperatorException("Unknown filter size: " + windowSize);
            }

            halfWindowWidth = windowWidth/2;
            halfWindowHeight = windowHeight/2;
        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Add user selected bands to target product.
     */
    private void addSelectedBands() {

        if(sourceBandNames == null || sourceBandNames.length == 0 && OperatorUtils.isComplex(sourceProduct)) {
            final Band[] bands = sourceProduct.getBands();
            final List<String> bandNameList = new ArrayList<String>(sourceProduct.getNumBands());
            for (Band band : bands) {
                if(band.getUnit().contains("intensity"))
                    bandNameList.add(band.getName());
            }
            sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);
        }
        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames);

        if (sourceBands.length <= 1) {
            throw new OperatorException("Multitemporal filtering cannot be applied with one source band. Select more bands.");
        }

        for (Band srcBand : sourceBands) {
            final String unit = srcBand.getUnit();
            if(unit == null) {
                throw new OperatorException("band " + srcBand.getName() + " requires a unit");
            }

            if (unit.contains(Unit.PHASE) || unit.contains(Unit.IMAGINARY) || unit.contains(Unit.REAL)) {
                throw new OperatorException("Please select amplitude or intensity bands.");
            } else {
                final Band targetBand = new Band(srcBand.getName(),
                                                 ProductData.TYPE_FLOAT32,
                                                 sourceProduct.getSceneRasterWidth(),
                                                 sourceProduct.getSceneRasterHeight());

                targetBand.setUnit(unit);
                targetProduct.addBand(targetBand);
            }
        }
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w  = targetRectangle.width;
        final int h  = targetRectangle.height;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);
        
        final Band[] targetBands = targetProduct.getBands();
        final int numBands = targetBands.length;
        final ProductData[] targetData = new ProductData[numBands];
        for (int i = 0; i < numBands; i++) {
            Tile targetTile = targetTiles.get(targetBands[i]);
            targetData[i] = targetTile.getDataBuffer();
        }

        final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);

        final Tile[] sourceTile = new Tile[numBands];
        final ProductData[] sourceData = new ProductData[numBands];
        final double[] bandNoDataValues = new double[numBands];
        for (int i = 0; i < numBands; i++) {
            final Band srcBand = sourceProduct.getBand(targetBands[i].getName());
            sourceTile[i] = getSourceTile(srcBand, sourceRectangle);
            sourceData[i] = sourceTile[i].getDataBuffer();
            bandNoDataValues[i] = srcBand.getNoDataValue();
        }

        final double[] localMeans = new double[numBands];
        double srcDataValue = 0.0;
        final int yMax = y0 + h;
        for(int y = y0; y < yMax; y++) {
            final int xMax = x0 + w;
            for (int x = x0; x < xMax; x++) {

                final int sourceIndex = sourceTile[0].getDataBufferIndex(x, y);

                double sum = 0.0;
                int n = 0;
                for (int i = 0; i < numBands; i++) {
                    srcDataValue = sourceData[i].getElemDoubleAt(sourceIndex);
                    if (srcDataValue == bandNoDataValues[i]) {
                        localMeans[i] = bandNoDataValues[i];
                        continue;
                    }

                    localMeans[i] = computeLocalMean(x, y, sourceTile[i], sourceData[i], bandNoDataValues[i]);

                    if (localMeans[i] != 0.0) {
                        sum += sourceData[i].getElemDoubleAt(sourceIndex) / localMeans[i];
                    }
                    n++;
                }
                if (n > 0) {
                    sum /= n;
                }

                final int targetIndex = targetTiles.get(targetBands[0]).getDataBufferIndex(x, y);
                for (int i = 0; i < numBands; i++) {
                    if (localMeans[i] != bandNoDataValues[i]) {
                        targetData[i].setElemDoubleAt(targetIndex, sum * localMeans[i]);
                    } else {
                        targetData[i].setElemDoubleAt(targetIndex, bandNoDataValues[i]);
                    }
                }
            }
        }
    }

    /**
     * Get source tile rectangle.
     * @param tx0 X coordinate for the upper left corner pixel in the target tile.
     * @param ty0 Y coordinate for the upper left corner pixel in the target tile.
     * @param tw The target tile width.
     * @param th The target tile height.
     * @return The source tile rectangle.
     */
    private Rectangle getSourceRectangle(final int tx0, final int ty0, final int tw, final int th) {
        final int x0 = Math.max(0, tx0 - halfWindowWidth);
        final int y0 = Math.max(0, ty0 - halfWindowHeight);
        final int xMax = Math.min(tx0 + tw - 1 + halfWindowWidth, sourceImageWidth);
        final int yMax = Math.min(ty0 + th - 1 + halfWindowHeight, sourceImageHeight);
        final int w = xMax - x0 + 1;
        final int h = yMax - y0 + 1;
        return new Rectangle(x0, y0, w, h);
    }

    /**
     * Compute mean value for pixels in a window with given center.
     * @param xc X coordinate of the center pixel.
     * @param yc Y coordinate of the center pixel.
     * @param srcTile Source tile.
     * @param srcData Source data.
     * @param noDataValue The noDataValue for source band.
     * @return The mean value.
     */
    private double computeLocalMean(int xc, int yc, Tile srcTile, ProductData srcData, double noDataValue) {
        final int x0 = Math.max(0, xc - halfWindowWidth);
        final int y0 = Math.max(0, yc - halfWindowHeight);
        final int xMax = Math.min(xc + halfWindowWidth, sourceImageWidth-1);
        final int yMax = Math.min(yc + halfWindowHeight, sourceImageHeight-1);

        double mean = 0.0;
        double value = 0.0;
        int n = 0;
        for (int y = y0; y < yMax; y++) {
            for (int x = x0; x < xMax; x++) {
                final int index = srcTile.getDataBufferIndex(x, y);
                value = srcData.getElemDoubleAt(index);
                if (value != noDataValue) {
                    mean += value;
                    n++;
                }
            }
        }
        return mean/n;
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(MultiTemporalSpeckleFilterOp.class);
        }
    }
}

