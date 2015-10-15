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

package org.esa.snap.core.gpf.main;

import com.bc.ceres.binding.ConverterRegistry;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.descriptor.DefaultParameterDescriptor;
import org.esa.snap.core.gpf.descriptor.ElementDescriptor;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.ParameterDescriptor;
import org.esa.snap.core.gpf.descriptor.SourceProductDescriptor;
import org.esa.snap.core.gpf.descriptor.SourceProductsDescriptor;
import org.esa.snap.core.gpf.descriptor.TargetPropertyDescriptor;
import org.esa.snap.core.gpf.graph.Graph;
import org.esa.snap.core.gpf.graph.GraphException;
import org.esa.snap.core.gpf.graph.Header;
import org.esa.snap.core.gpf.graph.HeaderParameter;
import org.esa.snap.core.gpf.graph.HeaderSource;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

class CommandLineUsage {

    private static final String COMMAND_LINE_USAGE_RESOURCE = "CommandLineUsage.txt";

    public static String getUsageText() {
        String usagePattern = getUsagePattern();

        OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        ArrayList<OperatorSpi> spiList = new ArrayList<>(registry.getOperatorSpis());
        ArrayList<DocElement> docElementList = new ArrayList<>(spiList.size());
        for (OperatorSpi operatorSpi : spiList) {
            String opName = getOperatorUIName(operatorSpi);
            if (!operatorSpi.getOperatorDescriptor().isInternal()) {
                final String descriptionLine;
                String description = operatorSpi.getOperatorDescriptor().getDescription();
                if (description != null) {
                    descriptionLine = description;
                } else {
                    descriptionLine = "No description available.";
                }
                docElementList.add(new DocElement("  " + opName, new String[]{descriptionLine}));
            }
        }
        StringBuilder opListText = new StringBuilder(1024);
        sortAlphabetically(docElementList);
        appendDocElementList(opListText, docElementList);

        return MessageFormat.format(usagePattern,
                                    CommandLineTool.TOOL_NAME,
                                    CommandLineArgs.DEFAULT_TARGET_FILEPATH,
                                    CommandLineArgs.DEFAULT_FORMAT_NAME,
                                    CommandLineArgs.DEFAULT_TILE_CACHE_SIZE_IN_M,
                                    CommandLineArgs.DEFAULT_TILE_SCHEDULER_PARALLELISM,
                                    opListText.toString());
    }

    private static String getOperatorUIName(OperatorSpi operatorSpi) {
        OperatorDescriptor operatorDescriptor = operatorSpi.getOperatorDescriptor();
        return operatorDescriptor.getAlias() != null ? operatorDescriptor.getAlias() : operatorDescriptor.getName();
    }

    private static String getUsagePattern() {
        StringBuilder sb = new StringBuilder(1024);
        try {
            try (BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(CommandLineArgs.class.getResourceAsStream(COMMAND_LINE_USAGE_RESOURCE)))) {
                while (true) {
                    String line = bufferedReader.readLine();
                    if (line == null) {
                        break;
                    }
                    sb.append(line).append('\n');
                }
            }
        } catch (IOException ignored) {
            // ignore
        }

        return sb.toString();
    }

    public static String getUsageTextForGraph(String path, CommandLineContext commandLineContext) {
        final Graph graph;
        try {
            graph = commandLineContext.readGraph(path, new HashMap<>());
        } catch (GraphException e) {
            return e.getMessage();
        } catch (IOException e) {
            return e.getMessage();
        }

        final StringBuilder usageText = new StringBuilder(1024);
        final Header header = graph.getHeader();

        if (header != null) {
            usageText.append("Usage:\n");
            usageText.append(MessageFormat.format("  {0} {1} [options] ", CommandLineTool.TOOL_NAME, path));
            ArrayList<DocElement> sourceDocElementList = createSourceDocElementList(header.getSources());
            ArrayList<DocElement> paramDocElementList = createParamDocElementList(header.getParameters());

            if (!sourceDocElementList.isEmpty()) {
                usageText.append("\nSource Options:\n");
                appendDocElementList(usageText, sourceDocElementList);
            }
            if (!paramDocElementList.isEmpty()) {
                usageText.append("\nParameter Options:\n");
                appendDocElementList(usageText, paramDocElementList);
            }
        }

        return usageText.toString();
    }

    private static ArrayList<DocElement> createSourceDocElementList(List<HeaderSource> sources) {
        ArrayList<DocElement> docElementList = new ArrayList<>(10);
        for (HeaderSource headerSource : sources) {
            String sourceSyntax = MessageFormat.format("  -S{0}=<file>", headerSource.getName());
            final ArrayList<String> descriptionLines = createSourceDescriptionLines(headerSource);
            docElementList.add(new DocElement(sourceSyntax, descriptionLines.toArray(new String[descriptionLines.size()])));
        }
        sortAlphabetically(docElementList);
        return docElementList;
    }

    private static ArrayList<DocElement> createParamDocElementList(List<HeaderParameter> parameterList) {
        ArrayList<DocElement> docElementList = new ArrayList<>(10);
        for (HeaderParameter parameter : parameterList) {
            String paramSyntax = MessageFormat.format("  -P{0}=<{1}>", parameter.getName(), parameter.getType());
            final ArrayList<String> descriptionLines = createParamDescriptionLines(parameter);
            docElementList.add(new DocElement(paramSyntax, descriptionLines.toArray(new String[descriptionLines.size()])));
        }
        sortAlphabetically(docElementList);
        return docElementList;
    }

    private static ArrayList<String> createParamDescriptionLines(HeaderParameter parameter) {
        final ArrayList<String> descriptionLines = new ArrayList<>();
        final String description = parameter.getDescription();

        if (!(description == null || description.isEmpty())) {
            descriptionLines.add(description);
        } else {
            descriptionLines.add(MessageFormat.format("Sets parameter ''{0}'' to <{1}>.",
                                                      parameter.getName(),
                                                      parameter.getType()));
        }
        final String interval = parameter.getInterval();
        if (!(interval == null || interval.isEmpty())) {
            descriptionLines.add(MessageFormat.format("Valid interval is {0}.", interval));
        }
        final String pattern = parameter.getPattern();
        if (!(pattern == null || pattern.isEmpty())) {
            descriptionLines.add(MessageFormat.format("Pattern for valid values is ''{0}''.", pattern));
        }
        final String format = parameter.getFormat();
        if (!(format == null || format.isEmpty())) {
            descriptionLines.add(MessageFormat.format("Format for valid values is ''{0}''.", format));
        }
        final String[] valueSet = parameter.getValueSet();
        if (!(valueSet == null || valueSet.length == 0)) {
            descriptionLines.add(MessageFormat.format("Value must be one of {0}.", toString(valueSet)));
        }
        final String defaultValue = parameter.getDefaultValue();
        if (!(defaultValue == null || defaultValue.isEmpty())) {
            descriptionLines.add(MessageFormat.format("Default value is ''{0}''.", defaultValue));
        }
        final String unit = parameter.getUnit();
        if (!(unit == null || unit.isEmpty())) {
            descriptionLines.add(MessageFormat.format("Parameter Unit is ''{0}''.", unit));
        }
        if (parameter.isNotNull()) {
            descriptionLines.add("This is a mandatory parameter.");
        }
        if (parameter.isNotEmpty()) {
            descriptionLines.add("Value must not be empty.");
        }

        return descriptionLines;
    }

    private static ArrayList<String> createSourceDescriptionLines(HeaderSource headerSource) {
        final ArrayList<String> descriptionLines = new ArrayList<>();

        final String description = headerSource.getDescription();
        if (!(description == null || description.isEmpty())) {
            descriptionLines.add(description);
        } else {
            descriptionLines.add(MessageFormat.format("Sets source ''{0}'' to <filepath>.", headerSource.getName()));
        }
        if (headerSource.isOptional()) {
            descriptionLines.add("This is an optional source.");
        } else {
            descriptionLines.add("This is a mandatory source.");
        }
        return descriptionLines;
    }

    ////////////////////////////////////////////////////////////////////

    public static String getUsageTextForOperator(String operatorName) {
        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            return MessageFormat.format("Unknown operator ''{0}''.", operatorName);
        }
        OperatorDescriptor operatorDescriptor = operatorSpi.getOperatorDescriptor();
        StringBuilder usageText = new StringBuilder(1024);
        usageText.append("Usage:\n");
        usageText.append(MessageFormat.format("  {0} {1} [options] ", CommandLineTool.TOOL_NAME, operatorName));
        ArrayList<DocElement> sourceDocElementList = createSourceDocElementList(operatorDescriptor);
        ArrayList<DocElement> paramDocElementList = createParamDocElementList(operatorDescriptor);
        ArrayList<DocElement> propertyDocElementList = createPropertyDocElementList(operatorDescriptor);
        final SourceProductsDescriptor productsDescriptor = operatorDescriptor.getSourceProductsDescriptor();
        if (productsDescriptor != null) {
            appendSourceFiles(usageText, productsDescriptor);
        }
        usageText.append("\n");

        if (operatorDescriptor.getDescription() != null) {
            usageText.append("\nDescription:\n");
            final String description = operatorDescriptor.getDescription();
            final String[] lines = description.split("\n");
            for (String line : lines) {
                usageText.append("  ");
                usageText.append(line);
                usageText.append("\n");
            }
        }
        if (!propertyDocElementList.isEmpty()) {
            usageText.append("\nComputed Properties:\n");
            appendDocElementList(usageText, propertyDocElementList);
        }

        usageText.append("\n");
        if (!sourceDocElementList.isEmpty()) {
            usageText.append("\nSource Options:\n");
            appendDocElementList(usageText, sourceDocElementList);
        }
        if (!paramDocElementList.isEmpty()) {
            usageText.append("\nParameter Options:\n");
            appendDocElementList(usageText, paramDocElementList);
        }

        usageText.append("\nGraph XML Format:\n");
        appendXmlUsage(usageText, operatorDescriptor);

        return usageText.toString();
    }

    private static void appendSourceFiles(StringBuilder usageText, SourceProductsDescriptor productsDescriptor) {
        if (productsDescriptor.getCount() < 0) {
            usageText.append("<source-file-1> <source-file-2> ...");
        } else if (productsDescriptor.getCount() == 1) {
            usageText.append("<source-file>");
        } else if (productsDescriptor.getCount() == 2) {
            usageText.append("<source-file-1> <source-file-2>");
        } else if (productsDescriptor.getCount() == 3) {
            usageText.append("<source-file-1> <source-file-2> <source-file-3>");
        } else if (productsDescriptor.getCount() > 3) {
            usageText.append(MessageFormat.format("<source-file-1> <source-file-2> ... <source-file-{0}>",
                                                  productsDescriptor.getCount()));
        }
    }

    private static ArrayList<DocElement> createParamDocElementList(OperatorDescriptor operatorDescriptor) {
        ArrayList<DocElement> docElementList = new ArrayList<>(10);
        ParameterDescriptor[] parameterDescriptors = operatorDescriptor.getParameterDescriptors();
        for (ParameterDescriptor parameter : parameterDescriptors) {
            if (isConverterAvailable(parameter) && !parameter.isDeprecated()) {
                String paramSyntax = String.format("  -P%s=<%s>", getName(parameter), getTypeName(parameter.getDataType()));
                final ArrayList<String> descriptionLines = createParamDescriptionLines(parameter);
                docElementList.add(new DocElement(paramSyntax, descriptionLines.toArray(new String[descriptionLines.size()])));
            }
        }
        sortAlphabetically(docElementList);
        return docElementList;
    }

    private static ArrayList<DocElement> createPropertyDocElementList(OperatorDescriptor operatorDescriptor) {
        ArrayList<DocElement> docElementList = new ArrayList<>(10);
        TargetPropertyDescriptor[] targetPropertyDescriptors = operatorDescriptor.getTargetPropertyDescriptors();
        // The sorting of the properties needs to be treated special, compared to the other DocElements.
        // It would be sorted by the Type name instead of the name of the property
        List<TargetPropertyDescriptor> targetPropertyDescriptorList = Arrays.asList(targetPropertyDescriptors);
        Collections.sort(targetPropertyDescriptorList, (tpd1, tpd2) -> getName(tpd1).compareToIgnoreCase(getName(tpd2))
        );
        for (TargetPropertyDescriptor property : targetPropertyDescriptors) {
            String propertySyntax = MessageFormat.format("{0} {1}", property.getDataType().getSimpleName(), getName(property));
            final ArrayList<String> descriptionLines = createTargetPropertyDescriptionLines(property);
            docElementList.add(new DocElement(propertySyntax, descriptionLines.toArray(new String[descriptionLines.size()])));
        }
        return docElementList;
    }

    private static ArrayList<DocElement> createSourceDocElementList(OperatorDescriptor operatorDescriptor) {
        ArrayList<DocElement> docElementList = new ArrayList<>(10);
        SourceProductDescriptor[] sourceProductDescriptors = operatorDescriptor.getSourceProductDescriptors();
        for (SourceProductDescriptor sourceProduct : sourceProductDescriptors) {
            String sourceSyntax = MessageFormat.format("  -S{0}=<file>", getName(sourceProduct));
            final ArrayList<String> descriptionLines = createSourceDescriptionLines(sourceProduct);
            docElementList.add(new DocElement(sourceSyntax, descriptionLines.toArray(new String[descriptionLines.size()])));
        }
        sortAlphabetically(docElementList);
        return docElementList;
    }

    private static ArrayList<String> createParamDescriptionLines(ParameterDescriptor parameter) {
        final ArrayList<String> descriptionLines = new ArrayList<>();
        if (parameter.getDescription() != null) {
            descriptionLines.add(parameter.getDescription());
        } else {
            descriptionLines.add(MessageFormat.format("Sets parameter ''{0}'' to <{1}>.",
                                                      getName(parameter),
                                                      getTypeName(parameter.getDataType())));
        }
        if (parameter.getInterval() != null) {
            descriptionLines.add(MessageFormat.format("Valid interval is {0}.", parameter.getInterval()));
        }
        if (parameter.getPattern() != null) {
            descriptionLines.add(MessageFormat.format("Pattern for valid values is ''{0}''.", parameter.getPattern()));
        }
        if (parameter.getFormat() != null) {
            descriptionLines.add(MessageFormat.format("Format for valid values is ''{0}''.", parameter.getFormat()));
        }
        if (parameter.getValueSet().length > 0) {
            descriptionLines.add(MessageFormat.format("Value must be one of {0}.", toString(parameter.getValueSet())));
        }
        if (parameter.getDefaultValue() != null) {
            descriptionLines.add(MessageFormat.format("Default value is ''{0}''.", parameter.getDefaultValue()));
        }
        if (parameter.getUnit() != null) {
            descriptionLines.add(MessageFormat.format("Parameter unit is ''{0}''.", parameter.getUnit()));
        }
        if (parameter.isNotNull()) {
            descriptionLines.add("This is a mandatory parameter.");
        }
        if (parameter.isNotEmpty()) {
            descriptionLines.add("Value must not be empty.");
        }
        return descriptionLines;
    }

    private static ArrayList<String> createSourceDescriptionLines(SourceProductDescriptor sourceProduct) {
        final ArrayList<String> descriptionLines = new ArrayList<>();
        if (sourceProduct.getDescription() != null) {
            descriptionLines.add(sourceProduct.getDescription());
        } else {
            descriptionLines.add(MessageFormat.format("Sets source ''{0}'' to <filepath>.", getName(sourceProduct)));
        }
        if (sourceProduct.getProductType() != null) {
            descriptionLines.add(MessageFormat.format("Valid product types must match ''{0}''.", sourceProduct.getProductType()));
        }
        if (sourceProduct.isOptional()) {
            descriptionLines.add("This is an optional source.");
        } else {
            descriptionLines.add("This is a mandatory source.");
        }
        return descriptionLines;
    }

    private static ArrayList<String> createTargetPropertyDescriptionLines(TargetPropertyDescriptor property) {
        final ArrayList<String> descriptionLines = new ArrayList<>();
        if (property.getDescription() != null) {
            descriptionLines.add(property.getDescription());
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
                    final String description = docElement.descriptionLines[i];
                    final String[] lines = description.split("\n");
                    appendLine(usageText, spaces(spacesCount), lines[0]);
                    for (int j = 1, linesLength = lines.length; j < linesLength; j++) {
                        appendLine(usageText, spaces(minSpaceCount + maxLength), lines[j]);
                    }
                    spacesCount = minSpaceCount + maxLength;
                }
            } else {
                usageText.append('\n');
            }
        }
    }

    private static void appendLine(StringBuilder builder, String spaces, String descriptionLine) {
        builder.append(spaces);
        builder.append(descriptionLine);
        builder.append('\n');
    }

    private static void sortAlphabetically(List<DocElement> docElementList) {
        Collections.sort(docElementList, (element1, element2) -> element1.syntax.compareToIgnoreCase(element2.syntax));
    }

    private static void appendXmlUsage(StringBuilder usageText, OperatorDescriptor operatorDescriptor) {

        final DomElement graphElem = new XppDomElement("graph");
        graphElem.setAttribute("id", "someGraphId");
        final DomElement versionElem = graphElem.createChild("version");
        versionElem.setValue("1.0");
        final DomElement nodeElem = graphElem.createChild("node");
        nodeElem.setAttribute("id", "someNodeId");
        final DomElement operatorElem = nodeElem.createChild("operator");
        operatorElem.setValue(operatorDescriptor.getAlias());
        DomElement sourcesElem = nodeElem.createChild("sources");
        for (SourceProductDescriptor sourceProduct : operatorDescriptor.getSourceProductDescriptors()) {
            convertSourceProductFieldToDom(sourceProduct, sourcesElem);
        }
        if (operatorDescriptor.getSourceProductsDescriptor() != null) {
            String name = getName(operatorDescriptor.getSourceProductsDescriptor());
            final DomElement child = sourcesElem.createChild(name);
            child.setValue(String.format("${%s}", name));
        }
        DomElement parametersElem = nodeElem.createChild("parameters");
        for (ParameterDescriptor parameter : operatorDescriptor.getParameterDescriptors()) {
            if (!parameter.isDeprecated()) {
                convertParameterFieldToDom(parameter, parametersElem);
            }
        }

        final StringTokenizer st = new StringTokenizer(graphElem.toXml().replace('\r', ' '), "\n");
        while (st.hasMoreElements()) {
            appendLine(usageText, "  ", st.nextToken());
        }
    }

    static void convertSourceProductFieldToDom(SourceProductDescriptor sourceProduct, DomElement sourcesElem) {
        String name = getName(sourceProduct);
        final DomElement child = sourcesElem.createChild(name);
        child.setValue(String.format("${%s}", name));
    }

    static void convertParameterFieldToDom(ParameterDescriptor parameter, DomElement parametersElem) {
        String name = getName(parameter);
        DomElement childElem = parametersElem.createChild(name);
        if (parameter.getDataType().isArray() && parameter.getItemAlias() != null) {
            String itemName = parameter.getItemAlias();
            DomElement element = childElem.createChild(itemName);
            if (!parameter.isStructure()) {
                ParameterDescriptor[] members = DefaultParameterDescriptor.getDataMemberDescriptors(parameter.getDataType().getComponentType());
                for (ParameterDescriptor member : members) {
                    convertParameterFieldToDom(member, element);
                }
            } else {
                element.setValue(getTypeName(parameter.getDataType().getComponentType()));
            }
            childElem.createChild("...");
        } else {
            if (isConverterAvailable(parameter)) {
                childElem.setValue(getTypeName(parameter.getDataType()));
            } else if (parameter.isStructure()) {
                ParameterDescriptor[] members = parameter.getStructureMemberDescriptors();
                for (ParameterDescriptor member : members) {
                    convertParameterFieldToDom(member, childElem);
                }
            } else {
                childElem.setValue(getTypeName(parameter.getDataType()));
            }
        }
    }

    private static boolean isConverterAvailable(ParameterDescriptor parameter) {
        return parameter.getConverterClass() != null
               || ConverterRegistry.getInstance().getConverter(parameter.getDataType()) != null;
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

    private static String getName(ElementDescriptor descriptor) {
        return descriptor.getAlias() != null && !descriptor.getAlias().isEmpty() ? descriptor.getAlias() : descriptor.getName();
    }

    private static String getTypeName(Class type) {
        if (type.isArray()) {
            final String typeName = getTypeName(type.getComponentType());
            return typeName + "," + typeName + "," + typeName + ",...";
        } else {
            final String typeName = type.getSimpleName();
            if (Character.isUpperCase(typeName.charAt(0))) {
                return Character.toLowerCase(typeName.charAt(0)) + typeName.substring(1);
            }
            return typeName;
        }
    }


    private static class DocElement {

        private String syntax;
        private String[] descriptionLines;

        private DocElement(String syntax, String[] descriptionLines) {
            this.syntax = syntax;
            this.descriptionLines = descriptionLines;
        }
    }
}
