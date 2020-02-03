/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
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
package org.esa.snap.core.gpf.internal;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertyDescriptorFactory;
import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.binding.PropertySetDescriptor;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.binding.dom.XppDomElement;
import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.glevel.MultiLevelImage;
import com.bc.ceres.jai.tilecache.DefaultSwapSpace;
import com.bc.ceres.jai.tilecache.SwappingTileCache;
import org.esa.snap.core.dataio.ProductIO;
import org.esa.snap.core.dataio.ProductWriter;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.datamodel.MetadataAttribute;
import org.esa.snap.core.datamodel.MetadataElement;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.ProductData;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorCancelException;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.ParameterDescriptorFactory;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.annotations.TargetProperty;
import org.esa.snap.core.gpf.common.WriteOp;
import org.esa.snap.core.gpf.descriptor.AnnotationOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.PropertySetDescriptorFactory;
import org.esa.snap.core.gpf.graph.GraphOp;
import org.esa.snap.core.gpf.monitor.TileComputationEvent;
import org.esa.snap.core.gpf.monitor.TileComputationObserver;
import org.esa.snap.core.util.ModuleMetadata;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.jai.JAIUtils;
import org.esa.snap.runtime.Config;

import javax.media.jai.BorderExtender;
import javax.media.jai.JAI;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;
import javax.media.jai.TileCache;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.RenderingHints;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.awt.image.WritableRaster;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.TimeZone;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * The context in which operators are executed.
 *
 * @author Norman Fomferra
 * @since 4.1
 */
public class OperatorContext {

    static final String PROCESSING_GRAPH_ELEMENT_NAME = "Processing_Graph";

    private static final String DATETIME_OUTPUT_PATTERN = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    private static final SimpleDateFormat DATETIME_OUTPUT_FORMAT = new SimpleDateFormat(DATETIME_OUTPUT_PATTERN);

    static {
        DATETIME_OUTPUT_FORMAT.setTimeZone(TimeZone.getTimeZone("UTC"));
    }

    private static TileCache tileCache;
    private static TileComputationObserver tileComputationObserver;

    private final Operator operator;
    private final List<Product> sourceProductList;
    private final Map<String, Product> sourceProductMap;
    private final Map<String, Object> targetPropertyMap;
    private final RenderingHints renderingHints;
    private final boolean computeTileMethodImplemented;
    private final boolean computeTileStackMethodImplemented;

    private String id;
    private Product targetProduct;
    private OperatorSpi operatorSpi;
    private Map<Band, OperatorImage> targetImageMap;
    private OperatorConfiguration configuration;
    private Logger logger;
    private boolean cancelled;
    private boolean disposed;
    private PropertySet parameterSet;
    private boolean initialising;
    private boolean requiresAllBands;
    private boolean executed;

    public OperatorContext(Operator operator) {
        if (operator == null) {
            throw new NullPointerException("operator");
        }

        this.operator = operator;
        this.computeTileMethodImplemented = isComputeTileMethodImplemented(operator.getClass());
        this.computeTileStackMethodImplemented = isComputeTileStackMethodImplemented(operator.getClass());
        this.sourceProductList = new ArrayList<>(3);
        this.sourceProductMap = new HashMap<>(3);
        this.targetPropertyMap = new HashMap<>(3);
        this.logger = SystemUtils.LOG;
        this.renderingHints = new RenderingHints(JAI.KEY_TILE_CACHE_METRIC, this);

        startTileComputationObservation();
    }

    /**
     * Makes sure that the given JAI OpImage has a valid tile cache (see System property {@link GPF#USE_FILE_TILE_CACHE_PROPERTY}),
     * or makes sure that it has none (see System property {@link GPF#DISABLE_TILE_CACHE_PROPERTY}).
     *
     * @param image Any JAI OpImage.
     */
    public static void setTileCache(OpImage image) {
        boolean disableTileCache = Config.instance().preferences().getBoolean(GPF.DISABLE_TILE_CACHE_PROPERTY, false);
        if (disableTileCache) {
            image.setTileCache(null);
        } else if (image.getTileCache() == null) {
            image.setTileCache(getTileCache());
            SystemUtils.LOG.finest(String.format("Tile cache assigned to %s", image));
        }
    }

    private static synchronized TileCache getTileCache() {
        if (tileCache == null) {
            boolean useFileTileCache = Config.instance().preferences().getBoolean(GPF.USE_FILE_TILE_CACHE_PROPERTY, false);
            if (useFileTileCache) {
                tileCache = new SwappingTileCache(JAI.getDefaultInstance().getTileCache().getMemoryCapacity(),
                                                  new DefaultSwapSpace(SwappingTileCache.DEFAULT_SWAP_DIR,
                                                                       SystemUtils.LOG));
            } else {
                tileCache = JAI.getDefaultInstance().getTileCache();
            }
            SystemUtils.LOG.fine(
                    String.format("All GPF operators will share an instance of %s with a capacity of %dM",
                                  tileCache.getClass().getName(),
                                  tileCache.getMemoryCapacity() / (1024 * 1024)));
        }
        return tileCache;
    }

    public String getId() {
        if (id == null) {
            OperatorDescriptor descriptor = getOperatorSpi().getOperatorDescriptor();
            String aliasName = descriptor.getAlias() != null ? descriptor.getAlias() : descriptor.getName();
            id = aliasName + '$' + Long.toHexString(System.currentTimeMillis()).toUpperCase();
        }
        return id;
    }

    public void setId(String id) {
        Assert.notNull(id, "id");
        this.id = id;
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        Assert.notNull(logger, "logger");
        this.logger = logger;
    }

    public Product getSourceProduct(String id) {
        return sourceProductMap.get(id);
    }

    public void setSourceProduct(String id, Product product) {
        if (product != null) {
            if (!sourceProductList.contains(product)) {
                sourceProductList.add(product);
            }
            sourceProductMap.put(id, product);
            updatePropertyDescriptors(); // the source product is used if the property is of type RasterDataNodeType
        }
    }

    public Product[] getSourceProducts() {
        return sourceProductList.toArray(new Product[sourceProductList.size()]);
    }

    public void setSourceProducts(Product[] products) {
        sourceProductList.clear();
        sourceProductMap.clear();
        for (int i = 0; i < products.length; i++) {
            Product product = products[i];
            final int productIndex = i + 1;
            setSourceProduct(GPF.SOURCE_PRODUCT_FIELD_NAME + '.' + productIndex, product);
            // kept for backward compatibility
            // since BEAM 4.9 the pattern above is preferred
            setSourceProduct(GPF.SOURCE_PRODUCT_FIELD_NAME + productIndex, product);
        }
    }

    public void setSourceProducts(Map<String, Product> sourceProducts) {
        sourceProductList.clear();
        sourceProductMap.clear();
        Set<Map.Entry<String, Product>> entries = sourceProducts.entrySet();
        for (Map.Entry<String, Product> entry : entries) {
            setSourceProduct(entry.getKey(), entry.getValue());
        }
    }

    public String getSourceProductId(Product product) {
        Set<Map.Entry<String, Product>> entrySet = sourceProductMap.entrySet();
        List<String> mappedIds = new ArrayList<>();
        for (Map.Entry<String, Product> entry : entrySet) {
            //noinspection ObjectEquality
            if (entry.getValue() == product) {
                mappedIds.add(entry.getKey());
            }
        }
        if (mappedIds.isEmpty()) {
            return null;
        }
        String id = mappedIds.get(0);
        for (String mappedId : mappedIds) {
            if (mappedId.contains(".")) {
                id = mappedId;
            }
        }
        return id;
    }

    public Product getTargetProduct() throws OperatorException {
        if (targetProduct == null) {
            initializeOperator();
        }
        return targetProduct;
    }

    public void setTargetProduct(Product targetProduct) {
        Assert.notNull(targetProduct, "targetProduct");
        this.targetProduct = targetProduct;
    }

    public Object getTargetProperty(String name) {
        getTargetProduct();

        return targetPropertyMap.get(name);
    }


    public boolean isCancelled() {
        return cancelled;
    }

    public void setCancelled(boolean cancelled) {
        this.cancelled = cancelled;
    }

    public void checkForCancellation() throws OperatorException {
        if (isCancelled()) {
            throw new OperatorCancelException("Operation canceled.");
        }
    }

    public OperatorSpi getOperatorSpi() {
        if (operatorSpi == null) {
            // create anonymous SPI
            operatorSpi = new OperatorSpi(operator.getClass()) {
            };
        }
        return operatorSpi;
    }

    public void setOperatorSpi(OperatorSpi operatorSpi) {
        this.operatorSpi = operatorSpi;
    }

    public Operator getOperator() {
        return operator;
    }


    public Object getParameter(String name) {
        Assert.notNull(name, "name");
        return getParameterSet().getValue(name);
    }

    public void setParameter(String name, Object value) {
        Assert.notNull(name, "name");
        PropertySet paramSet = getParameterSet();
        if (paramSet.isPropertyDefined(name)) {
            Property property = paramSet.getProperty(name);
            if (value != null) {
                setPropertyValue(value, property);
            } else {
                paramSet.removeProperty(property);
            }
        }
    }

    private void setPropertyValue(Object value, Property property) {
        try {
            if (value instanceof String && !String.class.isAssignableFrom(property.getType())) {
                property.setValueFromText((String) value);
            } else {
                property.setValue(value);
            }
        } catch (ValidationException e) {
            throw new OperatorException(formatExceptionMessage("%s", e.getMessage()), e);
        }
    }

    public Map<String, Object> getParameterMap() {
        PropertySet paramSet = getParameterSet();
        Property[] properties = paramSet.getProperties();
        Map<String, Object> parameterMap = new HashMap<>();
        for (Property property : properties) {
            parameterMap.put(property.getName(), property.getValue());
        }
        return parameterMap;
    }

    public void setParameterMap(Map<String, Object> parameters) {
        for (Entry<String, Object> entry : parameters.entrySet()) {
            setParameter(entry.getKey(), entry.getValue());
        }
    }

    public RenderingHints getRenderingHints() {
        return renderingHints;
    }

    public void addRenderingHints(RenderingHints renderingHints) {
        this.renderingHints.add(renderingHints);
    }

    public void setConfiguration(OperatorConfiguration opConfiguration) {
        this.configuration = opConfiguration;
    }

    public boolean isInitialized() {
        return targetProduct != null;
    }

    public boolean isComputeTileMethodImplemented() {
        return computeTileMethodImplemented;
    }

    public boolean isComputeTileStackMethodImplemented() {
        return computeTileStackMethodImplemented;
    }

    public Tile getSourceTile(RasterDataNode rasterDataNode, Rectangle region) {
        return getSourceTile(rasterDataNode, region, null);
    }

    public Tile getSourceTile(RasterDataNode rasterDataNode, Rectangle region, BorderExtender borderExtender) {
        suspendWatch();
        MultiLevelImage image = rasterDataNode.getSourceImage();
        /////////////////////////////////////////////////////////////////////
        //
        // Note: GPF pull-processing is triggered here!
        //
        Raster awtRaster;
        if (borderExtender != null) {
            awtRaster = image.getExtendedData(region, borderExtender);
        } else {
            awtRaster = image.getData(region); // Note: copyData is NOT faster!
        }
        //
        /////////////////////////////////////////////////////////////////////
        resumeWatch();
        return new TileImpl(rasterDataNode, awtRaster);
    }

    public OperatorImage getTargetImage(Band band) {
        return targetImageMap.get(band);
    }

    public boolean isDisposed() {
        return disposed;
    }

    public void dispose() {
        if (!disposed) {
            disposed = true;
            configuration = null;
            sourceProductMap.clear();
            sourceProductList.clear();
            Collection<OperatorImage> operatorImages = targetImageMap.values();
            for (OperatorImage image : operatorImages) {
                image.dispose();
            }
            targetImageMap.clear();
            operator.dispose();
        }
    }

    private static boolean isComputeTileMethodImplemented(Class<? extends Operator> aClass) {
        return implementsMethod(aClass, "computeTile",
                                new Class[]{
                                        Band.class,
                                        Tile.class,
                                        ProgressMonitor.class
                                });
    }

    private static boolean isComputeTileStackMethodImplemented(Class<? extends Operator> aClass) {
        return implementsMethod(aClass, "computeTileStack",
                                new Class[]{
                                        Map.class,
                                        Rectangle.class,
                                        ProgressMonitor.class
                                });
    }

    private boolean operatorMustComputeTileStack() {
        return operator.canComputeTileStack() && !operator.canComputeTile();
    }

    private static boolean implementsMethod(Class<?> aClass, String methodName, Class[] methodParameterTypes) {
        while (true) {
            if (Operator.class.equals(aClass)
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

    private void initializeOperator() throws OperatorException {
        Assert.state(targetProduct == null, "targetProduct == null");
        Assert.state(!initialising, "!initialising, attempt to call getTargetProduct() from within initialise()?");

        try {
            initialising = true;
            if (!(operator instanceof GraphOp)) {
                initSourceProductFields();
                updatePropertyDescriptors();
                injectConfiguration();
            }
            operator.initialize();
            initTargetProduct();
            initTargetProperties(operator.getClass());
            initTargetImages();
            initGraphMetadata();

            targetProduct.setModified(false);
        } finally {
            initialising = false;
        }
    }

    /**
     * Updates this operator forcing it to recreate the target product.
     * <i>Warning: Experimental API added by nf (25.02.2010)</i><br/>
     *
     * @throws OperatorException If an error occurs.
     * @since BEAM 4.8
     */
    public void updateOperator() throws OperatorException {
        targetProduct = null;
        executed = false;
        initializeOperator();
    }


    public PropertySet getParameterSet() {
        if (parameterSet == null) {
            if (operatorSpi == null) {
                PropertyDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory(sourceProductMap);
                parameterSet = PropertyContainer.createObjectBacked(operator, parameterDescriptorFactory);
            } else {
                OperatorDescriptor operatorDescriptor = operatorSpi.getOperatorDescriptor();
                PropertySetDescriptor propertySetDescriptor;
                try {
                    propertySetDescriptor = PropertySetDescriptorFactory.createForOperator(operatorDescriptor, sourceProductMap);
                } catch (ConversionException e) {
                    throw new OperatorException(e);
                }
                if (operatorDescriptor instanceof AnnotationOperatorDescriptor) {
                    parameterSet = PropertyContainer.createObjectBacked(operator, propertySetDescriptor);
                } else {
                    parameterSet = PropertyContainer.createMapBacked(new HashMap<>(), propertySetDescriptor);
                }
            }
        }
        return parameterSet;
    }

    private void initGraphMetadata() {
        final MetadataElement metadataRoot = targetProduct.getMetadataRoot();
        MetadataElement targetGraphME = metadataRoot.getElement(PROCESSING_GRAPH_ELEMENT_NAME);
        if (targetGraphME == null) {
            targetGraphME = new MetadataElement(PROCESSING_GRAPH_ELEMENT_NAME);
            metadataRoot.addElement(targetGraphME);
        }
        convertOperatorContextToMetadata(this, targetGraphME);
    }

    private void convertOperatorContextToMetadata(OperatorContext context, MetadataElement targetGraphME) throws OperatorException {
        String opId = context.getId();
        boolean contains = false;
        int nodeElementCount = 0;
        for (MetadataElement element : targetGraphME.getElements()) {
            MetadataAttribute idAttribute = element.getAttribute("id");
            if (idAttribute != null && idAttribute.getData().getElemString().equals(opId)) {
                contains = true;
            }
            if (element.getName().startsWith("node")) {
                nodeElementCount++;
            }
        }

        if (contains) {
            return;
        }
        Class<? extends Operator> operatorClass = context.operator.getClass();
        final String opName = OperatorSpi.getOperatorAlias(operatorClass);
        MetadataElement targetNodeME = new MetadataElement(String.format("node.%d", nodeElementCount));
        targetGraphME.addElement(targetNodeME);
        targetNodeME.addAttribute(new MetadataAttribute("id", ProductData.createInstance(opId), false));
        targetNodeME.addAttribute(new MetadataAttribute("operator", ProductData.createInstance(opName), false));

        final ModuleMetadata moduleMetadata = operatorSpi.getModuleMetadata();
        if (moduleMetadata != null) {
            final ProductData moduleName = ProductData.createInstance(moduleMetadata.getDisplayName());
            targetNodeME.addAttribute(new MetadataAttribute("moduleName", moduleName, false));
            final ProductData moduleVersion = ProductData.createInstance(moduleMetadata.getVersion());
            targetNodeME.addAttribute(new MetadataAttribute("moduleVersion", moduleVersion, false));
        }

        OperatorSpiRegistry operatorSpiRegistry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        OperatorSpi operatorSpi = operatorSpiRegistry.getOperatorSpi(opName);
        OperatorDescriptor operatorDescriptor;
        if (operatorSpi != null) {
            operatorDescriptor = operatorSpi.getOperatorDescriptor();
            if (operatorDescriptor.getDescription() != null) {
                ProductData purposeValue = ProductData.createInstance(operatorDescriptor.getDescription());
                targetNodeME.addAttribute(new MetadataAttribute("purpose", purposeValue, false));
            }
            if (operatorDescriptor.getAuthors() != null) {
                ProductData authorsValue = ProductData.createInstance(operatorDescriptor.getAuthors());
                targetNodeME.addAttribute(new MetadataAttribute("authors", authorsValue, false));
            }
            if (operatorDescriptor.getVersion() != null) {
                ProductData opVersionValue = ProductData.createInstance(operatorDescriptor.getVersion());
                targetNodeME.addAttribute(new MetadataAttribute("version", opVersionValue, false));
            }
            if (operatorDescriptor.getCopyright() != null) {
                ProductData copyrightValue = ProductData.createInstance(operatorDescriptor.getCopyright());
                targetNodeME.addAttribute(new MetadataAttribute("copyright", copyrightValue, false));
            }
        }
        ProductData processingTime = ProductData.createInstance(DATETIME_OUTPUT_FORMAT.format(new Date()));
        targetNodeME.addAttribute(new MetadataAttribute("processingTime", processingTime, false));

        List<MetadataAttribute> sourceAttributeList = new ArrayList<>(context.sourceProductList.size() * 2);
        for (Product sourceProduct : context.sourceProductList) {
            final String sourceId = context.getSourceProductId(sourceProduct);
            if (sourceId == null) {
                throw new OperatorException(formatExceptionMessage("Source product not found: " + sourceProduct.getName()));
            }
            final String sourceNodeId;
            if (sourceProduct.getFileLocation() != null) {
                sourceNodeId = sourceProduct.getFileLocation().toURI().toASCIIString();
            } else if (sourceProduct.getProductReader() instanceof OperatorProductReader) {
                final OperatorProductReader productReader = (OperatorProductReader) sourceProduct.getProductReader();
                convertOperatorContextToMetadata(productReader.getOperatorContext(), targetGraphME);
                sourceNodeId = productReader.getOperatorContext().getId();
            } else {
                sourceNodeId = "product:" + sourceProduct.getDisplayName();
            }
            final MetadataAttribute sourceAttribute = new MetadataAttribute(sourceId,
                                                                            ProductData.createInstance(sourceNodeId),
                                                                            false);
            sourceAttributeList.add(sourceAttribute);
        }
        final MetadataElement targetSourcesME = new MetadataElement("sources");
        Collections.sort(sourceAttributeList, (ma1, ma2) -> ma1.getName().compareTo(ma2.getName()));
        for (MetadataAttribute sourceAttribute : sourceAttributeList) {
            targetSourcesME.addAttribute(sourceAttribute);
        }
        targetNodeME.addElement(targetSourcesME);

        final DefaultDomConverter domConverter = new DefaultDomConverter(operatorClass,
                                                                         new ParameterDescriptorFactory(sourceProductMap));
        final XppDomElement parametersDom = new XppDomElement("parameters");
        try {
            domConverter.convertValueToDom(context.operator, parametersDom);
        } catch (ConversionException e) {
            e.printStackTrace();
        }
        final MetadataElement targetParametersME = new MetadataElement("parameters");
        addDomToMetadata(parametersDom, targetParametersME);
        targetNodeME.addElement(targetParametersME);
    }

    private static void addDomToMetadata(DomElement parentDE, MetadataElement parentME) {
        final HashMap<String, List<DomElement>> map = new HashMap<>(
                parentDE.getChildCount() + 5);
        for (DomElement childDE : parentDE.getChildren()) {
            final String name = childDE.getName();
            List<DomElement> elementList = map.get(name);
            if (elementList == null) {
                elementList = new ArrayList<>(3);
                map.put(name, elementList);
            }
            elementList.add(childDE);
        }
        for (Entry<String, List<DomElement>> entry : map.entrySet()) {
            String name = entry.getKey();
            final List<DomElement> elementList = entry.getValue();
            if (elementList.size() > 1) {
                for (int i = 0; i < elementList.size(); i++) {
                    addDomToMetadata(elementList.get(i), name + '.' + i, parentME);
                }
            } else {
                addDomToMetadata(elementList.get(0), name, parentME);
            }
        }
    }


    private static void addDomToMetadata(DomElement childDE, String name, MetadataElement parentME) {
        if (childDE.getChildCount() > 0 || childDE.getAttributeNames().length > 0) {
            final MetadataElement childME = new MetadataElement(name);
            addDomToMetadata(childDE, childME);
            parentME.addElement(childME);

            if (childDE.getAttributeNames().length != 0) {
                String[] attributeNames = childDE.getAttributeNames();
                for (String attributeName : attributeNames) {
                    String attributeValue = childDE.getAttribute(attributeName);
                    final ProductData valueMEAtrr = ProductData.createInstance(attributeValue);
                    final MetadataAttribute mdAttribute = new MetadataAttribute(attributeName, valueMEAtrr, true);
                    childME.addAttribute(mdAttribute);
                }
            }
        } else {
            String valueDE = childDE.getValue();
            if (valueDE == null) {
                valueDE = "";
            }
            final ProductData valueME = ProductData.createInstance(valueDE);
            final MetadataAttribute attribute = new MetadataAttribute(name, valueME, true);
            parentME.addAttribute(attribute);
        }
    }

    private void initTargetImages() {
        if (targetProduct.getPreferredTileSize() == null) {
            targetProduct.setPreferredTileSize(getPreferredTileSize());
        }
        final Band[] targetBands = targetProduct.getBands();
        Object[][] locks = null;
        if (operatorMustComputeTileStack()) {
            Dimension tileSize = targetProduct.getPreferredTileSize();
            int width = targetProduct.getSceneRasterWidth();
            int height = targetProduct.getSceneRasterHeight();
            locks = OperatorImageTileStack.createLocks(width, height, tileSize);
        }
        targetImageMap = new HashMap<>(targetBands.length * 2);
        for (final Band targetBand : targetBands) {
            // Only register non-virtual bands
            // Actually it should not be necessary to distinguish between regular and not regular bands.
            // What does 'regular' actually mean?
            // Here the question is if the operator needs to compute the data.
            // Actually there should only one implementation of Band and it should be declared final.
            // Virtual, filtered and all other types can be implemented by simply exchanging the source image.
            // Instead of deriving the band class, a band should have an associated type. It would serve the same purpose
            // as the type we already have for layers.
            if (isRegularBand(targetBand)) {

                OperatorImage opImage = getOwnedOperatorImage(targetBand);
                if (opImage == null) {

                    final OperatorImage image;
                    if (operatorMustComputeTileStack()) {
                        image = new OperatorImageTileStack(targetBand, this, locks);
                    } else {
                        image = new OperatorImage(targetBand, this);
                    }
                    targetImageMap.put(targetBand, image);

                    // Note: It is legal not to set the newly created operator image
                    // in the target band, if it already has a source image set.
                    // This case occurs for "pass-through" operators.
                    // Pull processing in GPF is primarily triggered by fetching tiles
                    // of the target images of this operator context, not directly
                    // by using the band's source images. Otherwise the WriteOp.computeTile()
                    // method would never be called.
                    //
                    if (!targetBand.isSourceImageSet()) {
                        targetBand.setSourceImage(image);
                    }
                } else {
                    // Someone has added a band whose image comes from another operator that
                    // has already been initialised.
                    targetBand.getSourceImage().reset();
                    targetImageMap.put(targetBand, opImage);
                }
            } else {
                final Operator operator = getOperator();
                // The WriteOp needs to be called for VirtualBands in order to write them as real bands, where necessary (NetCDF-CF).
                // For other operators it should not be called
                if (operator instanceof WriteOp) {
                    WriteOp writer = (WriteOp) operator;
                    ProductWriter productWriter = ProductIO.getProductWriter(writer.getFormatName());

                    if (productWriter != null && productWriter.shouldWrite(targetBand)) {
                        targetImageMap.put(targetBand, new OperatorImage(targetBand, this) {
                            @Override
                            protected void computeRect(PlanarImage[] ignored, WritableRaster tile, Rectangle destRect) {
                                executeOperator(ProgressMonitor.NULL);
                                Band targetBand = getTargetBand();
                                tile.setRect(targetBand.getGeophysicalImage().getData(destRect));
                                TileImpl targetTile = new TileImpl(targetBand, tile, destRect, false);
                                operator.computeTile(targetBand, targetTile, ProgressMonitor.NULL);
                            }
                        });
                    }
                }
            }

        }
    }

    private OperatorImage getOwnedOperatorImage(Band targetBand) {
        // If there is no source image then we can't own it
        if (!targetBand.isSourceImageSet()) {
            return null;
        }
        // If the source image is not an OperatorImage then we can't own it neither
        RenderedImage renderedImage = targetBand.getSourceImage().getImage(0);
        if (!(renderedImage instanceof OperatorImage)) {
            return null;
        }
        // If the OperatorImage's context is not us, then it is not ours
        OperatorImage operatorImage = (OperatorImage) renderedImage;
        //noinspection ObjectEquality
        if (this != operatorImage.getOperatorContext()) {
            return null;
        }
        // Now it must be an OperatorImage that we have created
        return operatorImage;
    }

    private Dimension getPreferredTileSize() {
        Dimension tileSize = null;
        for (final Product sourceProduct : sourceProductList) {
            if (sourceProduct.getPreferredTileSize() != null &&
                    sourceProduct.getSceneRasterWidth() == targetProduct.getSceneRasterWidth() &&
                    sourceProduct.getSceneRasterHeight() == targetProduct.getSceneRasterHeight()) {
                tileSize = sourceProduct.getPreferredTileSize();
                break;
            }
        }
        if (tileSize == null) {
            tileSize = JAIUtils.computePreferredTileSize(targetProduct.getSceneRasterWidth(),
                                                         targetProduct.getSceneRasterHeight(), 4);
        }
        return tileSize;
    }

    public static boolean isRegularBand(Band targetBand) {
        // Note: "instanceof" has intentionally not been used here.
        //noinspection ObjectEquality
        return targetBand.getClass() == Band.class;
    }

    private void initTargetProduct() throws OperatorException {
        Class<? extends Operator> operatorClass = operator.getClass();
        initTargetProduct(operatorClass);
        if (targetProduct == null) {
            final String message = formatExceptionMessage("No target product set.");
            throw new OperatorException(message);
        }
        if (targetProduct.getProductReader() == null) {
            targetProduct.setProductReader(new OperatorProductReader(this));
        }
        if (GPF.KEY_TILE_SIZE.isCompatibleValue(renderingHints.get(GPF.KEY_TILE_SIZE))) {
            targetProduct.setPreferredTileSize((Dimension) renderingHints.get(GPF.KEY_TILE_SIZE));
        }
    }

    private void initTargetProduct(Class<? extends Operator> operatorClass) {
        Class<?> superClass = operatorClass.getSuperclass();
        if (superClass != null && !superClass.equals(Operator.class)) {
            initTargetProduct((Class<? extends Operator>) superClass);
        }
        for (Field declaredField : operatorClass.getDeclaredFields()) {
            TargetProduct targetProductAnnotation = declaredField.getAnnotation(TargetProduct.class);
            if (targetProductAnnotation != null) {
                if (!declaredField.getType().equals(Product.class)) {
                    String msg = formatExceptionMessage("Field '%s' annotated as target product is not of type '%s'.",
                                                        declaredField.getName(), Product.class);
                    throw new OperatorException(msg);
                }
                final Product targetProduct = (Product) getOperatorFieldValue(declaredField);
                if (targetProduct != null) {
                    this.targetProduct = targetProduct;
                } else {
                    if (this.targetProduct != null) {
                        setOperatorFieldValue(declaredField, this.targetProduct);
                    } else {
                        final String message = formatExceptionMessage("No target product set.");
                        throw new OperatorException(message);
                    }
                }
            }
        }
    }

    private void initTargetProperties(Class<? extends Operator> operatorClass) throws OperatorException {
        Class<?> superClass = operatorClass.getSuperclass();
        if (superClass != null && !superClass.equals(Operator.class)) {
            initTargetProperties((Class<? extends Operator>) superClass);
        }
        Field[] declaredFields = operatorClass.getDeclaredFields();
        Map<String, Object> localTargetPropertyMap = new HashMap<>();
        for (Field declaredField : declaredFields) {
            TargetProperty targetPropertyAnnotation = declaredField.getAnnotation(TargetProperty.class);
            if (targetPropertyAnnotation != null) {
                Object propertyValue = getOperatorFieldValue(declaredField);
                String fieldName = declaredField.getName();
                if (localTargetPropertyMap.containsKey(fieldName)) {
                    final String message = formatExceptionMessage(
                            "Name of field '%s' is already used as target property alias.",
                            fieldName);
                    throw new OperatorException(message);
                }
                localTargetPropertyMap.put(fieldName, propertyValue);
                if (!targetPropertyAnnotation.alias().isEmpty()) {
                    String aliasName = targetPropertyAnnotation.alias();
                    if (localTargetPropertyMap.containsKey(aliasName)) {
                        final String message = formatExceptionMessage(
                                "Alias of field '%s' is already used by another target property.",
                                aliasName);
                        throw new OperatorException(message);
                    }
                    localTargetPropertyMap.put(aliasName, propertyValue);
                }
            }
        }
        targetPropertyMap.putAll(localTargetPropertyMap);
    }

    private void initSourceProductFields() throws OperatorException {
        initSourceProductFields(operator.getClass());
    }

    private void initSourceProductFields(Class<? extends Operator> operatorClass) {
        Field[] declaredFields = operatorClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            SourceProduct sourceProductAnnotation = declaredField.getAnnotation(SourceProduct.class);
            if (sourceProductAnnotation != null) {
                processSourceProductField(declaredField, sourceProductAnnotation);
            }
            SourceProducts sourceProductsAnnotation = declaredField.getAnnotation(SourceProducts.class);
            if (sourceProductsAnnotation != null) {
                processSourceProductsField(declaredField, sourceProductsAnnotation);
            }
        }
        Class<?> superClass = operatorClass.getSuperclass();
        if (superClass != null && !superClass.equals(Operator.class)) {
            initSourceProductFields((Class<? extends Operator>) superClass);
        }
    }

    private void processSourceProductField(Field declaredField, SourceProduct sourceProductAnnotation) throws
            OperatorException {
        if (declaredField.getType().equals(Product.class)) {
            String productMapName = declaredField.getName();
            Product sourceProduct = getSourceProduct(productMapName);
            if (sourceProduct == null) {
                productMapName = sourceProductAnnotation.alias();
                sourceProduct = getSourceProduct(productMapName);
            }
            if (sourceProduct != null) {
                validateSourceProduct(declaredField.getName(),
                                      sourceProduct,
                                      sourceProductAnnotation.type(),
                                      sourceProductAnnotation.bands());
                setSourceProductFieldValue(declaredField, sourceProduct);
                setSourceProduct(productMapName, sourceProduct);
            } else {
                sourceProduct = getSourceProductFieldValue(declaredField);
                if (sourceProduct != null) {
                    setSourceProduct(declaredField.getName(), sourceProduct);
                } else if (!sourceProductAnnotation.optional()) {
                    String text = "Mandatory source product (field '%s') not set.";
                    String msg = formatExceptionMessage(text, declaredField.getName());
                    throw new OperatorException(msg);
                }
            }
        } else {
            String text = "A source product (field '%s') must be of type '%s'.";
            String msg = formatExceptionMessage(text, declaredField.getName(), Product.class.getName());
            throw new OperatorException(msg);
        }
    }

    private void processSourceProductsField(Field declaredField, SourceProducts sourceProductsAnnotation) throws
            OperatorException {
        if (declaredField.getType().equals(Product[].class)) {
            Product[] sourceProducts = getSourceProductsFieldValue(declaredField);
            if (sourceProducts != null) {
                for (int i = 0; i < sourceProducts.length; i++) {
                    Product sourceProduct = sourceProducts[i];
                    setSourceProduct(GPF.SOURCE_PRODUCT_FIELD_NAME + '.' + (i + 1), sourceProduct);
                    // kept for backward compatibility
                    // since BEAM 4.9 the pattern above is preferred
                    setSourceProduct(GPF.SOURCE_PRODUCT_FIELD_NAME + (i + 1), sourceProduct);
                }
            }
            sourceProducts = getSourceProducts();
            if (sourceProducts.length > 0) {
                setSourceProductsFieldValue(declaredField, getUnnamedProducts());
            }
            if (sourceProductsAnnotation.count() < 0) {
                if (sourceProducts.length == 0) {
                    String msg = formatExceptionMessage("At least a single source product expected.");
                    throw new OperatorException(msg);
                }
            } else if (sourceProductsAnnotation.count() > 0) {
                if (sourceProductsAnnotation.count() != sourceProducts.length) {
                    String text = "Wrong number of source products. Required %d, found %d.";
                    String msg = formatExceptionMessage(text, sourceProductsAnnotation.count(), sourceProducts.length);
                    throw new OperatorException(msg);
                }
            }
            for (Product sourceProduct : sourceProducts) {
                validateSourceProduct(declaredField.getName(),
                                      sourceProduct,
                                      sourceProductsAnnotation.type(),
                                      sourceProductsAnnotation.bands());
            }
        } else {
            String text = "Source products (field '%s') must be of type '%s'.";
            String msg = formatExceptionMessage(text, declaredField.getName(), Product[].class.getName());
            throw new OperatorException(msg);
        }
    }

    private Product[] getUnnamedProducts() {
        final List<Product> srcProductList = new ArrayList<>(sourceProductList.size());
        srcProductList.addAll(sourceProductList);

        final Field[] sourceProductFields = getAnnotatedSourceProductFields(operator);
        for (Field sourceProductField : sourceProductFields) {
            Product product = sourceProductMap.get(sourceProductField.getName());
            if (product != null) {
                srcProductList.remove(product);
            }
            final SourceProduct annotation = sourceProductField.getAnnotation(SourceProduct.class);
            product = sourceProductMap.get(annotation.alias());
            if (product != null) {
                srcProductList.remove(product);
            }
        }
        return srcProductList.toArray(new Product[srcProductList.size()]);
    }

    private static Field[] getAnnotatedSourceProductFields(Operator operator1) {
        Field[] declaredFields = operator1.getClass().getDeclaredFields();
        List<Field> fieldList = new ArrayList<>();
        for (Field declaredField : declaredFields) {
            SourceProduct sourceProductAnnotation = declaredField.getAnnotation(SourceProduct.class);
            //noinspection VariableNotUsedInsideIf
            if (sourceProductAnnotation != null) {
                fieldList.add(declaredField);
            }
        }
        return fieldList.toArray(new Field[fieldList.size()]);
    }

    private Product getSourceProductFieldValue(Field declaredField) throws OperatorException {
        return (Product) getOperatorFieldValue(declaredField);
    }

    private void setSourceProductFieldValue(Field declaredField, Product sourceProduct) throws OperatorException {
        setOperatorFieldValue(declaredField, sourceProduct);
    }

    private Product[] getSourceProductsFieldValue(Field declaredField) throws OperatorException {
        return (Product[]) getOperatorFieldValue(declaredField);
    }

    private void setSourceProductsFieldValue(Field declaredField, Product[] sourceProducts) throws OperatorException {
        setOperatorFieldValue(declaredField, sourceProducts);
    }

    private void validateSourceProduct(String fieldName, Product sourceProduct, String typeRegExp,
                                       String[] bandNames) throws OperatorException {
        if (!typeRegExp.isEmpty()) {
            final String productType = sourceProduct.getProductType();
            if (!typeRegExp.equalsIgnoreCase(productType) && !Pattern.matches(typeRegExp, productType)) {
                String msg = formatExceptionMessage(
                        "A source product (field '%s') of type '%s' does not match type '%s'",
                        fieldName,
                        productType,
                        typeRegExp);
                throw new OperatorException(msg);
            }
        }

        if (bandNames.length != 0) {
            for (String bandName : bandNames) {
                if (!sourceProduct.containsBand(bandName)) {
                    String msg = formatExceptionMessage("A source product (field '%s') does not contain the band '%s'",
                                                        fieldName, bandName);
                    throw new OperatorException(msg);
                }
            }
        }
    }


    private Object getOperatorFieldValue(Field declaredField) throws OperatorException {
        boolean oldState = declaredField.isAccessible();
        try {
            declaredField.setAccessible(true);
            try {
                return declaredField.get(getOperator());
            } catch (IllegalAccessException e) {
                String msg = formatExceptionMessage("Unable to get declared field '%s'.", declaredField.getName());
                throw new OperatorException(msg, e);
            }
        } finally {
            declaredField.setAccessible(oldState);
        }
    }

    private void setOperatorFieldValue(Field declaredField, Object value) throws OperatorException {
        boolean oldState = declaredField.isAccessible();
        try {
            declaredField.setAccessible(true);
            try {
                declaredField.set(getOperator(), value);
            } catch (IllegalAccessException e) {
                String msg = formatExceptionMessage("Unable to set declared field '%s'", declaredField.getName());
                throw new OperatorException(msg, e);
            }
        } finally {
            declaredField.setAccessible(oldState);
        }
    }

    public void injectConfiguration() throws OperatorException {
        if (configuration != null) {
            try {
                configureOperator(configuration);
            } catch (OperatorException e) {
                throw e;
            } catch (Throwable t) {
                throw new OperatorException(formatExceptionMessage("%s", t.getMessage()), t);
            }
        }
    }

    private void configureOperator(OperatorConfiguration operatorConfiguration)
            throws ValidationException, ConversionException {

        OperatorDescriptor operatorDescriptor = getOperatorSpi().getOperatorDescriptor();
        ParameterDescriptorFactory descriptorFactory = new ParameterDescriptorFactory(sourceProductMap);

        PropertySetDescriptor propertySetDescriptor = PropertySetDescriptorFactory.createForOperator(operatorDescriptor,
                                                                                                     descriptorFactory.getSourceProductMap());

        Class<? extends Operator> operatorType = operatorDescriptor.getOperatorClass();
        DefaultDomConverter domConverter = new DefaultDomConverter(operatorType, descriptorFactory, propertySetDescriptor);
        domConverter.convertDomToValue(operatorConfiguration.getConfiguration(), getParameterSet());

        Set<OperatorConfiguration.Reference> referenceSet = operatorConfiguration.getReferenceSet();
        for (OperatorConfiguration.Reference reference : referenceSet) {
            Property property = getParameterSet().getProperty(reference.getParameterName());
            property.setValue(reference.getValue());
        }
    }

    void updatePropertyDescriptors() throws OperatorException {
        Property[] properties = getParameterSet().getProperties();
        for (Property property : properties) {
            PropertyDescriptor descriptor = property.getDescriptor();
            if (descriptor.getAttribute(RasterDataNodeValues.ATTRIBUTE_NAME) != null) {
                Product sourceProduct = sourceProductList.get(0);
                if (sourceProduct == null) {
                    throw new OperatorException(formatExceptionMessage("No source product."));
                }
                Object object = descriptor.getAttribute(RasterDataNodeValues.ATTRIBUTE_NAME);
                Class<? extends RasterDataNode> rasterDataNodeType = (Class<? extends RasterDataNode>) object;
                final boolean includeEmptyValue = !descriptor.isNotNull() && !descriptor.isNotEmpty() && !descriptor.getType().isArray();
                String[] names = RasterDataNodeValues.getNames(sourceProduct, rasterDataNodeType, includeEmptyValue);
                ValueSet valueSet = new ValueSet(names);
                descriptor.setValueSet(valueSet);
            }
        }
    }

    private String formatExceptionMessage(String format, Object... args) {
        Object[] allArgs = new Object[args.length + 1];
        allArgs[0] = operator.getClass().getSimpleName();
        System.arraycopy(args, 0, allArgs, 1, allArgs.length - 1);
        return String.format("Operator '%s': " + format, allArgs);
    }

    private void startTileComputationObservation() {
        if (tileComputationObserver == null) {
            String tchClass = Config.instance().preferences().get(GPF.TILE_COMPUTATION_OBSERVER_PROPERTY, null);
            if (tchClass != null) {
                try {
                    tileComputationObserver = (TileComputationObserver) Class.forName(tchClass).newInstance();
                    tileComputationObserver.setLogger(logger);
                    tileComputationObserver.start();
                } catch (Throwable t) {
                    getLogger().warning("Failed to instantiate tile computation observer: " + t.getMessage());
                }
            }
        }
    }

    public void stopTileComputationObservation() {
        if (tileComputationObserver != null) {
            tileComputationObserver.stop();
            tileComputationObserver = null;
        }
    }

    public void fireTileComputed(OperatorImage operatorImage, Rectangle destRect, long startNanos) {
        if (tileComputationObserver != null) {
            long endNanos = System.nanoTime();
            int tileX = operatorImage.XToTileX(destRect.x);
            int tileY = operatorImage.YToTileY(destRect.y);
            long nettoNanos = getNettoTime();
            tileComputationObserver.tileComputed(
                    new TileComputationEvent(operatorImage, tileX, tileY, startNanos, endNanos, nettoNanos));
        }
    }

    /////////////////////////////////////////////////////////////////////////////////////
    private final ThreadLocal<SuspendableStopWatch> nettoWatch = new ThreadLocal<SuspendableStopWatch>() {
        @Override
        protected SuspendableStopWatch initialValue() {
            return new SuspendableStopWatch();
        }
    };

    public void startWatch() {
        if (tileComputationObserver != null) {
            nettoWatch.get().start();
        }
    }

    public void stopWatch() {
        if (tileComputationObserver != null) {
            nettoWatch.get().stop();
        }
    }

    public void suspendWatch() {
        if (tileComputationObserver != null) {
            nettoWatch.get().suspend();
        }
    }

    public void resumeWatch() {
        if (tileComputationObserver != null) {
            nettoWatch.get().resume();
        }
    }

    public long getNettoTime() {
        return nettoWatch.get().getTime();
    }

    /////////////////////////////////////////////////////////////////////////////////////
    boolean isComputingImageOf(Band band) {
        if (band.isSourceImageSet()) {
            RenderedImage sourceImage = band.getSourceImage().getImage(0);
            OperatorImage targetImage = getTargetImage(band);
            //noinspection ObjectEquality
            return targetImage == sourceImage;
        } else {
            return false;
        }
    }

    public boolean requiresAllBands() {
        return requiresAllBands;
    }

    public void setRequiresAllBands(boolean requiresAllBands) {
        this.requiresAllBands = requiresAllBands;
    }

    public synchronized void executeOperator(ProgressMonitor pm) {
        if (!executed) {
            getOperator().doExecute(pm);
            executed = true;
        }
    }

    private static final class SuspendableStopWatch {

        private long startTime = -1;
        private long stopTime = -1;

        public void start() {
            this.stopTime = -1;
            this.startTime = System.nanoTime();
        }

        public void stop() {
            this.stopTime = System.nanoTime();
        }

        public void suspend() {
            this.stopTime = System.nanoTime();
        }

        public void resume() {
            this.startTime += (System.nanoTime() - this.stopTime);
            this.stopTime = -1;
        }

        public long getTime() {
            return this.stopTime - this.startTime;
        }
    }
}
