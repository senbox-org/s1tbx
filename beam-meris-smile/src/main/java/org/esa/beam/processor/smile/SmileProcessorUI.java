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
package org.esa.beam.processor.smile;

import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.DefaultRequestElementFactory;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.framework.processor.ui.AbstractProcessorUI;
import org.esa.beam.framework.processor.ui.ProcessorApp;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.GridBagConstraints;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * The interface for the Smile Correction Processor.
 *
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public class SmileProcessorUI extends AbstractProcessorUI {

    private JTabbedPane _tabbedPane;
    private ParamGroup _paramGroup;
    private File _requestFile;
    private SmileRequestElementFactory _factory;
    private Logger _logger;

    /**
     * Creates the UI class with default parameters
     */
    public SmileProcessorUI() {
        _factory = SmileRequestElementFactory.getInstance();
        _logger = Logger.getLogger(SmileConstants.LOGGER_NAME);
    }

    /**
     * Retrieves the base component for the processor specific user interface classes. This can be any Java Swing
     * containertype. This method creates the UI from scratch if not present.
     */
    @Override
    public JComponent getGuiComponent() {
        if (_tabbedPane == null) {
            createUI();
        }
        return _tabbedPane;
    }

    /**
     * Retrieves the requests currently edited.
     */
    @Override
    public Vector getRequests() throws ProcessorException {
        final Vector<Request> requests = new Vector<Request>();
        final Parameter outputParam = _paramGroup.getParameter(DefaultRequestElementFactory.OUTPUT_PRODUCT_PARAM_NAME);
        if (hasParameterEmptyString(outputParam)) {
            throw new ProcessorException("No output product specified.");
        }
        requests.add(createRequest());
        return requests;
    }

    /**
     * Sets a new Request list to be edited.
     *
     * @param requests the request list to be edited
     */
    @Override
    public void setRequests(final Vector requests) throws ProcessorException {
        Guardian.assertNotNull("requests", requests);
        if (!requests.isEmpty()) {
            final Request request = (Request) requests.elementAt(0);
            _requestFile = request.getFile();
            updateParamInputFile(request);
            updateParamOutputFile(request);
            updateParamOutputFormat(request);
            updateLogParameter(request);
        } else {
            setDefaultRequests();
        }
    }

    /**
     * Create a new default request for the sst processor and sets it to the UI
     */
    @Override
    public void setDefaultRequests() throws ProcessorException {
        final Vector<Request> requests = new Vector<Request>();
        requests.add(createDefaultRequest());
        setRequests(requests);
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

    /**
     * Creates a request with all paramneters set to their respective default values.
     *
     * @return the default request
     */
    private Request createDefaultRequest() {
        _paramGroup.getParameter(SmileConstants.INPUT_PRODUCT_PARAM_NAME).setDefaultValue();
        _paramGroup.getParameter(SmileConstants.OUTPUT_PRODUCT_PARAM_NAME).setDefaultValue();
        _paramGroup.getParameter(SmileConstants.OUTPUT_FORMAT_PARAM_NAME).setDefaultValue();
        _paramGroup.getParameter(SmileConstants.LOG_PREFIX_PARAM_NAME).setDefaultValue();
        _paramGroup.getParameter(SmileConstants.LOG_TO_OUTPUT_PARAM_NAME).setDefaultValue();
        return createRequest();
    }

    /**
     * Creates all user interface components of the sst user interface
     */
    private void createUI() {
        initParamGroup();
        _tabbedPane = new JTabbedPane();
        _tabbedPane.add("I/O Parameters", createPathTab());
        HelpSys.enableHelp(_tabbedPane, "smileScientificToolPlugIn");
    }

    /**
     * Initializes theparameter group to hold all parameter needed for the processor.
     */
    private void initParamGroup() {
        _paramGroup = new ParamGroup();
        _paramGroup.addParameter(_factory.createDefaultInputProductParameter());
        _paramGroup.addParameter(_factory.createDefaultOutputProductParameter());
        _paramGroup.addParameter(_factory.createOutputFormatParameter());
        _paramGroup.addParameter(_factory.createDefaultLogfileParameter());
        _paramGroup.addParameter(_factory.createDefaultLogPatternParameter(SmileConstants.DEFAULT_LOG_PREFIX));
        try {
            _paramGroup.addParameter(_factory.createLogToOutputParameter("false"));
        } catch (ParamValidateException e) {
            _logger.warning("Unable to validate parameter '" + ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
            Debug.trace(e);
        }
        _paramGroup.addParamChangeListener(createParamChangeListener());
    }

    private ParamChangeListener createParamChangeListener() {
        return new ParamChangeListener() {
            @Override
            public void parameterValueChanged(final ParamChangeEvent event) {
                final Parameter parameter = event.getParameter();
                if (parameter.getName().equals(SmileConstants.INPUT_PRODUCT_PARAM_NAME)) {
                    checkForValidInputProduct(parameter);
                } else if (parameter.getName().equals(SmileConstants.OUTPUT_PRODUCT_PARAM_NAME)) {
                    if (hasParameterEmptyString(parameter)) {
                        getApp().showWarningDialog("No output product specified.");  /*I18N*/
                    }
                }
            }
        };
    }

    /**
     * Create the UI tab for the path editing.
     *
     * @return the panel containing the paths tab
     */
    private JPanel createPathTab() {
        JPanel panel = GridBagUtils.createDefaultEmptyBorderPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints(null);
        gbc.gridy = 0;
        Parameter param;

        // input product
        // -------------
        param = _paramGroup.getParameter(SmileConstants.INPUT_PRODUCT_PARAM_NAME);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, weighty=1");
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, fill=HORIZONTAL, weightx=1, weighty=1");
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        // output product
        // --------------
        param = _paramGroup.getParameter(SmileConstants.OUTPUT_PRODUCT_PARAM_NAME);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "insets.top=7,anchor=SOUTHWEST, weighty=0.5");
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "insets.top=0,anchor=NORTHWEST, weighty=1");
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        // output format
        // -------------
        param = _paramGroup.getParameter(SmileConstants.OUTPUT_FORMAT_PARAM_NAME);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "insets.top=7, anchor=SOUTHWEST, fill=NONE, weightx = 0, weighty=0.5");
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "insets.top=0, anchor=NORTHWEST, weighty=0.5");
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        // logging
        // -------
        param = _paramGroup.getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, fill=HORIZONTAL, weighty=0.5, insets.top=7");
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, weighty=0.5, insets.top=0");
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        param = _paramGroup.getParameter(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, fill=HORIZONTAL, weighty=0.5, insets.bottom=0");
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        return panel;
    }

    /**
     * Creates an input product reference from the request.
     *
     * @return the reference to the input product
     */
    private ProductRef createInputProductRef() {
        String filePath = _paramGroup.getParameter(SmileConstants.INPUT_PRODUCT_PARAM_NAME).getValueAsText();
        return new ProductRef(new File(filePath), null, null);
    }

    /**
     * Creates an output product reference from the request.
     *
     * @return the reference to the output product
     */
    private ProductRef createOutputProductRef() {
        String fileName = _paramGroup.getParameter(SmileConstants.OUTPUT_PRODUCT_PARAM_NAME).getValueAsText();
        String fileFormat = _paramGroup.getParameter(SmileConstants.OUTPUT_FORMAT_PARAM_NAME).getValueAsText();

        return ProcessorUtils.createProductRef(fileName, fileFormat);
    }

    private Parameter createOutputFormatParamForRequest() {
        String outputFormat = _paramGroup.getParameter(SmileConstants.OUTPUT_FORMAT_PARAM_NAME).getValueAsText();
        return new Parameter(SmileConstants.OUTPUT_FORMAT_PARAM_NAME, outputFormat);
    }

    private void updateParamOutputFormat(Request request) {
        String format = request.getParameter(SmileConstants.OUTPUT_FORMAT_PARAM_NAME).getValueAsText();
        _paramGroup.getParameter(SmileConstants.OUTPUT_FORMAT_PARAM_NAME).setValue(format, null);
    }

    private void updateParamOutputFile(Request request) {
        File file = new File(request.getOutputProductAt(0).getFilePath());
        _paramGroup.getParameter(SmileConstants.OUTPUT_PRODUCT_PARAM_NAME).setValue(file, null);
    }

    private void updateParamInputFile(Request request) {
        File file = new File(request.getInputProductAt(0).getFilePath());
        _paramGroup.getParameter(SmileConstants.INPUT_PRODUCT_PARAM_NAME).setValue(file, null);
    }

    private void updateLogParameter(Request request) {
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

    private Request createRequest() {
        Request request = new Request();
        request.setType(SmileConstants.REQUEST_TYPE);
        request.setFile(_requestFile);
        request.addInputProduct(createInputProductRef());
        request.addOutputProduct(createOutputProductRef());
        request.addParameter(createOutputFormatParamForRequest());
        try {
            request.addParameter(
                    _factory.createParameter(SmileConstants.PARAM_NAME_OUTPUT_INCLUDE_ALL_SPECTRAL_BANDS, "true"));
            request.addParameter(_factory.createParameter(SmileConstants.PARAM_NAME_BANDS_TO_PROCESS,
                                                          StringUtils.arrayToCsv(
                                                                  EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES)));
        } catch (RequestElementFactoryException e) {
            Debug.trace(e);
        }
        request.addParameter(_paramGroup.getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME));
        request.addParameter(_paramGroup.getParameter(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME));
        return request;
    }

    /*
     * Brings up a message box if the input product is not valid. Valid input products are: MERIS level 1b products with
     * existing detector index band. The message box only comes up if the parameter contains an existing file. So you
     * can create requests with not existing input products without interfering message Box.
     */
    private void checkForValidInputProduct(Parameter parameter) {
        Object value = parameter.getValue();
        File file = null;
        if (value instanceof File) {
            file = (File) value;
        }
        if (value instanceof String) {
            file = new File((String) value);
        }
        if (file == null || !file.exists()) {
            return;
        }
        String msg = null;
        try {
            Product product = ProductIO.readProduct(file);
            if (product != null) {
                final String diName = EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME;
                if (product.getBand(diName) == null) {
                    final String ssiName = EnvisatConstants.MERIS_SPECTRAL_SHIFT_INDEX_DS_NAME;
                    if (product.getBand(ssiName) != null) {
                        msg = "The input product is obviously a MERIS L1b product but\n" +
                              "contains spectral shift indexes (band '" + ssiName + "')\n" +
                              "instead of MERIS detector indexes (band '" + diName + "').\n" +
                              "The Smile Correction Processor only works with detector indexes.";
                    } else {
                        msg = "The Smile Correction Processor only works with MERIS L1b products\n" +
                              "which also contain the MERIS detector indexes (band '" + diName + "').";
                    }
                }
                product.dispose();
            } else {
                msg = "Unknown file format.";
            }
        } catch (IOException e) {
            msg = e.getMessage();
        }
        if (msg != null) {
            JOptionPane.showMessageDialog(_tabbedPane, "Invalid input file:\n" + msg,
                                          SmileProcessor.PROCESSOR_NAME, JOptionPane.ERROR_MESSAGE);
        }
    }

    private static boolean hasParameterEmptyString(final Parameter parameter) {
        final String valueAsText = parameter.getValueAsText();
        return valueAsText.trim().length() <= 0;
    }
}
