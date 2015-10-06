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

import java.awt.Point;
import java.awt.Rectangle;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * A writer that accumulates data until all data for a chunk / tile is available.
 * Then this data chunk is written at once.
 *
 * @author MarcoZ
 */
public abstract class ChunkWriter {
    private final int sceneWidth;
    private final int sceneHeight;
    private final int chunkWidth;
    private final int chunkHeight;
    private final boolean yFlipped;
    private final int numChunksX;
    private final int numChunksY;
    private final Map<Point, Chunk> activeChunks;

    public ChunkWriter(int sceneWidth, int sceneHeight, int chunkWidth, int chunkHeight, boolean yFlipped) {
        this.sceneWidth = sceneWidth;
        this.sceneHeight = sceneHeight;
        this.chunkWidth = chunkWidth;
        this.chunkHeight = chunkHeight;
        this.yFlipped = yFlipped;
        this.numChunksX = (int) Math.ceil(sceneWidth / (double) chunkWidth);
        this.numChunksY = (int) Math.ceil(sceneHeight / (double) chunkHeight);
        this.activeChunks = new HashMap<Point, Chunk>();
    }

    public void write(int x, int y, int width, int height, ProductData data) throws IOException {
        if (yFlipped) {
            if (height != 1) {
                ProductData flippedData = ProductData.createInstance(data.getType(), data.getNumElems());
                for (int line = 0; line < height; line++) {
                    int flippedLine = (height - 1) - line;
                    int srcPos = line * width;
                    int destPos = flippedLine * width;
                    System.arraycopy(data.getElems(), srcPos, flippedData.getElems(), destPos, width);
                }
                data = flippedData;
            }
            y = sceneHeight - (y + height);
        }
        Point[] chunkIndices = getChunkIndices(x, y, width, height);
        Rectangle dataRect = new Rectangle(x, y, width, height);
        for (Point chunkIndex : chunkIndices) {
            Rectangle chunkRect = getChunkRect(chunkIndex);
            if (chunkRect.equals(dataRect)) {
                writeChunk(chunkRect, data);
            } else {
                Chunk chunk = activeChunks.get(chunkIndex);
                if (chunk == null) {
                    chunk = new Chunk(chunkRect, data.getType());
                    activeChunks.put(chunkIndex, chunk);
                }
                chunk.copyDataFrom(dataRect, data);
                if (chunk.complete()) {
                    writeChunk(chunkRect, chunk.getData());
                    activeChunks.remove(chunkIndex);
                }
            }
        }
    }

    Point[] getChunkIndices(int x, int y, int width, int height) {
        int minTileX = getChunkX(x);
        int maxTileX = getChunkX(x + width - 1);
        int minTileY = getChunkY(y);
        int maxTileY = getChunkY(y + height - 1);

        Point[] tileIndices =
                new Point[(maxTileY - minTileY + 1) * (maxTileX - minTileX + 1)];

        int tileIndexOffset = 0;
        for (int ty = minTileY; ty <= maxTileY; ty++) {
            for (int tx = minTileX; tx <= maxTileX; tx++) {
                tileIndices[tileIndexOffset++] = new Point(tx, ty);
            }
        }
        return tileIndices;
    }

    int getChunkX(int x) {
        return x / chunkWidth;
    }

    int getChunkY(int y) {
        return y / chunkHeight;
    }

    Rectangle getChunkRect(Point chunkIndex) {
        return new Rectangle(sceneWidth, sceneHeight).intersection(new Rectangle(
                chunkIndex.x * chunkWidth, chunkIndex.y * chunkHeight,
                chunkWidth, chunkHeight));
    }

    public int getNumChunksX() {
        return numChunksX;
    }

    public int getNumChunksY() {
        return numChunksY;
    }

    public abstract void writeChunk(Rectangle rect, ProductData data) throws IOException;
}
