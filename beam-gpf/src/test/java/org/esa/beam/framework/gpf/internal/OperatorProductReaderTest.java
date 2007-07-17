package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.TileCache;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.TargetProduct;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;

// todo - test with graph with more than one operator
public class OperatorProductReaderTest extends TestCase {

    private Op.Spi testOperatorSpi = new Op.Spi();
    private TileCache tileCache = GPF.getDefaultInstance().getTileCache();


    @Override
    protected void setUp() throws Exception {
        OperatorSpiRegistry registry = OperatorSpiRegistry.getInstance();
        registry.addOperatorSpi(testOperatorSpi);

        tileCache.clean();
    }

    @Override
    protected void tearDown() throws Exception {
        OperatorSpiRegistry registry = OperatorSpiRegistry.getInstance();
        registry.removeOperatorSpi(testOperatorSpi);
    }

    public void testTileSizeSmallerThanMinTileSizeOfTileCache() throws IOException, OperatorException {
        Product target = createTestTargetProduct();

        long cacheMinTileSize = tileCache.getMinimumTileSize();
        int tileSide = (int) Math.sqrt(cacheMinTileSize) - 1;
        ProductData data = ProductData.createInstance(ProductData.TYPE_INT8, tileSide * tileSide);
        target.getBandAt(0).readRasterData(0, 0, tileSide, tileSide, data, ProgressMonitor.NULL);

        Tile[] computedTiles = tileCache.getTiles(target.getBandAt(0));
        assertEquals(0, computedTiles.length);
        assertEquals(0, tileCache.getCurrentMemory());

    }

    public void testTileSizeBetweenMinAndDefaultTileSize() throws IOException, OperatorException {
        Product target = createTestTargetProduct();

        // TileSize is currently by default SceneWidth * 200;
        long cacheMinTileSize = tileCache.getMinimumTileSize();
        int tileSide = (int) Math.sqrt(cacheMinTileSize) + 10;
        ProductData data = ProductData.createInstance(ProductData.TYPE_INT8, tileSide * tileSide);
        target.getBandAt(0).readRasterData(0, 0, tileSide, tileSide, data, ProgressMonitor.NULL);

        Tile[] computedTiles = tileCache.getTiles(target.getBandAt(0));
        assertEquals(1, computedTiles.length);
        assertEquals(Tile.State.COMPUTED, computedTiles[0].getState());
        assertEquals(2 * 100 * 200, tileCache.getCurrentMemory());

    }

    public void testRequestIsBiggerAsTileSize() throws IOException, OperatorException {
        Product target = createTestTargetProduct();

        // TileSize is currently by default SceneWidth * 200;
        ProductData data = ProductData.createInstance(ProductData.TYPE_INT8, target.getSceneRasterWidth() * 250);
        target.getBandAt(0).readRasterData(0, 0, target.getSceneRasterWidth(), 250, data, ProgressMonitor.NULL);

        Tile[] computedTiles = tileCache.getTiles(target.getBandAt(0));
        assertEquals(2, computedTiles.length);
        assertEquals(Tile.State.COMPUTED, computedTiles[0].getState());
        assertEquals(Tile.State.COMPUTED, computedTiles[1].getState());
        assertEquals(2 * (2 * 100 * 200), tileCache.getCurrentMemory()); // two tiles for source and target raster
    }

    public void testRequestIsBiggerAsTileSizeWithTwoOperators() throws IOException, OperatorException {
        Product target1 = createTestTargetProduct();
        Product target2 = GPF.createProduct("Op", new HashMap<String, Object>(), target1, ProgressMonitor.NULL);

        // TileSize is currently by default SceneWidth * 200;
        ProductData data = ProductData.createInstance(ProductData.TYPE_INT8, target2.getSceneRasterWidth() * 250);
        target2.getBandAt(0).readRasterData(0, 0, target2.getSceneRasterWidth(), 250, data, ProgressMonitor.NULL);

        Tile[] computedTiles1 = tileCache.getTiles(target1.getBandAt(0));
        assertEquals(2, computedTiles1.length);
        assertEquals(Tile.State.COMPUTED, computedTiles1[0].getState());
        assertEquals(Tile.State.COMPUTED, computedTiles1[1].getState());

        Tile[] computedTiles2 = tileCache.getTiles(target2.getBandAt(0));
        assertEquals(2, computedTiles2.length);
        assertEquals(Tile.State.COMPUTED, computedTiles2[0].getState());
        assertEquals(Tile.State.COMPUTED, computedTiles2[1].getState());

        assertEquals(3 * (2 * 100 * 200), tileCache.getCurrentMemory()); // two tiles for source and target raster

        byte[] values = (byte[]) computedTiles2[0].getRaster().getDataBuffer().getElems();
        byte[] expectedValues = new byte[values.length];
        Arrays.fill(expectedValues, (byte) 2);
        assertTrue(Arrays.equals(expectedValues, values));
    }

    public void testMoreTiles() throws Exception {
        Product target = createTestTargetProduct();

        // TileSize is currently by default SceneWidth * 200;
        ProductData data = ProductData.createInstance(ProductData.TYPE_INT8, target.getSceneRasterWidth() * 180);

        target.getBandAt(0).readRasterData(0, 0, target.getSceneRasterWidth(), 180, data, ProgressMonitor.NULL);
        Tile[] computedTiles = tileCache.getTiles(target.getBandAt(0));
        assertEquals(1, computedTiles.length);
        assertEquals(Tile.State.COMPUTED, computedTiles[0].getState());
        assertEquals(2 * (100 * 200), tileCache.getCurrentMemory()); // one tile for source and target raster

        data = ProductData.createInstance(ProductData.TYPE_INT8, target.getSceneRasterWidth() * 200);
        target.getBandAt(0).readRasterData(0, 0, target.getSceneRasterWidth(), 200, data, ProgressMonitor.NULL);
        computedTiles = tileCache.getTiles(target.getBandAt(0));
        assertEquals(1, computedTiles.length);
        assertEquals(Tile.State.COMPUTED, computedTiles[0].getState());
        assertEquals(2 * (100 * 200), tileCache.getCurrentMemory()); // still one tile for source and target raster

        data = ProductData.createInstance(ProductData.TYPE_INT8, target.getSceneRasterWidth() * 50);
        target.getBandAt(0).readRasterData(0, 400, target.getSceneRasterWidth(), 50, data, ProgressMonitor.NULL);
        computedTiles = tileCache.getTiles(target.getBandAt(0));
        assertEquals(2, computedTiles.length);
        assertEquals(Tile.State.COMPUTED, computedTiles[0].getState());
        assertEquals(Tile.State.COMPUTED, computedTiles[1].getState());
        // two tiles for source and target raster but second is smaller, it's the remain of the raster
        assertEquals(2 * (100 * 200 ) + 2 * (100 * 100), tileCache.getCurrentMemory());

        data = ProductData.createInstance(ProductData.TYPE_INT8, target.getSceneRasterWidth());
        target.getBandAt(0).readRasterData(0, 250, target.getSceneRasterWidth(), 1, data, ProgressMonitor.NULL);
        computedTiles = tileCache.getTiles(target.getBandAt(0));
        assertEquals(2, computedTiles.length);
        assertEquals(Tile.State.COMPUTED, computedTiles[0].getState());
        assertEquals(Tile.State.COMPUTED, computedTiles[1].getState());
        // nothing has changed, cause tile is to small
        assertEquals(2 * (100 * 200 ) + 2 * (100 * 100), tileCache.getCurrentMemory());

        data = ProductData.createInstance(ProductData.TYPE_INT8, target.getSceneRasterWidth() * 250);
        target.getBandAt(0).readRasterData(0, 190, target.getSceneRasterWidth(), 250, data, ProgressMonitor.NULL);
        computedTiles = tileCache.getTiles(target.getBandAt(0));
        assertEquals(3, computedTiles.length);
        assertEquals(Tile.State.COMPUTED, computedTiles[0].getState());
        assertEquals(Tile.State.COMPUTED, computedTiles[1].getState());
        assertEquals(Tile.State.COMPUTED, computedTiles[2].getState());
        // all three tiles in cache
        assertEquals(2 * (100 * 200 ) + 2 * (100 * 200 ) + 2 * (100 * 100), tileCache.getCurrentMemory());
    }

    private Product createTestTargetProduct() throws OperatorException {
        Product testSourceProduct = createTestSourceProduct();
        HashMap<String, Object> parameters = new HashMap<String, Object>();
        return GPF.createProduct("Op", parameters, testSourceProduct, ProgressMonitor.NULL);
    }

    private Product createTestSourceProduct() {
        Product product = new Product("testProduct", "Foo", 100, 500);
        RasterDataNode rasterDataNode = product.addBand("band", ProductData.TYPE_INT8);
        rasterDataNode.setSynthetic(true);
        rasterDataNode.setData(ProductData.createInstance(ProductData.TYPE_INT8, 100 * 500));
        return product;
    }

    private static class Op extends AbstractOperator {

        @SourceProduct
        private Product sourceProduct;
        @TargetProduct
        private Product targetProduct;

        private Band sourceBand;
        private Band targetBand1;
        private Band targetBand2;

        public Op(OperatorSpi spi) {
            super(spi);
        }

        @Override
        protected Product initialize(ProgressMonitor pm) throws OperatorException {
            targetProduct = new Product(TileComputingStrategyTest.class.getName(), TileComputingStrategyTest.class.getName(),
                                        sourceProduct.getSceneRasterWidth(), sourceProduct.getSceneRasterHeight());
            sourceBand = sourceProduct.getBandAt(0);
            targetBand1 = targetProduct.addBand("target1", sourceBand.getDataType());
            targetBand2 = targetProduct.addBand("target2", sourceBand.getDataType());
            return targetProduct;
        }

        @Override
        public void computeBand(Raster targetRaster,
                                ProgressMonitor pm) throws OperatorException {

            Rectangle targetTileRectangle = targetRaster.getRectangle();
            Raster sourceRaster = getRaster(sourceBand, targetTileRectangle);

            int x0 = targetTileRectangle.x;
            int y0 = targetTileRectangle.y;
            int w = targetTileRectangle.width;
            int h = targetTileRectangle.height;
            for (int y = y0; y < y0 + h; y++) {
                for (int x = x0; x < x0 + w; x++) {
                    double v = sourceRaster.getDouble(x, y);
                    if (targetRaster.getRasterDataNode() == targetBand1) {
                        double v1 = 1.0 + v; // Place your transformation math here
                        targetRaster.setDouble(x, y, v1);
                    } else if (targetRaster.getRasterDataNode() == targetBand2) {
                        double v2 = 2.0 + v; // Place your transformation math here
                        targetRaster.setDouble(x, y, v2);
                    }
                }
            }
        }

        @Override
        public void computeAllBands(Rectangle targetTileRectangle,
                                    ProgressMonitor pm) throws OperatorException {

            Raster sourceRaster = getRaster(sourceBand, targetTileRectangle);
            Raster targetRaster1 = getRaster(targetBand1, targetTileRectangle);
            Raster targetRaster2 = getRaster(targetBand2, targetTileRectangle);

            int x0 = targetTileRectangle.x;
            int y0 = targetTileRectangle.y;
            int w = targetTileRectangle.width;
            int h = targetTileRectangle.height;
            for (int y = y0; y < y0 + h; y++) {
                for (int x = x0; x < x0 + w; x++) {
                    double v = sourceRaster.getDouble(x, y);
                    double v1 = 1.0 + v; // Place your transformation math here
                    double v2 = 2.0 + v; // Place your transformation math here
                    targetRaster1.setDouble(x, y, v1);
                    targetRaster2.setDouble(x, y, v2);
                }
            }
        }

        public static class Spi implements OperatorSpi {

            public Class<? extends Operator> getOperatorClass() {
                return Op.class;
            }

            public String getName() {
                return "Op";
            }

            public String getAuthor() {
                return "John Doe";
            }

            public String getCopyright() {
                return "GPL";
            }

            public String getDescription() {
                return "";
            }

            public String getVersion() {
                return "X.X.X";
            }
        }
    }

}
