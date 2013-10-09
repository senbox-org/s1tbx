package org.esa.beam.framework.datamodel;
/*
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

import org.esa.beam.util.math.DistanceCalculator;
import org.esa.beam.util.math.SphericalDistanceCalculator;

import javax.media.jai.PlanarImage;
import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.awt.image.Raster;
import java.util.ArrayList;
import java.util.List;

/**
 * A class for estimating the pixel position for a given geo-location. To be used with
 * pixel geo-codings in order to obtain a fast and accurate estimate.
 *
 * @author Ralf Quast
 */
public class PixelPosEstimator {

    private static final int LAT = 0;
    private static final int LON = 1;
    private static final int X = 2;
    private static final int Y = 3;
    private static final int MAX_POINT_COUNT_PER_TILE = 1000;

    private final Approximation[] approximations;
    private final Rectangle bounds;

    public PixelPosEstimator(PlanarImage lonImage, PlanarImage latImage, PlanarImage maskImage, double accuracy) {
        this(lonImage, latImage, maskImage, accuracy, new PixelSteppingFactory());
    }

    public PixelPosEstimator(PlanarImage lonImage, PlanarImage latImage, PlanarImage maskImage, double accuracy,
                             SteppingFactory steppingFactory) {
        if (maskImage == null) {
            maskImage = ConstantDescriptor.create((float) lonImage.getWidth(),
                                                  (float) lonImage.getHeight(),
                                                  new Byte[]{1}, null);
        }
        approximations = createApproximations(lonImage, latImage, maskImage, accuracy, steppingFactory);
        bounds = lonImage.getBounds();
    }

    public final boolean canGetPixelPos() {
        return approximations != null;
    }

    public Approximation getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        Approximation approximation = null;
        if (approximations != null) {
            if (pixelPos == null) {
                pixelPos = new PixelPos();
            }
            if (geoPos.isValid()) {
                double lat = geoPos.getLat();
                double lon = geoPos.getLon();
                approximation = findBestApproximation(lat, lon);
                if (approximation != null) {
                    final Rotator rotator = approximation.getRotator();
                    final Point2D p = new Point2D.Double(lon, lat);
                    rotator.transform(p);
                    lon = p.getX();
                    lat = p.getY();
                    final double x = approximation.getFX().getValue(lat, lon);
                    if (x < bounds.getMinX() || x > bounds.getMaxX()) {
                        pixelPos.setInvalid();
                    } else {
                        final double y = approximation.getFY().getValue(lat, lon);
                        if (y < bounds.getMinY() || y > bounds.getMaxY()) {
                            pixelPos.setInvalid();
                        } else {
                            pixelPos.x = (float) x;
                            pixelPos.y = (float) y;
                        }
                    }
                } else {
                    pixelPos.setInvalid();
                }
            } else {
                pixelPos.setInvalid();
            }
        }
        return approximation;
    }

    Approximation findBestApproximation(double lat, double lon) {
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
                                                PlanarImage maskImage,
                                                double accuracy,
                                                SteppingFactory steppingFactory) {
        final ArrayList<Approximation> approximations = new ArrayList<Approximation>();
        final int tileCountX = lonImage.getNumXTiles();
        final int tileCountY = lonImage.getNumYTiles();

        for (int y = 0; y < tileCountY; y++) {
            for (int x = 0; x < tileCountX; x++) {
                final Rectangle rectangle = lonImage.getTileRect(x, y);
                final Stepping stepping = steppingFactory.createStepping(rectangle, MAX_POINT_COUNT_PER_TILE);
                final double[][] data = extractWarpPoints(lonImage, latImage, maskImage, stepping);
                final Approximation approximation = Approximation.create(data, accuracy, rectangle);
                if (approximation == null) {
                    return null;
                }
                approximations.add(approximation);
            }
        }

        return approximations.toArray(new Approximation[approximations.size()]);
    }

    private static int getSample(int pixelX, int pixelY, PlanarImage image) {
        final int x = image.getMinX() + pixelX;
        final int y = image.getMinY() + pixelY;
        final int tileX = image.XToTileX(x);
        final int tileY = image.YToTileY(y);
        final Raster data = image.getTile(tileX, tileY);

        return data.getSample(x, y, 0);
    }

    private static double getSampleDouble(int pixelX, int pixelY, PlanarImage image) {
        final int x = image.getMinX() + pixelX;
        final int y = image.getMinY() + pixelY;
        final int tileX = image.XToTileX(x);
        final int tileY = image.YToTileY(y);
        final Raster data = image.getTile(tileX, tileY);

        return data.getSampleDouble(x, y, 0);
    }

    static double[][] extractWarpPoints(PlanarImage lonImage, PlanarImage latImage, PlanarImage maskImage,
                                        Stepping stepping) {
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
                final int mask = getSample(x, y, maskImage);
                if (mask != 0) {
                    final double lat = getSampleDouble(x, y, latImage);
                    final double lon = getSampleDouble(x, y, lonImage);
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
        }

        return pointList.toArray(new double[pointList.size()][4]);
    }

    public static final class Stepping {

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

    /**
     * Approximates the x(lat, lon) and y(lat, lon) functions.
     */
    public static final class Approximation {

        private final RationalFunctionModel fX;
        private final RationalFunctionModel fY;
        private final double maxDistance;
        private final Rotator rotator;
        private final DistanceCalculator calculator;
        private final Rectangle rectangle;

        /**
         * Creates a new instance of this class.
         *
         * @param data      The array of (lat, lon, x, y) points that is used to compute the approximations to the
         *                  x(lat, lon) and y(lat, lon) functions. Note that the contents of the data array is modified
         *                  by this method.
         * @param accuracy  The accuracy goal.
         * @param rectangle The domain of the lat(x, y) and lon(x, y) functions.
         *
         * @return a new approximation or {@code null} if the accuracy goal cannot not be met.
         */
        public static Approximation create(double[][] data, double accuracy, Rectangle rectangle) {
            final Point2D centerPoint = Rotator.calculateCenter(data, LON, LAT);
            final double centerLon = centerPoint.getX();
            final double centerLat = centerPoint.getY();
            final double maxDistance = maxDistance(data, centerLon, centerLat);

            final Rotator rotator = new Rotator(centerLon, centerLat);
            rotator.transform(data, LON, LAT);

            final int[] xIndices = new int[]{LAT, LON, X};
            final int[] yIndices = new int[]{LAT, LON, Y};

            final RationalFunctionModel fX = findBestModel(data, xIndices, accuracy);
            final RationalFunctionModel fY = findBestModel(data, yIndices, accuracy);
            if (fX == null || fY == null) {
                return null;
            }

            return new Approximation(fX, fY, maxDistance * 1.1, rotator,
                                     new SphericalDistanceCalculator(centerLon, centerLat), rectangle);
        }

        /**
         * Returns the (approximation to) the x(lat, lon) function.
         *
         * @return the (approximation to) the x(lat, lon) function.
         */
        public RationalFunctionModel getFX() {
            return fX;
        }

        /**
         * Returns the (approximation to) the y(lat, lon) function.
         *
         * @return the (approximation to) the y(lat, lon) function.
         */
        public RationalFunctionModel getFY() {
            return fY;
        }

        /**
         * Returns the maximum distance (in radian) within which this approximation is valid.
         *
         * @return the maximum distance (in radian).
         */
        public double getMaxDistance() {
            return maxDistance;
        }

        /**
         * Returns the distance (in radian) of 'the center of this approximation' to a given (lat, lon) point.
         *
         * @param lat The latitude.
         * @param lon The longitude.
         *
         * @return the distance (in radian).
         */
        public double getDistance(double lat, double lon) {
            return calculator.distance(lon, lat);
        }

        /**
         * Returns the {@code Rotator} associated with this approximation.
         *
         * @return
         */
        public Rotator getRotator() {
            return rotator;
        }

        /**
         * Returns the domain of the lat(x, y) and lon(x, y) functions associated with this approximation of the
         *
         * @return
         */
        public Rectangle getRectangle() {
            return rectangle;
        }

        private Approximation(RationalFunctionModel fX, RationalFunctionModel fY, double maxDistance,
                              Rotator rotator, DistanceCalculator calculator, Rectangle rectangle) {
            this.fX = fX;
            this.fY = fY;
            this.maxDistance = maxDistance;
            this.rotator = rotator;
            this.calculator = calculator;
            this.rectangle = rectangle;
        }

        private static double maxDistance(final double[][] data, double centerLon, double centerLat) {
            final DistanceCalculator distanceCalculator = new SphericalDistanceCalculator(centerLon, centerLat);
            double maxDistance = 0.0;
            for (final double[] p : data) {
                final double d = distanceCalculator.distance(p[LON], p[LAT]);
                if (d > maxDistance) {
                    maxDistance = d;
                }
            }
            return maxDistance;
        }

        private static RationalFunctionModel findBestModel(double[][] data, int[] indexes, double accuracy) {
            RationalFunctionModel bestModel = null;
            search:
            for (int degreeP = 0; degreeP <= 4; degreeP++) {
                for (int degreeQ = 0; degreeQ <= degreeP; degreeQ++) {
                    final int termCountP = RationalFunctionModel.getTermCountP(degreeP);
                    final int termCountQ = RationalFunctionModel.getTermCountQ(degreeQ);
                    if (data.length >= termCountP + termCountQ) {
                        final RationalFunctionModel model = createModel(degreeP, degreeQ, data, indexes);
                        if (bestModel == null || model.getRmse() < bestModel.getRmse()) {
                            bestModel = model;
                        }
                        if (bestModel.getRmse() < accuracy) {
                            break search;
                        }
                    }
                }
            }
            return bestModel;
        }

        private static RationalFunctionModel createModel(int degreeP, int degreeQ, double[][] data, int[] indexes) {
            final int ix = indexes[0];
            final int iy = indexes[1];
            final int iz = indexes[2];
            final double[] x = new double[data.length];
            final double[] y = new double[data.length];
            final double[] g = new double[data.length];
            for (int i = 0; i < data.length; i++) {
                x[i] = data[i][ix];
                y[i] = data[i][iy];
                g[i] = data[i][iz];
            }

            return new RationalFunctionModel(degreeP, degreeQ, x, y, g);
        }
    }

    public static interface SteppingFactory {

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

}
