package com.bc.ceres.fs;

public interface FileNodeFilter {
    static FileNodeFilter ALL = new FileNodeFilter() {
        public boolean accept(FileNode parent, String path) {
            return true;
        }
    };

    boolean accept(FileNode parent, String path);
}
