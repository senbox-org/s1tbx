/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.insar.gpf.coregistration;

import edu.emory.mathcs.jtransforms.fft.DoubleFFT_1D;
import org.apache.commons.math3.util.FastMath;
import org.esa.snap.engine_utilities.eo.Constants;

/**
 * Created by luis on 17/02/2016.
 */
public class FineRegistration {

    private final static int ITMAX = 200;
    private final static double TOL = 2.0e-4;      // Tolerance passed to brent
    private final static double GOLD = 1.618034;   // Here GOLD is the default ratio by which successive intervals are magnified
    private final static double GLIMIT = 100.0;    // GLIMIT is the maximum magnification allowed for a parabolic-fit step.
    private final static double TINY = 1.0e-20;
    private final static double CGOLD = 0.3819660; // CGOLD is the golden ratio;
    private final static double ZEPS = 1.0e-10;    // ZEPS is a small number that protects against trying to achieve fractional

    /**
     * Minimize coherence as a function of row shift and column shift using
     * Powell's method. The 1-D minimization subroutine linmin() is used. p
     * is the starting point and also the final optimal point.  \
     *
     * @param complexData the master and slave complex data
     * @param p           Starting point for the minimization.
     * @return fp
     */
    public static double powell(final ComplexCoregData complexData, final double[] p) {

        final double[][] directions = {{0, 1}, {1, 0}}; // set initial searching directions
        double fp = computeCoherence(complexData, p); // get function value for initial point
        //System.out.println("Initial 1 - coherence = " + fp);

        final double[] p0 = {p[0], p[1]}; // save the initial point
        final double[] currentDirection = {0.0, 0.0}; // current searching direction

        for (int iter = 0; iter < ITMAX; iter++) {

            //System.out.println("Iteration: " + iter);

            p0[0] = p[0];
            p0[1] = p[1];
            double fp0 = fp; // save function value for the initial point
            int imax = 0;     // direction index for the largest single step decrement
            double maxDecrement = 0.0; // the largest single step decrement

            for (int i = 0; i < 2; i++) { // for each iteration, loop through all directions in the set

                // copy the ith searching direction
                currentDirection[0] = directions[i][0];
                currentDirection[1] = directions[i][1];

                final double fpc = fp; // save function value at current point
                fp = linmin(complexData, p, currentDirection); // minimize function along the ith direction, and get new point in p
                //System.out.println("Decrement along direction " + (i+1) + ": " + (fpc - fp));

                final double decrement = Math.abs(fpc - fp);
                if (decrement > maxDecrement) { // if the single step decrement is the largest so far,
                    maxDecrement = decrement;   // record the decrement and the direction index.
                    imax = i;
                }
            }

            // After trying all directions, check the decrement from start point to end point.
            // If the decrement is less than certain amount, then stop.
            /*
            if (2.0*Math.abs(fp0 - fp) <= ftol*(Math.abs(fp0) + Math.abs(fp))) { //Termination criterion.
                System.out.println("Number of iterations: " + (iter+1));
                return fp;
            }
            */
            //Termination criterion 1: stop if coherence change is small
            if (Math.abs(fp0 - fp) < complexData.coherenceFuncToler) {
                //System.out.println("C1: Number of iterations: " + (iter+1));
                return fp;
            }

            //Termination criterion 2: stop if GCP shift is small
            if (Math.sqrt((p0[0] - p[0]) * (p0[0] - p[0]) + (p0[1] - p[1]) * (p0[1] - p[1])) < complexData.coherenceValueToler) {
                //System.out.println("C2: Number of iterations: " + (iter+1));
                return fp;
            }
            // Otherwise, prepare for the next iteration
            //final double[] pe = new double[2];
            final double[] averageDirection = {p[0] - p0[0], p[1] - p0[1]};
            final double norm = Math.sqrt(averageDirection[0] * averageDirection[0] +
                                                  averageDirection[1] * averageDirection[1]);
            for (int j = 0; j < 2; j++) {
                averageDirection[j] /= norm; // construct the average direction
                //pe[j] = p[j] + averageDirection[j]; // construct the extrapolated point
                //p0[j] = p[j]; // save the final opint of current iteration as the initial point for the next iteration
            }

            //final double fpe = computeCoherence(complexData, pe); // get function value for the extrapolated point.
            final double fpe = linmin(complexData, p, averageDirection); // JL test

            if (fpe < fp0) { // condition 1 for updating search direction

                final double d1 = (fp0 - fp - maxDecrement) * (fp0 - fp - maxDecrement);
                final double d2 = (fp0 - fpe) * (fp0 - fpe);

                if (2.0 * (fp0 - 2.0 * fp + fpe) * d1 < maxDecrement * d2) { // condition 2 for updating searching direction

                    // The calling of linmin() next line should be commented out because it changes
                    // the starting point for the next iteration and this average direction will be
                    // added to the searching directions anyway.
                    //fp = linmin(complexData, p, averageDirection); // minimize function along the average direction

                    for (int j = 0; j < 2; j++) {
                        directions[imax][j] = directions[1][j]; // discard the direction for the largest decrement
                        directions[1][j] = averageDirection[j]; // add the average direction as a new direction
                    }
                }
            }
        }
        return fp;
    }

    /**
     * Given a starting point p and a searching direction xi, moves and
     * resets p to where the function takes on a minimum value along the
     * direction xi from p, and replaces xi by the actual vector displacement
     * that p was moved. Also returns the minimum value. This is accomplished
     * by calling the routines mnbrak() and brent().
     *
     * @param complexData the master and slave complex data
     * @param p           The starting point
     * @param xi          The searching direction
     * @return The minimum function value
     */
    private static double linmin(final ComplexCoregData complexData, final double[] p, final double[] xi) {

        // set initial guess for brackets: [ax, bx, cx]
        final double[] bracketPoints = {0.0, 0.02, 0.0};

        // get new brackets [ax, bx, cx] that bracket a minimum of the function
        mnbrak(complexData, bracketPoints, p, xi);

        // find function minimum in the brackets
        return brent(complexData, bracketPoints, p, xi);
    }

    /**
     * Given a distinct initial points ax and bx in bracketPoints,
     * this routine searches in the downhill direction (defined by the
     * function as evaluated at the initial points) and returns new points
     * ax, bx, cx that bracket a minimum of the function.
     *
     * @param complexData   the master and slave complex data
     * @param bracketPoints The bracket points ax, bx and cx
     * @param p             The starting point
     * @param xi            The searching direction
     */
    private static void mnbrak(final ComplexCoregData complexData,
                               final double[] bracketPoints, final double[] p, final double[] xi) {

        double ax = bracketPoints[0];
        double bx = bracketPoints[1];

        double fa = computeCoherence(complexData, ax, p, xi);
        double fb = computeCoherence(complexData, bx, p, xi);

        if (fb > fa) { // Switch roles of a and b so that we can go
            // downhill in the direction from a to b.
            double tmp = ax;
            ax = bx;
            bx = tmp;

            tmp = fa;
            fa = fb;
            fb = tmp;
        }

        double cx = bx + GOLD * (bx - ax); // First guess for c.
        double fc = computeCoherence(complexData, cx, p, xi);

        double fu;
        while (fb > fc) { // Keep returning here until we bracket.

            final double r = (bx - ax) * (fb - fc); // Compute u by parabolic extrapolation from a; b; c.
            // TINY is used to prevent any possible division by zero.
            final double q = (bx - cx) * (fb - fa);

            double u = bx - ((bx - cx) * q - (bx - ax) * r) /
                    (2.0 * sign(Math.max(Math.abs(q - r), TINY), q - r));

            final double ulim = bx + GLIMIT * (cx - bx);

            // We won't go farther than this. Test various possibilities:
            if ((bx - u) * (u - cx) > 0.0) { // Parabolic u is between b and c: try it.

                fu = computeCoherence(complexData, u, p, xi);

                if (fu < fc) { // Got a minimum between b and c.

                    ax = bx;
                    bx = u;
                    break;

                } else if (fu > fb) { // Got a minimum between between a and u.

                    cx = u;
                    break;
                }

                // reach this point can only be:  fc <= fu <= fb
                u = cx + GOLD * (cx - bx); // Parabolic fit was no use. Use default magnification.
                fu = computeCoherence(complexData, u, p, xi);

            } else if ((cx - u) * (u - ulim) > 0.0) { // Parabolic fit is between c and its allowed limit.

                fu = computeCoherence(complexData, u, p, xi);

                if (fu < fc) {
                    bx = cx;
                    cx = u;
                    u = cx + GOLD * (cx - bx);
                    fb = fc;
                    fc = fu;
                    fu = computeCoherence(complexData, u, p, xi);
                }

            } else if ((u - ulim) * (ulim - cx) >= 0.0) { // Limit parabolic u to maximum allowed value.

                u = ulim;
                fu = computeCoherence(complexData, u, p, xi);

            } else { // Reject parabolic u, use default magnification.
                u = cx + GOLD * (cx - bx);
                fu = computeCoherence(complexData, u, p, xi);
            }

            ax = bx;
            bx = cx;
            cx = u; // Eliminate oldest point and continue.

            fa = fb;
            fb = fc;
            fc = fu;
        }

        bracketPoints[0] = ax;
        bracketPoints[1] = bx;
        bracketPoints[2] = cx;
    }

    /**
     * Given a bracketing triplet of abscissas [ax, bx, cx] (such that bx
     * is between ax and cx, and f(bx) is less than both f(ax) and f(cx)),
     * this routine isolates the minimum to a fractional precision of about
     * tol using Brent's method. p is reset to the point where function
     * takes on a minimum value along direction xi from p, and xi is replaced
     * by the axtual displacement that p moved. The minimum function value
     * is returned.
     *
     * @param complexData   the master and slave complex data
     * @param bracketPoints The bracket points ax, bx and cx
     * @param pp            The starting point
     * @param xi            The searching direction
     * @return The minimum unction value
     */
    private static double brent(final ComplexCoregData complexData,
                                final double[] bracketPoints, final double[] pp, final double[] xi) {

        final int maxNumIterations = 100; // the maximum number of iterations

        final double ax = bracketPoints[0];
        final double bx = bracketPoints[1];
        final double cx = bracketPoints[2];

        double d = 0.0;
        double u = 0.0;
        double e = 0.0; //This will be the distance moved on the step before last.
        double a = (ax < cx ? ax : cx); // a and b must be in ascending order,
        double b = (ax > cx ? ax : cx); // but input abscissas need not be.
        double x = bx; // Initializations...
        double w = bx;
        double v = bx;
        double fw = computeCoherence(complexData, x, pp, xi);
        double fv = fw;
        double fx = fw;

        for (int iter = 0; iter < maxNumIterations; iter++) { // Main loop.

            final double xm = 0.5 * (a + b);
            final double tol1 = TOL * Math.abs(x) + ZEPS;
            final double tol2 = 2.0 * tol1;

            if (Math.abs(x - xm) <= (tol2 - 0.5 * (b - a))) { // Test for done here.
                xi[0] *= x;
                xi[1] *= x;
                pp[0] += xi[0];
                pp[1] += xi[1];
                return fx;
            }

            if (Math.abs(e) > tol1) { // Construct a trial parabolic fit.

                final double r = (x - w) * (fx - fv);
                double q = (x - v) * (fx - fw);
                double p = (x - v) * q - (x - w) * r;
                q = 2.0 * (q - r);

                if (q > 0.0) {
                    p = -p;
                }

                q = Math.abs(q);
                final double etemp = e;
                e = d;

                if (Math.abs(p) >= Math.abs(0.5 * q * etemp) ||
                        p <= q * (a - x) || p >= q * (b - x)) {

                    e = (x >= xm ? a - x : b - x);
                    d = CGOLD * e;

                    // The above conditions determine the acceptability of the parabolic fit. Here we
                    // take the golden section step into the larger of the two segments.
                } else {

                    d = p / q; // Take the parabolic step.
                    u = x + d;
                    if (u - a < tol2 || b - u < tol2)
                        d = sign(tol1, xm - x);
                }

            } else {

                e = (x >= xm ? a - x : b - x); // larger part: from x to both ends
                d = CGOLD * e;
            }

            u = (Math.abs(d) >= tol1 ? x + d : x + sign(tol1, d));
            final double fu = computeCoherence(complexData, u, pp, xi);

            // This is the one function evaluation per iteration.
            if (fu <= fx) { // Now decide what to do with our func tion evaluation.

                if (u >= x) {
                    a = x;
                } else {
                    b = x;
                }
                v = w;
                w = x;
                x = u;

                fv = fw;
                fw = fx;
                fx = fu;

            } else {

                if (u < x) {
                    a = u;
                } else {
                    b = u;
                }

                if (fu <= fw || w == x) {

                    v = w;
                    w = u;
                    fv = fw;
                    fw = fu;

                } else if (fu <= fv || v == x || v == w) {

                    v = u;
                    fv = fu;
                }
            } // Done with housekeeping. Back for another iteration.
        }

        System.out.println("Too many iterations in brent");
        return -1.0;
    }

    private static double computeCoherence(final ComplexCoregData complexData,
                                           final double a, final double[] p, final double[] d) {

        final double[] point = {p[0] + a * d[0], p[1] + a * d[1]};
        return computeCoherence(complexData, point);
    }

    private static double computeCoherence(final ComplexCoregData complexData,
                                           final double[] point) {

        // Set penalty at the boundary of the pixel so that the searching area is within a pixel
        final double xShift = Math.abs(complexData.point0[0] - point[0]);
        final double yShift = Math.abs(complexData.point0[1] - point[1]);
//        if (xShift >= 0.5 || yShift >= 0.5) {
//            return 1.0;
//        }

        getComplexSlaveImagette(complexData, point);
        /*
        System.out.println("Real part of master imagette:");
        outputRealImage(complexData.mII);
        System.out.println("Imaginary part of master imagette:");
        outputRealImage(complexData.mIQ);
        System.out.println("Real part of slave imagette:");
        outputRealImage(complexData.sII);
        System.out.println("Imaginary part of slave imagette:");
        outputRealImage(complexData.sIQ);
        */

        double coherence = 0.0;
        if (complexData.useSlidingWindow) {

            final int maxR = complexData.fWindowHeight - complexData.coherenceWindowSize;
            final int maxC = complexData.fWindowWidth - complexData.coherenceWindowSize;
            for (int r = 0; r <= maxR; r++) {
                for (int c = 0; c <= maxC; c++) {
                    coherence += getCoherence(complexData, r, c, complexData.coherenceWindowSize, complexData.coherenceWindowSize);
                }
            }

            coherence /= (maxR + 1) * (maxC + 1);

        } else {
            coherence = getCoherence(complexData, 0, 0, complexData.fWindowWidth, complexData.fWindowHeight);
        }
        //System.out.println("coherence = " + coherence);

        return 1 - coherence;
    }

    private static double getCoherence(final ComplexCoregData complexData,
                                       final int row, final int col,
                                       final int coherenceWindowWidth, final int coherenceWindowHeight) {

        // Compute coherence of master and slave imagettes by creating a coherence image
        double sum1 = 0.0;
        double sum2 = 0.0;
        double sum3 = 0.0;
        double sum4 = 0.0;
        double mr, mi, sr, si;
        final double[][] mIIdata = complexData.mII;
        final double[][] mIQdata = complexData.mIQ;
        final double[][] sIIdata = complexData.sII;
        final double[][] sIQdata = complexData.sIQ;
        double[] mII, mIQ, sII, sIQ;
        int rIdx, cIdx;
        for (int r = 0; r < coherenceWindowHeight; r++) {
            rIdx = row + r;
            mII = mIIdata[rIdx];
            mIQ = mIQdata[rIdx];
            sII = sIIdata[rIdx];
            sIQ = sIQdata[rIdx];
            for (int c = 0; c < coherenceWindowWidth; c++) {
                cIdx = col + c;
                mr = mII[cIdx];
                mi = mIQ[cIdx];
                sr = sII[cIdx];
                si = sIQ[cIdx];
                sum1 += mr * sr + mi * si;
                sum2 += mi * sr - mr * si;
                sum3 += mr * mr + mi * mi;
                sum4 += sr * sr + si * si;
            }
        }

        return Math.sqrt(sum1 * sum1 + sum2 * sum2) / Math.sqrt(sum3 * sum4);
    }

    private static void getComplexSlaveImagette(final ComplexCoregData compleData,
                                                final double[] point) {

        compleData.sII = new double[compleData.fWindowHeight][compleData.fWindowWidth];
        compleData.sIQ = new double[compleData.fWindowHeight][compleData.fWindowWidth];

        final double[][] sII0data = compleData.sII0;
        final double[][] sIQ0data = compleData.sIQ0;
        final double[][] sIIdata = compleData.sII;
        final double[][] sIQdata = compleData.sIQ;

        final int x0 = (int) (compleData.point0[0] + 0.5);
        final int y0 = (int) (compleData.point0[1] + 0.5);

        final double xShift = x0 - point[0];
        final double yShift = y0 - point[1];
        //System.out.println("xShift = " + xShift);
        //System.out.println("yShift = " + yShift);

        final double[] rowArray = new double[compleData.fTwoWindowWidth];
        final double[] rowPhaseArray = new double[compleData.fTwoWindowWidth];
        final DoubleFFT_1D row_fft = new DoubleFFT_1D(compleData.fWindowWidth);

        int signalLength = rowArray.length / 2;
        computeShiftPhaseArray(xShift, signalLength, rowPhaseArray);
        for (int r = 0; r < compleData.fWindowHeight; r++) {
            int k = 0;
            final double[] sII = sII0data[r];
            final double[] sIQ = sIQ0data[r];
            for (int c = 0; c < compleData.fWindowWidth; c++) {
                rowArray[k++] = sII[c];
                rowArray[k++] = sIQ[c];
            }

            row_fft.complexForward(rowArray);
            multiplySpectrumByShiftFactor(rowArray, rowPhaseArray);
            row_fft.complexInverse(rowArray, true);
            for (int c = 0; c < compleData.fWindowWidth; c++) {
                sIIdata[r][c] = rowArray[2 * c];
                sIQdata[r][c] = rowArray[2 * c + 1];
            }
        }

        final double[] colArray = new double[compleData.fTwoWindowHeight];
        final double[] colPhaseArray = new double[compleData.fTwoWindowHeight];
        final DoubleFFT_1D col_fft = new DoubleFFT_1D(compleData.fWindowHeight);

        signalLength = colArray.length / 2;
        computeShiftPhaseArray(yShift, signalLength, colPhaseArray);
        for (int c = 0; c < compleData.fWindowWidth; c++) {
            int k = 0;
            for (int r = 0; r < compleData.fWindowHeight; r++) {
                colArray[k++] = sIIdata[r][c];
                colArray[k++] = sIQdata[r][c];
            }

            col_fft.complexForward(colArray);
            multiplySpectrumByShiftFactor(colArray, colPhaseArray);
            col_fft.complexInverse(colArray, true);
            for (int r = 0; r < compleData.fWindowHeight; r++) {
                sIIdata[r][c] = colArray[2 * r];
                sIQdata[r][c] = colArray[2 * r + 1];
            }
        }
    }

    private static void computeShiftPhaseArray(final double shift, final int signalLength, final double[] phaseArray) {

        int k2;
        double phaseK;
        final double phase = -2.0 * Constants.PI * shift / signalLength;
        final int halfSignalLength = (int) (signalLength * 0.5 + 0.5);

        for (int k = 0; k < signalLength; ++k) {
            if (k < halfSignalLength) {
                phaseK = phase * k;
            } else {
                phaseK = phase * (k - signalLength);
            }
            k2 = k * 2;
            phaseArray[k2] = FastMath.cos(phaseK);
            phaseArray[k2 + 1] = FastMath.sin(phaseK);
        }
    }

    private static void multiplySpectrumByShiftFactor(final double[] array, final double[] phaseArray) {

        int k2;
        double c, s;
        double real, imag;
        final int signalLength = array.length / 2;
        for (int k = 0; k < signalLength; ++k) {
            k2 = k * 2;
            c = phaseArray[k2];
            s = phaseArray[k2 + 1];
            real = array[k2];
            imag = array[k2 + 1];
            array[k2] = real * c - imag * s;
            array[k2 + 1] = real * s + imag * c;
        }
    }

    public static void getShiftedData(final ComplexCoregData complexData,
                                       final double[][] srcI, final double[][] srcQ,
                                       final double xShift, final double yShift,
                                       final double[][] tgtI, final double[][] tgtQ) {

        final double[] rowArray = new double[complexData.fTwoWindowWidth];
        final double[] rowPhaseArray = new double[complexData.fTwoWindowWidth];
        final DoubleFFT_1D row_fft = new DoubleFFT_1D(complexData.fWindowWidth);

        int signalLength = rowArray.length / 2;
        computeShiftPhaseArray(xShift, signalLength, rowPhaseArray);
        for (int r = 0; r < complexData.fWindowHeight; r++) {
            int k = 0;
            final double[] sII = srcI[r];
            final double[] sIQ = srcQ[r];
            for (int c = 0; c < complexData.fWindowWidth; c++) {
                rowArray[k++] = sII[c];
                rowArray[k++] = sIQ[c];
            }

            row_fft.complexForward(rowArray);
            multiplySpectrumByShiftFactor(rowArray, rowPhaseArray);
            row_fft.complexInverse(rowArray, true);
            for (int c = 0; c < complexData.fWindowWidth; c++) {
                tgtI[r][c] = rowArray[2 * c];
                tgtQ[r][c] = rowArray[2 * c + 1];
            }
        }

        final double[] colArray = new double[complexData.fTwoWindowHeight];
        final double[] colPhaseArray = new double[complexData.fTwoWindowHeight];
        final DoubleFFT_1D col_fft = new DoubleFFT_1D(complexData.fWindowHeight);

        signalLength = colArray.length / 2;
        computeShiftPhaseArray(yShift, signalLength, colPhaseArray);
        for (int c = 0; c < complexData.fWindowWidth; c++) {
            int k = 0;
            for (int r = 0; r < complexData.fWindowHeight; r++) {
                colArray[k++] = tgtI[r][c];
                colArray[k++] = tgtQ[r][c];
            }

            col_fft.complexForward(colArray);
            multiplySpectrumByShiftFactor(colArray, colPhaseArray);
            col_fft.complexInverse(colArray, true);
            for (int r = 0; r < complexData.fWindowHeight; r++) {
                tgtI[r][c] = colArray[2 * r];
                tgtQ[r][c] = colArray[2 * r + 1];
            }
        }
    }

    private static double sign(final double a, final double b) {
        if (b >= 0) return a;
        return -a;
    }

    // This function is for debugging only.
    private static void outputRealImage(final double[][] I) {
        final int row = I.length;
        final int col = I[0].length;
        for (double[] aI : I) {
            for (int c = 0; c < col; c++) {
                System.out.print(aI[c] + ",");
            }
        }
        System.out.println();
    }

    public static class ComplexCoregData {
        public double[][] mII = null;          // real part of master imagette for coherence computation
        public double[][] mIQ = null;          // imaginary part of master imagette for coherence computation
        public double[][] sII = null;          // real part of slave imagette for coherence computation
        public double[][] sIQ = null;          // imaginary part of slave imagette for coherence computation
        public double[][] sII0 = null;         // real part of initial slave imagette for coherence computation
        public double[][] sIQ0 = null;         // imaginary part of initial slave imagette for coherence computation
        public final double[] point0 = new double[2];  // initial slave GCP position

        public final int coherenceWindowSize;
        public final double coherenceFuncToler;
        public final double coherenceValueToler;

        public final int fWindowWidth;  // row dimension for master and slave imagette for computing coherence, must be power of 2
        public final int fWindowHeight; // column dimension for master and slave imagette for computing coherence, must be power of 2
        public final int fHalfWindowWidth;
        public final int fHalfWindowHeight;
        public final int fTwoWindowWidth;
        public final int fTwoWindowHeight;

        public final boolean useSlidingWindow;

        public ComplexCoregData(final int coherenceWindowSize, final double coherenceFuncToler,
                                final double coherenceValueToler,
                                final int fWindowWidth, final int fWindowHeight, final boolean useSlidingWindow) {
            this.coherenceWindowSize = coherenceWindowSize;
            this.coherenceFuncToler = coherenceFuncToler;
            this.coherenceValueToler = coherenceValueToler;

            this.fWindowWidth = fWindowWidth;
            this.fWindowHeight = fWindowHeight;
            this.fHalfWindowWidth = fWindowWidth / 2;
            this.fHalfWindowHeight = fWindowHeight / 2;
            this.fTwoWindowWidth = fWindowWidth * 2;
            this.fTwoWindowHeight = fWindowHeight * 2;

            this.useSlidingWindow = useSlidingWindow;
        }

        public void dispose() {
            mII = null;
            mIQ = null;
            sII = null;
            sIQ = null;
            sII0 = null;
            sIQ0 = null;
        }
    }
}
