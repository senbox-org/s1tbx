package org.esa.beam.framework.gpf.internal;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.Rectangle;
import java.util.HashMap;

public class SingleTargetTileTest extends TestCase {
    final int IMAGE_W = 4;
    final int IMAGE_H = 5;
    private Product product;

    @Override
    protected void setUp() throws Exception {
        product = new Product("N", "T", IMAGE_W, IMAGE_H);
    }

    @Override
    protected void tearDown() throws Exception {
        product.dispose();
        product = null;
    }

    public void testSingleTile() {

        Band band = product.addBand("B_FLOAT32", ProductData.TYPE_FLOAT32);

        TestOpImage image = new TestOpImage(band);
        assertEquals(IMAGE_W, image.getSampleModel().getWidth());
        assertEquals(IMAGE_H, image.getSampleModel().getHeight());

        // Force JAI tile computation
        image.getTiles();

        HashMap<Rectangle, TileImpl> gpfTiles = image.getGpfTiles();
        assertEquals(1, gpfTiles.size());

        TileImpl tile = gpfTiles.get(new Rectangle(IMAGE_W, IMAGE_H));
        assertNotNull(tile);
        assertEquals(true, tile.isTarget());
        assertSame(band, tile.getRasterDataNode());
        assertEquals(new Rectangle(IMAGE_W, IMAGE_H), tile.getRectangle());
        assertEquals(0, tile.getOffsetX());
        assertEquals(0, tile.getOffsetY());
        assertEquals(IMAGE_W, tile.getWidth());
        assertEquals(IMAGE_H, tile.getHeight());

        // test for initial sample values
        assertEquals(0.0, tile.getDouble(0, 0), 1e-5);
        assertEquals(0.0, tile.getDouble(0, 1), 1e-5);
        assertEquals(0.0, tile.getDouble(0, 2), 1e-5);
        assertEquals(0.0, tile.getDouble(0, 3), 1e-5);
        assertEquals(0.0, tile.getDouble(1, 0), 1e-5);
        assertEquals(0.0, tile.getDouble(2, 0), 1e-5);
        assertEquals(0.0, tile.getDouble(3, 0), 1e-5);
        assertEquals(0.0, tile.getDouble(1, 1), 1e-5);
        assertEquals(0.0, tile.getDouble(2, 2), 1e-5);
        assertEquals(0.0, tile.getDouble(3, 3), 1e-5);

        // Test that setter works
        tile.setDouble(2, 1, 0.03);
        assertEquals(0.03, tile.getDouble(2, 1), 1e-5);

        // Test that product data is wrapper for internal raster data buffer
        ProductData pd = tile.getDataBuffer();
        assertNotNull(pd);
        assertSame(pd, tile.getDataBuffer());

        pd.setElemDoubleAt(1 + 2 * IMAGE_W, 0.04);
        assertEquals(0.04, tile.getDouble(1, 2), 1e-5);

        ProductData sampleData = band.createCompatibleRasterData();
        for (int i = 0; i < sampleData.getNumElems(); i++) {
            sampleData.setElemDoubleAt(i, 100.0 + i);
        }

        tile.setSampleData(sampleData);

        assertEquals(100.0, tile.getDouble(0, 0), 1e-5);
        assertEquals(104.0, tile.getDouble(0, 1), 1e-5);
        assertEquals(108.0, tile.getDouble(0, 2), 1e-5);
        assertEquals(112.0, tile.getDouble(0, 3), 1e-5);
        assertEquals(101.0, tile.getDouble(1, 0), 1e-5);
        assertEquals(102.0, tile.getDouble(2, 0), 1e-5);
        assertEquals(103.0, tile.getDouble(3, 0), 1e-5);
        assertEquals(105.0, tile.getDouble(1, 1), 1e-5);
        assertEquals(110.0, tile.getDouble(2, 2), 1e-5);
        assertEquals(115.0, tile.getDouble(3, 3), 1e-5);
    }

}
