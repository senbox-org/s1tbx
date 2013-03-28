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

import com.bc.jexp.ParseException;
import com.bc.jexp.Term;
import org.esa.beam.dataio.dimap.DimapProductConstants;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.param.Parameter;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.util.logging.BeamLogManager;
import org.esa.beam.util.logging.CacheHandler;

import java.io.File;
import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Formatter;
import java.util.logging.Handler;
import java.util.logging.Logger;

/**
 * This class contains an assortment of convenience methods that may be useful when developing scientific processors
 * within the BEAM framework.
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public class ProcessorUtils {

    private static Logger _logger = BeamLogManager.getSystemLogger();


    /**
     * Creates a <code>Term</code> from the expression string passed in and validates that the product
     * supplied can interpret the expression.
     *
     * @param expression the expression string
     * @param product    the product to interprete the expression
     *
     * @return a validated term
     *
     * @throws ProcessorException on any failure
     */
    public static Term createTerm(String expression, Product product) throws ProcessorException {
        Guardian.assertNotNull("expression", expression);
        Guardian.assertNotNull("product", product);
        try {
            // try to creat a term - checks the correct syntax of the expression string and returns it
            return product.parseExpression(expression);
        } catch (ParseException e) {
            throw new ProcessorException(
                    "Unable to parse expression '" + expression + "':\n" + e.getMessage(), e);
        }
    }

    /**
     * Creates a product writer based on the information stored in the product reference passed in
     *
     * @param prodRef the <code>ProductRef</code> containing the information for the output product
     */
    public static ProductWriter createProductWriter(ProductRef prodRef) throws ProcessorException {
        ProductWriter writer;

        String format = prodRef.getFileFormat();
        if ((format == null) || (format.length() < 1)) {
            format = DimapProductConstants.DIMAP_FORMAT_NAME;
            _logger.warning(ProcessorConstants.LOG_MSG_NO_OUTPUT_FORMAT);
            _logger.warning(ProcessorConstants.LOG_MSG_USING + format);
        }

        writer = ProductIO.getProductWriter(format);
        if (writer == null) {
            throw new ProcessorException(ProcessorConstants.LOG_MSG_FAIL_CREATE_WRITER + format);
        }

        return writer;
    }

    /**
     * Sets a default set of logging handlers to the systemlogger.
     *
     * @param defaultPrefix the default log file prefix
     * @param request       the processing request
     * @param appName       the application name
     * @param appVersion    the application version string
     * @param copyrightInfo the copyright information
     */
    public static void setProcessorLoggingHandler(String defaultPrefix,
                                                  Request request,
                                                  String appName,
                                                  String appVersion,
                                                  String copyrightInfo) {
        setProcessorLoggingHandler(defaultPrefix, request,
                                   BeamLogManager.createFormatter(appName, appVersion, copyrightInfo));
    }

    /**
     * Sets a default set of logging handlers to the systemlogger.
     *
     * @param defaultPrefix the default log file prefix
     * @param request       the processing request
     * @param formatter     a formatter to be used
     */
    public static void setProcessorLoggingHandler(String defaultPrefix,
                                                  Request request,
                                                  Formatter formatter) {
        Logger sysLogger = BeamLogManager.getSystemLogger();
        Handler handler;
        CacheHandler cacheHandler = null;
        String logPrefix;
        boolean logToOutput;

        // first - remove all file handler attached (there might be some from the last run)
        // and find the CacheHandler, if one is available
        // --------------------------------------------------------------------------------
        Handler[] handlers = sysLogger.getHandlers();
        for (Handler handler1 : handlers) {
            if (handler1 instanceof FileHandler) {
                handler1.flush();
                sysLogger.removeHandler(handler1);
            }
            if (handler1 instanceof CacheHandler) {
                cacheHandler = (CacheHandler) handler1;
            }
        }

        // try to retrieve the prefix from the processing request. If this does not
        // succeed use the default passed in.
        // ------------------------------------------------------------------------
        Parameter param = request.getParameter(ProcessorConstants.LOG_PREFIX_PARAM_NAME);
        if (param != null) {
            logPrefix = param.getValueAsText();
        } else {
            sysLogger.warning("Parameter '" + ProcessorConstants.LOG_PREFIX_PARAM_NAME + "' not set!");
            sysLogger.warning(ProcessorConstants.LOG_MSG_USING + defaultPrefix);
            logPrefix = defaultPrefix;
        }

        // attach a file handler to the system logger
        // ------------------------------------------
        String logPattern = BeamLogManager.getLogFilePattern(logPrefix);
        try {
            BeamLogManager.ensureLogPathFromPatternExists(logPattern);
            handler = new FileHandler(logPattern);
            handler.setFormatter(formatter);
            if (cacheHandler != null) {
                cacheHandler.transferRecords(handler);
            }
            sysLogger.addHandler(handler);
        } catch (SecurityException e) {
            printFileHandlerFailure(e.getMessage());
        } catch (IOException e) {
            printFileHandlerFailure(e.getMessage());
        }

        // check if logging to the output product directory is needed
        // ----------------------------------------------------------
        param = request.getParameter(ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME);
        if (param != null) {
            logToOutput = (Boolean) param.getValue();
        } else {
            sysLogger.warning("Parameter '" + ProcessorConstants.LOG_TO_OUTPUT_PARAM_NAME + "' not set!");
            sysLogger.warning(ProcessorConstants.LOG_MSG_USING + "false");
            logToOutput = false;
        }

        // if it is - get the output product and attach a file handler in this directory
        // -----------------------------------------------------------------------------
        if (logToOutput) {
            if (request.getNumOutputProducts() > 0) {
                ProductRef outRef = request.getOutputProductAt(0);

                if (outRef != null) {
                    String outPath = outRef.getFilePath();
                    File outFile = new File(outPath);
                    String outDir = outFile.getParent();
                    if (outDir != null) {
                        logPattern = BeamLogManager.getLogFilePattern(new File(outDir), logPrefix);
                        try {
                            handler = new FileHandler(logPattern);
                            handler.setFormatter(formatter);
                            if (cacheHandler != null) {
                                cacheHandler.transferRecords(handler);
                            }
                            sysLogger.addHandler(handler);
                        } catch (SecurityException e) {
                            printFileHandlerFailure(e.getMessage());
                        } catch (IOException e) {
                            printFileHandlerFailure(e.getMessage());
                        }
                    }
                }
            }
        }

        // finally remove the cache handler
        if (cacheHandler != null) {
            sysLogger.removeHandler(cacheHandler);
        }
    }

    /**
     * Flushes all logging handlers attached to the system logger and removes them. Attaches a new CacheHandler.
     */
    public static void removeLoggingHandler() {
        Logger sysLogger = BeamLogManager.getSystemLogger();
        Handler[] handlers;
        Handler cacheHandler = null;

        handlers = sysLogger.getHandlers();
        for (Handler handler : handlers) {
            if (handler instanceof FileHandler) {
                handler.flush();
                sysLogger.removeHandler(handler);
            }
            if (handler instanceof CacheHandler) {
                cacheHandler = handler;
            }
        }

        if (cacheHandler != null) {
            cacheHandler.flush();
        } else {
            sysLogger.addHandler(new CacheHandler());
        }
    }

    /**
     * Creates a valid <code>ProductRef</code> from the given file path and file format string
     *
     * @param filePath
     * @param fileFormat
     *
     * @return <code>null</code> if no filePath is given or filePath is empty
     */
    public static ProductRef createProductRef(String filePath, String fileFormat) {
        if (filePath == null || filePath.trim().length() == 0) {
            return null;
        }

        if (fileFormat == null) {
            fileFormat = DimapProductConstants.DIMAP_FORMAT_NAME;
        }

        String[] extensions = ProductIO.getProductWriterExtensions(fileFormat);
        final String ensuredFilePath = FileUtils.ensureExtension(filePath, extensions[0]);

        return new ProductRef(new File(ensuredFilePath), fileFormat, null);
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////

    /**
     * Prints a file handler construction failure to the console.
     *
     * @param message
     */
    private static void printFileHandlerFailure(String message) {
        System.err.println("Failed to create log file:");
        System.err.println(message);
    }

}
