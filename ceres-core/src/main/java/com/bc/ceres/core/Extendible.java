package com.bc.ceres.core;

public interface Extendible {
    <E> E getExtension(Class<E> extensionType);
}
