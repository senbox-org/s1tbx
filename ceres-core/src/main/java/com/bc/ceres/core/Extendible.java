package com.bc.ceres.core;

public interface Extendible {
    <ET> ET getExtension(Class<ET> extensionType);
}
