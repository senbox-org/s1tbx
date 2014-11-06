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

import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;
import static com.bc.ceres.binio.internal.CompoundTypeImplTest.DATASET_TYPE;
import static com.bc.ceres.binio.internal.CompoundTypeImplTest.RECORD_TYPE;
import junit.framework.TestCase;

public class SequenceTypeImplTest extends TestCase {

    public void testSimpleSequenceTypes() {
        testSequenceType(new SequenceTypeImpl(SimpleType.BYTE, 512), SimpleType.BYTE, 512, "byte[512]", 512);
        testSequenceType(new SequenceTypeImpl(SimpleType.UBYTE, 512), SimpleType.UBYTE, 512, "ubyte[512]", 512);
        testSequenceType(new SequenceTypeImpl(SimpleType.SHORT, 512), SimpleType.SHORT, 512, "short[512]", 2 * 512);
        testSequenceType(new SequenceTypeImpl(SimpleType.USHORT, 512), SimpleType.USHORT, 512, "ushort[512]", 2 * 512);
        testSequenceType(new SequenceTypeImpl(SimpleType.INT, 512), SimpleType.INT, 512, "int[512]", 4 * 512);
        testSequenceType(new SequenceTypeImpl(SimpleType.UINT, 512), SimpleType.UINT, 512, "uint[512]", 4 * 512);
        testSequenceType(new SequenceTypeImpl(SimpleType.FLOAT, 512), SimpleType.FLOAT, 512, "float[512]", 4 * 512);
        testSequenceType(new SequenceTypeImpl(SimpleType.DOUBLE, 512), SimpleType.DOUBLE, 512, "double[512]", 8 * 512);

        testSequenceType(new GrowableSequenceTypeImpl(SimpleType.BYTE), SimpleType.BYTE, 0, "byte[]", 0);
        testSequenceType(new GrowableSequenceTypeImpl(SimpleType.UBYTE), SimpleType.UBYTE, 0, "ubyte[]", 0);
        testSequenceType(new GrowableSequenceTypeImpl(SimpleType.SHORT), SimpleType.SHORT, 0, "short[]", 0);
        testSequenceType(new GrowableSequenceTypeImpl(SimpleType.USHORT), SimpleType.USHORT, 0, "ushort[]", 0);
        testSequenceType(new GrowableSequenceTypeImpl(SimpleType.INT), SimpleType.INT, 0, "int[]", 0);
        testSequenceType(new GrowableSequenceTypeImpl(SimpleType.UINT), SimpleType.UINT, 0, "uint[]", 0);
        testSequenceType(new GrowableSequenceTypeImpl(SimpleType.FLOAT), SimpleType.FLOAT, 0, "float[]", 0);
        testSequenceType(new GrowableSequenceTypeImpl(SimpleType.DOUBLE), SimpleType.DOUBLE, 0, "double[]", 0);
    }

    public void testComplexSequenceTypes() {
        SequenceType sf = new GrowableSequenceTypeImpl(SimpleType.FLOAT);
        SequenceType ssf = new GrowableSequenceTypeImpl(sf);
        SequenceType sssf = new GrowableSequenceTypeImpl(ssf);
        testSequenceType(sssf, ssf, 0, "float[][][]", 0);

        sf = new SequenceTypeImpl(SimpleType.FLOAT, 32);
        ssf = new GrowableSequenceTypeImpl(sf);
        sssf = new GrowableSequenceTypeImpl(ssf);
        testSequenceType(sssf, ssf, 0, "float[32][][]", 0);

        sf = new SequenceTypeImpl(SimpleType.FLOAT, 32);
        ssf = new SequenceTypeImpl(sf, 43);
        sssf = new GrowableSequenceTypeImpl(ssf);
        testSequenceType(sssf, ssf, 0, "float[32][43][]", 0);

        sf = new SequenceTypeImpl(SimpleType.FLOAT, 32);
        ssf = new SequenceTypeImpl(sf, 43);
        sssf = new SequenceTypeImpl(ssf, 8);
        testSequenceType(sssf, ssf, 8, "float[32][43][8]", 8 * 43 * 32 * 4);

        SequenceType sr = new SequenceTypeImpl(RECORD_TYPE, 300);
        testSequenceType(sr, RECORD_TYPE, 300, "Record[300]", 300 * (2 + 4 + 2 * 8 + 16));
        sr = new GrowableSequenceTypeImpl(RECORD_TYPE);
        testSequenceType(sr, RECORD_TYPE, 0, "Record[]", 0);

        SequenceType sds = new SequenceTypeImpl(DATASET_TYPE, 12);
        testSequenceType(sds, DATASET_TYPE, 12, "Dataset[12]", 48);
        sds = new GrowableSequenceTypeImpl(DATASET_TYPE);
        testSequenceType(sds, DATASET_TYPE, 0, "Dataset[]", 0);
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