package org.esa.beam.dataio.envisat;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.dataio.ProductIOException;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.FileCacheImageInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
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
     * @param input A file or path to the aux file
     * @throws java.lang.IllegalArgumentException
     *                             if <code>input</code> type is not one of the supported input sources.
     * @throws java.io.IOException if an I/O error occurs
     */
    public void readProduct(Object input) throws IOException {
        File file;

        if (input instanceof String) {
            java.net.URL resURL = EnvisatAuxReader.class.getClassLoader().getResource((String) input);
            if (resURL != null) {
                file = new File(resURL.getPath());
            } else {
                file = new File((String) input);
            }

        } else if(input instanceof File) {
            file = (File)input;
        } else {
            throw new ProductIOException("readProduct input is not a File or path");
        }

        ImageInputStream imgInputStream = null;
        if(file.exists()) {
            imgInputStream = new FileImageInputStream(file);
        } else {
            file = new File(file.getAbsolutePath() + ".zip");
            final InputStream inputStream = EnvisatProductReaderPlugIn.getCompressedInputStream(file);
            if(inputStream == null)
                throw new IOException("ENVISAT aux product " + file.getAbsolutePath() + " not found");

            imgInputStream = new FileCacheImageInputStream(inputStream, null);
        }

        String productType = ProductFile.readProductType(imgInputStream);
        if (productType == null) {
            throw new IOException("not an ENVISAT product or ENVISAT product type not supported");
        }
        // We use only the first 9 characters for comparision, since the 10th can be either 'P' or 'C'
        String productTypeUC = productType.toUpperCase().substring(0, 9);

        ProductFile productFile = null;

        if (productTypeUC.startsWith("AS")) {
             _productFile = new AsarXCAProductFile(file, imgInputStream);
        } else if(productTypeUC.startsWith("DOR")) {
             _productFile = new DorisOrbitProductFile(file, imgInputStream);
        } else {
             throw new IOException("not an ENVISAT product or ENVISAT product type not supported");
        }

    }

    public Date getSensingStart()
    {
        return _productFile.getSensingStart();
    }

    public Date getSensingStop()
    {
        return _productFile.getSensingStop();
    }

    public ProductData getAuxData(String name) throws ProductIOException
    {
        if(_productFile == null)
            throw new ProductIOException("Auxiliary data file has not been read yet");

        Record gads = _productFile.getGADS();
        if(gads == null)
            throw new ProductIOException("GADS not found in Auxiliary data file");

        Field field = gads.getField(name);
        if(field == null)
            return null;

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

}
