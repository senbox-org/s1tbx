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
package org.esa.beam.dataio.hdf5;

import org.esa.beam.framework.dataio.ProductWriter;
import org.esa.beam.framework.dataio.ProductWriterPlugIn;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.io.BeamFileFilter;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.text.MessageFormat;
import java.util.Locale;

/**
 * The <code>Hdf5ProductWriterPlugIn</code> class is the plug-in entry-point for the HDF5 product writer.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public class Hdf5ProductWriterPlugIn implements ProductWriterPlugIn {

    public static final String HDF5_FORMAT_NAME = "HDF5";
    public static final String HDF5_FILE_EXTENSION = ".h5";

    private static final String _H5_CLASS_NAME = "ncsa.hdf.hdf5lib.H5";

    private static boolean hdf5LibAvailable = false;

    static {
        hdf5LibAvailable = loadHdf5Lib(Hdf5ProductWriterPlugIn.class) != null;
    }

    /**
     * Constructs a new HDF5 product writer plug-in instance.
     */
    public Hdf5ProductWriterPlugIn() {
    }

    /**
     * Returns whether or not the HDF5 library is available.
     */
    public static boolean isHdf5LibAvailable() {
        return hdf5LibAvailable;
    }

    /**
     * Returns a string array containing the single entry <code>&quot;HDF5&quot;</code>.
     */
    public String[] getFormatNames() {
        if (!isHdf5LibAvailable()) {
            return new String[0];
        }
        return new String[]{HDF5_FORMAT_NAME};
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
        if (!isHdf5LibAvailable()) {
            return new String[0];
        }
        return new String[]{HDF5_FILE_EXTENSION};
    }

    /**
     * Returns an array containing the classes that represent valid output types for this HDF5 product writer.
     * <p/>
     * <p> Intances of the classes returned in this array are valid objects for the <code>writeProductNodes</code>
     * method of the <code>AbstractProductWriter</code> interface (the method will not throw an
     * <code>InvalidArgumentException</code> in this case).
     *
     * @return an array containing valid output types, never <code>null</code>
     *
     * @see org.esa.beam.framework.dataio.AbstractProductWriter#writeProductNodes
     */
    public Class[] getOutputTypes() {
        if (!isHdf5LibAvailable()) {
            return new Class[0];
        }
        return new Class[]{String.class, File.class};
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p/>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param name the local for the given decription string, if <code>null</code> the default locale is used
     *
     * @return a textual description of this product reader/writer
     */
    public String getDescription(Locale name) {
        return "HDF5 product writer";
    }

    /**
     * Creates an instance of the actual HDF5 product writer class.
     *
     * @return a new instance of the <code>Hdf5ProductWriter</code> class
     */
    public ProductWriter createWriterInstance() {
        if (!isHdf5LibAvailable()) {
            return null;
        }
        return new Hdf5ProductWriter(this);
    }

    public BeamFileFilter getProductFileFilter() {
        String[] formatNames = getFormatNames();
        String formatName = "";
        if (formatNames.length > 0) {
            formatName = formatNames[0];
        }

        return new BeamFileFilter(formatName, getDefaultFileExtensions(), getDescription(null));
    }

    private static Class<?> loadHdf5Lib(Class<?> callerClass) {
        return loadClassWithNativeDependencies(callerClass,
                _H5_CLASS_NAME,
                "{0}: HDF-5 library not available: {1}: {2}");
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
