package org.esa.beam.framework.gpf.graph;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a directed acyclic graph (DAG) of {@link Node}s.
 *
 * @author Maximilian Aulinger
 * @author Norman Fomferra
 * @author Ralf Quast
 */
public class Graph {

    // IMPORTANT: Fields are deserialised by GraphIO, don't change names without adopting GraphIO
    private String id;
    private List<Node> nodeList;


    /**
     * Constructs an empty graph with the given <code>id</code>.
     */
    public Graph(String id) {
        this.id = id;
        init();
    }

    /**
     * Returns the Graph's id
     */
    public Object getId() {
        return id;
    }

    public int getNodeCount() {
        return nodeList.size();
    }

    /**
     * Adds a <code>Node</code> to the graph
     *
     * @throws IllegalArgumentException if the id of the given node is already in use
     */
    public void addNode(Node node) {
        if (nodeList.contains(getNode(node.getId()))) {
            throw new IllegalArgumentException("node ID duplicated");
        }
        nodeList.add(node);
    }

    /**
     * Removes the <code>Node</code> with the given <code>id</code> from the
     * <code>Graph</code> if present.
     *
     * @param id the id of the <code>Node</code> to be removed
     * @return <code>true</code> if the <code>Graph</code> contains a
     *         <code>Node</code> with the given id. Else <code>false</code>.
     */
    public boolean removeNode(String id) {
        return nodeList.remove(getNode(id));
    }

    public Node getNode(int index) {
        return nodeList.get(index);
    }

    /**
     * Returns the {@link Node} with the given <code>id</code> or
     * <code>null</code> if the <code>Graph</code> contains no respective
     * <code>Node</code>.
     *
     * @param id the id of the Node to be removed
     * @return <code>true</code> if the <code>Graph</code> contains a
     *         <code>Node</code> with the given id. Else <code>false</code>.
     */
    public Node getNode(String id) {
        for (Node node : nodeList) {
            if (node.getId().equalsIgnoreCase(id)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Returns an array containing all Nodes in this <code>Graph</code>.
     */
    public Node[] getNodes() {
        return nodeList.toArray(new Node[nodeList.size()]);
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
        if (this.nodeList == null) {
            this.nodeList = new ArrayList<Node>();
        }
    }

}
