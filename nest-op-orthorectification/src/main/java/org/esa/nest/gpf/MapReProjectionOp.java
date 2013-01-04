/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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

import org.esa.beam.framework.datamodel.CrsGeoCoding;
import org.esa.beam.framework.datamodel.ImageGeometry;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.gpf.operators.standard.reproject.Reproject;
import org.esa.beam.gpf.operators.standard.reproject.ReprojectionOp;
import org.esa.beam.jai.ImageManager;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.datamodel.AbstractMetadata;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.*;

/**

 */

@OperatorMetadata(alias="Reprojection", category = "Geometry", description="Applies a map projection")
public final class MapReProjectionOp extends ReprojectionOp {

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            label="Source Bands")
    private String[] sourceBandNames = null;

    @Parameter(description = "Whether to keep original or use custom resolution.",
               defaultValue = "true")
    protected boolean preserveResolution;

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.beam.framework.datamodel.Product} annotated with the
     * {@link org.esa.beam.framework.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.beam.framework.gpf.OperatorException
     *          If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {
            if(crs == null) {
                targetProduct = OperatorUtils.createDummyTargetProduct(new Product[] {sourceProduct});
                return;
            }

            validateCrsParameters();
            validateResamplingParameter();
            validateReferencingParameters();
            validateTargetGridParameters();

            /*
            * 1. Compute the target CRS
            */
            final CoordinateReferenceSystem targetCrs = createTargetCRS();
            /*
            * 2. Compute the target geometry
            */
            final ImageGeometry targetImageGeometry = createImageGeometry(targetCrs);

            /*
            * 3. Create the target product
            */
            final Rectangle targetRect = targetImageGeometry.getImageRect();
            targetProduct = new Product("projected_" + sourceProduct.getName(),
                    sourceProduct.getProductType(),
                    targetRect.width,
                    targetRect.height);
            targetProduct.setDescription(sourceProduct.getDescription());
            final Dimension tileSize = ImageManager.getPreferredTileSize(targetProduct);
            targetProduct.setPreferredTileSize(tileSize);
            /*
            * 4. Define some target properties
            */
            ProductUtils.copyMetadata(sourceProduct, targetProduct);
            ProductUtils.copyFlagCodings(sourceProduct, targetProduct);
            copyIndexCoding();
            try {
                targetProduct.setGeoCoding(new CrsGeoCoding(targetImageGeometry.getMapCrs(),
                        targetRect,
                        targetImageGeometry.getImage2MapTransform()));
            } catch (Exception e) {
                throw new OperatorException(e);
            }

            srcModel = ImageManager.getMultiLevelModel(sourceProduct.getBandAt(0));
            targetModel = ImageManager.createMultiLevelModel(targetProduct);
            reprojection = new Reproject(targetModel.getLevelCount());
            reprojectRasterDataNodes(sourceProduct.getBands());
            if (includeTiePointGrids) {
                reprojectRasterDataNodes(sourceProduct.getTiePointGrids());
            }

            ProductUtils.copyVectorData(sourceProduct, targetProduct);
            ProductUtils.copyMasks(sourceProduct, targetProduct);
            ProductUtils.copyOverlayMasks(sourceProduct, targetProduct);
            ProductUtils.copyRoiMasks(sourceProduct, targetProduct);
            targetProduct.setStartTime(sourceProduct.getStartTime());
            targetProduct.setEndTime(sourceProduct.getEndTime());
            targetProduct.setAutoGrouping(sourceProduct.getAutoGrouping());

            updateMetadata(targetProduct);

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void updateMetadata(Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        absRoot.setAttributeString(AbstractMetadata.map_projection, "map");
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.beam.framework.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator()
     * @see org.esa.beam.framework.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(MapReProjectionOp.class);
            super.setOperatorUI(MapReProjectionOpUI.class);
        }
    }
}