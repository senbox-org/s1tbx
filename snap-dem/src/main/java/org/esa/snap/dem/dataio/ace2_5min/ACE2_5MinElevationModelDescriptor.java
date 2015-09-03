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
package org.esa.snap.dem.dataio.ace2_5min;

import org.esa.snap.framework.dataop.dem.AbstractElevationModelDescriptor;
import org.esa.snap.framework.dataop.dem.ElevationModel;
import org.esa.snap.framework.dataop.resamp.Resampling;

public class ACE2_5MinElevationModelDescriptor extends AbstractElevationModelDescriptor {

    private static final String NAME = "ACE2_5Min";
    private static final String DB_FILE_SUFFIX = "_5M.ACE2";
    //private static final String ARCHIVE_URL_PATH = "http://nest.s3.amazonaws.com/data/5M_HEIGHTS.zip";
    public static final int NUM_X_TILES = 24;
    public static final int NUM_Y_TILES = 12;
    public static final int DEGREE_RES = 15;
    public static final int PIXEL_RES = 180;
    public static final int NO_DATA_VALUE = -500;
    public static final int RASTER_WIDTH = NUM_X_TILES * PIXEL_RES;
    public static final int RASTER_HEIGHT = NUM_Y_TILES * PIXEL_RES;

    public ACE2_5MinElevationModelDescriptor() {
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

    @Override
    public int getRasterWidth() {
        return RASTER_WIDTH;
    }

    @Override
    public int getRasterHeight() {
        return RASTER_HEIGHT;
    }

    @Override
    public int getTileWidthInDegrees() {
        return DEGREE_RES;
    }

    @Override
    public int getTileWidth() {
        return PIXEL_RES;
    }

    @Override
    public boolean canBeDownloaded() {
        return true;
    }

    public ElevationModel createDem(Resampling resamplingMethod) {
        return new ACE2_5MinElevationModel(this, resamplingMethod);
    }

    public String createTileFilename(int minLat, int minLon) {
        String latString = minLat < 0 ? Math.abs(minLat) + "S" : minLat + "N";
        while (latString.length() < 3) {
            latString = '0' + latString;
        }
        String lonString = minLon < 0 ? Math.abs(minLon) + "W" : minLon + "E";
        while (lonString.length() < 4) {
            lonString = '0' + lonString;
        }
        return latString + lonString + DB_FILE_SUFFIX;
    }

}
