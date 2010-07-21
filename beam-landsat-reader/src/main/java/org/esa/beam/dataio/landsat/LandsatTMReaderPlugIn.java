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

package org.esa.beam.dataio.landsat;

import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.File;
import java.util.Locale;

/**
 * The class <code>LandsatTMReaderPlugIn</code> is used to provide the product reader for Landsat TM products.
 *
 * @author Christian Berwanger (ai0263@umwelt-campus.de)
 */
public final class LandsatTMReaderPlugIn implements ProductReaderPlugIn {

    /**
     * Constructs a new Landsat TM product reader plug-in instance.
     */
    public LandsatTMReaderPlugIn() {

    }

    /**
     * @param input Checks whether the given object is an acceptable input for this product reader and if so, the method checks if it
     *              is capable of decoding the input's content.
     *
     * @return <code> true </code> if the input data could be opened <code> false </code> if not
     */
    public final DecodeQualification getDecodeQualification(final Object input) {
        LandsatTMFile file = null;
        try {
            if (input instanceof String) {
                file = new LandsatTMFile((String) input);
            } else if (input instanceof File) {
                file = new LandsatTMFile((File) input);
            } else {
                return DecodeQualification.UNABLE;
            }
            if(file.canDecodeInput()) {
                return DecodeQualification.INTENDED;
            }
        } catch (Throwable e) {
            return DecodeQualification.UNABLE;
        }finally {
            if(file != null) {
                file.close();
            }
        }
        return DecodeQualification.UNABLE;
    }

    /**
     * @param input
     *
     * @return inputFile
     */
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
     * <p/>
     * <p> Instances of the classes returned in this array are valid objects for the <code>setInput</code> method of the
     * <code>ProductReader</code> interface (the method will not throw an <code>InvalidArgumentException</code> in this
     * case).
     *
     * @return an array containing valid input types, never <code>null</code>
     */
    public final Class[] getInputTypes() {
        return LandsatConstants.INPUT_TYPES;
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    public final ProductReader createReaderInstance() {
        return new LandsatTMReader(this);
    }

    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(Locale.getDefault()));
    }

    /**
     * Gets the names of the product formats handled by this reader or writer plug-in.
     *
     * @return all possible format names
     */
    public final String[] getFormatNames() {
        return LandsatConstants.FILE_NAMES;
    }

    /**
     * Gets the default file extensions associated with each of the format names returned by the <code>{@link
     * #getFormatNames}</code> method. <p>The string array returned shall always have the same length as the array
     * returned by the <code>{@link #getFormatNames}</code> method. <p>The extensions returned in the string array shall
     * always include a leading colon ('.') character, e.g. <code>".hdf"</code>
     *
     * @return the default file extensions for this product I/O plug-in, never <code>null</code>
     */
    public final String[] getDefaultFileExtensions() {
        return LandsatConstants.LANDSAT_EXTENSIONS;
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
    public final String getDescription(Locale locale) {
        return LandsatConstants.DESCRIPTION;
    }
}
