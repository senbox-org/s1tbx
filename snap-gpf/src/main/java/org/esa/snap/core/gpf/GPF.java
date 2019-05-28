/*
 * Copyright (C) 2014 Brockmann Consult GmbH (info@brockmann-consult.de)
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
import com.bc.ceres.core.ServiceRegistryManager;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductManager;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.gpf.common.resample.DownsamplerSpi;
import org.esa.snap.core.gpf.common.resample.DownsamplerSpiRegistry;
import org.esa.snap.core.gpf.common.resample.DownsamplerSpiRegistryImpl;
import org.esa.snap.core.gpf.common.resample.UpsamplerSpi;
import org.esa.snap.core.gpf.common.resample.UpsamplerSpiRegistry;
import org.esa.snap.core.gpf.common.resample.UpsamplerSpiRegistryImpl;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.SourceProductDescriptor;
import org.esa.snap.core.gpf.descriptor.SourceProductsDescriptor;
import org.esa.snap.core.gpf.internal.OperatorSpiRegistryImpl;
import org.esa.snap.core.util.Guardian;

import java.awt.Dimension;
import java.awt.RenderingHints;
import java.io.File;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * <p>The facade for the Graph Processing Framework.
 * <p>The Graph Processing Framework makes extensive use of Java Advanced Imaging (JAI).
 * Therefore, configuring the JAI {@link javax.media.jai.TileCache TileCache} and
 * {@link javax.media.jai.TileScheduler TileScheduler} will also affect the overall performance of
 * the Graph Processing Framework.
 * <p>This class may be overridden in order to alter product creation behaviour of the static
 * {@code createProduct} methods of the GPF instance.
 * The current instance can be set by {@link #setDefaultInstance(GPF)}.
 *
 * @author Norman Fomferra
 * @since 4.1
 */
public class GPF {

    public static final String DISABLE_TILE_CACHE_PROPERTY = "snap.gpf.disableTileCache";
    public static final String USE_FILE_TILE_CACHE_PROPERTY = "snap.gpf.useFileTileCache";
    public static final String TILE_COMPUTATION_OBSERVER_PROPERTY = "snap.gpf.tileComputationObserver";
    public static final String BEEP_AFTER_PROCESSING_PROPERTY = "snap.gpf.beepAfterProcessing";
    public static final String SNAP_GPF_ALLOW_AUXDATA_DOWNLOAD = "snap.gpf.allowAuxdataDownload";

    public static final String SOURCE_PRODUCT_FIELD_NAME = "sourceProduct";
    public static final String TARGET_PRODUCT_FIELD_NAME = "targetProduct";

    /**
     * Key for GPF tile size {@link RenderingHints}.
     * <p>
     * The value for this key must be an instance of {@link Dimension} with
     * both width and height positive.
     */
    public static final RenderingHints.Key KEY_TILE_SIZE =
            new RenderingKey<>(1, Dimension.class, val -> val.width > 0 && val.height > 0);

    /**
     * An unmodifiable empty {@link Map Map}.
     * <p>
     * Can be used for convenience as a parameter for {@code createProduct()} if no
     * parameters are needed for the operator.
     *
     * @see #createProduct(String, Map)
     * @see #createProduct(String, Map, Product ...)
     * @see #createProduct(String, Map, Map)
     */
    public static final Map<String, Object> NO_PARAMS = Collections.unmodifiableMap(new TreeMap<>());

    /**
     * An unmodifiable empty {@link Map Map}.
     * <p>
     * Can be used for convenience as a parameter for {@code createProduct(String, Map, Map)} if no
     * source products are needed for the operator.
     *
     * @see #createProduct(String, Map, Map)
     */
    public static final Map<String, Product> NO_SOURCES = Collections.unmodifiableMap(new TreeMap<>());

    private static GPF defaultInstance;

    static {
        defaultInstance = new GPF();
        defaultInstance.spiRegistry.loadOperatorSpis();
        defaultInstance.upsamplerSpiRegistry.loadUpsamplerSpis();
        defaultInstance.downsamplerSpiRegistry.loadDownsamplerSpis();
    }

    private OperatorSpiRegistry spiRegistry;
    private UpsamplerSpiRegistry upsamplerSpiRegistry;
    private DownsamplerSpiRegistry downsamplerSpiRegistry;

    private ProductManager productManager;

    /**
     * Constructor.
     */
    protected GPF() {
        ServiceRegistryManager registryManager = ServiceRegistryManager.getInstance();
        spiRegistry = new OperatorSpiRegistryImpl(registryManager.getServiceRegistry(OperatorSpi.class));
        upsamplerSpiRegistry = new UpsamplerSpiRegistryImpl(registryManager.getServiceRegistry(UpsamplerSpi.class));
        downsamplerSpiRegistry = new DownsamplerSpiRegistryImpl(registryManager.getServiceRegistry(DownsamplerSpi.class));
    }

    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     *
     * @param operatorName the name of the operator to use.
     * @param parameters   the named parameters needed by the operator.
     *
     * @return the product created by the operator.
     *
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
     *
     * @return the product created by the operator.
     *
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
     *
     * @return the product created by the operator.
     *
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
     *
     * @return the product created by the operator.
     *
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
     *
     * @return the product created by the operator.
     *
     * @throws OperatorException if the product could not be created.
     */
    public static Product createProduct(final String operatorName,
                                        final Map<String, Object> parameters,
                                        final Product... sourceProducts) throws OperatorException {
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
     *
     * @return the product created by the operator.
     *
     * @throws OperatorException if the product could not be created.
     */
    public static Product createProduct(String operatorName,
                                        Map<String, Object> parameters,
                                        Product[] sourceProducts,
                                        RenderingHints renderingHints) throws OperatorException {
        Map<String, Product> sourceProductMap = NO_SOURCES;
        if (sourceProducts.length > 0) {
            OperatorSpi operatorSpi = GPF.getDefaultInstance().spiRegistry.getOperatorSpi(operatorName);
            if (operatorSpi == null) {
                throw new OperatorException(
                        String.format("Unknown operator '%s'. Is the name correctly spelled?", operatorName));
            }

            sourceProductMap = new HashMap<>(sourceProducts.length * 3);
            OperatorDescriptor operatorDescriptor = operatorSpi.getOperatorDescriptor();
            SourceProductDescriptor[] sourceProductDescriptors = operatorDescriptor.getSourceProductDescriptors();
            if(sourceProductDescriptors.length > 0) {
                sourceProductMap.put(SOURCE_PRODUCT_FIELD_NAME, sourceProducts[0]);
            }

            SourceProductsDescriptor sourceProductsDescriptor = operatorDescriptor.getSourceProductsDescriptor();
            if(sourceProductsDescriptor != null) {
                for (int i = 0; i < sourceProducts.length; i++) {
                    Product sourceProduct = sourceProducts[i];
                    sourceProductMap.put(SOURCE_PRODUCT_FIELD_NAME + "." + (i + 1), sourceProduct);
                    // kept for backward compatibility
                    // since BEAM 4.9 the pattern above is preferred
                    sourceProductMap.put(SOURCE_PRODUCT_FIELD_NAME + (i + 1), sourceProduct);
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
     *
     * @return the product created by the operator.
     *
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
     *
     * @return the product created by the operator.
     *
     * @throws OperatorException if the product could not be created.
     */
    public static Product createProduct(String operatorName,
                                        Map<String, Object> parameters,
                                        Map<String, Product> sourceProducts,
                                        RenderingHints renderingHints) throws OperatorException {
        return defaultInstance.createProductNS(operatorName, parameters, sourceProducts, renderingHints);
    }

    /**
     * Creates a product by using the operator specified by the given name.
     * The resulting product can be used as input product for a further call to {@code createProduct()}.
     * By concatenating multiple calls it is possible to set up a processing graph.
     * <p>All static {@code createProduct} methods delegate to this non-static (= NS) version.
     * It can be overriden by clients in order to alter product creation behaviour of the static
     * {@code createProduct} methods of the current GPF instance.
     *
     * @param operatorName   the name of the operator to use.
     * @param parameters     the named parameters needed by the operator.
     * @param sourceProducts the map of named source products.
     * @param renderingHints the rendering hints, may be {@code null}.
     *
     * @return the product created by the operator.
     *
     * @throws OperatorException if the product could not be created.
     */
    public Product createProductNS(String operatorName,
                                   Map<String, Object> parameters,
                                   Map<String, Product> sourceProducts,
                                   RenderingHints renderingHints) {
        Operator operator = createOperator(operatorName, parameters, sourceProducts, renderingHints);
        return operator.getTargetProduct();
    }

    /**
     * Creates an operator instance by using the given operator (alias) name.
     *
     * @param operatorName   the name of the operator to use.
     * @param parameters     the named parameters needed by the operator.
     * @param sourceProducts the map of named source products.
     * @param renderingHints the rendering hints, may be {@code null}.
     *
     * @return the product created by the operator.
     *
     * @throws OperatorException if the product could not be created.
     * @since BEAM 4.9
     */
    public Operator createOperator(String operatorName, Map<String, Object> parameters, Map<String, Product> sourceProducts,
                                   RenderingHints renderingHints) {
        OperatorSpi operatorSpi = spiRegistry.getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new OperatorException("No SPI found for operator '" + operatorName + "'");
        }
        return operatorSpi.createOperator(parameters, sourceProducts, renderingHints);
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
     * Gets the registry for upsampler SPIs.
     *
     * @return the registry for upsampler SPIs.
     */
    public UpsamplerSpiRegistry getUpsamplerSpiRegistry() {
        return upsamplerSpiRegistry;
    }

    /**
     * Gets the registry for downsampler SPIs.
     *
     * @return the registry for downsampler SPIs.
     */
    public DownsamplerSpiRegistry getDownsamplerSpiRegistry() {
        return downsamplerSpiRegistry;
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

    /**
     * Writes a product with the specified format to the given file.
     *
     * @param product     the product
     * @param file        the product file
     * @param formatName  the name of a supported product format, e.g. "HDF5". If {@code null}, the default format
     *                    "BEAM-DIMAP" will be used
     * @param incremental switch the product writer in incremental mode or not.
     * @param pm          a monitor to inform the user about progress
     */
    public static void writeProduct(Product product, File file, String formatName, boolean incremental, ProgressMonitor pm) {
        writeProduct(product, file, formatName, false, incremental, pm);
    }

    /**
      * Writes a product with the specified format to the given file.
      *
      * @param product     the product
      * @param file        the product file
      * @param formatName  the name of a supported product format, e.g. "HDF5". If {@code null}, the default format
      *                    "BEAM-DIMAP" will be used
      * @param clearCacheAfterRowWrite if true, the internal tile cache is cleared after a tile row has been written.
      * @param incremental switch the product writer in incremental mode or not.
      * @param pm          a monitor to inform the user about progress
      */
     public static void writeProduct(Product product, File file, String formatName, boolean clearCacheAfterRowWrite, boolean incremental, ProgressMonitor pm) {
         WriteOp writeOp = new WriteOp(product, file, formatName);
         writeOp.setDeleteOutputOnFailure(true);
         writeOp.setWriteEntireTileRows(true);
         writeOp.setClearCacheAfterRowWrite(clearCacheAfterRowWrite);
         writeOp.setIncremental(incremental);
         writeOp.writeProduct(pm);
     }

    /**
     * Gets the context product manager which can be used to exchange product instances across operators
     * or allow (reading) operators to check if a given product is already opened.
     *
     * @return The context product manager.
     * @since SNAP 3.0
     */
    public synchronized ProductManager getProductManager() {
        if (productManager == null) {
            productManager = new ProductManager();
        }
        return productManager;
    }

    /**
     * Sets the context product manager which can be used to exchange product instances across operators
     * or allow (reading) operators to check if a given product is already opened.
     *
     * @param productManager The new context product manager.
     * @since SNAP 3.0
     */
    public synchronized void setProductManager(ProductManager productManager) {
        Assert.notNull(productManager, "productManager");
        this.productManager = productManager;
    }

    static class RenderingKey<T> extends RenderingHints.Key {

        private final Class<T> objectClass;
        private final Validator<T> validator;

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
