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


package org.esa.beam.dataio.modis.productdb;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

public class ModisTiePointDescriptionTest extends TestCase {

    public ModisTiePointDescriptionTest(String name) {
        super(name);
    }

    public static Test suite() {
        return new TestSuite(ModisTiePointDescriptionTest.class);
    }

    public void testTheFunctionality() {
        String expName = "tie_point_name";
        String expScale = "scale_name";
        String expOffset = "offset_name";
        String expUnit = "unit_name";

        // all value null allowed
        ModisTiePointDescription desc = new ModisTiePointDescription(null, null, null, null);
        assertEquals(null, desc.getName());
        assertEquals(null, desc.getScaleAttribName());
        assertEquals(null, desc.getOffsetAttribName());
        assertEquals(null, desc.getUnitAttribName());

        // check values one after the other
        desc = new ModisTiePointDescription(expName, null, null, null);
        assertEquals(expName, desc.getName());
        assertEquals(null, desc.getScaleAttribName());
        assertEquals(null, desc.getOffsetAttribName());
        assertEquals(null, desc.getUnitAttribName());

        desc = new ModisTiePointDescription(null, expScale, null, null);
        assertEquals(null, desc.getName());
        assertEquals(expScale, desc.getScaleAttribName());
        assertEquals(null, desc.getOffsetAttribName());
        assertEquals(null, desc.getUnitAttribName());

        desc = new ModisTiePointDescription(null, null, expOffset, null);
        assertEquals(null, desc.getName());
        assertEquals(null, desc.getScaleAttribName());
        assertEquals(expOffset, desc.getOffsetAttribName());
        assertEquals(null, desc.getUnitAttribName());

        desc = new ModisTiePointDescription(null, null, null, expUnit);
        assertEquals(null, desc.getName());
        assertEquals(null, desc.getScaleAttribName());
        assertEquals(null, desc.getOffsetAttribName());
        assertEquals(expUnit, desc.getUnitAttribName());
    }
}
