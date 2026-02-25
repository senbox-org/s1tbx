/*
 * Copyright (C) 2026 by Microwave Remote Sensing Lab, IITBombay http://www.mrslab.in
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

package org.csa.rstb.polarimetric.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.csa.rstb.polarimetric.gpf.decompositions.EigenDecomposition;
import org.csa.rstb.polarimetric.gpf.support.DualPolProcessor;
import org.esa.s1tbx.commons.polsar.PolBandUtils;
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
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.*;
import java.util.Arrays;
import java.util.Map;

/**
 * Generate Radar Surface Index for dual-pol product
 * Note: 1. This operator computes duak-pol radar surface index (DpRSI).
 *       2. The input to the operator is assumed to be C2 product, i.e. product with 4 bands: c11, c12_real, c12_imag
 *          and c22.
 *       3. The operator provides window size as an input parameter for averaging the input covariance matrix C2.
 * Reference:
 * [1] Verma, A., Bhattacharya, A., Dey, S., Lopez-Martinez, C., and Gamba, P., 2024.
 *     Scattering Power Componenets from Dual-pol Sentinel-1 SLC and GRD SAR Data. 
 *     ISPRS Journal of Photogrammetry and Remote Sensing, 212, p.289-305.

 * [1] Verma, A., Bhattacharya, A., Dey, S., Lopez-Martinez, C., and Gamba, P., 2024.
 *     Radar Surface Scattering Index from Dual-pol Sentinel-1 SLC and GRD SAR Data. 
 *     International Conference on Machine Intelligence for GeoAnalytics and Remote Sensing (MIGARS).
 */

@OperatorMetadata(alias = "Dual-Radar-Surface-Index",
        category = "Radar/Polarimetric/Dual Polarimetry",
        authors = "Abhinav Verma et al.",
        version = "1.0",
        copyright = "Copyright (C) 2026 by Microwave Remote Sensing Lab, IITBombay",
        description = "Dual-pol Radar Surface Index")
public final class DPRSIOp extends Operator implements DualPolProcessor {

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(valueSet = {"3", "5", "7", "9", "11", "13", "15", "17", "19"}, defaultValue = "5", label = "Window Size")
    private int windowSize = 5;

    private int halfWindowSize = 0;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    final double NESZ = -16.0;

    // Cache valid region boundaries
    private int startX = 0;
    private int startY = 0;
    private int endX = 0;
    private int endY = 0;

    private double percentile95_g1 = 0.0;
    private double percentile5_g1 = 0.0;

    private PolBandUtils.MATRIX sourceProductType = null;
    private PolBandUtils.PolSourceBand[] srcBandList;

    private final static String DUAL_POL_RSI = "DpRSI";

    @Override
    public void initialize() throws OperatorException {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSARProduct();

            sourceProductType = PolBandUtils.getSourceProductType(sourceProduct);
            if (sourceProductType != PolBandUtils.MATRIX.C2) {
                throw new OperatorException("Dual-pol C2 matrix source product is expected.");
            }

            srcBandList = PolBandUtils.getSourceBands(sourceProduct, sourceProductType);
            halfWindowSize = windowSize / 2;
            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            // Region boundaries
            startX = halfWindowSize;
            startY = halfWindowSize;
            endX = sourceImageWidth - halfWindowSize;
            endY = sourceImageHeight - halfWindowSize;

            // Compute 5th and 95th percentiles for Stokes elements
            computeStokesScene();

            createTargetProduct();
            updateTargetProductMetadata();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Compute 5th and 95th percentiles for Stokes parameters (g1, g2, and g3).
     * The statistics has to be computed for the entire scene.
     */
    private void computeStokesScene() {

        try {
            if (startX >= endX || startY >= endY) {
                throw new OperatorException("Image too small for window size " + windowSize);
            }

            int validPixels = (endX - startX) * (endY - startY);
            float[] g1Array = new float[validPixels];

            int count = 0;
            final int chunkHeight = 100;

            // Threshold for slope mask

            for (final PolBandUtils.PolSourceBand bandList : srcBandList) {
                final double[][] Cr = new double[2][2];
                final double[][] Ci = new double[2][2];

                for (int y0 = startY; y0 < endY; y0 += chunkHeight) {
                    int chunkH = Math.min(chunkHeight, endY - y0);

                    int tileY0 = y0 - halfWindowSize;
                    int tileY1 = Math.min(sourceImageHeight - 1, y0 + chunkH - 1 + halfWindowSize);
                    int tileHeight = tileY1 - tileY0 + 1;

                    final Rectangle chunkRect = new Rectangle(0, tileY0, sourceImageWidth, tileHeight);
                    final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
                    final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];

                    for (int j = 0; j < bandList.srcBands.length; j++) {
                        sourceTiles[j] = getSourceTile(bandList.srcBands[j], chunkRect);
                        dataBuffers[j] = sourceTiles[j].getDataBuffer();
                    }

                    final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

                    // Processing each row in chunk
                    for (int y = y0; y < y0 + chunkH; y++) {
                        srcIndex.calculateStride(y - tileY0);

                        for (int x = startX; x < endX; x++) {
                            getMeanCovarianceMatrixC2(x, y, halfWindowSize, halfWindowSize,
                                    sourceImageWidth, sourceImageHeight,
                                    sourceProductType, sourceTiles, dataBuffers, Cr, Ci);

                            // Compute Stokes parameters
                            double g1 = Cr[0][0] - Cr[1][1];

                            // Collect and store final values of Stokes parameters
                            if (Double.isFinite(g1)) {
                                g1Array[count] = (float) Math.abs(g1);
                                count++;
                            }
                        }
                    }
                }
                break;
            }

            // Sort arrays for percentile calculation
            if (count > 100000) {
                Arrays.parallelSort(g1Array, 0, count);
            } else {
                Arrays.sort(g1Array, 0, count);
            }

            // Compute 5th and 95th percentiles
            percentile5_g1 = ComputePercentile(g1Array, 5.0);
            percentile95_g1 = ComputePercentile(g1Array, 95.0);

        } catch (Exception e) {
            throw new OperatorException("Error computing Stokes parameter percentiles: " + e.getMessage(), e);
        }
    }

    /**
     * This function computes the 5th and 95th percentile.
     */
    private double ComputePercentile(float[] sortedData, double p) {
        if (sortedData.length == 0) {
            throw new OperatorException("No data for percentile computation");
        }

        if (sortedData.length == 1) {
            return sortedData[0];
        }

        double position = (p / 100.0) * (sortedData.length - 1);

        int idx = (int) Math.floor(position);
        double frac = position - idx;

        if (idx >= sortedData.length - 1) {
            return sortedData[sortedData.length - 1];
        }

        if (idx < 0) {
            return sortedData[0];
        }

        return sortedData[idx] * (1.0 - frac) + sortedData[idx + 1] * frac;
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        addSelectedBands();

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    /**
     * Add bands to the target product.
     *
     * @throws OperatorException The exception.
     */
    private void addSelectedBands() throws OperatorException {

        // If more indices are computed, add their names here
        final String[] targetBandNames = new String[]{DUAL_POL_RSI};

        for (PolBandUtils.PolSourceBand bandList : srcBandList) {
            final Band[] targetBands = OperatorUtils.addBands(targetProduct, targetBandNames, bandList.suffix);
            bandList.addTargetBands(targetBands);
        }
    }

    private void updateTargetProductMetadata() throws Exception {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absRoot != null) {
            absRoot.setAttributeInt(AbstractMetadata.polsarData, 1);

            // Add metadata for traceability
            final MetadataElement procElem = new MetadataElement("DpRSI_Parameters");
            procElem.setAttributeDouble("percentile5_g1", percentile5_g1);
            procElem.setAttributeDouble("percentile95_g1", percentile95_g1);
            absRoot.addElement(procElem);
        }
        PolBandUtils.saveNewBandNames(targetProduct, srcBandList);
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.snap.core.gpf.OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        final int x0 = targetRectangle.x;
        final int y0 = targetRectangle.y;
        final int w = targetRectangle.width;
        final int h = targetRectangle.height;
        final int maxY = y0 + h;
        final int maxX = x0 + w;
        //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

        final double[][] Cr = new double[2][2]; // real part of covariance matrix
        final double[][] Ci = new double[2][2]; // imaginary part of covariance matrix

        final double p5_g1 = percentile5_g1;
        final double p95_g1 = percentile95_g1;

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {
            try {
                final TileData[] tileDataList = new TileData[bandList.targetBands.length];
                int i = 0;
                for (Band targetBand : bandList.targetBands) {
                    final Tile targetTile = targetTiles.get(targetBand);
                    tileDataList[i++] = new TileData(targetTile, targetBand.getName());
                }
                final TileIndex tgtIndex = new TileIndex(tileDataList[0].tile);

                final Tile[] sourceTiles = new Tile[bandList.srcBands.length];
                final ProductData[] dataBuffers = new ProductData[bandList.srcBands.length];
                final Rectangle sourceRectangle = getSourceRectangle(x0, y0, w, h);

                for (int j = 0; j < bandList.srcBands.length; j++) {
                    final Band srcBand = bandList.srcBands[j];
                    sourceTiles[j] = getSourceTile(srcBand, sourceRectangle);
                    dataBuffers[j] = sourceTiles[j].getDataBuffer();
                }
                final TileIndex srcIndex = new TileIndex(sourceTiles[0]);

                for (int y = y0; y < maxY; ++y) {
                    tgtIndex.calculateStride(y);
                    srcIndex.calculateStride(y);

                    for (int x = x0; x < maxX; ++x) {
                        final int tgtIdx = tgtIndex.getIndex(x);
                        final int srcIdx = srcIndex.getIndex(x);

                        // If input covariance matrix has been average then don't need to average it again.
                        // Comment out the following two lines and use the line below them, i.e. getCovarianceMatrixC2
                        getMeanCovarianceMatrixC2(x, y, halfWindowSize, halfWindowSize, sourceImageWidth,
                                sourceImageHeight, sourceProductType, sourceTiles, dataBuffers, Cr, Ci);

                        // Computing Stokes

                        double c11_dB = 10.0 * Math.log10(Cr[0][0]); //C11 in decibel
                        double g1 = Cr[0][0] - Cr[1][1];      // Linear H or V component

                        // Data cleaning (removing outliers)
                        g1 = Math.abs(g1);
                        double g1_cln = Math.min(Math.max(g1, p5_g1), p95_g1);

                        // NORMALIZE Stokes parameters using 95th percentiles (MANDATORY)
                        double g1_norm = g1_cln / p95_g1;

                        // Computing Entropy
                        final double[][] EigenVectRe = new double[2][2];
                        final double[][] EigenVectIm = new double[2][2];
                        final double[] EigenVal = new double[2];
                        EigenDecomposition.eigenDecomposition(2, Cr, Ci, EigenVectRe, EigenVectIm, EigenVal);

                        final double prob1 = EigenVal[0] / (EigenVal[0] + EigenVal[1]);
                        final double prob2 = EigenVal[1] / (EigenVal[0] + EigenVal[1]);

                        double ent = -prob1 * (Math.log(prob1) / Math.log(2)) - prob2 * (Math.log(prob2) / Math.log(2));

                        final double dpRSI_con1 = Math.sqrt(Math.max(0.0, 1.0 - Math.pow(g1_norm, 2)));

                        double dpRSI = (c11_dB > NESZ) ?
                                (1.0 - ent) * dpRSI_con1 :  // Valid pixels
                                dpRSI_con1;                   // Noise pixels

                        for (final TileData tileData : tileDataList) {
                            // Can add more indices here
                            if (tileData.bandName.contains(DUAL_POL_RSI)) {
                                tileData.dataBuffer.setElemFloatAt(tgtIdx, (float) dpRSI);
                            }
                        }
                    }
                }

            } catch (Throwable e) {
                OperatorUtils.catchOperatorException(getId(), e);
            }
        }
    }

    private Rectangle getSourceRectangle(final int tx0, final int ty0, final int tw, final int th) {
        final int x0 = Math.max(0, tx0 - halfWindowSize);
        final int y0 = Math.max(0, ty0 - halfWindowSize);
        final int xMax = Math.min(tx0 + tw - 1 + halfWindowSize, sourceImageWidth - 1);
        final int yMax = Math.min(ty0 + th - 1 + halfWindowSize, sourceImageHeight - 1);
        final int w = xMax - x0 + 1;
        final int h = yMax - y0 + 1;
        return new Rectangle(x0, y0, w, h);
    }

    private static class TileData {
        final Tile tile;
        final ProductData dataBuffer;
        final String bandName;

        public TileData(final Tile tile, final String bandName) {
            this.tile = tile;
            this.dataBuffer = tile.getDataBuffer();
            this.bandName = bandName;
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
            super(DPRSIOp.class);
        }
    }
}
