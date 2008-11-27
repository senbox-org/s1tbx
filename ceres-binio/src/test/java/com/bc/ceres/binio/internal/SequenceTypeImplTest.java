package com.bc.ceres.binio.internal;

import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;
import static com.bc.ceres.binio.internal.CompoundTypeImplTest.DATASET_TYPE;
import static com.bc.ceres.binio.internal.CompoundTypeImplTest.RECORD_TYPE;
import junit.framework.TestCase;

public class SequenceTypeImplTest extends TestCase {

    public void testSimpleSequenceTypes() {
        testSequenceType(new SequenceTypeImpl(SimpleType.BYTE), SimpleType.BYTE, -1, "byte[]", -1);
        testSequenceType(new SequenceTypeImpl(SimpleType.UBYTE), SimpleType.UBYTE, -1, "ubyte[]", -1);
        testSequenceType(new SequenceTypeImpl(SimpleType.SHORT), SimpleType.SHORT, -1, "short[]", -1);
        testSequenceType(new SequenceTypeImpl(SimpleType.USHORT), SimpleType.USHORT, -1, "ushort[]", -1);
        testSequenceType(new SequenceTypeImpl(SimpleType.INT), SimpleType.INT, -1, "int[]", -1);
        testSequenceType(new SequenceTypeImpl(SimpleType.UINT), SimpleType.UINT, -1, "uint[]", -1);
        testSequenceType(new SequenceTypeImpl(SimpleType.FLOAT), SimpleType.FLOAT, -1, "float[]", -1);
        testSequenceType(new SequenceTypeImpl(SimpleType.DOUBLE), SimpleType.DOUBLE, -1, "double[]", -1);

        testSequenceType(new SequenceTypeImpl(SimpleType.BYTE, 512), SimpleType.BYTE, 512, "byte[512]", 512);
        testSequenceType(new SequenceTypeImpl(SimpleType.UBYTE, 512), SimpleType.UBYTE, 512, "ubyte[512]", 512);
        testSequenceType(new SequenceTypeImpl(SimpleType.SHORT, 512), SimpleType.SHORT, 512, "short[512]", 2 * 512);
        testSequenceType(new SequenceTypeImpl(SimpleType.USHORT, 512), SimpleType.USHORT, 512, "ushort[512]", 2 * 512);
        testSequenceType(new SequenceTypeImpl(SimpleType.INT, 512), SimpleType.INT, 512, "int[512]", 4 * 512);
        testSequenceType(new SequenceTypeImpl(SimpleType.UINT, 512), SimpleType.UINT, 512, "uint[512]", 4 * 512);
        testSequenceType(new SequenceTypeImpl(SimpleType.FLOAT, 512), SimpleType.FLOAT, 512, "float[512]", 4 * 512);
        testSequenceType(new SequenceTypeImpl(SimpleType.DOUBLE, 512), SimpleType.DOUBLE, 512, "double[512]", 8 * 512);
    }

    public void testComplexSequenceTypes() {
        SequenceType sf = new SequenceTypeImpl(SimpleType.FLOAT);
        SequenceType ssf = new SequenceTypeImpl(sf);
        SequenceType sssf = new SequenceTypeImpl(ssf);
        testSequenceType(sssf, ssf, -1, "float[][][]", -1);

        sf = new SequenceTypeImpl(SimpleType.FLOAT, 32);
        ssf = new SequenceTypeImpl(sf);
        sssf = new SequenceTypeImpl(ssf);
        testSequenceType(sssf, ssf, -1, "float[32][][]", -1);

        sf = new SequenceTypeImpl(SimpleType.FLOAT, 32);
        ssf = new SequenceTypeImpl(sf, 43);
        sssf = new SequenceTypeImpl(ssf);
        testSequenceType(sssf, ssf, -1, "float[32][43][]", -1);

        sf = new SequenceTypeImpl(SimpleType.FLOAT, 32);
        ssf = new SequenceTypeImpl(sf, 43);
        sssf = new SequenceTypeImpl(ssf, 8);
        testSequenceType(sssf, ssf, 8, "float[32][43][8]", 8 * 43 * 32 * 4);

        SequenceType sr = new SequenceTypeImpl(RECORD_TYPE, 300);
        testSequenceType(sr, RECORD_TYPE, 300, "Record[300]", 300 * (2 + 4 + 3 * 8));
        sr = new SequenceTypeImpl(RECORD_TYPE);
        testSequenceType(sr, RECORD_TYPE, -1, "Record[]", -1);

        SequenceType sds = new SequenceTypeImpl(DATASET_TYPE, 12);
        testSequenceType(sds, DATASET_TYPE, 12, "Dataset[12]", -1);
        sds = new SequenceTypeImpl(DATASET_TYPE);
        testSequenceType(sds, DATASET_TYPE, -1, "Dataset[]", -1);
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


}