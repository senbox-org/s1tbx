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
package org.esa.beam.processor.smac.ui;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.dataio.envisat.DDDBException;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.DefaultRequestElementFactory;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.processor.smac.SmacConstants;
import org.esa.beam.processor.smac.SmacRequestElementFactory;
import org.esa.beam.processor.smac.SmacUtils;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.math.RsMathUtils;

import javax.swing.JComponent;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * The <code>SmacRequestParameterPool</code> class contains all the functionality which is needed to modify some of the
 * parameters in a SMAC processing request.
 * <p/>
 * <p>Also, it prepares the parameters which are needed to return a modified processing request.
 *
 * @deprecated since BEAM 4.11. No replacement.
 */
@Deprecated
public class SmacRequestParameterPool {

    private SmacRequestEditor _editor;
    private SmacRequestElementFactory _sREFactory = SmacRequestElementFactory.getInstance();
    private ParamGroup _paramGroup;
    private String _outFileFormat;
    private File _requestFile;
    private Logger _logger;

    /**
     * This constructor creates an instance of SmacRequestParameterPool.
     * @param editor The editor to create the parameter pool for.
     */
    public SmacRequestParameterPool(SmacRequestEditor editor) {
        _editor = editor;
        createParamGroup();
        _logger = Logger.getLogger(SmacConstants.LOGGER_NAME);
    }

    /**
     * Gets the named Parameter or null if it doesn't exitst in this DataModule.
     *
     * @param name the name of the Parameter to get
     *
     * @return the named Parameter or null if it doesn't exist.
     */
    public Parameter getParameter(String name) {
        return _paramGroup.getParameter(name);
    }

    private void addParameter(final Parameter parameter) {
        if (_paramGroup == null) {
            _paramGroup = new ParamGroup();
        }
        _paramGroup.addParameter(parameter);
    }

    public ParamGroup getParamGroup() {
        return _paramGroup;
    }

    /**
     * Creates a new request with the current status of elements in this class.
     *
     * @return a new request with the current status of elements in this class.
     */
    public Request getRequest() {
        Request request = new Request();
        request.setType(SmacConstants.REQUEST_TYPE);
        request.setFile(_requestFile);
        prepareParameters(request);
        prepareInputProduct(request);
        prepareOutputProduct(request);
        return request;
    }

    /**
     * Creates a new request with all parameters set to their default values
     * @return returns the default request.
     */
    public Request getDefaultRequest() {
        Request request = new Request();
        request.setType("SMAC");
        request.addInputProduct(createDefaultInputProduct());
        request.addOutputProduct(createDefaultOutputProduct());
        request.addParameter(_sREFactory.createDefaultLogPatternParameter(SmacConstants.DEFAULT_LOG_PREFIX));
        try {
            request.addParameter(_sREFactory.createLogToOutputParameter("false"));
        } catch (ParamValidateException e) {
            _logger.warning("Unable to validate parameter '" + ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
            Debug.trace(e);
        }
        request.addParameter(getParamFromFactory(SmacConstants.PRODUCT_TYPE_PARAM_NAME));
        request.addParameter(getParamFromFactory(SmacConstants.BANDS_PARAM_NAME));
        request.addParameter(getParamFromFactory(SmacConstants.AEROSOL_TYPE_PARAM_NAME));
        request.addParameter(getParamFromFactory(SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME));
        request.addParameter(getParamFromFactory(SmacConstants.USE_MERIS_ADS_PARAM_NAME));
        request.addParameter(getParamFromFactory(SmacConstants.SURFACE_AIR_PRESSURE_PARAM_NAME));
        request.addParameter(getParamFromFactory(SmacConstants.OZONE_CONTENT_PARAM_NAME));
        request.addParameter(getParamFromFactory(SmacConstants.RELATIVE_HUMIDITY_PARAM_NAME));
        request.addParameter(getParamFromFactory(SmacConstants.DEFAULT_REFLECT_FOR_INVALID_PIX_PARAM_NAME));
        request.addParameter(getParamFromFactory(SmacConstants.BITMASK_PARAM_NAME));
        request.addParameter(getParamFromFactory(SmacConstants.BITMASK_FORWARD_PARAM_NAME));
        request.addParameter(getParamFromFactory(SmacConstants.BITMASK_NADIR_PARAM_NAME));
        return request;
    }

    /**
     * Sets the current state of this class to the state of the given request.
     * @param request The request to set.
     */
    public void setRequest(Request request) {
        _requestFile = request.getFile();
        resetParameters(request);
        resetInputProduct(request);
        resetOutputProduct(request);
        resetLoggingParameter(request);
    }

    public void setOutputProductFormat(String format) {
        _outFileFormat = format;
    }


    /**
     * Scans the input product stored in the request for all the bands it contains. When the request contains no input
     * product - nothing happens.
     * <p/>
     * @param request the <code>Request</code> currently set
     *
     * @throws org.esa.beam.framework.processor.ProcessorException
     *          when a failure occurs
     */
    public void scanInputProductBands(Request request) throws ProcessorException {
        if (request.getNumInputProducts() > 0) {
            ProductRef ref = request.getInputProductAt(0);
            scanInputProductBands(ref.getFile());
        }
    }

    ///////////////////////////////////////////////////
    ///  end of public methods
    ///////////////////////////////////////////////////

    private void createParamGroup() {
        _paramGroup = new ParamGroup();
        addParameter(_sREFactory.createDefaultInputProductParameter());
        addParameter(createOutputProductParameter());
        addParameter(getParamFromFactory(SmacConstants.LOG_FILE_PARAM_NAME));
        addParameter(getParamFromFactory(SmacConstants.PRODUCT_TYPE_PARAM_NAME));
        addParameter(getParamFromFactory(SmacConstants.BANDS_PARAM_NAME));
        addParameter(getParamFromFactory(SmacConstants.AEROSOL_TYPE_PARAM_NAME));
        addParameter(getParamFromFactory(SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME));
        addParameter(getParamFromFactory(SmacConstants.HORIZONTAL_VISIBILITY_PARAM_NAME));
        addParameter(getParamFromFactory(SmacConstants.USE_MERIS_ADS_PARAM_NAME));
        addParameter(getParamFromFactory(SmacConstants.SURFACE_AIR_PRESSURE_PARAM_NAME));
        addParameter(getParamFromFactory(SmacConstants.OZONE_CONTENT_PARAM_NAME));
        addParameter(getParamFromFactory(SmacConstants.RELATIVE_HUMIDITY_PARAM_NAME));
        addParameter(getParamFromFactory(SmacConstants.DEFAULT_REFLECT_FOR_INVALID_PIX_PARAM_NAME));
        addParameter(getParamFromFactory(SmacConstants.BITMASK_PARAM_NAME));
        addParameter(getParamFromFactory(SmacConstants.BITMASK_FORWARD_PARAM_NAME));
        addParameter(getParamFromFactory(SmacConstants.BITMASK_NADIR_PARAM_NAME));
        addParameter(_sREFactory.createDefaultLogPatternParameter(SmacConstants.DEFAULT_LOG_PREFIX));
        try {
            addParameter(_sREFactory.createLogToOutputParameter("false"));
        } catch (ParamValidateException e) {
            _logger.warning("Unable to validate parameter '" + ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
            Debug.trace(e);
        }

        getParameter(SmacConstants.SURFACE_AIR_PRESSURE_PARAM_NAME).getEditor().setEnabled(false);
        getParameter(SmacConstants.OZONE_CONTENT_PARAM_NAME).getEditor().setEnabled(false);
        getParameter(SmacConstants.RELATIVE_HUMIDITY_PARAM_NAME).getEditor().setEnabled(false);

        _paramGroup.addParamChangeListener(createParamChangeListener());
    }

    private ParamChangeListener createParamChangeListener() {
        return new ParamChangeListener() {

            @Override
            public void parameterValueChanged(ParamChangeEvent e) {
                parameterChanged(e);
            }
        };
    }

    /**
     * Callback invoked on changes in a parameter
     *
     * @param e the event that triggered the callback
     */
    private void parameterChanged(final ParamChangeEvent e) {
        final Parameter param = e.getParameter();
        final String paramName = param.getName();

        if (SmacConstants.USE_MERIS_ADS_PARAM_NAME.equals(paramName)) {
            handleParameterChangedEventUseMeris(param);
        } else if (SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME.equals(paramName)) {
            final Parameter toUpdate = getParameter(SmacConstants.HORIZONTAL_VISIBILITY_PARAM_NAME);
            float value = ((Number) param.getValue()).floatValue();
            value = RsMathUtils.koschmiederInv(value);
            toUpdate.setValue(new Float(value), null);
        } else if (SmacConstants.HORIZONTAL_VISIBILITY_PARAM_NAME.equals(paramName)) {
            final Parameter toUpdate = getParameter(SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME);
            float value = ((Number) param.getValue()).floatValue();
            value = RsMathUtils.koschmieder(value);
            toUpdate.setValue(new Float(value), null);
        } else if (DefaultRequestElementFactory.INPUT_PRODUCT_PARAM_NAME.equals(paramName)) {
            final String type = scanFileType(param);
            if (!SmacUtils.isSupportedProductType(type)) {
                if (_editor != null && type.trim().length() != 0) {
                    _editor.showWarningDialog(
                            SmacConstants.LOG_MSG_UNSUPPORTED_INPUT_1 + type + SmacConstants.LOG_MSG_UNSUPPORTED_INPUT_2);
                }
            } else {
                setInputProductType(type);
                scanInputProductBands(param);
                toggleMerisECMWFButton(type);
                toggleBitmaskEditor(type);
            }
        } else if (DefaultRequestElementFactory.OUTPUT_PRODUCT_PARAM_NAME.equals(paramName)) {
            final String valueAsText = param.getValueAsText();
            if (_editor != null && valueAsText.trim().length() <= 0) {
                _editor.showWarningDialog("No output product specified.");      /*I18N*/
            }

        }
    }

    private void setInputProductType(String type) {
        Parameter typeParameter = getParameter(SmacConstants.PRODUCT_TYPE_PARAM_NAME);
        typeParameter.setValueAsText(type, null);
    }

    /*
     * Toggles the bitmask editor between the appropriate types Calls back to editor class.
     */
    private void toggleBitmaskEditor(String type) {
        if (_editor != null) {
            _editor.setBitmaskEditor(type);
        }
    }

    /**
     * Toggles the meris ECMWF data control and all associated controls according to the product type string passed in
     *
     * @param type the product type
     */
    private void toggleMerisECMWFButton(String type) {
        Parameter ecmwfParam = getParameter(SmacConstants.USE_MERIS_ADS_PARAM_NAME);
        ecmwfParam.setValue(Boolean.FALSE, null);
        JComponent component = ecmwfParam.getEditor().getComponent();
        if (SmacUtils.isSupportedMerisProductType(type)) {
            component.setEnabled(true);
        } else {
            component.setEnabled(false);
        }
        component.repaint();
    }

    private Parameter createOutputProductParameter() {
        final File defaultOutFile = new File(SystemUtils.getUserHomeDir(), SmacConstants.DEFAULT_FILE_NAME);
        final int fsm = ParamProperties.FSM_FILES_ONLY;
        final ParamProperties paramProps = _sREFactory.createFileParamInfo(fsm, defaultOutFile);
        paramProps.setLabel(SmacConstants.OUTPUT_PRODUCT_LABELTEXT);
        paramProps.setDescription(SmacConstants.OUTPUT_PRODUCT_DESCRIPTION);
        final Parameter param = new Parameter(SmacConstants.OUTPUT_PRODUCT_PARAM_NAME, paramProps);
        param.setDefaultValue();
        return param;
    }

    private Parameter getParamFromFactory(String compName) {
        return _sREFactory.createParamWithDefaultValueSet(compName);
    }

    private void prepareOutputProduct(Request request) {
        String fileName = _paramGroup.getParameter(SmacConstants.OUTPUT_PRODUCT_PARAM_NAME).getValueAsText();

        request.addOutputProduct(ProcessorUtils.createProductRef(fileName, _outFileFormat));
    }

    private void prepareInputProduct(Request request) {
        try {
            File inputFile = (File) getParameter(DefaultRequestElementFactory.INPUT_PRODUCT_PARAM_NAME).getValue();
            request.addInputProduct(_sREFactory.createInputProductRef(inputFile, null, null));
        } catch (RequestElementFactoryException e) {
            Debug.trace(e);
        }
    }

    private void addNewParam(String name, Request request) {
        Parameter toUpdate = _sREFactory.createParamWithDefaultValueSet(name);
        Parameter param = getParameter(name);
        toUpdate.setProperties(param.getProperties());
        toUpdate.setValue(param.getValue(), null);
        request.addParameter(toUpdate);
    }

    private void prepareParameters(Request request) {
        addNewParam(SmacConstants.PRODUCT_TYPE_PARAM_NAME, request);
        addNewParam(SmacConstants.BANDS_PARAM_NAME, request);
        addNewParam(SmacConstants.AEROSOL_TYPE_PARAM_NAME, request);
        addNewParam(SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME, request);
        addNewParam(SmacConstants.USE_MERIS_ADS_PARAM_NAME, request);
        addNewParam(SmacConstants.SURFACE_AIR_PRESSURE_PARAM_NAME, request);
        addNewParam(SmacConstants.OZONE_CONTENT_PARAM_NAME, request);
        addNewParam(SmacConstants.RELATIVE_HUMIDITY_PARAM_NAME, request);
        addNewParam(SmacConstants.DEFAULT_REFLECT_FOR_INVALID_PIX_PARAM_NAME, request);
        // check which bitmask expression
        Parameter prodType = getParameter(SmacConstants.PRODUCT_TYPE_PARAM_NAME);
        if (SmacUtils.isSupportedMerisProductType(prodType.getValueAsText())) {
            // meris
            addNewParam(SmacConstants.BITMASK_PARAM_NAME, request);
        } else {
            // aatsr
            addNewParam(SmacConstants.BITMASK_FORWARD_PARAM_NAME, request);
            addNewParam(SmacConstants.BITMASK_NADIR_PARAM_NAME, request);
        }

        request.addParameter(_paramGroup.getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME));
        request.addParameter(_paramGroup.getParameter(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME));
    }

    private void resetOutputProduct(Request request) {
        if (request.getNumOutputProducts() > 0) {
            ProductRef outputProduct = request.getOutputProductAt(0);
            _outFileFormat = outputProduct.getFileFormat();
            Parameter param = getParameter(SmacConstants.OUTPUT_PRODUCT_PARAM_NAME);
            resetFileParameter(outputProduct, param);
        }
    }

    private void resetInputProduct(Request request) {
        if (request.getNumInputProducts() > 0) {
            ProductRef inputProduct = request.getInputProductAt(0);
            Parameter param = getParameter(DefaultRequestElementFactory.INPUT_PRODUCT_PARAM_NAME);
            resetFileParameter(inputProduct, param);
        }
    }

    /**
     * Sets the logging related GUI components to the values set in the request.
     *
     * @param request the request to be checked
     */
    private void resetLoggingParameter(Request request) {
        Parameter param;
        Parameter toUpdate;

        param = request.getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME);
        if (param != null) {
            toUpdate = _paramGroup.getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME);
            toUpdate.setValue(param.getValue(), null);
        }
        param = request.getParameter(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME);
        if (param != null) {
            toUpdate = _paramGroup.getParameter(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME);
            toUpdate.setValue(param.getValue(), null);
        }
    }

    /*
     * Reads the parameter values out of threquest and sets parameters accordingly.
     */
    private void resetParameters(Request request) {
        Parameter param;
        param = request.getParameter(SmacConstants.PRODUCT_TYPE_PARAM_NAME);
        if (param != null) {
            resetParameter(param);
            toggleBitmaskEditor(param.getValueAsText());
        }
        resetParameter(request.getParameter(SmacConstants.BANDS_PARAM_NAME));
        resetParameter(request.getParameter(SmacConstants.AEROSOL_TYPE_PARAM_NAME));
        resetParameter(request.getParameter(SmacConstants.AEROSOL_OPTICAL_DEPTH_PARAM_NAME));
        resetParameter(request.getParameter(SmacConstants.USE_MERIS_ADS_PARAM_NAME));
        resetParameter(request.getParameter(SmacConstants.SURFACE_AIR_PRESSURE_PARAM_NAME));
        resetParameter(request.getParameter(SmacConstants.OZONE_CONTENT_PARAM_NAME));
        resetParameter(request.getParameter(SmacConstants.RELATIVE_HUMIDITY_PARAM_NAME));
        resetParameter(request.getParameter(SmacConstants.DEFAULT_REFLECT_FOR_INVALID_PIX_PARAM_NAME));
        resetParameter(request.getParameter(SmacConstants.BITMASK_PARAM_NAME));
        resetParameter(request.getParameter(SmacConstants.BITMASK_FORWARD_PARAM_NAME));
        resetParameter(request.getParameter(SmacConstants.BITMASK_NADIR_PARAM_NAME));
        //resetParameter(request.getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME));
        //resetParameter(request.getParameter(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME));
    }

    private void resetParameter(Parameter param) {
        if (param != null) {
            String name = param.getName();
            getParameter(name).setValue(param.getValue(), null);
        }
    }

    private static void resetFileParameter(ProductRef productReference, Parameter parameter) {
        Guardian.assertNotNull("productReference", productReference);
        Guardian.assertNotNull("parameter", parameter);
        File file = new File(productReference.getFilePath());
        parameter.setValue(file, null);
    }

    private void handleParameterChangedEventUseMeris(Parameter param) {
        boolean enable = (Boolean) param.getValue();
        getParameter(SmacConstants.SURFACE_AIR_PRESSURE_PARAM_NAME).getEditor().setEnabled(!enable);
        getParameter(SmacConstants.OZONE_CONTENT_PARAM_NAME).getEditor().setEnabled(!enable);
        getParameter(SmacConstants.RELATIVE_HUMIDITY_PARAM_NAME).getEditor().setEnabled(!enable);
    }

    /**
     * Scans the input product given in the parameter for a list of available bands
     *
     * @param inputProductParam the parameter describing the input product currently set
     */
    private void scanInputProductBands(Parameter inputProductParam) {
        File file = (File) inputProductParam.getValue();
        if (file != null) {
            try {
                scanInputProductBands(file);
            } catch (ProcessorException e) {
                Debug.trace(e);
            }
        }
    }

    /**
     * Scans the input product at the given URL for a list of available bands
     *
     * @param inFile the <code>URL</code> describing the file location
     *
     * @throws org.esa.beam.framework.processor.ProcessorException
     *          when product does not exist or rises an error when opened
     */
    private void scanInputProductBands(File inFile) throws ProcessorException {
        Product inProduct;

        if ((inFile == null) || !inFile.exists() || !inFile.isFile()) {
            return;
        }

        try {
            inProduct = ProductIO.readProduct(inFile);
        } catch (IOException e) {
            throw new ProcessorException(e.getMessage());
        } catch (DDDBException e) {
            throw new ProcessorException(e.getMessage());
        }

        if (inProduct == null) {
            throw new ProcessorException("unable to read input product");
        }

        Band[] bands = inProduct.getBands();
        Vector<String> bandNamesVec = new Vector<String>();

        for (Band band : bands) {
            String bandName = band.getName();
            if (band.getSpectralBandIndex() != -1 && isSupportedBand(bandName)) {
                bandNamesVec.add(bandName);
            }
        }
        inProduct.dispose();

        String[] bandNames = bandNamesVec.toArray(new String[bandNamesVec.size()]);
        Parameter bandParam = getParameter(SmacConstants.BANDS_PARAM_NAME);

        bandParam.setValueSet(bandNames);
        bandParam.getEditor().reconfigureUI();
    }

    private boolean isSupportedBand(String bandName) {
        List<String> bandNames = new ArrayList<String>(32);
        bandNames.addAll(Arrays.asList(SmacConstants.MERIS_L1B_BANDS));
        bandNames.addAll(Arrays.asList(SmacConstants.AATSR_L1B_BANDS));

        return bandNames.contains(bandName);
    }

    /*
     * Scans the given input product for the product type.
     *
     * @return a string containing the product type
     */
    private static String scanFileType(Parameter inputProductParam) {
        String stRet = "";

        File file = (File) inputProductParam.getValue();
        if ((file != null) && file.exists() && file.isFile()) {

            Product inProduct = null;
            try {
                inProduct = ProductIO.readProduct(file);
                if (inProduct != null) {
                    stRet = inProduct.getProductType();
                }
            } catch (IOException e) {
                Debug.trace(e);
            }finally {
                if(inProduct != null) {
                    inProduct.dispose();
                }
            }
        }
        return stRet;
    }

    /*
     * Creates a <code>ProductRef</code> pointing to the smac default input product
     */
    private static ProductRef createDefaultInputProduct() {
        return new ProductRef(new File(""));
    }

    /*
     * Creates a <code>ProductRef</code> pointing to the smac default input product
     */
    private static ProductRef createDefaultOutputProduct() {
        File defaultOutFile = new File(SystemUtils.getUserHomeDir(), SmacConstants.DEFAULT_FILE_NAME);
        ProductRef ref = new ProductRef(defaultOutFile);
        ref.setFileFormat(DimapProductConstants.DIMAP_FORMAT_NAME);

        return ref;
    }

}
