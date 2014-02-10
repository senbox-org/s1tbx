package org.esa.pfa.db;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;

/**
 * @author Norman Fomferra
 */
public class CommonOptions {
    boolean printStackTrace;
    boolean verbose;


    public static void addOptions(Options options) {
        options.addOption(opt('e', "error-info", "Print detailed error information (Java stack traces)."));
        options.addOption(opt('h', "help", "Print usage help."));
    }

    public void configure(CommandLine commandLine) {
        printStackTrace = commandLine.hasOption("error-info");
        verbose = commandLine.hasOption("verbose");
    }

    public boolean isPrintStackTrace() {
        return printStackTrace;
    }

    public boolean isVerbose() {
        return verbose;
    }

    public static Option opt(char opt, String longOpt, int numArgs, String argName, String description) {
        //noinspection AccessStaticViaInstance
        return OptionBuilder
                .withLongOpt(longOpt)
                .hasArgs(numArgs)
                .withArgName(argName)
                .withDescription(description)
                .create(opt);
    }

    public static Option opt(char opt, String longOpt, String description) {
        //noinspection AccessStaticViaInstance
        return OptionBuilder
                .withLongOpt(longOpt)
                .withDescription(description)
                .create(opt);
    }


    public void printError(Exception e) {
        String message = e.getMessage();
        if (message == null) {
            message = e.getClass().getSimpleName();
        }
        System.err.println("error: " + message);
        if (printStackTrace) {
            e.printStackTrace(System.err);
        }
    }
}
