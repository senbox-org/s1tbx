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

import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.*;

public class GcpDescriptor extends AbstractPlacemarkDescriptor {

    private static final SimpleFeatureType DEFAULT_FEATURE_TYPE = Placemark.createPointFeatureType("org.esa.beam.GroundControlPoint");

    public static GcpDescriptor getInstance() {
        return (GcpDescriptor) PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(GcpDescriptor.class.getName());
    }

    @Override
    public boolean isCompatibleWith(SimpleFeatureType featureType) {
        return featureType.getTypeName().equals("org.esa.beam.GroundControlPoint");
        // todo - comment in next line
//        return featureType.getGeometryDescriptor().getType().getBinding().isAssignableFrom(com.vividsolutions.jts.geom.Point.class);
    }

    @Override
    public SimpleFeatureType getBaseFeatureType() {
        return DEFAULT_FEATURE_TYPE;
    }

    @Override
    @Deprecated
    public String getShowLayerCommandId() {
        return "showGcpOverlay";
    }

    @Override
    @Deprecated
    public String getRoleName() {
        return "gcp";
    }

    @Override
    @Deprecated
    public String getRoleLabel() {
        return "GCP";
    }

    @Override
    @Deprecated
    public Image getCursorImage() {
        return null;
    }

    @Override
    @Deprecated
    public Point getCursorHotSpot() {
        return new Point();
    }

    @Override
    @Deprecated
    public PlacemarkGroup getPlacemarkGroup(Product product) {
        return product.getGcpGroup();
    }

    @Override
    @Deprecated
    public PixelPos updatePixelPos(GeoCoding geoCoding, GeoPos geoPos, PixelPos pixelPos) {
        return pixelPos;
    }

    @Override
    @Deprecated
    public GeoPos updateGeoPos(GeoCoding geoCoding, PixelPos pixelPos, GeoPos geoPos) {
        return geoPos;
    }
}
