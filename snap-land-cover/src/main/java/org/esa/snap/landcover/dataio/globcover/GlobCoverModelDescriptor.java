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
package org.esa.snap.landcover.dataio.globcover;

import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.engine_utilities.util.Settings;
import org.esa.snap.landcover.dataio.AbstractLandCoverModelDescriptor;
import org.esa.snap.landcover.dataio.FileLandCoverModel;
import org.esa.snap.landcover.dataio.LandCoverModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class GlobCoverModelDescriptor extends AbstractLandCoverModelDescriptor {

    public static final String NAME = "GlobCover";

    private static final File INSTALL_DIR = new File(Settings.instance().getAuxDataFolder().getAbsolutePath(),
            "LandCover" + File.separator + NAME);
    private static final File[] fileList = new File[]{
            new File(INSTALL_DIR, "globecover_1.tif.zip"),          //todo replace with coordinates
            new File(INSTALL_DIR, "globecover_2.tif.zip"),
            new File(INSTALL_DIR, "globecover_3.tif.zip"),
            new File(INSTALL_DIR, "globecover_4.tif.zip"),
            new File(INSTALL_DIR, "globecover_5.tif.zip"),
            new File(INSTALL_DIR, "globecover_6.tif.zip"),
            new File(INSTALL_DIR, "globecover_7.tif.zip"),
            new File(INSTALL_DIR, "globecover_8.tif.zip"),
            new File(INSTALL_DIR, "globecover_9.tif.zip"),
            new File(INSTALL_DIR, "globecover_10.tif.zip"),
            new File(INSTALL_DIR, "globecover_11.tif.zip"),
            new File(INSTALL_DIR, "globecover_12.tif.zip"),
            new File(INSTALL_DIR, "globecover_13.tif.zip"),
            new File(INSTALL_DIR, "globecover_14.tif.zip"),
            new File(INSTALL_DIR, "globecover_15.tif.zip"),
            new File(INSTALL_DIR, "globecover_16.tif.zip"),
            new File(INSTALL_DIR, "globecover_17.tif.zip"),
            new File(INSTALL_DIR, "globecover_18.tif.zip"),
            new File(INSTALL_DIR, "globecover_19.tif.zip"),
            new File(INSTALL_DIR, "globecover_20.tif.zip"),
            new File(INSTALL_DIR, "globecover_21.tif.zip"),
            new File(INSTALL_DIR, "globecover_22.tif.zip"),
            new File(INSTALL_DIR, "globecover_23.tif.zip"),
            new File(INSTALL_DIR, "globecover_24.tif.zip"),
            new File(INSTALL_DIR, "globecover_25.tif.zip"),
            new File(INSTALL_DIR, "globecover_26.tif.zip"),
            new File(INSTALL_DIR, "globecover_27.tif.zip"),
            new File(INSTALL_DIR, "globecover_28.tif.zip"),
            new File(INSTALL_DIR, "globecover_29.tif.zip"),
            new File(INSTALL_DIR, "globecover_30.tif.zip"),
            new File(INSTALL_DIR, "globecover_31.tif.zip"),
            new File(INSTALL_DIR, "globecover_32.tif.zip"),
            new File(INSTALL_DIR, "globecover_33.tif.zip"),
            new File(INSTALL_DIR, "globecover_34.tif.zip"),
            new File(INSTALL_DIR, "globecover_35.tif.zip"),
            new File(INSTALL_DIR, "globecover_36.tif.zip")
    };

    public GlobCoverModelDescriptor() {
        remotePath = "http://step.esa.int/auxdata/landcover/globcover/";
        name = NAME;
        NO_DATA_VALUE = 230;
        installDir = INSTALL_DIR;

        final Path moduleBasePath = ResourceInstaller.findModuleCodeBasePath(this.getClass());
        colourIndexFile = moduleBasePath.resolve("org/esa/snap/landcover/auxdata/globcover/globcover_index.col");
    }

    @Override
    public LandCoverModel createLandCoverModel(final Resampling resampling) throws IOException {
        return new FileLandCoverModel(this, fileList, resampling);
    }

    public String createTileFilename(final int minLat, final int minLon) {
        return fileList[0].getName();
    }

    @Override
    public boolean isInstalled() {
        return true;
    }
}
