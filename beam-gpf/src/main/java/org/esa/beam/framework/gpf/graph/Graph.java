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
     *
     * @param id the id of the graph
     */
    public Graph(String id) {
        this.id = id;
        init();
    }

    /**
     * Gets the graph's id
     *
     * @return the id of the graph
     */
    public Object getId() {
        return id;
    }

    /**
     * Gets the number nodes contained by this graph.
     *
     * @return the number nodes
     */
    public int getNodeCount() {
        return nodeList.size();
    }

    /**
     * Adds a <code>Node</code> to the graph
     *
     * @param node a node
     * @throws IllegalArgumentException if the id of the given node is already in use
     */
    public void addNode(Node node) {
        if (nodeList.contains(getNode(node.getId()))) {
            throw new IllegalArgumentException("node ID duplicated");
        }
        nodeList.add(node);
    }

    /**
     * Removes the {@link Node} with the given {@code id} from this graph if present.
     *
     * @param id the id of the {@link Node} to be removed
     * @return {@code true<} if the graph contains a {@link Node} with the given {@code id}. Else {@code false}.
     */
    public boolean removeNode(String id) {
        return nodeList.remove(getNode(id));
    }

    /**
     * Gets the {@link Node} at the given index.
     *
     * @param index the index
     * @return the node at the given index
     */
    public Node getNode(int index) {
        return nodeList.get(index);
    }

    /**
     * Returns the {@link Node} with the given {@code id} or
     * {@code null} if the graph contains no respective {@link Node}.
     *
     * @param id the id of the Node to be removed
     * @return {@code true} if the graph contains a {@link Node} with the given {@code id}. Else {@code false}.
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
     * Returns an array containing all nodes in this graph.
     *
     * @return an array of all nodes
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
