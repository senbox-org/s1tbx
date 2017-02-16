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

import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.engine_utilities.util.Settings;
import org.esa.snap.landcover.dataio.AbstractLandCoverModelDescriptor;
import org.esa.snap.landcover.dataio.LandCoverModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class AESBEOS2014CropModelDescriptor extends AbstractLandCoverModelDescriptor {

    public static final String NAME = "AAFC Canada 2014 Crop";

    private static final File INSTALL_DIR = new File(Settings.instance().getAuxDataFolder().getAbsolutePath(),
            "LandCover" + File.separator + "AAFC" + File.separator + "AESB_EOS_Crop_2014");
    private static final File[] fileList = new File[]{
            new File(INSTALL_DIR, "AAFC_2014_CI_AB_30m_v1_TIF.zip"),
            new File(INSTALL_DIR, "AAFC_2014_CI_BC_30m_v2_TIF.zip"),
            new File(INSTALL_DIR, "AAFC_2014_CI_MB_30m_v1_TIF.zip"),
            new File(INSTALL_DIR, "AAFC_2014_CI_NB_30m_v2_TIF.zip"),
            new File(INSTALL_DIR, "AAFC_2014_CI_NFL_30m_v1_TIF.zip"),
            new File(INSTALL_DIR, "AAFC_2014_CI_NS_30m_v2_TIF.zip"),
            new File(INSTALL_DIR, "AAFC_2014_CI_ON_30m_v2_TIF.zip"),
            new File(INSTALL_DIR, "AAFC_2014_CI_PEI_30m_v2_TIF.zip"),
            new File(INSTALL_DIR, "AAFC_2014_CI_QC_30m_v2_TIF.zip"),
            new File(INSTALL_DIR, "AAFC_2014_CI_SK_30m_v1_TIF.zip")
    };

    public AESBEOS2014CropModelDescriptor() {
        remotePath = "http://step.esa.int/auxdata/landcover/AAFC/AESB_EOS_Crop_2014/";
        name = NAME;
        NO_DATA_VALUE = -9999d;
        installDir = INSTALL_DIR;
        final Path moduleBasePath = ResourceInstaller.findModuleCodeBasePath(this.getClass());
        colourIndexFile = moduleBasePath.resolve("org/esa/snap/landcover/auxdata/aafc/aafc_crop_index.col");
    }

    @Override
    public synchronized boolean installFiles() {
        installMetadata();

        if (installDir != null) {
            isInstalled = true;
        }
        return isInstalled;
    }

    @Override
    public LandCoverModel createLandCoverModel(final Resampling resampling) throws IOException {
        return new FileLandCoverProvincialModel(this, fileList, resampling);
    }

    public String createTileFilename(final int minLat, final int minLon) {
        return fileList[0].getName();
    }
}
