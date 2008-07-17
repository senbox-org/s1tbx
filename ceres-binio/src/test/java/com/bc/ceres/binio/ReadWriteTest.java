package com.bc.ceres.binio;

import com.bc.ceres.binio.util.ByteArrayIOHandler;
import static com.bc.ceres.binio.util.TypeBuilder.*;
import junit.framework.TestCase;

import java.io.File;
import java.io.IOException;

public class ReadWriteTest extends TestCase {
    @Override
    protected void setUp() throws Exception {
        new File("test.dat").delete();
    }

    @Override
    protected void tearDown() throws Exception {
        new File("test.dat").delete();
    }

    public void testFixCompound() throws IOException {

        CompoundType type = COMP("Complex", MEMBER("x", DOUBLE), MEMBER("y", DOUBLE));

        ByteArrayIOHandler byteArrayIOHandler = new ByteArrayIOHandler();
        TracingIOHandler tracingIOHandler = new TracingIOHandler(byteArrayIOHandler);
        IOContext context = new IOContext(new Format(type), tracingIOHandler);
        CompoundData complex = context.getData();
        complex.setDouble("x", 23.04);
        complex.setDouble("y", 10.12);
        complex.flush();
        assertEquals("R(0,16)W(0,16)", tracingIOHandler.getTrace());
        tracingIOHandler.reset();
        assertEquals(23.04, complex.getDouble("x"), 1e-10);
        assertEquals(10.12, complex.getDouble("y"), 1e-10);
        assertEquals("", tracingIOHandler.getTrace());

        final byte[] byteData = byteArrayIOHandler.toByteArray();
        byteArrayIOHandler = new ByteArrayIOHandler(byteData);
        tracingIOHandler = new TracingIOHandler(byteArrayIOHandler);
        context = new IOContext(new Format(type), tracingIOHandler);
        complex = context.getData();
        assertEquals(23.04, complex.getDouble("x"), 1e-10);
        assertEquals(10.12, complex.getDouble("y"), 1e-10);
        assertEquals("R(0,16)", tracingIOHandler.getTrace());
    }

    public void testCompoundWithFixSequenceOfFixCompounds() throws IOException {

        CompoundType type =
                COMP("Data",
                     MEMBER("Complex_List",
                            SEQ(COMP("Complex",
                                     MEMBER("x", DOUBLE),
                                     MEMBER("y", DOUBLE)), 5)));

        ByteArrayIOHandler byteArrayIOHandler = new ByteArrayIOHandler();
        TracingIOHandler tracingIOHandler = new TracingIOHandler(byteArrayIOHandler);
        IOContext context = new IOContext(new Format(type), tracingIOHandler);
        CompoundData data = context.getData();
        SequenceData seq = data.getSequence("Complex_List");
        for (int i = 0; i < 5; i++) {
            CompoundData complex = seq.getCompound(i);
            complex.setDouble("x", i + 23.04);
            complex.setDouble("y", i + 10.12);
            complex.flush();
        }
        assertEquals("R(0,16)W(0,16)R(16,16)W(16,16)R(32,16)W(32,16)R(48,16)W(48,16)R(64,16)W(64,16)", tracingIOHandler.getTrace());

        tracingIOHandler.reset();
        for (int i = 0; i < 5; i++) {
            CompoundData complex = seq.getCompound(i);
            assertEquals(i + 23.04, complex.getDouble("x"), 1e-10);
            assertEquals(i + 10.12, complex.getDouble("y"), 1e-10);
        }
        assertEquals("R(0,16)R(16,16)R(32,16)R(48,16)R(64,16)", tracingIOHandler.getTrace());

        final byte[] byteData = byteArrayIOHandler.toByteArray();
        byteArrayIOHandler = new ByteArrayIOHandler(byteData);
        tracingIOHandler = new TracingIOHandler(byteArrayIOHandler);
        context = new IOContext(new Format(type), tracingIOHandler);
        data = context.getData();
        seq = data.getSequence("Complex_List");
        for (int i = 0; i < 5; i++) {
            CompoundData complex = seq.getCompound(i);
            assertEquals(i + 23.04, complex.getDouble("x"), 1e-10);
            assertEquals(i + 10.12, complex.getDouble("y"), 1e-10);
        }

        assertEquals("R(0,16)R(16,16)R(32,16)R(48,16)R(64,16)", tracingIOHandler.getTrace());
    }
}
