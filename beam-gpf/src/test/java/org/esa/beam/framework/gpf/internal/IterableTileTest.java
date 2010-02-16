package org.esa.beam.framework.gpf.internal;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.Tile;

import java.awt.Point;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;

public class IterableTileTest extends TestCase {

    public void testNumberOfLoops() {
        int numLoops = 0;
        TileImpl tile = createIterableTile(0, 0, 512, 384);
        for (Tile.Pos pos : tile) {
            numLoops++;
        }
        assertEquals(512 * 384, numLoops);
    }

    public void testStateAfterLoop() {
        TileImpl tile = createIterableTile(0, 0, 512, 384);
        Tile.Pos lastPos = null;
        for (Tile.Pos pos : tile) {
            lastPos = pos;
        }
        assertNotNull(lastPos);
        assertEquals(511, lastPos.x);
        assertEquals(383, lastPos.y);
    }

    static TileImpl createIterableTile(int x0, int y0, int w, int h) {
        Band band = new Band("x", ProductData.TYPE_INT32, w, h);
        WritableRaster raster = WritableRaster.createBandedRaster(DataBuffer.TYPE_INT,
                                                                  w, h, 1, new Point(x0, y0));
        return new TileImpl(band, raster);
    }

}
