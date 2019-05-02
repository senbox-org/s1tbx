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

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.util.Guardian;

import java.text.ParseException;

/**
 * A <code>MetadataElement</code> is a data node used to store metadata. Metadata elements can have any number of
 * metadata attributes of the type {@link MetadataAttribute} and any number of inner <code>MetadataElement</code>s.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision: 6651 $ $Date: 2009-10-27 12:59:39 +0100 (Di, 27 Okt 2009) $
 */
public class MetadataElement extends ProductNode {

    private ProductNodeGroup<MetadataElement> elements;

    private ProductNodeGroup<MetadataAttribute> attributes;

    //////////////////////////////////////////////////////////////////////////
    // Constructors

    /**
     * Constructs a new metadata element.
     *
     * @param name the element name
     */
    public MetadataElement(String name) {
        super(name);
    }

    //////////////////////////////////////////////////////////////////////////
    // Element support ('Composite' pattern support)

    /**
     * Gets the group of child elements. The method returns null, if this element has no children.
     *
     * @return The child element group, may be null.
     */
    public ProductNodeGroup<MetadataElement> getElementGroup() {
        return elements != null ? elements : null;
    }

    public MetadataElement getParentElement() {
        return getParentElement(this);
    }

    /**
     * Adds the given element to this element.
     *
     * @param element the element to added, ignored if <code>null</code>
     */
    public void addElement(MetadataElement element) {
        if (element == null) {
            return;
        }
        if (elements == null) {
            elements = new ProductNodeGroup<MetadataElement>(this, "elements", true);
        }
        elements.add(element);
    }

    /**
     * Adds the given element to this element at index.
     *
     * @param element the element to added, ignored if <code>null</code>
     * @param index   where to put it
     */
    public void addElementAt(MetadataElement element, int index) {
        if (element == null) {
            return;
        }
        if (elements == null) {
            elements = new ProductNodeGroup<MetadataElement>(this, "elements", true);
        }
        elements.add(index, element);
    }

    /**
     * Removes the given element from this element.
     *
     * @param element the element to be removed, ignored if <code>null</code>
     *
     * @return true, if so
     */
    public boolean removeElement(MetadataElement element) {
        return element != null && elements != null && elements.remove(element);
    }

    /**
     * @return the number of elements contained in this element.
     */
    public int getNumElements() {
        if (elements == null) {
            return 0;
        }
        return elements.getNodeCount();
    }

    /**
     * Returns the element at the given index.
     *
     * @param index the element index
     *
     * @return the element at the given index
     *
     * @throws IndexOutOfBoundsException if the index is out of bounds
     */
    public MetadataElement getElementAt(int index) {
        if (elements == null) {
            throw new IndexOutOfBoundsException();
        }
        return elements.get(index);
    }

    /**
     * Returns a string array containing the names of the groups contained in this element
     *
     * @return a string array containing the names of the groups contained in this element. If this element has no
     *         groups a zero-length-array is returned.
     */
    public String[] getElementNames() {
        if (elements == null) {
            return new String[0];
        }
        return elements.getNodeNames();
    }

    /**
     * Returns an array of elements contained in this element.
     *
     * @return an array of elements contained in this product. If this element has no elements a zero-length-array is
     *         returned.
     */
    public MetadataElement[] getElements() {
        if (elements == null) {
            return new MetadataElement[0];
        }
        return elements.toArray(new MetadataElement[0]);
    }

    /**
     * Returns the element with the given name.
     *
     * @param name the element name
     *
     * @return the element with the given name or <code>null</code> if a element with the given name is not contained in
     *         this element.
     */
    public MetadataElement getElement(String name) {
        Guardian.assertNotNullOrEmpty("name", name);
        if (elements == null) {
            return null;
        }
        return elements.get(name);
    }

    /**
     * Tests if a element with the given name is contained in this element.
     *
     * @param name the name, must not be <code>null</code>
     *
     * @return <code>true</code> if a element with the given name is contained in this element, <code>false</code>
     *         otherwise
     */
    public boolean containsElement(String name) {
        Guardian.assertNotNullOrEmpty("name", name);
        return elements != null && elements.contains(name);
    }

    /**
     * Gets the index of the given element.
     *
     * @param element The element .
     *
     * @return The element's index, or -1.
     *
     * @since BEAM 4.7
     */
    public int getElementIndex(MetadataElement element) {
        return elements.indexOf(element);
    }


    //////////////////////////////////////////////////////////////////////////
    // Attribute list support

    /**
     * Adds an attribute to this node.
     *
     * @param attribute the attribute to be added, <code>null</code> is ignored
     */
    public void addAttribute(MetadataAttribute attribute) {
        if (attribute == null) {
            return;
        }
        if (attributes == null) {
            attributes = new ProductNodeGroup<>(this, "attributes", true);
        }
        attributes.add(attribute);
    }

    /**
     * Removes the given attribute from this annotation. If an attribute with the same name already exists, the method
     * does nothing.
     *
     * @param attribute the attribute to be removed, <code>null</code> is ignored
     *
     * @return <code>true</code> if it was removed
     */
    public boolean removeAttribute(MetadataAttribute attribute) {
        return attribute != null && attributes != null && attributes.remove(attribute);
    }


    /**
     * Returns the number of attributes attached to this node.
     *
     * @return the number of attributes
     */
    public int getNumAttributes() {
        if (attributes == null) {
            return 0;
        }
        return attributes.getNodeCount();
    }

    /**
     * Returns the attribute at the given index.
     *
     * @param index the attribute index
     *
     * @return the attribute, or <code>null</code> if this node does not contain attributes
     *
     * @throws IndexOutOfBoundsException
     */
    public MetadataAttribute getAttributeAt(int index) {
        if (attributes == null) {
            throw new IndexOutOfBoundsException();
        }
        return attributes.get(index);
    }

    /**
     * Returns the names of all attributes of this node.
     *
     * @return the attribute name array, never <code>null</code>
     */
    public String[] getAttributeNames() {
        if (attributes == null) {
            return new String[0];
        }
        return attributes.getNodeNames();
    }

    /**
     * Returns an array of attributes contained in this element.
     *
     * @return an array of attributes contained in this product. If this element has no attributes a zero-length-array
     *         is returned.
     */
    public MetadataAttribute[] getAttributes() {
        if (attributes == null) {
            return new MetadataAttribute[0];
        }
        return attributes.toArray(new MetadataAttribute[attributes.getNodeCount()]);
    }

    /**
     * Returns the attribute with the given name.
     *
     * @param name the attribute name
     *
     * @return the attribute with the given name or <code>null</code> if it could not be found
     */
    public MetadataAttribute getAttribute(String name) {
        if (attributes == null) {
            return null;
        }
        return attributes.get(name);
    }

    /**
     * Checks whether this node has an element with the given name.
     *
     * @param name the attribute name
     *
     * @return <code>true</code> if so
     */
    public boolean containsAttribute(String name) {
        return attributes != null && attributes.contains(name);
    }

    /**
     * Gets the index of the given attribute.
     *
     * @param attribute The attribute.
     *
     * @return The attribute's index, or -1.
     *
     * @since BEAM 4.7
     */
    public int getAttributeIndex(MetadataAttribute attribute) {
        return attributes.indexOf(attribute);
    }


    //////////////////////////////////////////////////////////////////////////
    // Attribute access utility methods

    /**
     * Returns the double value of the attribute with the given name. <p>The given default value is returned if an
     * attribute with the given name could not be found in this node.
     *
     * @param name         the attribute name
     * @param defaultValue the default value
     *
     * @return the attribute value as double.
     *
     * @throws NumberFormatException if the attribute type is ASCII but cannot be converted to a number
     */
    public double getAttributeDouble(String name, double defaultValue) {
        final MetadataAttribute attribute = getAttribute(name);
        if (attribute == null) {
            return defaultValue;
        }
        if (attribute.getDataType() == ProductData.TYPE_ASCII) {
            return Double.parseDouble(attribute.getData().getElemString());
        }
        return attribute.getData().getElemDouble();
    }

    /**
     * Returns the double value of the attribute with the given name. <p>An Exception is thrown if an
     * attribute with the given name could not be found in this node.
     *
     * @param name the attribute name
     *
     * @return the attribute value as double.
     *
     * @throws NumberFormatException    if the attribute type is ASCII but cannot be converted to a number
     * @throws IllegalArgumentException if an attribute with the given name could not be found
     */
    public double getAttributeDouble(String name) {
        final MetadataAttribute attribute = getAttribute(name);
        if (attribute == null) {
            throw new IllegalArgumentException(getAttributeNotFoundMessage(name));
        }
        if (attribute.getDataType() == ProductData.TYPE_ASCII) {
            return Double.parseDouble(attribute.getData().getElemString());
        }
        return attribute.getData().getElemDouble();
    }

    /**
     * Returns the UTC value of the attribute with the given name. <p>The given default value is returned if an
     * attribute with the given name could not be found in this node.
     *
     * @param name         the attribute name
     * @param defaultValue the default value
     *
     * @return the attribute value as UTC.
     */
    public ProductData.UTC getAttributeUTC(String name, ProductData.UTC defaultValue) {
        try {
            final MetadataAttribute attribute = getAttribute(name);
            if (attribute != null) {
                return ProductData.UTC.parse(attribute.getData().getElemString());
            }
        } catch (ParseException e) {
            // continue
        }
        return defaultValue;
    }

    /**
     * Returns the UTC value of the attribute with the given name.
     *
     * @param name the attribute name
     *
     * @return the attribute value as UTC.
     *
     * @throws IllegalArgumentException if an attribute with the given name could not be found
     */
    public ProductData.UTC getAttributeUTC(String name) {
        try {
            final MetadataAttribute attribute = getAttribute(name);
            if (attribute != null) {
                return ProductData.UTC.parse(attribute.getData().getElemString());
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Unable to parse metadata attribute " + name);
        }
        throw new IllegalArgumentException(getAttributeNotFoundMessage(name));
    }

    /**
     * Returns the integer value of the attribute with the given name. <p>The given default value is returned if an
     * attribute with the given name could not be found in this node.
     *
     * @param name         the attribute name
     * @param defaultValue the default value
     *
     * @return the attribute value as integer.
     *
     * @throws NumberFormatException if the attribute type is ASCII but cannot be converted to a number
     */
    public int getAttributeInt(String name, int defaultValue) {
        final MetadataAttribute attribute = getAttribute(name);
        if (attribute == null) {
            return defaultValue;
        }
        if (attribute.getDataType() == ProductData.TYPE_ASCII) {
            return Integer.parseInt(attribute.getData().getElemString());
        }
        return attribute.getData().getElemInt();
    }

    /**
     * Returns the integer value of the attribute with the given name. <p>An Exception is thrown if an
     * attribute with the given name could not be found in this node.
     *
     * @param name the attribute name
     *
     * @return the attribute value as integer.
     *
     * @throws NumberFormatException    if the attribute type is ASCII but cannot be converted to a number
     * @throws IllegalArgumentException if an attribute with the given name could not be found
     */
    public int getAttributeInt(String name) {
        final MetadataAttribute attribute = getAttribute(name);
        if (attribute == null) {
            throw new IllegalArgumentException(getAttributeNotFoundMessage(name));
        }
        if (attribute.getDataType() == ProductData.TYPE_ASCII) {
            return Integer.parseInt(attribute.getData().getElemString());
        }
        return attribute.getData().getElemInt();
    }

    /**
     * Sets the attribute with the given name to the given integer value. <p>A new attribute with
     * <code>ProductData.TYPE_INT32</code> is added to this node if an attribute with the given name could not be found
     * in this node.
     *
     * @param name  the attribute name
     * @param value the new value
     */
    public void setAttributeInt(String name, int value) {
        final MetadataAttribute attribute = getAndMaybeCreateAttribute(name, ProductData.TYPE_INT32, 1);
        attribute.getData().setElemInt(value);
        attribute.fireProductNodeDataChanged();
        setModified(true);
    }

    /**
     * Sets the attribute with the given name to the given double value. <p>A new attribute with
     * <code>ProductData.TYPE_FLOAT64</code> is added to this node if an attribute with the given name could not be found
     * in this node.
     *
     * @param name  the attribute name
     * @param value the new value
     */
    public void setAttributeDouble(String name, double value) {
        final MetadataAttribute attribute = getAndMaybeCreateAttribute(name, ProductData.TYPE_FLOAT64, 1);
        attribute.getData().setElemDouble(value);
        attribute.fireProductNodeDataChanged();
        setModified(true);
    }

    /**
     * Sets the attribute with the given name to the given utc value. <p>A new attribute with
     * <code>ProductData.UTC</code> is added to this node if an attribute with the given name could not be found
     * in this node.
     *
     * @param name  the attribute name
     * @param value the new value
     */
    public void setAttributeUTC(String name, ProductData.UTC value) {
        final MetadataAttribute attribute = getAndMaybeCreateAttribute(name, ProductData.TYPE_UTC, 1);
        attribute.getData().setElems(value.getArray());
        attribute.fireProductNodeDataChanged();
        setModified(true);
    }

    /**
     * Returns the string value of the attribute with the given name. <p>An Exception is thrown if an
     * attribute with the given name could not be found in this node.
     *
     * @param name the attribute name
     *
     * @return the attribute value as integer.
     *
     * @throws IllegalArgumentException if an attribute with the given name could not be found
     */
    public String getAttributeString(String name) {
        final MetadataAttribute attribute = getAttribute(name);
        if (attribute == null) {
            throw new IllegalArgumentException(getAttributeNotFoundMessage(name));
        }
        return attribute.getData().getElemString();
    }

    /**
     * Returns the string value of the attribute with the given name. <p>The given default value is returned if an
     * attribute with the given name could not be found in this node.
     *
     * @param name         the attribute name
     * @param defaultValue the default value
     *
     * @return the attribute value as integer.
     */
    public String getAttributeString(String name, String defaultValue) {
        final MetadataAttribute attribute = getAttribute(name);
        if (attribute == null) {
            return defaultValue;
        }
        return attribute.getData().getElemString();
    }

    /**
     * Sets the attribute with the given name to the given string value. <p>A new attribute with
     * <code>ProductData.TYPE_ASCII</code> is added to this node if an attribute with the given name could not be found
     * in this node.
     *
     * @param name  the attribute name
     * @param value the new value
     */
    public void setAttributeString(String name, String value) {
        final MetadataAttribute attribute = getAndMaybeCreateAttribute(name, ProductData.TYPE_ASCII, 1);
        attribute.getData().setElems(value);
        attribute.fireProductNodeDataChanged();
        setModified(true);
    }

    @Override
    public void setModified(boolean modified) {
        boolean oldState = isModified();
        if (oldState != modified) {
            if (!modified) {
                if (elements != null) {
                    elements.setModified(false);
                }
                if (attributes != null) {
                    attributes.setModified(false);
                }
            }
            super.setModified(modified);
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // 'Visitor' pattern support


    /**
     * Accepts the given visitor. This method implements the well known 'Visitor' design pattern of the gang-of-four.
     * The visitor pattern allows to define new operations on the product data model without the need to add more code
     * to it. The new operation is implemented by the visitor.
     * <p>The method first visits (calls <code>acceptVisitor</code> for) all elements contained in this element and then
     * visits all attributes. Finally the method calls <code>visitor.visit(this)</code>.
     *
     * @param visitor the visitor
     */
    @Override
    public void acceptVisitor(ProductVisitor visitor) {
        Guardian.assertNotNull("visitor", visitor);
        for (int i = 0; i < getNumElements(); i++) {
            getElementAt(i).acceptVisitor(visitor);
        }
        for (int i = 0; i < getNumAttributes(); i++) {
            getAttributeAt(i).acceptVisitor(visitor);
        }
        visitor.visit(this);
    }

    //////////////////////////////////////////////////////////////////////////
    // Implementation helpers

    private MetadataAttribute getAndMaybeCreateAttribute(final String name, final int type, final int numElems) {
        MetadataAttribute attribute = getAttribute(name);
        if (attribute == null) {
            attribute = new MetadataAttribute(name, type, numElems);
            addAttribute(attribute);
        }
        return attribute;
    }

    /**
     * Gets an estimated, raw storage size in bytes of this product node.
     *
     * @param subsetDef if not <code>null</code> the subset may limit the size returned
     *
     * @return the size in bytes.
     */
    @Override
    public long getRawStorageSize(ProductSubsetDef subsetDef) {
        if (subsetDef != null && !subsetDef.containsNodeName(getName())) {
            return 0L;
        }
        long size = 0;
        for (int i = 0; i < getNumElements(); i++) {
            size += getElementAt(i).getRawStorageSize(subsetDef);
        }
        for (int i = 0; i < getNumAttributes(); i++) {
            size += getAttributeAt(i).getRawStorageSize(subsetDef);
        }
        return size;
    }

    public MetadataElement createDeepClone() {
        MetadataElement clone = new MetadataElement(getName());
        clone.setDescription(getDescription());
        MetadataAttribute[] attributes = getAttributes();
        for (MetadataAttribute attribute : attributes) {
            clone.addAttribute(attribute.createDeepClone());
        }
        MetadataElement[] elements = getElements();
        for (MetadataElement element : elements) {
            clone.addElement(element.createDeepClone());
        }
        return clone;
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     */
    @Override
    public void dispose() {
        if (attributes != null) {
            attributes.dispose();
            attributes = null;
        }
        if (elements != null) {
            elements.dispose();
            elements = null;
        }
        super.dispose();
    }

    static MetadataElement getParentElement(ProductNode node) {
        node = node.getOwner();
        while (node != null) {
            if (node instanceof MetadataElement) {
                return (MetadataElement) node;
            }
            node = node.getOwner();
        }
        return null;
    }

    private static String getAttributeNotFoundMessage(String name) {
        return "Metadata attribute '" + name + "' not found";
    }

}



