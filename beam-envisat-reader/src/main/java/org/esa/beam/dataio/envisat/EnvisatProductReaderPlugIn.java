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
package org.esa.beam.dataio.envisat;

import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.util.io.BeamFileFilter;

import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.MemoryCacheImageInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Locale;
import java.util.zip.GZIPInputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * The <code>EnvisatProductReaderPlugIn</code> class is an implementation of the <code>ProductReaderPlugIn</code>
 * interface exclusively for data products having the standard ESA/ENVISAT raw format.
 * <p/>
 * <p>XMLDecoder plug-ins are used to provide meta-information about a particular data format and to create instances of
 * the actual reader objects.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 * @see org.esa.beam.dataio.envisat.EnvisatProductReader
 */
public class EnvisatProductReaderPlugIn implements ProductReaderPlugIn {


    /**
     * Constructs a new ENVISAT product reader plug-in instance.
     */
    public EnvisatProductReaderPlugIn() {
    }

    /**
     * Returns a string array containing the single entry <code>&quot;ENVISAT&quot;</code>.
     */
    public String[] getFormatNames() {
        return new String[]{EnvisatConstants.ENVISAT_FORMAT_NAME};
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
        return new String[]{".N1", ".E1", ".E2", ".zip", ".gz"};
    }

    /**
     * Gets a short description of this plug-in. If the given locale is set to <code>null</code> the default locale is
     * used.
     * <p/>
     * <p> In a GUI, the description returned could be used as tool-tip text.
     *
     * @param name the local for the given decription string, if <code>null</code> the default locale is used
     * @return a textual description of this product reader/writer
     */
    public String getDescription(Locale name) {
        return "ENVISAT MERIS, AATSR and ASAR products";
    }

    /**
     * Checks whether the given object is an acceptable input for this product reader and if so, the method checks if
     * it's content has the ENVISAT format by checking if the first bytes in the file equals the ENVISAT magic file
     * string <code>PRODUCT=&quot;</code>.
     * <p/>
     * <p> ENVISAT product readers accept <code>java.lang.String</code> - a file path, <code>java.io.File</code> - an
     * abstract file path or a <code>javax.imageio.stream.ImageInputStream</code> - an already opened image input
     * stream.
     *
     * @param input the input object
     * @return <code>true</code> if the given input is an object referencing a physical ENVISAT data source.
     */
    public DecodeQualification getDecodeQualification(Object input) {
        if (input instanceof String) {
            // @todo 1 tb/tb check for zip extension and handle accordingly
            if (ProductFile.getProductType(new File((String) input)) != null) {
                return DecodeQualification.INTENDED;
            }
        } else if (input instanceof File) {
            final File inputFile = (File) input;
            if (ProductFile.getProductType(inputFile) != null) {
                return DecodeQualification.INTENDED;
            }

            final InputStream inputStream;
            try {
                inputStream = getInflaterInputStream(inputFile);
            } catch (IOException e) {
                return DecodeQualification.UNABLE;
            }

            final ImageInputStream dataInputStream = new MemoryCacheImageInputStream(inputStream);
            final String productType = ProductFile.getProductType(dataInputStream);
            try {
                dataInputStream.close();
                inputStream.close();
            } catch (IOException ignore) {
            }
            if (productType != null) {
                return DecodeQualification.INTENDED;
            }
        } else if (input instanceof ImageInputStream) {
            if (ProductFile.getProductType((ImageInputStream) input) != null) {
                return DecodeQualification.INTENDED;
            }
        }
        return DecodeQualification.UNABLE;
    }


    /**
     * Returns an array containing the classes that represent valid input types for an ENVISAT product reader.
     * <p/>
     * <p> Instances of the classes returned in this array are valid objects for the <code>readProductNodes</code>
     * method of the <code>AbstractProductReader</code> class (the method will not throw an
     * <code>InvalidArgumentException</code> in this case).
     *
     * @return an array containing valid input types, never <code>null</code>
     * @see org.esa.beam.framework.dataio.AbstractProductReader#readProductNodes
     */
    public Class[] getInputTypes() {
        return new Class[]{String.class, File.class, ImageInputStream.class, ProductFile.class};
    }

    /**
     * Creates an instance of the actual ENVISAT product reader class.
     *
     * @return a new instance of the <code>EnvisatProductReader</code> class
     */
    public ProductReader createReaderInstance() {
        return new EnvisatProductReader(this);
    }

    public BeamFileFilter getProductFileFilter() {
        return new BeamFileFilter(getFormatNames()[0], getDefaultFileExtensions(), getDescription(null));
    }

    /**
     * Opens an input stream for a compressed file (.gz or .zip).
     *
     * @param file the compressed file
     * @return the input stream
     * @throws java.io.IOException if an I/O error occurred
     */
    static InputStream getInflaterInputStream(File file) throws IOException {
        if (file.getName().endsWith(".gz")) {
            try {
                return createGZIPInputStream(file);
            } catch (IOException e) {
                // ok, try ZIP
            }
        }
        return createZIPInputStream(file);
    }

    private static InputStream createZIPInputStream(File file) throws IOException {
        final ZipFile productZip = new ZipFile(file, ZipFile.OPEN_READ);
        if (productZip.size() != 1) {
            throw new IllegalFileFormatException("Illegal ZIP format, single file entry expected.");
        }
        final Enumeration<? extends ZipEntry> entries = productZip.entries();
        final ZipEntry zipEntry = entries.nextElement();
        if (zipEntry == null || zipEntry.isDirectory()) {
            throw new IllegalFileFormatException("Illegal ZIP format, single file entry expected.");
        }
        return productZip.getInputStream(zipEntry);
    }

    private static InputStream createGZIPInputStream(File file) throws IOException {
        return new GZIPInputStream(new FileInputStream(file));
    }
}
