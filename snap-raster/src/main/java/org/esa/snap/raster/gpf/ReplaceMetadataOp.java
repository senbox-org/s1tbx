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

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

/**
 * Replaces the Metadata with that of another product
 */
@OperatorMetadata(alias = "ReplaceMetadata",
        category = "Tools",
        authors = "Jun Lu, Luis Veci",
        version = "1.0",
        copyright = "Copyright (C) 2015 by Array Systems Computing Inc.",
        description = "Replace the metadata of the first product with that of the second")
public class ReplaceMetadataOp extends Operator {

    @SourceProducts
    private Product[] sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(defaultValue = "Replace the metadata of the first product with that of the second", label = " ")
    String note;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public ReplaceMetadataOp() {
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
            if (sourceProduct.length != 2) {
                throw new OperatorException("ReplaceMetadataOp requires two source products.");
            }
            final Product masterProduct = sourceProduct[0];
            final Product slaveProduct = sourceProduct[1];

            // create target product
            targetProduct = new Product(masterProduct.getName(),
                    slaveProduct.getProductType(),
                    masterProduct.getSceneRasterWidth(),
                    masterProduct.getSceneRasterHeight());

            // Add target bands
            final Band[] bands = masterProduct.getBands();
            for (Band srcBand : bands) {

                final Band targetBand = ProductUtils.copyBand(srcBand.getName(), masterProduct, targetProduct, false);
                targetBand.setSourceImage(srcBand.getSourceImage());
            }

            ProductUtils.copyProductNodes(slaveProduct, targetProduct);

            final MetadataElement absRootMst = AbstractMetadata.getAbstractedMetadata(masterProduct);
            final int isPolsar = absRootMst.getAttributeInt(AbstractMetadata.polsarData, 0);
            final int isCalibrated = absRootMst.getAttributeInt(AbstractMetadata.abs_calibration_flag, 0);

            resetPolarizations(AbstractMetadata.getAbstractedMetadata(targetProduct), isPolsar, isCalibrated);

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    public static void resetPolarizations(final MetadataElement absRoot, final int isPolsar, final int isCal) {
        if (isPolsar > 0) {
            absRoot.setAttributeString(AbstractMetadata.mds1_tx_rx_polar, AbstractMetadata.NO_METADATA_STRING);
            absRoot.setAttributeString(AbstractMetadata.mds2_tx_rx_polar, AbstractMetadata.NO_METADATA_STRING);
            absRoot.setAttributeString(AbstractMetadata.mds3_tx_rx_polar, AbstractMetadata.NO_METADATA_STRING);
            absRoot.setAttributeString(AbstractMetadata.mds4_tx_rx_polar, AbstractMetadata.NO_METADATA_STRING);
            absRoot.setAttributeInt(AbstractMetadata.polsarData, 1);
        }
        if (isCal > 0) {     // polsarpro input already calibrated -- set by polsarpro reader
            absRoot.setAttributeInt(AbstractMetadata.abs_calibration_flag, 1);
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
            super(ReplaceMetadataOp.class);
        }
    }
}
