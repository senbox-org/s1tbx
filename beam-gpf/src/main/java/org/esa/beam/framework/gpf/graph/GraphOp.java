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

package org.esa.beam.framework.gpf.graph;

import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.internal.OperatorConfiguration;
import org.esa.beam.framework.gpf.internal.OperatorContext;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@OperatorMetadata(alias = "Graph",
                  description = "Encapsulates a graph into an operator.",
                  internal = true)
public class GraphOp extends Operator {

    private Operator operator;
    private OperatorContext operatorContext;

    private static void initNodeDependencies(GraphContext graphContext)
            throws GraphException {
        Graph graph = graphContext.getGraph();

        for (Node node : graph.getNodes()) {
            for (NodeSource source : node.getSources()) {
                String sourceNodeId = source.getSourceNodeId();
                Node sourceNode = graph.getNode(sourceNodeId);
                if (sourceNode == null
                        && !isSourceNodeIdInHeader(sourceNodeId, graph
                        .getHeader().getSources())) {
                    throw new GraphException("Missing source. Node Id: '"
                            + node.getId() + "' Source Id: '"
                            + source.getSourceNodeId() + "'");
                }
                if (sourceNode != null) {
                    graphContext.getNodeContext(sourceNode)
                            .incrementReferenceCount();
                    source.setSourceNode(sourceNode);
                }
            }
        }
    }

    private static boolean isSourceNodeIdInHeader(String sourceNodeId,
                                                  List<HeaderSource> headerSources) {
        for (HeaderSource headerSource : headerSources) {
            if (sourceNodeId.equals(headerSource.getName())) {
                return true;
            }
        }

        return false;
    }

    private void initOutput(GraphContext graphContext, ProgressMonitor pm)
            throws GraphException {
        final int outputCount = graphContext.getOutputCount();
        try {
            pm.beginTask("Creating output products", outputCount);
            for (Node node : graphContext.getGraph().getNodes()) {
                NodeContext nodeContext = graphContext.getNodeContext(node);
                if (nodeContext.isOutput()) {
                    initNodeContext(graphContext, nodeContext,
                                    SubProgressMonitor.create(pm, 1));
                    graphContext.addOutputNodeContext(nodeContext);
                }
            }
        } finally {
            pm.done();
        }
    }

    private void initNodeContext(GraphContext graphContext,
                                 final NodeContext nodeContext, ProgressMonitor pm)
            throws GraphException {
        try {
            NodeSource[] sources = nodeContext.getNode().getSources();

            pm.beginTask("Creating operator", sources.length + 4);

            if (nodeContext.isInitialized()) {
                return;
            }

            for (NodeSource source : sources) {
                NodeContext sourceNodeContext = graphContext
                        .getNodeContext(source.getSourceNode());
                if (sourceNodeContext != null) {
                    initNodeContext(graphContext, sourceNodeContext,
                                    SubProgressMonitor.create(pm, 1));
                    nodeContext.addSourceProduct(source.getName(),
                                                 sourceNodeContext.getTargetProduct());
                } else {
                    Product product = getSourceProduct(source.getSourceNodeId());
                    nodeContext.addSourceProduct(source.getName(), product);
                }
            }
            Node node = nodeContext.getNode();
            DomElement configuration = node.getConfiguration();
            OperatorConfiguration opConfiguration = GraphContext.createOperatorConfiguration(configuration,
                                                                                             graphContext,
                                                                                             operatorContext.getParameters());
            nodeContext.setParameters(opConfiguration);
            nodeContext.initTargetProduct();
            graphContext.getInitNodeContextDeque().addFirst(nodeContext);

        } catch (GraphException e) {
            throw e;
        } catch (Exception e) {
            throw new GraphException(e.getMessage(), e);
        } finally {
            pm.done();
        }
    }

    private void fillMapFromConfiguration(Map<String, Object> parameters) {
        parameters.put("THR", 66.0);

//        Xpp3Dom configuration = operatorContext.configuration.getConfiguration();
//        final Xpp3DomElement xpp3DomElement = Xpp3DomElement.createDomElement(configuration);
//        
//        ParameterDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory();
//        final DefaultDomConverter domConverter = new DefaultDomConverter(value.getClass(), parameterDescriptorFactory);
//        domConverter.convertDomToValue(xpp3DomElement, value);
    }

    @Override
    public void initialize() throws OperatorException {
        try {
            GraphOp.Spi spi = (Spi) getSpi();
            Graph graph = spi.graph;

            operatorContext = getOperatorContext();

            Map<String, Object> parameters = operatorContext.getParameters();
            if (parameters == null) {
                parameters = new HashMap<String, Object>();
            }
            fillMapFromConfiguration(parameters);
            operatorContext.setParameters(parameters);

            GraphContext graphContext = new GraphContext(graph, Logger
                    .getAnonymousLogger());
            initNodeDependencies(graphContext);
            initOutput(graphContext, ProgressMonitor.NULL);

            HeaderTarget headerTarget = graph.getHeader().getTarget();
            Node targetNode = graph.getNode(headerTarget.getNodeId());
            NodeContext targetNodeContext = null;

            for (Node node : graph.getNodes()) {
                if (node == targetNode) {
                    targetNodeContext = graphContext.getNodeContext(node);
                    break;
                }
            }
            if (targetNodeContext == null) {
                throw new OperatorException("No target node found...");
            }

//            operatorContext.passThrough = true;
            enablePassThrough();
            operator = targetNodeContext.getOperator();
            setTargetProduct(operator.getTargetProduct());
        } catch (GraphException e) {
            throw new OperatorException(e);
        }
    }

    // TODO make oc protected ???
    private OperatorContext getOperatorContext() {
        OperatorContext operatorContext;

        try {
            Field field = Operator.class.getDeclaredField("context");
            field.setAccessible(true);
            operatorContext = (OperatorContext) field.get(this);
            field.setAccessible(false);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }

        return operatorContext;
    }

    private void enablePassThrough() {
        try {
            Field field = OperatorContext.class.getDeclaredField("passThrough");
            field.setAccessible(true);
            field.set(operatorContext, true);
            field.setAccessible(false);
        } catch (IllegalAccessException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }

    public static class Spi extends OperatorSpi {

        private final Graph graph;

        public Spi(Graph graph) {
            super(GraphOp.class, graph.getId());
            this.graph = graph;
        }
    }
}
