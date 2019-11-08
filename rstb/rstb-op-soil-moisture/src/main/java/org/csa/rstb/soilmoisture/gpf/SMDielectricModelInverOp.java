/*
 * Copyright (C) 2019 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.csa.rstb.soilmoisture.gpf;

import com.bc.ceres.core.ProgressMonitor;
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
import org.esa.snap.engine_utilities.datamodel.Unit;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;

import java.awt.*;
import java.util.Calendar;
import java.util.Map;

/**
 * Soil moisture plays an important role in the estimation of dielectric properties in soil.
 * By inverting a dielectric model, soil moisture can be retrieved from the soil real dielectric constant (RDC).
 * The dielectric models include:
 * (1) Hallikainen
 * (2) Mironov
 */

@OperatorMetadata(alias = "SM-Dielectric-Modeling",
        category = "Radar/Soil Moisture",
        authors = "Cecilia Wong",
        description = "Performs SM inversion using dielectric model")
public class SMDielectricModelInverOp extends Operator {

    private static final double INVALID_SM_VALUE = -999d;
    private static final String PRODUCT_SUFFIX = "_SM";
    private static String rdcBandName = "RDC";
    // The source product contains the soil real dielectric constant (in Farad/m) as well as auxiliary data,
    // e.g., sand and clay percentages.
    @SourceProduct
    private Product sourceProduct = null;
    // There is one target product containing at least one band which is soil moisture (in m^3/m^3).
    @TargetProduct
    private Product targetProduct = null;
    @Parameter(valueSet = {DielectricModelFactory.HALLIKAINEN, DielectricModelFactory.MIRONOV},
            description = "Choice of dielectric models for SM inversion",
            defaultValue = DielectricModelFactory.HALLIKAINEN, label = "Dielectric model")
    private String modelToUse = DielectricModelFactory.HALLIKAINEN;
    @Parameter(description = "Minimum soil moisture value", defaultValue = "0.0", interval = "[0.0, 1.0)", unit = "m^3/m^3", label = "Min SM")
    private double minSM = 0.0;
    @Parameter(description = "Maximum soil moisture value", defaultValue = "0.55", interval = "(0.0, 1.0]", unit = "m^3/m^3", label = "Max SM")
    private double maxSM = 0.55;
    @Parameter(description = "Optional RDC in output", defaultValue = "true", label = "Output RDC")
    private boolean outputRDC = true;
    @Parameter(description = "Optional LandCover in output", defaultValue = "true", label = "Output Land Cover")
    private boolean outputLandCover = true;
    @Parameter(description = "Effective soil temperature", defaultValue = "18.0", interval = "[-50.0, 50.0]", unit = "Celsius", label = "Effective soil temperature")
    private double effectiveSoilTemperature = 18.0d;
    private int sourceImageWidth = 0;
    private int sourceImageHeight = 0;
    private DielectricModel dielectricModel = null;
    private Band smBand;
    private Band qualityIndexBand;

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public SMDielectricModelInverOp() {

    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link org.esa.snap.core.datamodel.Product} annotated with the
     * {@link org.esa.snap.core.gpf.annotations.TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.</p>
     * <p>The framework calls this method after it has created this operator.
     * Any client code that must be performed before computation of tile data
     * should be placed here.</p>
     *
     * @throws org.esa.snap.core.gpf.OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    @Override
    public void initialize() throws OperatorException {

        try {

            getSourceImageDimension();

            createTargetProduct();

            dielectricModel = DielectricModelFactory.createDielectricModel(this, sourceProduct, targetProduct,
                    INVALID_SM_VALUE, minSM, maxSM,
                    smBand, qualityIndexBand,
                    rdcBandName, modelToUse);
            if (dielectricModel instanceof MironovDielectricModel) {
                final MironovDielectricModel mironov = (MironovDielectricModel) dielectricModel;
                mironov.setParameters(effectiveSoilTemperature);
            }
            dielectricModel.initialize();

        } catch (Throwable e) {

            OperatorUtils.catchOperatorException(getId(), e);
        }
    }


    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     * <p>This method shall never be called directly.
     * </p>
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in <code>targetRasters</code>).
     * @param pm              A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.snap.core.gpf.OperatorException if an error occurs during computation of the target rasters.
     */
    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm)
            throws OperatorException {

        try {
            dielectricModel.computeTileStack(targetTiles, targetRectangle, pm);
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Get source image dimension.
     */
    private void getSourceImageDimension() {

        if (sourceProduct == null) {

            throw new OperatorException("There is no source product");
        }

        sourceImageWidth = sourceProduct.getSceneRasterWidth();
        sourceImageHeight = sourceProduct.getSceneRasterHeight();
    }

    /**
     * Create target product.
     */
    private void createTargetProduct() {

        // The target product has the same dimension as the source products.

        targetProduct = new Product(sourceProduct.getName() + PRODUCT_SUFFIX,
                sourceProduct.getProductType(),
                sourceProduct.getSceneRasterWidth(),
                sourceProduct.getSceneRasterHeight());

        // Add the bands that appear in the target product for all models.
        // The target bands all have the same dimensions as the target product.
        // Assume the target bands are all doubles.

        smBand = targetProduct.addBand("sm", ProductData.TYPE_FLOAT32);
        smBand.setUnit(Unit.SOIL_MOISTURE);
        smBand.setNoDataValue(INVALID_SM_VALUE);
        smBand.setNoDataValueUsed(true);

        // Note that "No Data Value" is set in BaseDielectricModel.
        qualityIndexBand = targetProduct.addBand("quality index", ProductData.TYPE_UINT8);

        for (Band band : sourceProduct.getBands()) {

            if (band.getName().equals(rdcBandName)) {
                if (outputRDC) {
                    ProductUtils.copyBand(rdcBandName, sourceProduct, targetProduct, true);
                }
            } else if (band.getUnit() != null && band.getUnit().equals(Unit.CLASS)) {
                if (outputLandCover) {
                    ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, true);
                }
            } else {
                ProductUtils.copyBand(band.getName(), sourceProduct, targetProduct, true);
            }
        }

        ProductUtils.copyProductNodes(sourceProduct, targetProduct);

        updateMetadata();
    }

    private void updateMetadata() {
        final MetadataElement root = AbstractMetadata.getAbstractedMetadata(targetProduct);
        MetadataElement productElem = root.getElement("Product_Information");
        if (productElem == null) {
            productElem = new MetadataElement("Product_Information");
            root.addElement(productElem);
        }

        AbstractMetadata.addAbstractedAttribute(productElem, "Product Name", ProductData.TYPE_ASCII, "", "");
        AbstractMetadata.setAttribute(productElem, "Product Name", targetProduct.getName());

        final String contextID = System.getProperty("ceres.context");
        Calendar cal = Calendar.getInstance();
        ProductData.UTC time = ProductData.UTC.create(cal.getTime(), 0);

        AbstractMetadata.addAbstractedAttribute(productElem, "Processing Time", ProductData.TYPE_UTC, "", "");
        AbstractMetadata.setAttribute(productElem, "Processing Time", time);

        AbstractMetadata.addAbstractedAttribute(productElem, "Processor", ProductData.TYPE_ASCII, "", "");
        AbstractMetadata.setAttribute(productElem, "Processor", contextID);

        AbstractMetadata.addAbstractedAttribute(productElem, "Processor version", ProductData.TYPE_ASCII, "", "");
        AbstractMetadata.setAttribute(productElem, "Processor version", System.getProperty(contextID + ".version"));

        AbstractMetadata.addAbstractedAttribute(productElem, "Dielectric model", ProductData.TYPE_ASCII, "", "");
        AbstractMetadata.setAttribute(productElem, "Dielectric model", modelToUse);
    }

    /**
     * Set the dielectric model to be used.
     * This function is used for unit test only.
     *
     * @param dielecModel The dielectric model to be used.
     */
    public void setModelToUse(final String dielecModel) {
        modelToUse = dielecModel;
    }

    /**
     * The SPI is used to register this operator in the graph processing framework
     * via the SPI configuration file
     * {@code META-INF/services/org.esa.snap.core.gpf.OperatorSpi}.
     * This class may also serve as a factory for new operator instances.
     *
     * @see org.esa.snap.core.gpf.OperatorSpi#createOperator()
     * @see org.esa.snap.core.gpf.OperatorSpi#createOperator(java.util.Map, java.util.Map)
     */
    public static class Spi extends OperatorSpi {
        public Spi() {
            super(SMDielectricModelInverOp.class);
        }
    }
}