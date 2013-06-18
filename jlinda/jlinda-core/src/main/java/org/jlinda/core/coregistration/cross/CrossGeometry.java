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

    private static final Logger logger = (Logger) LoggerFactory.getLogger(CrossGeometry.class);

    // used only for scaling
    private Window dataWindow;

    // original sampling geometry
    private double prfOriginal;
    private double rsrOriginal;

    // target sampling geometry
    private double prfTarget;
    private double rsrTarget;

    // ratios between geometries used for scaling
    private boolean ratiosComputed = false;
    private double ratioPRF;
    private double ratioRSR;

    // estimation parameters, can be overridden
    private int numberOfWindows = 5000;
    private int polyDegree = 2;

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

    private void setLoggerLevel() {
        // logger level
        logger.setLevel(Level.WARN);
    }

    private void computeFrequencyRatios() {
        ratioPRF = prfOriginal / prfTarget;
        ratioRSR = rsrOriginal / rsrTarget;

        ratiosComputed = true;
    }

    public void computeCoefficients() {

        // TODO: make estimation 'smarter', check on ratio between freqs, if ratio within some range, only then estimate

        if (!ratiosComputed) {
            computeFrequencyRatios();
        }

        // -----------------------------------------------
        // distribute points
        // -----------------------------------------------
        int[][] result = MathUtils.distributePoints(numberOfWindows, dataWindow);

        // -----------------------------------------------
        // create synthetic offsets
        // -----------------------------------------------
        double[][] resultMaster = new double[numberOfWindows][2];

        for (int i = 0; i < numberOfWindows; i++) {
            resultMaster[i][0] = result[i][0] * ratioPRF;
            resultMaster[i][1] = result[i][1] * ratioRSR;
        }

        double[][] offset = new double[numberOfWindows][2];
        for (int i = 0; i < numberOfWindows; i++) {
            offset[i][0] = result[i][0] - resultMaster[i][0];
            offset[i][1] = result[i][1] - resultMaster[i][1];
        }

        // -----------------------------------------------
        // estimation of (dummy) coregistration polynomial
        // -----------------------------------------------

        // declare matrices
        DoubleMatrix linesNorm = new DoubleMatrix(numberOfWindows, 1);
        DoubleMatrix pixelsNorm = new DoubleMatrix(numberOfWindows, 1);
        DoubleMatrix offset_lines = new DoubleMatrix(numberOfWindows, 1);
        DoubleMatrix offset_pixels = new DoubleMatrix(numberOfWindows, 1);

        // normalize, and store into jblas matrices
        for (int i = 0; i < numberOfWindows; i++) {
            linesNorm.put(i, PolyUtils.normalize2(resultMaster[i][0], dataWindow.linelo, dataWindow.linehi));
            pixelsNorm.put(i, PolyUtils.normalize2(resultMaster[i][1], dataWindow.pixlo, dataWindow.pixhi));
            offset_lines.put(i, offset[i][0]);
            offset_pixels.put(i, offset[i][1]);
        }

        // compute coefficients using polyFit2D
        // ...NOTE: order in which input axis are given => (x,y,z) <=
        coeffsAz = PolyUtils.polyFit2D(pixelsNorm, linesNorm, offset_lines, polyDegree);
        coeffsRg = PolyUtils.polyFit2D(pixelsNorm, linesNorm, offset_pixels, polyDegree);

        // show polynomials depending on logger level <- not in production
        logger.debug("coeffsAZ : estimated with PolyUtils.polyFit2D : {}", ArrayUtils.toString(coeffsAz));
        logger.debug("coeffsRg : estimated with PolyUtils.polyFit2D : {}", ArrayUtils.toString(coeffsRg));

    }

}
