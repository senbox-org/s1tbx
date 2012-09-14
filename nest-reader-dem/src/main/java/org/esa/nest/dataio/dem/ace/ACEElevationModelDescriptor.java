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
package org.esa.nest.dataio.dem.ace;

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.dataop.dem.AbstractElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.util.SystemUtils;
import org.esa.nest.util.Settings;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

public class ACEElevationModelDescriptor extends AbstractElevationModelDescriptor {

    private static final String NAME = "ACE30";
    private static final String DB_FILE_SUFFIX = ".ACE";
    private static final String ARCHIVE_URL_PATH = SystemUtils.BEAM_HOME_PAGE + "data/ACE.zip";
    public static final int NUM_X_TILES = 24;
    public static final int NUM_Y_TILES = 12;
    public static final int DEGREE_RES = 15;
    public static final int PIXEL_RES = 1800;
    public static final int NO_DATA_VALUE = -500;

    public static final GeoPos RASTER_ORIGIN = new GeoPos(60, 180);
    public static final int RASTER_WIDTH = NUM_X_TILES * PIXEL_RES;
    public static final int RASTER_HEIGHT = NUM_Y_TILES * PIXEL_RES;
    private static final Datum DATUM = Datum.WGS_84;

    private File aceDemInstallDir = null;

    public ACEElevationModelDescriptor() {
    }

    public String getName() {
        return NAME;
    }

    public Datum getDatum() {
        return DATUM;
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

    public GeoPos getRasterOrigin() {
        return RASTER_ORIGIN;
    }

    @Override
    public int getDegreeRes() {
        return DEGREE_RES;
    }

    @Override
    public int getPixelRes() {
        return PIXEL_RES;
    }

    @Override
    public File getDemInstallDir() {
        if(aceDemInstallDir == null) {
            final String path = Settings.instance().get("DEM/aceDEMDataPath");
            aceDemInstallDir = new File(path);
            if(!aceDemInstallDir.exists())
                aceDemInstallDir.mkdirs();
        }
        return aceDemInstallDir;
    }

    public boolean isDemInstalled() {
        return true;
    }

    public URL getDemArchiveUrl() {
        try {
            return new URL(ARCHIVE_URL_PATH);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("MalformedURLException not expected: " + ARCHIVE_URL_PATH);
        }
    }

    @Deprecated
    public ElevationModel createDem() {
        try {
            return new ACEElevationModel(this, Resampling.BILINEAR_INTERPOLATION);
        } catch (IOException e) {
            return null;
        }
    }

    public ElevationModel createDem(Resampling resamplingMethod) {
        try {
            return new ACEElevationModel(this, resamplingMethod);
        } catch (IOException e) {
            return null;
        }
    }

    public File getTileFile(int minLon, int minLat) {
        return new File(getDemInstallDir(), createTileFilename(minLat, minLon));
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
