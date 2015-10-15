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

package org.esa.snap.core.gpf.graph;

import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.internal.OperatorConfiguration;
import org.esa.snap.core.util.SystemUtils;

import javax.media.jai.JAI;
import java.awt.Dimension;
import java.text.MessageFormat;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Logger;

/**
 * The {@code GraphContext} holds the context for executing the {@link Graph} by the {@link GraphProcessor}.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @see Graph
 * @see GraphProcessor
 * @since 4.1
 */
public class GraphContext {

    private Graph graph;
    private Logger logger;
    private Map<Node, NodeContext> nodeContextMap;
    private List<NodeContext> outputNodeContextList;
    private ArrayDeque<NodeContext> initNodeContextDeque;


    /**
     * Creates a GraphContext for the given {@code graph} and a {@code logger}.
     *
     * @param graph the {@link Graph} to create the context for
     * @throws GraphException if the graph context could not be created
     */
    public GraphContext(Graph graph) throws GraphException {
        this(graph, null);
    }


    /**
     * Creates a GraphContext for the given {@code graph} and a {@code logger}.
     *
     * @param graph the {@link Graph} to create the context for
     * @throws GraphException if the graph context could not be created
     */
    public GraphContext(Graph graph, Operator graphOp) throws GraphException {
        if (graph.getNodeCount() == 0) {
            throw new GraphException("Empty graph.");
        }

        this.graph = graph;
        this.logger = SystemUtils.LOG;

        outputNodeContextList = new ArrayList<>(graph.getNodeCount() / 2);

        nodeContextMap = new HashMap<>(graph.getNodeCount() * 2);
        for (Node node : graph.getNodes()) {
            nodeContextMap.put(node, new NodeContext(this, node));
        }

        initNodeContextDeque = new ArrayDeque<>(graph.getNodeCount());
        initNodeDependencies();
        initOutput(graphOp);
    }

    private static boolean isSourceNodeIdInHeader(String sourceNodeId, List<HeaderSource> headerSources) {
        for (HeaderSource headerSource : headerSources) {
            if (sourceNodeId.equals(headerSource.getName())) {
                return true;
            }
        }
        return false;
    }

    private void initNodeDependencies() throws GraphException {
        Graph graph = getGraph();
        for (Node node : graph.getNodes()) {
            for (NodeSource source : node.getSources()) {
                String sourceNodeId = source.getSourceNodeId();
                Node sourceNode = graph.getNode(sourceNodeId);
                if (sourceNode == null) {
                    Header header = graph.getHeader();
                    boolean sourceDefinedInHeader = header != null && isSourceNodeIdInHeader(sourceNodeId, header.getSources());
                    if (!sourceDefinedInHeader) {
                        throw new GraphException(getMissingSourceMessage(node, source));
                    }
                }
                if (sourceNode != null) {
                    getNodeContext(sourceNode).incrementReferenceCount();
                    source.setSourceNode(sourceNode);
                }
            }
        }
    }

    OperatorConfiguration createOperatorConfiguration(DomElement domElement,
                                                      Map<String, Object> parameterContext) {
        if (domElement == null) {
            return null;
        }
        DomElement resolvedElement = new XppDomElement(domElement.getName());
        Set<OperatorConfiguration.Reference> references = new HashSet<>(17);
        DomElement[] children = domElement.getChildren();

        for (DomElement child : children) {
            String reference = child.getAttribute("refid");
            if (reference != null) {
                String parameterName = child.getName();
                if (reference.contains(".")) {
                    String[] referenceParts = reference.split("\\.");
                    String referenceNodeId = referenceParts[0];
                    String propertyName = referenceParts[1];
                    Node node = getGraph().getNode(referenceNodeId);
                    NodeContext referredNodeContext = getNodeContext(node);
                    Operator operator = referredNodeContext.getOperator();
                    OperatorConfiguration.PropertyReference propertyReference = new OperatorConfiguration.PropertyReference(parameterName, propertyName, operator);
                    references.add(propertyReference);
                } else {
                    OperatorConfiguration.ParameterReference parameterReference = new OperatorConfiguration.ParameterReference(parameterName, parameterContext.get(reference));
                    references.add(parameterReference);
                }
            } else {
                resolvedElement.addChild(child);
            }
        }

        return new OperatorConfiguration(resolvedElement, references);
    }

    private void initOutput(Operator graphOp) throws GraphException {
        for (Node node : getGraph().getNodes()) {
            NodeContext nodeContext = getNodeContext(node);
            if (nodeContext.isOutput()) {
                initNodeContext(nodeContext, graphOp);
                addOutputNodeContext(nodeContext);
            }
        }
    }

    private void initNodeContext(final NodeContext nodeContext, Operator graphOp) throws GraphException {

        if (nodeContext.isInitialized()) {
            return;
        }

        for (NodeSource source : nodeContext.getNode().getSources()) {
            NodeContext sourceNodeContext = getNodeContext(source.getSourceNode());
            Product sourceProduct = null;
            if (sourceNodeContext != null) {
                initNodeContext(sourceNodeContext, graphOp);
                sourceProduct = sourceNodeContext.getTargetProduct();
            } else {
                if (graphOp != null) {
                    sourceProduct = graphOp.getSourceProduct(source.getSourceNodeId());
                }
            }
            if (sourceProduct == null) {
                throw new GraphException(getMissingSourceMessage(nodeContext.getNode(), source));
            }
            nodeContext.addSourceProduct(source.getName(), sourceProduct);
        }
        Node node = nodeContext.getNode();
        DomElement configuration = node.getConfiguration();
        OperatorConfiguration opConfiguration = this.createOperatorConfiguration(configuration,
                                                                                 new HashMap<String, Object>());
        nodeContext.setOperatorConfiguration(opConfiguration);
        nodeContext.initTargetProduct();
        getInitNodeContextDeque().addFirst(nodeContext);
    }

    /**
     * Disposes this {@code GraphContext}.
     */
    public void dispose() {
        Deque<NodeContext> initNodeContextDeque = getInitNodeContextDeque();
        while (!initNodeContextDeque.isEmpty()) {
            NodeContext nodeContext = initNodeContextDeque.pop();
            nodeContext.dispose();
        }
    }

    /**
     * Gets the {@link Graph} of this context.
     *
     * @return the {@link Graph}
     */
    public Graph getGraph() {
        return graph;
    }

    /**
     * Gets the @link Logger} of this context.
     *
     * @return the logger
     */
    public Logger getLogger() {
        return logger;
    }

    /**
     * Sets the preferred tile size.
     *
     * @param preferredTileSize the preferred tile size
     */
    public void setPreferredTileSize(Dimension preferredTileSize) {
        JAI.setDefaultTileSize(preferredTileSize);
    }

    /**
     * Returns an array containing the output products generated by this graph's output
     * nodes, i.e. nodes that are not input to other nodes.
     *
     * @return an array containing the output products of this graph
     */
    public Product[] getOutputProducts() {
        Product[] products = new Product[outputNodeContextList.size()];
        for (int i = 0; i < products.length; i++) {
            products[i] = outputNodeContextList.get(i).getTargetProduct();
        }
        return products;
    }

    /**
     * Gets the {@link NodeContext}s in the reverse order as they were initialized.
     *
     * @return a deque of {@link NodeContext}s
     */
    public Deque<NodeContext> getInitNodeContextDeque() {
        return initNodeContextDeque;
    }

    /**
     * Gets the {@link NodeContext} of the given node.
     *
     * @param node the node to get the context for
     * @return the {@link NodeContext} of the given {@code node} or
     *         {@code null} if it's not contained in this context
     */
    public NodeContext getNodeContext(Node node) {
        return nodeContextMap.get(node);
    }

    /**
     * Gets all output {@link NodeContext}s of this {@code GraphContext}
     *
     * @return an array of all output {@link NodeContext}s
     */
    NodeContext[] getOutputNodeContexts() {
        return outputNodeContextList.toArray(new NodeContext[outputNodeContextList.size()]);
    }


    /**
     * Adds the given {@code nodeContext} to the list of output {@link NodeContext}s.
     *
     * @param nodeContext the {@link NodeContext} to add as output
     */
    void addOutputNodeContext(NodeContext nodeContext) {
        outputNodeContextList.add(nodeContext);
    }


    private static String getMissingSourceMessage(Node node, NodeSource source) {
        return MessageFormat.format("Missing source ''{0}'' in node ''{1}''",
                                    source.getSourceNodeId(), node.getId());
    }
}
