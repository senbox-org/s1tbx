/*
 * $Id: $
 *
 * Copyright (C) 2009 by Brockmann Consult (info@brockmann-consult.de)
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
package org.esa.beam.geospike;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.util.ProductUtils;
import org.geotools.geometry.GeneralDirectPosition;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.awt.Rectangle;

/**
 * A Geocoding based on a geotools MathTransform
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.6
 */
public class MathTransformGeoCoding implements GeoCoding {

    private final MathTransform mathTransform;
    private final boolean normalized;
    private final double normalizedLonMin;


    public MathTransformGeoCoding(MathTransform mathTransform, Rectangle productRect) {
        this.mathTransform = mathTransform;

        if (!productRect.isEmpty()) {
            final GeoPos[] geoPoints = createGeoBoundary(productRect);
            normalized = ProductUtils.normalizeGeoPolygon(geoPoints) != 0;
            double normalizedLonMinTemp = Double.MAX_VALUE;
            for (GeoPos geoPoint : geoPoints) {
                normalizedLonMinTemp = Math.min(normalizedLonMinTemp, geoPoint.lon);
            }
            normalizedLonMin = normalizedLonMinTemp;
        } else {
            normalized = false;
            normalizedLonMin = -180;
        }
    }

    private GeoPos[] createGeoBoundary(Rectangle rect) {
        final int step = (int) Math.max(16, (rect.getWidth() + rect.getHeight()) / 250);
        final PixelPos[] rectBoundary = ProductUtils.createRectBoundary(rect, step);
        final GeoPos[] geoPoints = new GeoPos[rectBoundary.length];
        for (int i = 0; i < geoPoints.length; i++) {
            geoPoints[i] = getGeoPos(rectBoundary[i], null);
        }
        return geoPoints;
    }

    @Override
    public boolean canGetGeoPos() {
        return true;
    }

    @Override
    public boolean canGetPixelPos() {
        return true;
    }

    @Override
    public void dispose() {
    }

    @Override
    public Datum getDatum() {
        // TODO
        return Datum.WGS_84;
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos(0.0f, 0.0f);
        }
        GeneralDirectPosition position = new GeneralDirectPosition(pixelPos);
        try {
            mathTransform.transform(position, position);
        } catch (Exception e) {
            e.printStackTrace();
            return geoPos;
        }
        geoPos.setLocation((float) position.getOrdinate(1), (float) position.getOrdinate(0));
        denormGeoPos(geoPos);
        return geoPos;
    }

    private void denormGeoPos(GeoPos geoPos) {
        while (geoPos.lon > 180.0f) {
            geoPos.lon -= 360.0f;
        }
        while (geoPos.lon < -180.0f) {
            geoPos.lon += 360.0f;
        }
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        final GeoPos geoPosNorm = normGeoPos(geoPos, new GeoPos());
        // ensure that pixel is out of image (= no source position)
        pixelPos.x = -1;
        pixelPos.y = -1;
        GeneralDirectPosition position = new GeneralDirectPosition(geoPosNorm.lon, geoPosNorm.lat);
        try {
            mathTransform.inverse().transform(position, position);
        } catch (Exception e) {
            e.printStackTrace();
            return pixelPos;
        }
        pixelPos.setLocation(position.getOrdinate(0), position.getOrdinate(1));
        return pixelPos;
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

    @Override
    public boolean isCrossingMeridianAt180() {
        return normalized;
    }

    @Override
    public CoordinateReferenceSystem getCRS() {
        return null;
    }

    @Override
    public void setCRS(CoordinateReferenceSystem crs) {
    }
}
