package org.jdoris.core.utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.jblas.DoubleMatrix;
import org.jblas.Solve;
import org.slf4j.LoggerFactory;

import static org.jblas.MatrixFunctions.pow;

public class PolyUtils {

    // TODO: Major clean-up and open sourcing of Polynomial and PolyFit classess from ppolabs.commons

    public static final Logger logger = (Logger) LoggerFactory.getLogger(PolyUtils.class);

    public static double normalize2(double data, final int min, final int max) {
        data -= (0.5 * (min + max));
        data /= (0.25 * (max - min));
        return data;
    }

    public static double normalize2(double data, final double min, final double max) {
        data -= (0.5 * (min + max));
        data /= (0.25 * (max - min));
        return data;
    }

    public static DoubleMatrix normalize(DoubleMatrix t) {
        return t.sub(t.get(t.length / 2)).div(10.0);
    }

    public static int degreeFromCoefficients(int numOfCoefficients) {
//        return (int) (0.5 * (-1 + (int) (Math.sqrt((double) (1 + 8 * numOfCoefficients))))) - 1;
        return (int) (0.5 * (-1 + (int) (Math.sqrt((double) (1 + 8 * numOfCoefficients))))) - 1;
    }

    public static int numberOfCoefficients(final int degree) {
        return (int) (0.5 * (Math.pow(degree + 1, 2) + degree + 1));
    }

    /**
     * polyfit
     * <p/>
     * Compute coefficients of x=a0+a1*t+a2*t^2+a3*t3 polynomial
     * for orbit interpolation.  Do this to facilitate a method
     * in case only a few datapoints are given.
     * Data t is normalized approximately [-x,x], then polynomial
     * coefficients are computed.  For poly_val this is repeated
     * see getxyz, etc.
     * <p/>
     * input:
     * - matrix by getdata with time and position info
     * output:
     * - matrix with coeff.
     * (input for interp. routines)
     */
    public static double[] polyFitNormalized(DoubleMatrix t, DoubleMatrix y, final int degree) throws IllegalArgumentException {
        return polyFit(normalize(t), y, degree);
    }

    public static double[] polyFit2D(final DoubleMatrix x, final DoubleMatrix y, final DoubleMatrix z, final int degree) throws IllegalArgumentException {

        logger.setLevel(Level.INFO);

        if (x.length != y.length || !x.isVector() || !y.isVector()) {
            logger.error("polyfit: require same size vectors.");
            throw new IllegalArgumentException("polyfit: require same size vectors.");
        }

        final int numOfObs = x.length;
        final int numOfUnkn = numberOfCoefficients(degree) + 1;

        DoubleMatrix A = new DoubleMatrix(numOfObs, numOfUnkn); // designmatrix

        DoubleMatrix mul;

        /** Set up design-matrix */
        for (int p = 0; p <= degree; p++) {
            for (int q = 0; q <= p; q++) {
                mul = pow(y, (p - q)).mul(pow(x, q));
                if (q == 0 && p == 0) {
                    A = mul;
                } else {
                    A = DoubleMatrix.concatHorizontally(A, mul);
                }
            }
        }

        // Fit polynomial
        logger.debug("Solving lin. system of equations with Cholesky.");
        DoubleMatrix N = A.transpose().mmul(A);
        DoubleMatrix rhs = A.transpose().mmul(z);

        // solution seems to be OK up to 10^-09!
        rhs = Solve.solveSymmetric(N, rhs);

        DoubleMatrix Qx_hat = Solve.solveSymmetric(N, DoubleMatrix.eye(N.getRows()));

        double maxDeviation = (N.mmul(Qx_hat).sub(DoubleMatrix.eye(Qx_hat.rows))).normmax();
        logger.debug("polyfit orbit: max(abs(N*inv(N)-I)) = " + maxDeviation);

        // ___ report max error... (seems sometimes this can be extremely large) ___
        if (maxDeviation > 1e-6) {
            logger.warn("polyfit orbit: max(abs(N*inv(N)-I)) = {}", maxDeviation);
            logger.warn("polyfit orbit interpolation unstable!");
        }

        // work out residuals
        DoubleMatrix y_hat = A.mmul(rhs);
        DoubleMatrix e_hat = y.sub(y_hat);

        if (e_hat.normmax() > 0.02) {
            logger.warn("WARNING: Max. polyFit2D approximation error at datapoints (x,y,or z?): {}", e_hat.normmax());
        } else {
            logger.info("Max. polyFit2D approximation error at datapoints (x,y,or z?): {}", e_hat.normmax());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("REPORTING POLYFIT LEAST SQUARES ERRORS");
            logger.debug(" time \t\t\t y \t\t\t yhat  \t\t\t ehat");
            for (int i = 0; i < numOfObs; i++) {
                logger.debug(" (" + x.get(i) + "," + y.get(i) + ") :" + "\t" + y.get(i) + "\t" + y_hat.get(i) + "\t" + e_hat.get(i));
            }
        }
        return rhs.toArray();
    }

    public static double[] polyFit(DoubleMatrix t, DoubleMatrix y, final int degree) throws IllegalArgumentException {

        logger.setLevel(Level.INFO);

        if (t.length != y.length || !t.isVector() || !y.isVector()) {
            logger.error("polyfit: require same size vectors.");
            throw new IllegalArgumentException("polyfit: require same size vectors.");
        }

        // Normalize _posting_ for numerical reasons
        final int numOfPoints = t.length;

        // Check redundancy
        final int numOfUnknowns = degree + 1;
        logger.debug("Degree of interpolating polynomial: {}", degree);
        logger.debug("Number of unknowns: {}", numOfUnknowns);
        logger.debug("Number of data points: {}", numOfPoints);

        if (numOfPoints < numOfUnknowns) {
            logger.error("Number of points is smaller than parameters solved for.");
            throw new IllegalArgumentException("Number of points is smaller than parameters solved for.");
        }

        // Set up system of equations to solve coeff :: Design matrix
        logger.debug("Setting up linear system of equations");
        DoubleMatrix A = new DoubleMatrix(numOfPoints, numOfUnknowns);
        // work with columns
        for (int j = 0; j <= degree; j++) {
            A.putColumn(j, pow(t, j));
        }

        // Fit polynomial through computed vector of phases
        logger.debug("Solving lin. system of equations with Cholesky.");

        DoubleMatrix N = A.transpose().mmul(A);
        DoubleMatrix rhs = A.transpose().mmul(y);

        // solution seems to be OK up to 10^-09!
        DoubleMatrix x = Solve.solveSymmetric(N, rhs);
        DoubleMatrix Qx_hat = Solve.solveSymmetric(N, DoubleMatrix.eye(N.getRows()));

        double maxDeviation = (N.mmul(Qx_hat).sub(DoubleMatrix.eye(Qx_hat.rows))).normmax();
        logger.debug("polyfit orbit: max(abs(N*inv(N)-I)) = " + maxDeviation);

        // ___ report max error... (seems sometimes this can be extremely large) ___
        if (maxDeviation > 1e-6) {
            logger.warn("polyfit orbit: max(abs(N*inv(N)-I)) = {}", maxDeviation);
            logger.warn("polyfit orbit interpolation unstable!");
        }

        // work out residuals
        DoubleMatrix y_hat = A.mmul(x);
        DoubleMatrix e_hat = y.sub(y_hat);

        // 0.05 is already 1 wavelength! (?)
        if (e_hat.normmax() > 0.02) {
            logger.warn("WARNING: Max. approximation error at datapoints (x,y,or z?): {}", e_hat.normmax());
        } else {
            logger.debug("Max. approximation error at datapoints (x,y,or z?): {}", e_hat.normmax());
        }

        if (logger.isDebugEnabled()) {
            logger.debug("REPORTING POLYFIT LEAST SQUARES ERRORS");
            logger.debug(" time \t\t\t y \t\t\t yhat  \t\t\t ehat");
            for (int i = 0; i < numOfPoints; i++) {
                logger.debug(" " + t.get(i) + "\t" + y.get(i) + "\t" + y_hat.get(i) + "\t" + e_hat.get(i));
            }

            for (int i = 0; i < numOfPoints - 1; i++) {
                // ___ check if dt is constant, not necessary for me, but may ___
                // ___ signal error in header data of SLC image ___
                double dt = t.get(i + 1) - t.get(i);
                logger.debug("Time step between point " + i + 1 + " and " + i + "= " + dt);

                if (Math.abs(dt - (t.get(1) - t.get(0))) > 0.001)// 1ms of difference we allow...
                    logger.warn("WARNING: Orbit: data does not have equidistant time interval?");
            }
        }
        return x.toArray();
    }

    public static double polyVal1D(double x, double[] coeffs) {
        double sum = 0.0;
        for (int d = coeffs.length - 1; d >= 0; --d) {
            sum *= x;
            sum += coeffs[d];
        }
        return sum;
    }

    public static double[][] polyval(final double[] x, final double[] y, final double coeff[], int degree) {

        if (degree < -1) {
            logger.warn("polyValGrid: degree < -1 ????");
        }

/*
        if (x.length > y.length) {
            logger.warn("polValGrid: x larger than y, while optimized for y larger x");
        }
*/

        if (degree == -1) {
            degree = degreeFromCoefficients(coeff.length);
        }

        // evaluate polynomial //
        double[][] result = new double[x.length][y.length];
        int i;
        int j;
        double c00, c10, c01, c20, c11, c02, c30, c21, c12, c03, c40, c31, c22, c13, c04, c50, c41, c32, c23, c14, c05;

        int columns = result[0].length;
        int rows = result.length;
        switch (degree) {
            case 0:
                result[0][0] = coeff[0];
                break;
            case 1:
                c00 = coeff[0];
                c10 = coeff[1];
                c01 = coeff[2];
                for (j = 0; j < columns; j++) {
                    double c00pc01y1 = c00 + c01 * y[j];
                    for (i = 0; i < rows; i++) {
                        result[i][j] = c00pc01y1 + c10 * x[i];
                    }
                }
                break;
            case 2:
                c00 = coeff[0];
                c10 = coeff[1];
                c01 = coeff[2];
                c20 = coeff[3];
                c11 = coeff[4];
                c02 = coeff[5];
                for (j = 0; j < columns; j++) {
                    double y1 = y[j];
                    double c00pc01y1 = c00 + c01 * y1;
                    double c02y2 = c02 * Math.pow(y1, 2);
                    double c11y1 = c11 * y1;
                    for (i = 0; i < rows; i++) {
                        double x1 = x[i];
                        result[i][j] = c00pc01y1
                                + c10 * x1
                                + c20 * Math.pow(x1, 2)
                                + c11y1 * x1
                                + c02y2;
                    }
                }
                break;
            case 3:
                c00 = coeff[0];
                c10 = coeff[1];
                c01 = coeff[2];
                c20 = coeff[3];
                c11 = coeff[4];
                c02 = coeff[5];
                c30 = coeff[6];
                c21 = coeff[7];
                c12 = coeff[8];
                c03 = coeff[9];
                for (j = 0; j < columns; j++) {
                    double y1 = y[j];
                    double y2 = Math.pow(y1, 2);
                    double c00pc01y1 = c00 + c01 * y1;
                    double c02y2 = c02 * y2;
                    double c11y1 = c11 * y1;
                    double c21y1 = c21 * y1;
                    double c12y2 = c12 * y2;
                    double c03y3 = c03 * y1 * y2;
                    for (i = 0; i < rows; i++) {
                        double x1 = x[i];
                        double x2 = Math.pow(x1, 2);
                        result[i][j] = c00pc01y1
                                + c10 * x1
                                + c20 * x2
                                + c11y1 * x1
                                + c02y2
                                + c30 * x1 * x2
                                + c21y1 * x2
                                + c12y2 * x1
                                + c03y3;
                    }
                }
                break;

            case 4:
                c00 = coeff[0];
                c10 = coeff[1];
                c01 = coeff[2];
                c20 = coeff[3];
                c11 = coeff[4];
                c02 = coeff[5];
                c30 = coeff[6];
                c21 = coeff[7];
                c12 = coeff[8];
                c03 = coeff[9];
                c40 = coeff[10];
                c31 = coeff[11];
                c22 = coeff[12];
                c13 = coeff[13];
                c04 = coeff[14];
                for (j = 0; j < columns; j++) {
                    double y1 = y[j];
                    double y2 = Math.pow(y1, 2);
                    double c00pc01y1 = c00 + c01 * y1;
                    double c02y2 = c02 * y2;
                    double c11y1 = c11 * y1;
                    double c21y1 = c21 * y1;
                    double c12y2 = c12 * y2;
                    double c03y3 = c03 * y1 * y2;
                    double c31y1 = c31 * y1;
                    double c22y2 = c22 * y2;
                    double c13y3 = c13 * y2 * y1;
                    double c04y4 = c04 * y2 * y2;
                    for (i = 0; i < rows; i++) {
                        double x1 = x[i];
                        double x2 = Math.pow(x1, 2);
                        result[i][j] = c00pc01y1
                                + c10 * x1
                                + c20 * x2
                                + c11y1 * x1
                                + c02y2
                                + c30 * x1 * x2
                                + c21y1 * x2
                                + c12y2 * x1
                                + c03y3
                                + c40 * x2 * x2
                                + c31y1 * x2 * x1
                                + c22y2 * x2
                                + c13y3 * x1
                                + c04y4;
                    }
                }
                break;
            case 5:
                c00 = coeff[0];
                c10 = coeff[1];
                c01 = coeff[2];
                c20 = coeff[3];
                c11 = coeff[4];
                c02 = coeff[5];
                c30 = coeff[6];
                c21 = coeff[7];
                c12 = coeff[8];
                c03 = coeff[9];
                c40 = coeff[10];
                c31 = coeff[11];
                c22 = coeff[12];
                c13 = coeff[13];
                c04 = coeff[14];
                c50 = coeff[15];
                c41 = coeff[16];
                c32 = coeff[17];
                c23 = coeff[18];
                c14 = coeff[19];
                c05 = coeff[20];
                for (j = 0; j < columns; j++) {
                    double y1 = y[j];
                    double y2 = Math.pow(y1, 2);
                    double y3 = y2 * y1;
                    double c00pc01y1 = c00 + c01 * y1;
                    double c02y2 = c02 * y2;
                    double c11y1 = c11 * y1;
                    double c21y1 = c21 * y1;
                    double c12y2 = c12 * y2;
                    double c03y3 = c03 * y3;
                    double c31y1 = c31 * y1;
                    double c22y2 = c22 * y2;
                    double c13y3 = c13 * y3;
                    double c04y4 = c04 * y2 * y2;
                    double c41y1 = c41 * y1;
                    double c32y2 = c32 * y2;
                    double c23y3 = c23 * y3;
                    double c14y4 = c14 * y2 * y2;
                    double c05y5 = c05 * y3 * y2;
                    for (i = 0; i < rows; i++) {
                        double x1 = x[i];
                        double x2 = Math.pow(x1, 2);
                        double x3 = x1 * x2;
                        result[i][j] = c00pc01y1
                                + c10 * x1
                                + c20 * x2
                                + c11y1 * x1
                                + c02y2
                                + c30 * x3
                                + c21y1 * x2
                                + c12y2 * x1
                                + c03y3
                                + c40 * x2 * x2
                                + c31y1 * x3
                                + c22y2 * x2
                                + c13y3 * x1
                                + c04y4
                                + c50 * x3 * x2
                                + c41y1 * x2 * x2
                                + c32y2 * x3
                                + c23y3 * x2
                                + c14y4 * x1
                                + c05y5;
                    }
                }
                break;

            default:
                for (j = 0; j < columns; j++) {
                    double yy = y[j];
                    for (i = 0; i < rows; i++) {
                        double xx = x[i];
                        result[i][j] = polyval(xx, yy, coeff, degree);
                    }
                }
        } // switch degree

        return result;
    }

    public static DoubleMatrix polyval(final DoubleMatrix x, final DoubleMatrix y, final DoubleMatrix coeff, int degree) {

        if (!x.isColumnVector()) {
            logger.warn("polyValGrid: require (x) standing data vectors!");
            throw new IllegalArgumentException("polyval functions require (x) standing data vectors!");
        }

        if (!y.isColumnVector()) {
            logger.warn("polyValGrid: require (y) standing data vectors!");
            throw new IllegalArgumentException("polyval functions require (y) standing data vectors!");
        }

        if (!coeff.isColumnVector()) {
            logger.warn("polyValGrid: require (coeff) standing data vectors!");
            throw new IllegalArgumentException("polyval functions require (coeff) standing data vectors!");
        }

        if (degree < -1) {
            logger.warn("polyValGrid: degree < -1 ????");
        }

/*
        if (x.length > y.length) {
            logger.warn("polValGrid: x larger than y, while optimized for y larger x");
        }
*/

        if (degree == -1) {
            degree = degreeFromCoefficients(coeff.length);
        }

        // evaluate polynomial //
        DoubleMatrix result = new DoubleMatrix(x.length, y.length);
        int i;
        int j;
        double c00, c10, c01, c20, c11, c02, c30, c21, c12, c03, c40, c31, c22, c13, c04, c50, c41, c32, c23, c14, c05;

        switch (degree) {
            case 0:
                result.put(0, 0, coeff.get(0, 0));
                break;
            case 1:
                c00 = coeff.get(0, 0);
                c10 = coeff.get(1, 0);
                c01 = coeff.get(2, 0);
                for (j = 0; j < result.columns; j++) {
                    double c00pc01y1 = c00 + c01 * y.get(j, 0);
                    for (i = 0; i < result.rows; i++) {
                        result.put(i, j, c00pc01y1 + c10 * x.get(i, 0));
                    }
                }
                break;
            case 2:
                c00 = coeff.get(0, 0);
                c10 = coeff.get(1, 0);
                c01 = coeff.get(2, 0);
                c20 = coeff.get(3, 0);
                c11 = coeff.get(4, 0);
                c02 = coeff.get(5, 0);
                for (j = 0; j < result.columns; j++) {
                    double y1 = y.get(j, 0);
                    double c00pc01y1 = c00 + c01 * y1;
                    double c02y2 = c02 * Math.pow(y1, 2);
                    double c11y1 = c11 * y1;
                    for (i = 0; i < result.rows; i++) {
                        double x1 = x.get(i, 0);
                        result.put(i, j, c00pc01y1
                                + c10 * x1
                                + c20 * Math.pow(x1, 2)
                                + c11y1 * x1
                                + c02y2);
                    }
                }
                break;
            case 3:
                c00 = coeff.get(0, 0);
                c10 = coeff.get(1, 0);
                c01 = coeff.get(2, 0);
                c20 = coeff.get(3, 0);
                c11 = coeff.get(4, 0);
                c02 = coeff.get(5, 0);
                c30 = coeff.get(6, 0);
                c21 = coeff.get(7, 0);
                c12 = coeff.get(8, 0);
                c03 = coeff.get(9, 0);
                for (j = 0; j < result.columns; j++) {
                    double y1 = y.get(j, 0);
                    double y2 = Math.pow(y1, 2);
                    double c00pc01y1 = c00 + c01 * y1;
                    double c02y2 = c02 * y2;
                    double c11y1 = c11 * y1;
                    double c21y1 = c21 * y1;
                    double c12y2 = c12 * y2;
                    double c03y3 = c03 * y1 * y2;
                    for (i = 0; i < result.rows; i++) {
                        double x1 = x.get(i, 0);
                        double x2 = Math.pow(x1, 2);
                        result.put(i, j, c00pc01y1
                                + c10 * x1
                                + c20 * x2
                                + c11y1 * x1
                                + c02y2
                                + c30 * x1 * x2
                                + c21y1 * x2
                                + c12y2 * x1
                                + c03y3);
                    }
                }
                break;

            case 4:
                c00 = coeff.get(0, 0);
                c10 = coeff.get(1, 0);
                c01 = coeff.get(2, 0);
                c20 = coeff.get(3, 0);
                c11 = coeff.get(4, 0);
                c02 = coeff.get(5, 0);
                c30 = coeff.get(6, 0);
                c21 = coeff.get(7, 0);
                c12 = coeff.get(8, 0);
                c03 = coeff.get(9, 0);
                c40 = coeff.get(10, 0);
                c31 = coeff.get(11, 0);
                c22 = coeff.get(12, 0);
                c13 = coeff.get(13, 0);
                c04 = coeff.get(14, 0);
                for (j = 0; j < result.columns; j++) {
                    double y1 = y.get(j, 0);
                    double y2 = Math.pow(y1, 2);
                    double c00pc01y1 = c00 + c01 * y1;
                    double c02y2 = c02 * y2;
                    double c11y1 = c11 * y1;
                    double c21y1 = c21 * y1;
                    double c12y2 = c12 * y2;
                    double c03y3 = c03 * y1 * y2;
                    double c31y1 = c31 * y1;
                    double c22y2 = c22 * y2;
                    double c13y3 = c13 * y2 * y1;
                    double c04y4 = c04 * y2 * y2;
                    for (i = 0; i < result.rows; i++) {
                        double x1 = x.get(i, 0);
                        double x2 = Math.pow(x1, 2);
                        result.put(i, j, c00pc01y1
                                + c10 * x1
                                + c20 * x2
                                + c11y1 * x1
                                + c02y2
                                + c30 * x1 * x2
                                + c21y1 * x2
                                + c12y2 * x1
                                + c03y3
                                + c40 * x2 * x2
                                + c31y1 * x2 * x1
                                + c22y2 * x2
                                + c13y3 * x1
                                + c04y4);
                    }
                }
                break;
            case 5:
                c00 = coeff.get(0, 0);
                c10 = coeff.get(1, 0);
                c01 = coeff.get(2, 0);
                c20 = coeff.get(3, 0);
                c11 = coeff.get(4, 0);
                c02 = coeff.get(5, 0);
                c30 = coeff.get(6, 0);
                c21 = coeff.get(7, 0);
                c12 = coeff.get(8, 0);
                c03 = coeff.get(9, 0);
                c40 = coeff.get(10, 0);
                c31 = coeff.get(11, 0);
                c22 = coeff.get(12, 0);
                c13 = coeff.get(13, 0);
                c04 = coeff.get(14, 0);
                c50 = coeff.get(15, 0);
                c41 = coeff.get(16, 0);
                c32 = coeff.get(17, 0);
                c23 = coeff.get(18, 0);
                c14 = coeff.get(19, 0);
                c05 = coeff.get(20, 0);
                for (j = 0; j < result.columns; j++) {
                    double y1 = y.get(j, 0);
                    double y2 = Math.pow(y1, 2);
                    double y3 = y2 * y1;
                    double c00pc01y1 = c00 + c01 * y1;
                    double c02y2 = c02 * y2;
                    double c11y1 = c11 * y1;
                    double c21y1 = c21 * y1;
                    double c12y2 = c12 * y2;
                    double c03y3 = c03 * y3;
                    double c31y1 = c31 * y1;
                    double c22y2 = c22 * y2;
                    double c13y3 = c13 * y3;
                    double c04y4 = c04 * y2 * y2;
                    double c41y1 = c41 * y1;
                    double c32y2 = c32 * y2;
                    double c23y3 = c23 * y3;
                    double c14y4 = c14 * y2 * y2;
                    double c05y5 = c05 * y3 * y2;
                    for (i = 0; i < result.rows; i++) {
                        double x1 = x.get(i, 0);
                        double x2 = Math.pow(x1, 2);
                        double x3 = x1 * x2;
                        result.put(i, j, c00pc01y1
                                + c10 * x1
                                + c20 * x2
                                + c11y1 * x1
                                + c02y2
                                + c30 * x3
                                + c21y1 * x2
                                + c12y2 * x1
                                + c03y3
                                + c40 * x2 * x2
                                + c31y1 * x3
                                + c22y2 * x2
                                + c13y3 * x1
                                + c04y4
                                + c50 * x3 * x2
                                + c41y1 * x2 * x2
                                + c32y2 * x3
                                + c23y3 * x2
                                + c14y4 * x1
                                + c05y5);
                    }
                }
                break;

            // TODO: solve up to 5 efficiently, do rest in loop
            default:
                for (j = 0; j < result.columns; j++) {
                    double yy = y.get(j, 0);
                    for (i = 0; i < result.rows; i++) {
                        double xx = x.get(i, 0);
                        result.put(i, j, polyval(xx, yy, coeff, degree));
                    }
                }
        } // switch degree

        return result;
    }

    public static double polyval(final double x, final double y, final DoubleMatrix coeff, int degree) {
        return polyval(x, y, coeff.toArray(), degree);
    }

    public static double polyval(final double x, final double y, final double[] coeff) {
        return polyval(x, y, coeff, degreeFromCoefficients(coeff.length));
    }

    public static double polyval(final double x, final double y, final double[] coeff, int degree) {

        if (degree < 0 || degree > 1000) {
            logger.warn("polyval: degree value [" + degree + "] not realistic!");
            throw new IllegalArgumentException("polyval: degree not realistic!");
        }

        //// Check default arguments ////
        if (degree < -1) {
            logger.warn("polyValGrid: degree < -1 ????");
            degree = degreeFromCoefficients(coeff.length);
        }

        //// Evaluate polynomial ////
        double sum = coeff[0];

        if (degree == 1) {
            sum += (coeff[1] * x
                    + coeff[2] * y);
        } else if (degree == 2) {
            sum += (coeff[1] * x
                    + coeff[2] * y
                    + coeff[3] * Math.pow(x, 2)
                    + coeff[4] * x * y
                    + coeff[5] * Math.pow(y, 2));
        } else if (degree == 3) {
            final double xx = Math.pow(x, 2);
            final double yy = Math.pow(y, 2);
            sum += (coeff[1] * x
                    + coeff[2] * y
                    + coeff[3] * xx
                    + coeff[4] * x * y
                    + coeff[5] * yy
                    + coeff[6] * xx * x
                    + coeff[7] * xx * y
                    + coeff[8] * x * yy
                    + coeff[9] * yy * y);
        } else if (degree == 4) {
            final double xx = Math.pow(x, 2);
            final double xxx = xx * x;
            final double yy = Math.pow(y, 2);
            final double yyy = yy * y;
            sum += (coeff[1] * x
                    + coeff[2] * y
                    + coeff[3] * xx
                    + coeff[4] * x * y
                    + coeff[5] * yy
                    + coeff[6] * xxx
                    + coeff[7] * xx * y
                    + coeff[8] * x * yy
                    + coeff[9] * yyy
                    + coeff[10] * xx * xx
                    + coeff[11] * xxx * y
                    + coeff[12] * xx * yy
                    + coeff[13] * x * yyy
                    + coeff[14] * yy * yy);
        } else if (degree == 5) {
            final double xx = Math.pow(x, 2);
            final double xxx = xx * x;
            final double xxxx = xxx * x;
            final double yy = Math.pow(y, 2);
            final double yyy = yy * y;
            final double yyyy = yyy * y;
            sum += (coeff[1] * x
                    + coeff[2] * y
                    + coeff[3] * xx
                    + coeff[4] * x * y
                    + coeff[5] * yy
                    + coeff[6] * xxx
                    + coeff[7] * xx * y
                    + coeff[8] * x * yy
                    + coeff[9] * yyy
                    + coeff[10] * xxxx
                    + coeff[11] * xxx * y
                    + coeff[12] * xx * yy
                    + coeff[13] * x * yyy
                    + coeff[14] * yyyy
                    + coeff[15] * xxxx * x
                    + coeff[16] * xxxx * y
                    + coeff[17] * xxx * yy
                    + coeff[18] * xx * yyy
                    + coeff[19] * x * yyyy
                    + coeff[20] * yyyy * y);
        } else if (degree != 0)                // degreee > 5
        {
            sum = 0.0;
            int coeffIndex = 0;
            for (int l = 0; l <= degree; l++) {
                for (int k = 0; k <= l; k++) {
                    sum += coeff[coeffIndex] * Math.pow(x, (double) (l - k)) * Math.pow(y, (double) k);
                    coeffIndex++;
                }
            }
        }

        return sum;

    }

}
