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
package org.esa.s1tbx.insar.gpf.coregistration.experimental;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.insar.gpf.coregistration.GCPManager;
import org.esa.s1tbx.insar.gpf.coregistration.WarpOp;
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

import java.awt.Rectangle;
import java.util.Map;
import java.util.Set;

/**
 * The operator corrects the absolute timing error of the product using SAR simulation.
 */

@OperatorMetadata(alias = "Abs-Timing-Err-Correction",
        category = "Radar/Coregistration",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2016 by Array Systems Computing Inc.",
        description = "Absolute timing error correction using SAR simulation",
        internal = true)
public class AbsTimingErrCorrectionOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The RMS threshold for eliminating invalid GCPs", interval = "(0, *)", defaultValue = "1.0",
            label = "RMS Threshold")
    private float rmsThreshold = 1.0f;

    private int warpPolynomialOrder = 1;
    private int maxIterations = 20;
    private ProductNodeGroup<Placemark> masterGCPGroup = null;
    private MetadataElement absRoot = null;
    private double rangeSpacing = 0.0;
    private double slantRangeToFirstPixel = 0.0;
    private double firstLineTime = 0.0;
    private double lastLineTime = 0.0;
    private double lineTimeInterval = 0.0;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private boolean warpDataAvailable = false;
    private WarpOp.WarpData warpData = null;
    private double avgZeroDopplerTimingErr = 0.0;
    private double avgSlantRangeTimingErr = 0.0;
    private String processedSlaveBand;

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSARProduct();
            validator.checkIfMapProjected(false);

            getSourceImageDimension();

            getMetadata();

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Get source image width and height.
     */
    private void getSourceImageDimension() {
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
    }

    /**
     * Retrieve required data from Abstracted Metadata
     *
     * @throws Exception if metadata not found
     */
    private void getMetadata() throws Exception {

        absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        rangeSpacing = AbstractMetadata.getAttributeDouble(absRoot, AbstractMetadata.range_spacing);

        lineTimeInterval = absRoot.getAttributeDouble(AbstractMetadata.line_time_interval) / Constants.secondsInDay; // s to day

        slantRangeToFirstPixel = absRoot.getAttributeDouble(AbstractMetadata.slant_range_to_first_pixel);

        firstLineTime = AbstractMetadata.parseUTC(
                absRoot.getAttributeString(AbstractMetadata.first_line_time)).getMJD(); // in days

        lastLineTime = AbstractMetadata.parseUTC(
                absRoot.getAttributeString(AbstractMetadata.last_line_time)).getMJD(); // in days

        processedSlaveBand = absRoot.getAttributeString("processed_slave");
    }

    /**
     * Create target product.
     *
     * @throws OperatorException The exception.
     */
    private void createTargetProduct() throws Exception {

        targetProduct = new Product(
                sourceProduct.getName(), sourceProduct.getProductType(), sourceImageWidth, sourceImageHeight);

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        addSelectedBands();

        ProductUtils.copyMetadata(sourceProduct, targetProduct);
        ProductUtils.copyMasks(sourceProduct, targetProduct);
        ProductUtils.copyVectorData(sourceProduct, targetProduct);
        targetProduct.setStartTime(sourceProduct.getStartTime());
        targetProduct.setEndTime(sourceProduct.getEndTime());
        targetProduct.setDescription(sourceProduct.getDescription());
    }

    /**
     * Add the user selected bands to target product.
     *
     * @throws OperatorException The exceptions.
     */
    private void addSelectedBands() throws OperatorException {

        final Band[] sourceBands = sourceProduct.getBands();
        if (sourceBands.length == 1) {
            throw new OperatorException("Source product should include a simulated intensity band. Only " +
                                        sourceBands[0].getName() + " found");
        }

        for (Band srcBand : sourceBands) {
            final String bandName = srcBand.getName();
            if (bandName.contains("Simulated_Intensity")) {
                continue;
            }
            final Band targetBand = targetProduct.addBand(bandName, srcBand.getDataType());
            ProductUtils.copyRasterDataNodeProperties(srcBand, targetBand);

            if (!bandName.equals(processedSlaveBand)) {
                targetBand.setSourceImage(srcBand.getSourceImage());
            }
        }
    }

    private synchronized void getWarpData(final Set<Band> keySet, final Rectangle targetRectangle) {

        if (warpDataAvailable) {
            return;
        }

        // find first real slave band
        for (Band targetBand : keySet) {
            if (targetBand.getName().equals(processedSlaveBand)) {
                final Band srcBand = sourceProduct.getBand(processedSlaveBand);
                if (srcBand != null) {
                    final Tile sourceRaster = getSourceTile(srcBand, targetRectangle);
                    break;
                }
            }
        }

        final Band masterBand = sourceProduct.getBandAt(0);
        masterGCPGroup = GCPManager.instance().getGcpGroup(masterBand);
        final int numSrcBands = sourceProduct.getNumBands();
        for (int i = 1; i < numSrcBands; ++i) { // loop through all slave bands

            final Band srcBand = sourceProduct.getBandAt(i);
            final String unit = srcBand.getUnit();
            if (unit == null || !unit.contains(Unit.AMPLITUDE) && !unit.contains(Unit.INTENSITY)) {
                continue;
            }

            ProductNodeGroup<Placemark> slaveGCPGroup = GCPManager.instance().getGcpGroup(srcBand);
            if (slaveGCPGroup.getNodeCount() < 3) {
                continue;
            }

            warpData = new WarpOp.WarpData(slaveGCPGroup);

            WarpOp.computeWARPPolynomialFromGCPs(sourceProduct, srcBand, warpPolynomialOrder, masterGCPGroup,
                                                 maxIterations, rmsThreshold, false, warpData);

            if (!warpData.notEnoughGCPs) {
                break;
            }
        }

        if (warpData.notEnoughGCPs) {
            throw new OperatorException("Do not have enough valid GCPs for the warp");
        }

        computeAbsTimingErr();

        updateTargetProductMetadata();

        warpDataAvailable = true;
    }

    private void computeAbsTimingErr() {

        double[] rangeOffset = new double[warpData.numValidGCPs];
        double[] azimuthOffset = new double[warpData.numValidGCPs];
        for (int i = 0; i < warpData.numValidGCPs; ++i) {

            final Placemark sPin = warpData.slaveGCPList.get(i);
            final PixelPos sGCPPos = sPin.getPixelPos();

            final Placemark mPin = masterGCPGroup.get(sPin.getName());
            final PixelPos mGCPPos = mPin.getPixelPos();

            rangeOffset[i] = mGCPPos.x - sGCPPos.x;
            azimuthOffset[i] = mGCPPos.y - sGCPPos.y;
        }

        avgSlantRangeTimingErr = computeWeightedAverage(rangeOffset) * rangeSpacing / Constants.lightSpeed;
        avgZeroDopplerTimingErr = computeWeightedAverage(azimuthOffset) * lineTimeInterval;
    }

    private double computeWeightedAverage(final double[] array) {

        double mean = 0.0;
        for (double v:array) {
            mean += v;
        }
        mean /= array.length;

        double[] var = new double[array.length];
        for (int i = 0; i < array.length; i++) {
            var[i] = (array[i] - mean)*(array[i] - mean);
        }

        double nominator = 0.0, denominator = 0.0;
        for (int i = 0; i < array.length; i++) {
            if (var[i] > 0.0) {
                nominator += array[i] / var[i];
                denominator += 1.0 / var[i];
            }
        }

        return nominator / denominator;
    }

    /**
     * Update metadata in the target product.
     *
     * @throws OperatorException The exception.
     */
    private void updateTargetProductMetadata() throws OperatorException {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);

        final double newSlantRange = slantRangeToFirstPixel + avgSlantRangeTimingErr*Constants.lightSpeed;
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.slant_range_to_first_pixel, newSlantRange);

        double newFirstLineUTC = firstLineTime + avgZeroDopplerTimingErr;
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.first_line_time, new ProductData.UTC(newFirstLineUTC));

        double newLastLineUTC = lastLineTime + avgZeroDopplerTimingErr;
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.last_line_time, new ProductData.UTC(newLastLineUTC));
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

        final Set<Band> keySet = targetTiles.keySet();
        if (!warpDataAvailable) {
            getWarpData(keySet, targetRectangle);
        }

        // copy slave data to target
        final Band targetBand = targetProduct.getBand(processedSlaveBand);
        final Band slaveBand = sourceProduct.getBand(processedSlaveBand);
        final Tile targetTile = targetTiles.get(targetBand);
        if (targetTile != null) {
            targetTile.setRawSamples(getSourceTile(slaveBand, targetRectangle).getRawSamples());
        }
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(AbsTimingErrCorrectionOp.class);
        }
    }
}
