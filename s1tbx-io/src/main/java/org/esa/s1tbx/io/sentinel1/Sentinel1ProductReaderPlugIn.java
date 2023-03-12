/*
 * Copyright (C) 2015 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.s1tbx.io.sentinel1;

import org.esa.s1tbx.commons.io.S1TBXFileFilter;
import org.esa.s1tbx.commons.io.S1TBXProductReaderPlugIn;
import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.util.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;

/**
 * The ReaderPlugIn for Sentinel1 products.
 */
public class Sentinel1ProductReaderPlugIn implements S1TBXProductReaderPlugIn {

    private final static String[] FORMAT_NAMES = new String[]{"SENTINEL-1"};
    private final static String[] FORMAT_FILE_EXTENSIONS = new String[]{".safe", ".zip"};
    private final static String PLUGIN_DESCRIPTION = "SENTINEL-1 Products";      /*I18N*/

    private final static String PRODUCT_PREFIX = "MANIFEST";
    final static String PRODUCT_HEADER_NAME = "manifest.safe";
    final static String PRODUCT_EXT = ".SAFE";

    private final static Class[] VALID_INPUT_TYPES = new Class[]{Path.class, File.class, String.class};

    private final static String ANNOTATION = "annotation";
    private final static String MEASUREMENT = "measurement";

    private final static String[] annotation_prefixes = new String[] {"s1", "s2-", "s3-", "s4-", "s5-", "s6-", "iw", "ew", "rs2", "asa"};

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if it
     * is capable of decoding the input's content.
     *
     * @param input any input object
     * @return true if this product reader can decode the given input, otherwise false.
     */
    @Override
    public DecodeQualification getDecodeQualification(final Object input) {
        Path path = ReaderUtils.getPathFromInput(input);
        if (path != null) {
            if(Files.isDirectory(path)) {
                path = path.resolve(PRODUCT_HEADER_NAME);
                if(!Files.exists(path)) {
                    return DecodeQualification.UNABLE;
                }
            }

            if(path.getFileName() != null) {
                final String filename = path.getFileName().toString().toLowerCase();
                if (filename.equals(PRODUCT_HEADER_NAME)) {
                    if (isLevel1(path) || isLevel2(path) || isLevel0(path)) {
                        return DecodeQualification.INTENDED;
                    }
                }
                if (filename.endsWith(".zip") && filename.startsWith("s1") &&
                        (ZipUtils.findInZip(path.toFile(), "s1", PRODUCT_HEADER_NAME) ||
                                ZipUtils.findInZip(path.toFile(), "rs2", PRODUCT_HEADER_NAME))) {
                    return DecodeQualification.INTENDED;
                }
                if (filename.startsWith("s1") && filename.endsWith(".safe") && Files.isDirectory(path)) {
                    Path manifest = path.resolve(PRODUCT_HEADER_NAME);
                    if (Files.exists(manifest)) {
                        if (isLevel1(manifest) || isLevel2(manifest) || isLevel0(manifest)) {
                            return DecodeQualification.INTENDED;
                        }
                    }
                }
            }
        }
        //todo zip stream

        return DecodeQualification.UNABLE;
    }

    static boolean isLevel1(final Path path) {
        if (ZipUtils.isZip(path)) {
            if(ZipUtils.findInZip(path.toFile(), "s1", ".tiff")) {
                return true;
            }
            final String name = path.getFileName().toString().toUpperCase();
            return name.contains("_1AS") || name.contains("_1AD") || name.contains("_1SS") || name.contains("_1SD");
        } else {
            final File annotationFolder = path.getParent().resolve(ANNOTATION).toFile();
            return annotationFolder.exists() && checkFolder(annotationFolder, ".xml");
        }
    }

    static boolean isLevel2(final Path path) {
        if (ZipUtils.isZip(path)) {
            return ZipUtils.findInZip(path.toFile(), "s1", ".nc");
        } else {
            final File measurementFolder = path.getParent().resolve(MEASUREMENT).toFile();
            return measurementFolder.exists() && checkFolder(measurementFolder, ".nc");
        }
    }

    static boolean isLevel0(final Path path) {
        if (ZipUtils.isZip(path)) {
            return ZipUtils.findInZip(path.toFile(), "s1", ".dat");
        } else {
            return checkFolder(path.getParent().toFile(), ".dat");
        }
    }

    static void validateInput(final Path path) throws IOException {
        if (ZipUtils.isZip(path)) {
            if(!ZipUtils.findInZip(path.toFile(), "s1", ".tiff")) {
                throw new IOException("measurement folder is missing in product");
            }
        } else {
            if (!Files.exists(path.getParent().resolve(ANNOTATION))) {
                throw new IOException("annotation folder is missing in product");
            }
        }
    }

    private static boolean checkFolder(final File folder, final String extension) {
        final File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                final String name = f.getName().toLowerCase();
                if (f.isFile()) {
                    for(String prefix : annotation_prefixes) {
                        if(name.startsWith(prefix) && (extension == null || name.endsWith(extension))) {
                            return true;
                        }
                    }
                }
            }
        }
        return false;
    }

    public static class FileFilter extends SnapFileFilter {

        public FileFilter() {
            super();
            setFormatName(FORMAT_NAMES[0]);
            setExtensions(FORMAT_FILE_EXTENSIONS);
            setDescription(PLUGIN_DESCRIPTION);
        }

        /**
         * Tests whether or not the given file is accepted by this filter. The default implementation returns
         * <code>true</code> if the given file is a directory or the path string ends with one of the registered extensions.
         * if no extension are defined, the method always returns <code>true</code>
         *
         * @param file the file to be or not be accepted.
         * @return <code>true</code> if given file is accepted by this filter
         */
        public boolean accept(final File file) {
            if (super.accept(file)) {
                final String name = file.getName().toUpperCase();
                return file.isDirectory() || (name.startsWith(PRODUCT_PREFIX) &&
                        name.endsWith(PRODUCT_EXT)) ||
                        (name.startsWith("S1") && name.endsWith(".ZIP"));
            }
            return false;
        }

    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    @Override
    public ProductReader createReaderInstance() {
        return new Sentinel1ProductReader(this);
    }

    /**
     * Returns an array containing the classes that represent valid input types for this reader.
     * <p>
     * <p> Intances of the classes returned in this array are valid objects for the <code>setInput</code> method of the
     * <code>ProductReader</code> interface (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid input types, never <code>null</code>
     */
    @Override
    public Class[] getInputTypes() {
        return VALID_INPUT_TYPES;
    }

    @Override
    public SnapFileFilter getProductFileFilter() {
        return new S1TBXFileFilter(this);
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    @Override
    public String[] getFormatNames() {
        return FORMAT_NAMES;
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same length as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    @Override
    public String[] getDefaultFileExtensions() {
        return FORMAT_FILE_EXTENSIONS;
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the local for the given decription string, if <code>null</code> the default locale is used
     * @return a textual description of this product reader/writer
     */
    @Override
    public String getDescription(final Locale locale) {
        return PLUGIN_DESCRIPTION;
    }

    @Override
    public String[] getProductMetadataFileExtensions() {
        return new String[] {PRODUCT_EXT};
    }

    @Override
    public String[] getProductMetadataFilePrefixes() {
        return new String[] {PRODUCT_PREFIX};
    }
}
