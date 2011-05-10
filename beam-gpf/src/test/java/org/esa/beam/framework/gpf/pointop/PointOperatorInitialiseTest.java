package org.esa.beam.framework.gpf.pointop;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.OperatorException;
import org.junit.Test;

import static org.junit.Assert.*;

public class PointOperatorInitialiseTest {

    @Test
    public void testInitialiseSequenceOfSampleOperator() throws Exception {
        MySampleOperator operator = new MySampleOperator();
        operator.setSourceProduct(new Product("N", "T", 200, 100));
        operator.getTargetProduct();
        assertEquals("12345", operator.trace);
    }

    @Test
    public void testInitialiseSequenceOfPixelOperator() throws Exception {
        MyPixelOperator operator = new MyPixelOperator();
        operator.setSourceProduct(new Product("N", "T", 200, 100));
        operator.getTargetProduct();
        assertEquals("12345", operator.trace);
    }

    @Test
    public void testDefaultValidateInputShallNotFail() throws Exception {
        MyPointOperator operator = new MyPointOperator();
        operator.setSourceProducts(new Product("N1", "T", 200, 100),
                                   new Product("N2", "T", 200, 100),
                                   new Product("N3", "T", 200, 100));
        try {
            operator.getTargetProduct();
        } catch (OperatorException e) {
            fail("OperatorException not expected.");
        }
    }

    @Test
    public void testDefaultValidateInputShallFail() throws Exception {
        MyPointOperator operator = new MyPointOperator();
        operator.setSourceProducts(new Product("N1", "T", 200, 100),
                                   new Product("N2", "T", 200, 100),
                                   new Product("N3", "T", 200, 101));
        try {
            operator.getTargetProduct();
            fail("OperatorException expected.");
        } catch (OperatorException e) {
            // Expected
        }
    }



    private static class MyPixelOperator extends PixelOperator {
        String trace = "";

        @Override
        protected void prepareInputs() throws OperatorException {
            trace += "1";
        }

        @Override
        protected Product createTargetProduct() throws OperatorException {
            trace += "2";
            return super.createTargetProduct();
        }

        @Override
        protected void configureTargetProduct(ProductConfigurer productConfigurer) {
            super.configureTargetProduct(productConfigurer);
            trace += "3";
        }

        @Override
        protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
            trace += "4";
        }

        @Override
        protected void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
            trace += "5";
        }

        @Override
        protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
            // not needed
        }
    }

    private static class MySampleOperator extends SampleOperator {
        String trace = "";

        @Override
        protected void prepareInputs() throws OperatorException {
            trace += "1";
        }

        @Override
        protected Product createTargetProduct() throws OperatorException {
            trace += "2";
            return super.createTargetProduct();
        }

        @Override
        protected void configureTargetProduct(ProductConfigurer productConfigurer) {
            super.configureTargetProduct(productConfigurer);
            trace += "3";
        }

        @Override
        protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
            trace += "4";
        }

        @Override
        protected void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
            trace += "5";
        }

        @Override
        protected void computeSample(int x, int y, Sample[] sourceSamples, WritableSample targetSample) {
            // not needed
        }
    }

    private static class MyPointOperator extends PointOperator {
        @Override
        protected void configureSourceSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        }

        @Override
        protected void configureTargetSamples(SampleConfigurer sampleConfigurer) throws OperatorException {
        }
    }
}
