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
package org.esa.s1tbx.calibration.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.calibration.gpf.support.CalibrationFactory;
import org.esa.s1tbx.calibration.gpf.support.Calibrator;
import org.esa.snap.datamodel.AbstractMetadata;
import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.MetadataElement;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.datamodel.VirtualBand;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.Tile;
import org.esa.snap.framework.gpf.annotations.OperatorMetadata;
import org.esa.snap.framework.gpf.annotations.Parameter;
import org.esa.snap.framework.gpf.annotations.SourceProduct;
import org.esa.snap.framework.gpf.annotations.TargetProduct;
import org.esa.snap.gpf.OperatorUtils;

import java.io.File;

/**
 * Calibration for all data products.
 *
 * @todo automatically search aux file in local repository using time period
 */
@OperatorMetadata(alias = "Calibration",
        category = "SAR Processing/Radiometric",
        authors = "Jun Lu, Luis Veci",
        copyright = "Copyright (C) 2014 by Array Systems Computing Inc.",
        description = "Calibration of products")
public class CalibrationOp extends Operator {

    @SourceProduct(alias = "source")
    private Product sourceProduct;
    @TargetProduct
    private Product targetProduct;

    @Parameter(description = "The list of source bands.", alias = "sourceBands",
            rasterDataNodeType = Band.class, label = "Source Band")
    private String[] sourceBandNames;

    @Parameter(valueSet = {LATEST_AUX, PRODUCT_AUX, EXTERNAL_AUX}, description = "The auxiliary file",
            defaultValue = LATEST_AUX, label = "Auxiliary File")
    private String auxFile = LATEST_AUX;

    @Parameter(description = "The antenna elevation pattern gain auxiliary data file.", label = "External Aux File")
    private File externalAuxFile = null;

    @Parameter(description = "Output image in complex", defaultValue = "false", label = "Save in complex")
    private Boolean outputImageInComplex = false;

    @Parameter(description = "Output image scale", defaultValue = "false", label = "Scale in dB")
    private Boolean outputImageScaleInDb = false;

    @Parameter(description = "Create gamma0 virtual band", defaultValue = "false", label = "Create gamma0 virtual band")
    private Boolean createGammaBand = false;

    @Parameter(description = "Create beta0 virtual band", defaultValue = "false", label = "Create beta0 virtual band")
    private Boolean createBetaBand = false;

    // for Sentinel-1 mission only
    @Parameter(description = "The list of polarisations", label = "Polarisations")
    private String[] selectedPolarisations;

    @Parameter(description = "Output sigma0 band", defaultValue = "true", label = "Output sigma0 band")
    private Boolean outputSigmaBand = true;

    @Parameter(description = "Output gamma0 band", defaultValue = "false", label = "Output gamma0 band")
    private Boolean outputGammaBand = false;

    @Parameter(description = "Output beta0 band", defaultValue = "false", label = "Output beta0 band")
    private Boolean outputBetaBand = false;

    @Parameter(description = "Output DN band", defaultValue = "false", label = "Output DN band")
    private Boolean outputDNBand = false;

    private Calibrator calibrator = null;

    public static final String PRODUCT_AUX = "Product Auxiliary File";
    public static final String LATEST_AUX = "Latest Auxiliary File";
    public static final String EXTERNAL_AUX = "External Auxiliary File";

    /**
     * Default constructor. The graph processing framework
     * requires that an operator has a default constructor.
     */
    public CalibrationOp() {
    }

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
            MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(sourceProduct);
            if (AbstractMetadata.getAttributeBoolean(absRoot, AbstractMetadata.coregistered_stack)) {
                throw new OperatorException("Cannot apply calibration to coregistered product.");
            }

            calibrator = CalibrationFactory.createCalibrator(sourceProduct);
            calibrator.setAuxFileFlag(auxFile);
            calibrator.setExternalAuxFile(externalAuxFile);
            calibrator.setOutputImageInComplex(outputImageInComplex);
            calibrator.setOutputImageIndB(outputImageScaleInDb);

            if (calibrator instanceof Sentinel1Calibrator) {
                Sentinel1Calibrator cal = (Sentinel1Calibrator) calibrator;
                cal.setUserSelections(sourceProduct,
                        selectedPolarisations, outputSigmaBand, outputGammaBand, outputBetaBand, outputDNBand);
            }
            targetProduct = calibrator.createTargetProduct(sourceProduct, sourceBandNames);
            calibrator.initialize(this, sourceProduct, targetProduct, false, true);

            if (createGammaBand) {
                createGammaVirtualBand(targetProduct, outputImageScaleInDb);
            }

            if (createBetaBand) {
                createBetaVirtualBand(targetProduct, outputImageScaleInDb);
            }

            updateTargetProductMetadata();

        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Update the metadata in the target product.
     */
    private void updateTargetProductMetadata() {

        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(targetProduct);
        absRoot.getAttribute(AbstractMetadata.abs_calibration_flag).getData().setElemBoolean(true);

        if(!outputImageInComplex) {
            absRoot.setAttributeString(AbstractMetadata.SAMPLE_TYPE, "DETECTED");
        }
    }

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".</p>
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancelation requests.
     * @throws org.esa.snap.framework.gpf.OperatorException If an error occurs during computation of the target raster.
     */
    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        try {
            calibrator.computeTile(targetBand, targetTile, pm);
        } catch (Throwable e) {
            OperatorUtils.catchOperatorException(getId(), e);
        }
    }

    /**
     * Create Gamma image as a virtual band.
     *
     * @param trgProduct           The target product
     * @param outputImageScaleInDb flag if output is in dB
     */
    public static void createGammaVirtualBand(final Product trgProduct, final boolean outputImageScaleInDb) {

        int count = 1;
        final Band[] bands = trgProduct.getBands();
        for (Band trgBand : bands) {

            final String unit = trgBand.getUnit();
            if (trgBand instanceof VirtualBand || (unit != null && unit.contains("phase"))) {
                continue;
            }

            final String trgBandName = trgBand.getName();
            final String expression;
            if (outputImageScaleInDb) {
                expression = "(pow(10," + trgBandName + "/10.0)" + " / cos(incident_angle * PI/180.0)) "
                        + "==0 ? 0 : 10 * log10(abs("
                        + "(pow(10," + trgBandName + "/10.0)" + " / cos(incident_angle * PI/180.0))"
                        + "))";
            } else {
                expression = trgBandName + " / cos(incident_angle * PI/180.0)";
            }
            String gammeBandName = "Gamma0";

            if (bands.length > 1) {
                if (trgBandName.contains("_HH"))
                    gammeBandName += "_HH";
                else if (trgBandName.contains("_VV"))
                    gammeBandName += "_VV";
                else if (trgBandName.contains("_HV"))
                    gammeBandName += "_HV";
                else if (trgBandName.contains("_VH"))
                    gammeBandName += "_VH";
            }
            if (outputImageScaleInDb) {
                gammeBandName += "_dB";
            }

            while (trgProduct.getBand(gammeBandName) != null) {
                gammeBandName += "_" + ++count;
            }

            final VirtualBand band = new VirtualBand(gammeBandName,
                    ProductData.TYPE_FLOAT32,
                    trgBand.getSceneRasterWidth(),
                    trgBand.getSceneRasterHeight(),
                    expression);
            band.setUnit(unit);
            band.setDescription("Gamma0 image");
            trgProduct.addBand(band);
        }
    }

    /**
     * Create Beta image as a virtual band.
     *
     * @param trgProduct           The target product
     * @param outputImageScaleInDb flag if output is in dB
     */
    public static void createBetaVirtualBand(final Product trgProduct, final boolean outputImageScaleInDb) {

        int count = 1;
        final Band[] bands = trgProduct.getBands();
        for (Band trgBand : bands) {

            final String unit = trgBand.getUnit();
            if (trgBand instanceof VirtualBand || (unit != null && unit.contains("phase"))) {
                continue;
            }

            final String trgBandName = trgBand.getName();
            final String expression;
            if (outputImageScaleInDb) {
                expression = "(pow(10," + trgBandName + "/10.0)" + " / sin(incident_angle * PI/180.0)) "
                        + "==0 ? 0 : 10 * log10(abs("
                        + "(pow(10," + trgBandName + "/10.0)" + " / sin(incident_angle * PI/180.0))"
                        + "))";
            } else {
                expression = trgBandName + " / sin(incident_angle * PI/180.0)";
            }
            String betaBandName = "Beta0";

            if (bands.length > 1) {
                if (trgBandName.contains("_HH"))
                    betaBandName += "_HH";
                else if (trgBandName.contains("_VV"))
                    betaBandName += "_VV";
                else if (trgBandName.contains("_HV"))
                    betaBandName += "_HV";
                else if (trgBandName.contains("_VH"))
                    betaBandName += "_VH";
            }
            if (outputImageScaleInDb) {
                betaBandName += "_dB";
            }

            while (trgProduct.getBand(betaBandName) != null) {
                betaBandName += "_" + ++count;
            }

            final VirtualBand band = new VirtualBand(betaBandName,
                    ProductData.TYPE_FLOAT32,
                    trgBand.getSceneRasterWidth(),
                    trgBand.getSceneRasterHeight(),
                    expression);
            band.setUnit(unit);
            band.setDescription("Beta0 image");
            trgProduct.addBand(band);
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
            super(CalibrationOp.class);
        }
    }
}
