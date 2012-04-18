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

import java.awt.Image;

/**
 * Placemark descriptor implementation for handling track data.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class TrackDescriptor extends AbstractPlacemarkDescriptor {

    public static TrackDescriptor getInstance() {
        return (TrackDescriptor) PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(TrackDescriptor.class.getName());
    }

    @Override
    public boolean isCompatibleWith(SimpleFeatureType featureType) {
        if (featureType.getGeometryDescriptor() == null) {
            return false;
        } else {
            return featureType.getGeometryDescriptor().getType().getBinding().isAssignableFrom(Point.class);
        }
    }

    @Override
    public void setUserData(SimpleFeatureType featureType) {
        featureType.getUserData().put("trackPoints", "true");
    }

    @Override
    public SimpleFeatureType getBaseFeatureType() {
        return null;
    }

    @Override
    public String getRoleName() {
        return null;
    }

    @Override
    public String getRoleLabel() {
        return "Track Points";
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
