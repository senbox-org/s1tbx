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

package org.esa.beam.framework.datamodel;

import com.vividsolutions.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.awt.Image;

/**
 * Placemark descriptor implementation for handling track data.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class TrackPlacemarkDescriptor extends AbstractPlacemarkDescriptor {

    private final SimpleFeatureType featureType;

    public TrackPlacemarkDescriptor(CoordinateReferenceSystem crs) {
        featureType = PlainFeatureFactory.createPlainFeatureType("trackPoints", Point.class, crs);
    }

    @Override
    public boolean isCompatibleWith(SimpleFeatureType featureType) {
        return featureType.getTypeName().equals(featureType.getTypeName());
    }

    @Override
    public SimpleFeatureType getBaseFeatureType() {
        return featureType;
    }

    @Override
    public String getRoleName() {
        return null;
    }

    @Override
    public String getRoleLabel() {
        return null;
    }

    @Override
    public PlacemarkGroup getPlacemarkGroup(Product product) {
        return null;
    }

    @Override
    public PixelPos updatePixelPos(GeoCoding geoCoding, GeoPos geoPos, PixelPos pixelPos) {
        return null;
    }

    @Override
    public GeoPos updateGeoPos(GeoCoding geoCoding, PixelPos pixelPos, GeoPos geoPos) {
        return null;
    }

    @Override
    public String getShowLayerCommandId() {
        return null;
    }

    @Override
    public Image getCursorImage() {
        return null;
    }

    @Override
    public java.awt.Point getCursorHotSpot() {
        return null;
    }
}
