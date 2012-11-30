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

final class BiCubicInterpolationResampling implements Resampling {

    private final static float[][] invA = {
                {1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {-3, 3, 0, 0, -2, -1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {2, -2, 0, 0, 1, 1, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 0, 1, 0, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, -3, 3, 0, 0, -2, -1, 0, 0},
                {0, 0, 0, 0, 0, 0, 0, 0, 2, -2, 0, 0, 1, 1, 0, 0},
                {-3, 0, 3, 0, 0, 0, 0, 0, -2, 0, -1, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, -3, 0, 3, 0, 0, 0, 0, 0, -2, 0, -1, 0},
                {9, -9, -9, 9, 6, 3, -6, -3, 6, -6, 3, -3, 4, 2, 2, 1},
                {-6, 6, 6, -6, -3, -3, 3, 3, -4, 4, -2, 2, -2, -2, -1, -1},
                {2, 0, -2, 0, 0, 0, 0, 0, 1, 0, 1, 0, 0, 0, 0, 0},
                {0, 0, 0, 0, 2, 0, -2, 0, 0, 0, 0, 0, 1, 0, 1, 0},
                {-6, 6, 6, -6, -4, -2, 4, 2, -3, 3, -3, 3, -2, -1, -2, -1},
                {4, -4, -4, 4, 2, 2, -2, -2, 2, -2, 2, -2, 1, 1, 1, 1}};

    public String getName() {
        return "BICUBIC_INTERPOLATION";
    }

    public final Index createIndex() {
        return new Index(2, 1);
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

        if (di >= 0) {
            final int i1 = i0 + 1;
            index.i[0] = (i0 < 0) ? 0 : (i0 > iMax) ? iMax : i0; //Index.crop(i0, iMax);
            index.i[1] = (i1 < 0) ? 0 : (i1 > iMax) ? iMax : i1; //Index.crop(i0 + 1, iMax);
            index.ki[0] = di;
        } else {
            final int i1 = i0 - 1;
            index.i[0] = (i1 < 0) ? 0 : (i1 > iMax) ? iMax : i1; //Index.crop(i0 - 1, iMax);
            index.i[1] = (i0 < 0) ? 0 : (i0 > iMax) ? iMax : i0; //Index.crop(i0, iMax);
            index.ki[0] = di + 1;
        }

        if (dj >= 0) {
            final int j1 = j0 + 1;
            index.j[0] = (j0 < 0) ? 0 : (j0 > jMax) ? jMax : j0; //Index.crop(j0, jMax);
            index.j[1] = (j1 < 0) ? 0 : (j1 > jMax) ? jMax : j1; //Index.crop(j0 + 1, jMax);
            index.kj[0] = dj;
        } else {
            final int j1 = j0 - 1;
            index.j[0] = (j1 < 0) ? 0 : (j1 > jMax) ? jMax : j1; //Index.crop(j0 - 1, jMax);
            index.j[1] = (j0 < 0) ? 0 : (j0 > jMax) ? jMax : j0; //Index.crop(j0, jMax);
            index.kj[0] = dj + 1;
        }

        /*
        final float di = x - i0;
        final float dj = y - j0;

        index.i0 = i0;
        index.j0 = j0;

        final int iMax = width - 1;
        final int jMax = height - 1;

        index.i[0] = Index.crop(i0, iMax);
        index.i[1] = Index.crop(i0 + 1, iMax);
        index.ki[0] = di;

        index.j[0] = Index.crop(j0, jMax);
        index.j[1] = Index.crop(j0 + 1, jMax);
        index.kj[0] = dj;
        */
    }

    public final float resample(final Raster raster,
                                final Index index) throws Exception {

        int[] x = new int[4];
        int[] y = new int[4];
        float[][] samples = new float[4][4];

        for (int i = 0; i < 4; i++) {
            x[i] = (int)Index.crop(index.i[0] - 1 + i, index.width-1);
            y[i] = (int)Index.crop(index.j[0] - 1 + i, index.height-1);
        }
        raster.getSamples(x, y, samples);

        for (int i = 0; i < 4; i++) {
            for (int j = 0; j < 4; j++) {
                if(Double.isNaN(samples[j][i])) {
                    return samples[1][1];
                }
            }
        }

        // the four grid points of a rectangular grid cell are numbered as the following:
        // p1    p2
        //
        // p3    p4

        final double[] z = new double[4];     // function values
        final double[] z1 = new double[4];    // 1st order derivative in y direction
        final double[] z2 = new double[4];    // 1st order derivative in x direction
        final double[] z12 = new double[4];   // cross derivative

        z[0] = samples[1][1];
        z[1] = samples[1][2];
        z[2] = samples[2][1];
        z[3] = samples[2][2];

        z1[0] = (samples[1][2] - samples[1][0]) / 2.0;
        z1[1] = (samples[1][3] - samples[1][1]) / 2.0;
        z1[2] = (samples[2][2] - samples[2][0]) / 2.0;
        z1[3] = (samples[2][3] - samples[2][1]) / 2.0;

        z2[0] = (samples[2][1] - samples[0][1]) / 2.0;
        z2[1] = (samples[2][2] - samples[0][2]) / 2.0;
        z2[2] = (samples[3][1] - samples[1][1]) / 2.0;
        z2[3] = (samples[3][2] - samples[1][2]) / 2.0;

        z12[0] = (samples[2][2] - samples[2][0] - samples[0][2] + samples[0][0]) / 4.0;
        z12[1] = (samples[2][3] - samples[2][1] - samples[0][3] + samples[0][1]) / 4.0;
        z12[2] = (samples[3][2] - samples[3][0] - samples[1][2] + samples[1][0]) / 4.0;
        z12[3] = (samples[3][3] - samples[3][1] - samples[1][3] + samples[1][1]) / 4.0;

        return bcuint(z, z1, z2, z12, index.ki[0], index.kj[0]);
    }

	private static float bcuint(final double z[], final double z1[], final double z2[],
                         final double z12[], final double t, final double u) {

        // alpha = [a00 a10 a20 a30 a01 a11 a21 a31 a02 a12 a22 a32 a03 a13 a23 a33]
		final double[][] a = new double[4][4];
		bcucof(z, z1, z2, z12, a);

        double ansy = 0.0f;
		for (int i = 3; i >= 0; i--) {
			ansy = t*ansy + ((a[i][3]*u + a[i][2])*u + a[i][1])*u + a[i][0];
		}

        // todo v is not used?? check with Jun
        /*
        float t2 = t*t;
        float t3 = t2*t;
        float u2 = u*u;
        float u3 = u2*u;

        float v = a[0][0] + a[0][1]*u + a[0][2]*u2 + a[0][3]*u3 +
                  a[1][0]*t + a[1][1]*t*u + a[1][2]*t*u2 + a[1][3]*t*u3 +
                  a[2][0]*t2 + a[2][1]*t2*u + a[2][2]*t2*u2 + a[2][3]*t2*u3 +
                  a[3][0]*t3 + a[3][1]*t3*u + a[3][2]*t3*u2 + a[3][3]*t3*u3;
        */
		return (float)ansy;
	}

	private static void bcucof(final double z[], final double z1[], final double z2[], final double z12[],
                               final double[][] a) {

        // x = [f(0,0) f(1,0) f(0,1) f(1,1) fx(0,0) fx(1,0) fx(0,1) fx(1,1) fy(0,0) fy(1,0) fy(0,1) fy(1,1) fxy(0,0) fxy(1,0) fxy(0,1) fxy(1,1)]
        // alpha = [a00 a10 a20 a30 a01 a11 a21 a31 a02 a12 a22 a32 a03 a13 a23 a33]
        // alpha = invA*x

        final double[] x = new double[16];
        for (int i = 0; i < 4; i++) {
            x[i] = z[i];
            x[i+4] = z1[i];
            x[i+8] = z2[i];
            x[i+12] = z12[i];
        }

		final double[] cl = new double[16];
		for (int i = 0; i < 16; i++) {
            double xx = 0.0;
			for (int k = 0; k < 16; k++) {
                xx += invA[i][k]*x[k];
			}
			cl[i] = xx;
		}
		
		int l = 0;
		for (int i = 0; i < 4; i++) {
			for (int j = 0; j < 4; j++) {
				a[j][i] = cl[l++];
			}
		}
	}

    @Override
    public String toString() {
        return "BiCubic interpolation resampling";
    }
}
