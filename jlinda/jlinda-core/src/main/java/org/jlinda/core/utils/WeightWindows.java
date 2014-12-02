package org.jlinda.core.utils;
import org.jblas.DoubleMatrix;

public class WeightWindows {

    private static int n;

    public static DoubleMatrix rect(final DoubleMatrix x) throws IllegalArgumentException {
        if (!x.isVector()) {
            System.err.println("myrect: only lying vectors.");
            throw new IllegalArgumentException();
        }

        return new DoubleMatrix(rect(x.toArray()));
    }

    public static double[] rect(final double[] x) {

        double[] rectWin = new double[x.length];

        for (int i = 0; i < x.length; ++i) {
            if (Math.abs(x[i]) <= 0.5) {
                rectWin[i] = 1.;
            }
        }

        return rectWin;
    }


    // w = .54 - .46*cos(2*pi*(0:M-1)'/(M-1));
    // see: http://www.mathworks.com/help/toolbox/signal/hamming.html
    public static double[] hamming(final int n) {
        double[] window = new double[n];
        for (int i = 0; i < n; i++) {
            window[i] = 0.54 - 0.46 * Math.cos(2 * Math.PI * ((double) i / (double) (n - 1)));
        }
        return window;
    }

    /**
     * myhamming
     * hamming window, lying vector
     * w = (a + (1.-a).*cos((2.*pi/fs).*fr)) .* myrect(fr./Br);
     * scale/shift filter by g(x)=f((x-xo)/s)
     * alpha==1 yields a myrect window
     * @param fr
     * @param br
     * @param fs
     * @param alpha
     * @return
     * @throws IllegalArgumentException
     */
    public static double[] hamming(final double[] fr, final double br, final double fs, final double alpha) throws IllegalArgumentException {

        if (alpha < 0.0 || alpha > 1.0) {
            System.err.println("myhamming: !alpha e{0..1}.");
            throw new IllegalArgumentException();
        }

        if (br > fs) {
            System.err.println("myhamming: RBW>RSR.");
            throw new IllegalArgumentException("Hamming weighting: RBW>RSR");
        }

        double[] hamWin = new double[fr.length];
        for (int i = 0; i < fr.length; ++i) {
            if (Math.abs(fr[i]/br) < 0.5) {   // rect window
                hamWin[i] = (alpha + (1 - alpha) * Math.cos((2 * Math.PI / fs) * fr[i]));
            }
        }
        return hamWin;
    }

    public static DoubleMatrix hamming(final DoubleMatrix fr, final double br, final double fs, final double alpha) throws IllegalArgumentException {
        if (!fr.isVector()) {
            System.err.println("myhamming: only lying vectors.");
            throw new IllegalArgumentException();
        }
        return new DoubleMatrix(hamming(fr.toArray(), br, fs, alpha));
    }

    public static double[] inverseHamming(final double[] hamming) {

        double[] invertHamming = new double[hamming.length];

        for (int i = 0; i < hamming.length; i++) {
            if (hamming[i] != 0) {
                invertHamming[i] = 1. / hamming[i];
            }
        }
        return invertHamming;
    }


    public static double[] inverseHamming(final double[] fr, final double br, final double fs, final double alpha) throws IllegalArgumentException {
        final double[] hamming = hamming(fr, br, fs, alpha);
        return inverseHamming(hamming);
    }


    public static DoubleMatrix inverseHamming(final DoubleMatrix fr, final double br, final double fs, final double alpha) throws IllegalArgumentException {
        if (!fr.isVector()) {
            System.err.println("myhamming: only lying vectors.");
            throw new IllegalArgumentException();
        }
//        return new DoubleMatrix(inverseHamming(fr.toArray(), br, fs, alpha));
        return new DoubleMatrix(inverseHamming(fr.data, br, fs, alpha));
    }


}
