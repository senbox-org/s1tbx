package org.esa.beam.framework.gpf.annotations;

import com.bc.ceres.binding.*;
import com.bc.ceres.binding.dom.DomConverter;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.HashMap;
import java.util.regex.Pattern;

public class ParameterDescriptorFactory implements ClassFieldDescriptorFactory {

    private Map<String, Product> sourceProductMap;

    public static ValueContainer createMapBackedOperatorValueContainer(String operatorName) {
        return createMapBackedOperatorValueContainer(operatorName, new HashMap<String, Object>());
    }

    public static ValueContainer createMapBackedOperatorValueContainer(String operatorName, Map<String, Object> operatorParameters) {
        return ValueContainer.createMapBacked(operatorParameters, getOpType(operatorName), new ParameterDescriptorFactory());
    }

    public ParameterDescriptorFactory() {
    }

    public ParameterDescriptorFactory(Map<String, Product> sourceProductMap) {
        this.sourceProductMap = sourceProductMap;
    }

    public ValueDescriptor createValueDescriptor(Field field) {
        try {
            return createValueDescriptorImpl(field);
        } catch (ConversionException e) {
            throw new IllegalArgumentException("field", e);
        }
    }

    private ValueDescriptor createValueDescriptorImpl(Field field) throws ConversionException {
        final boolean operatorDetected = Operator.class.isAssignableFrom(field.getDeclaringClass());
        Parameter parameter = field.getAnnotation(Parameter.class);
        if (operatorDetected && parameter == null) {
            return null;
        }
        ValueDescriptor valueDescriptor = new ValueDescriptor(field.getName(), field.getType());
        if (parameter == null) {
            return valueDescriptor;
        }
        if (parameter.validator() != Validator.class) {
            final Validator validator;
            try {
                validator = parameter.validator().newInstance();
            } catch (Throwable t) {
                throw new ConversionException("Failed to create validator.", t);
            }
            valueDescriptor.setValidator(validator);
        }
        if (parameter.domConverter() != DomConverter.class) {
            DomConverter domConverter;
            try {
                domConverter = parameter.domConverter().newInstance();
            } catch (Throwable t) {
                throw new ConversionException("Failed to create domConverter.", t);
            }
            valueDescriptor.setDomConverter(domConverter);
        }
        if (parameter.converter() != Converter.class) {
            Converter converter;
            try {
                converter = parameter.converter().newInstance();
            } catch (Throwable t) {
                throw new ConversionException("Failed to create converter.", t);
            }
            valueDescriptor.setConverter(converter);
        }
        if (ParameterDescriptorFactory.isSet(parameter.label())) {
            valueDescriptor.setDisplayName(parameter.label());
        } else {
            valueDescriptor.setDisplayName(valueDescriptor.getName());
        }
        if (ParameterDescriptorFactory.isSet(parameter.alias())) {
            valueDescriptor.setAlias(parameter.alias());
        }
        if (ParameterDescriptorFactory.isSet(parameter.itemAlias())) {
            valueDescriptor.setItemAlias(parameter.itemAlias());
        }
        if (valueDescriptor.getConverter() == null) {
            valueDescriptor.setDefaultConverter();
        }
        valueDescriptor.setItemsInlined(parameter.itemsInlined());
        valueDescriptor.setUnit(parameter.unit());
        valueDescriptor.setDescription(parameter.description());

        valueDescriptor.setNotNull(parameter.notNull());
        valueDescriptor.setNotEmpty(parameter.notEmpty());
        if (isSet(parameter.pattern())) {
            Pattern pattern = Pattern.compile(parameter.pattern());
            valueDescriptor.setPattern(pattern);
        }
        if (isSet(parameter.interval())) {
            ValueRange valueRange = ValueRange.parseValueRange(parameter.interval());
            valueDescriptor.setValueRange(valueRange);
        }
        if (isSet(parameter.format())) {
            valueDescriptor.setFormat(parameter.format());
        }
        if (isSet(parameter.valueSet())) {
            Converter converter;
            if (valueDescriptor.getType().isArray()) {
                Class<?> componentType = valueDescriptor.getType().getComponentType();
                converter = ConverterRegistry.getInstance().getConverter(componentType);
            } else {
                converter = valueDescriptor.getConverter();
            }
            ValueSet valueSet = ValueSet.parseValueSet(parameter.valueSet(), converter);
            valueDescriptor.setValueSet(valueSet);
        }
        if (isSet(parameter.defaultValue())) {
            Converter converter = valueDescriptor.getConverter();
            valueDescriptor.setDefaultValue(converter.parse(parameter.defaultValue()));
        }
        if (isSet(parameter.sourceProductId())) {
            valueDescriptor.setProperty("sourceId", parameter.sourceProductId());
            String[] values = new String[0];
            if (sourceProductMap != null) {
                String sourceProductId = parameter.sourceProductId();
                Product product = sourceProductMap.get(sourceProductId);
                if (product != null) {
                    values = product.getBandNames();
                }
            }
            valueDescriptor.setValueSet(new ValueSet(values));
        }
        return valueDescriptor;
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
