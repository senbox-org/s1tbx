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
