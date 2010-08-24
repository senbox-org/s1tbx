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

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

/**
 * Plugin class for the {@link LandsatGeotiffReader} reader.
 */
public class LandsatGeotiffReaderPlugin implements ProductReaderPlugIn {

    private static final Class[] READER_INPUT_TYPES = new Class[]{String.class,File.class};

    private static final String[] FORMAT_NAMES = new String[]{"LandsatGeoTIFF"};
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{".txt", ".TXT"};
    private static final String READER_DESCRIPTION = "Landsat Data Products (GeoTIFF)";
    private static final BeamFileFilter FILE_FILTER = new LandsatGeoTiffFileFilter();

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        File dir = getFileInput(input);
        if (dir == null) {
            return DecodeQualification.UNABLE;
        }
        File[] files = dir.listFiles();
        if (files == null) {
            return DecodeQualification.UNABLE;
        }
        for (File file : files) {
            if (isMetadataFile(file)) {
                FileReader fileReader = null;
                try {
                    fileReader = new FileReader(file);
                    LandsatMetadata landsatMetadata = new LandsatMetadata(fileReader);
                    if (landsatMetadata.isLandsatTM()) {
                        return DecodeQualification.INTENDED;
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


    static File getFileInput(Object input) {
        if (input instanceof String) {
            return getFileInput(new File((String) input));
        } else if (input instanceof File) {
            return getFileInput((File) input);
        }
        return null;
    }

    static File getFileInput(File file) {
        if (file.isDirectory()) {
            return file;
        } else {
            return file.getParentFile();
        }
    }

    static boolean isMetadataFile(File file) {
        String filename = file.getName().toLowerCase();
        return filename.endsWith("_mtl.txt");
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