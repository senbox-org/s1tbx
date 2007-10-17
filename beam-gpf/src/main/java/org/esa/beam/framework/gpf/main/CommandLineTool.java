package org.esa.beam.framework.gpf.main;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.annotations.ParameterDefinitionFactory;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.gpf.graph.Node;
import org.esa.beam.framework.gpf.graph.NodeSource;

import javax.media.jai.JAI;
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

        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();

        CommandLineArgs lineArgs = new CommandLineArgs(args);

        if (lineArgs.isHelpRequested()) {
            if (lineArgs.getOperatorName() != null) {
                commandLineContext.print(CommandLineUsage.getUsageText(lineArgs.getOperatorName()));
            } else {
                commandLineContext.print(CommandLineUsage.getUsageText());
            }
            return;
        }

        JAI.getDefaultInstance().getTileCache().setMemoryCapacity(lineArgs.getTileCacheCapacity());

        if (lineArgs.getOperatorName() != null) {
            Map<String, Object> parameters = getParameterMap(lineArgs);
            Map<String, Product> sourceProducts = getSourceProductMap(lineArgs);
            String opName = lineArgs.getOperatorName();
            Product targetProduct = createOpProduct(opName, parameters, sourceProducts);
            String filePath = lineArgs.getTargetFilepath();
            String formatName = lineArgs.getTargetFormatName();
            writeProduct(targetProduct, filePath, formatName);
        } else if (lineArgs.getGraphFilepath() != null) {
            Map<String, String> sourceNodeIdMap = getSourceNodeIdMap(lineArgs);
            Map<String, String> templateMap = new TreeMap<String, String>(sourceNodeIdMap);
            if (lineArgs.getParameterFilepath() != null) {
                templateMap.putAll(readParameterFile(lineArgs.getParameterFilepath()));
            }
            templateMap.putAll(lineArgs.getParameterMap());
            Graph graph = readGraph(lineArgs.getGraphFilepath(), templateMap);
            Node lastNode = graph.getNode(graph.getNodeCount() - 1);
            SortedMap<String, String> sourceFilepathsMap = lineArgs.getSourceFilepathMap();
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
            dom1.setValue(lineArgs.getTargetFilepath());
            configDom.addChild(dom1);
            Xpp3Dom dom2 = new Xpp3Dom("formatName");
            dom2.setValue(lineArgs.getTargetFormatName());
            configDom.addChild(dom2);
            targetNode.setConfiguration(configDom);
            graph.addNode(targetNode);

            executeGraph(graph);
        }
    }

    private Map<String, Object> getParameterMap(CommandLineArgs lineArgs) throws ValidationException, ConversionException {
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        ValueContainer container = ParameterDefinitionFactory.createMapBackedOperatorValueContainer(lineArgs.getOperatorName(), parameters);
        SortedMap<String, String> parameterMap = lineArgs.getParameterMap();
        Set<String> paramNames = parameterMap.keySet();
        for (String paramName : paramNames) {
            container.setFromText(paramName, parameterMap.get(paramName));
        }
        return parameters;
    }

    private Map<String, Product> getSourceProductMap(CommandLineArgs lineArgs) throws IOException {
        SortedMap<File, Product> fileToProductMap = new TreeMap<File, Product>();
        SortedMap<String, Product> productMap = new TreeMap<String, Product>();
        SortedMap<String, String> sourceFilepathsMap = lineArgs.getSourceFilepathMap();
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

    private Map<String, String> getSourceNodeIdMap(CommandLineArgs lineArgs) throws IOException {
        SortedMap<File, String> fileToNodeIdMap = new TreeMap<File, String>();
        SortedMap<String, String> nodeIdMap = new TreeMap<String, String>();
        SortedMap<String, String> sourceFilepathsMap = lineArgs.getSourceFilepathMap();
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
