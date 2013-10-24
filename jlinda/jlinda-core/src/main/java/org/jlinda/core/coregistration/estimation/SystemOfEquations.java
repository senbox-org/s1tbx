package org.jlinda.core.coregistration.estimation;

import gnu.trove.list.array.TFloatArrayList;
import gnu.trove.map.hash.TIntObjectHashMap;
import org.apache.commons.lang.ArrayUtils;
import org.jblas.DoubleMatrix;
import org.jblas.FloatMatrix;
import org.jblas.MatrixFunctions;
import org.jlinda.core.utils.PolyUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static org.jblas.MatrixFunctions.pow;

public class SystemOfEquations {

    private static Logger logger = LoggerFactory.getLogger(SystemOfEquations.class.getName());
    private int nObs;
    private DoubleMatrix data;
    private int minL;
    private int maxL;
    private int maxP;
    private int minP;

    DoubleMatrix yL = null;
    DoubleMatrix yP = null;
    DoubleMatrix A = null;
    DoubleMatrix Qy_1 = null;

    private static final int degree = 0;
    private int weightflag;

    /**
     * Fill Design and Observation matrices
     */
    public void fillDesignMatrix() {

        logger.debug("Setting up design matrix for LS adjustment");
        for (int i = 0; i < nObs; i++) {

            /** azimuth direction */
            yL.put(i, 0, data.get(i, 3));
            double posL = PolyUtils.normalize2(data.get(i, 1), minL, maxL);

            /** range direction */
            yP.put(i, 0, data.get(i, 4));
            double posP = PolyUtils.normalize2(data.get(i, 2), minP, maxP);

//                logger.debug("coregpm: ({}, {}): yL = {} , yP = {} .", posL, posP, yL.get(i, 0), yP.get(i, 0));

            /** Set up designmatrix */
            int index = 0;
            for (int j = 0; j <= degree; j++) {
                for (int k = 0; k <= j; k++) {
                    A.put(i, index, Math.pow(posL, j - k) * Math.pow(posP, k));
                    index++;
                }
            }
        }

    }

    /**
     * Construct design matrix for fitting of 2d polynomial to input data - Jblas implementation.
     *
     * @param line   vector of coordinates of points in azimuth direction
     * @param pixel  vactor of coordinates of points in range direction
     * @param degree polynomial degree
     * @return Design Matrix
     */
    public static DoubleMatrix constructDesignMatrix(final DoubleMatrix line, final DoubleMatrix pixel, final int degree) {
        DoubleMatrix A = new DoubleMatrix();
        DoubleMatrix mul;
        /** Set up design-matrix */
        for (int p = 0; p <= degree; p++) {
            for (int q = 0; q <= p; q++) {
                mul = pow(line, (p - q)).mul(pow(pixel, q));
                if (q == 0 && p == 0) {
                    A = mul;
                } else {
                    A = DoubleMatrix.concatHorizontally(A, mul);
                }
            }
        }
        return A;
    }

    /**
     * Construct design matrix for fitting of 2d polynomial to input data - Jblas implementation.
     *
     * @param line   vector of coordinates of points in azimuth direction
     * @param pixel  vactor of coordinates of points in range direction
     * @param degree polynomial degree
     * @return Design Matrix
     */
    public static FloatMatrix constructDesignMatrix(final FloatMatrix line, final FloatMatrix pixel, final int degree) {
        FloatMatrix A = new FloatMatrix();
        FloatMatrix mul;
        /** Set up design-matrix */
        for (int p = 0; p <= degree; p++) {
            for (int q = 0; q <= p; q++) {
                mul = pow(line, (p - q)).mul(pow(pixel, q));
                if (q == 0 && p == 0) {
                    A = mul;
                } else {
                    A = FloatMatrix.concatHorizontally(A, mul);
                }
            }
        }
        return A;
    }

    /**
     * Construct design matrix for fitting of 2d polynomial to input data - Trove MAP implementation.
     *
     * @param line   vector of coordinates of points in azimuth direction
     * @param pixel  vactor of coordinates of points in range direction
     * @param degree polynomial degree
     * @return Design Matrix
     */
    public static TIntObjectHashMap<float[]> constructDesignMatrix_FloatMAP(final float[] line, final float[] pixel, final int degree) {

        final int nObs = line.length;
        final int nUnkn = PolyUtils.numberOfCoefficients(degree);
        final TIntObjectHashMap<float[]> list = new TIntObjectHashMap<>();

        logger.debug("Setting up design matrix for LS adjustment");
        /** Set up designmatrix */
        for (int i = 0; i < nObs; i++) {
            final float[] mul = new float[nUnkn];
            int index = 0;
            for (int p = 0; p <= degree; p++) {
                for (int q = 0; q <= p; q++) {
                    mul[index++] = (float) (Math.pow(line[i], p - q) * Math.pow(pixel[i], q));
                }
            }
            list.put(i, mul);
        }
        return list;
    }

    /**
     * Construct design matrix for fitting of 2d polynomial to input data - Trove MAP implementation.
     *
     * @param line   vector of coordinates of points in azimuth direction
     * @param pixel  vactor of coordinates of points in range direction
     * @param degree polynomial degree
     * @return Design Matrix
     */
    public static TIntObjectHashMap<float[]> constructDesignMatrix_Trove(final TFloatArrayList line, final TFloatArrayList pixel, final int degree) {

        final int nObs = line.size();
        final int nUnkn = PolyUtils.numberOfCoefficients(degree);
        final TIntObjectHashMap<float[]> list = new TIntObjectHashMap<>();

        logger.debug("Setting up design matrix for LS adjustment");
        /** Set up designmatrix */
        for (int i = 0; i < nObs; i++) {
            final float[] mul = new float[nUnkn];
            int index = 0;
            for (int p = 0; p <= degree; p++) {
                for (int q = 0; q <= p; q++) {
                    mul[index++] = (float) (Math.pow(line.getQuick(i), p - q) * Math.pow(pixel.getQuick(i), q));
                }
            }
            list.put(i, mul);
        }
        return list;
    }

    /**
     * Construct design matrix for fitting of 2d polynomial to input data - Trove MAP implementation.
     * Ordering of equations is reverse wrt other implementations!
     * Example: 1st eq in java loop implementation will be last in Trove's.
     *
     * @param line   vector of coordinates of points in azimuth direction
     * @param pixel  vactor of coordinates of points in range direction
     * @param degree polynomial degree
     * @return Design Matrix
     */
    public static TIntObjectHashMap<double[]> constructDesignMatrix_DoubleMAP(final double[] line, final double[] pixel, final int degree) {

        final int nObs = line.length;
        final int nUnkn = PolyUtils.numberOfCoefficients(degree);
        final TIntObjectHashMap<double[]> list = new TIntObjectHashMap<>();

        logger.debug("Setting up design matrix for LS adjustment");

        /** Set up designmatrix */
        for (int i = 0; i < nObs; i++) {
            int index = 0;
            final double[] mul = new double[nUnkn];
            for (int p = 0; p <= degree; p++) {
                for (int q = 0; q <= p; q++) {
                    mul[index++] = (Math.pow(line[i], p - q) * Math.pow(pixel[i], q));
                }
            }
            list.put(i, mul);
        }
        return list;
    }

    /**
     * Construct design matrix for fitting of 2d polynomial to input data - Java array implementation.
     * Approximately 10% slower then Jblas implementation
     *
     * @param line   vector of coordinates of points in azimuth direction
     * @param pixel  vactor of coordinates of points in range direction
     * @param degree polynomial degree
     * @return Design Matrix
     */
    public static double[][] constructDesignMatrix_loop(final double[] line, final double[] pixel, final int degree) {

        final int nObs = line.length;
        final int nUnkn = PolyUtils.numberOfCoefficients(degree);
        final double[][] A = new double[nObs][nUnkn];

        logger.debug("Setting up design matrix for LS adjustment");
        /** Set up designmatrix */
        for (int i = 0; i < nObs; i++) {
            int index = 0;
            for (int p = 0; p <= degree; p++) {
                for (int q = 0; q <= p; q++) {
                    A[i][index] = Math.pow(line[i], p - q) * Math.pow(pixel[i], q);
                    index++;
                }
            }
        }
        return A;
    }

    /**
     * Construct design matrix for fitting of 2d polynomial to input data - Java array implementation.
     * Approximately 10% slower then Jblas implementation
     *
     * @param line   vector of coordinates of points in azimuth direction
     * @param pixel  vactor of coordinates of points in range direction
     * @param degree polynomial degree
     * @return Design Matrix
     */
    public static float[][] constructDesignMatrix_float_loop(final float[] line, final float[] pixel, final int degree) {

        final int nObs = line.length;
        final int nUnkn = PolyUtils.numberOfCoefficients(degree);
        final float[][] A = new float[nObs][nUnkn];

        logger.debug("Setting up design matrix for LS adjustment");
        /** Set up designmatrix */
        for (int i = 0; i < nObs; i++) {
            int index = 0;
            for (int p = 0; p <= degree; p++) {
                for (int q = 0; q <= p; q++) {
                    A[i][index] = (float) (Math.pow(line[i], p - q) * Math.pow(pixel[i], q));
                    index++;
                }
            }
        }
        return A;
    }

    public static void main(String[] args) {

        double[][] array2D_64F = {{1}, {2}};
        double[] array1D_64F = {1, 2};
        float[] array1D_32F = {1, 2};
        DoubleMatrix line = new DoubleMatrix(array2D_64F);
        DoubleMatrix pixel = new DoubleMatrix(array2D_64F);

        DoubleMatrix matrixA_64F = constructDesignMatrix(line, pixel, 2);
        double[][] arrayA = constructDesignMatrix_loop(array1D_64F, array1D_64F, 2);

        System.out.println("ArrayUtils = " + ArrayUtils.toString(arrayA));
        System.out.println("difference = " + matrixA_64F.sub(new DoubleMatrix(arrayA)).toString());

        logger.info("Testing Double Map");
        TIntObjectHashMap<double[]> mapA_64F = constructDesignMatrix_DoubleMAP(array1D_64F, array1D_64F, 2);
        System.out.println("Map = " + ArrayUtils.toString(mapA_64F.values()));

        logger.info("Testing Float Map");
        TIntObjectHashMap<float[]> mapA_32F = constructDesignMatrix_FloatMAP(array1D_32F, array1D_32F, 2);
        System.out.println("Map = " + ArrayUtils.toString(mapA_32F.values()));

        logger.info("Testing Trove Float Map");
        TIntObjectHashMap<float[]> mapA_32F_Trove = constructDesignMatrix_Trove(new TFloatArrayList(array1D_32F), new TFloatArrayList(array1D_32F), 2);
        System.out.println("Map = " + ArrayUtils.toString(mapA_32F_Trove.values()));


    }


    public void constructWeightMatrix() {

        // ______Weight matrix data______
        logger.debug("Setting up (inverse of) covariance matrix for LS adjustment");
        switch (weightflag) {
            case 0:
//                    for (i = 0; i < Nobs; i++)
//                        Qy_1.put(i, 0, 1.0d);
                Qy_1 = DoubleMatrix.ones(nObs, 1);
                break;
            case 1:
                logger.debug("Using sqrt(coherence) as weights.");

                Qy_1 = data.getColumn(5);
//                    for (i = 0; i < Nobs; i++)
//                        Qy_1(i, 0) = real8(Data(i, 5));// more weight to higher correlation
                // ______ Normalize weights to avoid influence on estimated var.factor ______
                logger.info("Normalizing covariance matrix for LS estimation.");
//                    Qy_1 = Qy_1 / mean(Qy_1);// normalize weights (for tests!)
                Qy_1.divi(Qy_1.mean());
                break;
            case 2:
                logger.debug("Using coherence as weights.");

                Qy_1 = MatrixFunctions.pow(data.getColumn(5), 2);
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


    }
}
