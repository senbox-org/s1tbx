package org.esa.snap.framework.gpf.pointop;

import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.datamodel.Kernel;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.framework.datamodel.ProductData;
import org.esa.snap.framework.gpf.OperatorException;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class PixelOperatorWithComputedSourcesTest {

    @Test
    public void testOp() throws Exception {
        Product sp = new Product("N", "T", 2, 2);
        sp.addBand("input", "0.1", ProductData.TYPE_FLOAT64);
        SampleOperatorWithComputedSources op = new SampleOperatorWithComputedSources();
        op.setSourceProduct(sp);
        Product tp = op.getTargetProduct();

        double[] output = new double[4];
        tp.getBand("output").readPixels(0, 0, 2, 2, output);

        assertEquals(22.0, output[0], 1E-5); // = source 2 because of Y < 1
        assertEquals(999, output[1], 1E5);   // = invalid because of X >= 1
        assertEquals(0.2, output[2], 1E-5);  // = source 1 because of Y >= 1
        assertEquals(999, output[3], 1E5);   // = invalid because of X >= 1
    }

    static class SampleOperatorWithComputedSources extends SampleOperator {
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
}
