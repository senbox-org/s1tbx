package org.esa.beam.framework.gpf;

import junit.framework.TestCase;

public class AbstractOperatorSpiTest extends TestCase {

    public void testConstructionWithoutName() {
        AbstractOperatorSpi abstractOperatorSpi = new AbstractOperatorSpi(AbstractOperator.class) {
        };

        assertEquals("org.esa.beam.framework.gpf.AbstractOperator", abstractOperatorSpi.getName());
        assertSame(AbstractOperator.class, abstractOperatorSpi.getOperatorClass());
    }

    public void testConstructionWithName() {
        AbstractOperatorSpi abstractOperatorSpi = new AbstractOperatorSpi(AbstractOperator.class, "foo") {
        };

        assertEquals("foo", abstractOperatorSpi.getName());
        assertSame(AbstractOperator.class, abstractOperatorSpi.getOperatorClass());
    }
}
