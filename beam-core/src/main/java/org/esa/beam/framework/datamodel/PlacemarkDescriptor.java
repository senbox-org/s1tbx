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

import org.opengis.feature.simple.SimpleFeature;

import java.awt.*;

/**
 * Placemark descriptors are used to describe and create {@link Placemark}s.
 * <p/>
 * New placemark descriptors can be added by using the Service Provider Interface
 * {@code META-INF/services/org.esa.beam.framework.datamodel.PlacemarkDescriptor}.
 * <p/>
 * Since this interface is likely to change, clients should not directly implement it.
 * Instead they should derive their implementation from {@link AbstractPlacemarkDescriptor}.
 *
 * @author Norman Fomferra
 * @version 2.0
 * @since BEAM 2.0 (full revision since BEAM 4.10)
 */
public interface PlacemarkDescriptor {

    Placemark createPlacemark(SimpleFeature feature);

    String getRoleName();

    String getRoleLabel();

    PlacemarkGroup getPlacemarkGroup(Product product);

    PixelPos updatePixelPos(GeoCoding geoCoding, GeoPos geoPos, PixelPos pixelPos);

    GeoPos updateGeoPos(GeoCoding geoCoding, PixelPos pixelPos, GeoPos geoPos);

    // GUI-related stuff

    String getShowLayerCommandId();

    PlacemarkSymbol createDefaultSymbol();

    Image getCursorImage();

    Point getCursorHotSpot();

}
