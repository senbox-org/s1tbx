/*
 * Copyright (C) 2021 by SENSAR B.V. http://www.sensar.nl
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
package org.esa.s1tbx.sar.gpf;

import com.bc.ceres.core.ProgressMonitor;
import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
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
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.eo.Constants;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import javax.media.jai.BorderExtender;
import java.awt.*;
import java.util.*;
import java.util.List;

/**
 * Create a basebanded SLC based on a subband of 1/3 the original bandwidth
 */
@OperatorMetadata(alias = "BandPassFilter",
        category = "Radar/SAR Utilities",
        authors = "Esteban Aguilera, Carlos Hernandez",
        version = "1.0",
        copyright = "Copyright (C) 2021 by SENSAR",
        description = "Creates a basebanded SLC based on a subband of 1/3 the original bandwidth")
public class BandPassFilterOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {"low", "high"},
            defaultValue = "low",
            label = "Subband")
    private String subband = "low";

    @Parameter(valueSet = {"0.5", "0.75", "0.8", "0.9", "1"},
            description = "Hamming alpha",
            defaultValue = "1",
            label = "Hamming alpha")
    private double alpha = 1;

    // Source and target bands
    private final Map<Band, Band> targetSourceMap = new HashMap<>(10);
    private final Map<Band, Band> complexSrcMap = new HashMap<>(10);
    private final Map<Band, Band> complexTgtMap = new HashMap<>(10);

    // Values to be loaded on initialization
    private double sourceAlpha;
    private double rangeSamplingRate;
    private double subbandCenterFrequency;
    private double subbandBandwidth;

    // Constants
    private static final String PRODUCT_SUFFIX_LOW = "_low_subband";
    private static final String PRODUCT_SUFFIX_HIGH = "_high_subband";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public BandPassFilterOp() {

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
            // Load metadata
            getProductMetadata();

            // Create target product
            createTargetProduct();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private double getSourceAlpha(MetadataElement abs) {

        final String windowType = abs.getAttributeString("range_window_type");
        if (windowType != null && windowType.equals("Hamming")) {
            return abs.getAttributeDouble("range_window_coefficient");
        } else {
            SystemUtils.LOG.warning("The source product is assumed to have no spectral windowing applied.");
            return 1;
        }
    }

    private void getProductMetadata() {

        // Load spectral parameters
        final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        sourceAlpha = getSourceAlpha(abs);
        rangeSamplingRate = abs.getAttributeDouble(AbstractMetadata.range_sampling_rate);
        subbandCenterFrequency = abs.getAttributeDouble(AbstractMetadata.range_bandwidth) / 3;
        subbandBandwidth = subbandCenterFrequency;
        if (subband.equals("low")) {
            subbandCenterFrequency = -subbandCenterFrequency;
        }
    }

    private void createTargetProduct() {

        String productSuffix = PRODUCT_SUFFIX_HIGH;
        if (subband.equals("low")) {
            productSuffix = PRODUCT_SUFFIX_LOW;
        }
        targetProduct = new Product(sourceProduct.getName() + productSuffix,
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        for (Band sourceBand : sourceProduct.getBands()) {
            if (sourceBand instanceof VirtualBand) {
                ProductUtils.copyVirtualBand(targetProduct, (VirtualBand) sourceBand, sourceBand.getName());
            } else {
                ProductUtils.copyBand(sourceBand.getName(), sourceProduct, targetProduct, false);
            }
        }

        // Update range_bandwidth and radar_frequency
        final MetadataElement absSource = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        final MetadataElement absTarget = AbstractMetadata.getAbstractedMetadata(targetProduct);
        absTarget.setAttributeDouble(AbstractMetadata.range_bandwidth, subbandBandwidth);
        absTarget.setAttributeDouble(AbstractMetadata.radar_frequency,
                                     absSource.getAttributeDouble(AbstractMetadata.radar_frequency)
                                             + subbandCenterFrequency);

        // Get target and source bands
        for (Band tgtBandQ : targetProduct.getBands()) {
            if (tgtBandQ.getUnit().equals(Unit.IMAGINARY)) {
                final Band tgtBandI = targetProduct.getBand(tgtBandQ.getName().replace("q_", "i_"));
                complexTgtMap.put(tgtBandI, tgtBandQ);
                for (Band srcBandI : sourceProduct.getBands()) {
                    if (srcBandI.getName().equals(tgtBandI.getName())) {
                        final Band srcBandQ = sourceProduct.getBand(srcBandI.getName().replace("i_", "q_"));
                        complexSrcMap.put(srcBandI, srcBandQ);
                        targetSourceMap.put(tgtBandI, srcBandI);
                    }
                }
            }
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTileMap   The target tiles associated with all target bands to be computed.
     * @param targetRectangle The rectangle of target tile.
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    public void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        // Add borders
        int w = targetRectangle.width;
        final Rectangle sourceRectangle = new Rectangle(targetRectangle.x - Math.floorDiv(w, 2),
                                                        targetRectangle.y,
                                                        targetRectangle.width + Math.floorDiv(w, 2) * 2,
                                                        targetRectangle.height);
        final BorderExtender border = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

        for (Band targetBandI : complexTgtMap.keySet()) { // for each SLC
            // Get target and source bands
            final Band sourceBandI = targetSourceMap.get(targetBandI);
            final Band sourceBandQ = complexSrcMap.get(sourceBandI);
            final Band targetBandQ = complexTgtMap.get(targetBandI);

            // Get tiles
            final Tile sourceTileI = getSourceTile(sourceBandI, sourceRectangle, border);
            final Tile sourceTileQ = getSourceTile(sourceBandQ, sourceRectangle, border);

            final Tile targetTileI = targetTileMap.get(targetBandI);
            final Tile targetTileQ = targetTileMap.get(targetBandQ);

            // Apply bandpass filter
            applyBandPassFilter(rangeSamplingRate * 1E6, subbandCenterFrequency * 1E6,
                                subbandBandwidth * 1E6, alpha, sourceAlpha,
                                sourceTileI, sourceTileQ, targetTileI, targetTileQ,
                                sourceRectangle, targetRectangle);
        }
    }

    private static void applyBandPassFilter(final double samplingRate, final double centerFrequency,
                                            final double bandwidth, final double alpha, final double sourceAlpha,
                                            final Tile sourceTileI, final Tile sourceTileQ,
                                            final Tile targetTileI, final Tile targetTileQ,
                                            final Rectangle sourceRectangle, final Rectangle targetRectangle) {

        int x0 = sourceRectangle.x;
        int y0 = sourceRectangle.y;
        int w = sourceRectangle.width;
        int h = sourceRectangle.height;
        int xMax = x0 + w;
        int yMax = y0 + h;

        // Get real and imaginary parts
        final ProductData sourceBufferI = sourceTileI.getDataBuffer();
        final ProductData sourceBufferQ = sourceTileQ.getDataBuffer();

        final TileIndex sourceIndex = new TileIndex(sourceTileI);

        final double[][] dataI = new double[h][w];
        final double[][] dataQ = new double[h][w];
        for (int y = y0; y < yMax; y++) {
            sourceIndex.calculateStride(y);
            final int yy = y - y0;
            for (int x = x0; x < xMax; x++) {
                final int sourceIdx = sourceIndex.getIndex(x);
                final int xx = x - x0;

                // Get values for later use
                dataI[yy][xx] = sourceBufferI.getElemDoubleAt(sourceIdx);
                dataQ[yy][xx] = sourceBufferQ.getElemDoubleAt(sourceIdx);
            }
        }

        // Center SLC spectrum around desired frequency
        for (int y = 0; y < dataI.length; y++) {
            for (int x = x0; x < xMax; x++) {
                // Compute cos and sin of phase ramp
                final double phase = -Constants.TWO_PI * centerFrequency * x / samplingRate;
                final double cosPhase = FastMath.cos(phase);
                final double sinPhase = FastMath.sin(phase);

                // Apply phase ramp in place
                final int xx = x - x0;
                final double valueI = dataI[y][xx] * cosPhase - dataQ[y][xx] * sinPhase;
                final double valueQ = dataI[y][xx] * sinPhase + dataQ[y][xx] * cosPhase;
                dataI[y][xx] = valueI;
                dataQ[y][xx] = valueQ;
            }
        }

        // Compute original spectral window and center around desired frequency
        double[] sourceWindow = new double[dataI[0].length];
        for (int n = 0; n < sourceWindow.length; n++) {
            sourceWindow[n] = hamming(sourceAlpha, n, sourceWindow.length);
        }
        sourceWindow = rotate(sourceWindow, -Math.floorDiv(sourceWindow.length, 2));

        double[] line = new double[2 * sourceWindow.length];
        DoubleFFT_1D fft = new DoubleFFT_1D(dataI[0].length);
        for (int n = 0; n < sourceWindow.length; n++) {
            int n2 = n * 2;
            line[n2] = sourceWindow[n];
            line[n2 + 1] = 0;
        }

        fft.complexInverse(line, true);
        double[] xAxis = new double[dataI[0].length];
        for (int x = x0; x < xMax; x++) {
            final int xx = x - x0;
            xAxis[xx] = x;
        }
        xAxis = rotate(xAxis, -Math.floorDiv(xAxis.length, 2));
        for (int x = x0; x < xMax; x++) {
            final int xx = x - x0;
            final int xx2 = xx * 2;

            // Compute cos and sin of phase ramp
            final double phase = -Constants.TWO_PI * centerFrequency * xAxis[xx] / samplingRate;
            final double cosPhase = FastMath.cos(phase);
            final double sinPhase = FastMath.sin(phase);

            // Apply phase ramp in place
            final double valueI = line[xx2] * cosPhase - line[xx2 + 1] * sinPhase;
            final double valueQ = line[xx2] * sinPhase + line[xx2 + 1] * cosPhase;
            line[xx2] = valueI;
            line[xx2 + 1] = valueQ;
        }
        fft.complexForward(line);

        for (int n = 0; n < sourceWindow.length; n++) {
            int n2 = n * 2;
            sourceWindow[n] = Math.sqrt(Math.pow(line[n2], 2) + Math.pow(line[n2 + 1], 2));
        }

        // Apply baseband filter in place
        final int hammingSize = (int) Math.round(dataI[0].length * bandwidth / samplingRate);
        double[] window = new double[dataI[0].length];
        for (int n = 0; n < window.length; n++) {
            if (n < hammingSize) {
                window[n] = hamming(alpha, n, hammingSize);
            } else {
                window[n] = 0.0;
            }
        }
        window = rotate(window, -Math.floorDiv(hammingSize, 2));

        line = new double[2 * dataI[0].length];
        fft = new DoubleFFT_1D(dataI[0].length);
        for (int y = 0; y < dataI.length; y++) {
            for (int x = 0; x < dataI[0].length; x++) {
                int x2 = x * 2;
                line[x2] = dataI[y][x];
                line[x2 + 1] = dataQ[y][x];
            }

            fft.complexForward(line);
            for (int n = 0; n < dataI[0].length; n++) {
                int n2 = n * 2;
                double weight = window[n];
                if (sourceWindow[n] > Double.MIN_VALUE) {
                    weight /= sourceWindow[n];
                }
                line[n2] = line[n2] * weight;
                line[n2 + 1] = line[n2 + 1] * weight;
            }
            fft.complexInverse(line, true);

            for (int x = 0; x < dataI[0].length; x++) {
                int x2 = x * 2;
                dataI[y][x] = line[x2];
                dataQ[y][x] = line[x2 + 1];
            }
        }

        // Write result
        final int overlapX = Math.floorDiv((sourceRectangle.width - targetRectangle.width), 2);
        final int overlapY = Math.floorDiv((sourceRectangle.height - targetRectangle.height), 2);
        x0 = targetRectangle.x;
        y0 = targetRectangle.y;
        w = targetRectangle.width;
        h = targetRectangle.height;
        xMax = x0 + w;
        yMax = y0 + h;

        final ProductData targetBufferI = targetTileI.getDataBuffer();
        final ProductData targetBufferQ = targetTileQ.getDataBuffer();

        final TileIndex targetIndex = new TileIndex(targetTileI);

        for (int y = y0; y < yMax; y++) {
            targetIndex.calculateStride(y);
            final int yy = y - y0 + overlapY;
            for (int x = x0; x < xMax; x++) {
                final int targetIdx = targetIndex.getIndex(x);
                final int xx = x - x0 + overlapX;

                targetBufferI.setElemDoubleAt(targetIdx, dataI[yy][xx]);
                targetBufferQ.setElemDoubleAt(targetIdx, dataQ[yy][xx]);
            }
        }
    }

    /**
     * Computes hamming weight.
     */
    private static double hamming(final double alpha, final int n, final int size) {

        return alpha + (1 - alpha) * FastMath.cos((2 * Math.PI * n) / (size - 1) - Math.PI);
    }

    /**
     * Circularly shifts array.
     */
    private static double[] rotate(final double[] data, final int shift) {

        Double[] auxData = new Double[data.length];
        for (int i = 0; i < data.length; i++) {
            auxData[i] = data[i];
        }
        List<Double> dataList = Arrays.asList(auxData);
        Collections.rotate(dataList, shift);
        return dataList.stream().mapToDouble(Double::doubleValue).toArray();
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

            super(org.esa.s1tbx.sar.gpf.BandPassFilterOp.class);
        }
    }
}