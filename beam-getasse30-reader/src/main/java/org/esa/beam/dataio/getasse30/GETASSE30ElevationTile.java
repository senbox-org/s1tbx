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

import com.bc.ceres.core.ProgressMonitor;
import com.bc.util.CachingObjectArray;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;

import java.io.IOException;

public class GETASSE30ElevationTile {

    private GETASSE30ElevationModel _dem;
    private CachingObjectArray _linesCache;
    private Product _product;

    public GETASSE30ElevationTile(final GETASSE30ElevationModel dem, final Product product) {
        _dem = dem;
        _product = product;

        _linesCache = new CachingObjectArray(getLineFactory());
        _linesCache.setCachedRange(0, product.getSceneRasterHeight());
    }

    public float getSample(int pixelX, int pixelY) throws IOException {
        final float[] line;
        try {
            line = (float[]) _linesCache.getObject(pixelY);
        } catch (Exception e) {
            throw convertLineCacheException(e);
        }
        return line[pixelX];
    }

    public void dispose() {
        clearCache();
        _linesCache = null;
        if (_product != null) {
            _product.dispose();
            _product = null;
        }
        _dem = null;
    }

    public void clearCache() {
        _linesCache.clear();
    }

    private CachingObjectArray.ObjectFactory getLineFactory() {
        final Band band = _product.getBandAt(0);
        final int width = _product.getSceneRasterWidth();
        return new CachingObjectArray.ObjectFactory() {
            public Object createObject(int index) throws Exception {
                _dem.updateCache(GETASSE30ElevationTile.this);
                return band.readPixels(0, index, width, 1, new float[width], ProgressMonitor.NULL);
            }
        };
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
