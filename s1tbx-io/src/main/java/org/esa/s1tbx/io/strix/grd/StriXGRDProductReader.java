
package org.esa.s1tbx.io.strix.grd;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.s1tbx.commons.io.S1TBXProductReaderPlugIn;
import org.esa.s1tbx.commons.io.SARReader;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;

/**
 * The product reader for Synspective StriX GRD products.
 */
public class StriXGRDProductReader extends SARReader {

    private StriXGRDProductDirectory dataDir;
    private final S1TBXProductReaderPlugIn readerPlugIn;

    /**
     * Constructs a new abstract product reader.
     *
     * @param readerPlugIn the reader plug-in which created this reader, can be <code>null</code> for internal reader
     *                     implementations
     */
    public StriXGRDProductReader(final S1TBXProductReaderPlugIn readerPlugIn) {
        super(readerPlugIn);
        this.readerPlugIn = readerPlugIn;
    }

    @Override
    public void close() throws IOException {
        if (dataDir != null) {
            dataDir.close();
            dataDir = null;
        }
        super.close();
    }

    /**
     * Provides an implementation of the <code>readProductNodes</code> interface method. Clients implementing this
     * method can be sure that the input object and eventually the subset information has already been set.
     * <p>
     * <p>This method is called as a last step in the <code>readProductNodes(input, subsetInfo)</code> method.
     *
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected Product readProductNodesImpl() throws IOException {
        try {
            Object input = getInput();
            if (input instanceof InputStream) {
                throw new IOException("InputStream not supported");
            }

            final Path path = getPathFromInput(input);
            File metadataFile = readerPlugIn.findMetadataFile(path);

            dataDir = new StriXGRDProductDirectory(metadataFile);
            dataDir.readProductDirectory();
            final Product product = dataDir.createProduct();

            addCommonSARMetadata(product);
            product.getGcpGroup();
            product.setFileLocation(metadataFile);
            product.setProductReader(this);

            return product;
        } catch (Throwable e) {
            handleReaderException(e);
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    protected void readBandRasterDataImpl(int sourceOffsetX, int sourceOffsetY, int sourceWidth, int sourceHeight,
                                          int sourceStepX, int sourceStepY, Band destBand, int destOffsetX,
                                          int destOffsetY, int destWidth, int destHeight, ProductData destBuffer,
                                          ProgressMonitor pm) {
    }
}
