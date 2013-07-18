/*
 * Copyright (C) 2011 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Locale;

/**
 * @author Thomas Storm
 */
public class Landsat8GeotiffReaderPlugin implements ProductReaderPlugIn {

    private static final Class[] READER_INPUT_TYPES = new Class[]{String.class, File.class};
    private static final String[] FORMAT_NAMES = new String[]{"Landsat8GeoTIFF"};
    private static final String[] DEFAULT_FILE_EXTENSIONS = new String[]{".txt", ".TXT", ".gz"};
    private static final String READER_DESCRIPTION = "Landsat 8 Data Products (GeoTIFF)";

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        if (!isLandsat8Filename(new File(input.toString()).getName())) {
            return DecodeQualification.UNABLE;
        }

        VirtualDir virtualDir;
        try {
            virtualDir = LandsatGeotiffReaderPlugin.getInput(input);
        } catch (IOException e) {
            return DecodeQualification.UNABLE;
        }

        if (virtualDir == null) {
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
            try {
                File file = virtualDir.getFile(fileName);
                if (isMetadataFile(file)) {
                    return DecodeQualification.INTENDED;
                }
            } catch (IOException ignore) {
                // file is broken, but be tolerant here
            }
        }
        // didn't find the expected metadata file
        return DecodeQualification.UNABLE;

    }

    static boolean isMetadataFile(File file) {
        if (!file.getName().toLowerCase().endsWith("_mtl.txt")) {
            return false;
        }
        BufferedReader reader = null;
        String line;
        try {
            reader = new BufferedReader(new FileReader(file));
            line = reader.readLine();
        } catch (IOException e) {
            return false;
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ignore) {
                }
            }
        }
        return line != null && line.matches("GROUP = L1_METADATA_FILE");
    }

    @Override
    public Class[] getInputTypes() {
        return READER_INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new LandsatGeotiffReader(this, new LandsatMetadataFactory.Landsat8MetadataFactory());
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
        return new BeamFileFilter(FORMAT_NAMES[0], DEFAULT_FILE_EXTENSIONS, READER_DESCRIPTION);
    }

    static boolean isLandsat8Filename(String filename) {
        if (filename.matches("L[O,T,C]8\\d{13}.{3}\\d{2}_MTL.(txt|TXT)")) {
            return true;
        } else if (filename.matches("L[O,T,C]8\\d{13}.{3}\\d{2}\\.tar\\.gz")) {
            return true;
        } else {
            return false;
        }
    }
}
