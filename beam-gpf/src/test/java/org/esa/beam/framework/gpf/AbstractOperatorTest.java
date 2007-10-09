package org.esa.beam.framework.gpf;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;


public class AbstractOperatorTest extends TestCase {

    public void testInitBehaviour() throws OperatorException {
        final DummyAbstractOperator op = new DummyAbstractOperator();
        assertFalse(op.isInitialized());
        final Product product = op.getTargetProduct();
        assertNotNull(product);
        assertTrue(op.initCalled);
        assertTrue(op.isInitialized());
    }

    private static class DummyAbstractOperator extends Operator {
        private boolean initCalled;

        @Override
        public Product initialize() throws OperatorException {
            initCalled = true;
            return new Product("x", "x", 1, 1);
        }

        @Override
        public void computeTile(Band band, Tile tile) throws OperatorException {
        }
    }

}
