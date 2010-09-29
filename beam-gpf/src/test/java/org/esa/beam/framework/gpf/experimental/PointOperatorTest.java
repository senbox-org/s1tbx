package org.esa.beam.framework.gpf.experimental;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.TiePointGeoCoding;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.gpf.Operator;

import javax.media.jai.JAI;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.image.Raster;

public class PointOperatorTest extends TestCase {
    private static final int M = 1024 * 1024;


    private static final int W = 1121;
    private static final int H = 1121 * 2; 
    // private static final int H = 14000; Avg #scans/orbit in Envisat MERIS

    private static final long[] CACHE_SIZES = {
            0L,
            16 * M,
            32 * M,
            64 * M,
            128 * M,
            256 * M,
            512 * M,
            (Runtime.getRuntime().maxMemory() / M) * M};

    public static void main(String[] args) {
        new PointOperatorTest().runTestsWithDifferentTileCacheCapacities();
    }

    private void runTestsWithDifferentTileCacheCapacities() {
        for (long cacheSize : CACHE_SIZES) {
            testPointOp(new NdviSampleOp(), cacheSize);
        }
        for (long cacheSize : CACHE_SIZES) {
            testPointOp(new NdviPixelOp(), cacheSize);
        }
    }

    public void testNdviSampleOp() {
        testPointOp(new NdviSampleOp(), 128 * M);
    }

    public void testNdviPixelOp() {
        testPointOp(new NdviPixelOp(), 128 * M);
    }

    private void testPointOp(Operator op, long cacheSize) {

        if (cacheSize <= 0L) {
            JAI.getDefaultInstance().getTileCache().setMemoryCapacity(0L);
            JAI.disableDefaultTileCache();
        } else {
            JAI.getDefaultInstance().getTileCache().setMemoryCapacity(cacheSize);
            JAI.enableDefaultTileCache();
        }

        Product sourceProduct = createSourceProduct(W, H);

        op.setSourceProduct(sourceProduct);
        Product targetProduct = op.getTargetProduct();

        assertNotNull(targetProduct.getTiePointGrid("latitude"));
        assertNotNull(targetProduct.getTiePointGrid("longitude"));
        assertTrue(targetProduct.getGeoCoding() instanceof TiePointGeoCoding);
        assertNotNull(targetProduct.getBand("ndvi"));
        assertNotNull(targetProduct.getBand("ndvi_flags"));

        long t0 = System.nanoTime();
        Raster ndviData = targetProduct.getBand("ndvi").getGeophysicalImage().getData(); //getTile(0, 0);
        Raster ndviFlagsData = targetProduct.getBand("ndvi_flags").getGeophysicalImage().getData(); // getTile(0, 0);
        long t1 = System.nanoTime();
        double time = (t1 - t0) * 1.0E-9;
        double mpixels = W * H / time / (1000 * 1000);
        System.out.println(op.getClass().getSimpleName() + ":");
        System.out.println("  Cache size: " + JAI.getDefaultInstance().getTileCache().getMemoryCapacity() / M + " M");
        System.out.println("  Total time: " + time + " s");
        System.out.println("  Throughput: " + mpixels + " mega-pixels/s");

        assertEquals((30f - 20f) / (30f + 20f), ndviData.getSampleFloat(0, 0, 0));
        assertEquals((30f - 20f) / (30f + 20f), ndviData.getSampleFloat(0, 1, 0));
        assertEquals((30f - 20f) / (30f + 20f), ndviData.getSampleFloat(1, 0, 0));
        assertEquals((30f - 20f) / (30f + 20f), ndviData.getSampleFloat(1, 1, 0));
        assertEquals(0, ndviFlagsData.getSample(0, 0, 0));
        assertEquals(0, ndviFlagsData.getSample(0, 1, 0));
        assertEquals(0, ndviFlagsData.getSample(1, 0, 0));
        assertEquals(0, ndviFlagsData.getSample(1, 1, 0));
    }

    private Product createSourceProduct(int w, int h) {
        Product sourceProduct = new Product("TEST.N1", "MER_RR__1P", w, h);
        Band rad08 = sourceProduct.addBand("radiance_8", ProductData.TYPE_INT16);
        Band rad10 = sourceProduct.addBand("radiance_10", ProductData.TYPE_INT16);
        rad08.setSourceImage(ConstantDescriptor.create(1f * w, 1f * h, new Short[]{2000}, null));
        rad10.setSourceImage(ConstantDescriptor.create(1f * w, 1f * h, new Short[]{3000}, null));
        rad08.setScalingFactor(0.01);
        rad10.setScalingFactor(0.01);
        TiePointGrid lat = new TiePointGrid("latitude", w, h, 0, 0, 1, 1, new float[w * h]);
        TiePointGrid lon = new TiePointGrid("longitude", w, h, 0, 0, 1, 1, new float[w * h]);
        sourceProduct.addTiePointGrid(lat);
        sourceProduct.addTiePointGrid(lon);
        sourceProduct.setGeoCoding(new TiePointGeoCoding(lat, lon));
        return sourceProduct;
    }

    public static class NdviSampleOp extends SampleOperator {

        @Override
        public void configureTargetProduct(Product product) {
            product.addBand("ndvi", ProductData.TYPE_FLOAT32);
            product.addBand("ndvi_flags", ProductData.TYPE_INT16);
        }

        @Override
        protected void configureSourceSamples(Configurator c) {
            c.defineSample(0, "radiance_10");
            c.defineSample(1, "radiance_8");
        }

        @Override
        public void configureTargetSamples(Configurator c) {
            c.defineSample(0, "ndvi");
            c.defineSample(1, "ndvi_flags");
        }

        @Override
        protected void computeSample(int x, int y, Sample[] sourceSamples, WritableSample targetSample) {
            double rad1 = sourceSamples[0].getDouble();
            double rad2 = sourceSamples[1].getDouble();
            double ndvi = (rad1 - rad2) / (rad1 + rad2);
            if (targetSample.getIndex() == 0) {
                targetSample.set(ndvi);
            } else if (targetSample.getIndex() == 1) {
                int ndviFlags = (ndvi < 0 ? 1 : 0) | (ndvi > 1 ? 2 : 0);
                targetSample.set(ndviFlags);
            }
        }
    }

    public static class NdviPixelOp extends PixelOperator {

        @Override
        public void configureTargetProduct(Product product) {
            product.addBand("ndvi", ProductData.TYPE_FLOAT32);
            product.addBand("ndvi_flags", ProductData.TYPE_INT16);
        }

        @Override
        protected void configureSourceSamples(Configurator c) {
            c.defineSample(0, "radiance_10");
            c.defineSample(1, "radiance_8");
        }

        @Override
        public void configureTargetSamples(Configurator c) {
            c.defineSample(0, "ndvi");
            c.defineSample(1, "ndvi_flags");
        }

        @Override
        protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
            double rad1 = sourceSamples[0].getDouble();
            double rad2 = sourceSamples[1].getDouble();
            double ndvi = (rad1 - rad2) / (rad1 + rad2);
            int ndviFlags = (ndvi < 0 ? 1 : 0) | (ndvi > 1 ? 2 : 0);
            targetSamples[0].set(ndvi);
            targetSamples[1].set(ndviFlags);
        }

    }
}
