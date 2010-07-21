/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.beam.framework.gpf.internal;

import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProduct;
import org.esa.beam.framework.gpf.annotations.TargetProperty;

import java.lang.reflect.Field;
import java.util.LinkedHashMap;
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
        sourceProductDescriptors = new LinkedHashMap<Field, SourceProduct>();
        parameterDescriptors = new LinkedHashMap<Field, Parameter>();
        propertyDescriptors = new LinkedHashMap<Field, TargetProperty>();
        processAnnotationsRec(operatorClass);
    }

    private void processAnnotationsRec(Class<?> operatorClass) {
        final Class<?> superclass = operatorClass.getSuperclass();
        if (superclass != null && !superclass.equals(Operator.class)) {
            processAnnotationsRec(superclass);
        }
        OperatorMetadata operatorMetadataAnnot = operatorClass.getAnnotation(OperatorMetadata.class);
        if (operatorMetadataAnnot != null) {
            this.operatorMetadata = operatorMetadataAnnot;
        }
        final Field[] declaredFields = operatorClass.getDeclaredFields();
        for (Field declaredField : declaredFields) {
            final Parameter parameterAnnot = declaredField.getAnnotation(Parameter.class);
            if (parameterAnnot != null) {
                parameterDescriptors.put(declaredField, parameterAnnot);
                continue;
            }
            final SourceProduct sourceProductAnnot = declaredField.getAnnotation(SourceProduct.class);
            if (sourceProductAnnot != null) {
                sourceProductDescriptors.put(declaredField, sourceProductAnnot);
                continue;
            }
            SourceProducts sourceProductsAnnot = declaredField.getAnnotation(SourceProducts.class);
            if (sourceProductsAnnot != null) {
                this.sourceProducts = sourceProductsAnnot;
                continue;
            }
            final TargetProduct targetProductAnnot = declaredField.getAnnotation(TargetProduct.class);
            if (targetProductAnnot != null) {
                this.targetProduct = targetProductAnnot;
                continue;
            }
            final TargetProperty targetPropertyAnnot = declaredField.getAnnotation(TargetProperty.class);
            if (targetPropertyAnnot != null) {
                propertyDescriptors.put(declaredField, targetPropertyAnnot);
            }
        }
    }
}