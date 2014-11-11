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

package com.bc.ceres.binio.util;

import com.bc.ceres.binio.CompoundMember;
import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.SimpleType;
import com.bc.ceres.binio.Type;
import junit.framework.TestCase;

import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.text.ParseException;

public class TypeParserTest extends TestCase {
    public void testSimpleCompound() throws IOException, ParseException {
        String code = "" +
                "Point {" +
                "  double x;" +
                "  double y;" +
                "  double z;" +
                "}";
        CompoundType[] types = parseTypes(code);
        assertNotNull(types);
        assertEquals(1, types.length);
        testPoint(types[0], SimpleType.DOUBLE);
    }

    public void testCompoundInCompound() throws IOException, ParseException {
        String code = "" +
                "Point {" +
                "  Complex x;" +
                "  Complex y;" +
                "  Complex z;" +
                "}" +
                "" +
                "Complex {" +
                "  double real;" +
                "  double imag;" +
                "}";
        CompoundType[] types = parseTypes(code);
        assertNotNull(types);
        assertEquals(2, types.length);
        testPoint(types[0], types[1]);
        testComplex(types[1]);

        assertSame(types[1], types[0].getMember(1).getType());
        assertSame(types[1], types[0].getMember(1).getType());
        assertSame(types[1], types[0].getMember(2).getType());
    }

    private static void testPoint(CompoundType type, Type memberType) {
        assertNotNull(type);
        assertEquals("Point", type.getName());
        assertEquals(3, type.getMemberCount());
        assertEquals("x", type.getMember(0).getName());
        assertEquals("y", type.getMember(1).getName());
        assertEquals("z", type.getMember(2).getName());
        assertSame(memberType, type.getMember(0).getType());
        assertSame(memberType, type.getMember(1).getType());
        assertSame(memberType, type.getMember(2).getType());
    }

    private static void testComplex(CompoundType type) {
        assertEquals("Complex", type.getName());
        assertEquals(2, type.getMemberCount());
        assertEquals("real", type.getMember(0).getName());
        assertEquals("imag", type.getMember(1).getName());
    }

    public void testSimpleFixSequence() throws IOException, ParseException {
        String code = "" +
                "Scanline {" +
                "  uint flags;" +
                "  double[512] data;" +
                "}";
        CompoundType[] types = parseTypes(code);
        assertNotNull(types);
        assertEquals(1, types.length);
        testScanline(types[0]);
    }

    private static void testScanline(CompoundType type) {
        assertNotNull(type);
        assertEquals("Scanline", type.getName());
        assertEquals(2, type.getMemberCount());
        CompoundMember member1 = type.getMember(0);
        CompoundMember member2 = type.getMember(1);
        assertNotNull(member1);
        assertNotNull(member2);
        assertEquals("flags", member1.getName());
        assertSame(SimpleType.UINT, member1.getType());
        assertEquals("data", member2.getName());
        assertTrue(member2.getType() instanceof SequenceType);
        SequenceType sequenceType = (SequenceType) member2.getType();
        assertSame(SimpleType.DOUBLE, sequenceType.getElementType());
        assertEquals(512, sequenceType.getElementCount());
        assertEquals(4 + 512 * 8, type.getSize());
    }

    public void testSequenceOfSequences() throws IOException, ParseException {
        String code = "" +
                "Matrix {" +
                "  double[4][3] data;" +
                "}";
        CompoundType[] types = parseTypes(code);
        assertNotNull(types);
        assertEquals(1, types.length);
        testMatrix(types[0]);
    }

    private static void testMatrix(CompoundType type) {
        assertEquals("Matrix", type.getName());
        assertEquals(1, type.getMemberCount());
        assertEquals("data", type.getMember(0).getName());
        assertTrue(type.getMember(0).getType() instanceof SequenceType);
        SequenceType sequenceType = (SequenceType) type.getMember(0).getType();
        assertEquals(3, sequenceType.getElementCount());
        assertTrue(sequenceType.getElementType() instanceof SequenceType);
        SequenceType sequenceType2 = (SequenceType) sequenceType.getElementType();
        assertEquals(4, sequenceType2.getElementCount());
        assertSame(SimpleType.DOUBLE, sequenceType2.getElementType());
    }

    public void testSimpleVarSequenceWithReference() throws IOException, ParseException {
        String code = "" +
                "Dataset {" +
                "  int lineCount;" +
                "  Scanline[lineCount] scanlines;" +
                "}" +
                "" +
                "Scanline {" +
                "  uint flags;" +
                "  double[512] data;" +
                "}";
        CompoundType[] types = parseTypes(code);
        assertNotNull(types);
        assertEquals(2, types.length);
        CompoundType datasetType = types[0];
        CompoundType scanlineType = types[1];

        testDataset(datasetType, scanlineType, -1);
        testScanline(scanlineType);
    }

    public void testGrowableSequence() throws IOException, ParseException {
        String code = "" +
                "Dataset {" +
                "  int lineCount;" +
                "  Scanline[] scanlines;" +
                "}" +
                "" +
                "Scanline {" +
                "  uint flags;" +
                "  double[512] data;" +
                "}";
        CompoundType[] types = parseTypes(code);
        assertNotNull(types);
        assertEquals(2, types.length);
        CompoundType datasetType = types[0];
        CompoundType scanlineType = types[1];

        testDataset(datasetType, scanlineType, 0);
        testScanline(scanlineType);
    }

    private static void testDataset(CompoundType datasetType, CompoundType scanlineType, int elementCount) {
        assertEquals("Dataset", datasetType.getName());
        assertEquals(2, datasetType.getMemberCount());
        CompoundMember member1 = datasetType.getMember(0);
        CompoundMember member2 = datasetType.getMember(1);

        assertEquals("lineCount", member1.getName());
        assertSame(SimpleType.INT, member1.getType());

        assertEquals("scanlines", member2.getName());
        assertTrue(member2.getType() instanceof SequenceType);
        assertSame(scanlineType, ((SequenceType) member2.getType()).getElementType());
        assertEquals(elementCount, ((SequenceType) member2.getType()).getElementCount());
    }

    private static CompoundType[] parseTypes(String code) throws IOException, ParseException {
        Reader reader = new StringReader(code);
        return TypeParser.parseUnit(reader);
    }

}