package org.jlinda.core.coregistration;

import org.apache.commons.math3.util.FastMath;
import org.ejml.data.DMatrixRMaj;
import org.ejml.data.DMatrix1Row;
import org.ejml.dense.row.factory.LinearSolverFactory_DDRM;
import org.ejml.dense.row.CommonOps_DDRM;
import org.ejml.interfaces.linsol.LinearSolverDense;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Placemark;
import org.esa.snap.core.datamodel.ProductNodeGroup;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.util.SystemUtils;
import org.jlinda.core.Orbit;
import org.jlinda.core.Point;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.jlinda.core.coregistration.estimation.SystemOfEquations;
import org.jlinda.core.utils.PolyUtils;

import javax.media.jai.WarpPolynomial;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import static org.jlinda.core.coregistration.utils.CPMUtils.*;

public class CPM implements PolynomialModel {

    private static final Logger logger = SystemUtils.LOG;

    private final List<Placemark> slaveGCPList = new ArrayList<>();

    private final int cpmDegree;
    private final int numUnknowns;
    private int numObservations;
    private int maxIterations;
    public double criticalValue;
    private String cpmWeight;

    // control flags
    private boolean doEstimation = true;
    public boolean noRedundancy = false;

    /** Empirically pre-calculated values - See lecture series on 'estimation theory' of PT */
    private final static double SIGMA_L = 0.15;
    private final static double SIGMA_P = 0.10;

    private Window normWin; // used for normalization in estimation for numerical stability

    // allocate arrays and collections
    private final List<Integer> index = new ArrayList<>();
    private final List<Double> yMaster = new ArrayList<>();
    private final List<Double> xMaster = new ArrayList<>();
    private final List<Double> ySlave = new ArrayList<>();
    private final List<Double> xSlave = new ArrayList<>();
    private final List<Double> yOffset = new ArrayList<>();
    private final List<Double> xOffset = new ArrayList<>();
    private double[] yError;
    private double[] xError;
    private final List<Double> coherence = new ArrayList<>();

    private double[] heightMaster;

    // statistics -- for legacy
    private final List<Double> rms = new ArrayList<>();
    private double rmsStd = 0;
    private double rmsMean = 0;
    private double rowResidualStd = 0;
    private double rowResidualMean = 0;
    private double colResidualStd = 0;
    private double colResidualMean = 0;

    // JAI polynomial -- for legacy

    private WarpPolynomial jaiWarp = null;

    // allocated cpm arrays
    private double[] xCoef;
    private double[] yCoef;

    // metadata needed for dem refinement
    private SLCImage masterMeta;
    private SLCImage slaveMeta;
    private Orbit masterOrbit;
    private Orbit slaveOrbit;

    // dem parameters
    private float demNoDataValue;

    public CPM(final int cpmDegree, final int maxIterations, final float criticalValue, final Window normalWindow,
               final ProductNodeGroup<Placemark> masterGCPGroup, ProductNodeGroup<Placemark> slaveGCPGroup) {

        this.numObservations = slaveGCPGroup.getNodeCount();
        this.cpmDegree = cpmDegree;
        this.numUnknowns = PolyUtils.numberOfCoefficients(cpmDegree);

        if (numObservations < numUnknowns) {
            this.noRedundancy = true;
            logger.severe("Number of windows > threshold is smaller than parameters solved for.");
        }

        if (!noRedundancy) {

            this.maxIterations = maxIterations;
            this.criticalValue = criticalValue;
            this.cpmWeight = "none";

            this.xCoef = new double[numUnknowns];
            this.yCoef = new double[numUnknowns];

            this.normWin = normalWindow; // master dimensions

            double tempSum = 0.0d;

            // populate arrays and collections
            for (int i = 0; i < numObservations; i++) {

                slaveGCPList.add(slaveGCPGroup.get(i));
                final Placemark sPin = slaveGCPList.get(i);
                final PixelPos sGCPPos = sPin.getPixelPos();
                //System.out.println("WARP: slave gcp[" + i + "] = " + "(" + sGCPPos.x + "," + sGCPPos.y + ")");

                final Placemark mPin = masterGCPGroup.get(sPin.getName());
                final PixelPos mGCPPos = mPin.getPixelPos();
                //System.out.println("WARP: master gcp[" + i + "] = " + "(" + mGCPPos.x + "," + mGCPPos.y + ")");

                index.add(i);
                yMaster.add(mGCPPos.y);
                xMaster.add(mGCPPos.x);
                ySlave.add(sGCPPos.y);
                xSlave.add(sGCPPos.x);
                yOffset.add(sGCPPos.y - mGCPPos.y);
                xOffset.add(sGCPPos.x - mGCPPos.x);
                coherence.add(1d);

                // check if master and slave coordinates are identical, if yes CPM coefficients will be set
                // directly, no need to compute them using estimators because most likely they would produce
                // incorrect result due to ill conditioned matrix.
                tempSum += Math.abs(xOffset.get(i) + yOffset.get(i));
            }

            if (tempSum < 0.01) {
                this.doEstimation = false;
            }

        }
    }

    public double getRMSStd() {
        return rmsStd;
    }

    public double getRMSMean() {
        return rmsMean;
    }

    public double getRowResidualStd() {
        return rowResidualStd;
    }

    public double getRowResidualMean() {
        return rowResidualMean;
    }

    public double getColResidualStd() {
        return colResidualStd;
    }

    public double getColResidualMean() {
        return colResidualMean;
    }

    public boolean isValid() {
        return !noRedundancy;
    }

    public WarpPolynomial getJAIWarp() {
        return jaiWarp;
    }

    public int getNumObservations() {
        return numObservations;
    }

    public double getRMS(int index) {
        return rms.get(index);
    }

    public double getXMasterCoord(int index) {
        return xMaster.get(index);
    }

    public double getYMasterCoord(int index) {
        return yMaster.get(index);
    }

    public double getXSlaveCoord(int index) {
        return xSlave.get(index);
    }

    public double getYSlaveCoord(int index) {
        return ySlave.get(index);
    }

    public List<Placemark> getSlaveGCPList() {
        return slaveGCPList;
    }

    public void setSlaveMeta(SLCImage slaveMeta) {
        this.slaveMeta = slaveMeta;
    }

    public void setSlaveOrbit(Orbit slaveOrbit) {
        this.slaveOrbit = slaveOrbit;
    }

    public void setMasterMeta(SLCImage masterMeta) {
        this.masterMeta = masterMeta;
    }

    public void setMasterOrbit(Orbit masterOrbit) {
        this.masterOrbit = masterOrbit;
    }

    public void setHeightMaster(final double[] heightArray) {
        heightMaster = heightArray;
    }

    public void setDemNoDataValue(float demNoDataValue) {
        this.demNoDataValue = demNoDataValue;
    }

    public void setUpDEMRefinement(SLCImage masterMeta, Orbit masterOrbit, SLCImage slaveMeta, Orbit slaveOrbit, double[] heightArray) {

        // master metadata
        setMasterMeta(masterMeta);
        setSlaveMeta(slaveMeta);

        // slave metadata
        setMasterOrbit(masterOrbit);
        setSlaveOrbit(slaveOrbit);

        // reference height for master acquistion
        setHeightMaster(heightArray);
    }

    public void setUpDemOffset() throws Exception {

        for (int i = 0; i < numObservations; i++) {

            Double height = heightMaster[i];

            Point delta;
            if (height.equals(demNoDataValue)) {

                // skip this offset if corresponding height is no-data-value
                delta = new Point(0, 0);

            } else {

                double line = yMaster.get(i);
                double pixel = xMaster.get(i);

                // reference
                Point refXYZ_0 = masterOrbit.lph2xyz(line, pixel, 0, masterMeta);
//                double[] refGeo_0 = Ellipsoid.xyz2ell(refXYZ_0);

                Point refXYZ_H = masterOrbit.lph2xyz(line, pixel, height, masterMeta);
//                double[] refGeo_H = Ellipsoid.xyz2ell(refXYZ_H);

//                Point master0 = masterOrbit.ell2lp(refGeo_0, masterMeta);
//                Point masterH = masterOrbit.ell2lp(refGeo_H, masterMeta);
//
//                Point slave0 = slaveOrbit.ell2lp(refGeo_0, slaveMeta);
//                Point slaveH = slaveOrbit.ell2lp(refGeo_H, slaveMeta);

                Point master0 = masterOrbit.xyz2lp(refXYZ_0, masterMeta);
                Point masterH = masterOrbit.xyz2lp(refXYZ_H, masterMeta);

                Point slave0 = slaveOrbit.xyz2lp(refXYZ_0, slaveMeta);
                Point slaveH = slaveOrbit.xyz2lp(refXYZ_H, slaveMeta);

                delta = (slaveH.min(slave0)).min(masterH.min(master0));

//                System.out.println("i = " + i + " -- " + refXYZ_0 + " -- " + refXYZ_H + " -- " + line + ", " + pixel + ", " + height);
            }
            
            double deltaY = delta.y;
            double deltaX = delta.x;

            yOffset.set(i, yOffset.get(i) - deltaY);
            xOffset.set(i, xOffset.get(i) - deltaX);

            ySlave.set(i, ySlave.get(i) - deltaY);
            xSlave.set(i, xSlave.get(i) - deltaX);
        }
    }

    public void computeCPM() {

        if (!doEstimation) {
            switch (cpmDegree) {
                case 1: {
                    xCoef = new double[3];
                    yCoef = new double[3];
                    xCoef[0] = 0; xCoef[1] = 1; xCoef[2] = 0;
                    yCoef[0] = 0; yCoef[1] = 0; yCoef[2] = 1;
                    break;
                }
                case 2: {
                    xCoef = new double[6];
                    yCoef = new double[6];
                    xCoef[0] = 0; xCoef[1] = 1; xCoef[2] = 0; xCoef[3] = 0; xCoef[4] = 0; xCoef[5] = 0;
                    yCoef[0] = 0; yCoef[1] = 0; yCoef[2] = 1; yCoef[3] = 0; yCoef[4] = 0; yCoef[5] = 0;
                    break;
                }
                case 3: {
                    xCoef = new double[10];
                    yCoef = new double[10];
                    xCoef[0] = 0; xCoef[1] = 1; xCoef[2] = 0; xCoef[3] = 0; xCoef[4] = 0;
                    xCoef[5] = 0; xCoef[6] = 0; xCoef[7] = 0; xCoef[8] = 0; xCoef[9] = 0;
                    yCoef[0] = 0; yCoef[1] = 0; yCoef[2] = 1; yCoef[3] = 0; yCoef[4] = 0;
                    yCoef[5] = 0; yCoef[6] = 0; yCoef[7] = 0; yCoef[8] = 0; yCoef[9] = 0;
                    break;
                }
                default:
                    throw new OperatorException("Incorrect WARP degree");
            }
            return;
        }

        estimateCPM();

    }

    private void estimateCPM() {

        //logger.info("Start EJML Estimation");

        int numIterations = 0;
        boolean estimationDone = false;

        DMatrixRMaj eL_hat = null;
        DMatrixRMaj eP_hat = null;
        DMatrixRMaj rhsL = null;
        DMatrixRMaj rhsP = null;

        // normalize master coordinates for stability -- only master!
        final List<Double> yMasterNorm = new ArrayList<>();
        final List<Double>  xMasterNorm = new ArrayList<>();
        for (int i = 0; i < yMaster.size(); i++) {
            yMasterNorm.add(PolyUtils.normalize2(yMaster.get(i), normWin.linelo, normWin.linehi));
            xMasterNorm.add(PolyUtils.normalize2(xMaster.get(i), normWin.pixlo, normWin.pixhi));
        }

        // helper variables
        int winL;
        int winP;
        int maxWSum_idx = 0;

        while (!estimationDone) {

            //logger.info("Start iteration: {}"+ numIterations);

            /** Remove identified outlier from previous estimation */
            if (numIterations != 0) {
                //logger.info("Removing observation {}, idxList {},  from observation vector."+ index.getQuick(maxWSum_idx)+ maxWSum_idx);

                index.remove(maxWSum_idx);
                yMasterNorm.remove(maxWSum_idx);
                xMasterNorm.remove(maxWSum_idx);
                yOffset.remove(maxWSum_idx);
                xOffset.remove(maxWSum_idx);

                // only for outlier removal
                yMaster.remove(maxWSum_idx);
                xMaster.remove(maxWSum_idx);
                ySlave.remove(maxWSum_idx);
                xSlave.remove(maxWSum_idx);
                coherence.remove(maxWSum_idx);

                // also take care of slave pins
                slaveGCPList.remove(maxWSum_idx);
            }

            /** Check redundancy */
            numObservations = index.size(); // Number of points > threshold
            if (numObservations < numUnknowns) {
                logger.severe("coregpm: Number of windows > threshold is smaller than parameters solved for.");
                throw new ArithmeticException("coregpm: Number of windows > threshold is smaller than parameters solved for.");
            }

            // work with normalized values
            DMatrixRMaj A = new DMatrixRMaj(SystemOfEquations.constructDesignMatrix_loop(yMasterNorm, xMasterNorm, cpmDegree));

            //logger.info("TIME FOR SETUP of SYSTEM : {}"+ stopWatch.lap("setup"));

            final double[] coherenceArray = new double[coherence.size()];
            for (int i = 0; i < coherenceArray.length; i++) {
                coherenceArray[i] = coherence.get(i);
            }

            DMatrix1Row  Qy_1; // vector
            double meanValue;
            switch (cpmWeight) {
                case "linear":
                    logger.info("Using sqrt(coherence) as weights");
                    Qy_1 = DMatrixRMaj.wrap(numObservations, 1, coherenceArray);
                    // Normalize weights to avoid influence on estimated var.factor
                    logger.info("Normalizing covariance matrix for LS estimation");
                    meanValue = CommonOps_DDRM.elementSum(Qy_1) / numObservations;
                    CommonOps_DDRM.divide(meanValue, Qy_1); // normalize vector
                    break;
                case "quadratic":
                    logger.info("Using coherence as weights.");
                    Qy_1 = DMatrixRMaj.wrap(numObservations, 1, coherenceArray);
                    CommonOps_DDRM.elementMult(Qy_1, Qy_1);
                    // Normalize weights to avoid influence on estimated var.factor
                    meanValue = CommonOps_DDRM.elementSum(Qy_1) / numObservations;
                    logger.info("Normalizing covariance matrix for LS estimation.");
                    CommonOps_DDRM.divide(meanValue, Qy_1); // normalize vector
                    break;
                case "bamler":
                    // TODO: see Bamler papers IGARSS 2000 and 2004
                    logger.warning("Bamler weighting method NOT IMPLEMENTED, falling back to None.");
                    Qy_1 = onesEJML(numObservations);
                    break;
                case "none":
                    //logger.info("No weighting.");
                    Qy_1 = onesEJML(numObservations);
                    break;
                default:
                    Qy_1 = onesEJML(numObservations);
                    break;
            }

            //logger.info("TIME FOR SETUP of VC diag matrix: {}"+ stopWatch.lap("diag VC matrix"));

            final double[] yOffsetArray = new double[yOffset.size()];
            for (int i = 0; i < coherenceArray.length; i++) {
                yOffsetArray[i] = yOffset.get(i);
            }
            final double[] xOffsetArray = new double[xOffset.size()];
            for (int i = 0; i < coherenceArray.length; i++) {
                xOffsetArray[i] = xOffset.get(i);
            }

            // tempMatrix_1 matrices
            final DMatrixRMaj yL_matrix = DMatrixRMaj.wrap(numObservations, 1, yOffsetArray);
            final DMatrixRMaj yP_matrix = DMatrixRMaj.wrap(numObservations, 1, xOffsetArray);

            // normal matrix
            final DMatrixRMaj N = new DMatrixRMaj(numUnknowns, numUnknowns); // = A_transpose.mmul(Qy_1_diag.mmul(A));

    /*
                // fork/join parallel implementation
                RowD1Matrix64F result = A.copy();
                DiagXMat dd = new DiagXMat(Qy_1, A, 0, A.numRows, result);
                ForkJoinPool pool = new ForkJoinPool();
                pool.invoke(dd);
                CommonOps.multAddTransA(A, dd.result, N);
    */

            CommonOps_DDRM.multAddTransA(A, diagxmat(Qy_1, A), N);
            DMatrixRMaj Qx_hat = N.copy();

            //logger.info("TIME FOR SETUP of NORMAL MATRIX: {}"+ stopWatch.lap("Normal matrix"));

            /** right hand sides */
            // azimuth
            rhsL = new DMatrixRMaj(numUnknowns, 1);// A_transpose.mmul(Qy_1_diag.mmul(yL_matrix));
            CommonOps_DDRM.multAddTransA(1d, A, diagxmat(Qy_1, yL_matrix), rhsL);
            // range
            rhsP = new DMatrixRMaj(numUnknowns, 1); // A_transpose.mmul(Qy_1_diag.mmul(yP_matrix));
            CommonOps_DDRM.multAddTransA(1d, A, diagxmat(Qy_1, yP_matrix), rhsP);
            //logger.info("TIME FOR SETUP of RightHand Side: {}"+ stopWatch.lap("Right-hand-side"));

            LinearSolverDense<DMatrixRMaj> solver = LinearSolverFactory_DDRM.leastSquares(100, 100);
            /** compute solution */
            if (!solver.setA(Qx_hat)) {
                throw new IllegalArgumentException("Singular Matrix");
            }
            solver.solve(rhsL, rhsL);
            solver.solve(rhsP, rhsP);
            //logger.info("TIME FOR SOLVING of System: {}"+ stopWatch.lap("Solving System"));

            /** inverting of Qx_hat for stability check */
            solver.invert(Qx_hat);

            //logger.info("TIME FOR INVERSION OF N: {}"+ stopWatch.lap("Inversion of N"));

            /** test inversion and check stability: max(abs([N*inv(N) - E)) ?= 0 */
            DMatrixRMaj tempMatrix_1 = new DMatrixRMaj(N.numRows, N.numCols);
            CommonOps_DDRM.mult(N, Qx_hat, tempMatrix_1);
            CommonOps_DDRM.subtractEquals(tempMatrix_1, CommonOps_DDRM.identity(tempMatrix_1.numRows, tempMatrix_1.numCols));
            double maxDeviation = CommonOps_DDRM.elementMaxAbs(tempMatrix_1);
            if (maxDeviation > .01) {
                logger.severe("COREGPM: maximum deviation N*inv(N) from unity = {}. This is larger than 0.01"+ maxDeviation);
                throw new IllegalStateException("COREGPM: maximum deviation N*inv(N) from unity)");
            } else if (maxDeviation > .001) {
                logger.warning("COREGPM: maximum deviation N*inv(N) from unity = {}. This is between 0.01 and 0.001"+ maxDeviation);
            }
            //logger.info("TIME FOR STABILITY CHECK: {}"+ stopWatch.lap("Stability Check"));

            //logger.info("Coeffs in Azimuth direction: {}"+ rhsL.toString());
            //logger.info("Coeffs in Range direction: {}"+ rhsP.toString());
            //logger.info("Max Deviation: {}"+ maxDeviation);
            //logger.info("System Quality: {}"+ solver.quality());

            /** some other stuff if the scale is okay */
            DMatrixRMaj Qe_hat = new DMatrixRMaj(numObservations, numObservations);
            DMatrixRMaj tempMatrix_2 = new DMatrixRMaj(numObservations, numUnknowns);

            CommonOps_DDRM.mult(A, Qx_hat, tempMatrix_2);
            CommonOps_DDRM.multTransB(-1, tempMatrix_2, A, Qe_hat);
            scaleInputDiag(Qe_hat, Qy_1);

            // solution: Azimuth
            DMatrixRMaj yL_hat = new DMatrixRMaj(numObservations, 1);
            eL_hat = new DMatrixRMaj(numObservations, 1);
            CommonOps_DDRM.mult(A, rhsL, yL_hat);
            CommonOps_DDRM.subtract(yL_matrix, yL_hat, eL_hat);

            // solution: Range
            DMatrixRMaj yP_hat = new DMatrixRMaj(numObservations, 1);
            eP_hat = new DMatrixRMaj(numObservations, 1);
            CommonOps_DDRM.mult(A, rhsP, yP_hat);
            CommonOps_DDRM.subtract(yP_matrix, yP_hat, eP_hat);

            //logger.info("TIME FOR DATA preparation for TESTING: {}"+ stopWatch.lap("Testing Setup"));

            /** overal model test (variance factor) */
            double overAllModelTest_L = 0;
            double overAllModelTest_P = 0;

            for (int i = 0; i < numObservations; i++) {
                overAllModelTest_L += FastMath.pow(eL_hat.get(i), 2) * Qy_1.get(i);
                overAllModelTest_P += FastMath.pow(eP_hat.get(i), 2) * Qy_1.get(i);
            }

            overAllModelTest_L = (overAllModelTest_L / FastMath.pow(SIGMA_L, 2)) / (numObservations - numUnknowns);
            overAllModelTest_P = (overAllModelTest_P / FastMath.pow(SIGMA_P, 2)) / (numObservations - numUnknowns);

            //logger.info("Overall Model Test Lines: {}"+ overAllModelTest_L);
            //logger.info("Overall Model Test Pixels: {}"+ overAllModelTest_P);

            //logger.info("TIME FOR OMT: {}"+ stopWatch.lap("OMT"));

            /** ---------------------- DATASNOPING ----------------------------------- **/
            /** Assumed Qy diag */

            /** initialize */
            DMatrixRMaj wTest_L = new DMatrixRMaj(numObservations, 1);
            DMatrixRMaj wTest_P = new DMatrixRMaj(numObservations, 1);

            for (int i = 0; i < numObservations; i++) {
                wTest_L.set(i, eL_hat.get(i) / (Math.sqrt(Qe_hat.get(i, i)) * SIGMA_L));
                wTest_P.set(i, eP_hat.get(i) / (Math.sqrt(Qe_hat.get(i, i)) * SIGMA_P));
            }

            /** find maxima's */
            // azimuth
            winL = absArgmax(wTest_L);
            double maxWinL = Math.abs(wTest_L.get(winL));
            //logger.info("maximum wtest statistic azimuth = {} for window number: {} "+ maxWinL+ index.getQuick(winL));

            // range
            winP = absArgmax(wTest_P);
            double maxWinP = Math.abs(wTest_P.get(winP));
            //logger.info("maximum wtest statistic range = {} for window number: {} "+ maxWinP+ index.getQuick(winP));

            /** use summed wTest in Azimuth and Range direction for outlier detection */
            DMatrixRMaj wTestSum = new DMatrixRMaj(numObservations);
            for (int i = 0; i < numObservations; i++) {
                wTestSum.set(i, FastMath.pow(wTest_L.get(i), 2) + FastMath.pow(wTest_P.get(i), 2));
            }

            maxWSum_idx = absArgmax(wTest_P);
            double maxWSum = wTest_P.get(winP);
            //logger.info("Detected outlier: summed sqr.wtest = {}; observation: {}"+ maxWSum+ index.getQuick(maxWSum_idx));

            /** Test if we are estimationDone yet */
            // check on number of observations
            if (numObservations <= numUnknowns) {
                logger.warning("NO redundancy!  Exiting iterations.");
                estimationDone = true;// cannot remove more than this
            }

            // check on test k_alpha
            if (Math.max(maxWinL, maxWinP) <= criticalValue) {
                // all tests accepted?
                //logger.info("All outlier tests accepted! (final solution computed)");
                estimationDone = true;
            }

            if (numIterations >= maxIterations) {
                //logger.info("max. number of iterations reached (exiting loop).");
                estimationDone = true; // we reached max. (or no max_iter specified)

            }

            /** Only warn if last iteration has been estimationDone */
            if (estimationDone) {
                if (overAllModelTest_L > 10) {
                    logger.warning("COREGPM: Overall Model Test, Lines = {} is larger than 10. (Suggest model or a priori sigma not correct.)"+ overAllModelTest_L);
                }
                if (overAllModelTest_P > 10) {
                    logger.warning("COREGPM: Overall Model Test, Pixels = {} is larger than 10. (Suggest model or a priori sigma not correct.)"+ overAllModelTest_P);
                }

                /** if a priori sigma is correct, max wtest should be something like 1.96 */
                if (Math.max(maxWinL, maxWinP) > 200.0) {
                    logger.warning("Recommendation: remove window number: {} and re-run step COREGPM.  max. wtest is: {}."+ index.get(winL)+ Math.max(maxWinL, maxWinP));
                }

            }

            //logger.info("TIME FOR wTestStatistics: {}"+ stopWatch.lap("WTEST"));
            //logger.info("Total Estimation TIME: {}"+ clock.getElapsedTime());

            numIterations++;// update counter here!

        } // only warn when iterating

        yError = eL_hat.getData();
        xError = eP_hat.getData();

        yCoef = rhsL.getData();
        xCoef = rhsP.getData();

    }

    public void wrapJaiWarpPolynomial() {

        final float[] xyMaster = new float[2 * numObservations];
        final float[] xySlave = new float[2 * numObservations];

        // work only with survived points!
        for (int i = 0; i < numObservations; i++) {
            final int j = 2 * i;
            xyMaster[j] = xMaster.get(i).floatValue();
            xyMaster[j + 1] = yMaster.get(i).floatValue();

            xySlave[j] = xSlave.get(i).floatValue();
            xySlave[j + 1] = ySlave.get(i).floatValue();
        }

        jaiWarp = WarpPolynomial.createWarp(xySlave, 0, xyMaster, 0, 2 * numObservations, 1f, 1f, 1f, 1f, cpmDegree);
    }

    public void computeEstimationStats() {

        // compute some statistics
        double rms2Mean = 0.0;
        double yError2Mean = 0.0;
        double xError2Mean = 0.0;

        if(yError != null) {
            for (int i = 0; i < numObservations; i++) {

                double dY = yError[i];
                double dX = xError[i];
                rms.add(Math.sqrt(dY * dY + dX * dX));
                double rmsVal = rms.get(i);

                rmsMean += rmsVal;
                rowResidualMean += yError[i];
                colResidualMean += xError[i];

                rms2Mean += rmsVal * rmsVal;
                yError2Mean += yError[i] * yError[i];
                xError2Mean += xError[i] * xError[i];
            }
        }

        // means
        rmsMean /= numObservations;
        rms2Mean /= numObservations;

        rowResidualMean /= numObservations;
        yError2Mean /= numObservations;

        colResidualMean /= numObservations;
        xError2Mean /= numObservations;

        // std
        rmsStd = Math.sqrt(rms2Mean - rmsMean * rmsMean);
        rowResidualStd = Math.sqrt(yError2Mean - rowResidualMean * rowResidualMean);
        colResidualStd = Math.sqrt(xError2Mean - colResidualMean * colResidualMean);
    }

}
