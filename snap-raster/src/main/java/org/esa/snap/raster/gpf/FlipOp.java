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
package org.esa.snap.raster.gpf;

import org.esa.snap.core.dataio.ProductFlipper;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.common.support.ProductFlipperExt;
import org.esa.snap.core.util.ProductUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * Creates a new product with only selected bands
 */

@OperatorMetadata(alias = "Flip",
        category = "Raster",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2015 by Array Systems Computing Inc.",
        description = "flips a product horizontal/vertical")
public final class FlipOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Bands")
    private String[] sourceBandNames;

    @Parameter(valueSet = {"Horizontal", "Vertical", "Horizontal and Vertical"},
            defaultValue = "Vertical", label = "Flip")
    private String flipType = "Vertical";

    private static final String PRODUCT_SUFFIX = "_Flip";

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
        ensureSingleRasterSize(sourceProduct);

        try {
            int flippingType = ProductFlipper.FLIP_BOTH;
            if (flipType.equalsIgnoreCase("Horizontal"))
                flippingType = ProductFlipper.FLIP_HORIZONTAL;
            else if (flipType.equalsIgnoreCase("Vertical"))
                flippingType = ProductFlipper.FLIP_VERTICAL;

            sourceProduct = ProductFlipperExt.createFlippedProduct(sourceProduct, flippingType,
                    sourceProduct.getName(), sourceProduct.getDescription());

            targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                    sourceProduct.getProductType(),
                    sourceProduct.getSceneRasterWidth(),
                    sourceProduct.getSceneRasterHeight());

            addSelectedBands();

            ProductUtils.copyProductNodes(sourceProduct, targetProduct);
        } catch (Throwable e) {
            throw new OperatorException(e);
        }
    }

    private void addSelectedBands() throws OperatorException {

        final Band[] sourceBands = getSourceBands(sourceProduct, sourceBandNames, false);

        for (Band srcBand : sourceBands) {
            final Band targetBand = ProductUtils.copyBand(srcBand.getName(), sourceProduct, targetProduct, false);
            targetBand.setSourceImage(srcBand.getSourceImage());
        }
    }

    /**
     * get the selected bands
     *
     * @param sourceProduct       the input product
     * @param sourceBandNames     the select band names
     * @param includeVirtualBands include virtual bands by default
     * @return band list
     * @throws OperatorException if source band not found
     */
    public static Band[] getSourceBands(final Product sourceProduct, String[] sourceBandNames, final boolean includeVirtualBands) throws OperatorException {

        if (sourceBandNames == null || sourceBandNames.length == 0) {
            final Band[] bands = sourceProduct.getBands();
            final List<String> bandNameList = new ArrayList<>(sourceProduct.getNumBands());
            for (Band band : bands) {
                if (!(band instanceof VirtualBand) || includeVirtualBands)
                    bandNameList.add(band.getName());
            }
            sourceBandNames = bandNameList.toArray(new String[bandNameList.size()]);
        }

        final List<Band> sourceBandList = new ArrayList<>(sourceBandNames.length);
        for (final String sourceBandName : sourceBandNames) {
            final Band sourceBand = sourceProduct.getBand(sourceBandName);
            if (sourceBand != null) {
                sourceBandList.add(sourceBand);
            }
        }
        return sourceBandList.toArray(new Band[sourceBandList.size()]);
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
            super(FlipOp.class);
        }
    }
}
