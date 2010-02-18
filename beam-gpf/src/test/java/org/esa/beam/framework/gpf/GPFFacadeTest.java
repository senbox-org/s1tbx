package org.esa.beam.framework.gpf;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.MetadataElement;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.awt.*;

public class GPFFacadeTest extends TestCase {

    public void testDefaultSettings() {
        GPF gpf = GPF.getDefaultInstance();
        assertNotNull(gpf);
        assertSame(gpf, GPF.getDefaultInstance());
    }

    public void testOperatorApi() throws IOException, OperatorException, URISyntaxException {
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();

        String filePath = GPFFacadeTest.class.getResource("test-product.dim").toURI().getPath();

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("file", new File(filePath));

        Product p1 = GPF.createProduct("Read", parameters);

        assertNotNull(p1);
        assertNotNull(p1.getBand("forrest_abundance"));
        assertNotNull(p1.getBand("ocean_abundance"));
        assertNotNull(p1.getBand("cloud_abundance"));
        assertNotNull(p1.getBand("cropland_abundance"));

        assertNotNull(p1.getFileLocation());
        assertEquals("test-product.dim", p1.getFileLocation().getName());

        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new FooOpSpi());

        Product p2 = GPF.createProduct("Foo", GPF.NO_PARAMS, p1);

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

    public void testProductName() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new FooOpSpi());

        String filePath = GPFFacadeTest.class.getResource("test-product.dim").toURI().getPath();

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("file", new File(filePath));

        Product p1 = GPF.createProduct("Read", parameters);

        Product p2 = GPF.createProduct("Foo", GPF.NO_PARAMS, new Product[]{p1});
        MetadataElement metadataElement = p2.getMetadataRoot().getElement("Processing_Graph");
        MetadataElement sourceElement = null;
        for (MetadataElement element : metadataElement.getElements()) {
            if (element.getAttribute("operator").getData().getElemString().equals("Foo")) {
                sourceElement = element.getElement("sources");
                break;
            }
        }
        assertNotNull(sourceElement);
        assertEquals(1, sourceElement.getNumAttributes());
        assertEquals("sourceProduct", sourceElement.getAttributeAt(0).getName());
        assertTrue(sourceElement.getAttributeAt(0).getData().getElemString().endsWith("test-product.dim"));
    }

    public void testMultiProductsNames() throws Exception {
        GPF.getDefaultInstance().getOperatorSpiRegistry().loadOperatorSpis();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new FooOpSpi());
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(new FoosOpSpi());

        String filePath = GPFFacadeTest.class.getResource("test-product.dim").toURI().getPath();

        Map<String, Object> parameters = new HashMap<String, Object>();
        parameters.put("file", new File(filePath));

        Product p1 = GPF.createProduct("Read", parameters);
        Product p2 = GPF.createProduct("Foo", GPF.NO_PARAMS, new Product[]{p1});
        Product p3 = GPF.createProduct("Foos", GPF.NO_PARAMS, new Product[]{p1, p2});
        MetadataElement metadataElement = p3.getMetadataRoot().getElement("Processing_Graph");
        MetadataElement sourceElement = null;
        for (MetadataElement element : metadataElement.getElements()) {
            if (element.getAttribute("operator").getData().getElemString().equals("Foos")) {
                sourceElement = element.getElement("sources");
                break;
            }
        }
        assertNotNull(sourceElement);
        assertEquals(2, sourceElement.getNumAttributes());
        assertEquals("sourceProduct.1", sourceElement.getAttributeAt(0).getName());
        assertEquals("sourceProduct.0", sourceElement.getAttributeAt(1).getName());

    }

    public void testTileSizeRenderingHint() {
        final RenderingHints renderingHints = new RenderingHints(null);

        try {
            renderingHints.put(GPF.KEY_TILE_SIZE, null);
            fail();
        } catch (Exception expected) {
        }
        try {
            renderingHints.put(GPF.KEY_TILE_SIZE, new Object());
            fail();
        } catch (Exception expected) {
        }
        try {
            renderingHints.put(GPF.KEY_TILE_SIZE, new Dimension());
            fail();
        } catch (Exception expected) {
        }

        final Dimension tileSize = new Dimension(1, 1);
        renderingHints.put(GPF.KEY_TILE_SIZE, tileSize);

        assertSame(tileSize, renderingHints.get(GPF.KEY_TILE_SIZE));
    }

    @OperatorMetadata(alias = "Foo")
    public static class FooOp extends Operator {
        @TargetProduct
        Product targetProduct;
        @SourceProduct
        Product sourceProduct;

        @Override
        public void initialize() throws OperatorException {
            targetProduct = new Product("X", "Y", sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
            String[] bandNames = sourceProduct.getBandNames();
            for (String s : bandNames) {
                targetProduct.addBand(s, ProductData.TYPE_FLOAT32);
            }
        }

        /**
         * {@inheritDoc}
         * <p/>
         * <p>The default implementation throws a runtime exception with the message "not implemented"</p>.
         */
        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
            Tile sourceTile = getSourceTile(sourceProduct.getBand(band.getName()), targetTile.getRectangle(), pm);
            ProductData rawTarget = targetTile.getRawSamples();
            ProductData rawSource = sourceTile.getRawSamples();
            int n = rawTarget.getNumElems();
            for (int i = 0; i < n; i++) {
                float v = rawSource.getElemFloatAt(i);
                if (v < 0) {
                    v = 0;
                }
                if (v > 1) {
                    v = 1;
                }
                rawTarget.setElemFloatAt(i, v);
            }
            targetTile.setRawSamples(rawTarget);
        }
    }

    public static class FooOpSpi extends OperatorSpi {
        public FooOpSpi() {
            super(FooOp.class);
        }
    }

    @OperatorMetadata(alias = "Foos")
    public static class FoosOp extends Operator {
        @TargetProduct
        Product targetProduct;
        @SourceProducts
        Product[] sourceProducts;

        @Override
        public void initialize() throws OperatorException {
            targetProduct = new Product("X", "Y", sourceProducts[0].getSceneRasterWidth(), sourceProducts[0].getSceneRasterHeight());
            String[] bandNames = sourceProducts[0].getBandNames();
            for (String s : bandNames) {
                targetProduct.addBand(s, ProductData.TYPE_FLOAT32);
            }
        }

        /**
         * {@inheritDoc}
         * <p/>
         * <p>The default implementation throws a runtime exception with the message "not implemented"</p>.
         */
        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
            byte[] dataBufferByte = targetTile.getDataBufferByte();
            Arrays.fill(dataBufferByte, (byte) 3);
        }
    }

    public static class FoosOpSpi extends OperatorSpi {
        public FoosOpSpi() {
            super(FoosOp.class);
        }
    }
}
