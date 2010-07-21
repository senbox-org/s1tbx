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

package com.bc.ceres.binio;

import junit.framework.TestCase;

public class FormatTest extends TestCase {

    public void testBasisFormat() {
        DataFormat basisFormat = new DataFormat();
        DataFormat format = new DataFormat();
        assertNull(format.getBasisFormat());
        format.setBasisFormat(basisFormat);
        assertSame(basisFormat, format.getBasisFormat());

        basisFormat.addTypeDef("string80", TypeBuilder.SEQUENCE(SimpleType.BYTE, 80));
        assertTrue(format.isTypeDef("string80"));
        assertTrue(format.getTypeDef("string80") == basisFormat.getTypeDef("string80"));

        format.addTypeDef("string80", TypeBuilder.SEQUENCE(SimpleType.USHORT, 80));
        assertTrue(format.isTypeDef("string80"));
        assertTrue(format.getTypeDef("string80") != basisFormat.getTypeDef("string80"));

        format.removeTypeDef("string80");
        assertTrue(format.isTypeDef("string80"));
        assertTrue(format.getTypeDef("string80") == basisFormat.getTypeDef("string80"));

        basisFormat.removeTypeDef("string80");
        assertFalse(format.isTypeDef("string80"));
    }

    public void testTypeDef() {
        DataFormat format = new DataFormat(TypeBuilder.COMPOUND("Point",
                                                                TypeBuilder.MEMBER("x", SimpleType.FLOAT),
                                                                TypeBuilder.MEMBER("y", SimpleType.FLOAT)));

        assertEquals(false, format.isTypeDef("bool"));
        try {
            format.getTypeDef("bool");
            fail();
        } catch (IllegalArgumentException e) {
            // ok
        }
        format.addTypeDef("bool", SimpleType.BYTE);
        assertSame(SimpleType.BYTE, format.getTypeDef("bool"));
        assertEquals(true, format.isTypeDef("bool"));

        assertEquals(false, format.isTypeDef("ui32"));
        format.addTypeDef("ui32", SimpleType.UINT);
        assertSame(SimpleType.UINT, format.getTypeDef("ui32"));


        assertSame(SimpleType.BYTE, format.getTypeDef("bool"));
        format.addTypeDef("bool", SimpleType.BYTE);
        try {
            format.addTypeDef("bool", SimpleType.SHORT);
            fail();
        } catch (IllegalArgumentException e) {
// ok
        }
        Type type = format.removeTypeDef("bool");
        assertSame(SimpleType.BYTE, type);
        assertEquals(false, format.isTypeDef("bool"));
        format.addTypeDef("bool", SimpleType.BYTE);
        assertEquals(true, format.isTypeDef("bool"));
    }
}
