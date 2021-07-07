/*
 * Copyright (C) 2021 SkyWatch. https://www.skywatch.com
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
package org.esa.s1tbx.insar.gpf.coregistration;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.InputProductValidator;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.text.MessageFormat;

/**
 * The Merge operator.
 */
@OperatorMetadata(alias = "ProductGroupMerge",
        category = "Radar/Coregistration/Stack Tools",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2021 by SkyWatch Space Applications Inc.",
        description = "Merge several coregistered stacks.")
public class ProductGroupMergeOp extends Operator {

    @SourceProducts
    private Product[] sourceProduct;

    @TargetProduct(description = "The target product.")
    private Product targetProduct = null;

    private Product refProduct;

    private static final String PRODUCT_SUFFIX = "_Stack";

    @Override
    public void initialize() throws OperatorException {

        try {
            if (sourceProduct == null) {
                return;
            }

            if (sourceProduct.length < 2) {
                throw new OperatorException("Please select at least two source coregistered products");
            }

            refProduct = sourceProduct[0];

            for (final Product prod : sourceProduct) {
                final InputProductValidator validator = new InputProductValidator(prod);
                if(validator.isTOPSARProduct() && !validator.isDebursted()) {
                    throw new OperatorException("For S1 TOPS SLC products, TOPS Coregistration should be used");
                }

                if (prod.getSceneGeoCoding() == null) {
                    throw new OperatorException(
                            MessageFormat.format("Product ''{0}'' has no geo-coding", prod.getName()));
                }
                if(refProduct != prod) {
                    if(!refProduct.isCompatibleProduct(prod, 0.0001f)) {
                        throw new OperatorException("Stack product "+prod.getName() +" is not compatible with " + refProduct.getName());
                    }
                }
            }

            targetProduct = new Product(OperatorUtils.createProductName(refProduct.getName(), ""),
                    refProduct.getProductType(),
                    refProduct.getSceneRasterWidth(),
                    refProduct.getSceneRasterHeight());

            ProductUtils.copyProductNodes(refProduct, targetProduct);

            for (final Product prod : sourceProduct) {
                for (Band band : prod.getBands()) {
                    if(targetProduct.getBand(band.getName()) == null) {
                        if (band instanceof VirtualBand) {
                            ProductUtils.copyVirtualBand(targetProduct, (VirtualBand) band, band.getName());
                        } else {
                            ProductUtils.copyBand(band.getName(), prod, band.getName(), targetProduct, true);
                        }
                    }
                }
            }

            // copy secondary abstracted metadata
            copySecondaryMetadata();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private void copySecondaryMetadata() {
        final MetadataElement targetSecondaryMetadataRoot = AbstractMetadata.getSlaveMetadata(targetProduct.getMetadataRoot());
        for (Product prod : sourceProduct) {
            if (prod != refProduct) {
                final MetadataElement secondaryMetadataRoot = AbstractMetadata.getSlaveMetadata(prod.getMetadataRoot());
                for(MetadataElement secMetadata : secondaryMetadataRoot.getElements()) {
                    if(!targetSecondaryMetadataRoot.containsElement(secMetadata.getName())) {
                        MetadataElement newElem = new MetadataElement(secMetadata.getName());
                        targetSecondaryMetadataRoot.addElement(newElem);
                        ProductUtils.copyMetadata(secMetadata, newElem);
                    }
                }
            }
        }
    }

    /**
     * Operator SPI.
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(ProductGroupMergeOp.class);
        }
    }
}
