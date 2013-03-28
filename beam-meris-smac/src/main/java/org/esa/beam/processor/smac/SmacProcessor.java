/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.processor.smac;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.jexp.Term;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestElementFactory;
import org.esa.beam.framework.processor.ui.ProcessorUI;
import org.esa.beam.processor.smac.ui.SmacRequestEditor;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ObjectUtils;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.math.RsMathUtils;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.logging.Logger;

/**
 * This is the main class for the SMAC scientific module.
 *
 * @deprecated since BEAM 4.11. No replacement.
 */
@Deprecated
public class SmacProcessor extends Processor {

    public static final String PROCESSOR_NAME = "BEAM SMAC Processor";
    private static final String PROCESSOR_SYMBOLIC_NAME = "beam-meris-smac";
    private static final String _merisBandPrefix = "reflec";  // was "reflectance" in version 1.0 - 1.2
    private static final String _version = "1.5.203";
    private static final String _copyright = "Copyright (C) 2002-2007 by Brockmann Consult (info@brockmann-consult.de)";
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

    private ArrayList<Band> _inputBandList;
    private Product _inputProduct;
    private Product _outputProduct;
    private String _bitMaskExpression;
    private String _bitMaskExpressionForward;
    private Term _bitMaskTerm;
    private Term _bitMaskTermForward;
    private TiePointGrid _szaBand;    // DELETE
    private TiePointGrid _szaFwdBand; // DELETE
    private TiePointGrid _saaBand;    // DELETE
    private TiePointGrid _saaFwdBand; // DELETE
    private TiePointGrid _vzaBand;    // DELETE
    private TiePointGrid _vzaFwdBand; // DELETE
    private TiePointGrid _vaaBand;    // DELETE
    private TiePointGrid _vaaFwdBand; // DELETE
    private TiePointGrid _wvBand;     // DELETE
    private TiePointGrid _o3Band; // DELETE
    private TiePointGrid _pressBand;  // DELETE
    private TiePointGrid _elevBand;

    private boolean _useMerisADS;
    private float _tau_aero_550;
    private float _u_h2o;
    private float _u_o3;
    private float _surf_press;
    private float _invalidPixel;
    private String _aerosolType;
    private String _sensorType;

    private SmacAlgorithm _algorithm;
    private SmacRequestEditor _editor;
    private SensorCoefficientManager _coeffMgr;

    private Logger _logger;
    public static final String HELP_ID = "smacScientificTool";


    /**
     * Constructs the object with default parameters.
     */
    public SmacProcessor() {
        _inputBandList = new ArrayList<Band>();
        _algorithm = new SmacAlgorithm();
        _bitMaskExpression = null;
        _logger = Logger.getLogger(SmacConstants.LOGGER_NAME);
        setDefaultHelpId(HELP_ID);
    }


    /**
     * Processes the request actually set.
     *
     * @param pm a monitor to inform the user about progress
     *
     * @throws org.esa.beam.framework.processor.ProcessorException
     *          on any failure during processing
     */
    @Override
    public void process(ProgressMonitor pm) throws ProcessorException {
        try {
            prepareProcessing();
            pm.beginTask("Creating output product...", _inputBandList.size() + 1);
            try {

                createOutputProduct(SubProgressMonitor.create(pm, 1));
                if (pm.isCanceled()) {
                    setCurrentStatus(ProcessorConstants.STATUS_ABORTED);
                    return;
                }

                // loop over all bands specified in request
                // ----------------------------------------
                pm.setSubTaskName(SmacConstants.LOG_MSG_PROCESSING_CORRECTION);
                for (int i = 0; i < _inputBandList.size(); i++) {
                    try {
                        // here we use a try/catch for each band processed, so that the processor does not completely quit
                        // when something goes wrong while processing on of many bands
                        // And we definitely decide which product type and dispacth appropriate.
                        if (ObjectUtils.equalObjects(_sensorType, SensorCoefficientManager.MERIS_NAME)) {
                            if (_useMerisADS) {
                                processMerisBandWithADS(_inputBandList.get(i), SubProgressMonitor.create(pm, 1));
                            } else {
                                processMerisBand(_inputBandList.get(i), SubProgressMonitor.create(pm, 1));
                            }
                        } else if (ObjectUtils.equalObjects(_sensorType, SensorCoefficientManager.AATSR_NAME)) {
                            processAatsrBand(_inputBandList.get(i), SubProgressMonitor.create(pm, 1));
                        } else {
                            pm.worked(1);
                        }

                        if (pm.isCanceled()) {
                            setCurrentStatus(ProcessorConstants.STATUS_ABORTED);
                            return;
                        }
                    } catch (IOException e) {
                        _logger.severe(ProcessorConstants.LOG_MSG_PROC_ERROR);
                        _logger.severe(e.getMessage());
                    }
                }
            } finally {
                cleanUp();
                pm.done();
            }
        } catch (IOException e) {
            throw new ProcessorException("An I/O error occurred:\n" + e.getMessage(), e);
        }

        _logger.info(ProcessorConstants.LOG_MSG_FINISHED_REQUEST);
    }

    /**
     * Creates the UI for the processor.
     */
    @Override
    public ProcessorUI createUI() {
        _editor = new SmacRequestEditor();
        return _editor;
    }

    /**
     * Retrieves the request element factory for the SMAC processor
     */
    @Override
    public RequestElementFactory getRequestElementFactory() {
        return SmacRequestElementFactory.getInstance();
    }

    /**
     * Retrieves the titlestring to be shown in the user interface.
     */
    @Override
    public String getUITitle() {
        return PROCESSOR_NAME;
    }

    /**
     * Retrieves the name of the processor
     */
    @Override
    public String getName() {
        return PROCESSOR_NAME;
    }

    /**
     * Returns the symbolic name of the processor.
     */
    @Override
    public String getSymbolicName() {
        return PROCESSOR_SYMBOLIC_NAME;
    }

    /**
     * Retrieves a version string of the processor
     */
    @Override
    public String getVersion() {
        return _version;
    }

    /**
     * Retrieves copyright information of the processor
     */
    @Override
    public String getCopyrightInformation() {
        return _copyright;
    }

    /**
     * Writes the processor specific logging file header to the logstream currently set.
     */
    @Override
    public void logHeader() {
        if (_editor != null) {
            _logger.info(SmacConstants.LOG_MSG_HEADER + _version);
            _logger.info(_copyright);
            _logger.info("");
        }
    }

    /**
     * Retrieves a progress message for the request passed in. Override this method if you need custom messaging.
     *
     * @param request the request
     *
     * @return the progress message for the request
     */
    @Override
    public String getProgressMessage(final Request request) {
        return SmacConstants.LOG_MSG_PROCESSING_CORRECTION;
    }

    /**
     * Retrieves the number of progressbars needed by the processor. Override this method if more than one progressbar
     * is needed, i.e. for multistage processes.
     *
     * @return the number og progressbars needed.
     */
    @Override
    public int getProgressDepth() {
        return 2;
    }

    @Override
    public void installAuxdata() throws ProcessorException {
        setAuxdataInstallDir(SmacConstants.SMAC_AUXDATA_DIR_PROPERTY, getDefaultAuxdataInstallDir());
        super.installAuxdata();
    }

    ///////////////////////////////////////////////////////////////////////////
    //////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////


    // Creates a bitmask expression depending on the processor type. For AATSR, two bitmask expressions are needed
    // depending on the view direction
    private void createBitmaskExpression() throws ProcessorException {
        if (ObjectUtils.equalObjects(_sensorType, SensorCoefficientManager.MERIS_NAME)) {
            createMerisBitmaskTerm();
        } else {
            createAatsrBitmaskTerm();
        }
    }

    // Creates an AATSR bitmask term given the bitmask expression from the request. If no expression is set, it uses the
    // default expression
    private void createAatsrBitmaskTerm() throws ProcessorException {
        if ("".equalsIgnoreCase(_bitMaskExpression)) {
            _bitMaskTerm = ProcessorUtils.createTerm(SmacConstants.DEFAULT_NADIR_FLAGS_VALUE, _inputProduct);
            _bitMaskTermForward = ProcessorUtils.createTerm(SmacConstants.DEFAULT_FORWARD_FLAGS_VALUE,
                                                            _inputProduct);

            _logger.warning(SmacConstants.LOG_MSG_NO_BITMASK);
            _logger.warning(SmacConstants.LOG_MSG_DEFAULT_NADIR_BITMASK + SmacConstants.DEFAULT_NADIR_FLAGS_VALUE);
            _logger.warning(SmacConstants.LOG_MSG_DEFAULT_FORWARD_BITMASK + SmacConstants.DEFAULT_FORWARD_FLAGS_VALUE);
        } else {
            _bitMaskTerm = ProcessorUtils.createTerm(_bitMaskExpression, _inputProduct);
            _bitMaskTermForward = ProcessorUtils.createTerm(_bitMaskExpressionForward, _inputProduct);

            _logger.info(SmacConstants.LOG_MSG_NADIR_BITMASK + _bitMaskExpression);
            _logger.info(SmacConstants.LOG_MSG_FORWARD_BITMASK + _bitMaskExpressionForward);
        }
    }

    // Creates a MERIS bitmask term given the bitmask expression from the request. If no expression is set, it uses the
    // default expression
    private void createMerisBitmaskTerm() throws ProcessorException {
        if ("".equalsIgnoreCase(_bitMaskExpression)) {
            _bitMaskTerm = ProcessorUtils.createTerm(SmacConstants.DEFAULT_MERIS_FLAGS_VALUE, _inputProduct);

            _logger.warning(SmacConstants.LOG_MSG_NO_BITMASK);
            _logger.warning(SmacConstants.LOG_MSG_DEFAULT_BITMASK + SmacConstants.DEFAULT_MERIS_FLAGS_VALUE);
        } else {
            _bitMaskTerm = ProcessorUtils.createTerm(_bitMaskExpression, _inputProduct);

            _logger.info(SmacConstants.LOG_MSG_BITMASK + _bitMaskExpression);
        }
    }


    // Creates the appropriate <code>Product</code> for the current request and assembles a list of <code>RsBands</code>
    // to be processed. This method does NOT load the tie point ADS because these are product specific.
    private void loadInputProduct() throws IOException,
                                           ProcessorException {
        Request request = getRequest();
        Band band;
        Parameter bandParam;
        String[] bandNames;

        // clear vector of bands
        // ---------------------
        _inputBandList.clear();

        // only the first product - there might be more but these will be ignored
        // ----------------------------------------------------------------------
        _inputProduct = loadInputProduct(0);

        // check what product type the input is and load the appropriate tie point ADS
        // ---------------------------------------------------------------------------
        _sensorType = SmacUtils.getSensorType(_inputProduct.getProductType());
        if (ObjectUtils.equalObjects(_sensorType, SensorCoefficientManager.MERIS_NAME)) {
            loadMerisBitmaskExpression();
            loadMERIS_ADS(_inputProduct);
        } else if (ObjectUtils.equalObjects(_sensorType, SensorCoefficientManager.AATSR_NAME)) {
            loadAatsrBitmaskExpression();
            loadAATSR_ADS(_inputProduct);
            _useMerisADS = false;   // then we must set this to false anyway
        } else {
            throw new ProcessorException(SmacConstants.LOG_MSG_UNSUPPORTED_SENSOR);
        }

        // set up the bands we need for this request
        // -----------------------------------------
        bandParam = request.getParameter(SmacConstants.BANDS_PARAM_NAME);
        checkParamNotNull(bandParam, "bands");
        bandNames = (String[]) bandParam.getValue();

        if ((bandNames == null) || (bandNames.length < 1)) {
            throw new ProcessorException(SmacConstants.LOG_MSG_NO_INPUT_BANDS);
        }

        for (String bandName : bandNames) {
            band = _inputProduct.getBand(bandName);
            if (band == null) {
                _logger.warning("The requested band '" + bandName + "' is not contained in the input product!");
            } else {
                if (band.getSpectralBandIndex() != -1) {
                    _inputBandList.add(band);
                } else {
                    _logger.warning(
                            "The requested band '" + bandName + "' is not a spectral band! It is excluded from processing");
                }
            }
        }


    }

    /**
     * Loads the bitmask expression for a MERIS product from the request
     */
    private void loadMerisBitmaskExpression() {
        Parameter param = getRequest().getParameter(SmacConstants.BITMASK_PARAM_NAME);
        if (param != null) {
            _bitMaskExpression = param.getValueAsText();
        } else {
            _bitMaskExpression = "";
        }
    }

    /**
     * Loads the bitmask expression for an AATSR product from the request
     */
    private void loadAatsrBitmaskExpression() {
        Request request = getRequest();
        Parameter paramNadir = request.getParameter(SmacConstants.BITMASK_NADIR_PARAM_NAME);
        if (paramNadir != null) {
            _bitMaskExpression = paramNadir.getValueAsText();
        } else {
            _bitMaskExpression = "";
        }

        Parameter paramForward = request.getParameter(SmacConstants.BITMASK_FORWARD_PARAM_NAME);
        if (paramForward != null) {
            _bitMaskExpressionForward = paramForward.getValueAsText();
        } else {
            _bitMaskExpressionForward = "";
        }
    }

    // Loads the MERIS ADS needed.
    private void loadMERIS_ADS(Product product) throws ProcessorException {
        Parameter useMeris;

        useMeris = getRequest().getParameter(SmacConstants.USE_MERIS_ADS_PARAM_NAME);
        checkParamNotNull(useMeris, SmacConstants.USE_MERIS_ADS_PARAM_NAME);

        // load the mandatory ADS
        // ----------------------
        _logger.info(SmacConstants.LOG_MSG_LOAD_MERIS_ADS);

        // sun zenith angle
        _szaBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisSzaIndex]);
        checkParamNotNull(_szaBand, EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisSzaIndex]);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisSzaIndex]);

        // sun azimuth angle
        _saaBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisSaaIndex]);
        checkParamNotNull(_saaBand, EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisSaaIndex]);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisSaaIndex]);

        // view zenith angle
        _vzaBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisVzaIndex]);
        checkParamNotNull(_vzaBand, EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisVzaIndex]);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisVzaIndex]);

        // view azimuth angle
        _vaaBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisVaaIndex]);
        checkParamNotNull(_vaaBand, EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisVaaIndex]);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisVaaIndex]);

        // if requested load the optional MERIS ADS
        // ----------------------------------------
        if (_useMerisADS) {
            // waterVapour
            _wvBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisWvIndex]);
            checkParamNotNull(_wvBand, EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisWvIndex]);
            _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisWvIndex]);

            // ozone
            _o3Band = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisO3Index]);
            checkParamNotNull(_o3Band, EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisO3Index]);
            _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisO3Index]);

            // atmospheric pressure
            _pressBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisPressIndex]);
            checkParamNotNull(_pressBand, EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisPressIndex]);
            _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisPressIndex]);

            // digital elevation
            _elevBand = product.getTiePointGrid(EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisElevIndex]);
            checkParamNotNull(_pressBand, EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisElevIndex]);
            _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.MERIS_TIE_POINT_GRID_NAMES[_merisElevIndex]);
        }

        _logger.info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    // Loads the AATSR ADS needed.
    private void loadAATSR_ADS(Product product) throws ProcessorException {

        _logger.info(SmacConstants.LOG_MSG_LOAD_AATSR_ADS);

        // sun elevation angle nadir
        _szaBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSzaIndex]);
        checkParamNotNull(_szaBand, EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSzaIndex]);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSzaIndex]);

        // sun elevation angle forward
        _szaFwdBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSzaFwdIndex]);
        checkParamNotNull(_szaFwdBand, EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSzaFwdIndex]);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSzaFwdIndex]);

        // sun azimuth angle nadir
        _saaBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSaaIndex]);
        checkParamNotNull(_saaBand, EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSaaIndex]);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSaaIndex]);

        // sun azimuth angle forward
        _saaFwdBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSaaFwdIndex]);
        checkParamNotNull(_saaFwdBand, EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSaaFwdIndex]);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrSaaFwdIndex]);

        // view elevation angle nadir
        _vzaBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVzaIndex]);
        checkParamNotNull(_vzaBand, EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVzaIndex]);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVzaIndex]);

        // view elevation angle forward
        _vzaFwdBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVzaFwdIndex]);
        checkParamNotNull(_vzaFwdBand, EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVzaFwdIndex]);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVzaFwdIndex]);

        // view azimuth angle nadir
        _vaaBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVaaIndex]);
        checkParamNotNull(_vaaBand, EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVaaIndex]);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVaaIndex]);

        // view azimuth angle forward
        _vaaFwdBand = product.getTiePointGrid(EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVaaFwdIndex]);
        checkParamNotNull(_vaaFwdBand, EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVaaFwdIndex]);
        _logger.fine(SmacConstants.LOG_MSG_LOADED + EnvisatConstants.AATSR_TIE_POINT_GRID_NAMES[_aatsrVaaFwdIndex]);

        _logger.info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    // Creates the output product for the given request.
    private void createOutputProduct(ProgressMonitor pm) throws IOException,
                                                                ProcessorException {

        // take only the first output product. There might be more but we will ignore
        // these in SMAC.
        ProductRef prod = getRequest().getOutputProductAt(0);
        checkParamNotNull(prod, "output product");

        String productType = _inputProduct.getProductType() + "_SMAC";
        String productName = getOutputProductNameSafe();
        int sceneWidth = _inputProduct.getSceneRasterWidth();
        int sceneHeight = _inputProduct.getSceneRasterHeight();
        _outputProduct = new Product(productName, productType, sceneWidth, sceneHeight);

        ProductWriter writer = ProcessorUtils.createProductWriter(prod);
        _outputProduct.setProductWriter(writer);

        // loop over bands and create them
        // -------------------------------
        if (ObjectUtils.equalObjects(_sensorType, SensorCoefficientManager.MERIS_NAME)) {
            addBandsToOutput("Atmosphere corrected MERIS band ", true);
        } else {
            addBandsToOutput("Atmosphere corrected band ");
        }
        ProductUtils.copyTiePointGrids(_inputProduct, _outputProduct);
        copyRequestMetaData(_outputProduct);
        copyFlagBands(_inputProduct, _outputProduct);

        // for MERIS FSG / FRG products
        copyBand(EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LATITUDE_BAND_NAME, _inputProduct, _outputProduct);
        copyBand(EnvisatConstants.MERIS_AMORGOS_L1B_CORR_LONGITUDE_BAND_NAME, _inputProduct, _outputProduct);
        copyBand(EnvisatConstants.MERIS_AMORGOS_L1B_ALTIUDE_BAND_NAME, _inputProduct, _outputProduct);

        copyGeoCoding(_inputProduct, _outputProduct);

        // and initialize the disk represenation
        writer.writeProductNodes(_outputProduct, new File(prod.getFilePath()));
        copyBandData(getBandNamesToCopy(), _inputProduct, _outputProduct, pm);
    }


    // Helper Routine. Converts a given MERIS L1b band name (radiance_x) to the band name needed in the output product
    // (reflectance_x).
    private static String convertMerisBandName(Band band) {
        String inBandName;
        String outBandName;
        inBandName = band.getName();

        outBandName = _merisBandPrefix;
        int blankIndex = inBandName.indexOf('_');
        if (blankIndex > 0) {
            outBandName += inBandName.substring(blankIndex, inBandName.length());
        }
        return outBandName;
    }

    /**
     * Adds requested bands to the output product.
     *
     * @param description a description template for the new bands to be created
     */
    private void addBandsToOutput(String description) {
        addBandsToOutput(description, false);
    }

    /**
     * Adds requested bands to the output product.
     *
     * @param description       a description template for the new bands to be created
     * @param bConvertMerisName it set to true, the MERIS l1b band name is converted from <i>radiance_n</i> to
     *                          <i>reflectance_n</i>
     */
    private void addBandsToOutput(String description, boolean bConvertMerisName) {

        for (Band inBand : _inputBandList) {
            String newBandName;
            String bandUnit;
            if (bConvertMerisName) {
                newBandName = convertMerisBandName(inBand);
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
            ProductUtils.copySpectralBandProperties(inBand, outBand);
            outBand.setDescription(description + inBand.getName());
            _outputProduct.addBand(outBand);
        }
    }

    // Loads the appropriate coefficient set for the band.
    private boolean loadBandCoefficients(Band band) {
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
                _algorithm.setSensorCoefficients(coeff);
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
            if (_editor != null) {
                _editor.showWarningDialog(SmacConstants.LOG_MSG_ERROR_COEFFICIENTS + band.getName());
            }
        }

        return bRet;
    }

    // Processes a single spectralBand of MERIS data.
    private void processMerisBand(Band spectralBand, ProgressMonitor pm) throws IOException {
        // load appropriate Sensor coefficientFile and init algorithm
        // ----------------------------------------------------------
        if (!loadBandCoefficients(spectralBand)) {
            _logger.severe(
                    SmacConstants.LOG_MSG_COEFF_NOT_FOUND_1 + spectralBand.getName() + SmacConstants.LOG_MSG_COEFF_NOT_FOUND_2);
            setCurrentStatus(ProcessorConstants.STATUS_FAILED);
            return;
        }

        _logger.info(
                SmacConstants.LOG_MSG_GENERATING_PIXEL_1 + spectralBand.getName() + SmacConstants.LOG_MSG_GENERATING_PIXEL_2); /*I18N*/

        // initialize vectors and other data
        int n;
        int width = spectralBand.getSceneRasterWidth();
        int height = spectralBand.getSceneRasterHeight();
        Band outBand = _outputProduct.getBand(convertMerisBandName(spectralBand));
        float[] sza = new float[width];
        float[] saa = new float[width];
        float[] vza = new float[width];
        float[] vaa = new float[width];
        float[] taup550 = new float[width];
        float[] uh2o = new float[width];
        float[] uo3 = new float[width];
        float[] press = new float[width];
        boolean[] process = new boolean[width];
        float[] toa = new float[width];
        float[] toa_corr = new float[width];

        for (n = 0; n < width; n++) {
            taup550[n] = _tau_aero_550;
            uh2o[n] = _u_h2o;
            uo3[n] = _u_o3;
            press[n] = _surf_press;
            process[n] = true;
        }

        // progress init
        pm.beginTask(SmacConstants.LOG_MSG_GENERATING_PIXEL_1 + spectralBand.getName() +
                     SmacConstants.LOG_MSG_GENERATING_PIXEL_2,
                     height * 6);
        try {
            // loop over all scanlines
            for (int y = 0; y < spectralBand.getSceneRasterHeight(); y++) {
                // read scanline
                spectralBand.readPixels(0, y, width, 1, toa, SubProgressMonitor.create(pm, 1));
                _szaBand.readPixels(0, y, width, 1, sza, SubProgressMonitor.create(pm, 1));
                _saaBand.readPixels(0, y, width, 1, saa, SubProgressMonitor.create(pm, 1));
                _vzaBand.readPixels(0, y, width, 1, vza, SubProgressMonitor.create(pm, 1));
                _vaaBand.readPixels(0, y, width, 1, vaa, SubProgressMonitor.create(pm, 1));

                // scale radiances to reflectances
                toa = RsMathUtils.radianceToReflectance(toa, sza, spectralBand.getSolarFlux(), toa);

                // forEachPixel bitmask
                if (_bitMaskTerm != null) {
                    _inputProduct.readBitmask(0, y, width, 1, _bitMaskTerm, process, ProgressMonitor.NULL);
                }

                // process scanline
                toa_corr = _algorithm.run(sza, saa, vza, vaa, taup550, uh2o, uo3, press, process,
                                          _invalidPixel, toa, toa_corr);

                // write scanline
                outBand.writePixels(0, y, width, 1, toa_corr, ProgressMonitor.NULL);

                // update progressbar
                pm.worked(1);
                if (pm.isCanceled()) {
                    _logger.warning(ProcessorConstants.LOG_MSG_PROC_CANCELED);
                    setCurrentStatus(ProcessorConstants.STATUS_ABORTED);
                    return;
                }
            }
        } finally {
            pm.done();
        }

        _logger.info(ProcessorConstants.LOG_MSG_PROC_SUCCESS);
    }

    // Processes a single band MERIS data using the MERIS ADS.
    private void processMerisBandWithADS(Band band, ProgressMonitor pm) throws IOException {
        // load appropriate Sensor coefficientFile and init algorithm
        // ----------------------------------------------------------
        if (!loadBandCoefficients(band)) {
            _logger.severe(
                    SmacConstants.LOG_MSG_COEFF_NOT_FOUND_1 + band.getName() + SmacConstants.LOG_MSG_COEFF_NOT_FOUND_2); /*I18N*/
            setCurrentStatus(ProcessorConstants.STATUS_FAILED);
            return;
        }

        _logger.info(
                SmacConstants.LOG_MSG_GENERATING_PIXEL_1 + band.getName() + SmacConstants.LOG_MSG_GENERATING_PIXEL_2); /*I18N*/

        // initialize vectors and other data
        // ---------------------------------
        int width = band.getSceneRasterWidth();
        int height = band.getSceneRasterHeight();
        Band outBand = _outputProduct.getBand(convertMerisBandName(band));
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
        float[] toa = new float[width];
        float[] toa_corr = new float[width];

        // set up vector - this parameter is constant for the request
        for (int x = 0; x < width; x++) {
            taup550[x] = _tau_aero_550;
            process[x] = true;
        }

        // progress bar init
        // -----------------
        pm.beginTask(SmacConstants.LOG_MSG_GENERATING_PIXEL_1 + band.getName() +
                     SmacConstants.LOG_MSG_GENERATING_PIXEL_2, height * 10);
        try {
            // loop over all scanlines
            // -----------------------
            for (int y = 0; y < height; y++) {
                // read scanline
                // -------------
                band.readPixels(0, y, width, 1, toa, SubProgressMonitor.create(pm, 1));
                _szaBand.readPixels(0, y, width, 1, sza, SubProgressMonitor.create(pm, 1));
                _saaBand.readPixels(0, y, width, 1, saa, SubProgressMonitor.create(pm, 1));
                _vzaBand.readPixels(0, y, width, 1, vza, SubProgressMonitor.create(pm, 1));
                _vaaBand.readPixels(0, y, width, 1, vaa, SubProgressMonitor.create(pm, 1));
                _wvBand.readPixels(0, y, width, 1, uh2o, SubProgressMonitor.create(pm, 1));
                _o3Band.readPixels(0, y, width, 1, uo3, SubProgressMonitor.create(pm, 1));
                _pressBand.readPixels(0, y, width, 1, press, SubProgressMonitor.create(pm, 1));
                _elevBand.readPixels(0, y, width, 1, elev, SubProgressMonitor.create(pm, 1));

                // scale radiance to reflectance
                // -------------------------------
                toa = RsMathUtils.radianceToReflectance(toa, sza, band.getSolarFlux(), toa);

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
                if (_bitMaskTerm != null) {
                    _inputProduct.readBitmask(0, y, width, 1, _bitMaskTerm, process, ProgressMonitor.NULL);
                }

                // process scanline
                // ----------------
                toa_corr = _algorithm.run(sza, saa, vza, vaa, taup550, uh2o, uo3, press, process,
                                          _invalidPixel, toa, toa_corr);

                // write scanline
                // --------------
                outBand.writePixels(0, y, width, 1, toa_corr, ProgressMonitor.NULL);

                // update progressbar
                // ------------------
                pm.worked(1);
                if (pm.isCanceled()) {
                    _logger.warning(ProcessorConstants.LOG_MSG_PROC_CANCELED);
                    setCurrentStatus(ProcessorConstants.STATUS_ABORTED);
                    return;
                }
            }
        } finally {
            pm.done();
        }

        _logger.info(ProcessorConstants.LOG_MSG_PROC_SUCCESS);
    }

    private void prepareProcessing() throws ProcessorException, IOException {
        _logger.info(ProcessorConstants.LOG_MSG_START_REQUEST);

        loadRequestParams();

        // create a vector of input bands
        // ------------------------------
        loadInputProduct();

        // create a bitmask expression for input
        // -------------------------------------
        createBitmaskExpression();

        installAuxdata();
        File smacAuxDir = getAuxdataInstallDir();

        String auxPathString = smacAuxDir.toString();
        try {
            _coeffMgr = new SensorCoefficientManager(smacAuxDir.toURI().toURL());
            _logger.fine(SmacConstants.LOG_MSG_AUX_DIR + auxPathString);
        } catch (IOException e) {
            _logger.severe(SmacConstants.LOG_MSG_AUX_ERROR + auxPathString);
            _logger.severe(e.getMessage());
        }
    }

    // Processes a single AATSR band.
    private void processAatsrBand(Band band, ProgressMonitor pm) throws IOException {
        // load appropriate Sensor coefficientFile and init algorithm
        // ----------------------------------------------------------
        if (!loadBandCoefficients(band)) {
            _logger.severe(
                    SmacConstants.LOG_MSG_COEFF_NOT_FOUND_1 + band.getName() + SmacConstants.LOG_MSG_COEFF_NOT_FOUND_2);
            setCurrentStatus(ProcessorConstants.STATUS_FAILED);
            return;
        }

        _logger.info(
                SmacConstants.LOG_MSG_GENERATING_PIXEL_1 + band.getName() + SmacConstants.LOG_MSG_GENERATING_PIXEL_2);

        // initialize vectors and other data
        // ---------------------------------
        int width = band.getSceneRasterWidth();
        int height = band.getSceneRasterHeight();
        Band outBand = _outputProduct.getBand(band.getName());
        boolean isForwardBand = checkForAATSRForwardBand(band);
        float[] sza = new float[width];
        float[] saa = new float[width];
        float[] vza = new float[width];
        float[] vaa = new float[width];
        float[] taup550 = new float[width];
        float[] uh2o = new float[width];
        float[] uo3 = new float[width];
        float[] press = new float[width];
        boolean[] process = new boolean[width];
        float[] toa = new float[width];
        float[] toa_corr = new float[width];

        TiePointGrid szaBand;
        TiePointGrid saaBand;
        TiePointGrid vzaBand;
        TiePointGrid vaaBand;
        Term bitMask;

        // set the tie point bands according to input band view
        // ----------------------------------------------------
        if (isForwardBand) {
            szaBand = _szaFwdBand;
            saaBand = _saaFwdBand;
            vzaBand = _vzaFwdBand;
            vaaBand = _vaaFwdBand;
            bitMask = _bitMaskTermForward;
        } else {
            szaBand = _szaBand;
            saaBand = _saaBand;
            vzaBand = _vzaBand;
            vaaBand = _vaaBand;
            bitMask = _bitMaskTerm;
        }

        // initialize vectors
        // ------------------
        for (int x = 0; x < width; x++) {
            taup550[x] = _tau_aero_550;
            uh2o[x] = _u_h2o;
            uo3[x] = _u_o3;
            press[x] = _surf_press;
            process[x] = true;
        }

        // progress init
        pm.beginTask(SmacConstants.LOG_MSG_GENERATING_PIXEL_1 + band.getName() +
                     SmacConstants.LOG_MSG_GENERATING_PIXEL_2,
                     height * 6);
        try {
            // loop over all scanlines
            for (int y = 0; y < band.getSceneRasterHeight(); y++) {
                // read scanline
                band.readPixels(0, y, width, 1, toa, SubProgressMonitor.create(pm, 1));
                szaBand.readPixels(0, y, width, 1, sza, SubProgressMonitor.create(pm, 1));
                saaBand.readPixels(0, y, width, 1, saa, SubProgressMonitor.create(pm, 1));
                vzaBand.readPixels(0, y, width, 1, vza, SubProgressMonitor.create(pm, 1));
                vaaBand.readPixels(0, y, width, 1, vaa, SubProgressMonitor.create(pm, 1));

                // scale sun and view elevation to zenith angles
                sza = RsMathUtils.elevationToZenith(sza, sza);
                vza = RsMathUtils.elevationToZenith(vza, sza);

                // forEachPixel bitmask
                if (bitMask != null) {
                    _inputProduct.readBitmask(0, y, width, 1, bitMask, process, ProgressMonitor.NULL);
                }

                // process scanline
                toa_corr = _algorithm.run(sza, saa, vza, vaa, taup550, uh2o, uo3, press, process,
                                          _invalidPixel, toa, toa_corr);

                // write scanline
                outBand.writePixels(0, y, width, 1, toa_corr, ProgressMonitor.NULL);

                // update progress
                pm.worked(1);
                if (pm.isCanceled()) {
                    _logger.warning(ProcessorConstants.LOG_MSG_PROC_CANCELED);
                    setCurrentStatus(ProcessorConstants.STATUS_ABORTED);
                    return;
                }
            }
        } finally {
            pm.done();
        }

        _logger.info(ProcessorConstants.LOG_MSG_PROC_SUCCESS);
    }

    // Scans the current request for parameters needed by the algorithm. Sets these parameters to fileds of the class
    private void loadRequestParams() throws ProcessorException {
        Parameter param;


        ProcessorUtils.setProcessorLoggingHandler(SmacConstants.DEFAULT_LOG_PREFIX, getRequest(),
                                                  getName(), getVersion(), getCopyrightInformation());

        // get aerosol optical depth
        // DELETE
        param = getRequest().getParameter(SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME);
        checkParamNotNull(param, SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME);
        _tau_aero_550 = (Float) (param.getValue());
        // DELETE

        // check for MERIS ads flag
        // DELETE
        param = getRequest().getParameter(SmacConstants.USE_MERIS_ADS_PARAM_NAME);
        checkParamNotNull(param, SmacConstants.USE_MERIS_ADS_PARAM_NAME);
        _useMerisADS = (Boolean) param.getValue();
        // DELETE

        // load the other parameters only if needed
        // ----------------------------------------
        if (!_useMerisADS) {
            // DELETE
            // water vapour content
            param = getRequest().getParameter(SmacConstants.RELATIVE_HUMIDITY_PARAM_NAME);
            checkParamNotNull(param, SmacConstants.RELATIVE_HUMIDITY_PARAM_NAME);
            _u_h2o = (Float) (param.getValue());
            // DELETE

            // DELETE
            // ozone content
            param = getRequest().getParameter(SmacConstants.OZONE_CONTENT_PARAM_NAME);
            checkParamNotNull(param, SmacConstants.OZONE_CONTENT_PARAM_NAME);
            _u_o3 = (Float) (param.getValue());
            // DELETE

            // DELETE
            // surface pressure
            param = getRequest().getParameter(SmacConstants.SURFACE_AIR_PRESSURE_PARAM_NAME);
            checkParamNotNull(param, SmacConstants.SURFACE_AIR_PRESSURE_PARAM_NAME);
            _surf_press = (Float) (param.getValue());
            // DELETE
        }

        // DELETE
        // get aerosol type
        param = getRequest().getParameter(SmacConstants.AEROSOL_TYPE_PARAM_NAME);
        checkParamNotNull(param, SmacConstants.AEROSOL_TYPE_PARAM_NAME);
        setAerosolType((String) param.getValue());
        // DELETE

        // DELETE
        // get invalid pixel value
        param = getRequest().getParameter(SmacConstants.DEFAULT_REFLECT_FOR_INVALID_PIX_PARAM_NAME);
        if (param == null) {
            _logger.warning(ProcessorConstants.LOG_MSG_NO_INVALID_PIXEL);
            _logger.warning(ProcessorConstants.LOG_MSG_USING + "0.0");
            _invalidPixel = 0.f;
        } else {
            _invalidPixel = (Float) param.getValue();
        }
        // DELETE
    }

    // DELETE

    /**
     * Converts the aerosol type given by the request to a string that can be understood by the
     * <code>SensorCoefficientManager</code>.
     *
     * @param type the request type string
     *
     * @throws java.lang.IllegalArgumentException
     *          on unknown aerosol types
     */
    private void setAerosolType(String type) {
        if (ObjectUtils.equalObjects(SmacConstants.AER_TYPE_DESERT, type)) {
            _aerosolType = SensorCoefficientManager.AER_DES_NAME;
        } else if (ObjectUtils.equalObjects(SmacConstants.AER_TYPE_CONTINENTAL, type)) {
            _aerosolType = SensorCoefficientManager.AER_CONT_NAME;
        } else {
            throw new IllegalArgumentException(SmacConstants.LOG_MSG_INVALID_AEROSOL + type);
        }
    }
    // DELETE

    /**
     * Checks if the given band is an AATSR forward band.
     *
     * @param band the <code>Band</code> to be checked.
     *
     * @return true when the band is a forward band
     */
    private static boolean checkForAATSRForwardBand(Band band) {
        boolean bRet = false;
        int bandIndex;

        for (int _aatsrMDSIndice : _aatsrMDSIndices) {
            bandIndex = _aatsrMDSIndice;
            if (ObjectUtils.equalObjects(band.getName(), EnvisatConstants.AATSR_L1B_BAND_NAMES[bandIndex])) {
                bRet = true;
                break;
            }
        }

        return bRet;
    }


    // Converts an array of ozone contents in DU to cm *atm
    private static float[] dobsonToCmAtm(float[] du) {
        Guardian.assertNotNull("du", du);

        for (int n = 0; n < du.length; n++) {
            du[n] = du[n] * _duToCmAtm;
        }

        return du;
    }


    // Converts an array of relative humidity values (in %) to water vapour content in g/cm^2. This method uses a simple
    // linear relation without plausibility checks
    private static float[] relativeHumidityTogcm2(float[] relHum) {
        Guardian.assertNotNull("relHum", relHum);

        for (int n = 0; n < relHum.length; n++) {
            relHum[n] = _relHumTogcm * relHum[n];
        }

        return relHum;
    }

    private void cleanUp() throws IOException {
        if (_inputProduct != null) {
            _inputProduct.dispose();
            _inputProduct = null;
        }
        if (_outputProduct != null) {
            if (isAborted()) {
                _outputProduct.getProductWriter().deleteOutput();
            }
            _outputProduct.dispose();
            _outputProduct = null;
        }
    }

    @Override
    protected void cleanupAfterFailure() {
        try {
            cleanUp();
        } catch (IOException e) {
            _logger.severe(e.getMessage());
        }
    }

    // Retrieves the output product name from request
    private String getOutputProductNameSafe() throws ProcessorException {
        Request request = getRequest();
        ProductRef prod = request.getOutputProductAt(0);
        if (prod == null) {
            throw new ProcessorException(ProcessorConstants.LOG_MSG_NO_OUTPUT_IN_REQUEST);
        }
        File prodFile = new File(prod.getFilePath());

        return FileUtils.getFilenameWithoutExtension(prodFile);
    }
}




