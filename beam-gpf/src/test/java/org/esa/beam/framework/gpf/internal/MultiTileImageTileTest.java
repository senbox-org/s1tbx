package org.esa.beam.framework.gpf.internal;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;

public class MultiTileImageTileTest extends AbstractTileImageTileTest {

    final static int IMAGE_W = 10;
    final static int IMAGE_H = 12;
    final static int TILE_SIZE = 6;
    private TestOpImage imageFLOAT32;

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        imageFLOAT32 = getImage("B_FLOAT32");
    }

    @Override
    public int getTileCount() {
        return 4;
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
        assertEquals(TILE_SIZE, imageFLOAT32.getSampleModel().getWidth());
        assertEquals(TILE_SIZE, imageFLOAT32.getSampleModel().getHeight());
        assertEquals(4, imageFLOAT32.getTileCount());
    }

    public void testTargetTile00() {
        testTargetTile(new Rectangle(0,
                                     0,
                                     TILE_SIZE,
                                     TILE_SIZE));
    }

    public void testTargetTile10() {
        testTargetTile(new Rectangle(TILE_SIZE,
                                     0,
                                     IMAGE_W - TILE_SIZE,
                                     TILE_SIZE));
    }

    public void testTargetTile01() {
        testTargetTile(new Rectangle(0,
                                     TILE_SIZE,
                                     TILE_SIZE,
                                     IMAGE_H - TILE_SIZE));
    }

    public void testTargetTile11() {
        testTargetTile(new Rectangle(TILE_SIZE,
                                     TILE_SIZE,
                                     IMAGE_W - TILE_SIZE,
                                     IMAGE_H - TILE_SIZE));
    }

    private void testTargetTile(Rectangle expectedRect) {
        TileImpl tile = imageFLOAT32.getTile(expectedRect);
        assertNotNull(tile);
        assertSame(getBand("B_FLOAT32"), tile.getRasterDataNode());

        testTileStructure(tile, expectedRect, 0, TILE_SIZE, true);
        testOnlySamplesFloatAccessible(tile);

        int x0 = tile.getMinX();
        int y0 = tile.getMinY();

        testIndexOutOfBoundsException(tile, x0 - 1, y0);
        testIndexOutOfBoundsException(tile, x0, y0 - 1);
        testIndexOutOfBoundsException(tile, x0 + TILE_SIZE, y0);
        testIndexOutOfBoundsException(tile, x0, y0 + TILE_SIZE);

        testFloatSample32IO(tile, x0, y0);
        testFloatSample32IO(tile, x0 + TILE_SIZE / 2, y0);
        testFloatSample32IO(tile, x0, y0 + TILE_SIZE / 2);
        testFloatSample32IO(tile, x0 + TILE_SIZE / 2, y0 + TILE_SIZE / 2);

        testFloat32RawSampleIO(tile, x0, y0);
        testFloat32RawSampleIO(tile, x0 + TILE_SIZE / 2, y0);
        testFloat32RawSampleIO(tile, x0, y0 + TILE_SIZE / 2);
        testFloat32RawSampleIO(tile, x0 + TILE_SIZE / 2, y0 + TILE_SIZE / 2);
    }

    public void testSourceTileIsContainedInImageTile00() {
        final int CHILD_X = 2;
        final int CHILD_Y = 3;
        final int CHILD_W = 4;
        final int CHILD_H = 2;

        Rectangle expectedRect = new Rectangle(CHILD_X, CHILD_Y, CHILD_W, CHILD_H);
        Raster raster = getImageData(imageFLOAT32, expectedRect);
        TileImpl tile = new TileImpl(getBand("B_FLOAT32"), raster);
        assertSame(getBand("B_FLOAT32"), tile.getRasterDataNode());
        testOnlySamplesFloatAccessible(tile);

        testTileStructure(tile, expectedRect, CHILD_Y * TILE_SIZE + CHILD_X, TILE_SIZE, false);
    }

    public void testSourceTileIsNotContainedInAnyImageTile() {
        final int CHILD_X = 5;
        final int CHILD_Y = 3;
        final int CHILD_W = 4;
        final int CHILD_H = 7;

        Rectangle expectedRect = new Rectangle(CHILD_X, CHILD_Y, CHILD_W, CHILD_H);
        Raster raster = getImageData(imageFLOAT32, expectedRect);
        TileImpl tile = new TileImpl(getBand("B_FLOAT32"), raster);
        assertSame(getBand("B_FLOAT32"), tile.getRasterDataNode());
        testOnlySamplesFloatAccessible(tile);

        testTileStructure(tile, expectedRect, 0, CHILD_W, false);
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

}
