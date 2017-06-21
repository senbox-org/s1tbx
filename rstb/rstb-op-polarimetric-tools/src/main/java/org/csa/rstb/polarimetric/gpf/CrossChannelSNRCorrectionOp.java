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
package org.csa.rstb.polarimetric.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.io.PolBandUtils;
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

import java.awt.Rectangle;
import java.util.Map;

/**
 * Perform cross-channel SNR correction using HV/VH coherence.
 */

@OperatorMetadata(alias = "Cross-Channel-SNR-Correction",
        category = "Radar/Polarimetric",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2016 by Array Systems Computing Inc.",
        description = "Compute general polarimetric parameters")
public final class CrossChannelSNRCorrectionOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The sliding window size", interval = "[1, 100]", defaultValue = "5",
            label = "Window Size")
    private int windowSize = 5;

    private int halfWindowSize = 0;

    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;

    private PolBandUtils.MATRIX sourceProductType = null;
    private PolBandUtils.PolSourceBand[] srcBandList;
    private Band hhBand = null, hvBand = null, vvBand = null, vhBand = null;

    @Override
    public void initialize() throws OperatorException {

        try {
            final InputProductValidator validator = new InputProductValidator(sourceProduct);
            validator.checkIfSARProduct();

            sourceProductType = PolBandUtils.getSourceProductType(sourceProduct);
            if (sourceProductType != PolBandUtils.MATRIX.FULL) {
                throw new OperatorException("Quad-pol source product is expected.");
            }

            srcBandList = PolBandUtils.getSourceBands(sourceProduct, sourceProductType);
            halfWindowSize = windowSize / 2;
            sourceImageWidth = sourceProduct.getSceneRasterWidth();
            sourceImageHeight = sourceProduct.getSceneRasterHeight();

            createTargetProduct();

            updateTargetProductMetadata();
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

        addSelectedBands();

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    /**
     * Add bands to the target product.
     *
     * @throws OperatorException The exception.
     */
    private void addSelectedBands() throws OperatorException {

        for (PolBandUtils.PolSourceBand bandList : srcBandList) {

            int k = 0;
            final Band[] targetBands = new Band[4];
            for (int i = 0; i < bandList.srcBands.length; ++i) {
                final String bandName = bandList.srcBands[i].getName();
                if (bandName.toLowerCase().contains("_hv") || bandName.toLowerCase().contains("_vh")) {
                    final Band targetBand = new Band(
                            bandName, ProductData.TYPE_FLOAT32, sourceImageWidth, sourceImageHeight);
                    targetBand.setUnit(bandList.srcBands[i].getUnit());
                    targetProduct.addBand(targetBand);
                    targetBands[k++] = targetBand;
                } else {
                    ProductUtils.copyBand(bandName, sourceProduct, targetProduct, true);
                }
            }
            bandList.addTargetBands(targetBands);
        }
    }

    /**
     * Update metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
        if (absRoot != null) {
            absRoot.setAttributeInt(AbstractMetadata.polsarData, 1);
        }
        PolBandUtils.saveNewBandNames(targetProduct, srcBandList);
    }

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

        final double[][] Cr = new double[4][4];
        final double[][] Ci = new double[4][4];

        for (final PolBandUtils.PolSourceBand bandList : srcBandList) {
            try {
                // save tile data for quicker access
                final TileData[] tileDataList = new TileData[bandList.targetBands.length];
                int i = 0;
                for (Band targetBand : bandList.targetBands) {
                    final Tile targetTile = targetTiles.get(targetBand);
                    tileDataList[i++] = new TileData(targetTile, targetBand.getName());
                }
                final TileIndex trgIndex = new TileIndex(tileDataList[0].tile);

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
                    trgIndex.calculateStride(y);
                    srcIndex.calculateStride(y);
                    for (int x = x0; x < maxX; ++x) {
                        final int tgtIdx = trgIndex.getIndex(x);

                        PolOpUtils.getMeanCovarianceMatrixC4(x, y, halfWindowSize, halfWindowSize,
                                sourceProductType, sourceTiles, dataBuffers, Cr, Ci);

                        final double gamma = Math.sqrt((Cr[1][2]*Cr[1][2] + Ci[1][2]*Ci[1][2]) / (Cr[1][1]*Cr[2][2]));

                        for (final TileData tileData : tileDataList) {
                            double v = 0.0;
                            if (tileData.bandName.contains("i_HV")) {
                                v = dataBuffers[2].getElemDoubleAt(srcIndex.getIndex(x));
                            } else if (tileData.bandName.contains("q_HV")) {
                                v = dataBuffers[3].getElemDoubleAt(srcIndex.getIndex(x));
                            } else if (tileData.bandName.contains("i_VH")) {
                                v = dataBuffers[4].getElemDoubleAt(srcIndex.getIndex(x));
                            } else if (tileData.bandName.contains("q_VH")) {
                                v = dataBuffers[5].getElemDoubleAt(srcIndex.getIndex(x));
                            }
                            tileData.dataBuffer.setElemFloatAt(tgtIdx, (float)(gamma*v));
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
            super(CrossChannelSNRCorrectionOp.class);
        }
    }
}