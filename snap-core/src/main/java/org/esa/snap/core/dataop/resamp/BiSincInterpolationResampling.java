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

import org.apache.commons.math3.util.FastMath;

class BiSincInterpolationResampling implements Resampling {

    private static final double PI = Math.PI;
    private static final double DoublePI = 2.0 * Math.PI;
    private final double kernelSizefraction;
    private final int kernelSize;
    private final int kernelSize1;
    private final int halfKernelSize;

    public BiSincInterpolationResampling(final int kernelSize) {
        this.kernelSize = kernelSize;
        this.kernelSize1 = kernelSize - 1;
        this.halfKernelSize = kernelSize/2;
        this.kernelSizefraction = DoublePI / kernelSize1;
    }

    public String getName() {
        return "BISINC_INTERPOLATION";
    }

    public final Index createIndex() {
        return new Index(kernelSize, 1);
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

        final int size = kernelSize;
        final int minI = i0 - halfKernelSize;
        final int minJ = j0 - halfKernelSize;

        int v;
        if (di >= 0) {
            for (int i = 0; i < size; i++) {
                v = minI + i;
                index.i[i] = Math.min(v >= 0 ? v : 0, iMax);
            }
            index.ki[0] = di;
        } else {
            for (int i = 0; i < size; i++) {
                v = minI - 1 + i;
                index.i[i] = Math.min(v >= 0 ? v : 0, iMax);
            }
            index.ki[0] = di + 1;
        }

        if (dj >= 0) {
            for (int j = 0; j < size; j++) {
                v = minJ + j;
                index.j[j] = Math.min(v >= 0 ? v : 0, jMax);
            }
            index.kj[0] = dj;
        } else {
            for (int j = 0; j < size; j++) {
                v = minJ - 1 + j;
                index.j[j] = Math.min(v >= 0 ? v : 0, jMax);
            }
            index.kj[0] = dj + 1;
        }
    }

    public final double resample(final Raster raster,
                                 final Index index) throws Exception {

        final int size = kernelSize;
        final int halfSize = halfKernelSize;
        final int nhalfSize = -halfSize;
        final double kernelSizefract = kernelSizefraction;

        final int[] x = new int[size];
        final int[] y = new int[size];
        final double[][] samples = new double[size][size];
        final double[] winX = new double[size];

        final double cx = index.ki[0] + halfSize;

        double sumX = 0.0, xx, xxPI;
        for (int n = 0; n < size; n++) {
            x[n] = (int) index.i[n];
            y[n] = (int) index.j[n];
            xx = cx - n;
            xxPI = xx * PI;
            winX[n] = xx >= nhalfSize && xx <= halfSize ?
                    xx == 0 ? 1.0 : FastMath.sin(xxPI) / (xxPI) *
                            (0.5 * (1.0 + FastMath.cos(kernelSizefract * xx))) : 0.0;
            //winX[n] = sincHanning(cx - n);
            sumX += winX[n];
        }

        if (!raster.getSamples(x, y, samples)) {
            if (Double.isNaN(samples[halfSize][halfSize])) {
                return samples[halfSize][halfSize];
            }
            BiCubicInterpolationResampling.replaceNaNWithMean(samples);
        }

        final double cy = index.kj[0] + halfSize;
        double sumY = 0, winY;
        double v = 0.0;
        for (int j = 0; j < size; j++) {
            xx = cy - j;
            xxPI = xx * PI;
            winY = xx >= nhalfSize && xx <= halfSize ?
                    xx == 0 ? 1.0 : FastMath.sin(xxPI) / (xxPI) *
                            (0.5 * (1.0 + FastMath.cos(kernelSizefract * xx))) : 0.0;
            //winY = sincHanning(cy - j);
            sumY += winY;
            for (int i = 0; i < size; i++) {
                v += samples[j][i]*winX[i]*winY;
            }
        }
        v /= sumX*sumY;

        return v;
    }

    private double sincHanning(final double x) {
        return x >= -halfKernelSize && x <= halfKernelSize ?
                x == 0 ? 1.0 : FastMath.sin(x * PI) / (x * PI) *
                        (0.5 * (1.0 + FastMath.cos(kernelSizefraction * x))) : 0.0;
    }

    public int getKernelSize() {
        return kernelSize;
    }

    @Override
    public String toString() {
        return "BiSinc interpolation resampling";
    }
}
