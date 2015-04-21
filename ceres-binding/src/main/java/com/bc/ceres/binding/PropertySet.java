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

/**
 * A loose aggregation of properties. Properties can be added to and removed from this set.
 * Property change events are fired whenever property values change.
 * <p>
 * The {@link PropertySet} interface is based on the well-known <i>Property List</i> design pattern.
 *
 * @author Norman Fomferra
 * @see Property
 * @since 0.10
 */
public interface PropertySet extends PropertyChangeEmitter {

    /**
     * Gets all properties currently contained in this set.
     *
     * @return The array of properties, which may be empty.
     */
    Property[] getProperties();

    /**
     * Tests if the named property is defined in this set.
     * For undefined properties, the method  {@link #getProperty(String) getProperty(name)} will
     * always return {@code null}.
     *
     * @param name The property name or the property's alias name (both case sensitive).
     * @return {@code true} if the property is defined.
     */
    boolean isPropertyDefined(String name);

    /**
     * Gets the named property.
     *
     * @param name The property name or the property's alias name (both case sensitive).
     * @return The property, or {@code null} if the property does not exist.
     * @see #isPropertyDefined(String)
     * @see com.bc.ceres.binding.PropertyDescriptor#getAlias()
     */
    Property getProperty(String name);

    /**
     * Adds a property to this set.
     *
     * @param property The property.
     */
    void addProperty(Property property);

    /**
     * Adds the given properties to this set.
     *
     * @param properties The properties to be added.
     */
    void addProperties(Property... properties);

    /**
     * Removes a property from this set.
     *
     * @param property The property.
     */
    void removeProperty(Property property);

    /**
     * Removes the given properties from this set.
     *
     * @param properties The properties to be removed.
     */
    void removeProperties(Property... properties);

    /**
     * Gets the value of the named property.
     *
     * @param name The property name.
     * @return The property value or {@code null} if a property with the given name does not exist.
     * @throws ClassCastException if the value is not of the requested type.
     */
    <T> T getValue(String name) throws ClassCastException;

    /**
     * Sets the value of the named property.
     *
     * @param name  The property name.
     * @param value The new property value.
     * @throws IllegalArgumentException If the value is illegal.
     *                                  The cause will always be a {@link ValidationException}.
     */
    void setValue(String name, Object value) throws IllegalArgumentException;

    /**
     * Sets all properties to their default values.
     *
     * @throws IllegalStateException If at least one of the default values is illegal.
     *                               The cause will always be a {@link ValidationException}.
     * @see PropertyDescriptor#getDefaultValue()
     * @since Ceres 0.12
     */
    void setDefaultValues() throws IllegalStateException;

    /**
     * Gets the descriptor for the named property.
     *
     * @param name The property name (case sensitive).
     * @return The descriptor, or {@code null} if the property is unknown.
     */
    PropertyDescriptor getDescriptor(String name);
}
