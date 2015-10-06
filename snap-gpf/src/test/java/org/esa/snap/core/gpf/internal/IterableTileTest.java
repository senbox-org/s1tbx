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

package org.esa.snap.core.gpf.internal;

import junit.framework.TestCase;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.gpf.Tile;

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
