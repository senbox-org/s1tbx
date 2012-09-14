/*
 * Copyright (C) 2012 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.dem.srtm3_geotiff;

import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.nest.dataio.dem.BaseElevationTile;
import org.esa.nest.dataio.dem.EarthGravitationalModel96;

public final class SRTM3GeoTiffElevationTile extends BaseElevationTile {
    /*
    private final float[][] egmArray;

    public SRTM3GeoTiffElevationTile(final SRTM3GeoTiffElevationModel dem, final Product product) {
        super(dem, product);

        egmArray = EarthGravitationalModel96.instance().computeEGMArray(product.getGeoCoding(), 20, 20);
    }

    protected void addGravitationalModel(final int index, final float[] line) {
        final int rowIdxInEGMArray = index / 300; // tile_height / numEGMSamplesInCol
        for (int i = 0; i < line.length; i++) {
            if (line[i] != noDataValue) {
                //final int colIdxInEGMArray = i/300; // tile_width / numEGMSamplesInRow = 6000 / 20 = 300
                line[i] += egmArray[rowIdxInEGMArray][i/300];
            }
        }
    }
    */

    private GeoCoding geoCoding = null;

    public SRTM3GeoTiffElevationTile(final SRTM3GeoTiffElevationModel dem, final Product product) {
        super(dem, product);
        geoCoding = product.getGeoCoding();
    }

    protected void addGravitationalModel(final int index, final float[] line) {
        final GeoPos geoPos = new GeoPos();
        final PixelPos pixPos = new PixelPos();
        for (int i = 0; i < line.length; i++) {
            if (line[i] != noDataValue) {
                pixPos.setLocation(i, index);
                geoCoding.getGeoPos(pixPos, geoPos);
                line[i] += EarthGravitationalModel96.instance().getEGM(geoPos.lat, geoPos.lon);
            }
        }   
    }
}