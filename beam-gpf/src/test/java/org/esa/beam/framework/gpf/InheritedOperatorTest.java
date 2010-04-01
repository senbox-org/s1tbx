package org.esa.beam.framework.gpf;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.io.IOException;


public class InheritedOperatorTest extends TestCase {


    public void testBasicOperatorStates() throws OperatorException, IOException {
        Product sourceProduct = new Product("test", "test", 10, 10);

        DerivedOp op = new DerivedOp();
        op.setSourceProduct(sourceProduct);
        op.setParameter("canExplode", true);

        Product targetProduct = op.getTargetProduct();
        assertSame(targetProduct, sourceProduct);

        assertEquals(true, op.canExplode);
    }

    private static class BaseOp extends Operator {
        @SourceProduct
        Product sourceProduct;
        @TargetProduct
        Product targetProduct;
        @Parameter
        boolean canExplode;

        @Override
        public void initialize() throws OperatorException {
            targetProduct = sourceProduct;
        }
    }

    private static class DerivedOp extends BaseOp {
        @Override
        public void initialize() throws OperatorException {
            super.initialize();
        }
    }
}