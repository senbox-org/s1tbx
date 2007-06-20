/*
 * $Id: L3UpdateUI.java,v 1.6 2007/04/18 13:01:13 norman Exp $
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


/**
 * Provides the user interface functionality for the Level 3 update processor.
 */
public class L3UpdateUI extends L3UI {

    private JPanel _uiPane;
    private FileArrayEditor _inProductEditor;
    private Request _updateRequest;

    /**
     * Constructs the object with given processor.
     */
    public L3UpdateUI(L3Processor processor) throws ProcessorException {
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
            createUI();
        }
        return _uiPane;
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
            request.addParameter(_reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME,
                                                                 L3Constants.PROCESS_TYPE_UPDATE));
            request.addParameter(_paramGroup.getParameter(L3Constants.DATABASE_PARAM_NAME));
            request.addParameter(_paramGroup.getParameter(L3Constants.LOG_PREFIX_PARAM_NAME));
            request.addParameter(_paramGroup.getParameter(L3Constants.LOG_TO_OUTPUT_PARAM_NAME));

            // add all files in the file list editor
            final List files = _inProductEditor.getFiles();
            File currentFile = null;
            for (int n = 0; n < files.size(); n++) {
                currentFile = (File) files.get(n);
                request.addInputProduct(new ProductRef(currentFile));
            }
        } catch (IllegalArgumentException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
        _requests.add(request);
    }

    /**
     * Create a set of new default requests.
     */
    @Override
    public void setDefaultRequestsImpl() throws ProcessorException {
        _updateRequest = new Request();
        _updateRequest.setType(L3Constants.REQUEST_TYPE);
        _updateRequest.addParameter(
                _reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME, L3Constants.PROCESS_TYPE_INIT));
        _updateRequest.addParameter(_reqElemFactory.generateDefaultDbLocation());
        _updateRequest.addParameter(
                _reqElemFactory.createDefaultLogPatternParameter(L3Constants.DEFAULT_LOG_PREFIX_UPDATE));
        try {
            final Parameter logToOutputParameter = _reqElemFactory.createLogToOutputParameter("false");
            logToOutputParameter.getProperties().setDefaultValue(false);
            _updateRequest.addParameter(logToOutputParameter);
        } catch (ParamValidateException e) {
            _logger.warning("Unable to validate parameter '" + ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
            Debug.trace(e);
        }
        _inProductEditor.setFiles(new ArrayList());
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Crates the user interface
     */
    private void createUI() {
        initInputProductEditor();
        _uiPane = createInputProductsPanel(_inProductEditor);
        HelpSys.enableHelp(_uiPane, "binningUpdateTool");
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
        _inProductEditor = new FileArrayEditor(editorParent, "Input products");
    }

    /**
     * Creates the parameter group
     */
    private void createParameterGroup() throws ProcessorException {
        _paramGroup = new ParamGroup();

        _paramGroup.addParameter(_reqElemFactory.createParameter(L3Constants.PROCESS_TYPE_PARAM_NAME,
                                                                 L3Constants.PROCESS_TYPE_UPDATE));
        _paramGroup.addParameter(_reqElemFactory.generateDefaultDbLocation());
        _paramGroup.addParameter(
                _reqElemFactory.createDefaultLogPatternParameter(L3Constants.DEFAULT_LOG_PREFIX_UPDATE));
        try {
            _paramGroup.addParameter(_reqElemFactory.createLogToOutputParameter("false"));
        } catch (ParamValidateException e) {
            _logger.warning("Unable to validate parameter '" + ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME + "'");
            Debug.trace(e);
        }
        _paramGroup.addParamChangeListener(new ParamChangeListener() {
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
        Parameter databaseParam = _paramGroup.getParameter(L3Constants.DATABASE_PARAM_NAME);
        File dbFile = (File) databaseParam.getValue();
        dbFile = FileUtils.ensureExtension(dbFile, BinDatabaseConstants.FILE_EXTENSION);
        try {
            databaseParam.setValue(dbFile);
        } catch (ParamValidateException e) {
            _logger.warning(e.getMessage());
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

        for (int n = 0; n < _requests.size(); n++) {
            request = (Request) _requests.elementAt(n);
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
            _updateRequest = request;
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
            param = _updateRequest.getParameter(L3Constants.DATABASE_PARAM_NAME);
            if (param != null) {
                toUpdate = _paramGroup.getParameter(L3Constants.DATABASE_PARAM_NAME);
                toUpdate.setValue(param.getValue());
            }

            final List<File> inFiles = new ArrayList<File>();
            ProductRef inProd = null;
            for (int n = 0; n < _updateRequest.getNumInputProducts(); n++) {
                inProd = _updateRequest.getInputProductAt(n);
                inFiles.add(inProd.getFile());
            }
            _inProductEditor.setFiles(inFiles);

            param = _updateRequest.getParameter(L3Constants.LOG_PREFIX_PARAM_NAME);
            if (param != null) {
                toUpdate = _paramGroup.getParameter(L3Constants.LOG_PREFIX_PARAM_NAME);
                toUpdate.setValue(param.getValue());
            }
            param = _updateRequest.getParameter(L3Constants.LOG_TO_OUTPUT_PARAM_NAME);
            if (param != null) {
                toUpdate = _paramGroup.getParameter(L3Constants.LOG_TO_OUTPUT_PARAM_NAME);
                toUpdate.setValue(param.getValue());
            }
        } catch (ParamValidateException e) {
            throw new ProcessorException(e.getMessage(), e);
        }
    }
}
