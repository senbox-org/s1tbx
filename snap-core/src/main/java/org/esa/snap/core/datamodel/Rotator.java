/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.datamodel;

import java.awt.geom.Point2D;

import static java.lang.Math.*;

/**
 * Class for rotating geographical positions.
 * The rotator makes the given coordinate the origin <code>(0.0, 0.0)</code> of the coordinate system.
 * If the coordinate which shall become the north pole of the new coordinate system is known the latitude value given into the
 * constructor must be changed. Instead of providing directly the latitude value, it must be <code>90 - latitude</code>.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public final class Rotator {

    private final double a11;
    private final double a12;
    private final double a13;
    private final double a21;
    private final double a22;
    private final double a23;
    private final double a31;
    private final double a32;
    private final double a33;

    private final double lon;
    private final double lat;
    private final double alpha;

    /**
     * Constructs a new rotation. In the rotated system, the point defined
     * by the (lon, lat) parameters is located at the origin, i.e. has the
     * geographical coordinates (0, 0).
     * <p>
     * The new rotation is composed of a clockwise rotation about the z-axis
     * (which corresponds to S-N direction) and a counter-clockwise rotation
     * about the y-axis (which corresponds to W-E direction).
     *
     * @param lon the geographical longitude of the point becoming the new
     *            origin.
     * @param lat the geographical latitude of the point becoming the new
     *            origin.
     */
    public Rotator(double lon, double lat) {
        this(lon, lat, 0.0);
    }

    /**
     * Constructs a new rotation. In the rotated system, the point defined
     * by the (lon, lat) parameters is located at the origin, i.e. has the
     * geographical coordinates (0, 0).
     * <p>
     * The new rotation is composed of a clockwise rotation about the z-axis
     * (which corresponds to S-N direction), a counter-clockwise rotation
     * about the y-axis (which corresponds to W-E direction), and a counter
     * clockwise rotation about the x-axis by the angle alpha.
     *
     * @param lon   the geographical longitude of the point becoming the new
     *              origin.
     * @param lat   the geographical latitude of the point becoming the new
     *              origin.
     * @param alpha the rotation angle (degrees) corresponding to the x-axis.
     */
    public Rotator(double lon, double lat, double alpha) {
        this.lon = lon;
        this.lat = lat;
        this.alpha = alpha;

        final double u = toRadians(lon);
        final double v = toRadians(lat);
        final double w = toRadians(alpha);

        final double cu = cos(u);
        final double cv = cos(v);
        final double cw = cos(w);
        final double su = sin(u);
        final double sv = sin(v);
        final double sw = sin(w);

        a11 = cu * cv;
        a12 = su * cv;
        a13 = sv;

        a21 = sw * (cu * sv) - su * cw;
        a22 = cw * cu + sw * (su * sv);
        a23 = -sw * cv;

        a31 = sw * -su - cw * (cu * sv);
        a32 = sw * cu - cw * (su * sv);
        a33 = cw * cv;
    }

    /**
     * Returns the central longitude of rotation.
     *
     * @return the central longitude of rotation.
     */
    public double getLon() {
        return lon;
    }

    /**
     * Returns the central latitude of rotation.
     *
     * @return the central latitude of rotation.
     */
    public double getLat() {
        return lat;
    }

    /**
     * Returns the x-axis rotation angle.
     *
     * @return the x-axis rotation angle.
     */
    public double getAlpha() {
        return alpha;
    }

    /**
     * Constructs a new rotation. In the rotated system, the point defined
     * by the (lon, lat) parameters is located at the origin, i.e. has the
     * geographical coordinates (0, 0).
     * <p>
     * The new rotation is composed of a clockwise rotation about the z-axis
     * (which corresponds to S-N direction) and a counter-clockwise rotation
     * about the y-axis (which corresponds to W-E direction).
     *
     * @param point the point becoming the new origin, the x and y components
     *              of which define, respectively, the geographical longitude
     *              and latitude.
     */
    public Rotator(Point2D point) {
        this(point, 0.0);
    }

    /**
     * Constructs a new rotation. In the rotated system, the point defined
     * by the (lon, lat) parameters is located at the origin, i.e. has the
     * geographical coordinates (0, 0).
     * <p>
     * The new rotation is composed of a clockwise rotation about the z-axis
     * (which corresponds to S-N direction), a counter-clockwise rotation
     * about the y-axis (which corresponds to W-E direction), and a counter
     * clockwise rotation about the x-axis by the angle alpha.
     *
     * @param point the point becoming the new origin, the x and y components
     *              of which define, respectively, the geographical longitude
     *              and latitude.
     * @param alpha the rotation angle (degrees) corresponding to the x-axis.
     */
    public Rotator(Point2D point, double alpha) {
        this(point.getX(), point.getY(), alpha);
    }

    public static Point2D calculateCenter(double[][] data, int lonIndex, int latIndex) {
        // calculate (x, y, z) in order to avoid issues with anti-meridian and poles
        final int size = data.length;
        final double[] x = new double[size];
        final double[] y = new double[size];
        final double[] z = new double[size];

        calculateXYZ(data, x, y, z, lonIndex, latIndex);

        double xc = 0.0;
        double yc = 0.0;
        double zc = 0.0;
        for (int i = 0; i < size; i++) {
            xc += x[i];
            yc += y[i];
            zc += z[i];
        }
        final double length = Math.sqrt(xc * xc + yc * yc + zc * zc);
        xc /= length;
        yc /= length;
        zc /= length;

        final double lat = toDegrees(asin(zc));
        final double lon = toDegrees(atan2(yc, xc));

        return new Point2D.Double(lon, lat);
    }

    static void calculateXYZ(double[][] data, double[] x, double[] y, double[] z, int lonIndex, int latIndex) {
        for (int i = 0; i < data.length; i++) {
            final double lon = data[i][lonIndex];
            final double lat = data[i][latIndex];
            final double u = toRadians(lon);
            final double v = toRadians(lat);
            final double w = cos(v);

            x[i] = cos(u) * w;
            y[i] = sin(u) * w;
            z[i] = sin(v);
        }
    }

    /**
     * Transforms a given geographical point into the rotated coordinate
     * system.
     *
     * @param point the point.
     */
    public void transform(Point2D point) {
        double lon = point.getX();
        double lat = point.getY();

        final double u = toRadians(lon);
        final double v = toRadians(lat);

        final double w = cos(v);
        final double x = cos(u) * w;
        final double y = sin(u) * w;
        final double z = sin(v);

        final double x2 = a11 * x + a12 * y + a13 * z;
        final double y2 = a21 * x + a22 * y + a23 * z;
        final double z2 = a31 * x + a32 * y + a33 * z;

        lat = toDegrees(asin(z2));
        lon = toDegrees(atan2(y2, x2));

        point.setLocation(lon, lat);
    }

    /**
     * Transforms a given set of geographical longitudes and latitudes
     * into the rotated coordinate system.
     *
     * @param lons the geographical longitudes.
     * @param lats the geographical latitudes.
     */
    public void transform(double[] lons, double[] lats) {
        for (int i = 0; i < lats.length; i++) {
            final double u = toRadians(lons[i]);
            final double v = toRadians(lats[i]);

            final double w = cos(v);
            final double x = cos(u) * w;
            final double y = sin(u) * w;
            final double z = sin(v);

            final double x2 = a11 * x + a12 * y + a13 * z;
            final double y2 = a21 * x + a22 * y + a23 * z;
            final double z2 = a31 * x + a32 * y + a33 * z;

            lats[i] = toDegrees(asin(z2));
            lons[i] = toDegrees(atan2(y2, x2));
        }
    }

    void transform(double[][] data, final int lonIndex, final int latIndex) {
        for (final double[] p : data) {
            final double u = toRadians(p[lonIndex]);
            final double v = toRadians(p[latIndex]);

            final double w = cos(v);
            final double x = cos(u) * w;
            final double y = sin(u) * w;
            final double z = sin(v);

            final double x2 = a11 * x + a12 * y + a13 * z;
            final double y2 = a21 * x + a22 * y + a23 * z;
            final double z2 = a31 * x + a32 * y + a33 * z;

            p[lonIndex] = toDegrees(atan2(y2, x2));
            p[latIndex] = toDegrees(asin(z2));
        }
    }

    /**
     * Transforms a given geographical point back into the unrotated
     * coordinate system.
     *
     * @param point the point.
     */
    public void transformInversely(Point2D point) {
        double lon = point.getX();
        double lat = point.getY();

        final double u = toRadians(lon);
        final double v = toRadians(lat);

        final double w = cos(v);
        final double x = cos(u) * w;
        final double y = sin(u) * w;
        final double z = sin(v);

        final double x2 = a11 * x + a21 * y + a31 * z;
        final double y2 = a12 * x + a22 * y + a32 * z;
        final double z2 = a13 * x + a23 * y + a33 * z;

        lat = toDegrees(asin(z2));
        lon = toDegrees(atan2(y2, x2));

        point.setLocation(lon, lat);
    }

    /**
     * Transforms a given set of geographical longitudes and latitudes
     * back into the unrotated coordinate system.
     *
     * @param lons the geographical longitudes.
     * @param lats the geographical latitudes.
     */
    public void transformInversely(double[] lons, double[] lats) {
        for (int i = 0; i < lats.length; i++) {
            final double u = toRadians(lons[i]);
            final double v = toRadians(lats[i]);

            final double w = cos(v);
            final double x = cos(u) * w;
            final double y = sin(u) * w;
            final double z = sin(v);

            final double x2 = a11 * x + a21 * y + a31 * z;
            final double y2 = a12 * x + a22 * y + a32 * z;
            final double z2 = a13 * x + a23 * y + a33 * z;

            lats[i] = toDegrees(asin(z2));
            lons[i] = toDegrees(atan2(y2, x2));
        }
    }
}
