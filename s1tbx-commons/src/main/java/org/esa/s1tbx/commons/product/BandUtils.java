/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.commons.product;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.VirtualBand;
import org.esa.snap.core.util.SystemUtils;

public class BandUtils {

    public static Band createBandFromVirtualBand(final VirtualBand band){
        final Band trgBand = new Band(band.getName(), band.getDataType(), band.getRasterWidth(), band.getRasterHeight());

        final ProductData data = band.createCompatibleRasterData(band.getRasterWidth(), band.getRasterHeight());
        trgBand.setRasterData(data);

        try {
            for (int y = 0; y < band.getRasterHeight(); y++) {
                float [] line = new float[band.getRasterWidth()];
                line = band.readPixels(0, y, band.getRasterWidth(), 1, line);
                trgBand.setPixels(0, y, line.length, 1, line);
            }

        } catch (Exception e){
            SystemUtils.LOG.severe(e.getMessage());
            e.printStackTrace();
        }
        return trgBand;
    }
}
