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
package org.esa.nest.dataio.dem;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.util.CachingObjectArray;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

class FileElevationTile {

    private CachingObjectArray linesCache;
    private Product product;
    private static final int maxLines = 500;
    private final List<Integer> indexList = new ArrayList<Integer>(maxLines);

    public FileElevationTile(final Product product) {
        this.product = product;

        linesCache = new CachingObjectArray(getLineFactory());
        linesCache.setCachedRange(0, product.getBandAt(0).getSceneRasterHeight());
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
            public Object createObject(final int index) throws Exception {
                updateCache(index);
                return band.readPixels(0, index, width, 1, new float[width], ProgressMonitor.NULL);
            }
        };
    }

     private void updateCache(int index) {
        indexList.remove((Object)index);
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
}