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

import org.esa.beam.framework.dataio.DecodeQualification;
import org.opengis.feature.simple.SimpleFeatureType;

import java.awt.Image;
import java.awt.Point;

public class PinDescriptor extends AbstractPlacemarkDescriptor {

    private static final SimpleFeatureType DEFAULT_FEATURE_TYPE = Placemark.createPointFeatureType("org.esa.beam.Pin");

    public static PinDescriptor getInstance() {
        return (PinDescriptor) PlacemarkDescriptorRegistry.getInstance().getPlacemarkDescriptor(PinDescriptor.class.getName());
    }

    @Override
    public DecodeQualification getQualification(SimpleFeatureType featureType) {
        if (featureType.getTypeName().equals("org.esa.beam.Pin")) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public void setUserData(SimpleFeatureType featureType) {
        super.setUserData(featureType);
        featureType.getUserData().put("defaultGeometry", DEFAULT_FEATURE_TYPE.getGeometryDescriptor().getLocalName());
    }

    @Override
    public SimpleFeatureType getBaseFeatureType() {
        return DEFAULT_FEATURE_TYPE;
    }

    @Override
    @Deprecated
    public String getShowLayerCommandId() {
        return "showPinOverlay";
    }

    @Override
    @Deprecated
    public String getRoleName() {
        return "pin";
    }

    @Override
    @Deprecated
    public String getRoleLabel() {
        return "pin";
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
        return product.getPinGroup();
    }

    @Override
    @Deprecated
    public PixelPos updatePixelPos(GeoCoding geoCoding, GeoPos geoPos, PixelPos pixelPos) {
        if (geoCoding == null || !geoCoding.canGetPixelPos() || geoPos == null) {
            return pixelPos;
        }
        return geoCoding.getPixelPos(geoPos, pixelPos);
    }

    @Override
    @Deprecated
    public GeoPos updateGeoPos(GeoCoding geoCoding, PixelPos pixelPos, GeoPos geoPos) {
        if (geoCoding == null || !geoCoding.canGetGeoPos()) {
            return geoPos;
        }
        return geoCoding.getGeoPos(pixelPos, geoPos);

    }
}
