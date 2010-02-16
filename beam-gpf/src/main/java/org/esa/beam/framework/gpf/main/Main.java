package org.esa.beam.framework.gpf.main;

import org.esa.beam.util.logging.BeamLogManager;

/**
 * The entry point for the GPF command-line tool.
 * For usage, see {@link org/esa/beam/framework/gpf/main/CommandLineUsage.txt}
 * or use the option "-h".
 */
public class Main {

    public static void main(String[] args) throws Exception {
        try {
            BeamLogManager.removeRootLoggerHandlers(); 
            new CommandLineTool().run(args);
        } catch (Exception e) {
            System.err.println("\nError: " + e.getMessage());
        }
    }

}
