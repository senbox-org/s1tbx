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

package org.esa.beam.framework.dataop.dem;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

class GeoCodingMock implements GeoCoding {

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
    public CoordinateReferenceSystem getMapCRS() {
        return null;
    }

    @Override
    public CoordinateReferenceSystem getImageCRS() {
        return null;
    }

    @Override
    public CoordinateReferenceSystem getGeoCRS() {
        return DefaultGeographicCRS.WGS84;
    }

    @Override
    public MathTransform getImageToMapTransform() {
        return null;
    }

    @Override
    public Datum getDatum() {
        return Datum.WGS_84;
    }

    @Override
    public GeoPos getGeoPos(PixelPos pixelPos, GeoPos geoPos) {
        if (geoPos == null) {
            geoPos = new GeoPos();
        }
        final float lat = 10.0f * (1 - pixelPos.y / OrthorectifierTest.SCENE_HEIGHT);
        final float lon = 10.0f * (pixelPos.x / OrthorectifierTest.SCENE_WIDTH);
        geoPos.setLocation(lat, lon);
        return geoPos;
    }

    @Override
    public PixelPos getPixelPos(GeoPos geoPos, PixelPos pixelPos) {
        if (pixelPos == null) {
            pixelPos = new PixelPos();
        }
        final float x = OrthorectifierTest.SCENE_WIDTH * (geoPos.lon / 10.0f);
        final float y = OrthorectifierTest.SCENE_HEIGHT * (1.0f - geoPos.lat / 10.0f);
        pixelPos.setLocation(x, y);
        return pixelPos;
    }

    @Override
    public boolean isCrossingMeridianAt180() {
        return false;
    }
}
