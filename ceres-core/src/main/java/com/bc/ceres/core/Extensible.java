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
     * @return the extension for this object corresponding to the specified type.
     */
    <E> E getExtension(Class<E> extensionType);
}
