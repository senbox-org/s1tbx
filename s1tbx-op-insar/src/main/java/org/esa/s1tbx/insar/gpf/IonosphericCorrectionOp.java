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
package org.esa.s1tbx.insar.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import javax.media.jai.BorderExtender;
import java.awt.*;
import java.util.Arrays;
import java.util.Map;

/**
 * Estimation of Ionospheric Phase Screen
 */
@OperatorMetadata(alias = "IonosphericCorrection",
        category = "Radar/Interferometric/Products",
        authors = "Esteban Aguilera, Carlos Hernandez",
        version = "1.0",
        copyright = "Copyright (C) 2021 by SENSAR",
        description = "Estimation of Ionospheric Phase Screens")
public class IonosphericCorrectionOp extends Operator {

    @SourceProducts
    private Product[] sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "Standard deviation for Gaussian filter",
            defaultValue = "81",
            label = "Sigma")
    private int sigma = 81;

    @Parameter(description = "Coherence threshold",
            interval = "[0, 1]",
            defaultValue = "0.6",
            label = "Coherence threshold")
    private double coherenceThreshold = 0.6;

    @Parameter(description = "Minimum coherence for output mask",
            interval = "[0, 1]",
            defaultValue = "0.2",
            label = "Min coherence for output mask")
    private double minCoherence = 0.2;

    // Targets bands
    private Band targetBandIonosphericPhase;
    private Band targetBandWrappedIonosphericPhase;
    private Band targetBandWrappedOriginalPhase;
    private Band targetBandWrappedCorrectedPhase;
    private Band targetBandCoherence;

    // Source bands and values to be loaded on initialization
    private Product productFull;
    private Band unwrappedPhaseLow;
    private Band unwrappedPhaseHigh;
    private Band unwrappedPhaseFull;
    private Band coherenceFull;
    private double centerFreqLow;
    private double centerFreqHigh;
    private double centerFreqFull;

    // Constants
    private static final String PRODUCT_SUFFIX = "_iono";
    private static final String IONOSPHERIC_PHASE_BAND_NAME = "ionosphericPhase";
    private static final String WRAPPED_IONOSPHERIC_PHASE_BAND_NAME = "wrappedIonosphericPhase";
    private static final String WRAPPED_ORIGINAL_PHASE_BAND_NAME = "wrappedOriginalPhase";
    private static final String WRAPPED_CORRECTED_PHASE_BAND_NAME = "wrappedCorrectedPhase";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public IonosphericCorrectionOp() {

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
            // Validate input products
            validateInputProducts();

            // Load metadata and source bands
            getProductMetadataAndSourceBands();

            // Create target product
            createTargetProduct();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void validateInputProducts() {

        // Validate number of input products
        if (sourceProduct.length != 3) {
            throw new OperatorException("This operator requires three input products.");
        }

        // Validate dimensions
        final Dimension rasterSize0 = sourceProduct[0].getSceneRasterSize();
        final Dimension rasterSize1 = sourceProduct[1].getSceneRasterSize();
        final Dimension rasterSize2 = sourceProduct[2].getSceneRasterSize();
        if (!rasterSize0.equals(rasterSize1) || !rasterSize0.equals(rasterSize2) || !rasterSize1.equals(rasterSize2)) {
            throw new OperatorException("Input products must have equal dimensions.");
        }
    }

    private void getProductMetadataAndSourceBands() {

        // Load and sort center frequencies
        final double[] frequencies = new double[3];
        final double[] sortedFrequencies = new double[3];
        for (int i = 0; i < sourceProduct.length; i++) {
            final MetadataElement abs = AbstractMetadata.getAbstractedMetadata(sourceProduct[i]);
            frequencies[i] = abs.getAttributeDouble(AbstractMetadata.radar_frequency) * 1E6;
        }
        if (frequencies[0] == frequencies[1] || frequencies[0] == frequencies[2] || frequencies[1] == frequencies[2]) {
            throw new OperatorException("The input products should have different center frequencies.");
        }
        System.arraycopy(frequencies, 0, sortedFrequencies, 0, 3);
        Arrays.sort(sortedFrequencies);

        // Load source bands and corresponding center frequencies
        for (int i = 0; i < sourceProduct.length; i++) {
            Band bandPhase = null;
            Band bandCoherence = null;
            for (String bandName : sourceProduct[i].getBandNames()) {
                if (bandName.contains("Unw_Phase_ifg")) {
                    bandPhase = sourceProduct[i].getBand(bandName);
                } else if (bandName.contains("coh")) {
                    bandCoherence = sourceProduct[i].getBand(bandName);
                }
            }
            if (bandPhase == null) {
                throw new OperatorException(
                        String.format("Band with interferometric phase is missing in input product %d.", i));
            }
            if (frequencies[i] == sortedFrequencies[0]) {
                centerFreqLow = frequencies[0];
                unwrappedPhaseLow = bandPhase;
            } else if (frequencies[i] == sortedFrequencies[1]) {
                centerFreqHigh = frequencies[2];
                unwrappedPhaseHigh = bandPhase;
            } else {
                if (bandCoherence == null) {
                    throw new OperatorException("Full-bandwidth coherence band is missing.");
                }
                centerFreqFull = frequencies[1];
                unwrappedPhaseFull = bandPhase;
                coherenceFull = bandCoherence;
                productFull = sourceProduct[i];
            }
        }
    }

    private void createTargetProduct() {

        targetProduct = new Product(productFull.getName() + PRODUCT_SUFFIX,
                                    productFull.getProductType(),
                                    productFull.getSceneRasterWidth(),
                                    productFull.getSceneRasterHeight());

        ProductUtils.copyProductNodes(productFull, targetProduct);

        // Define mask for output bands
        String validExpression = String.format("%s >= %f", coherenceFull.getName(), minCoherence);

        // Band for ionospheric phase
        targetBandIonosphericPhase = targetProduct.addBand(IONOSPHERIC_PHASE_BAND_NAME, ProductData.TYPE_FLOAT32);
        targetBandIonosphericPhase.setNoDataValue(Double.NaN);
        targetBandIonosphericPhase.setNoDataValueUsed(true);
        targetBandIonosphericPhase.setUnit(Unit.PHASE);
        targetBandIonosphericPhase.setDescription("Ionospheric Phase (unwrapped)");
        targetBandIonosphericPhase.setValidPixelExpression(validExpression);

        // Band for ionospheric phase (wrapped)
        targetBandWrappedIonosphericPhase = targetProduct.addBand(WRAPPED_IONOSPHERIC_PHASE_BAND_NAME, ProductData.TYPE_FLOAT32);
        targetBandWrappedIonosphericPhase.setNoDataValue(Double.NaN);
        targetBandWrappedIonosphericPhase.setNoDataValueUsed(true);
        targetBandWrappedIonosphericPhase.setUnit(Unit.PHASE);
        targetBandWrappedIonosphericPhase.setDescription("Ionospheric Phase (wrapped)");
        targetBandWrappedIonosphericPhase.setValidPixelExpression(validExpression);

        // Band for original interferometric phase (wrapped)
        targetBandWrappedOriginalPhase = targetProduct.addBand(WRAPPED_ORIGINAL_PHASE_BAND_NAME, ProductData.TYPE_FLOAT32);
        targetBandWrappedOriginalPhase.setNoDataValue(Double.NaN);
        targetBandWrappedOriginalPhase.setNoDataValueUsed(true);
        targetBandWrappedOriginalPhase.setUnit(Unit.PHASE);
        targetBandWrappedOriginalPhase.setDescription("Original Interferometric Phase (wrapped)");
        targetBandWrappedOriginalPhase.setValidPixelExpression(validExpression);

        // Band for corrected interferometric phase (wrapped)
        targetBandWrappedCorrectedPhase = targetProduct.addBand(WRAPPED_CORRECTED_PHASE_BAND_NAME, ProductData.TYPE_FLOAT32);
        targetBandWrappedCorrectedPhase.setNoDataValue(Double.NaN);
        targetBandWrappedCorrectedPhase.setNoDataValueUsed(true);
        targetBandWrappedCorrectedPhase.setUnit(Unit.PHASE);
        targetBandWrappedCorrectedPhase.setDescription("Corrected Interferometric Phase (wrapped)");
        targetBandWrappedCorrectedPhase.setValidPixelExpression(validExpression);

        // Band for coherence
        targetBandCoherence = ProductUtils.copyBand(coherenceFull.getName(), productFull, targetProduct, false);
        targetBandCoherence.setSourceImage(coherenceFull.getSourceImage());
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
        final int kernelSize = computeGaussianKernelSize(sigma);
        final Rectangle sourceRectangle = new Rectangle(targetRectangle.x - Math.floorDiv(kernelSize, 2),
                                                        targetRectangle.y - Math.floorDiv(kernelSize, 2),
                                                        targetRectangle.width + Math.floorDiv(kernelSize, 2) * 2,
                                                        targetRectangle.height + Math.floorDiv(kernelSize, 2) * 2);
        final BorderExtender border = BorderExtender.createInstance(BorderExtender.BORDER_ZERO);

        // Get tiles
        final Tile unwrappedPhaseLowTile = getSourceTile(unwrappedPhaseLow, sourceRectangle, border);
        final Tile unwrappedPhaseHighTile = getSourceTile(unwrappedPhaseHigh, sourceRectangle, border);
        final Tile unwrappedPhaseFullTile = getSourceTile(unwrappedPhaseFull, sourceRectangle, border);
        final Tile coherenceFullTile = getSourceTile(coherenceFull, sourceRectangle, border);

        final Tile ionosphericPhaseTile = targetTileMap.get(targetBandIonosphericPhase);
        final Tile wrappedIonosphericPhaseTile = targetTileMap.get(targetBandWrappedIonosphericPhase);
        final Tile wrappedOriginalPhaseTile = targetTileMap.get(targetBandWrappedOriginalPhase);
        final Tile wrappedCorrectedPhaseTile = targetTileMap.get(targetBandWrappedCorrectedPhase);

        // Estimate ionospheric phase
        estimateIonosphericPhase(centerFreqLow, centerFreqHigh, centerFreqFull, sigma, coherenceThreshold,
                                 unwrappedPhaseLowTile, unwrappedPhaseHighTile, unwrappedPhaseFullTile,
                                 coherenceFullTile, ionosphericPhaseTile, wrappedIonosphericPhaseTile,
                                 wrappedOriginalPhaseTile, wrappedCorrectedPhaseTile,
                                 sourceRectangle, targetRectangle);
    }

    private static void estimateIonosphericPhase(final double centerFreqLow, final double centerFreqHigh, final double centerFreqFull,
                                                 final int sigma, final double coherenceThreshold,
                                                 final Tile unwrappedPhaseLowTile, final Tile unwrappedPhaseHighTile,
                                                 final Tile unwrappedPhaseFullTile, final Tile coherenceFullTile,
                                                 final Tile ionosphericPhaseTile, final Tile wrappedIonosphericPhaseTile,
                                                 final Tile wrappedOriginalPhaseTile, final Tile wrappedCorrectedPhaseTile,
                                                 final Rectangle sourceRectangle, final Rectangle targetRectangle) {

        // Compute raw ionospheric phase
        int x0 = sourceRectangle.x;
        int y0 = sourceRectangle.y;
        int w = sourceRectangle.width;
        int h = sourceRectangle.height;
        int xMax = x0 + w;
        int yMax = y0 + h;

        final ProductData sourceBufferUnwrappedPhaseLow = unwrappedPhaseLowTile.getDataBuffer();
        final ProductData sourceBufferUnwrappedPhaseHigh = unwrappedPhaseHighTile.getDataBuffer();
        final ProductData sourceBufferUnwrappedPhaseFull = unwrappedPhaseFullTile.getDataBuffer();
        final ProductData sourceBufferCoherenceFull = coherenceFullTile.getDataBuffer();

        final TileIndex sourceIndex = new TileIndex(unwrappedPhaseFullTile);

        final double[][] ionosphericPhase = new double[h][w];
        final double[][] coherence = new double[h][w];
        final double[][] originalPhase = new double[h][w];
        for (int y = y0; y < yMax; y++) {
            sourceIndex.calculateStride(y);
            final int yy = y - y0;
            for (int x = x0; x < xMax; x++) {
                final int sourceIdx = sourceIndex.getIndex(x);
                final int xx = x - x0;

                // Get values
                final double phaseLow = sourceBufferUnwrappedPhaseLow.getElemDoubleAt(sourceIdx);
                final double phaseHigh = sourceBufferUnwrappedPhaseHigh.getElemDoubleAt(sourceIdx);
                final double phaseFull = sourceBufferUnwrappedPhaseFull.getElemDoubleAt(sourceIdx);
                final double cohFull = sourceBufferCoherenceFull.getElemDoubleAt(sourceIdx);

                // Solve for ionospheric phase
                final double ionoPhase = (centerFreqLow * centerFreqHigh)
                        / (centerFreqFull * (Math.pow(centerFreqHigh, 2) - Math.pow(centerFreqLow, 2)))
                        * (phaseLow * centerFreqHigh - phaseHigh * centerFreqLow);

                // Store values for later use
                ionosphericPhase[yy][xx] = ionoPhase;
                coherence[yy][xx] = cohFull;
                originalPhase[yy][xx] = phaseFull;
            }
        }

        // Filter ionospheric phase
        final double[][] thresholdedCoherence = new double[h][w];
        for (int y = 0; y < thresholdedCoherence.length; y++) {
            for (int x = 0; x < thresholdedCoherence[0].length; x++) {
                thresholdedCoherence[y][x] = (coherence[y][x] < coherenceThreshold) ? 0.0 : coherence[y][x];
            }
        }
        final double[][] filteredIonosphericPhase = filter(ionosphericPhase, thresholdedCoherence,
                                                           sigma);

        // Apply phase correction
        final double[][] correctedPhase = new double[h][w];
        for (int y = 0; y < filteredIonosphericPhase.length; y++) {
            for (int x = 0; x < filteredIonosphericPhase[0].length; x++) {
                correctedPhase[y][x] = originalPhase[y][x] - filteredIonosphericPhase[y][x];
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

        final ProductData targetBufferIonosphericPhase = ionosphericPhaseTile.getDataBuffer();
        final ProductData targetBufferWrappedIonosphericPhase = wrappedIonosphericPhaseTile.getDataBuffer();
        final ProductData targetBufferWrappedOriginalPhase = wrappedOriginalPhaseTile.getDataBuffer();
        final ProductData targetBufferWrappedCorrectedPhase = wrappedCorrectedPhaseTile.getDataBuffer();

        final TileIndex targetIndex = new TileIndex(ionosphericPhaseTile);

        for (int y = y0; y < yMax; y++) {
            targetIndex.calculateStride(y);
            final int yy = y - y0 + overlapY;
            for (int x = x0; x < xMax; x++) {
                final int targetIdx = targetIndex.getIndex(x);
                final int xx = x - x0 + overlapX;

                targetBufferIonosphericPhase.setElemDoubleAt(targetIdx, filteredIonosphericPhase[yy][xx]);
                targetBufferWrappedIonosphericPhase.setElemDoubleAt(targetIdx, wrap(filteredIonosphericPhase[yy][xx]));
                targetBufferWrappedOriginalPhase.setElemDoubleAt(targetIdx, wrap(originalPhase[yy][xx]));
                targetBufferWrappedCorrectedPhase.setElemDoubleAt(targetIdx, wrap(correctedPhase[yy][xx]));
            }
        }
    }

    /**
     * Applies a weighted Gaussian filter.
     */
    private static double[][] filter(final double[][] data, final double[][] weights, final int sigma) {

        // Weight data
        final int sizeY = data.length;
        final int sizeX = data[0].length;
        final double[][] weightedData = new double[sizeY][sizeX];
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                weightedData[y][x] = weights[y][x] * data[y][x];
            }
        }

        // Apply 2D Gaussian filter
        final double[][] filteredData = convolveWithGaussian2D(weightedData, sigma);
        final double[][] normalization = convolveWithGaussian2D(weights, sigma);

        // Normalize
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                filteredData[y][x] = filteredData[y][x] / normalization[y][x];
            }
        }

        return filteredData;
    }

    /**
     * Convolves data with a 2D Gaussian kernel.
     */
    private static double[][] convolveWithGaussian2D(final double[][] data, final int sigma) {

        // Define output array
        final int sizeY = data.length;
        final int sizeX = data[0].length;
        final double[][] result = new double[sizeY][sizeX];

        // Convolve along x axis
        for (int y = 0; y < sizeY; y++) {
            result[y] = convolveWithGaussian1D(data[y], sigma);
        }

        // Convolve along y axis
        final double[][] transposedResult = new double[sizeX][sizeY];
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                transposedResult[x][y] = result[y][x];
            }
        }
        for (int x = 0; x < sizeX; x++) {
            transposedResult[x] = convolveWithGaussian1D(transposedResult[x], sigma);
        }
        for (int y = 0; y < sizeY; y++) {
            for (int x = 0; x < sizeX; x++) {
                result[y][x] = transposedResult[x][y];
            }
        }

        return result;
    }

    /**
     * Convolves data with a 1D Gaussian kernel.
     */
    private static double[] convolveWithGaussian1D(final double[] data, final int sigma) {

        final int size = data.length;
        final double[] result = new double[size];
        final int kernel_size = computeGaussianKernelSize(sigma);
        final int halfWin = Math.floorDiv(kernel_size, 2);
        int correctionWin = 1;
        if (2 * halfWin == kernel_size) { // if `kernel_size` is even
            correctionWin = 0;
        }
        for (int y = halfWin; y < size - halfWin; y++) {
            double sum = 0;
            for (int r = -halfWin; r < halfWin + correctionWin; r++) {
                final double kernelValue = Math.exp(-(r * r) / (2 * Math.pow(sigma, 2)));
                sum += kernelValue * data[y + r];
            }
            result[y] = sum;
        }
        return result;
    }

    private static int computeGaussianKernelSize(final int sigma) {

        return sigma * 8 + 1;
    }

    /**
     * Wraps angle given in radians.
     */
    private static double wrap(final double phase) {

        return Math.atan2(Math.sin(phase), Math.cos(phase));
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

            super(IonosphericCorrectionOp.class);
        }
    }
}