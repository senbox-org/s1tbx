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

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.util.Locale;

/**
 * The reader plugin for the {@link CsvProductReader} class.
 *
 * @author Olaf Danne
 * @author Thomas Storm
 */
public class CsvProductReaderPlugIn implements ProductReaderPlugIn{

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        return null;
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[0];
    }

    @Override
    public ProductReader createReaderInstance() {
        return null;
    }

    @Override
    public String[] getFormatNames() {
        return new String[0];
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[0];
    }

    @Override
    public String getDescription(Locale locale) {
        return null;
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return null;
    }
}
