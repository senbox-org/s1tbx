package com.bc.ceres.core;

public interface ExtensionFactory<T> {
    <ET> ET getExtension(T extendibleObject, Class<ET> extensionType);

    Class<?>[] getExtensionTypes();
}
