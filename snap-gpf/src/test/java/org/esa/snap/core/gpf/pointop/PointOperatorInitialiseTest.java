package org.esa.snap.core.gpf.pointop;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.OperatorException;
import org.junit.Test;

import javax.media.jai.operator.ConstantDescriptor;

import static org.junit.Assert.*;

public class PointOperatorInitialiseTest {

    @Test
    public void testInitialiseSequenceOfSampleOperator() throws Exception {
        TracingSampleOperator operator = new TracingSampleOperator();
        operator.setSourceProduct(new Product("N", "T", 200, 100));
        operator.getTargetProduct();
        assertEquals("12345", operator.trace);
    }

    @Test
    public void testInitialiseSequenceOfPixelOperator() throws Exception {
        TracingPixelOperator operator = new TracingPixelOperator();
        operator.setSourceProduct(new Product("N", "T", 200, 100));
        operator.getTargetProduct();
        assertEquals("12345", operator.trace);
    }

    @Test
    public void testDefaultValidateInputShallNotFail() throws Exception {
        EmptyPointOperator operator = new EmptyPointOperator();
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
        EmptyPointOperator operator = new EmptyPointOperator();
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

    @Test
    public void testThatOnlyExistingBandsCanProvideSourceSamples() throws Exception {
        BadSourceSamplePointOperator operator = new BadSourceSamplePointOperator();
        operator.setSourceProducts(new Product("N1", "T", 200, 100));
        try {
            operator.getTargetProduct();
            fail("OperatorException expected, because only existing bands can provide source samples.");
        } catch (OperatorException e) {
            assertEquals("Product 'N1' does not contain a raster with name 'missing_band'", e.getMessage());
        }
    }

    @Test
    public void testThatOnlySourcelessBandsCanProvideTargetSamples() throws Exception {
        BadTargetSamplePointOperator operator = new BadTargetSamplePointOperator();
        operator.setSourceProducts(new Product("N1", "T", 200, 100));
        try {
            operator.getTargetProduct();
            fail("OperatorException expected, because only sourceless bands can provide target samples.");
        } catch (OperatorException e) {
            assertEquals("Raster 'const_7' must be sourceless, since it is a computed target", e.getMessage());
        }
    }

    private static class TracingPixelOperator extends PixelOperator {
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
        protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
            trace += "4";
        }

        @Override
        protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
            trace += "5";
        }

        @Override
        protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
            // not needed
        }
    }

    private static class TracingSampleOperator extends SampleOperator {
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
        protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
            trace += "4";
        }

        @Override
        protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
            trace += "5";
        }

        @Override
        protected void computeSample(int x, int y, Sample[] sourceSamples, WritableSample targetSample) {
            // not needed
        }
    }

    private static class EmptyPointOperator extends PointOperator {
        @Override
        protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
        }

        @Override
        protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
        }
    }

    private static class BadSourceSamplePointOperator extends PointOperator {

        @Override
        protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
            // OperatorException expected, since 'missing_band' is not a source band
            sampleConfigurer.defineSample(0, "missing_band");
        }

        @Override
        protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
        }
    }

    private static class BadTargetSamplePointOperator extends PointOperator {

        @Override
        protected void configureTargetProduct(ProductConfigurer productConfigurer) {
            super.configureTargetProduct(productConfigurer);

            final Band band = productConfigurer.addBand("const_7", ProductData.TYPE_FLOAT32);
            band.setSourceImage(ConstantDescriptor.create((float) productConfigurer.getTargetProduct().getSceneRasterWidth(),
                                                          (float) productConfigurer.getTargetProduct().getSceneRasterHeight(),
                                                          new Float[]{7.0F}, null));

        }

        @Override
        protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
        }

        @Override
        protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
            // OperatorException expected, since 'const_7' is not sourceless
            sampleConfigurer.defineSample(0, "const_7");
        }
    }
}
