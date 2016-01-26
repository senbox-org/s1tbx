/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.dataop.dem;

import org.esa.snap.core.datamodel.AngularDirection;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.transform.GeoCodingMathTransform;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Pointing;
import org.esa.snap.core.dataop.maptransf.Datum;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.math.RsMathUtils;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultDerivedCRS;
import org.geotools.referencing.cs.DefaultCartesianCS;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

/**
 * An <code>Orthorectifier</code> is a {@link GeoCoding} which performs an orthorectification algorithm on a base {@link
 * GeoCoding}.
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

    private final int sceneRasterWidth;
    private final int sceneRasterHeight;

    private final Pointing pointing;
    private final GeoCoding geoCoding;
    private final ElevationModel elevationModel;
    private final int maxIterationCount;
    private final DefaultDerivedCRS imageCRS;

    private volatile MathTransform imageToMapTransform;

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
        Guardian.assertGreaterThan("maxIterationCount", maxIterationCount, 1);
        this.sceneRasterWidth = sceneRasterWidth;
        this.sceneRasterHeight = sceneRasterHeight;
        this.pointing = pointing;
        this.geoCoding = pointing.getGeoCoding();
        this.elevationModel = elevationModel;
        this.maxIterationCount = maxIterationCount;

        final CoordinateReferenceSystem geoCRS = geoCoding.getGeoCRS();
        this.imageCRS = new DefaultDerivedCRS("Image CS based on " + geoCRS.getName(),
                                              geoCRS,
                                              new GeoCodingMathTransform(this),
                                              DefaultCartesianCS.DISPLAY);
    }

    /**
     * Gets the datum, the reference point or surface against which {@link GeoPos} measurements are made.
     *
     * @return the datum
     */
    @Override
    public Datum getDatum() {
        return geoCoding.getDatum();
    }

    @Override
    public CoordinateReferenceSystem getImageCRS() {
        return imageCRS;
    }

    @Override
    public CoordinateReferenceSystem getMapCRS() {
        return geoCoding.getMapCRS();
    }

    @Override
    public CoordinateReferenceSystem getGeoCRS() {
        return geoCoding.getGeoCRS();
    }

    @Override
    public MathTransform getImageToMapTransform() {
        synchronized (this) {
            if (imageToMapTransform == null) {
                try {
                    imageToMapTransform = CRS.findMathTransform(imageCRS, getMapCRS());
                } catch (FactoryException e) {
                    throw new IllegalStateException(
                            "Not able to find a math transformation from image to map CRS.", e);
                }
            }
        }
        return imageToMapTransform;
    }

    public Pointing getPointing() {
        return pointing;
    }

    public GeoCoding getGeoCoding() {
        return pointing.getGeoCoding();
    }

    public ElevationModel getElevationModel() {
        return elevationModel;
    }

    public int getMaxIterationCount() {
        return maxIterationCount;
    }

    /**
     * Returns the source pixel coordinate for a <i>true (corrected)</i> geographical coordinate.
     * <p>
     * Implements the prediction/correction algorithm from the MERIS Geometry Handbook, VT-P194-DOC-001-E, iss 1, rev 4,
     * page 29, figure 23.
     * <p>
     * Scope of the prediction/correction algorithm is to retrieve the pixel x,y
     * that matches the <i>true</i> lat,lon by the direct location model f(x,y) = lat,lon.
     *
     * @param geoPos   the <i>true (corrected)</i> geographical coordinate as lat/lon.
     * @param pixelPos an instance of <code>Point</code> to be used as return value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     *
     * @return the source pixel coordinate
     */
    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        pixelPos = performReverseLocationModel(geoPos, pixelPos);
        if (!isPixelPosValid(pixelPos)) {
            return pixelPos;
        }
        if (!mustCorrect(geoPos, pixelPos)) {
            return pixelPos;
        }
        final PixelPos correctedPixelPos = performPredictionCorrection(pixelPos);
        if (correctedPixelPos != null) {
            pixelPos.setLocation(correctedPixelPos);
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
    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        return performDirectLocationModel(pixelPos, 1f, geoPos);
    }


    /**
     * Checks whether or not this geo-coding can determine the geodetic position from a pixel position.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean canGetGeoPos() {
        return getGeoCoding().canGetGeoPos();
    }

    /**
     * Checks whether or not this geo-coding can determine the pixel position from a geodetic position.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean canGetPixelPos() {
        return getGeoCoding().canGetPixelPos();
    }

    /**
     * Checks whether or not the longitudes of this geo-coding cross the +/- 180 degree meridian.
     *
     * @return <code>true</code>, if so
     */
    @Override
    public boolean isCrossingMeridianAt180() {
        return getGeoCoding().isCrossingMeridianAt180();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        Orthorectifier that = (Orthorectifier) o;

        if (elevationModel != null ? !elevationModel.equals(that.elevationModel) : that.elevationModel != null) {
            return false;
        }
        if (!geoCoding.equals(that.geoCoding)) {
            return false;
        }

        return true;
    }

    @Override
    public int hashCode() {
        int result = geoCoding.hashCode();
        result = 31 * result + (elevationModel != null ? elevationModel.hashCode() : 0);
        return result;
    }

    /**
     * Releases all of the resources used by this object instance and all of its owned children. Its primary use is to
     * allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     */
    @Override
    public void dispose() {
    }


    private PixelPos performPredictionCorrection(final PixelPos pixelPos) {
        PixelPos correctedPixelPos = new PixelPos();
        if (correctPrediction(pixelPos, 1.0, correctedPixelPos)
            || correctPrediction(pixelPos, 0.5, correctedPixelPos)
            || correctPrediction(pixelPos, 2.0, correctedPixelPos)) {
            return correctedPixelPos;
        }
        return null;
    }

    private boolean correctPrediction(final PixelPos pixelPos,
                                      double factor,
                                      final PixelPos correctedPixelPos) {
        final PixelPos pp = new PixelPos();
        final GeoPos gp = new GeoPos();
        double dx;
        double dy;
        double r;
        correctedPixelPos.setLocation(pixelPos);
        for (int i = 0; i < maxIterationCount; i++) {
            performDirectLocationModel(correctedPixelPos, factor, gp);
            performReverseLocationModel(gp, pp);
            // Refinement of pixel position
            dx = pixelPos.x - pp.x;
            dy = pixelPos.y - pp.y;
            correctedPixelPos.x += dx;
            correctedPixelPos.y += dy;
            r = dx * dx + dy * dy;
            if (r < PIXEL_EPS_SQR) {
                return true;
            }
        }
        return false;
    }

    private PixelPos performReverseLocationModel(GeoPos geoPos, PixelPos pixelPos) {
        return geoCoding.getPixelPos(geoPos, pixelPos);
    }


    /**
     * Gets the geodetically corrected latitude and longitude value for a given pixel co-ordinate.
     * <p>
     * Implements the geodetic correction algorithm from the MERIS Geometry Handbook, VT-P194-DOC-001-E, iss 1, rev 4,
     * page 39, equations 6a and 6b.
     *
     * @param pixelPos the pixel position to be corrected
     * @param factor   A factor. Must be greater than zero.
     * @param geoPos   an existing geo position or null
     *
     * @return the geo position
     */
    private GeoPos performDirectLocationModel(PixelPos pixelPos, double factor, GeoPos geoPos) {
        geoPos = geoCoding.getGeoPos(pixelPos, geoPos);
        final double h = getElevation(geoPos, pixelPos);
        final AngularDirection vg = pointing.getViewDir(pixelPos, null);
        RsMathUtils.applyGeodeticCorrection(geoPos, factor * h, vg.zenith, vg.azimuth);
        return geoPos;
    }

    protected final boolean isPixelPosValid(PixelPos pixelPos) {
        return pixelPos.x >= 0 && pixelPos.x <= sceneRasterWidth &&
               pixelPos.y >= 0 && pixelPos.y <= sceneRasterHeight;
    }

    protected final double getElevation(GeoPos geoPos, PixelPos pixelPos) {
        double h = 0.0f;
        if (elevationModel != null) {
            try {
                h = elevationModel.getElevation(geoPos);
            } catch (Exception ignored) {
                // ignored
            }
            if (h == elevationModel.getDescriptor().getNoDataValue()) {
                h = 0.0f;
            }
        } else if (pointing.canGetElevation()) {
            if (pixelPos == null) {
                pixelPos = geoCoding.getPixelPos(geoPos, null);
            }
            h = pointing.getElevation(pixelPos);
        }
        return h;
    }


    private static boolean mustCorrect(GeoPos geoPos, PixelPos pixelPos) {
        return true;
//        _pointing.getViewDir(pixelPos, _vg);
//        final float elevation = getElevation(geoPos, pixelPos);
//        final double distance = Math.abs(elevation) * Math.tan(MathUtils.DTOR * _vg.zenith);
//        return distance < 25.0;   // todo (nf) - get from Pointing or so
    }

}
