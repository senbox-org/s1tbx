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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import jj2000.j2k.util.StringFormatException;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.eo.Constants;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The forest area detection operator.
 *
 * The operator implements the algorithm given in [1]. It is assumed that the input source
 * product has already been calibrated, speckle filtered, multilooked and terrain corrected.
 *
 * [1] F. Ling, R. Leiterer, Y. Huang, J. Reiche and Z. Li, "Forest Change Mapping in
 *     Northeast China Using SAR and InSAR Data", ISRSE 34, Sydney, Australia, 2011.
 */

@OperatorMetadata(alias = "Forest-Area-Detection",
        category = "Feature Extraction",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description = "Detect forest area.")
public class ForestAreaDetectionOp extends Operator {

    @SourceProduct(alias="source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct = null;

    @Parameter(valueSet = {WINDOW_SIZE_3x3, WINDOW_SIZE_5x5, WINDOW_SIZE_7x7, WINDOW_SIZE_9x9},
            defaultValue = WINDOW_SIZE_3x3, label="Window Size")
    private String windowSizeStr = WINDOW_SIZE_3x3;

    @Parameter(description = "The lower bound for ratio image", interval = "(0, *)", defaultValue = "3.76",
                label="Ratio lower bound (dB)")
    private double T_Ratio_Low = 3.76;

    @Parameter(description = "The upper bound for ratio image", interval = "(0, *)", defaultValue = "6.55",
                label="Ratio upper bound (dB)")
    private double T_Ratio_High = 6.55;

    @Parameter(description = "The lower bound for HV image", interval = "(-30, *)", defaultValue = "-13.85",
                label="HV lower bound (dB)")
    private double T_HV_Low = -13.85;

    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private int windowSize = 0;
    private int halfWindowSize = 0;

    private String[] sourceBandNames = new String[2];

    public static final String FOREST_MASK_NAME = "forest_mask";

    private static final String WINDOW_SIZE_3x3 = "3x3";
    private static final String WINDOW_SIZE_5x5 = "5x5";
    private static final String WINDOW_SIZE_7x7 = "7x7";
    private static final String WINDOW_SIZE_9x9 = "9x9";
    /*
    private static double T_Ratio_Low = 3.76; // dB
    private static double T_Ratio_High = 6.55; // dB
    private static double T_HV_Low = -13.85 ; // dB
    private static double T_HV_High = -10.85 ; // dB
    */

    @Override
    public void initialize() throws OperatorException {

        try {

            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            setWindowSize();

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Set Window size.
     */
    private void setWindowSize() {

        switch (windowSizeStr) {
            case WINDOW_SIZE_3x3:
                windowSize = 3;
                break;
            case WINDOW_SIZE_5x5:
                windowSize = 5;
                break;
            case WINDOW_SIZE_7x7:
                windowSize = 7;
                break;
            case WINDOW_SIZE_9x9:
                windowSize = 9;
                break;
            default:
                throw new OperatorException("Unknown window size: " + windowSize);
        }

        halfWindowSize = windowSize/2;
    }

    /**
     * Create target product.
     * @throws Exception The exception.
     */
    private void createTargetProduct() throws Exception {

        targetProduct = new Product(sourceProduct.getName(),
                                    sourceProduct.getProductType(),
                                    sourceImageWidth,
                                    sourceImageHeight);

        OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

        addSelectedBands();
    }

    /**
     * Add the user selected bands to target product.
     * @throws OperatorException The exceptions.
     */
    private void addSelectedBands() throws OperatorException {

        boolean hasHH = false, hasHV = false;
        final Band[] bands = sourceProduct.getBands();
        for (Band band : bands) {
            final String bandName = band.getName();
            if(!hasHH && band.getUnit() != null && band.getUnit().equals(Unit.INTENSITY) && bandName.contains("HH")) {
                sourceBandNames[0] = bandName;
                hasHH = true;
            }

            if(!hasHV && band.getUnit() != null && band.getUnit().equals(Unit.INTENSITY) && bandName.contains("HV")) {
                sourceBandNames[1] = bandName;
                hasHV = true;
            }

            if (hasHH && hasHV) {
                break;
            }
        }

        if (!hasHH || !hasHV) {
            throw new OperatorException("The source product should be processed and have intensity HH, HV bands");
        }

        for (String bandName : sourceProduct.getBandNames()) {
            ProductUtils.copyBand(bandName, sourceProduct, bandName, targetProduct, true);
        }

        final Band targetBandMask = new Band(FOREST_MASK_NAME,
                                             ProductData.TYPE_INT8,
                                             sourceImageWidth,
                                             sourceImageHeight);

        targetBandMask.setNoDataValue(-1);
        targetBandMask.setNoDataValueUsed(true);
        targetBandMask.setUnit(Unit.AMPLITUDE);
        targetProduct.addBand(targetBandMask);
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {

        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int tx0 = targetTileRectangle.x;
            final int ty0 = targetTileRectangle.y;
            final int tw  = targetTileRectangle.width;
            final int th  = targetTileRectangle.height;
            final ProductData trgData = targetTile.getDataBuffer();
            //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

            final Rectangle sourceTileRectangle = getSourceTileRectangle(tx0, ty0, tw, th);
            final Band sourceBandHH = sourceProduct.getBand(sourceBandNames[0]);
            final Band sourceBandHV = sourceProduct.getBand(sourceBandNames[1]);
            final Tile sourceTileHH = getSourceTile(sourceBandHH, sourceTileRectangle);
            final Tile sourceTileHV = getSourceTile(sourceBandHV, sourceTileRectangle);
            final ProductData srcDataHH = sourceTileHH.getDataBuffer();
            final ProductData srcDataHV = sourceTileHV.getDataBuffer();
            final double noDataValue = sourceBandHH.getNoDataValue();

            final TileIndex trgIndex = new TileIndex(targetTile);
            final TileIndex srcIndex = new TileIndex(sourceTileHH);    // src and trg tile are different size

            final int maxy = ty0 + th;
            final int maxx = tx0 + tw;

            double vHVDB, vRatioDB;
            for (int ty = ty0; ty < maxy; ty++) {
                trgIndex.calculateStride(ty);
                srcIndex.calculateStride(ty);
                for (int tx = tx0; tx < maxx; tx++) {
                    final int trgIdx = trgIndex.getIndex(tx);
                    final int srcIdx = srcIndex.getIndex(tx);

                    final double vHH = srcDataHH.getElemDoubleAt(srcIdx);
                    final double vHV = srcDataHV.getElemDoubleAt(srcIdx);
                    if (vHH == noDataValue || vHV == noDataValue) {
                        trgData.setElemIntAt(trgIdx, -1);
                        continue;
                    }

                    final double vRatio = computeHHHVRatio(
                            tx, ty, sourceTileHH, srcDataHH, sourceTileHV, srcDataHV, noDataValue);

                    if (vRatio == noDataValue) {
                        trgData.setElemIntAt(trgIdx, -1);
                        continue;
                    }

                    vRatioDB = 10.0*Math.log10(Math.max(vRatio, Constants.EPS));
                    vHVDB = 10.0*Math.log10(Math.max(vHV, Constants.EPS));

                    int maskBit = 0;
                    if (vRatioDB > T_Ratio_Low && vRatioDB < T_Ratio_High && vHVDB > T_HV_Low) {
                        maskBit = 1;
                    }

                    trgData.setElemIntAt(trgIdx, maskBit);
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Get source tile rectangle.
     * @param x0 X coordinate of pixel at the upper left corner of the target tile.
     * @param y0 Y coordinate of pixel at the upper left corner of the target tile.
     * @param w The width of the target tile.
     * @param h The height of the target tile.
     * @return The source tile rectangle.
     */
    private Rectangle getSourceTileRectangle(int x0, int y0, int w, int h) {

        int sx0 = x0;
        int sy0 = y0;
        int sw = w;
        int sh = h;

        if (x0 >= halfWindowSize) {
            sx0 -= halfWindowSize;
            sw += halfWindowSize;
        }

        if (y0 >= halfWindowSize) {
            sy0 -= halfWindowSize;
            sh += halfWindowSize;
        }

        if (x0 + w + halfWindowSize <= sourceImageWidth) {
            sw += halfWindowSize;
        }

        if (y0 + h + halfWindowSize <= sourceImageHeight) {
            sh += halfWindowSize;
        }

        return new Rectangle(sx0, sy0, sw, sh);
    }

    /**
     * Compute local coefficient of variance.
     * @param tx The x coordinate of the central pixel of the sliding window.
     * @param ty The y coordinate of the central pixel of the sliding window.
     * @param sourceTileHH The source image tile for HH band.
     * @param srcDataHH The source image data for HH band.
     * @param sourceTileHV The source image tile for HV band.
     * @param srcDataHV The source image data for HV band.
     * @param noDataValue the place holder for no data
     * @return The local coefficient of variance.
     */
    private double computeHHHVRatio(final int tx, final int ty, final Tile sourceTileHH, final ProductData srcDataHH,
                                    final Tile sourceTileHV, final ProductData srcDataHV, final double noDataValue) {

        final double[] samplesHH = new double[windowSize*windowSize];
        final double[] samplesHV = new double[windowSize*windowSize];

        final int numSamplesHH = getSamples(tx, ty, noDataValue, sourceTileHH, srcDataHH, samplesHH);
        if (numSamplesHH == 0) {
            return noDataValue;
        }

        final int numSamplesHV = getSamples(tx, ty, noDataValue, sourceTileHV, srcDataHV, samplesHV);
        if (numSamplesHV == 0) {
            return noDataValue;
        }

        final double meanHH = getMeanValue(samplesHH, numSamplesHH);

        final double meanHV = getMeanValue(samplesHV, numSamplesHV);

        if (meanHV == 0.0) {
            return noDataValue;
        }

        return meanHH/meanHV;
    }

    /**
     * Get source samples in the sliding window.
     * @param tx The x coordinate of the central pixel of the sliding window.
     * @param ty The y coordinate of the central pixel of the sliding window.
     * @param noDataValue the place holder for no data
     * @param sourceTile The source image tile.
     * @param srcData The source image data.
     * @param samples The sample array.
     * @return The number of samples.
     */
    private int getSamples(final int tx, final int ty, final double noDataValue,
                           final Tile sourceTile, final ProductData srcData, final double[] samples) {


        final int x0 = Math.max(tx - halfWindowSize, 0);
        final int y0 = Math.max(ty - halfWindowSize, 0);
        final int w  = Math.min(tx + halfWindowSize, sourceImageWidth - 1) - x0 + 1;
        final int h  = Math.min(ty + halfWindowSize, sourceImageHeight - 1) - y0 + 1;

        final TileIndex tileIndex = new TileIndex(sourceTile);

        int numSamples = 0;
        final int maxy = Math.min(y0 + h, sourceTile.getMaxY() - 1);
        final int maxx = Math.min(x0 + w, sourceTile.getMaxX() - 1);

        for (int y = y0; y < maxy; y++) {
            tileIndex.calculateStride(y);
            for (int x = x0; x < maxx; x++) {
                final double v = srcData.getElemDoubleAt(tileIndex.getIndex(x));
                if (v != noDataValue) {
                    samples[numSamples++] = v;
                }
            }
        }

        return numSamples;
    }

    /**
     * Get the mean value of the samples.
     * @param samples The sample array.
     * @param numSamples The number of samples.
     * @return mean The mean value.
     */
    private static double getMeanValue(final double[] samples, final int numSamples) {

        double mean = 0.0;
        for (int i = 0; i < numSamples; i++) {
            mean += samples[i];
        }
        mean /= numSamples;

        return mean;
    }

    /**
     * Operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(ForestAreaDetectionOp.class);
        }
    }
}