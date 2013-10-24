package org.jlinda.core.coregistration.cross;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.jblas.DoubleMatrix;
import org.jlinda.core.SLCImage;
import org.jlinda.core.Window;
import org.jlinda.core.utils.MathUtils;
import org.jlinda.core.utils.PolyUtils;
import org.slf4j.LoggerFactory;

/**
 * User: pmar@ppolabs.com
 * Date: 6/13/13
 * Time: 12:23 PM
 * <p/>
 * Description: Class that encapsulates all geometrical computations needed for CrossInSAR operator
 */
public class CrossGeometry {

    // logger
    private static final Logger logger = (Logger) LoggerFactory.getLogger(CrossGeometry.class);
    private static Level level = Level.WARN;

    // used only for normalization
    private Window dataWindow;

    // original sampling geometry: source
    private double prfOriginal;
    private double rsrOriginal;

    // target sampling geometry: target
    private double prfTarget;
    private double rsrTarget;

    // ratios between geometries used for scaling
    private boolean ratiosComputed = false;
    private double ratioPRF;
    private double ratioRSR;

    // estimation parameters, can be overridden
    private int polyDegree = 2;
    private int numberOfWindows = 5000;
    private boolean normalizeFlag = true;

    // geometry grids - need them because interfacing them with JAI
    private double[][] sourceGrid;
    private double[][] targetGrid;
    private double[][] offsetGrid;

    // coefficients of polynomials that describe transition from one sampling into another one
    private double[] coeffsAz;
    private double[] coeffsRg;

    public CrossGeometry() {
        setLoggerLevel();
    }

    public CrossGeometry(double prfOriginal, double prfTarget, double rsrOriginal, double rsrSlave, Window window) {

        setLoggerLevel();

        this.prfOriginal = prfOriginal;
        this.rsrOriginal = rsrOriginal;

        this.prfTarget = prfTarget;
        this.rsrTarget = rsrSlave;

        this.dataWindow = window;

        computeFrequencyRatios();
    }

    public CrossGeometry(SLCImage masterMeta, SLCImage slaveMeta) {

        setLoggerLevel();

        this.prfOriginal = masterMeta.getPRF();
        this.rsrOriginal = masterMeta.getRsr2x();

        this.prfTarget = slaveMeta.getPRF();
        this.rsrTarget = slaveMeta.getRsr2x();

        this.dataWindow = slaveMeta.getOriginalWindow();

        computeFrequencyRatios();

    }

    public int getNumberOfWindows() {
        return numberOfWindows;
    }

    public void setNumberOfWindows(int numberOfWindows) {
        this.numberOfWindows = numberOfWindows;
    }

    public void setDataWindow(Window dataWindow) {
        this.dataWindow = dataWindow;
    }

    public void setPrfOriginal(double prfOriginal) {
        this.prfOriginal = prfOriginal;
    }

    public void setPrfTarget(double prfTarget) {
        this.prfTarget = prfTarget;
    }

    public void setRsrOriginal(double rsrOriginal) {
        this.rsrOriginal = rsrOriginal;
    }

    public void setRsrTarget(double rsrTarget) {
        this.rsrTarget = rsrTarget;
    }

    public int getPolyDegree() {
        return polyDegree;
    }

    public void setPolyDegree(int polyDegree) {
        this.polyDegree = polyDegree;
    }

    public double getRatioPRF() {
        return ratioPRF;
    }

    public double getRatioRSR() {
        return ratioRSR;
    }

    public double[] getCoeffsAz() {
        return coeffsAz;
    }

    public double[] getCoeffsRg() {
        return coeffsRg;
    }

    public double[][] getSourceGrid() {
        return sourceGrid;
    }

    public double[][] getTargetGrid() {
        return targetGrid;
    }

    public double[][] getOffsetGrid() {
        return offsetGrid;
    }

    public void setNormalizeFlag(boolean normalizeFlag) {
        this.normalizeFlag = normalizeFlag;
    }

    private void setLoggerLevel() {
        // logger level
        logger.setLevel(level);
    }


    public void constructGrids() {

        // ToDo: make estimation 'smarter', check on ratio between freqs, if ratio within some range, only then estimate

        if (!ratiosComputed) {
            computeFrequencyRatios();
        }

        // -----------------------------------------------
        // distribute points
        // -----------------------------------------------
        sourceGrid = MathUtils.distributePointsDoubles(numberOfWindows, dataWindow);

        // -----------------------------------------------
        // create synthetic offsets
        // -----------------------------------------------
        targetGrid = new double[numberOfWindows][2];
        offsetGrid = new double[numberOfWindows][2];

        for (int i = 0; i < numberOfWindows; i++) {
            targetGrid[i][0] = sourceGrid[i][0] * ratioPRF;
            targetGrid[i][1] = sourceGrid[i][1] * ratioRSR;

            // semantic of jLinda is always : offset = (source - target)
            offsetGrid[i][0] = targetGrid[i][0] - sourceGrid[i][0];
            offsetGrid[i][1] = targetGrid[i][1] - sourceGrid[i][1];
        }

    }

    // compute Coefficients from Offsets - for jLinda semantics
    public void computeCoeffsFromOffsets() {

        constructGrids();

        // -----------------------------------------------
        // estimation of (dummy) coregistration polynomial
        // -----------------------------------------------

        // declare matrices
        DoubleMatrix sourceY = new DoubleMatrix(numberOfWindows, 1);
        DoubleMatrix sourceX = new DoubleMatrix(numberOfWindows, 1);
        DoubleMatrix offsetY = new DoubleMatrix(numberOfWindows, 1);
        DoubleMatrix offsetX = new DoubleMatrix(numberOfWindows, 1);

        // normalize, and store into jblas matrices
        for (int i = 0; i < numberOfWindows; i++) {
            if (normalizeFlag) {
                sourceY.put(i, PolyUtils.normalize2(sourceGrid[i][0], dataWindow.linelo, dataWindow.linehi));
                sourceX.put(i, PolyUtils.normalize2(sourceGrid[i][1], dataWindow.pixlo, dataWindow.pixhi));
            } else {
                sourceY.put(i, sourceGrid[i][0]);
                sourceX.put(i, sourceGrid[i][1]);
            }

            offsetY.put(i, offsetGrid[i][0]);
            offsetX.put(i, offsetGrid[i][1]);
        }

        // compute coefficients using polyFit2D
        // ...NOTE: order in which input axis are given => (x,y,z) <=
        coeffsAz = PolyUtils.polyFit2D(sourceX, sourceY, offsetY, polyDegree);
        coeffsRg = PolyUtils.polyFit2D(sourceX, sourceY, offsetX, polyDegree);

        // show polynomials depending on logger level <- not in production
        logger.debug("coeffsAZ (offsets): estimated with PolyUtils.polyFit2D : {}", ArrayUtils.toString(coeffsAz));
        logger.debug("coeffsRg (offsets): estimated with PolyUtils.polyFit2D : {}", ArrayUtils.toString(coeffsRg));

    }

    // compute coefficients from Coordinates - for NEST/BEAM/JAI semantics
    public void computeCoeffsFromCoords_JAI() {

        constructGrids();

        // -----------------------------------------------
        // estimation of (dummy) coregistration polynomial
        // -----------------------------------------------

        // declare matrices
        DoubleMatrix targetY = new DoubleMatrix(numberOfWindows, 1);
        DoubleMatrix targetX = new DoubleMatrix(numberOfWindows, 1);
        DoubleMatrix sourceY = new DoubleMatrix(numberOfWindows, 1);
        DoubleMatrix sourceX = new DoubleMatrix(numberOfWindows, 1);

        for (int i = 0; i < numberOfWindows; i++) {
            if (normalizeFlag) {
                sourceY.put(i, PolyUtils.normalize2(sourceGrid[i][0], dataWindow.linelo, dataWindow.linehi));
                sourceX.put(i, PolyUtils.normalize2(sourceGrid[i][1], dataWindow.pixlo, dataWindow.pixhi));
            } else {
                sourceY.put(i, sourceGrid[i][0]);
                sourceX.put(i, sourceGrid[i][1]);
            }

            targetY.put(i, targetGrid[i][0]);
            targetX.put(i, targetGrid[i][1]);
        }

        // compute coefficients using polyFit2D
        // ...NOTE: order in which input axis are given => (x,y,z) <=
        coeffsAz = PolyUtils.polyFit2D(sourceX, sourceY, targetY, polyDegree);
        coeffsRg = PolyUtils.polyFit2D(sourceX, sourceY, targetX, polyDegree);

        // show polynomials depending on logger level <- not in production
        logger.debug("coeffsAZ (coords): estimated with PolyUtils.polyFit2D : {}", ArrayUtils.toString(coeffsAz));
        logger.debug("coeffsRg (coords): estimated with PolyUtils.polyFit2D : {}", ArrayUtils.toString(coeffsRg));

    }

    private void computeFrequencyRatios() {
        ratioPRF = 1 / (prfOriginal / prfTarget);
        ratioRSR = 1 / (rsrOriginal / rsrTarget);

        ratiosComputed = true;
    }

}
