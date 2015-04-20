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
package org.esa.s1tbx.dataio.dem.ace;

import org.esa.s1tbx.dataio.dem.BaseElevationModel;
import org.esa.s1tbx.dataio.dem.ElevationFile;
import org.esa.snap.framework.dataio.ProductReaderPlugIn;
import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.datamodel.PixelPos;
import org.esa.snap.framework.dataop.resamp.Resampling;

import java.io.File;
import java.io.IOException;

public class ACEElevationModel extends BaseElevationModel {

    private static final ProductReaderPlugIn productReaderPlugIn = getReaderPlugIn(ACEReaderPlugIn.FORMAT_NAME);

    public ACEElevationModel(ACEElevationModelDescriptor descriptor, Resampling resamplingMethod) throws IOException {
        super(descriptor, resamplingMethod);
    }

    @Override
    public double getIndexX(final GeoPos geoPos) {
        return (geoPos.lon + 180.0) / DEGREE_RES_BY_NUM_PIXELS_PER_TILE;
    }

    @Override
    public double getIndexY(final GeoPos geoPos) {
        return RASTER_HEIGHT - (geoPos.lat + 90.0) / DEGREE_RES_BY_NUM_PIXELS_PER_TILE;
    }

    @Override
    public GeoPos getGeoPos(final PixelPos pixelPos) {
        final double pixelLat = (RASTER_HEIGHT - pixelPos.y) * DEGREE_RES_BY_NUM_PIXELS_PER_TILE - 90.0;
        final double pixelLon = pixelPos.x * DEGREE_RES_BY_NUM_PIXELS_PER_TILE - 180.0;
        return new GeoPos(pixelLat, pixelLon);
    }

    @Override
    protected void createElevationFile(final ElevationFile[][] elevationFiles,
                                       final int x, final int y, final File demInstallDir) {
        final int minLon = x * DEGREE_RES - 180;
        final int minLat = y * DEGREE_RES - 90;
        final String fileName = descriptor.createTileFilename(minLat, minLon);
        final File localFile = new File(demInstallDir, fileName);
        elevationFiles[x][NUM_Y_TILES - 1 - y] = new ACEFile(this, localFile, productReaderPlugIn.createReaderInstance());
    }
}
