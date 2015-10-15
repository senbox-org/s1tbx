package org.esa.snap.core.gpf.pointop;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Kernel;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.junit.Test;

import static org.junit.Assert.*;

public class PixelOperatorWithComputedSourcesTest {

    @Test
    public void testOp() throws Exception {
        Product sp = new Product("N", "T", 2, 2);
        sp.addBand("input", "0.1", ProductData.TYPE_FLOAT64);
        Operator op = new OpWithComputedSources();
        op.setSourceProduct(sp);
        Product tp = op.getTargetProduct();

        double[] output = new double[4];
        tp.getBand("output").readPixels(0, 0, 2, 2, output);

        assertEquals(22.0, output[0], 1E-5); // = source 2 because of Y < 1
        assertEquals(999, output[1], 1E-5);   // = invalid because of X >= 1
        assertEquals(0.2, output[2], 1E-5);  // = source 1 because of Y >= 1
        assertEquals(999, output[3], 1E-5);   // = invalid because of X >= 1
    }

    static class OpWithComputedSources extends SampleOperator {
        @Override
        protected void computeSample(int x, int y, Sample[] sourceSamples, WritableSample targetSample) {
            if (sourceSamples[0].getBoolean()) {
                targetSample.set(sourceSamples[2].getDouble());
            } else {
                targetSample.set(sourceSamples[1].getDouble());
            }
        }

        @Override
        protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
            sampleConfigurer.setValidPixelMask("X < 1");
            sampleConfigurer.defineComputedSample(0, ProductData.TYPE_UINT8, "Y < 1");
            sampleConfigurer.defineComputedSample(1, ProductData.TYPE_FLOAT64, "2 * input");
            sampleConfigurer.defineComputedSample(2, 1, new Kernel(2, 2, new double[]{10, 0, 100, 0}));
        }

        @Override
        protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
            sampleConfigurer.defineSample(0, "output");
        }

        @Override
        protected void configureTargetProduct(ProductConfigurer productConfigurer) {
            Band output = productConfigurer.addBand("output", ProductData.TYPE_FLOAT64);
            output.setNoDataValue(999);
            output.setNoDataValueUsed(true);
        }
    }

    @Test
    public void testOpWith2Products() throws Exception {
        Product sp1 = new Product("N", "T", 2, 2);
        sp1.addBand("input", "0.1", ProductData.TYPE_FLOAT64);
        Product sp2 = new Product("O", "T", 2, 2);
        sp2.addBand("input", "0.2", ProductData.TYPE_FLOAT64);

        Operator op = new OpWithComputedSourcesFrom2Products();
        op.setSourceProduct(sp1);
        op.setSourceProduct("source2", sp2);

        Product tp = op.getTargetProduct();

        double[] output = new double[4];
        tp.getBand("output").readPixels(0, 0, 2, 2, output);

        assertEquals(1.0, output[0], 1E-5);
        assertEquals(1.0, output[1], 1E-5);
        assertEquals(0.0, output[2], 1E-5);
        assertEquals(0.0, output[3], 1E-5);
    }

    static class OpWithComputedSourcesFrom2Products extends SampleOperator {
        @Override
        protected void computeSample(int x, int y, Sample[] sourceSamples, WritableSample targetSample) {
            targetSample.set(sourceSamples[0].getDouble());
        }

        @Override
        protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
            getSourceProduct().setRefNo(1);
            getSourceProduct("source2").setRefNo(2);
            sampleConfigurer.defineComputedSample(0, ProductData.TYPE_UINT8, "$1.Y < 1 AND $2.X > 0", getSourceProducts());
        }

        @Override
        protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
            sampleConfigurer.defineSample(0, "output");
        }

        @Override
        protected void configureTargetProduct(ProductConfigurer productConfigurer) {
            productConfigurer.addBand("output", ProductData.TYPE_UINT8);
        }
    }

    @Test
    public void testOpWith2ProductsWithoutRefNo() throws Exception {
        Product sp1 = new Product("N", "T", 2, 2);
        sp1.addBand("input", "0.1", ProductData.TYPE_FLOAT64);
        Product sp2 = new Product("O", "T", 2, 2);
        sp2.addBand("input", "0.2", ProductData.TYPE_FLOAT64);

        Operator op = new OpWithoutRefNo();
        op.setSourceProduct(sp1);
        op.setSourceProduct("source2", sp2);
        try {
            op.getTargetProduct();
            fail();
        } catch (IllegalArgumentException iae) {
            assertNotNull(iae);
            assertEquals("Product 'N' has no assigned reference number.", iae.getMessage());
        }
    }

    static class OpWithoutRefNo extends SampleOperator {
        @Override
        protected void computeSample(int x, int y, Sample[] sourceSamples, WritableSample targetSample) {
            targetSample.set(sourceSamples[0].getDouble());
        }

        @Override
        protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) throws OperatorException {
            sampleConfigurer.defineComputedSample(0, ProductData.TYPE_UINT8, "$1.Y < 1 AND $2.X > 0", getSourceProducts());
        }

        @Override
        protected void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) throws OperatorException {
            sampleConfigurer.defineSample(0, "output");
        }

        @Override
        protected void configureTargetProduct(ProductConfigurer productConfigurer) {
            productConfigurer.addBand("output", ProductData.TYPE_UINT8);
        }
    }
}
