/*
 * Copyright (C) 2018 Skywatch. https://www.skywatch.co
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
package org.csa.rstb.io.rcm;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;
import org.esa.snap.engine_utilities.gpf.ReaderUtils;
import org.esa.snap.engine_utilities.util.ZipUtils;

import java.io.File;
import java.util.Locale;

/**
 * The ReaderPlugIn for RCM.
 */
public class RCMProductReaderPlugIn implements ProductReaderPlugIn {

    private final static String[] FORMAT_NAMES = {"RCM"};
    private final static String[] FORMAT_FILE_EXTENSIONS = {"xml", "zip"};
    final static String PRODUCT_PREFIX = "RCM";
    final static String[] PRODUCT_HEADER_PREFIX = {"MANIFEST"};
    final static String[] PRODUCT_HEADER_EXT = {".SAFE"};
    private final static String PLUGIN_DESCRIPTION = "Radarsat Constellation Mission";

    private final Class[] VALID_INPUT_TYPES = new Class[]{File.class, String.class};

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if it
     * is capable of decoding the input's content.
     *
     * @param input any input object
     * @return true if this product reader can decode the given input, otherwise false.
     */
    public DecodeQualification getDecodeQualification(final Object input) {

        final File file = ReaderUtils.getFileFromInput(input);
        if (file != null) {
            if (file.getName().startsWith(PRODUCT_PREFIX) && ZipUtils.isZip(file)) {
                return DecodeQualification.INTENDED;
            }
            if (findMetadataFile(file) != null) {
                return DecodeQualification.INTENDED;
            }
        }
        return DecodeQualification.UNABLE;
    }

    public static File findMetadataFile(final File folder) {
        if (folder.isDirectory() && folder.getName().startsWith(PRODUCT_PREFIX)) {
            final File[] fileList = folder.listFiles();
            if (fileList != null) {
                for (File f : fileList) {
                    if (isValidProductName(f.getName())) {
                        String name = f.getName();
                        if (!(name.endsWith("MUX.xml") || name.endsWith("PAN.xml"))) {
                            return f;
                        }
                    }
                }
            }
        } else if (isValidProductName(folder.getName()) && folder.getParentFile().getName().startsWith(PRODUCT_PREFIX)) {
            return folder;
        }
        return null;
    }

    public static boolean isValidProductName(final String name) {
        final String filename = name.toUpperCase();
        for (String prefix : PRODUCT_HEADER_PREFIX) {
            if (filename.startsWith(prefix)) {
                for (String ext : PRODUCT_HEADER_EXT) {
                    if (filename.endsWith(ext)) {
                        return true;
                    }
                }
            }
        }
        return false;
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
    public Class[] getInputTypes() {
        return VALID_INPUT_TYPES;
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    public ProductReader createReaderInstance() {
        return new RCMProductReader(this);
    }

    public SnapFileFilter getProductFileFilter() {
        return new FileFilter();
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
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
    public String getDescription(final Locale locale) {
        return PLUGIN_DESCRIPTION;
    }

    public static class FileFilter extends SnapFileFilter {

        public FileFilter() {
            super();
            setFormatName(FORMAT_FILE_EXTENSIONS[0]);
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
                final String filename = file.getName().toLowerCase();
                if (file.isDirectory()) {
                    return true;
                }
                if (filename.startsWith(RCMConstants.PRODUCT_ZIP_PREFIX) && filename.endsWith(RCMConstants.PRODUCT_ZIP_EXT)) {
                    return true;
                }
                if (filename.equals(RCMConstants.PRODUCT_MANIFEST)) {
                    return true;
                }
            }
            return false;
        }
    }
}