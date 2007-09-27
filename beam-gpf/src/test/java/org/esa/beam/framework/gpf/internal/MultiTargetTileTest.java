package org.esa.beam.framework.gpf.internal;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.Rectangle;
import java.util.HashMap;

public class MultiTargetTileTest extends TestCase {
    final int IMAGE_W = 10;
    final int IMAGE_H = 12;
    final int TILE_SIZE = 6;
    private Product product;
    private HashMap<Rectangle, TileImpl> tiles;
    private TestOpImage image;

    @Override
    protected void setUp() throws Exception {
        product = new Product("N", "T", IMAGE_W, IMAGE_H);
        product.setPreferredTileSize(TILE_SIZE, TILE_SIZE);
        product.addBand("B_FLOAT32", ProductData.TYPE_FLOAT32);
        image = new TestOpImage(product.getBand("B_FLOAT32"));
        // Force JAI tile computation
        image.getTiles();
        tiles = image.getGpfTiles();
    }

    @Override
    protected void tearDown() throws Exception {
        product.dispose();
        product = null;
        tiles.clear();
        tiles = null;
        image.dispose();
        image = null;
    }

    public void testThat4TilesAreCreated() {
        assertEquals(4, tiles.size());
    }

    public void testThatImageIsTiled() {
        assertEquals(TILE_SIZE, image.getSampleModel().getWidth());
        assertEquals(TILE_SIZE, image.getSampleModel().getHeight());
    }

    public void testThatSampleData_IS_NOT_A_CopyForTile00() {
        Rectangle expectedRect = new Rectangle(0, 0,
                                               TILE_SIZE,
                                               TILE_SIZE);
        TileImpl tile = tiles.get(expectedRect);
        testTile(tile, false, expectedRect);
    }

    public void testThatSampleData_IS_A_CopyForTile10() {
        Rectangle expectedRect = new Rectangle(TILE_SIZE, 0,
                                               IMAGE_W - TILE_SIZE,
                                               TILE_SIZE);
        TileImpl tile = tiles.get(expectedRect);
        testTile(tile, true, expectedRect);
    }

    public void testThatSampleData_IS_NOT_A_CopyForTile01() {
        Rectangle expectedRect = new Rectangle(0, TILE_SIZE,
                                               TILE_SIZE,
                                               IMAGE_H - TILE_SIZE);
        TileImpl tile = tiles.get(expectedRect);
        testTile(tile, false, expectedRect);
    }

    public void testThatSampleData_IS_A_CopyForTile11() {
        Rectangle expectedRect = new Rectangle(TILE_SIZE, TILE_SIZE,
                                               IMAGE_W - TILE_SIZE,
                                               IMAGE_H - TILE_SIZE);
        TileImpl tile = tiles.get(expectedRect);
        testTile(tile, true, expectedRect);
    }

    private void testTile(TileImpl tile, boolean copy, Rectangle expectedRect) {
        assertNotNull(tile);

        assertEquals(true, tile.isTarget());
        assertSame(product.getBand("B_FLOAT32"), tile.getRasterDataNode());
        assertEquals(expectedRect, tile.getRectangle());
        assertEquals(expectedRect.x, tile.getMinX());
        assertEquals(expectedRect.y, tile.getMinY());
        assertEquals(expectedRect.width, tile.getWidth());
        assertEquals(expectedRect.height, tile.getHeight());
        assertEquals(0, tile.getScanlineOffset());
        assertEquals(TILE_SIZE, tile.getScanlineStride());
        assertNull(tile.getRawSamplesByte());
        assertNull(tile.getRawSamplesShort());
        assertNull(tile.getRawSamplesInt());
        assertNotNull(tile.getRawSamplesFloat());
        assertNull(tile.getRawSamplesDouble());

        int x0 = tile.getMinX();
        int y0 = tile.getMinY();

        testIndexOutOfBoundsException(tile, x0 - 1, y0);
        testIndexOutOfBoundsException(tile, x0, y0 - 1);
        testIndexOutOfBoundsException(tile, x0 + TILE_SIZE, y0);
        testIndexOutOfBoundsException(tile, x0, y0 + TILE_SIZE);

        testScaledSampleAccess(tile, x0, y0);
        testScaledSampleAccess(tile, x0 + 1, y0);
        testScaledSampleAccess(tile, x0, y0 + 1);
        testScaledSampleAccess(tile, x0 + 1, y0 + 1);

        testRawSampleAccess(tile, x0, y0, copy);
        testRawSampleAccess(tile, x0 + 3, y0 + 1, copy);
        testRawSampleAccess(tile, x0 + 3, y0 + 3, copy);
        testRawSampleAccess(tile, x0 + 3, y0 + 2, copy);
        testRawSampleAccess(tile, x0 + 1, y0 + 1, copy);
    }

    private void testIndexOutOfBoundsException(TileImpl tile, int x, int y) {
        try {
            tile.getSampleDouble(x, y);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException e) {
            // ok
        }
        try {
            tile.setSample(x, y, 0.0);
            fail("ArrayIndexOutOfBoundsException expected");
        } catch (ArrayIndexOutOfBoundsException e) {
            // ok
        }
    }

    private void testScaledSampleAccess(TileImpl tile, int x0, int y0) {
        tile.setSample(x0, y0, 0.23);
        assertEquals(0.23, tile.getSampleDouble(x0, y0), 1e-5);
    }

    private void testRawSampleAccess(TileImpl tile, int x, int y, boolean copy) {
        // Test that we DO NOT have direct access to data because dataBuffer IS a copy
        double oldValue = tile.getSampleDouble(x, y);
        double newValue = -123.45;
        assertTrue(Math.abs(oldValue - newValue) > 0.01); // should be clearly different
        ProductData sd = tile.getRawSampleData();
        int i = (y - tile.getMinY()) * tile.getRectangle().width + (x - tile.getMinX());
        sd.setElemDoubleAt(i, newValue);
        assertEquals(newValue, sd.getElemDoubleAt(i), 1e-5); // new value in buffer
        if (copy) {
            assertEquals(oldValue, tile.getSampleDouble(x, y), 1e-5); // still old value in raster data
            tile.setRawSampleData(sd); // commit changes
        }
        assertEquals(newValue, tile.getSampleDouble(x, y), 1e-5); // still old value in raster data
    }


}
