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

package org.esa.beam.framework.dataop.resamp;


final class BilinearInterpolationResampling implements Resampling {

    public String getName() {
        return "BILINEAR_INTERPOLATION";
    }

    public final Index createIndex() {
        return new Index(2, 1);
    }

    public final void computeIndex(final float x,
                                   final float y,
                                   final int width,
                                   final int height,
                                   final Index index) {
        index.x = x;
        index.y = y;
        index.width = width;
        index.height = height;

        final int i0 = (int) Math.floor(x);
        final int j0 = (int) Math.floor(y);

        final float di = x - (i0 + .5f);
        final float dj = y - (j0 + .5f);

        index.i0 = i0;
        index.j0 = j0;

        final int iMax = width - 1;
        if (di >= 0) {
            index.i[0] = Index.crop(i0 + 0, iMax);
            index.i[1] = Index.crop(i0 + 1, iMax);
            index.ki[0] = di;
        } else {
            index.i[0] = Index.crop(i0 - 1, iMax);
            index.i[1] = Index.crop(i0 + 0, iMax);
            index.ki[0] = di + 1;
        }

        final int jMax = height - 1;
        if (dj >= 0) {
            index.j[0] = Index.crop(j0 + 0, jMax);
            index.j[1] = Index.crop(j0 + 1, jMax);
            index.kj[0] = dj;
        } else {
            index.j[0] = Index.crop(j0 - 1, jMax);
            index.j[1] = Index.crop(j0 + 0, jMax);
            index.kj[0] = dj + 1;
        }
    }

    public final float resample(final Raster raster,
                                final Index index) throws Exception {

        final int i1 = index.i[0];
        final int i2 = index.i[1];
        final int j1 = index.j[0];
        final int j2 = index.j[1];

        final float ki = index.ki[0];
        final float kj = index.kj[0];

        final float z11 = raster.getSample(i1, j1);
        final float z12 = raster.getSample(i2, j1);
        final float z21 = raster.getSample(i1, j2);
        final float z22 = raster.getSample(i2, j2);

        if (Float.isNaN(z11) || Float.isNaN(z12) || Float.isNaN(z21) || Float.isNaN(z22)) {
            return raster.getSample(index.i0, index.j0);
        }

        return z11 * (1f - ki) * (1f - kj) +
                z12 * ki * (1f - kj) +
                z21 * (1f - ki) * kj +
                z22 * ki * kj;
    }

    @Override
    public String toString() {
        return "Bilinear interpolation resampling";
    }
}
