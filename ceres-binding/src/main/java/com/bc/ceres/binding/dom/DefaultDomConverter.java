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

    public DefaultDomConverter(Class<?> valueType, ValueDefinitionFactory valueDefinitionFactory) {
        this.valueType = valueType;
        this.valueContainerFactory = new ValueContainerFactory(valueDefinitionFactory);
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
            final ValueDefinition definition = model.getDefinition();
            final String itemAlias = definition.getItemAlias();
            if (definition.getType().isArray() && itemAlias != null && !itemAlias.isEmpty()) {
                final Object array = model.getValue();
                final int arrayLength = Array.getLength(array);
                final Converter itemConverter = getItemConverter(definition);
                final DomElement childElement = definition.getItemsInlined() ? parentElement : parentElement.createChild(getElementName(model));
                for (int i = 0; i < arrayLength; i++) {
                    final Object component = Array.get(array, i);
                    final DomElement itemElement = parentElement.createChild(itemAlias);
                    convertValueToDomImpl(component, itemConverter, itemElement);
                    childElement.addChild(itemElement);
                }
                if (!definition.getItemsInlined()) {
                    parentElement.addChild(childElement);
                }
            } else {
                final DomElement childElement = parentElement.createChild(getElementName(model));
                final Object childValue = model.getValue();
                final Converter converter = definition.getConverter();
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
                    final String itemAlias = model.getDefinition().getItemAlias();
                    final boolean inlined = model.getDefinition().getItemsInlined();
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
            final ValueDefinition definition = valueModel.getDefinition();
            final String itemAlias = definition.getItemAlias();
            if (itemAlias != null && !itemAlias.isEmpty()) {
                final DomElement[] arrayElements = childElement.getChildren(itemAlias);
                final Class<?> itemType = definition.getType().getComponentType();
                final Converter itemConverter = getItemConverter(definition);
                if (inlinedArray != null) {
                    Object item = convertDomToValueImpl(childElement,
                                                        itemConverter, itemType
                    );
                    inlinedArray.add(item);
                } else {
                    childValue = Array.newInstance(itemType, arrayElements.length);
                    for (int i = 0; i < arrayElements.length; i++) {
                        Object item = convertDomToValueImpl(arrayElements[i],
                                                            itemConverter, itemType
                        );
                        Array.set(childValue, i, item);
                        valueModel.setValue(childValue);
                    }
                }
            } else {
                childValue = convertDomToValueImpl(childElement,
                                                   definition.getConverter(), definition.getType()
                );
                valueModel.setValue(childValue);
            }
        }

        if (inlinedArrays != null) {
            for (Map.Entry<String, List<Object>> entry : inlinedArrays.entrySet()) {
                final String valueName = entry.getKey();
                final List<Object> valueList = entry.getValue();
                final Class<?> valueType = valueContainer.getValueDefinition(valueName).getType();
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
        final String alias = model.getDefinition().getAlias();
        if (alias != null && !alias.isEmpty()) {
            return alias;
        }
        return model.getDefinition().getName();
    }


    private static Converter getItemConverter(ValueDefinition definition) {
        Class<?> itemType = definition.getType().getComponentType();
        Converter itemConverter = definition.getItemConverter();
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