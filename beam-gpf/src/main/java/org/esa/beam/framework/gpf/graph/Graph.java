package org.esa.beam.framework.gpf.graph;

import java.util.ArrayList;
import java.util.List;

import org.esa.beam.framework.gpf.internal.ApplicationData;

import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;

/**
 * Represents a directed acyclic graph (DAG) of {@link Node}s.
 *
 * @author Maximilian Aulinger
 * @author Norman Fomferra
 * @author Ralf Quast
 */
public class Graph {

    public static final String CURRENT_VERSION = "1.0";
    // IMPORTANT: Fields are deserialised by GraphIO, don't change names without adopting GraphIO
    private String id;
    private String version;
    private Header header;
    private List<Node> nodeList;
    private List<ApplicationData> applicationData;


    /**
     * Constructs an empty graph with the given <code>id</code>.
     *
     * @param id the id of the graph
     */
    public Graph(String id) {
        this.id = id;
        version = CURRENT_VERSION;
        init();
    }

    /**
     * Gets the graph's id
     *
     * @return the id of the graph
     */
    public String getId() {
        return id;
    }
    
    /**
     * Gets the graph's version 
     * 
     * @return the version of the graph
     */
    public String getVersion() {
        return version;
    }
    
    /**
     * Gets the graph's header
     * 
     * @return the header of the graph
     */
    public Header getHeader() {
        return header;
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
     * Returns the Application data for the given application ID or null,
     * if for this id no application is available.
     * 
     * @param appId the application ID
     * @return the application data as an Xpp3Dom
     */
    public Xpp3Dom getApplicationData(String appId) {
        for (ApplicationData appData : applicationData) {
            if (appData.getId().equals(appId)) {
                return appData.getData();
            }
        }
        return null;
    }
    
    /**
     * Sets the application data for the given ID
     * 
     * @param id The application ID.
     * @param data The application data as Xpp3Dom.
     */
    public void setAppData(String id, Xpp3Dom data) {
        for (int i = 0; i < applicationData.size(); i++) {
            if (applicationData.get(i).getId().equals(id)) {
                applicationData.remove(i);
                break;
            }
        }
        ApplicationData appData = new ApplicationData(id, data);
        this.applicationData.add(appData);
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
        if (this.applicationData == null) {
            this.applicationData = new ArrayList<ApplicationData>();
        }
    }
}
