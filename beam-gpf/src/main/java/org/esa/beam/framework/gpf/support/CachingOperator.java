package org.esa.beam.framework.gpf.support;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.LinkedList;
import java.util.List;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Raster;

import com.bc.ceres.core.ProgressMonitor;

/**
 * The <code>CachingOperator</code> provides a basis for the implementation of
 * new {@link org.esa.beam.framework.gpf.Operator} implementations. Clients must only implement the
 * {@link #computeTile(Band, Rectangle, ProductDataCache, ProgressMonitor)} and the
 * {@link #createTargetProduct(ProgressMonitor)} methods. The <code>CachingOperator</code> provides a
 * simple caching. It will hold the
 * last calculated rectangle in memory until a different one is requested.
 *
 * @author Maximilian Aulinger
 * @author Marco Zuehlke
 */
public abstract class CachingOperator extends AbstractOperator {

    private CachingStrategy cachingStrategy;
    protected Dimension maxTileSize;
    private Product targetProduct;

    protected CachingOperator(OperatorSpi spi) {
        super(spi);
    }

    /**
     * {@inheritDoc}
     * <p/>
     * <p>CAUTION: Overrides of this method must always call <code>super.init();</code>
     * after they have done their own initialisation.</code>
     */
    @Override
    public Product initialize(ProgressMonitor pm) throws OperatorException {
        targetProduct = createTargetProduct(pm);
        initCaching();
        initSourceRetriever();
        return targetProduct;
    }

    /**
     * Creates the output Product of the Operator. Clients must implement this
     * method for the initial output Product creation. This method is invoked by
     * {@link #initialize(org.esa.beam.framework.gpf.OperatorContext, com.bc.ceres.core.ProgressMonitor)}.
     *
     * @return the created output Product
     * @throws OperatorException
     * @deprecated no replacement
     */
    public abstract Product createTargetProduct(ProgressMonitor pm) throws OperatorException;

    /**
     * Specifies wether this operator can compute all its target bands
     * at once. Otherwise it has to be asked for each band individually.
     *
     * @return true, if all target bands can be computed at once
     */
    public abstract boolean isComputingAllBandsAtOnce();


    /**
     * @deprecated no replacement
     */
    @SuppressWarnings("unused")
    protected void initSourceRetriever() throws OperatorException {
        // do nothing
    }

    // todo - remove method
    /**
     * {@inheritDoc}
     * <p/>
     * <p>The override delegates the call to the {@link CachingStrategy}.
     */
    public final void computeTile(Band band,
                                  Rectangle rectangle,
                                  ProductData destBuffer,
                                  ProgressMonitor pm) throws OperatorException {
        cachingStrategy.computeTargetRaster(band, rectangle, destBuffer, pm);
    }

    // todo - move code from above to here!
    @Override
    public void computeBand(Raster targetRaster, ProgressMonitor pm) throws OperatorException {
        super.computeBand(targetRaster, pm);
    }

    // todo - remove method
    /**
     * Computes a new tile for the given band.
     * <p>This method is called by the framework if and only if
     * {@link #isComputingAllBandsAtOnce()} returns <code>false</code>.</p>
     * <p>The default implementation throws a runtime exception.</p>
     *
     * @param band      the target band for which the requested region is computed
     * @param rectangle a rectangle specifying the region to be computed
     * @param cache     the target cache to put the calculated raster data into.
     *                  Use the target band as key for the calculated rasterdata.
     * @throws OperatorException if some kind of error occure during computing (optional)
     * @see #computeTiles(java.awt.Rectangle, ProductDataCache, com.bc.ceres.core.ProgressMonitor)
     * @see #computeTile(org.esa.beam.framework.datamodel.Band, java.awt.Rectangle, org.esa.beam.framework.datamodel.ProductData, com.bc.ceres.core.ProgressMonitor)
     */
    @SuppressWarnings("unused")
    public void computeTile(Band band, Rectangle rectangle,
                            ProductDataCache cache, ProgressMonitor pm) throws OperatorException {
        throw new RuntimeException("not implemented");
    }

    // todo - remove method
    /**
     * Computes all tiles for all bands.
     * <p>This method is called by the framework if and only if
     * {@link #isComputingAllBandsAtOnce()} returns <code>true</code>.</p>
     * <p>The default implementation throws a runtime exception.</p>
     *
     * @param rectangle a rectangle specifying the region to be computed
     * @param cache     the target cache to put the calculated raster data into.
     *                  Use the target band as key for the calculated rasterdata.
     * @throws OperatorException if some kind of error occure during computing (optional)
     * @see #computeTile(org.esa.beam.framework.datamodel.Band, java.awt.Rectangle, ProductDataCache, com.bc.ceres.core.ProgressMonitor)
     * @see #computeTile(org.esa.beam.framework.datamodel.Band, java.awt.Rectangle, org.esa.beam.framework.datamodel.ProductData, com.bc.ceres.core.ProgressMonitor)
     */
    @SuppressWarnings("unused")
    public void computeTiles(Rectangle rectangle,
                             ProductDataCache cache, ProgressMonitor pm) throws OperatorException {
        throw new RuntimeException("not implemented");
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void dispose() {
        cachingStrategy.dispose();
    }


    private void initCaching() {
        //cachingStrategy = new SimpleCachingStrategy(this);
        final int width = targetProduct.getSceneRasterWidth();
        final int height = targetProduct.getSceneRasterHeight();
        maxTileSize = new Dimension(width, 40);
        int maxSize = 15;
//        if (this instanceof ReadProductOp) {
//            maxTileSize = new Dimension(width, 200);
//            maxSize = 3;
//        }
        RectangleIterator iterator = new RectangleIterator(maxTileSize, width, height);
        List<Rectangle> rectList = new LinkedList<Rectangle>();
        while (iterator.hasNext()) {
            rectList.add(iterator.next());
        }
        cachingStrategy = new TileCachingStrategy(this, rectList, maxSize);
    }
}