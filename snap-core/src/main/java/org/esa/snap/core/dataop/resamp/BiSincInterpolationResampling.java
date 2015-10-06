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

    private static final double DoublePI = 2.0 * Math.PI;
    private final int kernelSize;
    private final int kernelSize1;
    private final int halfKernelSize;

    public BiSincInterpolationResampling(final int kernelSize) {
        this.kernelSize = kernelSize;
        this.kernelSize1 = kernelSize - 1;
        this.halfKernelSize = kernelSize/2;
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

        if (di >= 0) {
            for (int i = 0; i < kernelSize; i++) {
                index.i[i] = Math.min(Math.max(i0 - halfKernelSize + i, 0), iMax);
            }
            index.ki[0] = di;
        } else {
            for (int i = 0; i < kernelSize; i++) {
                index.i[i] = Math.min(Math.max(i0 - halfKernelSize - 1 + i, 0), iMax);
            }
            index.ki[0] = di + 1;
        }

        if (dj >= 0) {
            for (int j = 0; j < kernelSize; j++) {
                index.j[j] = Math.min(Math.max(j0 - halfKernelSize + j, 0), jMax);
            }
            index.kj[0] = dj;
        } else {
            for (int j = 0; j < kernelSize; j++) {
                index.j[j] = Math.min(Math.max(j0 - halfKernelSize - 1 + j, 0), jMax);
            }
            index.kj[0] = dj + 1;
        }
    }

    public final double resample(final Raster raster,
                                 final Index index) throws Exception {

        final int[] x = new int[kernelSize];
        final int[] y = new int[kernelSize];
        final double[][] samples = new double[kernelSize][kernelSize];
        final double[] winX = new double[kernelSize];

        final double cx = index.ki[0] + halfKernelSize;

        double sumX = 0.0;
        for (int n = 0; n < kernelSize; n++) {
            x[n] = (int) index.i[n];
            y[n] = (int) index.j[n];
            winX[n] = sincHanning(cx - n);
            sumX += winX[n];
        }

        if (!raster.getSamples(x, y, samples)) {
            if (Double.isNaN(samples[halfKernelSize][halfKernelSize])) {
                return samples[halfKernelSize][halfKernelSize];
            }
            BiCubicInterpolationResampling.replaceNaNWithMean(samples);
        }

        final double cy = index.kj[0] + halfKernelSize;
        double sumY = 0, winY;
        double v = 0.0;
        for (int j = 0; j < kernelSize; j++) {
            winY = sincHanning(cy - j);
            sumY += winY;
            for (int i = 0; i < kernelSize; i++) {
                v += samples[j][i]*winX[i]*winY;
            }
        }
        v /= sumX*sumY;

        return v;
    }

    private double sincHanning(final double x) {
        return x >= -halfKernelSize && x <= halfKernelSize ?
                x == 0 ? 1.0 : FastMath.sin(x * Math.PI) / (x * Math.PI) *
                        (0.5 * (1.0 + FastMath.cos(DoublePI * x / kernelSize1))) : 0.0;
    }

    public int getKernelSize() {
        return kernelSize;
    }

    @Override
    public String toString() {
        return "BiSinc interpolation resampling";
    }
}
