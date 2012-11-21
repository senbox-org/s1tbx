package org.esa.beam.framework.datamodel;/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.math.MathUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import javax.media.jai.PlanarImage;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.List;

class Estimator implements GeoCoding {

    private static final int LAT = 0;
    private static final int LON = 1;
    private static final int X = 2;
    private static final int Y = 3;

    private static final int MAX_NUM_POINTS_PER_TILE = 1000;
    private final Approximation[] approximations;

    Estimator(PlanarImage lonImage, PlanarImage latImage, SteppingFactory steppingFactory, double maxDegrees) {
        approximations = createApproximations(lonImage, latImage, steppingFactory, maxDegrees);
    }

    static RationalFunctionModel findBestModel(double[][] data, int[] indexes) {
        return new RationalFunctionModel(3, 3, data, indexes, 0); // 2, 0
    }

    @Override
    public boolean isCrossingMeridianAt180() {
        return false;
    }

    @Override
    public boolean canGetPixelPos() {
        return true;
    }

    @Override
    public boolean canGetGeoPos() {
        return false;
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (approximations != null) {
            if (pixelPos == null) {
                pixelPos = new PixelPos();
            }
            if (geoPos.isValid()) {
                double lat = geoPos.getLat();
                double lon = geoPos.getLon();
                final Approximation approximation = getBestApproximation(lat, lon);
                if (approximation != null) {
                    final Rotator rotator = approximation.getRotator();
                    final Point2D p = new Point2D.Double(lon, lat);
                    rotator.transform(p);
                    lon = p.getX();
                    lat = p.getY();
                    pixelPos.x = (float) approximation.getFX().getValue(lat, lon);
                    pixelPos.y = (float) approximation.getFY().getValue(lat, lon);
                } else {
                    pixelPos.setInvalid();
                }
            } else {
                pixelPos.setInvalid();
            }
        }
        return pixelPos;
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        return geoPos;
    }

    @Override
    public Datum getDatum() {
        return Datum.WGS_84;
    }

    @Override
    public void dispose() {
    }

    @Override
    public CoordinateReferenceSystem getImageCRS() {
        return null;
    }

    @Override
    public CoordinateReferenceSystem getMapCRS() {
        return DefaultGeographicCRS.WGS84;
    }

    @Override
    public CoordinateReferenceSystem getGeoCRS() {
        return DefaultGeographicCRS.WGS84;
    }

    @Override
    public MathTransform getImageToMapTransform() {
        return null;
    }

    Approximation getBestApproximation(double lat, double lon) {
        Approximation bestApproximation = null;
        if (approximations.length == 1) {
            Approximation a = approximations[0];
            final double distance = a.getDistance(lat, lon);
            if (distance < a.getMaxDistance()) {
                bestApproximation = a;
            }
        } else {
            double minDistance = Double.MAX_VALUE;
            for (final Approximation a : approximations) {
                final double distance = a.getDistance(lat, lon);
                if (distance < minDistance && distance < a.getMaxDistance()) {
                    minDistance = distance;
                    bestApproximation = a;
                }
            }
        }
        return bestApproximation;
    }

    static Approximation[] createApproximations(PlanarImage lonImage,
                                                PlanarImage latImage,
                                                SteppingFactory steppingFactory, double maxDegrees) {
        final int w = latImage.getWidth();
        final int h = latImage.getHeight();
        final int tileCount = calculateTileCount(lonImage, latImage, maxDegrees);

        final Dimension tileDimension = MathUtils.fitDimension(tileCount, w, h); // TODO - check why this routine can yield less rectangles than given by tileCount
        final int tileCountX = tileDimension.width; // 4
        final int tileCountY = tileDimension.height; // 32

        // compute actual approximations for all tiles
        final Approximation[] approximations = new Approximation[tileCountX * tileCountY];
        final Rectangle[] rectangles = MathUtils.subdivideRectangle(w, h, tileCountX, tileCountY, 1);
        for (int i = 0; i < rectangles.length; i++) {
            final Stepping stepping = steppingFactory.createStepping(rectangles[i], MAX_NUM_POINTS_PER_TILE);
            final Raster lonData = lonImage.getData(rectangles[i]);
            final Raster latData = latImage.getData(rectangles[i]);
            final double[][] data = extractWarpPoints(lonData, latData, stepping);
            final Approximation approximation = createApproximation(data);
            if (approximation == null) {
                return null;
            }
            approximations[i] = approximation;
        }
        return approximations;
    }

    static int calculateTileCount(PlanarImage lonImage, PlanarImage latImage, double maxDegrees) {
        final int w = latImage.getWidth();
        final int h = latImage.getHeight();
        final double lat0 = getSampleDouble(latImage, w / 4, h / 4);
        final double lon0 = getSampleDouble(lonImage, w / 4, h / 4);
        final DistanceCalculator distanceCalculator = new ArcDistanceCalculator(lon0, lat0);
        final double latX = getSampleDouble(latImage, 3 * w / 4, h / 4);
        final double lonX = getSampleDouble(lonImage, 3 * w / 4, h / 4);
        final double latY = getSampleDouble(latImage, w / 4, 3 * h / 4);
        final double lonY = getSampleDouble(lonImage, w / 4, 3 * h / 4);
        final double sizeX = Math.toDegrees(distanceCalculator.distance(lonX, latX)) / (w / 2);
        final double sizeY = Math.toDegrees(distanceCalculator.distance(lonY, latY)) / (h / 2);
        final double tileSizeX = maxDegrees / sizeX;
        final double tileSizeY = maxDegrees / sizeY;
        final int tileCountX = (int) (w / tileSizeX + 1);
        final int tileCountY = (int) (h / tileSizeY + 1);

        return tileCountX * tileCountY;
    }

    private static double getSampleDouble(PlanarImage image, int x, int y) {
        final int tileX = image.XToTileX(x);
        final int tileY = image.YToTileY(y);

        return image.getTile(tileX, tileY).getSampleDouble(x, y, 0);
    }

    static Approximation createApproximation(double[][] data) {
        final Point2D centerPoint = Rotator.calculateCenter(data, LON, LAT);
        final double centerLon = centerPoint.getX();
        final double centerLat = centerPoint.getY();
        final double maxDistance = maxDistance(data, centerLat, centerLon);

        final Rotator rotator = new Rotator(centerLon, centerLat);
        rotator.transform(data, LON, LAT);

        final int[] xIndices = new int[]{LAT, LON, X};
        final int[] yIndices = new int[]{LAT, LON, Y};

        final RationalFunctionModel fX = findBestModel(data, xIndices);
        final RationalFunctionModel fY = findBestModel(data, yIndices);
//        final FXYSum fX = TiePointGeoCoding.getBestPolynomial(data, xIndices);
//        final FXYSum fY = TiePointGeoCoding.getBestPolynomial(data, yIndices);
        if (fX == null || fY == null) {
            return null;
        }

        return new Approximation(fX, fY, centerLat, centerLon, maxDistance * 1.1, rotator);
    }

    static double maxDistance(final double[][] data, double centerLat, double centerLon) {
        final DistanceCalculator distanceCalculator = new ArcDistanceCalculator(centerLon, centerLat);
        double maxDistance = 0.0;
        for (final double[] p : data) {
            final double d = distanceCalculator.distance(p[LON], p[LAT]);
            if (d > maxDistance) {
                maxDistance = d;
            }
        }
        return maxDistance;
    }

    static double[][] extractWarpPoints(Raster lonData, Raster latData, Stepping stepping) {
        final int minX = stepping.getMinX();
        final int maxX = stepping.getMaxX();
        final int minY = stepping.getMinY();
        final int maxY = stepping.getMaxY();
        final int pointCountX = stepping.getPointCountX();
        final int pointCountY = stepping.getPointCountY();
        final int stepX = stepping.getStepX();
        final int stepY = stepping.getStepY();
        final int pointCount = stepping.getPointCount();
        final List<double[]> pointList = new ArrayList<double[]>(pointCount);

        for (int j = 0, k = 0; j < pointCountY; j++) {
            int y = minY + j * stepY;
            // adjust bottom border
            if (y > maxY) {
                y = maxY;
            }
            for (int i = 0; i < pointCountX; i++, k++) {
                int x = minX + i * stepX;
                // adjust right border
                if (x > maxX) {
                    x = maxX;
                }
                final double lat = latData.getSampleDouble(x, y, 0);
                final double lon = lonData.getSampleDouble(x, y, 0);
                if (lon >= -180.0 && lon <= 180.0 && lat >= -90.0 && lat <= 90.0) {
                    final double[] point = new double[4];
                    point[LAT] = lat;
                    point[LON] = lon;
                    point[X] = x + 0.5;
                    point[Y] = y + 0.5;
                    pointList.add(point);
                }
            }
        }

        return pointList.toArray(new double[pointList.size()][4]);
    }

    static final class Stepping {

        private final int minX;
        private final int minY;
        private final int maxX;
        private final int maxY;
        private final int pointCountX;
        private final int pointCountY;
        private final int stepX;
        private final int stepY;

        Stepping(int minX, int minY, int maxX, int maxY, int pointCountX, int pointCountY, int stepX, int stepY) {
            this.minX = minX;
            this.maxX = maxX;
            this.minY = minY;
            this.maxY = maxY;
            this.pointCountX = pointCountX;
            this.pointCountY = pointCountY;
            this.stepX = stepX;
            this.stepY = stepY;
        }

        int getMinX() {
            return minX;
        }

        int getMaxX() {
            return maxX;
        }

        int getMinY() {
            return minY;
        }

        int getMaxY() {
            return maxY;
        }

        int getPointCountX() {
            return pointCountX;
        }

        int getPointCountY() {
            return pointCountY;
        }

        int getStepX() {
            return stepX;
        }

        int getStepY() {
            return stepY;
        }

        int getPointCount() {
            return pointCountX * pointCountY;
        }
    }

    static final class Approximation {

        private final RationalFunctionModel fX;
        private final RationalFunctionModel fY;
        private final double centerLat;
        private final double centerLon;
        private final double maxDistance;
        private final Rotator rotator;
        private final DistanceCalculator distanceCalculator;

        public Approximation(RationalFunctionModel fX, RationalFunctionModel fY, double centerLat, double centerLon,
                             double maxDistance, Rotator rotator) {
            this.fX = fX;
            this.fY = fY;
            this.centerLat = centerLat;
            this.centerLon = centerLon;
            this.maxDistance = maxDistance;
            this.rotator = rotator;
            this.distanceCalculator = new ArcDistanceCalculator(centerLon, centerLat);
        }

        public RationalFunctionModel getFX() {
            return fX;
        }

        public RationalFunctionModel getFY() {
            return fY;
        }

        public double getCenterLat() {
            return centerLat;
        }

        public double getCenterLon() {
            return centerLon;
        }

        public double getMaxDistance() {
            return maxDistance;
        }

        public double getDistance(double lat, double lon) {
            return distanceCalculator.distance(lon, lat);
        }

        public Rotator getRotator() {
            return rotator;
        }
    }

    static interface SteppingFactory {

        Stepping createStepping(Rectangle rectangle, int maxPointCount);
    }

    static class PixelSteppingFactory implements SteppingFactory {

        @Override
        public Stepping createStepping(Rectangle rectangle, int maxPointCount) {
            final int sw = rectangle.width;
            final int sh = rectangle.height;
            final int minX = rectangle.x;
            final int minY = rectangle.y;
            final int maxX = minX + sw - 1;
            final int maxY = minY + sh - 1;

            // Determine stepX and stepY so that maximum number of points is not exceeded
            int pointCountX = sw;
            int pointCountY = sh;
            int stepX = 1;
            int stepY = 1;

            // Adjust number of warp points to be considered so that a maximum of circa
            // maxPointCount points is not exceeded
            boolean adjustStepX = true;
            while (pointCountX * pointCountY > maxPointCount) {
                if (adjustStepX) {
                    stepX++;
                    pointCountX = sw / stepX + 1;
                } else {
                    stepY++;
                    pointCountY = sh / stepY + 1;
                }
                adjustStepX = !adjustStepX;
            }
            pointCountX = Math.max(1, pointCountX);
            pointCountY = Math.max(1, pointCountY);

            // Make sure we include the right border points,
            // if sw/stepX not divisible without remainder
            if (sw % stepX != 0) {
                pointCountX++;
            }
            // Make sure we include the bottom border points,
            // if sh/stepY not divisible without remainder
            if (sh % stepY != 0) {
                pointCountY++;
            }

            return new Stepping(minX, minY, maxX, maxY, pointCountX, pointCountY, stepX, stepY);
        }
    }

    interface DistanceCalculator {

        double distance(double lon, double lat);
    }

    static final class ArcDistanceCalculator implements DistanceCalculator {

        private final double lon;
        private final double si;
        private final double co;

        ArcDistanceCalculator(double lon, double lat) {
            this.lon = lon;
            this.si = Math.sin(Math.toRadians(lat));
            this.co = Math.cos(Math.toRadians(lat));
        }

        @Override
        public double distance(double lon, double lat) {
            final double phi = Math.toRadians(lat);
            return Math.acos(si * Math.sin(phi) + co * Math.cos(phi) * Math.cos(Math.toRadians(lon - this.lon)));
        }
    }
}
