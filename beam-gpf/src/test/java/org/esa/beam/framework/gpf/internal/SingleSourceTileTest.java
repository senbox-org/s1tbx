package org.esa.beam.framework.gpf.internal;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.jai.SingleBandedSampleModel;

import java.awt.Point;
import java.awt.Rectangle;
import java.awt.image.DataBuffer;
import java.awt.image.WritableRaster;

public class SingleSourceTileTest extends TestCase {
    final int IMAGE_W = 5;
    final int IMAGE_H = 6;
    final int CHILD_X = 1;
    final int CHILD_Y = 2;
    final int CHILD_W = 2;
    final int CHILD_H = 2;
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

    public void testTileCorrespondsToFullRaster() {

        Band band = product.addBand("B_FLOAT32", ProductData.TYPE_FLOAT32);

        WritableRaster writableRaster = createSourceRaster();

        TileImpl tile = new TileImpl(band, writableRaster, new Rectangle(IMAGE_W, IMAGE_H), false);
        assertNotNull(tile);
        assertEquals(false, tile.isTarget());
        assertSame(band, tile.getRasterDataNode());
        assertEquals(new Rectangle(IMAGE_W, IMAGE_H), tile.getRectangle());
        assertEquals(0, tile.getMinX());
        assertEquals(0, tile.getMinY());
        assertEquals(IMAGE_W, tile.getWidth());
        assertEquals(IMAGE_H, tile.getHeight());
        assertEquals(0, tile.getScanlineOffset());
        assertEquals(IMAGE_W, tile.getScanlineStride());
        assertNull(tile.getRawSamplesByte());
        assertNull(tile.getRawSamplesShort());
        assertNull(tile.getRawSamplesInt());
        assertNotNull(tile.getRawSamplesFloat());
        assertNull(tile.getRawSamplesDouble());

        // test for initial sample values
        assertEquals(0.5, tile.getSampleDouble(0, 0), 1e-5);
        assertEquals(1.5, tile.getSampleDouble(0, 1), 1e-5);
        assertEquals(2.5, tile.getSampleDouble(0, 2), 1e-5);
        assertEquals(3.5, tile.getSampleDouble(0, 3), 1e-5);
        assertEquals(1.5, tile.getSampleDouble(1, 0), 1e-5);
        assertEquals(2.5, tile.getSampleDouble(2, 0), 1e-5);
        assertEquals(3.5, tile.getSampleDouble(3, 0), 1e-5);
        assertEquals(2.5, tile.getSampleDouble(1, 1), 1e-5);
        assertEquals(4.5, tile.getSampleDouble(2, 2), 1e-5);
        assertEquals(6.5, tile.getSampleDouble(3, 3), 1e-5);

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
        assertEquals(0.5, tile.getSampleDouble(0, 0), 1e-5);
        assertEquals(1.5, tile.getSampleDouble(0, 1), 1e-5);
        assertEquals(2.5, tile.getSampleDouble(0, 2), 1e-5);
        assertEquals(3.5, tile.getSampleDouble(0, 3), 1e-5);
        assertEquals(1.5, tile.getSampleDouble(1, 0), 1e-5);
        assertEquals(2.5, tile.getSampleDouble(2, 0), 1e-5);
        assertEquals(3.5, tile.getSampleDouble(3, 0), 1e-5);
        assertEquals(2.5, tile.getSampleDouble(1, 1), 1e-5);
        assertEquals(4.5, tile.getSampleDouble(2, 2), 1e-5);
        assertEquals(6.5, tile.getSampleDouble(3, 3), 1e-5);
    }

    public void testTileCorrespondsToChildRaster() {

        Band band = product.addBand("B_FLOAT32", ProductData.TYPE_FLOAT32);

        WritableRaster writableRaster = createSourceRaster();
        WritableRaster child = writableRaster.createWritableChild(CHILD_X, CHILD_Y, CHILD_W, CHILD_H, CHILD_X, CHILD_Y, null);

        TileImpl tile = new TileImpl(band, child, new Rectangle(CHILD_X, CHILD_Y, CHILD_W, CHILD_H), false);
        assertNotNull(tile);
        assertEquals(false, tile.isTarget());
        assertSame(band, tile.getRasterDataNode());
        assertEquals(new Rectangle(CHILD_X, CHILD_Y, CHILD_W, CHILD_H), tile.getRectangle());
        assertEquals(CHILD_X, tile.getMinX());
        assertEquals(CHILD_Y, tile.getMinY());
        assertEquals(CHILD_W, tile.getWidth());
        assertEquals(CHILD_H, tile.getHeight());
        assertEquals(CHILD_Y * IMAGE_W + CHILD_X, tile.getScanlineOffset());
        assertEquals(IMAGE_W, tile.getScanlineStride());
        assertNull(tile.getRawSamplesByte());
        assertNull(tile.getRawSamplesShort());
        assertNull(tile.getRawSamplesInt());
        assertNotNull(tile.getRawSamplesFloat());
        assertNull(tile.getRawSamplesDouble());

        // test for initial sample values
        assertEquals(3.5, tile.getSampleDouble(CHILD_X + 0, CHILD_Y + 0), 1e-5);
        assertEquals(4.5, tile.getSampleDouble(CHILD_X + 1, CHILD_Y + 0), 1e-5);
        assertEquals(4.5, tile.getSampleDouble(CHILD_X + 0, CHILD_Y + 1), 1e-5);
        assertEquals(5.5, tile.getSampleDouble(CHILD_X + 1, CHILD_Y + 1), 1e-5);

        // Test that setter still work
        tile.setSample(2, 1, 0.03);
        assertEquals(0.03, tile.getSampleDouble(2, 1), 1e-5);

        // Test that product data is wrapper for internal raster data buffer
        ProductData wrappedSampleData = tile.getRawSampleData();
        assertNotNull(wrappedSampleData);
        assertSame(wrappedSampleData, tile.getRawSampleData());

        ProductData newSampleData = band.createCompatibleRasterData();
        for (int i = 0; i < newSampleData.getNumElems(); i++) {
            newSampleData.setElemDoubleAt(i, 100.0 + i);
        }
        tile.setRawSampleData(newSampleData);

        // no change expected since this is a source tile
        assertEquals(3.5, tile.getSampleDouble(CHILD_X + 0, CHILD_Y + 0), 1e-5);
        assertEquals(4.5, tile.getSampleDouble(CHILD_X + 1, CHILD_Y + 0), 1e-5);
        assertEquals(4.5, tile.getSampleDouble(CHILD_X + 0, CHILD_Y + 1), 1e-5);
        assertEquals(5.5, tile.getSampleDouble(CHILD_X + 1, CHILD_Y + 1), 1e-5);
    }


    private WritableRaster createSourceRaster() {
        WritableRaster writableRaster = WritableRaster.createWritableRaster(new SingleBandedSampleModel(DataBuffer.TYPE_FLOAT, IMAGE_W, IMAGE_H), new Point());
        for (int y = 0; y < writableRaster.getHeight(); y++) {
            for (int x = 0; x < writableRaster.getWidth(); x++) {
                writableRaster.setSample(x, y, 0, x + y + 0.5);
            }
        }
        return writableRaster;
    }

}
