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

import static com.bc.ceres.binio.TypeBuilder.*;
import com.bc.ceres.binio.util.ByteArrayIOHandler;
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

        CompoundType type = COMPOUND("Complex", MEMBER("x", DOUBLE), MEMBER("y", DOUBLE));

        ByteArrayIOHandler byteArrayIOHandler = new ByteArrayIOHandler();
        TracingIOHandler tracingIOHandler = new TracingIOHandler(byteArrayIOHandler);
        DataContext context = new DataFormat(type).createContext(tracingIOHandler);
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
        context = new DataFormat(type).createContext(tracingIOHandler);
        complex = context.getData();
        assertEquals(23.04, complex.getDouble("x"), 1e-10);
        assertEquals(10.12, complex.getDouble("y"), 1e-10);
        assertEquals("R(0,16)", tracingIOHandler.getTrace());
    }

    public void testCompoundWithFixSequenceOfFixCompounds() throws IOException {

        CompoundType type =
                COMPOUND("Data",
                         MEMBER("Complex_List",
                                SEQUENCE(COMPOUND("Complex",
                                                  MEMBER("x", DOUBLE),
                                                  MEMBER("y", DOUBLE)), 5)));

        ByteArrayIOHandler byteArrayIOHandler = new ByteArrayIOHandler();
        TracingIOHandler tracingIOHandler = new TracingIOHandler(byteArrayIOHandler);
        DataContext context = new DataFormat(type).createContext(tracingIOHandler);
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
        context = new DataFormat(type).createContext(tracingIOHandler);
        data = context.getData();
        seq = data.getSequence("Complex_List");
        for (int i = 0; i < 5; i++) {
            CompoundData complex = seq.getCompound(i);
            assertEquals(i + 23.04, complex.getDouble("x"), 1e-10);
            assertEquals(i + 10.12, complex.getDouble("y"), 1e-10);
        }

        assertEquals("R(0,16)R(16,16)R(32,16)R(48,16)R(64,16)", tracingIOHandler.getTrace());
    }

    public void testWriteVarSequence() throws IOException {

        CompoundType type =
                COMPOUND("Data",
                         MEMBER("Counter", INT),
                         MEMBER("Complex_List",
                                VAR_SEQUENCE(COMPOUND("Complex",
                                                      MEMBER("x", DOUBLE),
                                                      MEMBER("y", DOUBLE)), "Counter")));

        ByteArrayIOHandler byteArrayIOHandler = new ByteArrayIOHandler();
        TracingIOHandler tracingIOHandler = new TracingIOHandler(byteArrayIOHandler);
        DataContext context = new DataFormat(type).createContext(tracingIOHandler);
        CompoundData data = context.getData();
        data.setInt("Counter", -1);
        SequenceData seq = data.getSequence("Complex_List");
        assertEquals(0, seq.getElementCount());
        assertEquals(0, seq.getSize());
        data.flush();
        assertEquals("R(0,4)W(0,4)", tracingIOHandler.getTrace());
// TODO - want to test also the following (nf 27.08.2009)
//        for (int i = 0; i < 5; i++) {
// TODO - Next statement throws a com.bc.ceres.binio.DataAccessException,
// TODO - instead seq.elementCount shall increase by one during the call to seq.getCompound(i) (nf 27.08.2009)                  
//            CompoundData complex = seq.getCompound(i);
//            complex.setDouble("x", i + 23.04);
//            complex.setDouble("y", i + 10.12);
//            complex.flush();
//        }
//        data.flush();
//        assertEquals("R(0,16)W(0,16)R(16,16)W(16,16)R(32,16)W(32,16)R(48,16)W(48,16)R(64,16)W(64,16)", tracingIOHandler.getTrace());
    }
}
