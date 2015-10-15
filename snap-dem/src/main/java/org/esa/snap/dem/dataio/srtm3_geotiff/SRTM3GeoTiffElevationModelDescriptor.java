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

import org.esa.snap.core.dataop.dem.AbstractElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.resamp.Resampling;

public class SRTM3GeoTiffElevationModelDescriptor extends AbstractElevationModelDescriptor {

    private static final String NAME = "SRTM 3Sec";
    private static final int NUM_X_TILES = 72;
    private static final int NUM_Y_TILES = 24;
    private static final int DEGREE_RES = 5;
    private static final int PIXEL_RES = 6000;
    private static final int NO_DATA_VALUE = -32768;

    private static final int RASTER_WIDTH = NUM_X_TILES * PIXEL_RES;
    private static final int RASTER_HEIGHT = NUM_Y_TILES * PIXEL_RES;

    public SRTM3GeoTiffElevationModelDescriptor() {
    }

    public String getName() {
        return NAME;
    }

    public int getNumXTiles() {
        return NUM_X_TILES;
    }

    public int getNumYTiles() {
        return NUM_Y_TILES;
    }

    public float getNoDataValue() {
        return NO_DATA_VALUE;
    }

    public int getRasterWidth() {
        return RASTER_WIDTH;
    }

    public int getRasterHeight() {
        return RASTER_HEIGHT;
    }

    public int getTileWidthInDegrees() {
        return DEGREE_RES;
    }

    public int getTileWidth() {
        return PIXEL_RES;
    }

    @Override
    public boolean canBeDownloaded() {
        return true;
    }

    @Override
    public synchronized ElevationModel createDem(Resampling resamplingMethod) {
        return new SRTM3GeoTiffElevationModel(this, resamplingMethod);
    }

}
