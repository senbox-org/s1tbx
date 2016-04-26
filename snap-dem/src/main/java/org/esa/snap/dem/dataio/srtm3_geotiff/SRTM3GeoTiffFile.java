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
package org.esa.snap.dem.dataio.srtm3_geotiff;

import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.dataop.dem.ElevationFile;
import org.esa.snap.core.dataop.dem.ElevationTile;
import org.esa.snap.engine_utilities.util.Settings;

import java.io.File;
import java.io.IOException;

/**
 * Holds information about a dem file.
 */
public final class SRTM3GeoTiffFile extends ElevationFile {

    private final SRTM3GeoTiffElevationModel demModel;

    private static final String remoteHTTP1 = "http://srtm.csi.cgiar.org/SRT-ZIP/SRTM_V41/SRTM_Data_GeoTiff/";
    private static final String remoteHTTP2 = "http://srtm.geog.kcl.ac.uk/portal/srtm41/srtm_data_geotiff/";
    private static final String remoteHTTP3 = "http://gis-lab.info/data/srtm-tif/";

    private static final String remoteFTP = Settings.instance().get("DEM.srtm3GeoTiffDEM_FTP", "xftp.jrc.it");
    private static final String remotePath = Settings.getPath("DEM.srtm3GeoTiffDEM_remotePath");
    private static final String remoteHTTP = Settings.instance().get("DEM.srtm3GeoTiffDEM_HTTP", remoteHTTP1);

    public SRTM3GeoTiffFile(final SRTM3GeoTiffElevationModel model, final File localFile, final ProductReader reader) {
        super(localFile, reader);
        this.demModel = model;
    }

    protected ElevationTile createTile(final Product product) throws IOException {
        final SRTM3GeoTiffElevationTile tile = new SRTM3GeoTiffElevationTile(demModel, product);
        demModel.updateCache(tile);
        return tile;
    }

    protected Boolean getRemoteFile() throws IOException {
        try {
            return getRemoteHttpFile(remoteHTTP);
        } catch (Exception e) {
            try {
                return getRemoteHttpFile(remoteHTTP1);
            } catch (Exception e2) {
                try {
                    return getRemoteHttpFile(remoteHTTP2);
                } catch (Exception e3) {
                    return getRemoteHttpFile(remoteHTTP3);
                }
            }
        }
    }
}
