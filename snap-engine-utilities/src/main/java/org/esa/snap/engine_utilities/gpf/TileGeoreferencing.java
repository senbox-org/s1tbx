/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.engine_utilities.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.CrsGeoCoding;
import org.esa.snap.core.datamodel.GeoCoding;
import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.util.SystemUtils;

/**
 * Handle getting the georefereing for the tile
 */
public final class TileGeoreferencing {

    private final GeoCoding geocoding;
    private final int width, size;
    private final int x1, y1;

    private boolean isCached;
    private double[] latPixels = null;
    private double[] lonPixels = null;
    private final boolean isCrossingMeridian;

    public TileGeoreferencing(final Product product, final int x1, final int y1, final int w, final int h) {
        geocoding = product.getSceneGeoCoding();
        isCrossingMeridian = geocoding.isCrossingMeridianAt180();
        final TiePointGrid latTPG = OperatorUtils.getLatitude(product);
        final TiePointGrid lonTPG = OperatorUtils.getLongitude(product);
        this.x1 = x1;
        this.y1 = y1;
        width = w;
        size = w * h;

        final boolean isCrsGeoCoding = geocoding instanceof CrsGeoCoding;
        isCached = !(latTPG == null || lonTPG == null) || isCrsGeoCoding;

        try {
            if (isCrsGeoCoding) {
                latPixels = new double[size];
                lonPixels = new double[size];
                ((CrsGeoCoding) geocoding).getPixels(x1, y1, w, h, latPixels, lonPixels);
            } else {
                if (latTPG != null) {
                    latPixels = new double[size];
                    latTPG.getPixels(x1, y1, w, h, latPixels, ProgressMonitor.NULL);
                }

                if (lonTPG != null) {
                    lonPixels = new double[size];
                    lonTPG.getPixels(x1, y1, w, h, lonPixels, ProgressMonitor.NULL);
                }
            }
        } catch (Exception e) {
            SystemUtils.LOG.severe("TileGeoreferencing tiepoint error " + e.getMessage());
            isCached = false;
        }
    }

    public void getGeoPos(final int x, final int y, final GeoPos geo) {

        if (isCached) {
            final int xx = x - x1;
            final int yy = y - y1;
            if (xx >= 0 && yy >= 0) {
                final int pos = yy * width + xx;
                if (pos < size) {
                    geo.setLocation(latPixels[pos], lonPixels[pos]);
                    return;
                }
            }
        }
        geocoding.getGeoPos(new PixelPos(x + 0.5f, y + 0.5f), geo);
    }

    public void getGeoPos(final PixelPos pix, final GeoPos geo) {

        if (isCached) {
            final int xx = (int) pix.getX() - x1;
            final int yy = (int) pix.getY() - y1;
            final int pos = yy * width + xx;
            if (xx >= 0 && yy >= 0 && pos < size) {
                geo.setLocation(latPixels[pos], lonPixels[pos]);
                return;
            }
        }
        geocoding.getGeoPos(pix, geo);
    }

    public void getPixelPos(final GeoPos geo, final PixelPos pix) {
        if (isCrossingMeridian && geo.lon < 0) {
            geo.lon += 360;
        }
        geocoding.getPixelPos(geo, pix);
    }
}
