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
package org.esa.snap.landcover.dataio;

import org.esa.snap.core.dataio.ProductIOPlugInManager;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.engine_utilities.download.downloadablecontent.DownloadableContent;

import java.io.File;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public abstract class AbstractLandCoverModel implements LandCoverModel, Resampling.Raster {

    private final int NUM_X_TILES;
    protected final int NUM_Y_TILES;
    protected final int NUM_PIXELS_PER_TILE;
    private final double NUM_PIXELS_PER_TILEinv;
    private final double NO_DATA_VALUE;
    protected final int DEGREE_RES;
    private final int RASTER_WIDTH;
    protected final int RASTER_HEIGHT;
    protected final double DEGREE_RES_BY_NUM_PIXELS_PER_TILE;
    protected final double DEGREE_RES_BY_NUM_PIXELS_PER_TILEinv;

    protected final LandCoverModelDescriptor descriptor;
    private final DownloadableContent[][] elevationFiles;
    private final Resampling resampling;
    private final Resampling.Raster resamplingRaster;

    private final List<LandCoverTile> elevationTileCache = new ArrayList<>(20);
    private int maxCacheSize = 60;

    public AbstractLandCoverModel(final LandCoverModelDescriptor descriptor, Resampling resamplingMethod) {
        this.descriptor = descriptor;
        if (resamplingMethod == null)
            resamplingMethod = Resampling.NEAREST_NEIGHBOUR;
        this.resampling = resamplingMethod;
        this.resamplingRaster = this;

        NUM_X_TILES = descriptor.getNumXTiles();
        NUM_Y_TILES = descriptor.getNumYTiles();
        NO_DATA_VALUE = descriptor.getNoDataValue();
        NUM_PIXELS_PER_TILE = descriptor.getPixelRes();
        NUM_PIXELS_PER_TILEinv = 1.0 / (double) NUM_PIXELS_PER_TILE;
        DEGREE_RES = descriptor.getDegreeRes();

        RASTER_WIDTH = NUM_X_TILES * NUM_PIXELS_PER_TILE;
        RASTER_HEIGHT = NUM_Y_TILES * NUM_PIXELS_PER_TILE;

        DEGREE_RES_BY_NUM_PIXELS_PER_TILE = DEGREE_RES / (double) NUM_PIXELS_PER_TILE;
        DEGREE_RES_BY_NUM_PIXELS_PER_TILEinv = 1.0 / DEGREE_RES_BY_NUM_PIXELS_PER_TILE;

        elevationFiles = createLandCoverFiles();    // must be last
    }

    public Resampling getResampling() {
        return resampling;
    }

    public LandCoverModelDescriptor getDescriptor() {
        return descriptor;
    }

    protected void setMaxCacheSize(final int size) {
        maxCacheSize = size;
    }

    public final double getLandCover(final GeoPos geoPos) throws Exception {
        if (geoPos.lon > 180) {
            geoPos.lon -= 360;
        }
        final double pixelY = getIndexY(geoPos);
        if (pixelY < 0) {
            return NO_DATA_VALUE;
        }

        final double elevation;
        //synchronized(resampling) {
        Resampling.Index newIndex = resampling.createIndex();
        resampling.computeIndex(getIndexX(geoPos), pixelY, RASTER_WIDTH, RASTER_HEIGHT, newIndex);
        elevation = resampling.resample(resamplingRaster, newIndex);
        //}
        return Double.isNaN(elevation) ? NO_DATA_VALUE : elevation;
    }

    public abstract double getIndexX(final GeoPos geoPos);

    public abstract double getIndexY(final GeoPos geoPos);

    public abstract GeoPos getGeoPos(final PixelPos pixelPos);

    public PixelPos getIndex(final GeoPos geoPos) {
        return new PixelPos((float) getIndexX(geoPos), (float) getIndexY(geoPos));
    }

    public void dispose() {
        for (LandCoverTile tile : elevationTileCache) {
            if (tile != null)
                tile.dispose();
        }
        elevationTileCache.clear();
        for (DownloadableContent[] elevationFile : elevationFiles) {
            for (DownloadableContent anElevationFile : elevationFile) {
                if (anElevationFile != null)
                    anElevationFile.dispose();
            }
        }
    }

    public int getWidth() {
        return RASTER_WIDTH;
    }

    public int getHeight() {
        return RASTER_HEIGHT;
    }

    public final float getSample(final double pixelX, final double pixelY) throws Exception {
        final int tileXIndex = (int) (pixelX * NUM_PIXELS_PER_TILEinv);
        final int tileYIndex = (int) (pixelY * NUM_PIXELS_PER_TILEinv);
        final LandCoverTile tile = (LandCoverTile) elevationFiles[tileXIndex][tileYIndex].getContentFile();
        if (tile == null) {
            return Float.NaN;
        }
        final float sample = tile.getSample((int) (pixelX - tileXIndex * NUM_PIXELS_PER_TILE),
                (int) (pixelY - tileYIndex * NUM_PIXELS_PER_TILE));

        return sample == ((float) NO_DATA_VALUE) ? Float.NaN : sample;
    }

    private DownloadableContent[][] createLandCoverFiles() {
        final DownloadableContent[][] elevationFiles = new DownloadableContent[NUM_X_TILES][NUM_Y_TILES];
        final File demInstallDir = descriptor.getInstallDir();
        for (int x = 0; x < elevationFiles.length; x++) {
            for (int y = 0; y < elevationFiles[x].length; y++) {

                createLandCoverFile(elevationFiles, x, y, demInstallDir);
            }
        }
        return elevationFiles;
    }

    protected abstract void createLandCoverFile(final DownloadableContent[][] elevationFiles,
                                                final int x, final int y, final File demInstallDir);

    public void updateCache(final LandCoverTile tile) {
        elevationTileCache.remove(tile);
        elevationTileCache.add(0, tile);
        while (elevationTileCache.size() > maxCacheSize) {
            final int index = elevationTileCache.size() - 1;
            final LandCoverTile lastTile = elevationTileCache.get(index);
            if (lastTile != null)
                lastTile.clearCache();
            elevationTileCache.remove(index);
        }
    }

    protected static ProductReaderPlugIn getReaderPlugIn(final String formatName) {
        final Iterator readerPlugIns = ProductIOPlugInManager.getInstance().getReaderPlugIns(formatName);
        return (ProductReaderPlugIn) readerPlugIns.next();
    }

    public final boolean getSamples(final int[] x, final int[] y, final double[][] samples) throws Exception {
        boolean allValid = true;
        for (int i = 0; i < y.length; i++) {
            final int tileYIndex = (int) (y[i] * NUM_PIXELS_PER_TILEinv);
            final int pixelY = y[i] - tileYIndex * NUM_PIXELS_PER_TILE;

            for (int j = 0; j < x.length; j++) {
                final int tileXIndex = (int) (x[j] * NUM_PIXELS_PER_TILEinv);

                final DownloadableContent content = elevationFiles[tileXIndex][tileYIndex];
                if (content == null) {
                    samples[i][j] = Double.NaN;
                    allValid = false;
                    continue;
                }
                final LandCoverTile tile = (LandCoverTile) content.getContentFile();
                if (tile == null) {
                    samples[i][j] = Double.NaN;
                    allValid = false;
                    continue;
                }

                samples[i][j] = tile.getSample(x[j] - tileXIndex * NUM_PIXELS_PER_TILE, pixelY);
                if (samples[i][j] == NO_DATA_VALUE) {
                    samples[i][j] = Double.NaN;
                    allValid = false;
                }
            }
        }
        return allValid;
    }
}