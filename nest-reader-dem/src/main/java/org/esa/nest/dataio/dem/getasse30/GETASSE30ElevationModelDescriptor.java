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

import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.dataop.dem.AbstractElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.util.SystemUtils;
import org.esa.nest.util.Settings;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class GETASSE30ElevationModelDescriptor extends AbstractElevationModelDescriptor {

    private static final String NAME = "GETASSE30";
    private static final String DB_FILE_SUFFIX = ".GETASSE30";
    private static final String ARCHIVE_URL_PATH = SystemUtils.BEAM_HOME_PAGE + "data/GETASSE30.zip";
    private static final int NUM_X_TILES = 24;
    private static final int NUM_Y_TILES = 12;
    private static final int DEGREE_RES = 15;
    private static final int PIXEL_RES = 1800;
    public static final int NO_DATA_VALUE = -9999;
    private static final GeoPos RASTER_ORIGIN = new GeoPos(90.0f, 180.0f);
    private static final int RASTER_WIDTH = NUM_X_TILES * PIXEL_RES;
    private static final int RASTER_HEIGHT = NUM_Y_TILES * PIXEL_RES;

    private static final Datum DATUM = Datum.WGS_84;
    private File demInstallDir = null;

    public GETASSE30ElevationModelDescriptor() {
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

    @Override
    public int getRasterHeight() {
        return RASTER_HEIGHT;
    }

    @Override
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
        if(demInstallDir == null) {
            final String path = Settings.instance().get("DEM/Getasse30DEMDataPath");
            demInstallDir = new File(path);
            if(!demInstallDir.exists())
                demInstallDir.mkdirs();
        }
        return demInstallDir;
    }

    @Override
    public boolean isDemInstalled() {
        return true;
    }

    @Override
    public URL getDemArchiveUrl() {
        try {
            return new URL(ARCHIVE_URL_PATH);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("MalformedURLException not expected: " + ARCHIVE_URL_PATH);
        }
    }

    @Override
    @Deprecated
    public ElevationModel createDem() {
        return createDem(Resampling.BILINEAR_INTERPOLATION);
    }

    @Override
    public ElevationModel createDem(Resampling resampling) {
       return new GETASSE30ElevationModel(this, resampling);
    }

    public File getTileFile(int minLon, int minLat) {
        return new File(getDemInstallDir(), createTileFilename(minLat, minLon));
    }

    public String createTileFilename(int minLat, int minLon) {
        String latString = minLat < 0 ? Math.abs(minLat) + "S" : minLat + "N";
        while (latString.length() < 3) {
            latString = "0" + latString;
        }
        String lonString = minLon < 0 ? Math.abs(minLon) + "W" : minLon + "E";
        while (lonString.length() < 4) {
            lonString = "0" + lonString;
        }
        return latString + lonString + DB_FILE_SUFFIX;
    }

}
