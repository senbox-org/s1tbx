package org.esa.beam.framework.gpf;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.Map;

public class GPFTest extends TestCase {

    public void testDefaultSettings() {
        GPF gpf = GPF.getDefaultInstance();
        assertNotNull(gpf);
        assertSame(gpf, GPF.getDefaultInstance());
    }

    public void testOperatorApi() throws IOException, OperatorException, URISyntaxException {
        OperatorSpiRegistry.getInstance().loadOperatorSpis();

        String filePath = GPFTest.class.getResource("test-product.dim").toURI().getPath();

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("filePath", filePath);

        Product p1 = GPF.createProduct("ReadProduct", parameters, ProgressMonitor.NULL);

        assertNotNull(p1);
        assertNotNull(p1.getBand("forrest_abundance"));
        assertNotNull(p1.getBand("ocean_abundance"));
        assertNotNull(p1.getBand("cloud_abundance"));
        assertNotNull(p1.getBand("cropland_abundance"));

        assertNotNull(p1.getFileLocation());
        assertEquals("test-product.dim", p1.getFileLocation().getName());

        OperatorSpiRegistry.getInstance().addOperatorSpi(new FooOpSpi());

        Product p2 = GPF.createProduct("Foo", GPF.NO_PARAMS, p1, ProgressMonitor.NULL);

        assertNotNull(p2);
        assertNotNull(p2.getBand("forrest_abundance"));
        assertNotNull(p2.getBand("ocean_abundance"));
        assertNotNull(p2.getBand("cloud_abundance"));
        assertNotNull(p2.getBand("cropland_abundance"));

        Band band = p2.getBand("ocean_abundance");
        band.readRasterDataFully(ProgressMonitor.NULL);

        ProductData rasterData = band.getRasterData();
        for (int i = 0; i < rasterData.getNumElems(); i++) {
            assertTrue(rasterData.getElemFloatAt(i) >= 0);
            assertTrue(rasterData.getElemFloatAt(i) <= 1);
        }
    }

    public static class FooOp extends AbstractOperator {
        @TargetProduct
        Product targetProduct;
        @SourceProduct
        Product sourceProduct;

        public FooOp(OperatorSpi spi) {
            super(spi);
        }

        /**
         * Called by {@link #initialize(org.esa.beam.framework.gpf.OperatorContext, com.bc.ceres.core.ProgressMonitor)} after the {@link org.esa.beam.framework.gpf.OperatorContext}
         * is stored.
         *
         * @param pm a progress monitor. Can be used to signal progress.
         * @return the target product
         * @see #initialize(org.esa.beam.framework.gpf.OperatorContext, com.bc.ceres.core.ProgressMonitor)
         */
        @Override
        protected Product initialize(ProgressMonitor pm) throws OperatorException {
            Product product = new Product("X", "Y", sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
            String[] bandNames = sourceProduct.getBandNames();
            for (String s : bandNames) {
                product.addBand(s, ProductData.TYPE_FLOAT32);
            }
            return product;
        }

        /**
         * {@inheritDoc}
         * <p/>
         * <p>The default implementation throws a runtime exception with the message "not implemented"</p>.
         */
        @Override
        public void computeBand(Band band, Raster targetRaster, ProgressMonitor pm) throws OperatorException {
            Raster sourceRaster = getRaster(sourceProduct.getBand(band.getName()), targetRaster.getRectangle());
            int n = targetRaster.getDataBuffer().getNumElems();
            for (int i = 0; i < n; i++) {
                float v = sourceRaster.getDataBuffer().getElemFloatAt(i);
                if (v < 0) {
                    v = 0;
                }
                if (v > 1) {
                    v = 1;
                }
                targetRaster.getDataBuffer().setElemFloatAt(i, v);
            }
        }
    }

    public static class FooOpSpi extends AbstractOperatorSpi {
        public FooOpSpi() {
            super(FooOp.class, "Foo");
        }
    }
}
