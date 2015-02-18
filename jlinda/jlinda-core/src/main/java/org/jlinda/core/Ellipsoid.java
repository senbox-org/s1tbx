package org.jlinda.core;

import org.esa.beam.util.logging.BeamLogManager;
import org.apache.commons.math3.util.FastMath;
import java.util.logging.Logger;

import static org.jlinda.core.Constants.DTOR;

public class Ellipsoid {

    Logger logger = BeamLogManager.getSystemLogger();

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

        final double r = Math.sqrt(xyz.x*xyz.x + xyz.y*xyz.y);
        final double nu = Math.atan2((xyz.z * a), (r * b));
        final double sinNu = FastMath.sin(nu);
        final double cosNu = FastMath.cos(nu);
        final double sin3 = sinNu*sinNu*sinNu;
        final double cos3 = cosNu*cosNu*cosNu;
        final double phi = Math.atan2((xyz.z + e2b * b * sin3), (r - e2 * a * cos3));
        final double lambda = Math.atan2(xyz.y, xyz.x);
        final double N = computeEllipsoidNormal(phi);
        final double height = (r / FastMath.cos(phi)) - N;

        return new double[]{phi, lambda, height};
    }

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
        final double A = Nph * FastMath.cos(phi);
        return new Point(
                A * FastMath.cos(lambda),
                A * FastMath.sin(lambda),
                (Nph - e2 * N) * FastMath.sin(phi));
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
        final double A = Nph * FastMath.cos(phi);
        return new Point(
                A * FastMath.cos(lambda),
                A * FastMath.sin(lambda),
                (Nph - e2 * N) * FastMath.sin(phi));
    }

    public static Point ell2xyz(final GeoPoint geoPoint, final double height) {
        return ell2xyz(geoPoint.lat * DTOR, geoPoint.lon * DTOR, height);
    }

    public static Point ell2xyz(final GeoPoint geoPoint) {
        return ell2xyz(geoPoint.lat * DTOR, geoPoint.lon * DTOR, 0.0);
    }

    public static void ell2xyz(final GeoPoint geoPoint, double[] xyz) {
        Point tempPoint = ell2xyz(geoPoint.lat * DTOR, geoPoint.lon * DTOR, 0.0);
        xyz[0] = tempPoint.x;
        xyz[1] = tempPoint.y;
        xyz[2] = tempPoint.z;
    }

    public static void ell2xyz(final GeoPoint geoPoint, final double height, final double[] xyz) {
        Point tempPoint = ell2xyz(geoPoint.lat * DTOR, geoPoint.lon * DTOR, height);
        xyz[0] = tempPoint.x;
        xyz[1] = tempPoint.y;
        xyz[2] = tempPoint.z;
    }

    private static double computeEllipsoidNormal(final double phi) {
        return a / Math.sqrt(1.0 - e2 * FastMath.pow(FastMath.sin(phi), 2));
    }

    private double computeCurvatureRadiusInMeridianPlane(final double phi) {
        return a * (1 - e2) / FastMath.pow((1 - e2 * FastMath.pow(FastMath.sin(phi), 2)), 3 / 2);

    }

    // first ecc.
    private static void set_ecc1st_sqr() {
        //  faster than e2=(sqr(a)-sqr(b))/sqr(a)
        e2 = 1.0 - FastMath.pow(b / a, 2);
    }

    // second ecc.
    private static void set_ecc2nd_sqr() {
        // faster than e2b=(sqr(a)-sqr(b))/sqr(b);
        e2b = FastMath.pow(a / b, 2) - 1.0;
    }

}
