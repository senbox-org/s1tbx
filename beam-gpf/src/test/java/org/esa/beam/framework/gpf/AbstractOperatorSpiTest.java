package org.esa.beam.framework.gpf;

import junit.framework.TestCase;

public class AbstractOperatorSpiTest extends TestCase {

    public void testConstructionWithoutName() {
        AbstractOperatorSpi abstractOperatorSpi = new AbstractOperatorSpi(Operator.class) {
        };

        assertEquals("org.esa.beam.framework.gpf.Operator", abstractOperatorSpi.getName());
        assertSame(Operator.class, abstractOperatorSpi.getOperatorClass());
    }

    public void testConstructionWithName() {
        AbstractOperatorSpi abstractOperatorSpi = new AbstractOperatorSpi(Operator.class, "foo") {
        };

        assertEquals("foo", abstractOperatorSpi.getName());
        assertSame(Operator.class, abstractOperatorSpi.getOperatorClass());
    }
}
