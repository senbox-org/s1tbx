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
package org.esa.s1tbx.sentinel1.gpf;

import com.bc.ceres.core.ProgressMonitor;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import org.apache.commons.math3.util.FastMath;
import org.esa.s1tbx.insar.gpf.support.Sentinel1Utils;
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
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.*;
import org.jblas.ComplexDoubleMatrix;
import org.jlinda.core.coregistration.utils.CoregistrationUtils;
import org.jlinda.nest.utils.TileUtilsDoris;

import java.awt.Rectangle;
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

@OperatorMetadata(alias = "Range-Shift",
        category = "Radar/Coregistration/S-1 TOPS Coregistration",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Estimate constant range offset for the whole image")
public class RangeShiftOp extends Operator {

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

    // parameters for fine coregistration using cross-correlation
    private int fineWinWidth = 0;
    private int fineWinHeight = 0;
    private int fineWinAccY = 0;
    private int fineWinAccX = 0;
    private int fineWinOvsFactor = 0;

    private boolean isRangeOffsetAvailable = false;
    private double azOffset = 0.0;
    private double rgOffset = 0.0;
    private Double noDataValue = -9999.0;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;
    private int subSwathIndex = 0;
    private String[] subSwathNames = null;
    private Band mstBandI = null;
    private Band mstBandQ = null;
    private Band slvBandI = null;
    private Band slvBandQ = null;

    private static final int maxRangeShift = 1;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public RangeShiftOp() {
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

            fineWinWidth = Integer.parseInt(fineWinWidthStr);
            fineWinHeight = Integer.parseInt(fineWinHeightStr);
            fineWinAccY = Integer.parseInt(fineWinAccAzimuth);
            fineWinAccX = Integer.parseInt(fineWinAccRange);
            fineWinOvsFactor = Integer.parseInt(fineWinOversampling);

            final Sentinel1Utils su = new Sentinel1Utils(sourceProduct);
            subSwath = su.getSubSwath();

            subSwathNames = su.getSubSwathNames();
            if (subSwathNames.length != 1) {
                throw new OperatorException("Split product is expected.");
            } else {
                subSwathIndex = 1; // subSwathIndex is always 1 because of split product
            }

            if (subSwath[subSwathIndex - 1].samplesPerBurst < fineWinWidth) {
                throw new OperatorException("Registration window width should not be grater than burst width " +
                        subSwath[subSwathIndex - 1].samplesPerBurst);
            }

            if (subSwath[subSwathIndex - 1].linesPerBurst < fineWinHeight) {
                throw new OperatorException("Registration window height should not be grater than burst height " +
                        subSwath[subSwathIndex - 1].linesPerBurst);
            }

            mstBandI = getSourceBand(StackUtils.MST, Unit.REAL);
            mstBandQ = getSourceBand(StackUtils.MST, Unit.IMAGINARY);
            slvBandI = getSourceBand(StackUtils.SLV, Unit.REAL);
            slvBandQ = getSourceBand(StackUtils.SLV, Unit.IMAGINARY);

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
            if (srcBandName.contains(StackUtils.MST) || srcBandName.contains("derampDemod")) {
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

        targetProduct.setPreferredTileSize(sourceProduct.getSceneRasterWidth(), 10);
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

        final int w = targetRectangle.width;
        final int h = targetRectangle.height;

        try {

            if (!isRangeOffsetAvailable) {
                estimateRangeOffset();
            }

            // perform range shift using FFT
            Band slaveBandI = null, slaveBandQ = null;
            Band targetBandI = null, targetBandQ = null;
            final String[] bandNames = sourceProduct.getBandNames();
            for (String bandName : bandNames) {
                if (bandName.contains("i_") && bandName.contains(StackUtils.SLV)) {
                    slaveBandI = sourceProduct.getBand(bandName);
                    targetBandI = targetProduct.getBand(bandName);
                } else if (bandName.contains("q_") && bandName.contains(StackUtils.SLV)) {
                    slaveBandQ = sourceProduct.getBand(bandName);
                    targetBandQ = targetProduct.getBand(bandName);
                }
            }

            final Tile slvTileI = getSourceTile(slaveBandI, targetRectangle);
            final Tile slvTileQ = getSourceTile(slaveBandQ, targetRectangle);
            final Tile tgtTileI = targetTileMap.get(targetBandI);
            final Tile tgtTileQ = targetTileMap.get(targetBandQ);
            final float[] slvArrayI = (float[]) slvTileI.getDataBuffer().getElems();
            final float[] slvArrayQ = (float[]) slvTileQ.getDataBuffer().getElems();
            final float[] tgtArrayI = (float[]) tgtTileI.getDataBuffer().getElems();
            final float[] tgtArrayQ = (float[]) tgtTileQ.getDataBuffer().getElems();

            /*
            //========== test data generation
            rgOffset = 1.0;//0.009;
            Band slaveBandI = null, slaveBandQ = null;
            Band targetBandI = null, targetBandQ = null;
            final String[] bandNames = sourceProduct.getBandNames();
            for (String bandName : bandNames) {
                if (bandName.contains("i_") && bandName.contains(StackUtils.MST)) {
                    slaveBandI = sourceProduct.getBand(bandName);
                } else if (bandName.contains("q_") && bandName.contains(StackUtils.MST)) {
                    slaveBandQ = sourceProduct.getBand(bandName);
                } else if (bandName.contains("i_") && bandName.contains(StackUtils.SLV)) {
                    targetBandI = targetProduct.getBand(bandName);
                } else if (bandName.contains("q_") && bandName.contains(StackUtils.SLV)) {
                    targetBandQ = targetProduct.getBand(bandName);
                }
            }

            final Tile slvTileI = getSourceTile(slaveBandI, targetRectangle);
            final Tile slvTileQ = getSourceTile(slaveBandQ, targetRectangle);
            final Tile tgtTileI = targetTileMap.get(targetBandI);
            final Tile tgtTileQ = targetTileMap.get(targetBandQ);
            final short[] slvArrayIS = (short[]) slvTileI.getDataBuffer().getElems();
            final short[] slvArrayQS = (short[]) slvTileQ.getDataBuffer().getElems();
            final float[] slvArrayI = new float[slvArrayIS.length];
            final float[] slvArrayQ = new float[slvArrayQS.length];
            for (int i = 0; i < slvArrayIS.length; i++) {
                slvArrayI[i] = (float)slvArrayIS[i];
                slvArrayQ[i] = (float)slvArrayQS[i];
            }
            final float[] tgtArrayI = (float[]) tgtTileI.getDataBuffer().getElems();
            final float[] tgtArrayQ = (float[]) tgtTileQ.getDataBuffer().getElems();
            //==========
            */

            final double[] line = new double[2*w];
            final double[] phase = new double[2*w];
            final DoubleFFT_1D row_fft = new DoubleFFT_1D(w);

            computeShiftPhaseArray(rgOffset, w, phase);

            for (int r = 0; r < h; r++) {
                final int rw = r * w;
                for (int c = 0; c < w; c++) {
                    line[2 * c] = slvArrayI[rw + c];
                    line[2 * c + 1] = slvArrayQ[rw + c];
                }

                row_fft.complexForward(line);

                multiplySpectrumByShiftFactor(line, phase);

                row_fft.complexInverse(line, true);

                for (int c = 0; c < w; c++) {
                    tgtArrayI[rw + c] = (float)line[2 * c];
                    tgtArrayQ[rw + c] = (float)line[2 * c + 1];
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Estimate range and azimuth offset using cross-correlation.
     * @throws Exception The exception.
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
            for (int i = 0; i < numBursts; i++) {
                checkForCancellation();
                final int burstIndex = i;

                final Thread worker = new Thread() {
                    @Override
                    public void run() {
                        try {
                            final double[] offset = new double[2]; // az/rg offset

                            estimateAzRgOffsets(burstIndex, offset);

                            /*System.out.println("x0 = " + rectangle.x + ", y0 = " + rectangle.y +
                                    ", w = " + rectangle.width + ", h = " + rectangle.height +
                                    ", azOffset = " + offset[0] + ", rgOffset = " + offset[1]);*/

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

                //SystemUtils.LOG.info("RangeShiftOp: burst = " + burstIndexArray.get(i) + ", azimuth offset = " + azShift);
                //SystemUtils.LOG.info("RangeShiftOp: burst = " + burstIndexArray.get(i) + ", range offset = " + rgShift);

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
                azOffset = sumAzOffset / count;
                rgOffset = sumRgOffset / count;
            } else {
                throw new OperatorException("estimateOffset failed.");
            }

            saveOverallRangeShift(rgOffset);

            saveRangeShiftPerBurst(rgOffsetArray, burstIndexArray);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("estimateOffset", e);
        }

        isRangeOffsetAvailable = true;
        //SystemUtils.LOG.info("RangeShiftOp: whole image azimuth offset = " + azOffset);
        SystemUtils.LOG.info("RangeShiftOp: Overall range shift = " + rgOffset);
    }

    private void estimateAzRgOffsets(final int burstIndex, final double[] offset) {

        final int burstHeight = subSwath[subSwathIndex - 1].linesPerBurst;
        final int burstWidth = subSwath[subSwathIndex - 1].samplesPerBurst;
        final int x0 = burstWidth / 2;
        final int y0 = burstHeight / 2 + burstIndex * burstHeight;
        final PixelPos mGCP = new PixelPos(x0, y0);
        final PixelPos sGCP = new PixelPos(x0, y0);

        getFineOffsets(mGCP, sGCP, offset);
    }

    private void getFineOffsets(final PixelPos mGCPPixelPos, final PixelPos sGCPPixelPos, final double[] offset) {

        try {
            ComplexDoubleMatrix mI = getComplexDoubleMatrix(
                    mstBandI, mstBandQ, mGCPPixelPos, fineWinWidth, fineWinHeight);

            ComplexDoubleMatrix sI = getComplexDoubleMatrix(
                    slvBandI, slvBandQ, sGCPPixelPos, fineWinWidth, fineWinHeight);

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

    private ComplexDoubleMatrix getComplexDoubleMatrix(
            final Band band1, final Band band2, final PixelPos pixelPos, final int fineWinWidth, final int fineWinHeight) {

        Rectangle rectangle = defineRectangleMask(pixelPos, fineWinWidth, fineWinHeight);
        Tile tileReal = getSourceTile(band1, rectangle);
        Tile tileImag = getSourceTile(band2, rectangle);
        return TileUtilsDoris.pullComplexDoubleMatrix(tileReal, tileImag);
    }

    private Rectangle defineRectangleMask(final PixelPos pixelPos, final int fineWinWidth, final int fineWinHeight) {
        int l0 = (int) (pixelPos.y - fineWinHeight/2);
        int lN = (int) (pixelPos.y + fineWinHeight/2 - 1);
        int p0 = (int) (pixelPos.x - fineWinWidth/2);
        int pN = (int) (pixelPos.x + fineWinWidth/2 - 1);
        return new Rectangle(p0, l0, pN - p0 + 1, lN - l0 + 1);
    }

    private Band getSourceBand(final String suffix, final String bandUnit) {

        final String[] bandNames = sourceProduct.getBandNames();
        for (String bandName : bandNames) {
            if (!bandName.contains(suffix)) {
                continue;
            }
            final Band band = sourceProduct.getBand(bandName);
            if (band.getUnit().contains(bandUnit)) {
                return band;
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
     * @see OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(RangeShiftOp.class);
        }
    }

}
