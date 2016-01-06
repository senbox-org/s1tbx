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
package org.esa.snap.landcover.dataio.AAFC;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.engine_utilities.util.Settings;
import org.esa.snap.landcover.dataio.AbstractLandCoverModelDescriptor;
import org.esa.snap.landcover.dataio.FileLandCoverModel;
import org.esa.snap.landcover.dataio.LandCoverModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class AAFCSandHRModelDescriptor extends AbstractLandCoverModelDescriptor {

    public static final String NAME = "AAFC Canada Sand Pct";

    private static final File INSTALL_DIR = new File(Settings.instance().getAuxDataFolder().getAbsolutePath(),
            "LandCover" + File.separator + "AAFC");
    private static final File file = new File(INSTALL_DIR, "AAFC_Canada_SAND_HR_pct.zip");

    public AAFCSandHRModelDescriptor() {
        remotePath = "http://step.esa.int/auxdata/landcover/AAFC/";
        name = NAME;
        installDir = INSTALL_DIR;
        metadataFileName = "soil_landscapes_canada_readme.txt";
        final Path moduleBasePath = ResourceInstaller.findModuleCodeBasePath(this.getClass());
        metadataSrcPath = moduleBasePath.resolve("org/esa/snap/landcover/auxdata/aafc/soil_landscapes_canada/");
        NO_DATA_VALUE = -9999d;
    }

    @Override
    public LandCoverModel createLandCoverModel(final Resampling resampling) throws IOException {
        return new FileLandCoverModel(this, new File[]{file}, resampling);
    }

    public String createTileFilename(final int minLat, final int minLon) {
        return file.getName();
    }

    @Override
    public int getDataType() {
        return ProductData.TYPE_FLOAT32;
    }
}
