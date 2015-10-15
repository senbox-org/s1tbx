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
package org.esa.snap.dem.dataio.srtm3_geotiff;

import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.dem.BaseElevationModel;
import org.esa.snap.core.dataop.dem.ElevationFile;
import org.esa.snap.core.dataop.resamp.Resampling;

import java.io.File;

public final class SRTM3GeoTiffElevationModel extends BaseElevationModel {

    private static final String DB_FILE_SUFFIX = ".tif";
    private static final ProductReaderPlugIn productReaderPlugIn = getReaderPlugIn("GeoTIFF");

    public SRTM3GeoTiffElevationModel(final SRTM3GeoTiffElevationModelDescriptor descriptor, final Resampling resamplingMethod) {
        super(descriptor, resamplingMethod);

        setMaxCacheSize(12);
    }

    @Override
    public double getIndexX(final GeoPos geoPos) {
        return ((geoPos.lon + 180.0) * DEGREE_RES_BY_NUM_PIXELS_PER_TILEinv);
    }

    @Override
    public double getIndexY(final GeoPos geoPos) {
        return ((60.0 - geoPos.lat) * DEGREE_RES_BY_NUM_PIXELS_PER_TILEinv);
    }

    @Override
    public GeoPos getGeoPos(final PixelPos pixelPos) {
        final double pixelLat = (RASTER_HEIGHT - pixelPos.y) * DEGREE_RES_BY_NUM_PIXELS_PER_TILE - 60.0;
        final double pixelLon = pixelPos.x * DEGREE_RES_BY_NUM_PIXELS_PER_TILE - 180.0;
        return new GeoPos(pixelLat, pixelLon);
    }

    @Override
    protected void createElevationFile(final ElevationFile[][] elevationFiles,
                                       final int x, final int y, final File demInstallDir) {
        final String fileName = createTileFilename(x + 1, y + 1);
        final File localFile = new File(demInstallDir, fileName);
        elevationFiles[x][y] = new SRTM3GeoTiffFile(this, localFile, productReaderPlugIn.createReaderInstance());
    }

    private String createTileFilename(final int tileX, final int tileY) {
        final StringBuilder name = new StringBuilder("srtm_");
        if (tileX < 10) {
            name.append('0');
        }
        name.append(tileX);
        name.append('_');
        if (tileY < 10) {
            name.append('0');
        }
        name.append(tileY);
        name.append(DB_FILE_SUFFIX);
        return name.toString();
    }

}
