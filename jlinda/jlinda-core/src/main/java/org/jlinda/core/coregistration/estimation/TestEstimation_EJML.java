package org.jlinda.core.coregistration.estimation;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TDoubleArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.ejml.alg.dense.linsol.LinearSolver;
import org.ejml.alg.dense.linsol.LinearSolverFactory;
import org.ejml.data.DenseMatrix64F;
import org.ejml.data.RowD1Matrix64F;
import org.ejml.ops.CommonOps;
import org.ejml.ops.MatrixFeatures;
import org.jlinda.core.coregistration.estimation.utils.SimpleAsciiFileParser;
import org.jlinda.core.utils.PolyUtils;
import org.perf4j.StopWatch;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.concurrent.RecursiveAction;


/**
 * 'Delft' school LS estimation procedure: EJML library used for math backend
 */
public class TestEstimation_EJML {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(TestEstimation_EJML.class);


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

            DenseMatrix64F A = new DenseMatrix64F(SystemOfEquations.constructDesignMatrix_loop(lines.toArray(), pixels.toArray(), degree));
//            DenseMatrix64F A_transpose = new DenseMatrix64F(A.numCols, A.numRows);
//            CommonOps.transpose(A, A_transpose);

            logger.info("TIME FOR SETUP of SYSTEM : {}", stopWatch.lap("setup"));

            RowD1Matrix64F Qy_1; // vector
//            double[] Qy_1_array = new double[numObs];
            double meanValue;
            switch (weight) {
                case "linear":
                    logger.debug("Using sqrt(coherence) as weights");
                    Qy_1 = DenseMatrix64F.wrap(numObs, 1, coh.toArray());
                    // Normalize weights to avoid influence on estimated var.factor
                    logger.debug("Normalizing covariance matrix for LS estimation");
                    meanValue = CommonOps.elementSum(Qy_1) / numObs;
                    CommonOps.divide(meanValue, Qy_1); // normalize vector
                    break;
                case "quadratic":
                    logger.debug("Using coherence as weights.");
                    Qy_1 = DenseMatrix64F.wrap(numObs, 1, coh.toArray());
                    CommonOps.elementMult(Qy_1, Qy_1);
                    // Normalize weights to avoid influence on estimated var.factor
                    meanValue = CommonOps.elementSum(Qy_1) / numObs;
                    logger.debug("Normalizing covariance matrix for LS estimation.");
                    CommonOps.divide(meanValue, Qy_1); // normalize vector
                    break;
                case "bamler":
                    // TODO: see Bamler papers IGARSS 2000 and 2004
                    logger.warn("Bamler weighting method NOT IMPLEMENTED, falling back to None.");
                    Qy_1 = onesEJML(numObs);
                    break;
                case "none":
                    logger.debug("No weighting.");
                    Qy_1 = onesEJML(numObs);
                    break;
                default:
                    Qy_1 = onesEJML(numObs);
//                    Qy_1 = onesEJML(numObs);

                    break;
            }

            logger.info("TIME FOR SETUP of VC diag matris: {}", stopWatch.lap("diag VC matrix"));

            /** tempMatrix_1 matrices */
            final DenseMatrix64F yL_matrix = DenseMatrix64F.wrap(numObs, 1, yL.toArray());
            final DenseMatrix64F yP_matrix = DenseMatrix64F.wrap(numObs, 1, yP.toArray());
            logger.info("TIME FOR SETUP of TEMP MATRICES: {}", stopWatch.lap("Temp matrices"));

            /** normal matrix */
            final DenseMatrix64F N = new DenseMatrix64F(numUnk, numUnk); // = A_transpose.mmul(Qy_1_diag.mmul(A));

/*
            // fork/join parallel implementation
            RowD1Matrix64F result = A.copy();
            DiagXMat dd = new DiagXMat(Qy_1, A, 0, A.numRows, result);
            ForkJoinPool pool = new ForkJoinPool();
            pool.invoke(dd);
            CommonOps.multAddTransA(A, dd.result, N);
*/

            CommonOps.multAddTransA(A, diagxmat(Qy_1, A), N);
            DenseMatrix64F Qx_hat = N.copy();

            logger.info("TIME FOR SETUP of NORMAL MATRIX: {}", stopWatch.lap("Normal matrix"));

            /** right hand sides */
            // azimuth
            DenseMatrix64F rhsL = new DenseMatrix64F(numUnk, 1);// A_transpose.mmul(Qy_1_diag.mmul(yL_matrix));
            CommonOps.multAddTransA(1d, A, diagxmat(Qy_1, yL_matrix), rhsL);
            // range
            DenseMatrix64F rhsP = new DenseMatrix64F(numUnk, 1); // A_transpose.mmul(Qy_1_diag.mmul(yP_matrix));
            CommonOps.multAddTransA(1d, A, diagxmat(Qy_1, yP_matrix), rhsP);
            logger.info("TIME FOR SETUP of RightHand Side: {}", stopWatch.lap("Right-hand-side"));

            LinearSolver<DenseMatrix64F> solver = LinearSolverFactory.leastSquares(100, 100);
            /** compute solution */
            if (!solver.setA(Qx_hat)) {
                throw new IllegalArgumentException("Singular Matrix");
            }
            solver.solve(rhsL, rhsL);
            solver.solve(rhsP, rhsP);
            logger.info("TIME FOR SOLVING of System: {}", stopWatch.lap("Solving System"));

            /** inverting of Qx_hat for stability check */
            solver.invert(Qx_hat);

            logger.info("TIME FOR INVERSION OF N: {}", stopWatch.lap("Inversion of N"));

            /** test inversion and check stability: max(abs([N*inv(N) - E)) ?= 0 */
            DenseMatrix64F tempMatrix_1 = new DenseMatrix64F(N.numRows, N.numCols);
            CommonOps.mult(N, Qx_hat, tempMatrix_1);
            CommonOps.subEquals(tempMatrix_1, CommonOps.identity(tempMatrix_1.numRows, tempMatrix_1.numCols));
            double maxDeviation = CommonOps.elementMaxAbs(tempMatrix_1);
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
            logger.debug("System Quality: {}", solver.quality());

            /** some other stuff if the scale is okay */
            DenseMatrix64F Qe_hat = new DenseMatrix64F(numObs, numObs);
            DenseMatrix64F tempMatrix_2 = new DenseMatrix64F(numObs, numUnk);

            CommonOps.mult(A, Qx_hat, tempMatrix_2);
            CommonOps.multTransB(-1, tempMatrix_2, A, Qe_hat);
            scaleInputDiag(Qe_hat, Qy_1);

            DenseMatrix64F yL_hat = new DenseMatrix64F(numObs, 1);
            DenseMatrix64F eL_hat = new DenseMatrix64F(numObs, 1);
            CommonOps.mult(A, rhsL, yL_hat);
            CommonOps.sub(yL_matrix, yL_hat, eL_hat);

            DenseMatrix64F yP_hat = new DenseMatrix64F(numObs, 1);
            DenseMatrix64F eP_hat = new DenseMatrix64F(numObs, 1);

            CommonOps.mult(A, rhsP, yP_hat);
            CommonOps.sub(yP_matrix, yP_hat, eP_hat);

            logger.info("TIME FOR DATA preparation for TESTING: {}", stopWatch.lap("Testing Setup"));

            /** overal model test (variance factor) */
            double overAllModelTest_L = 0;
            double overAllModelTest_P = 0;

            for (int i = 0; i < numObs; i++) {
                overAllModelTest_L += Math.pow(eL_hat.get(i), 2) * Qy_1.get(i);
                overAllModelTest_P += Math.pow(eP_hat.get(i), 2) * Qy_1.get(i);
            }

            /** WHAT IS THE REFERENCE FOR THESE CONSTANT VALUES???? */
            final double SIGMA_L = 0.15;
            final double SIGMA_P = 0.10;

            overAllModelTest_L = (overAllModelTest_L / Math.pow(SIGMA_L, 2)) / (numObs - numUnk);
            overAllModelTest_P = (overAllModelTest_P / Math.pow(SIGMA_P, 2)) / (numObs - numUnk);

            logger.debug("Overall Model Test Lines: {}", overAllModelTest_L);
            logger.debug("Overall Model Test Pixels: {}", overAllModelTest_P);

            logger.info("TIME FOR OMT: {}", stopWatch.lap("OMT"));

            /** ---------------------- DATASNOPING ----------------------------------- **/
            /** Assumed Qy diag */

            /** initialize */
            DenseMatrix64F wTest_L = new DenseMatrix64F(numObs, 1);
            DenseMatrix64F wTest_P = new DenseMatrix64F(numObs, 1);

            for (int i = 0; i < numObs; i++) {
                wTest_L.set(i, eL_hat.get(i) / (Math.sqrt(Qe_hat.get(i, i)) * SIGMA_L));
                wTest_P.set(i, eP_hat.get(i) / (Math.sqrt(Qe_hat.get(i, i)) * SIGMA_P));
            }

            /** find maxima's */
            // azimuth
            winL = absArgmax(wTest_L);
            double maxWinL = Math.abs(wTest_L.get(winL));
            logger.debug("maximum wtest statistic azimuth = {} for window number: {} ", maxWinL, index.get(winL));

            // range
            winP = absArgmax(wTest_P);
            double maxWinP = Math.abs(wTest_P.get(winP));
            logger.debug("maximum wtest statistic range = {} for window number: {} ", maxWinP, index.get(winP));

            /** use summed wTest in Azimuth and Range direction for outlier detection */
            DenseMatrix64F wTestSum = new DenseMatrix64F(numObs);
            for (int i = 0; i < numObs; i++) {
                wTestSum.set(i, Math.pow(wTest_L.get(i), 2) + Math.pow(wTest_P.get(i), 2));
            }

            maxWSum_idx = absArgmax(wTest_P);
            double maxWSum = wTest_P.get(winP);
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

    } // iterations remove outliers

    /**
     * C=diagxmat(vec,B) C=diag(vec) * B
     */
    public static DenseMatrix64F diagxmat(final RowD1Matrix64F diag, final RowD1Matrix64F B) {

        if (!MatrixFeatures.isVector(diag))
            logger.error("diagXMat: sizes A,B: diag is NOT vector.");

        DenseMatrix64F result = B.copy();
        for (int i = 0; i < result.numRows; i++) {
            for (int j = 0; j < result.numCols; j++) {
                result.unsafe_set(i, j, result.unsafe_get(i, j) * diag.get(i));
            }
        }
        return result;
    } // END diagxmat

    public static void scaleInputDiag(final RowD1Matrix64F matrix, final RowD1Matrix64F diag) {
        for (int i = 0; i < matrix.numRows; i++) {
            matrix.unsafe_set(i, i, matrix.unsafe_get(i, i) + 1 / diag.get(i));
        }
    }

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

    static class DiagXMat extends RecursiveAction {

        int mStart;
        int mLength;
        protected static int sThreshold = 100;

        RowD1Matrix64F diag;
        RowD1Matrix64F matrix;
        RowD1Matrix64F result; // return

        DiagXMat(RowD1Matrix64F diag, RowD1Matrix64F matrix, int start, int end, RowD1Matrix64F result) {
            if (!MatrixFeatures.isVector(diag))
                logger.error("diagXMat: sizes A,B: diag is NOT vector.");
            this.mStart = start;
            this.mLength = end;
            this.diag = diag;
            this.matrix = matrix;
            this.result = result;
        }

        public void computeDirectly() {
            for (int i = mStart; i < mLength; i++) {
                for (int j = 0; j < result.numCols; j++) {
                    result.unsafe_set(i, j, result.unsafe_get(i, j) * diag.get(i));
                }
            }
        }

        @Override
        protected void compute() {

            if (mLength < sThreshold) {
                computeDirectly();
                return;
            }

            int split = mLength / 2;

            invokeAll(new DiagXMat(diag, matrix, mStart, split, result),
                    new DiagXMat(diag, matrix, mStart + split, mLength - split, result));

        }
    }


}
