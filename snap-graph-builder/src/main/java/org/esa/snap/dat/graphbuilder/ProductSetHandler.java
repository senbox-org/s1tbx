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
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.graph.Graph;
import org.esa.beam.framework.gpf.graph.Node;
import org.esa.beam.gpf.operators.standard.ReadOp;
import org.esa.snap.gpf.ProductSetReaderOp;

import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

/**
 * Replaces ProductSetReader with ReadOp
 */
public class ProductSetHandler {

    private final Graph graph;
    private final GraphNodeList graphNodeList;
    private final GraphNode[] savedProductSetList;

    public ProductSetHandler(final Graph graph, final GraphNodeList graphNodeList) {
        this.graph = graph;
        this.graphNodeList = graphNodeList;

        this.savedProductSetList = replaceProductSetReaders();
    }

    private GraphNode[] replaceProductSetReaders() {
        final ProductSetData[] productSetDataList =
                findProductSets(OperatorSpi.getOperatorAlias(ProductSetReaderOp.class));
        final List<GraphNode> savedProductSetList = new ArrayList<>();

        int cnt = 0;
        for (ProductSetData psData : productSetDataList) {
            final GraphNode sourceNode = graphNodeList.findGraphNode(psData.nodeID);
            for (String filePath : psData.fileList) {

                replaceProductSetWithReaders(sourceNode, "inserted--" + sourceNode.getID() + "--" + cnt++, filePath);
            }
            if (!psData.fileList.isEmpty()) {
                removeNode(sourceNode);
                savedProductSetList.add(sourceNode);
            }
        }
        return savedProductSetList.toArray(new GraphNode[savedProductSetList.size()]);
    }

    private ProductSetData[] findProductSets(final String readerName) {
        final String SEPARATOR = ",";
        final String SEPARATOR_ESC = "\\u002C"; // Unicode escape repr. of ','
        final List<ProductSetData> productSetDataList = new ArrayList<>();

        for (Node n : graph.getNodes()) {
            if (n.getOperatorName().equalsIgnoreCase(readerName)) {
                final ProductSetData psData = new ProductSetData();
                psData.nodeID = n.getId();

                final DomElement config = n.getConfiguration();
                final DomElement[] params = config.getChildren();
                for (DomElement p : params) {
                    if (p.getName().equals("fileList") && p.getValue() != null) {

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

    public void restore() {
        for (GraphNode multiSrcNode : savedProductSetList) {

            final List<GraphNode> nodesToRemove = new ArrayList<>();
            final List<GraphNode> nodeList = graphNodeList.getGraphNodes();
            for (GraphNode n : nodeList) {
                final String id = n.getID();
                if (id.startsWith("inserted--" + multiSrcNode.getID()) && id.contains(multiSrcNode.getID())) {

                    graphNodeList.switchConnections(n, multiSrcNode.getID());
                    nodesToRemove.add(n);
                }
            }
            for (GraphNode r : nodesToRemove) {
                removeNode(r);
            }

            graphNodeList.add(multiSrcNode);
            graph.addNode(multiSrcNode.getNode());
        }
    }

    private void replaceProductSetWithReaders(final GraphNode sourceNode, final String id, final String value) {

        GraphNode newReaderNode = GraphExecuter.createNewGraphNode(graph, graphNodeList,
                OperatorSpi.getOperatorAlias(ReadOp.class), id);
        newReaderNode.setOperatorUI(null);
        final DomElement config = newReaderNode.getNode().getConfiguration();
        final DomElement fileParam = new XppDomElement("file");
        fileParam.setValue(value);
        config.addChild(fileParam);

        graphNodeList.switchConnections(sourceNode, newReaderNode.getID());
    }

    private void removeNode(final GraphNode node) {
        graphNodeList.remove(node);
        graph.removeNode(node.getID());
    }

    private static class ProductSetData {
        String nodeID = null;
        final List<String> fileList = new ArrayList<>(10);
    }
}
