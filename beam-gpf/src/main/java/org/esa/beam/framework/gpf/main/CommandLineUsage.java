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

package org.esa.beam.framework.gpf.main;

import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import com.bc.ceres.core.ServiceRegistry;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.gpf.annotations.*;
import org.esa.beam.framework.gpf.graph.*;
import org.esa.beam.framework.gpf.internal.OperatorClassDescriptor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.text.MessageFormat;
import java.util.*;
import java.util.Map.Entry;

class CommandLineUsage {
    private static final String COMMAND_LINE_USAGE_RESOURCE = "CommandLineUsage.txt";

    public static String getUsageText() {
        final String usagePattern = getUsagePattern();

        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        final ServiceRegistry<OperatorSpi> serviceRegistry = registry.getServiceRegistry();
        final Set<OperatorSpi> spiSet = serviceRegistry.getServices();
        final ArrayList<DocElement> docElementList = new ArrayList<DocElement>(spiSet.size());
        for (OperatorSpi operatorSpi : spiSet) {
            final String opAlias = operatorSpi.getOperatorAlias();
            final OperatorMetadata operatorMetadata = operatorSpi.getOperatorClass().getAnnotation(OperatorMetadata.class);
            if (operatorMetadata != null && !operatorMetadata.internal()) {
                final String descriptionLine;
                if (!operatorMetadata.description().isEmpty()) {
                    descriptionLine = operatorMetadata.description();
                } else {
                    descriptionLine = "No description available.";
                }
                docElementList.add(new DocElement("  " + opAlias, new String[]{descriptionLine}));
            }
        }
        StringBuilder opListText = new StringBuilder(1024);
        appendDocElementList(opListText, docElementList);
        return MessageFormat.format(usagePattern,
                                    CommandLineTool.TOOL_NAME,
                                    CommandLineArgs.DEFAULT_TARGET_FILEPATH,
                                    CommandLineArgs.DEFAULT_FORMAT_NAME,
                                    CommandLineArgs.DEFAULT_TILE_CACHE_SIZE_IN_M,
                                    CommandLineArgs.DEFAULT_TILE_SCHEDULER_PARALLELISM,
                                    opListText.toString());
    }

    private static String getUsagePattern() {
        StringBuilder sb = new StringBuilder(1024);
        try {
            BufferedReader bufferedReader = new BufferedReader(
                    new InputStreamReader(CommandLineArgs.class.getResourceAsStream(COMMAND_LINE_USAGE_RESOURCE)));
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
        } catch (IOException ignored) {
            // ignore
        }

        return sb.toString();
    }

    public static String getUsageTextForGraph(String path, CommandLineContext commandLineContext) {
        final Graph graph;
        try {
            graph = commandLineContext.readGraph(path, new HashMap<String, String>());
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
            ArrayList<DocElement> sourceDocElementList = createSourceDocuElementList(header.getSources());
            ArrayList<DocElement> paramDocElementList = createParamDocuElementList(header.getParameters());

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

    private static ArrayList<DocElement> createSourceDocuElementList(List<HeaderSource> sources) {
        ArrayList<DocElement> docElementList = new ArrayList<DocElement>(10);
        for (HeaderSource headerSource : sources) {
            String sourceSyntax = MessageFormat.format("  -S{0}=<file>", headerSource.getName());
            final ArrayList<String> descriptionLines = createSourceDecriptionLines(headerSource);
            docElementList.add(new DocElement(sourceSyntax, descriptionLines.toArray(new String[descriptionLines.size()])));
        }

        return docElementList;
    }

    private static ArrayList<DocElement> createParamDocuElementList(List<HeaderParameter> parameterList) {
        ArrayList<DocElement> docElementList = new ArrayList<DocElement>(10);

        for (HeaderParameter parameter : parameterList) {
            String paramSyntax = MessageFormat.format("  -P{0}=<{1}>", parameter.getName(), parameter.getType());
            final ArrayList<String> descriptionLines = createParamDescriptionLines(parameter);
            docElementList.add(new DocElement(paramSyntax, descriptionLines.toArray(new String[descriptionLines.size()])));
        }

        return docElementList;
    }

    private static ArrayList<String> createParamDescriptionLines(HeaderParameter parameter) {
        final ArrayList<String> descriptionLines = new ArrayList<String>();
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

    private static ArrayList<String> createSourceDecriptionLines(HeaderSource headerSource) {
        final ArrayList<String> descriptionLines = new ArrayList<String>();

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
        final OperatorClassDescriptor operatorClassDescriptor = new OperatorClassDescriptor(operatorSpi.getOperatorClass());
        StringBuilder usageText = new StringBuilder(1024);
        usageText.append("Usage:\n");
        usageText.append(MessageFormat.format("  {0} {1} [options] ", CommandLineTool.TOOL_NAME, operatorName));
        ArrayList<DocElement> sourceDocElementList = createSourceDocuElementList(operatorClassDescriptor);
        ArrayList<DocElement> paramDocElementList = createParamDocuElementList(operatorClassDescriptor);
        ArrayList<DocElement> propertyDocElementList = createPropertyDocuElementList(operatorClassDescriptor);
        final SourceProducts productsDescriptor = operatorClassDescriptor.getSourceProducts();
        if (productsDescriptor != null) {
            appendSourceFiles(usageText, productsDescriptor);
        }
        usageText.append("\n");

        if (operatorClassDescriptor.getOperatorMetadata() != null && !operatorClassDescriptor.getOperatorMetadata().description().isEmpty()) {
            usageText.append("\nDescription:\n");
            final String description = operatorClassDescriptor.getOperatorMetadata().description();
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
        if (!operatorClassDescriptor.getParameters().isEmpty()) {
            usageText.append("\nGraph XML Format:\n");
            appendXmlUsage(usageText, operatorClassDescriptor);
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
        for (Entry<Field, Parameter> entry : parameterMap.entrySet()) {
            final Field paramField = entry.getKey();
            final Parameter parameter = entry.getValue();
            if (isConverterAvailable(paramField.getType(), parameter)) {
                String paramSyntax = MessageFormat.format("  -P{0}=<{1}>", getParameterName(paramField, parameter), getTypeName(paramField.getType()));
                final ArrayList<String> descriptionLines = createParamDescriptionLines(paramField, parameter);
                docElementList.add(new DocElement(paramSyntax, descriptionLines.toArray(new String[descriptionLines.size()])));
            }
        }
        return docElementList;
    }

    private static ArrayList<DocElement> createPropertyDocuElementList(OperatorClassDescriptor operatorClassDescriptor) {
        ArrayList<DocElement> docElementList = new ArrayList<DocElement>(10);
        final Map<Field, TargetProperty> propertyMap = operatorClassDescriptor.getTargetProperties();
        for (Entry<Field, TargetProperty> entry : propertyMap.entrySet()) {
            final Field propertyField = entry.getKey();
            final TargetProperty property = entry.getValue();
            String propertySyntax = MessageFormat.format("{0} {1}", propertyField.getType().getSimpleName(), getTargetPropertyName(propertyField, property));
            final ArrayList<String> descriptionLines = createTargetPropertyDescriptionLines(property);
            docElementList.add(new DocElement(propertySyntax, descriptionLines.toArray(new String[descriptionLines.size()])));
        }
        return docElementList;
    }

    private static ArrayList<DocElement> createSourceDocuElementList(OperatorClassDescriptor operatorClassDescriptor) {
        ArrayList<DocElement> docElementList = new ArrayList<DocElement>(10);
        final Map<Field, SourceProduct> sourceProductMap = operatorClassDescriptor.getSourceProductMap();
        for (Entry<Field, SourceProduct> entry : sourceProductMap.entrySet()) {
            final Field sourceIdField = entry.getKey();
            final SourceProduct sourceProduct = entry.getValue();
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
                                                      getTypeName(paramField.getType())));
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
        if (!parameter.unit().isEmpty()) {
            descriptionLines.add(MessageFormat.format("Parameter Unit is ''{0}''.", parameter.unit()));
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

    private static ArrayList<String> createTargetPropertyDescriptionLines(TargetProperty property) {
        final ArrayList<String> descriptionLines = new ArrayList<String>();
        if (!property.description().isEmpty()) {
            descriptionLines.add(property.description());
        }

        return descriptionLines;
    }

    private static void appendDocElementList(StringBuilder usageText, List<DocElement> docElementList) {
        int maxLength = 0;
        final int minSpaceCount = 4;
        for (DocElement docElement : docElementList) {
            maxLength = Math.max(maxLength, docElement.syntax.length());
        }

        sortAlphabetically(docElementList);

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
        Collections.sort(docElementList, new Comparator<DocElement>() {
            @Override
            public int compare(DocElement element1, DocElement element2) {
                return element1.syntax.compareTo(element2.syntax);
            }
        });
    }

    private static void appendXmlUsage(StringBuilder usageText, OperatorClassDescriptor operatorClassDescriptor) {

        final DomElement graphElem = new XppDomElement("graph");
        graphElem.setAttribute("id", "someGraphId");
        final DomElement versionElem = graphElem.createChild("version");
        versionElem.setValue("1.0");
        final DomElement nodeElem = graphElem.createChild("node");
        nodeElem.setAttribute("id", "someNodeId");
        final DomElement operatorElem = nodeElem.createChild("operator");
        operatorElem.setValue(OperatorSpi.getOperatorAlias(operatorClassDescriptor.getOperatorClass()));
        DomElement sourcesElem = nodeElem.createChild("sources");
        for (Field sourceField : operatorClassDescriptor.getSourceProductMap().keySet()) {
            convertSourceProductFieldToDom(sourceField, sourcesElem);
        }
        if (operatorClassDescriptor.getSourceProducts() != null) {
            final DomElement child = sourcesElem.createChild("sourceProducts");
            child.setValue("${sourceProducts}");
        }
        DomElement parametersElem = nodeElem.createChild("parameters");
        for (Field paramField : operatorClassDescriptor.getParameters().keySet()) {
            convertParameterFieldToDom(paramField, parametersElem);
        }

        final StringTokenizer st = new StringTokenizer(graphElem.toXml().replace('\r', ' '), "\n");
        while (st.hasMoreElements()) {
            appendLine(usageText, "  ", st.nextToken());
        }
    }

    static void convertSourceProductFieldToDom(Field sourceField, DomElement sourcesElem) {
        final int mod = sourceField.getModifiers();
        if (Modifier.isTransient(mod) || Modifier.isFinal(mod) || Modifier.isStatic(mod)) {
            return;
        }
        final SourceProduct sourceProduct = sourceField.getAnnotation(SourceProduct.class);
        String name = sourceField.getName();
        if (sourceProduct != null && !sourceProduct.alias().isEmpty()) {
            name = sourceProduct.alias();
        }
        final DomElement child = sourcesElem.createChild(name);
        child.setValue("${" + name + "}");
    }

    static void convertParameterFieldToDom(Field paramField, DomElement parametersElem) {
        final int mod = paramField.getModifiers();
        if (Modifier.isTransient(mod) || Modifier.isFinal(mod) || Modifier.isStatic(mod)) {
            return;
        }
        final Parameter parameter = paramField.getAnnotation(Parameter.class);
        final boolean thisIsAnOperator = Operator.class.isAssignableFrom(paramField.getDeclaringClass());
        if (thisIsAnOperator && parameter != null || !thisIsAnOperator) {
            String name = paramField.getName();
            if (parameter != null && !parameter.alias().isEmpty()) {
                name = parameter.alias();
            }
            if (paramField.getType().isArray() && parameter != null && !parameter.itemAlias().isEmpty()) {
                DomElement childElem = parameter.itemsInlined() ? parametersElem : parametersElem.createChild(name);
                String itemName = parameter.itemAlias();
                final DomElement element = childElem.createChild(itemName);
                if (isConverterAvailable(paramField.getType(), parameter)) {
                    element.setValue(getTypeName(paramField.getType().getComponentType()));
                } else {
                    final Field[] declaredFields = paramField.getType().getComponentType().getDeclaredFields();
                    for (Field declaredField : declaredFields) {
                        convertParameterFieldToDom(declaredField, element);
                    }
                }
                childElem.createChild("...");
            } else {
                final DomElement childElem = parametersElem.createChild(name);
                Class<?> type = paramField.getType();
                if (isConverterAvailable(type, parameter)) {
                    childElem.setValue(getTypeName(type));
                } else {
                    final Field[] declaredFields = type.getDeclaredFields();
                    for (Field declaredField : declaredFields) {
                        convertParameterFieldToDom(declaredField, childElem);
                    }
                }
            }
        }
    }

    private static boolean isConverterAvailable(Class<?> type, Parameter parameter) {
        return (parameter != null && parameter.converter() != Converter.class)
                || ConverterRegistry.getInstance().getConverter(type) != null;
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

    private static String getTargetPropertyName(Field paramField, TargetProperty property) {
        return property.alias().isEmpty() ? paramField.getName() : property.alias();
    }

    private static String getSourceProductId(Field sourceProductField, SourceProduct sourceProduct) {
        return sourceProduct.alias().isEmpty() ? sourceProductField.getName() : sourceProduct.alias();
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
