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

package org.esa.snap.dataio.arcbin;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;
import java.util.Locale;

public class ArcBinGridReaderPlugIn implements ProductReaderPlugIn {

    private static final String DESCRIPTION = "ArcInfo Binary Grids";
    private static final String[] FILE_EXTENSIONS = new String[]{""};
    private static final String FORMAT_NAME = "ARC_INFO_BIN_GRID";
    private static final String[] FORMAT_NAMES = new String[]{FORMAT_NAME};
    private static final Class[] INPUT_TYPES = new Class[]{String.class, File.class};
    private static final SnapFileFilter FILE_FILTER = new ArcBinGridFileFilter();

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        File file = new File(String.valueOf(input));
        if (isGridDirectory(file.getParentFile())) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public Class[] getInputTypes() {
        return INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new ArcBinGridReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return DESCRIPTION;
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return FILE_FILTER;
    }

    private static class ArcBinGridFileFilter extends SnapFileFilter {

        private ArcBinGridFileFilter() {
            setFormatName(FORMAT_NAMES[0]);
            setDescription(DESCRIPTION);
        }

        @Override
        public boolean accept(final File file) {
            return file.isDirectory() || isGridDirectory(file.getParentFile());
        }
    }

    static boolean isGridDirectory(File dir) {
        if (dir == null) {
            return false;
        }
        if (!dir.isDirectory()) {
            return false;
        }
        if (ArcBinGridReader.getCaseInsensitiveFile(dir, Header.FILE_NAME) == null) {
            return false;
        }
        if (ArcBinGridReader.getCaseInsensitiveFile(dir, GeorefBounds.FILE_NAME) == null) {
            return false;
        }
        if (ArcBinGridReader.getCaseInsensitiveFile(dir, TileIndex.FILE_NAME) == null) {
            return false;
        }
        if (ArcBinGridReader.getCaseInsensitiveFile(dir, RasterDataFile.FILE_NAME) == null) {
            return false;
        }
        return true;
    }
}
