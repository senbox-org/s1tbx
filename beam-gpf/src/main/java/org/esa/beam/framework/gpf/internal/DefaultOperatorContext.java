package org.esa.beam.framework.gpf.internal;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Map.Entry;
import java.util.logging.Logger;

import javax.media.jai.PlanarImage;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.AbstractOperator;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorContext;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import com.bc.ceres.core.Assert;

/**
 * Default implementation for {@link org.esa.beam.framework.gpf.OperatorContext}.
 */
public class DefaultOperatorContext implements OperatorContext {

    private final String operatorName;
    private List<Product> sourceProductList;
    private Map<String, Product> sourceProductMap;
    private Product targetProduct;
    private OperatorSpi operatorSpi;
    private Operator operator;
    private Field[] parameterFields;
    private PlanarImage[] targetImages;
    private boolean tileMethodImplemented;
    private boolean tileStackMethodImplemented;

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


    public PlanarImage[] getTargetImages() {
        return targetImages;
    }


    public void setTargetImages(PlanarImage[] targetImages) {
        this.targetImages = targetImages;
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
        return Logger.getAnonymousLogger(); // todo - use a client-supplied application logger
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
    public String getSourceProductId(Product product) {
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
     *
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
     *
     * @param operator the operator
     */
    public void setOperator(Operator operator) {
        this.operator = operator;
        this.tileMethodImplemented = canOperatorComputeTile(operator.getClass());
        this.tileStackMethodImplemented = canOperatorComputeTileStack(operator.getClass());
    }

    /**
     * Adds a product to the list of source products.
     * One product instance can be registered with different ids, e.g. "source", "source1" and "input"
     * in consecutive calls.
     *
     * @param id      the id of the product
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
     *
     * @return {@code true} if this context is already initialized, otherwise {@code false}
     */
    public boolean isInitialized() {
        return operator != null;
    }

    /**
     * Sets the fields of the {@link Operator} annotated as {@link org.esa.beam.framework.gpf.annotations.Parameter Parameter}.
     *
     * @param parameterFields the fields annotated as {@link org.esa.beam.framework.gpf.annotations.Parameter Parameter}
     */
    public void setParameterFields(Field[] parameterFields) {
        Assert.notNull(parameterFields);
        this.parameterFields = parameterFields;
    }

    /**
     * Gets the fields of the {@link Operator} annotated as {@link org.esa.beam.framework.gpf.annotations.Parameter Parameter}.
     *
     * @return the fields annotated as {@link org.esa.beam.framework.gpf.annotations.Parameter Parameter}, may be {@code null}
     */
    public Field[] getParameterFields() {
        return parameterFields;
    }


    public boolean canComputeTile() {
        return tileMethodImplemented;
    }

    public boolean canComputeTileStack() {
        return tileStackMethodImplemented;
    }


    // todo - try to avoid data copying, this method is time-critical!
    // todo - the best way would be to wrap the returned awtRaster in "our" Tile
    public Tile getSourceTile(RasterDataNode rasterDataNode, Rectangle rectangle) {
        RenderedImage image = getSourceImage(rasterDataNode);
        /////////////////////////////////////////////////////////////////////
        //
        // Note: GPF pull-processing is triggered here!!!
        //
        java.awt.image.Raster awtRaster = image.getData(rectangle); // Note: copyData is NOT faster!
        //
        /////////////////////////////////////////////////////////////////////
        final ProductData rasterData = rasterDataNode.createCompatibleRasterData(rectangle.width, rectangle.height);
        final RasterImpl raster = new RasterImpl(rasterDataNode, rectangle, rasterData);
        awtRaster.getDataElements(rectangle.x, rectangle.y, rectangle.width, rectangle.height, rasterData.getElems());
        return raster;
    }

    private static RenderedImage getSourceImage(RasterDataNode rasterDataNode) {
        RenderedImage image = rasterDataNode.getImage();
        if (image != null) {
            return image;
        }
        image = new RasterDataNodeOpImage(rasterDataNode);
        rasterDataNode.setImage(image);
        return image;
    }

    public static boolean canOperatorComputeTile(Class<? extends Operator> aClass) {
        return implementsMethod(aClass, "computeTile",
                                new Class[]{
                                        Band.class,
                                        Tile.class});
    }

    public static boolean canOperatorComputeTileStack(Class<? extends Operator> aClass) {
        return implementsMethod(aClass, "computeTileStack",
                                new Class[]{
                                        Map.class,
                                        Rectangle.class});
    }

    private static boolean implementsMethod(Class<?> aClass, String methodName, Class[] methodParameterTypes) {
        while (true) {
            if (Operator.class.equals(aClass)
                    || AbstractOperator.class.equals(aClass)
                    || !Operator.class.isAssignableFrom(aClass)) {
                return false;
            }
            try {
                Method declaredMethod = aClass.getDeclaredMethod(methodName, methodParameterTypes);
                return declaredMethod.getModifiers() != Modifier.ABSTRACT;
            } catch (NoSuchMethodException e) {
                aClass = aClass.getSuperclass();
            }
        }
    }

    public boolean isCancellationRequested() {
        return false;
    }
}
