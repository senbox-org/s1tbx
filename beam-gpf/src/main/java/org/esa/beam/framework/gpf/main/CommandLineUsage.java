package org.esa.beam.framework.gpf.main;

import com.bc.ceres.binding.XmlConverter;
import com.bc.ceres.core.ServiceRegistry;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.PrettyPrintWriter;
import com.thoughtworks.xstream.io.xml.XppDomReader;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.gpf.ParameterXmlConverter;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.internal.OperatorClassDescriptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringWriter;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.*;

class CommandLineUsage {
    private static final String TOOL_NAME = "gpt";

    public static String getUsageText() {
        final String usagePattern = getUsagePattern();

        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        final ServiceRegistry<OperatorSpi> serviceRegistry = registry.getServiceRegistry();
        final Set<OperatorSpi> spiSet = serviceRegistry.getServices();
        final ArrayList<DocElement> docElementList = new ArrayList<DocElement>(spiSet.size());
        for (OperatorSpi operatorSpi : spiSet) {
            final String opAlias = operatorSpi.getOperatorAlias();
            final OperatorMetadata operatorMetadata = operatorSpi.getOperatorClass().getAnnotation(OperatorMetadata.class);
            final String descriptionLine;
            if (operatorMetadata != null && !operatorMetadata.description().isEmpty()) {
                descriptionLine = operatorMetadata.description();
            } else {
                descriptionLine = "No description available.";
            }
            docElementList.add(new DocElement("  " + opAlias, new String[]{descriptionLine}));
        }
        StringBuilder operatorDoc = new StringBuilder(1024);
        appendDocElementList(operatorDoc, docElementList);
        return MessageFormat.format(usagePattern, TOOL_NAME, operatorDoc.toString());
    }

    private static String getUsagePattern() {
        BufferedReader bufferedReader = new BufferedReader(
                new InputStreamReader(
                        CommandLineArgs.class.getResourceAsStream("CommandLineUsage.txt")));
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

    public static String getUsageText(String operatorName) {
        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            return MessageFormat.format("Unknown operator ''{0}''.", operatorName);
        }
        final OperatorClassDescriptor operatorClassDescriptor = new OperatorClassDescriptor(operatorSpi.getOperatorClass());
        StringBuilder usageText = new StringBuilder(1024);
        usageText.append("Usage:\n");
        usageText.append(MessageFormat.format("  {0} {1} [options] ", TOOL_NAME, operatorName));
        ArrayList<DocElement> sourceDocElementList = createSourceDocuElementList(operatorClassDescriptor);
        ArrayList<DocElement> paramDocElementList = createParamDocuElementList(operatorClassDescriptor);
        final SourceProducts productsDescriptor = operatorClassDescriptor.getSourceProducts();
        if (productsDescriptor != null) {
            appendSourceFiles(usageText, productsDescriptor);
        }
        usageText.append("\n");

        if (sourceDocElementList.size() > 0) {
            usageText.append("\nSource Options:\n");
            appendDocElementList(usageText, sourceDocElementList);
        }
        if (paramDocElementList.size() > 0) {
            usageText.append("\nParameter Options:\n");
            appendDocElementList(usageText, paramDocElementList);
        }
        if (operatorClassDescriptor.getParameters().size() > 0) {
            usageText.append("\nConfiguration XML:\n");
            appendXmlUsage(usageText, operatorClassDescriptor.getParameters());
        }

        return usageText.toString();
    }

    private static void appendSourceFiles(StringBuilder usageText, SourceProducts productsDescriptor) {
        if (productsDescriptor.count() < 0) {
            usageText.append("<source-file-1> <source-file-2> ...");
        } else if (productsDescriptor.count() == 1) {
            usageText.append("<source-file>");
        } else if (productsDescriptor.count() == 2) {
            usageText.append("<source-file-1> <source-file-2>");
        } else if (productsDescriptor.count() == 3) {
            usageText.append("<source-file-1> <source-file-2> <source-file-3>");
        } else if (productsDescriptor.count() > 3) {
            usageText.append(MessageFormat.format("<source-file-1> <source-file-2> ... <source-file-{0}>",
                                                  productsDescriptor.count()));
        }
    }

    private static ArrayList<DocElement> createParamDocuElementList(OperatorClassDescriptor operatorClassDescriptor) {
        ArrayList<DocElement> docElementList = new ArrayList<DocElement>(10);
        final Map<Field, Parameter> parameterMap = operatorClassDescriptor.getParameters();
        for (Field paramField : parameterMap.keySet()) {
            final Parameter parameter = parameterMap.get(paramField);
            String paramSyntax = MessageFormat.format("  -P{0}=<{1}>", getParameterName(paramField, parameter), getFieldTypeName(paramField));
            final ArrayList<String> descriptionLines = createParamDescriptionLines(paramField, parameter);
            docElementList.add(new DocElement(paramSyntax, descriptionLines.toArray(new String[descriptionLines.size()])));
        }
        return docElementList;
    }

    private static ArrayList<DocElement> createSourceDocuElementList(OperatorClassDescriptor operatorClassDescriptor) {
        ArrayList<DocElement> docElementList = new ArrayList<DocElement>(10);
        final Map<Field, SourceProduct> sourceProductMap = operatorClassDescriptor.getSourceProductMap();
        for (Field sourceIdField : sourceProductMap.keySet()) {
            final SourceProduct sourceProduct = sourceProductMap.get(sourceIdField);
            String sourceSyntax = MessageFormat.format("  -S{0}=<file>", getSourceProductId(sourceIdField, sourceProduct));
            final ArrayList<String> descriptionLines = createSourceDecriptionLines(sourceIdField, sourceProduct);
            docElementList.add(new DocElement(sourceSyntax, descriptionLines.toArray(new String[descriptionLines.size()])));
        }
        return docElementList;
    }

    private static ArrayList<String> createParamDescriptionLines(Field paramField, Parameter parameter) {
        final ArrayList<String> descriptionLines = new ArrayList<String>();
        if (!parameter.description().isEmpty()) {
            descriptionLines.add(parameter.description());
        } else {
            descriptionLines.add(MessageFormat.format("Sets parameter ''{0}'' to <{1}>.",
                                                      getParameterName(paramField, parameter),
                                                      getFieldTypeName(paramField)));
        }
        if (!parameter.interval().isEmpty()) {
            descriptionLines.add(MessageFormat.format("Valid interval is {0}.", parameter.interval()));
        }
        if (!parameter.pattern().isEmpty()) {
            descriptionLines.add(MessageFormat.format("Pattern for valid values is ''{0}''.", parameter.pattern()));
        }
        if (!parameter.format().isEmpty()) {
            descriptionLines.add(MessageFormat.format("Format for valid values is ''{0}''.", parameter.format()));
        }
        if (parameter.valueSet().length > 0) {
            descriptionLines.add(MessageFormat.format("Value must be one of {0}.", toString(parameter.valueSet())));
        }
        if (!parameter.defaultValue().isEmpty()) {
            descriptionLines.add(MessageFormat.format("Default value is ''{0}''.", parameter.defaultValue()));
        }
        if (parameter.notNull()) {
            descriptionLines.add("This is a mandatory parameter.");
        }
        if (parameter.notEmpty()) {
            descriptionLines.add("Value must not be empty.");
        }
        return descriptionLines;
    }

    private static ArrayList<String> createSourceDecriptionLines(Field sourceIdField, SourceProduct sourceProduct) {
        final ArrayList<String> descriptionLines = new ArrayList<String>();
        if (!sourceProduct.description().isEmpty()) {
            descriptionLines.add(sourceProduct.description());
        } else {
            descriptionLines.add(MessageFormat.format("Sets source ''{0}'' to <filepath>.", getSourceProductId(sourceIdField, sourceProduct)));
        }
        if (!sourceProduct.type().isEmpty()) {
            descriptionLines.add(MessageFormat.format("Valid product types must match ''{0}''.", sourceProduct.type()));
        }
        if (sourceProduct.optional()) {
            descriptionLines.add("This is an optional source.");
        } else {
            descriptionLines.add("This is a mandatory source.");
        }
        return descriptionLines;
    }

    private static void appendDocElementList(StringBuilder usageText, List<DocElement> docElementList) {
        int maxLength = 0;
        final int minSpaceCount = 4;
        for (DocElement docElement : docElementList) {
            maxLength = Math.max(maxLength, docElement.syntax.length());
        }
        for (DocElement docElement : docElementList) {
            usageText.append(docElement.syntax);
            if (docElement.descriptionLines.length > 0) {
                int spacesCount = minSpaceCount + maxLength - docElement.syntax.length();
                for (int i = 0; i < docElement.descriptionLines.length; i++) {
                    usageText.append(spaces(spacesCount));
                    usageText.append(docElement.descriptionLines[i]);
                    usageText.append('\n');
                    spacesCount = minSpaceCount + maxLength;
                }
            } else {
                usageText.append('\n');
            }
        }
    }

    private static void appendXmlUsage(StringBuilder usageText, Map<Field, Parameter> map) {
        Xpp3Dom parametersElem = new Xpp3Dom("parameters");
        for (Field paramField : map.keySet()) {
            final Parameter parameter = map.get(paramField);
            Xpp3Dom parameterElem = null;
            if (parameter.xmlConverter() != null) {
                final Class<? extends XmlConverter> aClass = parameter.xmlConverter();
                if (ParameterXmlConverter.class.isAssignableFrom(aClass)) {
                    try {
                        final ParameterXmlConverter xmlConverter = (ParameterXmlConverter) aClass.newInstance();
                        xmlConverter.insertDomTemplate(parametersElem);
                    } catch (Exception e) {
                        // ignore
                    }
                }
            }
            if (parameterElem == null) {
                parameterElem = new Xpp3Dom(paramField.getName());
                parameterElem.setValue(getFieldTypeName(paramField));
            }
            parametersElem.addChild(parameterElem);
        }
        final StringWriter writer = new StringWriter();
        new HierarchicalStreamCopier().copy(new XppDomReader(parametersElem), new PrettyPrintWriter(writer));
        final StringTokenizer st = new StringTokenizer(writer.toString().replace('\r', ' '), "\n");
        while (st.hasMoreElements()) {
            usageText.append("  ");
            usageText.append(st.nextToken());
            usageText.append('\n');
        }
    }

    private static String spaces(int n) {
        StringBuilder sb = new StringBuilder(n);
        for (int i = 0; i < n; i++) {
            sb.append(' ');
        }
        return sb.toString();
    }

    private static String toString(String[] strings) {
        StringBuilder sb = new StringBuilder(32);
        for (int i = 0; i < strings.length; i++) {
            if (i > 0) {
                sb.append(", ");
            }
            sb.append('\'');
            sb.append(strings[i]);
            sb.append('\'');
        }
        return sb.toString();
    }

    private static String getParameterName(Field paramField, Parameter parameter) {
        return parameter.alias().isEmpty() ? paramField.getName() : parameter.alias();
    }

    private static String getSourceProductId(Field sourceProductField, SourceProduct sourceProduct) {
        return sourceProduct.alias().isEmpty() ? sourceProductField.getName() : sourceProduct.alias();
    }

    private static String getFieldTypeName(Field paramField) {
        final String s = paramField.getType().getSimpleName();
        if (Character.isUpperCase(s.charAt(0))) {
            return Character.toLowerCase(s.charAt(0)) + s.substring(1);
        }
        return s;
    }


    private static class DocElement {
        String syntax;
        String[] descriptionLines;

        private DocElement(String syntax, String[] descriptionLines) {
            this.syntax = syntax;
            this.descriptionLines = descriptionLines;
        }
    }
}
