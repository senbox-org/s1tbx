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

import com.thoughtworks.xstream.io.xml.xppdom.XppDom;
import org.esa.beam.framework.gpf.graph.GraphContext;
import org.esa.beam.framework.gpf.graph.GraphException;
import org.esa.beam.framework.gpf.graph.NodeContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Keeps track of GraphNodes
 */
public class GraphNodeList {

    private final List<GraphNode> nodeList = new ArrayList<>(10);

    List<GraphNode> getGraphNodes() {
        return nodeList;
    }

    void clear() {
        nodeList.clear();
    }

    public void add(final GraphNode newGraphNode) {
        nodeList.add(newGraphNode);
    }

    public void remove(final GraphNode node) {
        // remove as a source from all nodes
        for (GraphNode n : nodeList) {
            n.disconnectOperatorSources(node.getID());
        }

        nodeList.remove(node);
    }

    public GraphNode findGraphNode(String id) {
        for (GraphNode n : nodeList) {
            if (n.getID().equals(id)) {
                return n;
            }
        }
        return null;
    }

    public GraphNode findGraphNodeByOperator(String operatorName) {
        for (GraphNode n : nodeList) {
            if (n.getOperatorName().equals(operatorName)) {
                return n;
            }
        }
        return null;
    }

    boolean isGraphComplete() {
        int nodesWithoutSources = 0;
        for (GraphNode n : nodeList) {
            if (!n.HasSources()) {
                ++nodesWithoutSources;
                if (!IsNodeASource(n))
                    return false;
            }
        }
        return nodesWithoutSources != nodeList.size();
    }

    public void assignParameters(final XppDom presentationXML) throws GraphException {
        for (GraphNode n : nodeList) {
            if (n.GetOperatorUI() != null) {
                n.AssignParameters(presentationXML);
            }
        }
    }

    void updateGraphNodes(final GraphContext graphContext) throws GraphException {
        if (graphContext != null) {
            for (GraphNode n : nodeList) {
                final NodeContext context = graphContext.getNodeContext(n.getNode());
                n.setSourceProducts(context.getSourceProducts());
                n.updateParameters();
            }
        }
    }

    public boolean IsNodeASource(final GraphNode sourceNode) {
        for (GraphNode n : nodeList) {
            if (n.isNodeSource(sourceNode))
                return true;
        }
        return false;
    }

    public GraphNode[] findConnectedNodes(final GraphNode sourceNode) {
        final List<GraphNode> connectedNodes = new ArrayList<GraphNode>();
        for (GraphNode n : nodeList) {
            if (n.isNodeSource(sourceNode))
                connectedNodes.add(n);
        }
        return connectedNodes.toArray(new GraphNode[connectedNodes.size()]);
    }

    public void switchConnections(final GraphNode oldNode, final String newNodeID) {
        final GraphNode[] connectedNodes = findConnectedNodes(oldNode);
        for (GraphNode node : connectedNodes) {
            node.connectOperatorSource(newNodeID);
        }
    }
}
