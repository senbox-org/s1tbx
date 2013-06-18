package org.jlinda.core.coregistration.cross;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.commons.lang.ArrayUtils;
import org.jblas.DoubleMatrix;
import org.jlinda.core.Window;
import org.jlinda.core.utils.MathUtils;
import org.jlinda.core.utils.PolyUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

/**
 * User: pmar@ppolabs.com
 * Date: 6/13/13
 * Time: 12:25 PM
 * Description: Unit test and prototypes for Cross Interferometry
 */
public class CrossGeometryTest {

    // logger
    private static final Logger logger = (Logger) LoggerFactory.getLogger(CrossGeometryTest.class);

    // data extent
    private static long line = 27000;
    private static long pixel = 5100;

    // ORIGINAL GEOMETRY : ENVISAT paramaters
    private static double prfASAR = 1652.4156494140625;       // [Hz]
    private static double rsrASAR = 19.20768 * 1000;  // [Hz]

    // TARGET GEOMETRY : ERS2 paramaters
    private static double prfERS = 1679.902; // 1679.95828476786;      // [Hz]
    private static double rsrERS = 18.962468 * 1000; //18.96245929824155 * 1000; // [Hz]

    // Estimation Parameters
    private static final int NUM_OF_WINDOWS = 5000;
    private static final int POLY_DEGREE = 2;

    @BeforeClass
    public static void setUp() throws Exception {

        // define logger level
        logger.setLevel(Level.TRACE);

    }

    @Test
    public void testComputeCoefficients() {

        CrossGeometry crossGeometry = new CrossGeometry();

        crossGeometry.setPrfOriginal(prfERS);
        crossGeometry.setRsrOriginal(rsrERS);

        crossGeometry.setPrfTarget(prfASAR);
        crossGeometry.setRsrTarget(rsrASAR);

        crossGeometry.setDataWindow(new Window(1, line, 1, pixel));

        // optional!
        // crossGeometry.setNumberOfWindows(NUM_OF_WINDOWS);
        // crossGeometry.setPolyDegree(POLY_DEGREE);

        crossGeometry.computeCoefficients();

        double[] coeffsAz = crossGeometry.getCoeffsAz();
        double[] coeffsRg = crossGeometry.getCoeffsRg();

        // show polynomials
        logger.debug("coeffsAZ : estimated with PolyUtils.polyFit2D : {}", ArrayUtils.toString(coeffsAz));
        logger.debug("coeffsRg : estimated with PolyUtils.polyFit2D : {}", ArrayUtils.toString(coeffsRg));

    }


    /* Prototype implementation:
    *  - based on matlab code
    *  - there is a small ~10^-8 numerical difference between prototype and class implementation
    *  - this difference is due to different (and in prototype inconsistent) use of data windows for normalization and estimation
    * */

    public void computeCrossPolynomial() {

        // estimation parameters
        int numOfObs = NUM_OF_WINDOWS;

        // ratios of frequencies
        double ratioRSR = rsrERS / rsrASAR;
        double ratioPRF = prfERS / prfASAR;

        // -----------------------------------------------
        // define solution spaces - ASAR geometry is 'slave' : SLAVE - MASTER
        // -----------------------------------------------
        // Window ersWindow = new Window(1, lineERS, 1, pixERS);
        Window asarWindow = new Window(0, line - 1, 0, pixel - 1); // start from 0 pixel

        // -----------------------------------------------
        // distribute points
        // -----------------------------------------------
        int[][] result = MathUtils.distributePoints(NUM_OF_WINDOWS, asarWindow);

        // -----------------------------------------------
        // create synthetic offsets
        // -----------------------------------------------
        double[][] resultMaster = new double[NUM_OF_WINDOWS][2];

        for (int i = 0; i < NUM_OF_WINDOWS; i++) {
            resultMaster[i][0] = result[i][0] * ratioPRF;
            resultMaster[i][1] = result[i][1] * ratioRSR;
        }

        double[][] offset = new double[NUM_OF_WINDOWS][2];
        for (int i = 0; i < NUM_OF_WINDOWS; i++) {
            offset[i][0] = result[i][0] - resultMaster[i][0];
            offset[i][1] = result[i][1] - resultMaster[i][1];
        }

        // -----------------------------------------------
        // estimation of (dummy) coregistration polynomial
        // -----------------------------------------------

        DoubleMatrix linesNorm = new DoubleMatrix(numOfObs, 1);
        DoubleMatrix pixelsNorm = new DoubleMatrix(numOfObs, 1);
        DoubleMatrix offset_lines = new DoubleMatrix(numOfObs, 1);
        DoubleMatrix offset_pixels = new DoubleMatrix(numOfObs, 1);

        // normalize, and store into jblas matrices
        for (int i = 0; i < numOfObs; i++) {
            linesNorm.put(i, PolyUtils.normalize2(resultMaster[i][0], 1, line));
            pixelsNorm.put(i, PolyUtils.normalize2(resultMaster[i][1], 1, pixel));
            offset_lines.put(i, offset[i][0]);
            offset_pixels.put(i, offset[i][1]);
        }

        double[] coeffsAz = PolyUtils.polyFit2D(pixelsNorm, linesNorm, offset_lines, POLY_DEGREE);
        double[] coeffsRg = PolyUtils.polyFit2D(pixelsNorm, linesNorm, offset_pixels, POLY_DEGREE);

        // show polynomials
        logger.debug("coeffsAZ (polyfit) = {}", ArrayUtils.toString(coeffsAz));
        logger.debug("coeffsRg (polyfit) = {}", ArrayUtils.toString(coeffsRg));

/*
        // --------------------------------------------------
        // Internal implementation of coefficients estimation
        // --------------------------------------------------
        DoubleMatrix A = SystemOfEquations.constructDesignMatrix(linesNorm, pixelsNorm, POLY_DEGREE);
        logger.debug("Solving linear system of equations with Cholesky");
        DoubleMatrix N = A.transpose().mmul(A);
        DoubleMatrix rhsL = A.transpose().mmul(offset_lines);
        DoubleMatrix rhsP = A.transpose().mmul(offset_pixels);

        rhsL = Solve.solveSymmetric(N, rhsL);
        rhsP = Solve.solveSymmetric(N, rhsP);

        logger.debug("coeffsAZ           = {}", ArrayUtils.toString(rhsL.toArray()));
        logger.debug("coeffsRg           = {}", ArrayUtils.toString(rhsP.toArray()));

*/

    }

}
