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
package org.esa.beam.processor.flh_mci.ui;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.BooleanExpressionEditor;
import org.esa.beam.framework.processor.DefaultRequestElementFactory;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.ui.AbstractProcessorUI;
import org.esa.beam.framework.processor.ui.ProcessorApp;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.processor.flh_mci.FlhMciConstants;
import org.esa.beam.processor.flh_mci.FlhMciProcessor;
import org.esa.beam.processor.flh_mci.FlhMciRequestElementFactory;
import org.esa.beam.util.BeamConstants;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * @deprecated since BEAM 4.10 - no replacement.
 */
@Deprecated
public class FlhMciUI extends AbstractProcessorUI implements ParamChangeListener {

    private ParamGroup _paramGroup;
    private JTabbedPane _tabbedPane;
    private String[] _formatNames;
    private JComboBox _fileFormatCombo;
    private String _outFileFormat;
    private String _inputProductType;
    private File _requestFile;
    private FlhMciProcessor _processor;
    private Logger _logger;

    private static final String PATHS_TAB_NAME = "I/O Parameters";
    private static final String PARAMETER_TAB_NAME = "Processing Parameters";
    private static final String _outFormatLabel = "Output product format:";
    private static final String _presetProductMismatchMessage = "Input product does not match current preset";

    /**
     * Constructs the object with default parameters. Initializes the array of output product formats. Creates the
     * parametergroup containing all parameters used by this processor.
     */
    public FlhMciUI() {
        _tabbedPane = null;
        _logger = Logger.getLogger(FlhMciConstants.LOGGER_NAME);
        _inputProductType = "";
        _outFileFormat = DimapProductConstants.DIMAP_FORMAT_NAME;
        scanWriterFormatStrings();
        createParamGroup();
    }

    /**
     * Sets the processor class for this UI.
     *
     * @param processor the processor driving this UI
     */
    public void setProcessor(FlhMciProcessor processor) {
        _processor = processor;
    }

    /**
     * Retrieves the base component for the processor specific user interface classes. This can be any Java Swing
     * containertype. This method creates the UI from scratch if not present
     */
    @Override
    public JComponent getGuiComponent() {
        if (_tabbedPane == null) {
            createUI();
        }

        return _tabbedPane;
    }

    /**
     * Sets a new Request to be edited.
     *
     * @param requests the requests to be edited
     */
    @Override
    public void setRequests(Vector requests) {
        Guardian.assertNotNull("requests", requests);
        if (!requests.isEmpty()) {
            Request request = (Request) requests.elementAt(0);
            _requestFile = request.getFile();
            setInputFile(request);
            setOutputFile(request);
            setParameter(request);
        } else {
            setDefaultRequests();
        }
    }

    /**
     * Retrieves the request currently edited.
     */
    @Override
    public Vector getRequests() throws ProcessorException {
        final Request request = new Request();
        request.setType(FlhMciConstants.REQUEST_TYPE);
        request.setFile(_requestFile);
        getInputFile(request);
        getOutputFile(request);
        getParameter(request);
        final Vector<Request> vRet = new Vector<Request>();
        vRet.add(request);
        return vRet;
    }

    /**
     * Create a new default request and set it in the UI
     */
    @Override
    public void setDefaultRequests() {
        final Vector<Request> init = new Vector<Request>();
        init.add(createDefaultRequest());
        setRequests(init);
        _paramGroup.getParameter(FlhMciConstants.PRESET_PARAM_NAME).setDefaultValue();
    }

    /**
     * Callback for parameter group
     *
     * @param evt the event that triggered the callback
     */
    @Override
    public void parameterValueChanged(final ParamChangeEvent evt) {
        try {
            if (evt.getParameter().getName().equals(DefaultRequestElementFactory.INPUT_PRODUCT_PARAM_NAME)) {
                handleUpdateInputProduct();
            } else if (evt.getParameter().getName().equals(FlhMciConstants.PROCESS_SLOPE_PARAM_NAME)) {
                handleUpdateSlopeBand();
            } else if (evt.getParameter().getName().equals(FlhMciConstants.PRESET_PARAM_NAME)) {
                handleUpdatePreset();
            }
        } catch (ProcessorException e) {
            Debug.trace(e);
        }
    }

    /**
     * Sets the processor app for the UI
     */
    @Override
    public void setApp(final ProcessorApp app) {
        super.setApp(app);
        if (_paramGroup != null) {
            app.markIODirChanges(_paramGroup);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /*
     * Creates a request with all parameters set to default values
     */
    private Request createDefaultRequest() {
        FlhMciRequestElementFactory factory = FlhMciRequestElementFactory.getInstance();
        Request request = new Request();

        request.addInputProduct(createDefaultInputProduct());
        request.addOutputProduct(createDefaultOutputProduct());
        request.addParameter(factory.createParamWithDefaultValueSet(FlhMciConstants.REQUEST_TYPE_PARAM_NAME));
        request.addParameter(factory.createParamWithDefaultValueSet(FlhMciConstants.BAND_LOW_PARAM_NAME));
        request.addParameter(factory.createParamWithDefaultValueSet(FlhMciConstants.BAND_SIGNAL_PARAM_NAME));
        request.addParameter(factory.createParamWithDefaultValueSet(FlhMciConstants.BAND_HIGH_PARAM_NAME));
        request.addParameter(factory.createParamWithDefaultValueSet(FlhMciConstants.LINEHEIGHT_BAND_NAME_PARAM_NAME));
        request.addParameter(factory.createParamWithDefaultValueSet(FlhMciConstants.PROCESS_SLOPE_PARAM_NAME));
        request.addParameter(factory.createParamWithDefaultValueSet(FlhMciConstants.SLOPE_BAND_NAME_PARAM_NAME));
        request.addParameter(factory.createParamWithDefaultValueSet(FlhMciConstants.BITMASK_PARAM_NAME));
        request.addParameter(factory.createParamWithDefaultValueSet(FlhMciConstants.INVALID_PIXEL_VALUE_PARAM_NAME));
        request.addParameter(
                factory.createParamWithDefaultValueSet(FlhMciConstants.CLOUD_CORRECTION_FACTOR_PARAM_NAME));
        request.addParameter(factory.createDefaultLogPatternParameter(FlhMciConstants.DEFAULT_LOG_PREFIX));
        try {
            request.addParameter(factory.createLogToOutputParameter("false"));
        } catch (ParamValidateException e) {
            _logger.warning("Unable to validate parameter '" + ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
            Debug.trace(e);
        }

        return request;
    }

    /*
     * Creates a <code>ProductRef</code> pointing to the FLH/MCI default input product
     */
    private static ProductRef createDefaultInputProduct() {
        FlhMciRequestElementFactory factory = FlhMciRequestElementFactory.getInstance();
        Parameter inProdParam = factory.createDefaultInputProductParameter();

        return new ProductRef(new File(inProdParam.getValueAsText()));
    }

    /*
     * Creates a <code>ProductRef</code> pointing to the SST default output product.
     */
    private static ProductRef createDefaultOutputProduct() {
        FlhMciRequestElementFactory factory = FlhMciRequestElementFactory.getInstance();
        Parameter outProdParam = factory.createDefaultOutputProductParameter();
        File outProd = (File) outProdParam.getValue();

        return new ProductRef(outProd);
    }


    /**
     * Creates the user interface components needed by this processor.
     */
    private void createUI() {
        _tabbedPane = new JTabbedPane();

        JPanel pathPanel = createPathTab();
        JPanel paramPanel = createParameterTab();

        _tabbedPane.add(PATHS_TAB_NAME, pathPanel);
        _tabbedPane.add(PARAMETER_TAB_NAME, paramPanel);
        HelpSys.enableHelp(_tabbedPane, "flhMciScientificTool");

        // initial filetype
        updateOutFileType();
    }

    /**
     * Create the UI tab for the path editing.
     *
     * @return the panel containing the paths tab
     */
    private JPanel createPathTab() {
        int line = 0;
        JPanel panel = GridBagUtils.createDefaultEmptyBorderPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints(null);
        Parameter param;

        // input product
        // -------------
        param = _paramGroup.getParameter(DefaultRequestElementFactory.INPUT_PRODUCT_PARAM_NAME);
        GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, weighty=1, gridy=" + String.valueOf(line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        GridBagUtils.setAttributes(gbc,
                                   "anchor=NORTHWEST, fill=HORIZONTAL, weightx=1, weighty=1, gridy=" + String.valueOf(
                                           line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        // output product
        // --------------
        param = _paramGroup.getParameter(FlhMciConstants.OUTPUT_PRODUCT_PARAM_NAME);
        GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, weighty=0.5, gridy=" + String.valueOf(line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, weighty=1, gridy=" + String.valueOf(line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        JLabel outFormatLabel = new JLabel(_outFormatLabel);
        _fileFormatCombo = new JComboBox(_formatNames);
        _fileFormatCombo.addActionListener(new ActionListener() {

            @Override
            public void actionPerformed(ActionEvent e) {
                updateOutFileType();
            }
        });
        GridBagUtils.setAttributes(gbc,
                                   "anchor=SOUTHWEST, fill=NONE, weightx = 0, weighty=0.5, insets.top=-16, gridy=" + String.valueOf(
                                           line++));
        GridBagUtils.addToPanel(panel, outFormatLabel, gbc);
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, weighty=0.5, insets.top=0 ,gridy=" + String.valueOf(line++));
        GridBagUtils.addToPanel(panel, _fileFormatCombo, gbc);

        // logging
        // -------
        param = _paramGroup.getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME);
        GridBagUtils.setAttributes(gbc,
                                   "anchor=SOUTHWEST, fill=HORIZONTAL, weighty=0.5, insets.bottom=0, gridy=" + String.valueOf(
                                           line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, weighty=0.5, gridy=" + String.valueOf(line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        param = _paramGroup.getParameter(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME);
        GridBagUtils.setAttributes(gbc,
                                   "anchor=SOUTHWEST, fill=HORIZONTAL, weighty=0.5, insets.bottom=0, gridy=" + String.valueOf(
                                           line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        return panel;
    }

    /*
     * Creates the parameter tab for flh/mci processor
     */
    private JPanel createParameterTab() {
        int line = 0;
        JPanel panel = GridBagUtils.createDefaultEmptyBorderPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints(null);
        Parameter param;

        // preset
        addParameterToParamPanel(FlhMciConstants.PRESET_PARAM_NAME, line++, gbc, panel);

        // low band - special positioning
        param = _paramGroup.getParameter(FlhMciConstants.BAND_LOW_PARAM_NAME);
        GridBagUtils.setAttributes(gbc, "gridy=" + String.valueOf(line++));
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, weighty=1, insets.top=4, fill=HORIZONTAL");
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        // high band - special positioning
        param = _paramGroup.getParameter(FlhMciConstants.BAND_HIGH_PARAM_NAME);
        GridBagUtils.setAttributes(gbc, "gridy=" + String.valueOf(line++));
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, weighty=1, insets.top=0");
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        // signal band
        param = _paramGroup.getParameter(FlhMciConstants.BAND_SIGNAL_PARAM_NAME);
        GridBagUtils.setAttributes(gbc, "gridy=" + String.valueOf(line++));
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, weighty=1, insets.bottom=4");
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);
        GridBagUtils.setAttributes(gbc, "insets.bottom=0, fill=NONE");

        // lineheight band name
        addParameterToParamPanel(FlhMciConstants.LINEHEIGHT_BAND_NAME_PARAM_NAME, line++, gbc, panel);

        // process slope - special treatement
        param = _paramGroup.getParameter(FlhMciConstants.PROCESS_SLOPE_PARAM_NAME);
        GridBagUtils.setAttributes(gbc, "gridy=" + String.valueOf(line++));
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, weighty=1, insets.top=4");
        JPanel slopePanel = new JPanel();
        slopePanel.add(param.getEditor().getComponent());
        GridBagUtils.addToPanel(panel, slopePanel, gbc);
        GridBagUtils.setAttributes(gbc, "insets.top=0");

        // slope band name
        addParameterToParamPanel(FlhMciConstants.SLOPE_BAND_NAME_PARAM_NAME, line++, gbc, panel);
        GridBagUtils.setAttributes(gbc, "insets.top=4");
        // bitmask, coud correction and invalid pixel
        addParameterToParamPanel(FlhMciConstants.BITMASK_PARAM_NAME, line++, gbc, panel);
        addParameterToParamPanel(FlhMciConstants.CLOUD_CORRECTION_FACTOR_PARAM_NAME, line++, gbc, panel);
        addParameterToParamPanel(FlhMciConstants.INVALID_PIXEL_VALUE_PARAM_NAME, line++, gbc, panel);

        return panel;
    }

    /*
     * Scans the ProductIO for all product format strings of the registered writer plugins
     */
    private void scanWriterFormatStrings() {
        ProductIOPlugInManager manager = ProductIOPlugInManager.getInstance();
        _formatNames = manager.getAllProductWriterFormatStrings();
    }

    /*
     * Callback for output file format combo box
     */
    private void updateOutFileType() {
        _outFileFormat = (String) _fileFormatCombo.getSelectedItem();
    }

    /*
     * Updates all components according to a change in the input product parameter
     */
    private void handleUpdateInputProduct() throws ProcessorException {
        try {
            final File file = (File) _paramGroup.getParameter(
                    DefaultRequestElementFactory.INPUT_PRODUCT_PARAM_NAME).getValue();
            if (file.canRead()) {
                Product inProduct = ProductIO.readProduct(file);
                if (inProduct == null) {
                    throw new ProcessorException("unable to read input product");
                }

                final Parameter parameter = _paramGroup.getParameter(FlhMciConstants.BITMASK_PARAM_NAME);
                parameter.getProperties().setPropertyValue(BooleanExpressionEditor.PROPERTY_KEY_SELECTED_PRODUCT,
                                                           inProduct);

                _inputProductType = inProduct.getProductType();
                scanForBandNames(inProduct);
                scanForFlags(inProduct);
                setPresetBox(_inputProductType);
            }
        } catch (IOException e) {
            Debug.trace(e);
        }
    }

    /*
     * Updates all components due to a change in the procdess slope parameter
     */
    private void handleUpdateSlopeBand() {
        Parameter paramProc = _paramGroup.getParameter(FlhMciConstants.PROCESS_SLOPE_PARAM_NAME);
        if (paramProc != null) {
            if ((Boolean) paramProc.getValue()) {
                enableSlopeBand(_paramGroup.getParameter(FlhMciConstants.SLOPE_BAND_NAME_PARAM_NAME).getValueAsText());
            } else {
                disableSlopeBand();
            }
        }
    }

    /*
     * Updates all components due to a change in the preset parameter
     */
    private void handleUpdatePreset() {
        Parameter paramPreset = _paramGroup.getParameter(FlhMciConstants.PRESET_PARAM_NAME);
        if (paramPreset != null) {
            setPresetBox(_inputProductType);
            FlhMciPresetManager manager = FlhMciPresetManager.getInstance();
            FlhMciPreset preset = manager.getPresetByName(paramPreset.getValueAsText());

            // when the preset is null, this is a general baseline, there is no specific
            // preset in the database
            if (preset != null) {
                updatePresetParameter(FlhMciConstants.BAND_LOW_PARAM_NAME, preset.getLowBandName());
                updatePresetParameter(FlhMciConstants.BAND_HIGH_PARAM_NAME, preset.getHighBandName());
                updatePresetParameter(FlhMciConstants.BAND_SIGNAL_PARAM_NAME, preset.getSignalBandName());
                updatePresetParameter(FlhMciConstants.LINEHEIGHT_BAND_NAME_PARAM_NAME, preset.getLineheightBandName());
                updatePresetParameter(FlhMciConstants.SLOPE_BAND_NAME_PARAM_NAME, preset.getSlopeBandName());
                updatePresetParameter(FlhMciConstants.BITMASK_PARAM_NAME, preset.getBitmaskExpression());
            }
        }
    }

    /*
     * Updates a preset parameter due to a preset change
     */
    private void updatePresetParameter(String paramName, String value) {
        Parameter toUpdate;

        toUpdate = _paramGroup.getParameter(paramName);
        toUpdate.setValue(value, null);
    }

    /*
     * Creates the parameter group for the UI class
     */
    private void createParamGroup() {
        FlhMciRequestElementFactory factory = FlhMciRequestElementFactory.getInstance();

        _paramGroup = new ParamGroup();
        _paramGroup.addParameter(factory.createDefaultInputProductParameter());
        _paramGroup.addParameter(factory.createDefaultOutputProductParameter());
        _paramGroup.addParameter(factory.createParamWithDefaultValueSet(FlhMciConstants.PRESET_PARAM_NAME));
        _paramGroup.addParameter(factory.createParamWithDefaultValueSet(FlhMciConstants.BAND_LOW_PARAM_NAME));
        _paramGroup.addParameter(factory.createParamWithDefaultValueSet(FlhMciConstants.BAND_SIGNAL_PARAM_NAME));
        _paramGroup.addParameter(factory.createParamWithDefaultValueSet(FlhMciConstants.BAND_HIGH_PARAM_NAME));
        _paramGroup.addParameter(
                factory.createParamWithDefaultValueSet(FlhMciConstants.LINEHEIGHT_BAND_NAME_PARAM_NAME));
        _paramGroup.addParameter(factory.createParamWithDefaultValueSet(FlhMciConstants.PROCESS_SLOPE_PARAM_NAME));
        _paramGroup.addParameter(factory.createParamWithDefaultValueSet(FlhMciConstants.BITMASK_PARAM_NAME));
        _paramGroup.addParameter(factory.createParamWithDefaultValueSet(FlhMciConstants.SLOPE_BAND_NAME_PARAM_NAME));
        _paramGroup.addParameter(
                factory.createParamWithDefaultValueSet(FlhMciConstants.INVALID_PIXEL_VALUE_PARAM_NAME));
        _paramGroup.addParameter(
                factory.createParamWithDefaultValueSet(FlhMciConstants.CLOUD_CORRECTION_FACTOR_PARAM_NAME));
        _paramGroup.addParameter(factory.createDefaultLogPatternParameter(FlhMciConstants.DEFAULT_LOG_PREFIX));
        try {
            _paramGroup.addParameter(factory.createLogToOutputParameter("false"));
        } catch (ParamValidateException e) {
            _logger.warning("Unable to validate parameter '" + ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
            Debug.trace(e);
        }
        _paramGroup.addParamChangeListener(this);
    }

    /*
     * Sets the input file parameter to the value stored in the request. Tries to open the input file and retrieve a
     * list of bands.
     */
    private void setInputFile(Request request) {
        if (request.getNumInputProducts() > 0) {
            ProductRef prodRef = request.getInputProductAt(0);
            Parameter param = _paramGroup.getParameter(DefaultRequestElementFactory.INPUT_PRODUCT_PARAM_NAME);
            File file = new File(prodRef.getFilePath());
            param.setValue(file, null);

            Product inProduct = null;
            try {
                if (file.canRead()) {
                    inProduct = ProductIO.readProduct(prodRef.getFile());
                    scanForBandNames(inProduct);
                    scanForFlags(inProduct);
                }
            } catch (IOException e) {
                _logger.warning(e.getMessage());
                Debug.trace(e);
            } finally {
                if (inProduct != null) {
                    inProduct.dispose();
                }
            }

        }
    }

    /*
     * Copies the value of the input file parameter to the request passed in
     */
    private void getInputFile(Request request) {
        File inputURL = (File) _paramGroup.getParameter(
                DefaultRequestElementFactory.INPUT_PRODUCT_PARAM_NAME).getValue();
        request.addInputProduct(new ProductRef(inputURL, null, null));
    }

    /*
     * Sets the output file parameter to the value stored in the request. Updates the file format combo box with the
     * correct value
     */
    private void setOutputFile(Request request) {
        if (request.getNumOutputProducts() > 0) {
            ProductRef outputProduct = request.getOutputProductAt(0);
            Parameter param = _paramGroup.getParameter(FlhMciConstants.OUTPUT_PRODUCT_PARAM_NAME);
            File file = new File(outputProduct.getFilePath());
            param.setValue(file, null);

            _outFileFormat = outputProduct.getFileFormat();
            if (_outFileFormat != null) {
                _fileFormatCombo.setSelectedItem(_outFileFormat);
            } else {
                // set default format - beam-dimap
                _outFileFormat = DimapProductConstants.DIMAP_FORMAT_NAME;
            }
        }
    }

    /*
     * Copies the value of the output file parameter to the request passed in
     */
    private void getOutputFile(Request request) {
        String fileName = _paramGroup.getParameter(FlhMciConstants.OUTPUT_PRODUCT_PARAM_NAME).getValueAsText();

        request.addOutputProduct(ProcessorUtils.createProductRef(fileName, _outFileFormat));
    }

    /*
     * Copies the values of the standard parameters to the request passed in
     */
    private void getParameter(Request request) {
        request.addParameter(_paramGroup.getParameter(FlhMciConstants.PRESET_PARAM_NAME));
        request.addParameter(_paramGroup.getParameter(FlhMciConstants.BAND_LOW_PARAM_NAME));
        request.addParameter(_paramGroup.getParameter(FlhMciConstants.BAND_SIGNAL_PARAM_NAME));
        request.addParameter(_paramGroup.getParameter(FlhMciConstants.BAND_HIGH_PARAM_NAME));
        request.addParameter(_paramGroup.getParameter(FlhMciConstants.LINEHEIGHT_BAND_NAME_PARAM_NAME));
        request.addParameter(_paramGroup.getParameter(FlhMciConstants.PROCESS_SLOPE_PARAM_NAME));
        request.addParameter(_paramGroup.getParameter(FlhMciConstants.SLOPE_BAND_NAME_PARAM_NAME));
        request.addParameter(_paramGroup.getParameter(FlhMciConstants.BITMASK_PARAM_NAME));
        request.addParameter(_paramGroup.getParameter(FlhMciConstants.INVALID_PIXEL_VALUE_PARAM_NAME));
        request.addParameter(_paramGroup.getParameter(FlhMciConstants.CLOUD_CORRECTION_FACTOR_PARAM_NAME));
        request.addParameter(_paramGroup.getParameter(FlhMciConstants.LOG_PREFIX_PARAM_NAME));
        request.addParameter(_paramGroup.getParameter(FlhMciConstants.LOG_TO_OUTPUT_PARAM_NAME));
    }

    /*
     * Updates all UI parameter components with the values stored in the request
     */
    private void setParameter(Request request) {
        setSimpleParameter(request.getParameter(FlhMciConstants.PRESET_PARAM_NAME));
        setSimpleParameter(request.getParameter(FlhMciConstants.BAND_LOW_PARAM_NAME));
        setSimpleParameter(request.getParameter(FlhMciConstants.BAND_SIGNAL_PARAM_NAME));
        setSimpleParameter(request.getParameter(FlhMciConstants.BAND_HIGH_PARAM_NAME));
        setSimpleParameter(request.getParameter(FlhMciConstants.LINEHEIGHT_BAND_NAME_PARAM_NAME));
        setSimpleParameter(request.getParameter(FlhMciConstants.BITMASK_PARAM_NAME));
        setSimpleParameter(request.getParameter(FlhMciConstants.INVALID_PIXEL_VALUE_PARAM_NAME));
        setSimpleParameter(request.getParameter(FlhMciConstants.CLOUD_CORRECTION_FACTOR_PARAM_NAME));
        setSimpleParameter(request.getParameter(FlhMciConstants.LOG_PREFIX_PARAM_NAME));
        setSimpleParameter(request.getParameter(FlhMciConstants.LOG_TO_OUTPUT_PARAM_NAME));
        setProcessSlopeParameter(request);
    }

    /*
     * Updates a single UI parameter with the parameter value passed in. Triggers a screen refresh of that parameter
     * editor
     */
    private void setSimpleParameter(Parameter param) {
        if (param != null) {
            Parameter toUpdate = _paramGroup.getParameter(param.getName());
            if (toUpdate != null) {
                toUpdate.setValue(param.getValue(), null);
            }
        }
    }

    /*
     * Sets the parameter for process slope an slope band name and updates the UI components accordingly
     */
    private void setProcessSlopeParameter(Request request) {
        Parameter processSlope = request.getParameter(FlhMciConstants.PROCESS_SLOPE_PARAM_NAME);

        if (processSlope != null) {
            Boolean bProcess;
            Parameter slopeUpdate = _paramGroup.getParameter(FlhMciConstants.PROCESS_SLOPE_PARAM_NAME);
            bProcess = (Boolean) processSlope.getValue();
            slopeUpdate.setValue(bProcess, null);

            if (bProcess) {
                Parameter slopeBandName = request.getParameter(FlhMciConstants.SLOPE_BAND_NAME_PARAM_NAME);
                enableSlopeBand(slopeBandName.getValueAsText());
            } else {
                disableSlopeBand();
            }
        }
    }

    /*
     * Disables the UI for the parameter slope band name
     */
    private void disableSlopeBand() {
        Parameter slopeParam = _paramGroup.getParameter(FlhMciConstants.SLOPE_BAND_NAME_PARAM_NAME);
        if (slopeParam != null) {
            slopeParam.getEditor().setEnabled(false);
        }
    }

    /*
     * Enables the UI for the parameter slope band name and sets the control to the parameter value passed as argument
     */
    private void enableSlopeBand(String value) {
        Guardian.assertNotNull("value", value);
        Parameter slopeParam = _paramGroup.getParameter(FlhMciConstants.SLOPE_BAND_NAME_PARAM_NAME);
        if (slopeParam != null) {
            slopeParam.setValue(value, null);
            slopeParam.getEditor().setEnabled(true);
        }
    }

    /**
     * Adds a parameter with given name to the parameter panel at given line index
     *
     * @param name  the parameter name
     * @param line  the line at which the parameter is added
     * @param gbc   the <code>GridBagConstraints</code> to be used
     * @param panel whre to add the parameter
     */
    private void addParameterToParamPanel(String name, int line, GridBagConstraints gbc, JPanel panel) {
        Parameter param;

        param = _paramGroup.getParameter(name);
        if (param != null) {
            GridBagUtils.setAttributes(gbc, "gridy=" + String.valueOf(line));
            GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, weighty=1");
            GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
            GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);
        }
    }

    /*
     * Scans the product passed in for the bands it contains and sets the valueset for the appropriate parameter. Keeps
     * the value currently set, even if it might not be valid in the new valueset
     */
    private void scanForBandNames(Product inProduct) {
        if (inProduct != null) {
            String[] spectralBandNames = extractSpectralBands(inProduct);
            Parameter param;
            String currentBand;

            param = _paramGroup.getParameter(FlhMciConstants.BAND_LOW_PARAM_NAME);
            currentBand = param.getValueAsText();
            param.getProperties().setValueSet(spectralBandNames);
            param.setValue(currentBand, null);

            param = _paramGroup.getParameter(FlhMciConstants.BAND_SIGNAL_PARAM_NAME);
            currentBand = param.getValueAsText();
            param.getProperties().setValueSet(spectralBandNames);
            param.setValue(currentBand, null);

            param = _paramGroup.getParameter(FlhMciConstants.BAND_HIGH_PARAM_NAME);
            currentBand = param.getValueAsText();
            param.getProperties().setValueSet(spectralBandNames);
            param.setValue(currentBand, null);
        } else {
            _logger.warning("input product is null");
            Debug.trace("input product is null");
        }
    }

    private static String[] extractSpectralBands(Product product) {
        String[] allBandNames = product.getBandNames();
        final Vector<String> spectralBandVector = new Vector<String>(allBandNames.length);

        for (String allBandName : allBandNames) {
            Band band = product.getBand(allBandName);
            if (band.getSpectralWavelength() > 1.0e-3) {
                spectralBandVector.add(allBandName);
            }
        }

        String[] spectralBands = new String[spectralBandVector.size()];
        spectralBands = spectralBandVector.toArray(spectralBands);

        return spectralBands;
    }

    /*
     * Scans the product passed in for the flag names
     */
    private void scanForFlags(Product inProd) {
        if (inProd != null) {
            String[] bitmaskFlags = inProd.getAllFlagNames();
            Parameter bitmask = _paramGroup.getParameter(FlhMciConstants.BITMASK_PARAM_NAME);
            bitmask.getProperties().setValueSet(bitmaskFlags);
        }
    }

    /*
     * Enables and disables presets according to the input product type selected
     */
    private void setPresetBox(String productType) {
        if (productType.length() == 0) {
            // short cut when no product is loaded
            return;
        }

        String currentPreset = _paramGroup.getParameter(FlhMciConstants.PRESET_PARAM_NAME).getValueAsText();

        Pattern l1bTypePattern = Pattern.compile("MER_..._1P");
        if (l1bTypePattern.matcher(productType).matches()) {
            // meris l1b - check for preset. L2 presets are invalid in this context
            if ((currentPreset.equals(FlhMciConstants.PRESET_PARAM_VALUE_SET[0]))
                || (currentPreset.equals(FlhMciConstants.PRESET_PARAM_VALUE_SET[2]))) {
                showInfoDialog(_presetProductMismatchMessage);
            }
        } else if (productType.equals(BeamConstants.MERIS_FR_L2_PRODUCT_TYPE_NAME)
                   || productType.equals(BeamConstants.MERIS_RR_L2_PRODUCT_TYPE_NAME)) {
            // meris L2 - check for preset. L1b presets are invalid
            if (currentPreset.equals(FlhMciConstants.PRESET_PARAM_VALUE_SET[1])) {
                showInfoDialog(_presetProductMismatchMessage);
            }
        } else {
            // anything else - just "General baseline height" allowed as preset
            if (!currentPreset.equals(FlhMciConstants.PRESET_PARAM_VALUE_SET[3])) {
                showInfoDialog(_presetProductMismatchMessage);
            }
        }
    }

    /**
     * Shows an information dialog with the message passed in.
     *
     * @param message the message to be shown
     */
    private void showInfoDialog(String message) {
        JOptionPane.showMessageDialog(_processor.getParentFrame(),
                                      message,
                                      _processor.getUITitle(),
                                      JOptionPane.INFORMATION_MESSAGE);
    }
}
