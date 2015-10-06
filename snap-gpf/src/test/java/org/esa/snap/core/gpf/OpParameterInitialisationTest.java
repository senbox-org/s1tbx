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

package org.esa.snap.core.gpf;

import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

/**
 * Tests the default value initialisation of GPF operators.
 *
 * @author Marco
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class OpParameterInitialisationTest {

    @Test
    public void testUnknownParameters() {
        try {
            final SomeOp op = new SomeOp();
            op.setParameterDefaultValues();
            op.setParameter("pi", 21);
            op.getTargetProduct(); // force initialisation through framework
            assertEquals(21, op.getParameter("pi"));
            assertEquals(21, op.pi);
        } catch (OperatorException e) {
            e.printStackTrace();
            fail("Unexpected: " + e.getMessage());
        }

        try {
            final SomeOp op = new SomeOp();
            op.setParameterDefaultValues();
            op.setParameter("iamAnUnknownParameter", -1);
            op.getTargetProduct(); // force initialisation through framework
        } catch (OperatorException e) {
            e.printStackTrace();
            fail("Unexpected: " + e.getMessage());
        }
    }

    @Test
    public void testParameterDefaultValueInitialisation() {
        final SomeOp op = new SomeOp();
        op.setParameterDefaultValues();
        testParameterValues(op, false);
        op.getTargetProduct(); // force initialisation through framework
        testParameterValues(op, true);
    }

    @Test
    public void testDerivedParameterDefaultValueInitialisation() {
        final SomeDerivedOp op = new SomeDerivedOp();
        op.setParameterDefaultValues();
        testParameterValues(op, false);
        assertEquals(new File("/usr/marco"), op.pFile);
        op.getTargetProduct(); // force initialisation through framework
        testParameterValues(op, true);
        assertEquals(new File("/usr/marco"), op.pFile);
    }

    private void testParameterValues(SomeOp op, boolean expectedInitialiseState) {
        assertEquals(expectedInitialiseState, op.initialized);
        assertEquals((byte) 123, op.pb);
        assertEquals('a', op.pc);
        assertEquals((short) 321, op.ph);
        assertEquals(12345, op.pi);
        assertEquals(1234512345L, op.pl);
        assertEquals(123.45F, op.pf, 1e-5);
        assertEquals(0.12345, op.pd, 1e-10);
        assertEquals("x", op.ps);

        assertNotNull(op.pab);
        assertEquals(3, op.pab.length);
        assertEquals((byte) 123, op.pab[0]);
        assertNotNull(op.pac);
        assertEquals(3, op.pac.length);
        assertEquals('a', op.pac[0]);
        assertEquals('b', op.pac[1]);
        assertEquals('c', op.pac[2]);
        assertNotNull(op.pah);
        assertEquals(3, op.pah.length);
        assertEquals((short) 321, op.pah[0]);
        assertNotNull(op.pai);
        assertEquals(3, op.pai.length);
        assertEquals(12345, op.pai[0]);
        assertNotNull(op.pal);
        assertEquals(3, op.pal.length);
        assertEquals(1234512345L, op.pal[0]);
        assertNotNull(op.paf);
        assertEquals(3, op.paf.length);
        assertEquals(123.45F, op.paf[0], 1e-5);
        assertNotNull(op.pad);
        assertEquals(3, op.pad.length);
        assertEquals(0.123450, op.pad[0], 1e-10);
        assertNotNull(op.pas);
        assertEquals(3, op.pas.length);
        assertEquals("x", op.pas[0]);
        assertEquals("y", op.pas[1]);
        assertEquals("z", op.pas[2]);
    }

    public static class SomeOp extends Operator {

        @Parameter(defaultValue = "123")
        byte pb;
        @Parameter(defaultValue = "a")
        char pc;
        @Parameter(defaultValue = "321")
        short ph;
        @Parameter(defaultValue = "12345")
        int pi;
        @Parameter(defaultValue = "1234512345")
        long pl;
        @Parameter(defaultValue = "123.45")
        float pf;
        @Parameter(defaultValue = "0.12345")
        double pd;
        @Parameter(defaultValue = "x")
        String ps;

        @Parameter(defaultValue = "123,122,121")
        byte[] pab;
        @Parameter(defaultValue = "a,b,c")
        char[] pac;
        @Parameter(defaultValue = "321,331,341")
        short[] pah;
        @Parameter(defaultValue = "12345,32345,42345")
        int[] pai;
        @Parameter(defaultValue = "1234512345,2234512345,3234512345")
        long[] pal;
        @Parameter(defaultValue = "123.45,133.45,143.45")
        float[] paf;
        @Parameter(defaultValue = "0.12345,-0.12345,1.12345")
        double[] pad;
        @Parameter(defaultValue = "x,y,z")
        String[] pas;

        boolean initialized = false;

        @Override
        public void initialize() throws OperatorException {
            initialized = true;
            setTargetProduct(new Product("A", "AT", 10, 10));
        }
    }

    public static class SomeDerivedOp extends SomeOp {
        @Parameter(defaultValue = "/usr/marco")
        File pFile;
    }

}
