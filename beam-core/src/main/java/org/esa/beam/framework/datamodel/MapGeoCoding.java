/*
 * $Id: MapGeoCoding.java,v 1.1.1.1 2006/09/11 08:16:45 norman Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.datamodel;

import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.maptransf.MapInfo;
import org.esa.beam.framework.dataop.maptransf.MapTransform;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.ProductUtils;

import java.awt.Rectangle;
import java.awt.geom.Point2D;

/**
 * A geo-coding based on a cartographical map.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class MapGeoCoding extends AbstractGeoCoding {

    private final MapInfo _mapInfo;
    private final double _mapOffsetX; // = Easting
    private final double _mapOffsetY; // = Northing
    private final double _pixelOffsetX; // = Reference pixel X
    private final double _pixelOffsetY; // = Reference pixel Y
    private final double _pixelSizeX;
    private final double _pixelSizeY;
    private final boolean _rotated;
    private final double _sinOrientation;
    private final double _cosOrientation;
    private final MapTransform _mapTransform;

    private final boolean _normalized;
    private final double _normalizedLonMin;

    private final Point2D _mapPos = new Point2D.Double();
    private final GeoPos _geoPosNorm = new GeoPos();


    /**
     * Constructs a map geo-coding based on the given map information.
     *
     * @param mapInfo the map infomation
     *
     * @throws IllegalArgumentException if the given mapInfo is <code>null</code>.
     */
    public MapGeoCoding(MapInfo mapInfo) {
        Guardian.assertNotNull("mapInfo", mapInfo);

        _mapInfo = mapInfo;
        _mapOffsetX = _mapInfo.getEasting();
        _mapOffsetY = _mapInfo.getNorthing();
        _pixelOffsetX = _mapInfo.getPixelX();
        _pixelOffsetY = _mapInfo.getPixelY();
        _pixelSizeX = _mapInfo.getPixelSizeX();
        _pixelSizeY = _mapInfo.getPixelSizeY();
        _rotated = _mapInfo.getOrientation() != 0;
        _sinOrientation = Math.sin(Math.toRadians(_mapInfo.getOrientation()));
        _cosOrientation = Math.cos(Math.toRadians(_mapInfo.getOrientation()));
        _mapTransform = _mapInfo.getMapProjection().getMapTransform();

        final Rectangle rect = new Rectangle(0, 0, mapInfo.getSceneWidth(), mapInfo.getSceneHeight());
        if (!rect.isEmpty()) {
            final GeoPos[] geoPoints = createGeoBoundary(rect);
            _normalized = ProductUtils.normalizeGeoPolygon(geoPoints) != 0;
            double normalizedLonMin = Double.MAX_VALUE;
            for (int i = 0; i < geoPoints.length; i++) {
                normalizedLonMin = Math.min(normalizedLonMin, geoPoints[i].lon);
            }
            _normalizedLonMin = normalizedLonMin;
        } else {
            _normalized = false;
            _normalizedLonMin = -180;
        }
    }

    /**
     * Returns the map information on which this geo-coding is based.
     */
    public MapInfo getMapInfo() {
        return _mapInfo;
    }

    /**
     * Checks whether or not the longitudes of this geo-coding cross the +/- 180 degree meridian. NOTE: This method is
     * not implemented in this class.
     *
     * @return always <code>false</code>
     */
    public boolean isCrossingMeridianAt180() {
        return _normalized;
    }

    /**
     * Checks whether this geo-coding can determine the geodetic position from a pixel position.
     *
     * @return <code>true</code>, if so
     */
    public boolean canGetGeoPos() {
        return true;
    }

    /**
     * Checks whether this geo-coding can determine the pixel position from a geodetic position.
     *
     * @return <code>true</code>, if so
     */
    public boolean canGetPixelPos() {
        return true;
    }

    /**
     * Returns the pixel co-ordinates as x/y for a given geographical position given as lat/lon.
     *
     * @param geoPos   the geographical position as lat/lon.
     * @param pixelPos an instance of <code>PixelPos</code> to be used as retun value. If this parameter is
     *                 <code>null</code>, the method creates a new instance which it then returns.
     *
     * @return the pixel co-ordinates as x/y
     */
    public final PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        final GeoPos geoPosNorm = normGeoPos(geoPos, _geoPosNorm);
        final Point2D mapPos = geoToMap(geoPosNorm, _mapPos);
        return mapToPixel(mapPos, pixelPos);
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
    public final GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        final Point2D mapPos = pixelToMap(pixelPos, _mapPos);
        final GeoPos geoPosNorm = mapToGeo(mapPos, _geoPosNorm);
        return denormGeoPos(geoPosNorm, geoPos);
    }

    /**
     * Releases all of the resources used by this geo-coding and all of its owned children. Its primary use is to allow
     * the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p/>
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     */
    public void dispose() {
    }

    /**
     * Gets the datum, the reference point or surface against which {@link GeoPos} measurements are made.
     *
     * @return the datum
     */
    public Datum getDatum() {
        return _mapInfo.getDatum();
    }

    public MapGeoCoding createDeepClone() {
        return new MapGeoCoding(_mapInfo.createDeepClone());
    }

    /////////////////////////////////////////////////////////////////////////
    // Private

    private Point2D geoToMap(final GeoPos geoPosNorm, Point2D mapPos) {
        return _mapTransform.forward(geoPosNorm, mapPos);
    }

    private GeoPos mapToGeo(final Point2D mapPos, GeoPos geoPos) {
        return _mapTransform.inverse(mapPos, geoPos);
    }

    private PixelPos mapToPixel(final Point2D mapPos, PixelPos pixelPos) {
        double px = +(mapPos.getX() - _mapOffsetX) / _pixelSizeX;
        double py = -(mapPos.getY() - _mapOffsetY) / _pixelSizeY;
        if (_rotated) {
            final double x = px * _cosOrientation - py * _sinOrientation;
            final double y = px * _sinOrientation + py * _cosOrientation;
            px = x;
            py = y;
        }
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        pixelPos.x = (float) (px + _pixelOffsetX);
        pixelPos.y = (float) (py + _pixelOffsetY);
        return pixelPos;
    }

    private Point2D pixelToMap(final PixelPos pixelPos, Point2D mapPos) {
        double px = pixelPos.x - _pixelOffsetX;
        double py = pixelPos.y - _pixelOffsetY;
        if (_rotated) {
            final double x = px;
            final double y = py;
            px = x * _cosOrientation + y * _sinOrientation;
            py = -x * _sinOrientation + y * _cosOrientation;
        }
        mapPos.setLocation(_mapOffsetX + px * _pixelSizeX,
                           _mapOffsetY - py * _pixelSizeY);
        return mapPos;
    }

    private GeoPos normGeoPos(final GeoPos geoPos, final GeoPos geoPosNorm) {
        geoPosNorm.lat = geoPos.lat;
        if (_normalized && geoPos.lon < _normalizedLonMin) {
            geoPosNorm.lon = geoPos.lon + 360.0f;
        } else {
            geoPosNorm.lon = geoPos.lon;
        }
        return geoPosNorm;
    }

    private GeoPos denormGeoPos(final GeoPos geoPosNorm, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        geoPos.lat = geoPosNorm.lat;
        geoPos.lon = geoPosNorm.lon;
        while (geoPos.lon > 180.0f) {
            geoPos.lon -= 360.0f;
        }
        while (geoPos.lon < -180.0f) {
            geoPos.lon += 360.0f;
        }
        return geoPos;
    }

    // Note: method uses getGeoPos, don't call before mapInfo properties are set
    private GeoPos[] createGeoBoundary(Rectangle rect) {
        final int step = (int) Math.max(16, (rect.getWidth() + rect.getHeight()) / 250);
        final PixelPos[] rectBoundary = ProductUtils.createRectBoundary(rect, step);
        final GeoPos[] geoPoints = new GeoPos[rectBoundary.length];
        for (int i = 0; i < geoPoints.length; i++) {
            geoPoints[i] = getGeoPos(rectBoundary[i], null);
        }
        return geoPoints;
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
    public boolean transferGeoCoding(final Scene srcScene, final Scene destScene, final ProductSubsetDef subsetDef) {
        final MapGeoCoding srcMapGeoCoding = ((MapGeoCoding)srcScene.getGeoCoding());
        final MapInfo srcMapInfo = srcMapGeoCoding.getMapInfo();
        float pixelX = srcMapInfo.getPixelX();
        float pixelY = srcMapInfo.getPixelY();
        float easting = srcMapInfo.getEasting();
        float northing = srcMapInfo.getNorthing();
        float pixelSizeX = srcMapInfo.getPixelSizeX();
        float pixelSizeY = srcMapInfo.getPixelSizeY();
        final float orientation = srcMapInfo.getOrientation();
        if (subsetDef != null) {
            final Rectangle region = subsetDef.getRegion();
            if (orientation != 0.0f) {
                // Adjust pixelX, pixelY so that we can conserve the orientation and easting, northing
                int x0 = 0;
                int y0 = 0;
                if (region != null) {
                    x0 = region.x;
                    y0 = region.y;
                }
                pixelX = scalePosition(pixelX - x0, subsetDef.getSubSamplingX());
                pixelY = scalePosition(pixelY - y0, subsetDef.getSubSamplingY());
            } else if (region != null) {
                // Adjust easting, northing so that pixelX, pixelY become their fraction, e.g. 0.0 or 0.5
                pixelX -= (float) Math.floor(pixelX);
                pixelY -= (float) Math.floor(pixelY);
                final PixelPos pixelPos = new PixelPos(region.x + pixelX, region.y + pixelY);
                final GeoPos geoPos = getGeoPos(pixelPos, null);
                final Point2D mapPoint = this.getMapInfo().getMapProjection().getMapTransform().forward(geoPos, null);
                easting = (float) mapPoint.getX();
                northing = (float) mapPoint.getY();
            }
            pixelSizeX *= subsetDef.getSubSamplingX();
            pixelSizeY *= subsetDef.getSubSamplingY();
        }

        final MapInfo destMapInfo = (MapInfo) srcMapInfo.clone();
        destMapInfo.setPixelX(pixelX);
        destMapInfo.setPixelY(pixelY);
        destMapInfo.setEasting(easting);
        destMapInfo.setNorthing(northing);
        destMapInfo.setPixelSizeX(pixelSizeX);
        destMapInfo.setPixelSizeY(pixelSizeY);
        // todo: check if this is correct
        destMapInfo.setSceneWidth(destScene.getRasterWidth());
        destMapInfo.setSceneHeight(destScene.getRasterHeight());
        destScene.setGeoCoding(new MapGeoCoding(destMapInfo));
        return true;
    }

    private static float scalePosition(final float position, final int subSampling) {
        if (subSampling == 1) {
            return position;
        }
        final int positionInt = (int) Math.floor(position);
        final float fraction = position - positionInt;
        return positionInt / subSampling + fraction;
    }

}
