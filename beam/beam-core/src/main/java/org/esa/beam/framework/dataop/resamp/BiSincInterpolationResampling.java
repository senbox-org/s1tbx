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


final class BiSincInterpolationResampling implements Resampling {

    private static final double DoublePI = 2.0*Math.PI;
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

        index.i[0] = Index.crop(i0 - 2, iMax);
        index.i[1] = Index.crop(i0 - 1, iMax);
        index.i[2] = Index.crop(i0, iMax);
        index.i[3] = Index.crop(i0 + 1, iMax);
        index.i[4] = Index.crop(i0 + 2, iMax);

        index.ki[0] = di;

        index.j[0] = Index.crop(j0 - 2, jMax);
        index.j[1] = Index.crop(j0 - 1, jMax);
        index.j[2] = Index.crop(j0, jMax);
        index.j[3] = Index.crop(j0 + 1, jMax);
        index.j[4] = Index.crop(j0 + 2, jMax);

        index.kj[0] = dj;
    }

    public final float resample(final Raster raster,
                                final Index index) throws Exception {
        final double[][] v = new double[5][5];
        for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 5; j++) {
                v[j][i] = raster.getSample(index.i[i], index.j[j]);
                if(Double.isNaN(v[j][i])) {
                    return raster.getSample(index.i0, index.j0);
                }
            }
        }

        final double muX = index.ki[0];
        final double muY = index.kj[0];

        final double f0 = sinc(muX + 2.0) * hanning(muX + 2.0);
        final double f1 = sinc(muX + 1.0) * hanning(muX + 1.0);
        final double f2 = sinc(muX + 0.0) * hanning(muX + 0.0);
        final double f3 = sinc(muX - 1.0) * hanning(muX - 1.0);
        final double f4 = sinc(muX - 2.0) * hanning(muX - 2.0);
        final double sum = f0 + f1 + f2 + f3 + f4;

        final double tmpV0 = (f0*v[0][0] + f1*v[0][1] + f2*v[0][2] + f3*v[0][3] + f4*v[0][4]) / sum;
        final double tmpV1 = (f0*v[1][0] + f1*v[1][1] + f2*v[1][2] + f3*v[1][3] + f4*v[1][4]) / sum;
        final double tmpV2 = (f0*v[2][0] + f1*v[2][1] + f2*v[2][2] + f3*v[2][3] + f4*v[2][4]) / sum;
        final double tmpV3 = (f0*v[3][0] + f1*v[3][1] + f2*v[3][2] + f3*v[3][3] + f4*v[3][4]) / sum;
        final double tmpV4 = (f0*v[4][0] + f1*v[4][1] + f2*v[4][2] + f3*v[4][3] + f4*v[4][4]) / sum;
        return (float)interpolationSinc(tmpV0, tmpV1, tmpV2, tmpV3, tmpV4, muY);
    }

    private static double interpolationSinc(
            final double y0, final double y1, final double y2, final double y3, final double y4, final double mu) {

        final double f0 = sinc(mu + 2.0) * hanning(mu + 2.0);
        final double f1 = sinc(mu + 1.0) * hanning(mu + 1.0);
        final double f2 = sinc(mu + 0.0) * hanning(mu + 0.0);
        final double f3 = sinc(mu - 1.0) * hanning(mu - 1.0);
        final double f4 = sinc(mu - 2.0) * hanning(mu - 2.0);
        final double sum = f0 + f1 + f2 + f3 + f4;
        return (f0*y0 + f1*y1 + f2*y2 + f3*y3 + f4*y4)/sum;
    }

    private static double sinc(final double x) {
        return (Double.compare(x, 0.0) == 0) ? 1.0 : Math.sin(x*Math.PI) / (x*Math.PI);
    }

    public static double hanning(final double x) {
        return (x >= -halfFilterLength && x <= halfFilterLength) ?
             0.5*(1.0 + Math.cos(DoublePI*x/(filterLength + 1))) : 0.0;
    }

    @Override
    public String toString() {
        return "BiSinc interpolation resampling";
    }
}
