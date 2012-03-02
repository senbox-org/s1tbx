/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import com.bc.ceres.core.ProgressMonitor;
import com.thoughtworks.xstream.io.copy.HierarchicalStreamCopier;
import com.thoughtworks.xstream.io.xml.XppDomWriter;
import com.thoughtworks.xstream.io.xml.XppReader;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.experimental.Output;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.gpf.graph.Node;
import org.esa.beam.framework.gpf.graph.NodeSource;
import org.esa.beam.framework.gpf.internal.OperatorExecutor;
import org.esa.beam.framework.gpf.internal.OperatorProductReader;
import org.esa.beam.gpf.operators.standard.ReadOp;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.logging.BeamLogManager;

import javax.media.jai.JAI;
import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.logging.Logger;

/**
 * The common command-line tool for the GPF.
 * For usage, see {@link org/esa/beam/framework/gpf/main/CommandLineUsage.txt}.
 */
class CommandLineTool {

    static final String TOOL_NAME = "gpt";
    static final String DEFAULT_TARGET_FILEPATH = "./target.dim";
    static final String DEFAULT_FORMAT_NAME = ProductIO.DEFAULT_FORMAT_NAME;
    static final int DEFAULT_TILE_CACHE_SIZE_IN_M = 512;
    static final int DEFAULT_TILE_SCHEDULER_PARALLELISM = Runtime.getRuntime().availableProcessors();
    static final String KEY_PARAMETERS_XML = "gpt.parameters.xml";

    private final CommandLineContext commandLineContext;

    static {
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
    }

    /**
     * Constructs a new tool.
     */
    CommandLineTool() {
        this(new DefaultCommandLineContext());
    }

    /**
     * Constructs a new tool with the given context.
     *
     * @param commandLineContext The context used to run the tool.
     */
    CommandLineTool(CommandLineContext commandLineContext) {
        this.commandLineContext = commandLineContext;
    }

    void run(String[] args) throws Exception {

        CommandLineArgs lineArgs = new CommandLineArgs(args);
        try {
            lineArgs.parseArguments();

            if (lineArgs.isHelpRequested()) {
                if (lineArgs.getOperatorName() != null) {
                    commandLineContext.print(CommandLineUsage.getUsageTextForOperator(lineArgs.getOperatorName()));
                } else if (lineArgs.getGraphFilePath() != null) {
                    commandLineContext.print(CommandLineUsage.getUsageTextForGraph(lineArgs.getGraphFilePath(),
                                                                                   commandLineContext));
                } else {
                    commandLineContext.print(CommandLineUsage.getUsageText());
                }
                return;
            }

            run(lineArgs);
        } catch (Exception e) {
            if (lineArgs.isStackTraceDump()) {
                e.printStackTrace(System.err);
            }
            throw e;
        }
    }

    private void run(CommandLineArgs lineArgs) throws ValidationException, IOException, GraphException {
        initializeJAI(lineArgs.getTileCacheCapacity(), lineArgs.getTileSchedulerParallelism());

        final OperatorSpiRegistry operatorSpiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();

        if (lineArgs.getOperatorName() != null) {
            // Operator name given: parameters and sources are parsed from command-line args
            runOperator(lineArgs);
        } else if (lineArgs.getGraphFilePath() != null) {
            // Path to Graph XML given: parameters and sources are parsed from command-line args
            runGraph(lineArgs, operatorSpiRegistry);
        }
    }

    private void runOperator(CommandLineArgs lineArgs)
            throws IOException, ValidationException {

        Map<String, String> parameterMap = getRawParameterMap(lineArgs);
        String operatorName = lineArgs.getOperatorName();
        Map<String, Object> parameters = convertParameterMap(operatorName, parameterMap);
        Map<String, Product> sourceProducts = getSourceProductMap(lineArgs);
        Product targetProduct = createOpProduct(operatorName, parameters, sourceProducts);
        // write product only if Operator does not implement the Output interface
        OperatorProductReader opProductReader = null;
        if (targetProduct.getProductReader() instanceof OperatorProductReader) {
            opProductReader = (OperatorProductReader) targetProduct.getProductReader();
        }
        if (opProductReader != null && opProductReader.getOperatorContext().getOperator() instanceof Output) {
            final Operator operator = opProductReader.getOperatorContext().getOperator();
            final OperatorExecutor executor = OperatorExecutor.create(operator);
            executor.execute(ProgressMonitor.NULL);
        } else {
            String filePath = lineArgs.getTargetFilePath();
            String formatName = lineArgs.getTargetFormatName();
            writeProduct(targetProduct, filePath, formatName, lineArgs.isClearCacheAfterRowWrite());
        }
    }

    private void runGraph(CommandLineArgs lineArgs, OperatorSpiRegistry operatorSpiRegistry)
            throws IOException, GraphException {

        Map<String, String> templateVariables = getRawParameterMap(lineArgs);

        Map<String, String> sourceNodeIdMap = getSourceNodeIdMap(lineArgs);
        templateVariables.putAll(sourceNodeIdMap);

        Graph graph = readGraph(lineArgs.getGraphFilePath(), templateVariables);
        Node lastNode = graph.getNode(graph.getNodeCount() - 1);
        SortedMap<String, String> sourceFilePathsMap = lineArgs.getSourceFilePathMap();

        // For each source path add a ReadOp to the graph
        String readOperatorAlias = OperatorSpi.getOperatorAlias(ReadOp.class);
        for (Entry<String, String> entry : sourceFilePathsMap.entrySet()) {
            String sourceId = entry.getKey();
            String sourceFilePath = entry.getValue();
            String sourceNodeId = sourceNodeIdMap.get(sourceId);
            if (graph.getNode(sourceNodeId) == null) {

                DomElement configuration = new DefaultDomElement("parameters");
                configuration.createChild("file").setValue(sourceFilePath);

                Node sourceNode = new Node(sourceNodeId, readOperatorAlias);
                sourceNode.setConfiguration(configuration);

                graph.addNode(sourceNode);
            }
        }

        final String operatorName = lastNode.getOperatorName();
        final OperatorSpi lastOpSpi = operatorSpiRegistry.getOperatorSpi(operatorName);
        if (lastOpSpi == null) {
            throw new GraphException(String.format("Unknown operator name'%s'. No SPI found.", operatorName));
        }

        if (!Output.class.isAssignableFrom(lastOpSpi.getOperatorClass())) {

            // If the graph's last node does not implement Output, then add a WriteOp
            String writeOperatorAlias = OperatorSpi.getOperatorAlias(WriteOp.class);

            DomElement configuration = new DefaultDomElement("parameters");
            configuration.createChild("file").setValue(lineArgs.getTargetFilePath());
            configuration.createChild("formatName").setValue(lineArgs.getTargetFormatName());
            configuration.createChild("clearCacheAfterRowWrite").setValue(
                    Boolean.toString(lineArgs.isClearCacheAfterRowWrite()));

            Node targetNode = new Node("WriteProduct$" + lastNode.getId(), writeOperatorAlias);
            targetNode.addSource(new NodeSource("source", lastNode.getId()));
            targetNode.setConfiguration(configuration);

            graph.addNode(targetNode);
        }
        executeGraph(graph);
    }

    private void initializeJAI(long memoryCapacity, int parallelism) {
        if (memoryCapacity > 0) {
            JAI.enableDefaultTileCache();
            JAI.getDefaultInstance().getTileCache().setMemoryCapacity(memoryCapacity);
        } else {
            JAI.getDefaultInstance().getTileCache().setMemoryCapacity(0L);
            JAI.disableDefaultTileCache();
        }
        if (parallelism > 0) {
            JAI.getDefaultInstance().getTileScheduler().setParallelism(parallelism);
        }

        final long tileCacheSize = JAI.getDefaultInstance().getTileCache().getMemoryCapacity() / (1024L * 1024L);
        final Logger systemLogger = BeamLogManager.getSystemLogger();
        systemLogger.info(MessageFormat.format("JAI tile cache size is {0} MB", tileCacheSize));
        final int schedulerParallelism = JAI.getDefaultInstance().getTileScheduler().getParallelism();
        systemLogger.info(MessageFormat.format("JAI tile scheduler parallelism is {0}", schedulerParallelism));

    }

    private static Map<String, Object> convertParameterMap(String operatorName, Map<String, String> parameterMap) throws
            ValidationException {
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        PropertyContainer container = ParameterDescriptorFactory.createMapBackedOperatorPropertyContainer(operatorName,
                                                                                                          parameters);
        // explicitly set default values for putting them into the backing map
        container.setDefaultValues();

        // handle xml parameters
        String xmlParameters = parameterMap.get(KEY_PARAMETERS_XML);
        if (xmlParameters != null) {
            OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
            Class<? extends Operator> operatorClass = operatorSpi.getOperatorClass();
            DefaultDomConverter domConverter = new DefaultDomConverter(operatorClass, new ParameterDescriptorFactory());

            DomElement parametersElement = createDomElement(xmlParameters);
            try {
                domConverter.convertDomToValue(parametersElement, container);
            } catch (ConversionException e) {
                throw new RuntimeException(String.format(
                        "Can not convert XML parameters for operator '%s'", operatorName));
            }

            parameterMap.remove(KEY_PARAMETERS_XML);
        }

        for (Entry<String, String> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();
            String paramValue = entry.getValue();
            final Property property = container.getProperty(paramName);
            if (property != null) {
                property.setValueFromText(paramValue);
            } else {
                throw new RuntimeException(String.format(
                        "Parameter '%s' is not known by operator '%s'", paramName, operatorName));
            }
        }
        return parameters;
    }

    private static DomElement createDomElement(String xml) {
        XppDomWriter domWriter = new XppDomWriter();
        new HierarchicalStreamCopier().copy(new XppReader(new StringReader(xml)), domWriter);
        XppDom xppDom = domWriter.getConfiguration();
        return new XppDomElement(xppDom);
    }


    private Map<String, Product> getSourceProductMap(CommandLineArgs lineArgs) throws IOException {
        SortedMap<File, Product> fileToProductMap = new TreeMap<File, Product>();
        SortedMap<String, Product> productMap = new TreeMap<String, Product>();
        SortedMap<String, String> sourceFilePathsMap = lineArgs.getSourceFilePathMap();
        for (Entry<String, String> entry : sourceFilePathsMap.entrySet()) {
            String sourceId = entry.getKey();
            String sourceFilePath = entry.getValue();
            Product product = addProduct(sourceFilePath, fileToProductMap);
            productMap.put(sourceId, product);
        }
        return productMap;
    }

    private Product addProduct(String sourceFilepath,
                               Map<File, Product> fileToProductMap) throws IOException {
        File sourceFile = new File(sourceFilepath).getCanonicalFile();
        Product product = fileToProductMap.get(sourceFile);
        if (product == null) {
            String s = sourceFile.getPath();
            product = readProduct(s);
            if (product == null) {
                throw new IOException("No appropriate product reader found for " + sourceFile);
            }
            fileToProductMap.put(sourceFile, product);
        }
        return product;
    }

    // TODO - also use this scheme in the GPF GUIs (nf, 2012-03-02)
    // See also [BEAM-1375] Allow gpt to use template variables in parameter files
    private Map<String, String> getRawParameterMap(CommandLineArgs lineArgs) throws IOException {
        Map<String, String> parameterMap = new HashMap<String, String>();
        if (lineArgs.getParametersFilePath() != null) {
            Map<String, String> templateVariables = new HashMap<String, String>();
            templateVariables.put("gpt.parametersFile", lineArgs.getParametersFilePath());
            templateVariables.put("gpt.operator", lineArgs.getOperatorName());
            templateVariables.put("gpt.graphFile", lineArgs.getGraphFilePath());
            templateVariables.put("gpt.targetFile", lineArgs.getTargetFilePath());
            templateVariables.put("gpt.targetFormat", lineArgs.getTargetFormatName());
            templateVariables.putAll(lineArgs.getParameterMap());
            templateVariables.putAll(lineArgs.getTargetFilePathMap());
            templateVariables.putAll(lineArgs.getSourceFilePathMap());
            parameterMap = readParameterFile(lineArgs.getParametersFilePath(), templateVariables);
        }
        // CLI parameters shall always overwrite file parameters
        parameterMap.putAll(lineArgs.getParameterMap());
        return parameterMap;
    }

    private Map<String, String> getSourceNodeIdMap(CommandLineArgs lineArgs) throws IOException {
        SortedMap<File, String> fileToNodeIdMap = new TreeMap<File, String>();
        SortedMap<String, String> nodeIdMap = new TreeMap<String, String>();
        SortedMap<String, String> sourceFilePathsMap = lineArgs.getSourceFilePathMap();
        for (Entry<String, String> entry : sourceFilePathsMap.entrySet()) {
            String sourceId = entry.getKey();
            String sourceFilePath = entry.getValue();
            String nodeId = addNodeId(sourceFilePath, fileToNodeIdMap);
            nodeIdMap.put(sourceId, nodeId);
        }
        return nodeIdMap;
    }

    private String addNodeId(String sourceFilePath,
                             Map<File, String> fileToNodeId) throws IOException {
        File sourceFile = new File(sourceFilePath).getCanonicalFile();
        String nodeId = fileToNodeId.get(sourceFile);
        if (nodeId == null) {
            nodeId = "ReadProduct$" + fileToNodeId.size();
            fileToNodeId.put(sourceFile, nodeId);
        }
        return nodeId;
    }

    Product readProduct(String filePath) throws IOException {
        return commandLineContext.readProduct(filePath);
    }

    void writeProduct(Product targetProduct,
                      String filePath,
                      String formatName,
                      boolean clearCacheAfterRowWrite) throws
            IOException {
        commandLineContext.writeProduct(targetProduct, filePath, formatName, clearCacheAfterRowWrite);
    }

    Graph readGraph(String filePath,
                    Map<String, String> templateVariables) throws IOException, GraphException {
        return commandLineContext.readGraph(filePath, templateVariables);
    }

    void executeGraph(Graph graph) throws GraphException {
        commandLineContext.executeGraph(graph);
    }

    Map<String, String> readParameterFile(String filePath,
                                          Map<String, String> templateVariables) throws IOException {
        return commandLineContext.readParametersFile(filePath, templateVariables);
    }

    private Product createOpProduct(String opName,
                                    Map<String, Object> parameters,
                                    Map<String, Product> sourceProducts) throws OperatorException {
        return commandLineContext.createOpProduct(opName, parameters, sourceProducts);
    }
}
