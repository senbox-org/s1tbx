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
import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.ParamParseException;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestElementFactoryException;
import org.esa.beam.framework.processor.RequestValidator;
import org.esa.beam.framework.processor.ui.ProcessorApp;
import org.esa.beam.framework.ui.io.FileArrayEditor;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.processor.binning.L3Constants;
import org.esa.beam.processor.binning.L3Processor;
import org.esa.beam.util.Debug;
import org.esa.beam.util.StringUtils;

import javax.swing.JComponent;
import javax.swing.JTabbedPane;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

@Deprecated
/**
 * This class implements the graphical user interface for the level 3 processor.
 *
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class L3OneShotUI extends L3UI {

    private JTabbedPane uiPane;
    private FileArrayEditor inProductEditor;
    private Request initRequest;
    private Request updateRequest;
    private Request finalRequest;
    private File firstListProductFile;
    private Product exampleProduct;
    private ProcessingParamsTable processingParamsTable;

    /**
     * Constructs the object with given processor.
     */
    public L3OneShotUI(L3Processor processor) throws ProcessorException {
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

    /**
     * Sets a new Request vector to be edited.
     */
    @Override
    public void setRequests() throws ProcessorException {
        initRequest = ensureRequestOfType(L3Constants.PROCESS_TYPE_INIT);
        updateRequest = ensureRequestOfType(L3Constants.PROCESS_TYPE_UPDATE);
        finalRequest = ensureRequestOfType(L3Constants.PROCESS_TYPE_FINALIZE);

        // make sure all requests work on the same database
        ensureDbLocation();
    }

    /**
     * Create a set of new default requests.
     */
    @Override
    protected void setDefaultRequestsImpl() throws ProcessorException {
        initRequest = ensureRequestOfType(L3Constants.PROCESS_TYPE_INIT);
        updateRequest = ensureRequestOfType(L3Constants.PROCESS_TYPE_UPDATE);
        finalRequest = ensureRequestOfType(L3Constants.PROCESS_TYPE_FINALIZE);

        requests.add(initRequest);
        requests.add(updateRequest);
        requests.add(finalRequest);
    }

    /**
     * Retrieves the requests currently edited.
     */
    @Override
    protected void collectRequestsFromUI(final List requests) throws ProcessorException {
        final Request initRequest = createInitRequest();
        final Request updateRequest = createUpdateRequest();
        final Request finalRequest = createFinalRequest();

        this.requests.add(initRequest);
        this.requests.add(updateRequest);
        this.requests.add(finalRequest);
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
                if (parameter == null || !StringUtils.contains(L3Constants.PROCESS_TYPE_VALUE_SET,
                                                               parameter.getValueAsText())) {
                    getApp().showInfoDialog("Unknown processing type.", null);
                    return false;
                }
                final String processingType = parameter.getValueAsText();

                if (L3Constants.PROCESS_TYPE_INIT.equals(processingType)) {
                    return validateProcessingParameters(request);
                } else if (L3Constants.PROCESS_TYPE_UPDATE.equals(processingType)) {

                } else if (L3Constants.PROCESS_TYPE_FINALIZE.equals(processingType)) {
                    if (request.getNumOutputProducts() == 0) {
                        getApp().showInfoDialog("Please choose an output product.",
                                                null);
                        return false;
                    }
                }
                return true;
            }
        });
    }


    /**
     * Called when the file list is updated
     */
    public void updatedList(File firstFile) {
        if (exampleProduct != null) {
            exampleProduct.dispose();
            exampleProduct = null;
        }

        if (firstFile != null) {
            if (!firstFile.equals(firstListProductFile)) {
                // something changed
                firstListProductFile = firstFile;
            }
        } else {
            firstListProductFile = null;
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    @Override
    protected boolean isOneShotUI() {
        return true;
    }

    /**
     * Crates the user interface
     */
    private void createUI() throws ProcessorException {
        initInputProductEditor();

        uiPane = new JTabbedPane();
        uiPane.add(TAB_NAME_INPUT, createInputProductsPanel(inProductEditor));
        uiPane.add(TAB_NAME_OUTPUT, createOutParamsPane());
        uiPane.add(TAB_NAME_PROCESSING_PARAMETERS, createProcessingParametersPanel());
        processingParamsTable = new ProcessingParamsTable();
        uiPane.add(TAB_NAME_BANDS, createBandsPanel(processingParamsTable));
        HelpSys.enableHelp(uiPane, "binningScientificTool");

        updateEstimatedProductSize(processingParamsTable);
    }

    private void initInputProductEditor() {
        final L3OneShotUI parent = this;
        final FileArrayEditor.EditorParent editorParent = new FileArrayEditor.EditorParent() {
            public File getUserInputDir() {
                return parent.getUserInputDir();
            }

            public void setUserInputDir(File newDir) {
                parent.setUserInputDir(newDir);
            }
        };
        inProductEditor = new FileArrayEditor(editorParent, "Input products");
        inProductEditor.setListener(new FileArrayEditor.FileArrayEditorListener() {
            public void updatedList(File[] files) {
                if (files != null && files.length > 0) {
                    parent.updatedList(files[0]);
                } else {
                    parent.updatedList(null);
                }
            }
        });
    }

    /**
     * Searches the vector of requests for a request of the desired processing type. If none is found, it creates a
     * default request of desired processType
     */
    private Request ensureRequestOfType(String desiredType) throws ProcessorException {
        for (int n = 0; n < requests.size(); n++) {
            final Request request = (Request) requests.elementAt(n);
            final Parameter param = request.getParameter(L3Constants.PROCESS_TYPE_PARAM_NAME);
            if (param == null) {
                throw new ProcessorException("The parameter '" + L3Constants.PROCESS_TYPE_PARAM_NAME + "' is not set");
            }
            final String processType = param.getValueAsText();
            if (processType.equalsIgnoreCase(desiredType)) {
                return request;
            }
        }

        // if not found the desired request type create a default one
        return createRequest(desiredType);
    }

    /**
     * Creates a request of desired process taype with default parameters set
     */
    private Request createRequest(String processType) throws ProcessorException {
        final Request request = new Request();
        request.setType(L3Constants.REQUEST_TYPE);
        request.addParameter(reqElemFactory.generateDefaultDbLocation());

        if (processType.equalsIgnoreCase(L3Constants.PROCESS_TYPE_INIT)) {
            request.addParameter(reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME,
                                                                 L3Constants.PROCESS_TYPE_INIT));
            request.addParameter(reqElemFactory.createParamWithDefaultValueSet(L3Constants.GRID_CELL_SIZE_PARAM_NAME));
            request.addParameter(reqElemFactory.createParamWithDefaultValueSet(L3Constants.LAT_MIN_PARAMETER_NAME));
            request.addParameter(reqElemFactory.createParamWithDefaultValueSet(L3Constants.LAT_MAX_PARAMETER_NAME));
            request.addParameter(reqElemFactory.createParamWithDefaultValueSet(L3Constants.LON_MIN_PARAMETER_NAME));
            request.addParameter(reqElemFactory.createParamWithDefaultValueSet(L3Constants.LON_MAX_PARAMETER_NAME));
            request.addParameter(
                    reqElemFactory.createParamWithDefaultValueSet(L3Constants.RESAMPLING_TYPE_PARAM_NAME));
        } else if (processType.equalsIgnoreCase(L3Constants.PROCESS_TYPE_UPDATE)) {
            request.addParameter(reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME,
                                                                 L3Constants.PROCESS_TYPE_UPDATE));
        } else if (processType.equalsIgnoreCase(L3Constants.PROCESS_TYPE_FINALIZE)) {
            request.addParameter(reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME,
                                                                 L3Constants.PROCESS_TYPE_FINALIZE));
            request.addParameter(reqElemFactory.createParamWithDefaultValueSet(L3Constants.TAILORING_PARAM_NAME));
        }

        return request;
    }

    /**
     * Adds an initial request filled with the current parameter settings to the request list requested from the outside
     * world.
     */
    private Request createInitRequest() throws RequestElementFactoryException {
        Request initRequest = new Request();

        initRequest.setType(L3Constants.REQUEST_TYPE);
        initRequest.addParameter(
                reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME, L3Constants.PROCESS_TYPE_INIT));
        initRequest.addParameter(paramGroup.getParameter(L3Constants.DATABASE_PARAM_NAME));
        initRequest.addParameter(paramGroup.getParameter(L3Constants.LAT_MIN_PARAMETER_NAME));
        initRequest.addParameter(paramGroup.getParameter(L3Constants.LAT_MAX_PARAMETER_NAME));
        initRequest.addParameter(paramGroup.getParameter(L3Constants.LON_MIN_PARAMETER_NAME));
        initRequest.addParameter(paramGroup.getParameter(L3Constants.LON_MAX_PARAMETER_NAME));
        initRequest.addParameter(paramGroup.getParameter(L3Constants.LOG_PREFIX_PARAM_NAME));
        initRequest.addParameter(paramGroup.getParameter(L3Constants.LOG_TO_OUTPUT_PARAM_NAME));

        initRequest.addParameter(paramGroup.getParameter(L3Constants.RESAMPLING_TYPE_PARAM_NAME));
        if (isFluxConserving()) {
            initRequest.addParameter(paramGroup.getParameter(L3Constants.CELLS_PER_DEGREE_PARAM_NAME));
        } else {
            initRequest.addParameter(paramGroup.getParameter(L3Constants.GRID_CELL_SIZE_PARAM_NAME));
        }
        collectProcessingParameters(processingParamsTable, initRequest);
        return initRequest;
    }

    /**
     * Adds an update request filled with the current parameter settings to the request list requested from the outside
     * world.
     */
    private Request createUpdateRequest() throws ProcessorException {
        Request updateRequest = new Request();

        updateRequest.setType(L3Constants.REQUEST_TYPE);
        updateRequest.addParameter(
                reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME, L3Constants.PROCESS_TYPE_UPDATE));
        updateRequest.addParameter(paramGroup.getParameter(L3Constants.DATABASE_PARAM_NAME));
        updateRequest.addParameter(paramGroup.getParameter(L3Constants.LOG_PREFIX_PARAM_NAME));
        updateRequest.addParameter(paramGroup.getParameter(L3Constants.LOG_TO_OUTPUT_PARAM_NAME));

        // add all files in the file list editor
        final List files = inProductEditor.getFiles();
        File currentFile = null;
        for (int n = 0; n < files.size(); n++) {
            currentFile = (File) files.get(n);
            updateRequest.addInputProduct(new ProductRef(currentFile));
        }
        return updateRequest;
    }

    /**
     * Adds a final request filled with the current parameter settings to the request list requested from the outside
     * world.
     */
    private Request createFinalRequest() throws RequestElementFactoryException {
        Request finalReq = new Request();

        finalReq.setType(L3Constants.REQUEST_TYPE);
        finalReq.addParameter(reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME,
                                                              L3Constants.PROCESS_TYPE_FINALIZE));
        finalReq.addParameter(paramGroup.getParameter(L3Constants.DATABASE_PARAM_NAME));
        finalReq.addParameter(reqElemFactory.createParameter(L3Constants.DELETE_DB_PARAMETER_NAME, "true"));
        finalReq.addParameter(paramGroup.getParameter(L3Constants.LOG_PREFIX_PARAM_NAME));
        finalReq.addParameter(paramGroup.getParameter(L3Constants.LOG_TO_OUTPUT_PARAM_NAME));
        finalReq.addParameter(paramGroup.getParameter(L3Constants.TAILORING_PARAM_NAME));

        getOutputFile(finalReq);
        return finalReq;
    }

    /**
     * updates the UI components with the given requests data
     */
    @Override
    protected void updateUI() throws ProcessorException {
        setOutputFile(finalRequest);

        final List<File> inFiles = new ArrayList<File>();
        for (int n = 0; n < updateRequest.getNumInputProducts(); n++) {
            final ProductRef inProd = updateRequest.getInputProductAt(n);
            inFiles.add(inProd.getFile());
        }
        inProductEditor.setFiles(inFiles);

        try {
            updateUIComponent(L3Constants.RESAMPLING_TYPE_PARAM_NAME, initRequest);
            updateUIComponent(L3Constants.GRID_CELL_SIZE_PARAM_NAME, initRequest);
            updateUIComponent(L3Constants.CELLS_PER_DEGREE_PARAM_NAME, initRequest);
            updateUIComponent(L3Constants.LAT_MIN_PARAMETER_NAME, initRequest);
            updateUIComponent(L3Constants.LAT_MAX_PARAMETER_NAME, initRequest);
            updateUIComponent(L3Constants.LON_MIN_PARAMETER_NAME, initRequest);
            updateUIComponent(L3Constants.LON_MAX_PARAMETER_NAME, initRequest);
            updateUIComponent(L3Constants.LOG_PREFIX_PARAM_NAME, initRequest);
            updateUIComponent(L3Constants.LOG_TO_OUTPUT_PARAM_NAME, initRequest);
            updateUIComponent(L3Constants.TAILORING_PARAM_NAME, finalRequest);

            final Request initRequest = this.initRequest;
            updateProcessingParams(processingParamsTable, initRequest);

            updateEstimatedProductSize(processingParamsTable);
        } catch (ParamValidateException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    /**
     * Extracts the database location from the init request. Creates a database location if none is set in init
     * requests. Then makes sure that all requests work on the same database
     */
    private void ensureDbLocation() throws ProcessorException {
        Parameter param = initRequest.getParameter(L3Constants.DATABASE_PARAM_NAME);
        String dbPath = param.getValueAsText();
        String otherPath = null;

        try {
            param = updateRequest.getParameter(L3Constants.DATABASE_PARAM_NAME);
            otherPath = param.getValueAsText();
            if (!otherPath.equalsIgnoreCase(dbPath)) {
                param.setValueAsText(dbPath);
            }

            param = finalRequest.getParameter(L3Constants.DATABASE_PARAM_NAME);
            otherPath = param.getValueAsText();
            if (!otherPath.equalsIgnoreCase(dbPath)) {
                param.setValueAsText(dbPath);
            }
        } catch (ParamParseException e) {
            throw new ProcessorException(e.getMessage(), e);
        } catch (ParamValidateException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    /**
     * Creates the parameter group
     */
    private void createParameterGroup() throws ProcessorException {
        paramGroup = new ParamGroup();

        paramGroup.addParameter(reqElemFactory.createDefaultOutputProductParameter());
        paramGroup.addParameter(reqElemFactory.createOutputFormatParameter());

        paramGroup.addParameter(reqElemFactory.generateDefaultDbLocation());
        paramGroup.addParameter(reqElemFactory.createParamWithDefaultValueSet(L3Constants.GRID_CELL_SIZE_PARAM_NAME));
        paramGroup.addParameter(reqElemFactory.createParamWithDefaultValueSet(L3Constants.LAT_MIN_PARAMETER_NAME));
        paramGroup.addParameter(reqElemFactory.createParamWithDefaultValueSet(L3Constants.LAT_MAX_PARAMETER_NAME));
        paramGroup.addParameter(reqElemFactory.createParamWithDefaultValueSet(L3Constants.LON_MIN_PARAMETER_NAME));
        paramGroup.addParameter(reqElemFactory.createParamWithDefaultValueSet(L3Constants.LON_MAX_PARAMETER_NAME));

        paramGroup.addParameter(reqElemFactory.createParamWithDefaultValueSet(L3Constants.TAILORING_PARAM_NAME));
        paramGroup.addParameter(
                reqElemFactory.createParamWithDefaultValueSet(L3Constants.RESAMPLING_TYPE_PARAM_NAME));
        paramGroup.addParameter(
                reqElemFactory.createParamWithDefaultValueSet(L3Constants.CELLS_PER_DEGREE_PARAM_NAME));
        paramGroup.addParameter(reqElemFactory.createDefaultLogPatternParameter(L3Constants.DEFAULT_LOGFILE_PREFIX));
        try {
            paramGroup.addParameter(reqElemFactory.createLogToOutputParameter("false"));
        } catch (ParamValidateException e) {
            logger.warning("Unable to validate parameter '" + ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
            Debug.trace(e);
        }
        paramGroup.addParamChangeListener(createResamplingChangeListener());
        paramGroup.addParamChangeListener(new ParamChangeListener() {
            public void parameterValueChanged(final ParamChangeEvent event) {
                updateEstimatedProductSize(processingParamsTable);
            }
        });
    }

    /**
     * Callback invokd on changes in the algorithm Parameter
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
     * Copies the value of the output file parameter to the request passed in
     */
    private void getOutputFile(Request request) {
        final String fileName = paramGroup.getParameter(L3Constants.OUTPUT_PRODUCT_PARAM_NAME).getValueAsText();
        final String fileFormat = paramGroup.getParameter(L3Constants.OUTPUT_FORMAT_PARAM_NAME).getValueAsText();

        final ProductRef outputProdutRef = ProcessorUtils.createProductRef(fileName, fileFormat);
        if (outputProdutRef != null) {
            request.addOutputProduct(outputProdutRef);
        }
    }

    @Override
    protected Product getExampleProduct(boolean forBandFilter) throws IOException {
        if (exampleProduct != null) {
            return exampleProduct;
        }
        if (firstListProductFile == null) {
            getApp().showInfoDialog("No input product selected.\n" +
                                    "Please choose at least one input product.", null);
            return null;
        } else {
            exampleProduct = ProductIO.readProduct(firstListProductFile);
            return exampleProduct;
        }
    }

}

