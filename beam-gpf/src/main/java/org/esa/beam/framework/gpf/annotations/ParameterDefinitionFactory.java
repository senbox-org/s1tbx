package org.esa.beam.framework.gpf.annotations;

import com.bc.ceres.binding.*;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.OperatorSpiRegistry;

import java.lang.reflect.Field;
import java.util.Map;
import java.util.regex.Pattern;

public class ParameterDefinitionFactory implements ValueDefinitionFactory {

    public ParameterDefinitionFactory() {
    }

    public ValueDefinition createValueDefinition(Field field) {
        ValueDefinition valueDefinition = null;
        Parameter parameter = field.getAnnotation(Parameter.class);
        if (parameter != null) {
            try {
                valueDefinition = createValueDefinition(field, parameter);
            } catch (ConversionException e) {
                throw new IllegalArgumentException("type", e);
            }
        }
        return valueDefinition;
    }

    public static ValueDefinition createValueDefinition(Field field, Parameter parameter) throws ConversionException {
        ValueDefinition valueDefinition = new ValueDefinition(field.getName(), field.getType());
        if (parameter.validator() != Validator.class) {
            final Validator validator;
            try {
                validator = parameter.validator().newInstance();
            } catch (Throwable t) {
                throw new ConversionException("Failed to create validator.", t);
            }
            valueDefinition.setValidator(validator);
        }
        if (parameter.converter() != Converter.class) {
            Converter converter;
            try {
                converter = parameter.converter().newInstance();
            } catch (Throwable t) {
                throw new ConversionException("Failed to create converter.", t);
            }
            valueDefinition.setConverter(converter);
        }
        if (valueDefinition.getConverter() == null) {
            valueDefinition.setConverter(ConverterRegistry.getInstance().getConverter(valueDefinition.getType()));
        }
        if (ParameterDefinitionFactory.isSet(parameter.label())) {
            valueDefinition.setDisplayName(parameter.label());
        } else {
            valueDefinition.setDisplayName(field.getName());
        }
        if (ParameterDefinitionFactory.isSet(parameter.alias())) {
            valueDefinition.setProperty("alias", parameter.alias());
        }
        valueDefinition.setUnit(parameter.unit());
        valueDefinition.setDescription(parameter.description());

        valueDefinition.setNotNull(parameter.notNull());
        valueDefinition.setNotEmpty(parameter.notEmpty());
        if (isSet(parameter.pattern())) {
            Pattern pattern = Pattern.compile(parameter.pattern());
            valueDefinition.setPattern(pattern);
        }
        if (isSet(parameter.interval())) {
            Interval interval = Interval.parseInterval(parameter.interval());
            valueDefinition.setInterval(interval);
        }
        if (isSet(parameter.format())) {
            valueDefinition.setFormat(parameter.format());
        }
        if (isSet(parameter.valueSet())) {
            Converter converter = valueDefinition.getConverter();
            ValueSet valueSet = ValueSet.parseValueSet(parameter.valueSet(), converter);
            valueDefinition.setValueSet(valueSet);
        }
        if (isSet(parameter.defaultValue())) {
            Converter converter = valueDefinition.getConverter();
            valueDefinition.setDefaultValue(converter.parse(parameter.defaultValue()));
        }
        return valueDefinition;
    }

    public static ValueContainer createMapBackedOperatorValueContainer(String operatorName, Map<String, Object> operatorParameters) {
        final OperatorSpiRegistry registry = GPF.getDefaultInstance().getOperatorSpiRegistry();
        registry.loadOperatorSpis();
        OperatorSpi operatorSpi = registry.getOperatorSpi(operatorName);
        if (operatorSpi == null) {
            throw new IllegalStateException("Operator SPI not found for operator [" + operatorName + "]");
        }
        Class<? extends Operator> operatorClass = operatorSpi.getOperatorClass();
        ValueContainerFactory factory = new ValueContainerFactory(new ParameterDefinitionFactory());
        return factory.createMapBackedValueContainer(operatorClass, operatorParameters);
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
        return !ParameterDefinitionFactory.isNull(value) && !ParameterDefinitionFactory.isEmpty(value);
    }

    private static boolean isSet(String[] value) {
        return !ParameterDefinitionFactory.isNull(value) && !ParameterDefinitionFactory.isEmpty(value);
    }

}
