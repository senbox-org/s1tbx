package org.esa.beam.dataio.envisat;

import org.esa.beam.framework.dataio.ProductIOException;
import org.esa.beam.framework.datamodel.ProductData;

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

/**
 * Created by IntelliJ IDEA.
 * User: lveci
 * Date: Apr 16, 2008
 * To change this template use File | Settings | File Templates.
 */
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
     * @throws java.lang.IllegalArgumentException
     *                             if <code>input</code> type is not one of the supported input sources.
     * @throws java.io.IOException if an I/O error occurs
     */
    public void readProduct(Object input) throws IOException {
        final String filePath;
        if (input instanceof String) {
            filePath = (String) input;
        } else if (input instanceof File) {
            filePath = ( (File) input).getPath();
        } else {
            throw new IllegalArgumentException("input");
        }

        File file = getFile(filePath);
        ImageInputStream iis = getImageInputStream(file);
        String productType = ProductFile.readProductType(iis);
        if (productType == null) {
            throw new IOException("Not an ENVISAT product or ENVISAT product type not supported: " + filePath);
        }
        // We use only the first 9 characters for comparision, since the 10th can be either 'P' or 'C'
        String productTypeUC = productType.toUpperCase().substring(0, 9);

        if (productTypeUC.startsWith("AS")) {
            _productFile = new AsarXCAProductFile(file, iis);
        } else if (productTypeUC.startsWith("DOR")) {
            _productFile = new DorisOrbitProductFile(file, iis);
        } else {
            throw new IOException("Not an ENVISAT product or ENVISAT product type not supported.");
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

        Record gads = _productFile.getGADS();
        if (gads == null) {
            throw new ProductIOException("GADS not found in Auxiliary data file");
        }

        Field field = gads.getField(name);
        if (field == null) {
            return null;
        }

        return field.getData();
    }

    /**
     * Closes the access to all currently opened resources such as file input streams and all resources of this children
     * directly owned by this reader. Its primary use is to allow the garbage collector to perform a vanilla job.
     * <p/>
     * <p>This method should be called only if it is for sure that this object instance will never be used again. The
     * results of referencing an instance of this class after a call to <code>close()</code> are undefined.
     * <p/>
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

    private File getFile(String filePath) throws FileNotFoundException {
        File file = null;

        String[] exts = new String[] {"", ".gz", ".zip"};
        for (String ext : exts) {
            URI fileUri = getFileURI(filePath + ext);
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
            throw new FileNotFoundException("ENVISAT auxiliary product not not found: " + filePath);
        }
        return file;
    }

    private ImageInputStream getImageInputStream(File file) throws IOException {
        if (file.getName().endsWith(".zip") || file.getName().endsWith(".gz")) {
            return new FileCacheImageInputStream(EnvisatProductReaderPlugIn.getInflaterInputStream(file), null);
        } else {
            return new FileImageInputStream(file);
        }
    }

    private URI getFileURI(String filePath) {
        URI fileUri = null;
        URL fileUrl = EnvisatAuxReader.class.getClassLoader().getResource(filePath);
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
