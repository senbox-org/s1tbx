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

import java.awt.Image;
import java.awt.Point;

public class GcpDescriptor implements PlacemarkDescriptor {

    public final static GcpDescriptor INSTANCE = new GcpDescriptor();

    private GcpDescriptor() {
    }

    @Override
    public String getShowLayerCommandId() {
        return "showGcpOverlay";
    }

    @Override
    public String getRoleName() {
        return "gcp";
    }

    @Override
    public String getRoleLabel() {
        return "GCP";
    }

    @Override
    public Image getCursorImage() {
        return null;
    }

    @Override
    public Point getCursorHotSpot() {
        return new Point();
    }

    @Override
    public PlacemarkGroup getPlacemarkGroup(Product product) {
        return product.getGcpGroup();
    }

    @Override
    public PlacemarkSymbol createDefaultSymbol() {
        return PlacemarkSymbol.createDefaultGcpSymbol();
    }

    @Override
    public PixelPos updatePixelPos(GeoCoding geoCoding, GeoPos geoPos, PixelPos pixelPos) {
        return pixelPos;
    }

    @Override
    public GeoPos updateGeoPos(GeoCoding geoCoding, PixelPos pixelPos, GeoPos geoPos) {
        return geoPos;
    }
}
