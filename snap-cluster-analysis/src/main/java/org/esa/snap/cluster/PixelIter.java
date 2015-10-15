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
package org.esa.snap.cluster;

import org.esa.snap.core.gpf.Tile;

import java.util.Iterator;

/**
 * Iterator for pixels within a region of interest.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class PixelIter {

    private final Tile[] tiles;
    private final Iterator<Tile.Pos> iterator;
    private final Roi roi;

    PixelIter(Tile[] tiles, Roi roi) {
        this.tiles = tiles.clone();
        this.roi = roi;
        iterator = tiles[0].iterator();
    }

    double[] next(double[] samples) {
        while (iterator.hasNext()) {
            Tile.Pos nextPos = iterator.next();
            if (roi == null || roi.contains(nextPos.x, nextPos.y)) {
                for (int i = 0; i < samples.length; i++) {
                    samples[i] = tiles[i].getSampleDouble(nextPos.x, nextPos.y);
                }
                return samples;
            }
        }
        return null;
    }
}
