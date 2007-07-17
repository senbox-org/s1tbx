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


    public DefaultOperatorContext(String operatorName) {
        this.operatorName = operatorName;
        this.sourceProductList = new ArrayList<Product>(3);
        this.sourceProductMap = new HashMap<String, Product>(3);
    }


    public String getOperatorName() {
        return operatorName;
    }

    public Logger getLogger() {
        return Logger.getAnonymousLogger(); // todo
    }

    public Product[] getSourceProducts() {
        return sourceProductList.toArray(new Product[sourceProductList.size()]);
    }

    public Product getSourceProduct(String name) {
        return sourceProductMap.get(name);
    }

    public String getIdForSourceProduct(Product product) {
        Set<Entry<String, Product>> entrySet = sourceProductMap.entrySet();
        for (Entry<String, Product> entry : entrySet) {
            if (entry.getValue() == product) {
                return entry.getKey();
            }
        }
        return null;
    }

    public Product getTargetProduct() {
        return targetProduct;
    }

    public void setTargetProduct(Product targetProduct) {
        this.targetProduct = targetProduct;
    }

    public OperatorSpi getOperatorSpi() {
        return operatorSpi;
    }

    public void setOperatorSpi(OperatorSpi operatorSpi) {
        this.operatorSpi = operatorSpi;
    }

    public Operator getOperator() {
        return operator;
    }

    public void setOperator(Operator operator) {
        this.operator = operator;
    }

    public Raster getRaster(RasterDataNode rasterDataNode, Rectangle rectangle, ProgressMonitor pm) throws
                                                                                                    OperatorException {
        Assert.notNull(rasterDataNode, "rasterDataNode");
        Assert.notNull(rectangle, "rectangle");
        return getTile(rasterDataNode, rectangle, null, pm).getRaster();
    }

    public Raster getRaster(RasterDataNode rasterDataNode, Rectangle rectangle, ProductData dataBuffer,
                            ProgressMonitor pm) throws OperatorException {
        Assert.notNull(rasterDataNode, "rasterDataNode");
        Assert.notNull(rectangle, "rectangle");
        Assert.notNull(dataBuffer, "dataBuffer");
        return getTile(rasterDataNode, rectangle, dataBuffer, pm).getRaster();
    }

    public void addSourceProduct(String name, Product product) {
        // same instances can be registered with different names, e.g. "source" and "source1", "input"
        if (!sourceProductList.contains(product)) {
            sourceProductList.add(product);
        }
        sourceProductMap.put(name, product);
    }

    public boolean isInitialized() {
        return operator != null;
    }

    public void setParameterFields(Field[] parameterFields) {
        Assert.notNull(parameterFields);
        this.parameterFields = parameterFields;
    }

    public Field[] getParameterFields() {
        return parameterFields;
    }

    /**
     * @return the layoutRectangles
     */
    public List<Rectangle> getLayoutRectangles() {
        return layoutRectangles;
    }


    /**
     * @param layoutRectangles the layoutRectangles to set
     */
    public void setLayoutRectangles(List<Rectangle> layoutRectangles) {
        this.layoutRectangles = layoutRectangles;
    }

    /**
     * Returns all rectangles that intersect with the given rectangle
     *
     * @param rectangle the rectangle to intersect with
     *
     * @return a list of rectangles
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
