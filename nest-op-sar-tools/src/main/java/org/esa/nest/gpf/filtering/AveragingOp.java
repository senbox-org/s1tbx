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
package org.esa.nest.gpf.filtering;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ProductUtils;
import org.esa.nest.datamodel.Unit;
import org.esa.nest.gpf.OperatorUtils;
import org.esa.nest.gpf.ReaderUtils;
import org.esa.nest.gpf.UndersamplingOp;

import java.awt.*;
import java.io.File;
import java.text.MessageFormat;
import java.util.*;

/**
 * Averaging multi-temporal images
 */
@OperatorMetadata(alias="Averaging",
        category = "Image Processing",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2013 by Array Systems Computing Inc.",
        description = "Averaging multi-temporal images")
public class AveragingOp extends Operator {

    @SourceProduct
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter
    private boolean allbands = true;

    @Parameter(description = "The list of source bands.", alias = "sourceBands", itemAlias = "band",
            rasterDataNodeType = Band.class, label="Source Bands")
    private String[] sourceBandNames;

    @Parameter(defaultValue = "true")
    private boolean average = true;


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
            targetProduct = new Product(sourceProduct.getName(),
                                        sourceProduct.getProductType(),
                                        sourceProduct.getSceneRasterWidth(),
                                        sourceProduct.getSceneRasterHeight());

            OperatorUtils.copyProductNodes(sourceProduct, targetProduct);

            final Band[] sourceBands;
            if(allbands) {
                sourceBands = getAllBands();
            } else {
                sourceBands = OperatorUtils.getSourceBands(sourceProduct, sourceBandNames);
            }

            if(average) {

                final StringBuilder expression = new StringBuilder("( ");
                int cnt = 0;
                for(Band band : sourceBands) {
                    if(cnt > 0)
                        expression.append(" + ");
                    expression.append(band.getName());
                    ++cnt;
                }
                expression.append(") / ");
                expression.append(sourceBands.length);

                final VirtualBand virtBand = new VirtualBand("Averaged",
                        ProductData.TYPE_FLOAT32,
                        sourceProduct.getSceneRasterWidth(),
                        sourceProduct.getSceneRasterHeight(),
                        expression.toString());
                virtBand.setUnit(sourceBands[0].getUnit());
                virtBand.setDescription("Averaged "+sourceBands[0].getUnit());
                virtBand.setNoDataValueUsed(true);
                virtBand.setNoDataValue(sourceBands[0].getNoDataValue());

                final Band srcBand = sourceProduct.getBand(virtBand.getName());
                if(srcBand != null) {
                    sourceProduct.removeBand(srcBand);
                }
                sourceProduct.addBand(virtBand);

                ProductUtils.copyBand("Averaged", sourceProduct, targetProduct, true);
            }

        } catch(Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    private Band[] getAllBands() {

        final Band[] bands = sourceProduct.getBands();
        final java.util.List<Band> bandList = new ArrayList<Band>(sourceProduct.getNumBands());
        for (Band band : bands) {
            if(!(band instanceof VirtualBand))
                bandList.add(band);
        }
        return bandList.toArray(new Band[bandList.size()]);
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
            super(AveragingOp.class);
        }
    }
}
