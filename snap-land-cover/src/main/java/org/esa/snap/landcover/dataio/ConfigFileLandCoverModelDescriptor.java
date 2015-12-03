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
package org.esa.snap.landcover.dataio;

import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.dataop.resamp.Resampling;
import org.esa.snap.core.util.PropertyMap;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class ConfigFileLandCoverModelDescriptor extends AbstractLandCoverModelDescriptor {

    private final File[] fileList;
    private final boolean isUTM;
    private final boolean isFloat;

    private final static String LANDCOVER = "landcover";

    public ConfigFileLandCoverModelDescriptor(final PropertyMap configProp, final String key) {

        final String descriptorName = configProp.getPropertyString(key);
        final String prefix = LANDCOVER + '.' + descriptorName;
        name = configProp.getPropertyString(prefix + ".name");
        remotePath = configProp.getPropertyString(prefix + ".remotePath");
        NO_DATA_VALUE = (double) configProp.getPropertyInt(prefix + ".nodatavalue");
        isUTM = configProp.getPropertyBool(prefix + ".utmIndexed");
        isFloat = configProp.getPropertyBool(prefix + ".floatData");
        final int numFiles = configProp.getPropertyInt(prefix + ".file.num");

        final List<File> fileArray = new ArrayList<>(numFiles);
        for (int i = 1; i <= numFiles; ++i) {
            final String filePath = configProp.getPropertyString(prefix + ".file." + i);
            if (filePath != null && !filePath.isEmpty()) {
                fileArray.add(new File(filePath));
            }
        }
        fileList = fileArray.toArray(new File[fileArray.size()]);

        final String colourIndex = configProp.getPropertyString(prefix + ".colourIndex");
        if (colourIndex != null && !colourIndex.isEmpty()) {
            //     colourIndexFile = new File(ResourceUtils.getResFolder(), "ColourIndex" + File.separator + colourIndex);
        }
    }

    @Override
    public LandCoverModel createLandCoverModel(Resampling resampling) throws IOException {
        return isUTM ? new FileLandCoverUTMModel(this, fileList, resampling) : new FileLandCoverModel(this, fileList, resampling);
    }

    public String createTileFilename(int minLat, int minLon) {
        return fileList[0].getName();
    }

    @Override
    public int getDataType() {
        return isFloat ? ProductData.TYPE_FLOAT32 : super.getDataType();
    }
}
