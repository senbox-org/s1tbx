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

public class SimpleTypeTest extends TestCase {
    public void testSimpleTypes() {
        testSimpleType(SimpleType.BYTE, "byte", 1);
        testSimpleType(SimpleType.UBYTE, "ubyte", 1);
        testSimpleType(SimpleType.SHORT, "short", 2);
        testSimpleType(SimpleType.USHORT, "ushort", 2);
        testSimpleType(SimpleType.INT, "int", 4);
        testSimpleType(SimpleType.UINT, "uint", 4);
        testSimpleType(SimpleType.LONG, "long", 8);
        testSimpleType(SimpleType.ULONG, "ulong", 8);
        testSimpleType(SimpleType.FLOAT, "float", 4);
        testSimpleType(SimpleType.DOUBLE, "double", 8);
    }

    private static void testSimpleType(SimpleType type, String expectedName, int expectedSize) {
        assertEquals(expectedName, type.getName());
        assertEquals(expectedSize, type.getSize());
        assertEquals(true, type.isSimpleType());
        assertEquals(false, type.isCollectionType());
        assertEquals(false, type.isSequenceType());
        assertEquals(false, type.isCompoundType());
    }


}
