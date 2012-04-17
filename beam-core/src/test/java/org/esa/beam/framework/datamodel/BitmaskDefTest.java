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
package org.esa.beam.framework.datamodel;

import java.awt.Color;

import junit.framework.TestCase;

public class BitmaskDefTest extends TestCase {

    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testFail() {
        final BitmaskDef bitmaskDef = new BitmaskDef("name", "desc", "abc def ghi", Color.red, 0.5f);
        bitmaskDef.setModified(false);
        assertFalse(bitmaskDef.isModified());
        assertEquals("abc def ghi", bitmaskDef.getExpr());

        bitmaskDef.updateExpression("def", "BBBBBB");
        assertEquals("abc BBBBBB ghi", bitmaskDef.getExpr());
        assertTrue(bitmaskDef.isModified());
    }
}
