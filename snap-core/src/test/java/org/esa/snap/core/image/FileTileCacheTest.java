/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.core.image;

import junit.framework.TestCase;

import javax.media.jai.PlanarImage;
import javax.media.jai.TiledImage;
import java.awt.Point;
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
