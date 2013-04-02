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

package org.esa.beam.framework.processor;

import java.io.File;

/**
 * Container class for command line parameters. This class is the result of the <code>CmdLineParser</code> after a
 * successful parser run.
 *
 * @see org.esa.beam.framework.processor.CmdLineParser
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public class CmdLineParams {

    private boolean _isInteractive;
    private boolean _isProgress;
    private boolean _isQuiet;
    private boolean _isVerbose;
    private boolean _isDebug;
    private File _configFile;
    private File _requestFile;

    /**
     * Constructs an empty object.
     */
    public CmdLineParams() {
    }

    /**
     * Constructs the object with given parameters.
     *
     * @param isInteractive <code>boolean</code> value whether interactive or not
     * @param isQuiet       <code>boolean</code> value whether quiet mode or not
     * @param isVerbose     <code>boolean</code> value whether verbose mode or not
     * @param config        location of the configuration file
     * @param request       location of the request file
     */
    public CmdLineParams(boolean isInteractive, boolean isQuiet, boolean isVerbose,
                         File config, File request) {
        _isInteractive = isInteractive;
        _isQuiet = isQuiet;
        _isVerbose = isVerbose;
        _configFile = config;
        _requestFile = request;
    }

    /**
     * Constructs the object with given parameters.
     *
     * @param isInteractive <code>boolean</code> value whether interactive or not
     * @param isQuiet       <code>boolean</code> value whether quiet mode or not
     * @param isVerbose     <code>boolean</code> value whether verbose mode or not
     * @param isDebugOn     <code>boolean</code> value whether debug mode on or not
     * @param config        location of the configuration file
     * @param request       location of the request file
     */
    public CmdLineParams(boolean isInteractive, boolean isQuiet, boolean isVerbose,
                         boolean isDebugOn, File config, File request) {
        this(isInteractive, isQuiet, isVerbose, config, request);
        _isDebug = isDebugOn;
    }

    /**
     * Returns whether the interactive flag is set or not.
     */
    public boolean isInteractive() {
        return _isInteractive;
    }

    /**
     * Sets the interactive flag to the value passed in.
     *
     * @param interactive the flag to be set
     */
    public void setInteractive(boolean interactive) {
        _isInteractive = interactive;
    }

    /**
     * Returns whether the progress flag is set or not.
     */
    public boolean isProgress() {
        return _isProgress && ! _isInteractive;
    }

    /**
     * Sets the progress flag to the value passed in.
     *
     * @param progress the flag to be set
     */
    public void setProgress(boolean progress) {
        _isProgress = progress;
    }

    /**
     * Returns whether the quiet flag is set or not.
     */
    public boolean isQuiet() {
        return _isQuiet;
    }

    /**
     * Sets the quiet flag to the value passed in.
     *
     * @param quiet the flag to be set
     */
    public void setQuiet(boolean quiet) {
        _isQuiet = quiet;
    }

    /**
     * Returns whether the verbose flag is set or not.
     */
    public boolean isVerbose() {
        return _isVerbose;
    }

    /**
     * Sets the verbose flag to the value passed in.
     *
     * @param verbose the flag to be set
     */
    public void setVerbose(boolean verbose) {
        _isVerbose = verbose;
    }

    /**
     * Returns whether the debug flag is on or not
     */
    public boolean isDebugOn() {
        return _isDebug;
    }

    /**
     * Sets the debug flag to the value passed in.
     *
     * @param debug the flag to be set
     */
    public void setDebugOn(boolean debug) {
        _isDebug = debug;
    }

    /**
     * Returns the config file, or null if none is set.
     */
    public File getConfigFile() {
        return _configFile;
    }

    /**
     * Sets the config file to the value passed in.
     *
     * @param config the config file location
     */
    public void setConfigFile(File config) {
        _configFile = config;
    }

    /**
     * Returns the request file, or null if none is set.
     */
    public File getRequestFile() {
        return _requestFile;
    }

    /**
     * Sets the request file to the value passed in.
     *
     * @param request the request file location
     */
    public void setRequestFile(File request) {
        _requestFile = request;
    }
}
