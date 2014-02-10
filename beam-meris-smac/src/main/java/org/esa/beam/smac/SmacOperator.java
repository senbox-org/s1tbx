/*
 * Copyright (C) 2013 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.smac;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.ObjectUtils;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.ResourceInstaller;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.math.RsMathUtils;

import java.awt.Color;
import java.awt.Rectangle;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

@SuppressWarnings({"UnusedDeclaration", "MismatchedReadAndWriteOfArray, FieldCanBeLocal"})
@OperatorMetadata(alias = "SmacOp",
                  version = "1.5.205",
                  authors = "H. Rahman, G. Dedieu (Algorithm), Tom Block (BEAM Implementation), Thomas Storm (GPF conversion)",
                  copyright = "Copyright (C) 2002-2014 by Brockmann Consult (info@brockmann-consult.de)",
                  description = "Applies the simplified method for atmospheric corrections of satellite measurements.")
public class SmacOperator extends Operator {

    private static final String DEFAULT_MERIS_FLAGS_VALUE = "l1_flags.LAND_OCEAN and not (l1_flags.INVALID or l1_flags.BRIGHT)";
    private static final String DEFAULT_FORWARD_FLAGS_VALUE = "cloud_flags_fward.LAND and not cloud_flags_fward.CLOUDY";
    private static final String DEFAULT_NADIR_FLAGS_VALUE = "cloud_flags_nadir.LAND and not cloud_flags_nadir.CLOUDY";
    private static final String LOG_MSG_LOADED = "Loaded ";
    static final String SMAC_AUXDATA_DIR_PROPERTY = "smac.auxdata.dir";
    private static final String PROCESSOR_SYMBOLIC_NAME = "beam-meris-smac";

    private static final int merisSzaIndex = 6;
    private static final int merisSaaIndex = 7;
    private static final int merisVzaIndex = 8;
    private static final int merisVaaIndex = 9;
    private static final int merisPressIndex = 12;
    private static final int merisElevIndex = 2;
    private static final int merisO3Index = 13;
    private static final int merisWvIndex = 14;
    private static final int aatsrSzaIndex = 7;
    private static final int aatsrSzaFwdIndex = 11;
    private static final int aatsrSaaIndex = 9;
    private static final int aatsrSaaFwdIndex = 13;
    private static final int aatsrVzaIndex = 8;
    private static final int aatsrVzaFwdIndex = 12;
    private static final int aatsrVaaIndex = 10;
    private static final int aatsrVaaFwdIndex = 14;
    private static final float duToCmAtm = 0.001f;
    private static final float relHumTogcm = 0.07f;

    private static final String merisBandPrefix = "reflec";  // was "reflectance" in version 1.0 - 1.2
    private static final String SMAC_MASK = "smac_mask";
    private static final String SMAC_MASK_FORWARD = "smac_mask_forward";

    private final List<Band> inputBandList;
    private final Logger logger;
    private String sensorType;

    private File auxdataInstallDir;
    private Map<String, String> bandNameMapping;
    private HashMap<String, SmacSensorCoefficients> coefficients;

    private TiePointGrid szaBand;
    private TiePointGrid saaBand;
    private TiePointGrid vzaBand;
    private TiePointGrid vaaBand;
    private TiePointGrid wvBand;
    private TiePointGrid o3Band;
    private TiePointGrid pressBand;
    private TiePointGrid elevBand;
    private TiePointGrid szaFwdBand;
    private TiePointGrid saaFwdBand;
    private TiePointGrid vzaFwdBand;
    private TiePointGrid vaaFwdBand;

    @Parameter(description = "Aerosol optical depth", label = "Aerosol optical depth")
    private Float tau_aero_550 = 0.2F;

    @Parameter(description = "Relative humidity", label = "Relative humidity")
    private Float u_h2o = 3.0F;

    @Parameter(description = "Ozone content", label = "Ozone content")
    private Float u_o3 = 0.15F;

    @Parameter(description = "Surface pressure", label = "Surface pressure")
    private Float surf_press = 1013.0F;

    @Parameter(description = "Use MERIS ADS", label = "Use MERIS ADS")
    private Boolean useMerisADS = true;

    @Parameter(description = "Aerosol type", label = "Aerosol type", notNull = true, valueSet = {
            SensorCoefficientManager.AER_CONT_NAME,
            SensorCoefficientManager.AER_DES_NAME
    }, defaultValue = SensorCoefficientManager.AER_CONT_NAME)
    private String aerosolType;

    @Parameter(description = "Default reflectance for invalid pixel", label = "Default reflectance for invalid pixel")
    Float invalidPixel = 0.0F;

    @Parameter(description = "Mask expression for the whole view (MERIS) or the nadir view (AATSR)",
               label = "Mask expression for the whole view (MERIS) or the nadir view (AATSR)")
    private String maskExpression = "";

    @Parameter(description = "Mask expression for the forward view (AATSR only)", label = "Mask expression for the forward view (AATSR only)")
    private String maskExpressionForward = "";

    @Parameter(description = "Bands to process", label = "Bands to process", notNull = true)
    private String[] bandNames;

    @SourceProduct(alias = "source", label = "Source product")
    private Product sourceProduct;

    @TargetProduct(label = "SMAC product")
    private Product targetProduct;

    public SmacOperator() {
        inputBandList = new ArrayList<>();
        logger = getLogger();
        bandNameMapping = new HashMap<>();
        coefficients = new HashMap<>();
    }

    @Override
    public void initialize() throws OperatorException {
        try {
            prepareProcessing();
            createOutputProduct();
        } catch (IOException e) {
            throw new OperatorException(e);
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        // here goes the allocation of data that is constant for the targetRectangle

        // initialize vectors and other data
        // ---------------------------------
        int width = targetRectangle.width;
        int height = targetRectangle.height;
        int x = targetRectangle.x;
        int y = targetRectangle.y;

        SourceData sourceData = new SourceData();
        sourceData.taup550 = new float[width * height];
        sourceData.process = new boolean[width * height];
        sourceData.uh2o = new float[width * height];
        sourceData.uo3 = new float[width * height];
        sourceData.press = new float[width * height];
        sourceData.toa = new float[width * height];

        for (int i = 0; i < width * height; i++) {
            sourceData.taup550[i] = tau_aero_550;
            sourceData.uh2o[i] = u_h2o;
            sourceData.uo3[i] = u_o3;
            sourceData.press[i] = surf_press;
            sourceData.process[i] = true;
        }

        sourceData.sza = getSourceTile(szaBand, targetRectangle).getSamplesFloat();
        sourceData.saa = getSourceTile(saaBand, targetRectangle).getSamplesFloat();
        sourceData.vza = getSourceTile(vzaBand, targetRectangle).getSamplesFloat();
        sourceData.vaa = getSourceTile(vaaBand, targetRectangle).getSamplesFloat();
        if (ObjectUtils.equalObjects(sensorType, SensorCoefficientManager.AATSR_NAME)) {
            sourceData.szaFwd = getSourceTile(szaFwdBand, targetRectangle).getSamplesFloat();
            sourceData.saaFwd = getSourceTile(saaFwdBand, targetRectangle).getSamplesFloat();
            sourceData.vzaFwd = getSourceTile(vzaFwdBand, targetRectangle).getSamplesFloat();
            sourceData.vaaFwd = getSourceTile(vaaFwdBand, targetRectangle).getSamplesFloat();
        } else {
            sourceData.uh2o = getSourceTile(wvBand, targetRectangle).getSamplesFloat();
            sourceData.uo3 = getSourceTile(o3Band, targetRectangle).getSamplesFloat();
            sourceData.press = getSourceTile(pressBand, targetRectangle).getSamplesFloat();
            sourceData.elev = getSourceTile(elevBand, targetRectangle).getSamplesFloat();
        }

        for (Map.Entry<Band, Tile> bandTileEntry : targetTiles.entrySet()) {
            Band sourceBand = sourceProduct.getBand(revertMerisBandName(bandTileEntry.getKey().getName(), bandNameMapping));
            sourceBand.setValidPixelExpression(""); // necessary in order to mimic the processor behavior
            Tile sourceTile = getSourceTile(sourceBand, targetRectangle);
            sourceData.toa = sourceTile.getSamplesFloat();
            Tile targetTile = bandTileEntry.getValue();
            try {
                if (ObjectUtils.equalObjects(sensorType, SensorCoefficientManager.MERIS_NAME)) {
                    if (useMerisADS) {
                        processMerisWithADS(sourceBand, sourceData, targetTile, targetRectangle, new SmacAlgorithm());
                    } else {
                        processMeris(sourceBand, sourceData, targetTile, targetRectangle, new SmacAlgorithm());
                    }
                } else if (ObjectUtils.equalObjects(sensorType, SensorCoefficientManager.AATSR_NAME)) {
                    processAatsr(sourceBand.getName(), sourceData, targetTile, targetRectangle, new SmacAlgorithm());
                }
            } catch (IOException e) {
                logger.severe("An error occurred during processing: ");
                logger.severe(e.getMessage());
            }
        }
    }

    @Override
    public void dispose() {
        if (sourceProduct != null) {
            sourceProduct.dispose();
            sourceProduct = null;
        }
    }

    // package private for testing reasons only
    void installAuxdata() {
        setAuxdataInstallDir(SMAC_AUXDATA_DIR_PROPERTY, getDefaultAuxdataInstallDir());
        try {
            installAuxdata(ResourceInstaller.getSourceUrl(getClass()), "auxdata/", auxdataInstallDir);
        } catch (IOException e) {
            throw new OperatorException("Failed to install auxdata into " + auxdataInstallDir, e);
        }
    }

    // package private for testing reasons only
    File getAuxdataInstallDir() {
        return auxdataInstallDir;
    }

    private void prepareProcessing() throws IOException {
        logger.info("Preparing SMAC processing");

        // create a vector of input bands
        // ------------------------------
        loadInputProduct();

        // create a bitmask expression for input
        // -------------------------------------
        createMask();
        installAuxdata();
    }

    private void loadInputProduct() throws IOException {
        // check what product type the input is and load the appropriate tie point ADS
        // ---------------------------------------------------------------------------
        sensorType = SmacUtils.getSensorType(sourceProduct.getProductType());
        if (ObjectUtils.equalObjects(sensorType, SensorCoefficientManager.MERIS_NAME)) {
            loadMERIS_ADS(sourceProduct);
        } else if (ObjectUtils.equalObjects(sensorType, SensorCoefficientManager.AATSR_NAME)) {
            loadAATSR_ADS(sourceProduct);
            useMerisADS = false;
        } else {
            throw new OperatorException("Unsupported sensor type!");
        }

        // set up the bands we need for this request
        // -----------------------------------------
        if (bandNames.length == 0) {
            throw new OperatorException("No input bands defined, processing cannot be performed");
        }

        for (String bandName : bandNames) {
            Band band = sourceProduct.getBand(bandName);
            if (band == null) {
                logger.warning("The requested band '" + bandName + "' is not contained in the input product!");
            } else {
                if (band.getSpectralBandIndex() != -1) {
                    inputBandList.add(band);
                } else {
                    logger.warning(
                            "The requested band '" + bandName +
                            "' is not a spectral band and will be excluded from processing");
                }
            }
        }
    }

    private void loadMERIS_ADS(Product product) {
        logger.info("Loading MERIS ADS");

        // sun zenith angle
        szaBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[merisSzaIndex]);
        Assert.notNull(szaBand);
        logger.fine(LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[merisSzaIndex]);

        // sun azimuth angle
        saaBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[merisSaaIndex]);
        Assert.notNull(saaBand);
        logger.fine(LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[merisSaaIndex]);

        // view zenith angle
        vzaBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[merisVzaIndex]);
        Assert.notNull(vzaBand);
        logger.fine(LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[merisVzaIndex]);

        // view azimuth angle
        vaaBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[merisVaaIndex]);
        Assert.notNull(vaaBand);
        logger.fine(LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[merisVaaIndex]);

        // if requested load the optional MERIS ADS
        // ----------------------------------------
        if (useMerisADS) {
            // waterVapour
            wvBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[merisWvIndex]);
            Assert.notNull(wvBand);
            logger.fine(LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[merisWvIndex]);

            // ozone
            o3Band = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[merisO3Index]);
            Assert.notNull(o3Band);
            logger.fine(LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[merisO3Index]);

            // atmospheric pressure
            pressBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[merisPressIndex]);
            Assert.notNull(pressBand);
            logger.fine(LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[merisPressIndex]);

            // digital elevation
            elevBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[merisElevIndex]);
            Assert.notNull(elevBand);
            logger.fine(LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[merisElevIndex]);
        }
        logger.info("... success");
    }

    private void loadAATSR_ADS(Product product) {

        logger.info("Loading AATSR ADS");

        // sun elevation angle nadir
        szaBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[aatsrSzaIndex]);
        Assert.notNull(szaBand);
        logger.fine(LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[aatsrSzaIndex]);

        // sun elevation angle forward
        szaFwdBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[aatsrSzaFwdIndex]);
        Assert.notNull(szaFwdBand);
        logger.fine(LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[aatsrSzaFwdIndex]);

        // sun azimuth angle nadir
        saaBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[aatsrSaaIndex]);
        Assert.notNull(saaBand);
        logger.fine(LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[aatsrSaaIndex]);

        // sun azimuth angle forward
        saaFwdBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[aatsrSaaFwdIndex]);
        Assert.notNull(saaFwdBand);
        logger.fine(LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[aatsrSaaFwdIndex]);

        // view elevation angle nadir
        vzaBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[aatsrVzaIndex]);
        Assert.notNull(vzaBand);
        logger.fine(LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[aatsrVzaIndex]);

        // view elevation angle forward
        vzaFwdBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[aatsrVzaFwdIndex]);
        Assert.notNull(vzaFwdBand);
        logger.fine(LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[aatsrVzaFwdIndex]);

        // view azimuth angle nadir
        vaaBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[aatsrVaaIndex]);
        Assert.notNull(vaaBand);
        logger.fine(LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[aatsrVaaIndex]);

        // view azimuth angle forward
        vaaFwdBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[aatsrVaaFwdIndex]);
        Assert.notNull(vaaFwdBand);
        logger.fine(LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[aatsrVaaFwdIndex]);

        logger.info("... success");
    }

    /**
     * Replaces createBitmaskTerm()
     */
    private void createMask() {
        if (ObjectUtils.equalObjects(sensorType, SensorCoefficientManager.MERIS_NAME)) {
            createMerisBitmaskTerm();
        } else {
            createAatsrBitmaskTerm();
        }
    }

    // Creates a MERIS bitmask term given the bitmask expression from the request. If no expression is set, it uses the
    // default expression
    private void createMerisBitmaskTerm() {
        if ("".equalsIgnoreCase(maskExpression)) {
            maskExpression = DEFAULT_MERIS_FLAGS_VALUE;
            logger.warning("No mask expression defined");
            logger.warning("Using default mask expression: " + DEFAULT_MERIS_FLAGS_VALUE);
        } else {
            logger.info("Using mask expression: " + maskExpression);
        }
        Mask mask = sourceProduct.addMask(SMAC_MASK, maskExpression, "", Color.BLACK, 0.0);
        mask.setValidPixelExpression(maskExpression);
    }

    // Creates an AATSR bitmask term given the bitmask expression from the request. If no expression is set, it uses the
    // default expression
    private void createAatsrBitmaskTerm() {
        Mask mask;
        Mask forwardMask;
        if ("".equalsIgnoreCase(maskExpression)) {
            mask = sourceProduct.addMask(SMAC_MASK, DEFAULT_NADIR_FLAGS_VALUE, "", Color.BLACK, 0.0);
            forwardMask = sourceProduct.addMask(SMAC_MASK_FORWARD, DEFAULT_FORWARD_FLAGS_VALUE, "", Color.BLACK, 0.0);

            logger.warning("No mask expression defined");
            logger.warning("Using default nadir mask expression: " + DEFAULT_NADIR_FLAGS_VALUE);
            logger.warning("Using default forward mask expression: " + DEFAULT_FORWARD_FLAGS_VALUE);
        } else {
            mask = sourceProduct.addMask(SMAC_MASK, maskExpression, "", Color.BLACK, 0.0);
            forwardMask = sourceProduct.addMask(SMAC_MASK_FORWARD, maskExpressionForward, "", Color.BLACK, 0.0);

            logger.info("Using nadir mask expression: " + maskExpression);
            logger.info("Using forward mask expression: " + maskExpressionForward);
        }
        mask.setValidPixelExpression(maskExpression);
        forwardMask.setValidPixelExpression(maskExpressionForward);
    }

    private void setAuxdataInstallDir(String auxdataDirPropertyName, File defaultAuxdataInstallDir) {
        Assert.argument(StringUtils.isNotNullAndNotEmpty(auxdataDirPropertyName), "auxdataDirPropertyName is not null and not empty");
        String auxdataDirPath = System.getProperty(auxdataDirPropertyName, defaultAuxdataInstallDir.getAbsolutePath());
        auxdataInstallDir = new File(auxdataDirPath);
    }

    private File getDefaultAuxdataInstallDir() {
        return new File(SystemUtils.getApplicationDataDir(), PROCESSOR_SYMBOLIC_NAME + "/auxdata");
    }

    private void installAuxdata(URL sourceLocation, String sourceRelPath, File auxdataInstallDir) throws IOException {
        new ResourceInstaller(sourceLocation, sourceRelPath, auxdataInstallDir)
                .install(".*", ProgressMonitor.NULL);
    }

    private void createOutputProduct() throws IOException {
        String productType = sourceProduct.getProductType() + "_SMAC";
        String productName = sourceProduct.getName() + "_SMAC";
        int sceneWidth = sourceProduct.getSceneRasterWidth();
        int sceneHeight = sourceProduct.getSceneRasterHeight();
        targetProduct = new Product(productName, productType, sceneWidth, sceneHeight);

        // loop over bands and create them
        // -------------------------------
        if (ObjectUtils.equalObjects(sensorType, SensorCoefficientManager.MERIS_NAME)) {
            addBandsToOutput("Atmosphere corrected MERIS band ", true);
        } else {
            addBandsToOutput("Atmosphere corrected band ", false);
        }
        ProductUtils.copyTiePointGrids(sourceProduct, targetProduct);

        // the following line copies the processing request as metadata into the target product.
        // there is no such mechanism in GPF yet, so it is commented out.
        // copyRequestMetaData(_outputProduct);

        ProductUtils.copyFlagBands(sourceProduct, targetProduct, true);

        // for MERIS FSG / FRG products
        ProductUtils.copyBand(EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LATITUDE_BAND_NAME, sourceProduct, targetProduct, true);
        ProductUtils.copyBand(EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LONGITUDE_BAND_NAME, sourceProduct, targetProduct, true);
        ProductUtils.copyBand(EnvisatConstants.MERIS_AMORGOS_L1B_ALTIUDE_BAND_NAME, sourceProduct, targetProduct, true);

        ProductUtils.copyGeoCoding(sourceProduct, targetProduct);
    }

    private void addBandsToOutput(String description, boolean convertMerisName) {
        for (Band inBand : inputBandList) {
            String newBandName;
            String bandUnit;
            if (convertMerisName) {
                newBandName = convertMerisBandName(inBand.getName(), bandNameMapping);
                bandUnit = "dl";
            } else {
                newBandName = inBand.getName();
                bandUnit = inBand.getUnit();
            }
            Band outBand = new Band(newBandName,
                                    inBand.getGeophysicalDataType(),
                                    inBand.getSceneRasterWidth(),
                                    inBand.getSceneRasterHeight());
            outBand.setUnit(bandUnit);
            outBand.setDescription(description + inBand.getName());
            ProductUtils.copySpectralBandProperties(inBand, outBand);
            targetProduct.addBand(outBand);
        }
    }

    // Helper Routine. Converts a given MERIS L1b band name (radiance_x) to the band name needed in the output product
    // (reflectance_x).
    static String convertMerisBandName(String bandName, Map<String, String> bandNameMapping) {
        String outBandName = merisBandPrefix;
        int blankIndex = bandName.indexOf('_');
        if (blankIndex > 0) {
            outBandName += bandName.substring(blankIndex, bandName.length());
        }
        bandNameMapping.put(outBandName, bandName);
        return outBandName;
    }

    // Helper Routine, inverse to convertMerisBandName
    static String revertMerisBandName(String targetBandName, Map<String, String> targetToSourceMapping) {
        for (String targetName : targetToSourceMapping.keySet()) {
            if (targetName.equals(targetBandName)) {
                return targetToSourceMapping.get(targetName);
            }
        }
        return targetBandName;
    }

    private void processMerisWithADS(Band spectralBand, SourceData sourceData, Tile targetTile, Rectangle targetRectangle, SmacAlgorithm algorithm) throws IOException {
        if (!setBandCoefficients(spectralBand.getName(), algorithm)) {
            logger.severe(String.format("Sensor coefficient file for spectral band '%s' not found!", spectralBand.getName()));
            return;
        }

        float[] toa = RsMathUtils.radianceToReflectance(sourceData.toa, sourceData.sza, spectralBand.getSolarFlux(), null);
        float[] press = RsMathUtils.simpleBarometric(sourceData.press, sourceData.elev, null);
        float[] uo3 = dobsonToCmAtm(sourceData.uo3);
        float[] uh2o = relativeHumidityTogcm2(sourceData.uh2o);

        Mask mask = sourceProduct.getMaskGroup().get(SMAC_MASK);
        int i = 0;
        for (int absY = targetRectangle.y; absY < targetRectangle.y + targetRectangle.height; absY++) {
            for (int absX = targetRectangle.x; absX < targetRectangle.x + targetRectangle.width; absX++) {
                sourceData.process[i] = mask.getSampleInt(absX, absY) != 0;
                i++;
            }
        }

        float[] toa_corr = new float[toa.length];
        toa_corr = algorithm.run(sourceData.sza, sourceData.saa, sourceData.vza, sourceData.vaa, sourceData.taup550,
                                 uh2o, uo3, press, sourceData.process, invalidPixel,
                                 toa, toa_corr);

        targetTile.setSamples(toa_corr);
    }

    // Processes MERIS data.
    private void processMeris(Band spectralBand, SourceData sourceData, Tile targetTile, Rectangle targetRectangle, SmacAlgorithm algorithm) throws IOException {
        if (!setBandCoefficients(spectralBand.getName(), algorithm)) {
            logger.severe("Sensor coefficient file for spectral band '" + spectralBand.getName() +
                          "' not found!");
            return;
        }

        float[] reflectances = RsMathUtils.radianceToReflectance(sourceData.toa, sourceData.sza, spectralBand.getSolarFlux(), null);

        Mask mask = sourceProduct.getMaskGroup().get(SMAC_MASK);
        int i = 0;
        for (int absY = targetRectangle.y; absY < targetRectangle.y + targetRectangle.height; absY++) {
            for (int absX = targetRectangle.x; absX < targetRectangle.x + targetRectangle.width; absX++) {
                sourceData.process[i] = mask.getSampleInt(absX, absY) != 0;
                i++;
            }
        }

        float[] toa_corr = new float[reflectances.length];
        toa_corr = algorithm.run(sourceData.sza, sourceData.saa, sourceData.vza, sourceData.vaa, sourceData.taup550,
                                 sourceData.uh2o, sourceData.uo3, sourceData.press, sourceData.process, invalidPixel,
                                 reflectances, toa_corr);

        targetTile.setSamples(toa_corr);
    }

    // Processes a single AATSR band.
    private void processAatsr(String bandName, SourceData sourceData, Tile targetTile, Rectangle targetRectangle, SmacAlgorithm algorithm) throws IOException {
        if (!setBandCoefficients(bandName, algorithm)) {
            logger.severe("Sensor coefficient file for spectral band '" + bandName +
                          "' not found!");
            return;
        }

        boolean isForwardBand = bandName.contains("fward");

        // set the tie point bands and mask according to input band view
        // and scale sun and view elevation to zenith angles
        Mask mask;
        float[] vza;
        float[] sza;
        if (isForwardBand) {
            vza = RsMathUtils.elevationToZenith(sourceData.vzaFwd, null);
            sza = RsMathUtils.elevationToZenith(sourceData.szaFwd, null);
            mask = sourceProduct.getMaskGroup().get(SMAC_MASK_FORWARD);
        } else {
            vza = RsMathUtils.elevationToZenith(sourceData.vza, null);
            sza = RsMathUtils.elevationToZenith(sourceData.sza, null);
            mask = sourceProduct.getMaskGroup().get(SMAC_MASK);
        }

        int i = 0;
        for (int absY = targetRectangle.y; absY < targetRectangle.y + targetRectangle.height; absY++) {
            for (int absX = targetRectangle.x; absX < targetRectangle.x + targetRectangle.width; absX++) {
                sourceData.process[i] = mask.getSampleInt(absX, absY) != 0;
                i++;
            }
        }

        float[] toa_corr = new float[sourceData.toa.length];
        toa_corr = algorithm.run(sourceData.sza, sourceData.saa, vza, sza, sourceData.taup550,
                                 sourceData.uh2o, sourceData.uo3, sourceData.press, sourceData.process, invalidPixel,
                                 sourceData.toa, toa_corr);

        targetTile.setSamples(toa_corr);
    }


    private boolean setBandCoefficients(String bandName, SmacAlgorithm algorithm) {
        if (coefficients.containsKey(bandName)) {
            algorithm.setSensorCoefficients(coefficients.get(bandName));
            return true;
        }
        URL url;
        SensorCoefficientFile coeff = new SensorCoefficientFile();
        boolean handleError = false;
        boolean success = false;

        try {
            url = getSensorCoefficientManager().getCoefficientFile(sensorType, bandName, aerosolType);
            if (url == null) {
                handleError = true;
            } else {
                coeff.readFile(new File(url.toURI()).getAbsolutePath());
                logger.info("Loaded sensor coefficient file " + url.getFile());
                algorithm.setSensorCoefficients(coeff);
                coefficients.put(bandName, coeff);
                success = true;
            }
        } catch (IOException e) {
            handleError = true;
            logger.severe(e.getMessage());
        } catch (URISyntaxException e) {
            handleError = true;
            logger.severe(e.getMessage());
        }

        if (handleError) {
            logger.severe("Unable to load sensor coefficients for band " + bandName);
        }

        return success;
    }

    private SensorCoefficientManager getSensorCoefficientManager() {
        File smacAuxDir = auxdataInstallDir;
        String auxPathString = smacAuxDir.toString();
        SensorCoefficientManager coeffMgr = null;
        try {
            coeffMgr = new SensorCoefficientManager(smacAuxDir.toURI().toURL());
            logger.fine("Using auxiliary data path: " + auxPathString);
        } catch (IOException e) {
            logger.severe("Error reading coefficients from: " + auxPathString);
            logger.severe(e.getMessage());
            logger.log(Level.FINE, e.getMessage(), e);
        }
        return coeffMgr;
    }

    // Converts an array of ozone contents in DU to cm *atm
    private static float[] dobsonToCmAtm(float[] du) {
        Assert.notNull(du, "du");
        for (int n = 0; n < du.length; n++) {
            du[n] = du[n] * duToCmAtm;
        }
        return du;
    }

    // Converts an array of relative humidity values (in %) to water vapour content in g/cm^2. This method uses a simple
    // linear relation without plausibility checks
    private static float[] relativeHumidityTogcm2(float[] relHum) {
        Assert.notNull(relHum, "relHum");

        for (int n = 0; n < relHum.length; n++) {
            relHum[n] = relHumTogcm * relHum[n];
        }

        return relHum;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SmacOperator.class);
        }

    }

    private class SourceData {

        float[] sza;
        float[] saa;
        float[] vza;
        float[] vaa;
        float[] szaFwd;
        float[] saaFwd;
        float[] vzaFwd;
        float[] vaaFwd;
        float[] uh2o;
        float[] uo3;
        float[] press;
        float[] elev;
        float[] taup550;
        boolean[] process;
        float[] toa;
    }
}
