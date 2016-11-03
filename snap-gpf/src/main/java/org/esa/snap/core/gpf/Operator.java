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

package org.esa.snap.core.gpf;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.datamodel.TiePointGrid;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.annotations.TargetProperty;
import org.esa.snap.core.gpf.internal.OperatorContext;

import javax.media.jai.BorderExtender;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.text.MessageFormat;
import java.util.Map;
import java.util.logging.Logger;


/**
 * The abstract base class for all operators intended to be extended by clients.
 * <p>The following methods are intended to be implemented or overridden:
 * <ul>
 * <li>{@link #initialize()}: must be implemented in order to initialise the operator and create the target
 * product.</li>
 * <li>{@link #computeTile(Band, Tile, ProgressMonitor) computeTile()}: implemented to compute the tile
 * for a single band.</li>
 * <li>{@link #computeTileStack(Map, Rectangle, ProgressMonitor)}: implemented to compute the tiles
 * for multiple bands.</li>
 * <li>{@link #dispose()}: can be overridden in order to free all resources previously allocated by the operator.</li>
 * </ul>
 *
 * <p>Generally, only one {@code computeTile} method needs to be implemented. It depends on the type of algorithm which
 * of both operations is most advantageous to implement:
 * <ol>
 * <li>If bands can be computed independently of each other, then it is
 * beneficial to implement the {@code computeTile()} method. This is the case for sub-sampling, map-projections,
 * band arithmetic, band filtering and statistic analyses.</li>
 * <li>{@code computeTileStack()} should be overridden in cases where the bands of a product cannot be computed independently, e.g.
 * because they are a simultaneous output. This is often the case for algorithms based on neural network, cluster analyses,
 * model inversion methods or spectral unmixing.</li>
 * </ol>
 * <p>For information on how to best implement the {@code computeTile()} or {@code computeTileStack()} method please
 * read also the {@link Tile} documentation.
 *
 * <p>The framework execute either the {@code computeTile()} or the {@code computeTileStack()} method
 * based on the current use case or request.
 * If tiles for single bands are requested, e.g. for image display, it will always prefer an implementation of
 * the {@code computeTile()} method and call it.
 * If all tiles are requested at once, e.g. writing a product to disk, it will attempt to use the {@code computeTileStack()}
 * method. If the framework cannot use its preferred operation, it will use the one implemented by the operator.
 *
 * @author Norman Fomferra
 * @author Marco Peters
 * @author Marco Zühlke
 * @see OperatorSpi
 * @see OperatorMetadata
 * @see Parameter
 * @see TargetProduct
 * @see TargetProperty
 * @see SourceProduct
 * @see SourceProducts
 * @see Tile
 *
 * @since 4.1
 */
public abstract class Operator {

    final OperatorContext context;

    /**
     * Constructs a new operator.
     */
    protected Operator() {
        context = new OperatorContext(this);
    }

    /**
     * Sets the operator parameters to their default values, if any.
     */
    public void setParameterDefaultValues() {
        context.getParameterSet().setDefaultValues();
    }

    /**
     * Overridden in order to force a call to {@link #dispose()}, if not already done.
     *
     * @throws Throwable The {@code Exception} raised by this method
     */
    @Override
    protected final void finalize() throws Throwable {
        context.dispose();
        super.finalize();
    }

    /**
     * Initializes this operator and sets the one and only target product.
     * <p>The target product can be either defined by a field of type {@link Product} annotated with the
     * {@link TargetProduct TargetProduct} annotation or
     * by calling {@link #setTargetProduct} method.
     *
     * <p> This method shall never be called directly. The framework calls this method after it has created
     * an instance of this {@code Operator}. This will occur
     * only once durting the lifetime of an {@code Operator} instance.
     * If not already done, calling the {@link #getTargetProduct()} will always trigger
     * a call to the {@code initialize()} method.
     *
     * Any client code that must be performed before computation of tile data
     * should be placed here.
     *
     * @throws OperatorException If an error occurs during operator initialisation.
     * @see #getTargetProduct()
     */
    public abstract void initialize() throws OperatorException;

    /**
     * Updates this operator forcing it to recreate the target product.
     */
    public final void update() {
        context.updateOperator();
    }

    /**
     * Executes the operator.
     * <p/>
     * Call this method to execute an operator that doesn't compute raster data tiles on its own.
     *
     * @param pm A progress monitor to be notified for long-running tasks.
     */
    public final void execute(ProgressMonitor pm) {
        getTargetProduct();
        context.executeOperator(pm);
    }

    /**
     * Executes the operator.
     * <p>
     * For operators that compute raster data tiles, the method is usually a no-op. Other operators might perform their
     * main work in this method, e.g. perform some image analysis such as extracting statistics and other features from
     * data products.
     * <p>
     * Don't call this method directly. The framework may call this method
     * <ol>
     * <li>once before the very first tile is computed, or</li>
     * <li>as a result of a call to {@link #execute(ProgressMonitor)}.</li>
     * </ol>
     * <p>
     * The default implementation does nothing.
     *
     * @param pm A progress monitor to be notified for long-running tasks.
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    public void doExecute(ProgressMonitor pm) throws OperatorException {
    }

    // todo - remove ProgressMonitor parameter, it has never been used and wastes processing time (nf - 17.12.2010)

    /**
     * Called by the framework in order to compute a tile for the given target band.
     * <p>The default implementation throws a runtime exception with the message "not implemented".
     * <p>This method shall never be called directly.
     *
     * @param targetBand The target band.
     * @param targetTile The current tile associated with the target band to be computed.
     * @param pm         A progress monitor which should be used to determine computation cancellation requests.
     * @throws OperatorException If an error occurs during computation of the target raster.
     */
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        throw new RuntimeException(
                MessageFormat.format("{0}: ''computeTile()'' method not implemented", getClass().getSimpleName()));
    }

    // todo - remove ProgressMonitor parameter, it has never been used and wastes processing time (nf - 17.12.2010)

    /**
     * Called by the framework in order to compute the stack of tiles for the given target bands.
     * <p>The default implementation throws a runtime exception with the message "not implemented".
     * <p>This method shall never be called directly.
     *
     * @param targetTiles     The current tiles to be computed for each target band.
     * @param targetRectangle The area in pixel coordinates to be computed (same for all rasters in {@code targetRasters}).
     * @param pm              A progress monitor which should be used to determine computation cancellation requests.
     * @throws OperatorException If an error occurs during computation of the target rasters.
     */
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws
            OperatorException {
        throw new RuntimeException(
                MessageFormat.format("{0}: ''computeTileStack()'' method not implemented", getClass().getSimpleName()));
    }

    /**
     * Releases the resources the operator has acquired during its lifetime.
     * The default implementation does nothing.
     * <p>
     * Overrides should make sure to call {@code super.dispose()} as well.
     */
    public void dispose() {
    }

    /**
     * Determines whether this operator's {@code computeTileStack} method can be used.
     * <p>
     * The default implementation of this method checks if the this operator's class
     * overrides the {@code Operator.computeTileStack} method.
     *
     * @return {@code true} if so.
     * @since SNAP 3.0
     */
    public boolean canComputeTile() {
        return context.isComputeTileMethodImplemented();
    }

    /**
     * Determines whether this operator's {@code computeTileStack} method can be used.
     * <p>
     * The default implementation of this method checks if the this operator's class
     * overrides the {@code Operator.computeTileStack} method.
     *
     * @return {@code true} if so.
     * @since SNAP 3.0
     */
    public boolean canComputeTileStack() {
        return context.isComputeTileStackMethodImplemented();
    }

    /**
     * Deactivates the {@link #computeTile(Band, Tile, ProgressMonitor) computeTile}
     * method. This method can be called from within the {@link #initialize()} method if the current operator configuration prevents
     * the computation of tiles of individual, independent target bands.
     *
     * @throws IllegalStateException if the {@link #computeTileStack(Map, Rectangle, ProgressMonitor) computeTileStack} method is not implemented
     * @deprecated since SNAP 3.0. Override {@link #canComputeTile()} instead.
     */
    @Deprecated
    protected final void deactivateComputeTileMethod() throws IllegalStateException {
        if (!canComputeTileStack()) {
            throw new IllegalStateException("!canComputeTileStack()");
        }
        //context.setComputeTileMethodImplemented(false);
    }

    /**
     * Provides the context product manager which can be used to exchange product instances across operators
     * or allow (reading) operators to check if a given product is already opened.
     *
     * @return A context product manager.
     * @since SNAP 3.0
     */
    public ProductManager getProductManager() {
        return GPF.getDefaultInstance().getProductManager();
    }

    // todo - seems not very helpful, only usage in WriteOp (nf - 17.12.2010)

    protected final void setRequiresAllBands(boolean requiresAllBands) {
        context.setRequiresAllBands(requiresAllBands);
    }

    /**
     * @return The operator's runtime identifier assigned by the framework.
     */
    public final String getId() {
        return context.getId();
    }

    /**
     * Gets the source products in the order they have been declared.
     *
     * @return The array source products.
     */
    public final Product[] getSourceProducts() {
        return context.getSourceProducts();
    }

    /**
     * Sets the source products.
     *
     * @param products The source products.
     * @since BEAM 4.2
     */
    public final void setSourceProducts(Product... products) {
        Assert.notNull(products, "products");
        context.setSourceProducts(products);
    }

    /**
     * Gets a single source product. This method is a shortcut for
     * {@code getSourceProduct("sourceProduct")}.
     *
     * @return The source product, or {@code null} if not set.
     * @since BEAM 4.2
     */
    public Product getSourceProduct() {
        Product product = getSourceProduct(GPF.SOURCE_PRODUCT_FIELD_NAME);
        if (product != null) {
            return product;
        }
        Product[] products = getSourceProducts();
        if (products.length > 0) {
            return products[0];
        }
        return null;
    }

    /**
     * Sets a single source product. This method is a shortcut for
     * {@code setSourceProduct("sourceProduct", sourceProduct)}.
     *
     * @param sourceProduct the source product to be set
     * @since BEAM 4.2
     */
    public void setSourceProduct(Product sourceProduct) {
        setSourceProduct(GPF.SOURCE_PRODUCT_FIELD_NAME, sourceProduct);
    }

    /**
     * Gets the source product using the specified name.
     *
     * @param id the identifier
     * @return the source product, or {@code null} if not found
     * @see #getSourceProductId(Product)
     */
    public final Product getSourceProduct(String id) {
        Assert.notNull(id, "id");
        return context.getSourceProduct(id);
    }

    /**
     * Sets a source product.
     * One product instance can be registered with different identifiers, e.g. "source", "source1" and "input"
     * in consecutive calls.
     *
     * @param id      a source product identifier
     * @param product the source product to be set
     * @since BEAM 4.2
     */
    public final void setSourceProduct(String id, Product product) {
        Assert.notNull(id, "id");
        Assert.notNull(product, "product");
        context.setSourceProduct(id, product);
    }

    /**
     * Gets the identifier for the given source product.
     *
     * @param product The source product.
     * @return The identifier, or {@code null} if no such exists.
     * @see #getSourceProduct(String)
     */
    public final String getSourceProductId(Product product) {
        Assert.notNull(product, "product");
        return context.getSourceProductId(product);
    }

    /**
     * Gets the target product for the operator.
     * <p>If the target product is not set, calling this method results in a
     * call to {@link #initialize()}.
     *
     * @return The target product.
     * @throws OperatorException May be caused by {@link #initialize()}, if the operator is not initialised,
     *                           or if the target product is not set.
     */
    public final Product getTargetProduct() throws OperatorException {
        return context.getTargetProduct();
    }

    /**
     * Sets the target product for the operator.
     * <p>Must be called from within the {@link #initialize()} method.
     *
     * @param targetProduct The target product.
     */
    public final void setTargetProduct(Product targetProduct) {
        context.setTargetProduct(targetProduct);
    }

    /**
     * Gets a target property of the operator.
     * <p>If the requested target property is not set, calling this method results in a
     * call to {@link #initialize()}.
     *
     * @param name the name of the property requested.
     * @return the target property requested.
     * @throws OperatorException May be caused by {@link #initialize()}, if the operator is not initialised,
     *                           or if the target product is not been set.
     */
    public final Object getTargetProperty(String name) throws OperatorException {
        return context.getTargetProperty(name);
    }

    /**
     * Gets the value for the parameter with the given name.
     *
     * @param name The parameter name.
     * @return The parameter value, which may be {@code null}.
     * @since BEAM 4.7
     */
    public Object getParameter(String name) {
        return context.getParameter(name);
    }

    /**
     * Gets the value for the parameter with the given name.
     *
     * @param name         The parameter name.
     * @param defaultValue The default value which is used in case {@link #getParameter(String)} returns {@code null}. May be {@code null}.
     * @return The parameter value, or the given {@code defaultValue}.
     * @since BEAM 5.0
     */
    public Object getParameter(String name, Object defaultValue) {
        Object parameter = context.getParameter(name);
        return parameter != null ? parameter : defaultValue;
    }

    /**
     * Sets the value for the parameter with the given name.
     *
     * @param name  The parameter name.
     * @param value The parameter value, which may be {@code null}.
     * @since BEAM 4.7
     */
    public void setParameter(String name, Object value) {
        context.setParameter(name, value);
    }

    /**
     * Gets a {@link Tile} for a given band and image region.
     *
     * @param rasterDataNode the raster data node of a data product,
     *                       e.g. a {@link Band Band} or
     *                       {@link TiePointGrid TiePointGrid}.
     * @param region         the image region in pixel coordinates
     * @return a tile.
     * @throws OperatorException if the tile request cannot be processed
     */
    public final Tile getSourceTile(RasterDataNode rasterDataNode, Rectangle region)
            throws OperatorException {
        return context.getSourceTile(rasterDataNode, region);
    }

    /**
     * Gets a {@link Tile} for a given band and image region. The region can overlap the bounds of source image.
     * This method is particularly useful if you need to compute target pixels from an n x m region around a corresponding source pixel.
     * In this case an extended tile will need to be read from the source.
     *
     * @param rasterDataNode the raster data node of a data product,
     *                       e.g. a {@link Band Band} or
     *                       {@link TiePointGrid TiePointGrid}.
     * @param region         The image region in pixel coordinates
     * @param borderExtender A strategy used to fill the raster regions that lie outside the bounds of the source image.
     * @return A tile whose region can overlap the bounds of source image.
     * @throws OperatorException if the tile request cannot be processed
     * @since BEAM 4.7.1
     */
    public final Tile getSourceTile(RasterDataNode rasterDataNode,
                                    Rectangle region,
                                    BorderExtender borderExtender) throws OperatorException {
        return context.getSourceTile(rasterDataNode, region, borderExtender);
    }

    /**
     * Checks for cancellation of the current processing request. Throws an exception, if the
     * request has been canceled (e.g. by the user).
     *
     * @throws OperatorException if the current processing request has been canceled (e.g. by the user).
     */
    protected final void checkForCancellation() throws OperatorException {
        context.checkForCancellation();
    }

    /**
     * Ensures that the given source products all have a scene geo-coding.
     * Operator implementations may use this method in their {@link #initialize()} method to ensure that their
     * sources are geo-coded.
     *
     * @param products The products to test.
     * @throws OperatorException if any product has no geo-coding.
     * @since SNAP 3
     */
    protected void ensureSceneGeoCoding(Product... products) throws OperatorException {
        for (Product product : products) {
            if (product.getSceneGeoCoding() == null) {
                throw new OperatorException(String.format("Source product '%s' must be geo-coded.", product.getName()));
            }
        }
    }

    /**
     * Ensures that the given source products only contain raster data nodes having the same size in pixels and that all
     * products have the same scene raster size.
     * Operator implementations may use this method in their {@link #initialize()} method if they can only deal with
     * single-size sources.
     *
     * @param products Source products products to test.
     * @return the unique raster size, {@code null} if {@code products} is an empty array
     * @throws OperatorException if the product contains multi-size rasters.
     * @since SNAP 3
     */
    protected Dimension ensureSingleRasterSize(Product... products) throws OperatorException {
        Dimension sceneRasterSize = null;
        for (Product product : products) {
            if (product.isMultiSize()) {
                String message = String.format("Source product '%s' contains rasters of different sizes and can not be processed.\n" +
                                                       "Please consider resampling it so that all rasters have the same size.",
                                               product.getName());
                throw new OperatorException(message);
            }
            if (sceneRasterSize == null) {
                sceneRasterSize = product.getSceneRasterSize();
            } else if (!product.getSceneRasterSize().equals(sceneRasterSize)) {
                throw new OperatorException(String.format("All source products must have the same raster size of %d x %d pixels.",
                                                          sceneRasterSize.width,
                                                          sceneRasterSize.height));
            }
        }
        return sceneRasterSize;
    }

    /**
     * Ensures that the given raster data nodes only contain raster data nodes having the same size in pixels.
     *
     * @param rasterDataNodes Other optional products to test.
     * @return the unique raster size, {@code null} if {@code rasterDataNodes} is an empty array
     * @throws OperatorException if the product contains multi-size rasters.
     * @since SNAP 3
     */
    protected Dimension ensureSingleRasterSize(RasterDataNode... rasterDataNodes) throws OperatorException {
        Dimension rasterSize = null;
        for (RasterDataNode rasterDataNode : rasterDataNodes) {
            if (rasterSize == null) {
                rasterSize = rasterDataNode.getRasterSize();
            } else if (!rasterSize.equals(rasterDataNode.getRasterSize())) {
                throw new OperatorException(String.format("All source rasters must have the same size of %d x %d pixels.",
                                                          rasterSize.width,
                                                          rasterSize.height));
            }
        }
        return rasterSize;
    }

    /**
     * Gets the logger whuich can be used to log information during initialisation and tile computation.
     *
     * @return The logger.
     */
    public final Logger getLogger() {
        return context.getLogger();
    }

    /**
     * Non-API.
     */
    public void stopTileComputationObservation() {
        context.stopTileComputationObservation();
    }

    /**
     * Sets the logger which can be used to log information during initialisation and tile computation.
     *
     * @param logger The logger.
     */
    public final void setLogger(Logger logger) {
        Assert.notNull(logger, "logger");
        context.setLogger(logger);
    }

    /**
     * Gets the SPI which was used to create this operator.
     * If no operator has been explicitly set, the method will return an anonymous
     * SPI.
     *
     * @return The operator SPI.
     */
    public final OperatorSpi getSpi() {
        return context.getOperatorSpi();
    }

    /**
     * Sets the SPI which was used to create this operator.
     *
     * @param operatorSpi The operator SPI.
     */
    public final void setSpi(OperatorSpi operatorSpi) {
        Assert.notNull(operatorSpi, "operatorSpi");
        Assert.argument(operatorSpi.getOperatorClass().isAssignableFrom(getClass()), "operatorSpi");
        context.setOperatorSpi(operatorSpi);
    }

}
