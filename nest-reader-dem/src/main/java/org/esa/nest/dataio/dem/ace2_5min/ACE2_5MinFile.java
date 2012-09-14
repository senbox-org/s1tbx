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
package org.esa.nest.dataio.dem.ace2_5min;

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.nest.dataio.dem.BaseElevationTile;
import org.esa.nest.dataio.dem.ElevationFile;
import org.esa.nest.dataio.dem.ElevationTile;

import java.io.File;
import java.io.IOException;

/**
 * Holds information about a dem file.
 */
public final class ACE2_5MinFile extends ElevationFile {

    private final ACE2_5MinElevationModel demModel;

    private static final String remoteHTTP = "http://nest.s3.amazonaws.com/data/ACE30/";

    public ACE2_5MinFile(final ACE2_5MinElevationModel model, final File localFile, final ProductReader reader) {
        super(localFile,  reader);
        this.demModel = model;
    }

    protected String getRemoteFTP() {
        return null;
    }

    protected String getRemotePath() {
        return null;
    }

    protected ElevationTile createTile(final Product product) {
        final ElevationTile tile = new BaseElevationTile(demModel, product);
        demModel.updateCache(tile);
        return tile;
    }

    protected boolean getRemoteFile() throws IOException{
        return getRemoteHttpFile(remoteHTTP);
    }
}