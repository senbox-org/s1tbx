package org.esa.beam.framework.gpf;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.TargetProperty;


public class OpWithTargetPropertyTest extends TestCase {

    public void testTargetPropertyIsComputedDuringOpInitialisation() {
        final Operator op = new PropertyOp();
        final Object targetProperty = op.getTargetProperty("theAnswer");

        assertNotNull(targetProperty);
        assertEquals(42, targetProperty);
    }

    public void testWrongPropertyOp() {
        final Operator op = new IllegalPropertyOp();
        try {
            op.getTargetProperty("theAnswer");
            fail("duplicated property name error expected");
        } catch (OperatorException e) {
        }
    }

    public static class PropertyOp extends Operator {

        @TargetProperty
        int theAnswer;

        @Override
        public void initialize() throws OperatorException {
            theAnswer = 42;
            setTargetProduct(new Product("A", "AT", 10, 10));
        }
    }

    public static class IllegalPropertyOp extends Operator {

        @TargetProperty
        int theAnswer;

        @TargetProperty(alias = "theAnswer")
        double anotherAnswer;

        @Override
        public void initialize() throws OperatorException {
            theAnswer = 42;
            setTargetProduct(new Product("A", "AT", 10, 10));
        }
    }

}