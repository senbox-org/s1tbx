package org.esa.beam.framework.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.internal.DefaultOperatorContext;
import org.esa.beam.framework.gpf.internal.OperatorContextInitializer;
import org.esa.beam.framework.gpf.internal.ParameterInjector;
import org.esa.beam.framework.gpf.internal.TileCacheImpl;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class GPF {

    /**
     * An unmodifiable empty {@link Map Map}.
     * Can be used for convenience as a parameter for {@code createProduct()} if no
     * parameters are needed for the operator.
     *
     * @see #createProduct(String, Map, ProgressMonitor) createProduct(String, Map, ProgressMonitor)
     * @see #createProduct(String, Map, Product, ProgressMonitor) createProduct(String, Map, Product, ProgressMonitor)
     * @see #createProduct(String, Map, Product[], ProgressMonitor) createProduct(String, Map, Product[], ProgressMonitor)
     * @see #createProduct(String, Map, Map, ProgressMonitor) createProduct(String, Map, Map, ProgressMonitor)
     */
    public static final Map<String, Object> NO_PARAMS = Collections.unmodifiableMap(new TreeMap<String, Object>());

    /**
     * An unmodifiable empty {@link Map Map}.
     * Can be used for convenience as a parameter for {@code createProduct()} if no
     * source products are needed for the operator.
     *
     * @see #createProduct(String, Map, ProgressMonitor) createProduct(String, Map, ProgressMonitor)
     * @see #createProduct(String, Map, Product, ProgressMonitor) createProduct(String, Map, Product, ProgressMonitor)
     * @see #createProduct(String, Map, Product[], ProgressMonitor) createProduct(String, Map, Product[], ProgressMonitor)
     * @see #createProduct(String, Map, Map, ProgressMonitor) createProduct(String, Map, Map, ProgressMonitor)
     */
    public static final Map<String, Product> NO_SOURCES = Collections.unmodifiableMap(new TreeMap<String, Product>());

    private static final String SOURCE_PRODUCT_FIELD_NAME = "sourceProduct";
    private static GPF defaultInstance = new GPF(new TileCacheImpl());

    private TileCache tileCache;

    private GPF(TileCache tileCache) {
        this.tileCache = tileCache;
    }

    /**
     * Gets the default singelton instance.
     *
     * @return the singelton instance
     */
    public static GPF getDefaultInstance() {
        return defaultInstance;
    }

    /**
     * Gets the {@link TileCache tile cache}.
     *
     * @return the tile cache
     */
    public TileCache getTileCache() {
        return tileCache;
    }


    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     *
     * @param operatorName the name of the operator to use
     * @param parameters   the named parameters needed by the operator
     * @param pm           a monitor to observe progress
     *
     * @return the product created by the operator
     *
     * @throws OperatorException if the product could not be created
     */
    public static Product createProduct(String operatorName,
                                        Map<String, Object> parameters, ProgressMonitor pm) throws OperatorException {
        return createProduct(operatorName, parameters, NO_SOURCES, pm);
    }

    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     *
     * @param operatorName  the name of the operator to use
     * @param parameters    the named parameters needed by the operator
     * @param sourceProduct a source product
     * @param pm            a monitor to observe progress
     *
     * @return the product created by the operator
     *
     * @throws OperatorException if the product could not be created
     */
    public static Product createProduct(final String operatorName,
                                        final Map<String, Object> parameters,
                                        final Product sourceProduct, ProgressMonitor pm) throws OperatorException {
        return createProduct(operatorName, parameters, new Product[]{sourceProduct}, pm);
    }

    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     *
     * @param operatorName   the name of the operator to use
     * @param parameters     the named parameters needed by the operator
     * @param sourceProducts an array of  source products
     * @param pm             a monitor to observe progress
     *
     * @return the product created by the operator
     *
     * @throws OperatorException if the product could not be created
     */
    public static Product createProduct(final String operatorName,
                                        final Map<String, Object> parameters,
                                        final Product[] sourceProducts, ProgressMonitor pm) throws OperatorException {
        Map<String, Product> sourceProductMap = NO_SOURCES;
        if (sourceProducts.length > 0) {
            sourceProductMap = new HashMap<String, Product>(sourceProducts.length);
            sourceProductMap.put(SOURCE_PRODUCT_FIELD_NAME, sourceProducts[0]);
            for (int i = 0; i < sourceProducts.length; i++) {
                Product sourceProduct = sourceProducts[i];
                sourceProductMap.put(SOURCE_PRODUCT_FIELD_NAME + (i + 1), sourceProduct);
            }
        }
        return createProduct(operatorName, parameters, sourceProductMap, pm);
    }

    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     *
     * @param operatorName   the name of the operator to use
     * @param parameters     the named parameters needed by the operator
     * @param sourceProducts a map of named source products
     * @param pm             a monitor to observe progress
     *
     * @return the product created by the operator
     *
     * @throws OperatorException if the product could not be created
     */
    public static Product createProduct(String operatorName,
                                        Map<String, Object> parameters,
                                        Map<String, Product> sourceProducts, ProgressMonitor pm) throws
                                                                                                 OperatorException {
        final DefaultOperatorContext defaultOperatorContext = new DefaultOperatorContext(operatorName);
        Set<Map.Entry<String, Product>> entries = sourceProducts.entrySet();
        for (Map.Entry<String, Product> entry : entries) {
            defaultOperatorContext.addSourceProduct(entry.getKey(), entry.getValue());
        }
        OperatorContextInitializer.initOperatorContext(defaultOperatorContext,
                                                       new MapParameterInjector(defaultOperatorContext, parameters),
                                                       pm);
        return defaultOperatorContext.getTargetProduct();
    }

    private static class MapParameterInjector implements ParameterInjector {

        private final DefaultOperatorContext defaultOperatorContext;
        private final Map<String, Object> parameters;

        public MapParameterInjector(DefaultOperatorContext defaultOperatorContext, Map<String, Object> parameters) {
            this.defaultOperatorContext = defaultOperatorContext;
            this.parameters = parameters;
        }

        public void injectParameters(Operator operator) throws OperatorException {
            Field[] parameterFields = defaultOperatorContext.getParameterFields();
            for (Field parameterField : parameterFields) {
                Object value = parameters.get(parameterField.getName());
                if (value != null) {
                    boolean oldFieldState = parameterField.isAccessible();
                    try {
                        parameterField.setAccessible(true);
                        // todo - validate before setting the value!!!
                        parameterField.set(operator, value);
                    } catch (IllegalAccessException e) {
                        throw new OperatorException("Failed to set parameter [" + parameterField.getName() + "]", e);
                    } finally {
                        parameterField.setAccessible(oldFieldState);
                    }
                }
            }
        }
    }
}
