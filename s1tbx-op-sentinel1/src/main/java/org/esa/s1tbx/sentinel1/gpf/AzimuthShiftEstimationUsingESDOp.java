/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc.
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
import org.esa.s1tbx.commons.Sentinel1Utils;
import org.esa.snap.core.datamodel.*;
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
import org.esa.snap.engine_utilities.gpf.*;
import org.esa.snap.engine_utilities.util.ResourceUtils;
import org.jblas.ComplexDoubleMatrix;
import org.jlinda.core.SLCImage;
import org.jlinda.core.coregistration.utils.CoregistrationUtils;
import org.jlinda.core.utils.BandUtilsDoris;
import org.jlinda.core.utils.CplxContainer;
import org.jlinda.core.utils.ProductContainer;
import org.jlinda.core.utils.TileUtilsDoris;

import java.awt.*;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * The operator estimates the global azimuth offset between master and slave images using the Enhanced Spectral
 * Diversity (ESD) approach. The estimation is done for a given sub-swath of a TOPS SLC product. Here it is assumed
 * that the source product is coregistered split product, i.e. the output of the S-1 Back Geocoding operator.
 * The estimation result is saved in a text file and will be used later in azimuth offset correction in the S-1
 * Enhanced Spectral Diversity operator.
 */

@OperatorMetadata(alias = "Azimuth-Shift-Estimation-ESD",
        category = "Radar/Coregistration/S-1 TOPS Coregistration",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2019 by SkyWatch Space Application Inc.",
        description = "Estimate azimuth offset for the whole image")
public class AzimuthShiftEstimationUsingESDOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct(description = "The target product which will use the master's grid.")
    private Product targetProduct = null;

    @Parameter(description = "The coherence threshold for outlier removal", interval = "(0, 1]", defaultValue = "0.15",
            label = "Coherence Threshold for Outlier Removal")
    private double cohThreshold = 0.15;

    @Parameter(description = "The number of windows per overlap for ESD", interval = "[1, 20]", defaultValue = "10",
            label = "Number of Windows Per Overlap for ESD")
    private int numBlocksPerOverlap = 10;

    private boolean isAzimuthOffsetAvailable = false;
    private Band firstBand = null;
    private Sentinel1Utils su;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;
    private int subSwathIndex = 0;

    private String[] subSwathNames = null;
    private String[] polarizations = null;

    private Map<String, CplxContainer> masterMap = new HashMap<>();
    private Map<String, CplxContainer> slaveMap = new HashMap<>();
    private Map<String, ProductContainer> targetMap = new HashMap<>();
    private Map<String, AzRgOffsets> targetOffsetMap = new HashMap<>();

    private static final int cohWin = 5; // window size for coherence calculation

    private boolean outputESDEstimationToFile = true;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public AzimuthShiftEstimationUsingESDOp() {
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
                throw new OperatorException("Split product is expected");
            } else {
                subSwathIndex = 1; // subSwathIndex is always 1 because of split product
            }

            constructSourceMetadata();

            constructTargetMetadata();

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void constructSourceMetadata() throws Exception {

        MetadataElement mstRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        metaMapPut(StackUtils.MST, mstRoot, sourceProduct, masterMap);

        final String slaveMetadataRoot = AbstractMetadata.SLAVE_METADATA_ROOT;
        MetadataElement slaveElem = sourceProduct.getMetadataRoot().getElement(slaveMetadataRoot);
        if (slaveElem == null) {
            slaveElem = sourceProduct.getMetadataRoot().getElement("Slave Metadata");
        }
        if (slaveElem == null) {
            throw new OperatorException("Product must be coregistered (missing Slave_Metadata in Metadata)");
        }

        MetadataElement[] slaveRoot = slaveElem.getElements();
        for (MetadataElement meta : slaveRoot) {
            if (!meta.getName().equals(AbstractMetadata.ORIGINAL_PRODUCT_METADATA))
                metaMapPut(StackUtils.SLV, meta, sourceProduct, slaveMap);
        }
    }

    private void metaMapPut(final String tag,
                            final MetadataElement root,
                            final Product product,
                            final Map<String, CplxContainer> map) throws Exception {

        for (String swath : subSwathNames) {

            final String subswath = swath.isEmpty() ? "" : '_' + swath.toUpperCase();

            for (String polarisation : polarizations) {
                final String pol = polarisation.isEmpty() ? "" : '_' + polarisation.toUpperCase();

                String mapKey = root.getAttributeInt(AbstractMetadata.ABS_ORBIT) + subswath + pol;

                final String date = OperatorUtils.getAcquisitionDate(root);
                final SLCImage meta = new SLCImage(root, product);

                meta.setMlAz(1);
                meta.setMlRg(1);

                Band bandReal = null;
                Band bandImag = null;
                for (String bandName : product.getBandNames()) {
                    if (bandName.contains(tag) && bandName.contains(date)) {
                        if (subswath.isEmpty() || bandName.contains(subswath)) {
                            if (pol.isEmpty() || bandName.contains(pol)) {
                                final Band band = product.getBand(bandName);
                                if (BandUtilsDoris.isBandReal(band)) {
                                    bandReal = band;
                                } else if (BandUtilsDoris.isBandImag(band)) {
                                    bandImag = band;
                                }
                            }
                        }
                    }
                }
                if(bandReal != null && bandImag != null) {
                    map.put(mapKey, new CplxContainer(date, meta, null, bandReal, bandImag));
                }
            }
        }
    }

    private void constructTargetMetadata() {

        for (String keyMaster : masterMap.keySet()) {
            CplxContainer master = masterMap.get(keyMaster);
            for (String keySlave : slaveMap.keySet()) {
                final CplxContainer slave = slaveMap.get(keySlave);
                if (master.polarisation == null || master.polarisation.equals(slave.polarisation)) {
                    final String productName = keyMaster + '_' + keySlave;
                    final ProductContainer product = new ProductContainer(productName, master, slave, true);
                    targetMap.put(productName, product);
                }
            }
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

        boolean firstBandSaved = false;
        for (Band srcBand : sourceProduct.getBands()) {
            final String srcBandName = srcBand.getName();
            if (srcBand instanceof VirtualBand) {
                ProductUtils.copyVirtualBand(targetProduct, (VirtualBand) srcBand, srcBandName);
            } else {
                if (firstBandSaved) {
                    ProductUtils.copyBand(srcBandName, sourceProduct, targetProduct, true);
                } else {
                    final Band targetBand = new Band(srcBandName,
                            srcBand.getDataType(),
                            srcBand.getRasterWidth(),
                            srcBand.getRasterHeight());

                    targetBand.setUnit(srcBand.getUnit());
                    targetProduct.addBand(targetBand);
                    firstBand = srcBand;
                    firstBandSaved = true;
                }
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

        for (String key : targetMap.keySet()) {
            final CplxContainer master = targetMap.get(key).sourceMaster;
            final CplxContainer slave = targetMap.get(key).sourceSlave;
            final String mstSlvTag = getMasterSlavePairTag(master, slave);
            //System.out.println("SpectralDiversityOp.updateTargetMetadata: mstSlvTag = " + mstSlvTag);

            final MetadataElement mstSlvTagElem = new MetadataElement(mstSlvTag);
            final MetadataElement OverallRgAzShiftElem = new MetadataElement("Overall_Azimuth_Shift");
            OverallRgAzShiftElem.addElement(new MetadataElement(subSwathNames[0]));
            mstSlvTagElem.addElement(OverallRgAzShiftElem);

            final MetadataElement AzShiftPerOverlapElem = new MetadataElement("Azimuth_Shift_Per_Overlap");
            AzShiftPerOverlapElem.addElement(new MetadataElement(subSwathNames[0]));
            mstSlvTagElem.addElement(AzShiftPerOverlapElem);

            final MetadataElement AzShiftPerBlockElem = new MetadataElement("Azimuth_Shift_Per_Block");
            AzShiftPerBlockElem.addElement(new MetadataElement(subSwathNames[0]));
            mstSlvTagElem.addElement(AzShiftPerBlockElem);

            ESDMeasurement.addElement(mstSlvTagElem);
        }
        absTgt.addElement(ESDMeasurement);
    }

    private String getMasterSlavePairTag(final CplxContainer master, final CplxContainer slave) {
        final String mstBandName = master.realBand.getName();
        final String slvBandName = slave.realBand.getName();
        final String mstTag = mstBandName.substring(mstBandName.indexOf("i_") + 2);
        final String slvTag = slvBandName.substring(slvBandName.indexOf("i_") + 2);
        return mstTag + "_" + slvTag;
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
            if (!isAzimuthOffsetAvailable) {
                estimateAzimuthOffset();
            }

            final Band targetBand = targetProduct.getBand(firstBand.getName());
            final Tile targetTile = targetTileMap.get(targetBand);
            if (targetTile != null) {
                targetTile.setRawSamples(getSourceTile(firstBand, targetRectangle).getRawSamples());
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Estimate azimuth offset using ESD approach
     */
    private synchronized void estimateAzimuthOffset() {

        if (isAzimuthOffsetAvailable) {
            return;
        }

        final int numPolarizations = polarizations.length;
        final int numOverlaps = subSwath[subSwathIndex - 1].numOfBursts - 1;
        final int numShifts = numOverlaps * numBlocksPerOverlap;
        final double spectralSeparation = computeSpectralSeparation();

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Estimating azimuth offset... ", numShifts * numPolarizations);

        final ThreadManager threadManager = new ThreadManager();
        try {
            for (String key : targetMap.keySet()) {

                final ProductContainer container = targetMap.get(key);
                final CplxContainer master = container.sourceMaster;
                final CplxContainer slave = container.sourceSlave;

                final Band mBandI = master.realBand;
                final Band mBandQ = master.imagBand;
                final Band sBandI = slave.realBand;
                final Band sBandQ = slave.imagBand;

                final List<AzimuthShiftData> azShiftArray = new ArrayList<>(numShifts);
                final double[][] shiftLUT = new double[numOverlaps][numBlocksPerOverlap];

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
                                        shiftLUT[overlapIndex][blockIndex] = azShift;
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

                final double azOffset = -totalOffset / numShifts;
                SystemUtils.LOG.fine("AzimuthShiftOp: Overall azimuth shift = " + azOffset);

                if (targetOffsetMap.get(key) == null) {
                    targetOffsetMap.put(key, new AzRgOffsets(azOffset, 0.0));
                } else {
                    targetOffsetMap.get(key).setAzOffset(azOffset);
                }

                final String mstSlvTag = getMasterSlavePairTag(master, slave);

                saveOverallAzimuthShift(mstSlvTag, azOffset);

                saveAzimuthShiftPerOverlap(mstSlvTag, averagedAzShiftArray);

                saveAzimuthShiftPerBlock(mstSlvTag, azShiftArray);

                if (outputESDEstimationToFile) {
                    final String fileName = mstSlvTag + "_azimuth_shift.txt";
                    outputESDEstimationToFile(fileName, shiftLUT, -azOffset);
                }
            }

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

    private void saveOverallAzimuthShift(final String mstSlvPairTag, final double azimuthShift) {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        final MetadataElement ESDMeasurement = absTgt.getElement("ESD Measurement");
        final MetadataElement mstSlvPairElem = ESDMeasurement.getElement(mstSlvPairTag);
        final MetadataElement OverallRgAzShiftElem = mstSlvPairElem.getElement("Overall_Azimuth_Shift");
        final MetadataElement swathElem = OverallRgAzShiftElem.getElement(subSwathNames[0]);

        final MetadataAttribute azimuthShiftAttr = new MetadataAttribute("azimuthShift", ProductData.TYPE_FLOAT32);
        azimuthShiftAttr.setUnit("pixel");
        swathElem.addAttribute(azimuthShiftAttr);
        swathElem.setAttributeDouble("azimuthShift", azimuthShift);
    }

    private void saveAzimuthShiftPerOverlap(final String mstSlvPairTag, final double[] averagedAzShiftArray) {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        final MetadataElement ESDMeasurement = absTgt.getElement("ESD Measurement");
        final MetadataElement mstSlvPairElem = ESDMeasurement.getElement(mstSlvPairTag);
        final MetadataElement AzShiftPerOverlapElem = mstSlvPairElem.getElement("Azimuth_Shift_Per_Overlap");
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

    private void saveAzimuthShiftPerBlock(final String mstSlvPairTag, final List<AzimuthShiftData> azShiftArray) {

        final MetadataElement absTgt = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absTgt == null) {
            return;
        }

        final MetadataElement ESDMeasurement = absTgt.getElement("ESD Measurement");
        final MetadataElement mstSlvPairElem = ESDMeasurement.getElement(mstSlvPairTag);
        final MetadataElement AzShiftPerBlockElem = mstSlvPairElem.getElement("Azimuth_Shift_Per_Block");
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

    private static void outputESDEstimationToFile(
            final String fileName, final double[][] shiftLUT, final double overallAzShift) throws OperatorException {

        final File logESDFile = new File(ResourceUtils.getReportFolder(), fileName);
        final int numOverlaps = shiftLUT.length;
        final int numBlocksPerOverlap = shiftLUT[0].length;
        PrintStream p = null;

        try {
            final FileOutputStream out = new FileOutputStream(logESDFile.getAbsolutePath(), false);
            p = new PrintStream(out);

            for (double[] aShiftLUT : shiftLUT) {
                for (int j = 0; j < numBlocksPerOverlap; ++j) {
                    p.format("%13.6f ", aShiftLUT[j]);
                }
                p.println();
            }

            p.println();
            p.print("Mean Azimuth shift = " + overallAzShift);

        } catch (IOException exc) {
            throw new OperatorException(exc);
        } finally {
            if (p != null)
                p.close();
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

    private static class AzRgOffsets {
        double azOffset;
        double rgOffset;

        public AzRgOffsets(final double azOffset, final double rgOffset) {
            this.azOffset = azOffset;
            this.rgOffset = rgOffset;
        }

        public void setAzOffset(final double azOffset) {
            this.azOffset = azOffset;
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
            super(AzimuthShiftEstimationUsingESDOp.class);
        }
    }

}
