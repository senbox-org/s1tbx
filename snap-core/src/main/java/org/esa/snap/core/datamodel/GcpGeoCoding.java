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

import org.esa.snap.core.dataio.ProductSubsetDef;
import org.esa.snap.core.dataop.maptransf.Datum;

import java.awt.geom.Point2D;
import java.util.Arrays;

import static java.lang.Math.*;

/**
 * Ground control point (GCP) geo-coding.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class GcpGeoCoding extends AbstractGeoCoding {

    private double[] x;
    private double[] y;
    private double[] lons;
    private double[] lats;

    private int sceneWidth;
    private int sceneHeight;

    private RationalFunctionMap2D forwardMap;
    private RationalFunctionMap2D inverseMap;
    private Rotator rotator;

    private GeoCoding originalGeoCoding;
    private Datum datum;
    private boolean boundaryCrossingMeridianAt180;
    private Method method;

    /**
     * Constructs a new instance of this class.
     *
     * @param method      the approximation method.
     * @param gcps        the ground control points.
     * @param sceneWidth  the scene width.
     * @param sceneHeight the scene height.
     * @param datum       the datum.
     */
    public GcpGeoCoding(Method method, Placemark[] gcps, int sceneWidth, int sceneHeight, Datum datum) {
        this.sceneWidth = sceneWidth;
        this.sceneHeight = sceneHeight;
        this.datum = datum;
        this.method = method;

        initCoordinates(gcps);
        initTransformations(method);
        boundaryCrossingMeridianAt180 = isBoundaryCrossingMeridianAt180();
    }

    /**
     * Constructs a new instance of this class.
     *
     * @param method      the approximation method.
     * @param x           the x coordinates.
     * @param y           the y coordinates.
     * @param lons        the longitudes.
     * @param lats        the latitudes.
     * @param sceneWidth  the scene width.
     * @param sceneHeight the scene height.
     * @param datum       the datum.
     */
    public GcpGeoCoding(Method method, double[] x, double[] y, double[] lons, double[] lats,
                        int sceneWidth, int sceneHeight, Datum datum) {
        this.x = Arrays.copyOf(x, x.length);
        this.y = Arrays.copyOf(y, y.length);
        this.lons = Arrays.copyOf(lons, lons.length);
        this.lats = Arrays.copyOf(lats, lats.length);

        this.sceneWidth = sceneWidth;
        this.sceneHeight = sceneHeight;
        this.datum = datum;
        this.method = method;

        initTransformations(method);
        boundaryCrossingMeridianAt180 = isBoundaryCrossingMeridianAt180();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean transferGeoCoding(Scene sourceScene, Scene targetScene, ProductSubsetDef subsetDef) {
        final double[] x2 = new double[x.length];
        final double[] y2 = new double[y.length];

        int offsetX = 0;
        int offsetY = 0;
        if (subsetDef != null && subsetDef.getRegion() != null) {
            offsetX = subsetDef.getRegion().x;
            offsetY = subsetDef.getRegion().y;
        }
        int subSamplingX = 1;
        int subSamplingY = 1;
        if (subsetDef != null) {
            subSamplingX = subsetDef.getSubSamplingX();
            subSamplingY = subsetDef.getSubSamplingY();
        }

        final int sceneWidth = targetScene.getRasterWidth();
        final int sceneHeight = targetScene.getRasterHeight();

        for (int i = 0; i < x2.length; i++) {
            x2[i] = (x[i] - offsetX) / subSamplingX;
            y2[i] = (y[i] - offsetY) / subSamplingY;
        }
        targetScene.setGeoCoding(new GcpGeoCoding(getMethod(), x2, y2, lons, lats, sceneWidth, sceneHeight, datum));
        return true;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isCrossingMeridianAt180() {
        return boundaryCrossingMeridianAt180;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canGetPixelPos() {
        return inverseMap != null;
    }

    /**
     * {@inheritDoc}
     */
    public boolean canGetGeoPos() {
        return forwardMap != null;
    }

    /**
     * {@inheritDoc}
     */
    public PixelPos getPixelPos(final GeoPos geoPos, PixelPos pixelPos) {
        final Point2D.Double point = new Point2D.Double(geoPos.lon, geoPos.lat);
        rotator.transform(point);
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        pixelPos.setLocation(inverseMap.getValue(point));
        if (!pixelPos.isValid() || pixelPos.x < 0 || pixelPos.x >= sceneWidth || pixelPos.y < 0 || pixelPos.y >= sceneHeight) {
            pixelPos.x = -1;
            pixelPos.y = -1;
        }
        return pixelPos;
    }

    /**
     * {@inheritDoc}
     */
    public GeoPos getGeoPos(final PixelPos pixelPos, GeoPos geoPos) {
        final Point2D point = forwardMap.getValue(pixelPos);
        rotator.transformInversely(point);
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        geoPos.setLocation(point.getY(), point.getX());

        return geoPos;
    }

    /**
     * {@inheritDoc}
     */
    public Datum getDatum() {
        return datum;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        GcpGeoCoding that = (GcpGeoCoding) o;

        if (!Arrays.equals(lats, that.lats)) return false;
        if (!Arrays.equals(lons, that.lons)) return false;
        if (method != that.method) return false;
        if (!Arrays.equals(x, that.x)) return false;
        if (!Arrays.equals(y, that.y)) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = Arrays.hashCode(x);
        result = 31 * result + Arrays.hashCode(y);
        result = 31 * result + Arrays.hashCode(lons);
        result = 31 * result + Arrays.hashCode(lats);
        result = 31 * result + method.hashCode();
        return result;
    }

    /**
     * {@inheritDoc}
     */
    public void dispose() {
        rotator = null;
        datum = null;

        forwardMap = null;
        inverseMap = null;
    }

    /**
     * Returns the root mean square error (RMSE) of the longitude approximation.
     *
     * @return the longitude RMSE.
     */
    public double getRmseLon() {
        return forwardMap.getRmseU();
    }

    /**
     * Returns the root mean square error (RMSE) of the latitude approximation.
     *
     * @return the latitude RMSE.
     */
    public double getRmseLat() {
        return forwardMap.getRmseV();
    }

    /**
     * Returns the approximation method used for this geo-coding.
     *
     * @return the approximation method.
     */
    public Method getMethod() {
        return method;

    }

    public void setOriginalGeoCoding(GeoCoding geoCoding) {
        originalGeoCoding = geoCoding;
    }

    public GeoCoding getOriginalGeoCoding() {
        return originalGeoCoding;
    }

    public void setGcps(Placemark[] gcps) {
        initCoordinates(gcps);
        initTransformations(method);
        boundaryCrossingMeridianAt180 = isBoundaryCrossingMeridianAt180();
    }

    private boolean isBoundaryCrossingMeridianAt180() {
        GeoPos geoPos1 = null;
        GeoPos geoPos2 = null;
        GeoPos geoPos3 = null;
        GeoPos geoPos4 = null;

        for (int i = 0; i < sceneWidth; i++) {
            geoPos1 = getGeoPos(new PixelPos(i + 0.5f, 0.0f), geoPos1);
            geoPos2 = getGeoPos(new PixelPos(i + 1.5f, 0.0f), geoPos2);
            geoPos3 = getGeoPos(new PixelPos(i + 0.5f, sceneHeight), geoPos3);
            geoPos4 = getGeoPos(new PixelPos(i + 1.5f, sceneHeight), geoPos4);

            if (isSegmentCrossingMeridianAt180(geoPos1.lon, geoPos2.lon)
                    || isSegmentCrossingMeridianAt180(geoPos3.lon, geoPos4.lon)) {
                return true;
            }
        }
        for (int i = 0; i < sceneHeight; i++) {
            geoPos1 = getGeoPos(new PixelPos(0.0f, i + 0.5f), geoPos1);
            geoPos2 = getGeoPos(new PixelPos(0.0f, i + 1.5f), geoPos2);
            geoPos3 = getGeoPos(new PixelPos(sceneWidth, i + 0.5f), geoPos3);
            geoPos4 = getGeoPos(new PixelPos(sceneWidth, i + 1.5f), geoPos4);

            if (isSegmentCrossingMeridianAt180(geoPos1.lon, geoPos2.lon)
                    || isSegmentCrossingMeridianAt180(geoPos3.lon, geoPos4.lon)) {
                return true;
            }
        }

        return false;
    }

    private void initTransformations(Method type) {
        final GeoPos center = calculateCentralGeoPos(lons, lats);

        double[] lons2 = Arrays.copyOf(lons, lons.length);
        double[] lats2 = Arrays.copyOf(lats, lats.length);

        rotator = new Rotator(center.lon, center.lat);
        rotator.transform(lons2, lats2);

        forwardMap = new RationalFunctionMap2D(type.getDegreeP(), type.getDegreeQ(), x, y, lons2, lats2);
        inverseMap = new RationalFunctionMap2D(type.getDegreeP(), type.getDegreeQ(), lons2, lats2, x, y);
    }

    private static boolean isSegmentCrossingMeridianAt180(double lon, double lon2) {
        return Math.abs(lon) > 90.0 && Math.abs(lon2) > 90.0 && lon * lon2 < 0.0;
    }

    private static void calculateXYZ(double[] lons, double[] lats, double[] x, double[] y, double[] z) {
        for (int i = 0; i < lats.length; i++) {
            final double u = toRadians(lons[i]);
            final double v = toRadians(lats[i]);
            final double w = cos(v);

            x[i] = cos(u) * w;
            y[i] = sin(u) * w;
            z[i] = sin(v);
        }
    }

    static GeoPos calculateCentralGeoPos(double[] lons, double[] lats) {
        // calculate (x, y, z) in order to avoid issues with anti-meridian and poles
        final int size = lats.length;
        final double[] x = new double[size];
        final double[] y = new double[size];
        final double[] z = new double[size];

        calculateXYZ(lons, lats, x, y, z);

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

        return new GeoPos(lat, lon);
    }

    private void initCoordinates(Placemark[] gcps) {
        for (final Placemark gcp : gcps) {
            final PixelPos pixelPos = gcp.getPixelPos();
            final GeoPos geoPos = gcp.getGeoPos();
            if (pixelPos == null || !pixelPos.isValid() || geoPos == null || !geoPos.isValid()) {
                throw new IllegalArgumentException("Invalid ground control point.");
            }
        }
        x = new double[gcps.length];
        y = new double[gcps.length];

        lons = new double[gcps.length];
        lats = new double[gcps.length];

        for (int i = 0; i < gcps.length; i++) {
            final PixelPos pixelPos = gcps[i].getPixelPos();
            x[i] = pixelPos.getX();
            y[i] = pixelPos.getY();

            final GeoPos geoPos = gcps[i].getGeoPos();
            lons[i] = geoPos.getLon();
            lats[i] = geoPos.getLat();
        }
    }

    static class RationalFunctionMap2D {

        private final RationalFunctionModel um;
        private final RationalFunctionModel vm;

        RationalFunctionMap2D(int degreeP, int degreeQ, double[] x, double[] y, double[] u, double[] v) {
            um = new RationalFunctionModel(degreeP, degreeQ, x, y, u);
            vm = new RationalFunctionModel(degreeP, degreeQ, x, y, v);
        }

        public final Point2D getValue(Point2D point) {
            return getValue(point.getX(), point.getY());
        }

        public final Point2D getValue(double x, double y) {
            return new Point2D.Double(um.getValue(x, y), vm.getValue(x, y));
        }

        public double getRmseU() {
            return um.getRmse();
        }

        public double getRmseV() {
            return vm.getRmse();
        }

    }

    /**
     * Class representing the approximation methods used for the GCP geo-coding.
     *
     * @author Ralf Quast
     * @version $Revision$ $Date$
     */
    public enum Method {
        // todo - use better names instead of  POLYNOMIAL<i> (nf - 18.01.2011)
        /**
         * Linear polynomial.
         */
        POLYNOMIAL1("Linear Polynomial", 1),
        /**
         * Quadratic polynomial.
         */
        POLYNOMIAL2("Quadratic Polynomial", 2),
        /**
         * Cubic polynomial.
         */
        POLYNOMIAL3("Cubic Polynomial", 3);

        private String name;
        private int degreeP;
        private int degreeQ;

        private Method(String name, int degreeP) {
            this(name, degreeP, 0);
        }

        private Method(String name, int degreeP, int degreeQ) {
            this.name = name;
            this.degreeP = degreeP;
            this.degreeQ = degreeQ;
        }

        /**
         * Returns the name of the approximation method.
         *
         * @return the name.
         */
        public String getName() {
            return name;
        }

        /**
         * Returns the degree of the numerator polynomial.
         *
         * @return the degree of the numerator polynomial.
         */
        public int getDegreeP() {
            return degreeP;
        }

        /**
         * Returns the degree of the denominator polynomial.
         *
         * @return the degree of the denominator polynomial.
         */
        private int getDegreeQ() {
            return degreeQ;
        }

        /**
         * Returns the number of terms for the numerator polynomial.
         *
         * @return the number of terms.
         */
        public int getTermCountP() {
            return ((getDegreeP() + 1) * (getDegreeP() + 2)) / 2;
        }

        /**
         * Returns the number of terms for the denominator polynomial.
         *
         * @return the number of terms.
         */
        private int getTermCountQ() {
            return ((getDegreeQ() + 1) * (getDegreeQ() + 2)) / 2 - 1;
        }

        @Override
        public String toString() {
            return name;
        }
    }

}
