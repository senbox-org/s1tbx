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

package org.esa.snap.dataio.envisat;

import org.esa.snap.core.dataio.IllegalFileFormatException;
import org.esa.snap.core.dataio.ProductIOException;
import org.esa.snap.core.datamodel.ProductData;

import javax.imageio.stream.FileCacheImageInputStream;
import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Date;


public class EnvisatAuxReader {

    /**
     * Represents the product's file.
     */
    protected ProductFile _productFile;

    public EnvisatAuxReader() {
    }

    /**
     * Reads a data product and returns a in-memory representation of it. This method was called by
     * <code>readProductNodes(input, subsetInfo)</code> of the abstract superclass.
     *
     * @param input A file or path to the aux file
     *
     * @throws java.lang.IllegalArgumentException if <code>input</code> type is not one of the supported input sources.
     * @throws java.io.IOException                if an I/O error occurs
     */
    public void readProduct(Object input) throws IOException {

        File file;
        if (input instanceof String) {
            file = getFile((String) input);
        } else if (input instanceof File) {
            file = (File) input;
            file = getFile(file.getAbsolutePath());
        } else {
            throw new IllegalArgumentException("input");
        }

        final ImageInputStream iis = getImageInputStream(file);
        final String productType = ProductFile.readProductType(iis);
        if (productType == null) {
            throw new IllegalFileFormatException("Not an ENVISAT product or ENVISAT product type not supported: " + file.toString());
        }
        // We use only the first 9 characters for comparison, since the 10th can be either 'P' or 'C'
        final String productTypeUC = productType.toUpperCase().substring(0, 9);

        if (productTypeUC.startsWith("AS")) {
            _productFile = new AsarXCAProductFile(file, iis);
        } else if (productTypeUC.startsWith("DOR")) {
            _productFile = new DorisOrbitProductFile(file, iis);
        } else {
            throw new IllegalFileFormatException("Not an ENVISAT product or ENVISAT product type not supported.");
        }

    }

    public Date getSensingStart() {
        return _productFile.getSensingStart();
    }

    public Date getSensingStop() {
        return _productFile.getSensingStop();
    }

    public ProductData getAuxData(String name) throws ProductIOException {
        if (_productFile == null) {
            throw new ProductIOException("Auxiliary data file has not been read yet");
        }

        final Record gads = _productFile.getGADS();
        if (gads == null) {
            throw new IllegalFileFormatException("GADS not found in Auxiliary data file");
        }

        final Field field = gads.getField(name);
        if (field == null) {
            return null;
        }

        return field.getData();
    }

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p>Overrides of this method should always call <code>super.close();</code> after disposing this instance.
     *
     * @throws IOException if an I/O error occurs
     */
    public void close() throws IOException {
        if (_productFile != null) {
            _productFile.close();
            _productFile = null;
        }
    }

    public static File getFile(String filePath) throws FileNotFoundException {
        File file = null;

        final String[] exts = new String[]{"", ".gz", ".zip"};
        for (String ext : exts) {
            final URI fileUri = getFileURI(filePath + ext);
            if (fileUri != null) {
                file = new File(fileUri);
                if (file.exists()) {
                    break;
                }
            }
            file = new File(filePath + ext);
            if (file.exists()) {
                break;
            }
        }
        if (file == null) {
            throw new FileNotFoundException("ENVISAT product not found: " + filePath);
        }
        return file;
    }

    private static ImageInputStream getImageInputStream(final File file) throws IOException {
        final String name = file.getName().toLowerCase();
        if (name.endsWith(".zip") || name.endsWith(".gz")) {
            return new FileCacheImageInputStream(EnvisatProductReaderPlugIn.getInflaterInputStream(file), null);
        } else {
            return new FileImageInputStream(file);
        }
    }

    private static URI getFileURI(final String filePath) {
        URI fileUri = null;
        final URL fileUrl = EnvisatAuxReader.class.getClassLoader().getResource(filePath);
        if (fileUrl != null) {
            try {
                fileUri = fileUrl.toURI();
            } catch (URISyntaxException e) {
                // ok
            }
        }
        return fileUri;
    }
}
