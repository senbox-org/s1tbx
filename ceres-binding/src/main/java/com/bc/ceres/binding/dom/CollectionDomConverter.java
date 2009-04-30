package com.bc.ceres.binding.dom;

import com.bc.ceres.binding.ConversionException;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.ValueDescriptor;

import java.util.Collection;

/**
 * todo - add API doc
 *
 * @author Marco Peters
 * @version $ Revision $ Date $
 * @since BEAM 4.6
 */
public class CollectionDomConverter implements DomConverter {

    private AbstractDomConverter parentConverter;

    public CollectionDomConverter(AbstractDomConverter parentConverter) {
        this.parentConverter = parentConverter;
    }

    @Override
    public void convertValueToDom(Object value, DomElement parentElement) throws ConversionException {
        if (value == null) {
            return;
        }
        parentElement.setAttribute("class", value.getClass().getName());
        final Collection collection = (Collection) value;
        for (Object item : collection) {
            String itemAlias = "item";
            final DomElement itemElement = parentElement.createChild(itemAlias);
            itemElement.setAttribute("class", item.getClass().getName());
            final ValueDescriptor itemDescriptor = new ValueDescriptor(itemAlias, item.getClass());
            final DomConverter domConverter = parentConverter.getDomConverter(itemDescriptor);
            if (domConverter != null) {
                domConverter.convertValueToDom(item, itemElement);
            } else {
                parentConverter.convertValueToDom(item, itemElement);
            }
        }

    }

    @Override
    public Class<?> getValueType() {
        return Collection.class;
    }

    @Override
    public Object convertDomToValue(DomElement parentElement, Object value) throws ConversionException,
                                                                                   ValidationException {
        String itemName = "item";
        final DomElement[] listElements = parentElement.getChildren(itemName);
        try {
            final String className = parentElement.getAttribute("class");
            final Collection collection = (Collection) ((Class<?>) Class.forName(className)).newInstance();
            for (final DomElement itemElement : listElements) {
                final String itemClassName = itemElement.getAttribute("class");
                Class<?> itemType = Class.forName(itemClassName);
                final DomConverter itemDomConverter = parentConverter.getDomConverter(
                        new ValueDescriptor(itemName, itemType));
                if (itemDomConverter != null) {
                    collection.add(itemDomConverter.convertDomToValue(itemElement, itemType));
                } else {
                    collection.add(parentConverter.convertDomToValue(itemElement, null));
                }
            }
            return collection;
        } catch (Exception e) {
            throw new ConversionException(e);
        }
    }

}