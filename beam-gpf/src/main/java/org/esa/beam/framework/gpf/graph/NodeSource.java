package org.esa.beam.framework.gpf.graph;

/**
 * A <code>NodeSource</code> represents a mapping from an nodeId to a
 * <code>Node</code> instance. The <code>GraphIO</code> uses sorces to
 * simplify the xml deserialization. Use this class to set the sources of
 * <code>Node</code>s to a <code>Node</code> with the given nodeId. The
 * <code>GraphProcessor</code> will resolve the nodeId to the respective
 * <code>Node</code> if present.
 */
public class NodeSource {

    private final String name;
    private final String sourceNodeId;

    // todo - move to NodeContext
    private Node sourceNode;

    /**
     * Constructs a <code>NodeSource</code>.
     *
     * @param name         the name of the source
     * @param sourceNodeId the identifier of the source node
     */
    public NodeSource(String name, String sourceNodeId) {
        this.name = name;
        this.sourceNodeId = sourceNodeId;
    }

    /**
     * Gets the name under which
     * this source can be accessed.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets the identifier of the source node.
     *
     * @return the identifier of the source node
     */
    public String getSourceNodeId() {
        return sourceNodeId;
    }

    // todo - move away following getter/setter

    /**
     * Returns the <code>Node</code> instance associatied with this
     * <code>NodeSource</code>.
     */
    public Node getSourceNode() {
        return sourceNode;
    }

    /**
     * Associates this <code>NodeSource</code> with this <code>NodeSource</code>.
     *
     * @param sourceNode the node to be set
     */
    void setSourceNode(Node sourceNode) {
        this.sourceNode = sourceNode;
    }
}
