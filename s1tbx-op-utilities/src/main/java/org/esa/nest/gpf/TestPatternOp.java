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
package org.esa.nest.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.eo.Constants;
import org.esa.snap.gpf.InputProductValidator;
import org.esa.snap.gpf.OperatorUtils;
import org.esa.snap.gpf.TileIndex;

import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Creates a new product with only selected bands
 */

@OperatorMetadata(alias = "TestPattern",
        category = "Utilities",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "For testing only")
public final class TestPatternOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            targetProduct = new Product(sourceProduct.getName(),
                    sourceProduct.getProductType(),
                    sourceProduct.getSceneRasterWidth(),
                    sourceProduct.getSceneRasterHeight());

            final Band[] selectedBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames, false);

            ProductUtils.copyProductNodes(sourceProduct, targetProduct);

            for (Band srcBand : selectedBands) {
                if (srcBand instanceof VirtualBand) {
                    continue;
                } else {
                    //ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, false);
                    final Band targetBand = new Band(srcBand.getName(), ProductData.TYPE_FLOAT32, srcBand.getSceneRasterWidth(), srcBand.getSceneRasterHeight());
                    targetProduct.addBand(targetBand);
                    //ProductUtils.copyRasterDataNodeProperties(srcBand, targetBand);
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.beam.framework.gpf.OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            final Rectangle targetTileRectangle = targetTile.getRectangle();
            final int x0 = targetTileRectangle.x;
            final int y0 = targetTileRectangle.y;
            final int w = targetTileRectangle.width;
            final int h = targetTileRectangle.height;
            //System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h + ", target band = " + targetBand.getName());

            final String targetBandName = targetBand.getName();
            final Band sourceBand = sourceProduct.getBand(targetBandName);
            final Tile sourceRaster = getSourceTile(sourceBand, targetTileRectangle);
            final ProductData srcData = sourceRaster.getDataBuffer();

            final ProductData trgData = targetTile.getDataBuffer();
            final TileIndex srcIndex = new TileIndex(sourceRaster);
            final TileIndex trgIndex = new TileIndex(targetTile);
            final int maxY = y0 + h;
            final int maxX = x0 + w;

            double dn;
            int srcIdx, trgIdx;
            for (int y = y0; y < maxY; ++y) {
                srcIndex.calculateStride(y);
                trgIndex.calculateStride(y);

                for (int x = x0; x < maxX; ++x) {
                    srcIdx = srcIndex.getIndex(x);
                    trgIdx = trgIndex.getIndex(x);

                    dn = srcData.getElemDoubleAt(srcIdx);
                    trgData.setElemDoubleAt(trgIdx, y);//dn);
                }
            }
        } catch (Exception e) {
            OperatorUtils.catchOperatorException(this.getId(), e);
        }
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(TestPatternOp.class);
        }
    }
}