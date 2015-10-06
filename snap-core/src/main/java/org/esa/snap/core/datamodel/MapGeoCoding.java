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
import org.esa.snap.core.dataop.maptransf.MapInfo;
import org.esa.snap.core.dataop.maptransf.MapTransform;
import org.esa.snap.core.dataop.maptransf.geotools.CoordinateReferenceSystems;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.ProductUtils;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.referencing.operation.transform.AffineTransform2D;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.crs.DerivedCRS;
import org.opengis.referencing.operation.MathTransform;

import java.awt.Rectangle;
import java.awt.geom.AffineTransform;
import java.awt.geom.NoninvertibleTransformException;
import java.awt.geom.Point2D;

/**
 * A geo-coding based on a cartographic map.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @deprecated since BEAM 4.8, use {@link CrsGeoCoding} instead.
 */
public class MapGeoCoding extends AbstractGeoCoding {

    private final MapInfo mapInfo;
    private final MapTransform mapTransform;
    private final AffineTransform imageToMapTransform;
    private final AffineTransform mapToImageTransform;

    private final boolean normalized;
    private final double normalizedLonMin;

    /**
     * Constructs a map geo-coding based on the given map information.
     *
     * @param mapInfo the map infomation
     *
     * @throws IllegalArgumentException if the given mapInfo is <code>null</code>.
     */
    public MapGeoCoding(MapInfo mapInfo) {
        Guardian.assertNotNull("mapInfo", mapInfo);

        this.mapInfo = mapInfo;

        imageToMapTransform = this.mapInfo.getPixelToMapTransform();
        try {
            mapToImageTransform = this.mapInfo.getPixelToMapTransform().createInverse();
        } catch (NoninvertibleTransformException e) {
            throw new IllegalArgumentException("mapInfo", e);
        }
        mapTransform = this.mapInfo.getMapProjection().getMapTransform();

        final Rectangle rect = new Rectangle(0, 0, this.mapInfo.getSceneWidth(), this.mapInfo.getSceneHeight());
        if (!rect.isEmpty()) {
            final GeoPos[] geoPoints = createGeoBoundary(rect);
            normalized = ProductUtils.normalizeGeoPolygon(geoPoints) != 0;
            double normalizedLonMin = Double.MAX_VALUE;
            for (GeoPos geoPoint : geoPoints) {
                normalizedLonMin = Math.min(normalizedLonMin, geoPoint.lon);
            }
            this.normalizedLonMin = normalizedLonMin;
        } else {
            normalized = false;
            normalizedLonMin = -180;
        }

        final CoordinateReferenceSystem mapCRS = CoordinateReferenceSystems.getCRS(mapInfo.getMapProjection(),
                                                                                    mapInfo.getDatum());
        setMapCRS(mapCRS);
        setImageCRS(createImageCRS(mapCRS, new AffineTransform2D(mapToImageTransform)));

        //TODO -- is this ok ?
        if (mapCRS instanceof DerivedCRS) {
            DerivedCRS derivedCRS = (DerivedCRS) mapCRS;
            CoordinateReferenceSystem baseCRS = derivedCRS.getBaseCRS();
            setGeoCRS(baseCRS);
        } else {
            setGeoCRS(DefaultGeographicCRS.WGS84);
        }
    }

    /**
     * Returns the map information on which this geo-coding is based.
     *
     * @return the map information
     */
    public MapInfo getMapInfo() {
        return mapInfo;
    }

    /**
     * Checks whether or not the longitudes of this geo-coding cross the +/- 180 degree meridian. NOTE: This method is
     * not implemented in this class.
     *
     * @return always <code>false</code>
     */
    @Override
    public boolean isCrossingMeridianAt180() {
        return normalized;
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
    @Override
    public final PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        final GeoPos geoPosNorm = normGeoPos(geoPos, new GeoPos());
        final Point2D mapPos = geoToMap(geoPosNorm, new Point2D.Double());
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
    @Override
    public final GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        final Point2D mapPos = pixelToMap(pixelPos, new Point2D.Double());
        final GeoPos geoPosNorm = mapToGeo(mapPos, new GeoPos());
        return denormGeoPos(geoPosNorm, geoPos);
    }

    /**
     * Releases all of the resources used by this geo-coding and all of its owned children. Its primary use is to allow
     * the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>dispose()</code> are undefined.
     * <p>Overrides of this method should always call <code>super.dispose();</code> after disposing this instance.
     */
    @Override
    public void dispose() {
    }

    /**
     * Gets the datum, the reference point or surface against which {@link GeoPos} measurements are made.
     *
     * @return the datum
     */
    @Override
    public Datum getDatum() {
        return mapInfo.getDatum();
    }

    public MapGeoCoding createDeepClone() {
        return new MapGeoCoding(mapInfo.createDeepClone());
    }

    /////////////////////////////////////////////////////////////////////////
    // Private

    private Point2D geoToMap(final GeoPos geoPosNorm, Point2D mapPos) {
        return mapTransform.forward(geoPosNorm, mapPos);
    }

    private GeoPos mapToGeo(final Point2D mapPos, GeoPos geoPos) {
        return mapTransform.inverse(mapPos, geoPos);
    }

    private PixelPos mapToPixel(final Point2D mapPos, PixelPos pixelPos) {
        if (pixelPos != null) {
            mapToImageTransform.transform(mapPos, pixelPos);
            return pixelPos;
        } else {
            Point2D point2D = mapToImageTransform.transform(mapPos, pixelPos);
            return new PixelPos(point2D.getX(), point2D.getY());
        }
    }

    private Point2D pixelToMap(final PixelPos pixelPos, Point2D mapPos) {
        return imageToMapTransform.transform(pixelPos, mapPos);
    }

    private GeoPos normGeoPos(final GeoPos geoPos, final GeoPos geoPosNorm) {
        geoPosNorm.lat = geoPos.lat;
        if (normalized && geoPos.lon < normalizedLonMin) {
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
     * {@link ProductSubsetDef subsetDef}.
     *
     * @param srcScene  the source scene
     * @param destScene the destination scene
     * @param subsetDef the definition of the subset, may be <code>null</code>
     *
     * @return true, if the geo-coding could be transferred.
     */
    @Override
    public boolean transferGeoCoding(final Scene srcScene, final Scene destScene, final ProductSubsetDef subsetDef) {
        final MapGeoCoding srcMapGeoCoding = ((MapGeoCoding) srcScene.getGeoCoding());
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

    @Override
    public MathTransform getImageToMapTransform() {
        return new AffineTransform2D(imageToMapTransform);

    }
}
