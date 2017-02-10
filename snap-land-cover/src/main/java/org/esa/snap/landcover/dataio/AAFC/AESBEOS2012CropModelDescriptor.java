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

import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.engine_utilities.util.Settings;
import org.esa.snap.landcover.dataio.AbstractLandCoverModelDescriptor;
import org.esa.snap.landcover.dataio.FileLandCoverUTMModel;
import org.esa.snap.landcover.dataio.LandCoverModel;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public class AESBEOS2012CropModelDescriptor extends AbstractLandCoverModelDescriptor {

    public static final String NAME = "AAFC Canada 2012 Crop";

    private static final File INSTALL_DIR = new File(Settings.instance().getAuxDataFolder().getAbsolutePath(),
            "LandCover" + File.separator + "AAFC" + File.separator + "AESB_EOS_Crop_2012");
    private static final File[] fileList = new File[]{
            new File(INSTALL_DIR, "STB-EOS_2012_CI_UTM10_30m_v2.zip"),
            new File(INSTALL_DIR, "STB-EOS_2012_CI_UTM11_30m_v3.zip"),
            new File(INSTALL_DIR, "STB-EOS_2012_CI_UTM12_30m_v3.zip"),
            new File(INSTALL_DIR, "STB-EOS_2012_CI_UTM13_30m_v2.zip"),
            new File(INSTALL_DIR, "STB-EOS_2012_CI_UTM14_30m_v2.zip"),
            new File(INSTALL_DIR, "STB-EOS_2012_CI_UTM15_30m_v2.zip"),
            new File(INSTALL_DIR, "STB-EOS_2012_CI_UTM16_30m_v2.zip"),
            new File(INSTALL_DIR, "STB-EOS_2012_CI_UTM17_30m_v2.zip"),
            new File(INSTALL_DIR, "STB-EOS_2012_CI_UTM18_30m_v2.zip"),
            new File(INSTALL_DIR, "STB-EOS_2012_CI_UTM19_30m_v2.zip"),
            new File(INSTALL_DIR, "STB-EOS_2012_CI_UTM20_30m_v3.zip")
    };

    public AESBEOS2012CropModelDescriptor() {
        remotePath = "http://step.esa.int/auxdata/landcover/AAFC/AESB_EOS_Crop_2012/";
        name = NAME;
        NO_DATA_VALUE = -9999d;
        installDir = INSTALL_DIR;
        metadataFileName = "AAFC_Crop_Mapping_2012_v2_Metadata_EN.xml";
        final Path moduleBasePath = ResourceInstaller.findModuleCodeBasePath(this.getClass());
        metadataSrcPath = moduleBasePath.resolve("org/esa/snap/landcover/auxdata/aafc/AAFC_Crop_Mapping_2012/");
        colourIndexFile = moduleBasePath.resolve("org/esa/snap/landcover/auxdata/aafc/aafc_crop_index.col");
    }

    @Override
    public LandCoverModel createLandCoverModel(final Resampling resampling) throws IOException {
        return new FileLandCoverUTMModel(this, fileList, resampling);
    }

    public String createTileFilename(final int minLat, final int minLon) {
        return fileList[0].getName();
    }
}
