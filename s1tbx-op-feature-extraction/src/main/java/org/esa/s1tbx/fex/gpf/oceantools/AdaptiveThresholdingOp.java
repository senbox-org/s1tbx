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
package org.esa.s1tbx.fex.gpf.oceantools;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.util.FastMath;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * The Adaptive Thresholding ship detection operator.
 * <p/>
 * The ship detection system consist of the following four steps:
 * 1. Pre-processing: this refers to applying calibration to make further prescreening stage easier and more accurate.
 * 2. Land masking: enabling to focus on area of interest.
 * 3. Pre-screening: this step is performed by applying a CFAR (Constant False Alarm Rate) detector.
 * 4. Discrimination: in order to reject false alarms.
 * <p/>
 * This operator implements the 2-parameter CFAR detector by applying an adaptive thresholding algorithm [1].
 * <p/>
 * [1] D. J. Crisp, "The State-of-the-Art in Ship Detection in Synthetic Aperture Radar Imagery." DSTO-RR-0272,
 * 2004-05.
 */
// todo replace t by t/sqrt(n) in case of multi-pixel target window, where n is the number of independent samples in target window

@OperatorMetadata(alias = "AdaptiveThresholding",
        category = "Radar/SAR Applications/Ocean Applications/Object Detection",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2015 by Array Systems Computing Inc.",
        description = "Detect ships using Constant False Alarm Rate detector.")
public class AdaptiveThresholdingOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct = null;

    //    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
//            sourceProductId = "source", label = "Source Bands")
    private String[] sourceBandNames = null;

    @Parameter(description = "Target window size", defaultValue = "75", label = "Target Window Size (m)")
    private int targetWindowSizeInMeter = 75;

    @Parameter(description = "Guard window size", defaultValue = "400.0", label = "Guard Window Size (m)")
    private double guardWindowSizeInMeter = 400.0;

    @Parameter(description = "Background window size", defaultValue = "1000.0", label = "Background Window Size (m)")
    private double backgroundWindowSizeInMeter = 1000.0;

    @Parameter(description = "Probability of false alarm", defaultValue = "6.5", label = "PFA (10^(-x))")
    private double pfa = 6.5;

    @Parameter(description = "Rough estimation of background threshold for quicker processing", defaultValue = "false", label = "Estimate background")
    private Boolean estimateBackground = false;

    private int sourceImageWidth;
    private int sourceImageHeight;
    private int targetWindowSize;
    private int halfGuardWindowSize;
    private int halfBackgroundWindowSize;

    private double t; // detector design parameter
    private double meanPixelSpacing; // in m

    private final HashMap<String, String> targetBandNameToSourceBandName = new HashMap<>(2);

    public static final String SHIPMASK_NAME = "_ship_bit_msk";
    private static final String PRODUCT_SUFFIX = "_THR";

    @Override
    public void initialize() throws OperatorException {
        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfCalibrated(true);
            validator.checkIfTOPSARBurstProduct(false);

            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            getMeanPixelSpacing();

            targetWindowSize = (int) (targetWindowSizeInMeter / meanPixelSpacing) + 1;
            final int guardWindowSize = (int) (guardWindowSizeInMeter / meanPixelSpacing) + 1;
            final int backgroundWindowSize = (int) (backgroundWindowSizeInMeter / meanPixelSpacing) + 1;

            halfGuardWindowSize = guardWindowSize / 2;
            halfBackgroundWindowSize = (backgroundWindowSize - 1) / 2;

            targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                                        sourceProduct.getProductType(),
                                        sourceImageWidth,
                                        sourceImageHeight);

            ProductUtils.copyProductNodes(sourceProduct, targetProduct);

            addSelectedBands();

            t = computeDetectorDesignParameter(pfa);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Compute mean pixel spacing (in m).
     *
     * @throws Exception The exception.
     */
    void getMeanPixelSpacing() throws Exception {
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final double rangeSpacing = AbstractMetadata.getAttributeDouble(abs, AbstractMetadata.range_spacing);
        final double azimuthSpacing = AbstractMetadata.getAttributeDouble(abs, AbstractMetadata.azimuth_spacing);
        final boolean srgrFlag = AbstractMetadata.getAttributeBoolean(abs, AbstractMetadata.srgr_flag);
        if (srgrFlag) {
            meanPixelSpacing = (rangeSpacing + azimuthSpacing) / 2.0;
        } else {
            meanPixelSpacing = (rangeSpacing / FastMath.sin(getIncidenceAngleAtCentreRangePixel()) + azimuthSpacing) / 2.0;
        }
    }

    /**
     * Get incidence angle at centre range pixel (in radian).
     *
     * @return The incidence angle.
     * @throws OperatorException The exceptions.
     */
    private double getIncidenceAngleAtCentreRangePixel() throws OperatorException {
        final int x = sourceImageWidth / 2;
        final int y = sourceImageHeight / 2;
        final TiePointGrid incidenceAngle = OperatorUtils.getIncidenceAngle(sourceProduct);
        if (incidenceAngle == null) {
            throw new OperatorException("incidence_angle tie point grid not found in product");
        }
        return incidenceAngle.getPixelDouble(x, y) * Constants.DTOR;
    }

    /**
     * Add the user selected bands to target product.
     *
     * @throws OperatorException The exceptions.
     */
    private void addSelectedBands() throws OperatorException {

        if (sourceBandNames == null || sourceBandNames.length == 0) { // if user did not select any band
            final Band[] bands = sourceProduct.getBands();
            final List<String> bandNameList = new ArrayList<>(sourceProduct.getNumBands());
            for (Band band : bands) {
                final String unit = band.getUnit();
                if (!(unit.contains(Unit.PHASE) || unit.contains(Unit.REAL) || unit.contains(Unit.IMAGINARY)))
                    bandNameList.add(band.getName());
            }
            sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);
        }

        for (String srcBandName : sourceBandNames) {
            final Band srcBand = sourceProduct.getBand(srcBandName);
            final String unit = srcBand.getUnit();
            if (unit != null && (unit.contains(Unit.PHASE) || unit.contains(Unit.REAL) || unit.contains(Unit.IMAGINARY))) {
                throw new OperatorException("Please select amplitude or intensity band for ship detection");
            } else {
                final String srcBandNames = srcBand.getName();
                final String targetBandName = srcBandNames + SHIPMASK_NAME;
                targetBandNameToSourceBandName.put(targetBandName, srcBandNames);

                final Band targetBand = ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, false);
                targetBand.setSourceImage(srcBand.getSourceImage());

                final Band targetBandMask = new Band(targetBandName,
                                                     ProductData.TYPE_INT8,
                                                     sourceImageWidth,
                                                     sourceImageHeight);

                targetBandMask.setUnit(Unit.AMPLITUDE);
                targetBandMask.setNoDataValue(0);
                targetBandMask.setNoDataValueUsed(true);
                targetProduct.addBand(targetBandMask);
            }
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int tx0 = targetTileRectangle.x;
            final int ty0 = targetTileRectangle.y;
            final int tw = targetTileRectangle.width;
            final int th = targetTileRectangle.height;
            final ProductData trgData = targetTile.getDataBuffer();
            //System.out.println("tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

            final int x0 = Math.max(tx0 - halfBackgroundWindowSize, 0);
            final int y0 = Math.max(ty0 - halfBackgroundWindowSize, 0);
            final int w = Math.min(tx0 + tw - 1 + halfBackgroundWindowSize, sourceImageWidth - 1) - x0 + 1;
            final int h = Math.min(ty0 + th - 1 + halfBackgroundWindowSize, sourceImageHeight - 1) - y0 + 1;
            final Rectangle sourceTileRectangle = new Rectangle(x0, y0, w, h);
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final String srcBandName = targetBandNameToSourceBandName.get(targetBand.getName());
            final Band sourceBand = sourceProduct.getBand(srcBandName);
            final Tile sourceTile = getSourceTile(sourceBand, sourceTileRectangle);
            final float[] data = sourceTile.getDataBufferFloat();

            final double noDataValue = sourceBand.getNoDataValue();

            double backgroundThreshold = 0;
            if(estimateBackground) {
                backgroundThreshold = computeBackgroundThreshold(data, noDataValue);
            }

            final TileIndex trgIndex = new TileIndex(targetTile);

            final int maxy = ty0 + th;
            final int maxx = tx0 + tw;
            for (int ty = ty0; ty < maxy; ty++) {
                trgIndex.calculateStride(ty);
                for (int tx = tx0; tx < maxx; tx++) {

                    final double targetMean = computeTargetMean(tx, ty, data, x0, y0, w, h, noDataValue);
                    if (noDataValue == targetMean) {
                        trgData.setElemIntAt(trgIndex.getIndex(tx), 0);
                        continue;
                    }

                    if(!estimateBackground) {
                        backgroundThreshold = computeBackgroundThreshold(tx, ty, data, x0, y0, w, h, noDataValue);
                    }
                    if (targetMean > backgroundThreshold) {
                        trgData.setElemIntAt(trgIndex.getIndex(tx), 1);
                    } else {
                        trgData.setElemIntAt(trgIndex.getIndex(tx), 0);
                    }
                }
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Compute the mean value for pixels in the target window.
     *
     * @param tx          The x coordinate of the central point of the target window.
     * @param ty          The y coordinate of the central point of the target window.
     * @param data        The source tile data array.
     * @param noDataValue
     * @return The mean value.
     */
    private double computeTargetMean(final int tx, final int ty, final float[] data,
                                     final int xx0, int yy0, int width, int height, final double noDataValue) {

        int index = ((ty - yy0) * width) + (tx - xx0);
        final double v = data[index];
        if (noDataValue == v) {
            return noDataValue;
        }

        if (targetWindowSize == 1) {
            return v;
        }

        final int x0 = Math.max((tx - xx0) - (targetWindowSize - 1) / 2, 0);
        final int y0 = Math.max((ty - yy0) - (targetWindowSize - 1) / 2, 0);
        final int w = Math.min((tx - xx0) + (targetWindowSize - 1) / 2, width - 1) - x0 + 1;
        final int h = Math.min((ty - yy0) + (targetWindowSize - 1) / 2, height - 1) - y0 + 1;

        double mean = 0.0;
        int numPixels = 0;

        final int maxy = y0 + h;
        final int maxx = x0 + w;
        for (int y = y0; y < maxy; y++) {
            for (int x = x0; x < maxx; x++) {
                final double val = data[(y * width) + x];

                if (noDataValue == val) {
                    return noDataValue;
                } else {
                    mean += val;
                    ++numPixels;
                }
            }
        }

        return mean / numPixels;
    }

    /**
     * Compute the standard deviation value for pixels in the background window.
     *
     * @param tx          The x coordinate of the central point of the background window.
     * @param ty          The y coordinate of the central point of the background window.
     * @param sourceTile  The source image tile.
     * @param noDataValue no data value
     * @return The std value.
     */
    private double computeBackgroundThreshold(final int tx, final int ty, final float[] data,
                                              final int xx0, int yy0, int width, int height, final double noDataValue) {

        final int x0 = Math.max((tx - xx0) - halfBackgroundWindowSize, 0);
        final int y0 = Math.max((ty - yy0) - halfBackgroundWindowSize, 0);
        final int w = Math.min((tx - xx0) + halfBackgroundWindowSize, width - 1) - x0 + 1;
        final int h = Math.min((ty - yy0) + halfBackgroundWindowSize, height - 1) - y0 + 1;

        // Compute the mean value for pixels in the background window.
        double sum = 0.0;
        double val;
        int numPixels = 0;
        final int maxy = y0 + h;
        final int maxx = x0 + w;

        final double[] dataArray = new double[w * h];
        for (int y = y0; y < maxy; y++) {
            final int yy = y - (ty - yy0);
            final boolean yGtrHalfGuard = ((yy < 0) ? -yy : yy) > halfGuardWindowSize;
            for (int x = x0; x < maxx; x++) {
                final int xx = x - (tx - xx0);
                if (yGtrHalfGuard || ((xx < 0) ? -xx : xx) > halfGuardWindowSize) {
                    val = data[(y * width) + x];
                    if (noDataValue == val) {
                        return Double.MAX_VALUE;
                    } else {
                        sum += val;
                        dataArray[numPixels] = val;
                        ++numPixels;
                    }
                }
            }
        }
        final double mean = sum / numPixels;

        // Compute the standard deviation value for pixels in the background window.
        double std = 0.0;
        double tmp;
        for (double value : dataArray) {
            tmp = value - mean;
            std += tmp * tmp;
        }

        final double backgroundSTD = Math.sqrt(std / numPixels);

        return mean + backgroundSTD * t;
    }

    private double computeBackgroundThreshold(final float[] data, final double noDataValue) {

        // Compute the mean value for pixels in the background window.
        double sum = 0.0;
        int numPixels = 0;

        for(float val : data) {
            if (noDataValue != val && val < 0.5) {
                sum += val;
                ++numPixels;
            }
        }
        final double mean = sum / numPixels;

        // Compute the standard deviation value for pixels in the background window.
        double std = 0.0;
        double tmp;
        for(float val : data) {
            if (noDataValue != val && val < 0.5) {
                tmp = val - mean;
                std += tmp * tmp;
            }
        }

        final double backgroundSTD = Math.sqrt(std / numPixels);

        return mean + backgroundSTD * t;
    }

    /**
     * Compute detector design parameter for given probability of false alarm.
     *
     * @param pfa The probability of false alarm
     * @return The desigm parameter.
     */
    private static double computeDetectorDesignParameter(final double pfa) {
        return Math.sqrt(2) * inverf(1.0 - 2.0 * FastMath.pow(10.0, -pfa));
    }

    /**
     * Compute the complementary error function erf(x) with fractional error everywhere less than 1.2 x 10^?7.
     *
     * @param x The input variable.
     * @return The error function value.
     */
    private static double erfc(double x) {

        final double z = Math.abs(x);
        final double t = 1.0 / (1.0 + 0.5 * z);
        double erfc = t * FastMath.exp(-z * z - 1.26551223 + t * (1.00002368 + t * (0.37409196 + t * (0.09678418 +
                t * (-0.18628806 + t * (0.27886807 + t * (-1.13520398 + t * (1.48851587 + t * (-0.82215223 +
                        t * 0.17087277)))))))));
        if (x < 0) {
            erfc = 2.0 - erfc;
        }
        return erfc;
    }

    /**
     * Compute error function value.
     *
     * @param x The input variable.
     * @return The error function value.
     */
    private static double erf(double x) {
        return 1.0 - erfc(x);
    }

    /**
     * Compute the inverse of complementary error function. Returns x such that erfc(x) = p for argument p
     * between 0 and 2.
     *
     * @param p The input complementary error function value.
     * @return The inverse of complementary error function.
     */
    private static double inverfc(final double p) {
        if (p >= 2.0) return -100.0;
        if (p <= 0.0) return 100.0;
        final double pp = (p < 1.0) ? p : 2.0 - p;
        final double t = Math.sqrt(-2.0 * Math.log(pp / 2.0));
        double x = -0.70711 * ((2.30753 + t * 0.27061) / (1.0 + t * (0.99229 + t * 0.04481)) - t);
        for (int j = 0; j < 2; j++) {
            final double err = erfc(x) - pp;
            x += err / (1.12837916709551257 * FastMath.exp(-Math.sqrt(x)) - x * err);
        }
        return (p < 1.0 ? x : -x);
    }

    /**
     * Compute the inverse of error function. Returns x such that erf(x) = p for argument p between -1 and 1.
     *
     * @param p The input error function value.
     * @return The inverse of error function.
     */
    private static double inverf(final double p) {
        return inverfc(1.0 - p);
    }


    /**
     * Operator SPI.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AdaptiveThresholdingOp.class);
        }
    }
}
