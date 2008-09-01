package com.bc.ceres.core;

/**
 * Interface for an object that can be modified by dynamically changing or adding
 * features.
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
