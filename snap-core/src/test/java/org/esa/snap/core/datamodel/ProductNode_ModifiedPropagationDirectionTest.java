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

import junit.framework.TestCase;

public class ProductNode_ModifiedPropagationDirectionTest extends TestCase {

    public void testFireNodeModified_SequenceDirection() {
        final Product product = new Product("n", "t", 5, 5);
        final MetadataElement root = product.getMetadataRoot();
        final MetadataElement elem1 = new MetadataElement("elem1");
        final MetadataElement elem2 = new MetadataElement("elem2");
        final MetadataElement elem3 = new MetadataElement("elem3");
        final MetadataAttribute attrib = new MetadataAttribute("attrib",
                                                               ProductData.createInstance(new int[]{2, 3}),
                                                              false);

        assertEquals(false, product.isModified());
        assertEquals(false, root.isModified());
        assertEquals(false, elem1.isModified());
        assertEquals(false, elem2.isModified());
        assertEquals(false, elem3.isModified());
        assertEquals(false, attrib.isModified());

        elem3.addAttribute(attrib);
        elem2.addElement(elem3);
        elem1.addElement(elem2);
        root.addElement(elem1);

        assertEquals(true, product.isModified());
        assertEquals(true, root.isModified());
        assertEquals(true, elem1.isModified());
        assertEquals(true, elem2.isModified());
        assertEquals(true, elem3.isModified());
        assertEquals(false, attrib.isModified());

        attrib.setModified(true);

        assertEquals(true, product.isModified());
        assertEquals(true, root.isModified());
        assertEquals(true, elem1.isModified());
        assertEquals(true, elem2.isModified());
        assertEquals(true, elem3.isModified());
        assertEquals(true, attrib.isModified());

        product.setModified(false);

        assertEquals(false, product.isModified());
        assertEquals(false, root.isModified());
        assertEquals(false, elem1.isModified());
        assertEquals(false, elem2.isModified());
        assertEquals(false, elem3.isModified());
        assertEquals(false, attrib.isModified());

        elem2.setModified(true);

        assertEquals(true, product.isModified());
        assertEquals(true, root.isModified());
        assertEquals(true, elem1.isModified());
        assertEquals(true, elem2.isModified());
        assertEquals(false, elem3.isModified());
        assertEquals(false, attrib.isModified());

        root.setModified(false);

        assertEquals(true, product.isModified());
        assertEquals(false, root.isModified());
        assertEquals(false, elem1.isModified());
        assertEquals(false, elem2.isModified());
        assertEquals(false, elem3.isModified());
        assertEquals(false, attrib.isModified());
    }
}
