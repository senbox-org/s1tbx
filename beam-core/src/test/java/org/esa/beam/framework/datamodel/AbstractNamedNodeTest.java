/*
 * $Id: AbstractNamedNodeTest.java,v 1.1.1.1 2006/09/11 08:16:51 norman Exp $
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

import junit.framework.TestCase;

public class AbstractNamedNodeTest extends TestCase {

    public AbstractNamedNodeTest(String testName) {
        super(testName);
    }

    public void testDummy() {
        // this dummy test is required for in order to avoid the following JUnit error message
        // junit.framework.AssertionFailedError: No tests found in org.esa.beam.framework.datamodel.AbstractNamedNodeTest
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
