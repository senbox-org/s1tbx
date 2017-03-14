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
import org.apache.commons.math3.analysis.UnivariateFunction;
import org.apache.commons.math3.analysis.integration.IterativeLegendreGaussIntegrator;
import org.apache.commons.math3.util.FastMath;
import org.esa.snap.core.datamodel.*;
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
import java.util.HashMap;
import java.util.Map;

import static org.apache.commons.math3.special.Gamma.gamma;

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

    @Parameter(description = "Target window size", defaultValue = "50", label = "Target Window Size (m)")
    private int targetWindowSizeInMeter = 50;

    @Parameter(description = "Guard window size", defaultValue = "500.0", label = "Guard Window Size (m)")
    private double guardWindowSizeInMeter = 500.0;

    @Parameter(description = "Background window size", defaultValue = "800.0", label = "Background Window Size (m)")
    private double backgroundWindowSizeInMeter = 800.0;

    @Parameter(description = "Probability of false alarm", defaultValue = "6.5", label = "PFA (10^(-x))")
    private double pfa = 6.5;

    @Parameter(description = "Rough estimation of background threshold for quicker processing", defaultValue = "false", label = "Estimate background")
    private Boolean estimateBackground = false;

    private int sourceImageWidth;
    private int sourceImageHeight;
    private int targetWindowSize;
    private int halfTargetWindowSize;
    private int halfGuardWindowSize;
    private int halfBackgroundWindowSize;

    private double t; // detector design parameter
    private double meanPixelSpacing; // in m

    private final HashMap<String, String> targetBandNameToSourceBandName = new HashMap<>(2);

    public static final String SHIPMASK_NAME = "_ship_bit_msk";
    private static final String PRODUCT_SUFFIX = "_THR";

    private static final double backgroundThreshold = 0.5;

    // For K-Distribution
    private final static boolean doKDistribution = false;
    private int numLooks;
    private double oneMinusPFA;
    private final static int NUM_INTEGRATION_PTS = 100; // TODO: fine tune?
    private final static double MAX_SOURCE_VALUE = 2.0; // TODO: fine tune?
    private final static int MAX_EVAL = 2000; // TODO: fine tune?
    private final static double DESIRED_ACCURACY = 1.0e-15; // TODO: This should depend on pfa

    @Override
    public void initialize() throws OperatorException {
        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfCalibrated(true);
            validator.checkIfTOPSARBurstProduct(false);

            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            getMeanPixelSpacing();

            targetWindowSize = Math.max(1, (int) (targetWindowSizeInMeter / meanPixelSpacing) + 1);
            final int guardWindowSize = (int) (guardWindowSizeInMeter / meanPixelSpacing) + 1;
            final int backgroundWindowSize = (int) (backgroundWindowSizeInMeter / meanPixelSpacing) + 1;

            halfTargetWindowSize = targetWindowSize / 2;
            halfGuardWindowSize = guardWindowSize / 2;
            halfBackgroundWindowSize = (backgroundWindowSize - 1) / 2;

            targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                                        sourceProduct.getProductType(),
                                        sourceImageWidth,
                                        sourceImageHeight);

            ProductUtils.copyProductNodes(sourceProduct, targetProduct);

            addSelectedBands();

            t = computeDetectorDesignParameter(pfa);

            if (estimateBackground == null) {
                estimateBackground = false;
            }

            if (doKDistribution) {
                final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
                final double rangeLooks = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_looks);
                numLooks = (int) rangeLooks;
                oneMinusPFA = 1.0 - Math.pow(10.0, -pfa);

                System.out.println("numLooks = " + numLooks + "; (1 - pfa) = " + oneMinusPFA + "; backgroundWindowSize = " + backgroundWindowSize);

                //debugKDistribution();
                //debugFindBoundsForT();
            }

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
            final Map<String, String> bandNameMap = new HashMap<>(sourceProduct.getNumBands());
            for (Band srcBand : bands) {
                // copy all bands
                if (srcBand instanceof VirtualBand) {
                    ProductUtils.copyVirtualBand(targetProduct, (VirtualBand) srcBand, srcBand.getName());
                } else {
                    ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, true);
                }

                final String unit = srcBand.getUnit();
                if (!(unit == null || unit.contains(Unit.PHASE) || unit.contains(Unit.REAL) || unit.contains(Unit.IMAGINARY))) {

                    String pol = OperatorUtils.getPolarizationFromBandName(srcBand.getName());
                    if (pol != null) {
                        bandNameMap.put(pol, srcBand.getName());
                    } else {
                        bandNameMap.put("NoPol", srcBand.getName());
                    }
                }
            }
            if (bandNameMap.containsKey("hv")) {
                sourceBandNames = new String[]{bandNameMap.get("hv")};
            } else if (bandNameMap.containsKey("vh")) {
                sourceBandNames = new String[]{bandNameMap.get("vh")};
            } else if (bandNameMap.containsKey("hh")) {
                sourceBandNames = new String[]{bandNameMap.get("hh")};
            } else {
                sourceBandNames = bandNameMap.values().toArray(new String[bandNameMap.size()]);
            }
        }

        for (String srcBandName : sourceBandNames) {
            final Band srcBand = sourceProduct.getBand(srcBandName);
            final String unit = srcBand.getUnit();
            if (unit != null && (unit.contains(Unit.PHASE) || unit.contains(Unit.REAL) || unit.contains(Unit.IMAGINARY))) {
                throw new OperatorException("Please select amplitude or intensity band for ship detection");
            } else {
                final String targetBandName = srcBandName + SHIPMASK_NAME;
                targetBandNameToSourceBandName.put(targetBandName, srcBandName);

                if (!targetProduct.containsBand(srcBandName)) {
                    final Band targetBand = ProductUtils.copyBand(srcBandName, sourceProduct, targetProduct, true);
                }

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

            final int x0, y0, w, h;
            final Rectangle sourceTileRectangle;
            if (estimateBackground) {
                x0 = tx0;
                y0 = ty0;
                w = tw;
                h = th;
                sourceTileRectangle = targetTileRectangle;
            } else {
                x0 = Math.max(tx0 - halfBackgroundWindowSize, 0);
                y0 = Math.max(ty0 - halfBackgroundWindowSize, 0);
                w = Math.min(tx0 + tw - 1 + halfBackgroundWindowSize, sourceImageWidth - 1) - x0 + 1;
                h = Math.min(ty0 + th - 1 + halfBackgroundWindowSize, sourceImageHeight - 1) - y0 + 1;
                sourceTileRectangle = new Rectangle(x0, y0, w, h);
            }
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final String srcBandName = targetBandNameToSourceBandName.get(targetBand.getName());
            final Band sourceBand = sourceProduct.getBand(srcBandName);
            final Tile sourceTile = getSourceTile(sourceBand, sourceTileRectangle);
            final TileIndex trgIndex = new TileIndex(targetTile);
            final float[] data = sourceTile.getDataBufferFloat();

            final double noDataValue = sourceBand.getNoDataValue();

            double backgroundThreshold = 0;
            if (estimateBackground) {
                backgroundThreshold = computeBackgroundThreshold(data, noDataValue);
            }

            final int maxy = ty0 + th;
            final int maxx = tx0 + tw;
            for (int ty = ty0; ty < maxy; ty++) {
                //System.out.println("ty = " + ty);
                trgIndex.calculateStride(ty);
                for (int tx = tx0; tx < maxx; tx++) {
                    //System.out.println("ty = " + ty + " tx = " + tx);
                    final double targetMean = computeTargetMean(tx, ty, data, x0, y0, w, h, noDataValue);
                    if (noDataValue == targetMean) {
                        trgData.setElemIntAt(trgIndex.getIndex(tx), 0);
                        continue;
                    }

                    if (!estimateBackground) {
                        if(targetMean < 0.005) {
                            trgData.setElemIntAt(trgIndex.getIndex(tx), 0);
                            continue;
                        }
                        backgroundThreshold = computeBackgroundThreshold(tx, ty, data, x0, y0, w, h, noDataValue);

                        // DEBUG...
                        /*
                        double oldT = computeBackgroundThreshold(tx, ty, data, x0, y0, w, h, noDataValue);
                        System.out.println("DEBUG: tx = " + tx + " ty = " + ty + ": backgroundThreshold = " + backgroundThreshold
                                + " oldT = " + oldT + " targetMean = " + targetMean);
                        */
                        // ...DEBUG
                    }
                    if (targetMean > backgroundThreshold) {
                        trgData.setElemIntAt(trgIndex.getIndex(tx), 1);
                    } else {
                        trgData.setElemIntAt(trgIndex.getIndex(tx), 0);
                    }
                }
            }

            //System.out.println("DONE tx0 = " + tx0 + ", ty0 = " + ty0 + ", tw = " + tw + ", th = " + th);

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

        final int x0 = Math.max((tx - xx0) - halfTargetWindowSize, 0);
        final int y0 = Math.max((ty - yy0) - halfTargetWindowSize, 0);
        final int w = Math.min((tx - xx0) + halfTargetWindowSize, width - 1) - x0 + 1;
        final int h = Math.min((ty - yy0) + halfTargetWindowSize, height - 1) - y0 + 1;

        double mean = 0.0;
        int numPixels = 0;
        int nodataCnt = 0;

        final int maxy = y0 + h;
        final int maxx = x0 + w;
        for (int y = y0; y < maxy; y++) {
            int yWidth = y * width;
            for (int x = x0; x < maxx; x++) {
                final double val = data[yWidth + x];

                if (noDataValue == val) {
                    nodataCnt++;
                } else {
                    mean += val;
                    ++numPixels;
                }
            }
        }

        if(nodataCnt > (0.1 * w*h)) {
            return noDataValue;
        }
        return mean / numPixels;
    }

    /**
     * Compute the standard deviation value for pixels in the background window.
     *
     * @param tx          The x coordinate of the central point of the background window.
     * @param ty          The y coordinate of the central point of the background window.
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
        final int maxy = y0 + h;
        final int maxx = x0 + w;

        final double[] dataArray = new double[w * h];
        int numValues = 0;

        for (int y = y0; y < maxy; y++) {
            final int yy = y - (ty - yy0);
            final int yWidth = y * width;
            final boolean yGtrHalfGuard = ((yy < 0) ? -yy : yy) > halfGuardWindowSize;
            for (int x = x0; x < maxx; x++) {
                final int xx = x - (tx - xx0);
                if (yGtrHalfGuard || ((xx < 0) ? -xx : xx) > halfGuardWindowSize) {
                    val = data[yWidth + x];
                    if (noDataValue != val) {// && val < backgroundThreshold) {
                        sum += val;
                        dataArray[numValues] = val;
                        numValues++;
                    }
                }
            }
        }
        final double mean = sum / numValues;

        // Compute the standard deviation value for pixels in the background window.
        double std = 0.0;
        double tmp;
        for (int i=0; i < numValues; ++i) {
            tmp = dataArray[i] - mean;
            std += tmp * tmp;
        }

        final double backgroundSTD = Math.sqrt(std / numValues);

        return mean + backgroundSTD * t;
    }

    private double computeBackgroundThreshold(final float[] data, final double noDataValue) {

        // Compute the mean value for pixels in the background window.
        double sum = 0.0;
        int numPixels = 0;

        for (float val : data) {
            if (noDataValue != val && val < backgroundThreshold) {
                sum += val;
                ++numPixels;
            }
        }
        final double mean = sum / numPixels;

        // Compute the standard deviation value for pixels in the background window.
        double std = 0.0;
        double tmp;
        for (float val : data) {
            if (noDataValue != val && val < backgroundThreshold) {
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

    // For K-distribution

    /**
     * Compute the mean, square mean and standard deviation values in the background window (ring).
     *
     * @param tx          The x coordinate of the central point of the target window.
     * @param ty          The y coordinate of the central point of the target window.
     * @param data        The source tile data array.
     * @param xx0         The x coordinate of the top left pixel of the source tile.
     * @param yy0         The y coordinate of the top left pixel of the source tile.
     * @param width       The width of the source tile.
     * @param height      The height of the source tile.
     * @param noDataValue Value representing no data available.
     * @param stats       The mean, mean of square and standard deviation values (output).
     * @return 'true' if successful
     */
    private boolean computeBackgroundStatistics(final int tx, final int ty, final float[] data,
                                                final int xx0, int yy0, int width, int height,
                                                final double noDataValue, final double[] stats) {

        // stats[0] = mean = <x>
        // stats[1] = mean of x^2 = <x^2>
        // stats[2] = standard deviation sigma

        final int x0 = Math.max((tx - xx0) - halfBackgroundWindowSize, 0);
        final int y0 = Math.max((ty - yy0) - halfBackgroundWindowSize, 0);
        final int w = Math.min((tx - xx0) + halfBackgroundWindowSize, width - 1) - x0 + 1;
        final int h = Math.min((ty - yy0) + halfBackgroundWindowSize, height - 1) - y0 + 1;

        // Compute the mean <x> and mean of square <x^2> values for pixels in the background window.
        double sum = 0.0;
        double sumsq = 0.0;
        double val;
        final int maxy = y0 + h;
        final int maxx = x0 + w;

        final double[] dataArray = new double[w * h];
        int numValues = 0;

        for (int y = y0; y < maxy; y++) {
            final int yy = y - (ty - yy0);
            final int yWidth = y * width;
            final boolean yGtrHalfGuard = ((yy < 0) ? -yy : yy) > halfGuardWindowSize;
            for (int x = x0; x < maxx; x++) {
                final int xx = x - (tx - xx0);
                if (yGtrHalfGuard || ((xx < 0) ? -xx : xx) > halfGuardWindowSize) {
                    val = data[yWidth + x];
                    if (noDataValue != val) {// && val < backgroundThreshold) {
                        sum += val;
                        sumsq += (val * val);
                        dataArray[numValues] = val;
                        numValues++;
                    }
                }
            }
        }

        if (numValues == 0) {
            return false;
        }

        final double mean = sum / numValues;
        stats[0] = mean;
        stats[1] = sumsq / numValues;

        double std = 0.0;
        double tmp;
        for (int i=0; i < numValues; ++i) {
            tmp = dataArray[i] - mean;
            std += tmp * tmp;
        }
        stats[2] = Math.sqrt(std / numValues);

        return true;
    }

    private double evaluateProbability(final UnivariateFunction pdf, final double x) {

        // integrate pdf from 0 to x

        double result;

        try {
            final IterativeLegendreGaussIntegrator integrator =
                    new IterativeLegendreGaussIntegrator(NUM_INTEGRATION_PTS, DESIRED_ACCURACY, sq(DESIRED_ACCURACY));
            result = integrator.integrate(MAX_EVAL, pdf, 0.0, x);
        } catch (Exception e) {
            //System.out.println(e.getMessage() + " x = " + x);
            result = -999;
        }

        return result;
    }

    private double integrateFromZeroToInfinity(final UnivariateFunction pdf) {

        final ModifiedFinitePDF mpdf = new ModifiedFinitePDF(pdf);

        double result;

        try {
            final IterativeLegendreGaussIntegrator integrator =
                    new IterativeLegendreGaussIntegrator(NUM_INTEGRATION_PTS, DESIRED_ACCURACY, sq(DESIRED_ACCURACY));
            result = integrator.integrate(MAX_EVAL, mpdf, 0.0, Math.PI / 2.0);
        } catch (Exception e) {
            result = -999;
        }

        return result;
    }

    private void findBoundsForT(final UnivariateFunction pdf, final double[] bounds) {

        // bounds[0] is lower bound
        // bounds[1] is upper bound

        // Find leftT and rightT such that leftVal <= (1 - PFA) <= rightVal

        bounds[0] = -999;
        bounds[1] = -999;

        double leftT = 0.0;
        double rightT = MAX_SOURCE_VALUE;

        boolean foundRightT = false;
        boolean foundLeftT = false;

        for (int i = 0; i < MAX_EVAL; i++) {
            double rightVal = evaluateProbability(pdf, rightT);
            if (rightVal < 0) {
                return;
            } else if (rightVal < oneMinusPFA) {
                leftT = rightT;
                foundLeftT = true;
                rightT *= 2.0;
            }  else if (rightVal > oneMinusPFA) {
                foundRightT = true;
                break;
            } else {
                bounds[0] = rightT;
                bounds[1] = rightT;
                return;
            }
        }

        if (!foundRightT) {  // Failed to find bounds for T
            System.out.println("DEBUG: ERROR Failed to find bounds for T. " + getParamsString(pdf));
            return;
        }

        if (!foundLeftT) {
            leftT = rightT/2.0;
            for (int i = 0; i < MAX_EVAL; i++) {
                double leftVal = evaluateProbability(pdf, leftT);
                if (leftVal < 0) {
                    return;
                } else if (leftVal > oneMinusPFA) {
                    rightT = leftT;
                    leftT /= 2.0;
                } else if (leftVal < oneMinusPFA) {
                    foundLeftT = true;
                    break;
                } else {
                    bounds[0] = leftT;
                    bounds[1] = leftT;
                    return;
                }
            }
        }

        if (foundLeftT) {
            bounds[0] = leftT;
            bounds[1] = rightT;

            // TODO remove when all is working fine
            // DEBUG...
            if (leftT > rightT) {
                System.out.println("DEBUG: leftT = " + leftT + "; rightT = " + rightT);
                throw new OperatorException("DEBUG findBoundsForT: BUG!!! leftT > rightT");
            }
            double leftVal = evaluateProbability(pdf, leftT);
            double rightVal = evaluateProbability(pdf, rightT);
            if (leftVal > oneMinusPFA || rightVal < oneMinusPFA) {
                System.out.println("DEBUG: leftT = " + leftT + "; rightT = " + rightT + " leftVal = " + leftVal
                        + " rightVal = " + rightVal + " 1-PFA = " + oneMinusPFA + getParamsString(pdf));
                throw new OperatorException("DEBUG findBoundsForT: BUG!!!");
            }
            // ...DEBUG
        }
    }

    private double computeT(final UnivariateFunction pdf,
                            final int tx, final int ty // tx and ty are for debugging only
    ) {

        final double[] bounds = new double[2];
        findBoundsForT(pdf, bounds);

        if (bounds[0] > bounds[1]) {
            // Should never happen
            throw new OperatorException("lower bound = " + bounds[0] + " cannot be >" + " upper bound = " + bounds[1]);
        }

        if (bounds[1] < 0.0) {
            System.out.println("DEBUG: ERROR tx = " + tx + " ty = " + ty + " : bounds = " + bounds[0] + ", " + bounds[1]);
            return Double.MAX_VALUE; // Failed to compute threshold
        }

        double leftT = bounds[0];
        double rightT = bounds[1];
        for (int i = 0; i < MAX_EVAL; i++) {
            double newT = (leftT + rightT)/2;
            double leftVal = evaluateProbability(pdf, leftT);
            if (leftVal < 0.0) {
                return Double.MAX_VALUE; // Failed to compute threshold
            }
            double rightVal = evaluateProbability(pdf, rightT);
            if (rightVal < 0.0) {
                return Double.MAX_VALUE; // Failed to compute threshold
            }
            if (Math.abs(leftVal - rightVal) < DESIRED_ACCURACY) {
                /*System.out.println("tx = " + tx + " ty = " + ty + " : leftT = " + leftT
                        + "; rightT = " + rightT + "; leftVal = " + leftVal + "; rightVal = " + rightVal
                        + " T = " + newT);*/
                if (Math.abs(evaluateProbability(pdf, newT) - oneMinusPFA) > DESIRED_ACCURACY) {
                    System.out.println("ERROR2: tx = " + tx + " ty = " + ty + ": " + getParamsString(pdf));
                }
                return newT;
            }
            double newVal = evaluateProbability(pdf, newT);
            if (newVal < 0.0) {
                return Double.MAX_VALUE; // Failed to compute threshold
            }
            if (newVal < oneMinusPFA) {
                leftT = newT;
            } else if (newVal > oneMinusPFA) {
                rightT = newT;
            } else {
                //System.out.println("tx = " + tx + " ty = " + ty + " : newVal = " + newVal + "; T = newT = " + newT);
                if (Math.abs(evaluateProbability(pdf, newT) - oneMinusPFA) > DESIRED_ACCURACY) {
                    System.out.println("ERROR3: tx = " + tx + " ty = " + ty + ": " + getParamsString(pdf));
                }
                return newT;
            }
        }

        System.out.println("DEBUG ERROR1: tx = " + tx + " ty = " + ty);

        return Double.MAX_VALUE; // Failed to compute threshold
    }

    private KDistributionPDF getScaledKDistribution(final double mu, final double nu) {

        KDistributionPDF pdf = new KDistributionPDF((double)numLooks, mu, nu, 1.0);

        double tmp = integrateFromZeroToInfinity(pdf);

        if (tmp <= 0) {
            return null;
        }

        KDistributionPDF spdf = new KDistributionPDF(numLooks, mu, nu, 1/tmp);

        return spdf;
    }

    private double computeBackgroundThreshold1(final int tx, final int ty, final float[] data,
                                               final int xx0, int yy0, int width, int height, final double noDataValue) {

        // Estimate mu and nu
        // mu = <x>
        // (1 + 1/nu)(1 + 1/L) = <x^2> / <x>^2
        // L is numLooks
        final double[] stats = new double[3]; // <x>, <x^2> and sigma
        final boolean ok = computeBackgroundStatistics(tx, ty, data, xx0, yy0, width, height, noDataValue, stats);
        if (!ok) {
            return Double.MAX_VALUE;
        }

        // stats[0] is <x> ; stats[1] = <x^2>; stats[2] = standard deviation sigma
        final double mu = stats[0];
        final double tmp1 = stats[1] / (mu * mu);
        final double tmp2 = 1.0 + (1.0 / (double) numLooks);
        final double nu = 1.0 / ((tmp1 / tmp2) - 1.0);

        final UnivariateFunction pdf = (nu < 0.0) ? new Chi2DistributionPDF((double) numLooks, stats[2]) :
                                                    getScaledKDistribution(mu, nu);

        if (pdf == null) {
            return Double.MAX_VALUE;
        }

        return computeT(pdf, tx, ty);
    }


    private String getParamsString(final UnivariateFunction pdf) {
        if (pdf instanceof KDistributionPDF) {
            return " K-dis params: " + ((KDistributionPDF) pdf).getParamsString();
        } else if (pdf instanceof Chi2DistributionPDF) {
            return " Chi2-dis params: " + ((Chi2DistributionPDF) pdf).getParamsString();
        }
        return "getParamsString() called for unknown function";
    }

    private static double sq(final double x) {
        return x*x;
    }

    class KDistributionPDF implements UnivariateFunction {

        final double L;
        final double mu;
        final double nu;
        final double ptmp1;
        final double z0tmp1;
        final double z0tmp2;
        final double scaleFactor;

        KDistributionPDF(final double L, final double mu, final double nu, double scaleFactor) {
            this.L = L;
            this.mu = mu;
            this.nu = nu;
            this.scaleFactor = scaleFactor;
            final double gammaNu = (nu < 1.0) ? (gamma(nu + 1.0) / nu) : gamma(nu);
            ptmp1 = (nu * L * Math.sqrt(Math.PI)) / (Math.sqrt(2.0) * gammaNu * gamma(L));
            z0tmp1 = (mu * (nu - L)) / (2 * nu);
            z0tmp2 = (4.0 * L * nu) / (mu * sq(nu - L));
        }

        double compute_z0(final double x) {
            if (nu == L) {
                return Math.sqrt(x);
            }
            double tmp = Math.sqrt(1.0 + z0tmp2 * x);
            tmp = (nu > L) ? (1 + tmp) : (1 - tmp);
            return z0tmp1 * tmp;
        }

        double compute_exp_f(final double x, final double z) {
            final double tmp1 = nu * z / mu;
            final double tmp2 = (L - 1.0) * Math.log(L * x / z) - (L * x / z) + ((nu - 1) * Math.log(tmp1)) - tmp1;
            //System.out.println("compute_exp_f: tmp1 " + tmp1 + " tmp2 = " + tmp2);
            return Math.exp(tmp2);
        }

        double compute_sqrt_f2prime(final double x, final double z) {
            final double sqz = sq(z);
            final double tmp = - (2.0 * L * x / (sqz * z)) - ((nu - L) / sqz);
            //System.out.println("compute_sqrt_f2prime: x = " + x + " z = " + z + " sqz = " + sqz + " tmp = " + tmp);
            return Math.sqrt(Math.abs(tmp));
        }

        double getL() {
            return L;
        }

        double getMu() {
            return mu;
        }

        double getNu() {
            return nu;
        }

        double getScaleFactor() {
            return scaleFactor;
        }

        @Override
        public double value(double v) {
            final double z0 = compute_z0(v);
            //System.out.println("z0 = " + z0 + " ptmp1 = " + ptmp1);
            final double val = ptmp1 * compute_exp_f(v, z0) / compute_sqrt_f2prime(v, z0);
            return val * scaleFactor;
        }

        String getParamsString() {
            return "L = " + L + "; mu = " + mu + "; nu = " + nu + "; scaleFactor = " + scaleFactor;
        }
    }

    class Chi2DistributionPDF implements UnivariateFunction {

        final double n;
        final double sigma;
        final double denominator;
        final double twoSigmaSq;

        Chi2DistributionPDF(final double n, final double sigma) {
            this.n = n;
            this.sigma = sigma;
            this.denominator = Math.pow(2.0, n) * Math.pow(sigma, 2.0*n) * gamma(n);
            this.twoSigmaSq = 2.0 * sigma * sigma;
        }

        double getN() {
            return n;
        }

        double getSigma() {
            return sigma;
        }

        @Override
        public double value(double v) {
            if (v < 0.0) {
                return 0.0;
            } else {
                return (Math.pow(v, n - 1.0) / denominator) * Math.exp(-v / twoSigmaSq);
            }
        }

        String getParamsString() {
            return "n = " + n + "; sigma = " + sigma;
        }
    }

    class ModifiedFinitePDF implements UnivariateFunction {

        // http://math.stackexchange.com/questions/1355828/estimating-the-value-of-an-improper-integral-numerically

        // -infinity, use -PI/2
        // +infinity, use +PI/2

        final UnivariateFunction pdf;

        ModifiedFinitePDF(UnivariateFunction pdf) {
            this.pdf = pdf;
        }

        @Override
        public double value(double v) {
            return 2.0 * pdf.value(Math.tan(v)) / (Math.cos(2.0 * v) + 1);
        }
    }

    // For testing and/or debugging K-distribution only...

    class NaturalExp implements UnivariateFunction {

        NaturalExp() {}

        @Override
        public double value(double v) {
            return Math.exp(v);
        }
    }

    class UniformDistribution implements UnivariateFunction {

        UniformDistribution() {}

        @Override
        public double value(double v) {
            return Math.exp(-v*v/2.0) / Math.sqrt(2.0*Math.PI);
        }
    }

    private double integrateFromNegativeInfinityToPositiveInfinity(final UnivariateFunction pdf) {

        final ModifiedFinitePDF mpdf = new ModifiedFinitePDF(pdf);

        double result;

        try {
            final IterativeLegendreGaussIntegrator integrator =
                    new IterativeLegendreGaussIntegrator(NUM_INTEGRATION_PTS, DESIRED_ACCURACY, sq(DESIRED_ACCURACY));
            result = integrator.integrate(MAX_EVAL, mpdf, -Math.PI / 2.0, Math.PI / 2.0);
        } catch (Exception e) {
            result = -999;
        }

        return result;
    }

    private void debugKDistribution() {

        KDistributionPDF pdf = new KDistributionPDF(numLooks, 0.08525186254131689, 17.343587288660498, 1);
        UnivariateFunction updf = new UniformDistribution();

        System.out.println("DEBUG K-Dis: intergrate Uniform distribution from 0 to +infinity = " +
                integrateFromZeroToInfinity(updf));
        System.out.println("DEBUG K-Dis: intergrate Uniform distribution from -infinity to +infinity = " +
                integrateFromNegativeInfinityToPositiveInfinity(updf));

        double tmp = integrateFromZeroToInfinity(pdf);
        System.out.println("DEBUG K-Dis: intergrate K-distribution (no scaling) from 0 to +infinity = " + tmp);
        System.out.println("DEBUG K-Dis: intergrate K-distribution from -infinity to +infinity = " +
                integrateFromNegativeInfinityToPositiveInfinity(pdf));

        KDistributionPDF spdf = new KDistributionPDF(numLooks, 0.08525186254131689, 17.343587288660498, 1/tmp);
        System.out.println("DEBUG K-Dis: intergrate K-distribution (scaled) from 0 to +infinity = "
                            + integrateFromZeroToInfinity(spdf));

        /*
        for (int i = 0; i < 100; i++) {
            double x = (double)i/100;
            double y = pdf.value(x);
            System.out.println(x + ", " + y);
        } */
    }

    private void debugFindBoundsForT() {
        double mu = 0.0036357872468928934;
        double nu = 48.280481127435564;
        KDistributionPDF pdf = getScaledKDistribution(mu, nu);
        double bounds[] = new double[2];
        findBoundsForT(pdf, bounds);
        System.out.println("DEBUG find bounds for T: " + bounds[0] + ", " + bounds[1]);
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
