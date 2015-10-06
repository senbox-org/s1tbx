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

import org.esa.snap.core.util.DefaultPropertyMap;
import org.esa.snap.core.util.Guardian;
import org.esa.snap.core.util.PropertyMap;

import java.util.List;

/**
 * The <code>ParamGroup</code> class represents a ordered list of parameters.
 *
 * @author Norman Fomferra
 * @author Sabine Embacher
 * @version $Revision$  $Date$
 */
public class ParamGroup {

    /**
     * The parameters contained in this group.
     *
     * @link aggregation
     * @associates <{Parameter}>
     * @supplierRole parameters
     */
    private List _parameters;


    /**
     * Constructs a new parameter group.
     */
    public ParamGroup() {
        _parameters = new java.util.Vector();
    }

    /**
     * Adds and configures parameters supplied through the given properties.
     * <p> For each entry in the properties having the form <code><i>any-string</i>.name = <i>param-name</i></code> the
     * method creates a new parameter with the name given by <i>param-name</i> if it does not already exists in this
     * list. Then, for each entry of the form <code><i>param-name</i>.<i>attrib-name</i> = <i>attrib-value</i></code>
     * contained in the given properties, the method sets/creates a new parameter attribute for the parameter's
     * <code>getProperties()</code> field.
     * <p> If a parameter found in the properties has the <code>null</code> value, the method sets the value of this
     * parameter to the value given by <code><i>param-name</i>.defaultValue = <i>default-value</i></code>, if it can be
     * found in the properties.
     *
     * @param propertyMap the properties used to add and configure new parameters
     *
     * @see Parameter#getProperties
     * @see ParamProperties
     */
    public static ParamGroup create(PropertyMap propertyMap) {
        ParamGroup paramGroup = new ParamGroup();
        final String nameSuffix = ".name";
        propertyMap.getPropertyKeys().stream().filter(key -> key.endsWith(nameSuffix)).forEach(key -> {
            String paramName = propertyMap.getPropertyString(key);
            Parameter parameter = paramGroup.createParameter(paramName);
            parameter.setPropertyValues(propertyMap);
            if (parameter.getValue() == null) {
                if (parameter.getProperties().getDefaultValue() != null) {
                    parameter.setDefaultValue();
                }
            }
        });
        return paramGroup;
    }

    /**
     * Sets parameter values supplied through the given property map.
     * <p> For each parameter contained in this list having the name <i>param-name</i> this method searches for entries
     * in the  property ma having the form <code><i>param-name</i> = <i>param-value</i></code>. If it can be found, the
     * parameter is set to the given textual value using the parameter's <code>setValueAsText</code> method.
     *
     * @param propertyMap the property map, must not be <code>null</code>
     * @param handler     an optional error handler, can be <code>null</code>
     *
     * @see Parameter#setValueAsText
     */
    public void setParameterValues(PropertyMap propertyMap, ParamExceptionHandler handler) {
        Guardian.assertNotNull("propertyMap", propertyMap);
        for (int i = 0; i < getNumParameters(); i++) {
            Parameter parameter = getParameterAt(i);
            String valueText = propertyMap.getPropertyString(parameter.getName(), null);
            if (valueText != null) {
                parameter.setValueAsText(valueText, handler);
            }
        }
    }

    /**
     * Gets the parameter values in this group as a property map instance.
     * <p> Simply returns <code>getParameterValues(null)</code>.
     *
     * @return the property map, never <code>null</code>
     *
     * @see #getParameterValues(PropertyMap)
     */
    public PropertyMap getParameterValues() {
        return getParameterValues(null);
    }

    /**
     * Gets the parameter values in this group as a property map instance.
     * <p> For each parameter contained in this list a new entry in the  property map instance is created. The key is
     * always the paramer's name and the value is created by using the parameter's <code>getValueAsText</code> method.
     *
     * @param propertyMap if not <code>null</code> used as return value, otherwise a new <code>Properties</code>
     *                    instance will be created and returned
     *
     * @return the property map, never <code>null</code>
     *
     * @see Parameter#getValueAsText
     */
    public PropertyMap getParameterValues(PropertyMap propertyMap) {
        if (propertyMap == null) {
            propertyMap = new DefaultPropertyMap();
        }
        for (int i = 0; i < getNumParameters(); i++) {
            Parameter parameter = getParameterAt(i);
            String valueText = parameter.getValueAsText();
            if (valueText != null) {
                propertyMap.setPropertyString(parameter.getName(), valueText);
            } else {
                propertyMap.setPropertyString(parameter.getName(), null);
            }
        }
        return propertyMap;
    }

    /**
     * Returns the number of parameters in this group.
     */
    public int getNumParameters() {
        return _parameters.size();
    }

    /**
     * Returns the parameter with the given index.
     *
     * @param index the parameter index
     *
     * @throws IndexOutOfBoundsException if the index is negative or greater or equal to <code>getNumParameters()</code>.
     */
    public Parameter getParameterAt(int index) {
        return (Parameter) _parameters.get(index);
    }

    /**
     * Returns the parameter with the given name.
     *
     * @return the parameter or <code>null</code> if the a parameter with the given name was not found in this group
     */
    public Parameter getParameter(String name) {
        return getParameter(name, false);
    }

    /**
     * Creates a parameter for the given name. If a parameter with the given name already exists in this group, its
     * reference is returned.
     *
     * @return the parameter, never <code>null</code>
     */
    public Parameter createParameter(String name) {
        return getParameter(name, true);
    }

    /**
     * Returns the index of the parameter with the given name. If a parameter with the given name was not found,
     * <code>-1</code> is returned.
     *
     * @return the parameter index, or <code>-1</code> if it was not found
     */
    public int getParameterIndex(String name) {
        int n = getNumParameters();
        for (int i = 0; i < n; i++) {
            if (getParameterAt(i).getName().equalsIgnoreCase(name)) {
                return i;
            }
        }
        return -1;
    }

    /**
     * Adds the given parameter to this group. If the parameter is <code>null</code> or the parameter already exists in
     * this group, nothing happens.
     *
     * @param parameter the parameter to be added
     */
    public void addParameter(Parameter parameter) {
        if (parameter != null && !_parameters.contains(parameter)) {
            if (getParameterIndex(parameter.getName()) >= 0) {
                throw new IllegalArgumentException(
                        ParamConstants.ERR_MSG_PARAM_IN_GROUP_1 + parameter.getName() + ParamConstants.ERR_MSG_PARAM_IN_GROUP_2);
            }
            _parameters.add(parameter);
        }
    }

    /**
     * Removes the given parameter from this group. If the parameter is <code>null</code> or the parameter does not
     * exists in this group, nothing happens.
     *
     * @param parameter the parameter to be removed
     */
    public void removeParameter(Parameter parameter) {
        if (parameter != null) {
            _parameters.remove(parameter);
        }
    }

    /**
     * Adds a parameter change listener to all parameters in this group.
     *
     * @param listener the listener to be added to all parameters
     */
    public void addParamChangeListener(ParamChangeListener listener) {
        if (listener == null) {
            return;
        }
        for (int i = 0; i < getNumParameters(); i++) {
            getParameterAt(i).addParamChangeListener(listener);
        }
    }

    /**
     * Removes the parameter change listener from all parameters in this group.
     *
     * @param listener the listener to be removed from all parameters
     */
    public void removeParamChangeListener(ParamChangeListener listener) {
        if (listener == null) {
            return;
        }
        for (int i = 0; i < getNumParameters(); i++) {
            getParameterAt(i).removeParamChangeListener(listener);
        }
    }

    private Parameter getParameter(String name, boolean create) {
        Parameter parameter = null;
        int index = getParameterIndex(name);
        if (index >= 0) {
            parameter = getParameterAt(index);
        } else if (create) {
            parameter = new Parameter(name);
            addParameter(parameter);
        }
        return parameter;
    }
}




