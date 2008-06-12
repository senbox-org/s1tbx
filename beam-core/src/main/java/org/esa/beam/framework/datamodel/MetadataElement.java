/*
 * $Id: MetadataElement.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
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

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.util.Guardian;

/**
 * A <code>MetadataElement</code> is a data node used to store metadata. Metadata elements can have any number of
 * metadata attributes of the type {@link MetadataAttribute} and any number of inner <code>MetadataElement</code>s.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$ $Date$
 */
public class MetadataElement extends ProductNode {

    private ProductNodeList<MetadataAttribute> _attributes;

    private ProductNodeList<MetadataElement> _elements;

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
     * Adds the given element to this element.
     *
     * @param element the element to added, ignored if <code>null</code>
     */
    public void addElement(MetadataElement element) {
        if (element == null) {
            return;
        }
        if (_elements == null) {
            _elements = new ProductNodeList<MetadataElement>();
        }
        addNamedNode(element, _elements);
    }


    /**
     * Removes the given element from this element.
     *
     * @param element the element to be removed, ignored if <code>null</code>
     */
    public boolean removeElement(MetadataElement element) {
        if (element == null) {
            return false;
        }
        if (_elements == null) {
            return false;
        }
        return removeNamedNode(element, _elements);
    }

    /**
     * Returns the number of elements contained in this element.
     */
    public int getNumElements() {
        if (_elements == null) {
            return 0;
        }
        return _elements.size();
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
        if (_elements == null) {
            throw new IndexOutOfBoundsException();
        }
        return _elements.getAt(index);
    }

    /**
     * Returns a string array containing the names of the groups contained in this element
     *
     * @return a string array containing the names of the groups contained in this element. If this element has no
     *         groups a zero-length-array is returned.
     */
    public String[] getElementNames() {
        if (_elements == null) {
            return new String[0];
        }
        return _elements.getNames();
    }

    /**
     * Returns an array of elements contained in this element.
     *
     * @return an array of elements contained in this product. If this element has no elements a zero-length-array is
     *         returned.
     */
    public MetadataElement[] getElements() {
        MetadataElement[] elements = new MetadataElement[getNumElements()];
        for (int i = 0; i < elements.length; i++) {
            elements[i] = getElementAt(i);
        }
        return elements;
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
        if (_elements == null) {
            return null;
        }
        return _elements.get(name);
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
        if (_elements == null) {
            return false;
        }
        return _elements.contains(name);
    }

    //////////////////////////////////////////////////////////////////////////
    // Attribute list support

    /**
     * Adds an attribute to this node. If an attribute with the same name already exists, the method does nothing.
     *
     * @param attribute the attribute to be added, <code>null</code> is ignored
     */
    public void addAttribute(MetadataAttribute attribute) {
        if (attribute == null) {
            return;
        }
        if (_attributes == null) {
            _attributes = new ProductNodeList<MetadataAttribute>();
        }
        // only add if the list does not contain it already
        if (_attributes.contains(attribute.getName())) {
            return;
        }
        addNamedNode(attribute, _attributes);
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
        if (attribute == null) {
            return false;
        }
        if (_attributes == null) {
            return false;
        }
        return removeNamedNode(attribute, _attributes);
    }


    /**
     * Returns the number of attributes attaached to this node.
     *
     * @return the number of attributes
     */
    public int getNumAttributes() {
        if (_attributes == null) {
            return 0;
        }
        return _attributes.size();
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
        if (_attributes == null) {
            throw new IndexOutOfBoundsException();
        }
        return _attributes.getAt(index);
    }

    /**
     * Returns the names of all attributes of this node.
     *
     * @return the attribute name array, never <code>null</code>
     */
    public String[] getAttributeNames() {
        if (_attributes == null) {
            return new String[0];
        }
        return _attributes.getNames();
    }

    /**
     * Returns an array of attributes contained in this element.
     *
     * @return an array of attributes contained in this product. If this element has no attributes a zero-length-array
     *         is returned.
     */
    public MetadataAttribute[] getAttributes() {
        MetadataAttribute[] attributes = new MetadataAttribute[getNumAttributes()];
        for (int i = 0; i < attributes.length; i++) {
            attributes[i] = getAttributeAt(i);
        }
        return attributes;
    }

    /**
     * Returns the attribute with the given name.
     *
     * @return the attribute with the given name or <code>null</code> if it could not be found
     */
    public MetadataAttribute getAttribute(String name) {
        if (_attributes == null) {
            return null;
        }
        return _attributes.get(name);
    }

    /**
     * Checks whether this node has an element with the given name.
     *
     * @return <code>true</code> if so
     */
    public boolean containsAttribute(String name) {
        if (_attributes == null) {
            return false;
        }
        return _attributes.contains(name);
    }


    //////////////////////////////////////////////////////////////////////////
    // Attribute access utility methods

    /**
     * Returns the integer value of the attribute with the given name. <p>The given default value is returned if an
     * attribute with the given name could not be found in this node.
     *
     * @param name         the attribute name
     * @param defaultValue the default value
     *
     * @return the attribute value as integer.
     */
    public int getAttributeInt(String name, int defaultValue) {
        MetadataAttribute attribute = getAttribute(name);
        if (attribute == null) {
            return defaultValue;
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
        MetadataAttribute attribute = getAndMaybeCreateAttribute(name, ProductData.TYPE_INT32, 1);
        attribute.getData().setElemInt(value);
        attribute.fireProductNodeDataChanged();
        setModified(true);
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
        MetadataAttribute attribute = getAttribute(name);
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
        MetadataAttribute attribute = getAndMaybeCreateAttribute(name, value);
        attribute.getData().setElems(value);
        attribute.fireProductNodeDataChanged();
        setModified(true);
    }

    /**
     * Calls the base class version of this method, then if <code>modified</code> if <code>false</code>, sets the
     * modified flag of all children to <code>false</code>.
     *
     * @param modified <code>true</code> if this node has been modified, <code>false otherwise</code>
     *
     * @see ProductNode#setModified
     */
    public void setModified(boolean modified) {
        super.setModified(modified);
        if (!modified) {
            // inform children
            for (int i = 0; i < getNumElements(); i++) {
                getElementAt(i).setModified(false);
            }
            for (int i = 0; i < getNumAttributes(); i++) {
                getAttributeAt(i).setModified(false);
            }
        }
    }

    //////////////////////////////////////////////////////////////////////////
    // 'Visitor' pattern support


    /**
     * Accepts the given visitor. This method implements the well known 'Visitor' design pattern of the gang-of-four.
     * The visitor pattern allows to define new operations on the product data model without the need to add more code
     * to it. The new operation is implemented by the visitor.
     * <p/>
     * <p>The method first visits (calls <code>acceptVisitor</code> for) all elements contained in this element and then
     * visits all attributes. Finally the method calls <code>visitor.visit(this)</code>.
     *
     * @param visitor the visitor
     */
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

    private MetadataAttribute getAndMaybeCreateAttribute(final String name, final String value) {
        MetadataAttribute attribute = getAttribute(name);
        if (attribute == null) {
            final ProductData data = ProductData.createInstance(value);
            attribute = new MetadataAttribute(name, data, false);
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
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     */
    public void dispose() {
        if (_attributes != null) {
            _attributes.dispose();
            _attributes = null;
        }
        if (_elements != null) {
            _elements.dispose();
            _elements = null;
        }
        super.dispose();
    }
}



