package org.esa.beam.framework.datamodel;

import org.esa.beam.util.math.CosineDistance;
import org.esa.beam.util.math.DistanceMeasure;

import javax.media.jai.PlanarImage;
import java.awt.Rectangle;
import java.awt.geom.Point2D;
import java.util.ArrayList;
import java.util.List;

/**
 * Approximates the x(lat, lon) and y(lat, lon) functions.
 * <p/>
 * This class does not belong to the public API.
 *
 * @author Ralf Quast
 */
public final class GeoApproximation {

    private static final int LAT = 0;
    private static final int LON = 1;
    private static final int X = 2;
    private static final int Y = 3;
    private static final int MAX_POINT_COUNT_PER_TILE = 1000;

    private final RationalFunctionModel fX;
    private final RationalFunctionModel fY;
    private final RationalFunctionModel fLon;
    private final RationalFunctionModel fLat;
    private final double maxDistance;
    private final Rotator rotator;
    private final DistanceMeasure calculator;
    private final Rectangle range;

    public static GeoApproximation[] createApproximations(PlanarImage lonImage,
                                                          PlanarImage latImage,
                                                          PlanarImage maskImage,
                                                          double accuracy) {
        final SampleSource lonSamples = new PixelPosEstimator.PlanarImageSampleSource(lonImage);
        final SampleSource latSamples = new PixelPosEstimator.PlanarImageSampleSource(latImage);
        final SampleSource maskSamples;
        if (maskImage != null) {
            maskSamples = new PixelPosEstimator.PlanarImageSampleSource(maskImage);
        } else {
            maskSamples = new SampleSource() {
                @Override
                public int getSample(int x, int y) {
                    return 1;
                }

                @Override
                public double getSampleDouble(int x, int y) {
                    return 1.0;
                }
            };
        }

        final ArrayList<Rectangle> rectangleList = new ArrayList<>();
        for (int y = 0; y < lonImage.getNumYTiles(); y++) {
            for (int x = 0; x < lonImage.getNumXTiles(); x++) {
                rectangleList.add(lonImage.getTileRect(x, y));
            }
        }
        final Rectangle[] rectangles = rectangleList.toArray(new Rectangle[rectangleList.size()]);
        return createApproximations(lonSamples, latSamples, maskSamples, accuracy, rectangles,
                                    new DefaultSteppingFactory());
    }

    /**
     * Creates a new instance of this class.
     *
     * @param lonSamples      The longitude samples.
     * @param latSamples      The latitude samples.
     * @param maskSamples     The mask samples.
     * @param accuracy        The accuracy goal.
     * @param range           The range of the x(lat, lon) and y(lat, lon) functions.
     * @param steppingFactory The stepping factory.
     *
     * @return a new approximation or {@code null} if the accuracy goal cannot not be met.
     */
    private static GeoApproximation create(SampleSource lonSamples,
                                           SampleSource latSamples,
                                           SampleSource maskSamples,
                                           double accuracy,
                                           Rectangle range,
                                           SteppingFactory steppingFactory) {
        final Stepping stepping = steppingFactory.createStepping(range, MAX_POINT_COUNT_PER_TILE);
        final double[][] data = extractWarpPoints(lonSamples, latSamples, maskSamples, stepping);
        return GeoApproximation.create(data, accuracy, range);
    }

    /**
     * Creates a new instance of this class.
     *
     * @param data     The array of (lat, lon, x, y) points that is used to compute the approximations to the
     *                 x(lat, lon) and y(lat, lon) functions. Note that the contents of the data array is modified
     *                 by this method.
     * @param accuracy The accuracy goal.
     * @param range    The range of the x(lat, lon) and y(lat, lon) functions.
     *
     * @return a new approximation or {@code null} if the accuracy goal cannot not be met.
     */
    static GeoApproximation create(double[][] data, double accuracy, Rectangle range) {
        final Point2D centerPoint = Rotator.calculateCenter(data, LON, LAT);
        final double centerLon = centerPoint.getX();
        final double centerLat = centerPoint.getY();
        // the equation below is correct, if and only if, the cosine distance is used for calculating distances
        final double maxDistance = 1.0 - Math.cos(1.1 * Math.acos(1.0 - maxDistance(data, centerLon, centerLat)));

        final Rotator rotator = new Rotator(centerLon, centerLat);
        rotator.transform(data, LON, LAT);

        final int[] xIndices = new int[]{LAT, LON, X};
        final int[] yIndices = new int[]{LAT, LON, Y};

        final RationalFunctionModel fX = findBestModel(data, xIndices, accuracy);
        final RationalFunctionModel fY = findBestModel(data, yIndices, accuracy);
        if (fX == null || fY == null) {
            return null;
        }

        final int[] lonIndices = new int[]{X, Y, LON};
        final int[] latIndices = new int[]{X, Y, LAT};
        final RationalFunctionModel fLon = findBestModel(data, lonIndices, 0.01);
        final RationalFunctionModel fLat = findBestModel(data, latIndices, 0.01);

        return new GeoApproximation(fX, fY, fLon, fLat, maxDistance, rotator, new CosineDistance(centerLon, centerLat),
                                    range);
    }

    /**
     * Among several approximations, returns the approximation that is most suitable for a given (lat, lon) point.
     *
     * @param approximations The approximations.
     * @param lat            The latitude.
     * @param lon            The longitude.
     *
     * @return the approximation that is most suitable for the given (lat, lon) point,
     * or {@code null}, if none is suitable.
     */
    static GeoApproximation findMostSuitable(GeoApproximation[] approximations, double lat, double lon) {
        GeoApproximation bestApproximation = null;
        if (approximations.length == 1) {
            GeoApproximation a = approximations[0];
            final double distance = a.getDistance(lat, lon);
            if (distance < a.getMaxDistance()) {
                bestApproximation = a;
            }
        } else {
            double minDistance = Double.MAX_VALUE;
            for (final GeoApproximation a : approximations) {
                final double distance = a.getDistance(lat, lon);
                if (distance < minDistance && distance < a.getMaxDistance()) {
                    minDistance = distance;
                    bestApproximation = a;
                }
            }
        }
        return bestApproximation;
    }

    /**
     * Among several approximations, returns the approximation that is suitable for a given pixel.
     *
     * @param approximations The approximations.
     * @param p              The pixel position.
     *
     * @return the approximation that is suitable for the given pixel,
     * or {@code null}, if none is suitable.
     */
    static GeoApproximation findSuitable(GeoApproximation[] approximations, PixelPos p) {
        for (final GeoApproximation a : approximations) {
            if (a.getRange().contains(p)) {
                return a;
            }
        }
        return null;
    }

    static GeoApproximation[] createApproximations(SampleSource lonSamples,
                                                   SampleSource latSamples,
                                                   SampleSource maskSamples,
                                                   double accuracy,
                                                   Rectangle[] rectangles,
                                                   SteppingFactory steppingFactory) {
        final ArrayList<GeoApproximation> approximations = new ArrayList<>(rectangles.length);
        for (final Rectangle rectangle : rectangles) {
            final GeoApproximation approximation = create(lonSamples, latSamples, maskSamples, accuracy,
                                                          rectangle, steppingFactory);
            if (approximation == null) {
                return null;
            }
            approximations.add(approximation);
        }

        return approximations.toArray(new GeoApproximation[approximations.size()]);
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
     * @return the {@code Rotator} associated with this approximation.
     */
    public Rotator getRotator() {
        return rotator;
    }

    /**
     * Returns the range of the x(lat, lon) and y(lat, lon) functions.
     *
     * @return the range of the x(lat, lon) and y(lat, lon) functions.
     */
    public Rectangle getRange() {
        return range;
    }

    /**
     * This method yields the pixel position corresponding to a geographic position.
     *
     * @param g The geographic position on input, the pixel position on output.
     */
    void g2p(final Point2D g) {
        rotator.transform(g);
        final double lon = g.getX();
        final double lat = g.getY();
        final double x = fX.getValue(lat, lon);
        final double y = fY.getValue(lat, lon);
        g.setLocation(x, y);
    }

    /**
     * This method yields the geographic position corresponding to a pixel position.
     *
     * @param p The pixel position on input, the geographic position on output.
     */
    void p2g(final Point2D p) {
        final double x = p.getX();
        final double y = p.getY();
        final double lon = fLon.getValue(x, y);
        final double lat = fLat.getValue(x, y);
        p.setLocation(lon, lat);
        rotator.transformInversely(p);
    }

    GeoApproximation(RationalFunctionModel fX,
                     RationalFunctionModel fY,
                     RationalFunctionModel fLon,
                     RationalFunctionModel fLat,
                     double maxDistance,
                     Rotator rotator, DistanceMeasure calculator, Rectangle range) {
        this.fX = fX;
        this.fY = fY;
        this.fLon = fLon;
        this.fLat = fLat;
        this.maxDistance = maxDistance;
        this.rotator = rotator;
        this.calculator = calculator;
        this.range = range;
    }

    private static double maxDistance(final double[][] data, double centerLon, double centerLat) {
        final DistanceMeasure distanceMeasure = new CosineDistance(centerLon, centerLat);
        double maxDistance = 0.0;
        for (final double[] p : data) {
            final double d = distanceMeasure.distance(p[LON], p[LAT]);
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

    static double[][] extractWarpPoints(SampleSource lonSamples,
                                        SampleSource latSamples,
                                        SampleSource maskSamples,
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
        final List<double[]> pointList = new ArrayList<>(pointCount);

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
                final int mask = maskSamples.getSample(x, y);
                if (mask != 0) {
                    final double lat = latSamples.getSampleDouble(x, y);
                    final double lon = lonSamples.getSampleDouble(x, y);
                    if (!Double.isNaN(lon) && lat >= -90.0 && lat <= 90.0) {
                        final double[] point = new double[4];
                        point[LAT] = lat;
                        point[LON] = normalizeLon(lon);
                        point[X] = x + 0.5;
                        point[Y] = y + 0.5;
                        pointList.add(point);
                    }
                }
            }
        }

        return pointList.toArray(new double[pointList.size()][4]);
    }

    static double normalizeLon(double lon) {
        if (lon < -360.0 || lon > 360.0) {
            lon %= 360.0;
        }
        if (lon < -180.0) {
            lon += 360.0;
        } else if (lon > 180.0) {
            lon -= 360.0;
        }
        return lon;
    }

}
