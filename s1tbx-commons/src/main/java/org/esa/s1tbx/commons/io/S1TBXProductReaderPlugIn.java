/*
 * Copyright (C) 2021 by SkyWatch Space Applications Inc. http://www.skywatch.com
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
package org.esa.s1tbx.commons.io;

import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

public interface S1TBXProductReaderPlugIn extends ProductReaderPlugIn {

    String[] getProductMetadataFileExtensions();

    String[] getProductMetadataFilePrefixes();

    default File findMetadataFile(final Path folderPath) {
        final File folder = folderPath.toFile();
        if (folder.isDirectory()) {
            final File[] fileList = folder.listFiles();
            if (fileList != null) {
                for (File f : fileList) {
                    if (f.isDirectory()) {
                        final File[] fileList2 = f.listFiles();
                        if (fileList2 != null) {
                            for (File f2 : fileList2) {
                                if (isPrimaryMetadataFileName(f2.getName())) {
                                    return f2;
                                }
                            }
                        }
                    } else if (isPrimaryMetadataFileName(f.getName())) {
                        return f;
                    }
                }
            }
        } else {
            try {
                if (isPrimaryMetadataFileName(folder.getName())) {
                    return folder;
                } else {
                    final File file = folderPath.toRealPath().toFile();
                    final String fileName = FileUtils.getFilenameWithoutExtension(file.getName());
                    for(String ext : getProductMetadataFileExtensions()) {
                        File metadataFile = new File(file.getParentFile(), fileName+ext);
                        if (metadataFile.exists() && isPrimaryMetadataFileName(metadataFile.getName())) {
                            return metadataFile;
                        }
                    }
                }
            } catch (IOException e) {
                SystemUtils.LOG.severe("Unable to findMetadataFile " + e.getMessage());
            }
        }
        return null;
    }

    default boolean isPrimaryMetadataFileName(final String metadataFileName) {
        final String filename = metadataFileName.toUpperCase();
        for(String ext : getProductMetadataFileExtensions()) {
            if (filename.endsWith(ext.toUpperCase())) {
                final String[] prefixes = getProductMetadataFilePrefixes();
                if (prefixes == null || prefixes.length == 0) {
                    return true;
                }
                for (String prefix : prefixes) {
                    if (filename.startsWith(prefix.toUpperCase())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }
}
