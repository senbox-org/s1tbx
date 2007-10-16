package org.esa.beam.framework.gpf.main;

import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.util.io.FileUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.*;

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

    public static String getUsageText(String operatorName) {
        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            return "Unknown operator \"" + operatorName + "\".";
        }
        StringBuilder usageText = new StringBuilder(1024);
        usageText.append("Usage: ");
        usageText.append("gpf ");
        usageText.append('"');
        usageText.append(operatorName);
        usageText.append('"');
        usageText.append(' ');
        ArrayList<DocuElement> paramDocuElementList = createParamDocuElementList(operatorSpi);
        if (paramDocuElementList.size() > 0) {
            usageText.append("{-P<name>=<value>} ");
        }
        ArrayList<DocuElement> sourceDocuElementList = createSourceDocuElementList(operatorSpi);
        if (sourceDocuElementList.size() > 0) {
            usageText.append("{-S<id>=<filepath>} ");
        }

        if (paramDocuElementList.size() > 0) {
            usageText.append("\n");
            collect(paramDocuElementList, usageText);
        }
        if (sourceDocuElementList.size() > 0) {
            usageText.append("\n");
            collect(sourceDocuElementList, usageText);
        }

        return usageText.toString();
    }

    private static ArrayList<DocuElement> createParamDocuElementList(OperatorSpi operatorSpi) {
        ArrayList<DocuElement> docuElementList = new ArrayList<DocuElement>(10);
        final Map<String, Parameter> parameterMap = operatorSpi.getParameterDescriptors();
        for (String paramName : parameterMap.keySet()) {
            final Parameter parameter = parameterMap.get(paramName);

            StringBuilder paramSyntax = new StringBuilder(32);
            paramSyntax.append("-P");
            paramSyntax.append(paramName);
            paramSyntax.append("=<value>"); // todo - need type info here!

            final ArrayList<String> docuLines = new ArrayList<String>();
            if (!parameter.description().isEmpty()) {
                StringBuilder paramDescr = new StringBuilder(32);
                paramDescr.append(parameter.description());
                paramDescr.append(".");
                docuLines.add(paramDescr.toString());
            }
            if (!parameter.interval().isEmpty()) {
                StringBuilder paramDescr = new StringBuilder(32);
                paramDescr.append("Valid interval is ");
                paramDescr.append(parameter.interval());
                paramDescr.append(".");
                docuLines.add(paramDescr.toString());
            }
            if (!parameter.pattern().isEmpty()) {
                StringBuilder paramDescr = new StringBuilder(32);
                paramDescr.append("Value pattern is ");
                paramDescr.append(parameter.pattern());
                paramDescr.append(".");
                docuLines.add(paramDescr.toString());
            }
            if (!parameter.format().isEmpty()) {
                StringBuilder paramDescr = new StringBuilder(32);
                paramDescr.append("Value format is ");
                paramDescr.append(parameter.format());
                paramDescr.append(".");
                docuLines.add(paramDescr.toString());
            }
            if (parameter.valueSet().length > 0) {
                StringBuilder paramDescr = new StringBuilder(32);
                final String[] strings = parameter.valueSet();
                paramDescr.append("Allowed values are ");
                for (int i = 0; i < strings.length; i++) {
                    if (i > 0) {
                        paramDescr.append(",");
                    }
                    paramDescr.append(strings[i]);
                }
                paramDescr.append(".");
                docuLines.add(paramDescr.toString());
            }
            if (!parameter.defaultValue().isEmpty()) {
                StringBuilder paramDescr = new StringBuilder(32);
                paramDescr.append("Default value is ");
                paramDescr.append(parameter.defaultValue());
                paramDescr.append(".");
                docuLines.add(paramDescr.toString());
            }
            docuElementList.add(new DocuElement(paramSyntax.toString(), docuLines.toArray(new String[docuLines.size()])));
        }
        return docuElementList;
    }

    private static ArrayList<DocuElement> createSourceDocuElementList(OperatorSpi operatorSpi) {
        ArrayList<DocuElement> docuElementList = new ArrayList<DocuElement>(10);
        final Map<String, SourceProduct> sourceProductMap = operatorSpi.getSourceProductDescriptors();
        for (String sourceId : sourceProductMap.keySet()) {
            final SourceProduct sourceProduct = sourceProductMap.get(sourceId);

            StringBuilder paramSyntax = new StringBuilder(32);
            paramSyntax.append("-S");
            paramSyntax.append(sourceId);
            paramSyntax.append("=<filepath>");

            final ArrayList<String> docuLines = new ArrayList<String>();
            if (!sourceProduct.description().isEmpty()) {
                StringBuilder paramDescr = new StringBuilder(32);
                paramDescr.append(sourceProduct.description());
                paramDescr.append(".");
                docuLines.add(paramDescr.toString());
            }
            if (sourceProduct.type().isEmpty()) {
                StringBuilder paramDescr = new StringBuilder(32);
                paramDescr.append("Valid product type pattern is \"");
                paramDescr.append(sourceProduct.type());
                paramDescr.append("\".");
                docuLines.add(paramDescr.toString());
            }
            if (sourceProduct.optional()) {
                docuLines.add("Mandatory source product.");
            } else {
                docuLines.add("Optional source product.");
            }
            docuElementList.add(new DocuElement(paramSyntax.toString(), docuLines.toArray(new String[docuLines.size()])));
        }
        return docuElementList;
    }

    private static void collect(List<DocuElement> docuElementList, StringBuilder usageText) {
        int maxLength = 0;
        for (DocuElement docuElement : docuElementList) {
            maxLength = Math.max(maxLength, docuElement.syntax.length());
        }
        for (DocuElement docuElement : docuElementList) {
            usageText.append(docuElement.syntax);
            if (docuElement.docuLines.length > 0) {
                int spacesCount = 2 + maxLength - docuElement.syntax.length();
                for (int i = 0; i < docuElement.docuLines.length; i++) {
                    usageText.append(spaces(spacesCount));
                    usageText.append(docuElement.docuLines[i]);
                    usageText.append('\n');
                    spacesCount = 2 + maxLength;
                }
            } else {
                usageText.append('\n');
            }
        }
    }

    private static class DocuElement {
        String syntax;
        String[] docuLines;

        private DocuElement(String syntax, String[] docuLines) {
            this.syntax = syntax;
            this.docuLines = docuLines;
        }
    }

    private static String spaces(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }
}
