/*
 * $Id$
 *
 * Copyright (C) 2007 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.binding.*;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.Assert;
import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.*;
import org.esa.beam.framework.gpf.graph.GraphOp;
import org.esa.beam.framework.gpf.graph.OperatorConfiguration;
import org.esa.beam.framework.gpf.graph.OperatorConfiguration.Reference;
import org.esa.beam.util.jai.JAIUtils;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import javax.media.jai.JAI;
import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.Map.Entry;
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * The context in which operators are executed.
 *
 * @author Norman Fomferra
 * @since 4.1
 */
public class OperatorContext {
    private static final String OPERATION_CANCELED_MESSAGE = "Operation canceled.";
    private String id;
    private Product targetProduct;
    private OperatorSpi operatorSpi;
    private Operator operator;
    private boolean computeTileMethodUsable;
    private boolean computeTileStackMethodUsage;
    private boolean passThrough;
    private List<Product> sourceProductList;
    private Map<String, Object> parameters;
    private Map<String, Product> sourceProductMap;
    private Map<String, Object> targetPropertyMap;
    private Map<Band, RasterDataNodeOpImage> targetImageMap;
    private OperatorConfiguration configuration;
    private Logger logger;
    private boolean disposed;
    private ValueContainer valueContainer;

    public OperatorContext(Operator operator) {
        this.operator = operator;
        this.computeTileMethodUsable = canOperatorComputeTile(operator.getClass());
        this.computeTileStackMethodUsage = canOperatorComputeTileStack(operator.getClass());
        this.sourceProductList = new ArrayList<Product>(3);
        this.sourceProductMap = new HashMap<String, Product>(3);
        this.targetPropertyMap = new HashMap<String, Object>(3);
        this.logger = Logger.getAnonymousLogger();
    }

    public String getId() {
        if (id == null) {
            id = getOperatorSpi().getOperatorAlias() + "$" + Long.toHexString(System.currentTimeMillis()).toUpperCase();
        }
        return id;
    }

    public void setId(String id) {
        Assert.notNull(id, "logger");
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
        if (!sourceProductList.contains(product)) {
            sourceProductList.add(product);
        }
        sourceProductMap.put(id, product);
    }

    public Product[] getSourceProducts() {
        return sourceProductList.toArray(new Product[sourceProductList.size()]);
    }

    public void setSourceProducts(Product[] products) {
        sourceProductList.clear();
        sourceProductMap.clear();
        for (int i = 0; i < products.length; i++) {
            Product product = products[i];
            setSourceProduct(GPF.SOURCE_PRODUCT_FIELD_NAME + (i + 1), product);
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
        for (Map.Entry<String, Product> entry : entrySet) {
            if (entry.getValue() == product) {
                return entry.getKey();
            }
        }
        return null;
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

    public boolean isPassThrough() {
        return passThrough;
    }

    public void checkForCancelation(ProgressMonitor pm) throws OperatorException {
        if (pm.isCanceled()) {
            throw new OperatorException(OPERATION_CANCELED_MESSAGE);
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

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = new HashMap<String, Object>(parameters);
    }


    public void setConfiguration(OperatorConfiguration opConfiguration) {
        this.configuration = opConfiguration;
    }

    public boolean isInitialized() {
        return targetProduct != null;
    }

    public boolean isComputeTileMethodUsable() {
        return computeTileMethodUsable;
    }

    public boolean isComputeTileStackMethodUsage() {
        return computeTileStackMethodUsage;
    }

    public void setComputeTileMethodUsable(boolean computeTileMethodUsable) {
        this.computeTileMethodUsable = computeTileMethodUsable;
    }

    public void setComputeTileStackMethodUsage(boolean computeTileStackMethodUsage) {
        this.computeTileStackMethodUsage = computeTileStackMethodUsage;
    }

    public Tile getSourceTile(RasterDataNode rasterDataNode, Rectangle rectangle, ProgressMonitor pm) {
        RenderedImage image = getSourceImage(rasterDataNode);
        ProgressMonitor oldPm = RasterDataNodeOpImage.setProgressMonitor(image, pm);
        try {
            /////////////////////////////////////////////////////////////////////
            //
            // Note: GPF pull-processing is triggered here!
            //
            Raster awtRaster = image.getData(rectangle); // Note: copyData is NOT faster!
            //
            /////////////////////////////////////////////////////////////////////
            return new TileImpl(rasterDataNode, awtRaster);
        } finally {
            RasterDataNodeOpImage.setProgressMonitor(image, oldPm);
        }
    }

    public RasterDataNodeOpImage getTargetImage(Band band) {
        return targetImageMap.get(band);
    }

    public boolean isDisposed() {
        return disposed;
    }

    public void dispose() {
        if (!disposed) {
            disposed = true;
            parameters = null;
            configuration = null;
            sourceProductMap.clear();
            sourceProductList.clear();
            Collection<RasterDataNodeOpImage> operatorImages = targetImageMap.values();
            for (RasterDataNodeOpImage image : operatorImages) {
                RasterDataNode rdn = image.getRasterDataNode();
                if (rdn != null && rdn.getImage() instanceof OperatorImage) {
                    rdn.setImage(null);
                }
                image.dispose();
                JAI.getDefaultInstance().getTileCache().removeTiles(image);
            }
            targetImageMap.clear();
            operator.dispose();
        }
    }

    private static RenderedImage getSourceImage(RasterDataNode rasterDataNode) {
        RenderedImage image = rasterDataNode.getImage();
        if (image == null) {
            image = new RasterDataNodeOpImage(rasterDataNode);
            rasterDataNode.setImage(image);
        }
        return image;
    }

    private static boolean canOperatorComputeTile(Class<? extends Operator> aClass) {
        return implementsMethod(aClass, "computeTile",
                new Class[]{
                        Band.class,
                        Tile.class,
                        ProgressMonitor.class});
    }

    private static boolean canOperatorComputeTileStack(Class<? extends Operator> aClass) {
        return implementsMethod(aClass, "computeTileStack",
                new Class[]{
                        Map.class,
                        Rectangle.class,
                        ProgressMonitor.class});
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
        Assert.state(operator != null, "operator != null");

        if (!(operator instanceof GraphOp)) {
            initSourceProductFields();
            injectParameterValues();
            injectConfiguration();
        }
        operator.initialize();
        initTargetProduct();
        initTargetProperties();
        if (!(operator instanceof GraphOp)) {
            initPassThrough();
        }
        initTargetImages();
        initGraphMetadata();

        ProductReader oldProductReader = targetProduct.getProductReader();
        if (oldProductReader == null) {
            OperatorProductReader operatorProductReader = new OperatorProductReader(this);
            targetProduct.setProductReader(operatorProductReader);
        }
    }

    private ValueContainer getOperatorValueContainer() {
        if (valueContainer == null) {
            ClassFieldDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory(sourceProductMap);
            valueContainer = ValueContainer.createObjectBacked(operator, parameterDescriptorFactory);
        }
        return valueContainer;
    }

    private void initGraphMetadata() {
        final MetadataElement metadataRoot = targetProduct.getMetadataRoot();
        MetadataElement targetGraphME = metadataRoot.getElement("Processing_Graph");
        if (targetGraphME == null) {
            targetGraphME = new MetadataElement("Processing_Graph");
            metadataRoot.addElement(targetGraphME);
        }
        convertOperatorContextToMetadata(this, targetGraphME);
    }

    private void convertOperatorContextToMetadata(OperatorContext context, MetadataElement targetGraphME) {
        String opId = context.getId();
        boolean contains = false;
        int nodeElementCount = 0;
        for (MetadataElement element : targetGraphME.getElements()) {
            MetadataAttribute idAttribute = element.getAttribute("id");
            if (idAttribute.getData().getElemString().equals(opId)) {
                contains = true;
            }
            if(element.getName().startsWith("node")) {
                nodeElementCount++;
            }
        }
        if (contains) {
            return;
        }
        final String opName = OperatorSpi.getOperatorAlias(context.operator.getClass());
        MetadataElement targetNodeME = new MetadataElement(String.format("node.%d",nodeElementCount));
        targetGraphME.addElement(targetNodeME);
        targetNodeME.addAttribute(new MetadataAttribute("id", ProductData.createInstance(opId), false));
        targetNodeME.addAttribute(new MetadataAttribute("operator", ProductData.createInstance(opName), false));
        final MetadataElement targetSourcesME = new MetadataElement("sources");
        for (String sourceId : context.sourceProductMap.keySet()) {
            final Product sourceProduct = context.sourceProductMap.get(sourceId);
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
            final MetadataAttribute sourceAttribute = new MetadataAttribute(sourceId, ProductData.createInstance(sourceNodeId), false);
            targetSourcesME.addAttribute(sourceAttribute);
        }
        targetNodeME.addElement(targetSourcesME);

        final DefaultDomConverter domConverter = new DefaultDomConverter(context.operator.getClass(), new ParameterDescriptorFactory(sourceProductMap));
        final Xpp3DomElement parametersDom = Xpp3DomElement.createDomElement("parameters");
        domConverter.convertValueToDom(context.operator, parametersDom);
        final MetadataElement targetParametersME = new MetadataElement("parameters");
        addDomToMetadata(parametersDom, targetParametersME);
        targetNodeME.addElement(targetParametersME);
    }

    private static void addDomToMetadata(DomElement parentDE, MetadataElement parentME) {
        final HashMap<String, List<DomElement>> map = new HashMap<String, List<DomElement>>(parentDE.getChildCount() + 5);
        for (DomElement childDE : parentDE.getChildren()) {
            final String name = childDE.getName();
            List<DomElement> elementList = map.get(name);
            if (elementList == null) {
                elementList = new ArrayList<DomElement>(3);
                map.put(name, elementList);
            }
            elementList.add(childDE);
        }
        for (Entry<String, List<DomElement>> entry : map.entrySet()) {
            String name = entry.getKey();
            final List<DomElement> elementList = entry.getValue();
            if (elementList.size() > 1) {
                for (int i = 0; i < elementList.size(); i++) {
                    addDomToMetadata(elementList.get(i), name + "." + i, parentME);
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

    private boolean initPassThrough() {
        passThrough = false;
        Product[] sourceProducts = getSourceProducts();
        for (Product sourceProduct : sourceProducts) {
            if (targetProduct == sourceProduct) {
                passThrough = true;
            }
        }
        return passThrough;
    }

    private void initTargetImages() {
        if (targetProduct.getPreferredTileSize() == null) {
            Dimension tileSize = null;
            for (Product sourceProduct : sourceProductList) {
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
            targetProduct.setPreferredTileSize(tileSize);
        }

        Band[] bands = targetProduct.getBands();
        targetImageMap = new HashMap<Band, RasterDataNodeOpImage>(bands.length * 2);
        for (Band band : bands) {
            final RasterDataNodeOpImage image;
            // Note: "instanceof" has intentionally not been used
            if (band.getClass() == Band.class) {
                // Create an image that calls the operator to compute a tile
                image = new OperatorImage(band, this);
            } else {
                // Create an image that calls Band.readRasterDataNode() to compute a tile (VirtualBand, FilterBand, ...)
                image = new RasterDataNodeOpImage(band);
            }
            if (band.getImage() == null) {
                band.setImage(image);
            }
            targetImageMap.put(band, image);
        }
    }

    private void initTargetProduct() throws OperatorException {
        Field[] declaredFields = operator.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
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
        if (targetProduct == null) {
            final String message = formatExceptionMessage("No target product set.");
            throw new OperatorException(message);
        }
    }

    private void initTargetProperties() throws OperatorException {
        Field[] declaredFields = operator.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            TargetProperty targetPropertyAnnotation = declaredField.getAnnotation(TargetProperty.class);
            if (targetPropertyAnnotation != null) {
                Object propertyValue = getOperatorFieldValue(declaredField);
                String fieldName = declaredField.getName();
                if (targetPropertyMap.containsKey(fieldName)) {
                    final String message = formatExceptionMessage("Name of field '%s' is already used as target property alias.",
                            fieldName);
                    throw new OperatorException(message);
                }
                targetPropertyMap.put(fieldName, propertyValue);
                if (!targetPropertyAnnotation.alias().isEmpty()) {
                    String aliasName = targetPropertyAnnotation.alias();
                    if (targetPropertyMap.containsKey(aliasName)) {
                        final String message = formatExceptionMessage("Alias of field '%s' is already used by another target property.",
                                aliasName);
                        throw new OperatorException(message);
                    }
                    targetPropertyMap.put(aliasName, propertyValue);
                }
            }
        }
    }

    private void initSourceProductFields() throws OperatorException {
        Field[] declaredFields = operator.getClass().getDeclaredFields();
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
    }

    private void processSourceProductField(Field declaredField, SourceProduct sourceProductAnnotation) throws OperatorException {
        if (declaredField.getType().equals(Product.class)) {
            Product sourceProduct = getSourceProduct(declaredField.getName());
            if (sourceProduct == null) {
                sourceProduct = getSourceProduct(sourceProductAnnotation.alias());
            }
            if (sourceProduct != null) {
                validateSourceProduct(declaredField.getName(),
                        sourceProduct,
                        sourceProductAnnotation.type(),
                        sourceProductAnnotation.bands());
                setSourceProductFieldValue(declaredField, sourceProduct);
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

    private void processSourceProductsField(Field declaredField, SourceProducts sourceProductsAnnotation) throws OperatorException {
        if (declaredField.getType().equals(Product[].class)) {
            Product[] sourceProducts = getSourceProducts();
            if (sourceProducts.length > 0) {
                setSourceProductsFieldValue(declaredField, sourceProducts);
            } else {
                sourceProducts = getSourceProductsFieldValue(declaredField);
                if (sourceProducts != null) {
                    for (int i = 0; i < sourceProducts.length; i++) {
                        Product sourceProduct = sourceProducts[i];
                        setSourceProduct(GPF.SOURCE_PRODUCT_FIELD_NAME + i, sourceProduct);
                    }
                }
                sourceProducts = getSourceProducts();
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

    private void validateSourceProduct(String fieldName, Product sourceProduct, String typeRegExp, String[] bandNames) throws OperatorException {
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
                configureValue(configuration, operator);
            } catch (OperatorException e) {
                throw e;
            } catch (Throwable t) {
                throw new OperatorException(formatExceptionMessage("%s", t.getMessage()), t);
            }
        }
    }

    private void configureValue(OperatorConfiguration operatorConfiguration, Object value) throws ValidationException, ConversionException {
        final Xpp3DomElement xpp3DomElement = Xpp3DomElement.createDomElement(operatorConfiguration.getConfiguration());
        ParameterDescriptorFactory parameterDescriptorFactory = new ParameterDescriptorFactory(sourceProductMap);
        final DefaultDomConverter domConverter = new DefaultDomConverter(value.getClass(), parameterDescriptorFactory);
        domConverter.convertDomToValue(xpp3DomElement, value);

        final ValueContainer valueContainer = ValueContainer.createObjectBacked(value, parameterDescriptorFactory);
        Set<Reference> referenceSet = operatorConfiguration.getReferenceSet();
        for (Reference reference : referenceSet) {
            ValueModel valueModel = valueContainer.getModel(reference.getParameterName());
            valueModel.setValue(reference.getValue());
        }
    }

    public void injectParameterDefaultValues() throws OperatorException {
        try {
            getOperatorValueContainer().setDefaultValues();
        } catch (ValidationException e) {
            throw new OperatorException(formatExceptionMessage("%s", e.getMessage()), e);
        }
    }

    private void injectParameterValues() throws OperatorException {
        if (parameters != null) {
            for (String parameterName : parameters.keySet()) {
                final ValueModel valueModel = getOperatorValueContainer().getModel(parameterName);
                if (valueModel == null) {
                    throw new OperatorException(formatExceptionMessage("Unknown parameter '%s'.", parameterName));
                }
                try {
                    ValueDescriptor descriptor = valueModel.getDescriptor();
                    if (((String) descriptor.getProperty("sourceId")) != null) {
                        String sourceId = (String) descriptor.getProperty("sourceId");
                        Product sourceProduct = getSourceProduct(sourceId);
                        if (sourceProduct == null) {
                            throw new OperatorException(formatExceptionMessage("Unknown sourceId '%s'.", sourceId));
                        }
                        ValueSet valueSet = new ValueSet(sourceProduct.getBandNames());
                        descriptor.setValueSet(valueSet);
                    }
                    valueModel.setValue(parameters.get(parameterName));
                } catch (ValidationException e) {
                    throw new OperatorException(formatExceptionMessage("%s", e.getMessage()), e);
                }
            }
        }
    }

    private String formatExceptionMessage(String format, Object... args) {
        Object[] allArgs = new Object[args.length+1];
        allArgs[0] = operator.getClass().getSimpleName();
        System.arraycopy(args, 0, allArgs, 1, allArgs.length - 1);
        return String.format("Operator '%s': " + format, allArgs);
    }
}