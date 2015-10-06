/*
 * Copyright (C) 2012 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package org.esa.snap.core.gpf.annotations;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.bc.ceres.binding.DefaultPropertySetDescriptor;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertyDescriptorFactory;
import com.bc.ceres.binding.PropertySetDescriptor;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.dom.DomConverter;
import org.esa.snap.core.datamodel.Product;
import org.esa.snap.core.datamodel.RasterDataNode;
import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.OperatorSpiRegistry;
import org.esa.snap.core.gpf.descriptor.AnnotationParameterDescriptor;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.ParameterDescriptor;
import org.esa.snap.core.gpf.descriptor.PropertySetDescriptorFactory;
import org.esa.snap.core.gpf.internal.RasterDataNodeValues;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

import static org.esa.snap.core.util.StringUtils.*;

public class ParameterDescriptorFactory implements PropertyDescriptorFactory {

    private Map<String, Product> sourceProductMap;

    public static PropertyContainer createMapBackedOperatorPropertyContainer(String operatorName) {
        return createMapBackedOperatorPropertyContainer(operatorName, new HashMap<String, Object>());
    }

    public static PropertyContainer createMapBackedOperatorPropertyContainer(String operatorName,
                                                                             Map<String, Object> operatorParameters) {
        return createMapBackedOperatorPropertyContainer(operatorName, operatorParameters, null);
    }

    public static PropertyContainer createMapBackedOperatorPropertyContainer(String operatorName,
                                                                             Map<String, Object> operatorParameters,
                                                                             Map<String, Product> sourceProductMap) {
        OperatorSpi opSpi = getOpSpi(operatorName);
        OperatorDescriptor operatorDescriptor = opSpi.getOperatorDescriptor();
        PropertySetDescriptor propertySetDescriptor;
        try {
            propertySetDescriptor = PropertySetDescriptorFactory.createForOperator(operatorDescriptor,
                                                                                   sourceProductMap);
        } catch (ConversionException e) {
            throw new OperatorException("Could not create property container for operator '" + operatorName + "'");
        }
        return PropertyContainer.createMapBacked(operatorParameters, propertySetDescriptor);
    }

    public ParameterDescriptorFactory() {
    }

    public ParameterDescriptorFactory(Map<String, Product> sourceProductMap) {
        this.sourceProductMap = sourceProductMap;
    }

    public Map<String, Product> getSourceProductMap() {
        return sourceProductMap;
    }

    public static PropertyDescriptor convert(ParameterDescriptor parameterDescriptor, Map<String, Product> sourceProductMap) throws
                                                                                                                             ConversionException {

        PropertyDescriptor propertyDescriptor = new PropertyDescriptor(parameterDescriptor.getName(),
                                                                       parameterDescriptor.getDataType());

        Class<? extends Validator> validatorClass = parameterDescriptor.getValidatorClass();
        if (validatorClass != null) {
            final Validator validator;
            try {
                validator = validatorClass.newInstance();
            } catch (Throwable t) {
                throw new ConversionException("Failed to create validator.", t);
            }
            propertyDescriptor.setValidator(validator);
        }

        Class<? extends DomConverter> domConverterClass = parameterDescriptor.getDomConverterClass();
        if (domConverterClass != null) {
            DomConverter domConverter;
            try {
                domConverter = domConverterClass.newInstance();
            } catch (Throwable t) {
                throw new ConversionException("Failed to create DOM converter.", t);
            }
            propertyDescriptor.setDomConverter(domConverter);
        }

        Class<? extends Converter> converterClass = parameterDescriptor.getConverterClass();
        if (converterClass != null) {
            Converter converter;
            try {
                converter = converterClass.newInstance();
            } catch (Throwable t) {
                throw new ConversionException("Failed to create converter.", t);
            }
            propertyDescriptor.setConverter(converter);
        }

        if (isNotNullAndNotEmpty(parameterDescriptor.getLabel())) {
            propertyDescriptor.setDisplayName(parameterDescriptor.getLabel());
        }

        if (isNotNullAndNotEmpty(parameterDescriptor.getAlias())) {
            propertyDescriptor.setAlias(parameterDescriptor.getAlias());
        }

        if (isNotNullAndNotEmpty(parameterDescriptor.getItemAlias())) {
            propertyDescriptor.setItemAlias(parameterDescriptor.getItemAlias());
        }

        if (propertyDescriptor.getConverter() == null) {
            propertyDescriptor.setDefaultConverter();
        }

        if (isNotNullAndNotEmpty(parameterDescriptor.getUnit())) {
            propertyDescriptor.setUnit(parameterDescriptor.getUnit());
        }

        if (isNotNullAndNotEmpty(parameterDescriptor.getDescription())) {
            propertyDescriptor.setDescription(parameterDescriptor.getDescription());
        }

        propertyDescriptor.setNotNull(parameterDescriptor.isNotNull());

        propertyDescriptor.setNotEmpty(parameterDescriptor.isNotEmpty());

        propertyDescriptor.setDeprecated(parameterDescriptor.isDeprecated());

        if (isNotNullAndNotEmpty(parameterDescriptor.getPattern())) {
            Pattern pattern = Pattern.compile(parameterDescriptor.getPattern());
            propertyDescriptor.setPattern(pattern);
        }

        if (isNotNullAndNotEmpty(parameterDescriptor.getInterval())) {
            ValueRange valueRange = ValueRange.parseValueRange(parameterDescriptor.getInterval());
            propertyDescriptor.setValueRange(valueRange);
        }

        if (isNotNullAndNotEmpty(parameterDescriptor.getFormat())) {
            propertyDescriptor.setFormat(parameterDescriptor.getFormat());
        }

        if (parameterDescriptor.getValueSet().length > 0) {
            Converter converter;
            if (propertyDescriptor.getType().isArray()) {
                Class<?> componentType = propertyDescriptor.getType().getComponentType();
                converter = ConverterRegistry.getInstance().getConverter(componentType);
            } else {
                converter = propertyDescriptor.getConverter();
            }
            ValueSet valueSet = ValueSet.parseValueSet(parameterDescriptor.getValueSet(), converter);
            propertyDescriptor.setValueSet(valueSet);
        }

        if (isNotNullAndNotEmpty(parameterDescriptor.getDefaultValue())) {
            Converter converter = propertyDescriptor.getConverter();
            propertyDescriptor.setDefaultValue(converter.parse(parameterDescriptor.getDefaultValue()));
        }

        if (parameterDescriptor.getRasterDataNodeClass() != null) {
            Class<? extends RasterDataNode> rasterDataNodeType = parameterDescriptor.getRasterDataNodeClass();
            propertyDescriptor.setAttribute(RasterDataNodeValues.ATTRIBUTE_NAME, rasterDataNodeType);
        }

        if (propertyDescriptor.getAttribute(RasterDataNodeValues.ATTRIBUTE_NAME) != null) {
            @SuppressWarnings("unchecked")
            Class<? extends RasterDataNode> rasterDataNodeType = (Class<? extends RasterDataNode>) propertyDescriptor.getAttribute(
                    RasterDataNodeValues.ATTRIBUTE_NAME);
            String[] values = new String[0];
            if (sourceProductMap != null && sourceProductMap.size() > 0) {
                Product firstProduct = sourceProductMap.values().iterator().next();
                if (firstProduct != null) {
                    boolean includeEmptyValue = !propertyDescriptor.isNotNull() && !propertyDescriptor.getType().isArray();
                    values = RasterDataNodeValues.getNames(firstProduct, rasterDataNodeType, includeEmptyValue);
                }
            }
            propertyDescriptor.setValueSet(new ValueSet(values));
        }

        if (parameterDescriptor.isStructure() && !hasConverterOrDomConverter(parameterDescriptor)) {
            ParameterDescriptor[] structureMemberDescriptors = parameterDescriptor.getStructureMemberDescriptors();
            DefaultPropertySetDescriptor propertySetDescriptor = new DefaultPropertySetDescriptor();
            for (ParameterDescriptor structureMemberDescriptor : structureMemberDescriptors) {
                propertySetDescriptor.addPropertyDescriptor(convert(structureMemberDescriptor, sourceProductMap));
            }
            propertyDescriptor.setPropertySetDescriptor(propertySetDescriptor);
        }

        return propertyDescriptor;
    }

    private static boolean hasConverterOrDomConverter(ParameterDescriptor parameterDescriptor) {
        return parameterDescriptor.getDomConverterClass() != null || parameterDescriptor.getConverterClass() != null;
    }

    @Override
    public PropertyDescriptor createValueDescriptor(Field field) {
        try {
            return createValueDescriptorImpl(field);
        } catch (ConversionException e) {
            final String message = String.format("field [%s]", field.getName());
            throw new IllegalArgumentException(message, e);
        }
    }

    private PropertyDescriptor createValueDescriptorImpl(Field field) throws ConversionException {

        final boolean operatorDetected = Operator.class.isAssignableFrom(field.getDeclaringClass());

        Parameter parameterAnnotation = field.getAnnotation(Parameter.class);
        if (operatorDetected && parameterAnnotation == null) {
            return null;
        }

        boolean isDeprecated = field.getAnnotation(Deprecated.class) != null;
        if (parameterAnnotation == null) {
            PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), field.getType());
            propertyDescriptor.setDeprecated(isDeprecated);
            return propertyDescriptor;
        }

        return convert(new AnnotationParameterDescriptor(field.getName(),
                                                         field.getType(),
                                                         isDeprecated, parameterAnnotation), sourceProductMap);
    }

    private static OperatorSpi getOpSpi(String operatorName) {
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.loadOperatorSpis();
        OperatorSpi operatorSpi = registry.getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalStateException("Operator SPI not found for operator [" + operatorName + "]");
        }
        return operatorSpi;
    }
}
