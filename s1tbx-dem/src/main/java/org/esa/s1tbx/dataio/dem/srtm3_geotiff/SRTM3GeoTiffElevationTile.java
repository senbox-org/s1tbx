/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.dataio.dem.srtm3_geotiff;

import org.esa.s1tbx.dataio.dem.BaseElevationTile;
import org.esa.s1tbx.dataio.dem.EarthGravitationalModel96;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.Product;
import org.esa.snap.gpf.TileGeoreferencing;

public final class SRTM3GeoTiffElevationTile extends BaseElevationTile {

    private final EarthGravitationalModel96 egm;

    public SRTM3GeoTiffElevationTile(final SRTM3GeoTiffElevationModel dem, final Product product) {
        super(dem, product);
        egm = EarthGravitationalModel96.instance();
    }

    protected void addGravitationalModel(final int index, final float[] line) {
        final GeoPos geoPos = new GeoPos();
        final TileGeoreferencing tileGeoRef = new TileGeoreferencing(product, 0, index, line.length, 1);
        final double[][] v = new double[4][4];
        for (int i = 0; i < line.length; i++) {
            if (line[i] != noDataValue) {
                tileGeoRef.getGeoPos(i, index, geoPos);
                line[i] += egm.getEGM(geoPos.lat, geoPos.lon, v);
            }
        }
    }
}
