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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import com.bc.ceres.core.PrintWriterProgressMonitor;
import com.jidesoft.utils.Lm;
import org.esa.beam.framework.processor.ui.ProcessorApp;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.logging.BeamFormatter;
import org.esa.beam.util.logging.BeamLogManager;

import java.util.Locale;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.io.File;

/**
 * Runs any processor passed in according to the command line parameters set. This class has a single static method
 * {@link #runProcessor} which implements the unified processing scenario performed for all scientific processors in the
 * BEAM processing framework.<p> Refer to the command line parser class {@link CmdLineParser} for a documentation of the
 * available comamnd line arguments.
 *
 * @deprecated since BEAM 4.11. Use the {@link org.esa.beam.framework.gpf Graph Processing Framework} instead.
 */
@Deprecated
public class ProcessorRunner {

    private CmdLineParser _parser;
    private Processor _processor;
    private RequestLoader _requestLoader;
    private String _helpID;
    private String _helpsetPath;

    public static void main(String[] args) {

        String processorClassName = System.getProperty("beam.processorClass");
        if (processorClassName == null) {
            throw new IllegalArgumentException("System property 'beam.processorClass' not set.");
        }
        try {
            ClassLoader classLoader = ProcessorRunner.class.getClassLoader();
            SystemUtils.init3rdPartyLibs(classLoader);
            Class<Processor> processorClass = (Class<Processor>) classLoader.loadClass(processorClassName);
            Processor processor = processorClass.newInstance();
            ProcessorRunner.runProcessor(args, processor);
        } catch (ClassNotFoundException e) {
            throw new IllegalArgumentException("Processor class not found.", e);
        } catch (InstantiationException e) {
            throw new IllegalArgumentException("Processor class could not be instantiated.", e);
        } catch (IllegalAccessException e) {
            throw new IllegalArgumentException("Processor no-args constuctor not accessible.", e);
        } catch (ClassCastException e) {
            throw new IllegalArgumentException(
                    String.format("Processor class must be of type %s.", Processor.class.getName()), e);
        }
    }

    /**
     * Implements the unified processing scenario performed for all scientific processors in the BEAM processing
     * framework. The method performs the following steps when run in interactive mode: <ul> <li>Initializes the
     * processor with a request, if one is present</li> <li>Creates the processor UI frame and attaches the processor to
     * it</li> <li>Attaches a progressbar to the processor and</li> <li>runs the processor UI frame</li> </ul> If the
     * processor runs in non-interactive mode it: <ul> <li>Loops over all requests in the request file and for each
     * request it</li> <li>initializes the processor with the current request</li> <li>logs the processor header
     * (optionally)</li> <li>logs the request and</li> <li>runs the processor on the current request</li> </ul>
     *
     * @param args      the commandline arguments retrieved by the <code>main</code> method
     * @param processor the processor
     *
     * @return <code>true</code> for success
     */
    public static boolean runProcessor(String[] args, Processor processor) {
        return runProcessor(args, processor, processor.getDefaultHelpSetPath(), processor.getDefaultHelpId());
    }

    private static boolean runProcessor(final String[] args,
                                       final Processor processor,
                                       final String helpsetPath,
                                       final String helpID) {
        Locale.setDefault(Locale.ENGLISH); // Force usage of english locale
        BeamLogManager.setSystemLoggerName("beam.processor");

        final ProcessorRunner runner = new ProcessorRunner();
        runner.setHelpID(helpID);
        runner.setHelpsetPath(helpsetPath);
        try {
            runner.run(args, processor);
        } catch (ProcessorException e) {
            BeamLogManager.getSystemLogger().log(Level.SEVERE, e.getMessage(), e);
            System.err.println(processor.getName() + " error: " + e.getMessage());
            return false;
        }

        return true;
    }

    private void setHelpsetPath(String helpsetPath) {
        _helpsetPath = helpsetPath;
    }

    private void setHelpID(String helpID) {
        _helpID = helpID;
    }


    /**
     * Initializes logging. Sets the system logger based on the package the processor originates. Creates an application
     * logger and initializes this one with the parameter supplied by the processor.
     *
     * @param processor
     */
    private static void initializeLogging(Processor processor) {
        BeamLogManager.setSystemLoggerName("beam.processor");
        final BeamFormatter formatter = BeamLogManager.createFormatter(processor.getName(), processor.getVersion(),
                                                                       processor.getCopyrightInformation());
        BeamLogManager.configureSystemLogger(formatter, true);
        BeamLogManager.getSystemLogger().setUseParentHandlers(false);
    }

    /**
     * Constructs the object with default parameters
     */
    private ProcessorRunner() {
        _parser = new CmdLineParser();
        _requestLoader = new RequestLoader();
    }

    /**
     * Runs the processor passed as argument.
     */
    private void run(String[] args, Processor processor)
            throws IllegalProcessorStateException,
                   ProcessorException,
                   RequestElementFactoryException {
        Guardian.assertNotNull("processor", processor);
        // set the processor
        _processor = processor;
        _parser.setArgs(args);
        if (_parser.isEmptyCommandLine()) {
            printUsage(processor);
            return;
        }
        run();
    }

    /**
     * Runs the processor.
     */
    private void run() throws ProcessorException {
        setRequestListFromCmdLine();
        CmdLineParams params = _parser.getCmdLineParams();

        if (params.isDebugOn()) {
            Debug.setEnabled(true);
        }
        setLogLevel(params);

        final int numRequests = _requestLoader.getNumRequests();
        if (params.isInteractive()) {
            Lm.verifyLicense("Brockmann Consult", "BEAM", "lCzfhklpZ9ryjomwWxfdupxIcuIoCxg2");
            // might be no request there in interactive mode
            if (numRequests > 0) {
                // Initialize processor with first request - interactive mode
                // can just handle one request at a time
                _processor.setRequest(_requestLoader.getRequestAt(0));
            }
            final ProcessorApp processorApp = new ProcessorApp(_processor, _requestLoader);
            processorApp.setStandAlone(true);
            try {
                processorApp.startUp(ProgressMonitor.NULL);
            } catch (Exception e) {
                throw new ProcessorException("Failed to start processor application.", e);
            }
            if (StringUtils.isNotNullAndNotEmpty(_helpsetPath)) {
                processorApp.addHelp(_processor.getClass().getClassLoader(), _helpsetPath);
            }
            processorApp.setHelpID(_helpID);
        } else { // non interactive mode

            initializeLogging(_processor);
            _processor.initProcessor();

            com.bc.ceres.core.ProgressMonitor progressMonitor;
            if (params.isProgress()) {
                progressMonitor = new PrintWriterProgressMonitor(System.out);
            } else {
                progressMonitor = ProgressMonitor.NULL;
            }
            try {
                progressMonitor.beginTask("Processing requests", numRequests);
                for (int i = 0; i < numRequests; i++) {
                    progressMonitor.setSubTaskName("Request " + (i + 1) + " of " + numRequests + "...");
                    _processor.processRequest(_requestLoader.getRequestAt(i),
                                              SubProgressMonitor.create(progressMonitor, 1));
                    if (_processor.isAborted()) {
                        break;
                    }
                }
            } finally {
                progressMonitor.done();
            }

            ProcessorUtils.removeLoggingHandler();
        }
    }

    ///////////////////////////////////////////////////////////////////////////
    /////// END OF PUBLIC
    ///////////////////////////////////////////////////////////////////////////


    /**
     * Assembles the request list from the command line arguments.
     */
    private void setRequestListFromCmdLine() throws IllegalProcessorStateException,
                                                    ProcessorException {
        File requestFile;

        // retrieve the command line arguments from the command line parser
        // ----------------------------------------------------------------
        CmdLineParams params = _parser.getCmdLineParams();

        // ask the processor about it's element factory and suply the loader with it
        // -------------------------------------------------------------------------
        _requestLoader.setElementFactory(_processor.getRequestElementFactory());

        // get the request file path from the command line arguments and check
        // -------------------------------------------------------------------
        requestFile = params.getRequestFile();
        if (requestFile == null) {
            if (params.isInteractive()) {
                // let the UI handle this
                return;
            } else {
                // error condition - we MUST have a request file in non interactive mode
                throw new IllegalProcessorStateException("no request file supplied");
            }
        }

        // parse the request file and store the requests in the request list
        // -----------------------------------------------------------------
        _requestLoader.setAndParseRequestFile(requestFile);
    }

    /**
     * Sets the logging level as stated in the command line. The logging levels match as follows: <ul> <li>quiet maps to
     * logging level WARNING</li> <li>verbose maps to logging level FINE</li> <li>debug maps to logging level
     * FINEST</li> </ul>
     *
     * @param params
     */
    private static void setLogLevel(CmdLineParams params) {
        Logger sysLogger = BeamLogManager.getSystemLogger();

        // first - default
        sysLogger.setLevel(Level.INFO);

        // now check commandline to eventually switch level
        if (params.isQuiet()) {
            sysLogger.setLevel(Level.WARNING);
        } else if (params.isVerbose()) {
            sysLogger.setLevel(Level.FINE);
        }

        if (params.isDebugOn()) {
            sysLogger.setLevel(Level.FINEST);
        }
    }

    /**
     * Prints a general help message when no commandline is supplied
     */
    public static void printUsage(Processor processor) {
        System.out.println(String.format("%s version %s", processor.getName(), processor.getVersion()));
        System.out.println("Usage: processor [options] [<processing-request>]");
        System.out.println();
        System.out.println("where <processing-request> an an XML file which is optional");
        System.out.println("only if the processor is run in interactive mode (-i).");
        System.out.println("Note that you can use the interactive mode to generate a default");
        System.out.println("request file for the processor. The available options are:");
        System.out.println();
        System.out.println("  -i or --interactive");
        System.out.println("     Runs processor in interactive mode.");
        System.out.println("  -p or --progress");
        System.out.println("     Activates the console progress monitor.");
        System.out.println("     Only available in non-interactive mode.");
        System.out.println("  -d or --debug");
        System.out.println("     Enables output of debug messages.");
        System.out.println();
    }
}
