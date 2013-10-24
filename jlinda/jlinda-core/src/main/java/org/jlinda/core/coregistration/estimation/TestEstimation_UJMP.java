package org.jlinda.core.coregistration.estimation;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.ejml.data.DenseMatrix64F;
import org.ejml.data.RowD1Matrix64F;
import org.jlinda.core.coregistration.estimation.utils.SimpleAsciiFileParser;
import org.jlinda.core.utils.PolyUtils;
import org.perf4j.StopWatch;
import org.slf4j.LoggerFactory;
import org.ujmp.core.Matrix;
import org.ujmp.core.MatrixFactory;
import org.ujmp.core.calculation.Calculation;
import org.ujmp.core.matrix.DenseMatrix;

import java.io.IOException;


/**
 * 'Delft' school LS estimation procedure: EJML library used for math backend
 */
public class TestEstimation_UJMP {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(TestEstimation_UJMP.class);

    public static void main(String[] args) throws IOException {
        StopWatch clockFull = new StopWatch();
        clockFull.start();

        logger.setLevel(Level.INFO);
        logger.trace("Start Estimation");

        /** estimation without Data Snooping -- only AdjustA */

        /** load data */
//        SimpleAsciiFileParser fileParser = new SimpleAsciiFileParser("/d1/list.ttt.txt");
        String inputFile = "CPM_Data.none_weight.BIG.csv";
        String inputDir = "/d2/test.processing/unit_tests/etna.volcano/process/crop/01486_21159.cpm/";

        /** define input parameters */
        int degree = 2;
        String weight = "linear";
//        String weight = "none";

        int numLines = 10000;

        boolean DONE = false;
        int ITERATION = 0;
        int MAX_ITERATIONS = 100;
        final double COH_TRESHOLD = 0.35d;

        final double CRIT_VALUE = 1.97d;

        final int minL = 1;
        final int maxL = 26292;
        final int minP = 1;
        final int maxP = 4900;

        int winL;
        int winP;

        SimpleAsciiFileParser fileParser = new SimpleAsciiFileParser(inputDir + inputFile, numLines);
        TIntObjectHashMap<double[]> data = fileParser.parseDoubleMap();

        /** reformat data to _processing_ structure : using CPM_DATA like file for testing */
        // (0) idx (1) PosL (2) PosX (3) OffL (4) OffX (5) Coherence (6)
        int numObs;
        int numUnk = PolyUtils.numberOfCoefficients(degree);

        /** ??? if i implement this in separate loops will JIT and HotSpot inline these? ??? */

        // allocate collections
        TIntArrayList index = new TIntArrayList();
        TDoubleArrayList lines = new TDoubleArrayList();
        TDoubleArrayList pixels = new TDoubleArrayList();
        TDoubleArrayList yL = new TDoubleArrayList();
        TDoubleArrayList yP = new TDoubleArrayList();
        TDoubleArrayList coh = new TDoubleArrayList();

        TIntObjectIterator<double[]> it = data.iterator();
        for (int i = data.size(); i-- > 0; ) {
            it.advance();
            double[] entry = it.value();
            double coherence = entry[5];
            if (coherence > COH_TRESHOLD) {
                index.add((int) entry[0]);
                lines.add(PolyUtils.normalize2(entry[1], minL, maxL));
                pixels.add(PolyUtils.normalize2(entry[2], minP, maxP));
                yL.add(entry[3]);
                yP.add(entry[4]);
                coh.add(coherence);
            }
        }

        int maxWSum_idx = 0;
        while (!DONE) {

            String codeBlockMessage = "LS ESTIMATION PROCEDURE";
            StopWatch stopWatch = new StopWatch();
            StopWatch clock = new StopWatch();
            clock.start();
            stopWatch.setTag(codeBlockMessage);
            stopWatch.start();

            logger.info("Start iteration: {}", ITERATION);

            /** Remove identified outlier from previous estimation */
            if (ITERATION != 0) {
                logger.info("Removing observation {}, index {},  from observation vector.", index.getQuick(maxWSum_idx), maxWSum_idx);
                index.removeAt(maxWSum_idx);
                lines.removeAt(maxWSum_idx);
                pixels.removeAt(maxWSum_idx);
                yL.removeAt(maxWSum_idx);
                yP.removeAt(maxWSum_idx);
                coh.removeAt(maxWSum_idx);
            }

            /** Check redundancy */
            numObs = index.size(); // Number of points > threshold
            if (numObs < numUnk) {
                logger.error("coregpm: Number of windows > threshold is smaller than parameters solved for.");
                throw new ArithmeticException("coregpm: Number of windows > threshold is smaller than parameters solved for.");
            }

            double[][] data1 = SystemOfEquations.constructDesignMatrix_loop(lines.toArray(), pixels.toArray(), degree);
            Matrix A = MatrixFactory.importFromArray(data1);
            Matrix A_transpose = A.transpose();

            logger.info("TIME FOR SETUP of SYSTEM : {}", stopWatch.lap("setup"));

            Matrix Qy_1; // vector
            switch (weight) {
                case "linear":
                    logger.debug("Using sqrt(coherence) as weights");
                    Qy_1 = MatrixFactory.linkToArray(coh.toArray());
                    // Normalize weights to avoid influence on estimated var.factor
                    logger.debug("Normalizing covariance matrix for LS estimation");
                    Qy_1.divide(Qy_1.getMeanValue());
                    break;
                case "quadratic":
                    Qy_1 = MatrixFactory.linkToArray(coh.toArray());
                    logger.debug("Using coherence as weights.");
                    Qy_1.times(Qy_1);
                    logger.debug("Normalizing covariance matrix for LS estimation");
                    Qy_1.divide(Qy_1.getMeanValue());
                    break;
                case "bamler":
                    // TODO: see Bamler papers IGARSS 2000 and 2004
                    logger.warn("Bamler weighting method NOT IMPLEMENTED, falling back to None.");
                    Qy_1 = DenseMatrix.factory.eye(numObs);
                    break;
                case "none":
                    logger.debug("No weighting.");
                    Qy_1 = DenseMatrix.factory.eye(numObs);
                    break;
                default:
                    Qy_1 = DenseMatrix.factory.eye(numObs);
//                    Qy_1 = onesEJML(numObs);

                    break;
            }

            logger.info("TIME FOR SETUP of VC diag matris: {}", stopWatch.lap("diag VC matrix"));

//            /** temp matrices */
            final Matrix yL_matrix = MatrixFactory.linkToArray(yL.toArray());
            final Matrix yP_matrix = MatrixFactory.linkToArray(yP.toArray());
            logger.info("TIME FOR SETUP of TEMP MATRICES: {}", stopWatch.lap("Temp matrices"));

            /** normal matrix */
            Matrix diagxmat = diagxmat(Qy_1, A);
            Matrix N = A_transpose.mtimes(diagxmat);
            Matrix Qx_hat = N.copy();
            logger.info("TIME FOR SETUP of NORMAL MATRIX: {}", stopWatch.lap("Normal matrix"));

            /** right hand sides */
            // azimuth
            Matrix rhsL = A_transpose.mtimes(diagxmat(Qy_1, yL_matrix));
            Matrix rhsP = A_transpose.mtimes(diagxmat(Qy_1, yP_matrix));
            logger.info("TIME FOR SETUP of RightHand Side: {}", stopWatch.lap("Right-hand-side"));

            rhsL = Qx_hat.solveSymm(rhsL);
            rhsP = Qx_hat.solveSymm(rhsP);

//
//            LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.leastSquares(100, 100);
//            /** compute solution */
//            if (!solver.setA(Qx_hat)) {
//                throw new IllegalArgumentException("Singular Matrix");
//            }
//            solver.solve(rhsL, rhsL);
//            solver.solve(rhsP, rhsP);
            logger.info("TIME FOR SOLVING of System: {}", stopWatch.lap("Solving System"));
//
//            double maxDeviation = solver.quality();
//            /** inverting of Qx_hat for stability check */
//            logger.info("Quality measure: {} ", maxDeviation);
//
////            DenseMatrix64F Qx_hat_invert = Qx_hat.copy();
//            solver.invert(Qx_hat);

            Qx_hat = Qx_hat.inv();


            logger.info("TIME FOR INVERSION OF N: {}", stopWatch.lap("Inversion of N"));
////            Qx_hat = Solve.solvePositive(Qx_hat, DenseMatrix64F.eye(Qx_hat.getRows())); // store inverted N back into Qx_hat
//
            /** test inversion and check stability: max(abs([N*inv(N) - E)) ?= 0 */

            Matrix eye = MatrixFactory.eye(Qx_hat.getRowCount(), Qx_hat.getColumnCount());
            double maxDeviation = (N.mtimes(Qx_hat).minus(eye)).abs(Calculation.Ret.LINK).getMaxValue();


//            double maxDeviation = abs(N.mmul(Qx_hat).sub(DenseMatrix64F.eye(Qx_hat.getRows()))).max();
            if (maxDeviation > .01) {
                logger.error("COREGPM: maximum deviation N*inv(N) from unity = {}. This is larger than 0.01", maxDeviation);
                throw new IllegalStateException("COREGPM: maximum deviation N*inv(N) from unity)");
            } else if (maxDeviation > .001) {
                logger.warn("COREGPM: maximum deviation N*inv(N) from unity = {}. This is between 0.01 and 0.001", maxDeviation);
            }
            logger.info("TIME FOR STABILITY CHECK: {}", stopWatch.lap("Stability Check"));

            logger.debug("Coeffs in Azimuth direction: {}", rhsL.toString());
            logger.debug("Coeffs in Range direction: {}", rhsP.toString());
            logger.debug("Max Deviation: {}", maxDeviation);

            /** some other stuff if the scale is okay */
            Matrix Qe_hat = A.mtimes(Qx_hat).mtimes(A_transpose).mtimes(-1d);
            scaleInputDiag(Qe_hat, Qy_1);

            Matrix yL_hat = A.mtimes(rhsL);
            Matrix eL_hat = yL_matrix.minus(yL_hat);
            Matrix yP_hat = A.mtimes(rhsP);
            Matrix eP_hat = yL_matrix.minus(yP_hat);
            logger.info("TIME FOR DATA preparation for TESTING: {}", stopWatch.lap("Testing Setup"));

            /** overal model test (variance factor) */
            double overAllModelTest_L = 0;
            double overAllModelTest_P = 0;

            for (int i = 0; i < numObs; i++) {
                overAllModelTest_L += Math.pow(eL_hat.getAsDouble(i, 0), 2) * Qy_1.getAsDouble(i, 0);
                overAllModelTest_P += Math.pow(eP_hat.getAsDouble(i, 0), 2) * Qy_1.getAsDouble(i, 0);
            }
            /** WHAT IS THE REFERENCE FOR THESE CONSTANT VALUES???? */
            final double SIGMA_L = 0.15;
            final double SIGMA_P = 0.10;
//
            overAllModelTest_L = (overAllModelTest_L / Math.pow(SIGMA_L, 2)) / (numObs - numUnk);
            overAllModelTest_P = (overAllModelTest_P / Math.pow(SIGMA_P, 2)) / (numObs - numUnk);

            logger.debug("Overall Model Test Lines: {}", overAllModelTest_L);
            logger.debug("Overall Model Test Pixels: {}", overAllModelTest_P);
//
            logger.info("TIME FOR OMT: {}", stopWatch.lap("OMT"));
            /** ---------------------- DATASNOPING ----------------------------------- **/
            /** Assumed Qy diag */

            /** initialize */
            Matrix wTest_L = DenseMatrix.factory.zeros(numObs, 1);
            Matrix wTest_P = DenseMatrix.factory.zeros(numObs, 1);

            for (int i = 0; i < numObs; i++) {
                wTest_L.setAsDouble(eL_hat.getAsDouble(i, 0) / (Math.sqrt(Qe_hat.getAsDouble(i, i)) * SIGMA_L), i, 0);
                wTest_P.setAsDouble(eP_hat.getAsDouble(i, 0) / (Math.sqrt(Qe_hat.getAsDouble(i, i)) * SIGMA_P), i, 0);
            }

            /** find maxima's */
            // azimuth
            winL = (int) wTest_L.indexOfMax(Calculation.Ret.LINK, 1).getAsDouble(1);
            double maxWinL = Math.abs(wTest_L.getAsDouble(winL, 0));

            // range
            winP = (int) wTest_P.indexOfMax(Calculation.Ret.LINK, 1).getAsDouble(1);
            double maxWinP = Math.abs(wTest_P.getAsDouble(winP, 0));

            logger.debug("maximum wtest statistic azimuth = {} for window number: {} ", maxWinL, index.get(winL));
            logger.debug("maximum wtest statistic range = {} for window number: {} ", maxWinP, index.get(winP));

            /** use summed wTest in Azimuth and Range direction for outlier detection */
            Matrix wTestSum = DenseMatrix.factory.zeros(numObs, 1);
            for (int i = 0; i < numObs; i++) {
                wTestSum.setAsDouble(Math.pow(wTest_L.getAsDouble(i, 0), 2) + Math.pow(wTest_P.getAsDouble(i, 0), 2), i, 0);
            }

            maxWSum_idx = (int) wTestSum.indexOfMax(Calculation.Ret.LINK, 1).getAsDouble(1);
            double maxWSum = wTestSum.getAsDouble(winP, 0);
            logger.info("Detected outlier: summed sqr.wtest = {}; observation: {}", maxWSum, index.get(maxWSum_idx));

            /** Test if we are done yet */
            // check on number of observations
            if (numObs <= numUnk) {
                logger.warn("NO redundancy!  Exiting iterations.");
                DONE = true;// cannot remove more than this
            }

            // check on test k_alpha
            if (Math.max(maxWinL, maxWinP) <= CRIT_VALUE) {
                // all tests accepted?
                logger.info("All outlier tests accepted! (final solution computed)");
                DONE = true;
            }

            if (ITERATION >= MAX_ITERATIONS) {
                clockFull.stop();
                logger.info("TOTAL TIME FOR for 100 ITERATIONS {}: ", clockFull.getElapsedTime());

                logger.info("max. number of iterations reached (exiting loop).");
                DONE = true; // we reached max. (or no max_iter specified)

            }

            /** Only warn if last iteration has been done */
            if (DONE) {
                // ___ use trace buffer to store string, remember to rewind it ___
                if (overAllModelTest_L > 10) {
                    logger.warn("COREGPM: Overall Model Test, Lines = {} is larger than 10. (Suggest model or a priori sigma not correct.)", overAllModelTest_L);
                }
                if (overAllModelTest_P > 10) {
                    logger.warn("COREGPM: Overall Model Test, Pixels = {} is larger than 10. (Suggest model or a priori sigma not correct.)", overAllModelTest_P);
                }

                /** if a priori sigma is correct, max wtest should be something like 1.96 */
                if (Math.max(maxWinL, maxWinP) > 200.0) {
                    logger.warn("Recommendation: remove window number: {} and re-run step COREGPM.  max. wtest is: {}.", index.get(winL), Math.max(maxWinL, maxWinP));
                }

            }

            logger.info("TIME FOR wTestStatistics: {}", stopWatch.lap("WTEST"));
            logger.info("Total Estimation TIME: {}", clock.getElapsedTime());

            ITERATION++;// update counter here!


        } // only warn when iterating


    }    // iterations remove outliers

    /**
     * C=diagxmat(vec,B) C=diag(vec) * B
     */

    public static Matrix diagxmat(final Matrix diag, final Matrix B) {

//        if (!diag.isColumnVector() || !diag.isRowVector()) {
//            logger.error("diagXMat: sizes A,B: diag is NOT vector.");
//
//        }

        Matrix result = B.copy();
        for (int i = 0; i < result.getRowCount(); i++) {
            for (int j = 0; j < result.getColumnCount(); j++) {
                result.setAsDouble(result.getAsDouble(i, j) * diag.getAsDouble(i, 0), i, j);

            }
        }
        return result;
    } // END diagxmat

    public static void scaleInputDiag(final Matrix matrix, Matrix diag) {
        for (int i = 0; i < matrix.getRowCount(); i++) {
            matrix.setAsDouble(matrix.getAsDouble(i, i) + 1 / diag.getAsDouble(i, 0), i, i);
        }
    } // END diagxmat

    public static double[] ones(int rows, int columns) {
        double[] m = new double[rows * columns];
        for (int i = 0; i < rows * columns; i++) {
            m[i] = 1;
        }
        return m;
    }

    public static double[] ones(int length) {
        return ones(length, 1);
    }

    public static RowD1Matrix64F onesEJML(int rows, int columns) {
        DenseMatrix64F m = new DenseMatrix64F(rows, columns);
        for (int i = 0; i < rows * columns; i++) {
            m.set(i, 1);
        }
        return m;
    }

    public static RowD1Matrix64F onesEJML(int length) {
        return onesEJML(length, 1);
    }

    /**
     * Returns the linear index of the maximal element of the abs()
     * matrix. If there are more than one elements with this value,
     * the first one is returned.
     */
    public static int absArgmax(RowD1Matrix64F matrix) {
        int numElements = matrix.getNumElements();
        if (numElements == 0) {
            return -1;
        }
        double v = Double.NEGATIVE_INFINITY;
        int a = -1;
        for (int i = 0; i < numElements; i++) {
            double abs = Math.abs(matrix.get(i));
            if (!Double.isNaN(abs) && abs > v) {
                v = abs;
                a = i;
            }
        }

        return a;
    }

    /**
     * Returns the linear index of the maximal element of the abs()
     * matrix. If there are more than one elements with this value,
     * the first one is returned.
     */
    public static int argmax(RowD1Matrix64F matrix) {
        int numElements = matrix.getNumElements();
        if (numElements == 0) {
            return -1;
        }
        double v = Double.NEGATIVE_INFINITY;
        int a = -1;
        for (int i = 0; i < numElements; i++) {
            if (!Double.isNaN(matrix.get(i)) && matrix.get(i) > v) {
                v = matrix.get(i);
                a = i;
            }
        }

        return a;
    }

}
