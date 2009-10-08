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