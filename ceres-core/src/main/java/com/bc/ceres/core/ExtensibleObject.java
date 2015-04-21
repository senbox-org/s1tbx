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
 * Base class for an object that can be dynamically extended.
 *
 * @author Ralf Quast
 * @version $Revision$ $Date$
 */
public class ExtensibleObject implements Extensible {
    /**
     * Gets the extension for this object corresponding to a specified extension
     * type.
     * <p>
     * The default implementation is
     * <pre>
     *    return ExtensionManager.getInstance().getExtension(this, extensionType);
     * </pre>
     *
     * @param extensionType the extension type.
     * @return the extension for this object corresponding to the specified type.
     * @see ExtensionManager
     */
    @Override
    public <E> E getExtension(Class<E> extensionType) {
        return ExtensionManager.getInstance().getExtension(this, extensionType);
    }
}
