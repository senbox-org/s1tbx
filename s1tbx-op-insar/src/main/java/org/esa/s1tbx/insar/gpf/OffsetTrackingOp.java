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
package org.esa.s1tbx.insar.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.insar.gpf.coregistration.CrossCorrelationOp;
import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.dataop.resamp.ResamplingFactory;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.*;
import org.jblas.ComplexDouble;
import org.jblas.ComplexDoubleMatrix;
import org.jlinda.core.coregistration.utils.CoregistrationUtils;
import org.jlinda.nest.utils.TileUtilsDoris;

import java.awt.*;
import java.util.ArrayList;
import java.util.Map;

/**
 * This operator performs cross-correlation on selected GCP-patches in master and slave images, and computes
 * glacier velocities based on the shift computed for each GCP. Then velocities for the whole image is computed
 * through interpolation of the velocities computed for GCPs.
 */

@OperatorMetadata(alias = "Offset-Tracking",
        category = "Radar/Feature Extraction",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2016 by Array Systems Computing Inc.",
        description = "Create velocity vectors from offset tracking")
public class OffsetTrackingOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The output grid azimuth spacing in pixels", interval = "(1, *)", defaultValue = "40",
            label = "Grid Azimuth Spacing in Pixels")
    private int gridAzimuthSpacing = 40;

    @Parameter(description = "The output grid range spacing in pixels", interval = "(1, *)", defaultValue = "40",
            label = "Grid Range Spacing in Pixels")
    private int gridRangeSpacing = 40;

    @Parameter(valueSet = {"32", "64", "128", "256", "512", "1024", "2048"}, defaultValue = "128",
            label = "Registration Window Width")
    private String registrationWindowWidth = "128";

    @Parameter(valueSet = {"32", "64", "128", "256", "512", "1024", "2048"}, defaultValue = "128",
            label = "Registration Window Height")
    private String registrationWindowHeight = "128";

    @Parameter(description = "The cross-correlation threshold", interval = "(0, *)", defaultValue = "0.1",
            label = "Cross-Correlation Threshold")
    private double xCorrThreshold = 0.1;

//    @Parameter(valueSet = {"2", "4", "8", "16", "32", "64", "128", "256"},
//            defaultValue = "16", label = "Search Window Accuracy in Azimuth Direction")
    private String registrationWindowAccAzimuth = "16";

//    @Parameter(valueSet = {"2", "4", "8", "16", "32", "64", "128", "256"},
//            defaultValue = "16", label = "Search Window Accuracy in Range Direction")
    private String registrationWindowAccRange = "16";

//    @Parameter(valueSet = {"2", "4", "8", "16", "32", "64"}, defaultValue = "16",
//            label = "Window oversampling factor")
    private String registrationOversampling = "16";

    @Parameter(valueSet = {"3", "5", "9", "11"}, defaultValue = "5",
            label = "Averaging Box Size")
    private String averageBoxSize = "5";

    @Parameter(description = "The threshold for eliminating invalid GCPs", interval = "(0, *)", defaultValue = "5.0",
            label = "Max Velocity (m/day)")
    private double maxVelocity = 5.0;

    @Parameter(description = "Radius for Hole-Filling", interval = "(0, *)", defaultValue = "4",
            label = "Radius for Hole-Filling")
    private int radius = 4;

    @Parameter(valueSet = {ResamplingFactory.NEAREST_NEIGHBOUR_NAME, ResamplingFactory.BILINEAR_INTERPOLATION_NAME,
            ResamplingFactory.BICUBIC_INTERPOLATION_NAME, ResamplingFactory.BISINC_5_POINT_INTERPOLATION_NAME,
            ResamplingFactory.CUBIC_CONVOLUTION_NAME}, defaultValue = ResamplingFactory.BICUBIC_INTERPOLATION_NAME,
            description = "Methods for velocity interpolation.", label = "Resampling Type")
    private String resamplingType = ResamplingFactory.BICUBIC_INTERPOLATION_NAME;

    @Parameter(defaultValue = "false", label = "Turn Off Spacial Average")
    private boolean turnOffSpacialAverage = false;

    @Parameter(defaultValue = "false", label = "Turn Off Fill Hole")
    private boolean turnOffFillHole = false;

    private boolean outputDebuggingBands = false;

    private int cWindowWidth = 0;
    private int cWindowHeight = 0;
    private int cHalfWindowWidth = 0;
    private int cHalfWindowHeight = 0;
    private int avgWindowSize = 0;
    private int halfAvgWindowSize = 0;
    private CrossCorrelationOp.CorrelationWindow corrWin = null;

    private Band masterBand = null;
    private Band slaveBand = null;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private int numGCPsPerAzLine = 0;
    private int numGCPsPerRgLine = 0;
    private int spacingX = 0;
    private int spacingY = 0;
    private int halfSpacingX = 0;
    private int halfSpacingY = 0;
    private double mstFirstLineTime = 0.0;
    private double slvFirstLineTime = 0.0;
    private double acquisitionTimeInterval = 0.0;
    private double rangeSpacing = 0.0;
    private double azimuthSpacing = 0.0;
    private double maxOffset = 0.0;
    private boolean velocityAvailable = false;
    private VelocityData velocityData = null;
    private Resampling selectedResampling = null;

    private final static double invalidIndex = -9999.0;
    private final static String PRODUCT_SUFFIX = "_Vel";
    private final static String VELOCITY = "Velocity";
    private final static String POINTS = "Points";
    private final static String RANGE_SHIFT = "Range_Shift";
    private final static String AZIMUTH_SHIFT = "Azimuth_Shift";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public OffsetTrackingOp() {
    }

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
            selectedResampling = ResamplingFactory.createResampling(resamplingType);
            avgWindowSize = Integer.parseInt(averageBoxSize);
            halfAvgWindowSize = avgWindowSize / 2;

            setRegistrationWindows();

            getMetadata();

            getMasterSlaveBands();

            createTargetProduct();

            createGCPGrid();

            updateTargetProductMetadata();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void setRegistrationWindows() {

        cWindowWidth = Integer.parseInt(registrationWindowWidth);
        cWindowHeight = Integer.parseInt(registrationWindowHeight);
        cHalfWindowWidth = cWindowWidth / 2;
        cHalfWindowHeight = cWindowHeight / 2;

        corrWin = new CrossCorrelationOp.CorrelationWindow(
                Integer.parseInt(registrationWindowWidth),
                Integer.parseInt(registrationWindowHeight),
                Integer.parseInt(registrationWindowAccAzimuth),
                Integer.parseInt(registrationWindowAccRange),
                Integer.parseInt(registrationOversampling));
    }

    private void getMetadata() throws Exception {

        final MetadataElement mstAbsRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);

        final MetadataElement slvAbsRoot = AbstractMetadata.getSlaveMetadata(sourceProduct.getMetadataRoot()).getElementAt(0);

        mstFirstLineTime = AbstractMetadata.parseUTC(
                mstAbsRoot.getAttributeString(AbstractMetadata.first_line_time)).getMJD(); // in days

        rangeSpacing = AbstractMetadata.getAttributeDouble(mstAbsRoot, AbstractMetadata.range_spacing);

        azimuthSpacing = AbstractMetadata.getAttributeDouble(mstAbsRoot, AbstractMetadata.azimuth_spacing);

        slvFirstLineTime = AbstractMetadata.parseUTC(
                slvAbsRoot.getAttributeString(AbstractMetadata.first_line_time)).getMJD(); // in days

        acquisitionTimeInterval = slvFirstLineTime - mstFirstLineTime; // in days

        maxOffset = maxVelocity * acquisitionTimeInterval; // in m
    }

    private void getMasterSlaveBands() {

        masterBand = getSourceBand(sourceProduct, StackUtils.MST);
        slaveBand = getSourceBand(sourceProduct, StackUtils.SLV);
        if(masterBand == null || slaveBand == null) {
            throw new OperatorException("Cannot find master or slave amplitude or intensity band");
        }
    }

    private static Band getSourceBand(final Product sourceProduct, final String tag) {

        for(Band band : sourceProduct.getBands()) {
            if(band.getName().toLowerCase().contains(tag) &&
                    (band.getUnit().contains(Unit.AMPLITUDE) || band.getUnit().contains(Unit.INTENSITY))) {
                return band;
            }
        }
        return null;
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                sourceProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        Band targetBand;
        final String suffix = StackUtils.getBandSuffix(slaveBand.getName());
        final String velocityBandName = VELOCITY + suffix;
        if (targetProduct.getBand(velocityBandName) == null) {
            targetBand = targetProduct.addBand(velocityBandName, ProductData.TYPE_FLOAT32);
            targetBand.setUnit(Unit.METERS_PER_DAY);
            targetBand.setDescription("Velocity");
            targetProduct.setQuicklookBandName(targetBand.getName());
        }

        if (outputDebuggingBands) {
            final String gcpPositionBandName = POINTS + suffix;
            if (targetProduct.getBand(gcpPositionBandName) == null) {
                targetBand = targetProduct.addBand(gcpPositionBandName, ProductData.TYPE_FLOAT32);
                targetBand.setUnit(Unit.METERS_PER_DAY);
                targetBand.setDescription("Velocity Points");
            }

            final String rangeShiftBandName = RANGE_SHIFT + suffix;
            if (targetProduct.getBand(rangeShiftBandName) == null) {
                targetBand = targetProduct.addBand(rangeShiftBandName, ProductData.TYPE_FLOAT32);
                targetBand.setUnit(Unit.METERS_PER_DAY);
                targetBand.setDescription("Range Shift");
            }

            final String azimuthShiftBandName = AZIMUTH_SHIFT + suffix;
            if (targetProduct.getBand(azimuthShiftBandName) == null) {
                targetBand = targetProduct.addBand(azimuthShiftBandName, ProductData.TYPE_FLOAT32);
                targetBand.setUnit(Unit.METERS_PER_DAY);
                targetBand.setDescription("Azimuth Shift");
            }
        }

        // co-registered image should have the same geo-coding as the master image
        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    private void createGCPGrid() {

        numGCPsPerAzLine = sourceImageHeight / gridAzimuthSpacing;
        numGCPsPerRgLine = sourceImageWidth / gridRangeSpacing;
        spacingX = gridRangeSpacing;
        spacingY = gridAzimuthSpacing;
        halfSpacingX = spacingX / 2;
        halfSpacingY = spacingY / 2;
        velocityData = new VelocityData(numGCPsPerAzLine, numGCPsPerRgLine);

        for (int i = 0; i < numGCPsPerAzLine; i++) {
            final int y = halfSpacingY + i*spacingY;
            for (int j = 0; j < numGCPsPerRgLine; j++) {
                final int x = halfSpacingX + j*spacingX;
                velocityData.mstGCPx[i][j] = x;
                velocityData.mstGCPy[i][j] = y;
                velocityData.slvGCPx[i][j] = invalidIndex;
                velocityData.slvGCPy[i][j] = invalidIndex;
            }
        }
    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        AbstractMetadata.setAttribute(absTgt, AbstractMetadata.coregistered_stack, 1);
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException
     *          If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;
        //System.out.println("OffsetTrackingOp: x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        try {
            if (pm.isCanceled())
                return;

            if (!velocityAvailable) {
                computeVelocity();
            }

            Tile tgtRangeShiftTile = null;
            Tile tgtAzimuthShiftTile = null;
            Tile tgtVelocityTile = null;
            Tile tgtGCPPositionTile = null;
            ProductData tgtRangeShiftBuffer = null;
            ProductData tgtAzimuthShiftBuffer = null;
            ProductData tgtVelocityBuffer = null;
            ProductData tgtGCPPositionBuffer = null;
            final Band[] targetBands = targetProduct.getBands();
            for (Band tgtBand:targetBands) {
                final String tgtBandName = tgtBand.getName();
                if (tgtBandName.contains(RANGE_SHIFT)) {
                    tgtRangeShiftTile = targetTileMap.get(tgtBand);
                    tgtRangeShiftBuffer = tgtRangeShiftTile.getDataBuffer();
                } else if (tgtBandName.contains(AZIMUTH_SHIFT)) {
                    tgtAzimuthShiftTile = targetTileMap.get(tgtBand);
                    tgtAzimuthShiftBuffer = tgtAzimuthShiftTile.getDataBuffer();
                } else if (tgtBandName.contains(VELOCITY)) {
                    tgtVelocityTile = targetTileMap.get(tgtBand);
                    tgtVelocityBuffer = tgtVelocityTile.getDataBuffer();
                } else if (tgtBandName.contains(POINTS)) {
                    tgtGCPPositionTile = targetTileMap.get(tgtBand);
                    tgtGCPPositionBuffer = tgtGCPPositionTile.getDataBuffer();
                }
            }

            if (tgtVelocityBuffer == null || outputDebuggingBands &&
                    (tgtGCPPositionBuffer == null || tgtRangeShiftBuffer == null || tgtAzimuthShiftBuffer == null)) {
                throw new OperatorException("Cannot find desired target bands");
            }

            final TileIndex tgtIndex = new TileIndex(tgtVelocityTile);

            final ResamplingRaster resamplingRasterVelocity = new ResamplingRaster(tgtVelocityTile, velocityData.velocity);
            ResamplingRaster resamplingRasterRangeShift = null;
            ResamplingRaster resamplingRasterAzimuthShift = null;
            if (outputDebuggingBands) {
                resamplingRasterRangeShift = new ResamplingRaster(tgtRangeShiftTile, velocityData.rangeShift);
                resamplingRasterAzimuthShift = new ResamplingRaster(tgtAzimuthShiftTile, velocityData.azimuthShift);
            }

            final Resampling.Index resamplingIndex = selectedResampling.createIndex();

            for (int y = y0; y < yMax; y++) {
                tgtIndex.calculateStride(y);
                final double i = (double)(y - halfSpacingY) / (double)spacingY;
                for (int x = x0; x < xMax; x++) {
                    final int tgtIdx = tgtIndex.getIndex(x);
                    final double j = (double)(x - halfSpacingX) / (double)spacingX;

                    selectedResampling.computeCornerBasedIndex(j, i, numGCPsPerRgLine, numGCPsPerAzLine, resamplingIndex);

                    tgtVelocityBuffer.setElemFloatAt(tgtIdx,
                            (float)selectedResampling.resample(resamplingRasterVelocity, resamplingIndex));

                    if (outputDebuggingBands) {
                        tgtRangeShiftBuffer.setElemFloatAt(tgtIdx,
                                (float)selectedResampling.resample(resamplingRasterRangeShift, resamplingIndex));

                        tgtAzimuthShiftBuffer.setElemFloatAt(tgtIdx,
                                (float)selectedResampling.resample(resamplingRasterAzimuthShift, resamplingIndex));
                    }
                }
            }

            // output GCP positions
            if (outputDebuggingBands && tgtGCPPositionBuffer != null) {
                for (int i = 0; i < numGCPsPerAzLine; i++) {
                    for (int j = 0; j < numGCPsPerRgLine; j++) {
                        final int x = (int) velocityData.mstGCPx[i][j];
                        final int y = (int) velocityData.mstGCPy[i][j];
                        if (velocityData.slvGCPx[i][j] != invalidIndex && velocityData.slvGCPy[i][j] != invalidIndex
                                && x >= x0 && x < xMax && y >= y0 && y < yMax) {
                            tgtGCPPositionBuffer.setElemFloatAt(
                                    tgtGCPPositionTile.getDataBufferIndex(x, y), (float) velocityData.velocity[i][j]);
                        /*
                        tgtGCPPositionBuffer.setElemFloatAt(tgtGCPPositionTile.getDataBufferIndex(x, y), 100.0f);
                        tgtGCPPositionBuffer.setElemFloatAt(tgtGCPPositionTile.getDataBufferIndex(x-2, y), 100.0f);
                        tgtGCPPositionBuffer.setElemFloatAt(tgtGCPPositionTile.getDataBufferIndex(x-1, y), 100.0f);
                        tgtGCPPositionBuffer.setElemFloatAt(tgtGCPPositionTile.getDataBufferIndex(x+1, y), 100.0f);
                        tgtGCPPositionBuffer.setElemFloatAt(tgtGCPPositionTile.getDataBufferIndex(x+2, y), 100.0f);
                        tgtGCPPositionBuffer.setElemFloatAt(tgtGCPPositionTile.getDataBufferIndex(x, y-2), 100.0f);
                        tgtGCPPositionBuffer.setElemFloatAt(tgtGCPPositionTile.getDataBufferIndex(x, y-1), 100.0f);
                        tgtGCPPositionBuffer.setElemFloatAt(tgtGCPPositionTile.getDataBufferIndex(x, y+1), 100.0f);
                        tgtGCPPositionBuffer.setElemFloatAt(tgtGCPPositionTile.getDataBufferIndex(x, y+2), 100.0f);
                        */
                        }
                    }
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    private synchronized void computeVelocity() {

        if (velocityAvailable) return;

        computeSlaveGCPs();

        computeGCPOffsets();

        if (!turnOffSpacialAverage) {
            averageOffsets();
        }

        if (!turnOffFillHole) {
            fillHoles();
        }

        computeGCPVelocities();

        writeGCPsToMetadata();

        velocityAvailable = true;
    }

    private void computeSlaveGCPs() {

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Compute slave GCP... ", numGCPsPerAzLine * numGCPsPerRgLine);

        final ThreadManager threadManager = new ThreadManager();
        try {
            for (int i = 0; i < numGCPsPerAzLine; i++) {
                checkForCancellation();
                for (int j = 0; j < numGCPsPerRgLine; j++) {
                    final int iIdx = i;
                    final int jIdx = j;

                    final PixelPos mGCP = new PixelPos(velocityData.mstGCPx[i][j], velocityData.mstGCPy[i][j]);
                    if (!checkGCPValidity(mGCP)) {
                        continue;
                    }

                    final Thread worker = new Thread() {
                        @Override
                        public void run() {
                            final PixelPos sGCP = new PixelPos(mGCP.x, mGCP.y);
                            boolean getSlaveGCP = getOffsets(mGCP, sGCP);
                            if (getSlaveGCP) {
                                saveSlaveGCP(sGCP);
                            }
                        }

                        private synchronized void saveSlaveGCP(final PixelPos sGCP) {
                            velocityData.slvGCPx[iIdx][jIdx] = sGCP.x;
                            velocityData.slvGCPy[iIdx][jIdx] = sGCP.y;
                        }
                    };
                    threadManager.add(worker);
                    status.worked(1);
                }
            }
            status.done();
            threadManager.finish();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeGCPsByXCorrelation", e);
        }
    }

    private void computeGCPOffsets() {

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Compute Offsets... ", numGCPsPerAzLine * numGCPsPerRgLine);

        final ThreadManager threadManager = new ThreadManager();
        try {
            for (int i = 0; i < numGCPsPerAzLine; i++) {
                for (int j = 0; j < numGCPsPerRgLine; j++) {
                    checkForCancellation();
                    final int iIdx = i;
                    final int jIdx = j;

                    if (velocityData.slvGCPx[i][j] == invalidIndex || velocityData.slvGCPy[i][j] == invalidIndex) {
                        continue;
                    }

                    final Thread worker = new Thread() {
                        @Override
                        public void run() {

                            final double xShift =
                                    (velocityData.mstGCPx[iIdx][jIdx] - velocityData.slvGCPx[iIdx][jIdx])*rangeSpacing;

                            final double yShift =
                                    (velocityData.mstGCPy[iIdx][jIdx] - velocityData.slvGCPy[iIdx][jIdx])*azimuthSpacing;

                            final double offset = Math.sqrt(xShift * xShift + yShift * yShift);

                            if (offset <= maxOffset) {
                                saveOffset(xShift, yShift);
                            } else { // outliers
                                synchronized(velocityData.slvGCPx) {
                                    velocityData.slvGCPx[iIdx][jIdx] = invalidIndex;
                                    velocityData.slvGCPy[iIdx][jIdx] = invalidIndex;
                                }
                            }
                        }

                        private synchronized void saveOffset(final double xShift, final double yShift) {
                            velocityData.rangeShift[iIdx][jIdx] = xShift;
                            velocityData.azimuthShift[iIdx][jIdx] = yShift;
                        }
                    };
                    threadManager.add(worker);
                    status.worked(1);
                }
            }
            status.done();
            threadManager.finish();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeGCPOffsets", e);
        }
    }

    private void averageOffsets() {

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Average Offsets... ", numGCPsPerAzLine * numGCPsPerRgLine);

        final ThreadManager threadManager = new ThreadManager();
        try {
            for (int i = 0; i < numGCPsPerAzLine; i++) {
                for (int j = 0; j < numGCPsPerRgLine; j++) {
                    checkForCancellation();
                    final int iIdx = i;
                    final int jIdx = j;

                    if (velocityData.slvGCPx[i][j] == invalidIndex || velocityData.slvGCPy[i][j] == invalidIndex) {
                        continue;
                    }

                    final Thread worker = new Thread() {
                        @Override
                        public void run() {

                            final int i0 = Math.max(iIdx - halfAvgWindowSize, 0);
                            final int iN = Math.min(iIdx + halfAvgWindowSize, numGCPsPerAzLine - 1);
                            final int j0 = Math.max(jIdx - halfAvgWindowSize, 0);
                            final int jN = Math.min(jIdx + halfAvgWindowSize, numGCPsPerRgLine - 1);

                            int count = 0;
                            double rangeShiftSum = 0.0, azimuthShiftSum = 0.0;
                            for (int ii = i0; ii <= iN; ii++) {
                                for (int jj = j0; jj <= jN; jj++) {
                                    if (velocityData.slvGCPx[ii][jj] != invalidIndex &&
                                            velocityData.slvGCPy[ii][jj] != invalidIndex) {

                                        rangeShiftSum += velocityData.rangeShift[ii][jj];
                                        azimuthShiftSum += velocityData.azimuthShift[ii][jj];
                                        count++;
                                    }
                                }
                            }

                            if (count > 0) {
                                final double xShift = rangeShiftSum / count;

                                final double yShift = azimuthShiftSum / count;

                                final double slvGCPx = velocityData.mstGCPx[iIdx][jIdx] - xShift / rangeSpacing;

                                final double slvGCPy = velocityData.mstGCPy[iIdx][jIdx] - yShift / azimuthSpacing;

                                saveOffset(xShift, yShift, slvGCPx, slvGCPy);
                            }
                        }

                        private synchronized void saveOffset(
                                final double xShift, final double yShift, final double slvGCPx, final double slvGCPy) {
                            velocityData.rangeShift[iIdx][jIdx] = xShift;
                            velocityData.azimuthShift[iIdx][jIdx] = yShift;
                            velocityData.slvGCPx[iIdx][jIdx] = slvGCPx;
                            velocityData.slvGCPy[iIdx][jIdx] = slvGCPy;
                        }
                    };
                    threadManager.add(worker);
                    status.worked(1);
                }
            }
            status.done();
            threadManager.finish();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("averageOffsets", e);
        }
    }

    private void fillHoles() {

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Fill Holes... ", numGCPsPerAzLine * numGCPsPerRgLine);

        final ThreadManager threadManager = new ThreadManager();
        try {
            final java.util.List<int[]> holeList = new ArrayList<>();
            for (int i = 0; i < numGCPsPerAzLine; i++) {
                for (int j = 0; j < numGCPsPerRgLine; j++) {
                    if (velocityData.slvGCPx[i][j] == invalidIndex || velocityData.slvGCPy[i][j] == invalidIndex) {
                        holeList.add(new int[]{i, j});
                    }
                }
            }

            for (int k = 0; k < holeList.size(); k++) {
                checkForCancellation();
                final int iIdx = holeList.get(k)[0];
                final int jIdx = holeList.get(k)[1];

                final Thread worker = new Thread() {
                    @Override
                    public void run() {

                        final int i0 = Math.max(iIdx - radius, 0);
                        final int iN = Math.min(iIdx + radius, numGCPsPerAzLine - 1);
                        final int j0 = Math.max(jIdx - radius, 0);
                        final int jN = Math.min(jIdx + radius, numGCPsPerRgLine - 1);

                        double xShiftMean = 0.0, yShiftMean = 0.0, totalWeight = 0.0;
                        for (int ii = i0; ii <= iN; ii++) {
                            for (int jj = j0; jj <= jN; jj++) {
                                if (!inList(ii, jj)) {

                                    final double w = 1.0 / Math.max(Math.abs(ii - iIdx), Math.abs(jj - jIdx));

                                    xShiftMean += w * velocityData.rangeShift[ii][jj];

                                    yShiftMean += w * velocityData.azimuthShift[ii][jj];

                                    totalWeight += w;
                                }
                            }
                        }

                        if (totalWeight != 0.0) {
                            xShiftMean /= totalWeight;
                            yShiftMean /= totalWeight;

                            final double slvGCPx = velocityData.mstGCPx[iIdx][jIdx] - xShiftMean / rangeSpacing;
                            final double slvGCPy = velocityData.mstGCPy[iIdx][jIdx] - yShiftMean / azimuthSpacing;

                            saveOffset(xShiftMean, yShiftMean, slvGCPx, slvGCPy);
                        }
                    }

                    private boolean inList(final int ii, final int jj) {

                        for (int k = 0; k < holeList.size(); k++) {
                            if (holeList.get(k)[0] == ii && holeList.get(k)[1] == jj) {
                                return true;
                            }
                        }
                        return false;
                    }

                    private synchronized void saveOffset(
                            final double xShift, final double yShift, final double slvGCPx, final double slvGCPy) {
                        velocityData.rangeShift[iIdx][jIdx] = xShift;
                        velocityData.azimuthShift[iIdx][jIdx] = yShift;
                        velocityData.slvGCPx[iIdx][jIdx] = slvGCPx;
                        velocityData.slvGCPy[iIdx][jIdx] = slvGCPy;
                    }
                };
                threadManager.add(worker);
                status.worked(1);
            }
            status.done();
            threadManager.finish();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("fillHoles", e);
        }
    }

    private void computeGCPVelocities() {

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Compute Velocities... ", numGCPsPerAzLine * numGCPsPerRgLine);

        final ThreadManager threadManager = new ThreadManager();
        try {
            for (int i = 0; i < numGCPsPerAzLine; i++) {
                for (int j = 0; j < numGCPsPerRgLine; j++) {
                    checkForCancellation();
                    final int iIdx = i;
                    final int jIdx = j;

                    if (velocityData.slvGCPx[i][j] == invalidIndex || velocityData.slvGCPy[i][j] == invalidIndex) {
                        continue;
                    }

                    final Thread worker = new Thread() {
                        @Override
                        public void run() {

                            final double xShift = velocityData.rangeShift[iIdx][jIdx];
                            final double yShift = velocityData.azimuthShift[iIdx][jIdx];
                            final double v = Math.sqrt(xShift * xShift + yShift * yShift) / acquisitionTimeInterval;
                            saveVelocity(v);
                        }

                        private synchronized void saveVelocity(final double v) {
                            velocityData.velocity[iIdx][jIdx] = v;
                        }
                    };
                    threadManager.add(worker);
                    status.worked(1);
                }
            }
            status.done();
            threadManager.finish();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("computeGCPVelocities", e);
        }
    }

    private boolean checkGCPValidity(final PixelPos pixelPos) {

        return (pixelPos.x - cHalfWindowWidth + 1 >= 0 && pixelPos.x + cHalfWindowWidth <= sourceImageWidth - 1) &&
               (pixelPos.y - cHalfWindowHeight + 1 >= 0 && pixelPos.y + cHalfWindowHeight <= sourceImageHeight - 1);
    }

    private boolean getOffsets(final PixelPos mGCPPixelPos, final PixelPos sGCPPixelPos) {

        try {
            final ComplexDoubleMatrix mI = getComplexDoubleMatrix(masterBand, null, mGCPPixelPos, corrWin);
            final ComplexDoubleMatrix sI = getComplexDoubleMatrix(slaveBand, null, sGCPPixelPos, corrWin);

            final double[] coarseOffset = {0, 0};

            double coherence = CoregistrationUtils.crossCorrelateFFT(
                    coarseOffset, mI, sI, corrWin.ovsFactor, corrWin.accY, corrWin.accX);

//            double coherence = CoregistrationUtils.normalizedCrossCorrelation(
//                    coarseOffset, mI, sI, corrWin.ovsFactor, corrWin.accY, corrWin.accX);

            if (coherence < xCorrThreshold) {
                return false;
            } else {
                sGCPPixelPos.x += coarseOffset[1];
                sGCPPixelPos.y += coarseOffset[0];
                return true;
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId() + " getOffsets ", e);
        }
        return false;
    }

    private void dumpComplexMatrix(ComplexDoubleMatrix I, final String title) {

        System.out.println(title);
        final int numRows = I.rows;
        final int numCols = I.columns;
        for (int r = 0; r < numRows; r++) {
            for (int c = 0; c < numCols; c++) {
                ComplexDouble v = I.get(r,c);
//                System.out.print(v.real() + " + j*" + v.imag() + ", ");
                System.out.print(v.real() + ", ");
            }
            System.out.println();
        }
        System.out.println();
    }

    private ComplexDoubleMatrix getComplexDoubleMatrix(
            final Band band1, final Band band2, final PixelPos pixelPos, final CrossCorrelationOp.CorrelationWindow corrWindow) {

        Rectangle rectangle = corrWindow.defineRectangleMask(pixelPos);
        Tile tileReal = getSourceTile(band1, rectangle);

        Tile tileImag = null;
        if (band2 != null) {
            tileImag = getSourceTile(band2, rectangle);
        }
        return TileUtilsDoris.pullComplexDoubleMatrix(tileReal, tileImag);
    }

    private void writeGCPsToMetadata() {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
        final String suffix = StackUtils.getBandSuffix(slaveBand.getName());
        final String velocityBandName = VELOCITY + suffix;

        final MetadataElement bandElem = AbstractMetadata.getBandAbsMetadata(absRoot, velocityBandName, true);

        MetadataElement warpDataElem = bandElem.getElement("WarpData");
        if (warpDataElem == null) {
            warpDataElem = new MetadataElement("WarpData");
            bandElem.addElement(warpDataElem);
        } else {
            // empty out element
            final MetadataAttribute[] attribList = warpDataElem.getAttributes();
            for (MetadataAttribute attrib : attribList) {
                warpDataElem.removeAttribute(attrib);
            }
        }

        int k = 0;
        for (int i = 0; i < numGCPsPerAzLine; i++) {
            for (int j = 0; j < numGCPsPerRgLine; j++) {
                if (velocityData.slvGCPx[i][j] != invalidIndex && velocityData.slvGCPx[i][j] != invalidIndex) {
                    final MetadataElement gcpElem = new MetadataElement("GCP" + k);
                    warpDataElem.addElement(gcpElem);

                    gcpElem.setAttributeDouble("mst_x", velocityData.mstGCPx[i][j]);
                    gcpElem.setAttributeDouble("mst_y", velocityData.mstGCPy[i][j]);
                    gcpElem.setAttributeDouble("slv_x", velocityData.slvGCPx[i][j]);
                    gcpElem.setAttributeDouble("slv_y", velocityData.slvGCPy[i][j]);
                    k++;
                }
            }
        }
    }

    public static class VelocityData {
        public final double[][] mstGCPx;
        public final double[][] mstGCPy;
        public final double[][] slvGCPx;
        public final double[][] slvGCPy;
        public final double[][] velocity;
        public final double[][] rangeShift;
        public final double[][] azimuthShift;

        public VelocityData(final int numGCPsPerAzimuthLine, final int numGCPsPerRangeLine) {
            this.mstGCPx = new double[numGCPsPerAzimuthLine][numGCPsPerRangeLine];
            this.mstGCPy = new double[numGCPsPerAzimuthLine][numGCPsPerRangeLine];
            this.slvGCPx = new double[numGCPsPerAzimuthLine][numGCPsPerRangeLine];
            this.slvGCPy = new double[numGCPsPerAzimuthLine][numGCPsPerRangeLine];
            this.rangeShift = new double[numGCPsPerAzimuthLine][numGCPsPerRangeLine];
            this.azimuthShift = new double[numGCPsPerAzimuthLine][numGCPsPerRangeLine];
            this.velocity = new double[numGCPsPerAzimuthLine][numGCPsPerRangeLine];
        }
    }

    private static class ResamplingRaster implements Resampling.Raster {

        private final Tile tile;
        private final double[][] data;
        private final boolean usesNoData;
        private final boolean scalingApplied;
        private final double noDataValue;
        private final double geophysicalNoDataValue;

        public ResamplingRaster(final Tile tile, final double[][] data) {
            this.tile = tile;
            this.data = data;
            final RasterDataNode rasterDataNode = tile.getRasterDataNode();
            this.usesNoData = rasterDataNode.isNoDataValueUsed();
            this.noDataValue = rasterDataNode.getNoDataValue();
            this.geophysicalNoDataValue = rasterDataNode.getGeophysicalNoDataValue();
            this.scalingApplied = rasterDataNode.isScalingApplied();
        }

        public final int getWidth() {
            return tile.getWidth();
        }

        public final int getHeight() {
            return tile.getHeight();
        }

        public boolean getSamples(final int[] x, final int[] y, final double[][] samples) throws Exception {
            boolean allValid = true;

            try {
                double val;
                int i = 0;
                while (i < y.length) {
                    int j = 0;
                    while (j < x.length) {
                        val = data[y[i]][x[j]];

                        if (usesNoData) {
                            if (scalingApplied && geophysicalNoDataValue == val || noDataValue == val) {
                                val = Double.NaN;
                                allValid = false;
                            }
                        }
                        samples[i][j] = val;
                        ++j;
                    }
                    ++i;
                }
            } catch (Exception e) {
                SystemUtils.LOG.severe(e.getMessage());
                allValid = false;
            }

            return allValid;
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
            super(OffsetTrackingOp.class);
        }
    }
}
