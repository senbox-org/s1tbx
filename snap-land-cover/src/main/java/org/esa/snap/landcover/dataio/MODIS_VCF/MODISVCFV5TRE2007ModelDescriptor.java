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
package org.esa.snap.landcover.dataio.MODIS_VCF;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.engine_utilities.util.Settings;
import org.esa.snap.landcover.dataio.AbstractLandCoverModelDescriptor;
import org.esa.snap.landcover.dataio.LandCoverModel;

import java.io.File;
import java.io.IOException;

public class MODISVCFV5TRE2007ModelDescriptor extends AbstractLandCoverModelDescriptor {

    public static final String NAME = "MODIS 2007 Tree Cover Percentage";

    private static final File INSTALL_DIR = new File(Settings.instance().getAuxDataFolder().getAbsolutePath(),
            "LandCover" + File.separator + "Global_VCF_ZIP" + File.separator + "Collection_5" + File.separator + "2007");
    private static final File[] fileList = new File[]{
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.FE1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.FE1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.FE2122.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.FE2324.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.FE2526.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.FE4142.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.FE4344.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.FE5758.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.FE5960.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.HG0102.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.HG1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.HG1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.HG2122.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.HG3334.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.HG3536.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.HG3738.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.HG3940.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.HG4950.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.HG5152.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.HG5354.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.HG5556.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.HG5758.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.HG5960.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ0102.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ0304.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ0506.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ0910.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ1112.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ2122.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ2324.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ3132.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ3334.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ3536.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ3738.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ3940.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ4142.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ4950.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ5152.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ5354.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ5556.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ5758.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.KJ5960.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML0102.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML0304.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML0506.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML0708.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML1516.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML2122.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML2324.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML2526.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML2930.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML3132.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML3334.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML3536.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML3738.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML3940.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML4344.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML4748.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML4950.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML5152.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML5354.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML5556.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML5758.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.ML5960.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN0102.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN0304.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN1314.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN1516.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN2122.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN2526.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN2728.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN2930.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN3132.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN3334.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN3536.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN3738.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN3940.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN4344.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN4546.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN4748.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN4950.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN5152.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN5354.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.PN5960.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ0102.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ0304.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ0506.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ1112.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ1314.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ1516.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ2526.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ2728.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ2930.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ3132.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ3334.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ3536.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ3738.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ3940.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ4142.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ4344.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ4546.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ4748.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ4950.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ5152.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.RQ5758.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS0910.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS1112.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS1314.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS1516.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS2122.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS2526.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS2728.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS2930.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS3132.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS3334.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS3536.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS3738.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS3940.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS4142.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS4344.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS4546.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS4748.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS4950.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS5152.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS5354.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.TS5556.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU0102.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU0304.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU0506.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU0708.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU0910.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU1112.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU1314.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU1516.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU2122.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU2324.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU2728.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU2930.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU3132.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU3334.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU3536.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU3738.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU3940.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU4142.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU4344.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU4546.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU4748.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU4950.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU5152.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU5354.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU5556.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU5758.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.VU5960.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW0102.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW0304.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW0506.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW0708.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW0910.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW1112.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW1314.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW1516.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW2122.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW2324.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW2526.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW2728.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW2930.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW3132.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW3334.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW3536.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW3738.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW3940.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW4142.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW4344.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW4546.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW4748.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW4950.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW5152.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW5354.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW5556.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW5758.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2007.XW5960.tif.zip")
    };

    public MODISVCFV5TRE2007ModelDescriptor() {
        remotePath = "http://step.esa.int/auxdata/landcover/Global_VCF_ZIP/Collection_5/2007/";
        name = NAME;
        NO_DATA_VALUE = 253d;
        unit = "tree_cover_percent";
        installDir = INSTALL_DIR;
    }

    @Override
    public LandCoverModel createLandCoverModel(final Resampling resampling) throws IOException {
        return new FileLandCoverUTMZoneLatBandModel(this, fileList, resampling);
    }

    public String createTileFilename(final int minLat, final int minLon) {
        return fileList[0].getName();
    }

    @Override
    public int getDataType() {
        return ProductData.TYPE_FLOAT32;
    }

    @Override
    public boolean isInstalled() {
        return true;
    }
}
