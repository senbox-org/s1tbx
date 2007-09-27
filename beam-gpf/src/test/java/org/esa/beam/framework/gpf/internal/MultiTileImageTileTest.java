package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;

public class MultiTileImageTileTest extends AbstractTileImageTileTest {

    final int IMAGE_W = 10;
    final int IMAGE_H = 12;
    final int TILE_SIZE = 6;
    private TestOpImage imageFLOAT32;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        imageFLOAT32 = getImage("B_FLOAT32");
    }

    @Override
    public Dimension getImageSize() {
        return new Dimension(IMAGE_W, IMAGE_H);
    }

    @Override
    public Dimension getTileSize() {
        return new Dimension(TILE_SIZE, TILE_SIZE);
    }

    public void testThatImageIsTiled() {
        assertEquals(4, imageFLOAT32.getNumTileImpls());
        assertEquals(TILE_SIZE, imageFLOAT32.getSampleModel().getWidth());
        assertEquals(TILE_SIZE, imageFLOAT32.getSampleModel().getHeight());
    }

    public void testThatSampleData_IS_NOT_A_CopyForTile00() {
        Rectangle expectedRect = new Rectangle(0, 0,
                                               TILE_SIZE,
                                               TILE_SIZE);
        TileImpl tile = imageFLOAT32.getTileImpl(expectedRect);
        testTargetTile(tile, false, expectedRect);
    }

    public void testThatSampleData_IS_A_CopyForTile10() {
        Rectangle expectedRect = new Rectangle(TILE_SIZE, 0,
                                               IMAGE_W - TILE_SIZE,
                                               TILE_SIZE);
        TileImpl tile = imageFLOAT32.getTileImpl(expectedRect);
        testTargetTile(tile, true, expectedRect);
    }

    public void testThatSampleData_IS_NOT_A_CopyForTile01() {
        Rectangle expectedRect = new Rectangle(0, TILE_SIZE,
                                               TILE_SIZE,
                                               IMAGE_H - TILE_SIZE);
        TileImpl tile = imageFLOAT32.getTileImpl(expectedRect);
        testTargetTile(tile, false, expectedRect);
    }

    public void testThatSampleData_IS_A_CopyForTile11() {
        Rectangle expectedRect = new Rectangle(TILE_SIZE, TILE_SIZE,
                                               IMAGE_W - TILE_SIZE,
                                               IMAGE_H - TILE_SIZE);
        TileImpl tile = imageFLOAT32.getTileImpl(expectedRect);
        testTargetTile(tile, true, expectedRect);
    }

    public void testSourceTileIsContainedInImageTile00() {
        final int CHILD_X = 2;
        final int CHILD_Y = 3;
        final int CHILD_W = 4;
        final int CHILD_H = 2;

        Band band = getBand("B_FLOAT32");
        Rectangle expectedRect = new Rectangle(CHILD_X, CHILD_Y, CHILD_W, CHILD_H);
        Raster raster = getImageData(imageFLOAT32, expectedRect);
        TileImpl tile = new TileImpl(band, raster);
        assertSame(band, tile.getRasterDataNode());
        testOnlySamplesFloatAccessible(tile);

        int expectedScanlineOffset = CHILD_Y * TILE_SIZE + CHILD_X;
        int expectedScanlineStride = TILE_SIZE;

        testTileStructure(tile, expectedRect, expectedScanlineOffset, expectedScanlineStride, false);
    }

    public void testSourceTileIsNotContainedInAnyImageTile() {
        final int CHILD_X = 5;
        final int CHILD_Y = 3;
        final int CHILD_W = 4;
        final int CHILD_H = 7;

        Band band = getBand("B_FLOAT32");
        Rectangle expectedRect = new Rectangle(CHILD_X, CHILD_Y, CHILD_W, CHILD_H);
        Raster raster = getImageData(imageFLOAT32, expectedRect);
        TileImpl tile = new TileImpl(band, raster);
        assertSame(band, tile.getRasterDataNode());
        testOnlySamplesFloatAccessible(tile);

        int expectedScanlineOffset = 0;
        int expectedScanlineStride = CHILD_W;

        testTileStructure(tile, expectedRect, expectedScanlineOffset, expectedScanlineStride, false);
    }


    private void testTargetTile(TileImpl tile, boolean copy, Rectangle expectedRect) {
        assertNotNull(tile);

        assertEquals(true, tile.isTarget());
        assertSame(getBand("B_FLOAT32"), tile.getRasterDataNode());
        testOnlySamplesFloatAccessible(tile);

        testTileStructure(tile, expectedRect, 0, TILE_SIZE, true);

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
