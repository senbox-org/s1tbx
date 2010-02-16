/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.cluster;

import org.esa.beam.framework.gpf.Tile;

import javax.media.jai.ROI;
import java.util.Iterator;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
class PixelIter {

    private final Tile[] tiles;
    private final Iterator<Tile.Pos> iterator;
    private final ROI roi;

    PixelIter(Tile[] tiles, ROI roi) {
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
