/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dat.graphbuilder;

import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import com.bc.ceres.core.ProgressMonitor;
import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.graph.*;
import org.esa.beam.framework.ui.BasicApp;
import org.esa.beam.gpf.operators.standard.WriteOp;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;
import org.esa.beam.visat.VisatApp;
import org.esa.snap.gpf.GPFProcessor;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.gpf.ui.DefaultUI;
import org.esa.snap.gpf.ui.OperatorUI;
import org.esa.snap.gpf.ui.OperatorUIDescriptor;
import org.esa.snap.gpf.ui.OperatorUIRegistry;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Observable;
import java.util.Set;

public class GraphExecuter extends Observable {

    private final GPF gpf;
    private Graph graph;
    private GraphContext graphContext = null;
    private GraphProcessor processor;
    private String graphDescription = "";
    private File lastLoadedGraphFile = null;

    private int idCount = 0;
    private final GraphNodeList graphNodeList = new GraphNodeList();

    public enum events {ADD_EVENT, REMOVE_EVENT, SELECT_EVENT}

    public GraphExecuter() {

        gpf = GPF.getDefaultInstance();
        gpf.getOperatorSpiRegistry().loadOperatorSpis();

        graph = new Graph("Graph");
    }

    public List<GraphNode> GetGraphNodes() {
        return graphNodeList.getGraphNodes();
    }

    public GraphNodeList getGraphNodeList() {
        return graphNodeList;
    }

    public void ClearGraph() {
        graph = null;
        graph = new Graph("Graph");
        lastLoadedGraphFile = null;
        graphNodeList.clear();
        idCount = 0;
    }

    public void setSelectedNode(GraphNode node) {
        if (node == null) return;
        setChanged();
        notifyObservers(new GraphEvent(events.SELECT_EVENT, node));
        clearChanged();
    }

    /**
     * Gets the list of operators
     *
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
        if (operatorMetadata != null)
            return operatorMetadata.category();
        return "";
    }

    public GraphNode addOperator(final String opName) {

        String id = opName;
        int cnt = 1;
        while (graphNodeList.findGraphNode(id) != null) {
            ++cnt;
            id = opName + '(' + cnt + ')';
        }
        final GraphNode newGraphNode = createNewGraphNode(graph, graphNodeList, opName, id);

        setChanged();
        notifyObservers(new GraphEvent(events.ADD_EVENT, newGraphNode));
        clearChanged();

        return newGraphNode;
    }

    static GraphNode createNewGraphNode(final Graph graph, final GraphNodeList graphNodeList,
                                        final String opName, final String id) {
        final Node newNode = new Node(id, opName);

        final XppDomElement parameters = new XppDomElement("parameters");
        newNode.setConfiguration(parameters);

        graph.addNode(newNode);

        final GraphNode newGraphNode = new GraphNode(newNode);
        graphNodeList.add(newGraphNode);

        newGraphNode.setOperatorUI(CreateOperatorUI(newGraphNode.getOperatorName()));

        return newGraphNode;
    }

    public static OperatorUI CreateOperatorUI(final String operatorName) {

        final OperatorUIRegistry reg = OperatorUIRegistry.getInstance();
        if (reg != null) {
            OperatorUIDescriptor desc = reg.getOperatorUIDescriptor(operatorName);
            if (desc != null) {
                return desc.createOperatorUI();
            }
            desc = OperatorUIRegistry.getInstance().getOperatorUIDescriptor("DefaultUI");
            if (desc != null) {
                return desc.createOperatorUI();
            }
        }
        return new DefaultUI();
    }

    public void removeOperator(final GraphNode node) {

        setChanged();
        notifyObservers(new GraphEvent(events.REMOVE_EVENT, node));
        clearChanged();

        removeNode(node);
    }

    private void removeNode(final GraphNode node) {
        graphNodeList.remove(node);
        graph.removeNode(node.getID());
    }

    public void setOperatorParam(final String id, final String paramName, final String value) {
        final Node node = graph.getNode(id);
        DomElement xml = node.getConfiguration().getChild(paramName);
        if (xml == null) {
            xml = new XppDomElement(paramName);
            node.getConfiguration().addChild(xml);
        }
        xml.setValue(value);
    }

    private void AssignAllParameters() throws GraphException {

        final XppDom presentationXML = new XppDom("Presentation");

        // save graph description
        final XppDom descXML = new XppDom("Description");
        descXML.setValue(graphDescription);
        presentationXML.addChild(descXML);

        graphNodeList.assignParameters(presentationXML);
        graph.setAppData("Presentation", presentationXML);
    }

    public boolean InitGraph() throws GraphException {
        if (graphNodeList.isGraphComplete()) {
            AssignAllParameters();

            ProductSetHandler productSetHandler = new ProductSetHandler(graph, graphNodeList);
            SubGraphHandler subGraphHandler = new SubGraphHandler(graph, graphNodeList);

            try {
                recreateGraphContext();
                graphNodeList.updateGraphNodes(graphContext);
                //todo recreateGraphContext();
            } catch (Exception e) {
                e.printStackTrace();
                throw new GraphException(e.getMessage());
            } finally {
                subGraphHandler.restore();
                productSetHandler.restore();
            }
            return true;
        }
        return false;
    }

    private void recreateGraphContext() throws GraphException {
        if (graphContext != null)
            graphContext.dispose();

        processor = new GraphProcessor();
        graphContext = new GraphContext(graph);
    }

    public void disposeGraphContext() {
        graphContext.dispose();
    }

    /**
     * Begins graph processing
     *
     * @param pm The ProgressMonitor
     */
    public void executeGraph(ProgressMonitor pm) {
        processor.executeGraph(graphContext, pm);
    }

    File saveGraph() throws GraphException {

        String filename = "myGraph";
        if (lastLoadedGraphFile != null)
            filename = lastLoadedGraphFile.getAbsolutePath();
        final BeamFileFilter fileFilter = new BeamFileFilter("XML", "xml", "Graph");
        final File filePath = VisatApp.getApp().showFileSaveDialog("Save Graph", false, fileFilter, ".xml", filename,
                BasicApp.PROPERTY_KEY_APP_LAST_SAVE_DIR);
        if (filePath != null)
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
        } catch (Exception e) {
            throw new GraphException("Unable to write graph to " + filePath + '\n' + e.getMessage());
        }
    }

    public void loadGraph(final File filePath, final boolean addUI) throws GraphException {

        try {
            if (filePath == null) return;
            final Graph graphFromFile = GPFProcessor.readGraph(filePath, null);

            setGraph(graphFromFile, addUI);
            lastLoadedGraphFile = filePath;
        } catch (Throwable e) {
            throw new GraphException("Unable to load graph " + filePath + '\n' + e.getMessage());
        }
    }

    public void setGraph(final Graph graphFromFile, final boolean addUI) throws GraphException {
        if (graphFromFile != null) {
            graph = graphFromFile;
            graphNodeList.clear();

            final XppDom presentationXML = graph.getApplicationData("Presentation");
            if (presentationXML != null) {
                // get graph description
                final XppDom descXML = presentationXML.getChild("Description");
                if (descXML != null && descXML.getValue() != null) {
                    graphDescription = descXML.getValue();
                }
            }

            final Node[] nodes = graph.getNodes();
            for (Node n : nodes) {
                final GraphNode newGraphNode = new GraphNode(n);
                if (presentationXML != null)
                    newGraphNode.setDisplayParameters(presentationXML);
                graphNodeList.add(newGraphNode);

                if (addUI) {
                    OperatorUI ui = CreateOperatorUI(newGraphNode.getOperatorName());
                    if (ui == null) {
                        throw new GraphException("Unable to load " + newGraphNode.getOperatorName());
                    }
                    newGraphNode.setOperatorUI(ui);
                }
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

    public List<File> getProductsToOpenInDAT() {
        final List<File> fileList = new ArrayList<File>(2);
        final Node[] nodes = graph.getNodes();
        for (Node n : nodes) {
            if (n.getOperatorName().equalsIgnoreCase(OperatorSpi.getOperatorAlias(WriteOp.class))) {
                final DomElement config = n.getConfiguration();
                final DomElement fileParam = config.getChild("file");
                final String filePath = fileParam.getValue();
                if (filePath != null && !filePath.isEmpty()) {
                    final File file = new File(filePath);
                    if (file.exists()) {
                        fileList.add(file);
                    } else {
                        final DomElement formatParam = config.getChild("formatName");
                        final String format = formatParam.getValue();

                        final String ext = ReaderUtils.findExtensionForFormat(format);

                        File newFile = new File(file.getAbsolutePath() + ext);
                        if (newFile.exists()) {
                            fileList.add(newFile);
                        } else {
                            final String name = FileUtils.getFilenameWithoutExtension(file);
                            newFile = new File(name + ext);
                            if (newFile.exists())
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
     *
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
        final GraphNode readNode = graphEx.getGraphNodeList().findGraphNode(readID);
        if (readNode != null) {
            graphEx.setOperatorParam(readNode.getID(), "file", readPath.getAbsolutePath());
        }

        if (writeID != null) {
            final GraphNode writeNode = graphEx.getGraphNodeList().findGraphNode(writeID);
            if (writeNode != null) {
                graphEx.setOperatorParam(writeNode.getID(), "formatName", format);
                graphEx.setOperatorParam(writeNode.getID(), "file", writePath.getAbsolutePath());
            }
        }
    }

    public static class GraphEvent {

        private final events eventType;
        private final Object data;

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
