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

/**
 * An interface used to implement different resampling strategies.
 *
 * @author Norman Fomferra
 * @author Jun Lu
 * @author Luis Veci
 */
public interface Resampling {

    /**
     * The nearest neighbour resampling method.
     */
    Resampling NEAREST_NEIGHBOUR = new NearestNeighbourResampling();
    /**
     * The bilinear interpolation resampling method.
     */
    Resampling BILINEAR_INTERPOLATION = new BilinearInterpolationResampling();
    /**
     * The cubic convolution resampling method.
     */
    Resampling CUBIC_CONVOLUTION = new CubicConvolutionResampling();
    /**
     * The bisinc interpolation resampling method.
     */
    Resampling BISINC_5_POINT_INTERPOLATION = new BiSinc5PointInterpolationResampling();
    Resampling BISINC_11_POINT_INTERPOLATION = new BiSinc11PointInterpolationResampling();
    Resampling BISINC_21_POINT_INTERPOLATION = new BiSinc21PointInterpolationResampling();
    /**
     * The bicubic spline interpolation resampling method.
     */
    Resampling BICUBIC_INTERPOLATION = new BiCubicInterpolationResampling();

    /**
     * Gets a unique identifier for this resampling method, e.g. "BILINEAR_INTERPOLATION".
     *
     * @return a unique name
     */
    String getName();

    /**
     * Factory method which creates an appropriate index for raster access.
     *
     * @return an appropriate index, never null
     */
    Index createIndex();

    /**
     * Computes the index's properties for the given pixel coordinate.
     *
     * @param x      the raster's x coordinate
     * @param y      the raster's y coordinate
     * @param width  the raster's width
     * @param height the raster's height
     * @param index  the index object to which the results are to be assigned
     */
    void computeIndex(double x, double y, int width, int height, Index index);

    /**
     * Computes the index's properties for the given non-pixel coordinate without pixel center intensity assumption.
     * <p/>
     * The default implementation computes the resampling index for given coordinate without pixel center intensity assumption.
     * It adds 0.5 to coordinates to counter the consideration of pixel center in base function.
     *
     * @param x      the raster's x coordinate
     * @param y      the raster's y coordinate
     * @param width  the raster's width
     * @param height the raster's height
     * @param index  the index object to which the results are to be assigned
     */
    default void computeCornerBasedIndex(double x, double y, int width, int height, Index index)  {
        computeIndex(x + 0.5, y + 0.5, width, height, index);
    }

    /**
     * Performs the actual resampling operation.
     * If a sample value could not be computed at the given index, e.g. in case of missing data,
     * the method returns the special value {@link Float#NaN}.
     *
     * @param raster the raster
     * @param index  the index, must be computed using the {@link #computeIndex} method
     * @return either the re-sampled sample value or {@link Float#NaN}.
     * @throws Exception if a non-runtime error occurs, e.g I/O error
     */
    double resample(Raster raster, Index index) throws Exception;

    default int getKernelSize() {
        return 4;
    }

    /**
     * A raster is a rectangular grid which provides sample values at a given raster position x,y.
     */
    interface Raster {

        /**
         * Gets the raster's width.
         *
         * @return the raster's width
         */
        int getWidth();

        /**
         * Gets the raster's height.
         *
         * @return the raster's height
         */
        int getHeight();

        /**
         * Gets the sample value at the given raster position or {@link Float#NaN}.
         *
         * @param x the pixel's X-coordinate
         * @param y the pixel's Y-coordinate
         *          the sample value or {@link Double#NaN} if data is missing at the given raster position
         * @return false if one value is Double#NaN
         * @throws Exception if a non-runtime error occurs, e.g I/O error
         */
        boolean getSamples(final int[] x, final int[] y, final double[][] samples) throws Exception;
    }

    /**
     * An index is used to provide resampling information at a given raster position x,y.
     */
    final class Index {

        //used as archive to recompute the index for an other resampling method
        public double x;
        public double y;
        public int width;
        public int height;

        // the index fields
        public double i0;
        public double j0;
        public final double[] i;
        public final double[] j;
        public final double[] ki;
        public final double[] kj;

        /**
         * Creates a new index.
         *
         * @param m the maximum number of different pixel positions required to perform a resampling
         * @param n the maximum number of polynomial coefficients required to perform a resampling
         */
        public Index(int m, int n) {
            i = new double[m];
            j = new double[m];
            ki = new double[n];
            kj = new double[n];
        }

        public static double crop(double i, double max) {
            return (i < 0) ? 0 : (i > max) ? max : i;
        }
    }
}
