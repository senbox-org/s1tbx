package org.esa.snap.core.gpf.descriptor;

import com.bc.ceres.core.Assert;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.gpf.annotations.SourceProduct;
import org.esa.snap.core.gpf.annotations.SourceProducts;
import org.esa.snap.core.gpf.annotations.TargetProduct;
import org.esa.snap.core.gpf.annotations.TargetProperty;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation helper class. Scans an operator class for field annotations and returns
 * their information as various descriptors.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
public class AnnotationOperatorDescriptorBody {
    private Class<? extends Operator> operatorClass;
    private List<ParameterDescriptor> parameterDescriptors;
    private List<SourceProductDescriptor> sourceProductDescriptors;
    private TargetProductDescriptor targetProductDescriptor;
    private List<TargetPropertyDescriptor> targetPropertyDescriptors;
    private SourceProductsDescriptor sourceProductsDescriptor;

    public AnnotationOperatorDescriptorBody(Class<? extends Operator> operatorClass) {
        Assert.notNull(operatorClass, "operatorClass");
        this.operatorClass = operatorClass;
        processAnnotations();
    }

    public Class<? extends Operator> getOperatorClass() {
        return operatorClass;
    }

    public SourceProductDescriptor[] getSourceProductDescriptors() {
        return sourceProductDescriptors != null
                ? sourceProductDescriptors.toArray(new SourceProductDescriptor[sourceProductDescriptors.size()])
                : new SourceProductDescriptor[0];
    }

    public SourceProductsDescriptor getSourceProductsDescriptor() {
        return sourceProductsDescriptor;
    }

    public TargetProductDescriptor getTargetProductDescriptor() {
        return targetProductDescriptor;
    }

    public TargetPropertyDescriptor[] getTargetPropertyDescriptors() {
        return targetPropertyDescriptors != null
                ? targetPropertyDescriptors.toArray(new TargetPropertyDescriptor[targetPropertyDescriptors.size()])
                : new TargetPropertyDescriptor[0];
    }

    public ParameterDescriptor[] getParameterDescriptors() {
        return parameterDescriptors != null
                ? parameterDescriptors.toArray(new ParameterDescriptor[parameterDescriptors.size()])
                : new ParameterDescriptor[0];
    }

    private void processAnnotations() {
        processAnnotationsRec(operatorClass);
    }

    private void processAnnotationsRec(Class<?> operatorClass) {

        final Class<?> superclass = operatorClass.getSuperclass();
        if (superclass != null && !superclass.equals(Operator.class)) {
            processAnnotationsRec(superclass);
        }

        final Field[] declaredFields = operatorClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {

            String fieldName = declaredField.getName();
            Class<?> fieldType = declaredField.getType();

            Parameter parameterAnnotation = declaredField.getAnnotation(Parameter.class);
            if (parameterAnnotation != null) {
                if (parameterDescriptors == null) {
                    parameterDescriptors = new ArrayList<>();
                }
                boolean isDeprecated = declaredField.getAnnotation(Deprecated.class) != null;
                parameterDescriptors.add(new AnnotationParameterDescriptor(fieldName, fieldType, isDeprecated, parameterAnnotation));
                continue;
            }

            SourceProduct sourceProductAnnotation = declaredField.getAnnotation(SourceProduct.class);
            if (sourceProductAnnotation != null && Product.class.isAssignableFrom(fieldType)) {
                if (sourceProductDescriptors == null) {
                    sourceProductDescriptors = new ArrayList<>();
                }
                sourceProductDescriptors.add(new AnnotationSourceProductDescriptor(fieldName, sourceProductAnnotation));
                continue;
            }

            SourceProducts sourceProductsAnnotation = declaredField.getAnnotation(SourceProducts.class);
            if (sourceProductsAnnotation != null && Product[].class.isAssignableFrom(fieldType)) {
                // Note: superclass declarations are overwritten here.
                sourceProductsDescriptor = new AnnotationSourceProductsDescriptor(fieldName, sourceProductsAnnotation);
                continue;
            }

            TargetProduct targetProductAnnotation = declaredField.getAnnotation(TargetProduct.class);
            if (targetProductAnnotation != null) {
                // Note: superclass declarations are overwritten here.
                targetProductDescriptor = new AnnotationTargetProductDescriptor(fieldName, targetProductAnnotation);
                continue;
            }

            TargetProperty targetPropertyAnnotation = declaredField.getAnnotation(TargetProperty.class);
            if (targetPropertyAnnotation != null) {
                if (targetPropertyDescriptors == null) {
                    targetPropertyDescriptors = new ArrayList<>();
                }
                targetPropertyDescriptors.add(new AnnotationTargetPropertyDescriptor(fieldName, fieldType, targetPropertyAnnotation));
            }
        }
    }
}
