/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.esa.nest.clustering.meanshift;

import org.esa.nest.clustering.fuzzykmeans.*;
import org.esa.beam.framework.gpf.Tile;

import java.util.Iterator;

/**
 * todo - add API doc
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 * @since BEAM 4.2
 */
public final class ClusterIteration {

    private final Tile[] tiles;
    private final Iterator<Tile.Pos> iterator;
    private final Roi roi;

    public ClusterIteration(Tile[] tiles, Roi roi) {
        this.tiles = tiles.clone();
        this.roi = roi;
        iterator = tiles[0].iterator();
    }

    public double[] next(final double[] samples) {
        final int length = samples.length;
        while (iterator.hasNext()) {
            Tile.Pos nextPos = iterator.next();
            if (roi.contains(nextPos.x, nextPos.y)) {
                for (int i = 0; i < length; i++) {
                    samples[i] = tiles[i].getSampleDouble(nextPos.x, nextPos.y);
                }
                return samples;
            }
        }
        return null;
    }
}
