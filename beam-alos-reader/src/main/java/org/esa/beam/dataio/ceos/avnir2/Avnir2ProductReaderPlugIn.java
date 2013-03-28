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

package org.esa.beam.dataio.ceos.avnir2;

import org.esa.beam.dataio.ceos.CeosHelper;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.io.FileUtils;

import java.io.File;
import java.io.FilenameFilter;
import java.util.Locale;

/**
 * The ReaderPlugIn for Avnir-2 products.
 *
 * @author Marco Peters
 */
public class Avnir2ProductReaderPlugIn implements ProductReaderPlugIn {

    private final static String _AVNIR2_INDICATION_KEY = "ALAV2";
    private final static int _MINIMUM_FILES = 7;    // 4 image files + leader file + volume file + trailer file

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if it
     * is capable of decoding the input's content.
     *
     * @param input any input object
     *
     * @return true if this product reader can decode the given input, otherwise false.
     */
    public DecodeQualification getDecodeQualification(final Object input) {
        final File file = CeosHelper.getFileFromInput(input);
        if (file == null) {
            return DecodeQualification.UNABLE;
        }
        final String filename = FileUtils.getFilenameWithoutExtension(file);
        if (!filename.startsWith(Avnir2Constants.VOLUME_FILE_PREFIX)) {
            return DecodeQualification.UNABLE; // not the volume file
        }

        final File parentDir = file.getParentFile();
        if (file.isFile() && parentDir != null && parentDir.isDirectory()) {
            final FilenameFilter filter = new FilenameFilter() {
                public boolean accept(final File dir, final String name) {
                    return name.contains(_AVNIR2_INDICATION_KEY);
                }
            };
            final File[] files = parentDir.listFiles(filter);
            if (files != null && files.length >= _MINIMUM_FILES) {
                return DecodeQualification.INTENDED;
            }
        }
        return DecodeQualification.UNABLE;
    }

    /**
     * Returns an array containing the classes that represent valid input types for this reader.
     * <p/>
     * <p> Intances of the classes returned in this array are valid objects for the <code>setInput</code> method of the
     * <code>ProductReader</code> interface (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid input types, never <code>null</code>
     */
    public Class[] getInputTypes() {
        return Avnir2Constants.VALID_INPUT_TYPES;
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    public ProductReader createReaderInstance() {
        return new Avnir2ProductReader(this);
    }

    public BeamFileFilter getProductFileFilter() {
        return new Avnir2FileFilter();
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    public String[] getFormatNames() {
        return Avnir2Constants.FORMAT_NAMES;
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
        return Avnir2Constants.FORMAT_FILE_EXTENSIONS;
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p/>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the local for the given decription string, if <code>null</code> the default locale is used
     *
     * @return a textual description of this product reader/writer
     */
    public String getDescription(final Locale locale) {
        return Avnir2Constants.PLUGIN_DESCRIPTION;
    }

    public static class Avnir2FileFilter extends BeamFileFilter {

        public Avnir2FileFilter() {
            super();
            setFormatName(Avnir2Constants.FORMAT_NAMES[0]);
            setDescription(Avnir2Constants.PLUGIN_DESCRIPTION);
        }

        /**
         * Tests whether or not the given file is accepted by this filter. The default implementation returns
         * <code>true</code> if the given file is a directory or the path string ends with one of the registered extensions.
         * if no extension are defined, the method always returns <code>true</code>
         *
         * @param file the file to be or not be accepted.
         *
         * @return <code>true</code> if given file is accepted by this filter
         */
        @Override
        public boolean accept(final File file) {
            if (super.accept(file)) {
                if (file.isDirectory() || file.getName().startsWith(Avnir2Constants.VOLUME_FILE_PREFIX)) {
                    return true;
                }
            }
            return false;
        }

    }
}
