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
package org.esa.nest.dataio.dem.srtm3_geotiff;

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

public class SRTM3GeoTiffElevationModelDescriptor extends AbstractElevationModelDescriptor {

    private static final String NAME = "SRTM 3Sec";
    private static final String DB_FILE_SUFFIX = ".tif";
    private static final String ARCHIVE_URL_PATH = SystemUtils.BEAM_HOME_PAGE + "data/ACE.zip";
    private static final int NUM_X_TILES = 72;
    private static final int NUM_Y_TILES = 24;
    private static final int DEGREE_RES = 5;
    private static final int PIXEL_RES = 6000;
    private static final int NO_DATA_VALUE = -32768;

    private static final GeoPos RASTER_ORIGIN = new GeoPos(60, 180);
    private static final int RASTER_WIDTH = NUM_X_TILES * PIXEL_RES;
    private static final int RASTER_HEIGHT = NUM_Y_TILES * PIXEL_RES;

    private static final Datum DATUM = Datum.WGS_84;

    private File demInstallDir = null;

    private static SRTM3GeoTiffElevationModel demModel = null;

    public SRTM3GeoTiffElevationModelDescriptor() {
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

    public int getDegreeRes() {
        return DEGREE_RES;
    }

    public int getPixelRes() {
        return PIXEL_RES;
    }

    @Override
    public File getDemInstallDir() {
        if(demInstallDir == null) {
            final String path = Settings.instance().get("DEM/srtm3GeoTiffDEMDataPath");
            demInstallDir = new File(path);
            if(!demInstallDir.exists())
                demInstallDir.mkdirs();
        }
        return demInstallDir;
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
    public synchronized ElevationModel createDem() {
        return new SRTM3GeoTiffElevationModel(this, Resampling.BILINEAR_INTERPOLATION);
    }

    public synchronized ElevationModel createDem(Resampling resamplingMethod) {
        return new SRTM3GeoTiffElevationModel(this, resamplingMethod);
    }

    public String createTileFilename(final int tileX, final int tileY) {
        final StringBuilder name = new StringBuilder("srtm_");
        if(tileX < 10) {
            name.append('0');
        }
        name.append(tileX);
        name.append('_');
        if(tileY < 10) {
            name.append('0');
        }
        name.append(tileY);
        name.append(DB_FILE_SUFFIX);
        return name.toString();
    }

}