package org.esa.s1tbx.gpf;

import Jama.Matrix;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.framework.datamodel.TiePointGrid;

/**
 * Created by lveci on 06/08/2014.
 */
public class TiePointInterpolator {

    private final TiePointGrid tpg;
    private final float[] tiePoints;
    private final int rasterWidth;

    private double[][] quadraticInterpCoeffs = null; // 2 order quadratic polynomial coefficients
    private double[] biquadraticInterpCoeffs = null; // 2 order biquadratic polynomial coefficients

    public enum InterpMode {BILINEAR, QUADRATIC, BIQUADRATIC}

    public TiePointInterpolator(final TiePointGrid tpg) {
        this.tpg = tpg;
        this.tiePoints = tpg.getTiePoints();
        this.rasterWidth = tpg.getSceneRasterWidth();
    }

    /**=========================== Quadratic/Biquadratic Interpolations ================================**/
    /**
     * Compute polynomial coefficients for quadratic interpolation. For each tie point record, 3 coefficients
     * are computed. The 3 coefficients are saved in a row in _quadraticInterpCoeffs as {a0, a1, a2}. The
     * quadratic polynomial is given as f(x) = a0 + a1*x + a2*x^2.
     */
    private synchronized void computeQuadraticInterpCoeffs() {

        if (quadraticInterpCoeffs != null) return;

        final int numCoeff = 3;
        final int width = tpg.getRasterWidth();
        final int height = tpg.getRasterHeight();

        final double[][] sampleIndexArray = new double[width][numCoeff];
        for (int c = 0; c < width; c++) {
            final int x = (int) (tpg.getOffsetX() + c * tpg.getSubSamplingX());
            sampleIndexArray[c][0] = 1.0;
            sampleIndexArray[c][1] = (double) (x);
            sampleIndexArray[c][2] = (double) (x * x);
        }
        final Matrix A = new Matrix(sampleIndexArray);

        quadraticInterpCoeffs = new double[height][numCoeff];
        final double[] tiePointArray = new double[width];
        for (int r = 0; r < height; r++) {
            final int rwidth = r * width;
            for (int c = 0; c < width; c++) {
                tiePointArray[c] = (double) (tiePoints[rwidth + c]);
            }
            final Matrix b = new Matrix(tiePointArray, width);
            final Matrix x = A.solve(b);
            quadraticInterpCoeffs[r] = x.getColumnPackedCopy();
        }
    }

    /**
     * Compute polynomial coefficients for biquadratic interpolation. The 6 coefficients are given as
     * _biquadraticInterpCoeffs = {a0, a1, a2, a3, a4, a5} and the biquadratic polynomial is given as
     * f(x,y) = a0 + a1*x + a2*y + a3*x^2 + a4*y*x + a5*y^2.
     */
    private synchronized void computeBiquadraticInterpCoeffs() {

        if (biquadraticInterpCoeffs != null) return;

        final int numCoeff = 6;
        final int w = tpg.getRasterWidth();
        final int h = tpg.getRasterHeight();
        final int n = w * h;

        // prepare matrix A
        final double[][] sampleIndexArray = new double[n][numCoeff];
        for (int i = 0; i < h; i++) {
            final int y = (int) (i * tpg.getSubSamplingY());
            final double yy = y * y;
            final int iw = i * w;
            for (int j = 0; j < w; j++) {
                final int k = iw + j;
                final int x = (int) (j * tpg.getSubSamplingX());
                sampleIndexArray[k][0] = 1.0;
                sampleIndexArray[k][1] = (double) (x);
                sampleIndexArray[k][2] = (double) (y);
                sampleIndexArray[k][3] = (double) (x * x);
                sampleIndexArray[k][4] = (double) (y * x);
                sampleIndexArray[k][5] = yy;
            }
        }
        final Matrix A = new Matrix(sampleIndexArray);

        // prepare matrix b
        final float[] tiePoints = tpg.getTiePoints();

        final double[] tiePointArray = new double[n];
        System.arraycopy(tiePoints, 0, tiePointArray, 0, n);
        final Matrix b = new Matrix(tiePointArray, n);

        // compute coefficients
        final Matrix x = A.solve(b);
        biquadraticInterpCoeffs = x.getColumnPackedCopy();
    }


    /**
     * Computes the interpolated sample for the pixel located at (x,y) given as floating point co-ordinates. <p/>
     * <p>
     * If the pixel co-odinates given by (x,y) are not covered by this tie-point grid, the method extrapolates.
     *
     * @param x            The X co-ordinate of the pixel location, given in the pixel co-ordinates of the data product to which
     *                     this tie-pint grid belongs to.
     * @param y            The Y co-ordinate of the pixel location, given in the pixel co-ordinates of the data product to which
     *                     this tie-pint grid belongs to.
     * @param interpMethod String indicating the interpolation method.
     * @return The interpolated sample value.
     * @throws ArrayIndexOutOfBoundsException if the co-ordinates are not in bounds
     */
    public double getPixelDouble(double x, double y, InterpMode interpMethod) {

        if (interpMethod == InterpMode.BILINEAR) {

            return tpg.getPixelDouble(x, y);

        } else if (interpMethod == InterpMode.QUADRATIC) {

            if (quadraticInterpCoeffs == null) {
                computeQuadraticInterpCoeffs();
            }
            int r = (int) ((y - tpg.getOffsetY()) / tpg.getSubSamplingY());
            if (r >= quadraticInterpCoeffs.length) {
                r = quadraticInterpCoeffs.length - 1;
            }
            return quadraticInterpCoeffs[r][0] + quadraticInterpCoeffs[r][1] * x + quadraticInterpCoeffs[r][2] * x * x;

        } else if (interpMethod == InterpMode.BIQUADRATIC) {

            if (biquadraticInterpCoeffs == null) {
                computeBiquadraticInterpCoeffs();
            }
            return biquadraticInterpCoeffs[0] + biquadraticInterpCoeffs[1] * x + biquadraticInterpCoeffs[2] * y
                    + biquadraticInterpCoeffs[3] * x * x + biquadraticInterpCoeffs[4] * x * y + biquadraticInterpCoeffs[5] * y * y;

        } else {
            throw new IllegalArgumentException("unsupported interpolation method");
        }
    }

    /**
     * Retrieves an array of tie point data interpolated to the product width and height as float array. If the given
     * array is <code>null</code> a new one is created and returned.
     *
     * @param x0           the x coordinate of the array to be read
     * @param y0           the y coordinate of the array to be read
     * @param w            the width of the array to be read
     * @param h            the height of the array to be read
     * @param pixels       the float array to be filled with data
     * @param pm           a monitor to inform the user about progress
     * @param interpMethod String indicating the interpolation method.
     * @return Array of interpolated sample values.
     * @throws IllegalArgumentException if the length of the given array is less than <code>w*h</code>.
     */
    public double[] getPixels(int x0, int y0, int w, int h, double[] pixels, ProgressMonitor pm, InterpMode interpMethod) {

        pixels = ensureMinLengthArray(pixels, w * h);
        if (interpMethod == InterpMode.BILINEAR) {

            return tpg.getPixels(x0, y0, w, h, pixels, pm);

        } else if (interpMethod == InterpMode.QUADRATIC || interpMethod == InterpMode.BIQUADRATIC) {

            int k = 0;
            final int maxY = y0 + h;
            final int maxX = x0 + w;
            for (int y = y0; y < maxY; y++) {
                for (int x = x0; x < maxX; x++) {
                    pixels[k++] = getPixelDouble(x, y, interpMethod);
                }
            }
            return pixels;

        } else {
            throw new IllegalArgumentException("unsupported interpolation method");
        }
    }

    protected static double[] ensureMinLengthArray(double[] array, int length) {
        if (array == null) {
            return new double[length];
        }
        if (array.length < length) {
            throw new IllegalArgumentException("The length of the given array is less than " + length);
        }
        return array;
    }
}
