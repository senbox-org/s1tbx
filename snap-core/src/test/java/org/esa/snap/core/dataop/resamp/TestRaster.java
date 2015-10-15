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

class TestRaster implements Resampling.Raster {

    float[][] array = new float[][]{
            new float[]{10, 20, 30, 40, 50},
            new float[]{30, 20, 30, 20, 30},
            new float[]{10, 40, 20, 30, 70},
            new float[]{20, 30, 40, 60, 80},
            new float[]{10, 40, 10, 90, 70},
    };

    public int getWidth() {
        return array[0].length;
    }

    public int getHeight() {
        return array.length;
    }

    public float getSample(double x, double y) {
        return array[(int) y][(int) x];
    }

    public boolean getSamples(int[] x, int[] y, double[][] samples) {
        for (int i = 0; i < y.length; i++) {
            for (int j = 0; j < x.length; j++) {
                samples[i][j] = getSample(x[j], y[i]);
            }
        }
        return true;
    }
}
