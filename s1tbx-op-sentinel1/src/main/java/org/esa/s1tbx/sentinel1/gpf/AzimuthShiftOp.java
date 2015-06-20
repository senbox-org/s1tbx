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
import org.esa.s1tbx.insar.gpf.Sentinel1Utils;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.VirtualBand;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.SourceProduct;
import org.esa.snap.framework.gpf.annotations.TargetProduct;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.gpf.StatusProgressMonitor;
import org.esa.snap.gpf.ThreadManager;
import org.esa.snap.gpf.TileIndex;
import org.esa.snap.util.ProductUtils;

import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Estimate global azimuth offset using Enhanced Spectral Diversity (ESD) approach.
 * Perform azimuth shift for all bursts in a sub-swath with the azimuth offset above
 * using a frequency domain method.
 */

@OperatorMetadata(alias = "Azimuth-Shift",
        category = "SAR Processing/Coregistration/S-1 TOPS Coregistration",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Estimate global azimuth offset for the whole image")
public class AzimuthShiftOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct(description = "The target product which will use the master's grid.")
    private Product targetProduct = null;

    private int cHalfWindowWidth = 0;
    private int cHalfWindowHeight = 0;
    private int rowUpSamplingFactor = 0;
    private int colUpSamplingFactor = 0;
    private boolean isOffsetAvailable = false;
    private double azOffset = 0.0;
    private double noDataValue = -9999.0;
    private Sentinel1Utils su = null;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;
    private int subSwathIndex = 0;
    private String swathIndexStr = null;
    private String[] subSwathNames = null;
    private String[] polarizations = null;

    private int cWindowWidth = 11;
    private int cWindowHeight = 11;

    static final String DerampDemodPhase = "derampDemodPhase";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public AzimuthShiftOp() {
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.snap.framework.datamodel.Product} annotated with the
     * {@link org.esa.snap.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.snap.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            checkDerampDemodPhaseBand();

            cHalfWindowWidth = cWindowWidth / 2;
            cHalfWindowHeight = cWindowHeight / 2;

            su = new Sentinel1Utils(sourceProduct);
            su.computeDopplerRate();
            subSwath = su.getSubSwath();

            subSwathNames = su.getSubSwathNames();
            if (subSwathNames.length != 1) {
                throw new OperatorException("Split product is expected.");
            } else {
                subSwathIndex = 1;//Integer.parseInt(subSwathNames[0].substring(subSwathNames[0].length()-1));
                swathIndexStr = subSwathNames[0].substring(2);
            }

            polarizations = su.getPolarizations();

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void checkDerampDemodPhaseBand() {

        boolean hasDerampDemodPhaseBand = false;
        final Band[] sourceBands = sourceProduct.getBands();
        for (Band band:sourceBands) {
            if (band.getName().equals(DerampDemodPhase)) {
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

        final String[] bandNames = sourceProduct.getBandNames();
        for (String srcBandName : bandNames) {
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
                ReaderUtils.createVirtualIntensityBand(targetProduct, targetProduct.getBand("i" + suffix), targetBand, suffix);
            }
        }

        targetProduct.setPreferredTileSize(512, subSwath[subSwathIndex - 1].linesPerBurst);
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.snap.framework.gpf.OperatorException
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
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        try {
            if (!isOffsetAvailable) {
                estimateOffset();
            }

            Band slvBandI = null, slvBandQ = null;
            Band tgtBandI = null, tgtBandQ = null;
            Band derampDemodPhaseBand = null;
            final Band[] sourceBands = sourceProduct.getBands();
            for (Band band:sourceBands) {
                final String bandName = band.getName();
                if (bandName.contains("i_") && bandName.contains("_slv")) {
                    slvBandI = band;
                    tgtBandI = targetProduct.getBand(bandName);
                }

                if (bandName.contains("q_") && bandName.contains("_slv")) {
                    slvBandQ = band;
                    tgtBandQ = targetProduct.getBand(bandName);
                }

                if (bandName.equals(DerampDemodPhase)) {
                    derampDemodPhaseBand = band;
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

            // perform deramp and demodulation
            final Tile slvTileI = getSourceTile(slvBandI, targetRectangle);
            final Tile slvTileQ = getSourceTile(slvBandQ, targetRectangle);
            final double[][] derampDemodI = new double[h][w];
            final double[][] derampDemodQ = new double[h][w];
            BackGeocodingOp.performDerampDemod(
                    slvTileI, slvTileQ, targetRectangle, derampDemodPhase, derampDemodI, derampDemodQ);

            // compute shift phase
            final double[] phase = new double[2*h];
            computeShiftPhaseArray(azOffset, h, phase);

            // perform azimuth shift using FFT, and perform reramp and remodulation
            final Tile tgtTileI = targetTileMap.get(tgtBandI);
            final Tile tgtTileQ = targetTileMap.get(tgtBandQ);
            final float[] tgtArrayI = (float[]) tgtTileI.getDataBuffer().getElems();
            final float[] tgtArrayQ = (float[]) tgtTileQ.getDataBuffer().getElems();

            final double[] col1 = new double[2 * h];
            final double[] col2 = new double[2 * h];
            final DoubleFFT_1D col_fft = new DoubleFFT_1D(h);
            for (int c = 0; c < w; c++) {
                for (int r = 0; r < h; r++) {
                    col1[2 * r] = derampDemodI[r][c];
                    col1[2 * r + 1] = derampDemodQ[r][c];

                    col2[2 * r] = derampDemodPhase[r][c];
                    col2[2 * r + 1] = 0.0;
                }

                col_fft.complexForward(col1);
                col_fft.complexForward(col2);

                multiplySpectrumByShiftFactor(col1, phase);
                multiplySpectrumByShiftFactor(col2, phase);

                col_fft.complexInverse(col1, true);
                col_fft.complexInverse(col2, true);

                for (int r = 0; r < h; r++) {
                    final double cosPhase = Math.cos(col2[2 * r]);
                    final double sinPhase = Math.sin(col2[2 * r]);
                    tgtArrayI[r * w + c] = (float)(col1[2 * r] * cosPhase + col1[2 * r + 1] * sinPhase);
                    tgtArrayQ[r * w + c] = (float)(-col1[2 * r] * sinPhase + col1[2 * r + 1] * cosPhase);
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        } finally {
            pm.done();
        }
    }

    /**
     * Estimate azimuth offset using ESD approach.
     * @throws Exception The exception.
     */
    private synchronized void estimateOffset() throws Exception {

        if (isOffsetAvailable) {
            return;
        }

        final int[] overlapSizeArray = computeBurstOverlapSize();
        final int numOverlaps = overlapSizeArray.length;

        final StatusProgressMonitor status = new StatusProgressMonitor(StatusProgressMonitor.TYPE.SUBTASK);
        status.beginTask("Estimating azimuth offset... ", numOverlaps);

        final ThreadManager threadManager = new ThreadManager();
        try {
            List<Double> azOffsetArray = new ArrayList<>(numOverlaps);

            for (int i = 0; i < numOverlaps; i++) {
                checkForCancellation();
                final int x0 = 0;
                final int y0 = subSwath[subSwathIndex - 1].linesPerBurst * (i + 1);
                final int w = subSwath[subSwathIndex - 1].samplesPerBurst;
                final int h = overlapSizeArray[i];

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
                            final Band mBandI = getBand("_mst", "i_", swathIndexStr, polarizations[0]);
                            final Band mBandQ = getBand("_mst", "q_", swathIndexStr, polarizations[0]);
                            final Band sBandI = getBand("_slv", "i_", swathIndexStr, polarizations[0]);
                            final Band sBandQ = getBand("_slv", "q_", swathIndexStr, polarizations[0]);

                            final double azOffset = estimateAzOffsets(mBandI, mBandQ, sBandI, sBandQ,
                                    backwardRectangle, forwardRectangle, spectralSeparation);
                            System.out.println("azOffset = " + azOffset);

                            synchronized(azOffsetArray) {
                                azOffsetArray.add(azOffset);
                            }
                        } catch (Throwable e) {
                            OperatorUtils.catchOperatorException("estimateOffset", e);
                        }
                    }
                };
                threadManager.add(worker);

                status.worked(1);
            }

            // todo The following simple average should be replaced by weighted average using coherence as weight
            double sumAzOffset = 0.0;
            for (Double anAzOffset : azOffsetArray) {
                sumAzOffset += anAzOffset;
            }
            azOffset = sumAzOffset / numOverlaps;

            status.done();
            threadManager.finish();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException("estimateOffset", e);
        }

        isOffsetAvailable = true;
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

        double[] mIBackArray = null;
        double[] mQBackArray = null;
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

        double[] sIBackArray = null;
        double[] sQBackArray = null;
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

        double[] mIForArray = null;
        double[] mQForArray = null;
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

        double[] sIForArray = null;
        double[] sQForArray = null;
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
        complexArrayMultiplication(backIntReal, backIntImag, forIntReal, forIntImag, diffIntReal, diffIntImag);

        double sumReal = 0.0;
        double sumImag = 0.0;
        for (int i = 0; i < arrayLength; i++) {
            final double theta = Math.atan2(diffIntImag[i], diffIntReal[i]);
            sumReal += Math.cos(theta);
            sumImag += Math.sin(theta);
        }

        final double phase = Math.atan2(sumImag, sumReal);
        return phase / (2 * Math.PI * spectralSeparation * subSwath[subSwathIndex - 1].azimuthTimeInterval);
    }

    private void complexArrayMultiplication(final double[] realArray1, final double[] imagArray1,
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
     * {@code META-INF/services/org.esa.snap.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.snap.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.snap.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(AzimuthShiftOp.class);
        }
    }

}
