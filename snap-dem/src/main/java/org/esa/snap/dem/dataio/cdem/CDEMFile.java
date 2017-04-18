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
package org.esa.snap.dem.dataio.cdem;

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
public final class CDEMFile extends ElevationFile {

    private final CDEMElevationModel demModel;

    private static final String remoteHTTP1 = "http://step.esa.int/auxdata/dem/cdem/";
    private static final String remoteHTTP = Settings.instance().get("DEM.CDEM_HTTP", remoteHTTP1);

    public CDEMFile(final CDEMElevationModel model, final File localFile, final ProductReader reader) {
        super(localFile, reader);
        this.demModel = model;
    }

    protected ElevationTile createTile(final Product product) throws IOException {
        return new CDEMElevationTile(demModel, product);
    }

    protected Boolean getRemoteFile() throws IOException {
        return getRemoteHttpFile(remoteHTTP);
    }

    public static String getRemoteHTTP() {
        return remoteHTTP;
    }
}
