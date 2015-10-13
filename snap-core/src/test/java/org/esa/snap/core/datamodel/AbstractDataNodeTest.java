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


public class AbstractDataNodeTest extends AbstractNamedNodeTest {


    @Override
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
