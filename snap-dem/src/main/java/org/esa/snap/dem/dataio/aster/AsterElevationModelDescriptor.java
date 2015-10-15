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
package org.esa.snap.dem.dataio.aster;

import org.esa.snap.core.dataop.dem.AbstractElevationModelDescriptor;
import org.esa.snap.core.dataop.dem.ElevationModel;
import org.esa.snap.core.dataop.resamp.Resampling;

public class AsterElevationModelDescriptor extends AbstractElevationModelDescriptor {

    private static final String NAME = "ASTER 1sec GDEM";
    //private static final String DB_FILE_SUFFIX = ".TIF";
    private static final int NUM_X_TILES = 360;
    private static final int NUM_Y_TILES = 166;
    private static final int DEGREE_RES = 1;
    private static final int PIXEL_RES = 3600;
    private static final int NO_DATA_VALUE = -9999;
    private static final int RASTER_WIDTH = NUM_X_TILES * PIXEL_RES;
    private static final int RASTER_HEIGHT = NUM_Y_TILES * PIXEL_RES;

    public AsterElevationModelDescriptor() {
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
        return false;
    }

    @Override
    public ElevationModel createDem(final Resampling resamplingMethod) {
        try {
            return new AsterElevationModel(this, resamplingMethod);
        } catch (Exception e) {
            return null;
        }
    }

}
