package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Raster;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.TileCache;

import java.awt.Rectangle;
import java.io.IOException;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.logging.Logger;

/**
 * Default implementation for {@link org.esa.beam.framework.gpf.OperatorContext}.
 */
public class DefaultOperatorContext implements OperatorComputationContext {

    private final String operatorName;
    private List<Product> sourceProductList;
    private Map<String, Product> sourceProductMap;
    private Product targetProduct;
    private OperatorSpi operatorSpi;
    private Operator operator;
    private Field[] parameterFields;
    private List<Rectangle> layoutRectangles;

    /**
     * Constructs an new context with the name of an operator.
     *
     * @param operatorName the name of the operator
     */
    public DefaultOperatorContext(String operatorName) {
        this.operatorName = operatorName;
        this.sourceProductList = new ArrayList<Product>(3);
        this.sourceProductMap = new HashMap<String, Product>(3);
    }

    /**
     * Gets the name of the {@link Operator operator} this context is responsible for.
     *
     * @return the name of the operator
     */
    public String getOperatorName() {
        return operatorName;
    }

    /**
     * {@inheritDoc}
     */
    public Logger getLogger() {
        return Logger.getAnonymousLogger(); // todo
    }

    /**
     * {@inheritDoc}
     */
    public Product[] getSourceProducts() {
        return sourceProductList.toArray(new Product[sourceProductList.size()]);
    }

    /**
     * {@inheritDoc}
     */
    public Product getSourceProduct(String id) {
        return sourceProductMap.get(id);
    }

    /**
     * {@inheritDoc}
     */
    public String getIdForSourceProduct(Product product) {
        Set<Entry<String, Product>> entrySet = sourceProductMap.entrySet();
        for (Entry<String, Product> entry : entrySet) {
            if (entry.getValue() == product) {
                return entry.getKey();
            }
        }
        return null;
    }

    /**
     * {@inheritDoc}
     */
    public Product getTargetProduct() {
        return targetProduct;
    }

    /**
     * Sets the target product of the {@link Operator operator}.
     *
     * @param targetProduct the target product
     */
    public void setTargetProduct(Product targetProduct) {
        this.targetProduct = targetProduct;
    }

    /**
     * Gets the {@link OperatorSpi SPI} of the {@link Operator operator}.
     *
     * @return the {@link OperatorSpi SPI}
     */
    public OperatorSpi getOperatorSpi() {
        return operatorSpi;
    }

    /**
     * Sets the {@link OperatorSpi SPI} of the {@link Operator operator}.
     * @param operatorSpi the {@link OperatorSpi SPI}
     */
    public void setOperatorSpi(OperatorSpi operatorSpi) {
        this.operatorSpi = operatorSpi;
    }

    /**
     * {@inheritDoc}
     */
    public Operator getOperator() {
        return operator;
    }

    /**
     * Sets the  {@link Operator operator}.
     * @param operator the operator
     */
    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    /**
     * {@inheritDoc}
     */
    public Raster getRaster(RasterDataNode rasterDataNode, Rectangle rectangle, ProgressMonitor pm) throws
                                                                                                    OperatorException {
        Assert.notNull(rasterDataNode, "rasterDataNode");
        Assert.notNull(rectangle, "rectangle");
        return getTile(rasterDataNode, rectangle, null, pm).getRaster();
    }

    /**
     * {@inheritDoc}
     */
    public Raster getRaster(RasterDataNode rasterDataNode, Rectangle rectangle, ProductData dataBuffer,
                            ProgressMonitor pm) throws OperatorException {
        Assert.notNull(rasterDataNode, "rasterDataNode");
        Assert.notNull(rectangle, "rectangle");
        Assert.notNull(dataBuffer, "dataBuffer");
        return getTile(rasterDataNode, rectangle, dataBuffer, pm).getRaster();
    }

    /**
     * Adds a product to the list of source products.
     * One product instance can be registered with different ids, e.g. "source", "source1" and "input"
     * in consecutive calls.
     *
     * @param id the id of the product
     * @param product the product
     */
    public void addSourceProduct(String id, Product product) {
        if (!sourceProductList.contains(product)) {
            sourceProductList.add(product);
        }
        sourceProductMap.put(id, product);
    }

    /**
     * Checks if this context is initialized or not.
     * A context is considered as initialized if {@code getOperator() != null} becomes true.
     * @return {@code true} if this context is already initialized, otherwise {@code false}
     */
    public boolean isInitialized() {
        return operator != null;
    }

    /**
     * Sets the fields of the {@link Operator} annotated as {@link org.esa.beam.framework.gpf.annotations.Parameter Parameter}.
     * @param parameterFields the fields annotated as {@link org.esa.beam.framework.gpf.annotations.Parameter Parameter}
     */
    public void setParameterFields(Field[] parameterFields) {
        Assert.notNull(parameterFields);
        this.parameterFields = parameterFields;
    }

    /**
     * Gets the fields of the {@link Operator} annotated as {@link org.esa.beam.framework.gpf.annotations.Parameter Parameter}.
     * @return the fields annotated as {@link org.esa.beam.framework.gpf.annotations.Parameter Parameter}, may be {@code null}
     */
    public Field[] getParameterFields() {
        return parameterFields;
    }

    /**
     * {@inheritDoc}
     */
    public List<Rectangle> getLayoutRectangles() {
        return layoutRectangles;
    }


    /**
     * {@inheritDoc}
     */
    public void setLayoutRectangles(List<Rectangle> layoutRectangles) {
        this.layoutRectangles = layoutRectangles;
    }

    /**
     * {@inheritDoc}
     */
    public List<Rectangle> getAffectedRectangles(Rectangle rectangle) {
        List<Rectangle> affectedRects = new LinkedList<Rectangle>();
        for (Rectangle rect : layoutRectangles) {
            if (rectangle.intersects(rect)) {
                affectedRects.add(rect);
            }
        }
        return affectedRects;
    }

    /**
     * {@inheritDoc}
     */
    public boolean isLayoutRectangle(Rectangle rectangle) {
        return layoutRectangles.contains(rectangle);
    }

    private Tile getTile(RasterDataNode rasterDataNode, Rectangle tileRectangle,
                         ProductData dataBuffer, ProgressMonitor pm) throws OperatorException {

        TileCache tileCache = GPF.getDefaultInstance().getTileCache();
        Tile requestedTile = tileCache.getTile(rasterDataNode, tileRectangle);
        if (requestedTile == null) {
            requestedTile = tileCache.createTile(rasterDataNode, tileRectangle, dataBuffer);
        }

        if (isSourceProduct(rasterDataNode.getProduct())) {
            // source
            if (requestedTile.getState() == Tile.State.NOT_COMPUTED) {
                readIntoTile(rasterDataNode, tileRectangle, requestedTile, pm);
            }
        } else {
            // target
            if (requestedTile.getState() == Tile.State.NOT_COMPUTED) {
                requestedTile.setState(Tile.State.COMPUTING);
            }
        }

        return requestedTile;
    }

    private void readIntoTile(RasterDataNode rasterDataNode, Rectangle tileRectangle, Tile cachedTile,
                              ProgressMonitor pm) throws OperatorException {
        try {
            cachedTile.setState(Tile.State.COMPUTING);
            rasterDataNode.readRaster(tileRectangle, cachedTile.getRaster().getDataBuffer(), pm);
            cachedTile.setState(Tile.State.COMPUTED);
        } catch (IOException e) {
            Throwable cause = e.getCause();
            if (cause instanceof OperatorException) {
                throw (OperatorException) cause;
            }
            throw new OperatorException(e);
        }
    }

    private boolean isSourceProduct(Product product) {
        for (Product sourceProduct : sourceProductList) {
            if (sourceProduct == product) {
                return true;
            }
        }
        return false;
    }

}
