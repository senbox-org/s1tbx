package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.*;

import java.awt.Rectangle;
import java.awt.image.RenderedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;
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
    //-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI
    private GpfOpImage[] opImages;

    public GpfOpImage[] getOpImages() {
        return opImages;
    }
    //-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI-JAI

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
        if (opImages == null) {
            Band[] bands = targetProduct.getBands();
            opImages = new GpfOpImage[bands.length];
            for (int i = 0; i < bands.length; i++) {
                Band band = bands[i];
                GpfOpImage opImage = GpfOpImage.create(this, band, ProgressMonitor.NULL);
                opImages[i] = opImage;
                band.setImage(opImage);
            }
        }
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
        classInfo = new ClassInfoImpl(operator.getClass());
    }

    /**
     * {@inheritDoc}
     */
    public Raster getRaster(RasterDataNode rasterDataNode, Rectangle rectangle, ProgressMonitor pm) throws
            OperatorException {
        Assert.notNull(rasterDataNode, "rasterDataNode");
        Assert.notNull(rectangle, "rectangle");
        return getSourceRaster(rasterDataNode, rectangle);
    }

    /**
     * Returns sources only!
     * {@inheritDoc}
     */
    @Deprecated
    public Raster getRaster(RasterDataNode rasterDataNode, Rectangle rectangle, ProductData dataBuffer,
                            ProgressMonitor pm) throws OperatorException {
        return getRaster(rasterDataNode, rectangle, pm);
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

    // todo - try to avoid data copying, this method is time-critical!
    // todo - the best way would be to wrap the returned awtRaster in "our" Raster
    private Raster getSourceRaster(RasterDataNode rasterDataNode, Rectangle rectangle) {
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

    private RenderedImage getSourceImage(RasterDataNode rasterDataNode) {
        RenderedImage image = rasterDataNode.getImage();
        if (image != null) {
            return image;
        }
        image = RasterDataNodeOpImage.create(rasterDataNode);
        rasterDataNode.setImage(image);
        return image;
    }

    private ClassInfo classInfo;

    public ClassInfo getClassInfo() {
        return classInfo;
    }

    private class ClassInfoImpl implements ClassInfo {

        private final boolean bandMethodImplemented;
        private final boolean tilesMethodImplemented;

        public ClassInfoImpl(Class operatorClass) {
            bandMethodImplemented = implementsComputeBandMethod(operatorClass);
            tilesMethodImplemented = implementsComputeAllBandsMethod(operatorClass);
        }

        public boolean isBandMethodImplemented() {
            return bandMethodImplemented;
        }

        public boolean isAllBandsMethodImplemented() {
            return tilesMethodImplemented;
        }
    }

    public static boolean implementsComputeBandMethod(Class<?> aClass) {
        return implementsMethod(aClass, "computeBand",
                                new Class[]{
                                        Raster.class,
                                        ProgressMonitor.class});
    }

    public static boolean implementsComputeAllBandsMethod(Class<?> aClass) {
        return implementsMethod(aClass, "computeAllBands",
                                new Class[]{
                                        Rectangle.class,
                                        ProgressMonitor.class});
    }

    private static boolean implementsMethod(Class<?> aClass, String methodName, Class[] methodParameterTypes) {
        if (Operator.class.equals(aClass)
                || AbstractOperator.class.equals(aClass)
                || !Operator.class.isAssignableFrom(aClass)) {
            return false;
        }
        try {
            Method declaredMethod = aClass.getDeclaredMethod(methodName, methodParameterTypes);
            return declaredMethod.getModifiers() != Modifier.ABSTRACT;
        } catch (NoSuchMethodException e) {
            Class<?> superclass = aClass.getSuperclass();
            return implementsMethod(superclass, methodName, methodParameterTypes);
        }
    }
}
