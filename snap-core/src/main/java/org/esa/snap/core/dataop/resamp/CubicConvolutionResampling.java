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


final class CubicConvolutionResampling implements Resampling {

    public String getName() {
        return "CUBIC_CONVOLUTION";
    }

    public final Index createIndex() {
        return new Index(4, 1);
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

        index.i0 = i0;
        index.j0 = j0;

        final double di = x - (i0 + 0.5);
        final double dj = y - (j0 + 0.5);

        final int iMax = width - 1;
        if (di >= 0) {
            index.i[0] = Index.crop(i0 - 1, iMax);
            index.i[1] = Index.crop(i0, iMax);
            index.i[2] = Index.crop(i0 + 1, iMax);
            index.i[3] = Index.crop(i0 + 2, iMax);
            index.ki[0] = di;
        } else {
            index.i[0] = Index.crop(i0 - 2, iMax);
            index.i[1] = Index.crop(i0 - 1, iMax);
            index.i[2] = Index.crop(i0, iMax);
            index.i[3] = Index.crop(i0 + 1, iMax);
            index.ki[0] = di + 1;
        }

        final int jMax = height - 1;
        if (dj >= 0) {
            index.j[0] = Index.crop(j0 - 1, jMax);
            index.j[1] = Index.crop(j0, jMax);
            index.j[2] = Index.crop(j0 + 1, jMax);
            index.j[3] = Index.crop(j0 + 2, jMax);
            index.kj[0] = dj;
        } else {
            index.j[0] = Index.crop(j0 - 2, jMax);
            index.j[1] = Index.crop(j0 - 1, jMax);
            index.j[2] = Index.crop(j0, jMax);
            index.j[3] = Index.crop(j0 + 1, jMax);
            index.kj[0] = dj + 1;
        }
    }

    public final double resample(final Raster raster,
                                 final Index index) throws Exception {

        final int[] x = new int[4];
        final int[] y = new int[4];
        final double[][] samples = new double[4][4];

        for (int i = 0; i < 4; i++) {
            x[i] = (int) index.i[i];
            y[i] = (int) index.j[i];
        }
        if (!raster.getSamples(x, y, samples)) {
            if (Double.isNaN(samples[1][1])) {
                return samples[1][1];
            }
            double mean = 0.0;
            int k = 0;
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if (!Double.isNaN(samples[i][j])) {
                        mean += samples[i][j];
                        k++;
                    }
                }
            }
            mean /= k;
            for (int i = 0; i < 4; i++) {
                for (int j = 0; j < 4; j++) {
                    if (Double.isNaN(samples[i][j])) {
                        samples[i][j] = mean;
                    }
                }
            }
        }

        final double muX = index.ki[0];
        final double muY = index.kj[0];
        final double muX2 = muX * muX;
        final double muX3 = muX2 * muX;

        final double c0 = 0.5 * (-muX + 2 * muX2 - muX3);
        final double c1 = 0.5 * (2 - 5 * muX2 + 3 * muX3);
        final double c2 = 0.5 * (muX + 4 * muX2 - 3 * muX3);
        final double c3 = 0.5 * (-muX2 + muX3);
        final double sum = c0 + c1 + c2 + c3;

        final double tmpV0 = (c0 * samples[0][0] + c1 * samples[0][1] + c2 * samples[0][2] + c3 * samples[0][3]) / sum;
        final double tmpV1 = (c0 * samples[1][0] + c1 * samples[1][1] + c2 * samples[1][2] + c3 * samples[1][3]) / sum;
        final double tmpV2 = (c0 * samples[2][0] + c1 * samples[2][1] + c2 * samples[2][2] + c3 * samples[2][3]) / sum;
        final double tmpV3 = (c0 * samples[3][0] + c1 * samples[3][1] + c2 * samples[3][2] + c3 * samples[3][3]) / sum;
        return interpolationCubic(tmpV0, tmpV1, tmpV2, tmpV3, muY, muY * muY, muY * muY * muY);
    }

    private static double interpolationCubic(
            final double y0, final double y1, final double y2, final double y3,
            final double t, final double t2, final double t3) {

        final double c0 = 0.5 * (-t + 2 * t2 - t3);
        final double c1 = 0.5 * (2 - 5 * t2 + 3 * t3);
        final double c2 = 0.5 * (t + 4 * t2 - 3 * t3);
        final double c3 = 0.5 * (-t2 + t3);
        final double sum = c0 + c1 + c2 + c3;
        return (c0 * y0 + c1 * y1 + c2 * y2 + c3 * y3) / sum;
    }

    @Override
    public String toString() {
        return "Cubic convolution resampling";
    }
}
