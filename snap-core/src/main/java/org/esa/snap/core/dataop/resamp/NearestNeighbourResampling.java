/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.dataop.resamp;


/**
 * This class implements the Nearest Neighbour resampling method.
 *
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 */
final class NearestNeighbourResampling implements Resampling {

    public String getName() {
        return "NEAREST_NEIGHBOUR";
    }

    public final Index createIndex() {
        return new Index(0, 0);
    }

    public final void computeIndex(final double x,
                                   final double y,
                                   int width, int height, final Index index) {
        index.x = x;
        index.y = y;
        index.width = width;
        index.height = height;

        index.i0 = Index.crop((int) Math.floor(x), width - 1);
        index.j0 = Index.crop((int) Math.floor(y), height - 1);

    }

    public final double resample(final Raster raster,
                                 final Index index) throws Exception {

        final int[] x = {(int) index.i0};
        final int[] y = {(int) index.j0};
        final double[][] samples = new double[1][1];
        raster.getSamples(x, y, samples);

        return samples[0][0];
    }

    @Override
    public String toString() {
        return "Nearest neighbour resampling";
    }
}
