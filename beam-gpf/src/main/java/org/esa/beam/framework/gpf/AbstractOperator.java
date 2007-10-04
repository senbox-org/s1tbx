package org.esa.beam.framework.gpf;

import java.awt.Rectangle;
import java.util.Map;
import java.util.logging.Logger;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;

import com.bc.ceres.core.ProgressMonitor;

/**
 * The abstract base class for all operators.
 * <p>This class is intended to be extended by clients. At least two methods need to be implemented.
 * The method {@link #initialize() initialize()} has to be implemented always.
 * It should setup all the things the operator needs to run.</p>
 * <p/>
 * Additionally one of {@link #computeBand(Tile, ProgressMonitor)  computeBand()} and
 * {@link #computeAllBands(java.awt.Rectangle, ProgressMonitor) computeAllBands()} or even both
 * must be implemented. Which one to implement depends on the algorithm the operator executes.
 * But in general the {@link #computeBand(Tile, ProgressMonitor)  computeBand()} method should be
 * preferred, if the algorithm can be executed in this way.
 * The framework will automatically detect the implemented method and call this one.
 * If both are implemented it depends on the use case which one is called. The framework will also take care of
 * calling the appropriate one in this case.
 * <p/>
 * </p>
 */
public abstract class AbstractOperator implements Operator {

    private OperatorContext context;

    /**
     * Constructs a new operator. Note that only SPIs should directly create operators.
     */
    protected AbstractOperator() {
    }

    /**
     * Gets the operator context.
     *
     * @return the operator context, or <code>null</code> if the
     *         {@link #initialize(OperatorContext, ProgressMonitor)} method has not yet been called.
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
     * @return the source product, or {@code null} if not found
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
     * Gets a {@link Tile} for a given band and rectangle.
     *
     * @param rasterDataNode the raster data node of a data product,
     *                       e.g. a {@link org.esa.beam.framework.datamodel.Band} or
     *                       {@link org.esa.beam.framework.datamodel.TiePointGrid}.
     * @param rectangle      the raster rectangle in pixel coordinates
     * @return a tile.
     */
    public Tile getSourceTile(RasterDataNode rasterDataNode, Rectangle rectangle) throws OperatorException {
        return context.getSourceTile(rasterDataNode, rectangle);
    }

    /**
     * {@inheritDoc}
     * <p>The default implementation stores the given <code>OperatorContext</code> for later use and returns
     * {@link #initialize()}.</p>
     */
    public final Product initialize(OperatorContext context, ProgressMonitor pm) throws OperatorException {
        this.context = context;
        return initialize();
    }

    /**
     * Called by {@link #initialize(OperatorContext, ProgressMonitor)} after the {@link OperatorContext}
     * is stored.
     *
     * @return the target product
     * @see #initialize(OperatorContext, ProgressMonitor)
     */
    protected abstract Product initialize() throws OperatorException;

    /**
     * {@inheritDoc}
     * <p/>
     * <p>The default implementation throws a runtime exception with the message "not implemented"</p>.
     */
    public void computeTile(Band band, Tile targetTile) throws OperatorException {
    	throw new OperatorException("not implemented (only Band supported)");
   	}

    /**
     * {@inheritDoc}
     * <p/>
     * <p>The default implementation throws a runtime exception with the message "not implemented"</p>.
     */
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle) throws OperatorException {
        throw new OperatorException("not implemented");
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p>The default implementation does nothing.</p>
     */
    public void dispose() {
    }
    
    /**
     * Returns true, if the computation should be canceled
     * 
     * @return true if computation should be canceled
     */
    protected boolean isCancellationRequested() {
        return context.isCancellationRequested();
    }
    
    /**
     * Return a progress monitor, that can be used by an operator
     * that want's to show progress
     * 
     * @return a progress monitor
     */
    protected ProgressMonitor createProgressMonitor() {
        return ProgressMonitor.NULL;
    }
}
