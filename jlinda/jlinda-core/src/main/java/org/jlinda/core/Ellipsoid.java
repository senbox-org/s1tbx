package org.jlinda.core;

import org.apache.log4j.Logger;

public class Ellipsoid {

    Logger logger = Logger.getLogger(Ellipsoid.class.getName());

    private static double e2 = 0.00669438003551279091;  // squared first  eccentricity (derived)
    private static double e2b = 0.00673949678826153145; // squared second eccentricity (derived)

    public static double a = Constants.WGS84_A; // semi major
    public static double b = Constants.WGS84_B; // semi minor
    public static String name = "WGS84";

    public Ellipsoid() {
        a = Constants.WGS84_A;
        b = Constants.WGS84_B;
        e2 = 0.00669438003551279091;
        e2b = 0.00673949678826153145;
        //set_ecc1st_sqr();// compute e2
        //set_ecc2nd_sqr();// compute e2b
        name = "WGS84";
    }


    public Ellipsoid(final double semiMajor, final double semiMinor) {
        a = semiMajor;
        b = semiMinor;
        set_ecc1st_sqr();// compute e2 (not required for zero-doppler iter.)
        set_ecc2nd_sqr();// compute e2b (not required for zero-doppler iter.)
        //set_name("unknown");
    }

    public Ellipsoid(Ellipsoid ell) {
        a = ell.a;
        b = ell.b;
        e2 = ell.e2;
        e2b = ell.e2b;
        name = ell.name;
    }

    public void showdata() {
        logger.info("ELLIPSOID: \tEllipsoid used (orbit, output): " + name + ".");
        logger.info("ELLIPSOID: a   = " + a);
        logger.info("ELLIPSOID: b   = " + b);
        logger.info("ELLIPSOID: e2  = " + e2);
        logger.info("ELLIPSOID: e2' = " + e2b);
    }

    /**
    *  Convert xyz cartesian coordinates to
    *  Geodetic ellipsoid coordinates latlonh
    *    xyz2ell
    *
    * Converts geocentric cartesian coordinates in the XXXX
    *  reference frame to geodetic coordinates.
    *  method of bowring see globale en locale geodetische systemen
    * input:
    *  - ellipsinfo, xyz, (phi,lam,hei)
    * output:
    *  - void (returned double[] lam<-pi,pi>, phi<-pi,pi>, hei)
    *
    */
    public static double[] xyz2ell(final Point xyz) {

//        double[] phi_lambda_height = new double[3];
        final double r = Math.sqrt(Math.pow(xyz.x, 2) + Math.pow(xyz.y, 2));
        final double nu = Math.atan2((xyz.z * a), (r * b));
        final double sin3 = Math.pow(Math.sin(nu), 3);
        final double cos3 = Math.pow(Math.cos(nu), 3);
        final double phi = Math.atan2((xyz.z + e2b * b * sin3), (r - e2 * a * cos3));
        final double lambda = Math.atan2(xyz.y, xyz.x);
        final double N = computeEllipsoidNormal(phi);
        final double height = (r / Math.cos(phi)) - N;

        return new double[]{phi, lambda, height};

    }


//    public static double[] xyz2ell(final Point xyz) {
//        double r = Math.sqrt(Math.pow(xyz.x, 2) + Math.pow(xyz.y, 2));
//        double nu = Math.atan2((xyz.z * a), (r * b));
//        double sin3 = Math.pow(Math.sin(nu), 3);
//        double cos3 = Math.pow(Math.cos(nu), 3);
//        phi = Math.atan2((xyz.z + e2b * b * sin3), (r - e2 * a * cos3));
//        lambda = Math.atan2(xyz.y, xyz.x);
//    }


    /**
     * ell2xyz
     * Converts wgs84 ellipsoid cn to geocentric cartesian coord.
     * input:
     * - phi,lam,hei (geodetic co-latitude, longitude, [rad] h [m]
     * output:
     * - cn XYZ
     */
    public static Point ell2xyz(final double phi, final double lambda, final double height) throws IllegalArgumentException {

        if (phi > Math.PI || phi < -Math.PI || lambda > Math.PI || lambda < -Math.PI) {
            throw new IllegalArgumentException("Ellipsoid.ell2xyz : input values for phi/lambda have to be in radians!");
        }

        final double N = computeEllipsoidNormal(phi);
        final double Nph = N + height;
        return new Point(
                Nph * Math.cos(phi) * Math.cos(lambda),
                Nph * Math.cos(phi) * Math.sin(lambda),
                (Nph - e2 * N) * Math.sin(phi));
    }

    public static Point ell2xyz(final double[] phiLambdaHeight) throws IllegalArgumentException {

        final double phi = phiLambdaHeight[0];
        final double lambda = phiLambdaHeight[1];
        final double height = phiLambdaHeight[2];

        if (phi > Math.PI || phi < -Math.PI || lambda > Math.PI || lambda < -Math.PI) {
            throw new IllegalArgumentException("Ellipsoid.ell2xyz(): phi/lambda values has to be in radians!");
        }

        final double N = computeEllipsoidNormal(phi);
        final double Nph = N + height;
        return new Point(
                Nph * Math.cos(phi) * Math.cos(lambda),
                Nph * Math.cos(phi) * Math.sin(lambda),
                (Nph - e2 * N) * Math.sin(phi));
    }

    private static double computeEllipsoidNormal(final double phi) {
        return a / Math.sqrt(1.0 - e2 * Math.pow(Math.sin(phi), 2));
    }

    private double computeCurvatureRadiusInMeridianPlane(final double phi) {
        return a * (1 - e2) / Math.pow((1 - e2 * Math.pow(Math.sin(phi), 2)), 3 / 2);

    }

    // first ecc.
    private static void set_ecc1st_sqr() {
        //  faster than e2=(sqr(a)-sqr(b))/sqr(a)
        e2 = 1.0 - Math.pow(b / a, 2);
    }

    // second ecc.
    private static void set_ecc2nd_sqr() {
        // faster than e2b=(sqr(a)-sqr(b))/sqr(b);
        e2b = Math.pow(a / b, 2) - 1.0;
    }

}
