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
package org.esa.s1tbx.io.cosmo;

import org.esa.s1tbx.io.netcdf.NetCDFReaderPlugIn;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;

import java.nio.file.Path;

/**
 * The ReaderPlugIn for CosmoSkymed products.
 */
public class CosmoSkymedReaderPlugIn extends NetCDFReaderPlugIn {

    private final static String[] COSMO_FORMAT_NAMES = {"CosmoSkymed"};
    private final static String[] COSMO_FORMAT_FILE_EXTENSIONS = {"h5"};
    private final static String COSMO_PLUGIN_DESCRIPTION = "Cosmo-Skymed Products";
    private final static String COSMO_FILE_PREFIX = "cs";

    public CosmoSkymedReaderPlugIn() {
        FORMAT_NAMES = COSMO_FORMAT_NAMES;
        FORMAT_FILE_EXTENSIONS = COSMO_FORMAT_FILE_EXTENSIONS;
        PLUGIN_DESCRIPTION = COSMO_PLUGIN_DESCRIPTION;
    }

    @Override
    protected DecodeQualification checkProductQualification(final Path path) {
        final String fileName = path.getFileName().toString().toLowerCase();
        if (fileName.endsWith(".h5") && fileName.startsWith(COSMO_FILE_PREFIX)) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    @Override
    public ProductReader createReaderInstance() {
        return new CosmoSkymedReader(this);
    }
}
