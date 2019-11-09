/*
 * Copyright (C) 2016 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.kompsat5;

import org.esa.s1tbx.io.netcdf.NetCDFReaderPlugIn;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The ReaderPlugIn for CosmoSkymed products.
 */
public class Kompsat5ReaderPlugIn extends NetCDFReaderPlugIn {

    private final static String[] KOMPSAT5_FORMAT_NAMES = {"Kompsat5"};
    private final static String[] KOMPSAT5_FORMAT_FILE_EXTENSIONS = {"h5", "tif", "aux.xml"};
    private final static String KOMPSAT5_PLUGIN_DESCRIPTION = "Kompsat-5 Products";
    private final static String KOMPSAT5_FILE_PREFIX = "k5_";

    public Kompsat5ReaderPlugIn() {
        FORMAT_NAMES = KOMPSAT5_FORMAT_NAMES;
        FORMAT_FILE_EXTENSIONS = KOMPSAT5_FORMAT_FILE_EXTENSIONS;
        PLUGIN_DESCRIPTION = KOMPSAT5_PLUGIN_DESCRIPTION;
    }

    public static File findMetadataFile(final File folder) {
        if(folder.isDirectory()) {
            final File[] fileList = folder.listFiles();
            if(fileList != null) {
                for(File f : fileList) {
                    if(isValidProductName(f.getName())) {
                        return f;
                    }
                }
            }
        } else {
            if(isValidProductName(folder.getName())) {
                return folder;
            }
        }
        return null;
    }

    public static boolean isValidProductName(final String name) {
        final String filename = name.toUpperCase();
        return filename.startsWith("K5_") && filename.endsWith("_AUX.XML");
    }

    @Override
    protected DecodeQualification checkProductQualification(final Path path) {
        final String fileName = path.getFileName().toString().toLowerCase();
        if(Files.isDirectory(path)) {
            final File metadataFile = findMetadataFile(path.toFile());
            if (metadataFile != null) {
                return DecodeQualification.INTENDED;
            }
        }
        if (fileName.startsWith(KOMPSAT5_FILE_PREFIX)) {
            for(String ext : KOMPSAT5_FORMAT_FILE_EXTENSIONS) {
                if(fileName.endsWith(ext)) {
                    return DecodeQualification.INTENDED;
                }
            }
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
        return new Kompsat5Reader(this);
    }

}
