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
package org.esa.beam.processor.cloud;

import org.esa.beam.dataio.envisat.EnvisatConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.param.*;
import org.esa.beam.framework.processor.*;
import org.esa.beam.framework.processor.ui.AbstractProcessorUI;
import org.esa.beam.framework.processor.ui.ProcessorApp;
import org.esa.beam.framework.ui.GridBagUtils;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;

import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import java.awt.GridBagConstraints;
import java.io.File;
import java.io.IOException;
import java.util.Vector;
import java.util.logging.Logger;

/**
 * Description of CloudProcessorUI
 *
 * @author Marco Peters
 *
 * @deprecated since BEAM 4.11. No replacement.
 */
@Deprecated
public class CloudProcessorUI extends AbstractProcessorUI {

    private JTabbedPane _tabbedPane;
    private ParamGroup _paramGroup;
    private File _requestFile;
    private CloudRequestElementFactory _factory;
    private final Logger _logger;

    /**
     * Creates the ui class with default parameters
     */
    public CloudProcessorUI() {
        _factory = CloudRequestElementFactory.getInstance();
        _logger = Logger.getLogger(CloudConstants.LOGGER_NAME);
    }

    /**
     * Retrieves the base component for the processor specific user interface classes. This can be any Java Swing
     * containertype. This method creates the UI from scratch if not present.
     */
    public JComponent getGuiComponent() {
        if (_tabbedPane == null) {
            createUI();
        }
        return _tabbedPane;
    }

    /**
     * Retrieves the requests currently edited.
     */
    public Vector getRequests() throws ProcessorException {
        final Vector<Request> requests = new Vector<Request>();
        final Parameter outputProductParam = _paramGroup.getParameter(CloudConstants.OUTPUT_PRODUCT_PARAM_NAME);
        if (hasParameterEmptyString(outputProductParam)) {
            throw new ProcessorException("No output product specified.");       /*I18N*/
        }
        requests.add(createRequest());
        return requests;
    }

    /**
     * Sets a new Request list to be edited.
     *
     * @param requests the request list to be edited must not be <code>null</code>.
     */
    public void setRequests(final Vector requests) throws ProcessorException {
        Guardian.assertNotNull("requests", requests);
        if (requests.size() > 0) {
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
        _paramGroup.getParameter(CloudConstants.INPUT_PRODUCT_PARAM_NAME).setDefaultValue();
        _paramGroup.getParameter(CloudConstants.OUTPUT_PRODUCT_PARAM_NAME).setDefaultValue();
        _paramGroup.getParameter(CloudConstants.OUTPUT_FORMAT_PARAM_NAME).setDefaultValue();
        _paramGroup.getParameter(CloudConstants.LOG_PREFIX_PARAM_NAME).setDefaultValue();
        _paramGroup.getParameter(CloudConstants.LOG_TO_OUTPUT_PARAM_NAME).setDefaultValue();
        return createRequest();
    }

    /**
     * Creates all user interface components of the sst user interface
     */
    private void createUI() {
        initParamGroup();
        _tabbedPane = new JTabbedPane();
        _tabbedPane.add("I/O Parameters", createPathTab());
    }

    /**
     * Initializes theparameter group to hold all parameter needed for the processor.
     */
    private void initParamGroup() {
        _paramGroup = new ParamGroup();
        final Parameter inputProductParameter = _factory.createDefaultInputProductParameter();
        _paramGroup.addParameter(inputProductParameter);
        final Parameter outputProductParameter = _factory.createDefaultOutputProductParameter();
        _paramGroup.addParameter(outputProductParameter);
        _paramGroup.addParameter(_factory.createOutputFormatParameter());
        _paramGroup.addParameter(_factory.createDefaultLogPatternParameter(CloudConstants.DEFAULT_LOG_PREFIX));
        try {
            _paramGroup.addParameter(_factory.createLogToOutputParameter("false"));
        } catch (ParamValidateException e) {
            _logger.warning("Unable to validate parameter '" + CloudConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
            Debug.trace(e);
        }

        inputProductParameter.addParamChangeListener(new ParamChangeListener() {
            public void parameterValueChanged(final ParamChangeEvent event) {
                checkForValidInputProduct(inputProductParameter);
            }
        });

        outputProductParameter.addParamChangeListener(new ParamChangeListener() {
            public void parameterValueChanged(final ParamChangeEvent event) {
                if (hasParameterEmptyString(outputProductParameter)) {
                    getApp().showWarningDialog("No output product specified."); /*I18N*/
                }
            }
        });
    }

    /**
     * Create the ui tab for the path editing.
     *
     * @return the panel containing the paths tab
     */
    private JPanel createPathTab() {
        final JPanel panel = GridBagUtils.createDefaultEmptyBorderPanel();
        final GridBagConstraints gbc = GridBagUtils.createConstraints(null);
        gbc.gridy = 0;
        Parameter param;

        // input product
        // -------------
        param = _paramGroup.getParameter(CloudConstants.INPUT_PRODUCT_PARAM_NAME);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, weighty=1");
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, fill=HORIZONTAL, weightx=1, weighty=1");
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        // output product
        // --------------
        param = _paramGroup.getParameter(CloudConstants.OUTPUT_PRODUCT_PARAM_NAME);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "insets.top=7,anchor=SOUTHWEST, weighty=0.5");
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "insets.top=0,anchor=NORTHWEST, weighty=1");
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        // output format
        // -------------
        param = _paramGroup.getParameter(CloudConstants.OUTPUT_FORMAT_PARAM_NAME);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "insets.top=7, anchor=SOUTHWEST, fill=NONE, weightx = 0, weighty=0.5");
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "insets.top=0, anchor=NORTHWEST, weighty=0.5");
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        // logging
        // -------
        param = _paramGroup.getParameter(CloudConstants.LOG_PREFIX_PARAM_NAME);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, fill=HORIZONTAL, weighty=0.5, insets.top=7");
        GridBagUtils.addToPanel(panel, param.getEditor().getLabelComponent(), gbc);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "anchor=NORTHWEST, weighty=0.5, insets.top=0");
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);

        param = _paramGroup.getParameter(CloudConstants.LOG_TO_OUTPUT_PARAM_NAME);
        gbc.gridy++;
        GridBagUtils.setAttributes(gbc, "anchor=SOUTHWEST, fill=HORIZONTAL, weighty=0.5, insets.bottom=0");
        GridBagUtils.addToPanel(panel, param.getEditor().getComponent(), gbc);


        return panel;
    }

    private Request createRequest() {
        final Request request = new Request();
        request.setType(CloudProcessor.REQUEST_TYPE);
        request.setFile(_requestFile);
        request.addInputProduct(createInputProductRef());
        request.addOutputProduct(createOutputProductRef());
        request.addParameter(createOutputFormatParamForRequest());
        request.addParameter(_paramGroup.getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME));
        request.addParameter(_paramGroup.getParameter(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME));
        return request;
    }

    /**
     * Creates an input product reference from the request.
     *
     * @return the product reference
     */
    private ProductRef createInputProductRef() {
        final String filePath = _paramGroup.getParameter(CloudConstants.INPUT_PRODUCT_PARAM_NAME).getValueAsText();
        return new ProductRef(new File(filePath), null, null);
    }

    /**
     * Creates an output product reference from the request.
     *
     * @return the product reference
     */
    private ProductRef createOutputProductRef() {
        final String fileName = _paramGroup.getParameter(CloudConstants.OUTPUT_PRODUCT_PARAM_NAME).getValueAsText();
        final String fileFormat = _paramGroup.getParameter(CloudConstants.OUTPUT_FORMAT_PARAM_NAME).getValueAsText();

        return ProcessorUtils.createProductRef(fileName, fileFormat);
    }

    private Parameter createOutputFormatParamForRequest() {
        final String outputFormat = _paramGroup.getParameter(CloudConstants.OUTPUT_FORMAT_PARAM_NAME).getValueAsText();
        return new Parameter(CloudConstants.OUTPUT_FORMAT_PARAM_NAME, outputFormat);
    }

    private void updateParamOutputFormat(final Request request) {
        final String format = request.getParameter(CloudConstants.OUTPUT_FORMAT_PARAM_NAME).getValueAsText();
        _paramGroup.getParameter(CloudConstants.OUTPUT_FORMAT_PARAM_NAME).setValue(format, null);
    }

    private void updateParamOutputFile(final Request request) {
        final File file = new File(request.getOutputProductAt(0).getFilePath());
        _paramGroup.getParameter(CloudConstants.OUTPUT_PRODUCT_PARAM_NAME).setValue(file, null);
    }

    private void updateParamInputFile(final Request request) {
        final File file = new File(request.getInputProductAt(0).getFilePath());
        _paramGroup.getParameter(CloudConstants.INPUT_PRODUCT_PARAM_NAME).setValue(file, null);
    }

    private void updateLogParameter(final Request request) {
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

    private static boolean hasParameterEmptyString(final Parameter parameter) {
        final String valueAsText = parameter.getValueAsText();

        return valueAsText.trim().length() <= 0;
    }

    private void checkForValidInputProduct(final Parameter parameter) {
        final Object value = parameter.getValue();
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
        Product product = null;
        try {
            product = ProductIO.readProduct(file);
            if (product != null) {
                final String productType = product.getProductType();
                final boolean isValidType = EnvisatConstants.MERIS_L1_TYPE_PATTERN.matcher(productType).matches();
                if (!isValidType) {
                    msg = "The specified input product is not of the expected type.\n" +
                            "The type of the product must be a MERIS L1b product.";           /*I18N*/
                } else {
                    final String[] radianceBandNames = EnvisatConstants.MERIS_L1B_SPECTRAL_BAND_NAMES;
                    final Band[] radianceBands = new Band[radianceBandNames.length];
                    for (int bandIndex = 0; bandIndex < radianceBandNames.length; bandIndex++) {
                        final String radianceBandName = radianceBandNames[bandIndex];
                        radianceBands[bandIndex] = product.getBand(radianceBandName);
                        if (radianceBands[bandIndex] == null) {
                            msg = "Source product does not contain band " + radianceBandName;       /*I18N*/
                            break;
                        }
                    }
                    final Band detectorBand = product.getBand(EnvisatConstants.MERIS_DETECTOR_INDEX_DS_NAME);
                    if (detectorBand == null) {
                        msg = "Source product does not contain detector band.";     /*I18N*/
                    }
                }

            } else {
                msg = "Unknown file format.";       /*I18N*/
            }
        } catch (IOException e) {
            msg = e.getMessage();
        }finally {
            if (product != null) {
                product.dispose();
            }
        }
        if (msg != null) {
            getApp().showWarningDialog("Invalid input file:\n" + msg); /*I18N*/
        }
    }
}
