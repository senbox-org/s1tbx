/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */
package org.esa.snap.engine_utilities.eo;

import Jama.Matrix;
import org.apache.commons.math3.util.FastMath;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.engine_utilities.datamodel.Orbits;
import org.esa.snap.engine_utilities.datamodel.PosVector;

public final class GeoUtils {
    private static final double EPS5 = 1e-5;
    private static final double EPS = 1e-10;

    public enum EarthModel {WGS84, GRS80}

    private GeoUtils() {
    }

    /**
     * Convert geodetic coordinate into cartesian XYZ coordinate (WGS84 geodetic system is used).
     *
     * @param geoPos The geodetic coordinate of a given pixel.
     * @param xyz    The xyz coordinates of the given pixel.
     */
    public static void geo2xyz(final GeoPos geoPos, final double xyz[]) {
        geo2xyz(geoPos.lat, geoPos.lon, 0.0, xyz, EarthModel.WGS84);
    }

    /**
     * Convert geodetic coordinate into cartesian XYZ coordinate with specified geodetic system.
     *
     * @param latitude  The latitude of a given pixel (in degree).
     * @param longitude The longitude of the given pixel (in degree).
     * @param altitude  The altitude of the given pixel (in m)
     * @param xyz       The xyz coordinates of the given pixel.
     * @param geoSystem The geodetic system.
     */
    public static void geo2xyz(final double latitude, final double longitude, final double altitude,
                               final double xyz[], final EarthModel geoSystem) {

        double a = 0.0;
        double e2 = 0.0;

        if (geoSystem == EarthModel.WGS84) {

            a = WGS84.a;
            e2 = WGS84.e2;

        } else if (geoSystem == EarthModel.GRS80) {

            a = GRS80.a;
            e2 = GRS80.e2;

        } else {
            throw new OperatorException("Incorrect geodetic system");
        }

        final double lat = latitude * Constants.DTOR;
        final double lon = longitude * Constants.DTOR;

        final double sinLat = FastMath.sin(lat);
        final double cosLat = FastMath.cos(lat);
        final double N = a / Math.sqrt(1 - e2 * sinLat * sinLat);

        xyz[0] = (N + altitude) * cosLat * FastMath.cos(lon); // in m
        xyz[1] = (N + altitude) * cosLat * FastMath.sin(lon); // in m
        xyz[2] = ((1 - e2) * N + altitude) * sinLat;   // in m
    }

    /**
     * Convert geodetic coordinate into cartesian XYZ coordinate with specified geodetic system.
     *
     * @param latitude  The latitude of a given pixel (in degree).
     * @param longitude The longitude of the given pixel (in degree).
     * @param altitude  The altitude of the given pixel (in m)
     * @param xyz       The xyz coordinates of the given pixel.
     */
    public static void geo2xyzWGS84(final double latitude, final double longitude, final double altitude,
                                    final PosVector xyz) {

        final double lat = latitude * Constants.DTOR;
        final double lon = longitude * Constants.DTOR;

        final double sinLat = FastMath.sin(lat);
        final double N = (WGS84.a / Math.sqrt(1.0 - WGS84.e2 * sinLat * sinLat));
        final double NcosLat = (N + altitude) * FastMath.cos(lat);

        xyz.x = NcosLat * FastMath.cos(lon); // in m
        xyz.y = NcosLat * FastMath.sin(lon); // in m
        xyz.z = (N + altitude - WGS84.e2 * N) * sinLat;
        //xyz.z = (WGS84.e2inv * N  + altitude) * sinLat;    // in m
    }

    /**
     * Convert cartesian XYZ coordinate into geodetic coordinate (WGS84 geodetic system is used).
     *
     * @param xyz    The xyz coordinate of the given pixel.
     * @param geoPos The geodetic coordinate of the given pixel.
     */
    public static void xyz2geo(final double xyz[], final GeoPos geoPos) {
        xyz2geoWGS84(xyz, geoPos);
    }

    /**
     * Convert cartesian XYZ coordinate into geodetic coordinate with specified geodetic system.
     *
     * @param xyz       The xyz coordinate of the given pixel.
     * @param geoPos    The geodetic coordinate of the given pixel.
     * @param geoSystem The geodetic system.
     */
    public static void xyz2geo(final double xyz[], final GeoPos geoPos, final EarthModel geoSystem) {

        double a = 0.0;
        double b = 0.0;
        double e2 = 0.0;
        double ep2 = 0.0;

        if (geoSystem == EarthModel.WGS84) {

            a = WGS84.a;
            b = WGS84.b;
            e2 = WGS84.e2;
            ep2 = WGS84.ep2;

        } else if (geoSystem == EarthModel.GRS80) {

            a = GRS80.a;
            b = GRS80.b;
            e2 = GRS80.e2;
            ep2 = GRS80.ep2;

        } else {
            throw new OperatorException("Incorrect geodetic system");
        }

        final double x = xyz[0];
        final double y = xyz[1];
        final double z = xyz[2];
        final double s = Math.sqrt(x * x + y * y);
        final double theta = FastMath.atan(z * a / (s * b));

        geoPos.lon = (float) (FastMath.atan(y / x) * Constants.RTOD);

        if (geoPos.lon < 0.0 && y >= 0.0) {
            geoPos.lon += 180.0;
        } else if (geoPos.lon > 0.0 && y < 0.0) {
            geoPos.lon -= 180.0;
        }

        geoPos.lat = (float) (FastMath.atan((z + ep2 * b * FastMath.pow(FastMath.sin(theta), 3)) /
                (s - e2 * a * FastMath.pow(FastMath.cos(theta), 3))) * Constants.RTOD);
    }

    /**
     * Convert cartesian XYZ coordinate into geodetic coordinate with specified geodetic system.
     *
     * @param xyz    The xyz coordinate of the given pixel.
     * @param geoPos The geodetic coordinate of the given pixel.
     */
    public static void xyz2geoWGS84(final double xyz[], final GeoPos geoPos) {

        final double x = xyz[0];
        final double y = xyz[1];
        final double z = xyz[2];
        final double s = Math.sqrt(x * x + y * y);
        final double theta = FastMath.atan(z * WGS84.a / (s * WGS84.b));

        geoPos.lon = (float) (FastMath.atan(y / x) * Constants.RTOD);

        if (geoPos.lon < 0.0 && y >= 0.0) {
            geoPos.lon += 180.0;
        } else if (geoPos.lon > 0.0 && y < 0.0) {
            geoPos.lon -= 180.0;
        }

        geoPos.lat = (float) (FastMath.atan((z + WGS84.ep2 * WGS84.b * FastMath.pow(FastMath.sin(theta), 3)) /
                (s - WGS84.e2 * WGS84.a * FastMath.pow(FastMath.cos(theta), 3))) * Constants.RTOD);
    }

    /**
     * Convert polar coordinates to Cartesian vector.
     * <p>
     * <b>Definitions:</b>
     * <ul>
     * <li>Latitude: angle from XY-plane towards +Z-axis.</li>
     * <li>Longitude: angle in XY-plane measured from +X-axis towards +Y-axis.</li>
     * </ul>
     * <p>
     * Note: Apache's FastMath used in implementation.
     *
     * @param latitude  The latitude of a given pixel (in degree).
     * @param longitude The longitude of the given pixel (in degree).
     * @param radius    The radius of the given point (in m)
     * @param xyz       The return array vector of X, Y and Z coordinates for the input point.
     * @author Petar Marikovic, PPO.labs
     */
    public static void polar2cartesian(final double latitude, final double longitude, final double radius, final double xyz[]) {

        final double latRad = latitude * Constants.DTOR;
        final double lonRad = longitude * Constants.DTOR;

        final double sinLat = FastMath.sin(latRad);
        final double cosLat = FastMath.cos(latRad);

        xyz[0] = radius * cosLat * FastMath.cos(lonRad);
        xyz[1] = radius * cosLat * FastMath.sin(lonRad);
        xyz[2] = radius * sinLat;
    }

    /**
     * Convert Cartesian to Polar coordinates.
     * <p>
     * <b>Definitions:</b>
     * <ul>
     * <li>Latitude: angle from XY-plane towards +Z-axis.</li>
     * <li>Longitude: angle in XY-plane measured from +X-axis towards +Y-axis.</li>
     * </ul>
     * <p>
     * Implementation Details: Unlike for rest of utility methods GeoPos class container is not used for storing polar
     * coordinates. GeoPos fields are declared as floats and can introduced numerical errors, especially in radius/height.
     *
     * <p>
     * Note: Apache's FastMath used in implementation.
     *
     *
     * @param xyz          Array of x, y, and z coordinates.
     * @param phiLamHeight Array of latitude (in radians), longitude (in radians), and radius (in meters).
     * @author Petar Marikovic, PPO.labs
     */
    public static void cartesian2polar(final double[] xyz, final double[] phiLamHeight) {

        phiLamHeight[2] = Math.sqrt(xyz[0] * xyz[0] + xyz[1] * xyz[1] + xyz[2] * xyz[2]);
        phiLamHeight[1] = Math.atan2(xyz[1], xyz[0]);
        phiLamHeight[0] = FastMath.asin(xyz[2] / phiLamHeight[2]);

    }

    /**
     * Compute accurate target position for given orbit information using Newton's method.
     *
     * @param data The orbit data.
     * @param xyz  The xyz coordinate for the target.
     * @param time The slant range time in seconds.
     */
    public static void computeAccurateXYZ(final Orbits.OrbitVector data, final double[] xyz, final double time) {

        final double a = Constants.semiMajorAxis;
        final double b = Constants.semiMinorAxis;
        final double a2 = a * a;
        final double b2 = b * b;
        final double del = 0.001;
        final int maxIter = 10;

        Matrix X = new Matrix(3, 1);
        final Matrix F = new Matrix(3, 1);
        final Matrix J = new Matrix(3, 3);

        X.set(0, 0, xyz[0]);
        X.set(1, 0, xyz[1]);
        X.set(2, 0, xyz[2]);

        J.set(0, 0, data.xVel);
        J.set(0, 1, data.yVel);
        J.set(0, 2, data.zVel);

        final double time2 = FastMath.pow(time * Constants.halfLightSpeed, 2.0);
        for (int i = 0; i < maxIter; i++) {

            final double x = X.get(0, 0);
            final double y = X.get(1, 0);
            final double z = X.get(2, 0);

            final double dx = x - data.xPos;
            final double dy = y - data.yPos;
            final double dz = z - data.zPos;

            F.set(0, 0, data.xVel * dx + data.yVel * dy + data.zVel * dz);
            F.set(1, 0, dx * dx + dy * dy + dz * dz - time2);
            F.set(2, 0, x * x / a2 + y * y / a2 + z * z / b2 - 1);

            J.set(1, 0, 2.0 * dx);
            J.set(1, 1, 2.0 * dy);
            J.set(1, 2, 2.0 * dz);
            J.set(2, 0, 2.0 * x / a2);
            J.set(2, 1, 2.0 * y / a2);
            J.set(2, 2, 2.0 * z / b2);

            X = X.minus(J.inverse().times(F));

            if (Math.abs(F.get(0, 0)) <= del && Math.abs(F.get(1, 0)) <= del && Math.abs(F.get(2, 0)) <= del) {
                break;
            }
        }

        xyz[0] = X.get(0, 0);
        xyz[1] = X.get(1, 0);
        xyz[2] = X.get(2, 0);
    }

    /**
     *  Given starting point, initial heading and distance in meters,
     *  calculate destination point and heading from destination to starting point
     *
     * @param pos1 lat lon position
     * @param dist distance in m
     * @param head1 azimuth in degree measured in the diretion North east south west
     * @return lat lon and heading
     */
    public static LatLonHeading vincenty_direct(final GeoPos pos1, final double dist, final double head1) {

        final LatLonHeading pos2 = new LatLonHeading();
        double lat1 = pos1.lat;
        double lon1 = pos1.lon;

        lat1 *= Constants.DTOR;
        lon1 *= Constants.DTOR;
        final double FAZ = head1 * Constants.DTOR;

        // Model WGS84:
        //    F=1/298.25722210;	// flatteing
        final double F = 0.0;  // defF

        // equatorial radius
        final double R = 1.0 - F;
        double TU = R * FastMath.tan(lat1);
        final double SF = FastMath.sin(FAZ);
        final double CF = FastMath.cos(FAZ);
        double BAZ = 0.0;
        if (CF != 0.0)
            BAZ = Math.atan2(TU, CF) * 2.0;
        final double CU = 1.0 / Math.sqrt(TU * TU + 1.0);
        final double SU = TU * CU;
        final double SA = CU * SF;
        final double C2A = -SA * SA + 1.0;
        double X = Math.sqrt((1.0 / R / R - 1.0) * C2A + 1.0) + 1.0;
        X = (X - 2.0) / X;
        double C = 1.0 - X;
        C = (X * X / 4.0 + 1) / C;
        double D = (0.375 * X * X - 1.0) * X;
        TU = dist / R / WGS84.a / C;
        double Y = TU;

        double SY, CY, CZ, E;
        do {
            SY = FastMath.sin(Y);
            CY = FastMath.cos(Y);
            CZ = FastMath.cos(BAZ + Y);
            E = CZ * CZ * 2.0 - 1.0;
            C = Y;
            X = E * CY;
            Y = E + E - 1.0;
            Y = (((SY * SY * 4.0 - 3.0) * Y * CZ * D / 6.0 + X) * D / 4.0 - CZ) * SY * D + TU;
        } while (Math.abs(Y - C) > EPS);

        BAZ = CU * CY * CF - SU * SY;
        C = R * Math.sqrt(SA * SA + BAZ * BAZ);
        D = SU * CY + CU * SY * CF;
        pos2.lat = Math.atan2(D, C);
        C = CU * CY - SU * SY * CF;
        X = Math.atan2(SY * SF, C);
        C = ((-3.0 * C2A + 4.0) * F + 4.0) * C2A * F / 16.0;
        D = ((E * CY * C + CZ) * SY * C + Y) * SA;
        pos2.lon = lon1 + X - (1.0 - C) * D * F;
        BAZ = Math.atan2(SA, BAZ) + Constants.PI;

        pos2.lon *= Constants.RTOD;
        pos2.lat *= Constants.RTOD;
        pos2.heading = BAZ * Constants.RTOD;

        while (pos2.heading < 0) {
            pos2.heading += 360;
        }

        return pos2;
    }

    /**
     * Given starting and end points
     * calculate distance in meters and initial headings from start to
     * end (return variable head1),
     * and from end to start point (return variable head2)
     *
     * @param pos1 first position
     * @param pos2 second position
     * @return distance and heading
     * dist:	distance in m
     * head1:	azimuth in degrees mesured in the direction North east south west
     * 			from (lon1,lat1) to (lon2, lat2)
     * head2:	azimuth in degrees mesured in the direction North east south west
     * 			from (lon2,lat2) to (lon1, lat1)
     */
    public static DistanceHeading vincenty_inverse(final GeoPos pos1, final GeoPos pos2) {

        final DistanceHeading output = new DistanceHeading();
        double lat1 = pos1.lat;
        double lon1 = pos1.lon;
        double lat2 = pos2.lat;
        double lon2 = pos2.lon;

        if ((Math.abs(lon1 - lon2) < EPS5) && (Math.abs(lat1 - lat2) < EPS5)) {
            output.distance = 0;
            output.heading1 = -1;
            output.heading2 = -1;
            return output;
        }

        lat1 *= Constants.DTOR;
        lat2 *= Constants.DTOR;
        lon1 *= Constants.DTOR;
        lon2 *= Constants.DTOR;

        // Model WGS84:
        //    F=1/298.25722210;	// flattening
        final double F = 0.0; //defF;

        final double R = 1 - F;
        double TU1 = R * FastMath.tan(lat1);
        double TU2 = R * FastMath.tan(lat2);
        final double CU1 = 1.0 / Math.sqrt(TU1 * TU1 + 1.0);
        final double SU1 = CU1 * TU1;
        final double CU2 = 1.0 / Math.sqrt(TU2 * TU2 + 1.0);
        double S = CU1 * CU2;
        double BAZ = S * TU2;
        double FAZ = BAZ * TU1;
        double X = lon2 - lon1;

        double SX, CX, SY, CY, Y, SA, C2A, CZ, E, C, D;
        do {
            SX = FastMath.sin(X);
            CX = FastMath.cos(X);
            TU1 = CU2 * SX;
            TU2 = BAZ - SU1 * CU2 * CX;
            SY = Math.sqrt(TU1 * TU1 + TU2 * TU2);
            CY = S * CX + FAZ;
            Y = Math.atan2(SY, CY);
            SA = S * SX / SY;
            C2A = -SA * SA + 1.;
            CZ = FAZ + FAZ;
            if (C2A > 0.)
                CZ = -CZ / C2A + CY;
            E = CZ * CZ * 2. - 1.;
            C = ((-3. * C2A + 4.) * F + 4.) * C2A * F / 16.;
            D = X;
            X = ((E * CY * C + CZ) * SY * C + Y) * SA;
            X = (1. - C) * X * F + lon2 - lon1;
        } while (Math.abs(D - X) > (0.01));

        FAZ = Math.atan2(TU1, TU2);
        BAZ = Math.atan2(CU1 * SX, BAZ * CX - SU1 * CU2) + Constants.PI;
        X = Math.sqrt((1. / R / R - 1.) * C2A + 1.) + 1.;
        X = (X - 2.) / X;
        C = 1. - X;
        C = (X * X / 4. + 1.) / C;
        D = (0.375 * X * X - 1.) * X;
        X = E * CY;
        S = 1. - E - E;
        S = ((((SY * SY * 4. - 3.) * S * CZ * D / 6. - X) * D / 4. + CZ) * SY * D + Y) * C * WGS84.a * R;

        output.distance = S;
        output.heading1 = FAZ * Constants.RTOD;
        output.heading2 = BAZ * Constants.RTOD;

        while (output.heading1 < 0)
            output.heading1 += 360;
        while (output.heading2 < 0)
            output.heading2 += 360;

        return output;
    }

    public static class LatLonHeading {
        public double lat;
        public double lon;
        public double heading;
    }

    public static class DistanceHeading {
        public double distance;
        public double heading1;
        public double heading2;
    }

    public interface WGS84 {
        double a = 6378137.0; // m
        double b = 6356752.3142451794975639665996337; //6356752.31424518; // m
        double earthFlatCoef = 1.0 / ((a - b) / a); //298.257223563;
        double e2 = 2.0 / earthFlatCoef - 1.0 / (earthFlatCoef * earthFlatCoef);
        double e2inv = 1 - WGS84.e2;
        double ep2 = e2 / (1 - e2);
    }

    public interface GRS80 {
        double a = 6378137; // m
        double b = 6356752.314140; // m
        double earthFlatCoef = 1.0 / ((a - b) / a); //298.257222101;
        double e2 = 2.0 / earthFlatCoef - 1.0 / (earthFlatCoef * earthFlatCoef);
        double ep2 = e2 / (1 - e2);
    }
}
