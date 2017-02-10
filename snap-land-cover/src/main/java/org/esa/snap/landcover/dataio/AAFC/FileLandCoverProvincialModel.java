/*
 * Copyright (C) 2017 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.snap.landcover.dataio.AAFC;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.landcover.dataio.FileLandCoverModel;
import org.esa.snap.landcover.dataio.FileLandCoverTile;
import org.esa.snap.landcover.dataio.LandCoverModelDescriptor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class FileLandCoverProvincialModel extends FileLandCoverModel {

    private List<TileData> tileDataList = new ArrayList<>();

    public FileLandCoverProvincialModel(final LandCoverModelDescriptor descriptor, final File[] files,
                                        final Resampling resamplingMethod) throws IOException {
        super(descriptor, files, resamplingMethod);

        tileDataList.add(new TileData(53.9112841586466, -114.73990703906945, files[0]));
        tileDataList.add(new TileData(52.914088704678846, -122.32311551754088, files[1]));
        tileDataList.add(new TileData(51.5524862030492, -98.40075508982784, files[2]));
        tileDataList.add(new TileData(46.53620643706265, -66.52326514628892, files[3]));
        tileDataList.add(new TileData(48.79890293939873, -55.115977591094314, files[4]));
        tileDataList.add(new TileData(45.31183706258599, -63.035088294228345, files[5]));
        tileDataList.add(new TileData(46.192169447119376, -84.6911456002628, files[6]));
        tileDataList.add(new TileData(46.5092422478255, -63.28092434982391, files[7]));
        tileDataList.add(new TileData(47.870066633078366, -72.51820504485306, files[8]));
        tileDataList.add(new TileData(52.20834616203186, -106.27851934747717, files[9]));
    }

    public synchronized double getLandCover(final GeoPos geoPos) throws Exception {
        try {
            TileData[] tileData = getTileData(geoPos);
            if (tileData[0].tile == null) {
                tileData[0].tile = new FileLandCoverTile(this, tileData[0].file, productReaderPlugIn.createReaderInstance());
            }
            FileLandCoverTile tile = tileData[0].tile;
            if (tile != null && tile.getTileGeocoding() != null) {
                PixelPos pix = tile.getTileGeocoding().getPixelPos(geoPos, null);
                if (!pix.isValid() || pix.x < 0 || pix.y < 0 || pix.x >= tile.getWidth() || pix.y >= tile.getHeight()) {
                    // try second closest
                    if (tileData[1].tile == null) {
                        tileData[1].tile = new FileLandCoverTile(this, tileData[1].file, productReaderPlugIn.createReaderInstance());
                    }
                    tile = tileData[1].tile;
                    if (tile != null && tile.getTileGeocoding() != null) {
                        pix = tile.getTileGeocoding().getPixelPos(geoPos, null);
                        if (!pix.isValid() || pix.x < 0 || pix.y < 0 || pix.x >= tile.getWidth() || pix.y >= tile.getHeight()) {
                            return tile.getNoDataValue();
                        }
                    }
                }

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

    private TileData[] getTileData(final GeoPos geoPos) throws IOException {

        double lat = geoPos.lat + 90;
        double lon = geoPos.lon + 180;
        TileData first = tileDataList.get(0);
        TileData second = first;
        double minDiff = Double.MAX_VALUE;

        for (TileData tileData : tileDataList) {
            double diff = Math.abs(lon - tileData.lon);
            if (diff < minDiff) {
                second = first;
                first = tileData;
                minDiff = diff;
            }
        }

        return new TileData[]{first, second};
    }

    private static class TileData {
        final double lat;
        final double lon;
        final File file;
        FileLandCoverTile tile;

        TileData(double lat, double lon, File file) {
            this.lat = lat + 90;
            this.lon = lon + 180;
            this.file = file;
        }
    }
}