package org.jlinda.core.coregistration.estimation;

import Jama.Matrix;
import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.jlinda.core.coregistration.estimation.utils.JamaUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.jlinda.core.coregistration.estimation.utils.SimpleAsciiFileParser;
import org.jlinda.core.utils.PolyUtils;
import org.perf4j.StopWatch;
import org.slf4j.LoggerFactory;

import java.io.IOException;


public class TestEstimation_JAMA {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(TestEstimation_JAMA.class);

    public static void main(String[] args) throws IOException {

        logger.setLevel(Level.INFO);
        logger.trace("Start Estimation");

        /** estimation without Data Snooping -- only AdjustA */

        // work only with arrays, DoubleMatrix is used only in estimation!!!

        /** load data */
//        SimpleAsciiFileParser fileParser = new SimpleAsciiFileParser("/d1/list.ttt.txt");
//        String inputFile = "CPM_Data.linear_weight.csv";
        String inputFile = "CPM_Data.none_weight.BIG.csv";
        String inputDir = "/d2/test.processing/unit_tests/etna.volcano/process/crop/01486_21159.cpm/";

        /** define input parameters */
        // only degree
        int degree = 2;
//        String weight = "linear";
        String weight = "none";

        int numLines = 10000;

        boolean DONE = false;
        int ITERATION = 0;
        int MAX_ITERATIONS = 10;
        final double COH_TRESHOLD = 0.3d;

        final double CRIT_VALUE = 1.97d;

        final int minL = 1;
        final int maxL = 26292;
        final int minP = 1;
        final int maxP = 4900;

        int winL;
        int winP;

        SimpleAsciiFileParser fileParser = new SimpleAsciiFileParser(inputDir + inputFile, numLines);
//        double[][] data = fileParser.parseDoubleArray();
        TIntObjectHashMap<double[]> data = fileParser.parseDoubleMap();

//        List<double[]> dataArray = Arrays.asList(data);
        /** reformat data to _processing_ structure : using CPM_DATA like file for testing */
        // (0) idx (1) PosL (2) PosX (3) OffL (4) OffX (5) Coherence (6) eL (7) eP (8) wtestL (9) wtestP
//        int numObs = data.length;
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
                lines.add((double) PolyUtils.normalize2(entry[1], minL, maxL));
                pixels.add((double) PolyUtils.normalize2(entry[2], minP, maxP));
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


            Matrix A = new Matrix(SystemOfEquations.constructDesignMatrix_loop(lines.toArray(), pixels.toArray(), degree));
            Matrix A_transpose = A.transpose();
            logger.info("TIME FOR SETUP of SYSTEM : {}", stopWatch.lap("setup"));


            Matrix Qy_1; // vector
            switch (weight) {
                case "linear":
                    logger.debug("Using sqrt(coherence) as weights");
                    Qy_1 = new Matrix(coh.toArray(), coh.size());
                    // Normalize weights to avoid influence on estimated var.factor
                    logger.debug("Normalizing covariance matrix for LS estimation");
//                    Qy_1.times(JamaUtils.sum(Qy_1) / Qy_1.getRowDimension());
                    JamaUtils.normalize(Qy_1);
                    break;
                case "quadratic":
                    logger.debug("Using coherence as weights.");
                    Qy_1 = new Matrix(coh.toArray(), coh.size());
                    Qy_1.times(Qy_1);
                    // Normalize weights to avoid influence on estimated var.factor
                    logger.debug("Normalizing covariance matrix for LS estimation.");
//                    Qy_1.times(JamaUtils.sum(Qy_1) / Qy_1.getRowDimension());
                    JamaUtils.normalize(Qy_1);
                    break;
                case "bamler":
                    // TODO: see Bamler papers IGARSS 2000 and 2004
                    logger.warn("Bamler weighting method NOT IMPLEMENTED, falling back to None.");
                    Qy_1 = JamaUtils.ones(numObs, 1);
                    break;
                case "none":
                    Qy_1 = JamaUtils.ones(numObs, 1);
                    break;
                default:
                    Qy_1 = JamaUtils.ones(numObs, 1);
                    break;
            }

            logger.info("TIME FOR SETUP of VC vector: {}", stopWatch.lap("VC vector"));

//            final Matrix Qy_1_diag = Matrix.diag(Qy_1);

//            Matrix Qy_1_diag = new Matrix(numObs, numObs);
//            for (int i = 0; i < numObs; i++) {
//                Qy_1_diag.set(i, i, Qy_1.get(i, 0));
//            }
//            logger.info("TIME FOR SETUP of VC diag matris: {}", stopWatch.lap("diag VC matrix"));


            /** temp matrices */
            final Matrix yL_matrix = new Matrix(yL.toArray(), yL.size());
            final Matrix yP_matrix = new Matrix(yP.toArray(), yP.size());
            logger.info("TIME FOR SETUP of TEMP MATRICES: {}", stopWatch.lap("Temp matrices"));

            /** normal matrix */
            final Matrix N = A_transpose.times(diagTimesMat_unsafe(Qy_1, A));
            Matrix Qx_hat = N.copy(); // store N into Qx_hat
            logger.info("TIME FOR SETUP of NORMAL MATRIX: {}", stopWatch.lap("Normal matrix"));

            /** right hand sides */
//            DoubleMatrix rhsL = A_transpose.mmul(Qy_1_diag.mmul(yL_matrix));
            Matrix rhsL = A_transpose.times(diagTimesMat_unsafe(Qy_1, yL_matrix));
            Matrix rhsP = A_transpose.times(diagTimesMat_unsafe(Qy_1, yP_matrix));
            logger.info("TIME FOR SETUP of RightHand Side: {}", stopWatch.lap("Right-hand-side"));

            /** compute solution */
            rhsL = Qx_hat.solve(rhsL);
//            rhsL = Solve.solvePositive(Qx_hat, rhsL);
//            rhsP = Solve.solvePositive(Qx_hat, rhsP);
            rhsP = Qx_hat.solve(rhsP);
            logger.info("TIME FOR SOLVING of System: {}", stopWatch.lap("Solving System"));

            /** inverting of Qx_hat for stability check */
            Qx_hat = Qx_hat.inverse();
//            Qx_hat = Qx_hat.solve(Matrix.identity(numUnk, numUnk));
//            Qx_hat = Solve.solvePositive(Qx_hat, DoubleMatrix.eye(Qx_hat.getRows())); // store inverted N back into Qx_hat
            logger.info("TIME FOR INVERSION OF N: {}", stopWatch.lap("Inversion of N"));

            /** test inversion and check stability: max(abs([N*inv(N) - E)) ?= 0 */
//            double maxDeviation = abs(N.mmul(Qx_hat).sub(DoubleMatrix.eye(Qx_hat.getRows()))).max();
            double maxDeviation = JamaUtils.getMax(N.times(Qx_hat).minus(Matrix.identity(numUnk, numUnk)));
            double minDeviation = JamaUtils.getMin(N.times(Qx_hat).minus(Matrix.identity(numUnk, numUnk)));
            maxDeviation = Math.max(maxDeviation, minDeviation);
            if (maxDeviation > .01) {
                logger.error("COREGPM: maximum deviation N*inv(N) from unity = {}. This is larger than 0.01", maxDeviation);
                throw new IllegalStateException("COREGPM: maximum deviation N*inv(N) from unity)");
            } else if (maxDeviation > .001) {
                logger.warn("COREGPM: maximum deviation N*inv(N) from unity = {}. This is between 0.01 and 0.001", maxDeviation);
            }
            logger.info("TIME FOR STABILITY CHECK: {}", stopWatch.lap("Stability Check"));

            logger.debug("Coeffs in Azimuth direction: {}", ArrayUtils.toString(rhsL.getArray()));
            logger.debug("Coeffs in Range direction: {}", ArrayUtils.toString(rhsP.getArray()));
            logger.debug("Max Deviation: {}", maxDeviation);

            /** some other stuff if the scale is okay */
            Matrix Qy_hat = A.times(Qx_hat.times(A_transpose));
            Matrix Qe_hat = Qy_hat.uminus();
            logger.debug("Qe_hat(1,1): {} ", Qe_hat.get(1, 1));

//            Qe_hat.addi(FloatMatrix.diag(FloatMatrix.ones(Qy_1.length).div(Qy_1)));

//            Matrix diagonalTemp = (new Matrix(numObs, 1, 1)).arrayRightDivide(Qy_1);
            for (int i = 0; i < numObs; i++) {
                Qe_hat.set(i, i, Qe_hat.get(i, i) + 1 / Qy_1.get(i, 0));
            }

            Matrix yL_hat = A.times(rhsL);
            Matrix eL_hat = yL_matrix.minus(yL_hat);

            Matrix yP_hat = A.times(rhsP);
            Matrix eP_hat = yP_matrix.minus(yP_hat);
//            scale diagonal
            logger.info("TIME FOR DATA preparation for TESTING: {}", stopWatch.lap("Testing Setup"));

            /** overal model test (variance factor) */
            double overAllModelTest_L = 0;
            double overAllModelTest_P = 0;

            for (int i = 0; i < numObs; i++) {
                overAllModelTest_L += Math.pow(eL_hat.get(i, 0), 2) * Qy_1.get(i, 0);
                overAllModelTest_P += Math.pow(eP_hat.get(i, 0), 2) * Qy_1.get(i, 0);
            }
            logger.info("TIME FOR OMT: {}", stopWatch.lap("OMT"));

            /** WHAT IS THE REFERENCE FOR THESE CONSTANT VALUES???? */
            final double SIGMA_L = 0.15;
            final double SIGMA_P = 0.10;

            overAllModelTest_L = (overAllModelTest_L / Math.pow(SIGMA_L, 2)) / (numObs - numUnk);
            overAllModelTest_P = (overAllModelTest_P / Math.pow(SIGMA_P, 2)) / (numObs - numUnk);

            logger.debug("Overall Model Test Lines: {}", overAllModelTest_L);
            logger.debug("Overall Model Test Pixels: {}", overAllModelTest_P);

            /** ---------------------- DATASNOPING ----------------------------------- **/
            /** Assumed Qy diag */

            /** initialize */
            Matrix wTest_L = new Matrix(numObs, 1);
            Matrix wTest_P = new Matrix(numObs, 1);

            for (int i = 0; i < numObs; i++) {
                wTest_L.set(i, 0, eL_hat.get(i, 0) / (Math.sqrt(Qe_hat.get(i, i)) * SIGMA_L));
                wTest_P.set(i, 0, eP_hat.get(i, 0) / (Math.sqrt(Qe_hat.get(i, i)) * SIGMA_P));
            }

            /** find maxima's */
            // azimuth
            double[] winL_Array = JamaUtils.getAbsArgMax(wTest_L);
            winL = (int) winL_Array[1];
            double maxWinL = winL_Array[0];
            logger.debug("maximum wtest statistic azimuth = {} for window number: {} ", maxWinL, index.get(winL));

            // range
            double[] winP_Array = JamaUtils.getAbsArgMax(wTest_P);
            winP = (int) winP_Array[1];
            double maxWinP = winP_Array[0];
            logger.debug("maximum wtest statistic range = {} for window number: {} ", maxWinP, index.get(winP));

            /** use summed wTest in Azimuth and Range direction for outlier detection */
            Matrix wTestSum = new Matrix(numObs, 1);
            for (int i = 0; i < numObs; i++) {
                wTestSum.set(i, 0, Math.pow(wTest_L.get(i, 0), 2) + Math.pow(wTest_P.get(i, 0), 2));
            }
            double[] maxWSum_idx_Array = JamaUtils.getAbsArgMax(wTestSum);
            maxWSum_idx = (int) maxWSum_idx_Array[1];
            double maxWSum = maxWSum_idx_Array[0];
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

        }


    } // only warn when iterating


//    }    // iterations remove outliers

    /**
     * C=diagxmat(vec,B) C=diag(vec) * B
     */

    public static Matrix diagTimesMat_unsafe(final Matrix diag, final Matrix B) {

        // no check on dimensions
        if (!JamaUtils.isColumnVector(diag) || !JamaUtils.isRowVector(diag)) {
            logger.error("diagxmat: diag is NOT vector");
        }

        Matrix result = B.copy();

        if (JamaUtils.isColumnVector(diag)) {
            for (int i = 0; i < result.getRowDimension(); i++) {
                for (int j = 0; j < result.getColumnDimension(); j++) {
                    result.set(i, j, result.get(i, j) * diag.get(i, 0));
                }
            }
        }
        if (JamaUtils.isRowVector(diag)) {
            for (int i = 0; i < result.getRowDimension(); i++) {
                for (int j = 0; j < result.getColumnDimension(); j++) {
                    result.set(i, j, result.get(i, j) * diag.get(0, i));
                }
            }
        }
        return result;
    } // END diagxmat


}
