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
import org.esa.s1tbx.insar.gpf.coregistration.CoarseRegistration;
import org.esa.s1tbx.insar.gpf.Sentinel1Utils;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.VirtualBand;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.Parameter;
import org.esa.snap.framework.gpf.annotations.SourceProduct;
import org.esa.snap.framework.gpf.annotations.TargetProduct;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.gpf.StatusProgressMonitor;
import org.esa.snap.gpf.ThreadManager;
import org.esa.snap.util.ProductUtils;

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
        category = "SAR Processing/Coregistration/S-1 TOPS Coregistration",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Estimate constant range offset for the whole image")
public class RangeShiftOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct(description = "The target product which will use the master's grid.")
    private Product targetProduct = null;

    @Parameter(valueSet = {"512", "1024", "2048"}, defaultValue = "512", label = "Registration Window Size")
    private String registrationWindowSize = "512";

    @Parameter(valueSet = {"2", "4", "8", "16"}, defaultValue = "4", label = "Interpolation Factor")
    private String interpFactor = "4";

    @Parameter(description = "The maximum number of iterations", interval = "(1, 20]", defaultValue = "10",
            label = "Max Iterations")
    private int maxIteration = 10;

    private int cWindowSize = 0;
    private int cHalfWindowSize = 0;
    private int upSamplingFactor = 0;
    private boolean isOffsetAvailable = false;
    private double gcpTolerance = 0.0;
    private double azOffset = 0.0;
    private double rgOffset = 0.0;
    private double noDataValue = -9999.0;
    private Sentinel1Utils su = null;
    private Sentinel1Utils.SubSwathInfo[] subSwath = null;
    private int subSwathIndex = 0;
    private String[] subSwathNames = null;
    private String[] polarizations = null;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public RangeShiftOp() {
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
            cWindowSize = Integer.parseInt(registrationWindowSize);
            cHalfWindowSize = cWindowSize / 2;
            upSamplingFactor = Integer.parseInt(interpFactor);
            gcpTolerance = 1.0 / upSamplingFactor;

            su = new Sentinel1Utils(sourceProduct);
            subSwath = su.getSubSwath();

            subSwathNames = su.getSubSwathNames();
            if (subSwathNames.length != 1) {
                throw new OperatorException("Split product is expected.");
            } else {
                subSwathIndex = 1; // subSwathIndex is always 1 because of split product
            }

            if (subSwath[subSwathIndex - 1].samplesPerBurst < cWindowSize) {
                throw new OperatorException("Registration window width should not be grater than burst width " +
                        subSwath[subSwathIndex - 1].samplesPerBurst);
            }

            if (subSwath[subSwathIndex - 1].linesPerBurst < cWindowSize) {
                throw new OperatorException("Registration window height should not be grater than burst height " +
                        subSwath[subSwathIndex - 1].linesPerBurst);
            }

            polarizations = su.getPolarizations();

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

        targetProduct.setPreferredTileSize(sourceProduct.getSceneRasterWidth(), 10);
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
        //final int xMax = x0 + w;
        //final int yMax = y0 + h;
        //System.out.println("DEMBasedCoregistrationOp: x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        try {
            if (!isOffsetAvailable) {
                estimateOffset();
            }

            // perform range shift using FFT
            Band slaveBandI = null;
            Band slaveBandQ = null;
            Band targetBandI = null;
            Band targetBandQ = null;
            final String[] bandNames = sourceProduct.getBandNames();
            for (String bandName : bandNames) {
                if (bandName.contains("i_") && bandName.contains("_slv")) {
                    slaveBandI = sourceProduct.getBand(bandName);
                    targetBandI = targetProduct.getBand(bandName);
                } else if (bandName.contains("q_") && bandName.contains("_slv")) {
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
    private synchronized void estimateOffset() throws Exception {

        if (isOffsetAvailable) {
            return;
        }

        final Rectangle[] rectangleArray = getRectanglesForAllBursts();
        final int numBursts = rectangleArray.length;
        final List<Double> azOffsetArray = new ArrayList<>(numBursts);
        final List<Double> rgOffsetArray = new ArrayList<>(numBursts);

        final StatusProgressMonitor status = new StatusProgressMonitor(numBursts,
                "Estimating azimuth and range offsets... ");
        int tileCnt = 0;

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
                            }
                        } catch (Throwable e) {
                            OperatorUtils.catchOperatorException("estimateOffset", e);
                        }
                    }
                };
                threadManager.add(worker);

                status.worked(tileCnt++);
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

        isOffsetAvailable = true;
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
            super(RangeShiftOp.class);
        }
    }

}
