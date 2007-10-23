package com.bc.ceres.binding.dom;

import com.thoughtworks.xstream.io.xml.xppdom.Xpp3Dom;
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
    public void convertValueToDom(Object value, Xpp3Dom dom) {
        final ValueContainer valueContainer = valueContainerFactory.createObjectBackedValueContainer(value);
        final ValueModel[] models = valueContainer.getModels();
        for (ValueModel model : models) {
            final ValueDefinition definition = model.getDefinition();
            final String itemAlias = definition.getItemAlias();
            if (definition.getType().isArray() && itemAlias != null && !itemAlias.isEmpty()) {
                final Object array = model.getValue();
                final int arrayLength = Array.getLength(array);
                final Converter componentConverter = getItemConverter(definition);
                final Xpp3Dom childElement = definition.isInlined() ? dom : new Xpp3Dom(getElementName(model));
                for (int i = 0; i < arrayLength; i++) {
                    final Xpp3Dom componentDom = new Xpp3Dom(itemAlias);
                    final Object component = Array.get(array, i);
                    if (componentConverter != null) {
                        componentDom.setValue(componentConverter.format(component));
                    } else {
                        convertValueToDom(component, componentDom);
                    }
                    childElement.addChild(componentDom);
                }
                if (!definition.isInlined()) {
                    dom.addChild(childElement);
                }
            } else {
                final Xpp3Dom childElement = new Xpp3Dom(getElementName(model));
                final Converter converter = definition.getConverter();
                if (converter != null) {
                    childElement.setValue(converter.format(model.getValue()));
                } else {
                    convertValueToDom(model.getValue(), childElement);
                }
                dom.addChild(childElement);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    public Object convertDomToValue(Xpp3Dom dom, Object value) throws ConversionException, ValidationException {
        if (value == null) {
            value = createValueInstance(getValueType());
        }
        HashMap<String, List<Object>> inlinedArrays = new HashMap<String, List<Object>>(10);
        List<Object> inlinedArray = null;
        final ValueContainer valueContainer = valueContainerFactory.createObjectBackedValueContainer(value);
        final Xpp3Dom[] children = dom.getChildren();
        for (Xpp3Dom childElement : children) {
            final String childElementName = childElement.getName();
            // todo - COLLECTIONS
            ValueModel valueModel = valueContainer.getModel(childElementName);
            if (valueModel == null) {
                final ValueModel[] valueModels = valueContainer.getModels();
                for (ValueModel model : valueModels) {
                    final String itemAlias = model.getDefinition().getItemAlias();
                    final boolean inlined = model.getDefinition().isInlined();
                    if (childElementName.equals(itemAlias) && inlined) {
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
                final Xpp3Dom[] arrayElements = childElement.getChildren(itemAlias);
                final Class<?> itemType = definition.getType().getComponentType();
                Converter itemConverter = getItemConverter(definition);

                if (inlinedArray != null) {
                    Object item = createItem(childElement,
                                             itemType,
                                             itemConverter
                    );
                    inlinedArray.add(item);
                } else {
                    childValue = Array.newInstance(itemType, arrayElements.length);
                    for (int i = 0; i < arrayElements.length; i++) {
                        Object item = createItem(arrayElements[i],
                                                 itemType,
                                                 itemConverter
                        );
                        Array.set(childValue, i, item);
                        valueModel.setValue(childValue);
                    }
                }
            } else {
                childValue = createItem(childElement,
                                        definition.getType(),
                                        definition.getConverter()
                );
                valueModel.setValue(childValue);
            }
        }

        for (Map.Entry<String, List<Object>> entry : inlinedArrays.entrySet()) {
            final String valueName = entry.getKey();
            final List<Object> valueList = entry.getValue();
            final Class<?> valueType = valueContainer.getValueDefinition(valueName).getType();
            final Object array = Array.newInstance(valueType.getComponentType(), valueList.size());
            valueContainer.getModel(valueName).setValue(valueList.toArray((Object[]) array));
        }

        return value;
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

    private Object createItem(Xpp3Dom childElement,
                              Class<?> type,
                              Converter converter
    ) throws ConversionException, ValidationException {
        Object childValue;
        if (converter != null) {
            childValue = converter.parse(childElement.getValue());
        } else {
            childValue = createValueInstance(type);
            childValue = convertDomToValue(childElement, childValue);
        }
        return childValue;
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