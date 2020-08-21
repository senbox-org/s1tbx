/*
 * Copyright (C) 2020 Skywatch Space Applications Inc. https://www.skywatch.com
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
package org.esa.s1tbx.stac.support;

import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.opengis.geometry.BoundingBox;

public class GeoCodingSupport {


    public static BoundingBox getBoundingBox(final Product product) {
        final GeoCoding geoCoding = product.getSceneGeoCoding();
        final PixelPos upperLeftPP = new PixelPos(0, 0);
        final PixelPos lowerRightPP = new PixelPos(product.getSceneRasterWidth(), product.getSceneRasterHeight());
        final GeoPos upperLeftGP = geoCoding.getGeoPos(upperLeftPP, null);
        final GeoPos lowerRightGP = geoCoding.getGeoPos(lowerRightPP, null);
        final double north = upperLeftGP.getLat();
        final double south = lowerRightGP.getLat();
        double east = lowerRightGP.getLon();
        final double west = upperLeftGP.getLon();
        if (geoCoding.isCrossingMeridianAt180()) {
            east += 360;
        }

        return new ReferencedEnvelope(west, east, north, south, DefaultGeographicCRS.WGS84);
    }
}
