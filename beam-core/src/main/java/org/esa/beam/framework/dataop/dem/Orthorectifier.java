/*
 * $Id: Orthorectifier.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (c) 2003 Brockmann Consult GmbH. All right reserved.
 * http://www.brockmann-consult.de
 */
package org.esa.beam.framework.dataop.dem;

import org.esa.beam.framework.datamodel.AngularDirection;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Pointing;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.math.RsMathUtils;

/**
 * An <code>Orthorectifier</code> is a {@link org.esa.beam.framework.datamodel.GeoCoding} which performs an orthorectification algorithm on a base {@link
 * GeoCoding}.
 * <p/>
 * <p><i>IMPORTANT NOTE: This class is not thread save. In order to use it safely, make sure to create a new instance of
 * this class for each orthorectifying thread.</i>
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @author Marco Peters
 * @version $Revision$ $Date$
 */
public class Orthorectifier implements GeoCoding {

    public static final float PIXEL_EPS = 0.1f;
    public static final float PIXEL_EPS_SQR = PIXEL_EPS * PIXEL_EPS;

    private final int _sceneRasterWidth;
    private final int _sceneRasterHeight;

    private final Pointing _pointing;
    private final GeoCoding _geoCoding;
    private final ElevationModel _elevationModel;
    private final int _maxIterationCount;

    protected final PixelPos _pp = new PixelPos();
    protected final PixelPos _pp2 = new PixelPos();
    protected final GeoPos _gp = new GeoPos();
    protected final AngularDirection _vg = new AngularDirection();

    /**
     * Constructs a new <code>Orthorectifier</code>.
     *
     * @param sceneRasterWidth  the scene raster width of the product which uses this orthorectifier as geo coding. Must
     *                          be greater than zero.
     * @param sceneRasterHeight the scene raster width of the product which uses this orthorectifier as geo coding. Must
     *                          be greater than zero.
     * @param pointing          the pointing, provides satellites viewing direction and base geo-coding. Must not be
     *                          <code>null</code>.
     * @param elevationModel    the provider for the elevation at a given lat/lon
     * @param maxIterationCount the maximum number of iterations, 10 is good choice. Must be greater than one.
     */
    public Orthorectifier(int sceneRasterWidth,
                          int sceneRasterHeight,
                          Pointing pointing,
                          ElevationModel elevationModel,
                          int maxIterationCount) {
        Guardian.assertGreaterThan("sceneRasterWidth", sceneRasterWidth, 0);
        Guardian.assertGreaterThan("sceneRasterHeight", sceneRasterHeight, 0);
        Guardian.assertNotNull("pointing", pointing);
        Guardian.assertNotNull("pointing.getGeoCoding()", pointing.getGeoCoding());
//        Guardian.assertNotNull("elevationModelel", elevationModelel);
        Guardian.assertGreaterThan("maxIterationCount", maxIterationCount, 1);
        _sceneRasterWidth = sceneRasterWidth;
        _sceneRasterHeight = sceneRasterHeight;
        _pointing = pointing;
        _geoCoding = pointing.getGeoCoding();
        _elevationModel = elevationModel;
        _maxIterationCount = maxIterationCount;
    }

    /**
     * Gets the datum, the reference point or surface against which {@link org.esa.beam.framework.datamodel.GeoPos} measurements are made.
     *
     * @return the datum
     */
    public Datum getDatum() {
        return _geoCoding.getDatum();
    }

    public Pointing getPointing() {
        return _pointing;
    }

    public GeoCoding getGeoCoding() {
        return _geoCoding;
    }

    public ElevationModel getElevationModel() {
        return _elevationModel;
    }

    public int getMaxIterationCount() {
        return _maxIterationCount;
    }

    /**
     * Returns the source pixel coordinate for a <i>true (corrected)</i> geographical coordinate.
     * <p/>
     * Implements the prediction/correction algorithm from the MERIS Geometry Handbook, VT-P194-DOC-001-E, iss 1, rev 4,
     * page 29, figure 23.
     * <p/>
     * Scope of the prediction/correction algorithm is to retrieve the pixel x,y
     * that matches the <i>true</i> lat,lon by the direct location model f(x,y) = lat,lon.
     *
     * @param geoPos   the <i>true (corrected)</i> geographical coordinate as lat/lon.
     * @param pixelPos an instance of <code>Point</code> to be used as return value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     *
     * @return the source pixel coordinate
     */
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        pixelPos = performReverseLocationModel(geoPos, pixelPos);
        if (!isPixelPosValid(pixelPos)) {
            return pixelPos;
        }
        if (!mustCorrect(geoPos, pixelPos)) {
            return pixelPos;
        }
        if (performPredictionCorrection(pixelPos)) {
            pixelPos.x = _pp.x;
            pixelPos.y = _pp.y;
        }
        return pixelPos;

    }

    /**
     * Gets the <i>true (corrected)</i> geographical coordinate for a given source pixel coordinate.
     *
     * @param pixelPos the pixel coordinate given as x,y
     * @param geoPos   an instance of <code>GeoPos</code> to be used as return value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     *
     * @return the <i>true (corrected)</i>  geographical coordinate as lat/lon.
     */
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        return performDirectLocationModel(pixelPos, 1f, geoPos);
    }


    /**
     * Checks whether or not this geo-coding can determine the geodetic position from a pixel position.
     *
     * @return <code>true</code>, if so
     */
    public boolean canGetGeoPos() {
        return getGeoCoding().canGetGeoPos();
    }

    /**
     * Checks whether or not this geo-coding can determine the pixel position from a geodetic position.
     *
     * @return <code>true</code>, if so
     */
    public boolean canGetPixelPos() {
        return getGeoCoding().canGetPixelPos();
    }

    /**
     * Checks whether or not the longitudes of this geo-coding cross the +/- 180 degree meridian.
     *
     * @return <code>true</code>, if so
     */
    public boolean isCrossingMeridianAt180() {
        return getGeoCoding().isCrossingMeridianAt180();
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     */
    public void dispose() {
    }


    private boolean performPredictionCorrection(final PixelPos pixelPos) {
        if (correctPrediction(pixelPos, 1.0)) {
            return true;
        }
        if (correctPrediction(pixelPos, 0.5)) {
            return true;
        }
        return correctPrediction(pixelPos, 2.0);
    }

    private boolean  correctPrediction(final PixelPos pixelPos, double factor) {
        _pp.x = pixelPos.x;
        _pp.y = pixelPos.y;
        float dx;
        float dy;
        float r;
        for (int i = 0; i < _maxIterationCount; i++) {
            performDirectLocationModel(_pp, factor, _gp);
            performReverseLocationModel(_gp, _pp2);
            // Refinement of pixel position
            dx = pixelPos.x - _pp2.x;
            dy = pixelPos.y - _pp2.y;
            _pp.x += dx;
            _pp.y += dy;
            r = dx * dx + dy * dy;
            if (r < PIXEL_EPS_SQR) {
                return true;
            }
        }
        return false;
    }

    private PixelPos performReverseLocationModel(GeoPos geoPos, PixelPos pixelPos) {
        return _geoCoding.getPixelPos(geoPos, pixelPos);
    }


    /**
     * Gets the geodetically corrected latitude and longitude value for a given pixel co-ordinate.
     * <p/>
     * Implements the geodetic correction algorithm from the MERIS Geometry Handbook, VT-P194-DOC-001-E, iss 1, rev 4,
     * page 39, equations 6a and 6b.
     *
     * @param pixelPos the pixel position to be corrected
     * @param geoPos
     *
     * @return
     */
    private GeoPos performDirectLocationModel(PixelPos pixelPos, double factor, GeoPos geoPos) {
        geoPos = _geoCoding.getGeoPos(pixelPos, geoPos);
        final float h = getElevation(geoPos, pixelPos);
        _pointing.getViewDir(pixelPos, _vg);
        RsMathUtils.applyGeodeticCorrection(geoPos, factor * h, _vg.zenith, _vg.azimuth);
        return geoPos;
    }

    protected final  boolean isPixelPosValid(PixelPos pixelPos) {
        return pixelPos.x >= 0 && pixelPos.x <= _sceneRasterWidth &&
               pixelPos.y >= 0 && pixelPos.y <= _sceneRasterHeight;
    }

    protected final float getElevation(GeoPos geoPos, PixelPos pixelPos) {
        float h = 0.0f;
        if (_elevationModel != null) {
            try {
                h = _elevationModel.getElevation(geoPos);
            } catch (Exception e) {
            }
            if (h == _elevationModel.getDescriptor().getNoDataValue()) {   // todo (nf) - optimize
                h = 0.0f;
            }
        } else if (_pointing.canGetElevation()) {
            if (pixelPos == null) {
                pixelPos = _geoCoding.getPixelPos(geoPos, null);
            }
            h = _pointing.getElevation(pixelPos);
        }
        return h;
    }


    private boolean mustCorrect(GeoPos geoPos, PixelPos pixelPos) {
        return true;
//        _pointing.getViewDir(pixelPos, _vg);
//        final float elevation = getElevation(geoPos, pixelPos);
//        final double distance = Math.abs(elevation) * Math.tan(MathUtils.DTOR * _vg.zenith);
//        return distance < 25.0;   // todo (nf) - get from Pointing or so
    }

}
