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

public final class BilinearInterpolationResampling implements Resampling {

    public String getName() {
        return "BILINEAR_INTERPOLATION";
    }

    public final Index createIndex() {
        return new Index(2, 1);
    }

    // Compute resampling index for given pixel coordinate assuming that the pixel intensity is located at its center.
    public final void computeIndex(final double x,
                                   final double y,
                                   final int width,
                                   final int height,
                                   final Index index) {
        index.x = x;
        index.y = y;
        index.width = width;
        index.height = height;

        final int i0 = (int) Math.floor(x);
        final int j0 = (int) Math.floor(y);

        final double di = x - (i0 + 0.5);
        final double dj = y - (j0 + 0.5);

        index.i0 = i0;
        index.j0 = j0;

        final int iMax = width - 1;
        if (di >= 0) {
            final int i1 = i0 + 1;
            index.i[0] = (i0 < 0) ? 0 : (i0 > iMax) ? iMax : i0;
            index.i[1] = (i1 < 0) ? 0 : (i1 > iMax) ? iMax : i1;
            index.ki[0] = di;
        } else {
            final int i1 = i0 - 1;
            index.i[0] = (i1 < 0) ? 0 : (i1 > iMax) ? iMax : i1;
            index.i[1] = (i0 < 0) ? 0 : (i0 > iMax) ? iMax : i0;
            index.ki[0] = di + 1;
        }

        final int jMax = height - 1;
        if (dj >= 0) {
            final int j1 = j0 + 1;
            index.j[0] = (j0 < 0) ? 0 : (j0 > jMax) ? jMax : j0;
            index.j[1] = (j1 < 0) ? 0 : (j1 > jMax) ? jMax : j1;
            index.kj[0] = dj;
        } else {
            final int j1 = j0 - 1;
            index.j[0] = (j1 < 0) ? 0 : (j1 > jMax) ? jMax : j1;
            index.j[1] = (j0 < 0) ? 0 : (j0 > jMax) ? jMax : j0;
            index.kj[0] = dj + 1;
        }
    }

    public final double resample(final Raster raster, final Index index) throws Exception {

        final int[] x = new int[]{(int) index.i[0], (int) index.i[1]};
        final int[] y = new int[]{(int) index.j[0], (int) index.j[1]};
        final double[][] samples = new double[2][2];

        if (!raster.getSamples(x, y, samples)) {
            return samples[0][0];
        }

        final double ki = index.ki[0];
        final double kj = index.kj[0];

        return samples[0][0] * (1f - ki) * (1f - kj) +
                samples[0][1] * ki * (1f - kj) +
                samples[1][0] * (1f - ki) * kj +
                samples[1][1] * ki * kj;
    }

    @Override
    public String toString() {
        return "Bilinear interpolation resampling";
    }
}
