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
 * A factory providing runtime extensions for a given object.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public interface ExtensionFactory {
    /**
     * Gets an instance of an extension type for the specified object.
     *
     * @param object        The object.
     * @param extensionType The type of the requested extension.
     * @return The extension instance, or {@code null} if the given object is not extensible by this factory.
     */
    Object getExtension(Object object, Class<?> extensionType);

    /**
     * @return The array of extension types supported by this factory.
     */
    Class<?>[] getExtensionTypes();
}
