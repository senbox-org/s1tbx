/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.beam.processor.sst;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.jexp.Term;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
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
import org.esa.beam.processor.sst.ui.SstUI;
import org.esa.beam.util.Debug;
import org.esa.beam.util.ProductUtils;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * The worker class for the sea surface temperature processor.
 *
 * @deprecated since BEAM 4.10 - no replacement.
 */
@Deprecated
public class SstProcessor extends Processor {

    public static final String PROCESSOR_NAME = "BEAM SST Processor";
    private static final String PROCESSOR_SYMBOLIC_NAME = "beam-aatsr-sst";
    private static final String _nadirSstBandName = "nadir_sst";
    private static final String _dualSstBandName = "dual_sst";
    private static final String _version = "1.5";
    private static final String _copyright = "Copyright (C) 2002-2011 by Brockmann Consult (info@brockmann-consult.de)";
    // coeffs are given for unscaled temperatures
    // thats why we do not need to scale the <x>.0 coeff
    private static final float COEFF_0_SCALE = 1.0f;

    private SstUI _ui;
    private boolean _processDual;
    private File _dualCoeffFile;
    private boolean _processNadir;
    private File _nadirCoeffFile;
    private SstCoefficientLoader _loader;
    private int[] _nadirCoeffMap;
    private float[] _a0;
    private float[] _a1;
    private float[] _a2;
    private float[] _b0;
    private float[] _b1;
    private float[] _b2;
    private float[] _b3;
    private float[] _c0;
    private float[] _c1;
    private float[] _c2;
    private float[] _c3;
    private float[] _c4;
    private float[] _d0;
    private float[] _d1;
    private float[] _d2;
    private float[] _d3;
    private float[] _d4;
    private float[] _d5;
    private float[] _d6;
    private int[] _dualCoeffMap;
    private float _invalidPixel;

    private Product _inputProduct;
    private Product _outputProduct;
    private Band _nadir370Band;
    private Band _nadir1100Band;
    private Band _nadir1200Band;
    private TiePointGrid _nadirSea;
    private Band _forward370Band;
    private Band _forward1100Band;
    private Band _forward1200Band;
    private TiePointGrid _forwardSea;
    private Band _nadirSstBand;
    private Band _dualSstBand;

    private String _dualBitmaskExpression;
    private Term _dualBitmaskTerm;
    private String _nadirBitmaskExpression;
    private Term _nadirBitmaskTerm;

    private Logger _logger;
    public static final String HELP_ID = "sstScientificTool";

    /**
     * Constructs the object with default parameters
     */
    public SstProcessor() {
        _processDual = false;
        _processNadir = false;
        _invalidPixel = SstConstants.DEFAULT_INVALID_PIXEL;
        _loader = new SstCoefficientLoader();
        _logger = Logger.getLogger(SstConstants.LOGGER_NAME);
        setDefaultHelpId(HELP_ID);
    }

    /**
     * Processes the request actually set.
     *
     * @throws org.esa.beam.framework.processor.ProcessorException
     *          on any failure during processing
     */
    @Override
    public void process(ProgressMonitor pm) throws ProcessorException {
        _logger.info(SstConstants.LOG_MSG_START_REQUEST);

        try {
            // load the request parameter
            loadRequestParams();

            // check if we have something to process
            if (!_processDual && !_processNadir) {
                _logger.info("Nothing to process");
                return;
            }

            // load the input product
            loadInputProduct();

            // create the output product
            createOutputProduct();

            // creates bitmask terms from the parsed strings
            createBitmaskTerms();

            // install auxdata
            installAuxdata();
            // parse the coefficient files
            setUpCoefficientStructures();

            pm.beginTask("Computing SST...", _processDual && _processNadir ? 2 : 1);
            try {
                // and now process
                if (_processDual) {
                    processDual(SubProgressMonitor.create(pm, 1));
                }
                if (_processNadir && !isAborted()) {
                    processNadir(SubProgressMonitor.create(pm, 1));
                }
            } finally {
                pm.done();
            }
            closeProducts();
        } catch (IOException e) {
            _logger.severe(e.getMessage());
            throw new ProcessorException("An I/O error occurred:\n" + e.getMessage(), e);
        }
        _logger.info(SstConstants.LOG_MSG_FINISHED_REQUEST);
    }

    /**
     * Returns the request element factory for this processor.
     */
    @Override
    public RequestElementFactory getRequestElementFactory() {
        return SstRequestElementFactory.getInstance();
    }

    /**
     * Creates the processor specific UI component and returns it to caller.
     */
    @Override
    public ProcessorUI createUI() {
        if (_ui == null) {
            _ui = new SstUI();
        }
        return _ui;
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
     * Retrieves a progress message for the request passed in. Override this method if you need custom messaging.
     *
     * @param request The request.
     *
     * @return the progress message for the request
     */
    @Override
    public String getProgressMessage(final Request request) {
        return "Generating pixels for SST";
    }

    @Override
    public void installAuxdata() throws ProcessorException {
        setAuxdataInstallDir(SstConstants.AUXDATA_DIR_PROPERTY, getDefaultAuxdataInstallDir());
        super.installAuxdata();
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Loads all parameters from the request.
     */
    private void loadRequestParams() throws ProcessorException {

        // make sure the request is meant for the sst processor
        Request.checkRequestType(getRequest(), SstConstants.REQUEST_TYPE);

        // initialize logging for the request
        ProcessorUtils.setProcessorLoggingHandler(SstConstants.DEFAULT_LOG_PREFIX, getRequest(),
                                                  getName(), getVersion(), getCopyrightInformation());

        // load dual view parameter
        loadDualViewSstParams();

        // load nadir view parameter
        loadNadirViewSstParams();

        // load the invalid pixel value
        loadInvalidPixel();

        // loads the bitmask expression(s)
        loadBitmaskExpressions();
    }

    /**
     * Loads all parameter needed for the dual view sst processing.
     */
    private void loadDualViewSstParams() {
        Request request = getRequest();

        _processDual = false;
        Parameter param = request.getParameter(SstConstants.PROCESS_DUAL_VIEW_SST_PARAM_NAME);
        if (param != null) {
            // check boolean parameter
            if ((Boolean) param.getValue()) {
                // now try to read the coefficient file parameter
                param = request.getParameter(SstConstants.DUAL_VIEW_COEFF_FILE_PARAM_NAME);
                if (param != null) {
                    _dualCoeffFile = (File) param.getValue();
                    _logger.info("Generating pixels of dual view SST");
                    _logger.info("... using coefficient file: " + _dualCoeffFile.toString());
                    _processDual = true;

                }
            }
        }

        if (!_processDual) {
            _logger.warning("Parameter \"" + SstConstants.DUAL_VIEW_COEFF_FILE_PARAM_NAME + "\" not set");
            _logger.warning("... generation of dual view SST skipped!");
        }
    }

    /**
     * Loads all parameter needed for the nadir view sst processing.
     */
    private void loadNadirViewSstParams() {
        Request request = getRequest();

        _processNadir = false;
        Parameter param = request.getParameter(SstConstants.PROCESS_NADIR_VIEW_SST_PARAM_NAME);
        if (param != null) {
            // check boolean parameter
            if ((Boolean) param.getValue()) {
                // now try to read the coefficient file parameter
                param = request.getParameter(SstConstants.NADIR_VIEW_COEFF_FILE_PARAM_NAME);
                if (param != null) {
                    _nadirCoeffFile = (File) param.getValue();
                    _logger.info("Generating nadir view SST");
                    _logger.info("... using coefficient file: " + _nadirCoeffFile.toString());
                    _processNadir = true;
                }
            }
        }

        if (!_processNadir) {
            _logger.warning("Parameter \"" + SstConstants.NADIR_VIEW_COEFF_FILE_PARAM_NAME + "\" not set");
            _logger.warning("... generation of nadir view SST skipped!");
        }
    }

    /**
     * Loads the parameter value for the invalid pixel from the current request
     */
    private void loadInvalidPixel() {
        Parameter param = getRequest().getParameter(SstConstants.INVALID_PIXEL_PARAM_NAME);

        if (param != null) {
            _invalidPixel = (Float) param.getValue();
            _logger.info("invalid pixel value: " + _invalidPixel);
        } else {
            _logger.warning("Parameter '" + SstConstants.INVALID_PIXEL_PARAM_NAME + "' not set");
            _logger.warning(ProcessorConstants.LOG_MSG_USING + SstConstants.DEFAULT_INVALID_PIXEL);
            _invalidPixel = SstConstants.DEFAULT_INVALID_PIXEL;
        }
    }

    /**
     * Loads the bitmask expression parameters from the request passed in
     */
    private void loadBitmaskExpressions() {
        Parameter param;
        Request request = getRequest();

        if (_processNadir) {
            param = request.getParameter(SstConstants.NADIR_VIEW_BITMASK_PARAM_NAME);
            if (param != null) {
                _nadirBitmaskExpression = param.getValueAsText();
            } else {
                _nadirBitmaskExpression = "";
            }
        }

        if (_processDual) {
            param = request.getParameter(SstConstants.DUAL_VIEW_BITMASK_PARAM_NAME);
            if (param != null) {
                _dualBitmaskExpression = param.getValueAsText();
            } else {
                _dualBitmaskExpression = "";
            }
        }
    }

    /**
     * Loads the input product from the current request
     */
    private void loadInputProduct() throws ProcessorException,
                                           IOException {
        _inputProduct = loadInputProduct(0);

        // now load the nadir bands
        // ------------------------
        _nadir370Band = loadBand(SstConstants.NADIR_370_BAND);
        _nadir1100Band = loadBand(SstConstants.NADIR_1100_BAND);
        _nadir1200Band = loadBand(SstConstants.NADIR_1200_BAND);
        _nadirSea = loadTiePtGrid(SstConstants.SUN_ELEV_NADIR);

        // and load forward bands if needed
        // --------------------------------
        if (_processDual) {
            _forward370Band = loadBand(SstConstants.FORWARD_370_BAND);
            _forward1100Band = loadBand(SstConstants.FORWARD_1100_BAND);
            _forward1200Band = loadBand(SstConstants.FORWARD_1200_BAND);
            _forwardSea = loadTiePtGrid(SstConstants.SUN_ELEV_FORWARD);
        }
    }

    /**
     * Loads a specific band from the input product.
     *
     * @param bandName bands name
     *
     * @return the band loaded
     */
    private Band loadBand(String bandName) throws ProcessorException {
        Band band;

        band = _inputProduct.getBand(bandName);
        if (band == null) {
            String message = "The requested band \"" + bandName + "\" not found in product!";
            _logger.severe(message);
            throw new ProcessorException(message);
        } else {
            _logger.fine(ProcessorConstants.LOG_MSG_LOADED_BAND + bandName);
        }

        return band;
    }

    /**
     * Loads a specific tie point grid from the input product.
     *
     * @param gridName the name of the grid to load
     *
     * @return the tie point grid loaded
     */
    private TiePointGrid loadTiePtGrid(String gridName) throws ProcessorException {
        TiePointGrid grid;

        grid = _inputProduct.getTiePointGrid(gridName);
        if (grid == null) {
            String message = "The requested tie point grid \"" + gridName + "\" not found in product!";
            _logger.severe(message);
            throw new ProcessorException(message);
        } else {
            _logger.fine("... loaded tie point grid: " + gridName);
        }

        return grid;
    }

    /**
     * Creates the output product for the given request
     */
    private void createOutputProduct() throws ProcessorException,
                                              IOException {

        // take only the first output product. There might be more but we will ignore
        // these in this processor.
        final ProductRef prod = getOutputProductSafe();

        // retrieve product specific inpormation
        // -------------------------------------
        final String productType = getOutputProductTypeSafe();
        final String productName = getOutputProductNameSafe();

        final int sceneWidth = _inputProduct.getSceneRasterWidth();
        final int sceneHeight = _inputProduct.getSceneRasterHeight();

        // create in memory representation of output product and
        // connect with appropriate writer
        // -----------------------------------------------------
        _outputProduct = new Product(productName, productType, sceneWidth, sceneHeight);
        final ProductWriter writer = ProcessorUtils.createProductWriter(prod);
        _outputProduct.setProductWriter(writer);

        // create the sst nadir band
        // -------------------------
        _nadirSstBand = new Band(_nadirSstBandName, ProductData.TYPE_FLOAT32, sceneWidth, sceneHeight);
        _nadirSstBand.setUnit(SstConstants.OUT_BAND_UNIT);
        _nadirSstBand.setDescription(SstConstants.OUT_BAND_NADIR_DESCRIPTION);
        _nadirSstBand.setGeophysicalNoDataValue(_invalidPixel);
        _nadirSstBand.setNoDataValueUsed(true);
        _outputProduct.addBand(_nadirSstBand);

        // create the dual sst band if needed
        // ----------------------------------
        if (_processDual) {
            _dualSstBand = new Band(_dualSstBandName, ProductData.TYPE_FLOAT32, sceneWidth, sceneHeight);
            _dualSstBand.setUnit(SstConstants.OUT_BAND_UNIT);
            _dualSstBand.setDescription(SstConstants.OUT_BAND_DUAL_DESCRIPTION);
            _dualSstBand.setGeophysicalNoDataValue(_invalidPixel);
            _dualSstBand.setNoDataValueUsed(true);
            _outputProduct.addBand(_dualSstBand);
        }

        // copy the tie point raster
        // -------------------------
        ProductUtils.copyTiePointGrids(_inputProduct, _outputProduct);

        // copy geocoding
        // --------------
        ProductUtils.copyGeoCoding(_inputProduct, _outputProduct);

        copyRequestMetaData(_outputProduct);

        // and initialize the disk represenation
        // -------------------------------------
        writer.writeProductNodes(_outputProduct, new File(prod.getFilePath()));
    }

    /**
     * Retrieves the first output product reference from the request.
     *
     * @throws org.esa.beam.framework.processor.ProcessorException
     *          when no output product is set in the request
     */
    private ProductRef getOutputProductSafe() throws ProcessorException {
        ProductRef prod;
        prod = getRequest().getOutputProductAt(0);
        if (prod == null) {
            throw new ProcessorException(ProcessorConstants.LOG_MSG_NO_OUTPUT_IN_REQUEST);
        }
        return prod;
    }

    /**
     * Retrieves the output product type from the input product type by appending "_SST" to the type string.
     *
     * @throws org.esa.beam.framework.processor.ProcessorException
     *          when an error occurs
     */
    private String getOutputProductTypeSafe() throws ProcessorException {
        String productType = _inputProduct.getProductType();
        if (productType == null) {
            throw new ProcessorException(SstConstants.LOG_MSG_NO_INPUT_TYPE);
        }

        return productType + "_SST";
    }

    /**
     * Retrieves the output product name from request
     *
     * @throws org.esa.beam.framework.processor.ProcessorException
     *          when an error occurs
     */
    private String getOutputProductNameSafe() throws ProcessorException {
        Request request = getRequest();
        ProductRef prod = request.getOutputProductAt(0);
        if (prod == null) {
            throw new ProcessorException(ProcessorConstants.LOG_MSG_NO_OUTPUT_IN_REQUEST);
        }
        File prodFile = new File(prod.getFilePath());

        return FileUtils.getFilenameWithoutExtension(prodFile);
    }


    /**
     * Tries to parse the bitmask expressions passed in and to create valid Bitmask terms that can be evaluated during
     * processing
     */
    private void createBitmaskTerms() throws ProcessorException {
        if (_processNadir) {
            if (_nadirBitmaskExpression.equalsIgnoreCase("")) {
                // no bitmask set
                _logger.info("No nadir bitmask set!");
                _logger.info(ProcessorConstants.LOG_MSG_PROCESS_ALL);
                _nadirBitmaskTerm = null;
            } else {
                // something is set - try to create a valid expression
                _nadirBitmaskTerm = ProcessorUtils.createTerm(_nadirBitmaskExpression, _inputProduct);
                _logger.info("using nadir bitmask: " + _nadirBitmaskExpression);
            }
        }

        if (_processDual) {
            if (_dualBitmaskExpression.equalsIgnoreCase("")) {
                // no bitmask set
                _logger.info("No dual bitmask set!");
                _logger.info(ProcessorConstants.LOG_MSG_PROCESS_ALL);
                _dualBitmaskTerm = null;
            } else {
                // something is set - try to create a valid expression
                _dualBitmaskTerm = ProcessorUtils.createTerm(_dualBitmaskExpression, _inputProduct);
                _logger.info("using dual bitmask: " + _dualBitmaskExpression);
            }
        }
    }

    /**
     * Loads the coefficient files from the request and stores the coefficients. Then build up the coefficient access
     * structures
     */
    private void setUpCoefficientStructures() throws ProcessorException {
        try {
            if (_processDual) {
                setUpDualCoeffStructures();
            }
            if (_processNadir) {
                setUpNadirCoeffStructures();
            }
        } catch (IOException e) {
            throw new ProcessorException("An I/O error occurred:\n" + e.getMessage(), e);
        }
    }

    private void setUpNadirCoeffStructures() throws IOException,
                                                    ProcessorException {
        SstCoefficientSet _nadirCoeffs = _loader.load(_nadirCoeffFile.toURI().toURL());
        int nMaxIndex = 0;
        int numCoeffs = _nadirCoeffs.getNumCoefficients();
        int nEnd;

        // check what is the highest map pixel
        // -----------------------------------
        for (int n = 0; n < numCoeffs; n++) {
            nEnd = _nadirCoeffs.getCoefficientsAt(n).getEnd();
            if (nEnd > nMaxIndex) {
                nMaxIndex = nEnd;
            }
        }
        // remember - index is zero based
        _nadirCoeffMap = new int[nMaxIndex + 1];

        _a0 = new float[numCoeffs];
        _a1 = new float[numCoeffs];
        _a2 = new float[numCoeffs];
        _b0 = new float[numCoeffs];
        _b1 = new float[numCoeffs];
        _b2 = new float[numCoeffs];
        _b3 = new float[numCoeffs];

        // now fill in the data
        // --------------------
        _logger.fine("Loading Nadir view coefficients ...");
        _logger.fine("... file contains " + numCoeffs + " coeffcient sets.");

        SstCoefficients coeffs;
        float[] a_coeffs;
        float[] b_coeffs;
        for (int n = 0; n < numCoeffs; n++) {
            coeffs = _nadirCoeffs.getCoefficientsAt(n);

            // fill index map
            for (int m = coeffs.getStart(); m <= coeffs.getEnd(); m++) {
                _nadirCoeffMap[m] = n;
            }

            // fill coefficients
            a_coeffs = coeffs.get_A_Coeffs();
            if (a_coeffs == null) {
                throw new ProcessorException("Invalid coefficient file: no nadir view \"a\" coefficients set");
            }
            _a0[n] = a_coeffs[0] * COEFF_0_SCALE;
            _a1[n] = a_coeffs[1];
            _a2[n] = a_coeffs[2];
            b_coeffs = coeffs.get_B_Coeffs();
            if (b_coeffs == null) {
                throw new ProcessorException("Invalid coefficient file: no nadir view \"b\" coefficients set");
            }
            _b0[n] = b_coeffs[0] * COEFF_0_SCALE;
            _b1[n] = b_coeffs[1];
            _b2[n] = b_coeffs[2];
            _b3[n] = b_coeffs[3];
        }

        _logger.fine("... finished.");
    }

    private void setUpDualCoeffStructures() throws IOException,
                                                   ProcessorException {
        SstCoefficientSet _dualCoeffs = _loader.load(_dualCoeffFile.toURI().toURL());
        int nMaxIndex = 0;
        int numCoeffs = _dualCoeffs.getNumCoefficients();
        int nEnd;

        // check what is the highest map pixel
        // -----------------------------------
        for (int n = 0; n < numCoeffs; n++) {
            nEnd = _dualCoeffs.getCoefficientsAt(n).getEnd();
            if (nEnd > nMaxIndex) {
                nMaxIndex = nEnd;
            }
        }
        // remember - index is zero based
        _dualCoeffMap = new int[nMaxIndex + 1];

        _c0 = new float[numCoeffs];
        _c1 = new float[numCoeffs];
        _c2 = new float[numCoeffs];
        _c3 = new float[numCoeffs];
        _c4 = new float[numCoeffs];
        _d0 = new float[numCoeffs];
        _d1 = new float[numCoeffs];
        _d2 = new float[numCoeffs];
        _d3 = new float[numCoeffs];
        _d4 = new float[numCoeffs];
        _d5 = new float[numCoeffs];
        _d6 = new float[numCoeffs];

        // now fill in the data
        // --------------------
        SstCoefficients coeffs;
        float[] c_coeffs;
        float[] d_coeffs;
        for (int n = 0; n < numCoeffs; n++) {
            coeffs = _dualCoeffs.getCoefficientsAt(n);

            // fill index map
            for (int m = coeffs.getStart(); m <= coeffs.getEnd(); m++) {
                _dualCoeffMap[m] = n;
            }

            // fill coefficients
            c_coeffs = coeffs.get_C_Coeffs();
            if (c_coeffs == null) {
                throw new ProcessorException("Invalid coefficient file: no dual view \"c\" coefficients set");
            }
            _c0[n] = c_coeffs[0] * COEFF_0_SCALE;
            _c1[n] = c_coeffs[1];
            _c2[n] = c_coeffs[2];
            _c3[n] = c_coeffs[3];
            _c4[n] = c_coeffs[4];
            d_coeffs = coeffs.get_D_Coeffs();
            if (d_coeffs == null) {
                throw new ProcessorException("Invalid coefficient file: no dual view \"d\" coefficients set");
            }
            _d0[n] = d_coeffs[0] * COEFF_0_SCALE;
            _d1[n] = d_coeffs[1];
            _d2[n] = d_coeffs[2];
            _d3[n] = d_coeffs[3];
            _d4[n] = d_coeffs[4];
            _d5[n] = d_coeffs[5];
            _d6[n] = d_coeffs[6];
        }
    }

    /**
     * Processes the dual view sst
     */
    private void processDual(ProgressMonitor pm) throws IOException {

        _logger.info("Generating dual view SST");

        // create vectors
        int width = _inputProduct.getSceneRasterWidth();
        int height = _inputProduct.getSceneRasterHeight();
        float[] ir11_f = new float[width];
        float[] ir12_f = new float[width];
        float[] ir37_f = new float[width];
        float[] sea_f = new float[width];
        float[] ir11_n = new float[width];
        float[] ir12_n = new float[width];
        float[] ir37_n = new float[width];
        float[] sea_n = new float[width];
        float[] sst = new float[width];
        boolean[] process = new boolean[width];
        int index;

        // set up process vector, there might be no bitmask set
        for (int x = 0; x < width; x++) {
            process[x] = true;
        }

        // progress init
        pm.beginTask("Generating pixels for dual view SST...", height);
        try {
            // loop over all scanlines
            for (int y = 0; y < height; y++) {
                _nadir1100Band.readPixels(0, y, width, 1, ir11_n, ProgressMonitor.NULL);
                _nadir1200Band.readPixels(0, y, width, 1, ir12_n, ProgressMonitor.NULL);
                _nadir370Band.readPixels(0, y, width, 1, ir37_n, ProgressMonitor.NULL);
                _nadirSea.readPixels(0, y, width, 1, sea_n, ProgressMonitor.NULL);
                _forward1100Band.readPixels(0, y, width, 1, ir11_f, ProgressMonitor.NULL);
                _forward1200Band.readPixels(0, y, width, 1, ir12_f, ProgressMonitor.NULL);
                _forward370Band.readPixels(0, y, width, 1, ir37_f, ProgressMonitor.NULL);
                _forwardSea.readPixels(0, y, width, 1, sea_f, ProgressMonitor.NULL);

                // forEachPixel bitmask
                if (_dualBitmaskTerm != null) {
                    _inputProduct.readBitmask(0, y, width, 1, _dualBitmaskTerm, process, ProgressMonitor.NULL);
                }

                // process the scanline
                for (int x = 0; x < width; x++) {
                    sst[x] = _invalidPixel;
                    if (process[x]) {
                        // get the coefficient set index
                        index = getDualCoefficientIndex(x);

                        // check whether day or night
                        if ((sea_n[x] < 0.f) && (sea_f[x] < 0.f) &&
                            (ir37_n[x] > 0.f) && (ir37_f[x] > 0.f)) {
                            // night time
                            sst[x] = _d0[index] + _d1[index] * ir11_n[x] + _d2[index] * ir12_n[x] +
                                     _d3[index] * ir37_n[x] + _d4[index] * ir11_f[x] + _d5[index] * ir12_f[x] +
                                     _d6[index] * ir37_f[x];
                        } else {
                            // daytime
                            sst[x] = _c0[index] + _c1[index] * ir11_n[x] + _c2[index] * ir12_n[x] +
                                     _c3[index] * ir11_f[x] + _c4[index] * ir12_f[x];
                        }
                    }
                }

                // write scanline
                _dualSstBand.writePixels(0, y, width, 1, sst, ProgressMonitor.NULL);

                // update progressbar
                pm.worked(1);
                if (pm.isCanceled()) {
                    _logger.warning(SstConstants.LOG_MSG_PROC_CANCELED);
                    setCurrentStatus(SstConstants.STATUS_ABORTED);
                    return;
                }
            }
        } finally {
            pm.done();
        }

        _logger.info(SstConstants.LOG_MSG_PROC_SUCCESS);
    }

    /**
     * Processes the nadir view sst
     */
    private void processNadir(ProgressMonitor pm) throws IOException {

        _logger.info("Generating nadir view SST");

        // create vectors
        // --------------
        int width = _inputProduct.getSceneRasterWidth();
        int height = _inputProduct.getSceneRasterHeight();
        float[] ir11 = new float[width];
        float[] ir12 = new float[width];
        float[] ir37 = new float[width];
        float[] sea = new float[width];
        float[] sst = new float[width];
        boolean[] process = new boolean[width];
        int index;

        // set up process vector, there might be no bitmask set
        for (int x = 0; x < width; x++) {
            process[x] = true;
        }

        // progress init
        pm.beginTask("Generating pixels for nadir view SST...", height);
        try {
            // loop over all scanlines
            for (int y = 0; y < height; y++) {
                _nadir1100Band.readPixels(0, y, width, 1, ir11, ProgressMonitor.NULL);
                _nadir1200Band.readPixels(0, y, width, 1, ir12, ProgressMonitor.NULL);
                _nadir370Band.readPixels(0, y, width, 1, ir37, ProgressMonitor.NULL);
                _nadirSea.readPixels(0, y, width, 1, sea, ProgressMonitor.NULL);

                // forEachPixel bitmask
                if (_nadirBitmaskTerm != null) {
                    _inputProduct.readBitmask(0, y, width, 1, _nadirBitmaskTerm, process, ProgressMonitor.NULL);
                }

                // process the scanline
                for (int x = 0; x < width; x++) {
                    sst[x] = _invalidPixel;
                    if (process[x]) {
                        index = getNadirCoefficientIndex(x);

                        // check whether day or night
                        if ((sea[x] < 0.f) && (ir37[x] > 0.f)) {
                            // night time
                            sst[x] = _b0[index] + _b1[index] * ir11[x] + _b2[index] * ir12[x] + _b3[index] * ir37[x];
                        } else {
                            // day time
                            sst[x] = _a0[index] + _a1[index] * ir11[x] + _a2[index] * ir12[x];
                        }
                    }
                }

                _nadirSstBand.writePixels(0, y, width, 1, sst, ProgressMonitor.NULL);

                pm.worked(1);
                if (pm.isCanceled()) {
                    _logger.warning(SstConstants.LOG_MSG_PROC_CANCELED);
                    setCurrentStatus(SstConstants.STATUS_ABORTED);
                    return;
                }
            }
        } finally {
            pm.done();
        }
        _logger.info(SstConstants.LOG_MSG_PROC_SUCCESS);
    }

    /**
     * Retrieves the coefficient set index based on the pixel location in the scanline for the dual view sst processing
     */
    private int getDualCoefficientIndex(final int pixPos) {
        return _dualCoeffMap[pixPos];
    }

    /**
     * Retrieves the coefficient set index based on the pixel location in the scanline for the nadir view sst
     * processing
     */
    private int getNadirCoefficientIndex(final int pixPos) {
        return _nadirCoeffMap[pixPos];
    }

    /**
     * Closes all open products.
     */
    private void closeProducts() throws IOException {
        if (_inputProduct != null) {
            _inputProduct.closeProductReader();
        }

        if (_outputProduct != null) {
            if (isAborted()) {
                _outputProduct.getProductWriter().deleteOutput();
            }
            _outputProduct.closeProductWriter();
        }
    }

    @Override
    protected void cleanupAfterFailure() {
        try {
            closeProducts();
        } catch (IOException e) {
            _logger.severe(e.getMessage());
            Debug.trace(e);
        }
    }
}
