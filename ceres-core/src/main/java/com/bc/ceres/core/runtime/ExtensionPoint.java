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
 * An extension point declared in a module.
 * <p>
 * If {@link #getDeclaringModule() declared} in a module manifest (module.xml), an extension point has the following syntax:
 * <pre>
 *    &lt;extensionPoint id="{@link #getId() id}"&gt;
 *       {@link #getConfigurationSchemaElement() configuration schema element 1}
 *       {@link #getConfigurationSchemaElement() configuration schema element 2}
 *       ...
 *    &lt;/extensionPoint&gt;
 * </pre>
 * This interface is not intended to be implemented by clients.
 */
public interface ExtensionPoint {

    /**
     * Gets the identifier.
     *
     * @return The identifier.
     */
    String getId();

    /**
     * Gets the qualified identifier (module identifier plus extension point identifier separated by a colon ':').
     *
     * @return The qualified identifier.
     */
    String getQualifiedId();

    /**
     * Gets the configuration schema element of this extension point.
     *
     * @return The configuration schema element.
     */
    ConfigurationSchemaElement getConfigurationSchemaElement();

    /**
     * Gets the module in which this extension point is declared.
     *
     * @return The declaring module.
     */
    Module getDeclaringModule();

    /**
     * Gets all extensions extending this extension point.
     *
     * @return All extensions, or {@code null} if the declaring module has not yet been registered.
     */
    Extension[] getExtensions();

    /**
     * Gets all configuration elements of all extensions extending this extension point.
     *
     * @return All configuration elements, or {@code null} if the declaring module has not yet been registered.
     */
    ConfigurationElement[] getConfigurationElements();
}

