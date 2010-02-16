package org.esa.beam.processor.cloud.internal;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.IllegalFileFormatException;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.dataio.ProductReaderPlugIn;
import org.esa.beam.framework.dataio.ProductSubsetDef;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * <p><i><b>IMPORTANT NOTE:</b>
 * This class not an API.
 * It is not intended to be used by clients.</i>
 * </p>
 * <p/>
 * A ProcessingNode takes a single input product and creates an output product
 * using the same pixel resolution and the same spatial reference system as the input product.
 * <p/>
 * The method {@link #readFrameData(org.esa.beam.framework.datamodel.Band, int, int, int, int, org.esa.beam.framework.datamodel.ProductData)}
 * will continue using pre-cached and pre-computed data as long as the frame rectangle will not change.
 * <p>If the frame rectangle changes, the {@link #processFrame(int, int, int, int, ProgressMonitor)} method is called in order
 * to compute the data frame for each of the bands contained in the target product.
 */
public abstract class ProcessingNode implements ProductReader {

    // ProductReader specific fields
    private final ProcessingNodePlugIn plugIn;
    private Object input;

    // ProcessingNode specific fields
    private Product[] sourceProducts;
    private Product targetProduct;
    private Rectangle frameRectangle;
    private Map frameDataMap;
    private FrameSizeCalculator fsc;

    protected ProcessingNode(final ProcessingNodePlugIn plugIn) {
        this.plugIn = plugIn;
        this.frameRectangle = null;
        this.frameDataMap = new HashMap(31);
    }

    protected Product getTargetProduct() {
        return targetProduct;
    }

    protected Product getSourceProduct() {
        return getSourceProduct(0);
    }

    protected Product getSourceProduct(final int index) {
        return sourceProducts.length > 0 ? sourceProducts[index] : null;
    }

    public Product createTargetProduct(final Product[] sourceProducts) {
        this.sourceProducts = sourceProducts != null ? sourceProducts : new Product[0];
        this.targetProduct = createTargetProductImpl();
        this.targetProduct.setProductReader(this);
        return targetProduct;
    }

    public ProductData getFrameData(final String targetBandName) {
        final Band targetBand = getTargetBand(targetBandName);
        final ProductData frameData;
        if (targetBand != null) {
            frameData = getFrameData(targetBand);
        } else {
            frameData = null;
        }
        return frameData;
    }

    public Band getTargetBand(final String targetBandName) {
        return getTargetProduct().getBand(targetBandName);
    }

    public ProductData getFrameData(final Band targetBand) {
        if (frameRectangle == null) {
            return null;
        }
        ProductData frameData = (ProductData) frameDataMap.get(targetBand);
        final int numElems = frameRectangle.width * frameRectangle.height;
        if (frameData == null || frameData.getNumElems() != numElems) {
            frameData = targetBand.createCompatibleProductData(numElems);
            frameDataMap.put(targetBand, frameData);
        }
        return frameData;
    }

    public void setFrameSizeCalculator(final FrameSizeCalculator frameSizeCalculator) {
        fsc = frameSizeCalculator;
        final Rectangle rectangle = getMinFrameSize();
        fsc.addMinFrameSize(rectangle.width, rectangle.height);
    }

    public void startProcessing() throws Exception {
    }

    protected Rectangle getMaxFrameSize() {
        return fsc.getMaxFrameSize();
    }

    protected abstract Product createTargetProductImpl();

    protected abstract void processFrame(int frameX, int frameY, int frameW, int frameH, ProgressMonitor pm) throws
                                                                                                             IOException;

    protected void clearFrameDataMap() {
        frameDataMap.clear();
    }

    public Rectangle getMinFrameSize() {
        return new Rectangle(1, 1);
    }

    public void setUp(final Map config) throws IOException {
    }

    public void tearDown() throws IOException {
    }

    private void setFrameRectangle(final int frameX, final int frameY, final int frameW, final int frameH) {
        frameRectangle = new Rectangle(frameX, frameY, frameW, frameH);
    }

    private synchronized void readFrameData(final Band targetBand, final int frameX, final int frameY, final int frameW,
                                            final int frameH, final ProductData targetData) throws IOException {
        if (isNewFrame(frameX, frameY, frameW, frameH)) {
            setFrameRectangle(frameX, frameY, frameW, frameH);
            processFrame(frameX, frameY, frameW, frameH, ProgressMonitor.NULL);
        }
        final ProductData frameData = getFrameData(targetBand);
        copyFrameData(frameData, targetData, frameX, frameY, frameW, frameH);
    }

    private boolean isNewFrame(final int frameX, final int frameY, final int frameW, final int frameH) {
        return frameRectangle == null || !frameRectangle.contains(frameX, frameY, frameW, frameH);
    }

    private void copyFrameData(final ProductData sourceData,
                               final ProductData targetData,
                               final int targetX,
                               final int targetY,
                               final int targetW,
                               final int targetH) throws IOException {
        final Object sourceElems = sourceData.getElems();
        final Object targetElems = targetData.getElems();
        final int targetNumElems = targetData.getNumElems();
        if (sourceElems.getClass().equals(targetElems.getClass())) {
            if (frameRectangle.x == targetX &&
                frameRectangle.y == targetY &&
                frameRectangle.width == targetW &&
                frameRectangle.height == targetH) {
                //System.out.println("copy1");
                System.arraycopy(sourceElems, 0, targetElems, 0, targetNumElems);
            } else {
                //System.out.println("copy2");
                final int offsetY = targetY - frameRectangle.y;
                int sourceIndex = frameRectangle.width * offsetY + targetX;
                int targetIndex = 0;
                for (int y = offsetY; y < offsetY + targetH; y++) {
                    System.arraycopy(sourceElems, sourceIndex, targetElems, targetIndex, targetW);
                    sourceIndex += frameRectangle.width;
                    targetIndex += targetW;
                }
            }
        } else {
            // todo - What's the meaning of this code structure?
            for (int y = targetY; y < targetY + targetW; y++) {
                for (int x = targetX; x < targetX + targetH; x++) {
                    //targetData.setElemDoubleAt(index, sourceData.getElemDoubleAt(i));
                    System.out.println("ERROR: not supported !!!!!");
                    throw new IOException("unsupported type conversion");
                }
            }
        }
    }

    /////////////////////////////////////////////////////////////////////////
    // ProductReader interface implementations

    /**
     * Returns the plug-in which created this product reader.
     *
     * @return the product reader plug-in, should never be <code>null</code>
     */
    public ProductReaderPlugIn getReaderPlugIn() {
        return plugIn;
    }

    /**
     * Retrives the current source product.
     */
    public Object getInput() {
        return input;
    }

    /**
     * Returns null since subsets are not supported.
     *
     * @return null
     */
    public final ProductSubsetDef getSubsetDef() {
        return null;
    }

    /**
     * Reads a data product and returns a in-memory representation of it.
     * <p/>
     * <p> The given subset info can be used to specify spatial and spectral portions of the original proudct. If the
     * subset is omitted, the complete product is read in.
     * <p/>
     * <p> Whether the band data - the actual pixel values - is read in immediately or later when pixels are requested,
     * is up to the implementation.
     *
     * @param input     null, a {@link Product} or an array or collection of {@link Product}s
     * @param subsetDef not supported, must be null
     *
     * @throws IllegalArgumentException if <code>input</code> is <code>null</code> or it's type is not one of the
     *                                  supported input sources.
     * @throws java.io.IOException      if an I/O error occurs
     * @throws org.esa.beam.framework.dataio.IllegalFileFormatException
     *                                  if the file format is illegal
     */
    public Product readProductNodes(final Object input, final ProductSubsetDef subsetDef) throws IOException,
                                                                                                 IllegalFileFormatException {
        if (subsetDef != null) {
            throw new IllegalArgumentException("subsetDef != null (subsets are not supported)");
        }
        final Product[] sourceProducts;
        if (input == null) {
            sourceProducts = new Product[0];
        } else if (input instanceof Product) {
            sourceProducts = new Product[]{(Product) input};
        } else if (input instanceof Product[]) {
            sourceProducts = (Product[]) input;
        } else if (input instanceof Collection) {
            sourceProducts = (Product[]) ((Collection) input).toArray(new Product[0]);
        } else {
            throw new IllegalArgumentException("illegal input type");
        }
        this.input = input;
        return createTargetProduct(sourceProducts);
    }

    /**
     * Reads raster data from the data source specified by the given destination band into the given in-memory buffer
     * and region.
     * <p/>
     * <h3>Destination band</h3> The destination band is used to identify the data source from which this method
     * transfers the sample values into the given destination buffer. The method does not modify the given destination
     * band at all. If this product reader has a <code>ProductSubsetDef</code> instance attached to it, the method
     * should also consider the specified spatial subset and sub-sampling (if any) applied to the destination band.
     * <p/>
     * <h3>Destination region</h3> The given destination region specified by the <code>frameX</code>,
     * <code>frameY</code>, <code>frameW</code> and <code>frameH</code> parameters are given in the band's
     * raster co-ordinates of the raster which results <i>after</i> applying the optional spatial subset and
     * sub-sampling given by the <code>ProductSubsetDef</code> instance to the <i>data source</i>. If no spatial subset
     * and sub-sampling is specified, the destination co-ordinates are identical with the source co-ordinates. The
     * destination region should always specify a sub-region of the band's scene raster.
     * <p/>
     * <h3>Destination buffer</h3> The first element of the destination buffer corresponds to the given
     * <code>frameX</code> and <code>frameY</code> of the destination region. The offset parameters are
     * <b>not</b> an offset within the buffer.<br> The number of elements in the buffer exactly be <code>frameW *
     * frameH</code>. The pixel values read are stored in line-by-line order, so the raster X co-ordinate varies
     * faster than the Y co-ordinate.
     *
     * @param targetBand the destination band which identifies the data source from which to read the sample values
     * @param frameX     the X-offset in the band's raster co-ordinates
     * @param frameY     the Y-offset in the band's raster co-ordinates
     * @param frameW     the frameW of region to be read given in the band's raster co-ordinates
     * @param frameH     the frameH of region to be read given in the band's raster co-ordinates
     * @param targetData the destination buffer which receives the sample values to be read
     *
     * @throws java.io.IOException      if an I/O error occurs
     * @throws IllegalArgumentException if the number of elements destination buffer not equals <code>frameW *
     *                                  frameH</code> or the destination region is out of the band's scene raster
     * @see org.esa.beam.framework.datamodel.Band#getSceneRasterWidth()
     * @see org.esa.beam.framework.datamodel.Band#getSceneRasterHeight()
     */
    public void readBandRasterData(final Band targetBand,
                                   final int frameX,
                                   final int frameY,
                                   final int frameW,
                                   final int frameH,
                                   final ProductData targetData,
                                   ProgressMonitor pm) throws IOException {
        readFrameData(targetBand, frameX, frameY, frameW, frameH, targetData);
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
    public void close() throws IOException {
        clearFrameDataMap();
    }


}
