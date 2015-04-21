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

import com.bc.ceres.core.CoreException;

/**
 * A configuration element of an extension.
 * <p>This interface also provides a way to create executable extension objects.
 * <p>
 * This interface is not intended to be implemented by clients.
 */
public interface ConfigurationElement extends ConfigurationElementBase<ConfigurationElement> {

    /**
     * Gets the corresponding schema element, if this is an element of an extension configuration.
     *
     * @return The schema element, or {@code null} if this is a schema element.
     *
     * @see #getDeclaringExtension()
     */
    ConfigurationSchemaElement getSchemaElement();

    /**
     * Gets the declaring extension, if this is an element of an extension configuration.
     *
     * @return The declaring extension, or {@code null} if this is a schema element.
     *
     * @see #getSchemaElement()
     */
    Extension getDeclaringExtension();

    /**
     * Creates and returns a new instance of the executable extension identified by
     * the named attribute of this configuration element. The named attribute value must
     * contain a fully qualified name of a Java class implementing the executable extension.
     * <p>
     * The specified class is instantiated using its 0-argument public constructor.
     * If the specified class implements the {@link ConfigurableExtension} interface, its
     * {@link ConfigurableExtension#configure(ConfigurationElement)}  configure} method is called, passing to the
     * object the configuration information that was used to create it.
     *
     * @param extensionType the expected type of the executable extension instance
     * @return the executable instance
     *
     * @throws CoreException    if an instance of the executable extension could not be created for any reason.
     * @throws RuntimeException if this is a schema element
     */
    <T> T createExecutableExtension(Class<T> extensionType) throws CoreException;
}
