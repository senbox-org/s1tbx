package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.dataio.ProductSubsetDef;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * A type-safe container for elements of the type <code>ProductNode</code>.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class ProductNodeGroup<T extends ProductNode> extends ProductNode {

    private final ProductNodeList<T> nodeList;

    /**
     * Constructs an product manager with an empty list of products.
     */
    public ProductNodeGroup(ProductNode owner, String name, String description) {
        super(name, description);
        nodeList = new ProductNodeList<T>();
        setOwner(owner);
    }

    /**
     * Returns the number of products in this product manager.
     */
    public int getNodeCount() {
        return nodeList.size();
    }

    /**
     * Returns the product at the given index.
     */
    public T get(int index) {
        return nodeList.getAt(index);
    }

    /**
     * Returns the display names of all products currently managed.
     *
     * @return an array containing the display names, never <code>null</code>, but the array can have zero length
     *
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
     * Returns an array of all products currently managed.
     *
     * @return an array containing the products, never <code>null</code>, but the array can have zero length
     */
    public T[] toArray(T[] a) {
        return nodeList.toArray(a);
    }

    public int indexOf(String name) {
        return nodeList.indexOf(name);
    }

    /**
     * Returns the product with the given display name.
     */
    public T getByDisplayName(final String displayName) {
        return nodeList.getByDisplayName(displayName);
    }

    /**
     * Returns the product with the given name.
     */
    public T get(String name) {
        return nodeList.get(name);
    }

    /**
     * Tests whether a node with the given name is contained in this group.
     * @param name the name
     * @return true, if so
     */
    public boolean contains(String name) {
        return nodeList.contains(name);
    }

    /**
     * Tests whether the given product is contained in this list.
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
     */
    public boolean add(T node) {
        Assert.notNull(node, "node");
        boolean added = nodeList.add(node);
        if (added) {
            node.setOwner(this);
            Product product = getProduct();
            if (product != null) {
                product.fireNodeAdded(node);
            }
            setModified(true);
        }
        return added;
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
            Product product = getProduct();
            if (product != null) {
                product.setModified(true);
                product.fireNodeRemoved(node);
            }
            node.setOwner(null);
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
    public void acceptVisitor(ProductVisitor visitor) {
        visitor.visit(this);
    }

    @Override
    public void dispose() {
        nodeList.dispose();
        super.dispose();
    }

    public void setSelectedNode(final int index) {
        final ProductNode[] nodes = toArray();
        for (int i = 0; i < nodes.length; i++) {
            nodes[i].setSelected(i == index);
        }
    }

    public void setSelectedNode(final String name) {
        if (name == null) {
            return;
        }
        final int index = indexOf(name);
        if (index != -1) {
            setSelectedNode(index);
        }
    }

    public T getSelectedNode() {
        final ProductNode[] nodes = toArray();
        for (final ProductNode  node : nodes) {
            if ( node.isSelected()) {
                return  (T) node;
            }
        }
        return null;
    }

    public Collection<T> getSelectedNodes() {
        final Collection<T> selectedNodes = new ArrayList<T>(16);
        final ProductNode[] nodes = toArray();
        for (final ProductNode  node : nodes) {
            if (node.isSelected()) {
                selectedNodes.add((T) node);
            }
        }
        return selectedNodes;
    }

    @Override
    public void updateExpression(final String oldExternalName, final String newExternalName) {
        for (final ProductNode  node : toArray()) {
            node.updateExpression(oldExternalName, newExternalName);
        }
    }
}
