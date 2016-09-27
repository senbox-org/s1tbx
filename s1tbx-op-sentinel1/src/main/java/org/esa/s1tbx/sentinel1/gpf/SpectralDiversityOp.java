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
package org.esa.s1tbx.sentinel1.gpf;

import com.bc.ceres.core.ProgressMonitor;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.insar.gpf.support.Sentinel1Utils;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.dataop.downloadable.StatusProgressMonitor;
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
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.gpf.ThreadManager;
import org.esa.snap.engine_utilities.gpf.TileIndex;
import org.jblas.ComplexDoubleMatrix;
import org.jlinda.core.coregistration.utils.CoregistrationUtils;
import org.jlinda.nest.utils.TileUtilsDoris;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Estimate range and azimuth offsets for each burst using cross-correlation with a 512x512 block in
 * the center of the burst. Then average the offsets computed for all bursts in the same sub-swath to
 * get one constant offset for the whole sub-swath.
 *
 * Perform range shift for all bursts in a sub-swath with the constant range offset computed above using
 * a frequency domain method.
 */

@OperatorMetadata(alias = "Enhanced-Spectral-Diversity",
        category = "Radar/Coregistration/S-1 TOPS Coregistration",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2016 by Array Systems Computing Inc.",
        description = "Estimate constant range and azimuth offsets for the whole image")
public class SpectralDiversityOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct(description = "The target product which will use the master's grid.")
    private Product targetProduct = null;

    @Parameter(valueSet = {"32", "64", "128","256", "512", "1024", "2048"}, defaultValue = "512",
            label = "Registration Window Width")
    private String fineWinWidthStr = "512";

    @Parameter(valueSet = {"32", "64", "128","256", "512", "1024", "2048"}, defaultValue = "512",
            label = "Registration Window Height")
    private String fineWinHeightStr = "512";

    @Parameter(valueSet = {"2", "4", "8", "16", "32", "64"}, defaultValue = "16",
            label = "Search Window Accuracy in Azimuth Direction")
    private String fineWinAccAzimuth = "16";

    @Parameter(valueSet = {"2", "4", "8", "16", "32", "64"}, defaultValue = "16",
            label = "Search Window Accuracy in Range Direction")
    private String fineWinAccRange = "16";

    @Parameter(valueSet = {"32", "64", "128", "256"}, defaultValue = "128",
            label = "Window oversampling factor")
    private String fineWinOversampling = "128";

    @Parameter(description = "The peak cross-correlation threshold", interval = "(0, *)", defaultValue = "0.1",
            label = "Cross-Correlation Threshold")
    private double xCorrThreshold = 0.1;

    @Parameter(description = "The coherence threshold for outlier removal", interval = "(0, 1]", defaultValue = "0.15",
            label = "Coherence Threshold for Outlier Removal")
    private double cohThreshold = 0.15;

    @Parameter(description = "The number of windows per overlap for ESD", interval = "[1, 20]", defaultValue = "10",
            label = "Number of Windows Per Overlap for ESD")
    private int numBlocksPerOverlap = 10;

    @Parameter(description = "Use user supplied range and azimuth shifts", defaultValue = "false",
            label = "Use user supplied shifts")
    private boolean useSuppliedShifts = false;

    @Parameter(description = "The overall azimuth shift", defaultValue = "0.0",
            label = "The overall azimuth shift")
    private double overallAzimuthShift = 0.0;

    @Parameter(description = "The overall range shift", defaultValue = "0.0",
            label = "The overall range shift")
    private double overallRangeShift = 0.0;

    private int fineWinWidth = 0;
    private int fineWinHeight = 0;
    private int fineWinAccY = 0;
    private int fineWinAccX = 0;
    private int fineWinOvsFactor = 0;

    private boolean isRangeOffsetAvailable = false;
    private boolean isAzimuthOffsetAvailable = false;
    private double azOffset = 0.0;
    private double rgOffset = 0.0;
    private Double noDataValue = -9999.0;
    private Sentinel1Utils su;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;
    private int subSwathIndex = 0;

    private String swathIndexStr = null;
    private String[] subSwathNames = null;
    private String[] polarizations = null;

    private static final int cohWin = 5; // window size for coherence calculation
    private static final int maxRangeShift = 1;
    private static final String DerampDemodPhase = "derampDemodPhase";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public SpectralDiversityOp() {
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
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSARProduct();
            validator.checkIfSentinel1Product();

            su = new Sentinel1Utils(sourceProduct);
            su.computeDopplerRate();
            su.computeReferenceTime();
            subSwath = su.getSubSwath();
            polarizations = su.getPolarizations();

            subSwathNames = su.getSubSwathNames();
            if (subSwathNames.length != 1) {
                throw new OperatorException("Split product is expected.");
            } else {
                subSwathIndex = 1; // subSwathIndex is always 1 because of split product
                swathIndexStr = subSwathNames[0].substring(2);
            }

            if (useSuppliedShifts) {

                azOffset = overallAzimuthShift;
                rgOffset = overallRangeShift;
                isRangeOffsetAvailable = true;
                isAzimuthOffsetAvailable = true;

            } else {

                fineWinWidth = Integer.parseInt(fineWinWidthStr);
                fineWinHeight = Integer.parseInt(fineWinHeightStr);
                fineWinAccY = Integer.parseInt(fineWinAccAzimuth);
                fineWinAccX = Integer.parseInt(fineWinAccRange);
                fineWinOvsFactor = Integer.parseInt(fineWinOversampling);

                if (subSwath[subSwathIndex - 1].samplesPerBurst < fineWinWidth) {
                    throw new OperatorException("Registration window width should not be grater than burst width " +
                            subSwath[subSwathIndex - 1].samplesPerBurst);
                }

                if (subSwath[subSwathIndex - 1].linesPerBurst < fineWinHeight) {
                    throw new OperatorException("Registration window height should not be grater than burst height " +
                            subSwath[subSwathIndex - 1].linesPerBurst);
                }
            }

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        final String[] srcBandNames = sourceProduct.getBandNames();
        for (String srcBandName : srcBandNames) {
            final Band band = sourceProduct.getBand(srcBandName);
            if (band instanceof VirtualBand) {
                continue;
            }

            Band targetBand;
            if (srcBandName.contains(StackUtils.MST)) {
                targetBand = ProductUtils.copyBand(srcBandName, sourceProduct, srcBandName, targetProduct, true);
            } else if (srcBandName.contains("azOffset") || srcBandName.contains("rgOffset") ||
                    srcBandName.contains("derampDemod")) {
                continue;
            } else {
                targetBand = new Band(srcBandName,
                        band.getDataType(),
                        band.getRasterWidth(),
                        band.getRasterHeight());

                targetBand.setUnit(band.getUnit());
                targetProduct.addBand(targetBand);
            }

            if(targetBand != null && srcBandName.startsWith("q_")) {
                final String suffix = srcBandName.substring(1);
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetProduct.getBand('i'+suffix), targetBand, suffix);
            }
        }

        targetProduct.setPreferredTileSize(512, subSwath[subSwathIndex - 1].linesPerBurst);
        updateTargetMetadata();
    }

    private void updateTargetMetadata() {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        MetadataElement ESDMeasurement = new MetadataElement("ESD Measurement");

        final MetadataElement OverallRgAzShiftElem = new MetadataElement("Overall_Range_Azimuth_Shift");
        OverallRgAzShiftElem.addElement(new MetadataElement(subSwathNames[0]));
        ESDMeasurement.addElement(OverallRgAzShiftElem);

        final MetadataElement RgShiftPerBurstElem = new MetadataElement("Range_Shift_Per_Burst");
        RgShiftPerBurstElem.addElement(new MetadataElement(subSwathNames[0]));
        ESDMeasurement.addElement(RgShiftPerBurstElem);

        final MetadataElement AzShiftPerOverlapElem = new MetadataElement("Azimuth_Shift_Per_Overlap");
        AzShiftPerOverlapElem.addElement(new MetadataElement(subSwathNames[0]));
        ESDMeasurement.addElement(AzShiftPerOverlapElem);

        final MetadataElement AzShiftPerBlockElem = new MetadataElement("Azimuth_Shift_Per_Block");
        AzShiftPerBlockElem.addElement(new MetadataElement(subSwathNames[0]));
        ESDMeasurement.addElement(AzShiftPerBlockElem);

        absTgt.addElement(ESDMeasurement);
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

        try {
            if (!isRangeOffsetAvailable) {
                estimateRangeOffset();
            }
            if (!isAzimuthOffsetAvailable) {
                estimateAzimuthOffset();
            }

            // perform range and azimuth shift using FFT
            for(String polarization : polarizations) {
                final Band sBandI = getBand(StackUtils.SLV, "i_", swathIndexStr, polarization);
                final Band sBandQ = getBand(StackUtils.SLV, "q_", swathIndexStr, polarization);

                performRangeAzimuthShift(sBandI, sBandQ, targetRectangle, targetTileMap);
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Estimate range and azimuth offset using cross-correlation.
     */
    private synchronized void estimateRangeOffset() {

        if (isRangeOffsetAvailable) {
            return;
        }

        final int numBursts = subSwath[subSwathIndex - 1].numOfBursts;
        final List<Double> azOffsetArray = new ArrayList<>(numBursts);
        final List<Double> rgOffsetArray = new ArrayList<>(numBursts);
        final List<Integer> burstIndexArray = new ArrayList<>(numBursts);

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Estimating range offsets... ", numBursts);

        final ThreadManager threadManager = new ThreadManager();
        try {
            final Band mBandI = getBand(StackUtils.MST, "i_", swathIndexStr, polarizations[0]);
            final Band mBandQ = getBand(StackUtils.MST, "q_", swathIndexStr, polarizations[0]);
            final Band sBandI = getBand(StackUtils.SLV, "i_", swathIndexStr, polarizations[0]);
            final Band sBandQ = getBand(StackUtils.SLV, "q_", swathIndexStr, polarizations[0]);

            for (int i = 0; i < numBursts; i++) {
                checkForCancellation();
                final int burstIndex = i;

                final Thread worker = new Thread() {
                    @Override
                    public void run() {
                        try {
                            final double[] offset = new double[2]; // az/rg offset

                            estimateAzRgOffsets(mBandI, mBandQ, sBandI, sBandQ, burstIndex, offset);

                            synchronized(azOffsetArray) {
                                azOffsetArray.add(offset[0]);
                                rgOffsetArray.add(offset[1]);
                                burstIndexArray.add(burstIndex);
                            }
                        } catch (Throwable e) {
                            OperatorUtils.catchOperatorException("estimateOffset", e);
                        }
                    }
                };
                threadManager.add(worker);
                status.worked(1);
            }
            status.done();
            threadManager.finish();

            double sumAzOffset = 0.0;
            double sumRgOffset = 0.0;
            int count = 0;
            for (int i = 0; i < azOffsetArray.size(); i++) {
                final double azShift = azOffsetArray.get(i);
                final double rgShift = rgOffsetArray.get(i);

                SystemUtils.LOG.fine("RangeShiftOp: burst = " + burstIndexArray.get(i) + ", range offset = " + rgShift
                                             + ", azimuth offset = " + azShift);

                if (noDataValue.equals(azShift) || noDataValue.equals(rgShift)) {
                    continue;
                }

                if (Math.abs(rgShift) > maxRangeShift) {
                    continue;
                }

                sumAzOffset += azShift;
                sumRgOffset += rgShift;
                count++;
            }

            if (count > 0) {
                //azOffset = sumAzOffset / (double)count;
                rgOffset = sumRgOffset / (double)count;
            } else {
                rgOffset = 0.0;
                SystemUtils.LOG.warning("RangeShiftOp: Cross-correlation failed for all bursts, set range shift to 0");
            }

            saveOverallRangeShift(rgOffset);

            saveRangeShiftPerBurst(rgOffsetArray, burstIndexArray);

            //SystemUtils.LOG.fine("RangeShiftOp: whole image azimuth offset = " + azOffset);
            SystemUtils.LOG.fine("RangeShiftOp: Overall range shift = " + rgOffset);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("estimateOffset", e);
        }

        isRangeOffsetAvailable = true;
    }

    private void estimateAzRgOffsets(final Band mBandI, final Band mBandQ, final Band sBandI, final Band sBandQ,
                                     final int burstIndex, final double[] offset) {

        final int burstHeight = subSwath[subSwathIndex - 1].linesPerBurst;
        final int burstWidth = subSwath[subSwathIndex - 1].samplesPerBurst;
        final int x0 = burstWidth / 2;
        final int y0 = burstHeight / 2 + burstIndex * burstHeight;
        final PixelPos mGCP = new PixelPos(x0, y0);
        final PixelPos sGCP = new PixelPos(x0, y0);

        getFineOffsets(mBandI, mBandQ, sBandI, sBandQ, mGCP, sGCP, offset);
    }

    private void getFineOffsets(final Band mBandI, final Band mBandQ, final Band sBandI, final Band sBandQ,
                                final PixelPos mGCPPixelPos, final PixelPos sGCPPixelPos, final double[] offset) {

        try {
            ComplexDoubleMatrix mI = getComplexDoubleMatrix(
                    mBandI, mBandQ, mGCPPixelPos, fineWinWidth, fineWinHeight);

            ComplexDoubleMatrix sI = getComplexDoubleMatrix(
                    sBandI, sBandQ, sGCPPixelPos, fineWinWidth, fineWinHeight);

            final double[] fineOffset = {0, 0};

            final double coherence = CoregistrationUtils.crossCorrelateFFT(
                    fineOffset, mI, sI, fineWinOvsFactor, fineWinAccY, fineWinAccX);

//            final double coherence = CoregistrationUtils.normalizedCrossCorrelation(
//                    fineOffset, mI, sI, fineWinOvsFactor, fineWinAccY, fineWinAccX);

            if (coherence < xCorrThreshold) {
                offset[0] = noDataValue;
                offset[1] = noDataValue;
            } else {
                offset[0] = -fineOffset[0];
                offset[1] = -fineOffset[1];
            }
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId() + " getFineOffsets ", e);
        }
    }

    private ComplexDoubleMatrix getComplexDoubleMatrix(
            final Band band1, final Band band2, final PixelPos pixelPos, final int fineWinWidth, final int fineWinHeight) {

        Rectangle rectangle = defineRectangleMask(pixelPos, fineWinWidth, fineWinHeight);
        Tile tileReal = getSourceTile(band1, rectangle);
        Tile tileImag = getSourceTile(band2, rectangle);
        return TileUtilsDoris.pullComplexDoubleMatrix(tileReal, tileImag);
    }

    private static Rectangle defineRectangleMask(final PixelPos pixelPos, final int fineWinWidth, final int fineWinHeight) {
        int l0 = (int) (pixelPos.y - fineWinHeight/2);
        int lN = (int) (pixelPos.y + fineWinHeight/2 - 1);
        int p0 = (int) (pixelPos.x - fineWinWidth/2);
        int pN = (int) (pixelPos.x + fineWinWidth/2 - 1);
        return new Rectangle(p0, l0, pN - p0 + 1, lN - l0 + 1);
    }

    /**
     * Estimate azimuth offset using ESD approach.
     */
    private synchronized void estimateAzimuthOffset() {

        if (isAzimuthOffsetAvailable) {
            return;
        }

        final int numOverlaps = subSwath[subSwathIndex - 1].numOfBursts - 1;
        final int numShifts = numOverlaps * numBlocksPerOverlap;

        //SystemUtils.LOG.info("estimateAzimuthOffset numOverlaps = " + numOverlaps);

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Estimating azimuth offset... ", numShifts);

        final ThreadManager threadManager = new ThreadManager();
        try {
            final Band mBandI = getBand(StackUtils.MST, "i_", swathIndexStr, polarizations[0]);
            final Band mBandQ = getBand(StackUtils.MST, "q_", swathIndexStr, polarizations[0]);
            final Band sBandI = getBand(StackUtils.SLV, "i_", swathIndexStr, polarizations[0]);
            final Band sBandQ = getBand(StackUtils.SLV, "q_", swathIndexStr, polarizations[0]);

            final double spectralSeparation = computeSpectralSeparation();

            final List<AzimuthShiftData> azShiftArray = new ArrayList<>(numShifts);

            for (int i = 0; i < numOverlaps; i++) {

                final Rectangle overlapInBurstOneRectangle =  new Rectangle();
                final Rectangle overlapInBurstTwoRectangle = new Rectangle();

                getOverlappedRectangles(i, overlapInBurstOneRectangle, overlapInBurstTwoRectangle);

                final double[][] coherence = computeCoherence(
                        overlapInBurstOneRectangle, mBandI, mBandQ, sBandI, sBandQ, cohWin);

                final int w = overlapInBurstOneRectangle.width / numBlocksPerOverlap; // block width
                final int h = overlapInBurstOneRectangle.height;
                final int x0BurstOne = overlapInBurstOneRectangle.x;
                final int y0BurstOne = overlapInBurstOneRectangle.y;
                final int y0BurstTwo = overlapInBurstTwoRectangle.y;
                final int overlapIndex = i;

                for (int j = 0; j < numBlocksPerOverlap; j++) {
                    checkForCancellation();
                    final int x0 = x0BurstOne + j * w;
                    final int blockIndex = j;

                    final Thread worker = new Thread() {
                        @Override
                        public void run() {
                            try {
                                final Rectangle blockInBurstOneRectangle = new Rectangle(x0, y0BurstOne, w, h);
                                final Rectangle blockInBurstTwoRectangle = new Rectangle(x0, y0BurstTwo, w, h);

                                final double[] blockCoherence = getBlockCoherence(blockIndex, w, h, coherence);

                                final double azShift = estimateAzOffsets(mBandI, mBandQ, sBandI, sBandQ, blockCoherence,
                                        blockInBurstTwoRectangle, blockInBurstOneRectangle, spectralSeparation);

                                synchronized(azShiftArray) {
                                    azShiftArray.add(new AzimuthShiftData(overlapIndex, blockIndex, azShift));
                                }
                            } catch (Throwable e) {
                                OperatorUtils.catchOperatorException("estimateOffset", e);
                            }
                        }
                    };
                    threadManager.add(worker);
                    status.worked(1);
                }
            }

            status.done();
            threadManager.finish();

            // todo The following simple average should be replaced by weighted average using coherence as weight
            final double[] averagedAzShiftArray = new double[numOverlaps];
            double totalOffset = 0.0;
            for (int i = 0; i < numOverlaps; i++) {
                double sumAzOffset = 0.0;
                for (int j = 0; j < numShifts; j++) {
                    if (azShiftArray.get(j).overlapIndex == i) {
                        sumAzOffset += azShiftArray.get(j).shift;
                    }
                }
                averagedAzShiftArray[i] = sumAzOffset / numBlocksPerOverlap;
                totalOffset += sumAzOffset;

                SystemUtils.LOG.fine(
                        "AzimuthShiftOp: overlap area = " + i + ", azimuth offset = " + averagedAzShiftArray[i]);
            }

            azOffset = -totalOffset / numShifts;
            SystemUtils.LOG.fine("AzimuthShiftOp: Overall azimuth shift = " + azOffset);

            saveOverallAzimuthShift(azOffset);

            saveAzimuthShiftPerOverlap(averagedAzShiftArray);

            saveAzimuthShiftPerBlock(azShiftArray);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("estimateAzimuthOffset", e);
        }

        isAzimuthOffsetAvailable = true;
    }

    private double computeSpectralSeparation () {

        final double tCycle =
                subSwath[subSwathIndex - 1].linesPerBurst * subSwath[subSwathIndex - 1].azimuthTimeInterval;

        double sumSpectralSeparation = 0.0;
        for (int b = 0; b < subSwath[subSwathIndex - 1].numOfBursts; b++) {
            for (int p = 0; p < subSwath[subSwathIndex - 1].samplesPerBurst; p++) {
                sumSpectralSeparation += subSwath[subSwathIndex - 1].dopplerRate[b][p] * tCycle;
            }
        }
        return sumSpectralSeparation / (subSwath[subSwathIndex - 1].numOfBursts *
                subSwath[subSwathIndex - 1].samplesPerBurst);
    }

    private void getOverlappedRectangles(final int overlapIndex,
                                         final Rectangle overlapInBurstOneRectangle,
                                         final Rectangle overlapInBurstTwoRectangle) {

        final int firstValidPixelOfBurstOne = getBurstFirstValidPixel(overlapIndex);
        final int lastValidPixelOfBurstOne = getBurstLastValidPixel(overlapIndex);
        final int firstValidPixelOfBurstTwo = getBurstFirstValidPixel(overlapIndex + 1);
        final int lastValidPixelOfBurstTwo = getBurstLastValidPixel(overlapIndex + 1);
        final int firstValidPixel = Math.max(firstValidPixelOfBurstOne, firstValidPixelOfBurstTwo);
        final int lastValidPixel = Math.min(lastValidPixelOfBurstOne, lastValidPixelOfBurstTwo);
        final int x0 = firstValidPixel;
        final int w = lastValidPixel - firstValidPixel + 1;

        final int numOfInvalidLinesInBurstOne = subSwath[subSwathIndex - 1].linesPerBurst -
                subSwath[subSwathIndex - 1].lastValidLine[overlapIndex] - 1;

        final int numOfInvalidLinesInBurstTwo = subSwath[subSwathIndex - 1].firstValidLine[overlapIndex + 1];

        final int numOverlappedLines = computeBurstOverlapSize(overlapIndex);

        final int h = numOverlappedLines - numOfInvalidLinesInBurstOne - numOfInvalidLinesInBurstTwo;

        final int y0BurstOne =
                subSwath[subSwathIndex - 1].linesPerBurst * (overlapIndex + 1) - numOfInvalidLinesInBurstOne - h;

        final int y0BurstTwo =
                subSwath[subSwathIndex - 1].linesPerBurst * (overlapIndex + 1) + numOfInvalidLinesInBurstTwo;

        overlapInBurstOneRectangle.setBounds(x0, y0BurstOne, w, h);
        overlapInBurstTwoRectangle.setBounds(x0, y0BurstTwo, w, h);
    }

    private int getBurstFirstValidPixel(final int burstIndex) {

        for (int lineIdx = 0; lineIdx < subSwath[subSwathIndex - 1].firstValidSample[burstIndex].length; lineIdx++) {
            if (subSwath[subSwathIndex - 1].firstValidSample[burstIndex][lineIdx] != -1) {
                return subSwath[subSwathIndex - 1].firstValidSample[burstIndex][lineIdx];
            }
        }
        return -1;
    }

    private int getBurstLastValidPixel(final int burstIndex) {

        for (int lineIdx = 0; lineIdx < subSwath[subSwathIndex - 1].lastValidSample[burstIndex].length; lineIdx++) {
            if (subSwath[subSwathIndex - 1].lastValidSample[burstIndex][lineIdx] != -1) {
                return subSwath[subSwathIndex - 1].lastValidSample[burstIndex][lineIdx];
            }
        }
        return -1;
    }

    private static double[] getBlockCoherence(
            final int blockIndex, final int blockWidth, final int blockHeight, final double[][] coherence) {

        final double[] blockCoherence = new double[blockWidth*blockHeight];

        for (int i = 0; i < blockCoherence.length; i++) {
            final int r = i / blockWidth;
            final int c = blockIndex*blockWidth + i - r*blockWidth;
            blockCoherence[i] = coherence[r][c];
        }
        return blockCoherence;
    }

    private void saveOverallRangeShift(final double rangeShift) {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        final MetadataElement ESDMeasurement = absTgt.getElement("ESD Measurement");
        final MetadataElement OverallRgAzShiftElem = ESDMeasurement.getElement("Overall_Range_Azimuth_Shift");
        final MetadataElement swathElem = OverallRgAzShiftElem.getElement(subSwathNames[0]);

        final MetadataAttribute rangeShiftAttr = new MetadataAttribute("rangeShift", ProductData.TYPE_FLOAT32);
        rangeShiftAttr.setUnit("pixel");
        swathElem.addAttribute(rangeShiftAttr);
        swathElem.setAttributeDouble("rangeShift", rangeShift);
    }

    private void saveOverallAzimuthShift(final double azimuthShift) {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        final MetadataElement ESDMeasurement = absTgt.getElement("ESD Measurement");
        final MetadataElement OverallRgAzShiftElem = ESDMeasurement.getElement("Overall_Range_Azimuth_Shift");
        final MetadataElement swathElem = OverallRgAzShiftElem.getElement(subSwathNames[0]);

        final MetadataAttribute azimuthShiftAttr = new MetadataAttribute("azimuthShift", ProductData.TYPE_FLOAT32);
        azimuthShiftAttr.setUnit("pixel");
        swathElem.addAttribute(azimuthShiftAttr);
        swathElem.setAttributeDouble("azimuthShift", azimuthShift);
    }

    private void saveRangeShiftPerBurst(final List<Double> rangeShiftArray, final List<Integer> burstIndexArray) {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        final MetadataElement ESDMeasurement = absTgt.getElement("ESD Measurement");
        final MetadataElement RangeShiftPerBurstElem = ESDMeasurement.getElement("Range_Shift_Per_Burst");
        final MetadataElement swathElem = RangeShiftPerBurstElem.getElement(subSwathNames[0]);

        swathElem.addAttribute(new MetadataAttribute("count", ProductData.TYPE_INT16));
        swathElem.setAttributeInt("count", rangeShiftArray.size());

        for (int i = 0; i < rangeShiftArray.size(); i++) {
            final MetadataElement burstListElem = new MetadataElement("RangeShiftList." + i);
            final MetadataAttribute rangeShiftAttr = new MetadataAttribute("rangeShift", ProductData.TYPE_FLOAT32);
            rangeShiftAttr.setUnit("pixel");
            burstListElem.addAttribute(rangeShiftAttr);
            burstListElem.setAttributeDouble("rangeShift", rangeShiftArray.get(i));
            burstListElem.addAttribute(new MetadataAttribute("burstIndex", ProductData.TYPE_INT16));
            burstListElem.setAttributeInt("burstIndex", burstIndexArray.get(i));
            swathElem.addElement(burstListElem);
        }
    }

    private void saveAzimuthShiftPerOverlap(final double[] averagedAzShiftArray) {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        final MetadataElement ESDMeasurement = absTgt.getElement("ESD Measurement");
        final MetadataElement AzShiftPerOverlapElem = ESDMeasurement.getElement("Azimuth_Shift_Per_Overlap");
        final MetadataElement swathElem = AzShiftPerOverlapElem.getElement(subSwathNames[0]);

        swathElem.addAttribute(new MetadataAttribute("count", ProductData.TYPE_INT16));
        swathElem.setAttributeInt("count", averagedAzShiftArray.length);

        for (int i = 0; i < averagedAzShiftArray.length; i++) {
            final MetadataElement overlapListElem = new MetadataElement("AzimuthShiftList." + i);
            final MetadataAttribute azimuthShiftAttr = new MetadataAttribute("azimuthShift", ProductData.TYPE_FLOAT32);
            azimuthShiftAttr.setUnit("pixel");
            overlapListElem.addAttribute(azimuthShiftAttr);
            overlapListElem.setAttributeDouble("azimuthShift", averagedAzShiftArray[i]);
            overlapListElem.addAttribute(new MetadataAttribute("overlapIndex", ProductData.TYPE_INT16));
            overlapListElem.setAttributeInt("overlapIndex", i);
            swathElem.addElement(overlapListElem);
        }
    }

    private void saveAzimuthShiftPerBlock(final List<AzimuthShiftData> azShiftArray) {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        final MetadataElement ESDMeasurement = absTgt.getElement("ESD Measurement");
        final MetadataElement AzShiftPerBlockElem = ESDMeasurement.getElement("Azimuth_Shift_Per_Block");
        final MetadataElement swathElem = AzShiftPerBlockElem.getElement(subSwathNames[0]);

        swathElem.addAttribute(new MetadataAttribute("count", ProductData.TYPE_INT16));
        swathElem.setAttributeInt("count", azShiftArray.size());

        for (int i = 0; i < azShiftArray.size(); i++) {
            final MetadataElement overlapListElem = new MetadataElement("AzimuthShiftList." + i);
            final MetadataAttribute azimuthShiftAttr = new MetadataAttribute("azimuthShift", ProductData.TYPE_FLOAT32);
            azimuthShiftAttr.setUnit("pixel");
            overlapListElem.addAttribute(azimuthShiftAttr);
            overlapListElem.setAttributeDouble("azimuthShift", azShiftArray.get(i).shift);
            overlapListElem.addAttribute(new MetadataAttribute("overlapIndex", ProductData.TYPE_INT16));
            overlapListElem.setAttributeInt("overlapIndex", azShiftArray.get(i).overlapIndex);
            overlapListElem.addAttribute(new MetadataAttribute("blockIndex", ProductData.TYPE_INT16));
            overlapListElem.setAttributeInt("blockIndex", azShiftArray.get(i).blockIndex);
            swathElem.addElement(overlapListElem);
        }
    }

    /**
     * Compute the number of lines in the overlapped area of given adjacent bursts.
     * @return The number of lines in the overlapped area.
     */
    private int computeBurstOverlapSize(final int overlapIndex) {

        final double endTime = subSwath[subSwathIndex - 1].burstLastLineTime[overlapIndex];
        final double startTime = subSwath[subSwathIndex - 1].burstFirstLineTime[overlapIndex + 1];
        return (int)((endTime - startTime) / subSwath[subSwathIndex - 1].azimuthTimeInterval);
    }

    private double estimateAzOffsets(final Band mBandI, final Band mBandQ, final Band sBandI, final Band sBandQ,
                                     final double[] blockCoherence, final Rectangle backwardRectangle,
                                     final Rectangle forwardRectangle, final double spectralSeparation) {

        final double[] mIBackArray = getSourceData(mBandI, backwardRectangle);
        final double[] mQBackArray = getSourceData(mBandQ, backwardRectangle);
        final double[] sIBackArray = getSourceData(sBandI, backwardRectangle);
        final double[] sQBackArray = getSourceData(sBandQ, backwardRectangle);

        final double[] mIForArray = getSourceData(mBandI, forwardRectangle);
        final double[] mQForArray = getSourceData(mBandQ, forwardRectangle);
        final double[] sIForArray = getSourceData(sBandI, forwardRectangle);
        final double[] sQForArray = getSourceData(sBandQ, forwardRectangle);

        final int arrayLength = mIBackArray.length;
        final double[] backIntReal = new double[arrayLength];
        final double[] backIntImag = new double[arrayLength];
        complexArrayMultiplication(mIBackArray, mQBackArray, sIBackArray, sQBackArray, backIntReal, backIntImag);

        final double[] forIntReal = new double[arrayLength];
        final double[] forIntImag = new double[arrayLength];
        complexArrayMultiplication(mIForArray, mQForArray, sIForArray, sQForArray, forIntReal, forIntImag);

        final double[] diffIntReal = new double[arrayLength];
        final double[] diffIntImag = new double[arrayLength];
        complexArrayMultiplication(forIntReal, forIntImag, backIntReal, backIntImag, diffIntReal, diffIntImag);

        double sumReal = 0.0, sumImag = 0.0;
        for (int i = 0; i < arrayLength; i++) {
            if (blockCoherence[i] > cohThreshold) {
                final double theta = Math.atan2(diffIntImag[i], diffIntReal[i]);
                sumReal += FastMath.cos(theta);
                sumImag += FastMath.sin(theta);
            }
        }

        final double phase = Math.atan2(sumImag, sumReal);
        return phase / (2 * Math.PI * spectralSeparation * subSwath[subSwathIndex - 1].azimuthTimeInterval);
    }

    private double[] getSourceData(final Band srcBand, final Rectangle rectangle) {

        final int dataType = srcBand.getDataType();
        final Tile srcTile = getSourceTile(srcBand, rectangle);

        double[] dataArray;
        if (dataType == ProductData.TYPE_INT16) {
            final short[] dataArrayShort = (short[]) srcTile.getDataBuffer().getElems();
            dataArray = new double[dataArrayShort.length];
            for (int i = 0; i < dataArrayShort.length; i++) {
                dataArray[i] = (double)dataArrayShort[i];
            }
        } else if (dataType == ProductData.TYPE_FLOAT32) {
            final float[] dataArrayFloat = (float[])srcTile.getDataBuffer().getElems();
            dataArray = new double[dataArrayFloat.length];
            for (int i = 0; i < dataArrayFloat.length; i++) {
                dataArray[i] = (double)dataArrayFloat[i];
            }
        } else {
            dataArray = (double[]) srcTile.getDataBuffer().getElems();
        }

        return dataArray;
    }

    private static void complexArrayMultiplication(final double[] realArray1, final double[] imagArray1,
                                            final double[] realArray2, final double[] imagArray2,
                                            final double[] realOutput, final double[] imagOutput) {

        final int arrayLength = realArray1.length;
        if (imagArray1.length != arrayLength || realArray2.length != arrayLength || imagArray2.length != arrayLength ||
                realOutput.length != arrayLength || imagOutput.length != arrayLength) {
            throw new OperatorException("Arrays of the same length are expected.");
        }

        for (int i = 0; i < arrayLength; i++) {
            realOutput[i] = realArray1[i] * realArray2[i] + imagArray1[i] * imagArray2[i];
            imagOutput[i] = imagArray1[i] * realArray2[i] - realArray1[i] * imagArray2[i];
        }
    }

    private Band getBand(final String suffix, final String prefix, final String swathIndexStr, final String polarization) {

        final String[] bandNames = sourceProduct.getBandNames();
        for (String bandName : bandNames) {
            if (bandName.contains(suffix) && bandName.contains(prefix) &&
                    bandName.contains(swathIndexStr) && bandName.contains(polarization)) {
                return sourceProduct.getBand(bandName);
            }
        }
        return null;
    }

    private static void computeShiftPhaseArray(final double shift, final int signalLength, final double[] phaseArray) {

        int k2;
        double phaseK;
        final double phase = -2.0 * Math.PI * shift / (double)signalLength;
        final int halfSignalLength = (int) (signalLength * 0.5 + 0.5);

        for (int k = 0; k < signalLength; ++k) {
            if (k < halfSignalLength) {
                phaseK = phase * k;
            } else {
                phaseK = phase * (k - signalLength);
            }
            k2 = k * 2;
            phaseArray[k2] = FastMath.cos(phaseK);
            phaseArray[k2 + 1] = FastMath.sin(phaseK);
        }
    }

    private static void multiplySpectrumByShiftFactor(final double[] array, final double[] phaseArray) {

        int k2;
        double c, s;
        double real, imag;
        final int signalLength = array.length / 2;
        for (int k = 0; k < signalLength; ++k) {
            k2 = k * 2;
            c = phaseArray[k2];
            s = phaseArray[k2 + 1];
            real = array[k2];
            imag = array[k2 + 1];
            array[k2] = real * c - imag * s;
            array[k2 + 1] = real * s + imag * c;
        }
    }

    private double[][] computeCoherence(final Rectangle rectangle, final Band mBandI, final Band mBandQ,
                                        final Band sBandI, final Band sBandQ, final int cohWin) {

        final int x0 = rectangle.x;
        final int y0 = rectangle.y;
        final int w = rectangle.width;
        final int h = rectangle.height;
        final int xMax = x0 + w;
        final int yMax = y0 + h;
        final int halfWindowSize = cohWin / 2;
        final double[][] coherence = new double[h][w];

        final Tile mstTileI = getSourceTile(mBandI, rectangle);
        final Tile mstTileQ = getSourceTile(mBandQ, rectangle);
        final ProductData mstDataBufferI = mstTileI.getDataBuffer();
        final ProductData mstDataBufferQ = mstTileQ.getDataBuffer();

        final Tile slvTileI = getSourceTile(sBandI, rectangle);
        final Tile slvTileQ = getSourceTile(sBandQ, rectangle);
        final ProductData slvDataBufferI = slvTileI.getDataBuffer();
        final ProductData slvDataBufferQ = slvTileQ.getDataBuffer();

        final TileIndex srcIndex = new TileIndex(mstTileI);

        final double[][] cohReal = new double[h][w];
        final double[][] cohImag = new double[h][w];
        final double[][] mstPower = new double[h][w];
        final double[][] slvPower = new double[h][w];
        for (int y = y0; y < yMax; ++y) {
            srcIndex.calculateStride(y);
            final int yy = y - y0;
            for (int x = x0; x < xMax; ++x) {
                final int srcIdx = srcIndex.getIndex(x);
                final int xx = x - x0;

                final float mI = mstDataBufferI.getElemFloatAt(srcIdx);
                final float mQ = mstDataBufferQ.getElemFloatAt(srcIdx);
                final float sI = slvDataBufferI.getElemFloatAt(srcIdx);
                final float sQ = slvDataBufferQ.getElemFloatAt(srcIdx);

                cohReal[yy][xx] = mI * sI + mQ * sQ;
                cohImag[yy][xx] = mQ * sI - mI * sQ;
                mstPower[yy][xx] = mI * mI + mQ * mQ;
                slvPower[yy][xx] = sI * sI + sQ * sQ;
            }
        }

        for (int y = y0; y < yMax; ++y) {
            final int yy = y - y0;
            for (int x = x0; x < xMax; ++x) {
                final int xx = x - x0;

                final int rowSt = Math.max(yy - halfWindowSize, 0);
                final int rowEd = Math.min(yy + halfWindowSize, h - 1);
                final int colSt = Math.max(xx - halfWindowSize, 0);
                final int colEd = Math.min(xx + halfWindowSize, w - 1);

                double cohRealSum = 0.0f, cohImagSum = 0.0f, mstPowerSum = 0.0f, slvPowerSum = 0.0f;
                int count = 0;
                for (int r = rowSt; r <= rowEd; r++) {
                    for (int c = colSt; c <= colEd; c++) {
                        cohRealSum += cohReal[r][c];
                        cohImagSum += cohImag[r][c];
                        mstPowerSum += mstPower[r][c];
                        slvPowerSum += slvPower[r][c];
                        count++;
                    }
                }

                if (count > 0 && mstPowerSum != 0.0 && slvPowerSum != 0.0) {
                    final double cohRealMean = cohRealSum / (double)count;
                    final double cohImagMean = cohImagSum / (double)count;
                    final double mstPowerMean = mstPowerSum / (double)count;
                    final double slvPowerMean = slvPowerSum / (double)count;
                    coherence[yy][xx] = Math.sqrt((cohRealMean * cohRealMean + cohImagMean * cohImagMean) /
                            (mstPowerMean * slvPowerMean));
                }
            }
        }
        return coherence;
    }

    private void performRangeAzimuthShift(final Band slvBandI, final Band slvBandQ, final Rectangle targetRectangle,
                                          Map<Band, Tile> targetTileMap) {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int burstIndex = y0 / subSwath[subSwathIndex - 1].linesPerBurst;

        final float noDataValue = (float)slvBandI.getNoDataValue();
        final Tile slvTileI = getSourceTile(slvBandI, targetRectangle);
        final Tile slvTileQ = getSourceTile(slvBandQ, targetRectangle);
        final float[] slvArrayI = (float[]) slvTileI.getDataBuffer().getElems();
        final float[] slvArrayQ = (float[]) slvTileQ.getDataBuffer().getElems();

        // Perform range shift

        final double[] line = new double[2*w];
        final double[] phaseRg = new double[2*w];
        final DoubleFFT_1D row_fft = new DoubleFFT_1D(w);
        final double[][] rangeShiftedI = new double[h][w];
        final double[][] rangeShiftedQ = new double[h][w];

        computeShiftPhaseArray(rgOffset, w, phaseRg);

        for (int r = 0; r < h; r++) {
            final int rw = r * w;
            for (int c = 0; c < w; c++) {
                int c2 = c * 2;
                line[c2] = slvArrayI[rw + c];
                line[c2 + 1] = slvArrayQ[rw + c];
            }

            row_fft.complexForward(line);

            multiplySpectrumByShiftFactor(line, phaseRg);

            row_fft.complexInverse(line, true);

            for (int c = 0; c < w; c++) {
                int c2 = c * 2;
                rangeShiftedI[r][c] = line[c2];
                rangeShiftedQ[r][c] = line[c2 + 1];
            }
        }

        // Perform azimuth Shift

        // get deramp/demodulation phase and perform deramp and demodulation
        final double[][] derampDemodPhase = su.computeDerampDemodPhase(subSwath, subSwathIndex, burstIndex, targetRectangle);
        final double[][] derampDemodI = new double[h][w];
        final double[][] derampDemodQ = new double[h][w];
        for (int r = 0; r < h; r++) {
            for (int c = 0; c < w; c++) {
                final double cosPhase = FastMath.cos(derampDemodPhase[r][c]);
                final double sinPhase = FastMath.sin(derampDemodPhase[r][c]);
                derampDemodI[r][c] = rangeShiftedI[r][c]*cosPhase - rangeShiftedQ[r][c]*sinPhase;
                derampDemodQ[r][c] = rangeShiftedI[r][c]*sinPhase + rangeShiftedQ[r][c]*cosPhase;
            }
        }

        // compute shift phase
        final double[] phaseAz = new double[2*h];
        computeShiftPhaseArray(azOffset, h, phaseAz);

        // perform azimuth shift using FFT, and perform reramp and remodulation
        final Band tgtBandI = targetProduct.getBand(slvBandI.getName());
        final Band tgtBandQ = targetProduct.getBand(slvBandQ.getName());
        final Tile tgtTileI = targetTileMap.get(tgtBandI);
        final Tile tgtTileQ = targetTileMap.get(tgtBandQ);
        final ProductData tgtDataI = tgtTileI.getDataBuffer();
        final ProductData tgtDataQ = tgtTileQ.getDataBuffer();

        final double[] col1 = new double[2 * h];
        final double[] col2 = new double[2 * h];
        final DoubleFFT_1D col_fft = new DoubleFFT_1D(h);
        for (int c = 0; c < w; c++) {
            final int x = x0 + c;
            for (int r = 0; r < h; r++) {
                int r2 = r * 2;
                col1[r2] = derampDemodI[r][c];
                col1[r2 + 1] = derampDemodQ[r][c];

                col2[r2] = derampDemodPhase[r][c];
                col2[r2 + 1] = 0.0;
            }

            col_fft.complexForward(col1);
            col_fft.complexForward(col2);

            multiplySpectrumByShiftFactor(col1, phaseAz);
            multiplySpectrumByShiftFactor(col2, phaseAz);

            col_fft.complexInverse(col1, true);
            col_fft.complexInverse(col2, true);

            for (int r = 0; r < h; r++) {
                if (slvArrayI[r*w + c] != noDataValue) {
                    int r2 = r * 2;
                    final int y = y0 + r;
                    final int idx = tgtTileI.getDataBufferIndex(x, y);

                    final double cosPhase = FastMath.cos(col2[r2]);
                    final double sinPhase = FastMath.sin(col2[r2]);
                    tgtDataI.setElemDoubleAt(idx, col1[r2] * cosPhase + col1[r2 + 1] * sinPhase);
                    tgtDataQ.setElemDoubleAt(idx, -col1[r2] * sinPhase + col1[r2 + 1] * cosPhase);
                }
            }
        }
    }


    private static class AzimuthShiftData {
        int overlapIndex;
        int blockIndex;
        double shift;

        public AzimuthShiftData(final int overlapIndex, final int blockIndex, final double shift) {
            this.overlapIndex = overlapIndex;
            this.blockIndex = blockIndex;
            this.shift = shift;
        }
    }


    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see OperatorSpi#createOperator()
     * @see OperatorSpi#createOperator(Map, Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SpectralDiversityOp.class);
        }
    }

}
