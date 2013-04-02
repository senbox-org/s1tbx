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

import org.esa.beam.util.Guardian;
import org.esa.beam.util.ObjectUtils;

import java.io.File;
import java.util.logging.Logger;

/**
 * Helper class to parse command lines with a given options set. The command lines that can be understood by this class
 * may consist of: <ul> <li> "-c" or "--config": a path to a configuration file <li> "-i" or "--interactive": set the
 * interactive flag, i.e. run the application with graphical user interface <li> "-q" or "--quiet": run in quiet mode,
 * i.e. no or sparse console logging <li> "-v" or "--verbose": a lot of console logging <li> "-d" or "--debug": run in
 * debug mode, i.e. logging a lot of debugging and state information <li> and as single command line argument without
 * option: the path to a request file </ul>
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public class CmdLineParser {

    private static final String _config = "-c";
    private static final String _configLong = "--config";
    private static final String _interactive = "-i";
    private static final String _interactiveLong = "--interactive";
    private static final String _progress = "-p";
    private static final String _progressLong = "--progress";
    private static final String _quiet = "-q";
    private static final String _quietLong = "--quiet";
    private static final String _verbose = "-v";
    private static final String _verboseLong = "--verbose";
    private static final String _debug = "-d";
    private static final String _debugLong = "--debug";

    private String[] _args;
    private boolean _isParsed;
    private boolean _isEmptyCmdLine;
    private CmdLineParams _params;
    private Logger _logger;

    /**
     * Constructs the object with default parameters
     */
    public CmdLineParser() {
        _args = null;
        _isParsed = false;
        _isEmptyCmdLine = true;
        _logger = Logger.getLogger(ProcessorConstants.PACKAGE_LOGGER_NAME);
    }

    /**
     * Constructs the object with given command line
     *
     * @throws ProcessorException when erros occur during parsing
     */
    public CmdLineParser(String[] args) throws ProcessorException {
        setArgs(args);
    }

    /**
     * Sets the command line arguments to be parsed.
     *
     * @throws ProcessorException when erros occur during parsing
     */
    public void setArgs(String[] args) throws ProcessorException {
        Guardian.assertNotNull("args", args);
        _args = args;
        if (args.length > 0) {
            parse();
            _isEmptyCmdLine = false;
        } else {
            _isEmptyCmdLine = true;
        }
    }

    /**
     * Returns the command line parameters parsed.
     *
     * @throws ProcessorException when the command line is not parsed yet
     */
    public CmdLineParams getCmdLineParams() throws ProcessorException {
        checkForParsed();
        return _params;
    }

    /**
     * Returns wheter the commandline is empty or has arguments
     */
    public boolean isEmptyCommandLine() {
        return _isEmptyCmdLine;
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Parses the command line set.
     */
    private void parse() throws ProcessorException {
        Guardian.assertNotNull("command line", _args);

        // reset state and allocate new parameter object
        // ---------------------------------------------
        _isParsed = false;
        _params = new CmdLineParams();

        int i;
        for (i = 0; i < _args.length; i++) {
            // check for config file argument
            // ------------------------------
            if (ObjectUtils.equalObjects(_args[i], _config)
                || ObjectUtils.equalObjects(_args[i], _configLong)) {
                if (i + 1 >= _args.length) {
                    _logger.severe("Incomplete command line argument for '" + _configLong + "'!");
                    throw new IllegalProcessorParamException("Illegal command line format.");
                }
                _params.setConfigFile(new File(_args[i + 1]));
                // must increment because we read TWO tokens
                i++;
                continue;
            }

            // check for interactive flag
            // --------------------------
            if (ObjectUtils.equalObjects(_args[i], _interactive)
                || ObjectUtils.equalObjects(_args[i], _interactiveLong)) {
                _params.setInteractive(true);
                continue;
            }

            // check for progress flag
            // --------------------------
            if (ObjectUtils.equalObjects(_args[i], _progress)
                || ObjectUtils.equalObjects(_args[i], _progressLong)) {
                _params.setProgress(true);
                continue;
            }

            // check for quiet flag
            // --------------------
            if (ObjectUtils.equalObjects(_args[i], _quiet)
                || ObjectUtils.equalObjects(_args[i], _quietLong)) {
                _params.setQuiet(true);
                continue;
            }

            // check for verbose flag
            // ----------------------
            if (ObjectUtils.equalObjects(_args[i], _verbose)
                || ObjectUtils.equalObjects(_args[i], _verboseLong)) {
                _params.setVerbose(true);
                continue;
            }

            // check for debug flag
            // --------------------
            if (ObjectUtils.equalObjects(_args[i], _debug)
                || ObjectUtils.equalObjects(_args[i], _debugLong)) {
                _params.setDebugOn(true);
                continue;
            }

            // the last one must be the request file
            // -------------------------------------
            _params.setRequestFile(new File(_args[i]));
        }
        _isParsed = true;
    }

    /**
     * Throws IllegalProcessorStateException when not parsed.
     */
    private void checkForParsed() throws IllegalProcessorStateException {
        if (_isEmptyCmdLine) {
            throw new IllegalProcessorStateException("Command line is empty.");
        }
        if (!_isParsed) {
            throw new IllegalProcessorStateException("Command line not parsed yet.");
        }
    }
}
