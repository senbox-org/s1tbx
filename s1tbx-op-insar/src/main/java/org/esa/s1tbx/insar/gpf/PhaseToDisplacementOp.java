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
package org.esa.s1tbx.insar.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.framework.datamodel.*;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.Parameter;
import org.esa.snap.framework.gpf.annotations.SourceProduct;
import org.esa.snap.framework.gpf.annotations.TargetProduct;
import org.esa.snap.util.ProductUtils;
import org.esa.s1tbx.insar.gpf.geometric.SARUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.datamodel.Unit;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.TileIndex;

import java.awt.*;
import java.util.Map;


@OperatorMetadata(alias = "PhaseToDisplacement",
        category = "SAR Processing/Interferometric/Products",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Phase To Displacement Conversion")
public final class PhaseToDisplacementOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;

    private double wavelength = 0.0;
    private Band unwrappedPhaseBand;

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
            // input should be flat-Earth-phase and topo-phase removed interferogram
            if (OperatorUtils.isMapProjected(sourceProduct)) {
                throw new OperatorException("Source product already map projected");
            }

            getMetadata();

            getSourceImageDimension();

            createTargetProduct();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Retrieve required data from Abstracted Metadata
     *
     * @throws Exception if metadata not found
     */
    private void getMetadata() throws Exception {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
        wavelength = SARUtils.getRadarFrequency(absRoot);
    }

    /**
     * Get source image width and height.
     */
    private void getSourceImageDimension() {
        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        targetProduct = new Product(sourceProduct.getName(),
                sourceProduct.getProductType(),
                sourceImageWidth,
                sourceImageHeight);

        addSelectedBands();

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);
    }

    /**
     * Add user selected bands to target product.
     */
    private void addSelectedBands() {

//        final Band[] sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, false);
        final Band[] sourceBands = sourceProduct.getBands();
        boolean validProduct = false;
        for (Band band : sourceBands) {
            if (band.getName().startsWith("Unw")) {
                validProduct = true;
                unwrappedPhaseBand = band;
                break;
            }
        }

        if (!validProduct) {
            throw new OperatorException("Cannot find UnwrappedPhase band in the source product.");
        }

        final Band targetBand = new Band("displacement", ProductData.TYPE_FLOAT32, sourceImageWidth, sourceImageHeight);
        targetBand.setUnit(Unit.METERS);
        targetProduct.addBand(targetBand);
    }

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        try {
            final Band sourceBand = unwrappedPhaseBand;
            final Tile sourceTile = getSourceTile(sourceBand, targetRectangle);
            final ProductData sourceData = sourceTile.getDataBuffer();

            final Band targetBand = targetProduct.getBand("displacement");
            final Tile targetTile = targetTiles.get(targetBand);
            final ProductData targetData = targetTile.getDataBuffer();
            final TileIndex srcIndex = new TileIndex(sourceTile);
            final TileIndex trgIndex = new TileIndex(targetTile);

            final int x0 = targetRectangle.x;
            final int y0 = targetRectangle.y;
            final int w = targetRectangle.width;
            final int h = targetRectangle.height;
            // System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h);

            final double wavelengthOver4PI = wavelength / (4.0*Math.PI);
            for (int y = y0; y < y0 + h; y++) {
                srcIndex.calculateStride(y);
                trgIndex.calculateStride(y);
                for (int x = x0; x < x0 + w; x++) {

                    final double phase = sourceData.getElemDoubleAt(srcIndex.getIndex(x));
                    final double displacement = wavelengthOver4PI*phase;
                    targetData.setElemDoubleAt(trgIndex.getIndex(x), displacement);
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
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
            super(PhaseToDisplacementOp.class);
        }
    }
}
