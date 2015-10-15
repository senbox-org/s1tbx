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

package org.esa.snap.dataio.getasse30;

import org.esa.snap.core.dataio.DecodeQualification;
import org.esa.snap.core.dataio.ProductReader;
import org.esa.snap.core.dataio.ProductReaderPlugIn;
import org.esa.snap.core.util.io.SnapFileFilter;

import java.io.File;
import java.util.Locale;


/**
 * A plugin for a product reader for GETASSE30 data.
 *
 * @author Norman Fomferra
 */
public class GETASSE30ReaderPlugIn implements ProductReaderPlugIn {

    public static final String FORMAT_NAME = "GETASSE30";
    public static final String[] FILE_EXTENSIONS = new String[]{
            ".getasse30"
    };
    public static final Class[] INPUT_TYPES = new Class[]{
            String.class,
            File.class,
    };
    public static final String DESCRIPTION = "GETASSE30 Data Product Tile";

    public GETASSE30ReaderPlugIn() {
    }

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if it
     * is capable of decoding the input's content.
     */
    public DecodeQualification getDecodeQualification(final Object input) {
        final File file = getInputFile(input);
        if (file != null) {
            final String fileName = file.getName().toLowerCase();
            final String[] extensions = getDefaultFileExtensions();
            for (final String extension : extensions) {
                if (fileName.endsWith(extension) && !fileName.equals(extension)) {
                    return DecodeQualification.INTENDED;
                }
            }
        }
        return DecodeQualification.UNABLE;
    }

    public static File getInputFile(final Object input) {
        File file = null;
        if (input instanceof String) {
            file = new File((String) input);
        } else if (input instanceof File) {
            file = (File) input;
        }
        return file;
    }

    /**
     * Returns an array containing the classes that represent valid input types for this reader.
     * <p> Intances of the classes returned in this array are valid objects for the <code>setInput</code> method of the
     * <code>ProductReader</code> interface (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid input types, never <code>null</code>
     */
    public Class[] getInputTypes() {
        return INPUT_TYPES;
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    public ProductReader createReaderInstance() {
        return new GETASSE30Reader(this);
    }

    public SnapFileFilter getProductFileFilter() {
        return new SnapFileFilter(FORMAT_NAME, FILE_EXTENSIONS, DESCRIPTION);
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    public String[] getFormatNames() {
        return new String[]{
                FORMAT_NAME
        };
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same lenhth as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    public String[] getDefaultFileExtensions() {
        return FILE_EXTENSIONS;
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the local for the given decription string, if <code>null</code> the default locale is used
     *
     * @return a textual description of this product reader/writer
     */
    public String getDescription(final Locale locale) {
        return DESCRIPTION;
    }


}
