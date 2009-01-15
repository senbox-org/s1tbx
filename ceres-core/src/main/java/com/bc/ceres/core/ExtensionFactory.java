package com.bc.ceres.core;

/**
 * A factory which provides instances of extension types for a given object.
 *
 * @author Norman Fomferra
 * @version $Revision$ $Date$
 */
public interface ExtensionFactory<T> {
    /**
     * Gets an instance of an extension type for the specified object of type T.
     *
     * @param object        The object.
     * @param extensionType The type of the requested extension.
     * @return The extension instance, or {@code null} if the given object is not extensible by this factory.
     */
    <E> E getExtension(T object, Class<E> extensionType);

    /**
     * @return The array of extension types supported by this factory.
     */
    Class<?>[] getExtensionTypes();
}
