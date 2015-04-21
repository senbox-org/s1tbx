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
 * An extension declared in a module.
 * <p>
 * If {@link #getDeclaringModule() declared} in a module manifest (module.xml), an extension has the following syntax:
 * <pre>
 *    &lt;extension point="{@link #getPoint() point}"&gt;
 *       {@link #getConfigurationElement() configuration element 1}
 *       {@link #getConfigurationElement() configuration element 2}
 *       ...
 *    &lt;/extension&gt;
 * </pre>
 * <p>
 * An extension can also have an optional identifier which makes it possible to retrieve it via the
 * {@link Module#getExtension(String)} method:
 * <pre>
 *    &lt;extension id="{@link #getId() id}" point="{@link #getPoint() point}"&gt;
 *       ...
 *    &lt;/extension&gt;
 * </pre>
 * This interface is not intended to be implemented by clients.
 */
public interface Extension {

    /**
     * Gets the name of the extension point which is extended by this extension.
     *
     * @return The name of the extension point.
     */
    String getPoint();

    /**
     * Gets the (optional) identifier.
     *
     * @return The identifier, can be {@code null}.
     */
    String getId();

    /**
     * Gets the configuration element of this extension.
     *
     * @return The configuration element.
     */
    ConfigurationElement getConfigurationElement();

    /**
     * Gets the module in which this extension is declared.
     *
     * @return The declaring module.
     */
    Module getDeclaringModule();

    /**
     * Gets the extension point which is extended by this extension.
     *
     * @return The extension point or {@code null} if the declaring module is yet neither registered nor resolved.
     */
    ExtensionPoint getExtensionPoint();
}
