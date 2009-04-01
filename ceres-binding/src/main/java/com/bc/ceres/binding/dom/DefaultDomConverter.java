package com.bc.ceres.binding.dom;

import com.bc.ceres.binding.ClassFieldDescriptorFactory;
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
public class DefaultDomConverter implements DomConverter<Object> {

    private Class<?> valueType;
    private final ClassFieldDescriptorFactory valueDescriptorFactory;

    public DefaultDomConverter(Class<?> valueType, ClassFieldDescriptorFactory valueDescriptorFactory) {
        this.valueType = valueType;
        this.valueDescriptorFactory = valueDescriptorFactory;
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
        final ValueContainer valueContainer = ValueContainer.createObjectBacked(value, valueDescriptorFactory);
        final ValueModel[] models = valueContainer.getModels();
        for (ValueModel model : models) {
            final ValueDescriptor descriptor = model.getDescriptor();
            DomConverter<Object> domConverter = (DomConverter<Object>) descriptor.getDomConverter();
            if (domConverter != null) {
                final DomElement childElement = parentElement.createChild(getElementName(model));
                domConverter.convertValueToDom(model.getValue(), childElement);
            } else if (isNamedItemArrayType(descriptor)) {
                final DomElement childElement = descriptor.getItemsInlined() ? parentElement : parentElement.createChild(getElementName(model));
                final Object array = model.getValue();
                if (array != null) {
                    final int arrayLength = Array.getLength(array);
                    final Converter itemConverter = getItemConverter(descriptor);
                    for (int i = 0; i < arrayLength; i++) {
                        final Object component = Array.get(array, i);
                        final DomElement itemElement = childElement.createChild(descriptor.getItemAlias());
                        convertValueToDomImpl(component, itemConverter, itemElement);
                    }
                }
            } else {
                final DomElement childElement = parentElement.createChild(getElementName(model));
                final Object childValue = model.getValue();
                final Converter converter = descriptor.getConverter();
                convertValueToDomImpl(childValue, converter, childElement);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException, ValidationException {
        if (value == null) {
            value = createValueInstance(getValueType());
        }
        HashMap<String, List<Object>> inlinedArrays = null;
        List<Object> inlinedArray;
        final ValueContainer valueContainer = ValueContainer.createObjectBacked(value, valueDescriptorFactory);
        final DomElement[] children = parentElement.getChildren();
        for (DomElement childElement : children) {
            final String childElementName = childElement.getName();
            // todo - COLLECTIONS
            ValueModel valueModel = valueContainer.getModel(childElementName);
            inlinedArray = null;
            if (valueModel == null) {
                //try to find model as inlined array element
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
            DomConverter domConverter = descriptor.getDomConverter();
            if (isNamedItemArrayType(descriptor)) {
                // if and only if an itemAlias is set, we parse the array element-wise 
                final DomElement[] arrayElements = childElement.getChildren(descriptor.getItemAlias());
                final Class<?> itemType = descriptor.getType().getComponentType();
                final Converter itemConverter = getItemConverter(descriptor);
                if (inlinedArray != null) {
                    Object item = convertDomToValueImpl(childElement, domConverter,
                                                        itemConverter, itemType);
                    inlinedArray.add(item);
                } else {
                    childValue = Array.newInstance(itemType, arrayElements.length);
                    for (int i = 0; i < arrayElements.length; i++) {
                        Object item = convertDomToValueImpl(arrayElements[i], domConverter,
                                                            itemConverter, itemType);
                        Array.set(childValue, i, item);
                    }
                    valueModel.setValue(childValue);
                }
            } else {
                childValue = convertDomToValueImpl(childElement,
                                                   domConverter,
                                                   descriptor.getConverter(),
                                                   descriptor.getType());
                valueModel.setValue(childValue);
            }
        }

        if (inlinedArrays != null) {
            for (Map.Entry<String, List<Object>> entry : inlinedArrays.entrySet()) {
                final String valueName = entry.getKey();
                final List<Object> valueList = entry.getValue();
                final Class<?> valueType = valueContainer.getDescriptor(valueName).getType();
                final Object array = Array.newInstance(valueType.getComponentType(), valueList.size());
                valueContainer.getModel(valueName).setValue(valueList.toArray((Object[]) array));
            }
        }

        return value;
    }

    private boolean isNamedItemArrayType(ValueDescriptor descriptor) {
        return descriptor.getType().isArray() && descriptor.getItemAlias() != null && !descriptor.getItemAlias().isEmpty();
    }

    private Object convertDomToValueImpl(DomElement childElement,
                                         DomConverter<?> domConverter,
                                         Converter converter,
                                         Class<?> valueType) throws ConversionException, ValidationException {
        final Object childValue;
        if (domConverter != null) {
            childValue = domConverter.convertDomToValue(childElement, null);
        } else if (converter != null) {
            final String text = childElement.getValue();
            if (text != null) {
                childValue = converter.parse(text);
            } else {
                childValue = null;
            }
        } else {
            DefaultDomConverter childConverter = new DefaultDomConverter(valueType, valueDescriptorFactory);
            try {
                childValue = childConverter.convertDomToValue(childElement, null);
            } catch (ValidationException e) {
                throw new ValidationException("In a member of '" + childElement.getName() + "': " + e.getMessage(), e);
            } catch (ConversionException e) {
                throw new ConversionException("In a member of '" + childElement.getName() + "': " + e.getMessage(), e);
            }
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
        Converter itemConverter = descriptor.getConverter();
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