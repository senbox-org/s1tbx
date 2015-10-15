/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.dataio.rtp;

import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;

import static org.esa.snap.dataio.rtp.RawTiledPyramidsProductCodecSpi.*;


class RawTiledPyramidsProductFileFilter extends SnapFileFilter {
    public RawTiledPyramidsProductFileFilter() {
        super(FORMAT_NAME, NO_FILE_EXTENSIONS, FORMAT_DESCRIPTION);
    }

    @Override
    public boolean accept(File file) {
        if (isProductDir(file.getParentFile())) {
            return file.getName().equals(HEADER_NAME);
        }
        return file.isDirectory();
    }

    @Override
    public boolean isCompoundDocument(File dir) {
        return isProductDir(dir);
    }

    @Override
    public FileSelectionMode getFileSelectionMode() {
        return FileSelectionMode.FILES_AND_DIRECTORIES;
    }
}
