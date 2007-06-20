/*
 * $Id: L3InitUI.java,v 1.5 2007/04/18 13:01:13 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.processor.binning.ui;

import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
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
import org.esa.beam.framework.help.HelpSys;
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


/**
 * Provides the user interface functionality for the Level 3 initial processor.
 */
public class L3InitUI extends L3UI {

    private Request _initRequest;
    private JTabbedPane _uiPane;
    private ProcessingParamsTable _processingParamsTable;
    private Product _exampleProduct;

    /**
     * Constructs the object with given processor.
     */
    public L3InitUI(L3Processor processor) throws ProcessorException {
        super(processor);
        _uiPane = null;
        createParameterGroup();
    }

    /**
     * Retrieves the base component for the processor specific user interface classes. This can be any Java Swing
     * containertype.
     */
    public JComponent getGuiComponent() {
        if (_uiPane == null) {
            try {
                createUI();
            } catch (ProcessorException e) {
                Debug.trace(e);
                _logger.severe("Unable to create user interface: " + e.getMessage());
                return null;
            }
        }
        return _uiPane;
    }

    protected void setRequests() throws ProcessorException {
        ensureInitRequest();
    }

    /**
     * Retrieves the requests currently edited.
     */
    protected void collectRequestsFromUI(final List requests) throws ProcessorException {
        Request request = new Request();
        request.setType(L3Constants.REQUEST_TYPE);
        addParameterToRequest(request);

        _requests.add(request);
    }

    /**
     * Create a set of new default requests.
     */
    protected void setDefaultRequestsImpl() throws ProcessorException {
        _initRequest = new Request();

        try {
            _initRequest.setType(L3Constants.REQUEST_TYPE);
            _initRequest.addParameter(
                    _reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME,
                                                    L3Constants.PROCESS_TYPE_INIT));
            _initRequest.addParameter(_reqElemFactory.generateDefaultDbLocation());
            _initRequest.addParameter(
                    _reqElemFactory.createParamWithDefaultValueSet(L3Constants.LAT_MIN_PARAMETER_NAME));
            _initRequest.addParameter(
                    _reqElemFactory.createParamWithDefaultValueSet(L3Constants.LAT_MAX_PARAMETER_NAME));
            _initRequest.addParameter(
                    _reqElemFactory.createParamWithDefaultValueSet(L3Constants.LON_MIN_PARAMETER_NAME));
            _initRequest.addParameter(
                    _reqElemFactory.createParamWithDefaultValueSet(L3Constants.LON_MAX_PARAMETER_NAME));
            _initRequest.addParameter(
                    _reqElemFactory.createDefaultLogPatternParameter(L3Constants.DEFAULT_LOG_PREFIX_INIT));
            _initRequest.addParameter(
                    _reqElemFactory.createParamWithDefaultValueSet(L3Constants.RESAMPLING_TYPE_PARAM_NAME));
            _initRequest.addParameter(
                    _reqElemFactory.createParamWithDefaultValueSet(L3Constants.GRID_CELL_SIZE_PARAM_NAME));

            try {
                _initRequest.addParameter(_reqElemFactory.createLogToOutputParameter("false"));
            } catch (ParamValidateException e) {
                _logger.warning("Unable to validate parameter '" + ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
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
        _uiPane = new JTabbedPane();
        _uiPane.addTab(TAB_NAME_PROCESSING_PARAMETERS, createProcessingParametersPanel());
        _processingParamsTable = new ProcessingParamsTable();
        _uiPane.addTab(TAB_NAME_BANDS, createBandsPanel(_processingParamsTable));
        HelpSys.enableHelp(_uiPane, "binningInitializeTool");
    }

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

    protected Product getExampleProduct(boolean forBandFilter) throws IOException {
        if (_exampleProduct != null) {
            final int answer;
            if (forBandFilter) {
                answer = getApp().showQuestionDialog("Do you want to use the example product you have loaded before?",
                                                     null);
            } else {
                answer = JOptionPane.YES_OPTION;
            }
            if (answer == JOptionPane.YES_OPTION) {
                return _exampleProduct;
            } else {
                _exampleProduct.dispose();
                _exampleProduct = null;
            }
        } else if (_exampleProduct == null && !forBandFilter) {
            final int answer = getApp().showQuestionDialog(
                    "Do you want to open an example product to validate the processing request?", null);
            if (answer == JOptionPane.NO_OPTION) {
                return null;
            }
        }

        final BeamFileChooser beamFileChooser = new BeamFileChooser();
        beamFileChooser.setCurrentDirectory(getUserInputDir());
        final int answer = beamFileChooser.showOpenDialog(_uiPane);
        if (BeamFileChooser.APPROVE_OPTION == answer) {
            final File selectedFile = beamFileChooser.getSelectedFile();
            final File inputDir = selectedFile.getParentFile();
            setUserInputDir(inputDir);
            _exampleProduct = ProductIO.readProduct(selectedFile, null);
            return _exampleProduct;
        }
        return null;
    }

    /**
     * Creates the parameter group
     */
    private void createParameterGroup() throws ProcessorException {
        _paramGroup = new ParamGroup();

        _paramGroup.addParameter(_reqElemFactory.generateDefaultDbLocation());
        _paramGroup.addParameter(_reqElemFactory.createParamWithDefaultValueSet(L3Constants.LAT_MIN_PARAMETER_NAME));
        _paramGroup.addParameter(_reqElemFactory.createParamWithDefaultValueSet(L3Constants.LAT_MAX_PARAMETER_NAME));
        _paramGroup.addParameter(_reqElemFactory.createParamWithDefaultValueSet(L3Constants.LON_MIN_PARAMETER_NAME));
        _paramGroup.addParameter(_reqElemFactory.createParamWithDefaultValueSet(L3Constants.LON_MAX_PARAMETER_NAME));
        _paramGroup.addParameter(_reqElemFactory.createDefaultLogPatternParameter(L3Constants.DEFAULT_LOG_PREFIX_INIT));
        _paramGroup.addParameter(
                _reqElemFactory.createParamWithDefaultValueSet(L3Constants.RESAMPLING_TYPE_PARAM_NAME));
        _paramGroup.addParameter(_reqElemFactory.createParamWithDefaultValueSet(L3Constants.GRID_CELL_SIZE_PARAM_NAME));
        _paramGroup.addParameter(
                _reqElemFactory.createParamWithDefaultValueSet(L3Constants.CELLS_PER_DEGREE_PARAM_NAME));

        try {
            _paramGroup.addParameter(_reqElemFactory.createLogToOutputParameter("false"));
        } catch (ParamValidateException e) {
            _logger.warning("Unable to validate parameter '" + ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
            Debug.trace(e);
        }

        _paramGroup.addParameter(
                _reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME, L3Constants.PROCESS_TYPE_INIT));

        _paramGroup.addParamChangeListener(createResamplingChangeListener());
        _paramGroup.addParamChangeListener(new ParamChangeListener() {
            public void parameterValueChanged(ParamChangeEvent event) {
                updateEstimatedProductSize(_processingParamsTable);
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

        for (int n = 0; n < _requests.size(); n++) {
            request = (Request) _requests.elementAt(n);
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
            _initRequest = request;
        } else {
            setDefaultRequests();
        }
    }

    /**
     * Adds the named Parameter to the request passed in
     */
    private void addParameterToRequest(Request request) throws RequestElementFactoryException {
        request.addParameter(_paramGroup.getParameter(L3Constants.PROCESS_TYPE_PARAM_NAME));
        request.addParameter(_paramGroup.getParameter(L3Constants.DATABASE_PARAM_NAME));
        request.addParameter(_paramGroup.getParameter(L3Constants.LAT_MIN_PARAMETER_NAME));
        request.addParameter(_paramGroup.getParameter(L3Constants.LAT_MAX_PARAMETER_NAME));
        request.addParameter(_paramGroup.getParameter(L3Constants.LON_MIN_PARAMETER_NAME));
        request.addParameter(_paramGroup.getParameter(L3Constants.LON_MAX_PARAMETER_NAME));
        request.addParameter(_paramGroup.getParameter(L3Constants.LOG_PREFIX_PARAM_NAME));
        request.addParameter(_paramGroup.getParameter(L3Constants.LOG_TO_OUTPUT_PARAM_NAME));

        request.addParameter(_paramGroup.getParameter(L3Constants.RESAMPLING_TYPE_PARAM_NAME));
        if (isFluxConsrving()) {
            request.addParameter(_paramGroup.getParameter(L3Constants.CELLS_PER_DEGREE_PARAM_NAME));
        } else {
            request.addParameter(_paramGroup.getParameter(L3Constants.GRID_CELL_SIZE_PARAM_NAME));
        }

        collectProcessingParameters(_processingParamsTable, request);
    }

    /**
     * Updates the UI and the connected parameters with the values of the currenr request
     */
    protected void updateUI() throws ProcessorException {
        try {
            updateUIComponent(L3Constants.DATABASE_PARAM_NAME, _initRequest);
            updateUIComponent(L3Constants.RESAMPLING_TYPE_PARAM_NAME, _initRequest);
            updateUIComponent(L3Constants.GRID_CELL_SIZE_PARAM_NAME, _initRequest);
            updateUIComponent(L3Constants.CELLS_PER_DEGREE_PARAM_NAME, _initRequest);
            updateUIComponent(L3Constants.LAT_MIN_PARAMETER_NAME, _initRequest);
            updateUIComponent(L3Constants.LAT_MAX_PARAMETER_NAME, _initRequest);
            updateUIComponent(L3Constants.LON_MIN_PARAMETER_NAME, _initRequest);
            updateUIComponent(L3Constants.LON_MAX_PARAMETER_NAME, _initRequest);
            updateUIComponent(ProcessorConstants.LOG_PREFIX_PARAM_NAME, _initRequest);
            updateUIComponent(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME, _initRequest);

            updateProcessingParams(_processingParamsTable, _initRequest);

            updateEstimatedProductSize(_processingParamsTable);
        } catch (ParamValidateException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }

    /**
     * Callback invoked on changes in the algorithm Parameter
     */
    private void handleUpdateAlgorithm() {
        Parameter algoParam = _paramGroup.getParameter(L3Constants.ALGORITHM_PARAMETER_NAME);
        Parameter weightParam = _paramGroup.getParameter(L3Constants.WEIGHT_COEFFICIENT_PARAMETER_NAME);
        String value = algoParam.getValueAsText();

        // check if algorithm requires weight coefficient or not.
        if (value.equalsIgnoreCase(L3Constants.ALGORITHM_VALUE_SET[2])) {
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
        Parameter databaseParam = _paramGroup.getParameter(L3Constants.DATABASE_PARAM_NAME);
        File dbFile = (File) databaseParam.getValue();
        dbFile = FileUtils.ensureExtension(dbFile, BinDatabaseConstants.FILE_EXTENSION);
        if (dbFile.exists()) {
            getApp().showWarningDialog(
                    "The database: " + dbFile.getPath() + " already exists.\nPlease choose another location."); /*I18N*/
        } else {
            try {
                databaseParam.setValue(dbFile);
            } catch (ParamValidateException e) {
                _logger.warning(e.getMessage());
            }
        }
    }
}
