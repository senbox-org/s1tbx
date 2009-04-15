package com.bc.ceres.binding.dom;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.Converter;
import com.bc.ceres.binding.ConverterRegistry;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueContainer;
import com.bc.ceres.binding.ValueDescriptor;
import com.bc.ceres.binding.ValueModel;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * {@inheritDoc}
 */
public abstract class AbstractDomConverter implements DomConverter {

    private final Class<?> valueType;

    protected AbstractDomConverter(Class<?> valueType) {
        this.valueType = valueType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Class<?> getValueType() {
        return valueType;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void convertValueToDom(Object value, DomElement parentElement) {
        final ValueContainer valueContainer = getValueContainer(value);
        final ValueModel[] models = valueContainer.getModels();
        for (ValueModel model : models) {
            final ValueDescriptor descriptor = model.getDescriptor();
            final DomConverter domConverter = descriptor.getDomConverter();
            if (domConverter != null) {
                final DomElement childElement = parentElement.createChild(getNameOrAlias(model));
                domConverter.convertValueToDom(model.getValue(), childElement);
            } else if (isArrayTypeWithNamedItems(descriptor)) {
                final DomElement childElement;
                if (descriptor.getItemsInlined()) {
                    childElement = parentElement;
                } else {
                    childElement = parentElement.createChild(getNameOrAlias(model));
                }
                final Object array = model.getValue();
                if (array != null) {
                    final int arrayLength = Array.getLength(array);
                    final Converter<?> itemConverter = getItemConverter(descriptor);
                    for (int i = 0; i < arrayLength; i++) {
                        final Object component = Array.get(array, i);
                        final DomElement itemElement = childElement.createChild(descriptor.getItemAlias());
                        convertValueToDomImpl(component, itemConverter, itemElement);
                    }
                }
            } else {
                final DomElement childElement = parentElement.createChild(getNameOrAlias(model));
                final Object childValue = model.getValue();
                Converter<?> converter = descriptor.getConverter();
                if (converter == null) {
                    converter = ConverterRegistry.getInstance().getConverter(descriptor.getType());
                }
                convertValueToDomImpl(childValue, converter, childElement);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                                                                                   ValidationException {
        if (value == null) {
            value = createValueInstance(getValueType());
        }
        final Map<String, List<Object>> inlinedArrays = new HashMap<String, List<Object>>();
        final ValueContainer valueContainer = getValueContainer(value);

        for (final DomElement child : parentElement.getChildren()) {
            final String childName = child.getName();
            // todo - convert Java collections (nf - 02.04.2009)
            ValueModel valueModel = valueContainer.getModel(childName);
            List<Object> inlinedArray = null;

            if (valueModel == null) {
                // try to find model as inlined array element
                final ValueModel[] valueModels = valueContainer.getModels();
                for (final ValueModel model : valueModels) {
                    final boolean inlined = model.getDescriptor().getItemsInlined();
                    if (inlined) {
                        final String itemAlias = model.getDescriptor().getItemAlias();
                        if (childName.equals(itemAlias)) {
                            final String name = getNameOrAlias(model);
                            inlinedArray = inlinedArrays.get(name);
                            if (inlinedArray == null) {
                                inlinedArray = new ArrayList<Object>();
                                inlinedArrays.put(name, inlinedArray);
                            }
                            valueModel = model;
                            break;
                        }
                    }
                }
                if (valueModel == null) {
                    throw new ConversionException(String.format("Illegal element '%s'.", childName));
                }
            }

            final Object childValue;
            final ValueDescriptor descriptor = valueModel.getDescriptor();
            final DomConverter domConverter = descriptor.getDomConverter();
            if (domConverter != null) {
                childValue = domConverter.convertDomToValue(child, null);
                valueModel.setValue(childValue);
            } else {
                if (isArrayTypeWithNamedItems(descriptor)) {
                    // if and only if an itemAlias is set, we parse the array element-wise
                    final DomElement[] arrayElements = child.getChildren(descriptor.getItemAlias());
                    final Class<?> itemType = descriptor.getType().getComponentType();
                    final Converter<?> itemConverter = getItemConverter(descriptor);
                    if (inlinedArray != null) {
                        Object item = convertDomToValueImpl(child, itemConverter, itemType);
                        inlinedArray.add(item);
                    } else {
                        childValue = Array.newInstance(itemType, arrayElements.length);
                        for (int i = 0; i < arrayElements.length; i++) {
                            Object item = convertDomToValueImpl(arrayElements[i], itemConverter, itemType);
                            Array.set(childValue, i, item);
                        }
                        valueModel.setValue(childValue);
                    }
                } else {
                    childValue = convertDomToValueImpl(child,
                                                       descriptor.getConverter(),
                                                       descriptor.getType());
                    valueModel.setValue(childValue);
                }
            }
        }

        if (!inlinedArrays.isEmpty()) {
            for (final Map.Entry<String, List<Object>> entry : inlinedArrays.entrySet()) {
                final String valueName = entry.getKey();
                final List<Object> valueList = entry.getValue();
                final Class<?> componentType = valueContainer.getDescriptor(valueName).getType().getComponentType();
                final Object array = Array.newInstance(componentType, valueList.size());

                valueContainer.getModel(valueName).setValue(valueList.toArray((Object[]) array));
            }
        }

        return value;
    }

    /**
     * Gets an appropriate {@link com.bc.ceres.binding.ValueContainer ValueContainer} for the given value.
     *
     * @param value The value.
     *
     * @return The value container.
     */
    protected abstract ValueContainer getValueContainer(Object value);

    /**
     * Creates a {@link DomConverter} for a child element of a given value type.
     * This factory method is called by the {@link #convertValueToDom(Object, DomElement)} method
     * in order to convert a value with no value {@link com.bc.ceres.binding.Converter Converter}
     * assigned to it.
     *
     * @param element   The element which requires the converter.
     * @param valueType The type of the value to be converted.
     *
     * @return The converter.
     */
    protected abstract DomConverter createChildConverter(DomElement element, Class<?> valueType);

    private void convertValueToDomImpl(Object value, Converter converter, DomElement element) {
        if (converter != null) {
            final String text = converter.format(value);
            if (text != null && !text.isEmpty()) {
                element.setValue(text);
            }
        } else {
            if (value != null) {
                final ValueContainer valueContainer = getValueContainer(value);
                final ValueModel[] models = valueContainer.getModels();
                for (ValueModel model : models) {
                    final ValueDescriptor descriptor = model.getDescriptor();
                    final DomConverter domConverter = descriptor.getDomConverter();
                    if (domConverter != null) {
                        final DomElement childElement = element.createChild(getNameOrAlias(model));
                        domConverter.convertValueToDom(model.getValue(), childElement);
                    } else if (isArrayTypeWithNamedItems(descriptor)) {
                        final DomElement childElement;
                        if (descriptor.getItemsInlined()) {
                            childElement = element;
                        } else {
                            childElement = element.createChild(getNameOrAlias(model));
                        }
                        final Object array = model.getValue();
                        if (array != null) {
                            final int arrayLength = Array.getLength(array);
                            final Converter<?> itemConverter = getItemConverter(descriptor);
                            for (int i = 0; i < arrayLength; i++) {
                                final Object component = Array.get(array, i);
                                final DomElement itemElement = childElement.createChild(descriptor.getItemAlias());
                                convertValueToDomImpl(component, itemConverter, itemElement);
                            }
                        }
                    } else {
                        final DomElement childElement = element.createChild(getNameOrAlias(model));
                        final Object childValue = model.getValue();
                        final Converter<?> converter1 = descriptor.getConverter();
                        convertValueToDomImpl(childValue, converter1, childElement);
                    }
                }
            }
        }
    }

    private Object convertDomToValueImpl(DomElement domElement,
                                         Converter<?> converter,
                                         Class<?> valueType) throws ConversionException, ValidationException {
        final Object childValue;

        if (converter == null) {
            converter = ConverterRegistry.getInstance().getConverter(valueType);
        }
        if (converter != null) {
            final String text = domElement.getValue();
            if (text != null) {
                try {
                    childValue = converter.parse(text);
                } catch (ConversionException e) {
                    throw new ConversionException(
                            "In a member of '" + domElement.getName() + "': " + e.getMessage(), e);
                }
            } else {
                childValue = null;
            }
        } else {
            final DomConverter domConverter = createChildConverter(domElement, valueType);
            try {
                childValue = domConverter.convertDomToValue(domElement, null);
            } catch (ValidationException e) {
                throw new ValidationException(
                        "In a member of '" + domElement.getName() + "': " + e.getMessage(), e);
            } catch (ConversionException e) {
                throw new ConversionException(
                        "In a member of '" + domElement.getName() + "': " + e.getMessage(), e);
            }
        }

        return childValue;
    }

    private static String getNameOrAlias(ValueModel model) {
        final String alias = model.getDescriptor().getAlias();
        if (alias != null && !alias.isEmpty()) {
            return alias;
        }
        return model.getDescriptor().getName();
    }

    private static Converter<?> getItemConverter(ValueDescriptor descriptor) {
        Class<?> itemType = descriptor.getType().getComponentType();
        Converter<?> itemConverter = descriptor.getConverter();
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
            throw new RuntimeException(
                    String.format("Failed to create instance of %s (default constructor missing?).", type.getName()),
                    t);
        }
        return childValue;
    }

    private boolean isArrayTypeWithNamedItems(ValueDescriptor descriptor) {
        return descriptor.getType().isArray() && descriptor.getItemAlias() != null && !descriptor.getItemAlias().isEmpty();
    }
}