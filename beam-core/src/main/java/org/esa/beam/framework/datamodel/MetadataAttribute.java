/*
 * $Id: MetadataAttribute.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
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

import org.esa.beam.util.Guardian;
import org.esa.beam.util.ObjectUtils;

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
     * <p/>
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
