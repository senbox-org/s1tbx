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

package org.esa.snap.dataio.netcdf;

/**
 * Provides storage for properties.
 */
interface PropertyStore {
    /**
     * Sets a property value with the given name.
     *
     * @param name  the name of the property
     * @param value the value of the property
     */
    void setProperty(String name, Object value);

    /**
     * Retrieves the value of a property.
     *
     * @param name  the name of the property
     * @return The value of the property or {@code null} if the property name is unknown.
     */
    Object getProperty(String name);
}
