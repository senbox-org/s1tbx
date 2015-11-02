/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dem.dataio.srtm1_hgt;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.dem.BaseElevationTile;
import org.esa.snap.dem.dataio.EarthGravitationalModel96;
import org.esa.snap.engine_utilities.gpf.TileGeoreferencing;

import java.io.IOException;

public final class SRTM1HgtElevationTile extends BaseElevationTile {

    private final EarthGravitationalModel96 egm;

    public SRTM1HgtElevationTile(final SRTM1HgtElevationModel dem, final Product product) throws IOException {
        super(dem, product);
        egm = EarthGravitationalModel96.instance();
    }

    protected void addGravitationalModel(final int index, final float[] line) throws Exception {
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
