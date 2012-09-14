/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dat.plugins.graphbuilder;

import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import com.bc.ceres.core.ProgressMonitor;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.gpf.OperatorUI;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.graph.*;
import org.esa.beam.gpf.operators.standard.ReadOp;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.io.FileUtils;
import org.esa.nest.gpf.GPFProcessor;
import org.esa.nest.gpf.ProductSetReaderOp;
import org.esa.nest.gpf.ReaderUtils;
import org.esa.nest.util.ResourceUtils;

import java.io.File;
import java.io.FileWriter;
import java.util.*;

public class GraphExecuter extends Observable {

    public final static String LAST_GRAPH_PATH = "graphbuilder.last_graph_path";
    
    private final GPF gpf;
    private Graph graph;
    private GraphContext graphContext = null;
    private GraphProcessor processor;
    private String graphDescription = "";
    private File lastLoadedGraphFile = null;

    private int idCount = 0;
    private final List<GraphNode> nodeList = new ArrayList<GraphNode>(10);

    public enum events { ADD_EVENT, REMOVE_EVENT, SELECT_EVENT }

    public GraphExecuter() {

        gpf = GPF.getDefaultInstance();
        gpf.getOperatorSpiRegistry().loadOperatorSpis();

        graph = new Graph("Graph");
    }

    public List<GraphNode> GetGraphNodes() {
        return nodeList;
    }

    public void ClearGraph() {
        graph = null;
        graph = new Graph("Graph");
        lastLoadedGraphFile = null;
        nodeList.clear();
        idCount = 0;
    }

    public GraphNode findGraphNode(String id) {
        for(GraphNode n : nodeList) {
            if(n.getID().equals(id)) {
                return n;
            }
        }
        return null;
    }

    public GraphNode findGraphNodeByOperator(String operatorName) {
        for(GraphNode n : nodeList) {
            if(n.getOperatorName().equals(operatorName)) {
                return n;
            }
        }
        return null;
    }

    public void setSelectedNode(GraphNode node) {
        if(node == null) return;
        setChanged();
        notifyObservers(new GraphEvent(events.SELECT_EVENT, node));
        clearChanged();
    }

    /**
     * Gets the list of operators
     * @return set of operator names
     */
    public Set<String> GetOperatorList() {
        return gpf.getOperatorSpiRegistry().getAliases();
    }

    public boolean isOperatorInternal(String alias) {
        final OperatorSpiRegistry registry = gpf.getOperatorSpiRegistry();
        final OperatorSpi operatorSpi = registry.getOperatorSpi(alias);
        final OperatorMetadata operatorMetadata = operatorSpi.getOperatorClass().getAnnotation(OperatorMetadata.class);
        return !(operatorMetadata != null && !operatorMetadata.internal());
    }

    public String getOperatorCategory(String alias) {
        final OperatorSpiRegistry registry = gpf.getOperatorSpiRegistry();
        final OperatorSpi operatorSpi = registry.getOperatorSpi(alias);
        final OperatorMetadata operatorMetadata = operatorSpi.getOperatorClass().getAnnotation(OperatorMetadata.class);
        if(operatorMetadata != null)
            return operatorMetadata.category();
        return "";
    }

    public GraphNode addOperator(final String opName) {

        final String id = "" + ++idCount + '-' + opName;
        final GraphNode newGraphNode = createNewGraphNode(opName, id);

        setChanged();
        notifyObservers(new GraphEvent(events.ADD_EVENT, newGraphNode));
        clearChanged();

        return newGraphNode;
    }

    private GraphNode createNewGraphNode(final String opName, final String id) {
        final Node newNode = new Node(id, opName);

        final XppDomElement parameters = new XppDomElement("parameters");
        newNode.setConfiguration(parameters);

        graph.addNode(newNode);

        final GraphNode newGraphNode = new GraphNode(newNode);
        nodeList.add(newGraphNode);

        newGraphNode.setOperatorUI(CreateOperatorUI(newGraphNode.getOperatorName()));

        return newGraphNode;
    }

    private static OperatorUI CreateOperatorUI(final String operatorName) {
        final OperatorSpi operatorSpi = GPF.getDefaultInstance().getOperatorSpiRegistry().getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            return null;
        }

        return operatorSpi.createOperatorUI();
    }

    public void removeOperator(final GraphNode node) {

        setChanged();
        notifyObservers(new GraphEvent(events.REMOVE_EVENT, node));
        clearChanged();

        removeNode(node);
    }

    private void removeNode(final GraphNode node) {
        // remove as a source from all nodes
        for(GraphNode n : nodeList) {
            n.disconnectOperatorSources(node.getID());
        }

        graph.removeNode(node.getID());
        nodeList.remove(node);
    }

    public void setOperatorParam(final String id, final String paramName, final String value) {
        final Node node = graph.getNode(id);
        DomElement xml = node.getConfiguration().getChild(paramName);
        if(xml == null) {
            xml = new XppDomElement(paramName);
            node.getConfiguration().addChild(xml);
        }
        xml.setValue(value);
    }

    private void AssignAllParameters() {

        final XppDom presentationXML = new XppDom("Presentation");

        // save graph description
        final XppDom descXML = new XppDom("Description");
        descXML.setValue(graphDescription);
        presentationXML.addChild(descXML);

        for(GraphNode n : nodeList) {
            if(n.GetOperatorUI() != null) {
                n.AssignParameters(presentationXML);
            }
        }

        graph.setAppData("Presentation", presentationXML);
    }

    boolean IsGraphComplete() {
        int nodesWithoutSources = 0;
        for(GraphNode n : nodeList) {
            if(!n.HasSources()) {
                ++nodesWithoutSources;
                if(!IsNodeASource(n))
                    return false;
            }
        }
        return nodesWithoutSources != nodeList.size();
    }

    private boolean IsNodeASource(final GraphNode sourceNode) {
        for(GraphNode n : nodeList) {
            if(n.isNodeSource(sourceNode))
                return true;
        }
        return false;
    }

    private GraphNode[] findConnectedNodes(final GraphNode sourceNode) {
        final List<GraphNode> connectedNodes = new ArrayList<GraphNode>();
        for(GraphNode n : nodeList) {
            if(n.isNodeSource(sourceNode))
                connectedNodes.add(n);
        }
        return connectedNodes.toArray(new GraphNode[connectedNodes.size()]);
    }

    public boolean InitGraph() throws GraphException {
        if(IsGraphComplete()) {
            AssignAllParameters();
            final GraphNode[] savedProductSetList = replaceProductSetReaders();

            try {
                recreateGraphContext();
                updateGraphNodes();
            } finally {
                restoreProductSetReaders(savedProductSetList);
            }
            return true;
        }
        return false;
    }

    private void recreateGraphContext() throws GraphException {
        if(graphContext != null)
            graphContext.dispose();

        processor = new GraphProcessor();
        graphContext = new GraphContext(graph);
    }

    private void updateGraphNodes() {
        if(graphContext != null) {
            for(GraphNode n : nodeList) {
                final NodeContext context = graphContext.getNodeContext(n.getNode());
                n.setSourceProducts(context.getSourceProducts());
            }
        }
    }

    public void disposeGraphContext() {
        graphContext.dispose();
    }

    /**
     * Begins graph processing
     * @param pm The ProgressMonitor
     */
    public void executeGraph(ProgressMonitor pm) {
        processor.executeGraph(graphContext, pm);
    }

    public void cancel() {
        graphContext.cancel();
    }

    File saveGraph() throws GraphException {

        String filename = "myGraph";
        if(lastLoadedGraphFile != null)
            filename = lastLoadedGraphFile.getAbsolutePath();
        final File filePath = ResourceUtils.GetFilePath("Save Graph", "XML", "xml", filename, "Graph", true,
                LAST_GRAPH_PATH, ResourceUtils.getGraphFolder("").getAbsolutePath());
        if(filePath != null)
            writeGraph(filePath.getAbsolutePath());
        return filePath;
    }

    private void writeGraph(final String filePath) throws GraphException {

        try {
            final FileWriter fileWriter = new FileWriter(filePath);

            try {
                AssignAllParameters();
                GraphIO.write(graph, fileWriter);
            } finally {
                fileWriter.close();
            }
        } catch(Exception e) {
            throw new GraphException("Unable to write graph to " + filePath +'\n'+ e.getMessage());
        }
    }

    public void loadGraph(final File filePath, final boolean addUI) throws GraphException {

        try {
            if(filePath == null) return;
            final Graph graphFromFile = GPFProcessor.readGraph(filePath, null);

            setGraph(graphFromFile, addUI);
            lastLoadedGraphFile = filePath;
        } catch(Throwable e) {
            throw new GraphException("Unable to load graph " + filePath +'\n'+e.getMessage());
        }
    }

    public void setGraph(final Graph graphFromFile, final boolean addUI) {
        if(graphFromFile != null) {
            graph = graphFromFile;
            nodeList.clear();

            final XppDom presentationXML = graph.getApplicationData("Presentation");
            if(presentationXML != null) {
                // get graph description
                final XppDom descXML = presentationXML.getChild("Description");
                if(descXML != null && descXML.getValue() != null) {
                    graphDescription = descXML.getValue();
                }
            }

            final Node[] nodes = graph.getNodes();
            for (Node n : nodes) {
                final GraphNode newGraphNode = new GraphNode(n);
                if(presentationXML != null)
                    newGraphNode.setDisplayParameters(presentationXML);
                nodeList.add(newGraphNode);

                if(addUI)
                    newGraphNode.setOperatorUI(CreateOperatorUI(newGraphNode.getOperatorName()));

                setChanged();
                notifyObservers(new GraphEvent(events.ADD_EVENT, newGraphNode));
                clearChanged();
            }
            idCount = nodes.length;
        }
    }

    public String getGraphDescription() {
        return graphDescription;
    }

    public void setGraphDescription(final String text) {
        graphDescription = text;
    }

    private ProductSetData[] findProductSets(final String readerName) {
        final String SEPARATOR = ",";
        final String SEPARATOR_ESC = "\\u002C"; // Unicode escape repr. of ','
        final List<ProductSetData> productSetDataList = new ArrayList<ProductSetData>();

        for(Node n : graph.getNodes()) {
            if(n.getOperatorName().equalsIgnoreCase(readerName)) {
                final ProductSetData psData = new ProductSetData();
                psData.nodeID = n.getId();

                final DomElement config = n.getConfiguration();
                final DomElement[] params = config.getChildren();
                for(DomElement p : params) {
                    if(p.getName().equals("fileList") && p.getValue() != null) {

                        final StringTokenizer st = new StringTokenizer(p.getValue(), SEPARATOR);
                        int length = st.countTokens();
                        for (int i = 0; i < length; i++) {
                            final String str = st.nextToken().replace(SEPARATOR_ESC, SEPARATOR);
                            psData.fileList.add(str);
                        }
                        break;
                    }
                }
                productSetDataList.add(psData);
            }
        }
        return productSetDataList.toArray(new ProductSetData[productSetDataList.size()]);
    }

    private GraphNode[] replaceProductSetReaders() {
        final ProductSetData[] productSetDataList =
                findProductSets(OperatorSpi.getOperatorAlias(ProductSetReaderOp.class));
        final List<GraphNode> savedProductSetList = new ArrayList<GraphNode>();

        int cnt = 0;
        for(ProductSetData psData : productSetDataList) {
            final GraphNode sourceNode = findGraphNode(psData.nodeID);
            for(String filePath : psData.fileList) {

                replaceProductSetWithReaders(sourceNode, "inserted--"+sourceNode.getID()+"--"+ cnt++, filePath);
            }
            if(!psData.fileList.isEmpty()) {
                removeNode(sourceNode);
                savedProductSetList.add(sourceNode);
            }
        }
        return savedProductSetList.toArray(new GraphNode[savedProductSetList.size()]);
    }

    private void restoreProductSetReaders(GraphNode[] savedProductSetList ) {
        for(GraphNode multiSrcNode : savedProductSetList) {

            final List<GraphNode> nodesToRemove = new ArrayList<GraphNode>();
            for(GraphNode n : nodeList) {
                final String id = n.getID();
                if(id.startsWith("inserted") && id.contains(multiSrcNode.getID())) {

                    switchConnections(n, multiSrcNode.getID());
                    nodesToRemove.add(n);
                }
            }
            for(GraphNode r : nodesToRemove) {
                removeNode(r);
            }

            nodeList.add(multiSrcNode);
            graph.addNode(multiSrcNode.getNode());
        }
    }

    private void replaceProductSetWithReaders(final GraphNode sourceNode, final String id, final String value) {

        GraphNode newReaderNode = createNewGraphNode(OperatorSpi.getOperatorAlias(ReadOp.class), id);
        newReaderNode.setOperatorUI(null);
        newReaderNode.getNode().getConfiguration();
        final DomElement config = newReaderNode.getNode().getConfiguration();
        final DomElement fileParam = new XppDomElement("file");
        fileParam.setValue(value);
        config.addChild(fileParam);

        switchConnections(sourceNode, newReaderNode.getID());
    }

    private void switchConnections(final GraphNode oldNode, final String newNodeID) {
        final GraphNode[] connectedNodes = findConnectedNodes(oldNode);
        for(GraphNode node : connectedNodes) {
            node.connectOperatorSource(newNodeID);
        }
    }

    public List<File> getProductsToOpenInDAT() {
        final List<File> fileList = new ArrayList<File>(2);
        final Node[] nodes = graph.getNodes();
        for(Node n : nodes) {
            if(n.getOperatorName().equalsIgnoreCase(OperatorSpi.getOperatorAlias(WriteOp.class))) {
                final DomElement config = n.getConfiguration();
                final DomElement fileParam = config.getChild("file");
                final String filePath = fileParam.getValue();
                if(filePath != null && !filePath.isEmpty()) {
                    final File file = new File(filePath);
                    if(file.exists()) {
                        fileList.add(file);
                    } else {
                        final DomElement formatParam = config.getChild("formatName");
                        final String format = formatParam.getValue();

                        final String ext = ReaderUtils.findExtensionForFormat(format);
                       
                        File newFile = new File(file.getAbsolutePath()+ext);
                        if(newFile.exists()) {
                            fileList.add(newFile);
                        } else {
                            final String name = FileUtils.getFilenameWithoutExtension(file);
                            newFile = new File(name+ext);
                            if(newFile.exists())
                                fileList.add(newFile);
                        }
                    }
                }
            }
        }
        return fileList;
    }

    /**
     * Update the nodes in the graph with the given reader file and writer file
     * @param graphEx
     * @param readID
     * @param readPath
     * @param writeID
     * @param writePath
     * @param format
     */
    public static void setGraphIO(final GraphExecuter graphEx,
                              final String readID, final File readPath,
                              final String writeID, final File writePath,
                              final String format) {
        final GraphNode readNode = graphEx.findGraphNode(readID);
        if (readNode != null) {
            graphEx.setOperatorParam(readNode.getID(), "file", readPath.getAbsolutePath());
        }

        if (writeID != null) {
            final GraphNode writeNode = graphEx.findGraphNode(writeID);
            if (writeNode != null) {
                graphEx.setOperatorParam(writeNode.getID(), "formatName", format);
                graphEx.setOperatorParam(writeNode.getID(), "file", writePath.getAbsolutePath());
            }
        }
    }

    private static class ProductSetData {
        String nodeID = null;
        final List<String> fileList = new ArrayList<String>(10);
    }

    public static class GraphEvent {

        private final events  eventType;
        private final Object  data;

        GraphEvent(events type, Object d) {
            eventType = type;
            data = d;
        }

        public Object getData() {
            return data;
        }

        public events getEventType() {
            return eventType;
        }
    }
}
