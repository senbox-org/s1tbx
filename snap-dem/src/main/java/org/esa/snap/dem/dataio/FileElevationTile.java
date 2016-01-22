/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.dem.dataio;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.util.CachingObjectArray;
import org.esa.snap.engine_utilities.gpf.TileGeoreferencing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class FileElevationTile {

    private CachingObjectArray linesCache;
    private Product product;
    private static final int maxLines = 500;
    private final List<Integer> indexList = new ArrayList<>(maxLines);
    private boolean useDEMGravitationalModel = true;
    private final EarthGravitationalModel96 egm;
    private final double noDataValue;

    public FileElevationTile(final Product product, final double noDataValue) throws IOException {
        this.product = product;
        this.noDataValue = noDataValue;
        egm = EarthGravitationalModel96.instance();

        linesCache = new CachingObjectArray(getLineFactory());
        linesCache.setCachedRange(0, product.getBandAt(0).getRasterHeight());
    }

    public float getSample(int pixelX, int pixelY) throws IOException {
        final float[] line;
        try {
            line = (float[]) linesCache.getObject(pixelY);
        } catch (Exception e) {
            throw convertLineCacheException(e);
        }
        return line[pixelX];
    }

    public void dispose() {
        clearCache();
        linesCache = null;
        if (product != null) {
            product.dispose();
            product = null;
        }
    }

    public void clearCache() {
        linesCache.clear();
    }

    private CachingObjectArray.ObjectFactory getLineFactory() {
        final Band band = product.getBandAt(0);
        final int width = product.getSceneRasterWidth();
        return new CachingObjectArray.ObjectFactory() {
            public Object createObject(final int pixelY) throws Exception {
                updateCache(pixelY);
                float[] line = band.readPixels(0, pixelY, width, 1, new float[width], ProgressMonitor.NULL);
                if (useDEMGravitationalModel) {
                    addGravitationalModel(pixelY, line);
                }
                return line;
            }
        };
    }

    private void updateCache(int index) {
        indexList.remove((Object) index);
        indexList.add(0, index);
        if (indexList.size() > maxLines) {
            final int i = indexList.size() - 1;
            linesCache.setObject(i, null);
            indexList.remove(i);
        }
    }

    private static IOException convertLineCacheException(Exception e) {
        IOException ioe;
        if (e instanceof IOException) {
            ioe = (IOException) e;
        } else {
            ioe = new IOException();
            ioe.setStackTrace(e.getStackTrace());
        }
        return ioe;
    }

    public void applyEarthGravitionalModel(boolean flag) {
        useDEMGravitationalModel = flag;
    }

    private void addGravitationalModel(final int index, final float[] line) throws Exception {
        final GeoPos geoPos = new GeoPos();
        final TileGeoreferencing tileGeoRef = new TileGeoreferencing(product, 0, index, line.length, 1);
        final double[][] v = new double[4][4];
        for (int i = 0; i < line.length; i++) {
            if (line[i] != noDataValue) {
                tileGeoRef.getGeoPos(i, index, geoPos);
                line[i] += egm.getEGM(geoPos.lat, geoPos.lon, v);
            }
        }
    }
}
