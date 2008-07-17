package com.bc.ceres.binio;

import junit.framework.TestCase;

public class TypeTest extends TestCase {
    private static final CompoundType RECORD_TYPE = new CompoundType("Record", new CompoundType.Member[]{
            new CompoundType.Member("id", SimpleType.USHORT),
            new CompoundType.Member("flags", SimpleType.UINT),
            new CompoundType.Member("x", SimpleType.DOUBLE),
            new CompoundType.Member("y", SimpleType.DOUBLE),
            new CompoundType.Member("z", SimpleType.DOUBLE),
    });
    private static final CompoundType DATASET_TYPE = new CompoundType("Dataset", new CompoundType.Member[]{
            new CompoundType.Member("recordCount", SimpleType.INT),
            new CompoundType.Member("records", new SequenceType(RECORD_TYPE)),
    });

    public void testSimpleTypes() {
        testSimpleType(SimpleType.BYTE, "byte", 1);
        testSimpleType(SimpleType.UBYTE, "ubyte", 1);
        testSimpleType(SimpleType.SHORT, "short", 2);
        testSimpleType(SimpleType.USHORT, "ushort", 2);
        testSimpleType(SimpleType.INT, "int", 4);
        testSimpleType(SimpleType.UINT, "uint", 4);
        testSimpleType(SimpleType.LONG, "long", 8);
        testSimpleType(SimpleType.FLOAT, "float", 4);
        testSimpleType(SimpleType.DOUBLE, "double", 8);
    }

    public void testSimpleSequenceTypes() {
        testSequenceType(new SequenceType(SimpleType.BYTE), SimpleType.BYTE, -1, "byte[]", -1);
        testSequenceType(new SequenceType(SimpleType.UBYTE), SimpleType.UBYTE, -1, "ubyte[]", -1);
        testSequenceType(new SequenceType(SimpleType.SHORT), SimpleType.SHORT, -1, "short[]", -1);
        testSequenceType(new SequenceType(SimpleType.USHORT), SimpleType.USHORT, -1, "ushort[]", -1);
        testSequenceType(new SequenceType(SimpleType.INT), SimpleType.INT, -1, "int[]", -1);
        testSequenceType(new SequenceType(SimpleType.UINT), SimpleType.UINT, -1, "uint[]", -1);
        testSequenceType(new SequenceType(SimpleType.FLOAT), SimpleType.FLOAT, -1, "float[]", -1);
        testSequenceType(new SequenceType(SimpleType.DOUBLE), SimpleType.DOUBLE, -1, "double[]", -1);

        testSequenceType(new SequenceType(SimpleType.BYTE, 512), SimpleType.BYTE, 512, "byte[512]", 512);
        testSequenceType(new SequenceType(SimpleType.UBYTE, 512), SimpleType.UBYTE, 512, "ubyte[512]", 512);
        testSequenceType(new SequenceType(SimpleType.SHORT, 512), SimpleType.SHORT, 512, "short[512]", 2 * 512);
        testSequenceType(new SequenceType(SimpleType.USHORT, 512), SimpleType.USHORT, 512, "ushort[512]", 2 * 512);
        testSequenceType(new SequenceType(SimpleType.INT, 512), SimpleType.INT, 512, "int[512]", 4 * 512);
        testSequenceType(new SequenceType(SimpleType.UINT, 512), SimpleType.UINT, 512, "uint[512]", 4 * 512);
        testSequenceType(new SequenceType(SimpleType.FLOAT, 512), SimpleType.FLOAT, 512, "float[512]", 4 * 512);
        testSequenceType(new SequenceType(SimpleType.DOUBLE, 512), SimpleType.DOUBLE, 512, "double[512]", 8 * 512);
    }

    public void testComplexSequenceTypes() {
        SequenceType sf = new SequenceType(SimpleType.FLOAT);
        SequenceType ssf = new SequenceType(sf);
        SequenceType sssf = new SequenceType(ssf);
        testSequenceType(sssf, ssf, -1, "float[][][]", -1);

        sf = new SequenceType(SimpleType.FLOAT, 32);
        ssf = new SequenceType(sf);
        sssf = new SequenceType(ssf);
        testSequenceType(sssf, ssf, -1, "float[32][][]", -1);

        sf = new SequenceType(SimpleType.FLOAT, 32);
        ssf = new SequenceType(sf, 43);
        sssf = new SequenceType(ssf);
        testSequenceType(sssf, ssf, -1, "float[32][43][]", -1);

        sf = new SequenceType(SimpleType.FLOAT, 32);
        ssf = new SequenceType(sf, 43);
        sssf = new SequenceType(ssf, 8);
        testSequenceType(sssf, ssf, 8, "float[32][43][8]", 8 * 43 * 32 * 4);

        SequenceType sr = new SequenceType(RECORD_TYPE, 300);
        testSequenceType(sr, RECORD_TYPE, 300, "Record[300]", 300 * (2 + 4 + 3 * 8));
        sr = new SequenceType(RECORD_TYPE);
        testSequenceType(sr, RECORD_TYPE, -1, "Record[]", -1);

        SequenceType sds = new SequenceType(DATASET_TYPE, 12);
        testSequenceType(sds, DATASET_TYPE, 12, "Dataset[12]", -1);
        sds = new SequenceType(DATASET_TYPE);
        testSequenceType(sds, DATASET_TYPE, -1, "Dataset[]", -1);
    }

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

        final CompoundType cd = DATASET_TYPE;
        assertEquals("Dataset", cd.getName());
        assertEquals(-1, cd.getSize());
        assertEquals(2, cd.getMemberCount());
        assertEquals(SimpleType.INT, cd.getMemberType(0));
    }

    private static void testSequenceType(SequenceType sequenceType, Type expectedElementType, int expectedElementCount, String expectedName, int expectedSize) {
        assertEquals(expectedName, sequenceType.getName());
        assertEquals(expectedSize, sequenceType.getSize());
        assertSame(expectedElementType, sequenceType.getElementType());
        assertEquals(expectedElementCount, sequenceType.getElementCount());
        assertEquals(false, sequenceType.isSimpleType());
        assertEquals(true, sequenceType.isCollectionType());
        assertEquals(true, sequenceType.isSequenceType());
        assertEquals(false, sequenceType.isCompoundType());
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
