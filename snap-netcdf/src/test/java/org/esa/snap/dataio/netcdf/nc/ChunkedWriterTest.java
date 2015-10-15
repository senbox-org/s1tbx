/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.netcdf.nc;


import org.esa.snap.core.datamodel.ProductData;
import org.junit.Test;

import java.awt.Point;
import java.awt.Rectangle;

import static org.junit.Assert.*;

public class ChunkedWriterTest {

    @Test
    public void testDimensions() throws Exception {
        DummyChunkWriter chunkWriter = new DummyChunkWriter(100, 100, 100, 100, false);
        assertEquals(1, chunkWriter.getNumChunksX());
        assertEquals(1, chunkWriter.getNumChunksY());

        chunkWriter = new DummyChunkWriter(100, 100, 50, 50, false);
        assertEquals(2, chunkWriter.getNumChunksX());
        assertEquals(2, chunkWriter.getNumChunksY());

        chunkWriter = new DummyChunkWriter(100, 100, 30, 30, false);
        assertEquals(4, chunkWriter.getNumChunksX());
        assertEquals(4, chunkWriter.getNumChunksY());
    }

    @Test
    public void testGetChunkX() throws Exception {
        ChunkWriter chunkWriter = new DummyChunkWriter(100, 100, 30, 30, false);
        assertEquals(0, chunkWriter.getChunkX(0));
        assertEquals(0, chunkWriter.getChunkX(1));
        assertEquals(0, chunkWriter.getChunkX(29));

        assertEquals(1, chunkWriter.getChunkX(30));
        assertEquals(1, chunkWriter.getChunkX(31));
        assertEquals(1, chunkWriter.getChunkX(59));

        assertEquals(2, chunkWriter.getChunkX(60));
        assertEquals(2, chunkWriter.getChunkX(61));
        assertEquals(2, chunkWriter.getChunkX(89));

        assertEquals(3, chunkWriter.getChunkX(90));
        assertEquals(3, chunkWriter.getChunkX(91));
        assertEquals(3, chunkWriter.getChunkX(99));
    }

    @Test
    public void testGetChunkY() throws Exception {
        ChunkWriter chunkWriter = new DummyChunkWriter(100, 100, 30, 40, false);
        assertEquals(0, chunkWriter.getChunkY(0));
        assertEquals(0, chunkWriter.getChunkY(1));
        assertEquals(0, chunkWriter.getChunkY(39));

        assertEquals(1, chunkWriter.getChunkY(40));
        assertEquals(1, chunkWriter.getChunkY(41));
        assertEquals(1, chunkWriter.getChunkY(79));

        assertEquals(2, chunkWriter.getChunkY(80));
        assertEquals(2, chunkWriter.getChunkY(81));
        assertEquals(2, chunkWriter.getChunkY(99));
    }

    @Test
    public void testGetChunkRect() throws Exception {
        ChunkWriter chunkWriter = new DummyChunkWriter(60, 50, 30, 40, false);
        assertEquals(new Rectangle(0, 0, 30, 40), chunkWriter.getChunkRect(new Point(0, 0)));
        assertEquals(new Rectangle(30, 0, 30, 40), chunkWriter.getChunkRect(new Point(1, 0)));
        assertEquals(new Rectangle(0, 40, 30, 10), chunkWriter.getChunkRect(new Point(0, 1)));
        assertEquals(new Rectangle(30, 40, 30, 10), chunkWriter.getChunkRect(new Point(1, 1)));
    }

    @Test
    public void testGetChunkIndices_OneChunk() throws Exception {
        DummyChunkWriter chunkWriter = new DummyChunkWriter(100, 100, 100, 100, false);
        Point[] chunks = chunkWriter.getChunkIndices(0, 0, 100, 100);
        assertNotNull(chunks);
        assertEquals(1, chunks.length);
        assertEquals(0, chunks[0].x);
        assertEquals(0, chunks[0].y);
        chunks = chunkWriter.getChunkIndices(10, 10, 10, 10);
        assertNotNull(chunks);
        assertEquals(1, chunks.length);
        assertEquals(0, chunks[0].x);
        assertEquals(0, chunks[0].y);
    }

    @Test
    public void testGetChunkIndices_FourChunks() throws Exception {
        DummyChunkWriter chunkWriter = new DummyChunkWriter(100, 100, 50, 50, false);
        Point[] chunks = chunkWriter.getChunkIndices(0, 0, 100, 100);
        assertNotNull(chunks);
        assertEquals(4, chunks.length);
        assertEquals(0, chunks[0].x);
        assertEquals(0, chunks[0].y);
        assertEquals(1, chunks[1].x);
        assertEquals(0, chunks[1].y);
        assertEquals(0, chunks[2].x);
        assertEquals(1, chunks[2].y);
        assertEquals(1, chunks[3].x);
        assertEquals(1, chunks[3].y);

        chunks = chunkWriter.getChunkIndices(10, 10, 10, 10);
        assertNotNull(chunks);
        assertEquals(1, chunks.length);
        assertEquals(0, chunks[0].x);
        assertEquals(0, chunks[0].y);

        chunks = chunkWriter.getChunkIndices(0, 0, 100, 10);
        assertNotNull(chunks);
        assertEquals(2, chunks.length);
        assertEquals(0, chunks[0].x);
        assertEquals(0, chunks[0].y);
        assertEquals(1, chunks[1].x);
        assertEquals(0, chunks[1].y);
    }

    @Test
    public void testWrite_OneChunk() throws Exception {
        DummyChunkWriter chunkWriter = new DummyChunkWriter(4, 4, 4, 4, false);
        assertNull(chunkWriter.writtenChunks[0][0]);
        chunkWriter.write(0, 0, 1, 1, ProductData.createInstance(new int[]{0}));
        chunkWriter.write(1, 0, 1, 1, ProductData.createInstance(new int[]{1}));
        chunkWriter.write(0, 1, 1, 1, ProductData.createInstance(new int[]{4}));
        chunkWriter.write(1, 1, 1, 1, ProductData.createInstance(new int[]{5}));
        chunkWriter.write(2, 0, 2, 2, ProductData.createInstance(new int[]{2, 3, 6, 7}));
        chunkWriter.write(0, 2, 4, 2, ProductData.createInstance(new int[]{8, 9, 10, 11, 12, 13, 14, 15}));
        assertNotNull(chunkWriter.writtenChunks[0][0]);
        int[] expected = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        assertArrayEquals(expected, (int[])chunkWriter.writtenChunks[0][0].getElems());
    }

    @Test
    public void testWrite_OneChunk_YFlipped() throws Exception {
        DummyChunkWriter chunkWriter = new DummyChunkWriter(4, 4, 4, 4, true);
        assertNull(chunkWriter.writtenChunks[0][0]);
        chunkWriter.write(0, 0, 1, 1, ProductData.createInstance(new int[]{0}));
        chunkWriter.write(1, 0, 1, 1, ProductData.createInstance(new int[]{1}));
        chunkWriter.write(0, 1, 1, 1, ProductData.createInstance(new int[]{4}));
        chunkWriter.write(1, 1, 1, 1, ProductData.createInstance(new int[]{5}));
        chunkWriter.write(2, 0, 2, 2, ProductData.createInstance(new int[]{2, 3, 6, 7}));
        chunkWriter.write(0, 2, 4, 2, ProductData.createInstance(new int[]{8, 9, 10, 11, 12, 13, 14, 15}));
        assertNotNull(chunkWriter.writtenChunks[0][0]);
        int[] expected = {12, 13, 14, 15, 8, 9, 10, 11, 4, 5, 6, 7, 0, 1, 2, 3};
        assertArrayEquals(expected, (int[])chunkWriter.writtenChunks[0][0].getElems());
    }

    @Test
    public void testWrite_FourChunk_AllAtOnce() throws Exception {
        DummyChunkWriter chunkWriter = new DummyChunkWriter(4, 4, 2, 2, false);
        assertNull(chunkWriter.writtenChunks[0][0]);
        assertNull(chunkWriter.writtenChunks[1][0]);
        assertNull(chunkWriter.writtenChunks[0][1]);
        assertNull(chunkWriter.writtenChunks[1][1]);

        int[] data = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15};
        chunkWriter.write(0, 0, 4, 4, ProductData.createInstance(data));
        assertNotNull(chunkWriter.writtenChunks[0][0]);
        assertNotNull(chunkWriter.writtenChunks[1][0]);
        assertNotNull(chunkWriter.writtenChunks[0][1]);
        assertNotNull(chunkWriter.writtenChunks[1][1]);
        assertArrayEquals(new int[]{0, 1, 4, 5}, (int[])chunkWriter.writtenChunks[0][0].getElems());
        assertArrayEquals(new int[]{2, 3, 6, 7}, (int[]) chunkWriter.writtenChunks[1][0].getElems());
        assertArrayEquals(new int[]{8, 9, 12, 13}, (int[]) chunkWriter.writtenChunks[0][1].getElems());
        assertArrayEquals(new int[]{10, 11, 14, 15}, (int[]) chunkWriter.writtenChunks[1][1].getElems());
    }

    @Test
    public void testWrite_FourChunk_FittingWrites() throws Exception {
        DummyChunkWriter chunkWriter = new DummyChunkWriter(4, 4, 2, 2, false);
        assertNull(chunkWriter.writtenChunks[0][0]);
        assertNull(chunkWriter.writtenChunks[1][0]);
        assertNull(chunkWriter.writtenChunks[0][1]);
        assertNull(chunkWriter.writtenChunks[1][1]);

        chunkWriter.write(0, 0, 2, 2, ProductData.createInstance(new int[]{0, 1, 4, 5}));
        assertNotNull(chunkWriter.writtenChunks[0][0]);
        assertNull(chunkWriter.writtenChunks[1][0]);
        assertNull(chunkWriter.writtenChunks[0][1]);
        assertNull(chunkWriter.writtenChunks[1][1]);
        assertArrayEquals(new int[]{0, 1, 4, 5}, (int[])chunkWriter.writtenChunks[0][0].getElems());

        chunkWriter.write(2, 0, 2, 2, ProductData.createInstance(new int[]{2, 3, 6, 7}));
        assertNotNull(chunkWriter.writtenChunks[0][0]);
        assertNotNull(chunkWriter.writtenChunks[1][0]);
        assertNull(chunkWriter.writtenChunks[0][1]);
        assertNull(chunkWriter.writtenChunks[1][1]);
        assertArrayEquals(new int[]{2, 3, 6, 7}, (int[]) chunkWriter.writtenChunks[1][0].getElems());

        chunkWriter.write(0, 2, 2, 2, ProductData.createInstance(new int[]{8, 9, 12, 13}));
        assertNotNull(chunkWriter.writtenChunks[0][0]);
        assertNotNull(chunkWriter.writtenChunks[1][0]);
        assertNotNull(chunkWriter.writtenChunks[0][1]);
        assertNull(chunkWriter.writtenChunks[1][1]);
        assertArrayEquals(new int[]{8, 9, 12, 13}, (int[]) chunkWriter.writtenChunks[0][1].getElems());

        chunkWriter.write(2, 2, 2, 2, ProductData.createInstance(new int[]{10, 11, 14, 15}));
        assertNotNull(chunkWriter.writtenChunks[0][0]);
        assertNotNull(chunkWriter.writtenChunks[1][0]);
        assertNotNull(chunkWriter.writtenChunks[0][1]);
        assertNotNull(chunkWriter.writtenChunks[1][1]);
        assertArrayEquals(new int[]{10, 11, 14, 15}, (int[]) chunkWriter.writtenChunks[1][1].getElems());
    }

        @Test
    public void testWrite_FourChunk_FittingWrites_YFlipped() throws Exception {
        DummyChunkWriter chunkWriter = new DummyChunkWriter(4, 4, 2, 2, true);
        assertNull(chunkWriter.writtenChunks[0][0]);
        assertNull(chunkWriter.writtenChunks[1][0]);
        assertNull(chunkWriter.writtenChunks[0][1]);
        assertNull(chunkWriter.writtenChunks[1][1]);

        chunkWriter.write(0, 0, 2, 2, ProductData.createInstance(new int[]{0, 1, 4, 5}));
        assertNull(chunkWriter.writtenChunks[0][0]);
        assertNull(chunkWriter.writtenChunks[1][0]);
        assertNotNull(chunkWriter.writtenChunks[0][1]);
        assertNull(chunkWriter.writtenChunks[1][1]);
        assertArrayEquals(new int[]{4, 5, 0, 1}, (int[])chunkWriter.writtenChunks[0][1].getElems());

        chunkWriter.write(2, 0, 2, 2, ProductData.createInstance(new int[]{2, 3, 6, 7}));
        assertNull(chunkWriter.writtenChunks[0][0]);
        assertNull(chunkWriter.writtenChunks[1][0]);
        assertNotNull(chunkWriter.writtenChunks[0][1]);
        assertNotNull(chunkWriter.writtenChunks[1][1]);
        assertArrayEquals(new int[]{6, 7, 2, 3}, (int[]) chunkWriter.writtenChunks[1][1].getElems());

        chunkWriter.write(0, 2, 2, 2, ProductData.createInstance(new int[]{8, 9, 12, 13}));
        assertNotNull(chunkWriter.writtenChunks[0][0]);
        assertNull(chunkWriter.writtenChunks[1][0]);
        assertNotNull(chunkWriter.writtenChunks[0][1]);
        assertNotNull(chunkWriter.writtenChunks[1][1]);
        assertArrayEquals(new int[]{12, 13, 8, 9}, (int[]) chunkWriter.writtenChunks[0][0].getElems());

        chunkWriter.write(2, 2, 2, 2, ProductData.createInstance(new int[]{10, 11, 14, 15}));
        assertNotNull(chunkWriter.writtenChunks[0][0]);
        assertNotNull(chunkWriter.writtenChunks[1][0]);
        assertNotNull(chunkWriter.writtenChunks[0][1]);
        assertNotNull(chunkWriter.writtenChunks[1][1]);
        assertArrayEquals(new int[]{14, 15, 10, 11}, (int[]) chunkWriter.writtenChunks[1][0].getElems());
    }

    @Test
    public void testWrite_FourChunk_LineOriented() throws Exception {
        DummyChunkWriter chunkWriter = new DummyChunkWriter(4, 4, 2, 2, false);
        assertNull(chunkWriter.writtenChunks[0][0]);
        assertNull(chunkWriter.writtenChunks[1][0]);
        assertNull(chunkWriter.writtenChunks[0][1]);
        assertNull(chunkWriter.writtenChunks[1][1]);

        chunkWriter.write(0, 0, 4, 1, ProductData.createInstance(new int[]{0, 1, 2, 3}));
        assertNull(chunkWriter.writtenChunks[0][0]);
        assertNull(chunkWriter.writtenChunks[1][0]);
        assertNull(chunkWriter.writtenChunks[0][1]);
        assertNull(chunkWriter.writtenChunks[1][1]);

        chunkWriter.write(0, 1, 4, 1, ProductData.createInstance(new int[]{4, 5, 6, 7}));
        assertNotNull(chunkWriter.writtenChunks[0][0]);
        assertNotNull(chunkWriter.writtenChunks[1][0]);
        assertNull(chunkWriter.writtenChunks[0][1]);
        assertNull(chunkWriter.writtenChunks[1][1]);

        assertArrayEquals(new int[]{0, 1, 4, 5}, (int[])chunkWriter.writtenChunks[0][0].getElems());
        assertArrayEquals(new int[]{2, 3, 6, 7}, (int[]) chunkWriter.writtenChunks[1][0].getElems());

        chunkWriter.write(0, 2, 4, 1, ProductData.createInstance(new int[]{8, 9, 10, 11}));
        assertNotNull(chunkWriter.writtenChunks[0][0]);
        assertNotNull(chunkWriter.writtenChunks[1][0]);
        assertNull(chunkWriter.writtenChunks[0][1]);
        assertNull(chunkWriter.writtenChunks[1][1]);

        chunkWriter.write(0, 3, 4, 1, ProductData.createInstance(new int[]{12, 13, 14, 15}));
        assertNotNull(chunkWriter.writtenChunks[0][0]);
        assertNotNull(chunkWriter.writtenChunks[1][0]);
        assertNotNull(chunkWriter.writtenChunks[0][1]);
        assertNotNull(chunkWriter.writtenChunks[1][1]);
        assertArrayEquals(new int[]{8, 9, 12, 13}, (int[]) chunkWriter.writtenChunks[0][1].getElems());
        assertArrayEquals(new int[]{10, 11, 14, 15}, (int[]) chunkWriter.writtenChunks[1][1].getElems());
    }

        @Test
    public void testWrite_FourChunk_LineOriented_YFlipped() throws Exception {
        DummyChunkWriter chunkWriter = new DummyChunkWriter(4, 4, 2, 2, true);
        assertNull(chunkWriter.writtenChunks[0][0]);
        assertNull(chunkWriter.writtenChunks[1][0]);
        assertNull(chunkWriter.writtenChunks[0][1]);
        assertNull(chunkWriter.writtenChunks[1][1]);

        chunkWriter.write(0, 0, 4, 1, ProductData.createInstance(new int[]{0, 1, 2, 3}));
        assertNull(chunkWriter.writtenChunks[0][0]);
        assertNull(chunkWriter.writtenChunks[1][0]);
        assertNull(chunkWriter.writtenChunks[0][1]);
        assertNull(chunkWriter.writtenChunks[1][1]);

        chunkWriter.write(0, 1, 4, 1, ProductData.createInstance(new int[]{4, 5, 6, 7}));
        assertNull(chunkWriter.writtenChunks[0][0]);
        assertNull(chunkWriter.writtenChunks[1][0]);
        assertNotNull(chunkWriter.writtenChunks[0][1]);
        assertNotNull(chunkWriter.writtenChunks[1][1]);

        assertArrayEquals(new int[]{4, 5, 0, 1}, (int[])chunkWriter.writtenChunks[0][1].getElems());
        assertArrayEquals(new int[]{6, 7, 2, 3}, (int[]) chunkWriter.writtenChunks[1][1].getElems());

        chunkWriter.write(0, 2, 4, 1, ProductData.createInstance(new int[]{8, 9, 10, 11}));
        assertNull(chunkWriter.writtenChunks[0][0]);
        assertNull(chunkWriter.writtenChunks[1][0]);
        assertNotNull(chunkWriter.writtenChunks[0][1]);
        assertNotNull(chunkWriter.writtenChunks[1][1]);

        chunkWriter.write(0, 3, 4, 1, ProductData.createInstance(new int[]{12, 13, 14, 15}));
        assertNotNull(chunkWriter.writtenChunks[0][0]);
        assertNotNull(chunkWriter.writtenChunks[1][0]);
        assertNotNull(chunkWriter.writtenChunks[0][1]);
        assertNotNull(chunkWriter.writtenChunks[1][1]);
        assertArrayEquals(new int[]{12, 13, 8, 9}, (int[]) chunkWriter.writtenChunks[0][0].getElems());
        assertArrayEquals(new int[]{14, 15, 10, 11}, (int[]) chunkWriter.writtenChunks[1][0].getElems());
    }

    private class DummyChunkWriter extends ChunkWriter {

        private final ProductData[][] writtenChunks;

        public DummyChunkWriter(int sceneWidth, int sceneHeight, int chunkWidth, int chunkHeight, boolean isYFlipped) {
            super(sceneWidth, sceneHeight, chunkWidth, chunkHeight, isYFlipped);
            this.writtenChunks = new ProductData[chunkWidth][chunkHeight];
        }

        @Override
        public void writeChunk(Rectangle rect, ProductData data) {
            writtenChunks[getChunkX(rect.x)][getChunkY(rect.y)] = data;
        }
    }
}
