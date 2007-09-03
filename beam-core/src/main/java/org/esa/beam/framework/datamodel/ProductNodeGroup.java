package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.Assert;
import org.esa.beam.framework.dataio.ProductSubsetDef;

import java.util.ArrayList;
import java.util.Collection;

/**
 * A type-safe container for elements of the type <code>ProductNode</code>.
 *
 * @author Norman Fomferra
 * @version $Revision: 1.1.1.1 $ $Date: 2006/09/11 08:16:45 $
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
     * Tests whether a product with the given name is contained in this list.
     */
    public boolean contains(String name) {
        return nodeList.contains(name);
    }

    /**
     * Tests whether the given product is contained in this list.
     */
    public boolean contains(final T node) {
        return nodeList.contains(node);
    }

    /**
     * Adds the given product to this product manager if it does not already exists and sets it's reference number one
     * biger than the greatest reference number in this product manager.
     *
     * @param node the product to be added, ignored if <code>null</code>
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
     * Removes the given product from this product manager if it exists.
     *
     * @param node the product to be removed
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
     * Removes all product from this group.
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
     * Gets all removed product nodes.
     *
     * @return a collection of all removed product nodes.
     */
    public Collection<T> getRemovedNodes() {
        return nodeList.getRemovedNodes();
    }


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

}
