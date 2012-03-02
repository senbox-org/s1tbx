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

package org.esa.beam.csv.dataio.reader;

import org.esa.beam.csv.dataio.CsvProductFile;
import org.esa.beam.csv.dataio.CsvProductSourceParser;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.util.Locale;

/**
 * The reader plugin for the {@link CsvProductReader} class.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvProductReaderPlugIn implements ProductReaderPlugIn {

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final File file = new File(input.toString());
        if(!isFileExtensionValid(file)) {
            return DecodeQualification.UNABLE;
        }

        final CsvProductSourceParser csvProductFile = new CsvProductFile(input.toString());
        try {
            csvProductFile.parse();
        } catch (CsvProductFile.ParseException e) {
            return DecodeQualification.UNABLE;
        }

        return DecodeQualification.SUITABLE;
    }

    private boolean isFileExtensionValid(File file) {
        boolean hasValidExtension = false;
        for (String extension : getDefaultFileExtensions()) {
            if(FileUtils.getExtension(file).toLowerCase().equals(extension)) {
                hasValidExtension = true;
                break;
            }
        }
        return hasValidExtension;
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new CsvProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{"CSV"};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".csv",".txt",".dat"};
    }

    @Override
    public String getDescription(Locale locale) {
        return "CSV products";
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }
}
