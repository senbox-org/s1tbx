package org.esa.snap.core.gpf.pointop;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.TiePointGeoCoding;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.util.ProductUtils;
import org.junit.Test;

import javax.media.jai.JAI;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.text.ParseException;
import java.util.Map;

import static org.junit.Assert.*;

public class PointOperatorTest {

    private static final int M = 1024 * 1024;


    private static final int W = 1121;
    private static final int H = 1121 * 2;
    // private static final int H = 14000; Avg #scans/orbit in Envisat MERIS

    private static final long[] CACHE_SIZES = {
            0L,
            16L * M,
            32L * M,
            64L * M,
            128L * M,
            256L * M,
            512L * M,
            (Runtime.getRuntime().maxMemory() / M) * M
    };

    public static void main(String[] args) throws ParseException {
        new PointOperatorTest().runTestsWithDifferentTileCacheCapacities();
    }

    private void runTestsWithDifferentTileCacheCapacities() throws ParseException {
        for (long cacheSize : CACHE_SIZES) {
            testPointOp(new NdviTileOp(), cacheSize);
        }
        for (long cacheSize : CACHE_SIZES) {
            testPointOp(new NdviSampleOp(), cacheSize);
        }
        for (long cacheSize : CACHE_SIZES) {
            testPointOp(new NdviTileStackOp(), cacheSize);
        }
        for (long cacheSize : CACHE_SIZES) {
            testPointOp(new NdviPixelOp(), cacheSize);
        }
    }

    @Test
    public void testNdviSampleOp() throws ParseException {
        testPointOp(new NdviSampleOp(), 128L * M);
    }

    @Test
    public void testNdviPixelOp() throws ParseException {
        testPointOp(new NdviPixelOp(), 128L * M);
    }

    @Test
    public void testNdviPixelOpWithGaps() throws ParseException {
        testPointOp(new NdviPixelOpWithGaps(), 128L * M);
    }

    private void testPointOp(Operator op, long cacheSize) throws ParseException {

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
        assertEquals(sourceProduct.getStartTime(), targetProduct.getStartTime());
        assertEquals(sourceProduct.getEndTime(), targetProduct.getEndTime());
        assertNotNull(targetProduct.getTiePointGrid("latitude"));
        assertNotNull(targetProduct.getTiePointGrid("longitude"));
        assertTrue(targetProduct.getSceneGeoCoding() instanceof TiePointGeoCoding);
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

        assertEquals((30.0f - 20.0f) / (30.0f + 20.0f), ndviData.getSampleFloat(0, 0, 0), 1e-5F);
        assertEquals((30.0f - 20.0f) / (30.0f + 20.0f), ndviData.getSampleFloat(0, 1, 0), 1e-5F);
        assertEquals((30.0f - 20.0f) / (30.0f + 20.0f), ndviData.getSampleFloat(1, 0, 0), 1e-5F);
        assertEquals((30.0f - 20.0f) / (30.0f + 20.0f), ndviData.getSampleFloat(1, 1, 0), 1e-5F);
        assertEquals(0, ndviFlagsData.getSample(0, 0, 0));
        assertEquals(0, ndviFlagsData.getSample(0, 1, 0));
        assertEquals(0, ndviFlagsData.getSample(1, 0, 0));
        assertEquals(0, ndviFlagsData.getSample(1, 1, 0));
    }

    private Product createSourceProduct(int w, int h) throws ParseException {
        Product sourceProduct = new Product("TEST.N1", "MER_RR__1P", w, h);
        sourceProduct.setStartTime(ProductData.UTC.parse("17-Jan-2008 12:13:27"));
        sourceProduct.setEndTime(ProductData.UTC.parse("17-Jan-2008 13:16:28"));
        Band rad08 = sourceProduct.addBand("radiance_8", ProductData.TYPE_INT16);
        Band rad10 = sourceProduct.addBand("radiance_10", ProductData.TYPE_INT16);
        rad08.setSourceImage(ConstantDescriptor.create(1.0f * w, 1.0f * h, new Short[]{2000}, null));
        rad10.setSourceImage(ConstantDescriptor.create(1.0f * w, 1.0f * h, new Short[]{3000}, null));
        rad08.setScalingFactor(0.01);
        rad10.setScalingFactor(0.01);
        TiePointGrid lat = new TiePointGrid("latitude", w, h, 0, 0, 1, 1, new float[w * h]);
        TiePointGrid lon = new TiePointGrid("longitude", w, h, 0, 0, 1, 1, new float[w * h]);
        sourceProduct.addTiePointGrid(lat);
        sourceProduct.addTiePointGrid(lon);
        sourceProduct.setSceneGeoCoding(new TiePointGeoCoding(lat, lon));
        return sourceProduct;
    }

    public static class NdviSampleOp extends SampleOperator {
        @Override
        protected void configureTargetProduct(ProductConfigurer productConfigurer) {
            super.configureTargetProduct(productConfigurer);
            productConfigurer.addBand("ndvi", ProductData.TYPE_FLOAT32);
            productConfigurer.addBand("ndvi_flags", ProductData.TYPE_INT16);
        }

        @Override
        protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) {
            sampleConfigurer.defineSample(0, "radiance_10");
            sampleConfigurer.defineSample(1, "radiance_8");
        }

        @Override
        public void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) {
            sampleConfigurer.defineSample(0, "ndvi");
            sampleConfigurer.defineSample(1, "ndvi_flags");
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
        protected void configureTargetProduct(ProductConfigurer productConfigurer) {
            super.configureTargetProduct(productConfigurer);
            productConfigurer.addBand("ndvi", ProductData.TYPE_FLOAT32);
            productConfigurer.addBand("ndvi_flags", ProductData.TYPE_INT16);
        }

        @Override
        protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) {
            sampleConfigurer.defineSample(0, "radiance_10");
            sampleConfigurer.defineSample(1, "radiance_8");
        }

        @Override
        public void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) {
            sampleConfigurer.defineSample(0, "ndvi");
            sampleConfigurer.defineSample(1, "ndvi_flags");
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

    public static class NdviPixelOpWithGaps extends PixelOperator {
        @Override
        protected void configureTargetProduct(ProductConfigurer productConfigurer) {
            super.configureTargetProduct(productConfigurer);
            productConfigurer.addBand("ndvi", ProductData.TYPE_FLOAT32);
            productConfigurer.addBand("ndvi_flags", ProductData.TYPE_INT16);
        }

        @Override
        protected void configureSourceSamples(SourceSampleConfigurer sampleConfigurer) {
            sampleConfigurer.defineSample(86, "radiance_10");
            sampleConfigurer.defineSample(423, "radiance_8");
        }

        @Override
        public void configureTargetSamples(TargetSampleConfigurer sampleConfigurer) {
            sampleConfigurer.defineSample(34, "ndvi");
            sampleConfigurer.defineSample(687, "ndvi_flags");
        }

        @Override
        protected void computePixel(int x, int y, Sample[] sourceSamples, WritableSample[] targetSamples) {
            double rad1 = sourceSamples[86].getDouble();
            double rad2 = sourceSamples[423].getDouble();
            double ndvi = (rad1 - rad2) / (rad1 + rad2);
            int ndviFlags = (ndvi < 0 ? 1 : 0) | (ndvi > 1 ? 2 : 0);
            targetSamples[34].set(ndvi);
            targetSamples[687].set(ndviFlags);
        }

    }

    public static class NdviStdOp extends Operator {

        @Override
        public void initialize() throws OperatorException {
            final Product inputProduct = getSourceProduct();
            final int sceneWidth = inputProduct.getSceneRasterWidth();
            final int sceneHeight = getSourceProduct().getSceneRasterHeight();
            Product targetProduct = new Product("ndvi", "NDVI_TYPE", sceneWidth, sceneHeight);

            targetProduct.setStartTime(inputProduct.getStartTime());
            targetProduct.setEndTime(inputProduct.getEndTime());
            ProductUtils.copyTiePointGrids(inputProduct, targetProduct);
            ProductUtils.copyGeoCoding(inputProduct, targetProduct);

            // create and add the NDVI band
            Band ndviOutputBand = new Band("ndvi", ProductData.TYPE_FLOAT32, sceneWidth,
                                           sceneHeight);
            targetProduct.addBand(ndviOutputBand);

            // create and add the NDVI flags band
            Band ndviFlagsOutputBand = new Band("ndvi_flags", ProductData.TYPE_INT16,
                                                sceneWidth, sceneHeight);
            ndviFlagsOutputBand.setDescription("NDVI specific flags");
            targetProduct.addBand(ndviFlagsOutputBand);
            setTargetProduct(targetProduct);
        }
    }

    public static class NdviTileStackOp extends NdviStdOp {

        @Override
        public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle,
                                     ProgressMonitor pm) throws
                OperatorException {

            final Product product = getSourceProduct();
            Tile sourceTile1 = getSourceTile(product.getBand("radiance_10"), targetRectangle);
            Tile sourceTile2 = getSourceTile(product.getBand("radiance_8"), targetRectangle);
            final Tile ndviTile = targetTiles.get(getTargetProduct().getBand("ndvi"));
            final Tile ndviFlagsTile = targetTiles.get(getTargetProduct().getBand("ndvi_flags"));

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    final float upper = sourceTile1.getSampleFloat(x, y);
                    final float lower = sourceTile2.getSampleFloat(x, y);
                    float ndviValue = (upper - lower) / (upper + lower);
                    int ndviFlags = (ndviValue < 0 ? 1 : 0) | (ndviValue > 1 ? 2 : 0);
                    ndviTile.setSample(x, y, ndviValue);
                    ndviFlagsTile.setSample(x, y, ndviFlags);
                }
            }

        }
    }

    public static class NdviTileOp extends NdviStdOp {

        @Override
        public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
            final Product product = getSourceProduct();
            final Rectangle targetRectangle = targetTile.getRectangle();
            Tile sourceTile1 = getSourceTile(product.getBand("radiance_10"), targetRectangle);
            Tile sourceTile2 = getSourceTile(product.getBand("radiance_8"), targetRectangle);

            for (int y = targetRectangle.y; y < targetRectangle.y + targetRectangle.height; y++) {
                for (int x = targetRectangle.x; x < targetRectangle.x + targetRectangle.width; x++) {
                    final float upper = sourceTile1.getSampleFloat(x, y);
                    final float lower = sourceTile2.getSampleFloat(x, y);
                    float ndviValue = (upper - lower) / (upper + lower);
                    if ("ndvi".equals(targetBand.getName())) {
                        targetTile.setSample(x, y, ndviValue);
                    } else if ("ndvi_flags".equals(targetBand.getName())) {
                        int ndviFlags = (ndviValue < 0 ? 1 : 0) | (ndviValue > 1 ? 2 : 0);
                        targetTile.setSample(x, y, ndviFlags);
                    }
                }
            }

        }

    }
}
