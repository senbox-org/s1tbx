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
package org.esa.beam.processor.sst.ui;

import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.ParamProperties;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.param.editors.BooleanExpressionEditor;
import org.esa.beam.framework.processor.DefaultRequestElementFactory;
import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.ui.AbstractProcessorUI;
import org.esa.beam.framework.processor.ui.ProcessorApp;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.processor.sst.SstCoefficientLoader;
import org.esa.beam.processor.sst.SstConstants;
import org.esa.beam.processor.sst.SstRequestElementFactory;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;

import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashMap;
import java.util.Vector;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * This class implements the graphical user interface for the BEAM Sea Surface Temperature processor.
 *
 * @deprecated since BEAM 4.10 - no replacement.
 */
@Deprecated
public class SstUI extends AbstractProcessorUI implements ParamChangeListener {

    private JTabbedPane _tabbedPane;
    private JComboBox _fileFormatCombo;
    private String[] _formatNames;
    private String _outFileFormat;
    private static final String PATHS_TAB_NAME = "I/O Parameters";
    private static final String PARAMETER_TAB_NAME = "Processing Parameters";
    private static final String _outFormatLabel = "Output product format:";

    private ParamGroup _paramGroup;
    private File _requestFile;
    private Logger _logger;

    private SstCoefficientLoader _loader;
    private HashMap<String, File> _nadirCoeffsMap;
    private String[] _nadirDescriptions;
    private HashMap<String, File> _dualCoeffsMap;
    private String[] _dualDescriptions;
    private boolean _mustDisableNadir = false;
    private boolean _mustDisableDual = false;
    private static File auxdataDir;

    /**
     * Creates the UI class with default parameters
     */
    public SstUI() {
        _tabbedPane = null;
        _nadirCoeffsMap = new HashMap<String, File>();
        _dualCoeffsMap = new HashMap<String, File>();
        _outFileFormat = DimapProductConstants.DIMAP_FORMAT_NAME;
        _loader = new SstCoefficientLoader();
        _logger = Logger.getLogger(SstConstants.LOGGER_NAME);
    }

    /**
     * Retrieves the base component for the processor specific user interface classes. This can be any Java Swing
     * containertype. This method creates the UI from scratch if not present.
     */
    public JComponent getGuiComponent() {
        if (_tabbedPane == null) {
            try {
                installAuxdata();
                scanCoefficientFiles();
            } catch (Exception e) {
                String msg = "Failed to initiallize SST auxdata: " + e.getMessage();
                _logger.log(Level.SEVERE, msg, e);
                getApp().showErrorDialog("SST Processor", msg);
            }
            scanWriterFormatStrings();
            createParamGroup();
            createUI();
        }
        return _tabbedPane;
    }

    /**
     * Sets a new Request list to be edited.
     *
     * @param requests the request list to be edited
     */
    public void setRequests(final Vector requests) {
        Guardian.assertNotNull("requests", requests);
        if (requests.size() > 0) {
            final Request request = (Request) requests.elementAt(0);
            _requestFile = request.getFile();
            setInputProduct(request);
            setOutputProduct(request);
            setDualViewParameters(request);
            setNadirViewParameters(request);
            setRemainingParameter(request);
        } else {
            setDefaultRequests();
        }
    }

    /**
     * Retrieves the requests currently edited.
     */
    public Vector getRequests() throws ProcessorException {
        final Request request = new Request();
        if (hasParameterEmptyString(_paramGroup.getParameter(ProcessorConstants.OUTPUT_PRODUCT_PARAM_NAME))) {
            throw new ProcessorException("No output product specified.");
        }
        request.setType(SstConstants.REQUEST_TYPE);
        request.setFile(_requestFile);
        getInputProduct(request);
        getOutputProduct(request);
        getParameter(request);
        final Vector<Request> vRet = new Vector<Request>();
        vRet.add(request);
        return vRet;
    }

    /**
     * Create a new default request for the sst processor and sets it to the UI
     */
    public void setDefaultRequests() {
        final SstRequestElementFactory factory = SstRequestElementFactory.getInstance();
        final Request request = new Request();
        request.addInputProduct(createDefaultInputProduct());
        request.addOutputProduct(createDefaultOutputProduct());
        request.addLogFileLocation(createDefaultLogFileLocation());
        request.addParameter(factory.createParamWithDefaultValueSet(SstConstants.PROCESS_DUAL_VIEW_SST_PARAM_NAME));
        request.addParameter(factory.createParamWithDefaultValueSet(SstConstants.DUAL_VIEW_COEFF_FILE_PARAM_NAME));
        request.addParameter(factory.createParamWithDefaultValueSet(SstConstants.DUAL_VIEW_COEFF_DESC_PARAM_NAME));
        request.addParameter(factory.createParamWithDefaultValueSet(SstConstants.DUAL_VIEW_BITMASK_PARAM_NAME));
        request.addParameter(factory.createParamWithDefaultValueSet(SstConstants.PROCESS_NADIR_VIEW_SST_PARAM_NAME));
        request.addParameter(factory.createParamWithDefaultValueSet(SstConstants.NADIR_VIEW_COEFF_FILE_PARAM_NAME));
        request.addParameter(factory.createParamWithDefaultValueSet(SstConstants.NADIR_VIEW_COEFF_DESC_PARAM_NAME));
        request.addParameter(factory.createParamWithDefaultValueSet(SstConstants.NADIR_VIEW_BITMASK_PARAM_NAME));
        request.addParameter(factory.createParamWithDefaultValueSet(SstConstants.INVALID_PIXEL_PARAM_NAME));
        final Vector<Request> vSet = new Vector<Request>();
        vSet.add(request);
        setRequests(vSet);
        handleUpdateNadirCoefficientFile();
        handleUpdateDualCoefficientFile();
    }

    /**
     * Callback for parameter group
     *
     * @param evt the event that triggered the callback
     */
    public void parameterValueChanged(ParamChangeEvent evt) {
        if (evt.getParameter().getName().equals(SstConstants.PROCESS_DUAL_VIEW_SST_PARAM_NAME)) {
            handleUpdateDualViewParameter();
        } else if (evt.getParameter().getName().equals(SstConstants.PROCESS_NADIR_VIEW_SST_PARAM_NAME)) {
            handleUpdateNadirViewParameter();
        } else if (evt.getParameter().getName().equals(DefaultRequestElementFactory.INPUT_PRODUCT_PARAM_NAME)) {
            handleUpdateInputProduct();
        } else if (evt.getParameter().getName().equals(SstConstants.NADIR_VIEW_COEFF_DESC_PARAM_NAME)) {
            handleUpdateNadirCoefficientFile();
        } else if (evt.getParameter().getName().equals(SstConstants.DUAL_VIEW_COEFF_DESC_PARAM_NAME)) {
            handleUpdateDualCoefficientFile();
        } else if (evt.getParameter().getName().equals(DefaultRequestElementFactory.OUTPUT_PRODUCT_PARAM_NAME)) {
            handleUpdateOutputProduct();
        }
    }

    /**
     * Sets the processor app for the UI
     */
    @Override
    public void setApp(ProcessorApp app) {
        super.setApp(app);
        if (_paramGroup != null) {
            app.markIODirChanges(_paramGroup);
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    private void installAuxdata() throws ProcessorException {
        Processor processor = getApp().getProcessor();
        processor.installAuxdata();
        auxdataDir = processor.getAuxdataInstallDir();
    }


    /**
     * Creates all user interface components of the sst user interface
     */
    private void createUI() {
        _tabbedPane = new JTabbedPane();

        JPanel panel = createPathTab();
        _tabbedPane.add(PATHS_TAB_NAME, panel);

        panel = createParameterTab();
        _tabbedPane.add(PARAMETER_TAB_NAME, panel);

        HelpSys.enableHelp(_tabbedPane, "sstScientificTool");

        if (_mustDisableDual) {
            disableDualView();
        }
        if (_mustDisableNadir) {
            disableNadirView();
        }
    }

    /**
     * Creates the panel for the input and output products pathes
     */
    private JPanel createPathTab() {
        int line = 0;
        JPanel panel = GridBagUtils.createDefaultEmptyBorderPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints(null);
        Parameter param;

        // input product
        // -------------
        param = _paramGroup.getParameter(DefaultRequestElementFactory.INPUT_PRODUCT_PARAM_NAME);
        GridBagUtils.setAttributes(gbc,
                                   "anchor=SOUTHWEST, fill=HORIZONTAL, weightx=1, weighty=1, gridy=" + String.valueOf(
                                           line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, weighty=1, gridy=" + String.valueOf(line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        // output product
        // --------------
        param = _paramGroup.getParameter(SstConstants.OUTPUT_PRODUCT_PARAM_NAME);
        GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, weighty=0.5, gridy=" + String.valueOf(line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, weighty=0.5, gridy=" + String.valueOf(line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        JLabel outFormatLabel = new JLabel(_outFormatLabel);
        _fileFormatCombo = new JComboBox(_formatNames);
        _fileFormatCombo.addActionListener(new ActionListener() {

            public void actionPerformed(ActionEvent e) {
                updateOutFileType();
            }
        });
        GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, fill=NONE, weighty=0.5, gridy=" + String.valueOf(line++));
        GridBagUtils.addToPanel(panel, outFormatLabel, gbc);
        GridBagUtils.setAttributes(gbc,
                                   "anchor=NORTHWEST, weighty=0.5, insets.bottom=8, gridy=" + String.valueOf(line++));
        GridBagUtils.addToPanel(panel, _fileFormatCombo, gbc);

        // logging
        // -------
        param = _paramGroup.getParameter(SstConstants.LOG_PREFIX_PARAM_NAME);
        GridBagUtils.setAttributes(gbc,
                                   "anchor=SOUTHWEST, fill=HORIZONTAL, weighty=0.5, insets.bottom=0, gridy=" + String.valueOf(
                                           line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, weighty=0.5, gridy=" + String.valueOf(line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        param = _paramGroup.getParameter(SstConstants.LOG_TO_OUTPUT_PARAM_NAME);
        GridBagUtils.setAttributes(gbc,
                                   "anchor=SOUTHWEST, fill=HORIZONTAL, weighty=0.5, insets.bottom=0, gridy=" + String.valueOf(
                                           line++));
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        return panel;
    }

    /**
     * Creates the panel for the parameters
     */
    private JPanel createParameterTab() {
        int line = 0;
        JPanel panel = GridBagUtils.createDefaultEmptyBorderPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints(null);

        // Dual view parameters
        GridBagUtils.setAttributes(gbc, "insets.bottom=4");
        addParameterToParamPanel(SstConstants.PROCESS_DUAL_VIEW_SST_PARAM_NAME, line++, gbc, panel);
        // set the coefficient descriptions found
        ParamProperties paramProps = _paramGroup.getParameter(
                SstConstants.DUAL_VIEW_COEFF_DESC_PARAM_NAME).getProperties();
        paramProps.setValueSet(_dualDescriptions);
        if (_dualDescriptions.length > 0) {
            paramProps.setDefaultValue(_dualDescriptions[0]);
        }
        GridBagUtils.setAttributes(gbc, "fill=HORIZONTAL");
        addParameterToParamPanel(SstConstants.DUAL_VIEW_COEFF_DESC_PARAM_NAME, line++, gbc, panel);
        paramProps.setValueSetBound(true);
        GridBagUtils.setAttributes(gbc, "fill=NONE, insets.bottom=8");
        addParameterToParamPanel(SstConstants.DUAL_VIEW_BITMASK_PARAM_NAME, line++, gbc, panel);
        GridBagUtils.setAttributes(gbc, "insets.bottom=4");

        // nadir view parameters
        addParameterToParamPanel(SstConstants.PROCESS_NADIR_VIEW_SST_PARAM_NAME, line++, gbc, panel);
        // set the coefficient descriptions found
        paramProps = _paramGroup.getParameter(SstConstants.NADIR_VIEW_COEFF_DESC_PARAM_NAME).getProperties();
        paramProps.setValueSet(_nadirDescriptions);
        if (_nadirDescriptions.length > 0) {
            paramProps.setDefaultValue(_nadirDescriptions[0]);
        }
        GridBagUtils.setAttributes(gbc, "fill=HORIZONTAL");
        addParameterToParamPanel(SstConstants.NADIR_VIEW_COEFF_DESC_PARAM_NAME, line++, gbc, panel);
        paramProps.setValueSetBound(true);
        GridBagUtils.setAttributes(gbc, "fill=NONE, insets.bottom=8");
        addParameterToParamPanel(SstConstants.NADIR_VIEW_BITMASK_PARAM_NAME, line++, gbc, panel);
        GridBagUtils.setAttributes(gbc, "insets.bottom=0");
        GridBagUtils.setAttributes(gbc, "insets.top=8");
        addParameterToParamPanel(SstConstants.INVALID_PIXEL_PARAM_NAME, line++, gbc, panel);
        return panel;
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
        Parameter param = _paramGroup.getParameter(name);
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, weighty=1, gridy=" + String.valueOf(line));
        if (param.getEditor().getLabelComponent() != null) {
            GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
            GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);
        } else {
            gbc.gridwidth--;
            GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);
            gbc.gridwidth++;
        }
    }

    /**
     * Creates and initializes the parameter group containing all editable parameter of this processor.
     */
    private void createParamGroup() {
        SstRequestElementFactory factory = SstRequestElementFactory.getInstance();
        _paramGroup = new ParamGroup();
        Parameter param;
        _paramGroup.addParameter(factory.createDefaultInputProductParameter());
        _paramGroup.addParameter(factory.createDefaultOutputProductParameter());
        _paramGroup.addParameter(factory.createDefaultLogfileParameter());
        _paramGroup.addParameter(factory.createDefaultLogPatternParameter(SstConstants.DEFAULT_LOG_PREFIX));
        try {
            _paramGroup.addParameter(factory.createLogToOutputParameter("false"));
        } catch (ParamValidateException e) {
            _logger.warning("Unable to validate parameter '" + ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
            Debug.trace(e);
        }
        _paramGroup.addParameter(factory.createParamWithDefaultValueSet(SstConstants.PROCESS_DUAL_VIEW_SST_PARAM_NAME));
        _paramGroup.addParameter(factory.createParamWithDefaultValueSet(SstConstants.DUAL_VIEW_COEFF_FILE_PARAM_NAME));
        param = factory.createParamWithDefaultValueSet(SstConstants.DUAL_VIEW_COEFF_DESC_PARAM_NAME);
        try {
            param.getProperties().setValueSet(_dualDescriptions);
            if (_dualDescriptions.length > 0) {
                param.setValue(_dualDescriptions[0]);
            }
        } catch (ParamValidateException e) {
            _logger.warning("Unable to validate parameter '" + SstConstants.DUAL_VIEW_COEFF_DESC_PARAM_NAME + "'");
            Debug.trace(e);
        }
        _paramGroup.addParameter(param);
        _paramGroup.addParameter(factory.createParamWithDefaultValueSet(SstConstants.DUAL_VIEW_BITMASK_PARAM_NAME));
        _paramGroup.addParameter(
                factory.createParamWithDefaultValueSet(SstConstants.PROCESS_NADIR_VIEW_SST_PARAM_NAME));
        _paramGroup.addParameter(factory.createParamWithDefaultValueSet(SstConstants.NADIR_VIEW_COEFF_FILE_PARAM_NAME));
        param = factory.createParamWithDefaultValueSet(SstConstants.NADIR_VIEW_COEFF_DESC_PARAM_NAME);
        try {
            param.getProperties().setValueSet(_nadirDescriptions);
            if (_nadirDescriptions.length > 0) {
                param.setValue(_nadirDescriptions[0]);
            }
        } catch (ParamValidateException e) {
            _logger.warning("Unable to validate parameter '" + SstConstants.NADIR_VIEW_COEFF_DESC_PARAM_NAME + "'");
            Debug.trace(e);
        }
        _paramGroup.addParameter(param);
        _paramGroup.addParameter(factory.createParamWithDefaultValueSet(SstConstants.NADIR_VIEW_BITMASK_PARAM_NAME));
        _paramGroup.addParameter(factory.createParamWithDefaultValueSet(SstConstants.INVALID_PIXEL_PARAM_NAME));
        _paramGroup.addParamChangeListener(this);
        handleUpdateNadirCoefficientFile();
        handleUpdateDualCoefficientFile();
    }

    /**
     * Updates the input product parameter due to an update in the request.
     */
    private void setInputProduct(Request request) {
        if (request.getNumInputProducts() > 0) {
            ProductRef prodRef = request.getInputProductAt(0);
            Parameter param = _paramGroup.getParameter(DefaultRequestElementFactory.INPUT_PRODUCT_PARAM_NAME);
            File file = new File(prodRef.getFilePath());
            param.setValue(file, null);

            // check if file exists and is file
            if (file.exists() && file.isFile()) {
                Product inProduct = null;
                try {
                    inProduct = ProductIO.readProduct(prodRef.getFile());
                    if (inProduct != null) {
                        scanForFlags(inProduct);
                    }
                } catch (IOException e) {
                    _logger.severe("Unable to read the input product '" + prodRef.getFilePath() + "'");
                    Debug.trace(e);
                } finally {
                    if (inProduct != null) {
                        inProduct.dispose();
                    }
                }
            }
        }
    }

    /**
     * Sets the output file parameter to the value stored in the request. Updates the file format combo box with the
     * correct value.
     */
    private void setOutputProduct(Request request) {
        if (request.getNumOutputProducts() > 0) {
            ProductRef outputProduct = request.getOutputProductAt(0);
            Parameter param = _paramGroup.getParameter(SstConstants.OUTPUT_PRODUCT_PARAM_NAME);
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

    /**
     * Sets the parameters concerning the dual view processing due to an update in the request.
     */
    private void setDualViewParameters(Request request) {
        Parameter param = request.getParameter(SstConstants.PROCESS_DUAL_VIEW_SST_PARAM_NAME);
        Parameter toUpdate = _paramGroup.getParameter(SstConstants.PROCESS_DUAL_VIEW_SST_PARAM_NAME);

        if (param != null) {
            if ((Boolean) param.getValue()) {
                toUpdate.setValue(Boolean.TRUE, null);
                enableDualViewParameters(true, request);
            } else {
                toUpdate.setValue(Boolean.FALSE, null);
                enableDualViewParameters(false, request);
            }
        }
    }

    /**
     * Sets the parameters concerning the dual view processing due to an update in user interface.
     */
    private void handleUpdateDualViewParameter() {
        Parameter param = _paramGroup.getParameter(SstConstants.PROCESS_DUAL_VIEW_SST_PARAM_NAME);
        Parameter nadirParam = _paramGroup.getParameter(SstConstants.PROCESS_NADIR_VIEW_SST_PARAM_NAME);

        if (!(Boolean) param.getValue()) {
            enableDualViewParameters(false, null);
            // prevent that both get switched off at the same time
            if (!(Boolean) nadirParam.getValue()) {
                enableNadirViewParameters(true, null);
            }
        } else {
            enableDualViewParameters(true, null);
        }
    }

    /**
     * Enables the gui components for the dual view processing
     */
    private void enableDualViewParameters(boolean enabled, Request request) {
        Parameter toUpdate;
        Parameter param;

        toUpdate = _paramGroup.getParameter(SstConstants.PROCESS_DUAL_VIEW_SST_PARAM_NAME);
        try {
            toUpdate.setValue(enabled);
        } catch (ParamValidateException e) {
            _logger.severe("Unable to validate parameter '" + SstConstants.PROCESS_DUAL_VIEW_SST_PARAM_NAME + "'");
            Debug.trace(e);
        }

        toUpdate = _paramGroup.getParameter(SstConstants.DUAL_VIEW_COEFF_DESC_PARAM_NAME);
        toUpdate.getEditor().setEnabled(enabled);

        if (request != null) {
            param = request.getParameter(SstConstants.DUAL_VIEW_COEFF_FILE_PARAM_NAME);
            if (param != null) {
                Parameter fileParam = _paramGroup.getParameter(SstConstants.DUAL_VIEW_COEFF_FILE_PARAM_NAME);

                File file = (File) param.getValue();

                fileParam.setValue(file, null);

                String description;
                try {
                    description = _loader.getDescription(file.toURI().toURL());
                    if ((description != null) && (description.length() > 0)) {
                        toUpdate.setValue(description);
                    }
                } catch (ParamValidateException e) {
                    _logger.severe(
                            "Unable to validate parameter '" + SstConstants.DUAL_VIEW_COEFF_FILE_PARAM_NAME + "'");
                    Debug.trace(e);
                } catch (IOException e) {
                    _logger.severe("Unable to read coefficient file '" + file.getAbsolutePath() + "'");
                    Debug.trace(e);
                }
            }
        }

        toUpdate = _paramGroup.getParameter(SstConstants.DUAL_VIEW_BITMASK_PARAM_NAME);
        if (request != null) {
            param = request.getParameter(SstConstants.DUAL_VIEW_BITMASK_PARAM_NAME);
            if (param != null) {
                toUpdate.setValue(param.getValue(), null);
            }
        }
        toUpdate.getEditor().setEnabled(enabled);
    }

    /**
     * Sets the parameters concerning the nadir view processing due to an update in the request.
     */
    private void setNadirViewParameters(Request request) {
        Parameter param = request.getParameter(SstConstants.PROCESS_NADIR_VIEW_SST_PARAM_NAME);
        Parameter toUpdate = _paramGroup.getParameter(SstConstants.PROCESS_NADIR_VIEW_SST_PARAM_NAME);

        if (param != null) {
            if ((Boolean) param.getValue()) {
                toUpdate.setValue(Boolean.TRUE, null);
                enableNadirViewParameters(true, request);
            } else {
                toUpdate.setValue(Boolean.FALSE, null);
                enableNadirViewParameters(false, request);
            }
        }
    }

    /**
     * Sets the parameters concerning the nadir view processing due to an update in user interface.
     */
    private void handleUpdateNadirViewParameter() {
        Parameter param = _paramGroup.getParameter(SstConstants.PROCESS_NADIR_VIEW_SST_PARAM_NAME);
        Parameter dualParam = _paramGroup.getParameter(SstConstants.PROCESS_DUAL_VIEW_SST_PARAM_NAME);


        if ((Boolean) param.getValue()) {
            enableNadirViewParameters(true, null);
        } else {
            enableNadirViewParameters(false, null);
            // prevent that both get switched off at the same time
            if ((Boolean) dualParam.getValue()) {
                return;
            }
            enableDualViewParameters(true, null);
        }
    }

    /**
     * Enables/disables the gui components for the nadir view processing. If a request is passed in (optional) the
     * values are changed according to the parameters set in the request
     *
     * @param enabled boolean whether the components shall be enabled or not#
     * @param request optionsl request. When set, the parameter values are updated before enabling/disabling them
     */
    private void enableNadirViewParameters(boolean enabled, Request request) {
        Parameter toUpdate;
        Parameter param;

        toUpdate = _paramGroup.getParameter(SstConstants.PROCESS_NADIR_VIEW_SST_PARAM_NAME);
        try {
            toUpdate.setValue(enabled);
        } catch (ParamValidateException e) {
            _logger.severe("Unable to validate parameter '" + SstConstants.PROCESS_NADIR_VIEW_SST_PARAM_NAME + "'");
            Debug.trace(e);
        }

        toUpdate = _paramGroup.getParameter(SstConstants.NADIR_VIEW_COEFF_DESC_PARAM_NAME);
        toUpdate.getEditor().setEnabled(enabled);

        if (request != null) {
            param = request.getParameter(SstConstants.NADIR_VIEW_COEFF_FILE_PARAM_NAME);
            if (param != null) {
                Parameter fileParam = _paramGroup.getParameter(SstConstants.NADIR_VIEW_COEFF_FILE_PARAM_NAME);

                File file = (File) param.getValue();

                fileParam.setValue(file, null);

                String description;
                try {
                    description = _loader.getDescription(file.toURI().toURL());
                    if ((description != null) && (description.length() > 0)) {
                        toUpdate.setValue(description);
                    }
                } catch (ParamValidateException e) {
                    _logger.severe(
                            "Unable to validate parameter '" + SstConstants.NADIR_VIEW_COEFF_FILE_PARAM_NAME + "'");
                    Debug.trace(e);
                } catch (IOException e) {
                    _logger.severe("Unable to read coefficient file '" + file.getAbsolutePath() + "'");
                    Debug.trace(e);
                }
            }
        }


        toUpdate = _paramGroup.getParameter(SstConstants.NADIR_VIEW_BITMASK_PARAM_NAME);
        if (request != null) {
            param = request.getParameter(SstConstants.NADIR_VIEW_BITMASK_PARAM_NAME);
            if (param != null) {
                toUpdate.setValue(param.getValue(), null);
            }
        }
        toUpdate.getEditor().setEnabled(enabled);
    }

    /**
     * Copies all parameter to the request passed in
     *
     * @param request the <code>Request</code> to be equipped with data
     */
    private void getParameter(Request request) {
        Parameter param;

        param = _paramGroup.getParameter(SstConstants.PROCESS_DUAL_VIEW_SST_PARAM_NAME);
        request.addParameter(param);
        if ((Boolean) param.getValue()) {
            // only need to add the parameter when dual view shall be processed
            request.addParameter(_paramGroup.getParameter(SstConstants.DUAL_VIEW_COEFF_FILE_PARAM_NAME));
            request.addParameter(_paramGroup.getParameter(SstConstants.DUAL_VIEW_BITMASK_PARAM_NAME));
        }

        param = _paramGroup.getParameter(SstConstants.PROCESS_NADIR_VIEW_SST_PARAM_NAME);
        request.addParameter(param);
        if ((Boolean) param.getValue()) {
            // only need to add the parameter when nadir view shall be processed
            request.addParameter(_paramGroup.getParameter(SstConstants.NADIR_VIEW_COEFF_FILE_PARAM_NAME));
            request.addParameter(_paramGroup.getParameter(SstConstants.NADIR_VIEW_BITMASK_PARAM_NAME));
        }

        param = _paramGroup.getParameter(SstConstants.INVALID_PIXEL_PARAM_NAME);
        request.addParameter(param);

        param = _paramGroup.getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME);
        request.addParameter(param);
        param = _paramGroup.getParameter(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME);
        request.addParameter(param);
    }

    /**
     * Sets the remaining parameter due to a new request set.
     *
     * @param request the <code>Request</code> containing the new value
     */
    private void setRemainingParameter(Request request) {
        Parameter param = request.getParameter(SstConstants.INVALID_PIXEL_PARAM_NAME);

        if (param != null) {
            Parameter toUpdate = _paramGroup.getParameter(SstConstants.INVALID_PIXEL_PARAM_NAME);
            toUpdate.setValue(param.getValue(), null);
        }

        param = request.getParameter(SstConstants.LOG_PREFIX_PARAM_NAME);
        if (param != null) {
            Parameter toUpdate = _paramGroup.getParameter(SstConstants.LOG_PREFIX_PARAM_NAME);
            toUpdate.setValue(param.getValue(), null);
        }

        param = request.getParameter(SstConstants.LOG_TO_OUTPUT_PARAM_NAME);
        if (param != null) {
            Parameter toUpdate = _paramGroup.getParameter(SstConstants.LOG_TO_OUTPUT_PARAM_NAME);
            toUpdate.setValue(param.getValue(), null);
        }
    }

    /**
     * Updates all components according to a change in the input product parameter
     */
    private void handleUpdateInputProduct() {
        try {
            File file = (File) _paramGroup.getParameter(
                    DefaultRequestElementFactory.INPUT_PRODUCT_PARAM_NAME).getValue();
            Product inProduct = ProductIO.readProduct(file);
            if (inProduct != null) {
                scanForFlags(inProduct);
                final Parameter dualViewParam = _paramGroup.getParameter(SstConstants.DUAL_VIEW_BITMASK_PARAM_NAME);
                dualViewParam.getProperties().setPropertyValue(BooleanExpressionEditor.PROPERTY_KEY_SELECTED_PRODUCT,
                                                               inProduct);
                final Parameter nadirViewParam = _paramGroup.getParameter(SstConstants.NADIR_VIEW_BITMASK_PARAM_NAME);
                nadirViewParam.getProperties().setPropertyValue(BooleanExpressionEditor.PROPERTY_KEY_SELECTED_PRODUCT,
                                                                inProduct);
            }
        } catch (IOException e) {
            _logger.severe("Unable to open input product!");
            Debug.trace(e);
        }
    }

    /**
     * Updates all components according to a change in the output product parameter
     */
    private void handleUpdateOutputProduct() {
        final Parameter outputParam = _paramGroup.getParameter(DefaultRequestElementFactory.OUTPUT_PRODUCT_PARAM_NAME);
        if (hasParameterEmptyString(outputParam)) {
            getApp().showWarningDialog("No output product specified.");
        }

    }


    /**
     * Updates the nadir view coefficient file
     */
    private void handleUpdateNadirCoefficientFile() {
        Parameter param = _paramGroup.getParameter(SstConstants.NADIR_VIEW_COEFF_DESC_PARAM_NAME);
        File nadirFile = _nadirCoeffsMap.get(param.getValueAsText());
        if (nadirFile != null) {
            _paramGroup.getParameter(SstConstants.NADIR_VIEW_COEFF_FILE_PARAM_NAME).setValue(nadirFile, null);
        }
    }

    /**
     * Updates the dual view coefficient file
     */
    private void handleUpdateDualCoefficientFile() {
        Parameter param = _paramGroup.getParameter(SstConstants.DUAL_VIEW_COEFF_DESC_PARAM_NAME);
        File dualFile = _dualCoeffsMap.get(param.getValueAsText());
        if (dualFile != null) {
            _paramGroup.getParameter(SstConstants.DUAL_VIEW_COEFF_FILE_PARAM_NAME).setValue(dualFile, null);
        }
    }

    /**
     * Scans the product passed in for the flag names.
     *
     * @param inProd the product to be scanned
     */
    private void scanForFlags(Product inProd) {
        String[] bitmaskFlags = inProd.getAllFlagNames();

        Parameter bitmask = _paramGroup.getParameter(SstConstants.DUAL_VIEW_BITMASK_PARAM_NAME);
        bitmask.getProperties().setValueSet(bitmaskFlags);
        bitmask = _paramGroup.getParameter(SstConstants.NADIR_VIEW_BITMASK_PARAM_NAME);
        bitmask.getProperties().setValueSet(bitmaskFlags);
    }

    /**
     * Copies the value of the input file parameter to the request passed in
     */
    private void getInputProduct(Request request) {
        String prodPath = _paramGroup.getParameter(
                DefaultRequestElementFactory.INPUT_PRODUCT_PARAM_NAME).getValueAsText();
        request.addInputProduct(new ProductRef(new File(prodPath), null, null));
    }

    /**
     * Copies the value of the output file parameter to the request passed in
     */
    private void getOutputProduct(final Request request) {
        final String fileName = _paramGroup.getParameter(SstConstants.OUTPUT_PRODUCT_PARAM_NAME).getValueAsText();

        request.addOutputProduct(ProcessorUtils.createProductRef(fileName, _outFileFormat));
    }


    /**
     * Scans the ProductIO for all product format strings of the registered writer plugins.
     */
    private void scanWriterFormatStrings() {
        ProductIOPlugInManager manager = ProductIOPlugInManager.getInstance();
        _formatNames = manager.getAllProductWriterFormatStrings();
    }

    /**
     * Scans the directories "BEAM_HOME/auxdata/sst/nadir_view" and "BEAM_HOME/auxdata/sst/nadir_view" for available
     * coefficient files. This method does NOT check for coefficient file integrity.
     */
    private void scanCoefficientFiles() throws IOException {
        _nadirDescriptions = new String[0];
        _mustDisableNadir = true;

        _dualDescriptions = new String[0];
        _mustDisableDual = true;

        scanNadirCoefficientFiles();

        scanDualCoefficientFiles();
    }

    /**
     * Scans for all coefficient files in the nadiR coefficients directory and loads them into the list box
     */
    private void scanNadirCoefficientFiles() throws IOException {
        File nadirDir = new File(auxdataDir, SstConstants.AUXPATH_NADIR_VIEW);
        if (!nadirDir.exists()) {
            throw new FileNotFoundException("file not found: " + nadirDir);
        }
        String[] nadirFileStrings;
        String description;
        File coeffFile;
        Vector<String> descVector = new Vector<String>();

        nadirFileStrings = nadirDir.list();
        if (nadirFileStrings == null) {
            return;
        }

        // loop over list to check for existing files
        for (String nadirFileString : nadirFileStrings) {
            coeffFile = new File(nadirDir, nadirFileString);
            if (coeffFile.exists() && coeffFile.isFile()) {
                try {
                    description = _loader.getDescription(coeffFile.toURI().toURL());
                    _nadirCoeffsMap.put(description, coeffFile);
                    descVector.add(description);
                } catch (IOException e) {
                    _logger.warning("Unable to read coefficient file '" + coeffFile + "'");
                    Debug.trace(e);
                }
            }
        }

        // check if at least one coefficient file was found
        if (descVector.size() > 0) {
            _mustDisableNadir = false;
            _nadirDescriptions = new String[descVector.size()];
            for (int i = 0; i < descVector.size(); i++) {
                _nadirDescriptions[i] = descVector.elementAt(i);
            }
        } else {
            throw new IOException("No coefficient files for nadir view SST found");
        }
    }

    /**
     * Scans for all coefficient files in the DUAL coefficients directory and loads them into the list box
     */
    private void scanDualCoefficientFiles() throws IOException {
        File dualDir = new File(auxdataDir, SstConstants.AUXPATH_DUAL_VIEW);
        if (!dualDir.exists()) {
            throw new FileNotFoundException("file not found: " + dualDir);
        }
        String[] dualFileStrings;
        String description;
        File coeffFile;
        Vector<String> descVector = new Vector<String>();

        dualFileStrings = dualDir.list();
        if (dualFileStrings == null) {
            return;
        }

        // loop over list to check for existing files
        for (String dualFileString : dualFileStrings) {
            coeffFile = new File(dualDir, dualFileString);
            if (coeffFile.exists() && coeffFile.isFile()) {
                try {
                    description = _loader.getDescription(coeffFile.toURI().toURL());
                    _dualCoeffsMap.put(description, coeffFile);
                    descVector.add(description);
                } catch (IOException e) {
                    _logger.warning("Unable to read coefficient file '" + coeffFile + "'");
                    Debug.trace(e);
                }
            }
        }

        // check if at least one coefficient file was found
        if (descVector.size() > 0) {
            _mustDisableDual = false;
            _dualDescriptions = new String[descVector.size()];
            for (int i = 0; i < descVector.size(); i++) {
                _dualDescriptions[i] = descVector.elementAt(i);
            }
        } else {
            throw new IOException("No coefficient files for dual view SST found");
        }
    }

    /**
     * Callback for output file format combo box
     */
    private void updateOutFileType() {
        _outFileFormat = (String) _fileFormatCombo.getSelectedItem();
    }

    /**
     * Creates a <code>ProductRef</code> pointing to the SST default input product
     */
    private static ProductRef createDefaultInputProduct() {
        Parameter inProdParam = SstRequestElementFactory.getInstance().createDefaultInputProductParameter();

        return new ProductRef(new File(inProdParam.getValueAsText()));
    }

    /**
     * Creates a <code>ProductRef</code> pointing to the SST default output product.
     */
    private static ProductRef createDefaultOutputProduct() {
        Parameter outProdParam = SstRequestElementFactory.getInstance().createDefaultOutputProductParameter();
        File outProd = (File) outProdParam.getValue();

        ProductRef ref = new ProductRef(outProd);
        ref.setFileFormat(DimapProductConstants.DIMAP_FORMAT_NAME);

        return ref;
    }

    private static boolean hasParameterEmptyString(final Parameter parameter) {
        final String valueAsText = parameter.getValueAsText();

        return valueAsText.trim().length() <= 0;
    }

    /**
     * Creates an URL pointing to the default logging file location for the SST processor.
     */
    private static File createDefaultLogFileLocation() {
        Parameter logFileParam = SstRequestElementFactory.getInstance().createDefaultLogfileParameter();
        return (File) logFileParam.getValue();

    }

    /**
     * Disables all nadir view controls
     */
    private void disableNadirView() {
        Parameter param;

        param = _paramGroup.getParameter(SstConstants.PROCESS_NADIR_VIEW_SST_PARAM_NAME);
        param.getEditor().setEnabled(false);
        param = _paramGroup.getParameter(SstConstants.NADIR_VIEW_BITMASK_PARAM_NAME);
        param.getEditor().setEnabled(false);
        param = _paramGroup.getParameter(SstConstants.NADIR_VIEW_COEFF_DESC_PARAM_NAME);
        param.getEditor().setEnabled(false);
    }

    /**
     * Disables all dual view controls
     */
    private void disableDualView() {
        Parameter param;

        param = _paramGroup.getParameter(SstConstants.PROCESS_DUAL_VIEW_SST_PARAM_NAME);
        param.getEditor().setEnabled(false);
        param = _paramGroup.getParameter(SstConstants.DUAL_VIEW_BITMASK_PARAM_NAME);
        param.getEditor().setEnabled(false);
        param = _paramGroup.getParameter(SstConstants.DUAL_VIEW_COEFF_DESC_PARAM_NAME);
        param.getEditor().setEnabled(false);
    }
}
