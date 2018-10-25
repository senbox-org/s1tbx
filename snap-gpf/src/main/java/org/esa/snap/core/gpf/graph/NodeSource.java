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

/**
 * A <code>NodeSource</code> represents a mapping from an nodeId to a
 * <code>Node</code> instance. The <code>GraphIO</code> uses sources to
 * simplify the xml deserialization. Use this class to set the sources of
 * <code>Node</code>s to a <code>Node</code> with the given nodeId. The
 * <code>GraphProcessor</code> will resolve the nodeId to the respective
 * <code>Node</code> if present.
 */
public class NodeSource {

    private String name;
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

    public void setName(final String name) {
        this.name = name;
    }
}
