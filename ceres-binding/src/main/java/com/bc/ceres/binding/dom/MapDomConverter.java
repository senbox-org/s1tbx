package com.bc.ceres.binding.dom;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.bc.ceres.binding.ValidationException;

import java.util.Map;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class MapDomConverter implements DomConverter {

    private AbstractDomConverter parentConverter;

    public MapDomConverter(AbstractDomConverter parentConverter) {
        this.parentConverter = parentConverter;
    }

    @Override
    public void convertValueToDom(Object value, DomElement parentElement) {
        if (value == null) {
            return;
        }
        parentElement.setAttribute("class", value.getClass().getName());
        final Map<?, ?> map = (Map<?, ?>) value;
        for (Map.Entry<?, ?> entry : map.entrySet()) {
            final DomElement entryElement = parentElement.createChild("entry");
            final DomElement keyElement = entryElement.createChild("key");
            final DomElement valueElement = entryElement.createChild("value");
            createChild(keyElement, entry.getKey());
            createChild(valueElement, entry.getValue());

        }
    }

    private void createChild(DomElement keyElement, Object value) {

        final Class<?> keyClass = value.getClass();
        keyElement.setAttribute("class", keyClass.getName());
        ConverterRegistry converterRegistry = ConverterRegistry.getInstance();
        final Converter<Object> objectConverter = converterRegistry.getConverter(keyClass);
        if (objectConverter != null) {
            keyElement.setValue(objectConverter.format(value));
        } else {
            parentConverter.convertValueToDom(value, keyElement);
        }
    }

    @Override
    public Class<?> getValueType() {
        return Map.class;
    }

    @Override
    public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                                                                                   ValidationException {
        final Map<Object, Object> map;
        try {
            final String className = parentElement.getAttribute("class");
            final Class<?> type = Class.forName(className);
            map = (Map<Object, Object>) type.newInstance();
        } catch (Exception e) {
            throw new ConversionException(e);
        }

        final DomElement[] entryElements = parentElement.getChildren("entry");
        for (DomElement entryElement : entryElements) {
            final Object entryKey = convertChild(entryElement.getChild("key"));

            final Object entryValue = convertChild(entryElement.getChild("value"));

            map.put(entryKey, entryValue);
        }

        return map;
    }

    private Object convertChild(DomElement childElement) throws ConversionException, ValidationException {
        final String keyClassName = childElement.getAttribute("class");
        final Class<?> keyType;
        try {
            keyType = Class.forName(keyClassName);
        } catch (ClassNotFoundException e) {
            throw new ConversionException(e);
        }
        final Converter<Object> keyConverter = ConverterRegistry.getInstance().getConverter(keyType);
        if (keyConverter != null) {
            return keyConverter.parse(childElement.getValue());
        } else {
            return parentConverter.convertDomToValue(childElement, null);
        }
    }
}
