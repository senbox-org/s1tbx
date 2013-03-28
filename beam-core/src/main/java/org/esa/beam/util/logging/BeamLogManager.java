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
package org.esa.beam.util.logging;

import org.esa.beam.util.Guardian;
import org.esa.beam.util.SystemUtils;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.logging.ConsoleHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;

/**
 * This class is the central manager class for logging. It exposes a set of convenience methods for the initializytion
 * and configuration of the logging framework.
 */
public class BeamLogManager {

    private static String _systemLoggerName = "beam";

    /**
     * Gets the name of the system logger. This method is used to determine the system logger used by all low-level
     * classes of the BEAM API. The logger name should always start with <code>"beam."</code>. High-level application
     * classes should always use loggers with the name <code>"beam.<i>app-name</i>"</code>.
     *
     * @return the name of the system logger, must not be <code>null</code>
     *
     * @see #setSystemLoggerName
     * @see #getSystemLogger
     */
    public static String getSystemLoggerName() {
        return _systemLoggerName;
    }

    /**
     * Sets the name of the system logger. This method should be used by BEAM applications to register the name for the
     * system logger which is used by all-low level classes of the BEAM API. The logger name should always start with
     * <code>"beam."</code>.
     *
     * @param systemLoggerName the name of the system logger, must not be <code>null</code>
     *
     * @see #getSystemLoggerName
     * @see #getSystemLogger
     */
    public static void setSystemLoggerName(String systemLoggerName) {
        Guardian.assertNotNull("systemLoggerName", systemLoggerName);

        _systemLoggerName = systemLoggerName;
    }

    /**
     * Gets the system logger determined by the name returned by the <code>getSystemLoggerName</code> method. Other
     * loggers are obtained using the standard Java 1.4 method <code>java.util.logging.Logger.getLogger(name)</code>.
     * BEAM logging works as follows: <ul> <li>Stand-alone processors set system logger name to "beam.processor"</li>
     * <li>Stand-alone processors and processors run from VISAT create log files always for logger "beam.processor"</li>
     * <li>VISAT sets system logger name to "beam.visat"</li> <li>VISAT creates log files always for logger
     * "beam.visat"</li> <li>pconvert sets system logger name to "beam.pconvert"</li> <li>pconvert creates log files
     * always for logger "beam.pconvert"</li> <li>All classes under org.esa.beam.framework.processor use logger
     * "beam.processor"</li> <li>All classes under org.esa.beam.processor.X use logger "beam.processor.X", where X
     * stands for smac, sst, ... <li>All classes under org.esa.beam.pconvert use logger "beam.pconvert"</li> <li>All
     * classes under org.esa.beam.visat.toolviews.X use logger "beam.visat.X", where X stands for pin, bitmask, barithm,
     * ...</li> <li>All classes under org.esa.beam.visat use logger "beam.visat"</li> <li>All other classes use current
     * system logger</li> </ul>
     *
     * @return the system logger
     *
     * @see #getSystemLoggerName
     * @see #setSystemLoggerName
     */
    public static Logger getSystemLogger() {
        return Logger.getLogger(getSystemLoggerName());
    }

    /**
     * Gets a log file pattern for the given filname prefix. The pattern returned includes an absolute path the BEAM's
     * system log directory <code><i>$BEAM_INSTALL_DIR$</i>/log</code> and a filename created from the given log
     * filename prefix. This pattern is used to create <code>java.util.logging.FileHandler</code> instances.
     *
     * @param logFilenamePrefix the log filename prefix
     *
     * @return a log file pattern for the given filname prefix
     *
     * @see #getLogFilePattern(File, String)
     */
    public static String getLogFilePattern(String logFilenamePrefix) {
        return getLogFilePattern(new File(SystemUtils.getApplicationHomeDir(), "log"), logFilenamePrefix);
    }

    /**
     * Gets a log file pattern for the specified output directory filname prefix. The pattern returned includes the
     * specified output directory and a filename created from the given log filename prefix. This pattern is used to
     * create <code>java.util.logging.FileHandler</code> instances.
     *
     * @param logFilenamePrefix the log filename prefix
     *
     * @return a log file pattern for the given filname prefix
     *
     * @see BeamLogManager#getLogFilePattern(String)
     */
    public static String getLogFilePattern(File outDir, String logFilenamePrefix) {
        Guardian.assertNotNull("outDir", outDir);
        Guardian.assertNotNull("logFilenamePrefix", logFilenamePrefix);

        return new File(outDir, logFilenamePrefix + "_%u.%g.log").getPath();
    }


    /**
     * Configures the system logger using the given formatter and installs an optional console handler.
     *
     * @param formatter   the formatter to be used
     * @param consoleEcho if true, a console handler will be installed
     */
    public static void configureSystemLogger(Formatter formatter,
                                             boolean consoleEcho) {
        Logger sysLogger = getSystemLogger();

        Handler handler;
        Handler[] sysHandlers = sysLogger.getHandlers();
        boolean hasNoConsoleHandler = true;
        boolean hasNoCacheHandler = true;

        // check if there is already a console handler
        // if so we do not need a second one
        for (Handler sysHandler : sysHandlers) {
            if (sysHandler instanceof ConsoleHandler) {
                hasNoConsoleHandler = false;
            } else if (sysHandler instanceof CacheHandler) {
                hasNoCacheHandler = false;
            }
        }
        if ((hasNoConsoleHandler) && (consoleEcho)) {
            handler = new ConsoleHandler();
            handler.setFormatter(formatter);
            handler.setLevel(Level.FINEST);
            sysLogger.addHandler(handler);
        }

        if (hasNoCacheHandler) {
            sysLogger.addHandler(new CacheHandler());
        }
    }

    /**
     * Retrieves the first registered CacheHandler in the list of handlers attached to the system logger.
     *
     * @return a CacheHandler
     */
    public static CacheHandler getRegisteredCacheHandler() {
        Logger sysLogger = getSystemLogger();
        Handler[] handlers = sysLogger.getHandlers();
        CacheHandler cacheHandler = null;
        for (Handler handler : handlers) {
            if (handler instanceof CacheHandler) {
                cacheHandler = (CacheHandler) handler;
                return cacheHandler;
            }
        }
        return null;
    }

    /**
     * Crates a logging formatter.
     *
     * @param appName       the application name
     * @param appVersion    the application version string
     * @param copyrightInfo the application copyright information
     *
     * @return a formatter
     */
    public static BeamFormatter createFormatter(String appName, String appVersion, String copyrightInfo) {
        final String logHeader = createLogHeader(appName, appVersion, copyrightInfo);
        return new BeamFormatter(logHeader);
    }

    /**
     * Removes all logging handlers from the root logger.
     * By default, all BEAM log messages are echoed to the console.
     * Call this method if you don't want this behaviour.
     */
    public static void removeRootLoggerHandlers() {
        Logger rootLogger = LogManager.getLogManager().getLogger("");
        Handler[] handlers = rootLogger.getHandlers();
        for (Handler handler : handlers) {
            rootLogger.removeHandler(handler);
        }
    }

    public static void ensureLogPathFromPatternExists(String logPattern) {
        File logDir = new File(logPattern).getParentFile();
        if (logDir != null && !logDir.exists()) {
            logDir.mkdirs();
        }
    }

    public static String createLogHeader(String appName, String appVersion, String copyrightInfo) {
        final StringWriter stringWriter = new StringWriter();
        final PrintWriter printWriter = new PrintWriter(stringWriter);
        printWriter.println("Logfile generated by " + appName + ", version " + appVersion);
        printWriter.println(copyrightInfo);
        printWriter.println();
        printWriter.println("\"" + appName + "\" comes with ABSOLUTELY NO WARRANTY.");
        printWriter.println("This is free software, and you are welcome to redistribute it under certain");
        printWriter.println("conditions. See the GNU Library General Public License for details.");
        printWriter.println();
        return stringWriter.toString();
    }
}
