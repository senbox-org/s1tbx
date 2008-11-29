package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SimpleType;
import junit.framework.TestCase;

public class CompoundTypeImplTest extends TestCase {
    static final CompoundType RECORD_TYPE = new CompoundTypeImpl("Record", new CompoundMemberImpl[]{
            new CompoundMemberImpl("id", SimpleType.USHORT),
            new CompoundMemberImpl("flags", SimpleType.UINT),
            new CompoundMemberImpl("x", SimpleType.DOUBLE),
            new CompoundMemberImpl("y", SimpleType.DOUBLE),
            new CompoundMemberImpl("z", SimpleType.DOUBLE),
    });
    static final CompoundType DATASET_TYPE = new CompoundTypeImpl("Dataset", new CompoundMemberImpl[]{
            new CompoundMemberImpl("recordCount", SimpleType.INT),
            new CompoundMemberImpl("records", new SequenceTypeImpl(RECORD_TYPE)),
    });

    public void testCompoundTypes() {
        final CompoundType cr = RECORD_TYPE;
        assertEquals("Record", cr.getName());
        assertEquals(2 + 4 + 3 * 8, cr.getSize());
        assertEquals(5, cr.getMemberCount());
        assertEquals(SimpleType.USHORT, cr.getMemberType(0));
        assertEquals(SimpleType.UINT, cr.getMemberType(1));
        assertEquals(SimpleType.DOUBLE, cr.getMemberType(2));
        assertEquals(SimpleType.DOUBLE, cr.getMemberType(3));
        assertEquals(SimpleType.DOUBLE, cr.getMemberType(4));
        assertEquals(-1, cr.getMemberIndex("ID"));
        assertEquals(0, cr.getMemberIndex("id"));
        assertEquals(1, cr.getMemberIndex("flags"));
        assertEquals(2, cr.getMemberIndex("x"));
        assertEquals(3, cr.getMemberIndex("y"));
        assertEquals(4, cr.getMemberIndex("z"));

        final CompoundType cd = DATASET_TYPE;
        assertEquals("Dataset", cd.getName());
        assertEquals(-1, cd.getSize());
        assertEquals(2, cd.getMemberCount());
        assertEquals(SimpleType.INT, cd.getMemberType(0));
        assertEquals(-1, cd.getMemberIndex("recordcount"));
        assertEquals(0, cd.getMemberIndex("recordCount"));
        assertEquals(1, cd.getMemberIndex("records"));
    }
}