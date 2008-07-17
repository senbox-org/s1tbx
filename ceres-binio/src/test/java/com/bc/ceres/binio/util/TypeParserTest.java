package com.bc.ceres.binio.util;

import com.bc.ceres.binio.CompoundType;
import com.bc.ceres.binio.SequenceType;
import com.bc.ceres.binio.SimpleType;
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
        Reader reader = new StringReader(code);
        CompoundType[] types = TypeParser.parseUnit(reader);
        assertNotNull(types);
        assertEquals(1, types.length);
        assertEquals("Point", types[0].getName());
        assertEquals(3, types[0].getMemberCount());
        assertEquals("x", types[0].getMember(0).getName());
        assertEquals("y", types[0].getMember(1).getName());
        assertEquals("z", types[0].getMember(2).getName());
        assertSame(SimpleType.DOUBLE, types[0].getMember(0).getType());
        assertSame(SimpleType.DOUBLE, types[0].getMember(1).getType());
        assertSame(SimpleType.DOUBLE, types[0].getMember(2).getType());
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
        Reader reader = new StringReader(code);
        CompoundType[] types = TypeParser.parseUnit(reader);
        assertNotNull(types);
        assertEquals(2, types.length);
        assertEquals("Point", types[0].getName());
        assertEquals(3, types[0].getMemberCount());
        assertEquals("x", types[0].getMember(0).getName());
        assertEquals("y", types[0].getMember(1).getName());
        assertEquals("z", types[0].getMember(2).getName());

        assertEquals("Complex", types[1].getName());
        assertEquals(2, types[1].getMemberCount());
        assertEquals("real", types[1].getMember(0).getName());
        assertEquals("imag", types[1].getMember(1).getName());

        assertSame(types[1], types[0].getMember(1).getType());
        assertSame(types[1], types[0].getMember(1).getType());
        assertSame(types[1], types[0].getMember(2).getType());
    }

    public void testSimpleFixSequence() throws IOException, ParseException {
        String code = "" +
                "Scanline {" +
                "  int flags;" +
                "  double[512] scan;" +
                "}";
        Reader reader = new StringReader(code);
        CompoundType[] types = TypeParser.parseUnit(reader);
        assertNotNull(types);
        assertEquals(1, types.length);
        assertEquals("Scanline", types[0].getName());
        assertEquals(2, types[0].getMemberCount());
        assertEquals("flags", types[0].getMember(0).getName());
        assertSame(SimpleType.INT, types[0].getMember(0).getType());
        assertEquals("scan", types[0].getMember(1).getName());
        assertTrue(types[0].getMember(1).getType() instanceof SequenceType);
        SequenceType sequenceType = (SequenceType) types[0].getMember(1).getType();
        assertSame(SimpleType.DOUBLE, sequenceType.getElementType());
        assertEquals(512, sequenceType.getElementCount());
    }

    public void testSimpleVarSequence() throws IOException, ParseException {
        String code = "" +
                "Scanline {" +
                "  int flags;" +
                "  double[] data;" +
                "}";
        Reader reader = new StringReader(code);
        CompoundType[] types = TypeParser.parseUnit(reader);
        assertNotNull(types);
        assertEquals(1, types.length);
        assertEquals("Scanline", types[0].getName());
        assertEquals(2, types[0].getMemberCount());
        assertEquals("flags", types[0].getMember(0).getName());
        assertSame(SimpleType.INT, types[0].getMember(0).getType());
        assertEquals("data", types[0].getMember(1).getName());
        assertTrue(types[0].getMember(1).getType() instanceof SequenceType);
        SequenceType sequenceType = (SequenceType) types[0].getMember(1).getType();
        assertSame(SimpleType.DOUBLE, sequenceType.getElementType());
        assertEquals(-1, sequenceType.getElementCount());
    }

    public void testSequenceOfSequences() throws IOException, ParseException {
        String code = "" +
                "Matrix {" +
                "  double[4][3] data;" +
                "}";
        Reader reader = new StringReader(code);
        CompoundType[] types = TypeParser.parseUnit(reader);
        assertNotNull(types);
        assertEquals(1, types.length);
        assertEquals("Matrix", types[0].getName());
        assertEquals(1, types[0].getMemberCount());
        assertEquals("data", types[0].getMember(0).getName());
        assertTrue(types[0].getMember(0).getType() instanceof SequenceType);
        SequenceType sequenceType = (SequenceType) types[0].getMember(0).getType();
        assertEquals(3, sequenceType.getElementCount());
        assertTrue(sequenceType.getElementType() instanceof SequenceType);
        SequenceType sequenceType2 = (SequenceType) sequenceType.getElementType();
        assertEquals(4, sequenceType2.getElementCount());
        assertSame(SimpleType.DOUBLE, sequenceType2.getElementType());
    }

//    public void testSimpleVarSequenceWithReference() throws IOException, ParseException {
//        String code = "" +
//                "Dataset {" +
//                "  int lineCount;" +
//                "  Scanline[$lineCount] scanlines;" +
//                "}" +
//                "" +
//                "Scanline {" +
//                "  int flags;" +
//                "  double[512] data;" +
//                "}";
//        Reader reader = new StringReader(code);
//        CompoundType[] types = TypeParser.parseCompoundTypes(reader);
//        assertNotNull(types);
//        assertEquals(1, types.length);
//        assertEquals("Scanline", types[0].getName());
//        assertEquals(2, types[0].getMemberCount());
//        assertEquals("flags", types[0].getMember(0).getName());
//        assertSame(SimpleType.INT, types[0].getMember(0).getType());
//        assertEquals("scan", types[0].getMember(1).getName());
//        assertTrue(types[0].getMember(1).getType() instanceof SequenceType);
//        SequenceType sequenceType = (SequenceType) types[0].getMember(1).getType();
//        assertSame(SimpleType.DOUBLE, sequenceType.getElementType());
//        assertEquals(-1, sequenceType.getElementCount());
//    }

}