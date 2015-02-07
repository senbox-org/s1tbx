/*
 * Copyright (C) 2014 by Array Systems Computing Inc. http://www.array.ca
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
package org.esa.nest.dataio.sentinel1;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.snap.gpf.ReaderUtils;
import org.esa.snap.util.ZipUtils;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

/**
 * The ReaderPlugIn for Sentinel1 products.
 */
public class Sentinel1ProductReaderPlugIn implements ProductReaderPlugIn {

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
            final String filename = file.getName().toLowerCase();
            if (filename.equals("manifest.safe")) {
                if (isLevel1(file) || isLevel2(file) || isLevel0(file))
                    return DecodeQualification.INTENDED;
            }
            if (filename.endsWith(".zip") && (ZipUtils.findInZip(file, "s1", Sentinel1Constants.PRODUCT_HEADER_NAME) ||
                    ZipUtils.findInZip(file, "rs2", Sentinel1Constants.PRODUCT_HEADER_NAME))) {
                return DecodeQualification.INTENDED;
            }
        }
        //todo zip stream

        return DecodeQualification.UNABLE;
    }

    public static boolean isLevel1(final File file) {
        if (ZipUtils.isZip(file)) {
            if(ZipUtils.findInZip(file, "s1", ".tiff")) {
                return true;
            }
            final String name = file.getName().toUpperCase();
            return name.contains("_1AS") || name.contains("_1AD") || name.contains("_1SS") || name.contains("_1SD");
        } else {
            final File baseFolder = file.getParentFile();
            final File annotationFolder = new File(baseFolder, "annotation");
            if (annotationFolder.exists()) {
                return checkFolder(annotationFolder, ".xml");
            }
            final File measurementFolder = new File(baseFolder, "measurement");
            return measurementFolder.exists() && checkFolder(measurementFolder, ".tiff");
        }
    }

    public static boolean isLevel2(final File file) {
        if (ZipUtils.isZip(file)) {
            return ZipUtils.findInZip(file, "s1", ".nc");
        } else {
            final File baseFolder = file.getParentFile();
            final File measurementFolder = new File(baseFolder, "measurement");
            return measurementFolder.exists() && checkFolder(measurementFolder, ".nc");
        }
    }

    public static boolean isLevel0(final File file) {
        if (ZipUtils.isZip(file)) {
            return ZipUtils.findInZip(file, "s1", ".dat");
        } else {
            final File baseFolder = file.getParentFile();
            return checkFolder(baseFolder, ".dat");
        }
    }

    static void validateInput(final File file) throws IOException {
        if (ZipUtils.isZip(file)) {
            if(!ZipUtils.findInZip(file, "s1", ".tiff")) {
                throw new IOException("measurement folder is missing in product");
            }
        } else {
            final File baseFolder = file.getParentFile();
            final File annotationFolder = new File(baseFolder, "annotation");
            if (!annotationFolder.exists()) {
                throw new IOException("annotation folder is missing in product");
            }
            final File measurementFolder = new File(baseFolder, "measurement");
            if (!measurementFolder.exists()) {
                throw new IOException("measurement folder is missing in product");
            }
        }
    }

    private static boolean checkFolder(final File folder, final String extension) {
        final File[] files = folder.listFiles();
        if (files != null) {
            for (File f : files) {
                final String name = f.getName().toLowerCase();
                if (f.isFile() && (name.startsWith("s1") || name.startsWith("asa") || name.startsWith("rs2"))) {
                    if (extension == null || name.endsWith(extension)) {
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
        return Sentinel1Constants.VALID_INPUT_TYPES;
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    public ProductReader createReaderInstance() {
        return new Sentinel1ProductReader(this);
    }

    public BeamFileFilter getProductFileFilter() {
        return new FileFilter();
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    public String[] getFormatNames() {
        return Sentinel1Constants.getFormatNames();
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
        return Sentinel1Constants.getForamtFileExtensions();
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
        return Sentinel1Constants.getPluginDescription();
    }

    public static class FileFilter extends BeamFileFilter {

        public FileFilter() {
            super();
            setFormatName(Sentinel1Constants.getFormatNames()[0]);
            setExtensions(Sentinel1Constants.getForamtFileExtensions());
            setDescription(Sentinel1Constants.getPluginDescription());
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
                if (file.isDirectory() || (name.startsWith(Sentinel1Constants.PRODUCT_HEADER_PREFIX) &&
                        name.endsWith(Sentinel1Constants.getIndicationKey())) ||
                        (name.startsWith("S1") && name.endsWith(".ZIP"))) {
                    return true;
                }
            }
            return false;
        }

    }
}