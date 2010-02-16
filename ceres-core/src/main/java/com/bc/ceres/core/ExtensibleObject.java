/* 
 * Copyright (C) 2002-2008 by Brockmann Consult
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
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
     * <p/>
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
