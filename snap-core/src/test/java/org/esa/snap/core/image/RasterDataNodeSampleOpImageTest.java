package org.esa.snap.core.image;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.junit.Before;
import org.junit.Test;

import java.awt.Rectangle;
import java.awt.image.Raster;

import static org.junit.Assert.*;

/**
 * @author Norman Fomferra
 */
public class RasterDataNodeSampleOpImageTest {
    private static final int Y_FACTOR = 1000;
    private RasterDataNodeSampleOpImage im0;
    private RasterDataNodeSampleOpImage im4;

    @Before
    public void setUp() throws Exception {
        Band b = new Band("b", ProductData.TYPE_FLOAT32, 1600, 800);

        im0 = new MyRasterDataNodeSampleOpImage(b, 0, 1.0);
        assertEquals(1600, im0.getWidth());
        assertEquals(800, im0.getHeight());
        assertEquals(5, im0.getNumXTiles());
        assertEquals(2, im0.getNumYTiles());

        im4 = new MyRasterDataNodeSampleOpImage(b, 4, 10.0);
        assertEquals(160, im4.getWidth());
        assertEquals(80, im4.getHeight());
        assertEquals(1, im4.getNumXTiles());
        assertEquals(1, im4.getNumYTiles());
    }

    @Test
    public void testItOnLevel0Tile00() throws Exception {
        Raster data = im0.getData(new Rectangle(0, 0, 100, 100));
        assertEquals(0, data.getSample(0, 0, 0));
        assertEquals(99, data.getSample(99, 0, 0));
        assertEquals(99 * Y_FACTOR, data.getSample(0, 99, 0));
        assertEquals(99 * Y_FACTOR + 99, data.getSample(99, 99, 0));
    }

    @Test
    public void testItOnLevel0TileNN() throws Exception {
        Raster data = im0.getData(new Rectangle(1500, 700, 100, 100));
        assertEquals(700 * Y_FACTOR + 1500, data.getSample(1500, 700, 0));
        assertEquals(700 * Y_FACTOR + 1500 + 99, data.getSample(1500 + 99, 700, 0));
        assertEquals((700 + 99) * Y_FACTOR + 1500, data.getSample(1500, 700 + 99, 0));
        assertEquals((700 + 99) * Y_FACTOR + 1500 + 99, data.getSample(1500 + 99, 700 + 99, 0));
    }

    @Test
    public void testItOnLevel4() throws Exception {
        Raster data = im4.getData();
        assertEquals(0, data.getSample(0, 0, 0));
        assertEquals(100, data.getSample(100 / 10, 0, 0));
        assertEquals(100 * Y_FACTOR, data.getSample(0, 100 / 10, 0));
        assertEquals(100 * Y_FACTOR + 100, data.getSample(100 / 10, 100 / 10, 0));
    }

    private static class MyRasterDataNodeSampleOpImage extends RasterDataNodeSampleOpImage {

        public MyRasterDataNodeSampleOpImage(Band b, int level, double scale) {
            super(b, new ResolutionLevel(level, scale));
        }

        @Override
        protected double computeSample(int x, int y) {
            return y * Y_FACTOR + x;
        }
    }
}
