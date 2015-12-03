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

import java.io.File;
import java.io.IOException;

public class FileLandCoverModelDescriptor extends AbstractLandCoverModelDescriptor {

    private final File file;

    public FileLandCoverModelDescriptor(final File file) {
        remotePath = null;
        name = file.getName();
        this.file = file;
        NO_DATA_VALUE = -9999d;
    }

    @Override
    public LandCoverModel createLandCoverModel(Resampling resampling) throws IOException {
        return new FileLandCoverModel(this, new File[]{file}, resampling);
    }

    public String createTileFilename(int minLat, int minLon) {
        return file.getName();
    }

    @Override
    public int getDataType() {
        return ProductData.TYPE_FLOAT32;
    }
}
