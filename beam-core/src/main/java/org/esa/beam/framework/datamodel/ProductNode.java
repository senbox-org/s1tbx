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
package org.esa.beam.framework.datamodel;

import com.bc.ceres.core.ExtensibleObject;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ObjectUtils;

/**
 * The <code>ProductNode</code> is the base class for all nodes within a remote sensing data product and even the data
 * product itself.
 *
 * @author Norman Fomferra
 */
public abstract class ProductNode extends ExtensibleObject {

    public final static String PROPERTY_NAME_NAME = "name";
    public final static String PROPERTY_NAME_DESCRIPTION = "description";

    private transient Product product;
    private transient ProductNode owner;
    private transient boolean modified;
    private String name;
    private String description;

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
        this.name = name;
        this.description = description;
    }

    /**
     * Sets the the owner node of this node.
     * <p>Overrides shall finally call <code>super.setOwner(owner)</code>.
     * </p>
     *
     * @param owner the new owner
     */
    public void setOwner(ProductNode owner) {
        if (owner != this.owner) {
            this.owner = owner;
            product = null;
        }
    }

    /**
     * @return The owner node of this node.
     */
    public ProductNode getOwner() {
        return owner;
    }

    /**
     * @return This node's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Sets this product's name.
     *
     * @param name The name.
     */
    public void setName(final String name) {
        Guardian.assertNotNull("name", name);
        setNodeName(name.trim(), false);
    }

    private void setNodeName(String trimmedName, boolean silent) {
        Guardian.assertNotNullOrEmpty("name contains only spaces", trimmedName);
        if (!ObjectUtils.equalObjects(name, trimmedName)) {
            if (!isValidNodeName(trimmedName)) {
                throw new IllegalArgumentException("The given name '" + trimmedName + "' is not a valid node name.");
            }
            final String oldName = name;
            name = trimmedName;
            if (!silent) {
                fireProductNodeChanged(PROPERTY_NAME_NAME, oldName, name);
                setModified(true);
            }
        }
    }

    /**
     * Returns a short textual description for this products node.
     *
     * @return a description or <code>null</code>
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets a short textual description for this products node.
     *
     * @param description a description, can be <code>null</code>
     */
    public void setDescription(String description) {
        if (!ObjectUtils.equalObjects(this.description, description)) {
            this.description = description;
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
        return modified;
    }

    /**
     * Sets this node's modified flag.
     * <p/>
     * If the modified flag changes to true and this node has an owner, the owner's modified flag is also set to
     * true.
     *
     * @param modified whether or not this node is beeing marked as modified.
     * @see org.esa.beam.framework.datamodel.Product#fireNodeChanged
     */
    public void setModified(boolean modified) {
        boolean oldState = this.modified;
        if (oldState != modified) {
            this.modified = modified;

            // If this node is modified, the owner is also modified.
            if (this.modified && getOwner() != null) {
                getOwner().setModified(true);
            }
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
        owner = null;
        product = null;
        description = null;
        name = null;
    }

    //////////////////////////////////////////////////////////////////////////
    // General utility methods

    /**
     * Tests whether the given name is valid name for a node.
     * A valid node name must not start with a dot. Also a valid node name must not contain
     * any of the character  <code>\/:*?"&lt;&gt;|</code>
     *
     * @param name the name to test
     * @return <code>true</code> if the name is a valid node identifier, <code>false</code> otherwise
     */
    public static boolean isValidNodeName(final String name) {
        if (name == null || "or".equalsIgnoreCase(name) || "and".equalsIgnoreCase(name) || "not".equalsIgnoreCase(
                name)) {
            return false;
        }
        String trimmedName = name.trim();
        return trimmedName.matches("[^\\\\/:*?\"<>|\\.][^\\\\/:*?\"<>|]*");
    }

    /**
     * Returns the product to which this node belongs to.
     *
     * @return the product, or <code>null</code> if this node was not owned by a product at the time this method was
     *         called
     */
    public Product getProduct() {
        if (product == null) {
            synchronized (this) {
                if (product == null) {
                    ProductNode owner = this;
                    do {
                        if (owner instanceof Product) {
                            product = (Product) owner;
                            break;
                        }
                        owner = owner.getOwner();
                    } while (owner != null);
                }
            }
        }
        return product;
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
     * Returns the product reader for the product to which this node belongs to. The method throws an
     * <code>IllegalStateException</code> if no such reader exists.
     *
     * @return the product reader, never <code>null</code>
     * @throws IllegalStateException if the the product reader is <code>null</code>
     */
    protected ProductReader getProductReaderSafe() {
        ProductReader productReader = getProductReader();
        if (productReader == null) {
            throw new IllegalStateException(
                    getClass().getName() + " '" + getName() + "': no ProductReader set");  /*I18N*/
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
     * Returns the product writer for the product to which this node belongs to. The method throws an
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

    public void fireProductNodeChanged(final String propertyName) {
        fireProductNodeChanged(propertyName, null, null);
    }

    public void fireProductNodeChanged(String propertyName, final Object oldValue, final Object newValue) {
        final Product product = getProduct();
        if (product != null) {
            product.fireNodeChanged(this, propertyName, oldValue, newValue);
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

}


