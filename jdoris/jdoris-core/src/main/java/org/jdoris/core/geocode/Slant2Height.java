package org.jdoris.core.geocode;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import org.jblas.DoubleMatrix;
import org.jblas.Solve;
import org.jdoris.core.Orbit;
import org.jdoris.core.Point;
import org.jdoris.core.SLCImage;
import org.jdoris.core.Window;
import org.jdoris.core.utils.MathUtils;
import org.jdoris.core.utils.PolyUtils;
import org.slf4j.LoggerFactory;

import static org.jdoris.core.Constants.PI;
import static org.jdoris.core.Constants.SOL;
import static org.jdoris.core.utils.LinearAlgebraUtils.matTxmat;
import static org.jdoris.core.utils.PolyUtils.normalize2;
import static org.jdoris.core.utils.PolyUtils.polyFit;

public class Slant2Height {

    private static final Logger logger = (Logger) LoggerFactory.getLogger(Slant2Height.class);

    private final SLCImage master;
    private final Orbit masterOrbit;

    private final SLCImage slave;
    private final Orbit slaveOrbit;

    private final int nPoints; // where ref.phase is evaluated
    private final int nHeights;
    private final int degree1D; // only possible now.
    private final int degree2D;

    private static final int MAXHEIGHT = 5000; // max hei for ref.phase
    private static final int TEN = 10;

    private DoubleMatrix tile;
    private Window tileWindow;
    private Window dataWindow;

    private DoubleMatrix rhs;

    private double minPhi;
    private double maxPhi;

    // TODO: clean-it-up and move helper methods in util classes

    /*
    * Slant2Height core class: optimized for use in NEST operators

      - methods from the prototype implementation refactored into ones that compute the math,
      and stores it into the fields that are declared final, and in another method that executes
      that math on the tiles that are pulled/pushes in the operators

      - testing done wrt doris.cpp.core code : 0.01m tolerance level, the difference is because
      of the different normalization parameters used in doris.cpp code. (I think jDoris version
      is more reliable).
    * */
    public Slant2Height(int nPoints, int nHeights, int degree1d, int degree2d,
                        SLCImage master, Orbit masterOrbit, SLCImage slave, Orbit slaveOrbit) {

        logger.setLevel(Level.DEBUG);

        this.nPoints = nPoints;
        this.nHeights = nHeights;
        this.degree1D = degree1d;
        this.degree2D = degree2d;

        this.master = master;
        this.masterOrbit = masterOrbit;
        this.slave = slave;
        this.slaveOrbit = slaveOrbit;

        if (degree1D - 1 > TEN) {
            logger.error("Internal ERROR: panic, programmers problem -> increase TEN.");
            throw new IllegalArgumentException();
        }

    }

    public synchronized void setTile(DoubleMatrix tile) {
        this.tile = tile;
    }

    public synchronized void setTileWindow(Window window) {
        this.tileWindow = window;
    }

    public void setDataWindow(Window window) {
        this.dataWindow = window;
    }

    public Window getTileWindow() {
        return tileWindow;
    }

    public DoubleMatrix getTile() {
        return tile;
    }


    /**
     * slant2h-eight (schwabisch)
     * <p/>
     * compute height in radar coded system (master):
     * <p/>
     * <p/>
     * 1.  compute reference phase for h=0,2000,4000 in nPoints
     * <p/>
     * 2.  solve system: h(phi) = a_0 + a_1*phi + a_2*phi*phi
     * (2nd degree 1D polynomial) for all nPoints
     * <p/>
     * 3.  compute a_i (l,p) = degree2D 2D polynomial
     * <p/>
     * 4.0 set offset to the one of first pixel , add this to all
     * this step is skipped, phase is w.r.t. h=0, ref. is subtracted
     * <p/>
     * 4.1 evaluate polynomial of 3. for all points (l,p) of
     * (multilooked) unwrapped interferogram
     * <p/>
     * Note: solution to system for betas seems not be very stable!??
     */
    public void schwabisch() throws Exception {

        logger.trace("slant2h Schwabisch (PM 01-Apr-2011)");

        final int heightStep = MAXHEIGHT / (nHeights - 1); // heights to eval ref.refPhase

        // Matrices for storing refPhase for all ref. ellipsoids
        //  refPhase(i,0)  refPhase for height 0
        //  refPhase(i,1)  refPhase for height Heigthsep * 1
        //  refPhase(i,Nh) refPhase for height 4000
//        Map<Integer, DoubleMatrix> refPhaseMap = Maps.newLinkedHashMap();

        DoubleMatrix refPhaseMatrix = new DoubleMatrix(nPoints, nHeights);

        // Distribute points in original master system (not multilooked)
        // (i,0): line, (i,1): pixel, (i,2) flagfromdisk (not used here)
        int[][] positionArray = MathUtils.distributePoints(nPoints, dataWindow);

        DoubleMatrix Position = new DoubleMatrix(nPoints, 2);
        for (int i = 0; i < nPoints; i++) {
            Position.put(i, 0, positionArray[i][0]);
            Position.put(i, 1, positionArray[i][1]);
        }

        /** ----------------------------------------------------------------------------*/
        /** -- STEP 1 : compute reference refPhase in N points for nHeights ------------*/
        /** ----------------------------------------------------------------------------*/

        // Compute reference refPhase in N points for height (numheight)
        logger.debug("S2H: schwabisch: STEP1: compute reference refPhase for nHeights.");
        DoubleMatrix refPhaseZero = new DoubleMatrix(nPoints);
        for (int heightIdx = 0; heightIdx < nHeights; heightIdx++) {

            int height = heightIdx * heightStep;

            DoubleMatrix refPhase = new DoubleMatrix(nPoints); // pseudo-observation

            // Compute delta r for all points
            for (int i = 0; i < nPoints; i++) {
                double phase = computeReferencePhase((double) positionArray[i][0], (double) positionArray[i][1], height, master, slave, masterOrbit, slaveOrbit);
                refPhase.put(i, phase);
            }

            // store refPhase at h = 0
            if (height == 0) {
                refPhaseZero = refPhase;
            }

            //  Subtract ref. refPhase at h=0 for all point
            //  this is the same as adding reference refPhase for all in uint
            refPhaseMatrix.putColumn(heightIdx, refPhase.sub(refPhaseZero));
        }

        /** ----------------------------------------------------------------------------*/
        /** -- STEP 2 : compute alpha coefficients of polynomials for these points -----*/
        /** ----------------------------------------------------------------------------*/

        logger.debug("S2H: schwabisch: STEP2: estimate coefficients 1d polynomial.");

//        DoubleMatrix design = new DoubleMatrix(nHeights, degree1D + 1); // design matrix
        DoubleMatrix alphas = new DoubleMatrix(nPoints, degree1D + 1); // pseudo-observation
        DoubleMatrix hei = new DoubleMatrix(nHeights, 1);
        for (int i = 0; i < nHeights; i++) {
            hei.put(i, 0, i * heightStep); // 0, .., 5000
        }

        // normalize tile to [0,1]
        minPhi = refPhaseMatrix.min();
        maxPhi = refPhaseMatrix.max();
        normalize(refPhaseMatrix, minPhi, maxPhi);

        for (int i = 0; i < nPoints; i++) {// solve system for all points
            alphas.putRow(i, new DoubleMatrix(polyFit(refPhaseMatrix.getRow(i), hei, degree1D)));
        } // loop over all points

        /** -------------------------------------------------------------------------------*/
        /** -- STEP 3 : Compute alpha_i coefficients of polynomials as function of (l,p) --*/
        /** -------------------------------------------------------------------------------*/

        logger.debug("S2H: schwabisch: STEP3: estimate coefficients for 2d polynomial.");
        // Compute alpha_i coefficients of polynomials as function of (l,p)
        // ... alpha_i = sum(k,l) beta_kl l^k p^l;
        // ... Solve simultaneous for all betas
        // ... this does not seem to be possibly with my routine, so do per alfa_i
        final int Nunk = PolyUtils.numberOfCoefficients(degree2D); // Number of unknowns

        // ______ Check redundancy is done before? ______
        if (nPoints < Nunk) {
            logger.error("slant2hschwabisch: N_observations<N_unknowns (increase S2H_NPOINTS or decrease S2H_DEGREE2D.");
            throw new IllegalArgumentException();
        }

        DoubleMatrix A = new DoubleMatrix(nPoints, Nunk); // designmatrix

        // Set up system of equations
        // .... Order unknowns: B00 B10 B01 B20 B11 B02 B30 B21 B12 B03 for degree=3
//        double minL = Position.getColumn(0).min();
//        double maxL = Position.getColumn(0).max();
//        double minP = Position.getColumn(1).min();
//        double maxP = Position.getColumn(1).max();
        double minL = dataWindow.linelo;
        double maxL = dataWindow.linehi;
        double minP = dataWindow.pixlo;
        double maxP = dataWindow.pixhi;

        for (int i = 0; i < nPoints; i++) {
            // ______ normalize coordinates ______
            double posL = normalize2(Position.get(i, 0), minL, maxL);
            double posP = normalize2(Position.get(i, 1), minP, maxP);

            int index = 0;
            for (int j = 0; j <= degree2D; j++) {
                for (int k = 0; k <= j; k++) {
                    A.put(i, index, Math.pow(posL, j - k) * Math.pow(posP, k));
                    index++;
                }
            }
        }

        // Solve 2d polynomial system for alfas at these points
        DoubleMatrix N = matTxmat(A, A);
        rhs = matTxmat(A, alphas);
        DoubleMatrix Qx_hat = N;

        // Solve the normal equations for all alpha_i
        // Simultaneous solution doesn't work somehow
        for (int i = 0; i < rhs.getColumns(); ++i) {
            DoubleMatrix rhs_alphai = rhs.getColumn(i);
            rhs.putColumn(i, Solve.solveSymmetric(Qx_hat, rhs_alphai));
        }

        // Test solution by inverse
        Qx_hat = Solve.solveSymmetric(Qx_hat, DoubleMatrix.eye(Qx_hat.getRows()));
        double maxdev = (N.mmul(Qx_hat).sub(DoubleMatrix.eye(Qx_hat.getRows()))).normmax();
        logger.debug("s2h schwaebisch: max(abs(N*inv(N)-I)) = {}", maxdev);
        if (maxdev > 0.01) {
            logger.warn("slant2h: possibly wrong solution. deviation from unity AtA*inv(AtA) = {} > 0.01", maxdev);
        }


    }


    public void applySchwabisch(final Window tileWindow, DoubleMatrix tile) {

        /** ----------------------------------------------------------------------------*/
        /** -- STEP 4 : compute height for all pixels in N points for nHeights ---------*/
        /** ----------------------------------------------------------------------------*/

        double minP = dataWindow.pixlo;
        double maxP = dataWindow.pixhi;
        double minL = dataWindow.linelo;
        double maxL = dataWindow.linehi;

        logger.debug("S2H: schwabisch: STEP4: compute height for all pixels.");
        // Evaluate for all points interferogram h=f(l,p,refPhase)
        //  .....recon with multilook, degree1D, degree2D free
        //  .....Multilook factors
        double mlFacL = 1;//unwrappedinterf.multilookL;
        double mlFacP = 1;//unwrappedinterf.multilookP;

        // Number of lines/pixels of multilooked unwrapped interferogram
        int mlLines = (int) (Math.floor((tileWindow.linehi - tileWindow.linelo + 1) / mlFacL));
        int mlPixels = (int) (Math.floor((tileWindow.pixhi - tileWindow.pixlo + 1) / mlFacP));

        // Line/pixel of first point in original master coordinates
        double firstLine = (double) (tileWindow.linelo) + (mlFacL - 1.) / 2.;
        double firstPixel = (double) (tileWindow.pixlo) + (mlFacP - 1.) / 2.;

        // ant axis of pixel coordinates ______
        DoubleMatrix p_axis = new DoubleMatrix(mlPixels, 1);
        for (int i = 0; i < tileWindow.pixels(); i++) {
            p_axis.put(i, 0, firstPixel + i * mlFacP);
        }
        normalize(p_axis, minP, maxP);

        // ant axis for azimuth coordinates ______
        DoubleMatrix l_axis = new DoubleMatrix(mlLines, 1);
        for (int k = 0; k < tileWindow.lines(); k++) {
            l_axis.put(k, 0, firstLine + k * mlFacL);
        }
        normalize(l_axis, minL, maxL);

        // ---> Lookup table because not known in advance what degree1D is
        DoubleMatrix[] pntALPHA = new DoubleMatrix[TEN];
        for (int k = 0; k <= degree1D; k++) {
            DoubleMatrix beta = new DoubleMatrix(PolyUtils.numberOfCoefficients(degree2D), 1);
            for (int l = 0; l < PolyUtils.numberOfCoefficients(degree2D); l++) {
                beta.put(l, 0, rhs.get(l, k)); // solution stored in rhs
            }
            pntALPHA[k] = PolyUtils.polyval(l_axis, p_axis, beta, degree2D);
        }

        // Evaluate h=f(l,p,phi) for all points in grid in BUFFER
        double[] coeffThisPoint = new double[degree1D + 1]; //DoubleMatrix(degree1D + 1, 1);

        for (int line = 0; line < mlLines; line++) {
            for (int pixel = 0; pixel < mlPixels; pixel++) {
                // Check if unwrapped ok, else compute h
                if (tile.get(line, pixel) != Double.NaN) // else leave NaN
                {
                    for (int k = 0; k < degree1D + 1; k++) {
                        coeffThisPoint[k] = pntALPHA[k].get(line, pixel);
                    }
                    double data = tile.get(line, pixel);
                    double x = PolyUtils.normalize2(data, minPhi, maxPhi);
                    double value = PolyUtils.polyVal1D(x, coeffThisPoint);
                    tile.put(line, pixel, value);
                }
            }
        }

    }

    // This is prototype implementation: to be removed in next cleanup....
    @Deprecated
    public void schwabischTotal() throws Exception {

        logger.trace("slant2h Schwabisch (PM 01-Apr-2011)");

        final int heightStep = MAXHEIGHT / (nHeights - 1); // heights to eval ref.refPhase

        // Matrices for storing refPhase for all ref. ellipsoids
        //  refPhase(i,0)  refPhase for height 0
        //  refPhase(i,1)  refPhase for height Heigthsep * 1
        //  refPhase(i,Nh) refPhase for height 4000
//        Map<Integer, DoubleMatrix> refPhaseMap = Maps.newLinkedHashMap();

        DoubleMatrix refPhaseMatrix = new DoubleMatrix(nPoints, nHeights);

        // Distribute points in original master system (not multilooked)
        // (i,0): line, (i,1): pixel, (i,2) flagfromdisk (not used here)
        int[][] positionArray = MathUtils.distributePoints(nPoints, dataWindow);

        DoubleMatrix Position = new DoubleMatrix(nPoints, 2);
        for (int i = 0; i < nPoints; i++) {
            Position.put(i, 0, positionArray[i][0]);
            Position.put(i, 1, positionArray[i][1]);
        }

        /** ----------------------------------------------------------------------------*/
        /** -- STEP 1 : compute reference refPhase in N points for nHeights ------------*/
        /** ----------------------------------------------------------------------------*/

        // Compute reference refPhase in N points for height (numheight)
        logger.debug("S2H: schwabisch: STEP1: compute reference refPhase for nHeights.");
        DoubleMatrix refPhaseZero = new DoubleMatrix(nPoints);
        for (int heightIdx = 0; heightIdx < nHeights; heightIdx++) {

            int height = heightIdx * heightStep;

            DoubleMatrix refPhase = new DoubleMatrix(nPoints); // pseudo-observation

            // Compute delta r for all points
            for (int i = 0; i < nPoints; i++) {
                double phase = computeReferencePhase((double) positionArray[i][0], (double) positionArray[i][1], height, master, slave, masterOrbit, slaveOrbit);
                refPhase.put(i, phase);
            }

            // store refPhase at h = 0
            if (height == 0) {
                refPhaseZero = refPhase;
            }

            //  Subtract ref. refPhase at h=0 for all point
            //  this is the same as adding reference refPhase for all in uint
            refPhaseMatrix.putColumn(heightIdx, refPhase.sub(refPhaseZero));
        }

        /** ----------------------------------------------------------------------------*/
        /** -- STEP 2 : compute alpha coefficients of polynomials for these points -----*/
        /** ----------------------------------------------------------------------------*/

        logger.debug("S2H: schwabisch: STEP2: estimate coefficients 1d polynomial.");
        DoubleMatrix alphas = new DoubleMatrix(nPoints, degree1D + 1); // pseudo-observation
        DoubleMatrix hei = new DoubleMatrix(nHeights, 1);
        for (int i = 0; i < nHeights; i++) {
            hei.put(i, 0, i * heightStep); // 0, .., 5000
        }

        // normalize tile to [0,1]
        double minPhi = refPhaseMatrix.min();
        double maxPhi = refPhaseMatrix.max();
        normalize(refPhaseMatrix, minPhi, maxPhi);

        for (int i = 0; i < nPoints; i++) {// solve system for all points
            alphas.putRow(i, new DoubleMatrix(polyFit(refPhaseMatrix.getRow(i), hei, degree1D)));
        } // loop over all points

        /** -------------------------------------------------------------------------------*/
        /** -- STEP 3 : Compute alpha_i coefficients of polynomials as function of (l,p) --*/
        /** -------------------------------------------------------------------------------*/

        logger.debug("S2H: schwabisch: STEP3: estimate coefficients for 2d polynomial.");
        // Compute alpha_i coefficients of polynomials as function of (l,p)
        // ... alpha_i = sum(k,l) beta_kl l^k p^l;
        // ... Solve simultaneous for all betas
        // ... this does not seem to be possibly with my routine, so do per alfa_i
        final int Nunk = PolyUtils.numberOfCoefficients(degree2D); // Number of unknowns

        // ______ Check redundancy is done before? ______
        if (nPoints < Nunk) {
            logger.error("slant2hschwabisch: N_observations<N_unknowns (increase S2H_NPOINTS or decrease S2H_DEGREE2D.");
            throw new IllegalArgumentException();
        }

        DoubleMatrix A = new DoubleMatrix(nPoints, Nunk); // designmatrix

        // Set up system of equations
        // .... Order unknowns: B00 B10 B01 B20 B11 B02 B30 B21 B12 B03 for degree=3
//        double minL = Position.getColumn(0).min();
//        double maxL = Position.getColumn(0).max();
//        double minP = Position.getColumn(1).min();
//        double maxP = Position.getColumn(1).max();
        double minL = dataWindow.linelo;
        double maxL = dataWindow.linehi;
        double minP = dataWindow.pixlo;
        double maxP = dataWindow.pixhi;

        for (int i = 0; i < nPoints; i++) {
            // ______ normalize coordinates ______
            double posL = normalize2(Position.get(i, 0), minL, maxL);
            double posP = normalize2(Position.get(i, 1), minP, maxP);

            int index = 0;
            for (int j = 0; j <= degree2D; j++) {
                for (int k = 0; k <= j; k++) {
                    A.put(i, index, Math.pow(posL, j - k) * Math.pow(posP, k));
                    index++;
                }
            }
        }

        // Solve 2d polynomial system for alfas at these points
        DoubleMatrix N = matTxmat(A, A);
        rhs = matTxmat(A, alphas);
        DoubleMatrix Qx_hat = N;

        // Solve the normal equations for all alpha_i
        // Simultaneous solution doesn't work somehow
        for (int i = 0; i < rhs.getColumns(); ++i) {
            DoubleMatrix rhs_alphai = rhs.getColumn(i);
            rhs.putColumn(i, Solve.solveSymmetric(Qx_hat, rhs_alphai));
        }

        // Test solution by inverse
        Qx_hat = Solve.solveSymmetric(Qx_hat, DoubleMatrix.eye(Qx_hat.getRows()));
        double maxdev = (N.mmul(Qx_hat).sub(DoubleMatrix.eye(Qx_hat.getRows()))).normmax();
        logger.debug("s2h schwaebisch: max(abs(N*inv(N)-I)) = {}", maxdev);
        if (maxdev > 0.01) {
            logger.warn("slant2h: possibly wrong solution. deviation from unity AtA*inv(AtA) = {} > 0.01", maxdev);
        }

        /** ----------------------------------------------------------------------------*/
        /** -- STEP 4 : compute height for all pixels in N points for nHeights ---------*/
        /** ----------------------------------------------------------------------------*/

        logger.debug("S2H: schwabisch: STEP4: compute height for all pixels.");
        // Evaluate for all points interferogram h=f(l,p,refPhase)
        //  .....recon with multilook, degree1D, degree2D free
        //  .....Multilook factors
        double mlFacL = 1;//unwrappedinterf.multilookL;
        double mlFacP = 1;//unwrappedinterf.multilookP;

        // Number of lines/pixels of multilooked unwrapped interferogram
        int mlLines = (int) (Math.floor((tileWindow.linehi - tileWindow.linelo + 1) / mlFacL));
        int mlPixels = (int) (Math.floor((tileWindow.pixhi - tileWindow.pixlo + 1) / mlFacP));

        // Line/pixel of first point in original master coordinates
        double firstLine = (double) (tileWindow.linelo) + (mlFacL - 1.) / 2.;
        double firstPixel = (double) (tileWindow.pixlo) + (mlFacP - 1.) / 2.;

        // ant axis of pixel coordinates ______
        DoubleMatrix p_axis = new DoubleMatrix(mlPixels, 1);
        for (int i = 0; i < tileWindow.pixels(); i++) {
            p_axis.put(i, 0, firstPixel + i * mlFacP);
        }
        normalize(p_axis, minP, maxP);

        // ant axis for azimuth coordinates ______
        DoubleMatrix l_axis = new DoubleMatrix(mlLines, 1);
        for (int k = 0; k < tileWindow.lines(); k++) {
            l_axis.put(k, 0, firstLine + k * mlFacL);
        }
        normalize(l_axis, minL, maxL);

        DoubleMatrix BUFFER = tile; //unwrappedinterf.readphase(bufferwin);

        // ---> Lookup table because not known in advance what degree1D is
        DoubleMatrix[] pntALPHA = new DoubleMatrix[TEN];
        for (int k = 0; k <= degree1D; k++) {
            DoubleMatrix beta = new DoubleMatrix(PolyUtils.numberOfCoefficients(degree2D), 1);
            for (int l = 0; l < PolyUtils.numberOfCoefficients(degree2D); l++) {
                beta.put(l, 0, rhs.get(l, k)); // solution stored in rhs
            }
            pntALPHA[k] = PolyUtils.polyval(l_axis, p_axis, beta, degree2D);
        }

        // Evaluate h=f(l,p,phi) for all points in grid in BUFFER
        double[] coeffThisPoint = new double[degree1D + 1]; //DoubleMatrix(degree1D + 1, 1);

        for (int line = 0; line < mlLines; line++) {
            for (int pixel = 0; pixel < mlPixels; pixel++) {
                // Check if unwrapped ok, else compute h
                if (BUFFER.get(line, pixel) != Double.NaN) // else leave NaN
                {
                    for (int k = 0; k < degree1D + 1; k++) {
                        coeffThisPoint[k] = pntALPHA[k].get(line, pixel);
                    }
                    double data = BUFFER.get(line, pixel);
                    double x = PolyUtils.normalize2(data, minPhi, maxPhi);
                    double value = PolyUtils.polyVal1D(x, coeffThisPoint);
                    BUFFER.put(line, pixel, value);
                }
            }
        }

    }// loop over BUFFERS and END slant2h


/*
    private double minValue(Collection<DoubleMatrix> values) {
        ArrayList<Double> temp = Lists.newArrayList();
        for (DoubleMatrix matrix : values) {
            temp.add(matrix.min());
        }
        return Collections.min(temp);
    }
*/

/*
    private double maxValue(Collection<DoubleMatrix> values) {
        ArrayList<Double> temp = Lists.newArrayList();
        for (DoubleMatrix matrix : values) {
            temp.add(matrix.min());
        }
        return Collections.max(temp);
    }
*/


    private double computeReferencePhase(Point p, SLCImage master, SLCImage slave, Orbit masterOrbit, Orbit slaveOrbit) throws Exception {
        double line = p.y;
        double pixel = p.x;
        double height = p.z;

        if (height == Double.NaN)
            height = 0;

        return computeReferencePhase(p.y, p.x, p.z, master, slave, masterOrbit, slaveOrbit);
    }

    private double computeReferencePhase(final double line, final double pixel, final double height,
                                         final SLCImage master, final SLCImage slave,
                                         final Orbit masterOrbit, final Orbit slaveOrbit) throws Exception {

        double mTimeRange = master.pix2tr(pixel);

        // Compute xyz of point P on ELLIPS for this line,pixel
        Point xyzMaster = masterOrbit.lph2xyz(line, pixel, height, master);

        // Compute xyz of slave satelite in orbit_slave from P
        Point timeSlave = slaveOrbit.xyz2t(xyzMaster, slave);

        return mTimeRange * ((-4. * PI * SOL) / master.getRadarWavelength()) - timeSlave.x * ((-4. * PI * SOL) / slave.getRadarWavelength());
    }

    private double computeReferencePhase(final double line, final double pixel,
                                         final SLCImage master, final SLCImage slave,
                                         final Orbit masterOrbit, final Orbit slaveOrbit) throws Exception {

        return computeReferencePhase(line, pixel, 0d, master, slave, masterOrbit, slaveOrbit);
    }

    private void normalize(final DoubleMatrix data, final double min, final double max) {
        data.subi(.5 * (min + max));
        data.divi(.25 * (max - min));
    }

}