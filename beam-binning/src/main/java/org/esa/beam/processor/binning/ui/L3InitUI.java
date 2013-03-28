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
package org.esa.beam.processor.binning.ui;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.framework.processor.RequestValidator;
import org.esa.beam.framework.processor.ui.ProcessorApp;
import org.esa.beam.processor.binning.L3Constants;
import org.esa.beam.processor.binning.L3Processor;
import org.esa.beam.processor.binning.database.BinDatabaseConstants;
import org.esa.beam.util.Debug;
import org.esa.beam.util.io.BeamFileChooser;
import org.esa.beam.util.io.FileUtils;

import javax.swing.JComponent;
import javax.swing.JOptionPane;
import javax.swing.JTabbedPane;
import java.io.File;
import java.io.IOException;
import java.util.List;

@Deprecated
/**
 * Provides the user interface functionality for the Level 3 initial processor.
 *
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class L3InitUI extends L3UI {

    private Request initRequest;
    private JTabbedPane uiPane;
    private ProcessingParamsTable processingParamsTable;
    private Product exampleProduct;

    /**
     * Constructs the object with given processor.
     */
    public L3InitUI(L3Processor processor) throws ProcessorException {
        super(processor);
        uiPane = null;
        createParameterGroup();
    }

    /**
     * Retrieves the base component for the processor specific user interface classes. This can be any Java Swing
     * containertype.
     */
    public JComponent getGuiComponent() {
        if (uiPane == null) {
            try {
                createUI();
            } catch (ProcessorException e) {
                Debug.trace(e);
                logger.severe("Unable to create user interface: " + e.getMessage());
                return null;
            }
        }
        return uiPane;
    }

    @Override
    protected void setRequests() throws ProcessorException {
        ensureInitRequest();
    }

    /**
     * Retrieves the requests currently edited.
     */
    @Override
    protected void collectRequestsFromUI(final List requests) throws ProcessorException {
        Request request = new Request();
        request.setType(L3Constants.REQUEST_TYPE);
        addParameterToRequest(request);

        this.requests.add(request);
    }

    /**
     * Create a set of new default requests.
     */
    @Override
    protected void setDefaultRequestsImpl() throws ProcessorException {
        initRequest = new Request();

        try {
            initRequest.setType(L3Constants.REQUEST_TYPE);
            initRequest.addParameter(
                    reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME,
                                                   L3Constants.PROCESS_TYPE_INIT));
            initRequest.addParameter(reqElemFactory.generateDefaultDbLocation());
            initRequest.addParameter(
                    reqElemFactory.createParamWithDefaultValueSet(L3Constants.LAT_MIN_PARAMETER_NAME));
            initRequest.addParameter(
                    reqElemFactory.createParamWithDefaultValueSet(L3Constants.LAT_MAX_PARAMETER_NAME));
            initRequest.addParameter(
                    reqElemFactory.createParamWithDefaultValueSet(L3Constants.LON_MIN_PARAMETER_NAME));
            initRequest.addParameter(
                    reqElemFactory.createParamWithDefaultValueSet(L3Constants.LON_MAX_PARAMETER_NAME));
            initRequest.addParameter(
                    reqElemFactory.createDefaultLogPatternParameter(L3Constants.DEFAULT_LOG_PREFIX_INIT));
            initRequest.addParameter(
                    reqElemFactory.createParamWithDefaultValueSet(L3Constants.RESAMPLING_TYPE_PARAM_NAME));
            initRequest.addParameter(
                    reqElemFactory.createParamWithDefaultValueSet(L3Constants.GRID_CELL_SIZE_PARAM_NAME));

            try {
                initRequest.addParameter(reqElemFactory.createLogToOutputParameter("false"));
            } catch (ParamValidateException e) {
                logger.warning("Unable to validate parameter '" + ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
                Debug.trace(e);
            }
        } catch (RequestElementFactoryException e) {
            throw e;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Crates the user interface
     */
    private void createUI() throws ProcessorException {
        uiPane = new JTabbedPane();
        uiPane.addTab(TAB_NAME_PROCESSING_PARAMETERS, createProcessingParametersPanel());
        processingParamsTable = new ProcessingParamsTable();
        uiPane.addTab(TAB_NAME_BANDS, createBandsPanel(processingParamsTable));
        HelpSys.enableHelp(uiPane, "binningInitializeTool");
    }

    @Override
    public void setApp(ProcessorApp app) {
        super.setApp(app);
        app.addRequestValidator(new RequestValidator() {
            public boolean validateRequest(Processor processor, Request request) {
                if (!validateRequestType(request)) {
                    return false;
                }

                final Parameter parameter = request.getParameter(L3Constants.PROCESS_TYPE_PARAM_NAME);
                if (parameter == null || !L3Constants.PROCESS_TYPE_INIT.equals(parameter.getValueAsText())) {
                    getApp().showInfoDialog("The processing type is not '" + L3Constants.PROCESS_TYPE_INIT + "'.",
                                            null);
                    return false;
                }

                return validateProcessingParameters(request);
            }
        });
    }

    @Override
    protected Product getExampleProduct(boolean forBandFilter) throws IOException {
        if (exampleProduct != null) {
            final int answer;
            if (forBandFilter) {
                answer = getApp().showQuestionDialog("Do you want to use the example product you have loaded before?",
                                                     null);
            } else {
                answer = JOptionPane.YES_OPTION;
            }
            if (answer == JOptionPane.YES_OPTION) {
                return exampleProduct;
            } else {
                exampleProduct.dispose();
                exampleProduct = null;
            }
        } else if (!forBandFilter) {
            final int answer = getApp().showQuestionDialog(
                    "Do you want to open an example product to validate the processing request?", null);
            if (answer == JOptionPane.NO_OPTION) {
                return null;
            }
        }

        final BeamFileChooser beamFileChooser = new BeamFileChooser();
        beamFileChooser.setCurrentDirectory(getUserInputDir());
        final int answer = beamFileChooser.showOpenDialog(uiPane);
        if (BeamFileChooser.APPROVE_OPTION == answer) {
            final File selectedFile = beamFileChooser.getSelectedFile();
            final File inputDir = selectedFile.getParentFile();
            if (inputDir != null) {
                setUserInputDir(inputDir);
            }
            exampleProduct = ProductIO.readProduct(selectedFile);
            return exampleProduct;
        }
        return null;
    }

    /**
     * Creates the parameter group
     */
    private void createParameterGroup() throws ProcessorException {
        paramGroup = new ParamGroup();

        paramGroup.addParameter(reqElemFactory.generateDefaultDbLocation());
        paramGroup.addParameter(reqElemFactory.createParamWithDefaultValueSet(L3Constants.LAT_MIN_PARAMETER_NAME));
        paramGroup.addParameter(reqElemFactory.createParamWithDefaultValueSet(L3Constants.LAT_MAX_PARAMETER_NAME));
        paramGroup.addParameter(reqElemFactory.createParamWithDefaultValueSet(L3Constants.LON_MIN_PARAMETER_NAME));
        paramGroup.addParameter(reqElemFactory.createParamWithDefaultValueSet(L3Constants.LON_MAX_PARAMETER_NAME));
        paramGroup.addParameter(reqElemFactory.createDefaultLogPatternParameter(L3Constants.DEFAULT_LOG_PREFIX_INIT));
        paramGroup.addParameter(
                reqElemFactory.createParamWithDefaultValueSet(L3Constants.RESAMPLING_TYPE_PARAM_NAME));
        paramGroup.addParameter(reqElemFactory.createParamWithDefaultValueSet(L3Constants.GRID_CELL_SIZE_PARAM_NAME));
        paramGroup.addParameter(
                reqElemFactory.createParamWithDefaultValueSet(L3Constants.CELLS_PER_DEGREE_PARAM_NAME));

        try {
            paramGroup.addParameter(reqElemFactory.createLogToOutputParameter("false"));
        } catch (ParamValidateException e) {
            logger.warning("Unable to validate parameter '" + ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
            Debug.trace(e);
        }

        paramGroup.addParameter(
                reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME, L3Constants.PROCESS_TYPE_INIT));

        paramGroup.addParamChangeListener(createResamplingChangeListener());
        paramGroup.addParamChangeListener(new ParamChangeListener() {
            public void parameterValueChanged(ParamChangeEvent event) {
                updateEstimatedProductSize(processingParamsTable);
            }
        });
    }

    /**
     * Checks the requests vector for a request of type init and sets the one found to the member field.
     */
    private void ensureInitRequest() throws ProcessorException {
        Request request = null;
        Parameter param = null;
        String value = null;
        boolean bFound = false;

        for (int n = 0; n < requests.size(); n++) {
            request = (Request) requests.elementAt(n);
            // check for correct type
            value = request.getType();
            if (!request.isRequestType(L3Constants.REQUEST_TYPE)) {
                continue;
            }

            param = request.getParameter(L3Constants.PROCESS_TYPE_PARAM_NAME);
            if (param == null) {
                continue;
            }

            value = param.getValueAsText();
            if (value == null) {
                continue;
            }

            if (!value.equalsIgnoreCase(L3Constants.PROCESS_TYPE_INIT)) {
                continue;
            }

            // passed all tests
            bFound = true;
            break;
        }

        if (bFound) {
            initRequest = request;
        } else {
            setDefaultRequests();
        }
    }

    /**
     * Adds the named Parameter to the request passed in
     */
    private void addParameterToRequest(Request request) throws RequestElementFactoryException {
        request.addParameter(paramGroup.getParameter(L3Constants.PROCESS_TYPE_PARAM_NAME));
        request.addParameter(paramGroup.getParameter(L3Constants.DATABASE_PARAM_NAME));
        request.addParameter(paramGroup.getParameter(L3Constants.LAT_MIN_PARAMETER_NAME));
        request.addParameter(paramGroup.getParameter(L3Constants.LAT_MAX_PARAMETER_NAME));
        request.addParameter(paramGroup.getParameter(L3Constants.LON_MIN_PARAMETER_NAME));
        request.addParameter(paramGroup.getParameter(L3Constants.LON_MAX_PARAMETER_NAME));
        request.addParameter(paramGroup.getParameter(L3Constants.LOG_PREFIX_PARAM_NAME));
        request.addParameter(paramGroup.getParameter(L3Constants.LOG_TO_OUTPUT_PARAM_NAME));

        request.addParameter(paramGroup.getParameter(L3Constants.RESAMPLING_TYPE_PARAM_NAME));
        if (isFluxConserving()) {
            request.addParameter(paramGroup.getParameter(L3Constants.CELLS_PER_DEGREE_PARAM_NAME));
        } else {
            request.addParameter(paramGroup.getParameter(L3Constants.GRID_CELL_SIZE_PARAM_NAME));
        }

        collectProcessingParameters(processingParamsTable, request);
    }

    /**
     * Updates the UI and the connected parameters with the values of the currenr request
     */
    @Override
    protected void updateUI() throws ProcessorException {
        try {
            updateUIComponent(L3Constants.DATABASE_PARAM_NAME, initRequest);
            updateUIComponent(L3Constants.RESAMPLING_TYPE_PARAM_NAME, initRequest);
            updateUIComponent(L3Constants.GRID_CELL_SIZE_PARAM_NAME, initRequest);
            updateUIComponent(L3Constants.CELLS_PER_DEGREE_PARAM_NAME, initRequest);
            updateUIComponent(L3Constants.LAT_MIN_PARAMETER_NAME, initRequest);
            updateUIComponent(L3Constants.LAT_MAX_PARAMETER_NAME, initRequest);
            updateUIComponent(L3Constants.LON_MIN_PARAMETER_NAME, initRequest);
            updateUIComponent(L3Constants.LON_MAX_PARAMETER_NAME, initRequest);
            updateUIComponent(ProcessorConstants.LOG_PREFIX_PARAM_NAME, initRequest);
            updateUIComponent(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME, initRequest);

            updateProcessingParams(processingParamsTable, initRequest);

            updateEstimatedProductSize(processingParamsTable);
        } catch (ParamValidateException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    /**
     * Callback invoked on changes in the algorithm Parameter
     */
    private void handleUpdateAlgorithm() {
        Parameter algoParam = paramGroup.getParameter(L3Constants.ALGORITHM_PARAMETER_NAME);
        Parameter weightParam = paramGroup.getParameter(L3Constants.WEIGHT_COEFFICIENT_PARAMETER_NAME);
        String value = algoParam.getValueAsText();

        // check if algorithm requires weight coefficient or not.
        if (value.equalsIgnoreCase(L3Constants.ALGORITHM_VALUE_MINIMUM_MAXIMUM)) {
            // is minmax - no weight required
            weightParam.getEditor().setEnabled(false);
        } else {
            // AME or MLE - needs weight coeff
            weightParam.getEditor().setEnabled(true);
        }
        weightParam.getEditor().updateUI();
    }

    /**
     * Callback invoked on changes in the database Parameter
     */
    private void handleUpdateDatabase() {
        Parameter databaseParam = paramGroup.getParameter(L3Constants.DATABASE_PARAM_NAME);
        File dbFile = (File) databaseParam.getValue();
        dbFile = FileUtils.ensureExtension(dbFile, BinDatabaseConstants.FILE_EXTENSION);
        if (dbFile.exists()) {
            getApp().showWarningDialog(
                    "The database: " + dbFile.getPath() + " already exists.\nPlease choose another location."); /*I18N*/
        } else {
            try {
                databaseParam.setValue(dbFile);
            } catch (ParamValidateException e) {
                logger.warning(e.getMessage());
            }
        }
    }
}
