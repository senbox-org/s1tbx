package org.esa.beam.framework.gpf.internal;

import junit.framework.TestCase;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.util.HashMap;

public abstract class AbstractTileImageTileTest extends TestCase {

    private Product product;
    private HashMap<String, TestOpImage> imageMap;

    @Override
    protected void setUp() throws Exception {
        Dimension imageSize = getImageSize();
        Dimension tileSize = getTileSize();
        product = new Product("N", "T", imageSize.width, imageSize.height);
        product.setPreferredTileSize(tileSize.width, tileSize.height);
        product.addBand("B_INT8", ProductData.TYPE_INT8);
        product.addBand("B_UINT8", ProductData.TYPE_UINT8);
        product.addBand("B_INT16", ProductData.TYPE_INT8);
        product.addBand("B_UINT16", ProductData.TYPE_UINT8);
        product.addBand("B_INT32", ProductData.TYPE_INT8);
        product.addBand("B_UINT32", ProductData.TYPE_UINT8);
        product.addBand("B_FLOAT32", ProductData.TYPE_FLOAT32);
        product.addBand("B_FLOAT64", ProductData.TYPE_FLOAT64);
        imageMap = new HashMap<String, TestOpImage>();
    }

    @Override
    protected void tearDown() throws Exception {
        for (TestOpImage testOpImage : imageMap.values()) {
            testOpImage.dispose();
        }
        imageMap.clear();
        product.dispose();
        product = null;
    }

    public abstract Dimension getImageSize();

    public abstract Dimension getTileSize();

    public Product getProduct() {
        return product;
    }

    public Band getBand(String name) {
        return product.getBand(name);
    }

    public TestOpImage getImage(String name) {
        Band band = getBand(name);
        assertNotNull(band);
        TestOpImage image = imageMap.get(name);
        if (image == null) {
            image = new TestOpImage(band);
            image.getTiles(); // Forces JAI tile computation.
            imageMap.put(name, image);

            Dimension tileSize = getTileSize();
            assertEquals(tileSize.width, image.getSampleModel().getWidth());
            assertEquals(tileSize.height, image.getSampleModel().getHeight());
        }
        return image;
    }

    protected void testTileStructure(TileImpl tile,
                                     Rectangle expectedRect,
                                     int expectedScanlineOffset,
                                     int expectedScanlineStride,
                                     boolean expectedTarget) {
        assertEquals(expectedRect, tile.getRectangle());
        assertEquals(expectedRect.x, tile.getMinX());
        assertEquals(expectedRect.y, tile.getMinY());
        assertEquals(expectedRect.x + expectedRect.width - 1, tile.getMaxX());
        assertEquals(expectedRect.y + expectedRect.height - 1, tile.getMaxY());
        assertEquals(expectedRect.width, tile.getWidth());
        assertEquals(expectedRect.height, tile.getHeight());
        assertEquals(expectedScanlineStride, tile.getScanlineStride());
        assertEquals(expectedScanlineOffset, tile.getScanlineOffset());
        if (expectedTarget) {
            assertEquals(true, tile.isWritable());
        }
    }

    protected Raster getImageData(TestOpImage image, Rectangle expectedRect) {
        Raster raster = image.getData(expectedRect);
        assertEquals(expectedRect.x, raster.getMinX());
        assertEquals(expectedRect.y, raster.getMinY());
        assertEquals(expectedRect.width, raster.getWidth());
        assertEquals(expectedRect.height, raster.getHeight());
        return raster;
    }

    protected void testOnlySamplesFloatAccessible(TileImpl tile) {
        assertNull(tile.getRawSamplesByte());
        assertNull(tile.getRawSamplesShort());
        assertNull(tile.getRawSamplesInt());
        assertNotNull(tile.getRawSamplesFloat());
        assertNull(tile.getRawSamplesDouble());
    }

    protected void testSampleFloatIO(TileImpl tile, int x0, int y0) {
        tile.setSample(x0, y0, 0.23f);
        assertEquals(0.23f, tile.getSampleFloat(x0, y0), 1e-5);
    }


    protected void testRawSamplesFloatIO(TileImpl tile, int x, int y) {
        float[] samplesFloat = tile.getRawSamplesFloat();
        assertNotNull(samplesFloat);

        float oldXYSample = tile.getSampleFloat(x, y);

        int lineOffset = tile.getScanlineOffset();
        int lineStride = tile.getScanlineStride();
        int index = lineOffset + (y - tile.getMinY()) * lineStride + (x - tile.getMinX());
        assertTrue(index >= 0);
        assertTrue(index < samplesFloat.length);

        float oldIndexSample = samplesFloat[index];
        assertEquals(oldXYSample, oldIndexSample, 1e-5f);

        float newIndexSample = 1234.56f;
        assertTrue(Math.abs(oldIndexSample - newIndexSample) > 0.01f);

        samplesFloat[index] = newIndexSample;

        float newXYSample = tile.getSampleFloat(x, y);

        assertEquals(newIndexSample, newXYSample, 1e-5f);
    }

}
