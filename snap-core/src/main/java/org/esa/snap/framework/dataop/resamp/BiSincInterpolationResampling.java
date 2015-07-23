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

package org.esa.snap.framework.dataop.resamp;

import org.apache.commons.math3.util.FastMath;

class BiSincInterpolationResampling implements Resampling {

    private static final double DoublePI = 2.0 * Math.PI;
    private int kernelSize = 0;
    private int halfKernelSize = 0;

    public BiSincInterpolationResampling(final int kernelSize) {
        this.kernelSize = kernelSize;
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

        for (int i = 0; i < kernelSize; i++) {
            x[i] = (int) index.i[i];
            y[i] = (int) index.j[i];
        }

        if (!raster.getSamples(x, y, samples)) {
            if (Double.isNaN(samples[halfKernelSize][halfKernelSize])) {
                return samples[halfKernelSize][halfKernelSize];
            }
            BiCubicInterpolationResampling.replaceNaNWithMean(samples);
        }

        final double muX = index.ki[0];
        final double muY = index.kj[0];
        final double cx = muX + halfKernelSize;
        final double cy = muY + halfKernelSize;

        final double[] winX = new double[kernelSize];
        final double[] winY = new double[kernelSize];
        double sumX = 0.0, sumY = 0.0;
        for (int n = 0; n < kernelSize; n++) {
            winX[n] = sinc(cx - n) * hanning(cx - n);
            winY[n] = sinc(cy - n) * hanning(cy - n);
            sumX += winX[n];
            sumY += winY[n];
        }

        double v = 0.0;
        for (int j = 0; j < kernelSize; j++) {
            for (int i = 0; i < kernelSize; i++) {
                v += samples[j][i]*winX[i]*winY[j];
            }
        }
        v /= sumX*sumY;

        return v;
    }

    private static double sinc(final double x) {
        return (Double.compare(x, 0.0) == 0) ? 1.0 : FastMath.sin(x * Math.PI) / (x * Math.PI);
    }

    public double hanning(final double x) {
        return (x >= -halfKernelSize && x <= halfKernelSize) ?
                0.5 * (1.0 + FastMath.cos(DoublePI * x / (kernelSize + 1))) : 0.0;
    }

    public int getKernelSize() {
        return kernelSize;
    }

    @Override
    public String toString() {
        return "BiSinc interpolation resampling";
    }
}
