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
import org.esa.s1tbx.insar.gpf.coregistration.CoarseRegistration;
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
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.gpf.ThreadManager;
import org.esa.snap.engine_utilities.gpf.TileIndex;

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
        copyright = "Copyright (C) 2016 by Array Systems Computing Inc.",
        description = "Estimate constant range and azimuth offsets for the whole image")
public class SpectralDiversityOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct(description = "The target product which will use the master's grid.")
    private Product targetProduct = null;

    @Parameter(valueSet = {"32", "64", "128","256", "512", "1024", "2048"}, defaultValue = "512",
            label = "Registration Window Size")
    private String registrationWindowSize = "512";

    @Parameter(valueSet = {"2", "4", "8", "16"}, defaultValue = "4", label = "Interpolation Factor")
    private String interpFactor = "4";

    @Parameter(description = "The maximum number of iterations", interval = "(1, 20]", defaultValue = "10",
            label = "Max Iterations")
    private int maxIteration = 10;

    private int cWindowSize = 0;
    private int upSamplingFactor = 0;
    private boolean isRangeOffsetAvailable = false;
    private boolean isAzimuthOffsetAvailable = false;
    private double gcpTolerance = 0.0;
    private double azOffset = 0.0;
    private double rgOffset = 0.0;
    private double noDataValue = -9999.0;
    private Sentinel1Utils su;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;
    private int subSwathIndex = 0;

    private String swathIndexStr = null;
    private String[] subSwathNames = null;
    private String[] polarizations = null;

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
            checkDerampDemodPhaseBand();

            cWindowSize = Integer.parseInt(registrationWindowSize);
            upSamplingFactor = Integer.parseInt(interpFactor);
            gcpTolerance = 1.0 / upSamplingFactor;

            su = new Sentinel1Utils(sourceProduct);
            su.computeDopplerRate();
            subSwath = su.getSubSwath();
            polarizations = su.getPolarizations();

            subSwathNames = su.getSubSwathNames();
            if (subSwathNames.length != 1) {
                throw new OperatorException("Split product is expected.");
            } else {
                subSwathIndex = 1; // subSwathIndex is always 1 because of split product
                swathIndexStr = subSwathNames[0].substring(2);
            }

            if (subSwath[subSwathIndex - 1].samplesPerBurst < cWindowSize) {
                throw new OperatorException("Registration window width should not be grater than burst width " +
                        subSwath[subSwathIndex - 1].samplesPerBurst);
            }

            if (subSwath[subSwathIndex - 1].linesPerBurst < cWindowSize) {
                throw new OperatorException("Registration window height should not be grater than burst height " +
                        subSwath[subSwathIndex - 1].linesPerBurst);
            }

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void checkDerampDemodPhaseBand() {

        boolean hasDerampDemodPhaseBand = false;
        final Band[] sourceBands = sourceProduct.getBands();
        for (Band band:sourceBands) {
            if (band.getName().contains(DerampDemodPhase)) {
                hasDerampDemodPhaseBand = true;
                break;
            }
        }

        if (!hasDerampDemodPhaseBand) {
            throw new OperatorException("Cannot find derampDemodPhase band in source product. " +
                                                "Please run Backgeocoding and select \"Output Deramp and Demod Phase\".");
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
            if (srcBandName.contains("_mst") || srcBandName.contains("derampDemod")) {
                targetBand = ProductUtils.copyBand(srcBandName, sourceProduct, srcBandName, targetProduct, true);
            } else if (srcBandName.contains("azOffset") || srcBandName.contains("rgOffset")) {
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
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetProduct.getBand("i"+suffix), targetBand, suffix);
            }
        }

        targetProduct.setPreferredTileSize(512, subSwath[subSwathIndex - 1].linesPerBurst);
        //targetProduct.setPreferredTileSize(sourceProduct.getSceneRasterWidth(), subSwath[subSwathIndex - 1].linesPerBurst);
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

        try {
            if (!isRangeOffsetAvailable) {
                estimateRangeOffset();
            }
            if (!isAzimuthOffsetAvailable) {
                estimateAzimuthOffset();
            }

            // perform range shift using FFT
            Band slvBandI = null, slvBandQ = null;
            Band tgtBandI = null, tgtBandQ = null;
            Band derampDemodPhaseBand = null;
            final String[] bandNames = sourceProduct.getBandNames();
            for (String bandName : bandNames) {
                if (bandName.contains("i_") && bandName.contains("_slv")) {
                    slvBandI = sourceProduct.getBand(bandName);
                    tgtBandI = targetProduct.getBand(bandName);
                } else if (bandName.contains("q_") && bandName.contains("_slv")) {
                    slvBandQ = sourceProduct.getBand(bandName);
                    tgtBandQ = targetProduct.getBand(bandName);
                }
                if (bandName.contains(DerampDemodPhase)) {
                    derampDemodPhaseBand = sourceProduct.getBand(bandName);
                }
            }

            // get deramp/demodulation phase
            final Tile derampDemodPhaseTile = getSourceTile(derampDemodPhaseBand, targetRectangle);
            final ProductData derampDemodPhaseData = derampDemodPhaseTile.getDataBuffer();
            final TileIndex index = new TileIndex(derampDemodPhaseTile);
            final double[][] derampDemodPhase = new double[h][w];
            for (int y = y0; y < yMax; y++) {
                index.calculateStride(y);
                final int yy = y - y0;
                for (int x = x0; x < xMax; x++) {
                    final int idx = index.getIndex(x);
                    derampDemodPhase[yy][x - x0] = derampDemodPhaseData.getElemDoubleAt(idx);
                }
            }

            // Azimuth Shift

            // perform deramp and demodulation
            final Tile slvTileI = getSourceTile(slvBandI, targetRectangle);
            final Tile slvTileQ = getSourceTile(slvBandQ, targetRectangle);
            final double[][] derampDemodI = new double[h][w];
            final double[][] derampDemodQ = new double[h][w];
            BackGeocodingOp.performDerampDemod(
                    slvTileI, slvTileQ, targetRectangle, derampDemodPhase, derampDemodI, derampDemodQ);

            // compute shift phase
            final double[] phaseAz = new double[2*h];
            computeShiftPhaseArray(azOffset, h, phaseAz);

            // perform azimuth shift using FFT, and perform reramp and remodulation
            final Tile tgtTileI = targetTileMap.get(tgtBandI);
            final Tile tgtTileQ = targetTileMap.get(tgtBandQ);
            final ProductData tgtDataI = tgtTileI.getDataBuffer();
            final ProductData tgtDataQ = tgtTileQ.getDataBuffer();

            final float[] slvArrayI = (float[]) slvTileI.getDataBuffer().getElems();
            final float[] slvArrayQ = (float[]) slvTileQ.getDataBuffer().getElems();
            final float[] tgtArrayI = (float[]) tgtTileI.getDataBuffer().getElems();
            final float[] tgtArrayQ = (float[]) tgtTileQ.getDataBuffer().getElems();

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
                    int r2 = r * 2;
                    final int y = y0 + r;
                    final double cosPhase = FastMath.cos(col2[r2]);
                    final double sinPhase = FastMath.sin(col2[r2]);
                    final int idx = tgtTileI.getDataBufferIndex(x, y);
                    tgtDataI.setElemDoubleAt(idx, (float)(col1[r2] * cosPhase + col1[r2 + 1] * sinPhase));
                    tgtDataQ.setElemDoubleAt(idx, (float)(-col1[r2] * sinPhase + col1[r2 + 1] * cosPhase));
                }
            }


            // Range Shift

            final double[] line = new double[2*w];
            final double[] phaseRg = new double[2*w];
            final DoubleFFT_1D row_fft = new DoubleFFT_1D(w);

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
                    tgtArrayI[rw + c] = (float)line[c2];
                    tgtArrayQ[rw + c] = (float)line[c2 + 1];
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Estimate range and azimuth offset using cross-correlation.
     * @throws Exception The exception.
     */
    private synchronized void estimateRangeOffset() throws Exception {

        if (isRangeOffsetAvailable) {
            return;
        }

        final Rectangle[] rectangleArray = getRectanglesForAllBursts();
        final int numBursts = rectangleArray.length;
        final List<Double> azOffsetArray = new ArrayList<>(numBursts);
        final List<Double> rgOffsetArray = new ArrayList<>(numBursts);
        final List<Integer> y0Array = new ArrayList<>(numBursts);

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Estimating range offsets... ", numBursts);

        final ThreadManager threadManager = new ThreadManager();
        try {
            for (final Rectangle rectangle:rectangleArray) {
                checkForCancellation();

                final Thread worker = new Thread() {
                    @Override
                    public void run() {
                        try {
                            final double[] offset = new double[2]; // az/rg offset

                            estimateAzRgOffsets(rectangle, offset);

                            /*System.out.println("x0 = " + rectangle.x + ", y0 = " + rectangle.y +
                                    ", w = " + rectangle.width + ", h = " + rectangle.height +
                                    ", azOffset = " + offset[0] + ", rgOffset = " + offset[1]);*/

                            synchronized(azOffsetArray) {
                                azOffsetArray.add(offset[0]);
                                rgOffsetArray.add(offset[1]);
                                y0Array.add(rectangle.y);
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

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("estimateOffset", e);
        }

        double sumAzOffset = 0.0;
        double sumRgOffset = 0.0;
        int count = 0;
        for (int i = 0; i < azOffsetArray.size(); i++) {
            final double azShift = azOffsetArray.get(i);
            final double rgShift = rgOffsetArray.get(i);
            final int b = getBurstIndex(y0Array.get(i), rectangleArray);
            if (b != -1) {
                SystemUtils.LOG.info("RangeShiftOp: burst = " + b + ", azimuth offset = " + azShift);
                SystemUtils.LOG.info("RangeShiftOp: burst = " + b + ", range offset = " + rgShift);
            }

            if (azShift == noDataValue || rgShift == noDataValue) {
                continue;
            }
            sumAzOffset += azShift;
            sumRgOffset += rgShift;
            count++;
        }

        if (count > 0) {
            azOffset = sumAzOffset / count;
            rgOffset = sumRgOffset / count;
        } else {
            throw new OperatorException("estimateOffset failed.");
        }

        isRangeOffsetAvailable = true;
        SystemUtils.LOG.info("RangeShiftOp: whole image azimuth offset = " + azOffset);
        SystemUtils.LOG.info("RangeShiftOp: whole image range offset = " + rgOffset);
    }

    private static int getBurstIndex(final int y0, final Rectangle[] rectangleArray) {

        for (int i = 0; i < rectangleArray.length; i++) {
            if (y0 == rectangleArray[i].y) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Get rectangles for all bursts for given sub-swath.
     * @return The rectangle array.
     */
    private Rectangle[] getRectanglesForAllBursts() {

        final int margin = 10;
        final int numBursts = subSwath[subSwathIndex - 1].numOfBursts;
        final int burstHeight = subSwath[subSwathIndex - 1].linesPerBurst;
        final int burstWidth = subSwath[subSwathIndex - 1].samplesPerBurst;
        final Rectangle[] rectangleArray = new Rectangle[numBursts];

        final int x0 = Math.max((burstWidth - cWindowSize) / 2 - margin, 0);
        for (int i = 0; i < numBursts; i++) {
            final int y0 = Math.max((burstHeight - cWindowSize) / 2 + i * burstHeight - margin, 0);
            rectangleArray[i] = new Rectangle(x0, y0, cWindowSize + 2*margin, cWindowSize + 2*margin);
        }

        return rectangleArray;
    }

    private void estimateAzRgOffsets(final Rectangle rectangle, final double[] offset) {

        final int x0 = rectangle.x;
        final int y0 = rectangle.y;
        final int w = rectangle.width;
        final int h = rectangle.height;

        final Band mBand = getAmplitudeOrIntensityBand("_mst");
        final Band sBand = getAmplitudeOrIntensityBand("_slv");
        final Tile mTile = getSourceTile(mBand, rectangle);
        final Tile sTile = getSourceTile(sBand, rectangle);
        final ProductData mData = mTile.getDataBuffer();
        final ProductData sData = sTile.getDataBuffer();

        final PixelPos mGCPPixelPos = new PixelPos(x0 + w/2, y0 + h/2);
        final PixelPos sGCPPixelPos = new PixelPos(x0 + w/2, y0 + h/2);

        CoarseRegistration coarseRegistration = new CoarseRegistration(cWindowSize, cWindowSize,
                upSamplingFactor, upSamplingFactor, maxIteration, gcpTolerance, mTile, mData, sTile, sData,
                sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());

        if (coarseRegistration.getCoarseSlaveGCPPosition(mGCPPixelPos, sGCPPixelPos)) {

            offset[0] = mGCPPixelPos.getY() - sGCPPixelPos.getY();
            offset[1] = mGCPPixelPos.getX() - sGCPPixelPos.getX();

        } else {

            offset[0] = noDataValue;
            offset[1] = noDataValue;
        }
    }

    private Band getAmplitudeOrIntensityBand(final String suffix) {

        final String[] bandNames = sourceProduct.getBandNames();
        for (String bandName : bandNames) {
            if (!bandName.contains(suffix)) {
                continue;
            }
            final Band band = sourceProduct.getBand(bandName);
            if (band.getUnit().contains(Unit.AMPLITUDE) || band.getUnit().contains(Unit.INTENSITY)) {
                return band;
            }
        }
        return null;
    }

    /**
     * Estimate azimuth offset using ESD approach.
     * @throws Exception The exception.
     */
    private synchronized void estimateAzimuthOffset() throws Exception {

        if (isAzimuthOffsetAvailable) {
            return;
        }

        final int[] overlapSizeArray = computeBurstOverlapSize();
        final int numOverlaps = overlapSizeArray.length;

        SystemUtils.LOG.info("estimateAzimuthOffset numOverlaps = " + numOverlaps);

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Estimating azimuth offset... ", numOverlaps);

        final ThreadManager threadManager = new ThreadManager();
        try {
            final List<Double> azOffsetArray = new ArrayList<>(numOverlaps);
            final List<Integer> overlapIndexArray = new ArrayList<>(numOverlaps);

            for (int i = 0; i < numOverlaps; i++) {
                checkForCancellation();
                final int x0 = (subSwath[subSwathIndex - 1].samplesPerBurst/2) - (cWindowSize/2);//0;
                final int y0 = subSwath[subSwathIndex - 1].linesPerBurst * (i + 1);
                final int w = cWindowSize;//subSwath[subSwathIndex - 1].samplesPerBurst;
                final int h = overlapSizeArray[i];
                final int overlapIndex = i;

                final double tCycle =
                        subSwath[subSwathIndex - 1].linesPerBurst * subSwath[subSwathIndex - 1].azimuthTimeInterval;

                double sumSpectralSeparation = 0.0;
                for (int b = 0; b < subSwath[subSwathIndex - 1].numOfBursts; b++) {
                    for (int p = 0; p < subSwath[subSwathIndex - 1].samplesPerBurst; p++) {
                        sumSpectralSeparation += subSwath[subSwathIndex - 1].dopplerRate[b][p] * tCycle;
                    }
                }
                final double spectralSeparation = sumSpectralSeparation / (subSwath[subSwathIndex - 1].numOfBursts *
                        subSwath[subSwathIndex - 1].samplesPerBurst);

                final Thread worker = new Thread() {
                    @Override
                    public void run() {
                        try {
                            final Rectangle backwardRectangle = new Rectangle(x0, y0, w, h);
                            final Rectangle forwardRectangle = new Rectangle(x0, y0 - h, w, h);
                            SystemUtils.LOG.info("forwardRectangle = " + forwardRectangle);
                            SystemUtils.LOG.info("backwardRectangle = " + backwardRectangle);

                            final Band mBandI = getBand("_mst", "i_", swathIndexStr, polarizations[0]);
                            final Band mBandQ = getBand("_mst", "q_", swathIndexStr, polarizations[0]);
                            final Band sBandI = getBand("_slv", "i_", swathIndexStr, polarizations[0]);
                            final Band sBandQ = getBand("_slv", "q_", swathIndexStr, polarizations[0]);

                            final double azOffset = estimateAzOffsets(mBandI, mBandQ, sBandI, sBandQ,
                                                                      backwardRectangle, forwardRectangle, spectralSeparation);
                            SystemUtils.LOG.info("azOffset = " + azOffset);

                            synchronized(azOffsetArray) {
                                azOffsetArray.add(azOffset);
                                overlapIndexArray.add(overlapIndex);
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

            // todo The following simple average should be replaced by weighted average using coherence as weight
            double sumAzOffset = 0.0;
            for (int i = 0; i < azOffsetArray.size(); i++) {
                final double anAzOffset = azOffsetArray.get(i);
                sumAzOffset += anAzOffset;
                SystemUtils.LOG.info(
                        "AzimuthShiftOp: overlap area = " + overlapIndexArray.get(i) + ", azimuth offset = " + anAzOffset);
            }
            azOffset = sumAzOffset / numOverlaps;
            SystemUtils.LOG.info("AzimuthShiftOp: whole image azimuth offset = " + azOffset);

            saveAzimuthOffsetToMetadata(overlapIndexArray, azOffsetArray);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("estimateOffset", e);
        }

        isAzimuthOffsetAvailable = true;
    }

    private void saveAzimuthOffsetToMetadata(final List<Integer> overlapIndexArray, final List<Double> azOffsetArray) {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        final double azimuthPixelSpacing = absTgt.getAttributeDouble(AbstractMetadata.azimuth_spacing);
        final MetadataElement ESDMeasurement = new MetadataElement("ESD_Measurement");
        final MetadataElement swathElem = new MetadataElement(subSwathNames[0]);
        swathElem.addAttribute(new MetadataAttribute("count", ProductData.TYPE_INT16));
        swathElem.setAttributeInt("count", overlapIndexArray.size());

        for (int i = 0; i < azOffsetArray.size(); i++) {
            final MetadataElement overlapListElem = new MetadataElement("OverlapList." + i);
            overlapListElem.addAttribute(new MetadataAttribute("azimuthShift", ProductData.TYPE_FLOAT32));
            overlapListElem.setAttributeDouble("azimuthShift", azOffsetArray.get(i)*azimuthPixelSpacing*100.0);
            overlapListElem.addAttribute(new MetadataAttribute("overlapIndex", ProductData.TYPE_INT16));
            overlapListElem.setAttributeInt("overlapIndex", overlapIndexArray.get(i));
            swathElem.addElement(overlapListElem);
        }

        ESDMeasurement.addElement(swathElem);
        absTgt.addElement(ESDMeasurement);
    }

    /**
     * Compute burst overlap size for all bursts in given sub-swath.
     * @return The burst overlap size array.
     */
    private int[] computeBurstOverlapSize() {

        final int numBursts = subSwath[subSwathIndex - 1].numOfBursts;
        int[] sizeArray = new int[numBursts - 1];

        for (int i = 0; i < numBursts - 1; i++) {
            final double endTime = subSwath[subSwathIndex - 1].burstLastLineTime[i];
            final double startTime = subSwath[subSwathIndex - 1].burstFirstLineTime[i+1];
            sizeArray[i] = (int)((endTime - startTime) / subSwath[subSwathIndex - 1].azimuthTimeInterval);
        }

        return sizeArray;
    }

    private double estimateAzOffsets(final Band mBandI, final Band mBandQ, final Band sBandI, final Band sBandQ,
                                     final Rectangle backwardRectangle, final Rectangle forwardRectangle,
                                     final double spectralSeparation) {

        final int mDataType = mBandI.getDataType();
        final int sDataType = sBandI.getDataType();

        final Tile mTileIBack = getSourceTile(mBandI, backwardRectangle);
        final Tile mTileQBack = getSourceTile(mBandQ, backwardRectangle);
        final Tile sTileIBack = getSourceTile(sBandI, backwardRectangle);
        final Tile sTileQBack = getSourceTile(sBandQ, backwardRectangle);

        double[] mIBackArray, mQBackArray;
        if (mDataType == ProductData.TYPE_INT16) {
            final short[] mIBackArrayShort = (short[]) mTileIBack.getDataBuffer().getElems();
            final short[] mQBackArrayShort = (short[]) mTileQBack.getDataBuffer().getElems();
            mIBackArray = new double[mIBackArrayShort.length];
            mQBackArray = new double[mQBackArrayShort.length];
            for (int i = 0; i < mIBackArrayShort.length; i++) {
                mIBackArray[i] = (double)mIBackArrayShort[i];
                mQBackArray[i] = (double)mQBackArrayShort[i];
            }
        } else {
            mIBackArray = (double[]) mTileIBack.getDataBuffer().getElems();
            mQBackArray = (double[]) mTileQBack.getDataBuffer().getElems();
        }

        double[] sIBackArray, sQBackArray;
        if (sDataType == ProductData.TYPE_FLOAT32) {
            final float[] sIBackArrayFloat = (float[])sTileIBack.getDataBuffer().getElems();
            final float[] sQBackArrayFloat = (float[])sTileQBack.getDataBuffer().getElems();
            sIBackArray = new double[sIBackArrayFloat.length];
            sQBackArray = new double[sQBackArrayFloat.length];
            for (int i = 0; i < sIBackArrayFloat.length; i++) {
                sIBackArray[i] = (double)sIBackArrayFloat[i];
                sQBackArray[i] = (double)sQBackArrayFloat[i];
            }
        } else {
            sIBackArray = (double[]) sTileIBack.getDataBuffer().getElems();
            sQBackArray = (double[]) sTileQBack.getDataBuffer().getElems();
        }

        final Tile mTileIFor = getSourceTile(mBandI, forwardRectangle);
        final Tile mTileQFor = getSourceTile(mBandQ, forwardRectangle);
        final Tile sTileIFor = getSourceTile(sBandI, forwardRectangle);
        final Tile sTileQFor = getSourceTile(sBandQ, forwardRectangle);

        double[] mIForArray, mQForArray;
        if (mDataType == ProductData.TYPE_INT16) {
            final short[] mIForArrayShort = (short[]) mTileIFor.getDataBuffer().getElems();
            final short[] mQForArrayShort = (short[]) mTileQFor.getDataBuffer().getElems();
            mIForArray = new double[mIForArrayShort.length];
            mQForArray = new double[mQForArrayShort.length];
            for (int i = 0; i < mIForArrayShort.length; i++) {
                mIForArray[i] = (double)mIForArrayShort[i];
                mQForArray[i] = (double)mQForArrayShort[i];
            }
        } else {
            mIForArray = (double[]) mTileIFor.getDataBuffer().getElems();
            mQForArray = (double[]) mTileQFor.getDataBuffer().getElems();
        }

        double[] sIForArray, sQForArray;
        if (sDataType == ProductData.TYPE_FLOAT32) {
            final float[] sIForArrayFloat = (float[])sTileIFor.getDataBuffer().getElems();
            final float[] sQForArrayFloat = (float[])sTileQFor.getDataBuffer().getElems();
            sIForArray = new double[sIForArrayFloat.length];
            sQForArray = new double[sQForArrayFloat.length];
            for (int i = 0; i < sIForArrayFloat.length; i++) {
                sIForArray[i] = (double)sIForArrayFloat[i];
                sQForArray[i] = (double)sQForArrayFloat[i];
            }
        } else {
            sIForArray = (double[]) sTileIFor.getDataBuffer().getElems();
            sQForArray = (double[]) sTileQFor.getDataBuffer().getElems();
        }

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
            final double theta = Math.atan2(diffIntImag[i], diffIntReal[i]);
            sumReal += FastMath.cos(theta);
            sumImag += FastMath.sin(theta);
        }

        final double phase = Math.atan2(sumImag, sumReal);
        return phase / (2 * Math.PI * spectralSeparation * subSwath[subSwathIndex - 1].azimuthTimeInterval);
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
        final double phase = -2.0 * Math.PI * shift / signalLength;
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
