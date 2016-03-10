/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
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

package com.bc.ceres.binding;

import com.bc.ceres.binding.dom.DomConverter;
import com.bc.ceres.binding.validators.ArrayValidator;
import com.bc.ceres.binding.validators.IntervalValidator;
import com.bc.ceres.binding.validators.MultiValidator;
import com.bc.ceres.binding.validators.NotEmptyValidator;
import com.bc.ceres.binding.validators.NotNullValidator;
import com.bc.ceres.binding.validators.PatternValidator;
import com.bc.ceres.binding.validators.TypeValidator;
import com.bc.ceres.binding.validators.ValueSetValidator;
import com.bc.ceres.core.Assert;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Describes a property by its name, type and a set of optional (mutable) attributes.
 * Examples for such attributes are a {@link ValueSet}, a {@link Pattern} or
 * an {@link ValueRange}.
 * Attribute changes may be observed by adding a property (attribute) change listeners
 * to instances of this class.
 *
 * @author Norman Fomferra
 * @since 0.6
 */
public class PropertyDescriptor {

    private final String name;
    private final Class<?> type;
    private volatile Validator effectiveValidator;

    private Map<String, Object> attributes;
    private PropertyChangeSupport attributeChangeSupport;

    private PropertySetDescriptor propertySetDescriptor;

    public PropertyDescriptor(String name, Class<?> type) {
        this(name, type, new HashMap<String, Object>(8));
    }

    public PropertyDescriptor(PropertyDescriptor propertyDescriptor) {
        this(propertyDescriptor.getName(), propertyDescriptor.getType(), propertyDescriptor.attributes);
    }

    public PropertyDescriptor(String name, Class<?> type, Map<String, Object> attributes) {
        Assert.notNull(name, "name");
        Assert.notNull(type, "type");
        Assert.notNull(attributes, "attributes");
        this.name = name;
        this.type = type;
        this.attributes = new HashMap<>(attributes);
        if (type.isPrimitive()) {
            setNotNull(true);
        }
        setDisplayName(createDisplayName(name));
        if (type.isEnum() && getValueSet() == null)  {
            setValueSet(new ValueSet(type.getEnumConstants()));
        }
    }

    public String getName() {
        return name;
    }

    public Class<?> getType() {
        return type;
    }

    public String getDisplayName() {
        return (String) getAttribute("displayName");
    }

    public void setDisplayName(String displayName) {
        Assert.notNull(displayName, "displayName");
        setAttribute("displayName", displayName);
    }

    public String getAlias() {
        return (String) getAttribute("alias");
    }

    public void setAlias(String alias) {
        setAttribute("alias", alias);
    }

    public String getUnit() {
        return (String) getAttribute("unit");
    }

    public void setUnit(String unit) {
        setAttribute("unit", unit);
    }

    public String getDescription() {
        return (String) getAttribute("description");
    }

    public void setDescription(String description) {
        setAttribute("description", description);
    }

    public boolean isNotNull() {
        return getBooleanProperty("notNull");
    }

    public void setNotNull(boolean notNull) {
        setAttribute("notNull", notNull);
    }

    public boolean isNotEmpty() {
        return getBooleanProperty("notEmpty");
    }

    public void setNotEmpty(boolean notEmpty) {
        setAttribute("notEmpty", notEmpty);
    }

    public boolean isDeprecated() {
        return getBooleanProperty("deprecated");
    }

    public void setDeprecated(boolean deprecated) {
        setAttribute("deprecated", deprecated);
    }

    public boolean isTransient() {
        return getBooleanProperty("transient");
    }

    public void setTransient(boolean b) {
        setAttribute("transient", b);
    }

    public String getFormat() {
        return (String) getAttribute("format");
    }

    public void setFormat(String format) {
        setAttribute("format", format);
    }

    public ValueRange getValueRange() {
        return (ValueRange) getAttribute("valueRange");
    }

    public void setValueRange(ValueRange valueRange) {
        setAttribute("valueRange", valueRange);
    }

    public Pattern getPattern() {
        return (Pattern) getAttribute("pattern");
    }

    public Object getDefaultValue() {
        return getAttribute("defaultValue");
    }

    public void setDefaultValue(Object defaultValue) {
        setAttribute("defaultValue", defaultValue);
    }

    public void setPattern(Pattern pattern) {
        setAttribute("pattern", pattern);
    }

    public ValueSet getValueSet() {
        return (ValueSet) getAttribute("valueSet");
    }

    public void setValueSet(ValueSet valueSet) {
        setAttribute("valueSet", valueSet);
    }

    public Converter<?> getConverter() {
        return getConverter(false);
    }

    public Converter<?> getConverter(boolean notNull) {
        final Converter<?> converter = (Converter<?>) getAttribute("converter");
        if (converter == null && notNull) {
            throw new IllegalStateException("no converter defined for value '" + getName() + "'");
        }
        return converter;
    }

    public void setDefaultConverter() {
        boolean hasItemAlias = getItemAlias() != null && !getItemAlias().isEmpty();
        boolean useItemConverter = getType().isArray() && hasItemAlias;
        if (!useItemConverter) {
            setConverter(ConverterRegistry.getInstance().getConverter(getType()));
        }
    }

    public void setConverter(Converter<?> converter) {
        setAttribute("converter", converter);
    }

    public DomConverter getDomConverter() {
        return (DomConverter) getAttribute("domConverter");
    }

    public void setDomConverter(DomConverter converter) {
        setAttribute("domConverter", converter);
    }

    public Validator getValidator() {
        return (Validator) getAttribute("validator");
    }

    public void setValidator(Validator validator) {
        setAttribute("validator", validator);
    }

    Validator getEffectiveValidator() {
        if (effectiveValidator == null) {
            synchronized (this) {
                if (effectiveValidator == null) {
                    effectiveValidator = createEffectiveValidator();
                }
            }
        }
        return effectiveValidator;
    }

    public PropertySetDescriptor getPropertySetDescriptor() {
        return propertySetDescriptor;
    }

    public void setPropertySetDescriptor(PropertySetDescriptor propertySetDescriptor) {
        this.propertySetDescriptor = propertySetDescriptor;
    }

    //////////////////////////////////////////////////////////////////////////////
    // Array/List item attributes

    public String getItemAlias() {
        return (String) getAttribute("itemAlias");
    }

    public void setItemAlias(String alias) {
        setAttribute("itemAlias", alias);
    }

    /**
     * @deprecated since BEAM 5
     */
    @Deprecated
    public boolean getItemsInlined() {
        return getBooleanProperty("itemsInlined");
    }

    /**
     * @deprecated since BEAM 5
     */
    @Deprecated
    public void setItemsInlined(boolean inlined) {
        setAttribute("itemsInlined", inlined);
    }

    //////////////////////////////////////////////////////////////////////////////
    // Generic attributes

    public Object getAttribute(String name) {
        return attributes.get(name);
    }

    public void setAttribute(String name, Object value) {
        Object oldValue = getAttribute(name);
        if (value != null) {
            attributes.put(name, value);
        } else {
            attributes.remove(name);
        }
        if (!equals(oldValue, value)) {
            firePropertyChange(name, value, oldValue);
        }
    }

    public final void addAttributeChangeListener(PropertyChangeListener listener) {
        if (attributeChangeSupport == null) {
            attributeChangeSupport = new PropertyChangeSupport(this);
        }
        attributeChangeSupport.addPropertyChangeListener(listener);
    }

    public final void removeAttributeChangeListener(PropertyChangeListener listener) {
        if (attributeChangeSupport != null) {
            attributeChangeSupport.removePropertyChangeListener(listener);
        }
    }

    public PropertyChangeListener[] getAttributeChangeListeners() {
        if (attributeChangeSupport == null) {
            return new PropertyChangeListener[0];
        }
        return this.attributeChangeSupport.getPropertyChangeListeners();
    }


    /////////////////////////////////////////////////////////////////////////
    // Package Local

    void initDefaults() {
        if (getConverter() == null) {
            setDefaultConverter();
        }
        if (getDefaultValue() == null && getType().isPrimitive()) {
            setDefaultValue(Property.PRIMITIVE_ZERO_VALUES.get(getType()));
        }
    }

    /////////////////////////////////////////////////////////////////////////
    // Private

    private void firePropertyChange(String propertyName, Object newValue, Object oldValue) {
        if (attributeChangeSupport == null) {
            return;
        }
        PropertyChangeListener[] propertyChangeListeners = getAttributeChangeListeners();
        PropertyChangeEvent evt = new PropertyChangeEvent(this, propertyName, oldValue, newValue);
        for (PropertyChangeListener propertyChangeListener : propertyChangeListeners) {
            propertyChangeListener.propertyChange(evt);
        }
    }

    private static boolean equals(Object a, Object b) {
        return a == b || !(a == null || b == null) && a.equals(b);
    }

    private boolean getBooleanProperty(String name) {
        Object v = getAttribute(name);
        return v != null && (Boolean) v;
    }

    private Validator createEffectiveValidator() {
        List<Validator> validators = new ArrayList<>(3);

        if (isNotNull()) {
            validators.add(new NotNullValidator());
        }

        validators.add(new TypeValidator());

        if (isNotEmpty()) {
            validators.add(new NotEmptyValidator());
        }
        if (getPattern() != null) {
            validators.add(new PatternValidator(getPattern()));
        }
        if (getValueSet() != null) {
            Validator valueSetValidator = new ValueSetValidator(this);
            if (getType().isArray()) {
                valueSetValidator = new ArrayValidator(valueSetValidator);
            }
            validators.add(valueSetValidator);
        }
        if (getValueRange() != null) {
            validators.add(new IntervalValidator(getValueRange()));
        }
        if (getValidator() != null) {
            validators.add(getValidator());
        }
        Validator validator;
        if (validators.isEmpty()) {
            validator = null;
        } else if (validators.size() == 1) {
            validator = validators.get(0);
        } else {
            validator = new MultiValidator(validators);
        }
        return validator;
    }

    public static String getDisplayName(PropertyDescriptor propertyDescriptor) {
        String label = propertyDescriptor.getDisplayName();
        if (label != null) {
            return label;
        }
        String name = propertyDescriptor.getName().replace("_", " ");
        return createDisplayName(name);
    }

    public static String createDisplayName(String name) {
        StringBuilder sb = new StringBuilder(name.length());
        for (int i = 0; i < name.length(); i++) {
            char ch = name.charAt(i);
            if (i == 0) {
                sb.append(Character.toUpperCase(ch));
            } else if (i > 0 && i < name.length() - 1
                    && Character.isUpperCase(ch) &&
                    Character.isLowerCase(name.charAt(i + 1))) {
                sb.append(' ');
                sb.append(Character.toLowerCase(ch));
            } else if (ch == '_'){
                sb.append(' ');
            } else {
                sb.append(ch);
            }
        }
        return sb.toString();
    }
}
