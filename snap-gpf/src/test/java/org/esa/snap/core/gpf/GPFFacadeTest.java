/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.GlobalTestConfig;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.math.MathUtils;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.*;


public class GPFFacadeTest {

    private static FooOpSpi foo = new FooOpSpi();
    private static FoosOpSpi foos = new FoosOpSpi();

    @BeforeClass
    public static void loadSpis() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(foo);
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(foos);
    }

    @AfterClass
    public static void unloadSpis() {
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(foo);
        GPF.getDefaultInstance().getOperatorSpiRegistry().removeOperatorSpi(foos);
    }

    @Test
    public void testDefaultSettings() {
        GPF gpf = GPF.getDefaultInstance();
        assertNotNull(gpf);
        assertSame(gpf, GPF.getDefaultInstance());
    }

    @Test
    public void testAutoParameterConversion() {

        // First test with no parameter values --> parameters have default values
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        FooOp fooOp = makeFooOp(parameters);
        assertEquals(0, fooOp.intParam);
        assertEquals(0.0, fooOp.doubleParam, 1e-7);
        assertEquals(null, fooOp.floatArrayParam);
        assertEquals(null, fooOp.stringParam);
        assertEquals(false, fooOp.booleanParam);

        // Then we test with parameter values that already have the expected parameter type
        parameters = new HashMap<String, Object>();
        parameters.put("intParam", 44);
        parameters.put("doubleParam", 0.441);
        parameters.put("floatArrayParam", new float[]{0.2f, 0.4f, 0.8f});
        parameters.put("stringParam", "Banana");
        parameters.put("booleanParam", true);
        fooOp = makeFooOp(parameters);
        assertEquals(44, fooOp.intParam);
        assertEquals(0.441, fooOp.doubleParam, 1e-7);
        assertTrue(Arrays.equals(new float[]{0.2f, 0.4f, 0.8f}, fooOp.floatArrayParam));
        assertEquals("Banana", fooOp.stringParam);
        assertEquals(true, fooOp.booleanParam);

        // Finally we test that values are correctly converted if provided as text
        parameters.put("intParam", "42");
        parameters.put("doubleParam", "0.421");
        parameters.put("floatArrayParam", "0.1, 0.2, 0.5");
        parameters.put("stringParam", "Mexico");
        parameters.put("booleanParam", "1");
        fooOp = makeFooOp(parameters);
        assertEquals(42, fooOp.intParam);
        assertEquals(0.421, fooOp.doubleParam, 1e-7);
        assertTrue(Arrays.equals(new float[]{0.1f, 0.2f, 0.5f}, fooOp.floatArrayParam));
        assertEquals("Mexico", fooOp.stringParam);
        assertEquals(true, fooOp.booleanParam);
    }

    private FooOp makeFooOp(Map<String, Object> parameters) {
        Product p1 = new Product("A", "B", 16, 16);
        HashMap<String, Product> sourceProducts = new HashMap<String, Product>();
        sourceProducts.put("sourceProduct", p1);
        FooOp fooOp = (FooOp) GPF.getDefaultInstance().createOperator("Foo", parameters, sourceProducts, null);
        fooOp.getTargetProduct();
        return fooOp;

    }

    @Test
    public void testOperatorApi() throws IOException, OperatorException, URISyntaxException {

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

    @Test
    public void testProductName() throws Exception {

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

    @Test
    public void testMultiProductsNames() throws Exception {

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
        assertEquals("sourceProduct.2", sourceElement.getAttributeAt(1).getName());

    }

    @Test
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

        @Parameter
        int intParam;

        @Parameter
        double doubleParam;

        @Parameter
        String stringParam;

        @Parameter
        boolean booleanParam;

        @Parameter
        float[] floatArrayParam;


        @Override
        public void initialize() throws OperatorException {
            targetProduct = new Product("X", "Y", sourceProduct.getSceneRasterWidth(),
                                        sourceProduct.getSceneRasterHeight());
            String[] bandNames = sourceProduct.getBandNames();
            for (String s : bandNames) {
                targetProduct.addBand(s, ProductData.TYPE_FLOAT32);
            }
        }

        /**
         * {@inheritDoc}
         * <p>The default implementation throws a runtime exception with the message "not implemented".
         */
        @Override
        public void computeTile(Band band, Tile targetTile, ProgressMonitor pm) throws OperatorException {
            Tile sourceTile = getSourceTile(sourceProduct.getBand(band.getName()), targetTile.getRectangle());
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
            targetProduct = new Product("X", "Y", sourceProducts[0].getSceneRasterWidth(),
                                        sourceProducts[0].getSceneRasterHeight());
            String[] bandNames = sourceProducts[0].getBandNames();
            for (String s : bandNames) {
                targetProduct.addBand(s, ProductData.TYPE_FLOAT32);
            }
        }

        /**
         * {@inheritDoc}
         * <p>The default implementation throws a runtime exception with the message "not implemented".
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

    @Test
    public void testWriteProduct() throws Exception {
        StackOp stackOp = new StackOp();
        Product source = new Product("name", "type", 1000, 1000);
        source.setPreferredTileSize(200, 200);
        stackOp.setSourceProduct(source);
        Product targetProduct = stackOp.getTargetProduct();
        File outputFile = GlobalTestConfig.getBeamTestDataOutputFile("GPFFacadeTest/testWriteProduct.dim");
        try {
            outputFile.getParentFile().mkdirs();
            GPF.writeProduct(targetProduct, outputFile, "BEAM-DIMAP", false, true, ProgressMonitor.NULL);
        } finally {
            FileUtils.deleteTree(outputFile.getParentFile());
        }
        assertEquals(5*5, stackOp.computeTileStackCounter.get());
    }

    @Test
    public void testWriteProductWithCacheClearing() throws Exception {
        StackOp stackOp = new StackOp();
        Product source = new Product("name", "type", 1000, 1000);
        source.setPreferredTileSize(200, 200);
        stackOp.setSourceProduct(source);
        Product targetProduct = stackOp.getTargetProduct();
        File outputFile = GlobalTestConfig.getBeamTestDataOutputFile("GPFFacadeTest/testWriteProduct.dim");
        try {
            outputFile.getParentFile().mkdirs();
            GPF.writeProduct(targetProduct, outputFile, "BEAM-DIMAP", true, true, ProgressMonitor.NULL);
        } finally {
            FileUtils.deleteTree(outputFile.getParentFile());
        }
        assertEquals(5*5, stackOp.computeTileStackCounter.get());
    }

    private static class StackOp extends Operator {

        AtomicInteger computeTileStackCounter = new AtomicInteger(0);
        Dimension tileSize;

        @Override
        public void initialize() throws OperatorException {
            Product sourceProduct = getSourceProduct();
            tileSize = sourceProduct.getPreferredTileSize();
            Product product = new Product("name", "type", sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
            product.addBand("A", ProductData.TYPE_FLOAT32);
            product.addBand("B", ProductData.TYPE_FLOAT32);
            setTargetProduct(product);
        }

        @Override
        public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
            //logTileIndex(targetRectangle.x, targetRectangle.y);
            computeTileStackCounter.incrementAndGet();
            Arrays.fill(targetTiles.get(getTargetProduct().getBand("A")).getDataBufferFloat(), 5f);
            Arrays.fill(targetTiles.get(getTargetProduct().getBand("B")).getDataBufferFloat(), 7f);
        }

        private void logTileIndex(int x, int y) {
            int tileX = MathUtils.floorInt(x / (double) tileSize.width);
            int tileY = MathUtils.floorInt(y / (double) tileSize.height);
            System.out.println("tileY = " + tileY + "  tileX = " + tileX);
        }
    }
}
