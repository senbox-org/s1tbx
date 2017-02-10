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
package org.esa.snap.landcover.dataio.MODIS_VCF;

import org.esa.snap.core.datamodel.GeoPos;
import org.esa.snap.core.datamodel.PixelPos;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.landcover.dataio.FileLandCoverModel;
import org.esa.snap.landcover.dataio.FileLandCoverTile;
import org.esa.snap.landcover.dataio.LandCoverModelDescriptor;
import uk.me.jstott.jcoord.LatLng;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

public class FileLandCoverUTMZoneLatBandModel extends FileLandCoverModel {

    // Map 'F' and 17 to "FE1718". 'F' is the lat band. 17 is the UTM zone.
    private HashMap<Character, HashMap<Integer, String>> latLonZonesMap = new HashMap<>();

    // Map "FE1718" to the tile
    private Map<String, FileLandCoverTile> tileMap = new HashMap<>();

    public FileLandCoverUTMZoneLatBandModel(final LandCoverModelDescriptor descriptor, final File[] files,
                                            final Resampling resamplingMethod) throws IOException {
        super(descriptor, files, resamplingMethod);
        buildLatBandAndLonZoneIDMaps();
    }

    public synchronized double getLandCover(final GeoPos geoPos) throws Exception {
        try {
            final LatLng ll = new LatLng(geoPos.lat, geoPos.lon);
            final char latBandID = getLatBand(ll.getLat());
            final int lonZoneID = ll.toUTMRef().getLngZone();
            final String tileID = getTileID(latBandID, lonZoneID);

            if (tileID != null) {
                FileLandCoverTile tile = tileMap.get(tileID);
                if (tile == null) {
                    tile = loadProduct(tileID);
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
            }
            return descriptor.getNoDataValue();
        } catch (Exception e) {
            throw new Exception("Problem reading : " + e.getMessage());
        }
    }

    private FileLandCoverTile loadProduct(final String tileID) throws Exception {
        // If doing a linear search through fileList is slow, may be we can map "E" and "17" directly to the File.
        for (File file : fileList) {
            if (file.getName().contains(tileID)) {
                final FileLandCoverTile tile = new FileLandCoverTile(this, file, productReaderPlugIn.createReaderInstance());
                tileMap.put(tileID, tile);
                tileList = tileMap.values().toArray(new FileLandCoverTile[tileMap.size()]);
                return tile;
            }
        }
        return null;
    }

    private String getTileID(final char latBandID, final int lonZoneID) {
        final HashMap<Integer, String> map = latLonZonesMap.get(latBandID);
        return (map == null) ? null : map.get(lonZoneID);
    }

    private void buildLatBandAndLonZoneIDMaps() {
        for (File f : fileList) {
            final String tileID = f.getName().substring(19, 25);
            for (int i = 0; i < 2; i++) {
                for (int j = 0; j < 4; j += 2) {
                    final Character latBandID = tileID.charAt(i);
                    final int zoneID = Integer.parseInt(tileID.substring(2 + j, 4 + j));
                    HashMap<Integer, String> map = latLonZonesMap.get(latBandID);
                    if (map == null) {
                        map = new HashMap<>();
                        latLonZonesMap.put(latBandID, map);
                    }
                    map.put(zoneID, tileID);
                    //System.out.println("FileLandCoverUTMZoneLatBandModel.buildLatBandAndLonZoneIDMaps: added (" + latBandID + "," + zoneID + ") -> " + tileID);
                }
            }
        }
    }

    private static char getLatBand(final double lat) {

        if (lat < -80d || lat > 84d) {
            return ' ';
        } else if (lat >= 72d) {
            return 'X'; // [72, 84]
        }

        if (lat < -32d) { // [-80, -32)
            final int tmp = ((int) (lat + 80d)) / 8;
            return (char) (tmp + (int) ('C'));
        } else if (lat < 8d) { // [-32, 8)
            final int tmp = ((int) (lat + 32d)) / 8;
            return (char) (tmp + (int) ('J'));
        } else { // [8, 72)
            final int tmp = ((int) (lat - 8d)) / 8;
            return (char) (tmp + (int) ('P'));
        }
    }
}