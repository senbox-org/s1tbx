package org.jlinda.core.coregistration.estimation;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import gnu.trove.iterator.TIntObjectIterator;
import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.list.array.TIntArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.jblas.FloatMatrix;
import org.jblas.Solve;
import org.jlinda.core.coregistration.estimation.utils.SimpleAsciiFileParser;
import org.jlinda.core.utils.PolyUtils;
import org.perf4j.StopWatch;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.jblas.MatrixFunctions.abs;
import static org.jblas.MatrixFunctions.pow;


public class TestEstimation_FloatMatrix {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(TestEstimation_FloatMatrix.class);

    public static void main(String[] args) throws IOException {

        StopWatch clockFull = new StopWatch();
        clockFull.start();

        logger.setLevel(Level.INFO);
        logger.trace("Start Estimation");

        /** estimation without Data Snooping -- only AdjustA */

        // work only with arrays, FloatMatrix is used only in estimation!!!

        /** load data */
//        SimpleAsciiFileParser fileParser = new SimpleAsciiFileParser("/d1/list.ttt.txt");
//        String inputFile = "CPM_Data.linear_weight.csv";
        String inputFile = "CPM_Data.none_weight.BIG.csv";
//        String inputFile = "cpm_input";
        String inputDir = "/d2/test.processing/unit_tests/etna.volcano/process/crop/01486_21159.cpm/";


        /** define input parameters */
        // only degree
        int degree = 2;
        String weight = "linear";
//        String weight = "quadratic";

        int numLines = 10000;

        boolean DONE = false;
        int ITERATION = 0;
        int MAX_ITERATIONS = 10;
        final double COH_TRESHOLD = 0.35d;

        final double CRIT_VALUE = 1.97d;

        final int minL = 1;
        final int maxL = 26292;
        final int minP = 1;
        final int maxP = 4900;

        int winL;
        int winP;

        SimpleAsciiFileParser fileParser = new SimpleAsciiFileParser(inputDir + inputFile, numLines);
//        double[][] data = fileParser.parseDoubleArray();
        TIntObjectHashMap<float[]> data = fileParser.parseFloatMap();

//        List<double[]> dataArray = Arrays.asList(data);
        /** reformat data to _processing_ structure : using CPM_DATA like file for testing */
        // (0) idx (1) PosL (2) PosX (3) OffL (4) OffX (5) Coherence (6) eL (7) eP (8) wtestL (9) wtestP
//        int numObs = data.length;
        int numObs;
        int numUnk = PolyUtils.numberOfCoefficients(degree);

        /** ??? if i implement this in separate loops will JIT and HotSpot inline these? ??? */

        // allocate collections
        TIntArrayList index = new TIntArrayList();
        TFloatArrayList lines = new TFloatArrayList();
        TFloatArrayList pixels = new TFloatArrayList();
        TFloatArrayList yL = new TFloatArrayList();
        TFloatArrayList yP = new TFloatArrayList();
        TFloatArrayList coh = new TFloatArrayList();

        TIntObjectIterator<float[]> it = data.iterator();
        for (int i = data.size(); i-- > 0; ) {
            it.advance();
            float[] entry = it.value();
            float coherence = entry[5];
            if (coherence > COH_TRESHOLD) {
                index.add((int) entry[0]);
                lines.add((float) PolyUtils.normalize2(entry[1], minL, maxL));
                pixels.add((float) PolyUtils.normalize2(entry[2], minP, maxP));
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


            FloatMatrix A = SystemOfEquations.constructDesignMatrix(new FloatMatrix(lines.toArray()), new FloatMatrix(pixels.toArray()), degree);
            final FloatMatrix A_transpose = A.transpose();
            logger.info("TIME FOR SETUP of SYSTEM : {}", stopWatch.lap("setup"));


            FloatMatrix Qy_1; // vector
            switch (weight) {
                case "linear":
                    logger.debug("Using sqrt(coherence) as weights");
                    Qy_1 = new FloatMatrix(coh.toArray());
                    // Normalize weights to avoid influence on estimated var.factor
                    logger.debug("Normalizing covariance matrix for LS estimation");
                    Qy_1.divi(Qy_1.mean()); // normalize vector
                    break;
                case "quadratic":
                    logger.debug("Using coherence as weights.");
                    Qy_1 = new FloatMatrix(coh.toArray());
                    Qy_1.muli(Qy_1);
                    // Normalize weights to avoid influence on estimated var.factor
                    logger.debug("Normalizing covariance matrix for LS estimation.");
                    Qy_1.divi(Qy_1.mean()); // normalize vector
                    break;
                case "bamler":
                    // TODO: see Bamler papers IGARSS 2000 and 2004
                    logger.warn("Bamler weighting method NOT IMPLEMENTED, falling back to None.");
                    Qy_1 = FloatMatrix.ones(numObs);
                    break;
                case "none":
                    logger.debug("No weighting.");
                    Qy_1 = FloatMatrix.ones(numObs);
                    break;
                default:
                    Qy_1 = FloatMatrix.ones(numObs);
                    break;
            }

            logger.info("TIME FOR SETUP of VC vector: {}", stopWatch.lap("VC vector"));

//            final FloatMatrix Qy_1_diag = FloatMatrix.diag(Qy_1);
            logger.info("TIME FOR SETUP of VC diag matris: {}", stopWatch.lap("diag VC matrix"));


            /** temp matrices */
            final FloatMatrix yL_matrix = new FloatMatrix(yL.toArray());
            final FloatMatrix yP_matrix = new FloatMatrix(yP.toArray());
            logger.info("TIME FOR SETUP of TEMP MATRICES: {}", stopWatch.lap("Temp matrices"));

            /** normal matrix */
//            final FloatMatrix N = A_transpose.mmul(Qy_1_diag.mmul(A));
            final FloatMatrix N = A_transpose.mmul(diagXMat(Qy_1, A));
            FloatMatrix Qx_hat = N.dup(); // store N into Qx_hat
            logger.info("TIME FOR SETUP of NORMAL MATRIX: {}", stopWatch.lap("Normal matrix"));

            /** right hand sides */
//            FloatMatrix rhsL = A_transpose.mmul(Qy_1_diag.mmul(yL_matrix));
//            FloatMatrix rhsP = A_transpose.mmul(Qy_1_diag.mmul(yP_matrix));
            FloatMatrix rhsL = A_transpose.mmul(diagXMat(Qy_1, yL_matrix));
            FloatMatrix rhsP = A_transpose.mmul(diagXMat(Qy_1, yP_matrix));
            logger.info("TIME FOR SETUP of RightHand Side: {}", stopWatch.lap("Right-hand-side"));

            /** compute solution */
            rhsL = Solve.solvePositive(Qx_hat, rhsL);
            rhsP = Solve.solvePositive(Qx_hat, rhsP);
            logger.info("TIME FOR SOLVING of System: {}", stopWatch.lap("Solving System"));

            /** inverting of Qx_hat for stability check */
            Qx_hat = Solve.solvePositive(Qx_hat, FloatMatrix.eye(Qx_hat.getRows())); // store inverted N back into Qx_hat
            logger.info("TIME FOR INVERSION OF N: {}", stopWatch.lap("Inversion of N"));

            /** test inversion and check stability: max(abs([N*inv(N) - E)) ?= 0 */
            double maxDeviation = (N.mmul(Qx_hat).sub(FloatMatrix.eye(Qx_hat.getRows()))).normmax();
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
            FloatMatrix Qy_hat = A.mmul(Qx_hat.mmul(A_transpose));
            FloatMatrix Qe_hat = Qy_hat.muli(-1f);
            Qe_hat.addi(FloatMatrix.diag(FloatMatrix.ones(Qy_1.length).div(Qy_1)));

            FloatMatrix yL_hat = A.mmul(rhsL);
            FloatMatrix eL_hat = yL_matrix.sub(yL_hat);

            FloatMatrix yP_hat = A.mmul(rhsP);
            FloatMatrix eP_hat = yP_matrix.sub(yP_hat);
//            scale diagonal
            logger.info("TIME FOR DATA preparation for TESTING: {}", stopWatch.lap("Testing Setup"));

            /** overal model test (variance factor) */
            double overAllModelTest_L;
            double overAllModelTest_P;

/*
        for (int i = 0; i < numObs; i++) {
            overAllModelTest_L += Math.pow(eL_hat.get(i), 2) * Qy_1.get(i);
            overAllModelTest_P += Math.pow(eP_hat.get(i), 2) * Qy_1.get(i);
        }
*/
            overAllModelTest_L = (pow(eL_hat, 2).mul(Qy_1)).sum();
            overAllModelTest_P = (pow(eP_hat, 2).mul(Qy_1)).sum();
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
            FloatMatrix wTest_L = new FloatMatrix(numObs);
            FloatMatrix wTest_P = new FloatMatrix(numObs);

            for (int i = 0; i < numObs; i++) {
                wTest_L.put(i, (float) (eL_hat.get(i) / (Math.sqrt(Qe_hat.get(i, i)) * SIGMA_L)));
                wTest_P.put(i, (float) (eP_hat.get(i) / (Math.sqrt(Qe_hat.get(i, i)) * SIGMA_P)));
            }

            /** find maxima's */
            // azimuth
            winL = abs(wTest_L).argmax();
            double maxWinL = abs(wTest_L).get(winL);
            logger.debug("maximum wtest statistic azimuth = {} for window number: {} ", maxWinL, index.get(winL));

            // range
            winP = abs(wTest_P).argmax();
            double maxWinP = abs(wTest_P).get(winP);
            logger.debug("maximum wtest statistic range = {} for window number: {} ", maxWinP, index.get(winP));

            /** use summed wTest in Azimuth and Range direction for outlier detection */
            FloatMatrix wTestSum = pow(wTest_L, 2).add(pow(wTest_P, 2));
            maxWSum_idx = abs(wTestSum).argmax();
            double maxWSum = abs(wTestSum).get(maxWSum_idx);
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
                logger.info("Time for {} iterations {}: ", ITERATION, clockFull.getElapsedTime());

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

    public static FloatMatrix diagXMat(final FloatMatrix diag, final FloatMatrix B) {

        if (!diag.isVector())
            logger.error("diagXMat: sizes A,B: diag is NOT vector.");

        FloatMatrix result = B.dup();

        for (int i = 0; i < result.getRows(); i++) {
            for (int j = 0; j < result.getColumns(); j++) {
                result.put(i, j, result.get(i, j) * diag.get(i));
            }
        }
        return result;
    } // END diagxmat


}
