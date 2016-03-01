/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.esa.snap.engine_utilities.gpf.TileIndex;

import java.awt.Rectangle;
import java.util.Map;

/**
 * Creates a new product with only selected bands
 */

@OperatorMetadata(alias = "TestPattern",
        category = "Tools",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2015 by Array Systems Computing Inc.",
        description = "For testing only", internal = true)
public final class TestPatternOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

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
                    final Band targetBand = new Band(srcBand.getName(), ProductData.TYPE_FLOAT32, srcBand.getRasterWidth(), srcBand.getRasterHeight());
                    targetProduct.addBand(targetBand);
                    ProductUtils.copyRasterDataNodeProperties(srcBand, targetBand);
                }
            }

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

//    @Override
//    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
//        computePattern(targetBand, targetTile);
//    }

    @Override
    public synchronized void computeTileStack(Map<Band, Tile> targetTileMap, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {
        for(Band targetBand : targetProduct.getBands()) {
            final Tile targetTile = targetTileMap.get(targetBand);
            //computePattern(targetBand, targetTile);
        }
    }

    private void computePattern(Band targetBand, Tile targetTile) {
        try {
            int minTileX = targetBand.getRasterWidth()-targetTile.getRectangle().x-1;
            int minTileY = targetBand.getRasterHeight()-targetTile.getRectangle().y-1;
            Rectangle targetTileRectangle = new Rectangle(targetTile.getRectangle().x, targetTile.getRectangle().y,
                                                          Math.min(minTileX, targetTile.getRectangle().width),
                                                          Math.min(minTileY, targetTile.getRectangle().height));
            final int x0 = targetTileRectangle.x;
            final int y0 = targetTileRectangle.y;
            final int w = targetTileRectangle.width;
            final int h = targetTileRectangle.height;

            if(x0 >= targetBand.getRasterWidth() || y0 >= targetBand.getRasterHeight() || w <= 0 || h <= 0) {
                return;
            }
            if(x0 >= 11880) {
                return;
            }

            System.out.println("x0 = " + x0 + ", y0 = " + y0 + ", w = " + w + ", h = " + h + ", target band = " + targetBand.getName());

            final Band sourceBand = sourceProduct.getBand(targetBand.getName());
            Tile sourceRaster = null;
            try {
                sourceRaster = getSourceTile(sourceBand, targetTileRectangle);
            } catch (Throwable t) {
                t.printStackTrace();
                sourceRaster = getSourceTile(sourceBand, targetTileRectangle);
            }
            final ProductData srcData = sourceRaster.getDataBuffer();

            final ProductData trgData = targetTile.getDataBuffer();
            final TileIndex srcIndex = new TileIndex(sourceRaster);
            final TileIndex trgIndex = new TileIndex(targetTile);
            final int maxY = y0 + h;
            final int maxX = x0 + w;

            int cnt = 0;
            double dn;
            int srcIdx, trgIdx;
            for (int y = y0; y < maxY; ++y) {
                srcIndex.calculateStride(y);
                trgIndex.calculateStride(y);

                for (int x = x0; x < maxX; ++x) {
                    srcIdx = srcIndex.getIndex(x);
                    trgIdx = trgIndex.getIndex(x);

                    dn = srcData.getElemDoubleAt(srcIdx);
                    trgData.setElemDoubleAt(trgIdx, dn);
                }
            }
        } catch (Exception e) {
            OperatorUtils.catchOperatorException(this.getId(), e);
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
            super(TestPatternOp.class);
        }
    }
}
