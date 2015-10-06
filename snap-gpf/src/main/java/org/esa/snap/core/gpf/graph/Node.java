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
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorSpi;

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
    private DomElement configuration;

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
     * @param source the <code>NodeSource</code> to be added.
     */
    public void addSource(NodeSource source) {
        sourceList.addSource(source);
    }

    /**
     * Removes a <code>NodeSource</code> from the <code>Node</code>.
     *
     * @param source the <code>NodeSource</code> to be removed
     */
    public void removeSource(NodeSource source) {
        sourceList.removeSource(source);
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
     * @return an array containing the Sources of this node.
     */
    public NodeSource[] getSources() {
        return sourceList.getSources();
    }

    /**
     * @return A {@link DomElement} storing the configuration elements of the node's
     *         Operator.
     */
    public DomElement getConfiguration() {
        return configuration;
    }

    /**
     * Sets the configuration for the node's {@link Operator} that
     * computes the target Product.
     *
     * @param configuration The configuration
     */
    public void setConfiguration(DomElement configuration) {
        this.configuration = configuration;
    }

    /**
     * Indirectly used by {@link GraphIO}. DO NOT REMOVE!
     *
     * @return this
     */
    @SuppressWarnings({"UnusedDeclaration"})
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
