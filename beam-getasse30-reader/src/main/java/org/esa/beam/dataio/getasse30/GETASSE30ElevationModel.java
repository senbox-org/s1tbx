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
package org.esa.beam.dataio.getasse30;

import org.esa.beam.framework.dataio.ProductIOPlugInManager;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.dem.ElevationModel;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.resamp.Resampling;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.bc.ceres.core.Assert;

public class GETASSE30ElevationModel implements ElevationModel, Resampling.Raster {

    public static final int NUM_X_TILES = GETASSE30ElevationModelDescriptor.NUM_X_TILES;
    public static final int NUM_Y_TILES = GETASSE30ElevationModelDescriptor.NUM_Y_TILES;
    public static final int DEGREE_RES = GETASSE30ElevationModelDescriptor.DEGREE_RES;
    public static final int NUM_PIXELS_PER_TILE = GETASSE30ElevationModelDescriptor.PIXEL_RES;
    public static final int NO_DATA_VALUE = GETASSE30ElevationModelDescriptor.NO_DATA_VALUE;
    public static final int RASTER_WIDTH = NUM_X_TILES * NUM_PIXELS_PER_TILE;
    public static final int RASTER_HEIGHT = NUM_Y_TILES * NUM_PIXELS_PER_TILE;

    private final GETASSE30ElevationModelDescriptor descriptor;
    private final GETASSE30ElevationTile[][] elevationTiles;
    private final List<GETASSE30ElevationTile> elevationTileCache;
    private final Resampling resampling;
    private final Resampling.Index resamplingIndex;
    private final Resampling.Raster resamplingRaster;

    public GETASSE30ElevationModel(GETASSE30ElevationModelDescriptor descriptor, Resampling resampling) throws IOException {
        Assert.notNull(descriptor, "descriptor");
        Assert.notNull(resampling, "resampling");
        this.descriptor = descriptor;
        this.resampling = resampling;
        this.resamplingIndex = resampling.createIndex();
        this.resamplingRaster = this;
        this.elevationTiles = createElevationTiles();
        this.elevationTileCache = new ArrayList<GETASSE30ElevationTile>();
    }

    @Override
    public ElevationModelDescriptor getDescriptor() {
        return descriptor;
    }

    @Override
    public float getElevation(GeoPos geoPos) throws Exception {
        float pixelX = (geoPos.lon + 180.0f) / DEGREE_RES * NUM_PIXELS_PER_TILE; // todo (nf) - consider 0.5
        float pixelY = RASTER_HEIGHT - (geoPos.lat + 90.0f) / DEGREE_RES * NUM_PIXELS_PER_TILE; // todo (nf) - consider 0.5, y = (90 - lon) / DEGREE_RES * NUM_PIXELS_PER_TILE;
        final double elevation;
        synchronized (resampling) {
            resampling.computeIndex(pixelX, pixelY,
                                     RASTER_WIDTH,
                                     RASTER_HEIGHT,
                                     resamplingIndex);
            elevation = resampling.resample(resamplingRaster, resamplingIndex);
        }
        if (Double.isNaN(elevation)) {
            return descriptor.getNoDataValue();
        }
        return (float)elevation;
    }

    @Override
    public Resampling getResampling() {
        return resampling;
    }

    @Override
    public void dispose() {
        elevationTileCache.clear();
        for (GETASSE30ElevationTile[] elevationTile : elevationTiles) {
            for (GETASSE30ElevationTile anElevationTile : elevationTile) {
                anElevationTile.dispose();
            }
        }
    }

    @Override
    public int getWidth() {
        return RASTER_WIDTH;
    }

    @Override
    public int getHeight() {
        return RASTER_HEIGHT;
    }

    @Override
    public final boolean getSamples(final int[] x, final int[] y, final double[][] samples) throws Exception {
        boolean allValid = true;
        for (int i = 0; i < y.length; i++) {
            final int tileYIndex = y[i] / NUM_PIXELS_PER_TILE;
            final int pixelY = y[i] - tileYIndex * NUM_PIXELS_PER_TILE;

            for (int j = 0; j < x.length; j++) {
                final int tileXIndex = x[j] / NUM_PIXELS_PER_TILE;

                final GETASSE30ElevationTile tile = getElevationTile(tileXIndex, tileYIndex);
                if (tile == null) {
                    samples[i][j] = Double.NaN;
                    allValid = false;
                    continue;
                }

                samples[i][j] = tile.getSample(x[j] - tileXIndex * NUM_PIXELS_PER_TILE, pixelY);
                if(samples[i][j] == NO_DATA_VALUE) {
                    samples[i][j] = Double.NaN;
                    allValid = false;
                }
            }
        }
        return allValid;
    }

    private GETASSE30ElevationTile[][] createElevationTiles() throws IOException {
        final GETASSE30ElevationTile[][] elevationTiles = new GETASSE30ElevationTile[NUM_X_TILES][NUM_Y_TILES];
        final ProductReaderPlugIn getasse30ReaderPlugIn = getGETASSE30ReaderPlugIn();
        for (int i = 0; i < elevationTiles.length; i++) {
            for (int j = 0; j < elevationTiles[i].length; j++) {
                final ProductReader productReader = getasse30ReaderPlugIn.createReaderInstance();
                final int minLon = i * DEGREE_RES - 180;
                final int minLat = j * DEGREE_RES - 90;
                final Product product = productReader.readProductNodes(descriptor.getTileFile(minLon, minLat), null);
                elevationTiles[i][NUM_Y_TILES - 1 - j] = new GETASSE30ElevationTile(this, product);
            }
        }
        return elevationTiles;
    }

    public void updateCache(GETASSE30ElevationTile tile) {
        elevationTileCache.remove(tile);
        elevationTileCache.add(0, tile);
        while (elevationTileCache.size() > 60) {
            final int index = elevationTileCache.size() - 1;
            GETASSE30ElevationTile lastTile = elevationTileCache.get(index);
            lastTile.clearCache();
            elevationTileCache.remove(index);
        }
    }

    private GETASSE30ElevationTile getElevationTile(final int lonIndex, final int latIndex) {
        return elevationTiles[lonIndex][latIndex];
    }

    private static GETASSE30ReaderPlugIn getGETASSE30ReaderPlugIn() {
        final Iterator readerPlugIns = ProductIOPlugInManager.getInstance().getReaderPlugIns(
                GETASSE30ReaderPlugIn.FORMAT_NAME);
        return (GETASSE30ReaderPlugIn) readerPlugIns.next();
    }
}
