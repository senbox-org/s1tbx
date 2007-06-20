package com.bc.ceres.fs;


public class DefaultFileNodeFactory implements FileNodeFactory {
    public FileNode createFileNode(String path) {
        return new DefaultFileNode(path);
    }
}
