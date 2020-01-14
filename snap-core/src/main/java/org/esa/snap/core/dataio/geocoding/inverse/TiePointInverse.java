package org.esa.snap.core.dataio.geocoding.inverse;

import org.esa.snap.core.dataio.geocoding.GeoRaster;
import org.esa.snap.core.dataio.geocoding.InverseCoding;
import org.esa.snap.core.dataio.geocoding.util.Approximation;
import org.esa.snap.core.dataio.geocoding.util.RasterUtils;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.math.FXYSum;
import org.esa.snap.core.util.math.MathUtils;

import java.awt.*;

public class TiePointInverse implements InverseCoding {

    private static final int MAX_NUM_POINTS_PER_TILE = 1000;
    private static final double GEOLOCATION_MARGIN = 1e-5; // degree margin to compensate for float/double conversion (approx 1m at the equator)

    private TiePointGrid lonGrid;
    private TiePointGrid latGrid;
    private Approximation[] approximations;
    private Boundaries boundaries;
    private int rasterWidth;
    private int rasterHeight;

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        pixelPos.setInvalid();
        if (!geoPos.isValid()) {
            return pixelPos;
        }

        double lat = normalizeLat(geoPos.lat);
        double lon = normalizeLon(geoPos.lon, boundaries.normalizedLonMin, boundaries.normalizedLonMax);

        Approximation approximation = getBestApproximation(approximations, lat, lon);
        // retry with pixel in overlap range, re-normalise
        // solves the problem with overlapping normalized and unnormalized orbit areas (AATSR)
        if (lon >= boundaries.overlapStart && lon <= boundaries.overlapEnd) {
            final double squareDistance;
            if (approximation != null) {
                squareDistance = approximation.getSquareDistance(lat, lon);
            } else {
                squareDistance = Double.MAX_VALUE;
            }
            double tempLon = lon + 360;
            final Approximation renormalizedApproximation = findRenormalizedApproximation(approximations, lat, tempLon, squareDistance);
            if (renormalizedApproximation != null) {
                approximation = renormalizedApproximation;
                lon = tempLon;
            }
        }
        if (approximation == null) {
            return pixelPos;
        }

        lat = rescaleLatitude(lat);
        lon = rescaleLongitude(lon, approximation.getCenterLon());
        pixelPos.x = approximation.getFX().computeZ(lat, lon);
        pixelPos.y = approximation.getFY().computeZ(lat, lon);

        if (pixelPos.x < 0 || pixelPos.x > rasterWidth || pixelPos.y < 0 || pixelPos.y > rasterHeight) {
            pixelPos.setInvalid();
        }

        return pixelPos;
    }

    @Override
    public void initialize(GeoRaster geoRaster, boolean containsAntiMeridian, PixelPos[] poleLocations) {
        lonGrid = new TiePointGrid("lon", geoRaster.getRasterWidth(), geoRaster.getRasterHeight(),
                geoRaster.getOffsetX(), geoRaster.getOffsetY(),
                geoRaster.getSubsamplingX(), geoRaster.getSubsamplingY(),
                RasterUtils.toFloat(geoRaster.getLongitudes()));

        rasterWidth = geoRaster.getSceneWidth();
        rasterHeight = geoRaster.getSceneHeight();

        latGrid = new TiePointGrid("lat", geoRaster.getRasterWidth(), geoRaster.getRasterHeight(),
                geoRaster.getOffsetX(), geoRaster.getOffsetY(),
                geoRaster.getSubsamplingX(), geoRaster.getSubsamplingY(),
                RasterUtils.toFloat(geoRaster.getLatitudes()));

        if (containsAntiMeridian) {
            lonGrid = normalizeLonGrid(lonGrid);
        }

        boundaries = initLatLonMinMax(lonGrid.getTiePoints(), latGrid.getTiePoints());
        approximations = getApproximations(lonGrid, latGrid);
    }

    @Override
    public void dispose() {
        if (latGrid != null) {
            latGrid.dispose();
            latGrid = null;
        }

        if (lonGrid != null) {
            lonGrid.dispose();
            lonGrid = null;
        }
    }

    // package access for testing only tb 2019-12-12
    static Approximation getBestApproximation(Approximation[] approximations, double lat, double lon) {
        Approximation approximation = null;

        double minSquareDistance = Double.MAX_VALUE;
        for (final Approximation approx : approximations) {
            final double squareDistance = approx.getSquareDistance(lat, lon);
            if (squareDistance < minSquareDistance && squareDistance < approx.getMinSquareDistance()) {
                minSquareDistance = squareDistance;
                approximation = approx;
            }
        }

        return approximation;
    }

    // package access for testing only tb 2019-12-13
    static Approximation findRenormalizedApproximation(Approximation[] approximations, final double lat,
                                                       final double renormalizedLon, final double distance) {
        Approximation renormalizedApproximation = getBestApproximation(approximations, lat, renormalizedLon);
        if (renormalizedApproximation != null) {
            double renormalizedDistance = renormalizedApproximation.getSquareDistance(lat, renormalizedLon);
            if (renormalizedDistance < distance) {
                return renormalizedApproximation;
            }
        }
        return null;
    }

    /**
     * Gets the normalized latitude value.
     * The method returns <code>Double.NaN</code> if the given latitude value is out of bounds.
     *
     * @param lat the raw latitude value in the range -90 to +90 degrees
     * @return the normalized latitude value, <code>Double.NaN</code> else
     */
    static double normalizeLat(double lat) {
        if (lat < -90 || lat > 90) {
            return Double.NaN;
        }
        return lat;
    }

    /**
     * Gets the normalized longitude value.
     * The method returns <code>Double.NaN</code> if the given longitude value is out of bounds
     * or if it's normalized value is not in the value range of this geo-coding's normalized longitude grid..
     *
     * @param lon the raw longitude value in the range -180 to +180 degrees
     * @return the normalized longitude value, <code>Double.NaN</code> else
     */
    static double normalizeLon(double lon, double normalizedLonMin, double normalizedLonMax) {
        if (lon < -180 || lon > 180) {
            return Double.NaN;
        }
        double normalizedLon = lon;
        if (normalizedLon < normalizedLonMin) {
            normalizedLon += 360;
        }
        if (normalizedLon < normalizedLonMin || normalizedLon > normalizedLonMax) {
            return Double.NaN;
        }
        return normalizedLon;
    }

    // package access for testing only tb 2019-12-11
    static Approximation[] getApproximations(TiePointGrid lonGrid, TiePointGrid latGrid) {
        final int numPoints = latGrid.getGridData().getNumElems();

        // 10 points are at least required for a quadratic polynomial
        // start with some appropriate tile number
        int numTiles = (int) Math.ceil(numPoints / 10.0);
        numTiles = Math.min(Math.max(1, numTiles), 300);

        final int width = latGrid.getGridWidth();
        final int height = latGrid.getGridHeight();
        final double subSamplingX = latGrid.getSubSamplingX();
        final double subSamplingY = latGrid.getSubSamplingY();

        int numTilesI = 1;
        int numTilesJ = 1;
        while (numTiles > 1) {
            final Dimension tileDim = MathUtils.fitDimension(numTiles, width * subSamplingX, height * subSamplingY);
            int newNumTilesI = tileDim.width;
            int newNumTilesJ = tileDim.height;
            int newNumTiles = newNumTilesI * newNumTilesJ;
            // 10 points are at least required for a quadratic polynomial
            if (numPoints / newNumTiles >= 10) {
                numTiles = newNumTiles;
                numTilesI = newNumTilesI;
                numTilesJ = newNumTilesJ;
                break;
            }
            numTiles--;
        }

        // Compute actual approximations for all tiles
        //
        final Approximation[] approximations = new Approximation[numTiles];
        final Rectangle[] rectangles = MathUtils.subdivideRectangle(width, height, numTilesI, numTilesJ, 1);
        for (int i = 0; i < rectangles.length; i++) {
            final Approximation approximation = createApproximation(lonGrid, latGrid, rectangles[i]);
            if (approximation == null) {
                return null;
            }
            approximations[i] = approximation;
        }

        return approximations;
    }

    // package access for testing only tb 2019-12-12
    static Approximation createApproximation(TiePointGrid normalizedLonGrid, TiePointGrid latGrid, Rectangle subsetRect) {
        final double[][] data = createWarpPoints(normalizedLonGrid, latGrid, subsetRect);

        double sumLat = 0.0;
        double sumLon = 0.0;
        for (final double[] point : data) {
            sumLat += point[0];
            sumLon += point[1];
        }
        double centerLon = sumLon / data.length;
        double centerLat = sumLat / data.length;

        final double maxSquareDistance = getMaxSquareDistance(data, centerLat, centerLon);

        for (int i = 0; i < data.length; i++) {
            data[i][0] = rescaleLatitude(data[i][0]);
            data[i][1] = rescaleLongitude(data[i][1], centerLon);
        }

        final int[] xIndices = new int[]{0, 1, 2};
        final int[] yIndices = new int[]{0, 1, 3};

        final FXYSum fX = getBestPolynomial(data, xIndices);
        final FXYSum fY = getBestPolynomial(data, yIndices);
        if (fX == null || fY == null) {
            return null;
        }

        return new Approximation(fX, fY, centerLat, centerLon, maxSquareDistance * 1.1);
    }

    // package access for testing only tb 2019-12-12
    static FXYSum getBestPolynomial(double[][] data, int[] indices) {
        // These are the potential polynomials which we will check
        final FXYSum[] potentialPolynomials = new FXYSum[]{
                new FXYSum.Linear(),
                new FXYSum.BiLinear(),
                new FXYSum.Quadric(),
                new FXYSum.BiQuadric(),
                new FXYSum.Cubic(),
                new FXYSum.BiCubic(),
                new FXYSum(FXYSum.FXY_4TH, 4),
                new FXYSum(FXYSum.FXY_BI_4TH, 4 + 4)
        };
        // Find the polynomial which best fits the warp points
        //
        double rmseMin = Double.MAX_VALUE;
        int index = -1;

        for (int i = 0; i < potentialPolynomials.length; i++) {
            final FXYSum potentialPolynomial = potentialPolynomials[i];
            final int order = potentialPolynomial.getOrder();
            final int numPointsRequired;
            if (order >= 0) {
                numPointsRequired = (order + 2) * (order + 1) / 2;
            } else {
                numPointsRequired = 2 * potentialPolynomial.getNumTerms();
            }

            if (data.length >= numPointsRequired) {
                try {
                    potentialPolynomial.approximate(data, indices);
                    double rmse = potentialPolynomial.getRootMeanSquareError();
                    if (rmse < rmseMin) {
                        index = i;
                        rmseMin = rmse;
                    }
                    // the old version contained a condition to use the lowest order polynomial with an maxError below half a pixel
                    // This led to quite large interpolation errors - therefore it is removed here. tb 2019-12-13
                } catch (ArithmeticException e) {
                    Debug.trace("Polynomial cannot be constructed due to a numerically singular or degenerate matrix:");
                    Debug.trace(e);
                }
            }
        }

        return index >= 0 ? potentialPolynomials[index] : null;
    }

    // package access for testing only tb 2019-12-12
    static double rescaleLatitude(double lat) {
        return lat / 90.0;
    }

    // package access for testing only tb 2019-12-12
    static double rescaleLongitude(double lon, double centerLon) {
        return (lon - centerLon) / 90.0;
    }

    // package access for testing only tb 2019-12-11
    static double getMaxSquareDistance(final double[][] data, double centerLat, double centerLon) {
        double maxSquareDistance = 0.0;
        for (final double[] point : data) {
            final double dLat = point[0] - centerLat;
            final double dLon = point[1] - centerLon;
            final double squareDistance = dLat * dLat + dLon * dLon;
            if (squareDistance > maxSquareDistance) {
                maxSquareDistance = squareDistance;
            }
        }
        return maxSquareDistance;
    }

    // package access for testing only tb 2019-12-11
    static double[][] createWarpPoints(TiePointGrid lonGrid, TiePointGrid latGrid, Rectangle subsetRect) {
        final int sw = subsetRect.width;
        final int sh = subsetRect.height;

        final int[] warpParameters = determineWarpParameters(sw, sh);
        int numU = warpParameters[0];
        int numV = warpParameters[1];
        int stepI = warpParameters[2];
        int stepJ = warpParameters[3];

        // Collect numU * numV warp points
        final int m = numU * numV;
        final double[][] data = new double[m][4];
        final int i1 = subsetRect.x;
        final int i2 = i1 + sw - 1;
        final int j1 = subsetRect.y;
        final int j2 = j1 + sh - 1;
        final int w = latGrid.getGridWidth();
        int k = 0;
        for (int v = 0; v < numV; v++) {
            int j = j1 + v * stepJ;
            // Adjust bottom border
            if (j > j2) {
                j = j2;
            }

            for (int u = 0; u < numU; u++) {
                int i = i1 + u * stepI;
                // Adjust right border
                if (i > i2) {
                    i = i2;
                }

                data[k][0] = latGrid.getGridData().getElemDoubleAt(j * w + i);
                data[k][1] = lonGrid.getGridData().getElemDoubleAt(j * w + i);
                data[k][2] = latGrid.getOffsetX() + i * latGrid.getSubSamplingX();
                data[k][3] = latGrid.getOffsetY() + j * latGrid.getSubSamplingY();

                k++;
            }
        }

        return data;
    }

    // package access for testing only tb 2019-12-11
    static int[] determineWarpParameters(int sw, int sh) {
        // Determine stepI and stepJ so that maximum number of warp points is not exceeded,
        // numU * numV shall be less than _MAX_NUM_POINTS_PER_TILE.
        //
        int numU = sw;
        int numV = sh;
        int stepI = 1;
        int stepJ = 1;

        // Adjust number of hor/ver (numU,numV) tie-points to be considered
        // so that a maximum of circa numPointsMax points is not exceeded
        boolean adjustStepI = numU >= numV;
        while (numU * numV > MAX_NUM_POINTS_PER_TILE) {
            if (adjustStepI) {
                stepI++;
                numU = sw / stepI;
                while (numU * stepI < sw) {
                    numU++;
                }
            } else {
                stepJ++;
                numV = sh / stepJ;
                while (numV * stepJ < sh) {
                    numV++;
                }
            }
            adjustStepI = numU >= numV;
        }
        return new int[]{numU, numV, stepI, stepJ};
    }

    // package access for testing only tb 2019-12-11
    static Boundaries initLatLonMinMax(float[] lonPoints, float[] latPoints) {
        final Boundaries boundaries = new Boundaries();

        boundaries.normalizedLonMin = +Double.MAX_VALUE;
        boundaries.normalizedLonMax = -Double.MAX_VALUE;
        for (double lonPoint : lonPoints) {
            boundaries.normalizedLonMin = Math.min(boundaries.normalizedLonMin, lonPoint);
            boundaries.normalizedLonMax = Math.max(boundaries.normalizedLonMax, lonPoint);
        }

        // add a little margin of ~10cm to compensate for rounding issues tb 2019-12-12
        boundaries.normalizedLonMin -= GEOLOCATION_MARGIN;
        boundaries.normalizedLonMax += GEOLOCATION_MARGIN;

        boundaries.latMin = +Double.MAX_VALUE;
        boundaries.latMax = -Double.MAX_VALUE;
        for (double latPoint : latPoints) {
            boundaries.latMin = Math.min(boundaries.latMin, latPoint);
            boundaries.latMax = Math.max(boundaries.latMax, latPoint);
        }

        boundaries.overlapStart = boundaries.normalizedLonMin;
        if (boundaries.overlapStart < -180) {
            boundaries.overlapStart += 360;
        }
        boundaries.overlapEnd = boundaries.normalizedLonMax;
        if (boundaries.overlapEnd > 180) {
            boundaries.overlapEnd -= 360;
        }

        return boundaries;
    }

    // package access for testing only tb 2019-12-11
    static TiePointGrid normalizeLonGrid(TiePointGrid lonGrid) {
        final int width = lonGrid.getGridWidth();
        final int height = lonGrid.getGridHeight();

        final float[] longitudes = lonGrid.getTiePoints();
        final int numValues = longitudes.length;
        final float[] normalizedLongitudes = new float[numValues];
        System.arraycopy(longitudes, 0, normalizedLongitudes, 0, numValues);

        boolean westNormalized = false;
        boolean eastNormalized = false;

        for (int y = 0; y < height; y++) {
            final int lineOffset = y * width;
            for (int x = 0; x < width; x++) {
                // Normalise line-wise, by detecting longituindal discontinuities. lonDelta is the difference between a base point and the current point
                final int index = x + lineOffset;
                final double p1;

                if (x == 0 && y == 0) {
                    // first point in grid: base point is un-normalised
                    p1 = normalizedLongitudes[index];
                } else if (x == 0) {
                    // first point in line: base point is the (possibly) normalised lon. of first point of last line
                    p1 = normalizedLongitudes[x + (y - 1) * width];
                } else {
                    // other points in line: base point is the (possibly) normalised lon. of last point in line
                    p1 = normalizedLongitudes[index - 1];
                }

                double p2 = normalizedLongitudes[index]; // the current, un-normalised point
                final double lonDelta = p2 - p1;  // difference = current point minus base point

                if (lonDelta > 180.0) {
                    p2 -= 360.0;  // place new point in the west (with a lon. < -180)
                    westNormalized = true; // mark what we've done
                    normalizedLongitudes[index] = (float) p2;
                } else if (lonDelta < -180.0) {
                    p2 += 360.0;  // place new point in the east (with a lon. > +180)
                    eastNormalized = true;  // mark what we've done
                    normalizedLongitudes[index] = (float) p2;
                }
            }
        }

        // West-normalisation can result in longitudes down to -540 degrees
        if (westNormalized) {
            // This ensures that the all longitude points are >= -180 degree
            for (int i = 0; i < numValues; i++) {
                normalizedLongitudes[i] += 360.f;
            }
        }

        if (eastNormalized || westNormalized) {
            return new TiePointGrid(lonGrid.getName(),
                    lonGrid.getGridWidth(),
                    lonGrid.getGridHeight(),
                    lonGrid.getOffsetX(),
                    lonGrid.getOffsetY(),
                    lonGrid.getSubSamplingX(),
                    lonGrid.getSubSamplingY(),
                    normalizedLongitudes);
        }

        return lonGrid;
    }

    static class Boundaries {
        double normalizedLonMin;
        double normalizedLonMax;
        double latMin;
        double latMax;
        double overlapStart;
        double overlapEnd;
    }
}
