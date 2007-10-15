package org.esa.beam.framework.gpf;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;

public class OperatorSpiTest extends TestCase {

    public void testNonAnnotatoedOpConstructionWithoutName() {
        OperatorSpi operatorSpi = new OperatorSpi(NonAnnotatoedFooOp.class) {
        };
        assertSame(NonAnnotatoedFooOp.class, operatorSpi.getOperatorClass());
        assertEquals("NonAnnotatoedFooOp", operatorSpi.getAliasName());
    }

    public void testNonAnnotatoedOpConstructionWithName() {
        OperatorSpi operatorSpi = new OperatorSpi(NonAnnotatoedFooOp.class, "foo") {
        };
        assertSame(NonAnnotatoedFooOp.class, operatorSpi.getOperatorClass());
        assertEquals("foo", operatorSpi.getAliasName());
    }

    public void testNonAnnotatoedOpConstructionWithNameAndMetadata() {
        OperatorSpi operatorSpi = new OperatorSpi(NonAnnotatoedFooOp.class,
                                                  "foo",
                                                  "1.4",
                                                  "Not available.",
                                                  "Marco, Ralf and Norman",
                                                  "(c) Brockmann") {
        };
        assertSame(NonAnnotatoedFooOp.class, operatorSpi.getOperatorClass());
        assertEquals("foo", operatorSpi.getAliasName());
        assertEquals("1.4", operatorSpi.getVersion());
        assertEquals("Not available.", operatorSpi.getDescription());
        assertEquals("Marco, Ralf and Norman", operatorSpi.getAuthor());
        assertEquals("(c) Brockmann", operatorSpi.getCopyright());
    }

    public void testAnnotatoedOpConstructionWithoutName() {
        OperatorSpi operatorSpi = new OperatorSpi(AnnotatedFooOp.class) {
        };
        assertSame(AnnotatedFooOp.class, operatorSpi.getOperatorClass());
        assertEquals("FooFighters", operatorSpi.getAliasName());
    }

    public static class NonAnnotatoedFooOp extends Operator {

        @Override
        public Product initialize() throws OperatorException {
            return null;
        }
    }

    @OperatorMetadata(alias = "FooFighters")
    public static class AnnotatedFooOp extends Operator {

        @Override
        public Product initialize() throws OperatorException {
            return null;
        }
    }
}
