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

public class AbstractNamedNodeTest extends TestCase {

    public void testDummy() {
        // this dummy test is required for in order to avoid the following JUnit error message
        // junit.framework.AssertionFailedError: No tests found in org.esa.snap.core.datamodel.AbstractNamedNodeTest
    }


    public void testSetDescription(ProductNode namedNode) {

        // old value --> null ?
        namedNode.setDescription(null);
        assertEquals(null, namedNode.getDescription());
        assertEquals(false, namedNode.isModified());

        // null --> new value: is modified ?
        namedNode.setDescription("The sensor type");
        assertEquals("The sensor type", namedNode.getDescription());
        assertEquals(true, namedNode.isModified());

        // old value == new value?
        namedNode.setModified(false);
        namedNode.setDescription("The sensor type");
        assertEquals("The sensor type", namedNode.getDescription());
        assertEquals(false, namedNode.isModified());

        // old value != new value?
        namedNode.setDescription("Upper left point");
        assertEquals("Upper left point", namedNode.getDescription());
        assertEquals(true, namedNode.isModified());
    }

    public void testSetUnit(DataNode dataNode) {

        // old value --> null ?
        dataNode.setUnit(null);
        assertEquals(null, dataNode.getUnit());
        assertEquals(false, dataNode.isModified());

        // null --> new value: is modified ?
        dataNode.setUnit("mg/m^3");
        assertEquals("mg/m^3", dataNode.getUnit());
        assertEquals(true, dataNode.isModified());

        // old value == new value?
        dataNode.setModified(false);
        dataNode.setUnit("mg/m^3");
        assertEquals("mg/m^3", dataNode.getUnit());
        assertEquals(false, dataNode.isModified());

        // old value != new value?
        dataNode.setUnit("g/cm^3");
        assertEquals("g/cm^3", dataNode.getUnit());
        assertEquals(true, dataNode.isModified());

    }
}
