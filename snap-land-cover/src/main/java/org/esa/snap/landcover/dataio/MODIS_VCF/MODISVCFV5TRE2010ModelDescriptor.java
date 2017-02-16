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

public class MODISVCFV5TRE2010ModelDescriptor extends AbstractLandCoverModelDescriptor {

    public static final String NAME = "MODIS 2010 Tree Cover Percentage";

    private static final File INSTALL_DIR = new File(Settings.instance().getAuxDataFolder().getAbsolutePath(),
            "LandCover" + File.separator + "Global_VCF_ZIP" + File.separator + "Collection_5" + File.separator + "2010");
    private static final File[] fileList = new File[]{
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.FE1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.FE1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.FE2122.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.FE2324.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.FE2526.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.FE4142.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.FE4344.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.FE5758.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.FE5960.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.HG0102.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.HG1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.HG1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.HG2122.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.HG3334.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.HG3536.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.HG3738.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.HG3940.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.HG4950.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.HG5152.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.HG5354.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.HG5556.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.HG5758.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.HG5960.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ0102.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ0304.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ0506.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ0910.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ1112.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ2122.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ2324.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ3132.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ3334.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ3536.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ3738.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ3940.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ4142.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ4950.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ5152.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ5354.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ5556.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ5758.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.KJ5960.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML0102.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML0304.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML0506.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML0708.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML1516.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML2122.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML2324.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML2526.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML2930.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML3132.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML3334.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML3536.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML3738.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML3940.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML4344.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML4748.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML4950.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML5152.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML5354.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML5556.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML5758.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.ML5960.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN0102.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN0304.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN1314.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN1516.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN2122.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN2526.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN2728.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN2930.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN3132.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN3334.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN3536.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN3738.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN3940.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN4344.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN4546.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN4748.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN4950.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN5152.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN5354.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.PN5960.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ0102.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ0304.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ0506.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ1112.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ1314.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ1516.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ2526.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ2728.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ2930.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ3132.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ3334.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ3536.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ3738.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ3940.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ4142.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ4344.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ4546.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ4748.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ4950.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ5152.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.RQ5758.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS0910.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS1112.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS1314.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS1516.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS2122.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS2526.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS2728.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS2930.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS3132.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS3334.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS3536.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS3738.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS3940.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS4142.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS4344.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS4546.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS4748.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS4950.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS5152.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS5354.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.TS5556.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU0102.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU0304.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU0506.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU0708.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU0910.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU1112.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU1314.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU1516.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU2122.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU2324.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU2728.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU2930.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU3132.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU3334.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU3536.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU3738.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU3940.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU4142.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU4344.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU4546.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU4748.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU4950.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU5152.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU5354.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU5556.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU5758.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.VU5960.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW0102.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW0304.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW0506.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW0708.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW0910.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW1112.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW1314.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW1516.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW1718.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW1920.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW2122.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW2324.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW2526.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW2728.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW2930.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW3132.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW3334.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW3536.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW3738.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW3940.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW4142.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW4344.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW4546.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW4748.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW4950.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW5152.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW5354.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW5556.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW5758.tif.zip"),
            new File(INSTALL_DIR, "MOD44B_V5_TRE.2010.XW5960.tif.zip")
    };

    public MODISVCFV5TRE2010ModelDescriptor() {
        remotePath = "http://step.esa.int/auxdata/landcover/Global_VCF_ZIP/Collection_5/2010/";
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
