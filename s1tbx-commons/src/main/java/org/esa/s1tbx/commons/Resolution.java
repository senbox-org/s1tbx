/*
 * Copyright (C) 2021 SkyWatch. https://www.skywatch.com
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
import org.esa.snap.engine_utilities.datamodel.AbstractMetadata;
import org.esa.snap.engine_utilities.eo.GeoUtils;

public class Resolution {

    private double resX, resY;

    public Resolution(final Product product) {
        final MetadataElement absRoot = AbstractMetadata.getAbstractedMetadata(product);
        resX = absRoot.getAttributeDouble(AbstractMetadata.range_spacing);
        resY = absRoot.getAttributeDouble(AbstractMetadata.azimuth_spacing);

        if (resX == 0 || resX == AbstractMetadata.NO_METADATA) {
            resX = getResolutionXAtCentre(product);
            absRoot.setAttributeDouble(AbstractMetadata.range_spacing, resX);
        }
        if (resY == 0 || resY == AbstractMetadata.NO_METADATA) {
            resY = getResolutionYAtCentre(product);
            absRoot.setAttributeDouble(AbstractMetadata.azimuth_spacing, resY);
        }
    }

    public double getResX() {
        return resX;
    }

    public double getResY() {
        return resY;
    }

    public static double getResolutionXAtCentre(final Product product) {
        final int numPixelsX = Math.min(200, product.getSceneRasterWidth());
        final int halfX = numPixelsX / 2;
        final int centreX = product.getSceneRasterWidth() / 2;
        final int centreY = product.getSceneRasterHeight() / 2;

        final GeoCoding geoCoding = product.getSceneGeoCoding();
        final GeoPos geoPosX1 = geoCoding.getGeoPos(new PixelPos(centreX - halfX, centreY), null);
        final GeoPos geoPosX2 = geoCoding.getGeoPos(new PixelPos(centreX + halfX, centreY), null);
        final GeoUtils.DistanceHeading distX = GeoUtils.vincenty_inverse(geoPosX1, geoPosX2);

        return distX.distance / numPixelsX;
    }

    public static double getResolutionYAtCentre(final Product product) {
        final int numPixelsY = Math.min(200, product.getSceneRasterHeight());
        final int halfY = numPixelsY / 2;
        final int centreX = product.getSceneRasterWidth() / 2;
        final int centreY = product.getSceneRasterHeight() / 2;

        final GeoCoding geoCoding = product.getSceneGeoCoding();
        final GeoPos geoPosY1 = geoCoding.getGeoPos(new PixelPos(centreX, centreY - halfY), null);
        final GeoPos geoPosY2 = geoCoding.getGeoPos(new PixelPos(centreX, centreY + halfY), null);
        final GeoUtils.DistanceHeading distY = GeoUtils.vincenty_inverse(geoPosY1, geoPosY2);

        return distY.distance / numPixelsY;
    }
}
