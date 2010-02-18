package org.esa.beam.dataio.ceos.avnir2;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.dataio.ceos.CeosHelper;
import org.esa.beam.dataio.ceos.IllegalCeosFormatException;
import org.esa.beam.framework.dataio.AbstractProductReader;
import org.esa.beam.framework.dataio.DecodeQualification;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.util.TreeNode;

import java.io.File;
import java.io.IOException;

/**
 * The product reader for Avnir-2 products.
 *
 * @author Marco Peters
 */
public class Avnir2ProductReader extends AbstractProductReader {

    private Avnir2ProductDirectory _avnir2Dir;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public Avnir2ProductReader(final ProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
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
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    public void close() throws IOException {
        if (_avnir2Dir != null) {
            _avnir2Dir.close();
            _avnir2Dir = null;
        }
        super.close();
    }

    /**
     * Retrieves a set of TreeNode objects that represent the physical product structure as stored on the harddrive.
     * The tree consisty of:
     * - a root node (the one returned) pointing to the directory that CONTAINS the product
     * - any number of nested children that compose the product.
     * Each TreeNod is configured as follows:
     * - id: contains a string representation of the path. For the root node, this is the
     * absolute path to the parent of the file returned by Product.getFileLocation().
     * For all subsequent nodes, the node name.
     * - content: each node stores as content a java.io.File object that physically defines the node.
     * <p/>
     * The method returns null when a TreeNode can not be assembled (i.e. in-memory product, created from stream ...)
     *
     * @return the root TreeNode or null
     */
    @Override
    public TreeNode<File> getProductComponents() {
        final File input = CeosHelper.getFileFromInput(getInput());
        if (input == null) {
            return null;
        }

        return _avnir2Dir.getProductComponents();
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p/>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws java.io.IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        final ProductReaderPlugIn readerPlugIn = getReaderPlugIn();
        final Object input = getInput();
        if (readerPlugIn.getDecodeQualification(input) == DecodeQualification.UNABLE) {
            throw new IOException("Unsupported product format."); /*I18N*/
        }
        final File fileFromInput = CeosHelper.getFileFromInput(getInput());
        Product product;
        try {
            _avnir2Dir = new Avnir2ProductDirectory(fileFromInput.getParentFile());
            product = _avnir2Dir.createProduct();
        } catch (IllegalCeosFormatException e) {
            final IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }
        product.setProductReader(this);
        product.setModified(false);

        return product;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) throws IOException {
        try {
            final Avnir2ImageFile imageFile = _avnir2Dir.getImageFile(destBand);
            imageFile.readBandRasterData(sourceOffsetX, sourceOffsetY,
                    sourceWidth, sourceHeight,
                    sourceStepX, sourceStepY,
                    destOffsetX, destOffsetY,
                    destWidth, destHeight,
                    destBuffer, pm);
        } catch (IllegalCeosFormatException e) {
            final IOException ioException = new IOException(e.getMessage());
            ioException.initCause(e);
            throw ioException;
        }

    }
}
