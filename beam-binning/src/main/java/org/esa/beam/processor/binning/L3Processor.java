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
package org.esa.beam.processor.binning;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.framework.processor.Processor;
import org.esa.beam.framework.processor.ProcessorConstants;
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.ProcessorUtils;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.framework.processor.RequestElementFactory;
import org.esa.beam.framework.processor.ui.ProcessorUI;
import org.esa.beam.processor.binning.ui.L3FinalUI;
import org.esa.beam.processor.binning.ui.L3InitUI;
import org.esa.beam.processor.binning.ui.L3OneShotUI;
import org.esa.beam.processor.binning.ui.L3UI;
import org.esa.beam.processor.binning.ui.L3UpdateUI;
import org.esa.beam.util.Guardian;

import java.util.logging.Logger;

@Deprecated
/**
 * The main class for the BEAM Level 3 processor.
 *
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
public class L3Processor extends Processor {

    public static final int UI_TYPE_INIT = 0;
    public static final int UI_TYPE_UPDATE = 1;
    public static final int UI_TYPE_FINAL = 2;
    public static final int UI_TYPE_ONE_SHOT = 3;

    private static final int TYPE_UNKNOWN = 0;
    private static final int TYPE_INIT = 1;
    private static final int TYPE_UPDATE = 2;
    private static final int TYPE_FINALIZE = 3;

    public static final String PROCESSOR_NAME = "BEAM L3 Processor";
    private static final String PROCESSOR_SYMBOLIC_NAME = "beam-binning";
    private static final String _initName = "BEAM L3 Initial Processor";
    private static final String _updateName = "BEAM L3 Update Processor";
    private static final String _finalName = "BEAM L3 Final Processor";
    private static final String _version = "2.0.100";
    private static final String _copyright = "Copyright (C) 2002-2004 by Brockmann Consult (info@brockmann-consult.de)";

    private int _processType;       // mod from _requestType, TLankester 25/04/05
    private int _uiType;
    private boolean _processingFailure;

    protected L3SubProcessor subProcessor;

    private L3UI _ui;
    private Logger _logger;
    public static final String HELP_ID = "binningScientificTool";

    /**
     * Constructs the processor with {@link #UI_TYPE_ONE_SHOT}.
     */
    public L3Processor() {
        this(UI_TYPE_ONE_SHOT);
    }

    /**
     * Constructs the processor.
     * @param uiType one of {@link #UI_TYPE_INIT}, {@link #UI_TYPE_UPDATE}, {@link #UI_TYPE_FINAL}, {@link #UI_TYPE_ONE_SHOT}
     */
    public L3Processor(final int uiType) {
        Guardian.assertWithinRange("uiType", uiType, UI_TYPE_INIT, UI_TYPE_ONE_SHOT);
        _processType = TYPE_UNKNOWN;
        _uiType = uiType;
        _logger = Logger.getLogger(L3Constants.LOGGER_NAME);
        setDefaultHelpId(HELP_ID);
    }

    /**
     * Processes a request.
     */
    @Override
    public void process(ProgressMonitor pm) throws ProcessorException {
        _logger.info(ProcessorConstants.LOG_MSG_START_REQUEST);
        _processingFailure = false;
        subProcessor = null;

        loadRequestParameter();
        getSubProcessor().process(pm);

        if (_processingFailure) {
            setCurrentStatus(L3Constants.STATUS_COMPLETED_WITH_WARNING);
        }
        _logger.info(ProcessorConstants.LOG_MSG_FINISHED_REQUEST);
    }

    /**
     * Retrieves the name of the processor
     */
    @Override
    public String getName() {
        String strRet = PROCESSOR_NAME;

        if (_uiType == UI_TYPE_INIT) {
            strRet = _initName;
        } else if (_uiType == UI_TYPE_UPDATE) {
            strRet = _updateName;
        } else if (_uiType == UI_TYPE_FINAL) {
            strRet = _finalName;
        }

        return strRet;
    }

    /**
     * Returns the symbolic name of the processor.
     */
    @Override
    public String getSymbolicName() {
        return PROCESSOR_SYMBOLIC_NAME;
    }

    /**
     * Retrieves a version string of the processor
     */
    @Override
    public String getVersion() {
        return _version;
    }

    /**
     * Retrieves copyright information of the processor
     */
    @Override
    public String getCopyrightInformation() {
        return _copyright;
    }

    /**
     * Retrieves the warning messages that occurred during processing (if any). This string is shown in the
     * application  dialog popping up after the processor ended processing with warnings.
     * Override to perform processor specific warning messages.
     *
     * @return the messages
     */
    @Override
    public String[] getWarningMessages() {
        if (subProcessor != null) {
            return subProcessor.getWarningMessages();
        } else {
            // this should never happen
            return super.getWarningMessages();
        }
    }

    /**
     * Creates the GUI for the processor.
     */
    @Override
    public ProcessorUI createUI() throws ProcessorException {
        if (_uiType == UI_TYPE_ONE_SHOT) {
            _ui = new L3OneShotUI(this);
        } else if (_uiType == UI_TYPE_INIT) {
            _ui = new L3InitUI(this);
        } else if (_uiType == UI_TYPE_UPDATE) {
            _ui = new L3UpdateUI(this);
        } else if (_uiType == UI_TYPE_FINAL) {
            _ui = new L3FinalUI(this);
        }

        return _ui;
    }

    /**
     * Returns the request element factory for this processor.
     */
    @Override
    public RequestElementFactory getRequestElementFactory() {
        return L3RequestElementFactory.getInstance();
    }

    /**
     * Retrieves the number of progressbars needed by the processor. Override this method if more than one progressbar
     * is needed, i.e. for multistage processes.
     *
     * @return the number of progressbars needed.
     */
    @Override
    public int getProgressDepth() {
        if (_uiType == UI_TYPE_ONE_SHOT) {
            return 3;
        } else {
            return 2;
        }
    }

    /**
     * Retrieve a message to be displayed on completion.
     */
    @Override
    public String getCompletionMessage() {
        String strRet = null;

        // just needed for the processor types that do not create an output product.
        if ((_uiType == UI_TYPE_INIT) || (_uiType == UI_TYPE_UPDATE)) {
            if (_processingFailure) {
                strRet = L3Constants.MSG_COMPLETED_WITH_WARNINGS;
            } else {
                strRet = L3Constants.MSG_COMPLETED_SUCCESSFUL;
            }
        }

        return strRet;
    }


    /**
     * Writes the processor specific logging file header to the logstream currently set.
     * <p/>
     * <p>This method is called by the processor runner initially after a logging sink has been created.
     */
    @Override
    public final void logHeader() {
        if (_ui == null) {
            _logger.info(L3Constants.LOG_MSG_HEADER + _version);
            _logger.info(_copyright);
            _logger.info("");
        }
    }

    /**
     * Retrieves a progress message for the request passed in. Override this method if you need custom messaging.
     *
     * @param request
     *
     * @return the progress message
     */
    @Override
    public String getProgressMessage(final Request request) {
        final Parameter procTypeParameter = request.getParameter(L3Constants.PROCESS_TYPE_PARAM_NAME);
        if (procTypeParameter == null) {
            return super.getProgressMessage(request);
        }
        final String processingType = procTypeParameter.getValueAsText();

        if (processingType.equalsIgnoreCase(L3Constants.PROCESS_TYPE_INIT)) {
            return "Initializing L3 Product";
        } else if (processingType.equalsIgnoreCase(L3Constants.PROCESS_TYPE_UPDATE)) {
            return "Updating L3 Product";
        } else if (processingType.equalsIgnoreCase(L3Constants.PROCESS_TYPE_FINALIZE)) {
            return "Finalizing L3 Product";
        }

        return super.getProgressMessage(request);
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    // sets the processing error flag
    protected void raiseErrorFlag() {
        _processingFailure = true;
    }

    /**
     * Sets the current processor state - called by subprocessors
     */
    protected void setState(final int state) {
        setCurrentStatus(state);
    }

    /**
     * Retrieves the current processor state - called by subprocessors
     */
    protected int getState() {
        return getCurrentStatus();
    }

    /**
     * Returns whether the request is of type init or not.
     */
    protected boolean isTypeInit() {
        return _processType == TYPE_INIT;
    }

    /**
     * Returns whether the request is of type update or not.
     */
    protected boolean isTypeUpdate() {
        return _processType == TYPE_UPDATE;
    }

    /**
     * Returns whether the request is of type finalize or not.
     */
    protected boolean isTypeFinalize() {
        return _processType == TYPE_FINALIZE;
    }

    /**
     * Loads all parameter needed from the current request
     */
    private void loadRequestParameter() throws ProcessorException {
        final Request request = getRequest();

        Request.checkRequestType(request, L3Constants.REQUEST_TYPE);
        readProcessType(request);

        String prefix = "l3";
        if (_processType == TYPE_INIT) {
            prefix = L3Constants.DEFAULT_LOG_PREFIX_INIT;
        } else if (_processType == TYPE_UPDATE) {
            prefix = L3Constants.DEFAULT_LOG_PREFIX_UPDATE;
        } else if (_processType == TYPE_FINALIZE) {
            prefix = L3Constants.DEFAULT_LOG_PREFIX_FINAL;
        }

        ProcessorUtils.setProcessorLoggingHandler(prefix, request,
                                                  getName(), getVersion(), getCopyrightInformation());
    }

    /**
     * Reads the process type from the request and sets the according field.
     * Mod. TLankester 25/04/05 with _processType instead of _requestType
     *
     * @throws ProcessorException when an unknown process type is set
     */
    private void readProcessType(final Request request) throws ProcessorException {
        final Parameter param = request.getParameter(L3Constants.PROCESS_TYPE_PARAM_NAME);

        if (param == null) {
            raiseError(
                    "Parameter of type \"" + L3Constants.PROCESS_TYPE_PARAM_NAME + "\" not set.\nPlease invoke the processor with a correct processing request.");
        }

        final String type = param.getValueAsText();
        if (type.equalsIgnoreCase(L3Constants.PROCESS_TYPE_INIT)) {
            _processType = TYPE_INIT;
        } else if (type.equalsIgnoreCase(L3Constants.PROCESS_TYPE_UPDATE)) {
            _processType = TYPE_UPDATE;
        } else if (type.equalsIgnoreCase(L3Constants.PROCESS_TYPE_FINALIZE)) {
            _processType = TYPE_FINALIZE;
        } else {
            _processType = TYPE_UNKNOWN;
        }
    }

    /**
     * Performs cleanup when a crash happened during processing. Called by base class on failures.
     */
    @Override
    protected void cleanupAfterFailure() {
        try {
            getSubProcessor().cleanUp();
        } catch (ProcessorException e) {
            _logger.severe("Couldn't clean up after a failure!");
        }
    }

    /**
     * Raises a ProcessorException exception with the given message and sets the processor's error flag.
     */
    protected void raiseError(final String message) throws ProcessorException {
        raiseErrorFlag();
        throw new ProcessorException(message);
    }

    /**
     * Retrievves the right subprocessor. If noen exist - create one.
     */
    protected L3SubProcessor getSubProcessor() throws ProcessorException {
        if (subProcessor == null) {
            if (isTypeInit()) {
                subProcessor = new L3InitialProcessor(this);
            } else if (isTypeUpdate()) {
                subProcessor = new L3UpdateProcessor(this);
            } else if (isTypeFinalize()) {
                subProcessor = new L3FinalProcessor(this);
            } else {
                raiseError(L3Constants.LOG_MSG_INVALID_REQUEST_TYPE);
            }
        }
        return subProcessor;
    }
}
