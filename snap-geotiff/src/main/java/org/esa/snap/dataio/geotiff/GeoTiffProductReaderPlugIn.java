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
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.StringUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.core.util.io.SnapFileFilter;

import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.Locale;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

public class GeoTiffProductReaderPlugIn implements ProductReaderPlugIn {

    private static final String[] FORMAT_NAMES = new String[]{"GeoTIFF"};
    private final String[] TIFF_FILE_EXTENSION = {".tif", ".tiff", ".gtif", ".btf"};
    private final String ZIP_FILE_EXTENSION = ".zip";
    private final String[] ALL_FILE_EXTENSIONS = StringUtils.addToArray(TIFF_FILE_EXTENSION, ZIP_FILE_EXTENSION);

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
            if (input instanceof String || input instanceof File) {
                final String ext = FileUtils.getExtension((File) imageIOInput);
                if (ext != null) {
                    boolean extensionMatch = Arrays.stream(TIFF_FILE_EXTENSION).anyMatch(ext::equalsIgnoreCase);
                    if (extensionMatch) {
                        return DecodeQualification.INTENDED;
                    } else if (ext.equalsIgnoreCase(ZIP_FILE_EXTENSION)) {
                        return checkZip((File) imageIOInput);
                    }
                }
                return DecodeQualification.UNABLE;
            }
            try (ImageInputStream stream = ImageIO.createImageInputStream(imageIOInput)) {
                return getDecodeQualificationImpl(stream);
            }
        } catch (Exception ignore) {
            // nothing to do, return value is already UNABLE
        }

        return DecodeQualification.UNABLE;
    }

    private DecodeQualification checkZip(final File file) throws IOException {
        final ZipFile productZip = new ZipFile(file, ZipFile.OPEN_READ);
        final Enumeration<? extends ZipEntry> entries = productZip.entries();
        boolean foundTiff = false;

        // RapidEye reader returns UNABLE as DecodeQualification on Mac. This
        // Incomplete and useless data from a zip file is opened as GeoTiff instead. This can be disturbing for users.
        // Even though it is not good that the GeoTiff reader has knowledge about RapidEye, no better solution was found so far.
        boolean foundNtif = productZip.stream().anyMatch((ze -> ze.getName().endsWith("ntf")));

        int entryCnt = 0;
        while (entries.hasMoreElements()) {
            final ZipEntry zipEntry = entries.nextElement();
            if (zipEntry != null && !zipEntry.isDirectory()) {
                entryCnt++;
                final String name = zipEntry.getName().toLowerCase();
                boolean extensionMatch = Arrays.stream(TIFF_FILE_EXTENSION).anyMatch(name::endsWith);
                if (!name.contains("/") && extensionMatch) {
                    foundTiff = true;
                }
                if (!foundNtif && foundTiff && entryCnt > 1) {
                    return DecodeQualification.SUITABLE;        // not exclusively a zipped tiff
                }
            }
        }
        if (foundTiff && entryCnt == 1) {
            return DecodeQualification.INTENDED;    // only zipped tiff
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
        return ALL_FILE_EXTENSIONS;
    }

    @Override
    public String getDescription(Locale locale) {
        return "GeoTIFF data product.";
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(FORMAT_NAMES[0], getDefaultFileExtensions(), getDescription(null));
    }
}
