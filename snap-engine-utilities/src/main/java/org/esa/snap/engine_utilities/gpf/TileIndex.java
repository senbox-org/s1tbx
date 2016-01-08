/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.gpf;

import org.esa.snap.core.gpf.Tile;

/**
 * calculates the index into a tile
 */
public final class TileIndex {

    private final int tileOffset;
    private final int tileStride;
    private final int tileMinX;
    private final int tileMinY;

    private int offset = 0;

    public TileIndex(final Tile tile) {
        tileOffset = tile.getScanlineOffset();
        tileStride = tile.getScanlineStride();
        tileMinX = tile.getMinX();
        tileMinY = tile.getMinY();
    }

    /**
     * calculates offset
     *
     * @param ty y pos
     * @return offset
     */
    public int calculateStride(final int ty) {
        offset = tileMinX - (((ty - tileMinY) * tileStride) + tileOffset);
        return offset;
    }

    public int getOffset() {
        return offset;
    }

    public int getIndex(final int tx) {
        return tx - offset;
    }
}
