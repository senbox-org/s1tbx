package com.bc.ceres.standalone;

import org.apache.commons.cli.*;

import java.util.HashMap;

/**
 *
 * Encapsulates the command line arguments handling.
 *
 * @author Bettina
 * @since Ceres 0.13.2
 */
public class CliHandler {

    private Options options;
    private String[] args;
    private final CommandLineParser parser;

    public CliHandler(String[] args) {
        this.options = createOptions();
        this.args = args;
        this.parser = new PosixParser();
    }

    public void printUsage() {
        HelpFormatter formatter = new HelpFormatter();
        String usage = "java -classpath path com.bc.ceres.standalone.MetadataEngineMain [options] [arg1] [arg2] ...";
        formatter.printHelp(usage, options);
    }

    public HashMap<String, String> fetchTemplateFiles() throws ParseException {
        String optionName = "v";
        return parseKeyValueOption(optionName);
    }

    public HashMap<String, String> fetchSourceItemFiles() throws ParseException {
        String optionName = "S";
        return parseKeyValueOption(optionName);
    }

    public String fetchTargetItemFile() throws ParseException {
        return parse("t");
    }

    public String fetchGlobalMetadataFile() throws ParseException {
        return parse("m");
    }

    public String[] fetchArguments() throws ParseException {
        CommandLine commandLine = parser.parse(options, args);
        return commandLine.getArgs();
    }

    private HashMap<String, String> parseKeyValueOption(String optionName) throws ParseException {
        CommandLine commandLine = parser.parse(options, args);
        String[] optionValues = commandLine.getOptionValues(optionName);

        HashMap<String, String> templates = new HashMap<String, String>();
        for (String optionValue : optionValues) {
            String[] splits = optionValue.split("=");
            if (splits.length != 2) {
                throw new IllegalArgumentException("Pattern for values of the option -" + optionName + " is: key=value");
            }
            templates.put(splits[0], splits[1]);
        }
        return templates;
    }

    private String parse(String optionValue) throws ParseException {
        CommandLine commandLine = parser.parse(options, args);
        return commandLine.getOptionValue(optionValue);
    }

    Options createOptions() {
        Options options = new Options();

        OptionBuilder.hasArg();
        OptionBuilder.withArgName("template>=<filePath");
        OptionBuilder.withDescription("The absolute path of the velocity templates (*.vm). Could be several given by key-value-pairs.");
        OptionBuilder.isRequired();
        options.addOption(OptionBuilder.create("v"));

        OptionBuilder.hasArg();
        OptionBuilder.withArgName("filePath");
        OptionBuilder.withDescription("The absolute item path (e.g. a product), the metadata file will be places next to the item " +
                "with the name 'itemName-templateName.templateSuffix. Refer to as $targetPath in velocity templates.");
        OptionBuilder.isRequired();
        options.addOption(OptionBuilder.create("t"));

        OptionBuilder.hasArg();
        OptionBuilder.withArgName("source>=<filePath");
        OptionBuilder.withDescription("Optional. The absolute path and name of the source items. Could be several given by key-value-pairs. " +
                "In the velocity templates the key will give you the content of the associated metadata file. The reference $sourcePaths " +
                "holds a list of the input item paths.");
        options.addOption(OptionBuilder.create("S"));

        OptionBuilder.hasArg();
        OptionBuilder.withArgName("filePath");
        OptionBuilder.withDescription("Optional. The absolute path and name of a text file to be included. E.g. global metadata. " +
                "Refer to as $metadata in velocity templates.");
        options.addOption(OptionBuilder.create("m"));

        return options;
    }
}

