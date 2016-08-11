/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.csv.dataio.reader;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.csv.dataio.Constants;
import org.esa.snap.csv.dataio.CsvFile;
import org.esa.snap.csv.dataio.CsvSourceParser;

import java.io.File;
import java.util.Locale;
import java.util.logging.Level;

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
        if (!isFileExtensionValid(file)) {
            return DecodeQualification.UNABLE;
        }

        CsvSourceParser csvFile = null;
        try {
            csvFile = CsvFile.createCsvSourceParser(input.toString());
            csvFile.parseMetadata();
            csvFile.checkReadingFirstRecord();
        } catch (Exception e) {
            String msg = String.format("Not able to decode CSV file, reason is '%s'", e.getMessage());
            SystemUtils.LOG.log(Level.WARNING, msg);
            return DecodeQualification.UNABLE;
        } finally {
            if (csvFile != null) {
                csvFile.close();
            }
        }

        return DecodeQualification.SUITABLE;
    }

    private boolean isFileExtensionValid(File file) {
        String fileExtension = FileUtils.getExtension(file);
        if (fileExtension != null) {
            return StringUtils.contains(getDefaultFileExtensions(), fileExtension.toLowerCase());
        }
        return false;
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
        return new String[]{Constants.FORMAT_NAME};
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".csv", ".txt"};
    }

    @Override
    public String getDescription(Locale locale) {
        return Constants.DESCRIPTION;
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }
}
