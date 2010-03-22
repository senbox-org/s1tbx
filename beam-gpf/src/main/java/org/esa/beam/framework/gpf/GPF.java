package org.esa.beam.framework.gpf;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.internal.OperatorSpiRegistryImpl;
import org.esa.beam.util.Guardian;

import java.awt.Dimension;
import java.awt.RenderingHints;
import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>The facade for the Graph Processing Framework.</p>
 * <p>The Graph Processing Framework makes extensive use of Java Advanced Imaging (JAI).
 * Therefore, configuring the JAI {@link javax.media.jai.TileCache TileCache} and
 * {@link javax.media.jai.TileScheduler TileScheduler} will also affect the overall performance of
 * the Graph Processing Framework.</p>
 * <p>This class may be overridden in order to alter product creation behaviour of the static
 * {@code createProduct} methods of the GPF instance.
 * The current instance can be set by {@link #setDefaultInstance(GPF)}.</p>
 *
 * @author Norman Fomferra
 * @since 4.1
 */
public class GPF {

    public static final String SOURCE_PRODUCT_FIELD_NAME = "sourceProduct";
    public static final String TARGET_PRODUCT_FIELD_NAME = "targetProduct";

    /**
     * Key for GPF tile size {@link RenderingHints}.
     * <p/>
     * The value for this key must be an instance of {@link Dimension} with
     * both width and height positive.
     */
    public static final RenderingHints.Key KEY_TILE_SIZE =
            new RenderingKey<Dimension>(1, Dimension.class, new RenderingKey.Validator<Dimension>() {
                @Override
                public boolean isValid(Dimension val) {
                    return val.width > 0 && val.height > 0;
                }
            });

    /**
     * An unmodifiable empty {@link Map Map}.
     * <p/>
     * Can be used for convenience as a parameter for {@code createProduct()} if no
     * parameters are needed for the operator.
     *
     * @see #createProduct(String,Map) createProduct(String, Map, ProgressMonitor)
     * @see #createProduct(String,Map,Product) createProduct(String, Map, Product, ProgressMonitor)
     * @see #createProduct(String,Map,Product[]) createProduct(String, Map, Product[], ProgressMonitor)
     * @see #createProduct(String,Map,Map) createProduct(String, Map, Map, ProgressMonitor)
     */
    public static final Map<String, Object> NO_PARAMS = Collections.unmodifiableMap(new TreeMap<String, Object>());

    /**
     * An unmodifiable empty {@link Map Map}.
     * <p/>
     * Can be used for convenience as a parameter for {@code createProduct()} if no
     * source products are needed for the operator.
     *
     * @see #createProduct(String,Map) createProduct(String, Map, ProgressMonitor)
     * @see #createProduct(String,Map,Product) createProduct(String, Map, Product, ProgressMonitor)
     * @see #createProduct(String,Map,Product[]) createProduct(String, Map, Product[], ProgressMonitor)
     * @see #createProduct(String,Map,Map) createProduct(String, Map, Map, ProgressMonitor)
     */
    public static final Map<String, Product> NO_SOURCES = Collections.unmodifiableMap(new TreeMap<String, Product>());

    private static GPF defaultInstance = new GPF();
    private OperatorSpiRegistry spiRegistry;

    protected GPF() {
        spiRegistry = new OperatorSpiRegistryImpl();
    }

    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     *
     * @param operatorName the name of the operator to use.
     * @param parameters   the named parameters needed by the operator.
     * @return the product created by the operator.
     * @throws OperatorException if the product could not be created.
     */
    public static Product createProduct(String operatorName,
                                        Map<String, Object> parameters) throws OperatorException {
        return createProduct(operatorName, parameters, NO_SOURCES);
    }

    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     *
     * @param operatorName   the name of the operator to use.
     * @param parameters     the named parameters needed by the operator.
     * @param renderingHints the rendering hints may be {@code null}.
     * @return the product created by the operator.
     * @throws OperatorException if the product could not be created.
     */
    public static Product createProduct(String operatorName,
                                        Map<String, Object> parameters,
                                        RenderingHints renderingHints) throws OperatorException {
        return createProduct(operatorName, parameters, NO_SOURCES, renderingHints);
    }

    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     *
     * @param operatorName  the name of the operator to use.
     * @param parameters    the named parameters needed by the operator.
     * @param sourceProduct a source product.
     * @return the product created by the operator.
     * @throws OperatorException if the product could not be created.
     */
    public static Product createProduct(final String operatorName,
                                        final Map<String, Object> parameters,
                                        final Product sourceProduct) throws OperatorException {
        return createProduct(operatorName, parameters, sourceProduct, null);
    }

    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     *
     * @param operatorName   the name of the operator to use.
     * @param parameters     the named parameters needed by the operator.
     * @param sourceProduct  the source product.
     * @param renderingHints the rendering hints may be {@code null}.
     * @return the product created by the operator.
     * @throws OperatorException if the product could not be created.
     */
    public static Product createProduct(final String operatorName,
                                        final Map<String, Object> parameters,
                                        final Product sourceProduct,
                                        RenderingHints renderingHints) throws OperatorException {
        return createProduct(operatorName, parameters, new Product[]{sourceProduct}, renderingHints);
    }

    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     *
     * @param operatorName   the name of the operator to use.
     * @param parameters     the named parameters needed by the operator.
     * @param sourceProducts the source products.
     * @return the product created by the operator.
     * @throws OperatorException if the product could not be created.
     */
    public static Product createProduct(final String operatorName,
                                        final Map<String, Object> parameters,
                                        final Product[] sourceProducts) throws OperatorException {
        return createProduct(operatorName, parameters, sourceProducts, null);
    }

    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     *
     * @param operatorName   the name of the operator to use.
     * @param parameters     the named parameters needed by the operator.
     * @param sourceProducts the source products.
     * @param renderingHints the rendering hints may be {@code null}.
     * @return the product created by the operator.
     * @throws OperatorException if the product could not be created.
     */
    public static Product createProduct(String operatorName,
                                        Map<String, Object> parameters,
                                        Product[] sourceProducts,
                                        RenderingHints renderingHints) throws OperatorException {
        Map<String, Product> sourceProductMap = NO_SOURCES;
        if (sourceProducts.length > 0) {
            sourceProductMap = new HashMap<String, Product>(sourceProducts.length);
            OperatorSpi operatorSpi = GPF.getDefaultInstance().spiRegistry.getOperatorSpi(operatorName);
            if(operatorSpi == null) {
                throw new OperatorException(String.format("Operator for '%s' not found. Maybe OperatorSpi is not loaded.", operatorName));
            }
            Field[] declaredFields = operatorSpi.getOperatorClass().getDeclaredFields();
            for (Field declaredField : declaredFields) {
                SourceProduct sourceProductAnnotation = declaredField.getAnnotation(SourceProduct.class);
                if (sourceProductAnnotation != null) {
                    sourceProductMap.put(SOURCE_PRODUCT_FIELD_NAME, sourceProducts[0]);
                }
                SourceProducts sourceProductsAnnotation = declaredField.getAnnotation(SourceProducts.class);
                if (sourceProductsAnnotation != null) {
                    for (int i = 0; i < sourceProducts.length; i++) {
                        Product sourceProduct = sourceProducts[i];
                        sourceProductMap.put(SOURCE_PRODUCT_FIELD_NAME + "." + (i), sourceProduct);
                    }
                }
            }
        }

        return defaultInstance.createProductNS(operatorName, parameters, sourceProductMap, renderingHints);
    }

    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     *
     * @param operatorName   the name of the operator to use.
     * @param parameters     the named parameters needed by the operator.
     * @param sourceProducts the map of named source products.
     * @return the product created by the operator.
     * @throws OperatorException if the product could not be created.
     */
    public static Product createProduct(String operatorName,
                                        Map<String, Object> parameters,
                                        Map<String, Product> sourceProducts) throws OperatorException {
        return createProduct(operatorName, parameters, sourceProducts, null);
    }

    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     *
     * @param operatorName   the name of the operator to use.
     * @param parameters     the named parameters needed by the operator.
     * @param sourceProducts the map of named source products.
     * @param renderingHints the rendering hints, may be {@code null}.
     * @return the product created by the operator.
     * @throws OperatorException if the product could not be created.
     */
    public static Product createProduct(String operatorName,
                                        Map<String, Object> parameters,
                                        Map<String, Product> sourceProducts,
                                        RenderingHints renderingHints) throws OperatorException {
        return defaultInstance.createProductNS(operatorName, parameters, sourceProducts, renderingHints);
    }

    /**
     * This method is deprecated. Use {@link GPF#createProductNS(String name, Map parameters,
     * Map sourceProducts, RenderingHints renderingHints)} instead.
     *
     * @param operatorName   the name of the operator to use.
     * @param parameters     the named parameters needed by the operator.
     * @param sourceProducts a map of named source products.
     * @return the product created by the operator.
     * @throws OperatorException if the product could not be created.
     */
    @Deprecated
    public Product createProductNS(String operatorName,
                                   Map<String, Object> parameters,
                                   Map<String, Product> sourceProducts) throws OperatorException {
        return createProductNS(operatorName, parameters, sourceProducts, null);
    }

    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     * <p>All static {@code createProduct} methods delegate to this non-static (= NS) version.
     * It can be overriden by clients in order to alter product creation behaviour of the static
     * {@code createProduct} methods of the current GPF instance.</p>
     *
     * @param operatorName   the name of the operator to use.
     * @param parameters     the named parameters needed by the operator.
     * @param sourceProducts the map of named source products.
     * @param renderingHints the rendering hints, may be {@code null}.
     * @return the product created by the operator.
     * @throws OperatorException if the product could not be created.
     */
    public Product createProductNS(String operatorName,
                                   Map<String, Object> parameters,
                                   Map<String, Product> sourceProducts,
                                   RenderingHints renderingHints) {
        OperatorSpi operatorSpi = spiRegistry.getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new OperatorException("No SPI found for operator '" + operatorName + "'");
        }
        Operator operator = operatorSpi.createOperator(parameters, sourceProducts, renderingHints);
        return operator.getTargetProduct();
    }

    /**
     * Gets the registry for operator SPIs.
     *
     * @return the registry for operator SPIs.
     */
    public OperatorSpiRegistry getOperatorSpiRegistry() {
        return spiRegistry;
    }

    /**
     * Sets the registry for operator SPIs.
     *
     * @param spiRegistry the registry for operator SPIs.
     */
    public void setOperatorSpiRegistry(OperatorSpiRegistry spiRegistry) {
        Guardian.assertNotNull("spiRegistry", spiRegistry);
        this.spiRegistry = spiRegistry;
    }

    /**
     * Gets the default GPF instance.
     *
     * @return the singelton instance.
     */
    public static GPF getDefaultInstance() {
        return defaultInstance;
    }

    /**
     * Sets the default GPF instance.
     *
     * @param defaultInstance the GPF default instance.
     */
    public static void setDefaultInstance(GPF defaultInstance) {
        GPF.defaultInstance = defaultInstance;
    }

    static class RenderingKey<T> extends RenderingHints.Key {
        private final Class<T> objectClass;
        private final Validator<T> validator;

        RenderingKey(int privateKey, Class<T> objectClass) {
            this(privateKey, objectClass, new Validator<T>() {
                @Override
                public boolean isValid(T val) {
                    return true;
                }
            });
        }

        RenderingKey(int privateKey, Class<T> objectClass, Validator<T> validator) {
            super(privateKey);
            this.objectClass = objectClass;
            this.validator = validator;
        }

        @Override
        public final boolean isCompatibleValue(Object val) {
            //noinspection unchecked
            return val != null && objectClass.isAssignableFrom(val.getClass()) && validator.isValid((T) val);
        }

        interface Validator<T> {
            boolean isValid(T val);
        }
    }

}
