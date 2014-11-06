package com.bc.ceres.standalone;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.OptionBuilder;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.cli.PosixParser;

import java.util.HashMap;

/**
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
        String usage = "java -classpath path com.bc.ceres.standalone.MetadataEngineMain -t /path/targetItem.suff -v templateX=/path/metadata.txt.vm [-v templateY=/path/report.xml.vm] [optional options] [arg1] [arg2] ...";
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

    public HashMap<String, String> fetchGlobalMetadataFiles() throws ParseException {
        String optionName = "m";
        return parseKeyValueOption(optionName);
    }

    public String[] fetchArguments() throws ParseException {
        CommandLine commandLine = parser.parse(options, args);
        return commandLine.getArgs();
    }

    private HashMap<String, String> parseKeyValueOption(String optionName) throws ParseException {
        CommandLine commandLine = parser.parse(options, args);
        String[] optionValues = commandLine.getOptionValues(optionName);

        HashMap<String, String> keyValues = new HashMap<String, String>();
        if (optionValues != null) {
            for (String optionValue : optionValues) {
                String[] splits = optionValue.split("=");
                if (splits.length != 2) {
                    throw new IllegalArgumentException("Pattern for values of the option -" + optionName + " is: key=value");
                }
                keyValues.put(splits[0], splits[1]);
            }
        }
        return keyValues;
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
        OptionBuilder.withDescription("The absolute item path (e.g. a product), the metadata file will be placed next to the item. " +
                                              "It gets the name 'itemName-templateName.templateSuffix'. Refer to as $targetPath in velocity templates. If the targetPath is a " +
                                              "directory, the metadata file will get the name of the velocity template without the suffix *.vm");
        OptionBuilder.isRequired();
        options.addOption(OptionBuilder.create("t"));

        OptionBuilder.hasArg();
        OptionBuilder.withArgName("source>=<filePath");
        OptionBuilder.withDescription("Optional. The absolute path and name of the source items. Could be several given by key-value-pairs. " +
                                              "In the velocity templates the key will give you the content of the associated metadata file(s). The reference $sourcePaths " +
                                              "holds a map of the input item paths. The reference $sourceMetadata holds a map with all source-metadata, which can be " +
                                              "referenced by their key. " +
                                              "($sourceMetadata.get(\"source\").get(\"metadata_xml\").content");
        options.addOption(OptionBuilder.create("S"));

        OptionBuilder.hasArg();
        OptionBuilder.withArgName("myKey>=<filePath");
        OptionBuilder.withDescription("Optional. The absolute path and name of text file(s) (e.g. global metadata, LUTs) to be included as " +
                                              "ceres-metadata - Resource. Refer to as $myKey in velocity templates. ($myKey.content; $myKey.map.get(\"key\"), if it was " +
                                              "a *.properties file or $myKey.path)");
        options.addOption(OptionBuilder.create("m"));

        return options;
    }
}

