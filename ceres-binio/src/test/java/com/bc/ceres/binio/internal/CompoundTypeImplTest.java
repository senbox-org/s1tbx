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

package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SimpleType;
import junit.framework.TestCase;

public class CompoundTypeImplTest extends TestCase {

    static final CompoundType COMPLEX = new CompoundTypeImpl("Complex", new CompoundMemberImpl[]{
            new CompoundMemberImpl("real", SimpleType.DOUBLE),
            new CompoundMemberImpl("imag", SimpleType.DOUBLE),
    });
    static final CompoundType RECORD_TYPE = new CompoundTypeImpl("Record", new CompoundMemberImpl[]{
            new CompoundMemberImpl("id", SimpleType.USHORT),
            new CompoundMemberImpl("flags", SimpleType.UINT),
            new CompoundMemberImpl("x", SimpleType.DOUBLE),
            new CompoundMemberImpl("y", SimpleType.DOUBLE),
            new CompoundMemberImpl("z", COMPLEX),
    });
    static final CompoundType DATASET_TYPE = new CompoundTypeImpl("Dataset", new CompoundMemberImpl[]{
            new CompoundMemberImpl("recordCount", SimpleType.INT),
            new CompoundMemberImpl("records", new GrowableSequenceTypeImpl(RECORD_TYPE)),
    });

    public void testCompoundTypes() {
        final CompoundType rt = RECORD_TYPE;
        assertEquals("Record", rt.getName());
        assertEquals(2 + 4 + 2 * 8 + 16, rt.getSize());
        assertEquals(5, rt.getMemberCount());
        assertEquals(SimpleType.USHORT, rt.getMemberType(0));
        assertEquals(SimpleType.UINT, rt.getMemberType(1));
        assertEquals(SimpleType.DOUBLE, rt.getMemberType(2));
        assertEquals(SimpleType.DOUBLE, rt.getMemberType(3));
        assertEquals(COMPLEX, rt.getMemberType(4));
        assertEquals(-1, rt.getMemberIndex("ID"));
        assertEquals(0, rt.getMemberIndex("id"));
        assertEquals(1, rt.getMemberIndex("flags"));
        assertEquals(2, rt.getMemberIndex("x"));
        assertEquals(3, rt.getMemberIndex("y"));
        assertEquals(4, rt.getMemberIndex("z"));

        final CompoundType dst = DATASET_TYPE;
        assertEquals("Dataset", dst.getName());
        assertEquals(4, dst.getSize());
        assertEquals(2, dst.getMemberCount());
        assertEquals(SimpleType.INT, dst.getMemberType(0));
        assertEquals(-1, dst.getMemberIndex("recordcount"));
        assertEquals(0, dst.getMemberIndex("recordCount"));
        assertEquals(1, dst.getMemberIndex("records"));
    }
}