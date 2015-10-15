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
package org.esa.snap.core.param;

import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.PropertyMap;

import javax.swing.event.EventListenerList;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

/**
 * A <code>Parameter</code> is a named item which has a value of type <code>Object</code>.
 * <p>Every parameter has an associated <code>ParamProperties</code> reference assigned to it which stores the
 * properties such as type, value range and more. A single <code>ParamProperties</code> instance can be shared over
 * multiple <code>Parameter</code> instances.
 * <p>Parameters also have an <code>ParamEditor</code> to let a user modify the value in a GUI and a
 * <code>ParamValidator</code> to parse, format and validate it.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$  $Date$
 * @see ParamProperties
 * @see ParamEditor
 * @see ParamValidator
 * @deprecated since BEAM 4.11, use the {@link com.bc.ceres.binding Ceres Binding API} instead
 */
@Deprecated
public class Parameter {

    /**
     * The parameter's name.
     */
    private final String _name;

    private ParamProperties _properties;

    /**
     * The parameter's current value
     */
    private Object _value;

    /**
     * The list of parameter change listeners.
     */
    private EventListenerList _listenerList;

    /**
     * Listens for changes in this parameter's properties.
     */
    private PropertyChangeListener _propertyChangeListener;

    private ParamEditor _editor;

    /**
     * The enabled state of parameters swing
     */
    private boolean _uiEnabled;

    private ParamValidator _validator;

    public Parameter(String name) {
        this(name, null, null);
    }

    public Parameter(String name, Object value) {
        this(name, value, null);
    }

    public Parameter(String name, ParamProperties properties) {
        this(name, null, properties, true);
    }

    public Parameter(String name, Object value, ParamProperties properties) {
        this(name, value, properties, false);
    }

    private Parameter(String name, Object value, ParamProperties properties, boolean useDefaultValue) {
        Guardian.assertNotNullOrEmpty("name", name);

        _name = name;

        if (value == null && properties != null && useDefaultValue) {
            _value = properties.getDefaultValue();
        } else {
            _value = value;
        }

        if (properties != null) {
            _properties = properties;
        } else {
            _properties = new ParamProperties();
        }

        _uiEnabled = true;

        _propertyChangeListener = createParamPropertiesPropertyChangeListener();
        _properties.addPropertyChangeListener(_propertyChangeListener);

        if (_properties.getValueType() == null && _value != null) {
            _properties.setValueType(_value.getClass());
        }

        if (_properties.getDefaultValue() == null && _value != null) {
            _properties.setDefaultValue(_value);
        }
    }

    /**
     * Returns the name of this parameter
     */
    public String getName() {
        return _name;
    }

    /**
     * Returns the value type of this parameter
     */
    public Class getType() {
        return getProperties().getValueType();
    }

    /**
     * Returns the current value of this parameter.
     */
    public Object getValue() {
        return _value;
    }

    /**
     * Sets the given value as text. The method first validates the given value before sets it to this parameter's
     * value.
     *
     * @param newValue the new value to be set
     */
    public void setValue(Object newValue) throws ParamValidateException {
        if (equalsValue(newValue)) {
            return;
        }
        validateValue(newValue);
        Object oldValue = getValue();
        _value = newValue;
        fireParamValueChanged(this, oldValue);
        updateUI();
    }

    /**
     * Sets the given value as text. The method first validates the given value before sets it to this parameter's
     * value.
     * <p>Any <code>ParamValidateException</code> occurring while performing the validation are delegated to the given
     * exception handler (if any).
     *
     * @param newValue the new value to be set
     * @param handler  an exception handler
     * @return <code>true</code> if this parameter's value has been changed
     * @throws IllegalArgumentException if the value could not be set and <code>handler</code> is null or the handler
     *                                  did not handle the error
     */
    public boolean setValue(Object newValue, ParamExceptionHandler handler) {
        try {
            setValue(newValue);
            return true;
        } catch (ParamValidateException e) {
            handleParamException(handler, e);
            return false;
        }
    }

    /**
     * Sets the value of this parameter to the default value specified in this parameter's attributes.
     */
    public void setDefaultValue() {
        Object defaultValue = getProperties().getDefaultValue();
        if (!isLegalValue(defaultValue)
                && defaultValue instanceof String) {
            setValueAsText((String) defaultValue, null); // Force IllegalArgumentException on error
        } else {
            setValue(defaultValue, null); // Force IllegalArgumentException on error
        }
    }

    /**
     * Returns the value type of this parameter. If the value type has not been specified in this parameter's
     * attributes, the string type (<code>java.lang.String.class</code>) is returned.
     *
     * @return this parameter's value type, never <code>null</code>
     */
    public Class getValueType() {
        Class valueType = getProperties().getValueType();
        return valueType != null ? valueType : java.lang.String.class;
    }

    /**
     * Tests if this parameter's value type is a, is derived from or implements the given value type.
     * <p>
     * The method should be used instead of the <code>instanceof</code> operator in order to determine whether this
     * parameter's value is compatible with a given class (or interface). That is, if the parameter's value is
     * <code>null</code>, the <code>instanceof</code> operator would simply return <code>false</code>.
     *
     * @param valueType the value type
     * @return <code>true</code>, if this parameter's value type is a, is derived from or implements the given value
     *         type
     */
    public boolean isTypeOf(Class valueType) {
        return valueType.isAssignableFrom(getValueType());
    }

    /**
     * Tests if the given value is a legal value for this parameter.
     *
     * @return <code>true</code> if so
     */
    public boolean isLegalValue(Object value) {
        try {
            validateValue(value);
            return true;
        } catch (ParamValidateException e) {
            return false;
        }
    }

    /**
     * Returns additional parameter information. Parameter informations are provided as a set of attributes. The value
     * returned is guaranteed to be always different from <code>null</code>.
     *
     * @return the parameter information, never <code>null</code>
     */
    public ParamProperties getProperties() {
        return _properties;
    }

    /**
     * Sets the additional parameter information. Parameter informations are provided as a set of attributes.
     *
     * @param properties the additional parameter information, must not be <code>null</code>
     */
    public void setProperties(ParamProperties properties) {
        Guardian.assertNotNull("properties", properties);
        _properties.removePropertyChangeListener(_propertyChangeListener);
        _properties = properties;
        _properties.addPropertyChangeListener(_propertyChangeListener);
        if (_editor != null) {
            _editor.updateUI();
            _editor.reconfigureUI();
        }
    }

    /**
     * Sets the value given as text.
     * <p>The method first parses the given text then validates the resulting object and finally sets this parameter to
     * the validated object.
     *
     * @param textValue the text value to be parsed, validated and set
     * @throws ParamParseException    if the given text value could not be converted to the required parameter type
     * @throws ParamValidateException if the parsed value is not valid
     */
    public void setValueAsText(String textValue) throws ParamParseException,
            ParamValidateException {
        Object value = parseValue(textValue);
        setValue(value);
    }

    /**
     * Sets the value given as text.
     * <p>The method first parses the given text then validates the resulting object and finally sets this parameter to
     * the validated object.
     * <p>Any <code>ParamParseException</code> or <code>ParamValidateException</code> occurring during parsing and
     * validation are delegated to the given exception handler (if any).
     *
     * @param textValue the text value to be parsed, validated and set
     * @param handler   an exception handler
     * @return <code>true</code> if this parameter's value has been changed
     * @throws IllegalArgumentException if the value could not be set and <code>handler</code> is null or the handler
     *                                  did not handle the error
     */
    public boolean setValueAsText(String textValue, ParamExceptionHandler handler) {
        try {
            setValueAsText(textValue);
            return true;
        } catch (ParamException e) {
            handleParamException(handler, e);
            return false;
        }
    }

    /**
     * Returns the value of this parameter as string or an empty string.
     *
     * @return the value of this parameter as string or an empty string.
     */
    public String getValueAsText() {
        try {
            return formatValue(getValue());
        } catch (ParamFormatException e) {
            return ""; // @todo 1 nf/nf - is this OK? Do we really need ParamFormatException ?
        }
    }

    public Object parseValue(String text) throws ParamParseException {
        return getValidator().parse(this, text);
    }

    public String formatValue(Object value) throws ParamFormatException {
        return getValidator().format(this, value);
    }

    public void validateValue(Object value) throws ParamValidateException {
        getValidator().validate(this, value);
    }

    public boolean equalsValue(Object value) {
        return getValidator().equalValues(this, getValue(), value);
    }

    public void setPropertyValues(PropertyMap propertyMap) {
        getProperties().setPropertyValues(getName(), propertyMap);
    }

    public ParamValidator getValidator() {
        if (_validator == null) {
            _validator = createValidator();
        }
        return _validator;
    }

    protected ParamValidator createValidator() {
        return getProperties().createValidator();
    }

    public ParamEditor getEditor() {
        if (_editor == null) {
            _editor = createEditor();
        }
        return _editor;
    }

    public boolean isUIEnabled() {
        if (_editor != null) {
            return _editor.isEnabled();
        }
        return false;
    }

    public void setUIEnabled(boolean enabled) {
        _uiEnabled = enabled;
        if (_editor != null) {
            _editor.setEnabled(enabled);
        }
    }

    public void updateUI() {
        if (_editor != null) {
            _editor.updateUI();
        }
    }

    /**
     * @return an appropriate _editor for this parameter _properties, can be <code>null</code> if an instantiation error
     *         occurs
     */
    protected ParamEditor createEditor() {
        ParamEditor editor = ParamEditorFactory.createParamEditor(this);
        if (editor != null) {
            editor.setEnabled(_uiEnabled);
        }
        return editor;
    }

    public void setValueSet(String[] valueSet) {
        getProperties().setValueSet(valueSet);
        if (_editor != null) {
            _editor.reconfigureUI();
        }
    }

    /**
     * Adds a parameter change listener to this parameter.
     *
     * @param listener the listener to be added
     */
    public void addParamChangeListener(ParamChangeListener listener) {
        if (listener != null) {
            if (_listenerList == null) {
                _listenerList = new EventListenerList();
            }
            _listenerList.add(ParamChangeListener.class, listener);
        }
    }

    /**
     * Removes the parameter change listener from this parameter.
     *
     * @param listener the listener to be removed
     */
    public void removeParamChangeListener(ParamChangeListener listener) {
        if (listener != null && _listenerList != null) {
            _listenerList.add(ParamChangeListener.class, listener);
        }
    }

    protected void fireParamValueChanged(Parameter parameter, Object oldValue) {

//        if (Debug.isEnabled()) {
//            StringBuffer sb = new StringBuffer();
//            sb.append("value of parameter '");
//            sb.append(getName());
//            sb.append("' changed from ");
//            appendParamValue(sb, oldValue);
//            sb.append(" to ");
//            appendParamValue(sb, parameter.getValue());
//            Debug.trace(sb.toString());
//        }

        if (_listenerList == null) {
            return;
        }
        ParamChangeEvent event = null;
        // Guaranteed to return a non-null array
        Object[] listeners = _listenerList.getListenerList();
        // Process the listeners last to first, notifying
        // those that are interested in this event
        for (int i = listeners.length - 2; i >= 0; i -= 2) {
            if (listeners[i] == ParamChangeListener.class) {
                // Lazily create the event:
                if (event == null) {
                    event = new ParamChangeEvent(this, parameter, oldValue);
                }
                ((ParamChangeListener) listeners[i + 1]).parameterValueChanged(event);
            }
        }
    }
// @todo - 5 28.07.04-tb is never used anywhere - remove?
//    private void appendParamValue(StringBuffer sb, Object oldValue) {
//        if (oldValue == null) {
//            sb.append("(null)");
//        } else {
//            try {
//                String text = getValidator().format(this, oldValue);
//                sb.append('"');
//                sb.append(text);
//                sb.append('"');
//            } catch (ParamFormatException e) {
//                sb.append("(format-error: ");
//                sb.append(e.getMessage());
//                sb.append(")");
//            }
//        }
//    }

    private PropertyChangeListener createParamPropertiesPropertyChangeListener() {
        return new PropertyChangeListener() {

            /**
             * This method gets called when a bound property is changed.
             *
             * @param evt A PropertyChangeEvent object describing the event source and the property that has changed.
             */
            public void propertyChange(PropertyChangeEvent evt) {
                //Debug.trace("propertyChange(" + evt + ")");
                if (_editor != null) {
                    _editor.reconfigureUI();
                }
            }
        };
    }


    private void handleParamException(ParamExceptionHandler handler, ParamException e) {
        boolean handled = false;
        if (handler != null) {
            handled = handler.handleParamException(e);
        }
        if (!handled) {
            throw new IllegalArgumentException("illegal value for parameter '" + getName() + "': " + e.getMessage());
        }
    }

}




