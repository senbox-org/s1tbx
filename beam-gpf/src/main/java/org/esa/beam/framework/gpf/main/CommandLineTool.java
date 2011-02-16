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

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.dom.DefaultDomElement;
import com.bc.ceres.binding.dom.DomElement;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.ParameterDescriptorFactory;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.gpf.graph.Node;
import org.esa.beam.framework.gpf.graph.NodeSource;
import org.esa.beam.gpf.operators.standard.ReadOp;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.logging.BeamLogManager;

import javax.media.jai.JAI;
import java.io.File;
import java.io.IOException;
import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * The common command-line tool for the GPF.
 * For usage, see {@link org/esa/beam/framework/gpf/main/CommandLineUsage.txt}.
 */
class CommandLineTool {

    static final String TOOL_NAME = "gpt";
    static final String DEFAULT_TARGET_FILEPATH = "./target.dim";
    static final String DEFAULT_FORMAT_NAME = ProductIO.DEFAULT_FORMAT_NAME;
    static final int DEFAULT_TILE_CACHE_SIZE_IN_M = 512;
    static final int DEFAULT_TILE_SCHEDULER_PARALLELSIM = Runtime.getRuntime().availableProcessors();

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
                } else if (lineArgs.getGraphFilepath() != null) {
                    commandLineContext.print(CommandLineUsage.getUsageTextForGraph(lineArgs.getGraphFilepath(),
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

    private void run(CommandLineArgs lineArgs) throws ValidationException, ConversionException, IOException, GraphException {
        long memoryCapacity = lineArgs.getTileCacheCapacity();
        if (memoryCapacity > 0) {
            JAI.enableDefaultTileCache();
            JAI.getDefaultInstance().getTileCache().setMemoryCapacity(memoryCapacity);
        } else {
            JAI.getDefaultInstance().getTileCache().setMemoryCapacity(0L);
            JAI.disableDefaultTileCache();
        }
        int parallelism = lineArgs.getTileSchedulerParallelism();
        if (parallelism > 0) {
            JAI.getDefaultInstance().getTileScheduler().setParallelism(parallelism);
        }
        BeamLogManager.getSystemLogger().info(MessageFormat.format("JAI tile cache size is {0} MB", JAI.getDefaultInstance().getTileCache().getMemoryCapacity() / (1024*1024)));
        BeamLogManager.getSystemLogger().info(MessageFormat.format("JAI tile scheduler parallelism is {0}", JAI.getDefaultInstance().getTileScheduler().getParallelism()));

        if (lineArgs.getOperatorName() != null) {
            // Operator name given: parameters and sources are parsed from command-line args
            Map<String, Product> sourceProducts = getSourceProductMap(lineArgs);
            Map<String, Object> parameters = getParameterMap(lineArgs);
            String opName = lineArgs.getOperatorName();
            Product targetProduct = createOpProduct(opName, parameters, sourceProducts);
            String filePath = lineArgs.getTargetFilepath();
            String formatName = lineArgs.getTargetFormatName();
            writeProduct(targetProduct, filePath, formatName, lineArgs.isClearCacheAfterRowWrite());
        } else if (lineArgs.getGraphFilepath() != null) {
            // Path to Graph XML given: parameters and sources are parsed from command-line args
            Map<String, String> sourceNodeIdMap = getSourceNodeIdMap(lineArgs);
            Map<String, String> parameters = new TreeMap<String, String>(sourceNodeIdMap);
            if (lineArgs.getParameterFilepath() != null) {
                parameters.putAll(readParameterFile(lineArgs.getParameterFilepath()));
            }
            parameters.putAll(lineArgs.getParameterMap());
            Graph graph = readGraph(lineArgs.getGraphFilepath(), parameters);
            Node lastNode = graph.getNode(graph.getNodeCount() - 1);
            SortedMap<String, String> sourceFilepathsMap = lineArgs.getSourceFilepathMap();

            // For each source path add a ReadOp to the graph
            String readOperatorAlias = OperatorSpi.getOperatorAlias(ReadOp.class);
            for (Entry<String, String> entry : sourceFilepathsMap.entrySet()) {
                String sourceId = entry.getKey();
                String sourceFilepath = entry.getValue();
                String sourceNodeId = sourceNodeIdMap.get(sourceId);
                if (graph.getNode(sourceNodeId) == null) {

                    DomElement configuration = new DefaultDomElement("parameters");
                    configuration.createChild("file").setValue(sourceFilepath);

                    Node sourceNode = new Node(sourceNodeId, readOperatorAlias);
                    sourceNode.setConfiguration(configuration);

                    graph.addNode(sourceNode);
                }
            }

            // If the graph's last node isn't a WriteOp, then add one
            String writeOperatorAlias = OperatorSpi.getOperatorAlias(WriteOp.class);
            if (!lastNode.getOperatorName().equals(writeOperatorAlias)) {

                DomElement configuration = new DefaultDomElement("parameters");
                configuration.createChild("file").setValue(lineArgs.getTargetFilepath());
                configuration.createChild("formatName").setValue(lineArgs.getTargetFormatName());
                configuration.createChild("clearCacheAfterRowWrite").setValue(Boolean.toString(lineArgs.isClearCacheAfterRowWrite()));

                Node targetNode = new Node("WriteProduct$" + lastNode.getId(), writeOperatorAlias);
                targetNode.addSource(new NodeSource("source", lastNode.getId()));
                targetNode.setConfiguration(configuration);

                graph.addNode(targetNode);
            }

            executeGraph(graph);
        }
    }

    private Map<String, Object> getParameterMap(CommandLineArgs lineArgs) throws ValidationException, IOException {
        Map<String, String> parameterMap = new HashMap<String, String>();
        if (lineArgs.getParameterFilepath() != null) {
            parameterMap = readParameterFile(lineArgs.getParameterFilepath());
        }

        String operatorName = lineArgs.getOperatorName();
        parameterMap.putAll(lineArgs.getParameterMap());
        return convertParameterMap(operatorName, parameterMap);
    }

    private static Map<String, Object> convertParameterMap(String operatorName, Map<String, String> parameterMap) throws ValidationException {
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        PropertyContainer container = ParameterDescriptorFactory.createMapBackedOperatorPropertyContainer(operatorName, parameters);
        // explicitly set default values for putting them into the backing map
        container.setDefaultValues();
        for (Entry<String, String> entry : parameterMap.entrySet()) {
            String paramName = entry.getKey();
            String paramValue = entry.getValue();
            final Property model = container.getProperty(paramName);
            if (model != null) {
                model.setValueFromText(paramValue);
            } else {
                throw new RuntimeException(String.format(
                        "Parameter '%s' is not known by operator '%s'", paramName, operatorName));
            }
        }
        return parameters;
    }

    private Map<String, Product> getSourceProductMap(CommandLineArgs lineArgs) throws IOException {
        SortedMap<File, Product> fileToProductMap = new TreeMap<File, Product>();
        SortedMap<String, Product> productMap = new TreeMap<String, Product>();
        SortedMap<String, String> sourceFilepathsMap = lineArgs.getSourceFilepathMap();
        for (Entry<String, String> entry : sourceFilepathsMap.entrySet()) {
            String sourceId = entry.getKey();
            String sourceFilepath = entry.getValue();
            Product product = addProduct(sourceFilepath, fileToProductMap);
            productMap.put(sourceId, product);
        }
        return productMap;
    }

    private Product addProduct(String sourceFilepath, Map<File, Product> fileToProductMap) throws IOException {
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

    private Map<String, String> getSourceNodeIdMap(CommandLineArgs lineArgs) throws IOException {
        SortedMap<File, String> fileToNodeIdMap = new TreeMap<File, String>();
        SortedMap<String, String> nodeIdMap = new TreeMap<String, String>();
        SortedMap<String, String> sourceFilepathsMap = lineArgs.getSourceFilepathMap();
        for (Entry<String, String> entry : sourceFilepathsMap.entrySet()) {
            String sourceId = entry.getKey();
            String sourceFilepath = entry.getValue();
            String nodeId = addNodeId(sourceFilepath, fileToNodeIdMap);
            nodeIdMap.put(sourceId, nodeId);
        }
        return nodeIdMap;
    }

    private String addNodeId(String sourceFilepath, Map<File, String> fileToNodeId) throws IOException {
        File sourceFile = new File(sourceFilepath).getCanonicalFile();
        String nodeId = fileToNodeId.get(sourceFile);
        if (nodeId == null) {
            nodeId = "ReadProduct$" + fileToNodeId.size();
            fileToNodeId.put(sourceFile, nodeId);
        }
        return nodeId;
    }

    Product readProduct(String productFilepath) throws IOException {
        return commandLineContext.readProduct(productFilepath);
    }

    void writeProduct(Product targetProduct, String filePath, String formatName, boolean clearCacheAfterRowWrite) throws IOException {
        commandLineContext.writeProduct(targetProduct, filePath, formatName, clearCacheAfterRowWrite);
    }

    Graph readGraph(String filepath, Map<String, String> parameterMap) throws IOException, GraphException {
        return commandLineContext.readGraph(filepath, parameterMap);
    }

    void executeGraph(Graph graph) throws GraphException {
        commandLineContext.executeGraph(graph);
    }

    Map<String, String> readParameterFile(String propertiesFilepath) throws IOException {
        return commandLineContext.readParameterFile(propertiesFilepath);
    }

    private Product createOpProduct(String opName, Map<String, Object> parameters, Map<String, Product> sourceProducts) throws OperatorException {
        return commandLineContext.createOpProduct(opName, parameters, sourceProducts);
    }
}
