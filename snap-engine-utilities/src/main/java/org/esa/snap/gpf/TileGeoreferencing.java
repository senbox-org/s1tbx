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
package org.esa.snap.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.*;

/**
 * Handle getting the georefereing for the tile
 */
public final class TileGeoreferencing {

    final TiePointGrid latTPG;
    final TiePointGrid lonTPG;
    final GeoCoding geocoding;
    final int width;
    final int x1;
    final int y1;
    final int size;

    boolean isCached;
    float[] latPixels = null;
    float[] lonPixels = null;
    final boolean isCrossingMeridian;

    public TileGeoreferencing(final Product product, final int x1, final int y1, final int w, final int h) {
        geocoding = product.getGeoCoding();
        isCrossingMeridian = geocoding.isCrossingMeridianAt180();
        latTPG = OperatorUtils.getLatitude(product);
        lonTPG = OperatorUtils.getLongitude(product);
        this.x1 = x1;
        this.y1 = y1;
        width = w;
        size = w * h;

        final boolean isCrsGeoCoding = geocoding instanceof CrsGeoCoding;
        isCached = !(latTPG == null || lonTPG == null) || isCrsGeoCoding;

        try {
            if (isCrsGeoCoding) {
                latPixels = new float[size];
                lonPixels = new float[size];
                ((CrsGeoCoding) geocoding).getPixels(x1, y1, w, h, latPixels, lonPixels);
            } else {
                if (latTPG != null) {
                    latPixels = new float[size];
                    latTPG.getPixels(x1, y1, w, h, latPixels, ProgressMonitor.NULL);
                }

                if (lonTPG != null) {
                    lonPixels = new float[size];
                    lonTPG.getPixels(x1, y1, w, h, lonPixels, ProgressMonitor.NULL);
                }
            }
        } catch (Exception e) {
            System.out.println("TileGeoreferencing tiepoint error " + e.getMessage());
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
