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

public class AESBEOS2016CropModelDescriptor extends AbstractLandCoverModelDescriptor {

    public static final String NAME = "AAFC Canada 2016 Crop";

    private static final File INSTALL_DIR = new File(Settings.instance().getAuxDataFolder().getAbsolutePath(),
            "LandCover" + File.separator + "AAFC" + File.separator + "AESB_EOS_Crop_2016");
    private static final File[] fileList = new File[]{
            new File(INSTALL_DIR, "ci_2016_ab_30m_v1.zip"),
            new File(INSTALL_DIR, "ci_2016_bc_30m_v1.zip"),
            new File(INSTALL_DIR, "ci_2016_mb_30m_v1.zip"),
            new File(INSTALL_DIR, "ci_2016_nb_30m_v1.zip"),
            new File(INSTALL_DIR, "ci_2016_nfl_30m_v1.zip"),
            new File(INSTALL_DIR, "ci_2016_ns_30m_v1.zip"),
            new File(INSTALL_DIR, "ci_2016_on_30m_v1.zip"),
            new File(INSTALL_DIR, "ci_2016_pei_30m_v1.zip"),
            new File(INSTALL_DIR, "ci_2016_qc_30m_v1.zip"),
            new File(INSTALL_DIR, "ci_2016_sk_30m_v1.zip")
    };

    public AESBEOS2016CropModelDescriptor() {
        remotePath = "http://step.esa.int/auxdata/landcover/AAFC/AESB_EOS_Crop_2016/";
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
