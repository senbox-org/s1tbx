package org.jdoris.core;

import org.apache.log4j.Logger;
import org.jblas.Decompose;
import org.jblas.DoubleMatrix;
import org.jblas.Solve;
import org.jdoris.core.utils.LinearAlgebraUtils;

import static org.jblas.MatrixFunctions.abs;
import static org.jdoris.core.utils.LinearAlgebraUtils.matTxmat;
import static org.jdoris.core.utils.MathUtils.rad2deg;
import static org.jdoris.core.utils.MathUtils.sqr;
import static org.jdoris.core.utils.PolyUtils.normalize2;

/**
 * User: pmar@ppolabs.com
 * Date: 3/18/11
 * Time: 9:28 PM
 */

// BASELINE is a new type (class) that is isInitialized using orbits
// and then either models the baseline parameters such as Bperp
// or can give them exact.
// Usage in the programs is something like in main do:
// BASELINE baseline;
// baseline.init(orbit1,orbit2,product?master?);
// and pass baseline to subprograms, there use baseline.get_bperp(x,y) etc.
// For stability of normalmatrix, internally data are normalized
// using line/1024  pixel/1024  height/1024
// probably it is better to only do this in model_param
// but I also did it in eval_param because no time to check for correctness.

public class Baseline {

    static Logger logger = Logger.getLogger(Baseline.class.getName());

    private boolean isInitialized;
    private double masterWavelength;   // tmp for now used for h_amb
    private double nearRange;           // range = nearRange + drange_dp*pixel
    private double drange_dp;           // range = nearRange + drange_dp*pixel
    private double orbitConvergence;   // tmp for now constant
    private double orbitHeading;       // tmp for now NOT USED
    private double linMin;               // for normalization
    private double linMax;               // for normalization
    private double pixMin;               // for normalization
    private double pixMax;               // for normalization
    private double hMin;               // height at which parameters are modeled
    private double hMax;               // to model phase=f(line,pix,hei) and hei=g(line,pix,phase)

    private int numCoeffs;              // ==10th degree of 3D-poly to model B(l,p,h)
    // --- B(l,p,h) = a000 +
    //                a100*l   + a010*p   + a001*h   +
    //                a110*l*p + a101*l*h + a011*p*h +
    //                a200*l^2 + a020*p^2 + a002*h^2

    // TODO: refactor to SmallDoubleMatrix class
    // --- Coefficients ---
    private DoubleMatrix bperpCoeffs;          // perpendicular baseline
    private DoubleMatrix bparCoeffs;           // parallel baseline
    private DoubleMatrix thetaCoeffs;          // viewing angle
    private DoubleMatrix thetaIncCoeffs;      // incidence angle to satellite
    //double avg_height_ambiguity;  //center height ambiguity

    // constants for baseline modeling
    final private static int N_POINTS_AZI = 10; // approx every 10km in azimuth
    final private static int N_POINTS_RNG = 10; // approx every 10km ground range
    final private static int N_HEIGHTS = 4;     // one more level than required for poly

    public Baseline() {

        logger.trace("Baseline class");

        isInitialized = false;
        numCoeffs = 10;
        linMin = 0.0;        // for normalization
        linMax = 25000.0;    // for normalization
        pixMin = 0.0;        // for normalization
        pixMax = 5000.0;     // for normalization
        hMin = 0.0;        // height at which baseline is computed.
        hMax = 5000.0;     // height at which baseline is computed.
        masterWavelength = 0.0;
        orbitConvergence = 0.0; // tmp for now constant
        orbitHeading = 0.0;     // tmp for now NOT USED

    }


    /**
     * --- B(l,p,h) = a000 +
     * a100*l   + a010*p   + a001*h   +
     * a110*l*p + a101*l*h + a011*p*h +
     * a200*l^2 + a020*p^2 + a002*h^2
     */
    private double polyVal(final DoubleMatrix C,
                           final double line,
                           final double pixel,
                           final double height) throws Exception {

        logger.trace("polyVal for baseline modeling");

        if (C.length != 10) {
            throw new Exception();
        } else {
            return
                    C.get(0, 0) +
                            C.get(1, 0) * line + C.get(2, 0) * pixel + C.get(3, 0) * height +
                            C.get(4, 0) * line * pixel + C.get(5, 0) * line * height + C.get(6, 0) * pixel * height +
                            C.get(7, 0) * sqr(line) + C.get(8, 0) * sqr(pixel) + C.get(9, 0) * sqr(height);
        }
    }

    /**
     * Return baselineparameters
     */
    private static void compute_B_Bpar_Bperp_Theta(BaselineComponents BBparBperptheta,
                                                   final Point point, final Point master, final Point slave) {

        logger.trace("BBparBperpTheta method");
        BBparBperptheta.b = master.distance(slave); // baseline. abs. value (in plane master,point,slave)
        final double range1 = master.distance(point);
        final double range2 = slave.distance(point);
        BBparBperptheta.bpar = range1 - range2;  // parallel baseline, sign ok
        final Point r1 = master.min(point);// points from P to M
        final Point r2 = slave.min(point);
        BBparBperptheta.theta = master.angle(r1);// viewing angle
        BBparBperptheta.bperp = sqr(BBparBperptheta.b) - sqr(BBparBperptheta.bpar);

        // check for the sign of Bperp
        if (BBparBperptheta.bperp < 0.0) {
            BBparBperptheta.bperp = 0.0;
        } else if (BBparBperptheta.theta > master.angle(r2)) { // perpendicular baseline, sign ok
            BBparBperptheta.bperp = Math.sqrt(BBparBperptheta.bperp);
        } else {
            BBparBperptheta.bperp = -Math.sqrt(BBparBperptheta.bperp);
        }
    }

    /**
     * returns incidence angle in radians based on coordinate of
     * point P on ellips and point M in orbit
     */
    private double computeIncAngle(final Point master, final Point point) {

        logger.trace("IncidenceAngle method");
        final Point r1 = master.min(point);// points from P to M
        return point.angle(r1);

    }

    public void model(final SLCImage master, final SLCImage slave, Orbit masterOrbit, Orbit slaveOrbit) throws Exception {

        if (!masterOrbit.isInterpolated()) {
            logger.debug("Baseline cannot be computed, master orbit not initialized.");
            throw new Exception("Baseline.model_parameters: master orbit not initialized");
        } else if (!slaveOrbit.isInterpolated()) {
            logger.debug("Baseline cannot be computed, slave orbit not initialized.");
            throw new Exception("Baseline.model_parameters: slave orbit not initialized");
        }

        if (isInitialized) {
            logger.warn("baseline already isInitialized??? (returning)");
            return;
        }

        masterWavelength = master.getRadarWavelength();

        // Model r = nearRange + drange_dp*p -- p starts at 1
        nearRange = master.pix2range(1.0);
        drange_dp = master.pix2range(2.0) - master.pix2range(1.0);
        nearRange -= drange_dp; // -- p starts at 1

        // Set min/maxima for normalization
        linMin = master.currentWindow.linelo; // also used during polyval...
        linMax = master.currentWindow.linehi;
        pixMin = master.currentWindow.pixlo;
        pixMax = master.currentWindow.pixhi;
        hMin = 0.0;
        hMax = 5000.0;

        // Loop counters and sampling
        int cnt = 0; // matrix index
        final double deltaPixels = master.currentWindow.pixels() / N_POINTS_RNG;
        final double deltaLines = master.currentWindow.lines() / N_POINTS_AZI;
        final double deltaHeight = (hMax - hMin) / N_HEIGHTS;

        // Declare matrices for modeling Bperp
        // Note: for stability of normalmatrix, fill aMatrix with normalized line, etc.

        // perpendicular baseline
        DoubleMatrix bPerpMatrix = new DoubleMatrix(N_POINTS_AZI * N_POINTS_RNG * N_HEIGHTS, 1);

        // parallel baseline
        DoubleMatrix bParMatrix = new DoubleMatrix(N_POINTS_AZI * N_POINTS_RNG * N_HEIGHTS, 1);

        // viewing angle
        DoubleMatrix thetaMatrix = new DoubleMatrix(N_POINTS_AZI * N_POINTS_RNG * N_HEIGHTS, 1);

        // incidence angle
        DoubleMatrix thetaIncMatrix = new DoubleMatrix(N_POINTS_AZI * N_POINTS_RNG * N_HEIGHTS, 1);

        // design matrix
        DoubleMatrix aMatrix = new DoubleMatrix(N_POINTS_AZI * N_POINTS_RNG * N_HEIGHTS, numCoeffs);


        // Loop over heights(k), lines(i), pixels(j) to estimate baseline
        // height levels
        for (long k = 0; k < N_HEIGHTS; ++k) {

            final double height = hMin + k * deltaHeight;

            // azimuth direction
            for (long i = 0; i < N_POINTS_AZI; ++i) {

                final double line = master.currentWindow.linelo + i * deltaLines;
                Point pointOnEllips;           // point, returned by lp2xyz
                double sTazi, sTrange;

                // Azimuth time for this line
                final double mTazi = master.line2ta(line);

                // xyz for master satellite from time
                final Point pointOnMasterOrb = masterOrbit.getXYZ(mTazi);

                // Continue looping in range direction
                for (long j = 0; j < N_POINTS_RNG; ++j) {

                    final double pixel = master.currentWindow.pixlo + j * deltaPixels;

                    // ______ Range time for this pixel ______
                    //final double m_trange = master.pix2tr(pixel);
                    pointOnEllips = masterOrbit.lph2xyz(line, pixel, height, master);

                    // Compute xyz for slave satellite from pointOnEllips
                    Point pointTime = slaveOrbit.xyz2t(pointOnEllips, slave);
                    sTazi = pointTime.y;
                    sTrange = pointTime.x;

                    // Slave position
                    final Point pointOnSlaveOrb = slaveOrbit.getXYZ(sTazi);

                    // Compute angle between near parallel orbits
                    final Point velOnMasterOrb = masterOrbit.getXYZDot(mTazi);
                    final Point velOnSlaveOrb = slaveOrbit.getXYZDot(sTazi);
                    final double angleOrbits = velOnMasterOrb.angle(velOnSlaveOrb);

                    logger.debug("Angle between orbits master-slave (at l,p= " + line + "," + pixel + ") = " +
                            rad2deg(angleOrbits) + " [deg]");

                    // Note: convergence assumed constant!
                    orbitConvergence = angleOrbits;
                    //final heading = angle(velOnMasterOrb,[1 0 0]) //?
                    //orbitHeading = 0.0; // not yet used

                    // The baseline parameters, derived from the positions (x,y,z)
                    //    alpha is angle counterclockwize(b, plane with normal=rho1=rho2)
                    //    theta is angle counterclockwize(rho1 = pointOnMasterOrb, r1 = pointOnMasterOrb - pointOnEllips, r2 = pointOnSlaveOrb - pointOnEllips)

                    // construct helper class
                    BaselineComponents baselineComponents = new BaselineComponents().invoke();
                    compute_B_Bpar_Bperp_Theta(baselineComponents, pointOnEllips, pointOnMasterOrb, pointOnSlaveOrb);

                    final double b = baselineComponents.getB();
                    final double bPar = baselineComponents.getBpar();
                    final double bPerp = baselineComponents.getBperp();
                    final double theta = baselineComponents.getTheta();
                    final double thetaInc = computeIncAngle(pointOnMasterOrb, pointOnEllips); // [rad]!!!

                    // Modelling of bPerp(l,p) = a00 + a10*l + a01*p
                    bPerpMatrix.put(cnt, 0, bPerp);
                    bParMatrix.put(cnt, 0, bPar);
                    thetaMatrix.put(cnt, 0, theta);
                    thetaIncMatrix.put(cnt, 0, thetaInc);

                    // --- b(l,p,h) = a000 +
                    //                a100*l   + a010*p   + a001*h   +
                    //                a110*l*p + a101*l*h + a011*p*h +
                    //                a200*l^2 + a020*p^2 + a002*h^2
                    aMatrix.put(cnt, 0, 1.0);
                    aMatrix.put(cnt, 1, normalize2(line, linMin, linMax));

                    aMatrix.put(cnt, 2, normalize2(pixel, pixMin, pixMax));
                    aMatrix.put(cnt, 3, normalize2(height, hMin, hMax));
                    aMatrix.put(cnt, 4, normalize2(line, linMin, linMax) * normalize2(pixel, pixMin, pixMax));
                    aMatrix.put(cnt, 5, normalize2(line, linMin, linMax) * normalize2(height, hMin, hMax));
                    aMatrix.put(cnt, 6, normalize2(pixel, pixMin, pixMax) * normalize2(height, hMin, hMax));
                    aMatrix.put(cnt, 7, sqr(normalize2(line, linMin, linMax)));
                    aMatrix.put(cnt, 8, sqr(normalize2(pixel, pixMin, pixMax)));
                    aMatrix.put(cnt, 9, sqr(normalize2(height, hMin, hMax)));
                    cnt++;

                    // b/alpha representation of baseline
                    final double alpha = (bPar == 0 && bPerp == 0) ? Double.NaN : theta - Math.atan2(bPar, bPerp);            // sign ok atan2

                    // horizontal/vertical representation of baseline
                    final double bH = b * Math.cos(alpha); // sign ok
                    final double bV = b * Math.sin(alpha); // sign ok

                    // TODO: check sign of infinity!!!
                    // Height ambiguity: [h] = -lambda/4pi * (r1sin(theta)/bPerp) * phi==2pi
                    final double hAmbiguity = (bPerp == 0) ? Double.POSITIVE_INFINITY : -master.getRadarWavelength() * (pointOnMasterOrb.min(pointOnEllips)).norm() * Math.sin(theta) / (2.0 * bPerp);

                    // Some extra info if in DEBUG unwrapMode
                    logger.debug("The baseline parameters for (l,p,h) = " + line + ", " + pixel + ", " + height);
                    logger.debug("\talpha (deg), BASELINE: \t" + rad2deg(alpha) + " \t" + b);
                    logger.debug("\tbPar, bPerp:      \t" + bPar + " \t" + bPerp);
                    logger.debug("\tbH, bV:           \t" + bH + " \t" + bV);
                    logger.debug("\tHeight ambiguity: \t" + hAmbiguity);
                    logger.debug("\ttheta (deg):      \t" + rad2deg(theta));
                    logger.debug("\tthetaInc (deg):  \t" + rad2deg(thetaInc));
                    logger.debug("\tpointOnMasterOrb (x,y,z) = " + pointOnMasterOrb.toString());
                    logger.debug("\tpointOnSlaveOrb (x,y,z) = " + pointOnSlaveOrb.toString());
                    logger.debug("\tpointOnEllips (x,y,z) = " + pointOnEllips.toString());
                } // loop pixels
            } // loop lines
        } // loop heights

        // Model all Baselines as 2d polynomial of degree 1
        DoubleMatrix nMatrix = matTxmat(aMatrix, aMatrix);
        DoubleMatrix rhsBperp = matTxmat(aMatrix, bPerpMatrix);
        DoubleMatrix rhsBpar = matTxmat(aMatrix, bParMatrix);
        DoubleMatrix rhsTheta = matTxmat(aMatrix, thetaMatrix);
        DoubleMatrix rhsThetaInc = matTxmat(aMatrix, thetaIncMatrix);
//        DoubleMatrix Qx_hat   = nMatrix;

        final DoubleMatrix Qx_hat = LinearAlgebraUtils.invertChol(Decompose.cholesky(nMatrix).transpose());

        // TODO: refactor to _internal_ cholesky decomposition
        // choles(Qx_hat);               // Cholesky factorisation normalmatrix
        // solvechol(Qx_hat,rhsBperp);   // Solution Bperp coefficients in rhsB
        // solvechol(Qx_hat,rhsBpar);    // Solution Theta coefficients in rhsTheta
        // solvechol(Qx_hat,rhsTheta);       // Solution Theta coefficients in rhsTheta
        // solvechol(Qx_hat,rhsThetaInc);   // Solution Theta_inc coefficients in rhsThetaInc
        // invertchol(Qx_hat);           // Covariance matrix of normalized unknowns

        rhsBperp = Solve.solvePositive(nMatrix, rhsBperp);
        rhsBpar = Solve.solvePositive(nMatrix, rhsBpar);
        rhsTheta = Solve.solvePositive(nMatrix, rhsTheta);
        rhsThetaInc = Solve.solvePositive(nMatrix, rhsThetaInc);

        // Info on inversion, normalization is ok______
        final DoubleMatrix yHatBperp = aMatrix.mmul(rhsBperp);
        final DoubleMatrix eHatBperp = bPerpMatrix.sub(yHatBperp);
        //DoubleMatrix Qe_hat  = Qy - Qy_hat;
        //DoubleMatrix y_hatT  = aMatrix * rhsTheta;
        //DoubleMatrix e_hatT  = thetaMatrix - y_hatT;

        // Copy estimated coefficients to private fields
        bperpCoeffs = rhsBperp;
        bparCoeffs = rhsBpar;
        thetaCoeffs = rhsTheta;
        thetaIncCoeffs = rhsThetaInc;

        // Test inverse -- repair matrix!!!
        for (int i = 0; i < Qx_hat.rows; i++) {
            for (int j = 0; j < i; j++) {
                Qx_hat.put(j, i, Qx_hat.get(i, j));
            }
        }

        final double maxDev = abs(nMatrix.mmul(Qx_hat).sub(DoubleMatrix.eye(Qx_hat.rows))).max();

        logger.debug("BASELINE: max(abs(nMatrix*inv(nMatrix)-I)) = " + maxDev);

        if (maxDev > .01) {
            logger.warn("BASELINE: max. deviation nMatrix*inv(nMatrix) from unity = " + maxDev + ". This is larger than .01: do not use this!");
        } else if (maxDev > .001) {
            logger.warn("BASELINE: max. deviation nMatrix*inv(nMatrix) from unity = " + maxDev + ". This is between 0.01 and 0.001 (maybe not use it)");
        }


        // Output solution and give max error
        // --- B(l,p,h) = a000 +
        //                a100*l   + a010*p   + a001*h   +
        //                a110*l*p + a101*l*h + a011*p*h +
        //                a200*l^2 + a020*p^2 + a002*h^2
        logger.debug("--------------------");
        logger.debug("Result of modeling: Bperp(l,p) = a000 + a100*l + a010*p + a001*h + ");
        logger.debug(" a110*l*p + a101*l*h + a011*p*h + a200*l^2 + a020*p^2 + a002*h^2");
        logger.debug("l,p,h in normalized coordinates [-2:2].");
        logger.debug("Bperp_a000 = " + rhsBperp.get(0, 0));
        logger.debug("Bperp_a100 = " + rhsBperp.get(1, 0));
        logger.debug("Bperp_a010 = " + rhsBperp.get(2, 0));
        logger.debug("Bperp_a001 = " + rhsBperp.get(3, 0));
        logger.debug("Bperp_a110 = " + rhsBperp.get(4, 0));
        logger.debug("Bperp_a101 = " + rhsBperp.get(5, 0));
        logger.debug("Bperp_a011 = " + rhsBperp.get(6, 0));
        logger.debug("Bperp_a200 = " + rhsBperp.get(7, 0));
        logger.debug("Bperp_a020 = " + rhsBperp.get(8, 0));
        logger.debug("Bperp_a002 = " + rhsBperp.get(9, 0));
        double maxerr = (abs(eHatBperp)).max();

        if (maxerr > 2.00)//
        {
            logger.warn("Max. error bperp modeling at 3D datapoints: " + maxerr + "m");

        } else {
            logger.info("Max. error bperp modeling at 3D datapoints: " + maxerr + "m");
        }
        logger.debug("--------------------");
        logger.debug("Range: r(p) = r0 + dr*p");
        logger.debug("l and p in un-normalized, absolute, coordinates (1:nMatrix).");
        final double range1 = master.pix2range(1.0);
        final double range5000 = master.pix2range(5000.0);
        final double drange = (range5000 - range1) / 5000.0;
        logger.debug("range = " + (range1 - drange) + " + " + drange + "*p");

        // orbit initialized
        isInitialized = true;
    }


    // ----- Getters ---------

    public double getRange(final double pixel) {
        return nearRange + drange_dp * pixel;
    }


    /**
     * Polyval modeled quantities
     * --- B(l,p,h) = a000 +
     * a100*l   + a010*p   + a001*h   +
     * a110*l*p + a101*l*h + a011*p*h +
     * a200*l^2 + a020*p^2 + a002*h^2
     * <p/>
     * l,p,h coefficients take normalized input
     */
    public double getBperp(final double line, final double pixel, final double height) throws Exception {
        return polyVal(bperpCoeffs,
                normalize2(line, linMin, linMax),
                normalize2(pixel, pixMin, pixMax),
                normalize2(height, hMin, hMax));
    }

    // Return BPERP
    public double getBperp(final double line, final double pixel) throws Exception {
        return getBperp(line, pixel, 0);
    }

    public double getBperp(final Point p) throws Exception {
        return getBperp(p.y, p.x, p.z);
    }

    // Return BPAR
    public double getBpar(final double line, final double pixel, final double height) throws Exception {
        return polyVal(bparCoeffs,
                normalize2(line, linMin, linMax),
                normalize2(pixel, pixMin, pixMax),
                normalize2(height, hMin, hMax));
    }

    // Return BPAR
    public double getBpar(final double line, final double pixel) throws Exception {
        return getBpar(line, pixel, 0);
    }

    public double getBpar(final Point p) throws Exception {
        return getBpar(p.y, p.x, p.z);
    }


    // Return THETA
    public double getTheta(final double line, final double pixel, final double height) throws Exception {
        return polyVal(thetaCoeffs,
                normalize2(line, linMin, linMax),
                normalize2(pixel, pixMin, pixMax),
                normalize2(height, hMin, hMax));
    }

    public double getTheta(final Point p) throws Exception {
        return getTheta(p.y, p.x, p.z);
    }

    // Return THETA_INC
    public double getThetaInc(final double line, final double pixel, final double height) throws Exception {
        return polyVal(thetaIncCoeffs,
                normalize2(line, linMin, linMax),
                normalize2(pixel, pixMin, pixMax),
                normalize2(height, hMin, hMax));
    }

    public double getThetaInc(final Point p) throws Exception {
        return getThetaInc(p.y, p.x, p.z);
    }


    // Derived quantities: do not normalize these!!!
    // ----------------------------------------------------------------
    // Return B
    public double getB(final double line, final double pixel, final double height) throws Exception {
        return Math.sqrt(sqr(getBpar(line, pixel, height)) + sqr(getBperp(line, pixel, height)));
    }

    public double getB(final Point p) throws Exception {
        return getB(p.y, p.x, p.z);
    }

    // Return alpha baseline orientation
    public double getAlpha(final double line, final double pixel, final double height) throws Exception {
        final double Bperp = getBperp(line, pixel, height);
        final double Bpar = getBpar(line, pixel, height);
        final double theta = getTheta(line, pixel, height);
        final double alpha = (Bpar == 0 && Bperp == 0) ? Double.NaN : theta - Math.atan2(Bpar, Bperp); // sign ok atan2
        return alpha;// sign ok
    }

    // Return alpha baseline orientation
    public double getAlpha(final Point p) throws Exception {
        final double Bperp = getBperp(p);
        final double Bpar = getBpar(p);
        final double theta = getTheta(p);
        final double alpha = (Bpar == 0 && Bperp == 0) ? Double.NaN : theta - Math.atan2(Bpar, Bperp); // sign ok atan2
        return alpha;// sign ok
    }

    // Return Bh
    public double getBhor(final double line, final double pixel, final double height) throws Exception {
        final double B = getB(line, pixel, height);
        final double alpha = getAlpha(line, pixel, height);
        return B * Math.cos(alpha);// sign ok
    }

    // Return Bv
    public double getBvert(final double line, final double pixel, final double height) throws Exception {
        final double B = getB(line, pixel, height);
        final double alpha = getAlpha(line, pixel, height);
        return B * Math.sin(alpha);// sign ok
    }

    // Return Height ambiguity
    public double getHamb(final double line, final double pixel, final double height) throws Exception {
        //final double theta     =  get_theta(line,pixel,height);
        final double theta_inc = getThetaInc(line, pixel, height);
        final double Bperp = getBperp(line, pixel, height);
        final double range_MP = getRange(pixel);// >

        final double h_amb = (Bperp == 0) ? Double.POSITIVE_INFINITY : // inf
                -masterWavelength * range_MP * Math.sin(theta_inc) / (2.0 * Bperp);// this is wrt local
        //-masterWavelength*range_MP*sin(theta)/(2.0*Bperp);// this is wrt
        return h_amb;
    }

    // Return orbit convergence to user
    // public double get_orb_conv(final double line, final double pixel, final double height=0.0) const
    //      {
    //      // do not use l,p..
    //      return orbitConvergence;
    //      };// END get_orb_conv()

    // Dump overview of all
    void dump(final double line, final double pixel, final double height) throws Exception {

        if (!isInitialized) {
            logger.debug("Exiting dumpbaseline, baseline not initialized.");
            return;
        }

        // Modeled quantities
        final double Bperp = getBperp(line, pixel, height);
        final double Bpar = getBpar(line, pixel, height);
        final double theta = getTheta(line, pixel, height);
        final double theta_inc = getThetaInc(line, pixel, height);

        // Derived quantities
        final double B = getB(line, pixel, height);
        final double alpha = getAlpha(line, pixel, height);
        final double Bh = getBhor(line, pixel, height);
        final double Bv = getBvert(line, pixel, height);
        final double h_amb = getHamb(line, pixel, height);

        // Height ambiguity: [h] = -lambda/4pi * (r1sin(theta)/Bperp) * phi==2pi

        // Log output to screen as INFO
        logger.info("The baseline parameters for (l,p,h) = " +
                line + ", " + pixel + ", " + height);

        logger.info("\tBpar, Bperp:      \t" + Bpar + " \t" + Bperp);
        logger.debug("\tB, alpha (deg):  \t" + B + " \t" + rad2deg(alpha));
        logger.debug("\tBh, Bv:          \t" + Bh + " \t" + Bv);
        logger.info("\tHeight ambiguity: \t" + h_amb);
        logger.info("\tLook angle (deg): \t" + rad2deg(theta));
        logger.debug("\tIncidence angle (deg): \t" + rad2deg(theta_inc));

    }

    // helper class for passing values as reference
    private final class BaselineComponents {

        private double b;
        private double bpar;
        private double bperp;
        private double theta;

        public double getB() {
            return b;
        }

        public double getBpar() {
            return bpar;
        }

        public double getBperp() {
            return bperp;
        }

        public double getTheta() {
            return theta;
        }

        public BaselineComponents invoke() {
            b = 0;
            bpar = 0;
            bperp = 0;
            theta = 0;
            return this;
        }
    }
}

