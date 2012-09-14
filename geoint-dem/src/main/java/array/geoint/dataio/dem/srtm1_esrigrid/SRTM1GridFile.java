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
package array.geoint.dataio.dem.srtm1_esrigrid;

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.nest.dataio.dem.ElevationFile;
import org.esa.nest.dataio.dem.ElevationTile;
import org.esa.nest.util.Settings;
import org.esa.nest.util.ftpUtils;

import java.io.File;
import java.io.IOException;

/**
 * Holds information about a dem file.
 */
public final class SRTM1GridFile extends ElevationFile {

    private final SRTM1GridElevationModel demModel;

    private static final String remoteFTP = "";
    private static final String remotePath = "";
    
    public SRTM1GridFile(SRTM1GridElevationModel model, File localFile, ProductReader reader) {
        super(localFile,  reader);
        this.demModel = model;
    }

    protected String getRemoteFTP() {
        return remoteFTP;
    }

    protected String getRemotePath() {
        return remotePath;
    }

    protected ElevationTile createTile(final Product product) {
        final SRTM1GridElevationTile tile = new SRTM1GridElevationTile(demModel, product);
        demModel.updateCache(tile);
        return tile;
    }

    protected boolean getRemoteFile() throws IOException {
        return false;
    }
}