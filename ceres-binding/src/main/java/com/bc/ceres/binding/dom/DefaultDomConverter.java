package com.bc.ceres.binding.dom;

import com.bc.ceres.binding.*;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@inheritDoc}
 */
public class DefaultDomConverter implements DomConverter {

    private Class<?> valueType;
    private ValueContainerFactory valueContainerFactory;

    public DefaultDomConverter(Class<?> valueType, ValueDescriptorFactory valueDescriptorFactory) {
        this.valueType = valueType;
        this.valueContainerFactory = new ValueContainerFactory(valueDescriptorFactory);
    }

    /**
     * {@inheritDoc}
     */
    public Class<?> getValueType() {
        return valueType;
    }

    /**
     * {@inheritDoc}
     */
    public void convertValueToDom(Object value, DomElement parentElement) {
        final ValueContainer valueContainer = valueContainerFactory.createObjectBackedValueContainer(value);
        final ValueModel[] models = valueContainer.getModels();
        for (ValueModel model : models) {
            final ValueDescriptor descriptor = model.getDescriptor();
            final String itemAlias = descriptor.getItemAlias();
            if (descriptor.getType().isArray() && itemAlias != null && !itemAlias.isEmpty()) {
                final DomElement childElement = descriptor.getItemsInlined() ? parentElement : parentElement.createChild(getElementName(model));
                final Object array = model.getValue();
                if (array != null) {
                    final int arrayLength = Array.getLength(array);
                    final Converter itemConverter = getItemConverter(descriptor);
                    for (int i = 0; i < arrayLength; i++) {
                        final Object component = Array.get(array, i);
                        final DomElement itemElement = parentElement.createChild(itemAlias);
                        convertValueToDomImpl(component, itemConverter, itemElement);
                        childElement.addChild(itemElement);
                    }
                }
                if (!descriptor.getItemsInlined()) {
                    parentElement.addChild(childElement);
                }
            } else {
                final DomElement childElement = parentElement.createChild(getElementName(model));
                final Object childValue = model.getValue();
                final Converter converter = descriptor.getConverter();
                convertValueToDomImpl(childValue, converter, childElement);
                parentElement.addChild(childElement);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException, ValidationException {
        if (value == null) {
            value = createValueInstance(getValueType());
        }
        HashMap<String, List<Object>> inlinedArrays = null;
        List<Object> inlinedArray;
        final ValueContainer valueContainer = valueContainerFactory.createObjectBackedValueContainer(value);
        final DomElement[] children = parentElement.getChildren();
        for (DomElement childElement : children) {
            final String childElementName = childElement.getName();
            // todo - COLLECTIONS
            ValueModel valueModel = valueContainer.getModel(childElementName);
            inlinedArray = null;
            if (valueModel == null) {
                final ValueModel[] valueModels = valueContainer.getModels();
                for (ValueModel model : valueModels) {
                    final String itemAlias = model.getDescriptor().getItemAlias();
                    final boolean inlined = model.getDescriptor().getItemsInlined();
                    if (childElementName.equals(itemAlias) && inlined) {
                        if (inlinedArrays == null) {
                            inlinedArrays = new HashMap<String, List<Object>>(3);
                        }
                        inlinedArray = inlinedArrays.get(getElementName(model));
                        if (inlinedArray == null) {
                            inlinedArray = new ArrayList<Object>();
                            inlinedArrays.put(getElementName(model), inlinedArray);
                        }
                        valueModel = model;
                        break;
                    }
                }
                if (valueModel == null) {
                    throw new ConversionException(String.format("Illegal element '%s'.", childElementName));
                }
            }
            final Object childValue;
            final ValueDescriptor descriptor = valueModel.getDescriptor();
            final String itemAlias = descriptor.getItemAlias();
            if (itemAlias != null && !itemAlias.isEmpty()) {
                final DomElement[] arrayElements = childElement.getChildren(itemAlias);
                final Class<?> itemType = descriptor.getType().getComponentType();
                final Converter itemConverter = getItemConverter(descriptor);
                if (inlinedArray != null) {
                    Object item = convertDomToValueImpl(childElement,
                                                        itemConverter, itemType);
                    inlinedArray.add(item);
                } else {
                    childValue = Array.newInstance(itemType, arrayElements.length);
                    for (int i = 0; i < arrayElements.length; i++) {
                        Object item = convertDomToValueImpl(arrayElements[i],
                                                            itemConverter, itemType);
                        Array.set(childValue, i, item);
                        valueModel.setValue(childValue);
                    }
                }
            } else {
                childValue = convertDomToValueImpl(childElement,
                                                   descriptor.getConverter(), descriptor.getType());
                valueModel.setValue(childValue);
            }
        }

        if (inlinedArrays != null) {
            for (Map.Entry<String, List<Object>> entry : inlinedArrays.entrySet()) {
                final String valueName = entry.getKey();
                final List<Object> valueList = entry.getValue();
                final Class<?> valueType = valueContainer.getValueDescriptor(valueName).getType();
                final Object array = Array.newInstance(valueType.getComponentType(), valueList.size());
                valueContainer.getModel(valueName).setValue(valueList.toArray((Object[]) array));
            }
        }

        return value;
    }

    private Object convertDomToValueImpl(DomElement childElement,
                                         Converter converter,
                                         Class<?> valueType) throws ConversionException, ValidationException {
        Object childValue;
        if (converter != null) {
            final String text = childElement.getValue();
            if (text != null) {
                childValue = converter.parse(text);
            } else {
                childValue = null;
            }
        } else {
            childValue = createValueInstance(valueType);
            childValue = convertDomToValue(childElement, childValue);
        }
        return childValue;
    }

    private void convertValueToDomImpl(Object value,
                                       Converter converter,
                                       DomElement element) {
        if (converter != null) {
            final String text = converter.format(value);
            if (text != null && !text.isEmpty()) {
                element.setValue(text);
            }
        } else {
            if (value != null) {
                convertValueToDom(value, element);
            }
        }
    }

    private static String getElementName(ValueModel model) {
        final String alias = model.getDescriptor().getAlias();
        if (alias != null && !alias.isEmpty()) {
            return alias;
        }
        return model.getDescriptor().getName();
    }


    private static Converter getItemConverter(ValueDescriptor descriptor) {
        Class<?> itemType = descriptor.getType().getComponentType();
        Converter itemConverter = descriptor.getItemConverter();
        if (itemConverter == null) {
            itemConverter = ConverterRegistry.getInstance().getConverter(itemType);
        }
        return itemConverter;
    }

    private Object createValueInstance(Class<?> type) {
        Object childValue;
        try {
            childValue = type.newInstance();
        } catch (Throwable t) {
            throw new RuntimeException(String.format("Failed to create instance of %s (default constructor missing?).", type.getName()), t);
        }
        return childValue;
    }
}