/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.commons;

import org.esa.snap.core.datamodel.*;
import org.esa.snap.core.util.ProductUtils;
import org.esa.snap.engine_utilities.gpf.OperatorUtils;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import javax.measure.Unit;
import java.awt.geom.Rectangle2D;

/**
 * help create a CRS GeoCoding
 */
public class CRSGeoCodingHandler {

    private final CoordinateReferenceSystem targetCRS;
    private final CrsGeoCoding geoCoding;
    private final int targetWidth;
    private final int targetHeight;

    public CRSGeoCodingHandler(final Product sourceProduct, final String mapProjection,
                               final double pixelSpacingInDegree, final double pixelSpacingInMeter) throws Exception {
        this(sourceProduct, mapProjection, pixelSpacingInDegree, pixelSpacingInMeter, false);
    }

    public CRSGeoCodingHandler(final Product sourceProduct, final String mapProjection,
                               final double pixelSpacingInDegree, final double pixelSpacingInMeter,
                               final boolean alignToStandardGrid) throws Exception {
        this(sourceProduct, mapProjection, pixelSpacingInDegree, pixelSpacingInMeter, alignToStandardGrid, 0, 0);
    }

    public CRSGeoCodingHandler(final Product sourceProduct, final String mapProjection,
                               final double pixelSpacingInDegree, final double pixelSpacingInMeter,
                               final boolean alignToStandardGrid, final double standardGridOriginX,
                               final double standardGridOriginY) throws Exception {

        targetCRS = getCRS(sourceProduct, mapProjection);

        final OperatorUtils.ImageGeoBoundary srcImageBoundary = OperatorUtils.computeImageGeoBoundary(sourceProduct);

        double pixelSizeX = pixelSpacingInMeter;
        double pixelSizeY = pixelSpacingInMeter;
        if (targetCRS.getName().getCode().equals("WGS84(DD)") ||
                isDegree(targetCRS.getCoordinateSystem().getAxis(0).getUnit())) {
            pixelSizeX = pixelSpacingInDegree;
            pixelSizeY = pixelSpacingInDegree;
        }

        final Rectangle2D bounds = new Rectangle2D.Double();
        double lonMin = srcImageBoundary.lonMin;
        double lonMax = srcImageBoundary.lonMax;

        bounds.setFrameFromDiagonal(lonMin, srcImageBoundary.latMin, lonMax, srcImageBoundary.latMax);
        final ReferencedEnvelope boundsEnvelope = new ReferencedEnvelope(bounds, DefaultGeographicCRS.WGS84);
        final ReferencedEnvelope targetEnvelope = boundsEnvelope.transform(targetCRS, true, 200);

        double easting = targetEnvelope.getMinimum(0);
        double northing = targetEnvelope.getMaximum(1);
        if (alignToStandardGrid) {
            // Force pixels to be aligned with a specified origin point (e.g. 0,0) in the output CRS.
            // This guarantees that the image grids are always aligned when reprojecting or resampling images.
            easting = Math.floor((easting - standardGridOriginX) / pixelSizeX) * pixelSizeX + standardGridOriginX;
            northing = Math.ceil((northing - standardGridOriginY) / pixelSizeY) * pixelSizeY + standardGridOriginY;
            targetWidth = (int) Math.ceil((targetEnvelope.getMaximum(0) - easting) / pixelSizeX);
            targetHeight = (int) Math.ceil((northing - targetEnvelope.getMinimum(1)) / pixelSizeY);
        } else {
            targetWidth = (int) Math.floor(targetEnvelope.getWidth() / pixelSizeX);
            targetHeight = (int) Math.floor(targetEnvelope.getHeight() / pixelSizeY);
        }

        geoCoding = new CrsGeoCoding(targetCRS,
                targetWidth,
                targetHeight,
                easting,
                northing,
                pixelSizeX, pixelSizeY);
    }

    public CRSGeoCodingHandler(final GeoPos geoPosUL, final GeoPos geoPosUR, final GeoPos geoPosLL,
                               final GeoPos geoPosLR, final GeoPos geoPosCT, final String mapProjection,
                               final double pixelSpacingInDegree, final double pixelSpacingInMeter) throws Exception {

        final boolean alignToStandardGrid = false;
        final double standardGridOriginX = 0;
        final double standardGridOriginY = 0;

        targetCRS = getCRS(geoPosCT, mapProjection);

        final OperatorUtils.ImageGeoBoundary srcImageBoundary = getGeoBoundary(geoPosUL, geoPosUR, geoPosLL, geoPosLR);

        double pixelSizeX = pixelSpacingInMeter;
        double pixelSizeY = pixelSpacingInMeter;
        if (targetCRS.getName().getCode().equals("WGS84(DD)") ||
                isDegree(targetCRS.getCoordinateSystem().getAxis(0).getUnit())) {
            pixelSizeX = pixelSpacingInDegree;
            pixelSizeY = pixelSpacingInDegree;
        }

        final Rectangle2D bounds = new Rectangle2D.Double();
        double lonMin = srcImageBoundary.lonMin;
        double lonMax = srcImageBoundary.lonMax;

        bounds.setFrameFromDiagonal(lonMin, srcImageBoundary.latMin, lonMax, srcImageBoundary.latMax);
        final ReferencedEnvelope boundsEnvelope = new ReferencedEnvelope(bounds, DefaultGeographicCRS.WGS84);
        final ReferencedEnvelope targetEnvelope = boundsEnvelope.transform(targetCRS, true, 200);

        double easting = targetEnvelope.getMinimum(0);
        double northing = targetEnvelope.getMaximum(1);
        if (alignToStandardGrid) {
            // Force pixels to be aligned with a specified origin point (e.g. 0,0) in the output CRS.
            // This guarantees that the image grids are always aligned when reprojecting or resampling images.
            easting = Math.floor((easting - standardGridOriginX) / pixelSizeX) * pixelSizeX + standardGridOriginX;
            northing = Math.ceil((northing - standardGridOriginY) / pixelSizeY) * pixelSizeY + standardGridOriginY;
            targetWidth = (int) Math.ceil((targetEnvelope.getMaximum(0) - easting) / pixelSizeX);
            targetHeight = (int) Math.ceil((northing - targetEnvelope.getMinimum(1)) / pixelSizeY);
        } else {
            targetWidth = (int) Math.floor(targetEnvelope.getWidth() / pixelSizeX);
            targetHeight = (int) Math.floor(targetEnvelope.getHeight() / pixelSizeY);
        }

        geoCoding = new CrsGeoCoding(targetCRS,
                targetWidth,
                targetHeight,
                easting,
                northing,
                pixelSizeX, pixelSizeY);
    }

    public static boolean isDegree(final Unit unit) {
        String symbol = unit.getSymbol();
        if(symbol != null) {
            return symbol.equals("°");
        }
        String str = unit.toString();
        return str.equals("°");
    }

    public static CoordinateReferenceSystem getCRS(final Product sourceProduct, String crs) throws Exception {
        try {
            if (crs == null || crs.isEmpty() || crs.equals("WGS84(DD)")) {
                return DefaultGeographicCRS.WGS84;
            }
            return CRS.parseWKT(crs);
        } catch (Exception e) {
            // prefix with EPSG, if there are only numbers
            if (crs.matches("[0-9]*")) {
                crs = "EPSG:" + crs;
            }
            // append center coordinates for AUTO code
            if (crs.matches("AUTO:[0-9]*")) {
                final GeoPos centerGeoPos = ProductUtils.getCenterGeoPos(sourceProduct);
                crs = String.format("%s,%s,%s", crs, centerGeoPos.lon, centerGeoPos.lat);
            }
            // force longitude==x-axis and latitude==y-axis
            return CRS.decode(crs, true);
        }
    }

    public static CoordinateReferenceSystem getCRS(final GeoPos centerGeoPos, String crs) throws Exception {
        try {
            if (crs == null || crs.isEmpty() || crs.equals("WGS84(DD)")) {
                return DefaultGeographicCRS.WGS84;
            }
            return CRS.parseWKT(crs);
        } catch (Exception e) {
            // prefix with EPSG, if there are only numbers
            if (crs.matches("[0-9]*")) {
                crs = "EPSG:" + crs;
            }
            // append center coordinates for AUTO code
            if (crs.matches("AUTO:[0-9]*")) {
                crs = String.format("%s,%s,%s", crs, centerGeoPos.lon, centerGeoPos.lat);
            }
            // force longitude==x-axis and latitude==y-axis
            return CRS.decode(crs, true);
        }
    }

    private OperatorUtils.ImageGeoBoundary getGeoBoundary(final GeoPos geoPosUL, final GeoPos geoPosUR,
                                                          final GeoPos geoPosLL, final GeoPos geoPosLR) {

        final OperatorUtils.ImageGeoBoundary geoBoundary = new OperatorUtils.ImageGeoBoundary();
        final double[] lats = {geoPosUL.getLat(), geoPosUR.getLat(), geoPosLL.getLat(), geoPosLR.getLat()};
        final double[] lons = {geoPosUL.getLon(), geoPosUR.getLon(), geoPosLL.getLon(), geoPosLR.getLon()};

        geoBoundary.latMin = 90.0;
        geoBoundary.latMax = -90.0;
        for (double lat : lats) {
            if (lat < geoBoundary.latMin) {
                geoBoundary.latMin = lat;
            }
            if (lat > geoBoundary.latMax) {
                geoBoundary.latMax = lat;
            }
        }

        geoBoundary.lonMin = 180.0;
        geoBoundary.lonMax = -180.0;
        for (double lon : lons) {
            if (lon < geoBoundary.lonMin) {
                geoBoundary.lonMin = lon;
            }
            if (lon > geoBoundary.lonMax) {
                geoBoundary.lonMax = lon;
            }
        }

        if (geoBoundary.lonMax - geoBoundary.lonMin >= 180) {
            geoBoundary.lonMin = 360.0;
            geoBoundary.lonMax = 0.0;
            for (double lon : lons) {
                if (lon < 0) {
                    lon += 360;
                }
                if (lon < geoBoundary.lonMin) {
                    geoBoundary.lonMin = lon;
                }
                if (lon > geoBoundary.lonMax) {
                    geoBoundary.lonMax = lon;
                }
            }
        }

        return geoBoundary;
    }

    public int getTargetWidth() {
        return targetWidth;
    }

    public int getTargetHeight() {
        return targetHeight;
    }

    public CoordinateReferenceSystem getTargetCRS() {
        return targetCRS;
    }

    public CrsGeoCoding getCrsGeoCoding() {
        return geoCoding;
    }
}
