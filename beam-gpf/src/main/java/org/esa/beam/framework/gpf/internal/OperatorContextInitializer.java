package org.esa.beam.framework.gpf.internal;

import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.SubProgressMonitor;
import org.esa.beam.framework.dataio.ProductReader;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.util.jai.JAIUtils;

import java.awt.Dimension;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;


public class OperatorContextInitializer {

    public static void initOperatorContext(DefaultOperatorContext operatorContext,
                                           ParameterInjector injector,
                                           ProgressMonitor pm) throws OperatorException {
        pm.beginTask("initializing operator contex", 5);
        try {
            OperatorSpi operatorSpi = operatorContext.getOperatorSpi();
            if (operatorSpi == null) {
                String operatorName = operatorContext.getOperatorName();
                if (operatorName == null || operatorName.isEmpty()) {
                    throw new IllegalStateException("operatorSpiClassName == null || operatorSpiClassName.isEmpty()");
                }
                operatorSpi = OperatorSpiRegistry.getInstance().getOperatorSpi(operatorName);
                if (operatorSpi == null) {
                    throw new OperatorException(String.format("Unknown operator [%s].", operatorName));
                }
                operatorContext.setOperatorSpi(operatorSpi);
            }
            pm.worked(1);

            Operator operator;
            try {
                Constructor<? extends Operator> constructor = operatorSpi.getOperatorClass().getDeclaredConstructor(OperatorSpi.class);
                operator = constructor.newInstance(operatorSpi);
                operatorContext.setOperator(operator);
            } catch (Throwable e) {
                throw new OperatorException(String.format("Failed to create instance of operator [%s].", operatorSpi.getName()), e);
            }
            pm.worked(1);

            initAnnotatedSourceProductFields(operatorContext);

            operatorContext.setParameterFields(getParameterFields(operator));

            injector.injectParameters(operator);
            pm.worked(1);

            Product targetProduct = operator.initialize(operatorContext, SubProgressMonitor.create(pm, 1));
            if (targetProduct == null) {
                throw new OperatorException(String.format("Operator [%s] has no target product.", operatorSpi.getName()));
            }
            initTargetProductFieldIfNotDone(operator, targetProduct);
            operatorContext.setTargetProduct(targetProduct);

            final GpfOpImage[] targetImages = createTargetImages(targetProduct, operatorContext);
            operatorContext.setTargetImages(targetImages);

            ProductReader oldProductReader = targetProduct.getProductReader();
            if (oldProductReader == null) {
                OperatorProductReader operatorProductReader = new OperatorProductReader(operatorContext);
                targetProduct.setProductReader(operatorProductReader);
            }

            pm.worked(1);
        } finally {
            pm.done();
        }
    }

    private static GpfOpImage[] createTargetImages(Product targetProduct, DefaultOperatorContext operatorContext) {
        if (targetProduct.getPreferredTileSize() == null) {
            Dimension tileSize = JAIUtils.computePreferredTileSize(targetProduct.getSceneRasterWidth(),
                                                                   targetProduct.getSceneRasterHeight());
            targetProduct.setPreferredTileSize(tileSize);
        }

        Band[] bands = targetProduct.getBands();
        final GpfOpImage[] targetImages = new GpfOpImage[bands.length];
        for (int i = 0; i < bands.length; i++) {
            Band band = bands[i];
            GpfOpImage opImage = new GpfOpImage(band, operatorContext);
            targetImages[i] = opImage;
            band.setImage(opImage);
        }
        return targetImages;
    }

    private static Field[] getParameterFields(Operator operator) {
        List<Field> parameterFields = new ArrayList<Field>();
        collectParameterFields(operator.getClass(), parameterFields);
        return parameterFields.toArray(new Field[parameterFields.size()]);
    }

    private static void collectParameterFields(Class operatorClass, List<Field> parameterFields) {
        if (operatorClass.getSuperclass().isAssignableFrom(Operator.class)) {
            collectParameterFields(operatorClass.getSuperclass(), parameterFields);
        }
        Field[] declaredFields = operatorClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            if (declaredField.getAnnotation(Parameter.class) != null) {
                parameterFields.add(declaredField);
            }
        }
    }

    private static void initTargetProductFieldIfNotDone(Operator operator, Product targetProduct) throws
            OperatorException {
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

    private static void initAnnotatedSourceProductFields(DefaultOperatorContext nodeContext) throws OperatorException {
        Field[] declaredFields = nodeContext.getOperator().getClass().getDeclaredFields();
        for (Field declaredField : declaredFields) {
            SourceProduct sourceProductAnnotation = declaredField.getAnnotation(SourceProduct.class);
            if (sourceProductAnnotation != null) {
                if (declaredField.getType().equals(Product.class)) {
                    Product sourceProduct = nodeContext.getSourceProduct(declaredField.getName());
                    if (sourceProduct == null) {
                        sourceProduct = nodeContext.getSourceProduct(sourceProductAnnotation.alias());
                    }
                    if (sourceProduct != null) {
                        validateSourceProduct(sourceProduct, declaredField, sourceProductAnnotation);
                        boolean oldState = declaredField.isAccessible();
                        try {
                            declaredField.setAccessible(true);
                            try {
                                declaredField.set(nodeContext.getOperator(), sourceProduct);
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
                    Product[] sourceProducts = nodeContext.getSourceProducts();
                    if (sourceProducts.length > 0) {
                        boolean oldState = declaredField.isAccessible();
                        try {
                            declaredField.setAccessible(true);
                            try {
                                declaredField.set(nodeContext.getOperator(), sourceProducts);
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

    private static void validateSourceProduct(Product sourceProduct, Field declaredField,
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
}
