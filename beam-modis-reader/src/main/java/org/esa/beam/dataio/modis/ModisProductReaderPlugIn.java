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
package org.esa.beam.dataio.modis;

import ncsa.hdf.hdflib.HDFConstants;
import org.esa.beam.dataio.modis.hdf.HdfAttributes;
import org.esa.beam.dataio.modis.hdf.HdfUtils;
import org.esa.beam.dataio.modis.hdf.IHDF;
import org.esa.beam.dataio.modis.hdf.lib.HDF;
import org.esa.beam.dataio.modis.productdb.ModisProductDb;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.text.MessageFormat;
import java.util.Locale;

public class ModisProductReaderPlugIn implements ProductReaderPlugIn {

    // This is here just to keep the property name
//    private static final String HDF4_PROPERTY_KEY = "ncsa.hdf.hdflib.HDFLibrary.hdflib";

    private static final String _H4_CLASS_NAME = "ncsa.hdf.hdflib.HDFLibrary";
    private static boolean hdfLibAvailable = false;

    static {
        hdfLibAvailable = loadHdf4Lib(ModisProductReaderPlugIn.class) != null;
    }

    /**
     * @return whether or not the HDF4 library is available.
     */
    public static boolean isHdf4LibAvailable() {
        return hdfLibAvailable;
    }

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if it
     * is capable of decoding the input's content.
     */
    public DecodeQualification getDecodeQualification(Object input) {
        if (!isHdf4LibAvailable()) {
            return DecodeQualification.UNABLE;
        }

        File file = null;
        if (input instanceof String) {
            file = new File((String) input);
        } else if (input instanceof File) {
            file = (File) input;
        }

        if (file != null && file.exists() && file.isFile() && file.getPath().toLowerCase().endsWith(ModisConstants.DEFAULT_FILE_EXTENSION)) {
            try {
                String path = file.getPath();
                final IHDF ihdf = HDF.getWrap();
                if (ihdf.Hishdf(path)) {
                    int fileId = HDFConstants.FAIL;
                    int sdStart = HDFConstants.FAIL;
                    try {
                        fileId = ihdf.Hopen(path, HDFConstants.DFACC_RDONLY);
                        sdStart = ihdf.SDstart(path, HDFConstants.DFACC_RDONLY);
                        HdfAttributes globalAttrs = HdfUtils.readAttributes(sdStart);

                        // check wheter daac or imapp
                        ModisGlobalAttributes modisAttributes;
                        if (globalAttrs.getStringAttributeValue(ModisConstants.STRUCT_META_KEY) == null) {
                            modisAttributes = new ModisImappAttributes(file, sdStart, globalAttrs);
                        } else {
                            modisAttributes = new ModisDaacAttributes(globalAttrs);
                        }
                        final String productType = modisAttributes.getProductType();
                        if (ModisProductDb.getInstance().isSupportedProduct(productType)) {
                            return DecodeQualification.INTENDED;
                        }
                    } finally {
                        if (sdStart != HDFConstants.FAIL) {
                            ihdf.Hclose(sdStart);
                        }
                        if (fileId != HDFConstants.FAIL) {
                            ihdf.Hclose(fileId);
                        }
                    }
                }
            } catch (Exception ignore) {
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
        if (!isHdf4LibAvailable()) {
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
        if (!isHdf4LibAvailable()) {
            return null;
        }

        return new ModisProductReader(this);
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
        if (!isHdf4LibAvailable()) {
            return new String[0];
        }

        return new String[]{ModisConstants.DEFAULT_FILE_EXTENSION};
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p/>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param locale the local for the given decription string, if <code>null</code> the default locale is used
     * @return a textual description of this product reader/writer
     */
    public String getDescription(Locale locale) {
        return ModisConstants.READER_DESCRIPTION;
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    public String[] getFormatNames() {
        if (!isHdf4LibAvailable()) {
            return new String[0];
        }

        return new String[]{ModisConstants.FORMAT_NAME};
    }

    private static Class<?> loadHdf4Lib(Class<?> callerClass) {
        return loadClassWithNativeDependencies(callerClass,
                _H4_CLASS_NAME,
                "{0}: HDF-4 library not available: {1}: {2}");
    }

    private static Class<?> loadClassWithNativeDependencies(Class<?> callerClass, String className, String warningPattern) {
        ClassLoader classLoader = callerClass.getClassLoader();

        String classResourceName = "/" + className.replace('.', '/') + ".class";
        SystemUtils.class.getResource(classResourceName);
        if (callerClass.getResource(classResourceName) != null) {
            try {
                return Class.forName(className, true, classLoader);
            } catch (Throwable error) {
                BeamLogManager.getSystemLogger().warning(MessageFormat.format(warningPattern, callerClass, error.getClass(), error.getMessage()));
                return null;
            }
        } else {
            return null;
        }
    }
}
