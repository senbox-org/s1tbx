package org.esa.beam.framework.gpf.internal;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.jai.RasterDataNodeOpImage;
import org.esa.beam.util.jai.SingleBandedSampleModel;

import javax.media.jai.PlanarImage;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;
import java.util.HashMap;

public class SingleSourceTileTest extends TestCase {
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

        WritableRaster writableRaster = WritableRaster.createWritableRaster(new SingleBandedSampleModel(DataBuffer.TYPE_FLOAT, IMAGE_W, IMAGE_H), new Point());
        for (int y = 0; y < writableRaster.getHeight(); y++) {
            for (int x = 0; x < writableRaster.getWidth(); x++) {
                writableRaster.setSample(x, y, 0, (x + 1.0) * (y + 1.0) + 0.5);
            }
        }

        TileImpl tile = new TileImpl(band, writableRaster, new Rectangle(IMAGE_W, IMAGE_H), false);
        assertNotNull(tile);
        assertEquals(false, tile.isTarget());
        assertSame(band, tile.getRasterDataNode());
        assertEquals(new Rectangle(IMAGE_W, IMAGE_H), tile.getRectangle());
        assertEquals(0, tile.getMinX());
        assertEquals(0, tile.getMinY());
        assertEquals(IMAGE_W, tile.getWidth());
        assertEquals(IMAGE_H, tile.getHeight());

        // test for initial sample values
        assertEquals(1.5, tile.getSampleDouble(0, 0), 1e-5);
        assertEquals(2.5, tile.getSampleDouble(0, 1), 1e-5);
        assertEquals(3.5, tile.getSampleDouble(0, 2), 1e-5);
        assertEquals(4.5, tile.getSampleDouble(0, 3), 1e-5);
        assertEquals(2.5, tile.getSampleDouble(1, 0), 1e-5);
        assertEquals(3.5, tile.getSampleDouble(2, 0), 1e-5);
        assertEquals(4.5, tile.getSampleDouble(3, 0), 1e-5);
        assertEquals(4.5, tile.getSampleDouble(1, 1), 1e-5);
        assertEquals(9.5, tile.getSampleDouble(2, 2), 1e-5);
        assertEquals(16.5, tile.getSampleDouble(3, 3), 1e-5);

        // Test that setter still work
        tile.setSample(2, 1, 0.03);
        assertEquals(0.03, tile.getSampleDouble(2, 1), 1e-5);

        // Test that product data is wrapper for internal raster data buffer
        ProductData wrappedSampleData = tile.getRawSampleData();
        assertNotNull(wrappedSampleData);
        assertSame(wrappedSampleData, tile.getRawSampleData());

        wrappedSampleData.setElemDoubleAt(1 + 2 * IMAGE_W, 0.04);
        assertEquals(0.04, tile.getSampleDouble(1, 2), 1e-5);

        ProductData newSampleData = band.createCompatibleRasterData();
        for (int i = 0; i < newSampleData.getNumElems(); i++) {
            newSampleData.setElemDoubleAt(i, 100.0 + i);
        }
        tile.setRawSampleData(newSampleData);

        // no change expected since this is a source tile
        assertEquals(1.5, tile.getSampleDouble(0, 0), 1e-5);
        assertEquals(2.5, tile.getSampleDouble(0, 1), 1e-5);
        assertEquals(3.5, tile.getSampleDouble(0, 2), 1e-5);
        assertEquals(4.5, tile.getSampleDouble(0, 3), 1e-5);
        assertEquals(2.5, tile.getSampleDouble(1, 0), 1e-5);
        assertEquals(3.5, tile.getSampleDouble(2, 0), 1e-5);
        assertEquals(4.5, tile.getSampleDouble(3, 0), 1e-5);
        assertEquals(4.5, tile.getSampleDouble(1, 1), 1e-5);
        assertEquals(9.5, tile.getSampleDouble(2, 2), 1e-5);
        assertEquals(16.5, tile.getSampleDouble(3, 3), 1e-5);
    }

    static class TestOpImage extends RasterDataNodeOpImage {

        private HashMap<Rectangle, TileImpl> gpfTiles = new HashMap<Rectangle, TileImpl>(4);

        public TestOpImage(Band band) {
            super(band);
        }

        public HashMap<Rectangle, TileImpl> getGpfTiles() {
            return gpfTiles;
        }

        @Override
        protected void computeRect(PlanarImage[] planarImages, WritableRaster writableRaster, Rectangle rectangle) {
            gpfTiles.put(rectangle, new TileImpl(getRasterDataNode(), writableRaster, rectangle));
            for (int y = rectangle.y; y < rectangle.y + rectangle.height; y++) {
                for (int x = rectangle.x; x < rectangle.x + rectangle.width; x++) {
                    writableRaster.setSample(x, y, 0, (x + 1.0) * (y + 1.0) + 0.5);
                }
            }
        }
    }
}
