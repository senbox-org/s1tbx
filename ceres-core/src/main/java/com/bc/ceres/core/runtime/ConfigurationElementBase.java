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

package com.bc.ceres.core.runtime;
/**
 * A configuration element, with its attributes and children,
 * directly reflects the content and structure of the extension and extension point
 * sections within the declaring module's manifest (module.xml) file.
 * <p>
 * This interface is not intended to be implemented by clients.
 */
public interface ConfigurationElementBase<T> {

    /**
     * Gets the name of this element.
     *
     * @return The name of this element.
     */
    String getName();

    /**
     * Gets the value of this element.
     *
     * @return The value of this element, or {@code null}.
     */
    String getValue();

    /**
     * Gets the value of the attribute with the specified name.
     *
     * @param attributeName The attribute name.
     *
     * @return The value of the attribute, or {@code null}.
     */
    String getAttribute(String attributeName);

    /**
     * Gets the names of all attributes.
     *
     * @return The array of names, which may be empty.
     */
    String[] getAttributeNames();

    /**
     * Gets the element which contains this element.
     * If this element is an immediate child of an extension or an extension point, the returned value is {@code null}.
     *
     * @return The parent of this configuration element or {@code null} if this is a root element.
     */
    T getParent();

    /**
     * Gets the child element with the specified element name.
     *
     * @param elementName The element name.
     *
     * @return The child element or {@code null} if no such could be found.
     */
    T getChild(String elementName);

    /**
     * Gets all children.
     *
     * @return The array of children, which may be empty.
     */
    T[] getChildren();

    /**
     * Gets all children with the specified element name.
     *
     * @param elementName The element name.
     *
     * @return The array of children, which may be empty.
     */
    T[] getChildren(String elementName);
}
