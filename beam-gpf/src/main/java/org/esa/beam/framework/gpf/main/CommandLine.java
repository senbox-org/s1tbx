package org.esa.beam.framework.gpf.main;

import com.bc.ceres.binding.ConversionException;
import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.util.io.FileUtils;

import java.util.HashMap;
import java.util.Iterator;

/**
 * The common command-line for GPF.
 */
public class CommandLine {
    private String[] args;
    private String operatorName;
    private String graphFilepath;
    private String targetFilepath;
    private HashMap<String, String> parameterValues;
    private HashMap<String, String> sourceFilepaths;
    private String outputFormatName;


    public CommandLine(String[] args) {
        this.args = args.clone();
        sourceFilepaths = new HashMap<String, String>();
        try {
            parseArgs();
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }

    public String[] getArgs() {
        return args;
    }

    private void parseArgs() throws Exception, ConversionException {
        targetFilepath = null;
        outputFormatName = null;
        parameterValues = new HashMap<String, String>();


        int argCount = 0;
        for (int i = 0; i < args.length; i++) {
            String arg = args[i];
            if (arg.startsWith("-")) {
                if (arg.startsWith("-P")) {
                    String[] pair = parseNameValuePair(arg);
                    parameterValues.put(pair[0], pair[1]);
                } else if (arg.startsWith("-S")) {
                    String[] pair = parseNameValuePair(arg);
                    sourceFilepaths.put(pair[0], pair[1]);
                } else if (arg.equals("-f")) {
                    outputFormatName = parseOptionArgument(arg, i);
                    i++;
                } else {
                    throw error("Unknown option '" + arg + "'");
                }
            } else {
                if (argCount == 0) {
                    if (arg.endsWith(".xml") || arg.endsWith(".XML") || arg.contains("/") || arg.contains("\\")) {
                        graphFilepath = arg;
                    } else {
                        operatorName = arg;
                    }
                } else if (argCount == 1) {
                    targetFilepath = arg;
                } else {
                    int index = argCount - 2;
                    if (index == 0) {
                        sourceFilepaths.put(GPF.SOURCE_PRODUCT_FIELD_NAME, arg);
                    }
                    sourceFilepaths.put(GPF.SOURCE_PRODUCT_FIELD_NAME + (index + 1), arg);
                }
                argCount++;
            }
        }
        if (targetFilepath == null) {
            throw error("Missing output file (argument #2)");
        }
        if (outputFormatName == null) {
            String extension = FileUtils.getExtension(targetFilepath);
            outputFormatName = detectWriterFormat(extension);
            if (outputFormatName == null) {
                throw error("Output format unknown");
            }
        }
    }

    private String parseOptionArgument(String arg, int index) throws Exception {
        if (index < args.length - 1) {
            return args[index + 1];
        } else {
            throw error("Missing argument for option '" + arg + "'");
        }
    }

    private String[] parseNameValuePair(String arg) throws Exception {
        int pos = arg.indexOf('=');
        if (pos == -1) {
            throw error("missing '=' in '" + arg + "'");
        }
        String name = arg.substring(2, pos).trim();
        if (name.isEmpty()) {
            throw error("empty name in '" + arg + "'");
        }
        String value = arg.substring(pos + 1).trim();
        return new String[]{name, value};
    }

    private String detectWriterFormat(String extension) {
        ProductIOPlugInManager registry = ProductIOPlugInManager.getInstance();
        Iterator<ProductWriterPlugIn> ins = registry.getAllWriterPlugIns();
        while (ins.hasNext()) {
            ProductWriterPlugIn productWriterPlugIn = ins.next();
            String[] strings = productWriterPlugIn.getDefaultFileExtensions();
            for (String string : strings) {
                if (string.equalsIgnoreCase(extension)) {
                    return productWriterPlugIn.getFormatNames()[0];
                }
            }
        }
        return null;
    }

    private static Exception error(String m) {
        return new Exception(m);
    }

    public static void printUsage() {
        System.out.println("Usage:");
        System.out.println("  gpf <op-name>|<graph-file> [options] <target-file> <source-file-1> <source-file-2>...");
        System.out.println();
        System.out.println("Arguments:");
        System.out.println("  <op-name>            Name of an operator");
        System.out.println("  <graph-file>         Filepath to operator graph file (XML)");
        System.out.println("  <target-file>        Filepath to target file");
        System.out.println("  <source-file>        Filepath to source file(s)");
        System.out.println();
        System.out.println("Options:");
        System.out.println("  -f <format>          Output file format, e.g. GeoTIFF, HDF5, BEAM-DIMAP");
        System.out.println("  -P<name>=<value>     ");
        System.out.println("  -S<id>=<source-file> ");
        System.out.println("  -T<id>=<target-file> ");
    }


    public String getOperatorName() {
        return operatorName;
    }

    public String getSourceFilepath(int index) {
        return getSourceFilepath(GPF.SOURCE_PRODUCT_FIELD_NAME + (index + 1));
    }

    public String getTargetFilepath() {
        return targetFilepath;
    }


    public String getOutputFormatName() {
        return outputFormatName;
    }

    public String getSourceFilepath(String id) {
        return sourceFilepaths.get(id);
    }

    public String getGraphFilepath() {
        return graphFilepath;
    }

    public String getParameterValue(String name) {
        return parameterValues.get(name);
    }
}
