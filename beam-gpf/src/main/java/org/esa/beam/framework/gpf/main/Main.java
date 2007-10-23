package org.esa.beam.framework.gpf.main;

/**
 * The entry point for the GPF command-line tool.
 * For usage, see {@link org/esa/beam/framework/gpf/main/CommandLineUsage.txt}
 * or use the option "-h".
 */
public class Main {

    public static void main(String[] args) {
        try {
            new CommandLineTool().run(args);
        } catch (Exception e) {
            System.err.println("Error: " + e.getMessage());
            e.printStackTrace();
        }
    }

}
