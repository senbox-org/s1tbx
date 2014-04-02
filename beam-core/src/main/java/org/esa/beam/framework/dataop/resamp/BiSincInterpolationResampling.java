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

package org.esa.beam.framework.dataop.resamp;


import org.apache.commons.math.util.FastMath;

final class BiSincInterpolationResampling implements Resampling {

    private static final double DoublePI = 2.0 * Math.PI;
    private static final int filterLength = 5;
    private static final double halfFilterLength = filterLength * 0.5;

    public String getName() {
        return "BISINC_INTERPOLATION";
    }

    public final Index createIndex() {
        return new Index(5, 1);
    }

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
        final int jMax = height - 1;

        int i_0, i_1, i_2, i_3, i_4;
        if (di >= 0) {
            i_0 = i0 - 2;
            i_1 = i0 - 1;
            i_2 = i0;
            i_3 = i0 + 1;
            i_4 = i0 + 2;
            index.ki[0] = di;
        } else {
            i_0 = i0 - 3;
            i_1 = i0 - 2;
            i_2 = i0 - 1;
            i_3 = i0;
            i_4 = i0 + 1;
            index.ki[0] = di + 1;
        }
        index.i[0] = (i_0 < 0) ? 0 : (i_0 > iMax) ? iMax : i_0;
        index.i[1] = (i_1 < 0) ? 0 : (i_1 > iMax) ? iMax : i_1;
        index.i[2] = (i_2 < 0) ? 0 : (i_2 > iMax) ? iMax : i_2;
        index.i[3] = (i_3 < 0) ? 0 : (i_3 > iMax) ? iMax : i_3;
        index.i[4] = (i_4 < 0) ? 0 : (i_4 > iMax) ? iMax : i_4;

        int j_0, j_1, j_2, j_3, j_4;
        if (dj >= 0) {
            j_0 = j0 - 2;
            j_1 = j0 - 1;
            j_2 = j0;
            j_3 = j0 + 1;
            j_4 = j0 + 2;
            index.kj[0] = dj;
        } else {
            j_0 = j0 - 3;
            j_1 = j0 - 2;
            j_2 = j0 - 1;
            j_3 = j0;
            j_4 = j0 + 1;
            index.kj[0] = dj + 1;
        }
        index.j[0] = (j_0 < 0) ? 0 : (j_0 > jMax) ? jMax : j_0;
        index.j[1] = (j_1 < 0) ? 0 : (j_1 > jMax) ? jMax : j_1;
        index.j[2] = (j_2 < 0) ? 0 : (j_2 > jMax) ? jMax : j_2;
        index.j[3] = (j_3 < 0) ? 0 : (j_3 > jMax) ? jMax : j_3;
        index.j[4] = (j_4 < 0) ? 0 : (j_4 > jMax) ? jMax : j_4;
    }

    public final double resample(final Raster raster,
                                 final Index index) throws Exception {

        final int[] x = new int[5];
        final int[] y = new int[5];
        final double[][] samples = new double[5][5];

        for (int i = 0; i < 5; i++) {
            x[i] = (int) index.i[i];
            y[i] = (int) index.j[i];
        }

        if (!raster.getSamples(x, y, samples)) {
            if (Double.isNaN(samples[2][2])) {
                return samples[2][2];
            }
            BiCubicInterpolationResampling.replaceNaNWithMean(samples);
        }

        final double muX = index.ki[0];
        final double muY = index.kj[0];

        final double f0 = sinc(muX + 2.0) * hanning(muX + 2.0);
        final double f1 = sinc(muX + 1.0) * hanning(muX + 1.0);
        final double f2 = sinc(muX + 0.0) * hanning(muX + 0.0);
        final double f3 = sinc(muX - 1.0) * hanning(muX - 1.0);
        final double f4 = sinc(muX - 2.0) * hanning(muX - 2.0);
        final double sum = f0 + f1 + f2 + f3 + f4;

        final double tmpV0 = (f0 * samples[0][0] + f1 * samples[0][1] + f2 * samples[0][2] + f3 * samples[0][3] + f4 * samples[0][4]) / sum;
        final double tmpV1 = (f0 * samples[1][0] + f1 * samples[1][1] + f2 * samples[1][2] + f3 * samples[1][3] + f4 * samples[1][4]) / sum;
        final double tmpV2 = (f0 * samples[2][0] + f1 * samples[2][1] + f2 * samples[2][2] + f3 * samples[2][3] + f4 * samples[2][4]) / sum;
        final double tmpV3 = (f0 * samples[3][0] + f1 * samples[3][1] + f2 * samples[3][2] + f3 * samples[3][3] + f4 * samples[3][4]) / sum;
        final double tmpV4 = (f0 * samples[4][0] + f1 * samples[4][1] + f2 * samples[4][2] + f3 * samples[4][3] + f4 * samples[4][4]) / sum;

        return interpolationSinc(tmpV0, tmpV1, tmpV2, tmpV3, tmpV4, muY);
    }

    private static double interpolationSinc(
            final double y0, final double y1, final double y2, final double y3, final double y4, final double mu) {

        final double f0 = sinc(mu + 2.0) * hanning(mu + 2.0);
        final double f1 = sinc(mu + 1.0) * hanning(mu + 1.0);
        final double f2 = sinc(mu + 0.0) * hanning(mu + 0.0);
        final double f3 = sinc(mu - 1.0) * hanning(mu - 1.0);
        final double f4 = sinc(mu - 2.0) * hanning(mu - 2.0);
        final double sum = f0 + f1 + f2 + f3 + f4;
        return (f0 * y0 + f1 * y1 + f2 * y2 + f3 * y3 + f4 * y4) / sum;
    }

    private static double sinc(final double x) {
        return (Double.compare(x, 0.0) == 0) ? 1.0 : FastMath.sin(x * Math.PI) / (x * Math.PI);
    }

    public static double hanning(final double x) {
        return (x >= -halfFilterLength && x <= halfFilterLength) ?
                0.5 * (1.0 + FastMath.cos(DoublePI * x / (filterLength + 1))) : 0.0;
    }

    @Override
    public String toString() {
        return "BiSinc interpolation resampling";
    }
}
