package com.bc.ceres.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

public interface FileNode {
    boolean isDirectory();

    boolean isFile();

    boolean exists();

    String getParent();

    List<String> getChildren(FileNodeFilter filter);

    FileNode createChild(String path);

    String getPath();

    InputStream openInputStream() throws IOException;

    OutputStream openOutputStream() throws IOException;
}
