package org.esa.beam.framework.gpf.graph;


import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpi;

import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;

/**
 * A node in a processing graph. A <code>Node</code> has an arbitrary nummber
 * of sources and produces one target product. A set of nodes may be joined
 * together via the sources to form a directed acyclic graph (DAG). The
 * <code>Node</code> uses an {@link Operator} implementation to
 * transform the target Products of the source Nodes to a target Product. The
 * <code>Node</code> will create its <code>Operator</code> by using the
 * given OperatorSpi.
 *
 * @author Maximilian Aulinger
 * @author Norman Fomferra
 * @author Ralf Quast
 * @author Marco Zühlke
 * @since 4.1
 */
public class Node {

    // IMPORTANT: Fields are deserialised by GraphIO, don't change names without adopting GraphIO
    private String id;
    private String operatorName;
    private SourceList sourceList;
    private Xpp3Dom configuration;

    /**
     * Constructs a new <code>Node</code> instance.
     *
     * @param id           a unique identifier for the node
     * @param operatorName the name of the operator
     */
    public Node(String id, String operatorName) {
        this.id = id;
        this.operatorName = operatorName;
        init();
    }

    /**
     * Gets the uniqe node identifier.
     *
     * @return the identifier
     */
    public String getId() {
        return id;
    }

    /**
     * Gets the name of the operator. This can be either the fully qualified class name of the {@link OperatorSpi}
     * or an alias name.
     *
     * @return the name of the operator.
     */
    public String getOperatorName() {
        return operatorName;
    }

    /**
     * Adds a <code>NodeSource</code> to the <code>Node</code>.
     *
     * @param source the <code>NodeSource</code> to be added
     */
    public void addSource(NodeSource source) {
        sourceList.addSource(source);
    }

    /**
     * Returns the <code>NodeSource</code> at the given index position
     *
     * @param index the index of the <code>NodeSource</code> to return
     * @return the <code>NodeSource</code> at the given index position
     * @throws IndexOutOfBoundsException if index is out of range
     */
    public NodeSource getSource(int index) throws IndexOutOfBoundsException {
        return sourceList.getSource(index);
    }

    /**
     * Returns an array containig all the Sources of this Node.
     *
     * @return an array containing the Sources of this Node
     */
    public NodeSource[] getSources() {
        return sourceList.getSources();
    }

    /**
     * Returns a {@link Xpp3Dom} storing the configuration elements of the Node's
     * Operator.
     */
    public Xpp3Dom getConfiguration() {
        return configuration;
    }

    /**
     * Sets the configuration for the Node's {@link org.esa.beam.framework.gpf.Operator} that
     * computes the target Product.
     */
    public void setConfiguration(Xpp3Dom configuration) {
        this.configuration = configuration;
    }

    /**
     * Indirectly used by {@link GraphIO}. DO NOT REMOVE!
     *
     * @return this
     */
    private Object readResolve() {
        init();
        return this;
    }

    private void init() {
        if (sourceList == null) {
            sourceList = new SourceList();
        }
    }
}
