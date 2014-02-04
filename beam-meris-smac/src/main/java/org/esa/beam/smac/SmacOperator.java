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
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.processor.smac.SensorCoefficientFile;
import org.esa.beam.processor.smac.SensorCoefficientManager;
import org.esa.beam.processor.smac.SmacAlgorithm;
import org.esa.beam.processor.smac.SmacConstants;
import org.esa.beam.processor.smac.SmacUtils;
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
                  authors = "H. Rahman, G. Dedieu (Algorithm), Tom Block (BEAM Implementation)",
                  copyright = "Copyright (C) 2002-2014 by Brockmann Consult (info@brockmann-consult.de)",
                  description = "Applies the simplified method for atmospheric corrections of satellite measurements.")
public class SmacOperator extends Operator {

    private static final String PROCESSOR_SYMBOLIC_NAME = "beam-meris-smac";

    private static final int _merisSzaIndex = 6;    // DELETE
    private static final int _merisSaaIndex = 7;    // DELETE
    private static final int _merisVzaIndex = 8;    // DELETE
    private static final int _merisVaaIndex = 9;    // DELETE
    private static final int _merisPressIndex = 12; // DELETE
    private static final int _merisElevIndex = 2;
    private static final int _merisO3Index = 13;    // DELETE
    private static final int _merisWvIndex = 14;    // DELETE
    private static final int _aatsrSzaIndex = 7;    // DELETE
    private static final int _aatsrSzaFwdIndex = 11;    // DELETE
    private static final int _aatsrSaaIndex = 9;    // DELETE
    private static final int _aatsrSaaFwdIndex = 13;    // DELETE
    private static final int _aatsrVzaIndex = 8;    // DELETE
    private static final int _aatsrVzaFwdIndex = 12;    // DELETE
    private static final int _aatsrVaaIndex = 10;   // DELETE
    private static final int _aatsrVaaFwdIndex = 14;    // DELETE
    private static final int[] _aatsrMDSIndices = {3, 4, 5, 6, 10, 11, 12, 13};
    private static final float _duToCmAtm = 0.001f;
    private static final float _relHumTogcm = 0.07f;

    private static final String _merisBandPrefix = "reflec";  // was "reflectance" in version 1.0 - 1.2
    private static final String SMAC_MASK = "smac_mask";
    private static final String SMAC_MASK_FORWARD = "smac_mask_forward";

    private final List<Band> _inputBandList;
    private SensorCoefficientManager _coeffMgr;
    private final Logger _logger;
    private String _sensorType;
    private File _auxdataInstallDir;
    private Map<String, String> bandNameMapping;

    private TiePointGrid _szaBand;
    private TiePointGrid _saaBand;
    private TiePointGrid _vzaBand;
    private TiePointGrid _vaaBand;
    private TiePointGrid _wvBand;
    private TiePointGrid _o3Band;
    private TiePointGrid _pressBand;
    private TiePointGrid _elevBand;
    private TiePointGrid _szaFwdBand;
    private TiePointGrid _saaFwdBand;
    private TiePointGrid _vzaFwdBand;
    private TiePointGrid _vaaFwdBand;


    @Parameter(description = "Aerosol optical depth")
    private Float _tau_aero_550 = 0.2F;

    @Parameter(description = "Relative humidity")
    private Float _u_h2o = 3.0F;

    @Parameter(description = "Ozone content")
    private Float _u_o3 = 0.15F;

    @Parameter(description = "Surface pressure")
    private Float _surf_press = 1013.0F;

    @Parameter(description = "Use MERIS ADS", notNull = true)
    private Boolean _useMerisADS;

    @Parameter(description = "Aerosol type", notNull = true, valueSet = {
            SensorCoefficientManager.AER_CONT_NAME,
            SensorCoefficientManager.AER_DES_NAME
    })
    private String _aerosolType;

    @Parameter(description = "Default reflectance for invalid pixel")
    Float _invalidPixel = 0.0F;

    @Parameter(description = "Mask expression for the whole view (MERIS) or the nadir view (AATSR)")
    private String _bitMaskExpression = "";

    @Parameter(description = "Mask expression for the forward view (AATSR only)")
    private String _bitMaskExpressionForward = "";

    @Parameter(description = "bands", notNull = true)
    private String[] bandNames;

    @SourceProduct(alias = "source")
    private Product sourceProduct;

    @TargetProduct
    private Product targetProduct;

    public SmacOperator() {
        _inputBandList = new ArrayList<>();
        _logger = getLogger();
        bandNameMapping = new HashMap<>();
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
        for (Map.Entry<Band, Tile> bandTileEntry : targetTiles.entrySet()) {
            Band sourceBand = sourceProduct.getBand(revertMerisBandName(bandTileEntry.getKey().getName(), bandNameMapping));
            sourceBand.setValidPixelExpression(""); // necessary in order to mimic the processor behavior
            Tile sourceTile = getSourceTile(sourceBand, targetRectangle);
            Tile targetTile = bandTileEntry.getValue();
            try {
                if (ObjectUtils.equalObjects(_sensorType, SensorCoefficientManager.MERIS_NAME)) {
                    if (_useMerisADS) {
                        processMerisBandWithADS(sourceBand, sourceTile, targetTile, targetRectangle, new SmacAlgorithm());
                    } else {
                        processMerisBand(sourceBand, sourceTile, targetTile, targetRectangle, new SmacAlgorithm());
                    }
                } else if (ObjectUtils.equalObjects(_sensorType, SensorCoefficientManager.AATSR_NAME)) {
                    processAatsrBand(sourceBand, sourceTile, targetTile, targetRectangle, new SmacAlgorithm());
                }
            } catch (IOException e) {
                _logger.severe(ProcessorConstants.LOG_MSG_PROC_ERROR);
                _logger.severe(e.getMessage());
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

    private void prepareProcessing() throws IOException {
        _logger.info("Preparing SMAC processing");

        // create a vector of input bands
        // ------------------------------
        loadInputProduct();

        // create a bitmask expression for input
        // -------------------------------------
        createMask();

        installAuxdata();
        File smacAuxDir = _auxdataInstallDir;

        String auxPathString = smacAuxDir.toString();
        try {
            _coeffMgr = new SensorCoefficientManager(smacAuxDir.toURI().toURL());
            _logger.fine(SmacConstants.LOG_MSG_AUX_DIR + auxPathString);
        } catch (IOException e) {
            _logger.severe(SmacConstants.LOG_MSG_AUX_ERROR + auxPathString);
            _logger.severe(e.getMessage());
            _logger.log(Level.FINE, e.getMessage(), e);
        }
    }

    private void loadInputProduct() throws IOException {
        // check what product type the input is and load the appropriate tie point ADS
        // ---------------------------------------------------------------------------
        _sensorType = SmacUtils.getSensorType(sourceProduct.getProductType());
        if (ObjectUtils.equalObjects(_sensorType, SensorCoefficientManager.MERIS_NAME)) {
            loadMERIS_ADS(sourceProduct);
        } else if (ObjectUtils.equalObjects(_sensorType, SensorCoefficientManager.AATSR_NAME)) {
            loadAATSR_ADS(sourceProduct);
            _useMerisADS = false;
        } else {
            throw new OperatorException(SmacConstants.LOG_MSG_UNSUPPORTED_SENSOR);
        }

        // set up the bands we need for this request
        // -----------------------------------------
        if (bandNames.length == 0) {
            throw new OperatorException(SmacConstants.LOG_MSG_NO_INPUT_BANDS);
        }

        for (String bandName : bandNames) {
            Band band = sourceProduct.getBand(bandName);
            if (band == null) {
                _logger.warning("The requested band '" + bandName + "' is not contained in the input product!");
            } else {
                if (band.getSpectralBandIndex() != -1) {
                    _inputBandList.add(band);
                } else {
                    _logger.warning(
                            "The requested band '" + bandName +
                            "' is not a spectral band and will be excluded from processing");
                }
            }
        }
    }

    private void loadMERIS_ADS(Product product) {
        _logger.info(SmacConstants.LOG_MSG_LOAD_MERIS_ADS);

        // sun zenith angle
        _szaBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisSzaIndex]);
        Assert.notNull(_szaBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisSzaIndex]);

        // sun azimuth angle
        _saaBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisSaaIndex]);
        Assert.notNull(_saaBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisSaaIndex]);

        // view zenith angle
        _vzaBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisVzaIndex]);
        Assert.notNull(_vzaBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisVzaIndex]);

        // view azimuth angle
        _vaaBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisVaaIndex]);
        Assert.notNull(_vaaBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisVaaIndex]);

        // if requested load the optional MERIS ADS
        // ----------------------------------------
        if (_useMerisADS) {
            // waterVapour
            _wvBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisWvIndex]);
            Assert.notNull(_wvBand);
            _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisWvIndex]);

            // ozone
            _o3Band = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisO3Index]);
            Assert.notNull(_o3Band);
            _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisO3Index]);

            // atmospheric pressure
            _pressBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisPressIndex]);
            Assert.notNull(_pressBand);
            _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisPressIndex]);

            // digital elevation
            _elevBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisElevIndex]);
            Assert.notNull(_elevBand);
            _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisElevIndex]);
        }
        _logger.info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    // Loads the AATSR ADS needed.
    private void loadAATSR_ADS(Product product) {

        _logger.info(SmacConstants.LOG_MSG_LOAD_AATSR_ADS);

        // sun elevation angle nadir
        _szaBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSzaIndex]);
        Assert.notNull(_szaBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSzaIndex]);

        // sun elevation angle forward
        _szaFwdBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSzaFwdIndex]);
        Assert.notNull(_szaFwdBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSzaFwdIndex]);

        // sun azimuth angle nadir
        _saaBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSaaIndex]);
        Assert.notNull(_saaBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSaaIndex]);

        // sun azimuth angle forward
        _saaFwdBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSaaFwdIndex]);
        Assert.notNull(_saaFwdBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSaaFwdIndex]);

        // view elevation angle nadir
        _vzaBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVzaIndex]);
        Assert.notNull(_vzaBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVzaIndex]);

        // view elevation angle forward
        _vzaFwdBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVzaFwdIndex]);
        Assert.notNull(_vzaFwdBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVzaFwdIndex]);

        // view azimuth angle nadir
        _vaaBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVaaIndex]);
        Assert.notNull(_vaaBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVaaIndex]);

        // view azimuth angle forward
        _vaaFwdBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVaaFwdIndex]);
        Assert.notNull(_vaaFwdBand);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVaaFwdIndex]);

        _logger.info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    /**
     * Replaces createBitmaskTerm()
     */
    private void createMask() {
        if (ObjectUtils.equalObjects(_sensorType, SensorCoefficientManager.MERIS_NAME)) {
            createMerisBitmaskTerm();
        } else {
            createAatsrBitmaskTerm();
        }
    }

    // Creates a MERIS bitmask term given the bitmask expression from the request. If no expression is set, it uses the
    // default expression
    private void createMerisBitmaskTerm() {
        if ("".equalsIgnoreCase(_bitMaskExpression)) {
            _bitMaskExpression = SmacConstants.DEFAULT_MERIS_FLAGS_VALUE;
            _logger.warning(SmacConstants.LOG_MSG_NO_BITMASK);
            _logger.warning(SmacConstants.LOG_MSG_DEFAULT_BITMASK + SmacConstants.DEFAULT_MERIS_FLAGS_VALUE);
        } else {
            _logger.info(SmacConstants.LOG_MSG_BITMASK + _bitMaskExpression);
        }
        Mask mask = sourceProduct.addMask(SMAC_MASK, _bitMaskExpression, "", Color.BLACK, 0.0);
        mask.setValidPixelExpression(_bitMaskExpression);
    }

    // Creates an AATSR bitmask term given the bitmask expression from the request. If no expression is set, it uses the
    // default expression
    private void createAatsrBitmaskTerm() {
        if ("".equalsIgnoreCase(_bitMaskExpression)) {
            sourceProduct.addMask(SMAC_MASK, SmacConstants.DEFAULT_NADIR_FLAGS_VALUE, "", Color.BLACK, 0.0);
            sourceProduct.addMask(SMAC_MASK_FORWARD, SmacConstants.DEFAULT_FORWARD_FLAGS_VALUE, "", Color.BLACK, 0.0);

            _logger.warning(SmacConstants.LOG_MSG_NO_BITMASK);
            _logger.warning(SmacConstants.LOG_MSG_DEFAULT_NADIR_BITMASK + SmacConstants.DEFAULT_NADIR_FLAGS_VALUE);
            _logger.warning(SmacConstants.LOG_MSG_DEFAULT_FORWARD_BITMASK + SmacConstants.DEFAULT_FORWARD_FLAGS_VALUE);
        } else {
            sourceProduct.addMask(SMAC_MASK, _bitMaskExpression, "", Color.BLACK, 0.0);
            sourceProduct.addMask(SMAC_MASK_FORWARD, _bitMaskExpressionForward, "", Color.BLACK, 0.0);

            _logger.info(SmacConstants.LOG_MSG_NADIR_BITMASK + _bitMaskExpression);
            _logger.info(SmacConstants.LOG_MSG_FORWARD_BITMASK + _bitMaskExpressionForward);
        }
    }

    private void installAuxdata() {
        setAuxdataInstallDir(SmacConstants.SMAC_AUXDATA_DIR_PROPERTY, getDefaultAuxdataInstallDir());
        try {
            installAuxdata(ResourceInstaller.getSourceUrl(getClass()), "auxdata/", _auxdataInstallDir);
        } catch (IOException e) {
            throw new OperatorException("Failed to install auxdata into " + _auxdataInstallDir, e);
        }
    }

    private void setAuxdataInstallDir(String auxdataDirPropertyName, File defaultAuxdataInstallDir) {
        Assert.argument(StringUtils.isNotNullAndNotEmpty(auxdataDirPropertyName), "auxdataDirPropertyName is not null and not empty");
        String auxdataDirPath = System.getProperty(auxdataDirPropertyName, defaultAuxdataInstallDir.getAbsolutePath());
        _auxdataInstallDir = new File(auxdataDirPath);
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
        targetProduct.setPreferredTileSize(targetProduct.getSceneRasterWidth(), 1);

        // loop over bands and create them
        // -------------------------------
        if (ObjectUtils.equalObjects(_sensorType, SensorCoefficientManager.MERIS_NAME)) {
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
        for (Band inBand : _inputBandList) {
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
        String outBandName = _merisBandPrefix;
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

    // Processes a single band MERIS data using the MERIS ADS.
    private void processMerisBandWithADS(Band sourceBand, Tile sourceTile, Tile targetTile, Rectangle targetRectangle, SmacAlgorithm algorithm) throws IOException {
        // load appropriate Sensor coefficientFile and init algorithm
        // ----------------------------------------------------------
        if (!loadBandCoefficients(sourceBand, algorithm)) {
            _logger.severe(
                    SmacConstants.LOG_MSG_COEFF_NOT_FOUND_1 + sourceBand.getName() +
                    SmacConstants.LOG_MSG_COEFF_NOT_FOUND_2);
            return;
        }

        _logger.info(
                SmacConstants.LOG_MSG_GENERATING_PIXEL_1 + sourceBand.getName() +
                SmacConstants.LOG_MSG_GENERATING_PIXEL_2);

        // initialize vectors and other data
        // ---------------------------------
        int width = targetRectangle.width;
        int height = targetRectangle.height;
        int x = targetRectangle.x;
        int y = targetRectangle.y;

        float[] sza = new float[width];
        float[] saa = new float[width];
        float[] vza = new float[width];
        float[] vaa = new float[width];
        float[] taup550 = new float[width];
        float[] uh2o = new float[width];
        float[] uo3 = new float[width];
        float[] press = new float[width];
        float[] elev = new float[width];
        boolean[] process = new boolean[width];
        float[] toa_corr = new float[width];
        float[] toa = sourceTile.getSamplesFloat();

        // set up vector - this parameter is constant for the request
        for (int i = 0; i < width; i++) {
            taup550[i] = _tau_aero_550;
            process[i] = true;
        }

        _szaBand.readPixels(x, y, width, height, sza);
        _saaBand.readPixels(x, y, width, height, saa);
        _vzaBand.readPixels(x, y, width, height, vza);
        _vaaBand.readPixels(x, y, width, height, vaa);
        _wvBand.readPixels(x, y, width, height, uh2o);
        _o3Band.readPixels(x, y, width, height, uo3);
        _pressBand.readPixels(x, y, width, height, press);
        _elevBand.readPixels(x, y, width, height, elev);

        // scale radiance to reflectance
        // -------------------------------
        toa = RsMathUtils.radianceToReflectance(toa, sza, sourceBand.getSolarFlux(), toa);

        // correct pressure due to elevation
        // ---------------------------------
        press = RsMathUtils.simpleBarometric(press, elev, press);

        // scale DU to cm * atm
        // ----------------------
        uo3 = dobsonToCmAtm(uo3);

        // scale relative humidity to g/cm^2
        // -----------------------------------
        uh2o = relativeHumidityTogcm2(uh2o);

        // forEachPixel bitmask
        // ----------------
        if (sourceProduct.getMaskGroup().get(SMAC_MASK) != null) {
            sourceProduct.getMaskGroup().get(SMAC_MASK).readValidMask(x, targetRectangle.y, targetRectangle.width, 1, process);
        }

        // process tile
        // ----------------
        toa_corr = algorithm.run(sza, saa, vza, vaa, taup550, uh2o, uo3, press, process,
                                 _invalidPixel, toa, toa_corr);

        // write scanline
        // --------------
        targetTile.setSamples(toa_corr);
        _logger.info(ProcessorConstants.LOG_MSG_PROC_SUCCESS);
    }

    // Processes a single spectralBand of MERIS data.
    private void processMerisBand(Band spectralBand, Tile sourceTile, Tile targetTile, Rectangle targetRectangle, SmacAlgorithm algorithm) throws IOException {

        // load appropriate Sensor coefficientFile and init algorithm
        // ----------------------------------------------------------
        if (!loadBandCoefficients(spectralBand, algorithm)) {
            _logger.severe(SmacConstants.LOG_MSG_COEFF_NOT_FOUND_1 + spectralBand.getName() +
                           SmacConstants.LOG_MSG_COEFF_NOT_FOUND_2);
            return;
        }

        _logger.info(SmacConstants.LOG_MSG_GENERATING_PIXEL_1 + spectralBand.getName() +
                     SmacConstants.LOG_MSG_GENERATING_PIXEL_2);

        // initialize vectors and other data
        int width = targetRectangle.width;
        int height = targetRectangle.height;
        int x = targetRectangle.x;
        int y = targetRectangle.y;

        float[] sza;
        float[] saa;
        float[] vza;
        float[] vaa;
        float[] taup550 = new float[width * height];
        float[] uh2o = new float[width * height];
        float[] uo3 = new float[width * height];
        float[] press = new float[width * height];
        boolean[] process = new boolean[width * height];
        float[] toa_corr = new float[width * height];
        float[] toa = new float[width * height];

        toa = sourceTile.getSamplesFloat();

        for (int n = 0; n < width; n++) {
            taup550[n] = _tau_aero_550;
            uh2o[n] = _u_h2o;
            uo3[n] = _u_o3;
            press[n] = _surf_press;
            process[n] = true;
        }

        Tile szaTile = getSourceTile(_szaBand, targetRectangle);
        Tile saaTile = getSourceTile(_saaBand, targetRectangle);
        Tile vzaTile = getSourceTile(_vzaBand, targetRectangle);
        Tile vaaTile = getSourceTile(_vaaBand, targetRectangle);

        sza = szaTile.getSamplesFloat();
        saa = saaTile.getSamplesFloat();
        vza = vzaTile.getSamplesFloat();
        vaa = vaaTile.getSamplesFloat();

        // scale radiances to reflectances
        toa = RsMathUtils.radianceToReflectance(toa, sza, spectralBand.getSolarFlux(), toa);


        // forEachPixel bitmask
        Mask mask = sourceProduct.getMaskGroup().get(SMAC_MASK);
        int i = 0;
        for (int absY = y; absY < targetRectangle.y + targetRectangle.height; absY++) {
            for (int absX = x; absX < targetRectangle.x + targetRectangle.width; absX++) {
                process[i] = mask.getSampleInt(absX, absY) == 255;
                i++;
            }
        }

        // process scanline
        toa_corr = algorithm.run(sza, saa, vza, vaa, taup550, uh2o, uo3, press, process,
                                 _invalidPixel, toa, toa_corr);

        // write scanline
        targetTile.setSamples(toa_corr);
        _logger.info(ProcessorConstants.LOG_MSG_PROC_SUCCESS);
    }

    // Processes a single AATSR band.
    private void processAatsrBand(Band band, Tile sourceTile, Tile targetTile, Rectangle targetRectangle, SmacAlgorithm algorithm) throws IOException {
        // load appropriate Sensor coefficientFile and init algorithm
        // ----------------------------------------------------------
        if (!loadBandCoefficients(band, algorithm)) {
            _logger.severe(
                    SmacConstants.LOG_MSG_COEFF_NOT_FOUND_1 + band.getName() + SmacConstants.LOG_MSG_COEFF_NOT_FOUND_2);
            return;
        }

        _logger.info(
                SmacConstants.LOG_MSG_GENERATING_PIXEL_1 + band.getName() + SmacConstants.LOG_MSG_GENERATING_PIXEL_2);

        // initialize vectors and other data
        // ---------------------------------
        int x = targetRectangle.x;
        int y = targetRectangle.y;
        int width = targetRectangle.width;
        int height = targetRectangle.height;
        boolean isForwardBand = checkForAATSRForwardBand(band);
        float[] sza = new float[width * height];
        float[] saa = new float[width * height];
        float[] vza = new float[width * height];
        float[] vaa = new float[width * height];
        float[] taup550 = new float[width * height];
        float[] uh2o = new float[width * height];
        float[] uo3 = new float[width * height];
        float[] press = new float[width * height];
        boolean[] process = new boolean[width * height];
        float[] toa_corr = new float[width * height];
        float[] toa = sourceTile.getSamplesFloat();

        TiePointGrid szaBand;
        TiePointGrid saaBand;
        TiePointGrid vzaBand;
        TiePointGrid vaaBand;
        Mask bitMask;

        // set the tie point bands according to input band view
        // ----------------------------------------------------
        if (isForwardBand) {
            szaBand = _szaFwdBand;
            saaBand = _saaFwdBand;
            vzaBand = _vzaFwdBand;
            vaaBand = _vaaFwdBand;
            bitMask = sourceProduct.getMaskGroup().get(SMAC_MASK);
        } else {
            szaBand = _szaBand;
            saaBand = _saaBand;
            vzaBand = _vzaBand;
            vaaBand = _vaaBand;
            bitMask = sourceProduct.getMaskGroup().get(SMAC_MASK_FORWARD);
        }

        // initialize vectors
        // ------------------
        for (int i = 0; i < width; i++) {
            taup550[i] = _tau_aero_550;
            uh2o[i] = _u_h2o;
            uo3[i] = _u_o3;
            press[i] = _surf_press;
            process[i] = true;
        }

        szaBand.readPixels(x, y, width, height, sza);
        saaBand.readPixels(x, y, width, height, saa);
        vzaBand.readPixels(x, y, width, height, vza);
        vaaBand.readPixels(x, y, width, height, vaa);

        // scale sun and view elevation to zenith angles
        sza = RsMathUtils.elevationToZenith(sza, sza);
        vza = RsMathUtils.elevationToZenith(vza, sza);

        // forEachPixel bitmask
        if (bitMask != null) {
            bitMask.readValidMask(x, y, width, height, process);
        }

        // process scanline
        toa_corr = algorithm.run(sza, saa, vza, vaa, taup550, uh2o, uo3, press, process,
                                 _invalidPixel, toa, toa_corr);

        // write scanline
        targetTile.setSamples(toa_corr);

        _logger.info(ProcessorConstants.LOG_MSG_PROC_SUCCESS);
    }

    private boolean loadBandCoefficients(Band band, SmacAlgorithm algorithm) {
        boolean bRet = false;
        URL url;
        SensorCoefficientFile coeff = new SensorCoefficientFile();
        boolean handleError = false;

        try {
            url = _coeffMgr.getCoefficientFile(_sensorType, band.getName(), _aerosolType);
            if (url == null) {
                handleError = true;
            } else {
                coeff.readFile(new File(url.toURI()).getAbsolutePath());
                _logger.info(SmacConstants.LOG_MSG_LOADED_COEFFICIENTS + url.getFile());
                algorithm.setSensorCoefficients(coeff);
                bRet = true;
            }
        } catch (IOException e) {
            handleError = true;
            _logger.severe(e.getMessage());
        } catch (URISyntaxException e) {
            handleError = true;
            _logger.severe(e.getMessage());
        }

        if (handleError) {
            _logger.severe(SmacConstants.LOG_MSG_ERROR_COEFFICIENTS + band.getName());
        }

        return bRet;
    }

    // Converts an array of ozone contents in DU to cm *atm
    private static float[] dobsonToCmAtm(float[] du) {
        Assert.notNull(du, "du");
        for (int n = 0; n < du.length; n++) {
            du[n] = du[n] * _duToCmAtm;
        }
        return du;
    }

    // Converts an array of relative humidity values (in %) to water vapour content in g/cm^2. This method uses a simple
    // linear relation without plausibility checks
    private static float[] relativeHumidityTogcm2(float[] relHum) {
        Assert.notNull(relHum, "relHum");

        for (int n = 0; n < relHum.length; n++) {
            relHum[n] = _relHumTogcm * relHum[n];
        }

        return relHum;
    }

    /**
     * Checks if the given band is an AATSR forward band.
     *
     * @param band the <code>Band</code> to be checked.
     *
     * @return true when the band is a forward band
     */
    private static boolean checkForAATSRForwardBand(Band band) {
        for (int aatsrMDSIndex : _aatsrMDSIndices) {
            if (ObjectUtils.equalObjects(band.getName(), EnvisatConstants.AATSR_L1B_BAND_NAMES[aatsrMDSIndex])) {
                return true;
            }
        }
        return false;
    }

    public static class Spi extends OperatorSpi {

        public Spi() {
            super(SmacOperator.class);
        }

    }

}
