package org.jlinda.core.coregistration.cross;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.apache.commons.lang3.ArrayUtils;
import org.jblas.DoubleMatrix;
import org.jlinda.core.Window;
import org.jlinda.core.utils.MathUtils;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import javax.media.jai.WarpGeneralPolynomial;
import javax.media.jai.WarpPolynomial;
import java.awt.geom.Point2D;

import static org.jlinda.core.utils.PolyUtils.*;

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
    private static long lineLo = 1;
    private static long pixelLo = 1;
    private static long lineHi = 27000;
    private static long pixelHi = 5100;

    // ORIGINAL GEOMETRY : ENVISAT paramaters
    private static double prfASAR = 1652.4156494140625; // [Hz]
    private static double rsrASAR = 19.20768; // [MHz]

    // TARGET GEOMETRY : ERS2 paramaters
    private static double prfERS = 1679.902; // 1679.95828476786;  // [Hz]
    private static double rsrERS = 18.962468; //18.96245929824155; // [MHz]

    // Estimation Parameters
    private static final int NUM_OF_WINDOWS = 5000;
    private static final int POLY_DEGREE = 1;

    @BeforeClass
    public static void setUp() throws Exception {

        // define logger level
        logger.setLevel(Level.TRACE);

    }

    @Test
    public void testComputeCoeffsFromOffsets() {

        CrossGeometry crossGeometry = new CrossGeometry();

        crossGeometry.setPrfOriginal(prfERS);
        crossGeometry.setRsrOriginal(rsrERS);

        crossGeometry.setPrfTarget(prfASAR);
        crossGeometry.setRsrTarget(rsrASAR);

        crossGeometry.setDataWindow(new Window(lineLo, lineHi, pixelLo, pixelHi));

        // optional!
        crossGeometry.setNumberOfWindows(NUM_OF_WINDOWS);
        crossGeometry.setPolyDegree(POLY_DEGREE);
        crossGeometry.setNormalizeFlag(true);


        crossGeometry.computeCoeffsFromOffsets();

        double[] coeffsAz = crossGeometry.getCoeffsAz();
        double[] coeffsRg = crossGeometry.getCoeffsRg();

        // show polynomials
        logger.debug("coeffsAZ (from offsets): estimated with PolyUtils.polyFit2D : {}", ArrayUtils.toString(coeffsAz));
        logger.debug("coeffsRg (from offsets): estimated with PolyUtils.polyFit2D : {}", ArrayUtils.toString(coeffsRg));
        logger.debug("-----");

    }

    @Test
    public void testComputeCoeffsFromCoords() {

        CrossGeometry crossGeometry = new CrossGeometry();

        crossGeometry.setPrfOriginal(prfERS);
        crossGeometry.setRsrOriginal(rsrERS);

        crossGeometry.setPrfTarget(prfASAR);
        crossGeometry.setRsrTarget(rsrASAR);

        crossGeometry.setDataWindow(new Window(lineLo, lineHi, pixelLo, pixelHi));

        // optional!
        // crossGeometry.setNumberOfWindows(NUM_OF_WINDOWS);
        crossGeometry.setPolyDegree(POLY_DEGREE);
        crossGeometry.setNormalizeFlag(false);

        crossGeometry.computeCoeffsFromCoords_JAI();

        double[] coeffsAz = crossGeometry.getCoeffsAz();
        double[] coeffsRg = crossGeometry.getCoeffsRg();

        // show polynomials
        logger.debug("coeffsAZ (from coeffs): estimated with PolyUtils.polyFit2D : {}", ArrayUtils.toString(coeffsAz));
        logger.debug("coeffsRg (from coeffs): estimated with PolyUtils.polyFit2D : {}", ArrayUtils.toString(coeffsRg));
        logger.debug("-----");

    }


    /* Prototype and experimental implementation for:
    *  - shoot out between different polynomial estimation and evaluation methods
    *      1st method - jLinda.polyFit2D
    *      2nd method - jLinda.polyFit2D with normalized variables
    *      3rd method - JAI.WarpPolynomial
    *      4th method - polynomial estimation using jLinda, evaluation using JAI
    *  - it is based on matlab and javaschool closed-source code
    *  - this implementation exposes problems and inconsistencies with polyUtils
    *  - note that JAI seems to introduces bias in evaluation of polynomial
    * */
    // ToDo - to be removed after operator committed
    @Test
    public void computeAndEvaluateCrossPoly() {

        logger.trace("================================");
        logger.trace(" Cross InSAR Geometry prototype ");
        logger.trace("================================");

        long yMin = lineLo; // = 0;
        long xMin = pixelLo; // = 0;
        long yMax = lineHi;
        long xMax = pixelHi;

        // Target Geometry Parameters (ENVISAT paramaters)
        double targetFactorY = prfASAR; // PRF of ASAR [Hz]
        double targetFactorX = rsrASAR; // RSR of ASAR [Hz]

        // Source Geometry Parameters (ERS2 paramaters)
        double sourceFactorY = prfERS; // PRF of ERS [Hz]
        double sourceFactorX = rsrERS; // RSR of ERS [Hz]

        // Estimation Parameters
        int numberOfObservations = NUM_OF_WINDOWS;
        int polyDegree = POLY_DEGREE;

        Window win = new Window(yMin, yMax, xMin, xMax);

        // ratios of scaling factors
        double ratioX = 1 / (sourceFactorX / targetFactorX);
        double ratioY = 1 / (sourceFactorY / targetFactorY);

        // distribute points for estimation
        int[][] srcArray = MathUtils.distributePoints(numberOfObservations, win);

        // create synthetic offset between source and target geometries
        double[][] tgtArray = new double[numberOfObservations][2];

        for (int i = 0; i < numberOfObservations; i++) {
            tgtArray[i][0] = srcArray[i][0] * ratioY;
            tgtArray[i][1] = srcArray[i][1] * ratioX;
        }

        // semantics is : source - target
        double[][] dltArray = new double[numberOfObservations][2];
        for (int i = 0; i < numberOfObservations; i++) {
            dltArray[i][0] = tgtArray[i][0] - srcArray[i][0];
            dltArray[i][1] = tgtArray[i][1] - srcArray[i][1];
        }

            /*
             * estimation of (dummy) resampling polynomial
             */

        // first declare jBlas matrices
        DoubleMatrix srcY = new DoubleMatrix(numberOfObservations, 1);
        DoubleMatrix srcX = new DoubleMatrix(numberOfObservations, 1);

        DoubleMatrix srcYNorm = new DoubleMatrix(numberOfObservations, 1);
        DoubleMatrix srcXNorm = new DoubleMatrix(numberOfObservations, 1);

        DoubleMatrix txtY = new DoubleMatrix(numberOfObservations, 1);
        DoubleMatrix tgtX = new DoubleMatrix(numberOfObservations, 1);

        DoubleMatrix dltY = new DoubleMatrix(numberOfObservations, 1);
        DoubleMatrix dltX = new DoubleMatrix(numberOfObservations, 1);

        // repackage observation and design matrices into jblas, also normalize
        for (int i = 0; i < numberOfObservations; i++) {
            srcY.put(i, srcArray[i][0]);
            srcX.put(i, srcArray[i][1]);

            txtY.put(i, tgtArray[i][0]);
            tgtX.put(i, tgtArray[i][1]);

            srcYNorm.put(i, normalize2(srcArray[i][0], win.linelo, win.linehi));
            srcXNorm.put(i, normalize2(srcArray[i][1], win.pixlo, win.pixhi));

            dltY.put(i, dltArray[i][0]);
            dltX.put(i, dltArray[i][1]);
        }

        // compute coefficients using polyFit2D
        // ...NOTE: order in which input axis are given => (x,y,z) <=
        double[] coeffsX = polyFit2D(srcX, srcY, dltX, polyDegree);
        double[] coeffsY = polyFit2D(srcX, srcY, dltY, polyDegree);

        double[] coeffsXNorm = polyFit2D(srcXNorm, srcYNorm, dltX, polyDegree);
        double[] coeffsYNorm = polyFit2D(srcXNorm, srcYNorm, dltY, polyDegree);

        // for JAI semantics!
        double[] coeffsXTemp = polyFit2D(srcY, srcX, tgtX, polyDegree);
        double[] coeffsYTemp = polyFit2D(srcY, srcX, txtY, polyDegree);

        // show polynomials depending on logger level <- not in production
        logger.debug("coeffsXNorm : estimated with PolyUtils.polyFit2D : {}", ArrayUtils.toString(coeffsXNorm));
        logger.debug("coeffsYNorm : estimated with PolyUtils.polyFit2D : {}", ArrayUtils.toString(coeffsYNorm));
        logger.debug("-----");

        // show polynomials depending on logger level <- not in production
        logger.debug("coeffsX : estimated with PolyUtils.polyFit2D : {}", ArrayUtils.toString(coeffsX));
        logger.debug("coeffsY : estimated with PolyUtils.polyFit2D : {}", ArrayUtils.toString(coeffsY));
        logger.debug("-----");

        /* JAI Example */

        float[] src1DArray = new float[2 * numberOfObservations];
        float[] tgt1DArray = new float[2 * numberOfObservations];

        // declare src1DArray and dest
        for (int i = 0; i < numberOfObservations; ++i) {
            int j = 2 * i;
            src1DArray[j] = (float) (srcX.get(i));
            src1DArray[j + 1] = (float) (srcY.get(i));
            tgt1DArray[j] = (float) tgtX.get(i);
            tgt1DArray[j + 1] = (float) txtY.get(i);
        }


        float preScaleX = 1.0F / xMax;
        float postScaleX = (float) xMax;
        float preScaleY = 1.0F / yMax;
        float postScaleY = (float) yMax;
            /* Note: Warp semantic (transforms coordinates from destination to srcArray) is the
             *       opposite of jLinda semantic (transforms coordinates from srcArray to
             *       destination). We have to interchange srcArray and destination arrays for the
             *       direct transform.
             */
        WarpPolynomial warpTotal = WarpPolynomial.createWarp(tgt1DArray, 0, src1DArray, 0, 2 * numberOfObservations, preScaleX, preScaleY, postScaleX, postScaleY, polyDegree);

        // show polynomials depending on logger level <- not in production
        logger.debug("coeffsX : estimated with JAI.WarpPolynomial.createWarp : {}", ArrayUtils.toString(warpTotal.getXCoeffs()));
        logger.debug("coeffsY : estimated with JAI.WarpPolynomial.createWarp : {}", ArrayUtils.toString(warpTotal.getYCoeffs()));
        logger.debug("-----");

        // show polynomials depending on logger level <- not in production
        logger.debug("coeffsX : estimated with PolyFIt for JAI : {}", ArrayUtils.toString(coeffsXTemp));
        logger.debug("coeffsY : estimated with PolyFit for JAI : {}", ArrayUtils.toString(coeffsYTemp));
        logger.debug("-----");

        float[] xCoeffs = new float[coeffsXTemp.length];
        float[] yCoeffs = new float[coeffsYTemp.length];
        for (int i = 0; i < coeffsY.length; i++) {
            yCoeffs[i] = (float) coeffsYTemp[i];
            xCoeffs[i] = (float) coeffsXTemp[i];
        }

        WarpPolynomial warpInterp = new WarpGeneralPolynomial(xCoeffs, yCoeffs);

        Point2D srcPoint = new Point2D.Double(1354, 23214);
//        Point2D srcPoint = new Point2D.Double(2523, 2683);
        Point2D tgtPointExp = new Point2D.Double(srcPoint.getX() * ratioX, srcPoint.getY() * ratioY);

        Point2D srcPointNorm = new Point2D.Double(
                normalize2(srcPoint.getX(), win.pixlo, win.pixhi),
                normalize2(srcPoint.getY(), win.linelo, win.linehi));

        Point2D tgtPoint_JAI_1 = warpTotal.mapDestPoint(srcPoint);
        Point2D tgtPoint_JAI_2 = warpInterp.mapDestPoint(srcPoint);

        double tgtX_polyfit = srcPoint.getX() + polyval(srcPoint.getY(), srcPoint.getX(), coeffsX);
        double tgtY_polyfit = srcPoint.getY() + polyval(srcPoint.getY(), srcPoint.getX(), coeffsY);

        double tgtX_polyfit_norm = srcPoint.getX() + polyval(srcPointNorm.getY(), srcPointNorm.getX(), coeffsXNorm);
        double tgtY_polyfit_norm = srcPoint.getY() + polyval(srcPointNorm.getY(), srcPointNorm.getX(), coeffsYNorm);

        logger.debug("============ ");
        logger.debug("Input Test Point (x,y): {}, {}", srcPoint.getX(), srcPoint.getY());
        logger.debug("============ ");
        logger.debug("Target Expected: {}, {}", tgtPointExp.getX(), tgtPointExp.getY());
        logger.debug("Offset Expected: {}, {}", tgtPointExp.getX() - srcPoint.getX(), tgtPointExp.getY() - srcPoint.getY());
        logger.debug("============ ");
        logger.debug("Target - JAI-Impl-1: {}, {}", tgtPoint_JAI_1.getX(), tgtPoint_JAI_1.getY());
        logger.debug("Target - JAI-Impl-2: {}, {}", tgtPoint_JAI_2.getX(), tgtPoint_JAI_2.getY());
        logger.debug(" ---- ");
        logger.debug("Target - polyFit     : {}, {}", tgtX_polyfit, tgtY_polyfit);
        logger.debug("Target - polyFit-Norm: {}, {}", tgtX_polyfit_norm, tgtY_polyfit_norm);
        logger.debug("============ ");
        logger.debug("Error - JAI-Impl-1: {}, {}", tgtPoint_JAI_1.getX() - tgtPointExp.getX(), tgtPoint_JAI_1.getY() - tgtPointExp.getY());
        logger.debug("Error - JAI-Impl-2: {}, {}", tgtPoint_JAI_2.getX() - tgtPointExp.getX(), tgtPoint_JAI_2.getY() - tgtPointExp.getY());
        logger.debug(" ---- ");
        logger.debug("Error - polyFit     : {}, {}", tgtX_polyfit - tgtPointExp.getX(), tgtY_polyfit - tgtPointExp.getY());
        logger.debug("Error - polyFit-Norm: {}, {}", tgtX_polyfit_norm - tgtPointExp.getX(), tgtY_polyfit_norm - tgtPointExp.getY());

    }

}
