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

    private static final String remoteHTTP1 = "http://srtm.csi.cgiar.org/wp-content/uploads/files/srtm_5x5/TIFF/";
    private static final String remoteHTTP2 = "http://cgiar-csi-srtm.openterrain.org.s3.amazonaws.com/source/";

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
            boolean found = getRemoteHttpFile(remoteHTTP);
            if(!found) {
                found = getRemoteHttpFile(remoteHTTP1);
                if(!found) {
                    found = getRemoteHttpFile(remoteHTTP2);
                }
            }
            return found;
        } catch (Exception e) {
            try {
                return getRemoteHttpFile(remoteHTTP1);
            } catch (Exception e2) {
                return getRemoteHttpFile(remoteHTTP2);
            }
        }
    }
}
