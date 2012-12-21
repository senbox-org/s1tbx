/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.dem.aster;

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.util.io.FileUtils;
import org.esa.nest.dataio.dem.ElevationFile;
import org.esa.nest.dataio.dem.ElevationTile;

import java.io.File;
import java.io.IOException;

/**
 * Holds information about a dem file.
 */
public final class AsterFile extends ElevationFile {

    private final AsterElevationModel demModel;

    public AsterFile(final AsterElevationModel model, final File localFile, final ProductReader reader) {
        super(localFile, reader);
        this.demModel = model;
    }

    protected String getRemoteFTP() {
        return null;
    }

    protected String getRemotePath() {
        return null;    
    }

    protected boolean getRemoteFile() throws IOException {
        remoteFileExists = false;
        return false;
    }

    protected ElevationTile createTile(final Product product) {
        final AsterElevationTile tile = new AsterElevationTile(demModel, product);
        demModel.updateCache(tile);
        return tile;
    }

    protected boolean findLocalFile() {
        if (localFile.exists() && localFile.isFile() && localFile.length() > 0) {
            return true;
        } else {
            final String name = FileUtils.getFilenameWithoutExtension(localFile.getName());
            // check for version 2
            final String v2Name = name.replace("ASTGTM", "ASTGTM2");
            final File v2File = new File(localFile.getParentFile(), v2Name+".zip");
            if(v2File.exists()) {
                localFile = new File(localFile.getParentFile(), v2Name+"_dem.tif");
                return true;
            } else {
                // check if unzipped
                final File unzipFile = new File(localFile.getParentFile(), name+"_dem.tif");
                if(unzipFile.exists()) {
                    localFile = unzipFile;
                    return true;
                } else {
                    final File v2UnzipFile = new File(localFile.getParentFile(), v2Name+"_dem.tif");
                    if(v2UnzipFile.exists()) {
                        localFile = v2UnzipFile;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    protected File getFileFromZip(File dataFile) throws IOException {
        if(!dataFile.exists())  {
            final String v2Name = dataFile.getName().replace("ASTGTM", "ASTGTM2");
            dataFile = new File(dataFile.getParentFile(), v2Name);
        }
        return super.getFileFromZip(dataFile);
    }
}