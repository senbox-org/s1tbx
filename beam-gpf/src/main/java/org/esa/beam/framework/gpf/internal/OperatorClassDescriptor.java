package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.annotations.*;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;

public class OperatorClassDescriptor {

    private final Class<? extends Operator> operatorClass;
    private Map<Field, SourceProduct> sourceProductDescriptors;
    private Map<Field, Parameter> parameterDescriptors;
    private Map<Field, TargetProperty> propertyDescriptors;
    private OperatorMetadata operatorMetadata;
    private TargetProduct targetProduct;
    private SourceProducts sourceProducts;

    public OperatorClassDescriptor(Class<? extends Operator> operatorClass) {
        this.operatorClass = operatorClass;
        processAnnotations();
    }

    public final Class<? extends Operator> getOperatorClass() {
        return operatorClass;
    }

    /**
     * @return The operator's metadata,
     *         or {@code null} if the operator has no {@link org.esa.beam.framework.gpf.annotations.OperatorMetadata} annotation..
     */
    public final OperatorMetadata getOperatorMetadata() {
        return operatorMetadata;
    }

    /**
     * @return The descriptor for the operator's target product,
     *         or {@code null} if no operator field has been declared with a {@link org.esa.beam.framework.gpf.annotations.TargetProduct} annotation..
     */
    public final TargetProduct getTargetProduct() {
        return targetProduct;
    }

    /**
     * @return The descriptor for the operator's source products (array),
     *         or {@code null} if no operator field has been declared with a {@link org.esa.beam.framework.gpf.annotations.SourceProducts} annotation..
     */
    public final SourceProducts getSourceProducts() {
        return sourceProducts;
    }

    /**
     * @return The operator's source products descriptors,
     *         or {@code null} if no operator field has been declared with a {@link org.esa.beam.framework.gpf.annotations.SourceProduct} annotation..
     */
    public final Map<Field, SourceProduct> getSourceProductMap() {
        return sourceProductDescriptors;
    }

    /**
     * @return The operator's parameter descriptors,
     *         or {@code null} if no operator field has been declared with a {@link org.esa.beam.framework.gpf.annotations.Parameter} annotation.
     */
    public final Map<Field, Parameter> getParameters() {
        return parameterDescriptors;
    }
    
    /**
     * @return The operator's target properties descriptors,
     *         or {@code null} if no operator field has been declared with a {@link org.esa.beam.framework.gpf.annotations.TargetProperty} annotation.
     */
    public final Map<Field, TargetProperty> getTargetProperties() {
        return propertyDescriptors;
    }

    private void processAnnotations() {
        sourceProductDescriptors = new HashMap<Field, SourceProduct>();
        parameterDescriptors = new HashMap<Field, Parameter>();
        propertyDescriptors = new HashMap<Field, TargetProperty>();
        processAnnotationsRec(operatorClass);
    }

    private void processAnnotationsRec(Class<?> operatorClass) {
        final Class<?> superclass = operatorClass.getSuperclass();
        if (superclass != null && !superclass.equals(Operator.class)) {
            processAnnotationsRec(superclass);
        }
        OperatorMetadata operatorMetadata = operatorClass.getAnnotation(OperatorMetadata.class);
        if (operatorMetadata != null) {
            this.operatorMetadata = operatorMetadata;
        }
        final TargetProduct targetProduct = operatorClass.getAnnotation(TargetProduct.class);
        if (targetProduct != null) {
            this.targetProduct = targetProduct;
        }
        SourceProducts sourceProducts = operatorClass.getAnnotation(SourceProducts.class);
        if (sourceProducts != null) {
            this.sourceProducts = sourceProducts;
        }
        final Field[] declaredFields = operatorClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            final SourceProduct sourceProduct = declaredField.getAnnotation(SourceProduct.class);
            if (sourceProduct != null) {
                sourceProductDescriptors.put(declaredField, sourceProduct);
            }
            final Parameter parameter = declaredField.getAnnotation(Parameter.class);
            if (parameter != null) {
                parameterDescriptors.put(declaredField, parameter);
            }
            final TargetProperty targetProperty = declaredField.getAnnotation(TargetProperty.class);
            if (targetProperty != null) {
                propertyDescriptors.put(declaredField, targetProperty);
            }
        }
    }
}