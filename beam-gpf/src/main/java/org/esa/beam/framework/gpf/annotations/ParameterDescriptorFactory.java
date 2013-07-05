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

package org.esa.beam.framework.gpf.annotations;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.bc.ceres.binding.PropertyContainer;
import com.bc.ceres.binding.PropertyDescriptor;
import com.bc.ceres.binding.PropertyDescriptorFactory;
import com.bc.ceres.binding.Validator;
import com.bc.ceres.binding.ValueRange;
import com.bc.ceres.binding.ValueSet;
import com.bc.ceres.binding.dom.DomConverter;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;
import org.esa.beam.framework.gpf.internal.RasterDataNodeValues;

import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Pattern;

public class ParameterDescriptorFactory implements PropertyDescriptorFactory {

    private Map<String, Product> sourceProductMap;

    public static PropertyContainer createMapBackedOperatorPropertyContainer(String operatorName) {
        return createMapBackedOperatorPropertyContainer(operatorName, new HashMap<String, Object>());
    }

    public static PropertyContainer createMapBackedOperatorPropertyContainer(String operatorName,
                                                                             Map<String, Object> operatorParameters) {
        return createMapBackedOperatorPropertyContainer(operatorName, operatorParameters, new ParameterDescriptorFactory());
    }

    public static PropertyContainer createMapBackedOperatorPropertyContainer(String operatorName,
                                                                             Map<String, Object> operatorParameters,
                                                                             ParameterDescriptorFactory descriptorFactory) {
        return PropertyContainer.createMapBacked(operatorParameters, getOpType(operatorName), descriptorFactory);
    }

    public ParameterDescriptorFactory() {
    }

    public ParameterDescriptorFactory(Map<String, Product> sourceProductMap) {
        this.sourceProductMap = sourceProductMap;
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
        Parameter parameter = field.getAnnotation(Parameter.class);
        if (operatorDetected && parameter == null) {
            return null;
        }
        PropertyDescriptor propertyDescriptor = new PropertyDescriptor(field.getName(), field.getType());
        if (parameter == null) {
            return propertyDescriptor;
        }
        if (parameter.validator() != Validator.class) {
            final Validator validator;
            try {
                validator = parameter.validator().newInstance();
            } catch (Throwable t) {
                throw new ConversionException("Failed to create validator.", t);
            }
            propertyDescriptor.setValidator(validator);
        }
        if (parameter.domConverter() != DomConverter.class) {
            DomConverter domConverter;
            try {
                domConverter = parameter.domConverter().newInstance();
            } catch (Throwable t) {
                throw new ConversionException("Failed to create domConverter.", t);
            }
            propertyDescriptor.setDomConverter(domConverter);
        }
        if (parameter.converter() != Converter.class) {
            Converter converter;
            try {
                converter = parameter.converter().newInstance();
            } catch (Throwable t) {
                throw new ConversionException("Failed to create converter.", t);
            }
            propertyDescriptor.setConverter(converter);
        }
        if (ParameterDescriptorFactory.isSet(parameter.label())) {
            propertyDescriptor.setDisplayName(parameter.label());
        }
        if (ParameterDescriptorFactory.isSet(parameter.alias())) {
            propertyDescriptor.setAlias(parameter.alias());
        }
        if (ParameterDescriptorFactory.isSet(parameter.itemAlias())) {
            propertyDescriptor.setItemAlias(parameter.itemAlias());
        }
        if (propertyDescriptor.getConverter() == null) {
            propertyDescriptor.setDefaultConverter();
        }
        propertyDescriptor.setItemsInlined(parameter.itemsInlined());
        propertyDescriptor.setUnit(parameter.unit());
        propertyDescriptor.setDescription(parameter.description());

        propertyDescriptor.setNotNull(parameter.notNull());
        propertyDescriptor.setNotEmpty(parameter.notEmpty());
        if (isSet(parameter.pattern())) {
            Pattern pattern = Pattern.compile(parameter.pattern());
            propertyDescriptor.setPattern(pattern);
        }
        if (isSet(parameter.interval())) {
            ValueRange valueRange = ValueRange.parseValueRange(parameter.interval());
            propertyDescriptor.setValueRange(valueRange);
        }
        if (isSet(parameter.format())) {
            propertyDescriptor.setFormat(parameter.format());
        }
        if (isSet(parameter.valueSet())) {
            Converter converter;
            if (propertyDescriptor.getType().isArray()) {
                Class<?> componentType = propertyDescriptor.getType().getComponentType();
                converter = ConverterRegistry.getInstance().getConverter(componentType);
            } else {
                converter = propertyDescriptor.getConverter();
            }
            ValueSet valueSet = ValueSet.parseValueSet(parameter.valueSet(), converter);
            propertyDescriptor.setValueSet(valueSet);
        }
        if (isSet(parameter.defaultValue())) {
            Converter converter = propertyDescriptor.getConverter();
            propertyDescriptor.setDefaultValue(converter.parse(parameter.defaultValue()));
        }
        if (parameter.rasterDataNodeType() != RasterDataNode.class) {
            Class<? extends RasterDataNode> rasterDataNodeType = parameter.rasterDataNodeType();
            propertyDescriptor.setAttribute(RasterDataNodeValues.ATTRIBUTE_NAME, rasterDataNodeType);
        }
        if (propertyDescriptor.getAttribute(RasterDataNodeValues.ATTRIBUTE_NAME) != null) {
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
        return propertyDescriptor;
    }

    private static Class<? extends Operator> getOpType(String operatorName) {
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.loadOperatorSpis();
        OperatorSpi operatorSpi = registry.getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalStateException("Operator SPI not found for operator [" + operatorName + "]");
        }
        return operatorSpi.getOperatorClass();
    }

    private static boolean isNull(Object value) {
        return value == null;
    }

    private static boolean isEmpty(String value) {
        return value.isEmpty();
    }

    private static boolean isEmpty(String[] value) {
        return value.length == 0;
    }

    private static boolean isSet(String value) {
        return !ParameterDescriptorFactory.isNull(value) && !ParameterDescriptorFactory.isEmpty(value);
    }

    private static boolean isSet(String[] value) {
        return !ParameterDescriptorFactory.isNull(value) && !ParameterDescriptorFactory.isEmpty(value);
    }
}
