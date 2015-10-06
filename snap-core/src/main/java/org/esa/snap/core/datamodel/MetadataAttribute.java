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

import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ObjectUtils;

/**
 * A <code>MetadataAttribute</code> is part of a <code>{@link MetadataElement}</code> and represents a key/value pair.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class MetadataAttribute extends DataNode {

    public MetadataAttribute(String name, int type) {
        this(name, type, 1);
    }

    public MetadataAttribute(String name, int type, int numElems) {
        this(name, ProductData.createInstance(type, numElems), false);
    }

    public MetadataAttribute(String name, ProductData data, boolean readOnly) {
        super(name, data, readOnly);
    }

    public MetadataElement getParentElement() {
        return MetadataElement.getParentElement(this);
    }

    @Override
    public boolean equals(Object object) {

        if (!super.equals(object)) {
            return false;
        }

        MetadataAttribute attribute = (MetadataAttribute) object;

        return ObjectUtils.equalObjects(attribute.getData(), getData());

    }

    //////////////////////////////////////////////////////////////////////////
    // Visitor-Pattern support

    /**
     * Accepts the given visitor. This method implements the well known 'Visitor' design pattern of the gang-of-four.
     * The visitor pattern allows to define new operations on the product data model without the need to add more code
     * to it. The new operation is implemented by the visitor.
     * <p>The method simply calls <code>visitor.visit(this)</code>.
     *
     * @param visitor the visitor
     */
    @Override
    public void acceptVisitor(ProductVisitor visitor) {
        Guardian.assertNotNull("visitor", visitor);
        visitor.visit(this);
    }

    public MetadataAttribute createDeepClone() {
        MetadataAttribute clone = new MetadataAttribute(getName(), getData().createDeepClone(), isReadOnly());
        clone.setDescription(getDescription());
        clone.setSynthetic(isSynthetic());
        clone.setUnit(getUnit());
        return clone;
    }
}
