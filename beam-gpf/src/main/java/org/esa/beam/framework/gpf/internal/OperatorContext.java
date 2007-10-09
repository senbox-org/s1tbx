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

import com.bc.ceres.core.ProgressMonitor;
import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.framework.gpf.*;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.Guardian;
import org.esa.beam.util.jai.JAIUtils;
import org.esa.beam.util.jai.RasterDataNodeOpImage;

import java.awt.Dimension;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.logging.Logger;

/**
 * The context in which operators are executed.
 *
 * @author Norman Fomferra
 * @author Marco Zühlke
 * @version $Revision$ $Date$
 * @since 4.1
 */
public class OperatorContext {
    private String operatorName;
    private List<Product> sourceProductList;
    private Map<String, Object> parameters;
    private Map<String, Product> sourceProductMap;
    private Product targetProduct;
    private OperatorSpi operatorSpi;
    private Operator operator;
    private RenderedImage[] targetImages;
    private boolean tileMethodImplemented;
    private boolean tileStackMethodImplemented;
    private Xpp3Dom configuration;
    private Logger logger;

    /**
     * Constructs an new context with the name of an operator.
     *
     * @param operatorName the name of the operator
     */
    public OperatorContext(String operatorName) {
        this();
        this.operatorName = operatorName;
    }

    public OperatorContext() {
        sourceProductList = new ArrayList<Product>(3);
        sourceProductMap = new HashMap<String, Product>(3);
        logger = Logger.getAnonymousLogger();
    }

    public RenderedImage[] getTargetImages() {
        return targetImages;
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
        return logger;
    }

    public void setLogger(Logger logger) {
        this.logger = logger;
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
        Set<Map.Entry<String, Product>> entrySet = sourceProductMap.entrySet();
        for (Map.Entry<String, Product> entry : entrySet) {
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
        if (targetProduct == null) {
            try {
                initOperator();
            } catch (OperatorException e) {
                throw new RuntimeException(e);
            }
        }
        return targetProduct;
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
        return targetProduct != null;
    }

    public boolean canComputeTile() {
        return tileMethodImplemented;
    }

    public boolean canComputeTileStack() {
        return tileStackMethodImplemented;
    }

    public Tile getSourceTile(RasterDataNode rasterDataNode, Rectangle rectangle) {
        RenderedImage image = getSourceImage(rasterDataNode);
        /////////////////////////////////////////////////////////////////////
        //
        // Note: GPF pull-processing is triggered here!!!
        //
        Raster awtRaster = image.getData(rectangle); // Note: copyData is NOT faster!
        //
        /////////////////////////////////////////////////////////////////////
        return new TileImpl(rasterDataNode, awtRaster);
    }

    private static RenderedImage getSourceImage(RasterDataNode rasterDataNode) {
        RenderedImage image = rasterDataNode.getImage();
        if (image == null) {
            image = new RasterDataNodeOpImage(rasterDataNode);
            rasterDataNode.setImage(image);
        }
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

    public boolean isCancellationRequested() {
        // todo - implement missing logic here (nf - 08.10.2007)
        return false;
    }

    public ProgressMonitor createProgressMonitor() {
        // todo - implement missing logic here (nf - 08.10.2007)
        return ProgressMonitor.NULL;
    }

    public void initOperatorContext() throws OperatorException {
        OperatorSpi operatorSpi = getOperatorSpi();
        if (operatorSpi == null) {
            String operatorName = getOperatorName();
            operatorSpi = findOperatorSpi(operatorName);
            setOperatorSpi(operatorSpi);
        }

        Operator operator;
        try {
            operator = operatorSpi.getOperatorClass().newInstance();
            setOperator(operator);
        } catch (Throwable e) {
            throw new OperatorException(
                    String.format("Failed to create instance of operator [%s].", operatorSpi.getName()), e);
        }

        initOperator();
    }

    private OperatorSpi findOperatorSpi(String operatorName) throws OperatorException {
        OperatorSpi operatorSpi;
        if (operatorName == null || operatorName.isEmpty()) {
            throw new IllegalStateException("operatorSpiClassName == null || operatorSpiClassName.isEmpty()");
        }
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        operatorSpi = registry.getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new OperatorException(String.format("Unknown operator [%s].", operatorName));
        }
        return operatorSpi;
    }

    private void initOperator() throws OperatorException {
        Guardian.assertTrue("operator != null", operator != null);

        if (parameters != null) {
            new MapParameterInjector(parameters).injectParameters(operator);
        }
        if (configuration != null) {
            new Xpp3DomParameterInjector(configuration).injectParameters(operator);
        }
        initSourceProductFields();
        targetProduct = operator.initialize();
        if (targetProduct == null) {
            throw new IllegalStateException(String.format("Operator [%s] has no target product.", operator.getClass().getName()));
        }
        initTargetProductField();
        initTargetOpImages();

        ProductReader oldProductReader = targetProduct.getProductReader();
        if (oldProductReader == null) {
            OperatorProductReader operatorProductReader = new OperatorProductReader(operator);
            targetProduct.setProductReader(operatorProductReader);
        }
    }

    private void initTargetOpImages() {
        if (targetProduct.getPreferredTileSize() == null) {
            Dimension tileSize = JAIUtils.computePreferredTileSize(targetProduct.getSceneRasterWidth(),
                                                                   targetProduct.getSceneRasterHeight(), 4);
            targetProduct.setPreferredTileSize(tileSize);
        }

        Band[] bands = targetProduct.getBands();
        targetImages = new GpfOpImage[bands.length];
        for (int i = 0; i < bands.length; i++) {
            Band band = bands[i];
            if (band.getImage() == null) {
                if (band instanceof VirtualBand) {
                    band.setImage(new RasterDataNodeOpImage(band));
                } else {
                    band.setImage(new GpfOpImage(band, this));
                }
            }
            targetImages[i] = band.getImage();
        }
    }

    public void initTargetProductField() throws OperatorException {
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

    public void initSourceProductFields() throws OperatorException {
        Field[] declaredFields = getOperator().getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            SourceProduct sourceProductAnnotation = declaredField.getAnnotation(SourceProduct.class);
            if (sourceProductAnnotation != null) {
                if (declaredField.getType().equals(Product.class)) {
                    Product sourceProduct = getSourceProduct(declaredField.getName());
                    if (sourceProduct == null) {
                        sourceProduct = getSourceProduct(sourceProductAnnotation.alias());
                    }
                    if (sourceProduct != null) {
                        validateSourceProduct(sourceProduct, declaredField, sourceProductAnnotation);
                        boolean oldState = declaredField.isAccessible();
                        try {
                            declaredField.setAccessible(true);
                            try {
                                declaredField.set(getOperator(), sourceProduct);
                            } catch (IllegalAccessException e) {
                                String text = "Unable to initialize declared field '%s'";
                                String msg = String.format(text, declaredField.getName());
                                throw new OperatorException(msg, e);
                            }
                        } finally {
                            declaredField.setAccessible(oldState);
                        }
                    } else if (!sourceProductAnnotation.optional()) {
                        String text = "Mandatory source product field '%s' not set.";
                        String msg = String.format(text, declaredField.getName());
                        throw new OperatorException(msg);
                    }
                } else {
                    String text = "Annotated field '%s' is not of type '%s'.";
                    String msg = String.format(text, declaredField.getName(), Product.class.getName());
                    throw new OperatorException(msg);
                }
            }
            SourceProducts sourceProductsAnnotation = declaredField.getAnnotation(SourceProducts.class);
            if (sourceProductsAnnotation != null) {
                if (declaredField.getType().equals(Product[].class)) {
                    Product[] sourceProducts = getSourceProducts();
                    if (sourceProducts.length > 0) {
                        boolean oldState = declaredField.isAccessible();
                        try {
                            declaredField.setAccessible(true);
                            try {
                                declaredField.set(getOperator(), sourceProducts);
                            } catch (IllegalAccessException e) {
                                String text = "Unable to initialize declared field '%s'.";
                                String msg = String.format(text, declaredField.getName());
                                throw new OperatorException(msg, e);
                            }
                        } finally {
                            declaredField.setAccessible(oldState);
                        }
                    } else if (!sourceProductsAnnotation.optional()) {
                        String text = "Mandatory source products field '%s' not set.";
                        String msg = String.format(text, declaredField.getName());
                        throw new OperatorException(msg);
                    }
                } else {
                    String text = "Annotated field '%s' is not of type '%s'.";
                    String msg = String.format(text, declaredField.getName(), Product[].class.getName());
                    throw new OperatorException(msg);
                }
            }
        }
    }

    public static void validateSourceProduct(Product sourceProduct,
                                             Field declaredField,
                                             SourceProduct sourceProductAnnotation) throws OperatorException {
        if (!sourceProductAnnotation.type().isEmpty() &&
                !sourceProductAnnotation.type().equals(sourceProduct.getProductType())) {
            String msg = String.format(
                    "The source product '%s' must be of type '%s' but is of type '%s'",
                    declaredField.getName(), sourceProductAnnotation.type(),
                    sourceProduct.getProductType());
            throw new OperatorException(msg);
        }
        if (sourceProductAnnotation.bands().length != 0) {
            String[] expectedBandNames = sourceProductAnnotation.bands();
            for (String bandName : expectedBandNames) {
                if (!sourceProduct.containsBand(bandName)) {
                    String msg = String.format("The source product '%s' does not contain the band '%s'",
                                               declaredField.getName(), bandName);
                    throw new OperatorException(msg);
                }
            }
        }
    }

    public void setParameters(Map<String, Object> parameters) {
        this.parameters = new HashMap<String, Object>(parameters);
    }

    public void setParameters(Xpp3Dom configuration) {
        this.configuration = configuration;
    }
}