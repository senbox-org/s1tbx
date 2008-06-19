/*
 * $Id: ChrisProductReaderPlugIn.java,v 1.5 2007/04/10 13:55:42 ralf Exp $
 *
 * Copyright (C) 2002,2003  by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package org.esa.beam.dataio.chris;

import ncsa.hdf.hdflib.HDFConstants;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.util.Locale;

public class ChrisProductReaderPlugIn implements ProductReaderPlugIn {

    // This is here just to keep the property name
    // private static final String HDF4_PROPERTY_KEY = "ncsa.hdf.hdflib.HDFLibrary.hdflib";

    private static boolean hdfLibraryAvailable;

    static {
        try {
            hdfLibraryAvailable = Class.forName("ncsa.hdf.hdflib.HDFLibrary") != null;
        } catch (Throwable t) {
            // ignore, {@code hdfLibraryAvailable} is already {@code false}
        }
        //noinspection StaticVariableUsedBeforeInitialization
        if (!hdfLibraryAvailable) {
            BeamLogManager.getSystemLogger().info("HDF library is not available");
        }
    }

    /**
     * Returns whether or not the HDF library is available.
     *
     * @return {@code true} if the HDF library is available, {@code false}
     *         otherwise.
     */
    public static boolean isHDFLibraryAvailable() {
        return hdfLibraryAvailable;
    }

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if it
     * is capable of decoding the input's content.
     */
    public DecodeQualification getDecodeQualification(Object input) {
        if (!isHDFLibraryAvailable()) {
            return DecodeQualification.UNABLE;
        }

        final File file;

        if (input instanceof String) {
            file = new File((String) input);
        } else if (input instanceof File) {
            file = (File) input;
        } else {
            return DecodeQualification.UNABLE;
        }

        // @todo 2 rq/rq write test for this logic!
        if (file.getPath().toLowerCase().endsWith(ChrisConstants.DEFAULT_FILE_EXTENSION) && file.isFile()) {
            int fileId = -1;
            try {
                fileId = HDF4Lib.Hopen(file.getPath(), HDFConstants.DFACC_RDONLY);
                if (fileId != -1) {
                    if (isChrisModeAttributeAvailable(file)) {
                        return DecodeQualification.INTENDED;
                    }
                }
            } catch (Exception e) {
                // nothing to do, return value is already false
            } finally {
                if (fileId != -1) {
                    try {
                        HDF4Lib.Hclose(fileId);
                    } catch (Exception ignore) {
                        // nothing to do, return value is already false
                    }
                }
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
        if (!isHDFLibraryAvailable()) {
            return new Class[0];
        }

        return new Class[]{String.class, File.class};
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    public ProductReader createReaderInstance() {
        if (!isHDFLibraryAvailable()) {
            return null;
        }

        return new ChrisProductReader(this);
    }

    public BeamFileFilter getProductFileFilter() {
        String[] formatNames = getFormatNames();
        String formatName = "";
        if (formatNames.length > 0) {
            formatName = formatNames[0];
        }
        return new BeamFileFilter(formatName, getDefaultFileExtensions(), getDescription(null));
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
        if (!isHDFLibraryAvailable()) {
            return new String[0];
        }

        return new String[]{ChrisConstants.DEFAULT_FILE_EXTENSION};
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
    public String getDescription(Locale locale) {
        return ChrisConstants.READER_DESCRIPTION;
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    public String[] getFormatNames() {
        if (!isHDFLibraryAvailable()) {
            return new String[0];
        }

        return new String[]{ChrisConstants.FORMAT_NAME};
    }


    private static boolean isChrisModeAttributeAvailable(File file) throws Exception {
        final int sdId = HDF4Lib.SDstart(file.getPath(), HDFConstants.DFACC_RDONLY);
        if (sdId == -1) {
            return false;
        }
        final String[] nameBuffer = new String[]{""};
        final int[] attributeInfo = new int[16];
        try {
            HDF4Lib.SDattrinfo(sdId, 0, nameBuffer, attributeInfo);
            final String name = nameBuffer[0];
            int numberType = attributeInfo[0];
            int arrayLength = attributeInfo[1];
            byte[] data = new byte[arrayLength];
            if ("Sensor Type".equalsIgnoreCase(name) && numberType == HDFConstants.DFNT_CHAR) {
                HDF4Lib.SDreadattr(sdId, 0, data);
                return "CHRIS".equalsIgnoreCase(new String(data));
            }
        } finally {
            HDF4Lib.SDend(sdId);
        }
        return false;
    }

}
