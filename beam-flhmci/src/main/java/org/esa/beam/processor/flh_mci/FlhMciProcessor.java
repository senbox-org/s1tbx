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
package org.esa.beam.processor.flh_mci;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.jexp.Term;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestElementFactory;
import org.esa.beam.framework.processor.ui.ProcessorUI;
import org.esa.beam.processor.flh_mci.ui.FlhMciUI;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * The main class for the FLH_MCI processor.
 *
 * @deprecated since BEAM 4.10 - no replacement.
 */
@Deprecated
public final class FlhMciProcessor extends Processor {

    public static final String PROCESSOR_NAME = "BEAM FLH/MCI Processor";
    private static final String PROCESSOR_SYMBOLIC_NAME = "beam-flhmci";
    public static final String PROCESSOR_VERSION = "1.6.202";
    public static final String PROCESSOR_COPYRIGHT = "Copyright (C) 2002-2004 by Brockmann Consult (info@brockmann-consult.de)";

    private Product _inputProduct;
    private Product _outputProduct;

    private float _wavelengthLow;
    private float _wavelengthSignal;
    private float _wavelengthHigh;
    private Band _lowBand;
    private Band _signalBand;
    private Band _highBand;

    private String _lineheightBandName;
    private Band _lineheightBand;
    private String _slopeBandName;
    private Band _slopeBand;
    private boolean _processSlope;

    private float _invalidPixelValue;
    private float _cloudCorrectionValue;
    private String _bitMaskExpression;
    private Term _bitMaskTerm;
    private BaselineAlgorithm _algorithm;

    private FlhMciUI _ui;
    private Logger _logger;
    public static final String HELP_ID = "flhMciScientificTool";

    /**
     * Constructs the object with default parameters.
     */
    public FlhMciProcessor() {
        _invalidPixelValue = 0.f;
        _algorithm = null;
        _ui = null;
        _logger = Logger.getLogger(FlhMciConstants.LOGGER_NAME);
        setDefaultHelpId(HELP_ID);
    }

    /**
     * Processes the request actually set.
     *
     * @throws org.esa.beam.framework.processor.ProcessorException
     *          on any failure during processing
     */
    @Override
    public final void process(ProgressMonitor pm) throws ProcessorException {
        pm.beginTask("Creating output product...", 2);
        try {
            _logger.info(ProcessorConstants.LOG_MSG_START_REQUEST);


            try {
                // scan the request
                // ----------------
                loadRequestParams();

                // create a vector of input bands
                // ------------------------------
                loadInputProduct();

                // create output product
                // ---------------------
                createOutputProduct(SubProgressMonitor.create(pm, 1));
                if (pm.isCanceled()) {
                    setCurrentStatus(ProcessorConstants.STATUS_ABORTED);
                    return;
                }

                // create bitmask expression
                // -------------------------
                createBitmaskExpression();

                // initialize the algorithms
                // -------------------------
                initBaselineAlgorithm();

                // process lineheight and optionally slope
                // ---------------------------------------
                pm.setSubTaskName("Computing line height and slope");
                processLineheightAndSlope(SubProgressMonitor.create(pm, 1));
            } finally {
                try {
                    if (isAborted()) {
                        deleteOutput();
                    }
                } finally {
                    closeProducts();
                }
            }
        } catch (IOException e) {
            _logger.severe(e.getMessage());
            setCurrentStatus(ProcessorConstants.STATUS_FAILED);
            throw new ProcessorException("An I/O error occurred:\n" + e.getMessage(), e);
        } finally {
            pm.done();
        }
        _logger.info(ProcessorConstants.LOG_MSG_FINISHED_REQUEST);
    }

    private void deleteOutput() throws IOException {
        if (_outputProduct == null) {
            return;
        }
        final ProductWriter productWriter = _outputProduct.getProductWriter();
        if (productWriter != null) {
            productWriter.deleteOutput();
        }
    }

    /**
     * Writes the processor specific logging file header to the logstream currently set.
     * <p/>
     * <p>This method is called by the processor runner initially after a logging sink has been created.
     */
    @Override
    public final void logHeader() {
        if (_ui == null) {
            _logger.info(FlhMciConstants.LOG_MSG_HEADER + PROCESSOR_VERSION);
            _logger.info(PROCESSOR_COPYRIGHT);
            _logger.info("");
        }
    }

    /**
     * Creates the GUI for the processor.
     */
    @Override
    public final ProcessorUI createUI() {
        _ui = new FlhMciUI();
        _ui.setProcessor(this);
        return _ui;
    }

    /**
     * Returns the request element factory for this processor.
     */
    @Override
    public final RequestElementFactory getRequestElementFactory() {
        return FlhMciRequestElementFactory.getInstance();
    }

    /**
     * Retrieves the titlestring to be shown in the user interface. Override this method to set a processor specific
     * title string.
     */
    @Override
    public final String getUITitle() {
        return PROCESSOR_NAME;
    }

    /**
     * Retrieves the name of the processor
     */
    @Override
    public final String getName() {
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
    public final String getVersion() {
        return PROCESSOR_VERSION;
    }

    /**
     * Retrieves copyright information of the processor
     */
    @Override
    public final String getCopyrightInformation() {
        return PROCESSOR_COPYRIGHT;
    }

    /**
     * Retrieves a progress message for the request passed in. Override this method if you need custom messaging.
     *
     * @param request
     *
     * @return the progress message for the request
     */
    @Override
    public String getProgressMessage(final Request request) {
        return FlhMciConstants.LOG_MSG_GENERATE_PIXEL;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Load the parameters from the current request to the processor
     */
    private void loadRequestParams() throws ProcessorException {
        Request request = getRequest();

        // check that we have the correct request type
        // -------------------------------------------
        Request.checkRequestType(request, FlhMciConstants.REQUEST_TYPE);

        ProcessorUtils.setProcessorLoggingHandler(FlhMciConstants.DEFAULT_LOG_PREFIX, request,
                                                  getName(), getVersion(), getCopyrightInformation());

        _logger.info(FlhMciConstants.LOG_MSG_LOAD_REQUEST);

        // load invalid pixel value
        loadInvalidPixelValue(request);
        // lineheight band name
        loadLineheightBandNameParameter(request);
        // slope band name and boolean whether to process it
        loadSlopeBandParameter(request);
        // bitmask expression
        loadBitmaskExpression(request);
        // cloud correction value
        loadCloudCorrectionParameter(request);

        _logger.info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    /**
     * Loads the value for invalidPixel from the request. If none is set, this method uses 0.0 as default
     *
     * @param request the request to be loaded
     */
    private void loadInvalidPixelValue(Request request) {
        Parameter param = request.getParameter(FlhMciConstants.INVALID_PIXEL_VALUE_PARAM_NAME);
        if (param != null) {
            _invalidPixelValue = ((Float) param.getValue()).floatValue();
            _logger.info(FlhMciConstants.LOG_MSG_INVALID_PIXEL + _invalidPixelValue);
        } else {
            _logger.warning(ProcessorConstants.LOG_MSG_NO_INVALID_PIXEL);
            _logger.warning(ProcessorConstants.LOG_MSG_USING + FlhMciConstants.DEFAULT_INVALID_PIXEL_VALUE);
            _invalidPixelValue = FlhMciConstants.DEFAULT_INVALID_PIXEL_VALUE.floatValue();
        }
    }

    /**
     * Loads the value for the lineheigt band name from the request. If none is set, this method uses "FLH" as default.
     *
     * @param request the request to be loaded
     */
    private void loadLineheightBandNameParameter(Request request) {
        Parameter param = request.getParameter(FlhMciConstants.LINEHEIGHT_BAND_NAME_PARAM_NAME);
        if (param != null) {
            _lineheightBandName = param.getValueAsText();
            _logger.info(FlhMciConstants.LOG_MSG_LINEHEIGHT_NAME + _lineheightBandName);
        } else {
            _logger.warning(FlhMciConstants.LOG_MSG_NO_LINEHEIGHT);
            _logger.warning(ProcessorConstants.LOG_MSG_USING + FlhMciConstants.DEFAULT_LINE_HEIGHT_BAND_NAME);
            _lineheightBandName = FlhMciConstants.DEFAULT_LINE_HEIGHT_BAND_NAME;
        }
    }

    /**
     * Loads the parameter for the slope band from the request.
     *
     * @param request the request containing the parameter
     */
    private void loadSlopeBandParameter(Request request) {
        Parameter processSlopeParam = request.getParameter(FlhMciConstants.PROCESS_SLOPE_PARAM_NAME);
        if (processSlopeParam != null) {
            if (((Boolean) processSlopeParam.getValue()).booleanValue()) {
                _logger.info(FlhMciConstants.LOG_MSG_SLOPE_ENABLED); /*I18N*/
                _processSlope = true;

                // load the slope band name
                Parameter slopeBandParam = request.getParameter(FlhMciConstants.SLOPE_BAND_NAME_PARAM_NAME);
                if (slopeBandParam != null) {
                    _slopeBandName = slopeBandParam.getValueAsText();
                    _logger.info(FlhMciConstants.LOG_MSG_SLOPE_BAND_NAME + _slopeBandName);                /*I18N*/
                } else {
                    _logger.warning(FlhMciConstants.LOG_MSG_NO_SLOPE_BAND); /*I18N*/
                    _logger.warning(
                            ProcessorConstants.LOG_MSG_USING + FlhMciConstants.DEFAULT_SLOPE_BAND_NAME); /*I18N*/
                    _slopeBandName = FlhMciConstants.DEFAULT_SLOPE_BAND_NAME;
                }
            } else {
                _processSlope = false;
            }
        } else {
            _logger.warning(FlhMciConstants.LOG_MSG_NO_SLOPE_PARAMETER); /*I18N*/
            _logger.warning(FlhMciConstants.LOG_MSG_NO_SLOPE_PROCESS); /*I18N*/
        }
    }

    /**
     * Loads the bitmask expression value from the request passed in
     *
     * @param request the request to be loaded
     */
    private void loadBitmaskExpression(Request request) {
        Parameter param;
        param = request.getParameter(FlhMciConstants.BITMASK_PARAM_NAME);
        if (param != null) {
            _bitMaskExpression = param.getValueAsText();
        } else {
            _bitMaskExpression = "";
        }
    }

    private void loadCloudCorrectionParameter(Request request) {
        Parameter param = request.getParameter(FlhMciConstants.CLOUD_CORRECTION_FACTOR_PARAM_NAME);
        if (param != null) {
            _cloudCorrectionValue = ((Float) param.getValue()).floatValue();
            _logger.info(FlhMciConstants.LOG_MSG_CLOUD_CORRECT + _cloudCorrectionValue);
        } else {
            _logger.warning(FlhMciConstants.LOG_MSG_NO_CLOUD_CORRECT);
            _logger.warning(ProcessorConstants.LOG_MSG_USING + FlhMciConstants.DEFAULT_CLOUD_CORRECTION_FACTOR);
            _cloudCorrectionValue = FlhMciConstants.DEFAULT_CLOUD_CORRECTION_FACTOR.floatValue();
        }
    }

    /**
     * Returns a <code>Parameter</code> with the given name.
     *
     * @throws org.esa.beam.framework.processor.ProcessorException
     *          when no parameter is found
     */
    private Parameter getParameterSafe(String paramName) throws ProcessorException {
        Parameter param;
        param = getRequest().getParameter(paramName);
        if (param == null) {
            throw new ProcessorException(FlhMciConstants.ERROR_NO_PARAMETER + paramName);
        }
        return param;
    }

    /**
     * Creates the appropriate input <code>Product</code> for the current request and assembles a list of
     * <code>RsBands</code> to be processed.
     */
    private void loadInputProduct() throws ProcessorException,
                                           IOException {
        // only the first product - there might be more but these will be ignored
        // ----------------------------------------------------------------------
        _inputProduct = loadInputProduct(0);

        // load the bands
        // --------------
        loadLowBand();
        loadSignalBand();
        loadHighBand();
    }

    /**
     * Loads the high baseline band from the input product and the corresponding center wavelength
     */
    private void loadHighBand() throws ProcessorException {
        _highBand = loadBand(FlhMciConstants.BAND_HIGH_PARAM_NAME);
        _wavelengthHigh = _highBand.getSpectralWavelength();
        if (_wavelengthHigh < 1e-3) {
            throw new ProcessorException(
                    "The band '" + _highBand.getName() + "' does not contain spectral information.\nPlease select a spectral band for processing");
        }
        _logger.info(FlhMciConstants.LOG_MSG_CENTER_WAVE + _wavelengthHigh);
    }

    /**
     * Loads the signal band from the input product and the corresponding center wavelength
     */
    private void loadSignalBand() throws ProcessorException {
        _signalBand = loadBand(FlhMciConstants.BAND_SIGNAL_PARAM_NAME);
        _wavelengthSignal = _signalBand.getSpectralWavelength();
        if (_wavelengthSignal < 1e-3) {
            throw new ProcessorException(
                    "The band '" + _signalBand.getName() + "' does not contain spectral information.\nPlease select a spectral band for processing");
        }
        _logger.info(FlhMciConstants.LOG_MSG_CENTER_WAVE + _wavelengthSignal);
    }

    /**
     * Loads the low baseline band from the input product and the corresponding center wavelength
     */
    private void loadLowBand() throws ProcessorException {
        _lowBand = loadBand(FlhMciConstants.BAND_LOW_PARAM_NAME);
        _wavelengthLow = _lowBand.getSpectralWavelength();
        if (_wavelengthLow < 1e-3) {
            throw new ProcessorException(
                    "The band '" + _lowBand.getName() + "' does not contain spectral information.\nPlease select a spectral band for processing");
        }
        _logger.info(FlhMciConstants.LOG_MSG_CENTER_WAVE + _wavelengthLow);
    }

    /**
     * Loads a specific band from the input product.
     *
     * @param parameterName the request parameter which holds this bands name
     *
     * @return the band loaded
     */
    private Band loadBand(String parameterName) throws ProcessorException {
        Parameter param;
        String bandName;
        Band band;

        param = getParameterSafe(parameterName);
        bandName = param.getValueAsText();
        testBandNameForEmptyString(bandName);
        band = _inputProduct.getBand(bandName);
        if (band == null) {
            String message = "Requested band '" + bandName + "' not found in product!";
            _logger.severe(message);
            throw new ProcessorException(message);
        } else {
            _logger.info(ProcessorConstants.LOG_MSG_LOADED_BAND + bandName);
        }

        return band;
    }

    private static void testBandNameForEmptyString(String bandName) throws ProcessorException {
        if ((bandName == null) || (bandName.length() == 0)) {
            throw new ProcessorException("Please enter a valid name");
        }
    }

    /**
     * Creates the output product for the given request
     */
    private void createOutputProduct(ProgressMonitor pm) throws ProcessorException,
                                                                IOException {
        // take only the first output product. There might be more but we will ignore
        // these in this processor.
        final ProductRef prodRef = getOutputProductSafe();

        // retrieve product specific information
        // -------------------------------------
        final File outputFile = new File(prodRef.getFilePath());
        final String productType = getOutputProductTypeSafe();
        final String productName = FileUtils.getFilenameWithoutExtension(outputFile);
        final int sceneWidth = _inputProduct.getSceneRasterWidth();
        final int sceneHeight = _inputProduct.getSceneRasterHeight();

        // create in memory representation of output product and
        // connect with appropriate writer
        // -----------------------------------------------------
        _outputProduct = new Product(productName, productType, sceneWidth, sceneHeight);
        final ProductWriter writer = ProcessorUtils.createProductWriter(prodRef);
        _outputProduct.setProductWriter(writer);

        // create the bands for lineheight and slope (if wanted)
        // -----------------------------------------------------

        // get unit from signal band
        _lineheightBand = new Band(_lineheightBandName, ProductData.TYPE_FLOAT32, sceneWidth, sceneHeight);
        _lineheightBand.setUnit(_signalBand.getUnit());
        _lineheightBand.setDescription(FlhMciConstants.LINEHEIGHT_BAND_DESCRIPTION);
        ProductUtils.copySpectralBandProperties(_signalBand, _lineheightBand);
        _outputProduct.addBand(_lineheightBand);

        if (_processSlope) {
            _slopeBand = new Band(_slopeBandName, ProductData.TYPE_FLOAT32, sceneWidth, sceneHeight);
            _slopeBand.setUnit(_signalBand.getUnit() + " * nm^-1");
            _slopeBand.setDescription(FlhMciConstants.SLOPE_BAND_DESCRIPTION);
            _outputProduct.addBand(_slopeBand);
        }

        ProductUtils.copyTiePointGrids(_inputProduct, _outputProduct);
        copyRequestMetaData(_outputProduct);
        copyFlagBands(_inputProduct, _outputProduct);

        copyGeoCoding(_inputProduct, _outputProduct);

        // initialize the disk represenation
        // ---------------------------------
        writer.writeProductNodes(_outputProduct, outputFile);
        copyBandData(getBandNamesToCopy(), _inputProduct, _outputProduct, pm);
    }

    /**
     * Creates a bitmask expression from the bitmask string passed in from the request.
     */
    private void createBitmaskExpression() throws ProcessorException {
        if (_bitMaskExpression.equalsIgnoreCase("")) {
            // no bitmask set
            _logger.info(ProcessorConstants.LOG_MSG_NO_BITMASK);
            _logger.info(ProcessorConstants.LOG_MSG_PROCESS_ALL);
            _bitMaskTerm = null;
        } else {
            _bitMaskTerm = ProcessorUtils.createTerm(_bitMaskExpression, _inputProduct);
        }
    }

    /**
     * Retrieves the output product type from the input product type by appending "_FLH_MCI" to the type string.
     *
     * @throws org.esa.beam.framework.processor.ProcessorException
     *          when an error occurs
     */
    private String getOutputProductTypeSafe() throws ProcessorException {
        String productType = _inputProduct.getProductType();
        if (productType == null) {
            throw new ProcessorException(ProcessorConstants.LOG_MSG_NO_INPUT_TYPE);
        }

        return productType + "_FLH_MCI";
    }

    /**
     * Retrieves the first output product reference from the request.
     *
     * @throws org.esa.beam.framework.processor.ProcessorException
     *          when no output product is set in the request
     */
    private ProductRef getOutputProductSafe() throws ProcessorException {
        final ProductRef prodRef = getRequest().getOutputProductAt(0);
        if (prodRef == null) {
            throw new ProcessorException(ProcessorConstants.LOG_MSG_NO_OUTPUT_IN_REQUEST);
        }
        return prodRef;
    }

    /**
     * Processes the line height and (optionally) the slope of the input product set.
     */
    private void processLineheightAndSlope(ProgressMonitor pm) throws IOException {
        _logger.info(ProcessorConstants.LOG_MSG_START_REQUEST);

        // initialize vectors and other data
        // ---------------------------------
        int width = _inputProduct.getSceneRasterWidth();
        int height = _inputProduct.getSceneRasterHeight();
        float[] low = new float[width];
        float[] signal = new float[width];
        float[] high = new float[width];
        float[] flh = new float[width];
        float[] slope = null;
        if (_processSlope) {
            slope = new float[width];
        }
        boolean[] process = new boolean[width];

        // set up vector - in case all pixels shall be processed
        for (int x = 0; x < width; x++) {
            process[x] = true;
        }

        // set cloud correction value
        _algorithm.setCloudCorrectionFactor(_cloudCorrectionValue);

        // progress bar init
        pm.beginTask(FlhMciConstants.LOG_MSG_GENERATE_PIXEL, height);
        try {
            // loop over all scanlines
            for (int y = 0; y < height; y++) {
                // read scanline
                _lowBand.readPixels(0, y, width, 1, low, ProgressMonitor.NULL);
                _signalBand.readPixels(0, y, width, 1, signal, ProgressMonitor.NULL);
                _highBand.readPixels(0, y, width, 1, high, ProgressMonitor.NULL);

                // forEachPixel bitmask
                if (_bitMaskTerm != null) {
                    _inputProduct.readBitmask(0, y, width, 1, _bitMaskTerm, process, ProgressMonitor.NULL);
                }

                // process scanline and write to disk
                flh = _algorithm.process(low, high, signal, process, flh);
                _lineheightBand.writePixels(0, y, width, 1, flh, ProgressMonitor.NULL);
                if (_processSlope) {
                    slope = _algorithm.processSlope(low, high, process, slope);
                    _slopeBand.writePixels(0, y, width, 1, slope, ProgressMonitor.NULL);
                }

                pm.worked(1);
                if (pm.isCanceled()) {
                    _logger.warning(FlhMciConstants.LOG_MSG_PROC_CANCELED);
                    setCurrentStatus(FlhMciConstants.STATUS_ABORTED);
                    break;
                }
            }
        } finally {
            pm.done();
        }

        _logger.info(FlhMciConstants.LOG_MSG_PROC_SUCCESS);
    }

    /**
     * Initializes the baseline algorithm class. If none is present, creates the algorithm object
     */
    private void initBaselineAlgorithm() throws ProcessorException {
        if (_algorithm == null) {
            _algorithm = new BaselineAlgorithm();
        }

        // set wavelengths
        _algorithm.setWavelengths(_wavelengthLow, _wavelengthHigh, _wavelengthSignal);

        // set invalid pixel
        _algorithm.setInvalidValue(_invalidPixelValue);
    }

    /**
     * Closes any open products.
     */
    private void closeProducts() {
        if (_inputProduct != null) {
            _inputProduct.dispose();
        }
        if (_outputProduct != null) {
            _outputProduct.dispose();
        }
    }

    /**
     * Called by framework after a processing failure.
     */
    @Override
    protected final void cleanupAfterFailure() {
        closeProducts();
    }
}




