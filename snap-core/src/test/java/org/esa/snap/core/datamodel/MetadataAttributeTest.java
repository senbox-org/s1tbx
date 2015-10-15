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

import java.util.Arrays;

public class MetadataAttributeTest extends AbstractNamedNodeTest {

    MetadataAttribute _attributeInt = null;
    MetadataAttribute _attributeFloat = null;
    MetadataAttribute _attributeString = null;

    @Override
    protected void setUp() {
        _attributeInt = new MetadataAttribute("attributeInt", ProductData.createInstance(ProductData.TYPE_INT32, 3),
                                              false);
        _attributeFloat = new MetadataAttribute("attributeFloat", ProductData.createInstance(ProductData.TYPE_FLOAT32),
                                                false);
        _attributeString = new MetadataAttribute("attributeString",
                                                 ProductData.createInstance(ProductData.TYPE_ASCII, 32), false);
    }

    @Override
    protected void tearDown() {
    }

    public void testRsAttribute() {
        try {
            new MetadataAttribute(null, ProductData.createInstance(ProductData.TYPE_FLOAT32),
                                  false);
            fail("new MetadataAttribute(null, false, Value.create(Value.TYPE_FLOAT32)) should not be possible");
        } catch (IllegalArgumentException e) {
        }

        try {
            new MetadataAttribute("a2", null, false);
            fail("new MetadataAttribute(\"a2\", false, null)) should not be possible");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testSetValueWithWrongType() {
        // rejects illegal type?
        try {
            _attributeInt.setDataElems("5");
            fail("IllegalArgumentException expected");
        } catch (IllegalArgumentException e) {
        }
    }

    public void testSetData() {

        try {
            _attributeInt.setDataElems(null);
            fail("IllegalArgumentException expected because data is null");
        } catch (IllegalArgumentException e) {
        }

        // null --> new value: is modified ?
        _attributeInt.setDataElems(new int[]{1, 2, 3});
        assertEquals(true, Arrays.equals(new int[]{1, 2, 3}, (int[]) _attributeInt.getDataElems()));
        assertEquals(true, _attributeInt.isModified());

        // old value == new value?
        _attributeInt.setDataElems(new int[]{1, 2, 3});
        assertEquals(true, Arrays.equals(new int[]{1, 2, 3}, (int[]) _attributeInt.getDataElems()));
        assertEquals(true, _attributeInt.isModified());
    }

    public void testSetDescription() {
        testSetDescription(_attributeInt);
        testSetDescription(_attributeFloat);
        testSetDescription(_attributeString);
    }

    public void testSetUnit() {
        testSetUnit(_attributeInt);
        testSetUnit(_attributeFloat);
        testSetUnit(_attributeString);
    }

    public void testASCIIAttribute() {
        final MetadataAttribute attribute1 = new MetadataAttribute("name", ProductData.TYPE_ASCII, 1);
        attribute1.getData().setElems("new data");
        //attribute should be of type ASCII and not INT8
        assertEquals(attribute1.getDataType(), ProductData.TYPE_ASCII);
        final MetadataAttribute attribute2 = new MetadataAttribute("name", new ProductData.ASCII("my string"), false);
        //attribute should be of type ASCII and not INT8
        assertEquals(attribute2.getDataType(), ProductData.TYPE_ASCII);
    }
}


