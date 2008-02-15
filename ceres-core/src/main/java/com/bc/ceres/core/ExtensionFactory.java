package com.bc.ceres.core;

public interface ExtensionFactory<T> {
    <E> E getExtension(T extendibleObject, Class<E> extensionType);

    Class<?>[] getExtensionTypes();
}
