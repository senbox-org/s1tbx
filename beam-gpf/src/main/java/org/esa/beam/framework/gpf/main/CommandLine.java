package org.esa.beam.framework.gpf.main;

import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.util.io.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Iterator;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The common command-line for GPF.
 */
class CommandLine {
    private String[] args;
    private String operatorName;
    private String graphFilepath;
    private String targetFilepath;
    private TreeMap<String, String> parameterMap;
    private TreeMap<String, String> sourceFilepathMap;
    private String targetFormatName;
    private String parameterFilepath;
    private TreeMap<String, String> targetFilepathMap;
    private boolean helpRequested;

    public CommandLine(String[] args) throws Exception {
        this.args = args.clone();
        if (this.args.length == 0) {
            helpRequested = true;
            return;
        }

        sourceFilepathMap = new TreeMap<String, String>();
        targetFilepathMap = new TreeMap<String, String>();
        parameterMap = new TreeMap<String, String>();

        int argCount = 0;
        for (int i = 0; i < this.args.length; i++) {
            String arg = this.args[i];
            if (arg.startsWith("-")) {
                if (arg.startsWith("-P")) {
                    String[] pair = parseNameValuePair(arg);
                    parameterMap.put(pair[0], pair[1]);
                } else if (arg.startsWith("-S")) {
                    String[] pair = parseNameValuePair(arg);
                    sourceFilepathMap.put(pair[0], pair[1]);
                } else if (arg.startsWith("-T")) {
                    String[] pair = parseNameValuePair(arg);
                    targetFilepathMap.put(pair[0], pair[1]);
                } else if (arg.equals("-t")) {
                    targetFilepath = parseOptionArgument(arg, i);
                    i++;
                } else if (arg.equals("-f")) {
                    targetFormatName = parseOptionArgument(arg, i);
                    i++;
                } else if (arg.equals("-p")) {
                    parameterFilepath = parseOptionArgument(arg, i);
                    i++;
                } else if (arg.equals("-h")) {
                    helpRequested = true;
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
                } else {
                    int index = argCount - 1;
                    if (index == 0) {
                        sourceFilepathMap.put(GPF.SOURCE_PRODUCT_FIELD_NAME, arg);
                    }
                    sourceFilepathMap.put(GPF.SOURCE_PRODUCT_FIELD_NAME + (index + 1), arg);
                }
                argCount++;
            }
        }
        if (operatorName == null && graphFilepath == null && !helpRequested) {
            throw error("Either operator name or graph XML file must be given");
        }
        if (graphFilepath == null && targetFilepathMap.size() != 0) {
            throw error("Defined target products only valid for graph XML");
        }
        if (targetFilepath == null && targetFilepathMap.size() == 0) {
            targetFilepath = "target.dim";
        }
        if (targetFormatName == null && targetFilepath != null) {
            targetFormatName = detectWriterFormat(FileUtils.getExtension(targetFilepath));
            if (targetFormatName == null) {
                throw error("Output format unknown");
            }
        }
    }

    public String[] getArgs() {
        return args;
    }

    public String getOperatorName() {
        return operatorName;
    }

    public String getGraphFilepath() {
        return graphFilepath;
    }

    public String getTargetFilepath() {
        return targetFilepath;
    }

    public String getTargetFormatName() {
        return targetFormatName;
    }

    public String getParameterFilepath() {
        return parameterFilepath;
    }

    public SortedMap<String, String> getParameterMap() {
        return parameterMap;
    }

    public SortedMap<String, String> getSourceFilepathMap() {
        return sourceFilepathMap;
    }

    public SortedMap<String, String> getTargetFilepathMap() {
        return targetFilepathMap;
    }

    public boolean isHelpRequested() {
        return helpRequested;
    }

    public static String getUsageText() {
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(
                        CommandLine.class.getResourceAsStream("CommandLineUsage.txt")));
        StringBuilder sb = new StringBuilder(1024);
        try {
            try {
                while (true) {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        break;
                    }
                    sb.append(line).append('\n');
                }
            } finally {
                bufferedReader.close();
            }
        } catch (IOException e) {
            // ignore
        }
        return sb.toString();
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
            throw error("Missing '=' in '" + arg + "'");
        }
        String name = arg.substring(2, pos).trim();
        if (name.isEmpty()) {
            throw error("Empty identifier in '" + arg + "'");
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

}
