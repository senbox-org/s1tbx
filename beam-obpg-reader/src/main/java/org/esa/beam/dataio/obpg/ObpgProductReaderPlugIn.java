/*
 * $Id: ModisProductReaderPlugIn.java,v 1.10 2006/10/17 15:07:32 marcop Exp $
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
package org.esa.beam.dataio.obpg;

import ncsa.hdf.hdflib.HDFConstants;
import org.esa.beam.dataio.obpg.hdf.HdfAttribute;
import org.esa.beam.dataio.obpg.hdf.ObpgUtils;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.StringUtils;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class ObpgProductReaderPlugIn implements ProductReaderPlugIn {

    // This is here just to keep the property name
//    private static final String HDF4_PROPERTY_KEY = "ncsa.hdf.hdflib.HDFLibrary.hdflib";

    public static final String DEFAULT_FILE_EXTENSION = ".hdf";
    public static final String READER_DESCRIPTION = "NASA Ocean Color (OBPG) Products";
    public static final String FORMAT_NAME = "NASA-OBPG";

    private static final String[] magicStrings = {
                "MODISA Level-2 Data",
                "MODIST Level-2 Data",
                "SeaWiFS Level-2 Data",
                "CZCS Level-2 Data",
                "OCTS Level-2 Data"
    };

    private static boolean hdfLibAvailable = false;

    ObpgUtils utils = new ObpgUtils();

    static {
        // check the availability of the hdf library - if none is present set global flag.
        Throwable cause = null;
        try {
            hdfLibAvailable = Class.forName("ncsa.hdf.hdflib.HDFLibrary") != null;
        } catch (ClassNotFoundException e) {
            cause = e;
            hdfLibAvailable = false;
        } catch (LinkageError e) {
            cause = e;
            hdfLibAvailable = false;
        }
        if (!hdfLibAvailable) {
            String msg;
            if (cause != null) {
                msg = String.format("HDF-4 library not usable: %s: %s", cause.getClass(), cause.getMessage());
            } else {
                msg = "HDF-4 library not usable";
            }
            BeamLogManager.getSystemLogger().info(msg);
        }
    }

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if it
     * is capable of decoding the input's content.
     */
    public DecodeQualification getDecodeQualification(Object input) {
        try {
            int fileId = HDFConstants.FAIL;
            try {
                final File file = getInputFile(input);
                if (!hdfLibAvailable
                    || file == null
                    || !file.isFile()
                    || !utils.isHdfFile(file.getPath())) {
                    return DecodeQualification.UNABLE;
                }
                fileId = utils.openHdfFileReadOnly(file.getPath());
                final int sdStart = utils.openSdInterfaceReadOnly(file.getPath());
                final List<HdfAttribute> list = utils.readGlobalAttributes(sdStart);
                for (HdfAttribute hdfAttribute : list) {
                    if ("Title".equals(hdfAttribute.getName())) {
                        final String value = hdfAttribute.getStringValue();
                        if (value != null) {
                            if (StringUtils.containsIgnoreCase(magicStrings, value.trim())) {
                                return DecodeQualification.INTENDED;
                            }
                        }
                        break;
                    }
                }
            } finally {
                if (fileId != HDFConstants.FAIL) {
                    utils.closeHdfFile(fileId);
                }
            }
        } catch (Exception e) {
            // nothing to do, return value is already false
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
        if (!hdfLibAvailable) {
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
        if (!hdfLibAvailable) {
            return null;
        }

        return new ObpgProductReader(this);
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
        if (!hdfLibAvailable) {
            return new String[0];
        }

        return new String[]{DEFAULT_FILE_EXTENSION};
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
        return READER_DESCRIPTION;
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    public String[] getFormatNames() {
        if (!hdfLibAvailable) {
            return new String[0];
        }

        return new String[]{FORMAT_NAME};
    }

    private File getInputFile(Object input) {
        try {
            return ObpgUtils.getInputFile(input);
        } catch (IllegalArgumentException e) {
            //ignore
        }
        return null;
    }
}