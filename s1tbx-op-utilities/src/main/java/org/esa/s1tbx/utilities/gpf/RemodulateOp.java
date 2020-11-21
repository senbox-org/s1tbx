/*
 * Copyright (C) 2020 by SENSAR B.V. http://www.sensar.nl
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
package org.esa.s1tbx.utilities.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.apache.commons.math3.util.FastMath;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.StackUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.Rectangle;
import java.util.HashMap;
import java.util.Map;

/**
 * Remodulation/reramping of SLC data
 */
@OperatorMetadata(alias = "Remodulate",
        category = "Radar/SAR Utilities/Resampling",
        authors = "Carlos Hernandez, Esteban Aguilera, Reinier Oost, David A. Monge",
        version = "1.0",
        copyright = "Copyright (C) 2020 by SENSAR",
        description = "Remodulation and reramping of SLC data")
public class RemodulateOp extends Operator {

    @SourceProduct
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    private final Map<Band, Band> sourceRasterMap = new HashMap<>(10);
    private final Map<Band, Band> complexSrcMap = new HashMap<>(10);
    private final Map<Band, Band> complexTgtMap = new HashMap<>(10);
    private final Map<Band, Band> demodPhaseMap = new HashMap<>(10);

    private static final String PRODUCT_SUFFIX = "_Remod";
    private static final String DEMOD_PHASE_PREFIX = "DemodPhase";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public RemodulateOp() {

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
            createTargetProduct();
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                                    sourceProduct.getProductType(),
                                    sourceProduct.getSceneRasterWidth(),
                                    sourceProduct.getSceneRasterHeight());

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        // Define source bands
        Band sourceBandI = null;
        Band sourceBandQ = null;

        // Master
        final String[] masterBandNames = StackUtils.getMasterBandNames(sourceProduct);
        for (String bandName : masterBandNames) {
            if (bandName.contains("i_")) {
                sourceBandI = sourceProduct.getBand(bandName);
            } else {
                sourceBandQ = sourceProduct.getBand(bandName);
            }
        }
        // Check if master bands are null (this could happen if dropped by the BandSelect operator)
        if (sourceBandI != null && sourceBandQ != null) {
            createTargetBands(sourceBandI, sourceBandQ);
        }

        // Slaves
        final String[] slaveProductNames = StackUtils.getSlaveProductNames(sourceProduct);
        for (String slaveProductName : slaveProductNames) {
            final String[] slvBandNames = StackUtils.getSlaveBandNames(sourceProduct, slaveProductName);
            for (String bandName : slvBandNames) {
                if (bandName.contains("i_")) {
                    sourceBandI = sourceProduct.getBand(bandName);
                } else {
                    sourceBandQ = sourceProduct.getBand(bandName);
                }
            }
            createTargetBands(sourceBandI, sourceBandQ);
        }
    }

    private void createTargetBands(final Band sourceBandI, final Band sourceBandQ) {

        // Define target bands
        final Band targetBandI;
        final Band targetBandQ;

        // Get (demodulation phase) band from sourceProduct
        final String demodBandName = DEMOD_PHASE_PREFIX + StackUtils.getBandSuffix(sourceBandQ.getName());
        final Band sourceDemodPhaseBand = sourceProduct.getBand(demodBandName);

        if (sourceDemodPhaseBand == null) { // if band has not been previously demodulated
            targetBandI = ProductUtils.copyBand(sourceBandI.getName(), sourceProduct, targetProduct, false);
            targetBandI.setSourceImage(sourceBandI.getSourceImage());
            targetBandQ = ProductUtils.copyBand(sourceBandQ.getName(), sourceProduct, targetProduct, false);
            targetBandQ.setSourceImage(sourceBandQ.getSourceImage());
        } else {
            // Add bands to targetProduct
            targetBandI = targetProduct.addBand(sourceBandI.getName(), ProductData.TYPE_FLOAT32);
            ProductUtils.copyRasterDataNodeProperties(sourceBandI, targetBandI);
            targetBandQ = targetProduct.addBand(sourceBandQ.getName(), ProductData.TYPE_FLOAT32);
            ProductUtils.copyRasterDataNodeProperties(sourceBandQ, targetBandQ);

            // Store source and target bands in HashMaps
            sourceRasterMap.put(targetBandI, sourceBandI); // (target I: source I) band pairs
            sourceRasterMap.put(targetBandQ, sourceBandQ); // (target Q: source Q) band pairs
            complexSrcMap.put(sourceBandI, sourceBandQ); // (source I: source Q) band pairs
            complexTgtMap.put(targetBandI, targetBandQ); // (target I: target Q) band pairs
            demodPhaseMap.put(sourceBandI, sourceDemodPhaseBand); // (source I: source demod phase band) pairs
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

        for (Band targetBandI : complexTgtMap.keySet()) { // for each SLC

            // Get source and target bands
            final Band sourceBandI = sourceRasterMap.get(targetBandI);
            final Band sourceBandQ = complexSrcMap.get(sourceBandI);
            final Band targetBandQ = complexTgtMap.get(targetBandI);
            final Band sourceDemodPhaseBand = demodPhaseMap.get(sourceBandI);

            // Get source tiles
            final Tile sourceTileI = getSourceTile(sourceBandI, targetRectangle);
            final Tile sourceTileQ = getSourceTile(sourceBandQ, targetRectangle);
            final Tile sourceDemodPhaseTile = getSourceTile(sourceDemodPhaseBand, targetRectangle);

            // Get target tiles
            final Tile targetTileI = targetTileMap.get(targetBandI);
            final Tile targetTileQ = targetTileMap.get(targetBandQ);

            // Remodulate
            remodulate(sourceTileI, sourceTileQ, targetTileI, targetTileQ,
                       sourceDemodPhaseTile, targetRectangle);
        }
    }

    private void remodulate(final Tile sourceTileI, final Tile sourceTileQ,
                            final Tile targetTileI, final Tile targetTileQ,
                            final Tile sourceDemodPhaseTile, final Rectangle rectangle) {

        final int x0 = rectangle.x;
        final int y0 = rectangle.y;
        final int xMax = x0 + rectangle.width;
        final int yMax = y0 + rectangle.height;

        final ProductData sourceBufferI = sourceTileI.getDataBuffer();
        final ProductData sourceBufferQ = sourceTileQ.getDataBuffer();
        final ProductData targetBufferI = targetTileI.getDataBuffer();
        final ProductData targetBufferQ = targetTileQ.getDataBuffer();
        final ProductData sourceDemodPhaseBuffer = sourceDemodPhaseTile.getDataBuffer();

        final TileIndex sourceIndex = new TileIndex(sourceTileI);
        final TileIndex targetIndex = new TileIndex(targetTileI);

        for (int y = y0; y < yMax; y++) {
            sourceIndex.calculateStride(y);
            targetIndex.calculateStride(y);
            for (int x = x0; x < xMax; x++) {
                final int sourceIdx = sourceIndex.getIndex(x);
                final int targetIdx = targetIndex.getIndex(x);

                // Get value of real and imaginary bands
                final double valueI = sourceBufferI.getElemDoubleAt(sourceIdx);
                final double valueQ = sourceBufferQ.getElemDoubleAt(sourceIdx);
                final double demodPhase = sourceDemodPhaseBuffer.getElemDoubleAt(sourceIdx);

                // Get cos and sin of demodulation
                final double cosPhase = FastMath.cos(demodPhase);
                final double sinPhase = FastMath.sin(demodPhase);

                // Calculate remodulated real and imaginary parts
                final double remodI = valueI * cosPhase + valueQ * sinPhase;
                final double remodQ = -valueI * sinPhase + valueQ * cosPhase;

                // Write to product
                targetBufferI.setElemDoubleAt(targetIdx, remodI);
                targetBufferQ.setElemDoubleAt(targetIdx, remodQ);
            }
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

            super(RemodulateOp.class);
        }
    }
}