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
package org.esa.beam.framework.datamodel;

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.Debug;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.FXYSum;
import org.esa.beam.util.math.MathUtils;

import java.awt.Dimension;
import java.awt.Rectangle;

/**
 * A geo-coding based on two tie-point grids. One grid stores the latitude tie-points, the other stores the longitude
 * tie-points.
 */
public class TiePointGeoCoding extends AbstractGeoCoding {

    private static final double _ABS_ERROR_LIMIT = 0.5; // pixels
    private static final int _MAX_NUM_POINTS_PER_TILE = 1000;

    private TiePointGrid _latGrid;
    private TiePointGrid _lonGrid;
    private Datum _datum;
    private final boolean _swathResampling = true;

    private boolean _normalized;
    private float _normalizedLonMin;
    private float _normalizedLonMax;
    private TiePointGrid _normalizedLonGrid; // TODO - remove instance!

    private Approximation[] _approximations;
    private float _latMin;
    private float _latMax;

    private float _overlapStart;
    private float _overlapEnd;

    /**
     * Constructs geo-coding based on two given tie-point grids providing coordinates on the WGS-84 datum.
     *
     * @param latGrid the latitude grid
     * @param lonGrid the longitude grid
     */
    public TiePointGeoCoding(TiePointGrid latGrid, TiePointGrid lonGrid) {
        this(latGrid, lonGrid, Datum.WGS_84);
    }

    /**
     * Constructs geo-coding based on two given tie-point grids.
     *
     * @param latGrid the latitude grid
     * @param lonGrid the longitude grid
     * @param datum   the geodetic datum
     */
    public TiePointGeoCoding(TiePointGrid latGrid, TiePointGrid lonGrid, Datum datum) {
        Guardian.assertNotNull("latGrid", latGrid);
        Guardian.assertNotNull("lonGrid", lonGrid);
        Guardian.assertNotNull("datum", datum);
        if (latGrid.getRasterWidth() != lonGrid.getRasterWidth() ||
            latGrid.getRasterHeight() != lonGrid.getRasterHeight() ||
            latGrid.getOffsetX() != lonGrid.getOffsetX() ||
            latGrid.getOffsetY() != lonGrid.getOffsetY() ||
            latGrid.getSubSamplingX() != lonGrid.getSubSamplingX() ||
            latGrid.getSubSamplingY() != lonGrid.getSubSamplingY()) {
            throw new IllegalArgumentException("latGrid is not compatible with lonGrid");
        }
        _latGrid = latGrid;
        _lonGrid = lonGrid;
        _datum = datum;
        initNormalizedLonGrid();
        initLatLonMinMax();
        // detection disabled, mz,mp 18.03.2008
        // test show big improvements for AVHRR and small ones for MERIS
//        _swathResampling = detectSwathResampling();
        initApproximations();
    }

    /**
     * Gets the datum, the reference point or surface against which {@link GeoPos} measurements are made.
     *
     * @return the datum
     */
    @Override
    public Datum getDatum() {
        return _datum;
    }

    /**
     * Gets the flag indicating that the geographic boundary of the tie-points in this geo-coding
     * intersects the 180 degree meridian.
     *
     * @return true if so
     */
    @Override
    public boolean isCrossingMeridianAt180() {
        return _normalized;
    }

    /**
     * Gets the number of approximations used for the transformation map (lat,lon) --> image (x,y).
     *
     * @return the number of approximations, zero if no approximations could be computed
     */
    public int getNumApproximations() {
        return _approximations != null ? _approximations.length : 0;
    }

    /**
     * Gets the approximations for the given index.
     *
     * @param index the index, must be between 0 and {@link #getNumApproximations()} - 1
     *
     * @return the approximation, never null
     */
    public Approximation getApproximation(int index) {
        return _approximations[index];
    }

    /**
     * Checks whether this geo-coding can determine the geodetic position from a pixel position.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean canGetGeoPos() {
        return true;
    }

    /**
     * Checks whether this geo-coding can determine the pixel position from a geodetic position.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean canGetPixelPos() {
        return _approximations != null;
    }

    /**
     * Returns the latitude grid, never <code>null</code>.
     */
    public TiePointGrid getLatGrid() {
        return _latGrid;
    }

    /**
     * Returns the longitude grid, never <code>null</code>.
     */
    public TiePointGrid getLonGrid() {
        return _lonGrid;
    }

    /**
     * Returns the latitude and longitude value for a given pixel co-ordinate.
     *
     * @param pixelPos the pixel's co-ordinates given as x,y
     * @param geoPos   an instance of <code>GeoPos</code> to be used as retun value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     *
     * @return the geographical position as lat/lon.
     */
    @Override
    public GeoPos getGeoPos(final PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        if (pixelPos.x < 0 || pixelPos.x > _latGrid.getSceneRasterWidth() ||
            pixelPos.y < 0 || pixelPos.y > _latGrid.getSceneRasterHeight()) {
            geoPos.setInvalid();
        } else {
            geoPos.lat = _latGrid.getPixelFloat(pixelPos.x, pixelPos.y);
            geoPos.lon = _lonGrid.getPixelFloat(pixelPos.x, pixelPos.y);
        }
        return geoPos;
    }

    /**
     * Returns the pixel co-ordinates as x/y for a given geographical position given as lat/lon.
     *
     * @param geoPos   the geographical position as lat/lon.
     * @param pixelPos an instance of <code>Point</code> to be used as retun value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     *
     * @return the pixel co-ordinates as x/y
     */
    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        Approximation[] approximations = _approximations;
        if (approximations != null) {
            float lat = normalizeLat(geoPos.lat);
            float lon = normalizeLon(geoPos.lon);
            // ensure that pixel is out of image (= no source position)
            if (pixelPos == null) {
                pixelPos = new PixelPos();
            }
            pixelPos.setInvalid();

            if (!Float.isNaN(lat) && !Float.isNaN(lon)) {
                Approximation approximation = getBestApproximation(approximations, lat, lon);
                if (approximation == null) {
                    // retry with pixel in overlap range, re-normalise
                    // solves the problem with overlapping normalized and unnormalized orbit areas (AATSR)
                    if (lon >= _overlapStart && lon <= _overlapEnd) {
                        lon += 360;
                        approximation = getBestApproximation(approximations, lat, lon);
                    }
                }
                if (approximation != null) {
                    if (_swathResampling) {
                        lat = (float) rescaleLatitude(lat);
                        final float centerLon = approximation.getCenterLon();
                        lon = (float) rescaleLongitude(lon, centerLon);
                    }
                    pixelPos.x = (float) approximation.getFX().computeZ(lat, lon);
                    pixelPos.y = (float) approximation.getFY().computeZ(lat, lon);
                }
            }
        }
        return pixelPos;
    }

    private double rescaleLongitude(double lon, double centerLon) {
        return (lon - centerLon) / 90.0;
    }

    private double rescaleLatitude(double lat) {
        return lat / 90.0;
    }

    /**
     * Gets the normalized latitude value.
     * The method returns <code>Float.NaN</code> if the given latitude value is out of bounds.
     *
     * @param lat the raw latitude value in the range -90 to +90 degrees
     *
     * @return the normalized latitude value, <code>Float.NaN</code> else
     */
    public static float normalizeLat(float lat) {
        if (lat < -90 || lat > 90) {
            return Float.NaN;
        }
        return lat;
    }

    /**
     * Gets the normalized longitude value.
     * The method returns <code>Float.NaN</code> if the given longitude value is out of bounds
     * or if it's normalized value is not in the value range of this geo-coding's normalized longitude grid..
     *
     * @param lon the raw longitude value in the range -180 to +180 degrees
     *
     * @return the normalized longitude value, <code>Float.NaN</code> else
     */
    public final float normalizeLon(float lon) {
        if (lon < -180 || lon > 180) {
            return Float.NaN;
        }
        float normalizedLon = lon;
        if (normalizedLon < _normalizedLonMin) {
            normalizedLon += 360;
        }
        if (normalizedLon < _normalizedLonMin || normalizedLon > _normalizedLonMax) {
            return Float.NaN;
        }
        return normalizedLon;
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     */
    @Override
    public void dispose() {
        if (_normalizedLonGrid != _lonGrid) {
            _normalizedLonGrid.dispose();
            _normalizedLonGrid = null;
        }
        _latGrid = null;
        _lonGrid = null;
        _approximations = null;
    }

    /////////////////////////////////////////////////////////////////////////
    // Private stuff

    private void initNormalizedLonGrid() {
        final int w = _lonGrid.getRasterWidth();
        final int h = _lonGrid.getRasterHeight();

        float p1;
        float p2;
        float lonDelta;
        boolean westNormalized = false;
        boolean eastNormalized = false;

        final float[] longitudes = _lonGrid.getTiePoints();
        final int numValues = longitudes.length;
        final float[] normalizedLongitudes = new float[numValues];
        System.arraycopy(longitudes, 0, normalizedLongitudes, 0, numValues);
        float lonDeltaMax = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) { // Normalise line-wise, by detecting longituidal discontinuities. lonDelta is the difference between a base point and the current point
                final int index = x + y * w;
                if (x == 0 && y == 0) { // first point in grid: base point is un-normalised
                    p1 = normalizedLongitudes[index];
                } else if (x == 0) { // first point in line: base point is the (possibly) normalised lon. of first point of last line
                    p1 = normalizedLongitudes[x + (y - 1) * w];
                } else { // other points in line: base point is the (possibly) normalised lon. of last point in line
                    p1 = normalizedLongitudes[index - 1];
                }
                p2 = normalizedLongitudes[index]; // the current, un-normalised point
                lonDelta = p2 - p1;  // difference = current point minus base point

                if (lonDelta > 180.0f) {
                    p2 -= 360.0f;  // place new point in the west (with a lon. < -180)
                    westNormalized = true; // mark what we've done
                    normalizedLongitudes[index] = p2;
                } else if (lonDelta < -180.0f) {
                    p2 += 360.0f;  // place new point in the east (with a lon. > +180)
                    eastNormalized = true;  // mark what we've done
                    normalizedLongitudes[index] = p2;
                } else {
                    lonDeltaMax = Math.max(lonDeltaMax, Math.abs(lonDelta));
                }
            }
        }

        // West-normalisation can result in longitudes down to -540 degrees
        if (westNormalized) {
            // This ensures that the all longitude points are >= -180 degree
            for (int i = 0; i < numValues; i++) {
                normalizedLongitudes[i] += 360;
            }
        }

        _normalized = westNormalized || eastNormalized;
        if (_normalized) {
            _normalizedLonGrid = new TiePointGrid(_lonGrid.getName(),
                                                  _lonGrid.getRasterWidth(),
                                                  _lonGrid.getRasterHeight(),
                                                  _lonGrid.getOffsetX(),
                                                  _lonGrid.getOffsetY(),
                                                  _lonGrid.getSubSamplingX(),
                                                  _lonGrid.getSubSamplingY(),
                                                  normalizedLongitudes,
                                                  _lonGrid.getDiscontinuity());
        } else {
            _normalizedLonGrid = _lonGrid;
        }

        Debug.trace("TiePointGeoCoding.westNormalized = " + westNormalized);
        Debug.trace("TiePointGeoCoding.eastNormalized = " + eastNormalized);
        Debug.trace("TiePointGeoCoding.normalized = " + _normalized);
        Debug.trace("TiePointGeoCoding.lonDeltaMax = " + lonDeltaMax);
    }

    private void initLatLonMinMax() {
        final float[] latPoints = getLatGrid().getTiePoints();
        final float[] lonPoints = getNormalizedLonGrid().getTiePoints();
        _normalizedLonMin = +Float.MAX_VALUE;
        _normalizedLonMax = -Float.MAX_VALUE;
        _latMin = +Float.MAX_VALUE;
        _latMax = -Float.MAX_VALUE;
        for (int i = 0; i < lonPoints.length; i++) {
            _normalizedLonMin = Math.min(_normalizedLonMin, lonPoints[i]);
            _normalizedLonMax = Math.max(_normalizedLonMax, lonPoints[i]);
            _latMin = Math.min(_latMin, latPoints[i]);
            _latMax = Math.max(_latMax, latPoints[i]);
        }

        _overlapStart = _normalizedLonMin;
        if (_overlapStart < -180) {
            _overlapStart += 360;
        }
        _overlapEnd = _normalizedLonMax;
        if (_overlapEnd > 180) {
            _overlapEnd -= 360;
        }

        Debug.trace("TiePointGeoCoding.normalizedLonMin = " + _normalizedLonMin);
        Debug.trace("TiePointGeoCoding.normalizedLonMax = " + _normalizedLonMax);
        Debug.trace("TiePointGeoCoding.latMin = " + _latMin);
        Debug.trace("TiePointGeoCoding.latMax = " + _latMax);
        Debug.trace("TiePointGeoCoding.overlapRange = " + _overlapStart + " - " + _overlapEnd);

    }

    private void initApproximations() {
        final int numPoints = _latGrid.getRasterData().getNumElems();
        final int w = _latGrid.getRasterWidth();
        final int h = _latGrid.getRasterHeight();

        // Compute number of required approximation tiles
        //
        int numTiles; // 10 degree sizing
        if (h > 2) {
            final float lonSpan = _normalizedLonMax - _normalizedLonMin;
            final float latSpan = _latMax - _latMin;
            final float angleSpan = Math.max(lonSpan, latSpan);
            numTiles = Math.round(angleSpan / 10.0f);
            if (numTiles < 1) {
                numTiles = 1;
            }
        } else {
            numTiles = 30;
        }
        while (numTiles > 1) {
            // 10 points are at least required for a quadric polynomial
            if (numPoints / numTiles >= 10) {
                break;
            }
            numTiles--;
        }
        // tb 210406    AATSR has a angle span over 360 degrees
//        if (numTiles > 36) { // max. 36 x 10 degree = 360 degree
//            numTiles = 36;
//        }
        final Dimension tileDim = MathUtils.fitDimension(numTiles, w, h);
        int numTilesI = tileDim.width;
        int numTilesJ = tileDim.height;
        numTiles = numTilesI * numTilesJ;

        Debug.trace("TiePointGeoCoding.numTiles =  " + numTiles);
        Debug.trace("TiePointGeoCoding.numTilesI = " + numTilesI);
        Debug.trace("TiePointGeoCoding.numTilesJ = " + numTilesJ);

        // Compute actual approximations for all tiles
        //
        _approximations = new Approximation[numTiles];
        final Rectangle[] rectangles = MathUtils.subdivideRectangle(w, h, numTilesI, numTilesJ, 1);
        for (int i = 0; i < rectangles.length; i++) {
            final Approximation approximation = createApproximation(rectangles[i]);
            if (approximation == null) {
                _approximations = null;
                return;
            }
            _approximations[i] = approximation;
        }
    }

    private static FXYSum getBestPolynomial(double[][] data, int[] indices) {
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

        // Find the polynomial which best fitts the warp points
        //
        double rmseMin = Double.MAX_VALUE;
        int index = -1;
        for (int i = 0; i < potentialPolynomials.length; i++) {
            FXYSum potentialPolynomial = potentialPolynomials[i];
            final int order = potentialPolynomial.getOrder();
            final int numPointsRequired;
            if (order >= 0) {
                numPointsRequired = (order + 2) * (order + 1) / 2;
            } else {
                numPointsRequired = 2 * potentialPolynomial.getNumTerms();
            }
            if (data.length >= numPointsRequired) {
                // @todo 1 tb/nf - replace try/catch by appropriate tie-point testing routine which
                // @todo 1 tb/nf - is used before potentialPolynomial.approximate() is executed
                try {
                    potentialPolynomial.approximate(data, indices);
                    double rmse = potentialPolynomial.getRootMeanSquareError();
                    double maxError = potentialPolynomial.getMaxError();
                    // Debug.trace("Checked polynomial " + (i + 1) + ": RMSE = " + rmse + ", max error = " + maxError);
                    if (rmse < rmseMin) {
                        index = i;
                        rmseMin = rmse;
                    }
                    if (maxError < _ABS_ERROR_LIMIT) { // this accuracy is sufficient
                        index = i;
                        break;
                    }
                } catch (RuntimeException e) {
                    Debug.trace("RuntimeException catched during polynomial approximation!");
                    Debug.trace("Yes, we know that it is not ok to catch RuntimeExceptions,");
                    Debug.trace("but the problem is probably caused by a singular matrix:");
                    Debug.trace(e);
                }
            }
        }
        return index >= 0 ? potentialPolynomials[index] : null;
    }


    private double[][] createWarpPoints(Rectangle subsetRect) {
        final TiePointGrid latGrid = getLatGrid();
        final TiePointGrid lonGrid = getNormalizedLonGrid();
        final int w = latGrid.getRasterWidth();
        final int sw = subsetRect.width;
        final int sh = subsetRect.height;
        final int i1 = subsetRect.x;
        final int i2 = i1 + sw - 1;
        final int j1 = subsetRect.y;
        final int j2 = j1 + sh - 1;

        Debug.trace("Selecting warp points for X/Y approximations");
        Debug.trace("  subset rectangle (in tie point coordinates): " + subsetRect);
        Debug.trace("  index i: " + i1 + " to " + i2);
        Debug.trace("  index j: " + j1 + " to " + j2);

        // Determine stepI and stepJ so that maximum number of warp points is not exceeded,
        // numU * numV shall be less than _MAX_NUM_POINTS_PER_TILE.
        //
        int numU = sw;
        int numV = sh;
        int stepI = 1;
        int stepJ = 1;

        // Adjust number of hor/ver (numU,numV) tie-points to be considered
        // so that a maximum of circa numPointsMax points is not exceeded
        boolean adjustStepI = true;
        while (numU * numV > _MAX_NUM_POINTS_PER_TILE) {
            if (adjustStepI) {
                stepI++;
                numU = sw / stepI;
            } else {
                stepJ++;
                numV = sh / stepJ;
            }
            adjustStepI = !adjustStepI;
        }
        numU = Math.max(1, numU);
        numV = Math.max(1, numV);

        // Make sure we include the right border tie-points
        // if sw/stepI not divisible without remainder
        if (sw % stepI != 0) {
            numU++;
        }
        // Make sure we include the bottom border tie-points
        // if sh/stepJ not divisible without remainder
        if (sh % stepJ != 0) {
            numV++;
        }

        // Collect numU * numV warp points
        //
        final int m = numU * numV;
        final double[][] data = new double[m][4];
        float lat, lon, x, y;
        int i, j, k = 0;
        for (int v = 0; v < numV; v++) {
            j = j1 + v * stepJ;
            // Adjust bottom border
            if (j > j2) {
                j = j2;
            }
            for (int u = 0; u < numU; u++) {
                i = i1 + u * stepI;
                // Adjust right border
                if (i > i2) {
                    i = i2;
                }
                lat = latGrid.getRasterData().getElemFloatAt(j * w + i);
                lon = lonGrid.getRasterData().getElemFloatAt(j * w + i);
                x = latGrid.getOffsetX() + i * latGrid.getSubSamplingX();
                y = latGrid.getOffsetY() + j * latGrid.getSubSamplingY();
                data[k][0] = lat;
                data[k][1] = lon;
                data[k][2] = x;
                data[k][3] = y;
                k++;
            }
        }

        Debug.assertTrue(k == m);
        Debug.trace("TiePointGeoCoding: numU=" + numU + ", stepI=" + stepI);
        Debug.trace("TiePointGeoCoding: numV=" + numV + ", stepJ=" + stepJ);

        return data;
    }

    private Approximation createApproximation(Rectangle subsetRect) {
        final double[][] data = createWarpPoints(subsetRect);

        float sumLat = 0.0f;
        float sumLon = 0.0f;
        for (final double[] point : data) {
            sumLat += point[0];
            sumLon += point[1];
        }
        float centerLon = sumLon / data.length;
        float centerLat = sumLat / data.length;
        final float maxSquareDistance = getMaxSquareDistance(data, centerLat, centerLon);

        if (_swathResampling) {
            for (int i = 0; i < data.length; i++) {
                data[i][0] = rescaleLatitude(data[i][0]);
                data[i][1] = rescaleLongitude(data[i][1], centerLon);
            }
        }

        final int[] xIndices = new int[]{0, 1, 2};
        final int[] yIndices = new int[]{0, 1, 3};

        final FXYSum fX = getBestPolynomial(data, xIndices);
        final FXYSum fY = getBestPolynomial(data, yIndices);
        if (fX == null || fY == null) {
            return null;
        }

        final double rmseX = fX.getRootMeanSquareError();
        final double rmseY = fY.getRootMeanSquareError();

        final double maxErrorX = fX.getMaxError();
        final double maxErrorY = fY.getMaxError();

        Debug.trace(
                "TiePointGeoCoding: RMSE X      = " + rmseX + ", " + (rmseX < _ABS_ERROR_LIMIT ? "OK" : "too large"));
        Debug.trace(
                "TiePointGeoCoding: RMSE Y      = " + rmseY + ", " + (rmseY < _ABS_ERROR_LIMIT ? "OK" : "too large"));
        Debug.trace(
                "TiePointGeoCoding: Max.error X = " + maxErrorX + ", " + (maxErrorX < _ABS_ERROR_LIMIT ? "OK" : "too large"));
        Debug.trace(
                "TiePointGeoCoding: Max.error Y = " + maxErrorY + ", " + (maxErrorY < _ABS_ERROR_LIMIT ? "OK" : "too large"));

        return new Approximation(subsetRect, fX, fY, centerLat, centerLon, maxSquareDistance * 1.1f);
    }

    private static float getMaxSquareDistance(final double[][] data, float centerLat, float centerLon) {
        float maxSquareDistance = 0.0f;
        for (final double[] point : data) {
            final float dLat = (float) point[0] - centerLat;
            final float dLon = (float) point[1] - centerLon;
            final float squareDistance = dLat * dLat + dLon * dLon;
            if (squareDistance > maxSquareDistance) {
                maxSquareDistance = squareDistance;
            }
        }
        return maxSquareDistance;
    }

    private static Approximation getBestApproximation(final Approximation[] approximations, float lat, float lon) {
        Approximation approximation = null;
        if (approximations.length == 1) {
            Approximation a = approximations[0];
            final float squareDistance = a.getSquareDistance(lat, lon);
            if (squareDistance < a.getMinSquareDistance()) {
                approximation = a;
            }
        } else {
            float minSquareDistance = Float.MAX_VALUE;
            for (final Approximation a : approximations) {
                final float squareDistance = a.getSquareDistance(lat, lon);
                if (squareDistance < minSquareDistance && squareDistance < a.getMinSquareDistance()) {
                    minSquareDistance = squareDistance;
                    approximation = a;
                }
            }
        }

        return approximation;
    }

    private TiePointGrid getNormalizedLonGrid() {
        return _normalizedLonGrid;
    }

    float getNormalizedLonMin() {
        return _normalizedLonMin;
    }

    /**
     * Transfers the geo-coding of the {@link Scene srcScene} to the {@link Scene destScene} with respect to the given
     * {@link org.esa.beam.framework.dataio.ProductSubsetDef subsetDef}.
     *
     * @param srcScene  the source scene
     * @param destScene the destination scene
     * @param subsetDef the definition of the subset, may be <code>null</code>
     *
     * @return true, if the geo-coding could be transferred.
     */
    @Override
    public boolean transferGeoCoding(final Scene srcScene, final Scene destScene, final ProductSubsetDef subsetDef) {
        final String latGridName = getLatGrid().getName();
        final String lonGridName = getLonGrid().getName();
        final Product destProduct = destScene.getProduct();
        TiePointGrid latGrid = destProduct.getTiePointGrid(latGridName);
        if (latGrid == null) {
            latGrid = TiePointGrid.createSubset(getLatGrid(), subsetDef);
            destProduct.addTiePointGrid(latGrid);
        }
        TiePointGrid lonGrid = destProduct.getTiePointGrid(lonGridName);
        if (lonGrid == null) {
            lonGrid = TiePointGrid.createSubset(getLonGrid(), subsetDef);
            destProduct.addTiePointGrid(lonGrid);
        }
        if (latGrid != null && lonGrid != null) {
            destScene.setGeoCoding(new TiePointGeoCoding(latGrid, lonGrid, getDatum()));
            return true;
        } else {
            return false;
        }
    }

    private boolean detectSwathResampling() {
        final int num = getNormalizedLonGrid().getRasterWidth();
        float centerLon = 0;
        for (int i = 0; i < num; i++) {
            final float lon = getNormalizedLonGrid().getTiePoints()[i];
            centerLon += lon;
        }
        centerLon /= num;
        float[] data = new float[num];
        float[] dataSin = new float[num];
        for (int i = 0; i < num; i++) {
            final float lon = getNormalizedLonGrid().getTiePoints()[i];
            data[i] = lon;
            dataSin[i] = (float) rescaleLongitude(lon, centerLon);
        }
        final float result = linearRegression(data);
        final float resultSin = linearRegression(dataSin);
        return (resultSin < result);
    }

    private float linearRegression(float[] data) {
        final int num = data.length;
        float sumX = 0;
        float sumY = 0;
        float sumXX = 0;
        float sumYY = 0;
        float sumXY = 0;
        for (int x = 0; x < num; x++) {
            float y = data[x];
            sumX += x;
            sumY += y;
            sumXX += x * x;
            sumYY += y * y;
            sumXY += x * y;
        }
        float sxx = sumXX - sumX * sumX / num;
        float sxy = sumXY - sumX * sumY / num;
        float b = sxy / sxx;
        float a = (sumY - b * sumX) / num;

        float squareError = 0;
        for (int x = 0; x < num; x++) {
            float y = data[x];
            float currentResidual = (y - (a + b * x));
            squareError += currentResidual * currentResidual;
        }
        squareError /= num;
        squareError = (float) Math.sqrt(squareError);

        return squareError;
    }

    /////////////////////////////////////////////////////////////////////////
    // Inner Classes

    public final static class Approximation {

        private final FXYSum _fX;
        private final FXYSum _fY;
        private final Rectangle _subsetRect;
        private final float _centerLat;
        private final float _centerLon;
        private final float _minSquareDistance;

        public Approximation(Rectangle subsetRect, FXYSum fX, FXYSum fY, float centerLat, float centerLon,
                             float minSquareDistance) {
            _subsetRect = subsetRect;
            _fX = fX;
            _fY = fY;
            _centerLat = centerLat;
            _centerLon = centerLon;
            _minSquareDistance = minSquareDistance;
        }

        public final FXYSum getFX() {
            return _fX;
        }

        public final FXYSum getFY() {
            return _fY;
        }

        public Rectangle getSubsetRect() {
            return _subsetRect;
        }

        public float getCenterLat() {
            return _centerLat;
        }

        public float getCenterLon() {
            return _centerLon;
        }

        public float getMinSquareDistance() {
            return _minSquareDistance;
        }

        /**
         * Computes the square distance to the given geographical coordinate.
         *
         * @param lat the latitude value
         * @param lon the longitude value
         *
         * @return the square distance
         */
        public final float getSquareDistance(float lat, float lon) {
            final float dx = lon - _centerLon;
            final float dy = lat - _centerLat;
            return dx * dx + dy * dy;
        }
    }
}
