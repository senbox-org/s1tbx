package org.esa.beam.framework.gpf;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;

public class OperatorSpiTest extends TestCase {

    public void testNonAnnotatoedOpConstructionWithoutName() {
        OperatorSpi operatorSpi = new OperatorSpi(NonAnnotatoedFooOp.class) {
        };
        assertSame(NonAnnotatoedFooOp.class, operatorSpi.getOperatorClass());
        assertEquals("NonAnnotatoedFooOp", operatorSpi.getOperatorAlias());
    }

    public void testNonAnnotatoedOpConstructionWithName() {
        OperatorSpi operatorSpi = new OperatorSpi(NonAnnotatoedFooOp.class, "foo") {
        };
        assertSame(NonAnnotatoedFooOp.class, operatorSpi.getOperatorClass());
        assertEquals("foo", operatorSpi.getOperatorAlias());
    }

    public void testAnnotatoedOpConstructionWithoutName() {
        OperatorSpi operatorSpi = new OperatorSpi(AnnotatedFooOp.class) {
        };
        assertSame(AnnotatedFooOp.class, operatorSpi.getOperatorClass());
        assertEquals("FooFighters", operatorSpi.getOperatorAlias());
        assertNotNull(operatorSpi.getOperatorMetadata());
        assertNotNull(operatorSpi.getParameterDescriptors());
        assertEquals(1, operatorSpi.getParameterDescriptors().size());
        assertNotNull(operatorSpi.getParameterDescriptors().get("threshold"));
        assertNotNull(operatorSpi.getSourceProductDescriptors());
        assertEquals(1, operatorSpi.getSourceProductDescriptors().size());
        assertNotNull(operatorSpi.getSourceProductDescriptors().get("input"));
    }

    public static class NonAnnotatoedFooOp extends Operator {

        @Override
        public Product initialize() throws OperatorException {
            return null;
        }
    }

    @OperatorMetadata(alias = "FooFighters")
    public static class AnnotatedFooOp extends Operator {
        @SourceProduct
        Product input;

        Product auxdata;

        @Parameter
        double threshold;

        int maxCount;

        @Override
        public Product initialize() throws OperatorException {
            return null;
        }
    }
}
