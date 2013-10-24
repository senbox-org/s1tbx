package org.jlinda.core.coregistration.estimation;

import org.jblas.Decompose;
import org.jblas.DoubleMatrix;
import org.jblas.MatrixFunctions;
import org.jblas.Solve;
import org.jlinda.core.coregistration.estimation.utils.SimpleAsciiFileParser;
import org.jlinda.core.utils.LinearAlgebraUtils;
import org.jlinda.core.utils.PolyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

import static org.jblas.MatrixFunctions.abs;

/**
 * Stub for estimation in Java
 */
public class Estimation {

    static Logger logger = LoggerFactory.getLogger(Estimation.class.getName());

    private static final String FILE_NAME = "/d1/list.txt";
    private static double[][] data = null;

    public static void main(String[] args) throws IOException {

        SimpleAsciiFileParser asciiFileParser = new SimpleAsciiFileParser(FILE_NAME);
        data = asciiFileParser.parseDoubleArray();

        createSystemOfEquations();
        solveSystemOfEquations();
        // checks
        checkStabilityOfEstimations();

        // data-snooping testing
        dataSnooping();

    }

    private static void createSystemOfEquations() {

        // values for modeling polynomial
        final double THRESHOLD = 0.4d;
        final int DEGREE = 2;

        final long MAX_ITERATIONS = 1000;// max. of pnts to remove
        final double CRIT_VALUE = 1.97;// crit. value outlier removal

        final int Nunk = PolyUtils.numberOfCoefficients(DEGREE);

        final int weightflag = 0;


        // Start with initial values
        final int osfactor = 32;// oversamplingsfactor
        int corrwinL = 64;// window size to compute FINE correlation
        int corrwinP = 64;// window size to compute FINE correlation

        // factors for normalization : hardcoding it here
        final double minL = 1;
        final double maxL = 26672;
        final double minP = 1;
        final double maxP = 5142;

        corrwinL = Math.max(10, corrwinL - 8);
        corrwinP = Math.max(10, corrwinP - 8);

//        // _____ oversampling factor is bin in which maximum can be found _____
//        // _____ ovsf=16-->apriorisigma=0.03
        final double ACCURACY = 0.5 * (1.0 / (double) osfactor);

        // if the image is oversampled, then still use orig spacing
        double SIGMAL = -999.9;// sigma in orig pixels
        double SIGMAP = -999.9;// seems range direction is better???

        DoubleMatrix Data = new DoubleMatrix(data);

        int ITERATION = 0;
        int DONE = 0;
        // sqr: level significance: alpha=0.001; power of test: gamma=0.80
        //real4 CRIT_VALUE = sqrt(3.29);
        logger.info("Critical value for outlier test: {}", CRIT_VALUE);
        int winL = 0;// window number to be removed
        int winP = 0;// window number of largest w -test in range
        DoubleMatrix eL_hat;
        DoubleMatrix eP_hat;
        DoubleMatrix wtestL;
        DoubleMatrix wtestP;
        DoubleMatrix rhsL;
        DoubleMatrix rhsP;
        DoubleMatrix Qx_hat;
        double maxdev = 0.0;
        double overallmodeltestL = 0.0;
        double overallmodeltestP = 0.0;
        double maxwL;
        double maxwP;
        int i, j, k, index;


        while (DONE != 1) {

            logger.info("Start iteration: {} ", ITERATION);

            // ______ Remove identified outlier from previous estimation ______
            if (ITERATION != 0) {
                DoubleMatrix tmp_DATA = Data.dup(); //(remove_observation_i,*);
                Data.resize(Data.rows - 1, Data.columns);
                j = 0;// counter over reduced obs.vector
                for (i = 0; i < tmp_DATA.rows; i++) {// counter over original window numbers
                    if (i != winL) { // do not copy the one to be removed.
                        Data.putRow(j, tmp_DATA.getRow(i));// copy back without removed obs.
                        j++;// fill next row of Data
                    } else {
                        logger.info("Removing observation " + i + " from observation vector.");
                    }
                }
            }

            // ______Check redundancy______
            int Nobs = Data.rows; // Number of points > threshold
            if (Nobs < Nunk) {
                logger.error("coregpm: Number of windows > threshold is smaller than parameters solved for.");
                throw new ArithmeticException("coregpm: Number of windows > threshold is smaller than parameters solved for.");
            }

            // ______Set up system of equations______
            // ______Order unknowns: A00 A10 A01 A20 A11 A02 A30 A21 A12 A03 for degree=3______
            DoubleMatrix yL = new DoubleMatrix(Nobs, 1); // observation
            DoubleMatrix yP = new DoubleMatrix(Nobs, 1); // observation
            DoubleMatrix A = new DoubleMatrix(Nobs, Nunk); // designmatrix
            DoubleMatrix Qy_1 = new DoubleMatrix(Nobs, 1); // a priori covariance matrix (diag)

            // ______ Normalize data for polynomial ______
            logger.info("coregpm: polynomial normalized by factors: " + minL + " " + maxL + " " + minP + " " + maxP + " to [-2,2]");


            // ______Fill matrices______
            logger.debug("Setting up design matrix for LS adjustment");
            for (i = 0; i < Nobs; i++) {
                double posL = PolyUtils.normalize2(Data.get(i, 1), minL, maxL);
                double posP = PolyUtils.normalize2(Data.get(i, 2), minP, maxP);
                yL.put(i, 0, Data.get(i, 3));
                yP.put(i, 0, Data.get(i, 4));

                logger.debug("coregpm: (" + posL + ", " + posP + "): yL=" + yL.get(i, 0) + " yP=" + yP.get(i, 0));

                // ______Set up designmatrix______
                index = 0;
                for (j = 0; j <= DEGREE; j++) {
                    for (k = 0; k <= j; k++) {
                        A.put(i, index, Math.pow(posL, j - k) * Math.pow(posP, k));
                        index++;
                    }
                }
            }

            // ______Weight matrix data______
            logger.debug("Setting up (inverse of) covariance matrix for LS adjustment");
            switch (weightflag) {
                case 0:
//                    for (i = 0; i < Nobs; i++)
//                        Qy_1.put(i, 0, 1.0d);
                    Qy_1 = DoubleMatrix.ones(Nobs, 1);
                    break;
                case 1:
                    logger.debug("Using sqrt(coherence) as weights.");

                    Qy_1 = Data.getColumn(5);
//                    for (i = 0; i < Nobs; i++)
//                        Qy_1(i, 0) = real8(Data(i, 5));// more weight to higher correlation
                    // ______ Normalize weights to avoid influence on estimated var.factor ______
                    logger.info("Normalizing covariance matrix for LS estimation.");
//                    Qy_1 = Qy_1 / mean(Qy_1);// normalize weights (for tests!)
                    Qy_1.divi(Qy_1.mean());
                    break;
                case 2:
                    logger.debug("Using coherence as weights.");

                    Qy_1 = MatrixFunctions.pow(Data.getColumn(5), 2);
//                    for (i = 0; i < Nobs; i++)
//                        Qy_1(i, 0) = real8(Data(i, 5)) * real8(Data(i, 5));// more weight to higher correlation
                    // ______ Normalize weights to avoid influence on estimated var.factor ______
                    logger.info("Normalizing covariance matrix for LS estimation.");
//                    Qy_1 = Qy_1 / mean(Qy_1);// normalize weights (for tests!)
                    Qy_1.divi(Qy_1.mean());
                    break;
//                // --- Bamler paper igarss 2000 and 2004; Bert Kampes, 16-Aug-2005 ---
//                case 3:
//                    // for coherent cross-correlation the precision of the shift is
//                    // sigma_cc = sqrt(3/(2N))*sqrt(1-coh^2)/(pi*coh) in units of pixels
//                    // for incoherent cross-correlation as we do, sigma seems approx. [BK]
//                    // sigma_ic = sqrt(2/coh)*sigma_cc
//                    // actually with osf^1.5 (but we will ignore that here)
//                    // it seems for large N this is to optimistic, maybe because of a bias
//                    // in the coherence estimator, or some other reason;  in any case,
//                    // the result is a large number of warnings.
//                    logger.debug("Using expression Bamler04 as weights.");
//                    for (i = 0; i < Nobs; i++) {
//                        // N_corr: number of samples for cross-corr; approx. FC_WINSIZE
//                        // number of effictive samples depends on data ovs factor
//                        // Bamler 2000: also on oversampling ratio of data, but ignored here.
//                        double N_corr = (corrwinL * corrwinP) /(master.getOvsAz() * master.getOvsRg());
//                        double coh = Data.get(i, 5);// estimated correlation; assume unbiased?
//                        double sigma_cc = Math.sqrt(3.0 / (2.0 * N_corr)) * Math.sqrt(1.0 - Math.pow(coh, 2)) / (Constants.PI * coh);
//                        double sigma_ic = Math.sqrt(2.0 / coh) * sigma_cc;
//                        logger.debug("Window " + i + ": estimated coherence   = " + coh);
//                        logger.debug("Window " + i + ": sigma(estimated shift) for coherent cross-correlation = " + sigma_cc + " [pixel]");
//                        logger.debug("Window " + i + ": sigma(estimated shift) = " + sigma_ic + " [pixel]");
//                        Qy_1.put(i, 0, 1.0 / Math.sqrt(sigma_ic));// Qy_1=diag(inverse(Qy));
//                        SIGMAL = 1.0;// remove this factor effectively
//                        SIGMAP = 1.0;// remove this factor effectively
//                    }
//                    break;
                default:
                    logger.error("Panic, CPM not possible with checked input.");
                    throw new IllegalArgumentException("Panic, CPM not possible with checked input.");
            }

            // ______Compute Normalmatrix, rghthandside______
            DoubleMatrix N = LinearAlgebraUtils.matTxmat(A, diagxmat(Qy_1, A));
            rhsL = LinearAlgebraUtils.matTxmat(A, diagxmat(Qy_1, yL));
            rhsP = LinearAlgebraUtils.matTxmat(A, diagxmat(Qy_1, yP));
            Qx_hat = N.dup();

            // ______Compute solution______
//            LinearAlgebraUtils.chol_inplace(Qx_hat); // Cholesky factorisation normalmatrix
//            solvechol(Qx_hat, rhsL); // Solution unknowns in rhs
//            solvechol(Qx_hat, rhsP); // Solution unknowns in rhs
            rhsL = Solve.solve(Qx_hat, rhsL);
            rhsP = Solve.solve(Qx_hat, rhsP);

            Qx_hat = Decompose.cholesky(Qx_hat).transpose();
            // TODO: implement checks on Stability of the system

            LinearAlgebraUtils.invertChol_inplace(Qx_hat);
            LinearAlgebraUtils.arrangeCholesky_inplace(Qx_hat); // repair Qx

//            maxdev = max(abs(N * Qx_hat - eye(real8(Qx_hat.lines()))));

            maxdev = abs(N.mmul(Qx_hat).sub(DoubleMatrix.eye(Qx_hat.rows))).max();
            logger.info("coregpm: max(abs(N*inv(N)-I)) = {}", maxdev);

            if (maxdev > .01) {
                logger.error("coregpm: maximum deviation N*inv(N) from unity = {}. This is larger than 0.01", maxdev);
                throw new IllegalStateException("coregpm: maximum deviation N*inv(N) from unity)");
            } else if (maxdev > .001) {
                logger.warn("coregpm: maximum deviation N*inv(N) from unity = {}. This is between 0.01 and 0.001", maxdev);
            }


            // ______Some other stuff, scale is ok______
            DoubleMatrix Qy_hat = A.mmul(matxmatT(Qx_hat, A));
            DoubleMatrix yL_hat = A.mmul(rhsL);
            DoubleMatrix yP_hat = A.mmul(rhsP);
            eL_hat = yL.sub(yL_hat);
            eP_hat = yP.sub(yP_hat);
            //  matrix<real4> Qe_hat    = Qy - Qy_hat;
            DoubleMatrix Qe_hat = Qy_hat.neg();

            for (i = 0; i < Nobs; i++) {
                double value = Qe_hat.get(i, i) + (1. / Qy_1.get(i, 0));
                Qe_hat.put(i, i, value);
            }

        }

    }

    private static void solveSystemOfEquations() {
        // solve using jblas calls (no internal stuff) -- jblas is much faster

    }

    private static void checkStabilityOfEstimations() {
        // inversion checks

    }

    private static void dataSnooping() {


    }

    private static DoubleMatrix diagxmat(DoubleMatrix vector, DoubleMatrix matrix) {
        // TODO: this will be very slow, perheps better to loop?!
        DoubleMatrix diagonal = DoubleMatrix.diag(vector);
        return diagonal.mul(matrix);
    }

    private static DoubleMatrix matxmatT(DoubleMatrix a, DoubleMatrix b) {
        return a.mmul(b.transpose());
    }

}
