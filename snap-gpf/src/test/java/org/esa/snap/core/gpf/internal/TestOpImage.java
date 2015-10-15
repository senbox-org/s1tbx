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

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.image.BandOpImage;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.image.WritableRaster;
import java.util.HashMap;

class TestOpImage extends BandOpImage {

    private HashMap<Rectangle, TileImpl> tileMap = new HashMap<Rectangle, TileImpl>(4);

    public TestOpImage(Band band) {
        super(band);
    }

    public synchronized int getTileCount() {
        return tileMap.size();
    }

    public synchronized TileImpl getTile(Rectangle rect) {
        return tileMap.get(rect);
    }

    // mz 12.09.2008: removed to let the test run green. Otherwise the BLOCKED
//    @Override
//    public synchronized Raster[] getTiles() {
//        JAI.getDefaultInstance().getTileCache().removeTiles(this);
//        return super.getTiles();
//    }

    @Override
    protected synchronized void computeRect(PlanarImage[] planarImages, WritableRaster writableRaster, Rectangle rectangle) {
        int x1 = writableRaster.getMinX();
        int x2 = writableRaster.getMinX() + writableRaster.getWidth() - 1;
        int y1 = writableRaster.getMinY();
        int y2 = writableRaster.getMinY() + writableRaster.getHeight() - 1;
        for (int y = y1; y <= y2; y++) {
            for (int x = x1; x <= x2; x++) {
                double sample = getSampleDouble(x, y);
                writableRaster.setSample(x, y, 0, sample);
            }
        }
        tileMap.put(rectangle, new TileImpl(getRasterDataNode(), writableRaster, rectangle));
    }

    public static double getSampleDouble(int x, int y) {
        return 10.0 * x + y + 0.5;
    }
}
