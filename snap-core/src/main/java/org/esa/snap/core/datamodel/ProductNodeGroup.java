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

package org.esa.snap.core.datamodel;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.dataio.ProductSubsetDef;

import java.util.Collection;

/**
 * A type-safe container for elements of the type <code>ProductNode</code>.
 *
 * @author Norman Fomferra
 */
public class ProductNodeGroup<T extends ProductNode> extends ProductNode {

    private final ProductNodeList<T> nodeList;
    private final boolean takingOverNodeOwnership;

    /**
     * Constructs a node group with no owner and which will not take ownership of added children.
     *
     * @param name The group name.
     * @since BEAM 4.8
     */
    public ProductNodeGroup(String name) {
        this(null, name, false);
    }

    /**
     * Constructs a node group for the given owner.
     *
     * @param owner                   The owner of the group.
     * @param name                    The group name.
     * @param takingOverNodeOwnership If {@code true}, child nodes will have this group as owner after adding.
     */
    public ProductNodeGroup(ProductNode owner, String name, boolean takingOverNodeOwnership) {
        super(name, "");
        this.nodeList = new ProductNodeList<T>();
        this.takingOverNodeOwnership = takingOverNodeOwnership;
        setOwner(owner);
    }

    /**
     * @return {@code true}, if child nodes will have this group as owner after adding.
     */
    public boolean isTakingOverNodeOwnership() {
        return takingOverNodeOwnership;
    }

    /**
     * @return The number of product nodes in this product group.
     */
    public int getNodeCount() {
        return nodeList.size();
    }

    /**
     * @param index The node index.
     * @return The product node at the given index.
     */
    public T get(int index) {
        return nodeList.getAt(index);
    }

    /**
     * Returns the display names of all products currently managed.
     *
     * @return an array containing the display names, never <code>null</code>, but the array can have zero length
     * @see ProductNode#getDisplayName()
     */
    public String[] getNodeDisplayNames() {
        return nodeList.getDisplayNames();
    }

    /**
     * Returns the names of all products currently managed.
     *
     * @return an array containing the names, never <code>null</code>, but the array can have zero length
     */
    public String[] getNodeNames() {
        return nodeList.getNames();
    }

    /**
     * Returns an array of all products currently managed.
     *
     * @return an array containing the products, never <code>null</code>, but the array can have zero length
     */
    public ProductNode[] toArray() {
        return nodeList.toArray();
    }

    /**
     * @param array the array into which the elements of the list are to be stored, if it is big enough; otherwise, a
     *              new array of the same runtime type is allocated for this purpose.
     * @return an array containing the product nodes, never <code>null</code>, but the array can have zero length
     */
    public T[] toArray(T[] array) {
        return nodeList.toArray(array);
    }

    public int indexOf(String name) {
        return nodeList.indexOf(name);
    }

    public int indexOf(T element) {
        return nodeList.indexOf(element);
    }

    /**
     * @param displayName the display name
     * @return the product node with the given display name.
     */
    public T getByDisplayName(final String displayName) {
        return nodeList.getByDisplayName(displayName);
    }

    /**
     * @param name the name
     * @return the product node with the given name.
     */
    public T get(String name) {
        return nodeList.get(name);
    }

    /**
     * Tests whether a node with the given name is contained in this group.
     *
     * @param name the name
     * @return true, if so
     */
    public boolean contains(String name) {
        return nodeList.contains(name);
    }

    /**
     * Tests whether the given product is contained in this list.
     *
     * @param node the node
     * @return true, if so
     */
    public boolean contains(final T node) {
        return nodeList.contains(node);
    }

    /**
     * Adds the given node to this group.
     *
     * @param node the node to be added, ignored if <code>null</code>
     * @return true, if the node has been added
     */
    public boolean add(T node) {
        Assert.notNull(node, "node");
        boolean added = nodeList.add(node);
        if (added) {
            notifyAdded(node);
        }
        return added;
    }

    /**
     * Adds the given node to this group.
     *
     * @param index the index.
     * @param node  the node to be added, ignored if <code>null</code>
     */
    public void add(int index, T node) {
        Assert.notNull(node, "node");
        nodeList.add(index, node);
        notifyAdded(node);
    }

    /**
     * Removes the given node from this group.
     *
     * @param node the node to be removed
     * @return true, if the node was removed
     */
    public boolean remove(T node) {
        Assert.notNull(node, "node");
        boolean removed = nodeList.remove(node);
        if (removed) {
            notifyRemoved(node);
        }
        return removed;
    }

    /**
     * Removes all nodes from this group.
     */
    public void removeAll() {
        final ProductNode[] nodes = toArray();
        for (ProductNode node : nodes) {
            remove((T) node);
        }
    }

    public void clearRemovedList() {
        nodeList.clearRemovedList();
    }

    /**
     * Gets all removed node nodes.
     *
     * @return a collection of all removed node nodes.
     */
    public Collection<T> getRemovedNodes() {
        return nodeList.getRemovedNodes();
    }

    @Override
    public long getRawStorageSize(ProductSubsetDef subsetDef) {
        long size = 0;
        ProductNode[] nodes = toArray();
        for (ProductNode node : nodes) {
            if (subsetDef.isNodeAccepted(node.getName())) {
                size += node.getRawStorageSize(subsetDef);
            }
        }
        return size;
    }

    @Override
    public void setModified(boolean modified) {
        boolean oldState = isModified();
        if (oldState != modified) {
            if (!modified) {
                for (ProductNode node : toArray()) {
                    node.setModified(false);
                }
                clearRemovedList();
            }
            super.setModified(modified);
        }
    }

    @Override
    public void acceptVisitor(ProductVisitor visitor) {
        if (takingOverNodeOwnership) {
            for (final ProductNode node : toArray()) {
                node.acceptVisitor(visitor);
            }
        }
        visitor.visit(this);
    }

    @Override
    public void dispose() {
        nodeList.dispose();
        super.dispose();
    }

    @Override
    public void updateExpression(final String oldExternalName, final String newExternalName) {
        if (takingOverNodeOwnership) {
            for (final ProductNode node : toArray()) {
                node.updateExpression(oldExternalName, newExternalName);
            }
        }
    }

    private void notifyAdded(T node) {
        // Intended: set owner=this before notifying listeners
        if (takingOverNodeOwnership) {
            node.setOwner(this);
        }

        // notify listeners
        Product product = getProduct();
        if (product != null) {
            product.fireNodeAdded(node, this);
        }

        // Intended: set modified=true is last operation
        setModified(true);
    }

    private void notifyRemoved(T node) {
        // notify listeners
        Product product = getProduct();
        if (product != null) {
            product.fireNodeRemoved(node, this);
        }

        // Intended: set owner=null after notifying listeners
        if (takingOverNodeOwnership && node.getOwner() == this) {
            node.setOwner(null);
        }

        // Intended: set modified=true is last operation
        setModified(true);
    }

}
