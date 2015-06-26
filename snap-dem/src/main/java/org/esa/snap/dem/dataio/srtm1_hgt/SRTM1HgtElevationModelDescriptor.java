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

import org.esa.snap.framework.datamodel.GeoPos;
import org.esa.snap.framework.dataop.dem.AbstractElevationModelDescriptor;
import org.esa.snap.framework.dataop.dem.ElevationModel;
import org.esa.snap.framework.dataop.maptransf.Datum;
import org.esa.snap.framework.dataop.resamp.Resampling;

import java.io.IOException;
import java.net.URL;

public class SRTM1HgtElevationModelDescriptor extends AbstractElevationModelDescriptor {

    private static final String NAME = "SRTM 1Sec HGT";
    private static final String DB_FILE_SUFFIX = ".hgt";
    private static final int NUM_X_TILES = 360;
    private static final int NUM_Y_TILES = 120;
    private static final int DEGREE_RES = 1;
    private static final int PIXEL_RES = 3600;
    public static final int NO_DATA_VALUE = -32768;

    private static final GeoPos RASTER_ORIGIN = new GeoPos(60.0f, 180.0f);
    private static final int RASTER_WIDTH = NUM_X_TILES * PIXEL_RES;
    private static final int RASTER_HEIGHT = NUM_Y_TILES * PIXEL_RES;

    private static final Datum DATUM = Datum.WGS_84;

    public SRTM1HgtElevationModelDescriptor() {
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public Datum getDatum() {
        return DATUM;
    }

    @Override
    public int getNumXTiles() {
        return NUM_X_TILES;
    }

    @Override
    public int getNumYTiles() {
        return NUM_Y_TILES;
    }

    @Override
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
    public boolean canBeDownloaded() {
        return true;
    }

    @Override
    public boolean isDemInstalled() {
        return true;
    }

    @Override
    public URL getDemArchiveUrl() {
        return null;
    }

    @Override
    public ElevationModel createDem(Resampling resampling) {
        try {
            return new SRTM1HgtElevationModel(this, resampling);
        } catch (IOException e) {
            return null;
        }
    }

    public String createTileFilename(int minLat, int minLon) {
        final StringBuilder name = new StringBuilder();
        name.append(minLat < 0 ? "S" : "N");
        String latString = String.valueOf(Math.abs(minLat));
        while (latString.length() < 2) {
            latString = '0' + latString;
        }
        name.append(latString);

        name.append(minLon < 0 ? "W" : "E");
        String lonString = String.valueOf(Math.abs(minLon));
        while (lonString.length() < 3) {
            lonString = '0' + lonString;
        }
        name.append(lonString);
        name.append(".hgt.zip");

        return name.toString();
    }

}
