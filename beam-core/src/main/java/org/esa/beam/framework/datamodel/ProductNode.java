/*
 * $Id: ProductNode.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.datamodel;

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ObjectUtils;

/**
 * The <code>ProductNode</code> is the base class for all nodes within a remote sensing data product and even the data
 * product itself.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public abstract class ProductNode {

    public final static String PROPERTY_NAME_OWNER = "owner";
    public final static String PROPERTY_NAME_NAME = "name";
    public final static String PROPERTY_NAME_DESCRIPTION = "description";
    public final static String PROPERTY_NAME_MODIFIED = "modified";

    private Product _product;
    private ProductNode _owner;
    private String _name;
    private String _description;
    private boolean _modified;
    public final static String PROPERTY_NAME_SELECTED = "selected";
    private boolean selected;

    /**
     * Constructs a new product node with the given name.
     *
     * @param name the node name, must not be <code>null</code>
     * @throws IllegalArgumentException if the given name is not a valid node identifier
     */
    protected ProductNode(String name) {
        this(name, null);
    }

    /**
     * Constructs a new product node with the given name and an optional description.
     *
     * @param name        the node name, must not be <code>null</code>
     * @param description a descriptive string, can be <code>null</code>
     * @throws IllegalArgumentException if the given name is not a valid node identifier
     */
    protected ProductNode(String name, String description) {
        Guardian.assertNotNull("name", name);
        name = name.trim();
        Guardian.assertNotNullOrEmpty("name", name);
        if (!isValidNodeName(name)) {
            Debug.trace("warning: invalid product node name: '" + name + "'");
//            throw new IllegalArgumentException("The given name '" + name + "' is not a valid node name");
        }
        _name = name;
        _description = description;
    }

    /**
     * Sets the the owner node of this node.
     * <p>Overrides shall finally call <code>super.setOwner(owner)</code>.
     * </p>
     *
     * @param owner the new owner
     */
    protected void setOwner(ProductNode owner) {
        if (owner != _owner) {
            _owner = owner;
            _product = null;
            fireProductNodeChanged(PROPERTY_NAME_OWNER);
            setModified(true);
        }
    }

    /**
     * @return The owner node of this node.
     */
    public ProductNode getOwner() {
        return _owner;
    }

    /**
     * @return This node's name.
     */
    public String getName() {
        return _name;
    }

    /**
     * Sets this product's name.
     *
     * @param name The name.
     */
    public void setName(final String name) {
        Guardian.assertNotNull("name", name);
        String trimmedName = name.trim();
        Guardian.assertNotNullOrEmpty("name contains only spaces", trimmedName);
        if (!ObjectUtils.equalObjects(_name, trimmedName)) {
            if (!isValidNodeName(trimmedName)) {
                throw new IllegalArgumentException("The given name '" + trimmedName + "' is not a valid node name.");
            }
            additionalNameCheck(trimmedName);
            setNodeName(trimmedName);
        }
    }

    /**
     * @param trimmedName The trimmed name.
     * @deprecated Since 4.1. Don't use this anymore.
     */
    @Deprecated
    protected void additionalNameCheck(String trimmedName) {
    }

    private void setNodeName(String trimmedName) {
        final String oldName = _name;
        _name = trimmedName;
        fireProductNodeChanged(PROPERTY_NAME_NAME, oldName);
        setModified(true);
    }

    /**
     * Returns a short textual description for this products node.
     *
     * @return a description or <code>null</code>
     */
    public String getDescription() {
        return _description;
    }

    /**
     * Sets a short textual description for this products node.
     *
     * @param description a description, can be <code>null</code>
     */
    public void setDescription(String description) {
        if (!ObjectUtils.equalObjects(_description, description)) {
            _description = description;
            fireProductNodeChanged(PROPERTY_NAME_DESCRIPTION);
            setModified(true);
        }
    }

    /**
     * Returns whether or not this node is modified.
     *
     * @return <code>true</code> if so
     */
    public boolean isModified() {
        return _modified;
    }

    /**
     * Sets this node's modified flag.
     * <p/>
     * <p>If the modified flag changes to true and this node has an owner, the owner's modified flag is also set to
     * true.
     * <p/>
     * <p>If the modified flag changes and if this node is part of a <code>Product</code>, the method fires a
     * 'NodeChange' event on the product.
     *
     * @param modified whether or not this node is beeing marked as modified.
     * @see org.esa.beam.framework.datamodel.Product#fireNodeChanged
     */
    public void setModified(boolean modified) {
        boolean oldValue = _modified;
        if (oldValue != modified) {
            _modified = modified;
            if (_modified) {
                if (getOwner() != null) {
                    getOwner().setModified(true);
                }
            }
            fireProductNodeChanged(PROPERTY_NAME_MODIFIED);
        }
    }

    @Override
    public String toString() {
        return getClass().getName() + "[name=" + getName() + "]";
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     */
    public void dispose() {
        _owner = null;
        _description = null;
        _name = null;
    }

    //////////////////////////////////////////////////////////////////////////
    // General utility methods

    /**
     * Tests whether the given name is valid name for a node.
     * A valid node name must not start with a dot. Also a valid node name must not contain
     * any of the character  <code>\/:*?"&lt;&gt;|</code>
     *
     * @param name the name to test
     * @return <code>true</code> if the name is a valid node ifentifier, <code>false</code> otherwise
     */
    public static boolean isValidNodeName(final String name) {
        if (name == null || "or".equalsIgnoreCase(name) || "and".equalsIgnoreCase(name) || "not".equalsIgnoreCase(name)) {
            return false;
        }
        String trimedName = name.trim();
        return trimedName.matches("[^\\\\/:*?\"<>|\\.][^\\\\/:*?\"<>|]*");
    }

    /**
     * Returns the product to which this node belongs to.
     *
     * @return the product, or <code>null</code> if this node was not owned by a product at the time this method was
     *         called
     * @throws IllegalStateException if this node does not belong to a product
     */
    public Product getProduct() {
        if (_product == null) {
            ProductNode owner = this;
            do {
                if (owner instanceof Product) {
                    _product = (Product) owner;
                    break;
                }
                owner = owner.getOwner();
            } while (owner != null);
        }
        return _product;
    }

    /**
     * Returns safely the product to which this node belongs to.
     *
     * @return the product, never <code>null</code>
     * @throws IllegalStateException if this node does not belong to a product
     */
    protected Product getProductSafe() throws IllegalStateException {
        Product product = getProduct();
        if (product == null) {
            throw new IllegalStateException(
                    "node '" + getName() + "' must be owned by a product before it can be used"); /*I18N*/
        }
        return product;
    }

    /**
     * Returns the product reader for the product to which this node belongs to.
     *
     * @return the product reader, or <code>null</code> if no such exists
     */
    public ProductReader getProductReader() {
        final Product product = getProduct();
        return product != null ? product.getProductReader() : null;
    }

    /**
     * Returns the product reader for the product to which this node belongs to. The method thrws an
     * <code>IllegalStateException</code> if no such reader exists.
     *
     * @return the product reader, never <code>null</code>
     * @throws IllegalStateException if the the product reader is <code>null</code>
     */
    protected ProductReader getProductReaderSafe() {
        ProductReader productReader = getProductReader();
        if (productReader == null) {
            throw new IllegalStateException(getClass().getName() + " '" + getName() + "': no ProductReader set");  /*I18N*/
        }
        return productReader;
    }

    /**
     * Returns the product writer for the product to which this node belongs to.
     *
     * @return the product writer, or <code>null</code> if no such exists
     */
    public ProductWriter getProductWriter() {
        Product product = getProduct();
        return (product != null) ? product.getProductWriter() : null;
    }

    /**
     * Returns the product writer for the product to which this node belongs to. The method thrws an
     * <code>IllegalStateException</code> if no such writer exists.
     *
     * @return the product writer, never <code>null</code>
     * @throws IllegalStateException if the the product writer is <code>null</code>
     */
    protected ProductWriter getProductWriterSafe() {
        ProductWriter productWriter = getProductWriter();
        if (productWriter == null) {
            throw new IllegalStateException(
                    getClass().getName() + " '" + getName() + "': no ProductWriter set"); /*I18N*/
        }
        return productWriter;
    }

    /**
     * Returns this node's display name. The display name is the product reference string with the node name appended.
     * <p>Example: The string <code>"[2] <i>node-name</i>"</code> means node <code><i>node-name</i></code> of the
     * product with the reference number <code>2</code>.
     *
     * @return this node's name with a product prefix <br>or this node's name only if this node's product prefix is
     *         <code>null</code>
     * @see #getProductRefString
     */
    public String getDisplayName() {
        final String prefix = getProductRefString();
        if (prefix == null) {
            return getName();
        }
        return prefix.concat(" ").concat(getName());
    }

    /**
     * Gets the product reference string. The product reference string is the product reference number enclosed in
     * square brackets. <p>Example: The string <code>"[2]"</code> stands for a product with the reference number
     * <code>2</code>.
     *
     * @return the product reference string. <br>or <code>null</code> if this node has no product <br>or
     *         <code>null</code> if its product reference number was inactive
     */
    public String getProductRefString() {
        final Product product = getProduct();
        return product != null ? product.getRefStr() : null;
    }


    /**
     * Gets an estimated, raw storage size in bytes of this product node.
     *
     * @return the size in bytes.
     */
    public long getRawStorageSize() {
        return getRawStorageSize(null);
    }

    /**
     * Gets an estimated, raw storage size in bytes of this product node.
     *
     * @param subsetDef if not <code>null</code> the subset may limit the size returned
     * @return the size in bytes.
     */
    public abstract long getRawStorageSize(ProductSubsetDef subsetDef);

    /**
     * Asks a product node to replace all occurences of and references to the node name
     * given by {@code oldExternalName} with {@code oldExternalName}. Such references most often occur
     * in band arithmetic expressions.
     *
     * @param oldExternalName The old node name.
     * @param newExternalName The new node name.
     */
    public void updateExpression(final String oldExternalName, final String newExternalName) {
    }

    /**
     * Utility method which adds the given node tho the supplied node list.
     * <p/>
     * <p>Note that this method automatically sets the owner of the given <code>node</code> to this node instance.
     * Therefore this method should only be called on <code>ProductNode</code>s which own the given
     * <code>nodeList</code>
     * <p/>
     * <p>If the given node has already a parent product, it's modified flag is set and a 'NodeAdded' event is fired.
     *
     * @param node     the node to be added
     * @param nodeList the node list to which to add the node
     * @see #removeNamedNode
     */
    protected void addNamedNode(ProductNode node, ProductNodeList nodeList) {
        if (node != null && nodeList != null) {
            nodeList.add(node);
            node.setOwner(this);
            Product product = getProduct();
            if (product != null) {
                product.fireNodeAdded(node);
            }
            setModified(true);
        }
    }

    /**
     * Utility method which removes  the given node tho the supplied node list. The method fires a 'NodeRemoved' event.
     * <p/>
     * <p>Note that this method automatically sets the owner of the given <code>node</code> to <code>null</code>.
     * Therefore this method should only be called on <code>ProductNode</code>s which own the given
     * <code>nodeList</code>
     * <p/>
     * <p>If the given node has already a parent product and the given node could be removed, the node's modified flag
     * is set and a 'NodeRemoved' event is fired.
     *
     * @param node     the node to be removed
     * @param nodeList the node list from which to remove the node
     * @return <code>true</code> if the node has been removed
     * @see #addNamedNode
     */
    protected boolean removeNamedNode(ProductNode node, ProductNodeList nodeList) {
        boolean removed = false;
        if (node != null && nodeList != null) {
            removed = nodeList.remove(node);
            if (removed) {
                Product product = getProduct();
                if (product != null) {
                    product.setModified(true);
                    product.fireNodeRemoved(node);
                }
                node.setOwner(null);
            }
        }
        return removed;
    }

    protected void fireProductNodeChanged(final String propertyName) {
        fireProductNodeChanged(propertyName, null);
    }

    protected void fireProductNodeChanged(String propertyName, final Object oldValue) {
        final Product product = getProduct();
        if (product != null) {
            product.fireNodeChanged(this, propertyName, oldValue);
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // 'Visitor' pattern support

    /**
     * Accepts the given visitor. This method implements the well known 'Visitor' design pattern of the gang-of-four.
     * The visitor pattern allows to define new operations on the product data model without the need to add more code
     * to it. The new operation is implemented by the visitor.
     *
     * @param visitor the visitor
     */
    public abstract void acceptVisitor(ProductVisitor visitor);


    /**
     * Returns whether or not this node is part of the given subset.
     *
     * @param subsetDef The subset definition.
     * @return <code>true</code> if the subset is not <code>null</code> and it contains a node name equal to this node's
     *         name.
     */
    protected boolean isPartOfSubset(ProductSubsetDef subsetDef) {
        return subsetDef == null || subsetDef.containsNodeName(getName());
    }

    /**
     * Physically remove this node from the file associated with the given product writer. The default implementation
     * does nothing.
     *
     * @param productWriter the product writer to be used to remove this node from the underlying file.
     */
    public void removeFromFile(ProductWriter productWriter) {
    }

    public boolean isSelected() {
        return selected;
    }

    public void setSelected(boolean selected) {
        if (this.selected != selected) {
            this.selected = selected;
            fireProductNodeChanged(PROPERTY_NAME_SELECTED);
        }
    }
}


