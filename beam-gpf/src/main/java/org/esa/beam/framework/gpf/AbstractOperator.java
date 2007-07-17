package org.esa.beam.framework.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.*;
import java.util.logging.Logger;

/**
 * The abstract base class for all operators.
 * <p>This class is intended to be extended by clients. Two methods need to be implemented:
 * <ul>
 * <li>{@link #initialize(com.bc.ceres.core.ProgressMonitor) initialize()}</li>
 * <li>{@link #computeTile(org.esa.beam.framework.datamodel.Band, java.awt.Rectangle, org.esa.beam.framework.datamodel.ProductData, com.bc.ceres.core.ProgressMonitor) computeTargetRaster()}</li>
 * </ul>
 * </p>
 */
public abstract class AbstractOperator implements Operator {

    private final OperatorSpi spi;
    private OperatorContext context;

    /**
     * Constructs a new operator. Note that only SPIs should directly create operators.
     *
     * @param spi the service provider interface
     */
    protected AbstractOperator(OperatorSpi spi) {
        this.spi = spi;
    }

    /**
     * Gets the operator context.
     *
     * @return the operator context, or <code>null</code> if the {@link #initialize(OperatorContext, com.bc.ceres.core.ProgressMonitor) init()} method has not yet been called.
     */
    public OperatorContext getContext() {
        return context;
    }

    /**
     * Gets a logger for the operator.
     */
    public Logger getLogger() {
        return context.getLogger();
    }

    /**
     * Gets the source product using the specified name.
     *
     * @param name the identifier
     * @return the source product, or <code>null</code> if not found
     */
    public Product getSourceProduct(String name) {
        return context.getSourceProduct(name);
    }

    /**
     * Gets the source products in the order they have been declared.
     *
     * @return the array source products
     */
    public Product[] getSourceProducts() {
        return context.getSourceProducts();
    }

    /**
     * Gets the target product for the operator.
     *
     * @return the target product
     */
    public Product getTargetProduct() {
        return context.getTargetProduct();
    }

    /**
     * Gets a {@link org.esa.beam.framework.gpf.Raster} for a given band and rectangle.
     *
     * @param rasterDataNode the raster data node of a data product, e.g. a {@link org.esa.beam.framework.datamodel.Band} or {@link org.esa.beam.framework.datamodel.TiePointGrid}.
     * @param rectangle  the raster rectangle in pixel coordinates
     * @return a tile.
     */
    public Raster getRaster(RasterDataNode rasterDataNode, Rectangle rectangle) throws OperatorException {
        return context.getRaster(rasterDataNode, rectangle, ProgressMonitor.NULL);
    }

    /**
     * Gets a {@link org.esa.beam.framework.gpf.Raster} for a given band and rectangle.
     *
     * @param rasterDataNode the raster data node of a data product, e.g. a {@link org.esa.beam.framework.datamodel.Band} or {@link org.esa.beam.framework.datamodel.TiePointGrid}.
     * @param rectangle  the raster rectangle in pixel coordinates
     * @param dataBuffer     a data buffer to be reused by the tile, its size must be equal to <code>tileRectangle.width * tileRectangle.height</code>
     * @return a tile which will reuse the given data buffer
     */
    public Raster getRaster(RasterDataNode rasterDataNode, Rectangle rectangle, ProductData dataBuffer) throws OperatorException {
        return context.getRaster(rasterDataNode, rectangle, dataBuffer, ProgressMonitor.NULL);
    }

    /**
     * {@inheritDoc}
     * <p>The default implementation returns the SPI passed to the constructor.</p>
     */
    public OperatorSpi getSpi() {
        return spi;
    }

    /**
     * {@inheritDoc}
     * <p>The default implementation stores the given <code>OperatorContext</code> for later use and returns
     * {@link #initialize(com.bc.ceres.core.ProgressMonitor)}.</p>
     */
    public final Product initialize(OperatorContext context, ProgressMonitor pm) throws OperatorException {
        this.context = context;
        return initialize(pm);
    }

    /**
     * Called by {@link #initialize(OperatorContext, com.bc.ceres.core.ProgressMonitor)} after the {@link OperatorContext}
     * is stored.
     *
     * @param pm a progress monitor. Can be used to signal pregress.
     * @return the target product
     * @see #initialize(OperatorContext, com.bc.ceres.core.ProgressMonitor)
     */
    protected abstract Product initialize(ProgressMonitor pm) throws OperatorException;

    /**
     * {@inheritDoc}
     * <p/>
     * <p>The default implementation throws a runtime exception with the message "not implemented"</p>.
     */
    public void computeBand(Raster targetRaster,
                            ProgressMonitor pm) throws OperatorException {
            throw new OperatorException("not implemented (only Band supported)");
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p>The default implementation throws a runtime exception with the message "not implemented"</p>.
     */
    public void computeAllBands(Rectangle targetTileRectangle,
                             ProgressMonitor pm) throws OperatorException {
        throw new OperatorException("not implemented");
    }

    /**
     * Computes a new tile. Reads raster data from the data source specified by the given destination
     * band into the given in-memory buffer and region. Clients must implement
     * this method to provide the calculation of the output raster data for the
     * given region and band.<p/>
     * <h3>Destination band</h3>
     * The destination band is used to identify the data source from which this
     * method transfers the sample values into the given destination buffer. The
     * method does not modify the given destination band at all. <p/>
     * <h3>Destination region</h3>
     * The given destination region specified by the <code>rectangle</code>,
     * The destination region should always specify a sub-region of the band's
     * scene raster. <p/>
     * <h3>Destination buffer</h3>
     * The number of elements in the buffer must exactly match
     * <code>rectangle.width * rectangle.height</code>. The pixel values read
     * are stored in line-by-line order, so the raster X co-ordinate varies
     * faster than the Y co-ordinate.
     * <p/>
     * <p>Clients are adviced to use the progress monitor <code>pm</code> if the method may take
     * a while to finish. The progress monitor informs the framework about the progress being made.</p>
     *
     * @param band          the destination band which identifies the data source from
     *                      which to read the sample values
     * @param tileRectangle a rectangle specifying the region to be computed
     * @param dataBuffer    the destination buffer which receives the sample values to be
     *                      read
     * @param pm            a progress monitor. Can be used to signal pregress.
     * @throws OperatorException        if some kind of error occure during computing (optional)
     * @throws IllegalArgumentException if the number of elements destination buffer not equals
     *                                  <code>destWidth destHeight</code> or the destination region
     *                                  is out of the band's scene raster
     * @see org.esa.beam.framework.datamodel.Band#getSceneRasterWidth()
     * @see org.esa.beam.framework.datamodel.Band#getSceneRasterHeight()
     * @deprecated use {@link Operator#computeBand(Raster,com.bc.ceres.core.ProgressMonitor)}
     */
    protected void computeTile(Band band, Rectangle tileRectangle, ProductData dataBuffer, ProgressMonitor pm)
            throws OperatorException {
        throw new OperatorException("not implemented");
    }


    /**
     * {@inheritDoc}
     * <p/>
     * <p>The default implementation does nothing.</p>
     */
    public void dispose() {
    }
}
