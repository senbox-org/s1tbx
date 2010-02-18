package org.esa.beam.jai;

import junit.framework.TestCase;

import javax.media.jai.PlanarImage;
import javax.media.jai.TiledImage;
import java.awt.*;
import java.awt.image.ColorModel;
import java.awt.image.DataBuffer;
import java.awt.image.PixelInterleavedSampleModel;
import java.io.File;

public class FileTileCacheTest extends TestCase {

    public void testGetImageId() {
        FileTileCache cache = new FileTileCache(new File("."));
        TiledImage image1 = createImage();
        TiledImage image2 = createImage();
        TiledImage image3 = createImage();

        String id1 = cache.getImageId(image1);
        String id2 = cache.getImageId(image2);
        String id3 = cache.getImageId(image3);

        assertNotNull(id1);
        assertNotNull(id2);
        assertNotNull(id3);

        assertTrue(!id1.equals(id2));
        assertTrue(!id1.equals(id3));
        assertTrue(!id2.equals(id3));

        assertSame(id1, cache.getImageId(image1));
        assertSame(id2, cache.getImageId(image2));
        assertSame(id3, cache.getImageId(image3));
    }

    public void testGetTileIndices() {
        TiledImage image = createImage();

        assertEquals(3, image.getNumXTiles());
        assertEquals(4, image.getNumYTiles());

        Point[] indices = FileTileCache.getTileIndices(image);
        assertNotNull(indices);

        Point[] expectedIndices = {
                new Point(0, 0), new Point(1, 0), new Point(2, 0),
                new Point(0, 1), new Point(1, 1), new Point(2, 1),
                new Point(0, 2), new Point(1, 2), new Point(2, 2),
                new Point(0, 3), new Point(1, 3), new Point(2, 3),
        };
        assertEquals(expectedIndices.length, indices.length);
        for (int i = 0; i < expectedIndices.length; i++) {
            assertEquals("i=" + i, expectedIndices[i], indices[i]);
        }
    }

    private static TiledImage createImage() {
        PixelInterleavedSampleModel sm = new PixelInterleavedSampleModel(DataBuffer.TYPE_USHORT, 120, 80, 1, 120, new int[]{0});
        ColorModel cm = PlanarImage.createColorModel(sm);
        TiledImage image = new TiledImage(0, 0, 256, 256, 0, 0, sm, cm);
        return image;
    }
}
