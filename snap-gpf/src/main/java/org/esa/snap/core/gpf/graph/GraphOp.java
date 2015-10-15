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

import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.internal.OperatorContext;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@OperatorMetadata(alias = "Graph",
                  description = "Encapsulates a graph into an operator.",
                  internal = true)
public class GraphOp extends Operator {

    @Override
    public void initialize() throws OperatorException {
        try {
            Graph graph = ((Spi) getSpi()).graph;
            OperatorContext operatorContext = getOperatorContext();
            setOperatorParameters(graph, operatorContext);
            NodeContext targetNodeContext = getTargetNodeContext(graph);
            if (targetNodeContext == null) {
                throw new OperatorException("No target node found...");
            }
            setTargetProduct(targetNodeContext.getOperator().getTargetProduct());
        } catch (GraphException e) {
            throw new OperatorException(e);
        }
    }

    private void setOperatorParameters(Graph graph, OperatorContext operatorContext) {
        // todo - implement me, this code here is not adequate - we need the graph's header in order to get the GraphOp's parameters
        Map<String, Object> parameters = operatorContext.getParameterMap();
        if (parameters == null) {
            parameters = new HashMap<String, Object>();
        }
        List<HeaderParameter> parameters1 = graph.getHeader().getParameters();
        for (HeaderParameter headerParameter : parameters1) {
            //TODO use them, convert them.....
        }
        fillMapFromConfiguration(parameters);
        operatorContext.setParameterMap(parameters);
    }

    private void fillMapFromConfiguration(Map<String, Object> parameters) {
//        parameters.put("THR", 66.0);

//        Xpp3Dom configuration = operatorContext.configuration.getConfiguration();
//        final XppDomElement xpp3DomElement = XppDomElement.createDomElement(configuration);
//
//        ParameterDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory();
//        final DefaultDomConverter domConverter = new DefaultDomConverter(value.getClass(), parameterDescriptorFactory);
//        domConverter.convertDomToValue(xpp3DomElement, value);
    }

    private NodeContext getTargetNodeContext(Graph graph) throws GraphException {
        GraphContext graphContext = new GraphContext(graph, this);

        NodeContext targetNodeContext = null;
        Header header = graph.getHeader();
        if (header != null) {
            HeaderTarget headerTarget = header.getTarget();
            Node targetNode = graph.getNode(headerTarget.getNodeId());
            for (Node node : graph.getNodes()) {
                if (node == targetNode) {
                    targetNodeContext = graphContext.getNodeContext(node);
                    break;
                }
            }
        }
        return targetNodeContext;
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

    public abstract static class Spi extends OperatorSpi {

        private final Graph graph;

        public Spi(Graph graph) {
            super(GraphOp.class, graph.getId());
            this.graph = graph;
        }
    }
}
