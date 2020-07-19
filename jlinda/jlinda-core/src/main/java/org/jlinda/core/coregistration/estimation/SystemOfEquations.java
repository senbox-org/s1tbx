package org.jlinda.core.coregistration.estimation;

import org.apache.commons.math3.util.FastMath;
import org.jlinda.core.utils.PolyUtils;
import java.util.List;

public class SystemOfEquations {

    /**
     * Construct design matrix for fitting of 2d polynomial to input data - Java array implementation.
     * Approximately 10% slower then Jblas implementation
     *
     * @param line   vector of coordinates of points in azimuth direction
     * @param pixel  vactor of coordinates of points in range direction
     * @param degree polynomial degree
     * @return Design Matrix
     */
    public static double[][] constructDesignMatrix_loop(final List<Double> line, final List<Double> pixel, final int degree) {

        final int nObs = line.size();
        final int nUnkn = PolyUtils.numberOfCoefficients(degree);
        final double[][] A = new double[nObs][nUnkn];

        //logger.info("Setting up design matrix for LS adjustment");
        // Set up designmatrix
        for (int i = 0; i < nObs; i++) {
            final double linei = line.get(i);
            final double pixeli = pixel.get(i);
            int index = 0;
            for (int p = 0; p <= degree; p++) {
                for (int q = 0; q <= p; q++) {
                    A[i][index] = FastMath.pow(linei, p - q) * FastMath.pow(pixeli, q);
                    index++;
                }
            }
        }
        return A;
    }
}
