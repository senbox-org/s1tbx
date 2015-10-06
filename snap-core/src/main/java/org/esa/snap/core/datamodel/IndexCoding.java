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

import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.Guardian;


/**
 * Provides the information required to decode integer sample values that
 * represent index values (e.g. types, classes, categories).
 * @since BEAM 4.2
 */
public class IndexCoding extends SampleCoding {

    /**
     * Constructs a new index coding object with the given name.
     *
     * @param name the name
     */
    public IndexCoding(String name) {
        super(name);
    }
    
    /**
     * Returns a metadata attribute wich is the representation of the index with the given name. This method delegates to
     * getPropertyValue(String).
     *
     * @param name the flag name
     * @return a metadata attribute wich is the representation of the flag with the given name
     */
    public MetadataAttribute getIndex(String name) {
        return getAttribute(name);
    }

    /**
     * Returns a string array which contains the names of all indexes contained in this <code>IndexCoding</code> object.
     *
     * @return a string array which contains all names of this <code>FlagCoding</code>.<br> If this
     *         <code>FlagCoding</code> does not contain any flag, <code>null</code> is returned
     */
    public String[] getIndexNames() {
        return getAttributeNames();
    }

    /**
     * Adds a new index definition to this flags coding.
     *
     * @param name        the index name
     * @param value       the index value
     * @param description the description text
     * @throws IllegalArgumentException if <code>name</code> is null
     * @return A new attribute representing the coded index. 
     */
    public MetadataAttribute addIndex(String name, int value, String description) {
        return addSample(name, value, description);
    }

    /**
     * Returns the flag mask value for the specified flag name.
     *
     * @param name the flag name
     * @return flagMask the flag's bit mask as a 32 bit integer
     * @throws IllegalArgumentException if <code>name</code> is null, or a flag with the name does not exist
     */
    public int getIndexValue(String name) {
        Guardian.assertNotNull("name", name);
        MetadataAttribute attribute = getAttribute(name);
        if (attribute == null) {
            throw new IllegalArgumentException("index '" + name + "' not found");
        }
        Debug.assertTrue(attribute.getData().isInt());
        Debug.assertTrue(attribute.getData().isScalar());
        return attribute.getData().getElemInt();
    }

    //////////////////////////////////////////////////////////////////////////
    // Visitor-Pattern support

    /**
     * Accepts the given visitor. This method implements the well known 'Visitor' design pattern of the gang-of-four.
     * The visitor pattern allows to define new operations on the product data model without the need to add more code
     * to it. The new operation is implemented by the visitor.
     * <p>The method simply calls <code>visitor.visit(this)</code>.
     *
     * @param visitor the visitor, must not be <code>null</code>
     */
    @Override
    public void acceptVisitor(ProductVisitor visitor) {
        Guardian.assertNotNull("visitor", visitor);
        visitor.visit(this);
    }
}
