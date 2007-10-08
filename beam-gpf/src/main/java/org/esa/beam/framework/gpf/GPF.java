package org.esa.beam.framework.gpf;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.internal.OperatorSpiRegistryImpl;
import org.esa.beam.util.Guardian;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

public class GPF {

    public static final String SOURCE_PRODUCT_FIELD_NAME = "sourceProduct";
    public static final String TARGET_PRODUCT_FIELD_NAME = "targetProduct";


    /**
     * An unmodifiable empty {@link Map Map}.
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
     * @param operatorName the name of the operator to use
     * @param parameters   the named parameters needed by the operator
     * @return the product created by the operator
     * @throws OperatorException if the product could not be created
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
     * @param operatorName  the name of the operator to use
     * @param parameters    the named parameters needed by the operator
     * @param sourceProduct a source product
     * @return the product created by the operator
     * @throws OperatorException if the product could not be created
     */
    public static Product createProduct(final String operatorName,
                                        final Map<String, Object> parameters,
                                        final Product sourceProduct) throws OperatorException {
        return createProduct(operatorName, parameters, new Product[]{sourceProduct});
    }

    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     *
     * @param operatorName   the name of the operator to use
     * @param parameters     the named parameters needed by the operator
     * @param sourceProducts an array of  source products
     * @return the product created by the operator
     * @throws OperatorException if the product could not be created
     */
    public static Product createProduct(final String operatorName,
                                        final Map<String, Object> parameters,
                                        final Product[] sourceProducts) throws OperatorException {
        Map<String, Product> sourceProductMap = NO_SOURCES;
        if (sourceProducts.length > 0) {
            sourceProductMap = new HashMap<String, Product>(sourceProducts.length);
            sourceProductMap.put(SOURCE_PRODUCT_FIELD_NAME, sourceProducts[0]);
            for (int i = 0; i < sourceProducts.length; i++) {
                Product sourceProduct = sourceProducts[i];
                sourceProductMap.put(SOURCE_PRODUCT_FIELD_NAME + (i + 1), sourceProduct);
            }
        }
        return defaultInstance.createProductNS(operatorName, parameters, sourceProductMap);
    }

    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     *
     * @param operatorName   the name of the operator to use
     * @param parameters     the named parameters needed by the operator
     * @param sourceProducts a map of named source products
     * @return the product created by the operator
     * @throws OperatorException if the product could not be created
     */
    public static Product createProduct(String operatorName,
                                        Map<String, Object> parameters,
                                        Map<String, Product> sourceProducts) throws OperatorException {
        return defaultInstance.createProductNS(operatorName, parameters, sourceProducts);
    }

    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     *
     * @param operatorName   the name of the operator to use
     * @param parameters     the named parameters needed by the operator
     * @param sourceProducts a map of named source products
     * @return the product created by the operator
     * @throws OperatorException if the product could not be created
     */
    public Product createProductNS(String operatorName,
                                   Map<String, Object> parameters,
                                   Map<String, Product> sourceProducts) throws OperatorException {
        OperatorSpi operatorSpi = spiRegistry.getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new OperatorException("operator SPI not found for operator'" + operatorName + "'");
        }
        Operator operator = operatorSpi.createOperator(parameters, sourceProducts);
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
     * @return the singelton instance
     */
    public static GPF getDefaultInstance() {
        return defaultInstance;
    }

    /**
     * Sets the default GPF instance.
     *
     * @param defaultInstance the GPF default instance
     */
    public static void setDefaultInstance(GPF defaultInstance) {
        GPF.defaultInstance = defaultInstance;
    }
}
