package org.esa.beam.framework.gpf.main;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.annotations.ParameterDefinitionFactory;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.gpf.graph.Node;
import org.esa.beam.framework.gpf.graph.NodeSource;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * The common command-line tool for the GPF.
 * For usage, see {@link org/esa/beam/framework/gpf/main/CommandLineUsage.txt}.
 */
class CommandLineTool {
    private final CommandLineContext commandLineContext;

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
        CommandLine line = new CommandLine(args);
        if (line.isHelpRequested()) {
            commandLineContext.print(CommandLine.getUsageText());
        }
        if (line.getOperatorName() != null) {
            Map<String, Object> parameters = getParameterMap(line);
            Map<String, Product> sourceProducts = getSourceProductMap(line);
            String opName = line.getOperatorName();
            Product targetProduct = createOpProduct(opName, parameters, sourceProducts);
            String filePath = line.getTargetFilepath();
            String formatName = line.getTargetFormatName();
            writeProduct(targetProduct, filePath, formatName);
        } else if (line.getGraphFilepath() != null) {
            Map<String, String> sourceNodeIdMap = getSourceNodeIdMap(line);
            Map<String, String> templateMap = new TreeMap<String, String>(sourceNodeIdMap);
            if (line.getParameterFilepath() != null) {
                templateMap.putAll(readParameterFile(line.getParameterFilepath()));
            }
            templateMap.putAll(line.getParameterMap());
            Graph graph = readGraph(line.getGraphFilepath(), templateMap);
            Node lastNode = graph.getNode(graph.getNodeCount() - 1);
            SortedMap<String, String> sourceFilepathsMap = line.getSourceFilepathMap();
            for (String sourceId : sourceNodeIdMap.keySet()) {
                String sourceNodeId = sourceNodeIdMap.get(sourceId);
                if (graph.getNode(sourceNodeId) == null) {
                    Node sourceNode = new Node(sourceNodeId, "ReadProduct");
                    Xpp3Dom parameters = new Xpp3Dom("parameters");
                    Xpp3Dom filePath = new Xpp3Dom("filePath");
                    filePath.setValue(sourceFilepathsMap.get(sourceId));
                    parameters.addChild(filePath);
                    sourceNode.setConfiguration(parameters);
                    graph.addNode(sourceNode);
                }
            }
            Node targetNode = new Node("WriteProduct$" + lastNode.getId(), "WriteProduct");
            targetNode.addSource(new NodeSource("input", lastNode.getId()));
            Xpp3Dom configDom = new Xpp3Dom("parameters");
            Xpp3Dom dom1 = new Xpp3Dom("filePath");
            dom1.setValue(line.getTargetFilepath());
            configDom.addChild(dom1);
            Xpp3Dom dom2 = new Xpp3Dom("formatName");
            dom2.setValue(line.getTargetFormatName());
            configDom.addChild(dom2);
            targetNode.setConfiguration(configDom);
            graph.addNode(targetNode);

            executeGraph(graph);
        }
    }

    private Map<String, Object> getParameterMap(CommandLine line) throws ValidationException, ConversionException {
        HashMap<String, Object> parameters = new HashMap<String, Object>();

        ValueContainer container = ParameterDefinitionFactory.createMapBackedOperatorValueContainer(line.getOperatorName(), parameters);
        SortedMap<String, String> hashMap = line.getParameterMap();
        Set<String> paramNames = hashMap.keySet();
        for (String paramName : paramNames) {
            container.setFromText(paramName, hashMap.get(paramName));
        }
        return parameters;
    }

    private Map<String, Product> getSourceProductMap(CommandLine line) throws IOException {
        SortedMap<File, Product> fileToProductMap = new TreeMap<File, Product>();
        SortedMap<String, Product> productMap = new TreeMap<String, Product>();
        SortedMap<String, String> sourceFilepathsMap = line.getSourceFilepathMap();
        for (String sourceId : sourceFilepathsMap.keySet()) {
            String sourceFilepath = sourceFilepathsMap.get(sourceId);
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
                throw new IOException("No approriate product reader found for " + sourceFile);
            }
            fileToProductMap.put(sourceFile, product);
        }
        return product;
    }

    private Map<String, String> getSourceNodeIdMap(CommandLine line) throws IOException {
        SortedMap<File, String> fileToNodeIdMap = new TreeMap<File, String>();
        SortedMap<String, String> nodeIdMap = new TreeMap<String, String>();
        SortedMap<String, String> sourceFilepathsMap = line.getSourceFilepathMap();
        for (String sourceId : sourceFilepathsMap.keySet()) {
            String sourceFilepath = sourceFilepathsMap.get(sourceId);
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

    public Product readProduct(String productFilepath) throws IOException {
        return commandLineContext.readProduct(productFilepath);
    }

    public void writeProduct(Product targetProduct, String filePath, String formatName) throws IOException {
        commandLineContext.writeProduct(targetProduct, filePath, formatName);
    }

    public Graph readGraph(String filepath, Map<String, String> parameterMap) throws IOException {
        return commandLineContext.readGraph(filepath, parameterMap);
    }

    public void executeGraph(Graph graph) throws GraphException {
        commandLineContext.executeGraph(graph);
    }

    public Map<String, String> readParameterFile(String propertiesFilepath) throws IOException {
        return commandLineContext.readParameterFile(propertiesFilepath);
    }

    private Product createOpProduct(String opName, Map<String, Object> parameters, Map<String, Product> sourceProducts) throws OperatorException {
        return commandLineContext.createOpProduct(opName, parameters, sourceProducts);
    }

}
