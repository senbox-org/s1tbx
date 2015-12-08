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

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.resamp.Resampling;
import uk.me.jstott.jcoord.LatLng;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FileLandCoverUTMModel extends FileLandCoverModel {

    private Map<Integer, FileLandCoverTile> zoneTileMap = new HashMap();

    public FileLandCoverUTMModel(final LandCoverModelDescriptor descriptor, final File[] files,
                                 final Resampling resamplingMethod) throws IOException {
        super(descriptor, files, resamplingMethod);
    }

    public synchronized double getLandCover(final GeoPos geoPos) throws Exception {
        try {
            final LatLng ll = new LatLng(geoPos.lat, geoPos.lon);
            final int zone = ll.toUTMRef().getLngZone();

            FileLandCoverTile tile = zoneTileMap.get(zone);
            if (tile == null) {
                tile = loadProduct(zone);
            }
            if (tile != null && tile.getTileGeocoding() != null) {
                final PixelPos pix = tile.getTileGeocoding().getPixelPos(geoPos, null);
                if (!pix.isValid() || pix.x < 0 || pix.y < 0 || pix.x >= tile.getWidth() || pix.y >= tile.getHeight())
                    return tile.getNoDataValue();

                resampling.computeIndex(pix.x, pix.y, tile.getWidth(), tile.getHeight(), resamplingIndex);

                final double value = resampling.resample(tile, resamplingIndex);
                if (Double.isNaN(value)) {
                    return tile.getNoDataValue();
                }
                return value;
            }
            return descriptor.getNoDataValue();
        } catch (Exception e) {
            throw new Exception("Problem reading : " + e.getMessage());
        }
    }

    private FileLandCoverTile loadProduct(final int zone) throws Exception {
        for (File file : fileList) {
            if (file.getName().contains("UTM" + zone)) {
                final FileLandCoverTile tile = new FileLandCoverTile(this, file, productReaderPlugIn.createReaderInstance());
                zoneTileMap.put(zone, tile);
                tileList = zoneTileMap.values().toArray(new FileLandCoverTile[zoneTileMap.size()]);
                return tile;
            }
        }
        return null;
    }
}