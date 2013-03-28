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

import org.esa.beam.framework.param.ParamChangeEvent;
import org.esa.beam.framework.param.ParamChangeListener;
import org.esa.beam.framework.param.ParamGroup;
import org.esa.beam.framework.param.ParamValidateException;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProductRef;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.ui.io.FileArrayEditor;
import org.esa.beam.framework.help.HelpSys;
import org.esa.beam.processor.binning.L3Constants;
import org.esa.beam.processor.binning.L3Processor;
import org.esa.beam.processor.binning.database.BinDatabaseConstants;
import org.esa.beam.util.Debug;
import org.esa.beam.util.io.FileUtils;

import javax.swing.JComponent;
import javax.swing.JPanel;
import java.io.File;
import java.util.ArrayList;
import java.util.List;

@Deprecated
/**
 * Provides the user interface functionality for the Level 3 update processor.
 *
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class L3UpdateUI extends L3UI {

    private JPanel uiPane;
    private FileArrayEditor inProductEditor;
    private Request updateRequest;

    /**
     * Constructs the object with given processor.
     */
    public L3UpdateUI(L3Processor processor) throws ProcessorException {
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
            createUI();
        }
        return uiPane;
    }

    @Override
    protected void setRequests() throws ProcessorException {
        ensureUpdateRequest();
    }

    /**
     * Retrieves the requests currently edited.
     */
    @Override
    protected void collectRequestsFromUI(final List requests) throws ProcessorException {
        Request request = new Request();

        try {
            request.setType(L3Constants.REQUEST_TYPE);
            request.addParameter(reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME,
                                                                 L3Constants.PROCESS_TYPE_UPDATE));
            request.addParameter(paramGroup.getParameter(L3Constants.DATABASE_PARAM_NAME));
            request.addParameter(paramGroup.getParameter(L3Constants.LOG_PREFIX_PARAM_NAME));
            request.addParameter(paramGroup.getParameter(L3Constants.LOG_TO_OUTPUT_PARAM_NAME));

            // add all files in the file list editor
            final List files = inProductEditor.getFiles();
            File currentFile = null;
            for (int n = 0; n < files.size(); n++) {
                currentFile = (File) files.get(n);
                request.addInputProduct(new ProductRef(currentFile));
            }
        } catch (IllegalArgumentException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
        this.requests.add(request);
    }

    /**
     * Create a set of new default requests.
     */
    @Override
    public void setDefaultRequestsImpl() throws ProcessorException {
        updateRequest = new Request();
        updateRequest.setType(L3Constants.REQUEST_TYPE);
        updateRequest.addParameter(
                reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME, L3Constants.PROCESS_TYPE_INIT));
        updateRequest.addParameter(reqElemFactory.generateDefaultDbLocation());
        updateRequest.addParameter(
                reqElemFactory.createDefaultLogPatternParameter(L3Constants.DEFAULT_LOG_PREFIX_UPDATE));
        try {
            final Parameter logToOutputParameter = reqElemFactory.createLogToOutputParameter("false");
            logToOutputParameter.getProperties().setDefaultValue(false);
            updateRequest.addParameter(logToOutputParameter);
        } catch (ParamValidateException e) {
            logger.warning("Unable to validate parameter '" + ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
            Debug.trace(e);
        }
        inProductEditor.setFiles(new ArrayList());
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Crates the user interface
     */
    private void createUI() {
        initInputProductEditor();
        uiPane = createInputProductsPanel(inProductEditor);
        HelpSys.enableHelp(uiPane, "binningUpdateTool");
    }

    private void initInputProductEditor() {
        final L3UpdateUI parent = this;
        final FileArrayEditor.EditorParent editorParent = new FileArrayEditor.EditorParent() {
            public File getUserInputDir() {
                return parent.getUserInputDir();
            }

            public void setUserInputDir(File newDir) {
                parent.setUserInputDir(newDir);
            }
        };
        inProductEditor = new FileArrayEditor(editorParent, "Input products");
    }

    /**
     * Creates the parameter group
     */
    private void createParameterGroup() throws ProcessorException {
        paramGroup = new ParamGroup();

        paramGroup.addParameter(reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME,
                                                                 L3Constants.PROCESS_TYPE_UPDATE));
        paramGroup.addParameter(reqElemFactory.generateDefaultDbLocation());
        paramGroup.addParameter(
                reqElemFactory.createDefaultLogPatternParameter(L3Constants.DEFAULT_LOG_PREFIX_UPDATE));
        try {
            paramGroup.addParameter(reqElemFactory.createLogToOutputParameter("false"));
        } catch (ParamValidateException e) {
            logger.warning("Unable to validate parameter '" + ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
            Debug.trace(e);
        }
        paramGroup.addParamChangeListener(new ParamChangeListener() {
            public void parameterValueChanged(ParamChangeEvent event) {
                if (event.getParameter().getName().equals(L3Constants.DATABASE_PARAM_NAME)) {
                    handleUpdateDatabase();
                }
            }
        });
    }

    /**
     * Callback invoked on changes in the database Parameter
     */
    private void handleUpdateDatabase() {
        Parameter databaseParam = paramGroup.getParameter(L3Constants.DATABASE_PARAM_NAME);
        File dbFile = (File) databaseParam.getValue();
        dbFile = FileUtils.ensureExtension(dbFile, BinDatabaseConstants.FILE_EXTENSION);
        try {
            databaseParam.setValue(dbFile);
        } catch (ParamValidateException e) {
            logger.warning(e.getMessage());
        }
    }

    /**
     * Checks the requests vector for a request of type init and sets the one found to the member field.
     */
    private void ensureUpdateRequest() throws ProcessorException {
        Request request = null;
        Parameter param = null;
        String value = null;
        boolean bFound = false;

        for (int n = 0; n < requests.size(); n++) {
            request = (Request) requests.elementAt(n);
            // check for correct type
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

            if (!value.equalsIgnoreCase(L3Constants.PROCESS_TYPE_UPDATE)) {
                continue;
            }

            // passed all tests
            bFound = true;
            break;
        }

        if (bFound) {
            updateRequest = request;
        } else {
            setDefaultRequests();
        }
    }

    /**
     * Updates the UI and the connected parameters with the values of the currenr request
     */
    @Override
    protected void updateUI() throws ProcessorException {
        Parameter toUpdate = null;
        Parameter param = null;

        try {
            param = updateRequest.getParameter(L3Constants.DATABASE_PARAM_NAME);
            if (param != null) {
                toUpdate = paramGroup.getParameter(L3Constants.DATABASE_PARAM_NAME);
                toUpdate.setValue(param.getValue());
            }

            final List<File> inFiles = new ArrayList<File>();
            ProductRef inProd = null;
            for (int n = 0; n < updateRequest.getNumInputProducts(); n++) {
                inProd = updateRequest.getInputProductAt(n);
                inFiles.add(inProd.getFile());
            }
            inProductEditor.setFiles(inFiles);

            param = updateRequest.getParameter(L3Constants.LOG_PREFIX_PARAM_NAME);
            if (param != null) {
                toUpdate = paramGroup.getParameter(L3Constants.LOG_PREFIX_PARAM_NAME);
                toUpdate.setValue(param.getValue());
            }
            param = updateRequest.getParameter(L3Constants.LOG_TO_OUTPUT_PARAM_NAME);
            if (param != null) {
                toUpdate = paramGroup.getParameter(L3Constants.LOG_TO_OUTPUT_PARAM_NAME);
                toUpdate.setValue(param.getValue());
            }
        } catch (ParamValidateException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }
}
