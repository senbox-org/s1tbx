/*
 * Copyright (C) 2013 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.polsarpro;

import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.esa.beam.dataio.envi.EnviConstants;
import org.esa.beam.dataio.envi.EnviProductReaderPlugIn;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;

import java.io.File;
import java.io.FileFilter;
import java.util.Locale;

public class PolsarProProductReaderPlugIn extends EnviProductReaderPlugIn {

    public static final String FORMAT_NAME = "PolSARPro";

    @Override
    public ProductReader createReaderInstance() {
        return new PolsarProProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{ FORMAT_NAME };
    }

    @Override
    public String getDescription(Locale locale) {
        return "PolSARPro";
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        if (input instanceof File) {
            final File folder = (File) input;
            if(folder.isDirectory()) {
                final FileFilter filter = new SuffixFileFilter(EnviConstants.HDR_EXTENSION);
                for(File file : folder.listFiles(filter)) {
                    final DecodeQualification fileQualification = super.getDecodeQualification(file);
                    if(fileQualification != DecodeQualification.UNABLE)
                        return fileQualification;
                }
                return DecodeQualification.UNABLE;
            } 
        } 

        return DecodeQualification.UNABLE; //super.getDecodeQualification(input);
    }
}