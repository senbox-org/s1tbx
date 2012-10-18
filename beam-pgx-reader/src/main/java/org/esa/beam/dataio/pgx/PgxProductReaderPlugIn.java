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
package org.esa.beam.dataio.pgx;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * @author Norman Fomferra
 * @see PgxProductReader
 */
public class PgxProductReaderPlugIn implements ProductReaderPlugIn {

    public PgxProductReaderPlugIn() {
    }

    public String[] getFormatNames() {
        return new String[]{"PGX"};
    }

    public String[] getDefaultFileExtensions() {
        return new String[]{".pgx"};
    }

    public String getDescription(Locale name) {
        return "PGX images";
    }

    public DecodeQualification getDecodeQualification(Object input) {
        if (input instanceof String || input instanceof File) {
            if (canReadHeader(new File(input.toString()))) {
                return DecodeQualification.INTENDED;
            }
        } else if (input instanceof ImageInputStream) {
            if (canReadHeader((ImageInputStream) input)) {
                return DecodeQualification.INTENDED;
            }
        }
        return DecodeQualification.UNABLE;
    }

    private boolean canReadHeader(File file) {
        if (!file.isFile() || file.length() == 0) {
            return false;
        }
        try {
            final FileImageInputStream stream = new FileImageInputStream(file);
            try {
                return canReadHeader(stream);
            } finally {
                stream.close();
            }
        } catch (IOException e) {
            return false;
        }
    }

    private boolean canReadHeader(ImageInputStream stream) {
        try {
            return PgxProductReader.readerHeader(stream) != null;
        } catch (IOException e) {
            return false;
        }
    }

    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class, ImageInputStream.class};
    }

    public ProductReader createReaderInstance() {
        return new PgxProductReader(this);
    }

    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

}
