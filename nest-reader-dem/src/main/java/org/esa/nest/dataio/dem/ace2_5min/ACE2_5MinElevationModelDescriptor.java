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
package org.esa.nest.dataio.dem.ace2_5min;

import com.bc.ceres.swing.progress.ProgressMonitorSwingWorker;
import com.bc.io.FileDownloader;
import com.bc.io.FileUnpacker;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.dataop.dem.AbstractElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.maptransf.Datum;
import org.esa.beam.framework.dataop.resamp.Resampling;
import org.esa.beam.visat.VisatApp;
import org.esa.nest.util.Settings;

import java.awt.*;
import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

public class ACE2_5MinElevationModelDescriptor extends AbstractElevationModelDescriptor {

    private static final String NAME = "ACE2_5Min";
    private static final String DB_FILE_SUFFIX = "_5M.ACE2";
    private static final String ARCHIVE_URL_PATH = "http://nest.s3.amazonaws.com/data/5M_HEIGHTS.zip";
    public static final int NUM_X_TILES = 24;
    public static final int NUM_Y_TILES = 12;
    public static final int DEGREE_RES = 15;
    public static final int PIXEL_RES = 180;
    public static final int NO_DATA_VALUE = -500;
    public static final GeoPos RASTER_ORIGIN = new GeoPos(90.0f, 180.0f);
    public static final int RASTER_WIDTH = NUM_X_TILES * PIXEL_RES;
    public static final int RASTER_HEIGHT = NUM_Y_TILES * PIXEL_RES;
    private static final Datum DATUM = Datum.WGS_84;

    private File aceDemInstallDir = null;

    public ACE2_5MinElevationModelDescriptor() {
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
    public File getDemInstallDir() {
        if(aceDemInstallDir == null) {
            final String path = Settings.instance().get("DEM/ace2_5MinDEMDataPath");
            aceDemInstallDir = new File(path);
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
        if (!isDemInstalled()) {
            installDemFiles(null);
        }
        return new ACE2_5MinElevationModel(this, Resampling.NEAREST_NEIGHBOUR);
    }

    public ElevationModel createDem(Resampling resamplingMethod) {
        if (!isDemInstalled()) {
            installDemFiles(null);
        }
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

    @Override
    public synchronized boolean installDemFiles(Object uiComponent) {
        if (isDemInstalled()) {
            return true;
        }
        if (isInstallingDem()) {
            return true;
        }
        final Component parent = uiComponent instanceof Component ? (Component) uiComponent : null;

        final File demInstallDir = getDemInstallDir();
        if (!demInstallDir.exists()) {
            final boolean success = demInstallDir.mkdirs();
            if (!success) {
                return false;
            }
        }

        try {
            final VisatApp visatApp = VisatApp.getApp();
            if(visatApp != null) {
                visatApp.setStatusBarMessage("Downloading ACE2 5Min DEM...");
            }

            final File archiveFile = FileDownloader.downloadFile(getDemArchiveUrl(), demInstallDir, parent);
            FileUnpacker.unpackZip(archiveFile, demInstallDir, parent);
            archiveFile.delete();

            if(visatApp != null) {
                visatApp.setStatusBarMessage("");
            }
        } catch(Exception e) {
            return false;
        }
        return true;
    }

    private void installWithProgressMonitor(final Component parent) {
        final ProgressMonitorSwingWorker worker = new ProgressMonitorSwingWorker(VisatApp.getApp().getMainFrame(),
                "Installing Ace2 5min DEM...") {
            @Override
            protected Object doInBackground(com.bc.ceres.core.ProgressMonitor pm) throws Exception {

                pm.beginTask("Installing Ace2 5min DEM", 3);
                try {
                    final URL archiveUrl = getDemArchiveUrl();
                    final File demInstallDir = getDemInstallDir();

                    final File archiveFile = FileDownloader.downloadFile(archiveUrl, demInstallDir, parent);
                    pm.worked(1);
                    FileUnpacker.unpackZip(archiveFile, demInstallDir, parent);
                    pm.worked(1);
                    archiveFile.delete();
                    pm.worked(1);
                } catch(Exception e) {
                    System.out.println(e.getMessage());
                    return false;
                } finally {
                    pm.done();
                }
                return true;
            }
        };
        worker.executeWithBlocking();
    }
}