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
package org.esa.beam.dataio.avhrr;

import org.esa.beam.dataio.avhrr.noaa.KlmAvhrrFile;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

/**
 * The plug-in class for the {@link org.esa.beam.dataio.avhrr.AvhrrReader AVHRR/3 Level-1b reader}.
 *
 * @see <a href="http://www2.ncdc.noaa.gov/doc/klm/">NOAA KLM User's Guide</a>
 */
public class AvhrrReaderPlugIn implements ProductReaderPlugIn {

    public static final String FORMAT_NAME = "NOAA_AVHRR_3_L1B";

    private static final String[] FILE_EXTENSIONS = new String[]{""};
    private static final String DESCRIPTION = "NOAA-AVHRR/3 Level-1b Data Product";
    private static final Class[] INPUT_TYPES = new Class[]{
            String.class,
            File.class,
    };

    public AvhrrReaderPlugIn() {
    }

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        final File file = getInputFile(input);
        if (KlmAvhrrFile.canDecode(file)) {
            return DecodeQualification.INTENDED;
        }
        return DecodeQualification.UNABLE;
    }

    public static File getInputFile(Object input) {
        File file = null;
        if (input instanceof String) {
            file = new File((String) input);
        } else if (input instanceof File) {
            file = (File) input;
        }
        return file;
    }

    @Override
    public Class[] getInputTypes() {
        return INPUT_TYPES;
    }

    @Override
    public ProductReader createReaderInstance() {
        return new AvhrrReader(this);
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

    @Override
    public String[] getFormatNames() {
        return new String[]{
                FORMAT_NAME
        };
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return DESCRIPTION;
    }
}
