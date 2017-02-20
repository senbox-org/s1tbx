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

public class AESBEOS2000CropModelDescriptor extends AbstractLandCoverModelDescriptor {

    public static final String NAME = "AAFC Canada 2000 Crop";

    private static final File INSTALL_DIR = new File(Settings.instance().getAuxDataFolder().getAbsolutePath(),
            "LandCover" + File.separator + "AAFC" + File.separator + "AESB_EOS_Crop_2000");
    private static final File[] fileList = new File[]{
            new File(INSTALL_DIR, "LCV_UTM9_AAFC_30M_2000_v12.zip"),
            new File(INSTALL_DIR, "LCV_UTM10_AAFC_30M_2000_v12.zip"),
            new File(INSTALL_DIR, "LCV_UTM11_AAFC_30M_2000_v12.zip"),
            new File(INSTALL_DIR, "LCV_UTM12_AAFC_30M_2000_v11.zip"),
            new File(INSTALL_DIR, "LCV_UTM13_AAFC_30M_2000_v11.zip"),
            new File(INSTALL_DIR, "LCV_UTM14_AAFC_30M_2000_v11.zip"),
            new File(INSTALL_DIR, "LCV_UTM15_AAFC_30M_2000_v12.zip"),
            new File(INSTALL_DIR, "LCV_UTM16_AAFC_30M_2000_v12.zip"),
            new File(INSTALL_DIR, "LCV_UTM17_AAFC_30M_2000_v12.zip"),
            new File(INSTALL_DIR, "LCV_UTM18_AAFC_30M_2000_v12.zip"),
            new File(INSTALL_DIR, "LCV_UTM19_AAFC_30M_2000_v11.zip"),
            new File(INSTALL_DIR, "LCV_UTM20_AAFC_30M_2000_v11.zip"),
            new File(INSTALL_DIR, "LCV_UTM21_AAFC_30M_2000_v11.zip"),
            new File(INSTALL_DIR, "LCV_UTM22_AAFC_30M_2000_v11.zip"),
    };

    public AESBEOS2000CropModelDescriptor() {
        remotePath = "http://step.esa.int/auxdata/landcover/AAFC/AESB_EOS_Crop_2000/";
        name = NAME;
        installDir = INSTALL_DIR;
        NO_DATA_VALUE = -9999d;
        metadataFileName = "LCV_CA_AAFC_30M_2000_v12_EN.xml";
        final Path moduleBasePath = ResourceInstaller.findModuleCodeBasePath(this.getClass());
        metadataSrcPath = moduleBasePath.resolve("org/esa/snap/landcover/auxdata/aafc/LCV_CA_AAFC_30M_2000/");
        colourIndexFile = moduleBasePath.resolve("org/esa/snap/landcover/auxdata/aafc/aafc_crop_index.col");
    }

    @Override
    public LandCoverModel createLandCoverModel(Resampling resampling) throws IOException {
        return new FileLandCoverUTMModel(this, fileList, resampling);
    }

    public String createTileFilename(int minLat, int minLon) {
        return fileList[0].getName();
    }
}
