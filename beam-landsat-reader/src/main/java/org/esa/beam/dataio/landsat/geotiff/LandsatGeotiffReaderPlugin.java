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

package org.esa.beam.dataio.landsat.geotiff;

import com.bc.ceres.core.VirtualDir;
import org.esa.beam.dataio.landsat.tgz.VirtualDirTgz;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

/**
 * Plugin class for the {@link LandsatGeotiffReader} reader.
 */
public class LandsatGeotiffReaderPlugin implements ProductReaderPlugIn {

    private static final Class[] READER_INPUT_TYPES = new Class[]{String.class, File.class};

    private static final String[] FORMAT_NAMES = new String[]{"LandsatGeoTIFF"};
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{".txt", ".TXT"};
    private static final String READER_DESCRIPTION = "Landsat Data Products (GeoTIFF)";
    private static final BeamFileFilter FILE_FILTER = new LandsatGeoTiffFileFilter();

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        // Landsat 7 files must start with L7
        VirtualDir virtualDir;
        try {
            virtualDir = getInput(input);
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }

        if (virtualDir == null) {
            return DecodeQualification.UNABLE;
        }

        if (virtualDir.isCompressed() || virtualDir.isArchive()) {
            if (isMatchingArchiveFileName(getFileInput(input).getName())) {
                return DecodeQualification.INTENDED;
            }
            return DecodeQualification.UNABLE;
        }

        String[] list;
        try {
            list = virtualDir.list("");
            if (list == null || list.length == 0) {
                return DecodeQualification.UNABLE;
            }
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }

        for (String fileName : list) {
            FileReader fileReader = null;
            try {
                File file = virtualDir.getFile(fileName);
                if (isMetadataFile(file)) {
                    fileReader = new FileReader(file);
                    LandsatMetadata landsatMetadata = new LandsatMetadata(fileReader);
                    if (landsatMetadata.isLandsatTM() || landsatMetadata.isLandsatETM_Plus()) {
                        return DecodeQualification.INTENDED;
                    }
                }
            } catch (IOException ignore) {
            } finally {
                if (fileReader != null) {
                    try {
                        fileReader.close();
                    } catch (IOException ignore) {
                    }
                }
            }
        }
        return DecodeQualification.UNABLE;
    }

    @Override
    public Class[] getInputTypes() {
        return READER_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new LandsatGeotiffReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return DEFAULT_FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return READER_DESCRIPTION;
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return FILE_FILTER;
    }

    /**
     * Retrieves the VirtualDir for input from the input object passed in
     *
     * @param input the input object (File or String)
     * @return the VirtualDir representing the product
     * @throws java.io.IOException on disk access failures
     */
    public static VirtualDir getInput(Object input) throws IOException {
        File inputFile = getFileInput(input);

        if (inputFile.isFile() && !isCompressedFile(inputFile)) {
            final File absoluteFile = inputFile.getAbsoluteFile();
            inputFile = absoluteFile.getParentFile();
            if (inputFile == null) {
                throw new IOException("Unable to retrieve parent to file: " + absoluteFile.getAbsolutePath());
            }
        }

        VirtualDir virtualDir = VirtualDir.create(inputFile);
        if (virtualDir == null) {
            virtualDir = new VirtualDirTgz(inputFile);
        }
        return virtualDir;
    }

    static File getFileInput(Object input) {
        if (input instanceof String) {
            return new File((String) input);
        } else if (input instanceof File) {
            return (File) input;
        }
        return null;
    }

    static boolean isMetadataFile(File file) {
        final String filename = file.getName().toLowerCase();
        return filename.endsWith("_mtl.txt");
    }

    static boolean isCompressedFile(File file) {
        final String extension = FileUtils.getExtension(file);
        if (StringUtils.isNullOrEmpty(extension)) {
            return false;
        }

        return extension.contains("zip")
                || extension.contains("tar")
                || extension.contains("tgz")
                || extension.contains("gz");
    }

    static boolean isMatchingArchiveFileName(String fileName) {
        return StringUtils.isNotNullAndNotEmpty(fileName) &&
                (fileName.startsWith("L5_")
                || fileName.startsWith("LT5")
                || fileName.startsWith("L7_")
                || fileName.startsWith("LE7"));
    }

    private static class LandsatGeoTiffFileFilter extends BeamFileFilter {

        public LandsatGeoTiffFileFilter() {
            super();
            setFormatName(FORMAT_NAMES[0]);
            setDescription(READER_DESCRIPTION);
        }

        @Override
        public boolean accept(final File file) {
            if (file.isDirectory()) {
                return true;
            }
            return isMetadataFile(file);
        }
    }
}