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
package org.esa.nest.dataio.dem.getasse30;

import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.nest.dataio.dem.BaseElevationModel;
import org.esa.nest.dataio.dem.ElevationFile;

import java.io.File;

public class GETASSE30ElevationModel extends BaseElevationModel {

    private static final ProductReaderPlugIn productReaderPlugIn = getReaderPlugIn(GETASSE30ReaderPlugIn.FORMAT_NAME);

    public GETASSE30ElevationModel(final GETASSE30ElevationModelDescriptor descriptor, final Resampling resamplingMethod) {
        super(descriptor, resamplingMethod);
    }

    @Override
    public double getIndexX(final GeoPos geoPos) {
        return ((geoPos.lon + 180.0) / DEGREE_RES * NUM_PIXELS_PER_TILE) + 0.5;
    }

    @Override
    public double getIndexY(final GeoPos geoPos) {
        return (RASTER_HEIGHT - (geoPos.lat + 90.0) / DEGREE_RES * NUM_PIXELS_PER_TILE) + 0.5;
    }

    @Override
    public GeoPos getGeoPos(final PixelPos pixelPos) {
        final float pixelLat = (RASTER_HEIGHT - pixelPos.y) / (DEGREE_RES * NUM_PIXELS_PER_TILE) - 90.0f;
        final float pixelLon = pixelPos.x / (DEGREE_RES * NUM_PIXELS_PER_TILE) - 180.0f;
        return new GeoPos(pixelLat, pixelLon);
    }

    @Override
    protected void createElevationFile(final ElevationFile[][] elevationFiles,
                                       final int x, final int y, final File demInstallDir) {
        final int minLon = x * DEGREE_RES - 180;
        final int minLat = y * DEGREE_RES - 90;
        final String fileName = descriptor.createTileFilename(minLat, minLon);
        final File localFile = new File(demInstallDir, fileName);
        elevationFiles[x][NUM_Y_TILES - 1 - y] = new GETASSE30File(this, localFile, productReaderPlugIn.createReaderInstance());
    }
}
