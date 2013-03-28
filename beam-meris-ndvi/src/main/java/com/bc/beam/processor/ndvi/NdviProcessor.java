/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package com.bc.beam.processor.ndvi;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.FlagCoding;
import org.esa.beam.framework.datamodel.MetadataAttribute;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestElementFactory;
import org.esa.beam.framework.processor.ui.IOParameterPage;
import org.esa.beam.framework.processor.ui.MultiPageProcessorUI;
import org.esa.beam.framework.processor.ui.ProcessingParameterPage;
import org.esa.beam.framework.processor.ui.ProcessorUI;
import org.esa.beam.util.ProductUtils;

import java.awt.Color;
import java.io.File;
import java.io.IOException;
import java.util.logging.Logger;

/**
 * The <code>NdviProcessor</code> implements all specific functionality to calculate a ndvi product from a given MERIS
 * product. This simple processor does not take any flags into account, it just calculates the ndvi over the whole
 * product.
 *
 * @deprecated since BEAM 4.11. No replacement.
 */
@Deprecated
public class NdviProcessor extends Processor {

    // Constants
    public static final String PROCESSOR_NAME = "NDVI Processor";
    public static final String PROCESSOR_VERSION = "1.2.203";
    public static final String PROCESSOR_COPYRIGHT = "Copyright (C) 2003 by Brockmann Consult (info@brockmann-consult.de)";

    public static final String LOGGER_NAME = "beam.processor.processor";

    public static final String HELP_ID = "NDVIProcessorPlugIn";
    public static final String HELPSET_PATH = "com/bc/beam/processor/ndvi/help/NdviProcessor.hs";

    public static final String DEFAULT_OUTPUT_FORMAT = DimapProductConstants.DIMAP_FORMAT_NAME;
    private static final String DEFAULT_LOG_PREFIX = "ndvi";
    public static final String DEFAULT_OUTPUT_PRODUCT_NAME = "MER_NDVI2P.dim";

    public static final String REQUEST_TYPE = "NDVI";

    public static final String NDVI_PRODUCT_TYPE = "MER_NDVI2P";
    public static final String NDVI_BAND_NAME = "ndvi";
    public static final String NDVI_FLAGS_BAND_NAME = "ndvi_flags";
    public static final String NDVI_ARITHMETIC_FLAG_NAME = "NDVI_ARITHMETIC";
    public static final String NDVI_LOW_FLAG_NAME = "NDVI_NEGATIVE";
    public static final String NDVI_HIGH_FLAG_NAME = "NDVI_SATURATION";
    public static final int NDVI_ARITHMETIC_FLAG_VALUE = 1;
    public static final int NDVI_LOW_FLAG_VALUE = 1 << 1;
    public static final int NDVI_HIGH_FLAG_VALUE = 1 << 2;

    public static final String L1FLAGS_INPUT_BAND_NAME = "l1_flags";


    public static final String LOWER_BAND_PARAM_NAME = "lower_band";
    public static final String LOWER_BAND_PARAM_DEFAULT = "radiance_6";

    public static final String LOWER_FACTOR_PARAM_NAME = "lower_factor";
    public static final Float LOWER_FACTOR_PARAM_DEFAULT = 1.0f;

    public static final String UPPER_BAND_PARAM_NAME = "upper_band";
    public static final String UPPER_BAND_PARAM_DEFAULT = "radiance_10";

    public static final String UPPER_FACTOR_PARAM_NAME = "upper_factor";
    public static final Float UPPER_FACTOR_PARAM_DEFAULT = 1.0f;

    // Fields
    private Product _inputProduct;
    private Product _outputProduct;
    private Band _lowerInputBand;
    private Band _upperInputBand;
    private Band _ndviOutputBand;
    private Band _ndviFlagsOutputBand;
    private Logger _logger;
    private float _upperFactor = UPPER_FACTOR_PARAM_DEFAULT;
    private float _lowerFactor = LOWER_FACTOR_PARAM_DEFAULT;
    private String _upperBandName = UPPER_BAND_PARAM_DEFAULT;
    private String _lowerBandName = LOWER_BAND_PARAM_DEFAULT;

    public NdviProcessor() {
        _logger = Logger.getLogger(LOGGER_NAME);
        setDefaultHelpId(HELP_ID);
        setDefaultHelpSetPath(HELPSET_PATH);
    }

    /**
     * Worker method invoked by framework to process a single request.
     */
    @Override
    public void process(ProgressMonitor pm) throws ProcessorException {
        ProcessorUtils.setProcessorLoggingHandler(DEFAULT_LOG_PREFIX, getRequest(),
                                                  getName(), getVersion(), getCopyrightInformation());

        pm.beginTask("Processing NDVI...", 10);
        try {
            _logger.info("Started processing ...");

            // check the request type
            final Request request = getRequest();
            Request.checkRequestType(request, REQUEST_TYPE);

            _lowerFactor = (Float) request.getParameter(LOWER_FACTOR_PARAM_NAME).getValue();
            _upperFactor = (Float) request.getParameter(UPPER_FACTOR_PARAM_NAME).getValue();

            _upperBandName = request.getParameter(UPPER_BAND_PARAM_NAME).getValueAsText();
            _lowerBandName = request.getParameter(LOWER_BAND_PARAM_NAME).getValueAsText();

            // load input product
            loadInputProduct();

            // create the output product
            createOutputProduct(SubProgressMonitor.create(pm, 1));

            // and process the processor
            processNdvi(SubProgressMonitor.create(pm, 9));
        } catch (IOException e) {
            // catch all exceptions expect ProcessorException and throw ProcessorException
            throw new ProcessorException(e.getMessage());
        } finally {
            pm.done();
            if (_outputProduct != null) {
                _outputProduct.dispose();
            }
            if (_inputProduct != null) {
                _inputProduct.dispose();
            }
        }
    }

    /**
     * Retrieves the name of the processor
     */
    @Override
    public String getName() {
        return PROCESSOR_NAME;
    }

    /**
     * Retrieves a version string of the processor
     */
    @Override
    public String getVersion() {
        return PROCESSOR_VERSION;
    }

    /**
     * Retrieves copyright information of the processor
     */
    @Override
    public String getCopyrightInformation() {
        return PROCESSOR_COPYRIGHT;
    }

    /**
     * Retrieves the request element facory for this processor.
     */
    @Override
    public RequestElementFactory getRequestElementFactory() {
        return NdviRequestElementFactory.getInstance();
    }

    /**
     * Creates the UI for the processor. Override to perform processor specific
     * UI initializations.
     */
    @Override
    public ProcessorUI createUI() throws ProcessorException {

        final IOParameterPage ioParameterPage = new IOParameterPage(new IOParameterPage.InputProductValidator() {
            @Override
            public boolean validate(final Product product) {
                if (!isMerisL1Type(product.getProductType())) {
                    setErrorMessage("Invalid product type: MERIS Level 1b required.");
                    return false;
                }
                return true;
            }
        });
        ioParameterPage.setDefaultOutputProductFileName(DEFAULT_OUTPUT_PRODUCT_NAME);
        ioParameterPage.setDefaultLogPrefix(DEFAULT_LOG_PREFIX);

        final NdviRequestElementFactory factory = NdviRequestElementFactory.getInstance();
        final Parameter lowerFactorParam = factory.createParameter(NdviProcessor.LOWER_FACTOR_PARAM_NAME);
        final Parameter lowerBandParam = factory.createParameter(NdviProcessor.LOWER_BAND_PARAM_NAME);
        final Parameter upperFactorParam = factory.createParameter(NdviProcessor.UPPER_FACTOR_PARAM_NAME);
        final Parameter upperBandParam = factory.createParameter(NdviProcessor.UPPER_BAND_PARAM_NAME);
        final ParamGroup processingParamGroup = new ParamGroup();

        processingParamGroup.addParameter(lowerBandParam);
        processingParamGroup.addParameter(lowerFactorParam);

        processingParamGroup.addParameter(upperBandParam);
        processingParamGroup.addParameter(upperFactorParam);

        final ProcessingParameterPage processingParamPage = new ProcessingParameterPage(processingParamGroup);
        processingParamPage.setTitle("NDVI Parameter");

        final MultiPageProcessorUI ndviProcessorUI = new MultiPageProcessorUI(NdviProcessor.REQUEST_TYPE);
        ndviProcessorUI.addPage(ioParameterPage);
        ndviProcessorUI.addPage(processingParamPage);
        return ndviProcessorUI;

    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Loads the input product from the request. Opens the product and opens
     * both bands needed to process the processor.
     */
    private void loadInputProduct() throws ProcessorException, IOException {
        _inputProduct = loadInputProduct(0);

        if (!isMerisL1Type(_inputProduct.getProductType())) {
            throw new ProcessorException("Invalid product type: MERIS Level 1b required.");
        }

        _lowerInputBand = _inputProduct.getBand(_lowerBandName);
        if (_lowerInputBand == null) {
            throw new ProcessorException("Can not load band " + _lowerBandName);
        }
        _logger.info(ProcessorConstants.LOG_MSG_LOADED_BAND + _lowerBandName);

        _upperInputBand = _inputProduct.getBand(_upperBandName);
        if (_upperInputBand == null) {
            throw new ProcessorException("Can not load band " + _upperBandName);
        }
        _logger.info(ProcessorConstants.LOG_MSG_LOADED_BAND + _upperBandName);

        _logger.info(ProcessorConstants.LOG_MSG_LOADED_BAND + L1FLAGS_INPUT_BAND_NAME);
    }

    /**
     * Creates the output product skeleton.
     */
    private void createOutputProduct(ProgressMonitor pm) throws ProcessorException, IOException {
        // get the request from the base class
        // -----------------------------------
        final Request request = getRequest();

        // get the scene size from the input product
        // -----------------------------------------
        final int sceneWidth = getSceneWidth();
        final int sceneHeight = getSceneHeight();

        // get the output product from the request. The request holds objects of
        // type ProductRef which contain all the information needed here
        // --------------------------------------------------------------------
        final ProductRef outputRef;

        // the request can contain any number of output products, we take the first ..
        outputRef = request.getOutputProductAt(0);
        if (outputRef == null) {
            throw new ProcessorException("No output product in request");
        }

        // create the in memory represenation of the output product
        // ---------------------------------------------------------
        // the product itself
        _outputProduct = new Product(DEFAULT_OUTPUT_PRODUCT_NAME, NDVI_PRODUCT_TYPE, sceneWidth, sceneHeight);

        // create and add the NDVI band
        //
        _ndviOutputBand = new Band(NDVI_BAND_NAME, ProductData.TYPE_UINT8, sceneWidth, sceneHeight);
        _ndviOutputBand.setScalingOffset(0.0);
        _ndviOutputBand.setScalingFactor(1.0 / 255.0);
        _outputProduct.addBand(_ndviOutputBand);

        // copy all tie point grids to output product
        ProductUtils.copyTiePointGrids(_inputProduct, _outputProduct);
        // copy L1b flag band
        copyFlagBands(_inputProduct, _outputProduct);
        // copy geo-coding to the output product
        copyGeoCoding(_inputProduct, _outputProduct);

        // create and add the NDVI flags coding
        //
        final FlagCoding ndviFlagCoding = createNdviFlagCoding();
        _outputProduct.getFlagCodingGroup().add(ndviFlagCoding);

        // create and add the NDVI flags band
        //
        _ndviFlagsOutputBand = new Band(NDVI_FLAGS_BAND_NAME, ProductData.TYPE_UINT8, sceneWidth, sceneHeight);
        _ndviFlagsOutputBand.setDescription("NDVI specific flags");
        _ndviFlagsOutputBand.setSampleCoding(ndviFlagCoding);
        _outputProduct.addBand(_ndviFlagsOutputBand);

        // Copy predefined bitmask definitions
        ProductUtils.copyMasks(_inputProduct, _outputProduct);
        ProductUtils.copyOverlayMasks(_inputProduct, _outputProduct);

        _outputProduct.addMask(NDVI_ARITHMETIC_FLAG_NAME, (NDVI_FLAGS_BAND_NAME + "." + NDVI_ARITHMETIC_FLAG_NAME),
                               "An arithmetic exception occurred.",
                               Color.red.brighter(), 0.7);
        _outputProduct.addMask(NDVI_LOW_FLAG_NAME, (NDVI_FLAGS_BAND_NAME + "." + NDVI_LOW_FLAG_NAME),
                               "NDVI value is too low.",
                               Color.red, 0.7);
        _outputProduct.addMask(NDVI_HIGH_FLAG_NAME, (NDVI_FLAGS_BAND_NAME + "." + NDVI_HIGH_FLAG_NAME),
                               "NDVI value is too high.",
                               Color.red.darker(), 0.7);


        // retrieve the default disk writer from the ProductIO package
        // this is the BEAM_DIMAP format, the toolbox native file format
        // and attach to the writer to the output product
        //
        final ProductWriter writer = ProcessorUtils.createProductWriter(outputRef);
        _outputProduct.setProductWriter(writer);

        // and initialize the disk representation
        writer.writeProductNodes(_outputProduct, new File(outputRef.getFilePath()));
        copyBandData(getBandNamesToCopy(), _inputProduct, _outputProduct, pm);
        
        _logger.info("Output product successfully created");
    }

    private int getSceneHeight() {
        return _inputProduct.getSceneRasterHeight();
    }

    private int getSceneWidth() {
        return _inputProduct.getSceneRasterWidth();
    }

    public static FlagCoding createNdviFlagCoding() {

        final FlagCoding ndviFlagCoding = new FlagCoding("ndvi_flags");
        ndviFlagCoding.setDescription("NDVI Flag Coding");

        MetadataAttribute attribute;

        attribute = new MetadataAttribute(NDVI_ARITHMETIC_FLAG_NAME, ProductData.TYPE_INT32);
        attribute.getData().setElemInt(NDVI_ARITHMETIC_FLAG_VALUE);
        attribute.setDescription("NDVI value calculation failed due to an arithmetic exception");
        ndviFlagCoding.addAttribute(attribute);

        attribute = new MetadataAttribute(NDVI_LOW_FLAG_NAME, ProductData.TYPE_INT32);
        attribute.getData().setElemInt(NDVI_LOW_FLAG_VALUE);
        attribute.setDescription("NDVI value is too low");
        ndviFlagCoding.addAttribute(attribute);

        attribute = new MetadataAttribute(NDVI_HIGH_FLAG_NAME, ProductData.TYPE_INT32);
        attribute.getData().setElemInt(NDVI_HIGH_FLAG_VALUE);
        attribute.setDescription("NDVI value is too high");
        ndviFlagCoding.addAttribute(attribute);

        return ndviFlagCoding;
    }


    /**
     * Performs the actual processing of the output product. Reads both input bands line
     * by line, calculates the processor and writes the result to the output band
     * @param pm
     */
    private void processNdvi(ProgressMonitor pm) throws IOException {

        final int width = getSceneWidth();
        final int height = getSceneHeight();

        // first of all - allocate memory for single scan lines
        //
        final float[] lower = new float[width];
        final float[] upper = new float[width];
        final float[] ndvi = new float[width];
        final int[] ndviFlags = new int[width];

        float highValue, lowValue, ndviValue;
        int ndviFlagsValue;

        pm.beginTask("Generating NDVI pixels...", height);

        try {
            // for all required bands loop over all scanlines
            //
            for (int y = 0; y < height; y++) {

                // read the input data
                //
                _lowerInputBand.readPixels(0, y, width, 1, lower, ProgressMonitor.NULL);
                _upperInputBand.readPixels(0, y, width, 1, upper, ProgressMonitor.NULL);

                // process the complete scanline
                //
                for (int x = 0; x < width; x++) {
                    highValue = _upperFactor * upper[x];
                    lowValue = _lowerFactor * lower[x];
                    ndviValue = (highValue - lowValue) / (highValue + lowValue);
                    ndviFlagsValue = 0;
                    if (Float.isNaN(ndviValue) || Float.isInfinite(ndviValue)) {
                        ndviFlagsValue |= NDVI_ARITHMETIC_FLAG_VALUE;
                        ndviValue = 0f;
                    }
                    if (ndviValue < 0.0f) {
                        ndviFlagsValue |= NDVI_LOW_FLAG_VALUE;
                        //ndviValue = 0.0f;
                    }
                    if (ndviValue > 1.0f) {
                        ndviFlagsValue |= NDVI_HIGH_FLAG_VALUE;
                        //ndviValue = 1.0f;
                    }
                    ndvi[x] = ndviValue;
                    ndviFlags[x] = ndviFlagsValue;
                }

                // write the result
                //
                _ndviOutputBand.writePixels(0, y, width, 1, ndvi, ProgressMonitor.NULL);
                _ndviFlagsOutputBand.writePixels(0, y, width, 1, ndviFlags, ProgressMonitor.NULL);

                // Notify process listeners about processing progress and
                // check whether or not processing shall be terminated
                //
                pm.worked(1);
                if (pm.isCanceled()) {
                    // Processing terminated!
                    // --> Completely remove output product
                    _outputProduct.getProductWriter().deleteOutput();
                    // Immediately terminate now
                    _logger.info(ProcessorConstants.LOG_MSG_PROC_CANCELED);
                    _logger.info("The output product is completely removed.");
                    return;
                }
            }
        } finally {
            pm.done();
        }

        // If processing is not interupted the logger logs success
        //
        _logger.info(ProcessorConstants.LOG_MSG_SUCCESS);
    }

    private static boolean isMerisL1Type(final String productType) {
        return EnvisatConstants.MERIS_L1_TYPE_PATTERN.matcher(productType).matches();
    }

}
