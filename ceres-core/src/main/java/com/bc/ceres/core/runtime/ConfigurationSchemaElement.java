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
 * A configuration element of an extension point (schema).
 * <p>
 * This interface is not intended to be implemented by clients.
 */
public interface ConfigurationSchemaElement extends ConfigurationElementBase<ConfigurationSchemaElement> {

    /**
     * Gets the declaring extension point, if this is an element of an extension point configuration (the schema).
     *
     * @return The declaring extension point, or {@code null} if this is not a schema element.
     */
    ExtensionPoint getDeclaringExtensionPoint();
}