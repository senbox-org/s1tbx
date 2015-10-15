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

package org.esa.snap.core.dataio.dimap;

import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;

import static org.esa.snap.core.dataio.dimap.DimapProductConstants.*;


public class DimapFileFilter extends SnapFileFilter {
    public DimapFileFilter() {
        super(DIMAP_FORMAT_NAME, DIMAP_HEADER_FILE_EXTENSION, "BEAM-DIMAP product files");
    }

    @Override
    public boolean accept(File file) {
        if (file.isFile() && hasHeaderExt(file)) {
            return FileUtils.exchangeExtension(file, DIMAP_DATA_DIRECTORY_EXTENSION).isDirectory();
        } else {
            return file.isDirectory() && !isDataDir(file);
        }
    }

    @Override
    public boolean isCompoundDocument(File dir) {
        return isDataDir(dir);
    }

    private boolean isDataDir(File dir) {
        return hasDataExt(dir) && FileUtils.exchangeExtension(dir, DIMAP_HEADER_FILE_EXTENSION).isFile();
    }

    private boolean hasHeaderExt(File file) {
        return file.getName().endsWith(DIMAP_HEADER_FILE_EXTENSION);
    }

    private boolean hasDataExt(File file) {
        return file.getName().endsWith(DIMAP_DATA_DIRECTORY_EXTENSION);
    }
}
