package org.esa.beam.framework.gpf.graph;

import org.esa.beam.framework.gpf.OperatorContext;
import org.esa.beam.framework.gpf.internal.DefaultOperatorContext;

import java.util.logging.Logger;

/**
 * Default implementation for {@link OperatorContext}.
 */
class NodeContext extends DefaultOperatorContext {

    private final GraphContext graphContext;
    private final Node node;
    private int referenceCount;

    public NodeContext(GraphContext graphContext, Node node) {
        super(node.getOperatorName());
        this.graphContext = graphContext;
        this.node = node;
    }

    public GraphContext getGraphContext() {
        return graphContext;
    }

    public Node getNode() {
        return node;
    }

    @Override
    public Logger getLogger() {
        return graphContext.getLogger();
    }

    /**
     * Returns <code>true</code> if this <code>Node</code> does not act as
     * source for other nodes, i.e. has no target nodes.
     *
     * @return <code>true</code> if this node has no target nodes
     */
    public boolean isOutput() {
        return referenceCount == 0;
    }

    void incrementReferenceCount() {
        this.referenceCount++;
    }
}
