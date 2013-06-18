package org.jlinda.core.coregistration;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.jblas.ComplexDouble;
import org.jblas.ComplexDoubleMatrix;
import org.jblas.DoubleMatrix;
import org.jlinda.core.Constants;
import org.jlinda.core.SLCImage;
import org.slf4j.LoggerFactory;

public class LUT {

    private static final Logger logger = (ch.qos.logback.classic.Logger) LoggerFactory.getLogger(LUT.class);


    public static final String NEAREST_NEIGHBOR = "Nearest-neighbor interpolation";
    public static final String BILINEAR = "Bilinear interpolation";
    public static final String BICUBIC = "Bicubic interpolation";
    public static final String BICUBIC2 = "Bicubic2 interpolation";
    public static final String RECT = "Step function (nearest-neighbor)";
    public static final String TRI = "Linear interpolation";
    public static final String CC4P = "Cubic convolution (4 points)";
    public static final String CC6P = "Cubic convolution (6 points)";
    public static final String TS6P = "Truncated sinc (6 points)";
    public static final String TS8P = "Truncated sinc (8 points)";
    public static final String TS16P = "Truncated sinc (16 points)";


    private enum ResampleKernels {
        RECT, TRI,
        TS6P, TS8P, TS16P,
        CC4P, CC6P,
        RS_KNAB4P, RS_KNAB6P, RS_KNAB8P, RS_KNAB10P, RS_KNAB16P,
        RS_RC6P, RS_RC12P
    }

    private static final int INTERVAL = 127;            // precision: 1./interval [pixel]
    private final int nInterval = INTERVAL + 1;   // size of lookup table
    private final double dx = 1.0 / INTERVAL;       // interval look up table

    private DoubleMatrix kernel;
    private DoubleMatrix axis;

    private String method;
    private int kernelLength;

    public static int getInterval() {
        return INTERVAL;
    }

    public LUT(String method, int kernelLength) {
        this.method = method;
        this.kernelLength = kernelLength;

        logger.setLevel(Level.TRACE);
        logger.trace("Start LUT [development code]");

    }

    public DoubleMatrix getKernel() {
        return kernel;
    }

    public DoubleMatrix getAxis() {
        return axis;
    }

    // construct kernel axis for numberOfKernelPoints
    // eg. kernelLength = 4  --->  xKernelAxis = [-1 0 1 2]
    private double[] defineAxis(final int kernelLength) {
        final double[] xKernelAxis = new double[kernelLength];
        for (int i = 0; i < kernelLength; ++i) {
            xKernelAxis[i] = 1.0d - (kernelLength / 2) + i;
        }
        return xKernelAxis;
    }

    /**
    * Shift interpolation kernel as function of Doppler centroid frequency
     */
    public ComplexDoubleMatrix shiftKernel(SLCImage metaData, double line, double pixel, int noKernelPoints) {

        // ...to shift spectrum of convolution kernel to fDC of data, multiply in the space domain with
        // a phase trend of -2pi*(time)*fDC/PRF to shift back (no need here) use +fDC

        int kernelLine = (int) (line * INTERVAL + 0.5); // lookup table index

        // working with complex kernel
        ComplexDoubleMatrix complexKernelLine = new ComplexDoubleMatrix(kernel.getRow(kernelLine));
        final DoubleMatrix pntAxisRow = axis.getRow(kernelLine);

        // get Doppler centroid and calculate shift trend
        double trend = 2.0 * Constants.PI * metaData.doppler.pix2fdc(pixel) / metaData.getPRF();

        for (int i = 0; i < noKernelPoints; ++i) {

            // Modify kernel, shift spectrum to fDC
            double t = pntAxisRow.get(i) * trend;
            complexKernelLine.put(i, complexKernelLine.get(i).mul(new ComplexDouble(Math.cos(t), (-1) * Math.sin(t)))); // note '-' (see manual)
        }

        return complexKernelLine;

    }

    public void constructLUT() {

        double[] kernelAxis = defineAxis(kernelLength);

        // temp matrices
        DoubleMatrix kernelTemp;
        DoubleMatrix axisTemp;

        // initialization
        kernel = new DoubleMatrix(nInterval,kernelLength);
        axis = new DoubleMatrix(nInterval, kernelLength);

        for (int i = 0; i < nInterval; i++) {
            //                switch (ResampleKernels.valueOf(resampleMethod.toUpperCase())) {
            //                    case RECT:
            //                        kernelTemp = new DoubleMatrix(rect(kernelAxis));
            //                        kernel.putRow(i,kernelTemp);
            //                        break;
            //                    case TRI:
            //            ......
            if (method.equals(RECT)) {
                kernelTemp = new DoubleMatrix(rect(kernelAxis));
                kernelTemp.divi(kernelTemp.sum()); // normalize
                kernel.putRow(i, kernelTemp);
            } else if (method.equals(TRI)) {
                kernelTemp = new DoubleMatrix(tri(kernelAxis));
                kernelTemp.divi(kernelTemp.sum()); // normalize
                kernel.putRow(i, kernelTemp);
            } else if (method.equals(TS6P)) {
                kernelTemp = new DoubleMatrix(ts6(kernelAxis));
                kernelTemp.divi(kernelTemp.sum()); // normalize
                kernel.putRow(i, kernelTemp);
            } else if (method.equals(TS8P)) {
                kernelTemp = new DoubleMatrix(ts8(kernelAxis));
                kernelTemp.divi(kernelTemp.sum()); // normalize
                kernel.putRow(i, kernelTemp);
            } else if (method.equals(TS16P)) {
                kernelTemp = new DoubleMatrix(ts16(kernelAxis));
                kernelTemp.divi(kernelTemp.sum()); // normalize
                kernel.putRow(i, kernelTemp);
            } else if (method.equals(CC4P)) {
                kernelTemp = new DoubleMatrix(cc4(kernelAxis));
                kernelTemp.divi(kernelTemp.sum()); // normalize
                kernel.putRow(i, kernelTemp);
            } else if (method.equals(CC6P)) {
                kernelTemp = new DoubleMatrix(cc6(kernelAxis));
                kernelTemp.divi(kernelTemp.sum()); // normalize
                kernel.putRow(i, kernelTemp);
            }
            axisTemp = new DoubleMatrix(kernelAxis);
            axis.putRow(i, axisTemp);
            kernelAxis = axisTemp.sub(dx).toArray();
        }

        // normalization
        kernel.divColumnVector(kernel.rowSums());

    }

    /**
     * Log kernels to check sum, etc.
     */
    public void overviewOfLut() {

        logger.debug("Overview of LUT for interpolation kernel follows:");
        logger.debug("-------------------------------------------------");

        for (int i = 0; i < nInterval; ++i) {

            // math
            DoubleMatrix row = kernel.getRow(i);
            double sum = row.sum();
//            row.divi(sum);

            // logger
            logger.debug(axis.getRow(i).toString());
            logger.debug("Normalized kernel by dividing LUT elements by sum:");
//            logger.debug(row.toString() + "( sum   : " + sum + ")");
            logger.debug("{} ( {} : sum )", row.toString(), sum);

        }
        logger.debug("Resample: normalized lookup table created (kernel and axis).");
    }


    // methods for interpolators
    // -------------------------------------------
    // cc4: cubic convolution 4 points
    // input:
    //   - x-axis
    // output:
    //  - y=f(x); function evaluated at x
    public double[] cc4(final double[] x) {

        final double alpha = -1.0;
        final double[] y = new double[x.length];

        for (int i = 0; i < y.length; i++) {
            final double xx2 = Math.sqrt(x[i]);
            final double xx = Math.sqrt(xx2);
            if (xx < 1)
                y[i] = (alpha + 2) * xx2 * xx - (alpha + 3) * xx2 + 1;
            else if (xx < 2)
                y[i] = alpha * xx2 * xx - 5 * alpha * xx2 + 8 * alpha * xx - 4 * alpha;
            else
                y[i] = 0;
        }

        return y;

    } // END cc4


    // cc6: cubic convolution 4 points
    // input:
    //   - x-axis
    // output:
    //  - y=f(x); function evaluated at x

    private double[] cc6(final double[] x) {

        final double alpha = -0.5;
        final double beta = 0.5;
        final double[] y = new double[x.length];

        for (int i = 0; i < y.length; i++) {
            final double xx2 = Math.pow(x[i], 2);
            final double xx = Math.sqrt(xx2);
            if (xx < 1)
                y[i] = (alpha - beta + 2) * xx2 * xx - (alpha - beta + 3) * xx2 + 1;
                //y[i] = (alpha+beta+2)*xx2*xx - (alpha+beta+3)*xx2 + 1; // wrong in reference paper??
            else if (xx < 2)
                y[i] = alpha * xx2 * xx - (5 * alpha - beta) * xx2
                        + (8 * alpha - 3 * beta) * xx - (4 * alpha - 2 * beta);
            else if (xx < 3)
                y[i] = beta * xx2 * xx - 8 * beta * xx2 + 21 * beta * xx - 18 * beta;
            else
                y[i] = 0.0;
        }

        return y;

    } // END cc6

    // ts6: truncated sinc 6 points
    // input:
    //   - x-axis
    // output:
    //  - y=f(x); function evaluated at x

    private double[] ts6(final double[] x) {

        final double[] y = new double[x.length];

        for (int i = 0; i < y.length; i++)
            y[i] = sinc(x[i]) * rect(x[i] / 6.0);

        return y;

    } // END ts6

    // ts8: truncated sinc 8 points
    // input:
    //   - x-axis
    // output:
    //  - y=f(x); function evaluated at x

    private double[] ts8(final double[] x) {

        final double[] y = new double[x.length];

        for (int i = 0; i < y.length; i++)
            y[i] = sinc(x[i]) * rect(x[i] / 8.0);

        return y;

    } // END ts8

    // ts16: truncated sinc 6 points
    // input:
    //   - x-axis
    // output:
    //  - y=f(x); function evaluated at x

    private double[] ts16(final double[] x) {

        double[] y = new double[x.length];

        for (int i = 0; i < y.length; i++)
            y[i] = sinc(x[i]) * rect(x[i] / 16.0);

        return y;

    } // END cc6

    // rect :: rect function for matrix (stepping function?)
    // input:
    //    - x-axis
    // output:
    //    - y=f(x); function evaluated at x

    private double[] rect(final double[] x) {
        final double[] y = new double[x.length];
        for (int i = 0; i < y.length; i++) {
            y[i] = rect(x[i]);
        }
        return y;
    } // END rect

    // tri ::  tri function for matrix (piecewize linear?, triangle)
    // input:
    //    - x-axis
    // output:
    //    - y=f(x); function evaluated at x

    private double[] tri(final double[] x) {
        final double[] y = new double[x.length];
        for (int i = 0; i < y.length; i++) {
            y[i] = tri(x[i]);
        }
        return y;
    } // END tri

        /*
              knab :: KNAB window of N points, oversampling factor CHI
                   defined by: Migliaccio IEEE letters vol41,no5, pp1105,1110, 2003
                   k = sinc(x).*(cosh((pi*v*L/2)*sqrt(1-(2.*x./L).^2))/cosh(pi*v*L/2));
                    input:
                       - x-axis
                       - oversampling factor of bandlimited sigal CHI
                       - N points of kernel size
                   output:
                       - y=f(x); function evaluated at x
        */

    private double[] knab(final double[] x, final double CHI, final int N) {
        final double[] y = new double[x.length];
        final double v = 1.0 - 1.0 / CHI;
        final double vv = Math.PI * v * N / 2.0;
        final double coshvv = Math.cosh(vv);

        for (int i = 0; i < y.length; i++) {
            y[i] = sinc(x[i]) * Math.cosh(vv * Math.sqrt(1.0 - Math.pow(2.0 * x[i] / (double) N, 2))) / coshvv;
        }
        return y;

    } // END knab

        /*
       rc_kernel: Raised Cosine window of N points, oversampling factor CHI
                  defined by: Cho, Kong and Kim, J.Elektromagn.Waves and appl
                                    vol19, no.1, pp, 129-135, 2005;
       claimed to be best, 0.9999 for 6 points kernel.
       k(x) = sinc(x).*[cos(v*pi*x)/(1-4*v^2*x^2)]*rect(x/L)
             where v = 1-B/fs = 1-1/Chi (roll-off factor; ERS: 15.55/18.96)
             L = 6 (window size)
        input:
                - x-axis
                - oversampling factor of bandlimited sigal CHI
                - N points of kernel size
       output:
                - y=f(x); function evaluated at x
        */

    private double[] rc_kernel(final double[] x, final double CHI, final int N) {

        final double[] y = new double[x.length];
        final double v = 1.0 - 1.0 / CHI;// alpha in paper cho05
        final double v2 = 2.0 * v;
        final double vPI = v * Math.PI;

        for (int i = 0; i < y.length; i++) {
            y[i] = sinc(x[i]) * rect(x[i] / N) *
                    Math.cos(vPI * x[i]) / (1.0 - Math.pow(v2 * x[i], 2));
        }
        return y;
    } // END rc_kernel

    private double sinc(final double x) {
        return ((x == 0) ? 1 : Math.sin(Math.PI * x) / (Math.PI * x));
    }

    private double rect(final double x) {
        double ans = 0.0;
        if (x < 0.5 && x > -0.5) {
            ans = 1;
        } else if (x == 0.5 || x == -0.5) {
            ans = 0.5;
        }
        return ans;
    }

    private double tri(final double x) {
        double ans = 0.0;
        if (x < 1.0 && x > -1.0) {
            ans = (x < 0) ? 1 + x : 1 - x;
        }
        return ans;
    }

}