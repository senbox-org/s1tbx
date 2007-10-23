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
import com.bc.ceres.binding.accessors.ClassFieldAccessor;
import com.bc.ceres.binding.dom.DefaultDomConverter;
import com.bc.ceres.binding.dom.DomElement;
import com.bc.ceres.core.ProgressMonitor;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.*;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.*;
import org.esa.beam.util.Guardian;
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
import java.util.logging.Logger;
import java.util.regex.Pattern;

/**
 * The context in which operators are executed.
 *
 * @author Norman Fomferra
 * @since 4.1
 */
public class OperatorContext {
    private Product targetProduct;
    private OperatorSpi operatorSpi;
    private Operator operator;
    private boolean tileMethodImplemented;
    private boolean tileStackMethodImplemented;
    private boolean passThrough;
    private List<Product> sourceProductList;
    private Map<String, Object> parameters;
    private Map<String, Product> sourceProductMap;
    private Map<Band, OperatorImage> targetImages;
    private Xpp3Dom configuration;
    private Logger logger;
    private boolean disposed;

    public OperatorContext(Operator operator) {
        this.operator = operator;
        this.tileMethodImplemented = canOperatorComputeTile(operator.getClass());
        this.tileStackMethodImplemented = canOperatorComputeTileStack(operator.getClass());
        this.sourceProductList = new ArrayList<Product>(3);
        this.sourceProductMap = new HashMap<String, Product>(3);
        this.logger = Logger.getAnonymousLogger();
    }

    public Logger getLogger() {
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
    }

    public Product[] getSourceProducts() {
        return sourceProductList.toArray(new Product[sourceProductList.size()]);
    }

    public Product getSourceProduct(String id) {
        return sourceProductMap.get(id);
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
            initTargetProduct();
        }
        return targetProduct;
    }

    public boolean isPassThrough() {
        return passThrough;
    }

    public void checkForCancelation(ProgressMonitor pm) throws OperatorException {
        if (pm.isCanceled()) {
            throw new OperatorException("Operation canceled.");
        }
    }

    public OperatorSpi getOperatorSpi() {
        if (operatorSpi == null) {
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

    public void setSourceProducts(Map<String, Product> sourceProducts) {
        Set<Map.Entry<String, Product>> entries = sourceProducts.entrySet();
        for (Map.Entry<String, Product> entry : entries) {
            addSourceProduct(entry.getKey(), entry.getValue());
        }
    }

    public void addSourceProduct(String id, Product product) {
        if (!sourceProductList.contains(product)) {
            sourceProductList.add(product);
        }
        sourceProductMap.put(id, product);
    }

    public Map<String, Object> getParameters() {
        return parameters;
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = new HashMap<String, Object>(parameters);
    }


    public void setConfiguration(Xpp3Dom configuration) {
        this.configuration = configuration;
    }

    public boolean isInitialized() {
        return targetProduct != null;
    }

    public boolean canComputeTile() {
        return tileMethodImplemented;
    }

    public boolean canComputeTileStack() {
        return tileStackMethodImplemented;
    }

    public Tile getSourceTile(RasterDataNode rasterDataNode, Rectangle rectangle, ProgressMonitor pm) {
        RenderedImage image = getSourceImage(rasterDataNode);
        ProgressMonitor oldPm = RasterDataNodeOpImage.setProgressMonitor(image, pm);
        try {
            /////////////////////////////////////////////////////////////////////
            //
            // Note: GPF pull-processing is triggered here!!!
            //
            Raster awtRaster = image.getData(rectangle); // Note: copyData is NOT faster!
            //
            /////////////////////////////////////////////////////////////////////
            return new TileImpl(rasterDataNode, awtRaster);
        } finally {
            RasterDataNodeOpImage.setProgressMonitor(image, oldPm);
        }
    }

    public OperatorImage getTargetImage(Band band) {
        return targetImages.get(band);
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
            Collection<OperatorImage> operatorImages = targetImages.values();
            for (OperatorImage image : operatorImages) {
                image.dispose();
                JAI.getDefaultInstance().getTileCache().removeTiles(image);
            }
            targetImages.clear();
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
                    || Operator.class.equals(aClass)
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

    private void initTargetProduct() throws OperatorException {
        Guardian.assertTrue("operator != null", operator != null);

        injectParameters();
        injectConfiguration();
        initSourceProductFields();
        targetProduct = operator.initialize();
        if (targetProduct == null) {
            throw new IllegalStateException(String.format("Operator [%s] has no target product.", operator.getClass().getName()));
        }
        initPassThrough();
        initTargetProductField();
        initTargetImages();
        initGraphMetadata();

        ProductReader oldProductReader = targetProduct.getProductReader();
        if (oldProductReader == null) {
            OperatorProductReader operatorProductReader = new OperatorProductReader(this);
            targetProduct.setProductReader(operatorProductReader);
        }
    }

    private void initGraphMetadata() {
        final MetadataElement metadataRoot = targetProduct.getMetadataRoot();
        MetadataElement graphMetadata = metadataRoot.getElement("Processing_Graph");// todo - element name (mp - 23.10.2007)
        if (graphMetadata == null) {
            graphMetadata = new MetadataElement("Processing_Graph");
            metadataRoot.addElement(graphMetadata);
        }

        MetadataElement nodeMetadata = new MetadataElement("node");
        nodeMetadata.addAttribute(new MetadataAttribute("id", ProductData.createInstance(targetProduct.getName()), false));
        nodeMetadata.addAttribute(new MetadataAttribute("operator", ProductData.createInstance(OperatorSpi.getOperatorAlias(operator.getClass())), false));
        final MetadataElement sourcesMetadata = new MetadataElement("sources");
        for (String sourceId : sourceProductMap.keySet()) {
            final Product product = sourceProductMap.get(sourceId);
            String productRefStr;
            if (product.getFileLocation() != null) {
                productRefStr = product.getFileLocation().getPath();
            } else {
                productRefStr = product.getName(); // todo - obtain reference ID for potential operator target product
            }
            sourcesMetadata.addAttribute(new MetadataAttribute(sourceId, ProductData.createInstance(productRefStr), false));
        }
        nodeMetadata.addElement(sourcesMetadata);

        final DefaultDomConverter domConverter = new DefaultDomConverter(operator.getClass(), new ParameterDefinitionFactory());
        final Xpp3DomElement parametersDom = Xpp3DomElement.createDomElement("parameters");
        domConverter.convertValueToDom(operator, parametersDom);
        final MetadataElement parametersMetadata = new MetadataElement("parameters");
        addDomToMetadata(parametersDom, parametersMetadata);
        nodeMetadata.addElement(parametersMetadata);

        graphMetadata.addElement(nodeMetadata);
    }

    private void addDomToMetadata(DomElement parentDE, MetadataElement parentME) {
        for (DomElement childDE : parentDE.getChildren()) {
            if (childDE.getChildCount() > 0) {
                final MetadataElement childME = new MetadataElement(childDE.getName());
                addDomToMetadata(childDE, childME);
                parentME.addElement(childME);
            } else {
                String valueDE = childDE.getValue();
                if (valueDE == null) {
                    valueDE = "";
                }
                final ProductData valueME = ProductData.createInstance(valueDE);
                final MetadataAttribute attribute = new MetadataAttribute(childDE.getName(), valueME, true);
                parentME.addAttribute(attribute);
            }
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
            Dimension tileSize = JAIUtils.computePreferredTileSize(targetProduct.getSceneRasterWidth(),
                                                                   targetProduct.getSceneRasterHeight(), 4);
            targetProduct.setPreferredTileSize(tileSize);
        }

        Band[] bands = targetProduct.getBands();
        targetImages = new HashMap<Band, OperatorImage>(bands.length * 2);
        for (Band band : bands) {
            OperatorImage image = new OperatorImage(band, this);
            if (band.getImage() == null) {
                band.setImage(image);
            }
            targetImages.put(band, image);
        }
    }

    private void initTargetProductField() throws OperatorException {
        Field[] declaredFields = operator.getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            TargetProduct targetProductAnnotation = declaredField.getAnnotation(TargetProduct.class);
            if (targetProductAnnotation != null) {
                if (!declaredField.getType().equals(Product.class)) {
                    String text = "field '%s' annotated as target product is not of type '%s'.";
                    String msg = String.format(text, declaredField.getName(), Product.class);
                    throw new OperatorException(msg);
                }
                boolean oldState = declaredField.isAccessible();
                try {
                    declaredField.setAccessible(true);
                    Object target = declaredField.get(operator);
                    if (target != targetProduct) {
                        declaredField.set(operator, targetProduct);
                    }
                } catch (IllegalAccessException e) {
                    String text = "not able to initialize declared field '%s'";
                    String msg = String.format(text, declaredField.getName());
                    throw new OperatorException(msg, e);
                } finally {
                    declaredField.setAccessible(oldState);
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
                    addSourceProduct(declaredField.getName(), sourceProduct);
                } else if (!sourceProductAnnotation.optional()) {
                    String text = "Mandatory source product (field '%s') not set.";
                    String msg = String.format(text, declaredField.getName());
                    throw new OperatorException(msg);
                }
            }
        } else {
            String text = "A source product (field '%s') must be of type '%s'.";
            String msg = String.format(text, declaredField.getName(), Product.class.getName());
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
                        addSourceProduct(GPF.SOURCE_PRODUCT_FIELD_NAME + i, sourceProduct);
                    }
                }
                sourceProducts = getSourceProducts();
            }
            if (sourceProductsAnnotation.count() < 0) {
                if (sourceProducts.length == 0) {
                    String msg = "At least a single source product expected.";
                    throw new OperatorException(msg);
                }
            } else if (sourceProductsAnnotation.count() > 0) {
                if (sourceProductsAnnotation.count() != sourceProducts.length) {
                    String text = "Wrong number of source products. Required %d, found %d.";
                    String msg = String.format(text, sourceProductsAnnotation.count(), sourceProducts.length);
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
            String msg = String.format(text, declaredField.getName(), Product[].class.getName());
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

    private static void validateSourceProduct(String fieldName, Product sourceProduct, String typeRegExp, String[] bandNames) throws OperatorException {
        if (!typeRegExp.isEmpty()) {
            final String productType = sourceProduct.getProductType();
            if (!typeRegExp.equalsIgnoreCase(productType) && !Pattern.matches(typeRegExp, productType)) {
                String msg = String.format(
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
                    String msg = String.format("A source product (field '%s') does not contain the band '%s'",
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
                String text = "Unable to get declared field '%s'.";
                String msg = String.format(text, declaredField.getName());
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
                String text = "Unable to set declared field '%s'";
                String msg = String.format(text, declaredField.getName());
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
                throw new OperatorException(t.getMessage(), t);
            }
        }
    }

    private static void configureValue(Xpp3Dom parentElement, Object value) throws ValidationException, ConversionException {
        final Xpp3DomElement xpp3DomElement = Xpp3DomElement.createDomElement(parentElement);
        final DefaultDomConverter domConverter = new DefaultDomConverter(value.getClass(), new ParameterDefinitionFactory());
        domConverter.convertDomToValue(xpp3DomElement, value);
    }

    private static Field getField(Object object, String valueName) {
        try {
            return object.getClass().getDeclaredField(valueName);
        } catch (NoSuchFieldException e) {
            final Field[] declaredFields = object.getClass().getDeclaredFields();
            for (Field declaredField : declaredFields) {
                final Parameter parameter = declaredField.getAnnotation(Parameter.class);
                if (parameter != null && !parameter.alias().isEmpty() && valueName.equals(parameter.alias())) {
                    return declaredField;
                }
            }
            return null;
        }
    }

    public void injectParameters() throws OperatorException {
        if (parameters != null) {
            final ValueDefinitionFactory valueDefinitionFactory = new ParameterDefinitionFactory();
            for (String valueName : parameters.keySet()) {
                final Field field = getField(operator, valueName);
                if (field == null) {
                    throw new OperatorException(String.format("Unknown parameter '%s'.", valueName));
                }
                final ValueDefinition definition = valueDefinitionFactory.createValueDefinition(field);
                final ValueModel valueModel = new ValueModel(definition, new ClassFieldAccessor(operator, field));
                try {
                    valueModel.setValue(parameters.get(valueName));
                } catch (ValidationException e) {
                    throw new OperatorException(String.format(e.getMessage(), e));
                }
            }
        }
    }

}