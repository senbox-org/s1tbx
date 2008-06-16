package org.esa.beam.framework.gpf;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.Parameter;

import java.io.File;

/**
 * Tests the default value initialisation of GPF operators.
 *
 * @author Marco
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public class OpParameterDefaultValueInitialisationTest extends TestCase {

    public void testParameterDefaultValueInitialisation() {
        final ParameterDefaultValueOp op = new ParameterDefaultValueOp();
        testParameterValues(op, 12345, 0.123450);
        assertEquals(false, op.initialized);
        op.getTargetProduct(); // force initialisation through framework
        assertEquals(true, op.initialized);
        testParameterValues(op, 12345 + 1, 0.123450);
    }

    public void testDerivedParameterDefaultValueInitialisation() {
        final DerivedParameterDefaultValueOp op = new DerivedParameterDefaultValueOp();
        testParameterValues(op, 12345, 0.123450);
        assertEquals(new File("/usr/marco"), op.pf);
        assertEquals(false, op.initialized);
        op.getTargetProduct(); // force initialisation through framework
        assertEquals(true, op.initialized);
        testParameterValues(op, 12345 + 1, 0.123450);
        assertEquals(new File("/usr/marco"), op.pf);
    }

    private void testParameterValues(ParameterDefaultValueOp op, int pi, double pd) {
        assertEquals((byte) 123, op.pb);
        assertEquals('a', op.pc);
        assertEquals((short) 321, op.ph);
        assertEquals(pi, op.pi);
        assertEquals(1234512345L, op.pl);
        assertEquals(123.45F, op.pf, 1e-5);
        assertEquals(pd, op.pd, 1e-10);
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

    public static class ParameterDefaultValueOp extends Operator {

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
            pi++;
            setTargetProduct(new Product("A", "AT", 10, 10));
        }
    }

    public static class DerivedParameterDefaultValueOp extends ParameterDefaultValueOp {
        @Parameter(defaultValue = "/usr/marco")
        File pf;
    }

}
