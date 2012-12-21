/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.dem.aster;

import com.bc.io.FileUnpacker;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.dataio.dem.BaseElevationModel;
import org.esa.nest.dataio.dem.ElevationFile;

import java.awt.*;
import java.io.File;

public final class AsterElevationModel extends BaseElevationModel {

    private static final ProductReaderPlugIn productReaderPlugIn = getReaderPlugIn("GeoTIFF");

    public AsterElevationModel(final AsterElevationModelDescriptor descriptor, final Resampling resamplingMethod) {
        super(descriptor, resamplingMethod);

        unpackTileBundles();

        setMaxCacheSize(12);
    }

    @Override
    public double getIndexX(final GeoPos geoPos) {
        return ((geoPos.lon + 180.0) / DEGREE_RES_BY_NUM_PIXELS_PER_TILE);
    }

    @Override
    public double getIndexY(final GeoPos geoPos) {
        return (RASTER_HEIGHT - (geoPos.lat + 83.0) / DEGREE_RES_BY_NUM_PIXELS_PER_TILE);
    }

    @Override
    public GeoPos getGeoPos(final PixelPos pixelPos) {
        final double pixelLat = (RASTER_HEIGHT - pixelPos.y) / DEGREE_RES_BY_NUM_PIXELS_PER_TILE - 83.0;
        final double pixelLon = pixelPos.x * DEGREE_RES_BY_NUM_PIXELS_PER_TILE - 180.0;
        return new GeoPos((float)pixelLat, (float)pixelLon);
    }

    @Override
    protected void createElevationFile(final ElevationFile[][] elevationFiles,
                                       final int x, final int y, final File demInstallDir) {
        final int minLon = x * DEGREE_RES - 180;
        final int minLat = y * DEGREE_RES - 83;
        final String fileName = descriptor.createTileFilename(minLat, minLon);
        final File localFile = new File(demInstallDir, fileName);
        elevationFiles[x][NUM_Y_TILES - 1 - y] = new AsterFile(this, localFile, productReaderPlugIn.createReaderInstance());
    }

    private void unpackTileBundles() {

        final File parentFolder = descriptor.getDemInstallDir();
        final File[] files = parentFolder.listFiles();

        try {
            for(File f : files) {
                final String name = f.getName().toLowerCase();
                if(name.startsWith("tiles_") && name.endsWith(".zip")) {
                    Component component = null;
                    if(VisatApp.getApp() != null) {
                        component = VisatApp.getApp().getApplicationWindow();
                    }
                    FileUnpacker.unpackZip(f, parentFolder, component);
                    f.delete();
                }
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }
}