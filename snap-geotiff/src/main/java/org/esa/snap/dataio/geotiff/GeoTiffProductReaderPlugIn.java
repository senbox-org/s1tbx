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
package org.esa.snap.dataio.geotiff;

import it.geosolutions.imageioimpl.plugins.tiff.TIFFImageReader;
import org.esa.snap.framework.dataio.DecodeQualification;
import org.esa.snap.framework.dataio.ProductReader;
import org.esa.snap.framework.dataio.ProductReaderPlugIn;
import org.esa.snap.util.io.BeamFileFilter;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.InputStream;
import java.util.Iterator;
import java.util.Locale;

public class GeoTiffProductReaderPlugIn implements ProductReaderPlugIn {

    private static final String[] FORMAT_NAMES = new String[]{"GeoTIFF"};

    @Override
    public DecodeQualification getDecodeQualification(Object input) {
        try {
            final Object imageIOInput;
            if (input instanceof String) {
                imageIOInput = new File((String) input);
            } else if (input instanceof File || input instanceof InputStream) {
                imageIOInput = input;
            } else {
                return DecodeQualification.UNABLE;        
            }
            final ImageInputStream stream = ImageIO.createImageInputStream(imageIOInput);
            try {
                return getDecodeQualificationImpl(stream);
            } finally {
                stream.close();
            }
        } catch (Exception ignore) {
            // nothing to do, return value is already UNABLE
        }

        return DecodeQualification.UNABLE;
    }

    static DecodeQualification getDecodeQualificationImpl(ImageInputStream stream) {
        try {
            Iterator<ImageReader> imageReaders = ImageIO.getImageReaders(stream);
            TIFFImageReader imageReader = null;
            while (imageReaders.hasNext()) {
                final ImageReader reader = imageReaders.next();
                if (reader instanceof TIFFImageReader) {
                    imageReader = (TIFFImageReader) reader;
                    break;
                }
            }
            if (imageReader == null) {
                return DecodeQualification.UNABLE;
            }
        } catch (Exception ignore) {
            return DecodeQualification.UNABLE;
        }
        return DecodeQualification.SUITABLE;
    }

    @Override
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class, InputStream.class,};
    }

    @Override
    public ProductReader createReaderInstance() {
        return new GeoTiffProductReader(this);
    }

    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    @Override
    public String[] getDefaultFileExtensions() {
        return new String[]{".tif", ".tiff", ".btf"};
    }

    @Override
    public String getDescription(Locale locale) {
        return "GeoTIFF data product.";
    }

    @Override
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(FORMAT_NAMES[0], getDefaultFileExtensions(), getDescription(null));
    }
}
