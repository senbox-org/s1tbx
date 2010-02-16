package org.esa.beam.framework.dataop.resamp;

/**
 * An interface used to implement different resampling strategies.
 *
 * @author Norman Fomferra
 * @author Norman Fomferra (norman.fomferra@brockmann-consult.de)
 * @version $Revision$ $Date$
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
    void computeIndex(float x, float y, int width, int height, Index index);

    /**
     * Performs the actual resampling operation.
     * If a sample value could not be computed at the given index, e.g. in case of missing data,
     * the method returns the special value {@link Float#NaN}.
     *
     * @param raster the raster
     * @param index  the index, must be computed using the {@link #computeIndex} method
     *
     * @return either the re-sampled sample value or {@link Float#NaN}.
     *
     * @throws Exception if a non-runtime error occurs, e.g I/O error
     */
    float resample(Raster raster, Index index) throws Exception;

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
         *
         * @return the sample value or {@link Float#NaN} if data is missing at the given raster position
         *
         * @throws Exception if a non-runtime error occurs, e.g I/O error
         */
        float getSample(int x, int y) throws Exception;
    }

    /**
     * An index is used to provide resampling information at a given raster position x,y.
     */
    class Index {

        //used as archieve to recompute the index for an other resampling method
        public float x;
        public float y;
        public int width;
        public int height;

        // the index fields
        public int i0;
        public int j0;
        public final int[] i;
        public final int[] j;
        public final float[] ki;
        public final float[] kj;

        /**
         * Creates a new index.
         *
         * @param m the maximum number of different pixel positions required to perform a resampling
         * @param n the maximum number of polynomial coefficients required to perform a resampling
         */
        public Index(int m, int n) {
            i = new int[m];
            j = new int[m];
            ki = new float[n];
            kj = new float[n];
        }

        public static int crop(int i, int max) {
            return (i < 0) ? 0 : (i > max) ? max : i;
        }
    }
}
