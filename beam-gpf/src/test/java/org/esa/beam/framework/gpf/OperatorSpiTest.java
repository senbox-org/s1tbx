package org.esa.beam.framework.gpf;

import junit.framework.TestCase;

public class OperatorSpiTest extends TestCase {

    public void testConstructionWithoutName() {
        OperatorSpi operatorSpi = new OperatorSpi(Operator.class);
        assertSame(Operator.class, operatorSpi.getOperatorClass());
        assertEquals("Operator", operatorSpi.getName());
        assertEquals("1.0", operatorSpi.getVersion());
    }

    public void testConstructionWithName() {
        OperatorSpi operatorSpi = new OperatorSpi(Operator.class, "foo");
        assertSame(Operator.class, operatorSpi.getOperatorClass());
        assertEquals("foo", operatorSpi.getName());
        assertEquals("1.0", operatorSpi.getVersion());
    }

    public void testConstruction() {
        OperatorSpi operatorSpi = new OperatorSpi(Operator.class, "foo", "1.4", "Not available.", "Bibo", "(c) Sesamestreet");
        assertSame(Operator.class, operatorSpi.getOperatorClass());
        assertEquals("foo", operatorSpi.getName());
        assertEquals("1.4", operatorSpi.getVersion());
        assertEquals("Not available.", operatorSpi.getDescription());
        assertEquals("Bibo", operatorSpi.getAuthor());
        assertEquals("(c) Sesamestreet", operatorSpi.getCopyright());
    }
}
