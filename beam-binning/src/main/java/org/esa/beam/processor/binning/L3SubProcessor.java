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
import org.esa.beam.framework.processor.ProcessorException;
import org.esa.beam.framework.processor.Request;
import org.esa.beam.processor.binning.database.BinDatabaseConstants;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;

import java.io.File;
import java.util.logging.Logger;

@Deprecated
/**
 * @Deprecated since beam-binning 2.1.2 as part of the BEAM 4.11-release. Use module 'beam-binning2' instead.
 */
abstract public class L3SubProcessor {

    private L3Processor _parent;
    private Logger logger;
    private String[] _warningMessages;

    /**
     * Creates the object with given parent processor
     */
    public L3SubProcessor(L3Processor parent) {
        Guardian.assertNotNull("parent", parent);
        _parent = parent;
        logger = Logger.getLogger(L3Constants.LOGGER_NAME);
        _warningMessages = null;
    }

    abstract public void process(ProgressMonitor pm) throws ProcessorException;

    /**
     * Perform cleanup after a processor failure happened. Invoked by processor framework.
     */
    public void cleanUp() {
        //TODO
//        try {
//            closeBinDatabase();
//        } catch (IOException e) {
//            logError(L3Constants.LOG_MSG_ERROR_CLOSE_BINDB);
//            logError(e.getMessage());
//        }
    }

    /**
     * Retrieves the warning messages that occurred during processing (if any). This string is shown in the application
     * dialog popping up after the processor ended processing with warnings.
     * Override to perform processor specific warning messages.
     *
     * @return the messages
     */
    protected String[] getWarningMessages() {
        return _warningMessages;
    }

    /**
     * Adds a warning message to the array of messages held by the processor
     *
     * @param message the new warning message
     */
    protected void addWarningMessage(String message) {
        if (_warningMessages == null) {
            _warningMessages = new String[]{message};
        } else {
            _warningMessages = StringUtils.addToArray(_warningMessages, message);
        }
    }

    /**
     * @return the logger object.
     */
    protected Logger getLogger() {
        return logger;
    }

    /**
     * Sets the error flag in the base processor, so processor can react accordingly.
     */
    protected void raiseErrorFlag() {
        _parent.raiseErrorFlag();
    }

    /**
     * Retrieves the request currently used by the parent processor
     */
    protected Request getRequest() {
        return _parent.getRequest();
    }


    protected void handleError(String message) throws ProcessorException {
        _parent.raiseError(message);
    }

    /**
     * Retrieves the parameter with given name from the request
     *
     * @param paramName the parameter name
     */
    protected Parameter getParameter(String paramName, String errorMessage) throws ProcessorException {
        final Parameter parameter = getRequest().getParameter(paramName);
        if (parameter == null) {
            handleError("Parameter \"" + paramName + "\" not set!\n" + errorMessage);
        }
        final Object value = parameter.getValue();
        if (value == null) {
            handleError("Parameter \"" + paramName + "\" has no value set!\n" + errorMessage);
        }
        return parameter;
    }

    /**
     * Retrieves the paramter with the given name from the request.
     * If the paramter is not set the given default value will be returned.
     *
     * @param paramName
     * @param defaultValue
     *
     * @return the parameter or the default value.
     */
    protected String getStringParamterSafe(String paramName, String defaultValue) {
        final Parameter param = getRequest().getParameter(paramName);
        if (param == null) {
            getLogger().warning("Parameter '" + paramName + "' not set");
            getLogger().warning("... using default value '" + defaultValue + "'");
            return defaultValue;
        } else {
            return param.getValueAsText();
        }
    }

    /**
     * Loads the float parameter with given name and if the parameter is not present
     * sets the default value passed in
     */
    protected float getFloatParameterSafe(String paramName, float defaultValue) {
        final Parameter param = getRequest().getParameter(paramName);
        if (param == null) {
            getLogger().warning("Parameter '" + paramName + "' not set");
            getLogger().warning("... using default value '" + defaultValue + "'");
            return defaultValue;
        } else {
            return (Float) param.getValue();
        }
    }

    /**
     * Sets the current processor state
     */
    protected void setCurrentState(int state) {
        _parent.setState(state);
    }

    /**
     * Retrieves the current processor state
     */
    protected int getCurrentState() {
        return _parent.getState();
    }

    /**
     * Retrieves whether the processor is aborted or not.
     *
     * @return the aborted state
     */
    protected boolean isAborted() {
        return _parent.isAborted();
    }

    /**
     * Checks if the location passed in can be used to create a new database. The method checks if <ul> <li>the location
     * is a directory</li> <li>the directory is empty</li> <li>and if the location does not exist, tries to create the
     * directory tree</li> </ul>
     */
    protected void ensureDBLocationForCreate(File dbLocation) throws ProcessorException {
        Guardian.assertNotNull("dbLocation", dbLocation);

        if (dbLocation.exists()) {
// TODO: nf: Who has out-commented this and why?            
//            if(dbLocation.isDirectory()) {
//                if(dbLocation.list().length != 0) {
//                    throw new ProcessorException("The given database directory is not empty. " + dbLocation);
//                }
//            }else{
//                throw new ProcessorException("The specified database directory name belongs to a file. " + dbLocation);
//            }
        } else {
            dbLocation.mkdirs();
        }
    }

    /**
     * Checks if the location passed in can be used to load an existing database. The method checks if <ul> <li>the
     * location is a directory</li> <li>the directory contains a context properties file</li> </ul>
     */
    protected void ensureDBLocationForLoad(File dbLocation) throws ProcessorException {
        Guardian.assertNotNull("dbLocation", dbLocation);

        if (!dbLocation.exists()) {
            throw new ProcessorException("The database location does not exist");
        }

        if (!dbLocation.isDirectory()) {
            throw new ProcessorException("The database location passed in is not a directory");
        }

        final File properties = new File(dbLocation, BinDatabaseConstants.CONTEXT_PROPERTIES_FILE);
        if (!properties.exists()) {
            throw new ProcessorException("The database seems to be corrupted,\nno context-properties file found!");
        }
    }
}
