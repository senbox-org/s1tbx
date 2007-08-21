/*
 * $Id: GETASSE30ElevationModel.java,v 1.1 2006/09/14 13:19:16 marcop Exp $
 *
 * Copyright (C) 2002 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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

public class GETASSE30ElevationModel implements ElevationModel, Resampling.Raster {

    public static final int NUM_X_TILES = GETASSE30ElevationModelDescriptor.NUM_X_TILES;
    public static final int NUM_Y_TILES = GETASSE30ElevationModelDescriptor.NUM_Y_TILES;
    public static final int DEGREE_RES = GETASSE30ElevationModelDescriptor.DEGREE_RES;
    public static final int NUM_PIXELS_PER_TILE = GETASSE30ElevationModelDescriptor.PIXEL_RES;
    public static final int NO_DATA_VALUE = GETASSE30ElevationModelDescriptor.NO_DATA_VALUE;
    public static final int RASTER_WIDTH = NUM_X_TILES * NUM_PIXELS_PER_TILE;
    public static final int RASTER_HEIGHT = NUM_Y_TILES * NUM_PIXELS_PER_TILE;

    private final GETASSE30ElevationModelDescriptor _descriptor;
    private final GETASSE30ElevationTile[][] _elevationTiles;
    private final List _elevationTileCache;
    private final Resampling _resampling;
    private final Resampling.Index _resamplingIndex;
    private final Resampling.Raster _resamplingRaster;

    public GETASSE30ElevationModel(GETASSE30ElevationModelDescriptor descriptor) throws IOException {
        _descriptor = descriptor;
        _resampling = Resampling.BILINEAR_INTERPOLATION;
        _resamplingIndex = _resampling.createIndex();
        _resamplingRaster = this;
        _elevationTiles = createEleveationTiles();
        _elevationTileCache = new ArrayList();
    }

    public ElevationModelDescriptor getDescriptor() {
        return _descriptor;
    }

    public float getElevation(GeoPos geoPos) throws Exception {
        float pixelX = (geoPos.lon + 180.0f) / DEGREE_RES * NUM_PIXELS_PER_TILE; // todo (nf) - consider 0.5
        float pixelY = RASTER_HEIGHT - (geoPos.lat + 90.0f) / DEGREE_RES * NUM_PIXELS_PER_TILE; // todo (nf) - consider 0.5, y = (90 - lon) / DEGREE_RES * NUM_PIXELS_PER_TILE;
        _resampling.computeIndex(pixelX, pixelY,
                                 RASTER_WIDTH,
                                 RASTER_HEIGHT,
                                 _resamplingIndex);

        final float elevation = _resampling.resample(_resamplingRaster, _resamplingIndex);
        if (Float.isNaN(elevation)) {
            return _descriptor.getNoDataValue();
        }
        return elevation;
    }

    public void dispose() {
        _elevationTileCache.clear();
        for (int i = 0; i < _elevationTiles.length; i++) {
            for (int j = 0; j < _elevationTiles[i].length; j++) {
                _elevationTiles[i][j].dispose();
            }
        }
    }

    public int getWidth() {
        return RASTER_WIDTH;
    }

    public int getHeight() {
        return RASTER_HEIGHT;
    }

    public float getSample(int pixelX, int pixelY) throws IOException {
        final int tileXIndex = pixelX / NUM_PIXELS_PER_TILE;
        final int tileYIndex = pixelY / NUM_PIXELS_PER_TILE;
        final GETASSE30ElevationTile tile = getElevationTile(tileXIndex, tileYIndex);
        final int tileX = pixelX - tileXIndex * NUM_PIXELS_PER_TILE;
        final int tileY = pixelY - tileYIndex * NUM_PIXELS_PER_TILE;
        final float sample = tile.getSample(tileX, tileY);
        if (sample == _descriptor.getNoDataValue()) {
            return Float.NaN;
        }
        return sample;
    }

    private GETASSE30ElevationTile[][] createEleveationTiles() throws IOException {
        final GETASSE30ElevationTile[][] elevationTiles = new GETASSE30ElevationTile[NUM_X_TILES][NUM_Y_TILES];
        final ProductReaderPlugIn getasse30ReaderPlugIn = getGETASSE30ReaderPlugIn();
        for (int i = 0; i < elevationTiles.length; i++) {
            for (int j = 0; j < elevationTiles[i].length; j++) {
                final ProductReader productReader = getasse30ReaderPlugIn.createReaderInstance();
                final int minLon = i * DEGREE_RES - 180;
                final int minLat = j * DEGREE_RES - 90;
                final Product product = productReader.readProductNodes(_descriptor.getTileFile(minLon, minLat), null);
                elevationTiles[i][NUM_Y_TILES - 1 - j] = new GETASSE30ElevationTile(this, product);
            }
        }
        return elevationTiles;
    }

    public void updateCache(GETASSE30ElevationTile tile) {
        _elevationTileCache.remove(tile);
        _elevationTileCache.add(0, tile);
        while (_elevationTileCache.size() > 60) {
            final int index = _elevationTileCache.size() - 1;
            GETASSE30ElevationTile lastTile = (GETASSE30ElevationTile) _elevationTileCache.get(index);
            lastTile.clearCache();
            _elevationTileCache.remove(index);
        }
    }

    private GETASSE30ElevationTile getElevationTile(final int lonIndex, final int latIndex) {
        return _elevationTiles[lonIndex][latIndex];
    }

    private static GETASSE30ReaderPlugIn getGETASSE30ReaderPlugIn() {
        final Iterator readerPlugIns = ProductIOPlugInManager.getInstance().getReaderPlugIns(
                GETASSE30ReaderPlugIn.FORMAT_NAME);
        return (GETASSE30ReaderPlugIn) readerPlugIns.next();
    }
}
