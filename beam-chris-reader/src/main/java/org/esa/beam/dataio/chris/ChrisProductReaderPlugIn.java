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

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import ucar.ma2.DataType;
import ucar.nc2.Attribute;
import ucar.nc2.NetcdfFile;

import java.io.File;
import java.util.List;
import java.util.Locale;

public class ChrisProductReaderPlugIn implements ProductReaderPlugIn {

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if it
     * is capable of decoding the input's content.
     */
    public DecodeQualification getDecodeQualification(Object input) {
        final File file;
        if (input instanceof String) {
            file = new File((String) input);
        } else if (input instanceof File) {
            file = (File) input;
        } else {
            return DecodeQualification.UNABLE;
        }

        // @todo 2 rq/rq write test for this logic!
        if (file.isFile() && file.getPath().toLowerCase().endsWith(ChrisConstants.DEFAULT_FILE_EXTENSION)) {
            NetcdfFile ncFile = null;
            try {
                ncFile = NetcdfFile.open(file.getAbsolutePath());
                
                System.out.println(ncFile.toString());
                System.out.println();
                if (isSensorTypeAttributeCorrect(ncFile)) {
                    List<Attribute> globalNcAttributes = ncFile.getGlobalAttributes();
                    for (Attribute attribute : globalNcAttributes) {
                        System.out.println(attribute.getName() + " " + attribute.getDataType());
                    }
                    System.out.println();
                    return DecodeQualification.INTENDED;
                }
            } catch (Throwable e) {
                e.printStackTrace();
                // nothing to do, return value is already false
            } finally {
                if (ncFile != null) {
                    try {
                        ncFile.close();
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
        return new Class[]{String.class, File.class};
    }

    /**
     * Creates an instance of the actual product reader class. This method should never return <code>null</code>.
     *
     * @return a new reader instance, never <code>null</code>
     */
    public ProductReader createReaderInstance() {
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
        return new String[]{ChrisConstants.FORMAT_NAME};
    }

    private static boolean isSensorTypeAttributeCorrect(NetcdfFile ncFile) throws Exception {
        Attribute attribute = ncFile.findGlobalAttributeIgnoreCase("Sensor Type");
        return (attribute != null && 
                attribute.getDataType() == DataType.STRING &&
                attribute.getStringValue().equalsIgnoreCase("CHRIS"));
     }
}
