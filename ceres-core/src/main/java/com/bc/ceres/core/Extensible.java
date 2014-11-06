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

package com.bc.ceres.core;

/**
 * Objects implementing this interface can be dynamically extended.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public interface Extensible {
    /**
     * Gets the extension for this object corresponding to a specified extension
     * type.
     *
     * @param extensionType the extension type.
     * @return the extension for this object corresponding to the specified type,
     *         or {@code null} if an extension of type {@code extensionType} cannot be delivered.
     */
    <E> E getExtension(Class<E> extensionType);
}
