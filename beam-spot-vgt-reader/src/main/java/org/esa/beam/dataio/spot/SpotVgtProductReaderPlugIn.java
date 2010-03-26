/*
 * Copyright (C) 2010  by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 */
package org.esa.beam.dataio.spot;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertySet;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Locale;

public class SpotVgtProductReaderPlugIn implements ProductReaderPlugIn {

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if it
     * is capable of decoding the input's content.
     */
    public DecodeQualification getDecodeQualification(Object input) {
        File file = getFileInput(input);
        if (file == null) {
            return DecodeQualification.UNABLE;
        }

        FileNode fileNode = FileNode.create(file);
        if (fileNode == null) {
            return DecodeQualification.UNABLE;
        }

        try {
            try {
                Reader reader = fileNode.getReader(SpotVgtConstants.PHYS_VOL_FILENAME);
                if (reader == null) {
                    return DecodeQualification.UNABLE;
                }
                try {
                    PhysVolDescriptor descriptor = new PhysVolDescriptor(reader);
                    String[] strings = fileNode.list(descriptor.getLogVolDirName());
                    if (strings.length == 0) {
                        return DecodeQualification.UNABLE;
                    }
                } finally {
                    reader.close();
                }
                return DecodeQualification.INTENDED;
            } catch (IOException e) {
                return DecodeQualification.UNABLE;
            }
        } finally {
            fileNode.close();
        }
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
        return new SpotVgtProductReader(this);
    }

    /**
     * Gets an instance of {@link org.esa.beam.util.io.BeamFileFilter} for use in a {@link javax.swing.JFileChooser JFileChooser}.
     *
     * @return a file filter
     */
    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(SpotVgtConstants.FORMAT_NAME,
                                  getDefaultFileExtensions(),
                                  getDescription(null));
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
        return new String[]{
                ".txt", ".TXT", ".zip", ".ZIP"
        };
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
        return SpotVgtConstants.READER_DESCRIPTION;
    }

    /**
     * Gets the names of the product formats handled by this product I/O plug-in.
     *
     * @return the names of the product formats handled by this product I/O plug-in, never <code>null</code>
     */
    public String[] getFormatNames() {
        return new String[]{SpotVgtConstants.FORMAT_NAME};
    }

    static String getBandName(String name) {
        int p1 = name.indexOf("_");
        int p2 = name.lastIndexOf('.');
        return name.substring(p1 == -1 ? 0 : p1 + 1, p2);
    }

    static PropertySet readPhysVolDescriptor(File inputFile) throws IOException {
        return readKeyValuePairs(inputFile);
    }

    static PropertySet readKeyValuePairs(File inputFile) throws IOException {
        return readKeyValuePairs(new FileReader(inputFile));
    }

    static PropertySet readKeyValuePairs(Reader reader) throws IOException {
        BufferedReader breader = new BufferedReader(reader);
        try {
            PropertySet headerProperties = new PropertyContainer();
            String line;
            while ((line = breader.readLine()) != null) {
                line = line.trim();
                int i = line.indexOf(' ');
                String key, value;
                if (i > 0) {
                    key = line.substring(0, i);
                    value = line.substring(i + 1).trim();
                } else {
                    key = line;
                    value = "";
                }
                headerProperties.addProperty(Property.create(key, value));
            }
            return headerProperties;
        } finally {
            breader.close();
        }
    }

    static File getFileInput(Object input) {
        if (input instanceof String) {
            return getFileInput(new File((String) input));
        } else if (input instanceof File) {
            return getFileInput((File) input);
        }
        return null;
    }

    static File getFileInput(File file) {
        if (file.isDirectory()) {
            return file;
        } else if (file.getName().endsWith(".zip") || file.getName().endsWith(".ZIP")) {
            return file;
        } else if (file.getName().endsWith(".txt") || file.getName().endsWith(".TXT")) {
            return file.getParentFile();
        }
        return null;
    }
}
