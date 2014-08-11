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
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.graph.*;
import org.esa.snap.gpf.SubGraphOp;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Replaces SubGraphOp with operators from the sub-graph
 */
public class SubGraphHandler {

    private final Graph graph;
    private final GraphNodeList graphNodeList;
    private final GraphNode[] savedSubGraphList;
    private GraphNode[] nodesToRemove;

    public SubGraphHandler(final Graph graph, final GraphNodeList graphNodeList) throws GraphException {
        this.graph = graph;
        this.graphNodeList = graphNodeList;

        this.savedSubGraphList = replaceAllSubGraphs();
    }

    private GraphNode[] replaceAllSubGraphs() throws GraphException {
        final SubGraphData[] dataList = findSubGraphs(OperatorSpi.getOperatorAlias(SubGraphOp.class));
        final List<GraphNode> savedList = new ArrayList<>();

        for (SubGraphData data : dataList) {
            final GraphNode sourceNode = graphNodeList.findGraphNode(data.nodeID);
            if (data.subGraph != null) {
                replaceSubGraph(sourceNode, data.subGraph);

                removeNode(sourceNode);
                savedList.add(sourceNode);
            }
        }
        return savedList.toArray(new GraphNode[savedList.size()]);
    }

    private SubGraphData[] findSubGraphs(final String opName) throws GraphException {
        try {
            final List<SubGraphData> dataList = new ArrayList<>();
            for (Node n : graph.getNodes()) {
                if (n.getOperatorName().equalsIgnoreCase(opName)) {
                    final SubGraphData data = new SubGraphData();
                    data.nodeID = n.getId();

                    final DomElement config = n.getConfiguration();
                    final DomElement[] params = config.getChildren();
                    for (DomElement p : params) {
                        if (p.getName().equals("graphFile") && p.getValue() != null) {

                            data.subGraph = GraphIO.read(new FileReader(p.getValue()));
                            break;
                        }
                    }
                    dataList.add(data);
                }
            }
            return dataList.toArray(new SubGraphData[dataList.size()]);
        } catch (Exception e) {
            throw new GraphException(e.getMessage(), e);
        }
    }

    public void restore() {
        for (GraphNode savedNode : savedSubGraphList) {

            for (GraphNode n : nodesToRemove) {
                graphNodeList.switchConnections(n, savedNode.getID());
                removeNode(n);
            }

            graphNodeList.add(savedNode);
            graph.addNode(savedNode.getNode());
        }
    }

    private void replaceSubGraph(final GraphNode subGraphOpNode, final Graph subGraph) {

        final List<GraphNode> toRemove = new ArrayList<>();
        final Node[] nodes = subGraph.getNodes();
        final Node firstNode = nodes[0];
        final Node lastNode = nodes[nodes.length - 1];
        final GraphNode[] connectedNodes = graphNodeList.findConnectedNodes(subGraphOpNode);

        final NodeSource[] sources = subGraphOpNode.getNode().getSources();
        for (NodeSource source : sources) {

            final Map<String, Node> subGraphNodeMap = new HashMap<>();
            for (Node subNode : nodes) {
                final Node newNode = new Node(source.getSourceNodeId() + subNode.getId(), subNode.getOperatorName());
                subGraphNodeMap.put(subNode.getId(), newNode);
                newNode.setConfiguration(subNode.getConfiguration());

                graph.addNode(newNode);
                GraphNode newGraphNode = new GraphNode(newNode);
                graphNodeList.add(newGraphNode);
                toRemove.add(newGraphNode);

                for (NodeSource ns : subNode.getSources()) {
                    Node src = subGraphNodeMap.get(ns.getSourceNodeId());
                    if (src != null) {
                        newGraphNode.connectOperatorSource(src.getId());
                    }
                }

                if (subNode == firstNode) {
                    newNode.addSource(source);
                } else if (subNode == lastNode) {
                    // switch connections
                    for (GraphNode node : connectedNodes) {
                        node.connectOperatorSource(newNode.getId());
                    }
                }
            }
        }
        nodesToRemove = toRemove.toArray(new GraphNode[toRemove.size()]);
    }

    private void removeNode(final GraphNode node) {
        graphNodeList.remove(node);
        graph.removeNode(node.getID());
    }

    private static class SubGraphData {
        String nodeID = null;
        Graph subGraph = null;
    }
}
