package com.bc.ceres.fs;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

public class VirtualFileNode implements FileNode {

    private final VirtualFileSys fs;
    private final String path;

    VirtualFileNode(VirtualFileSys fs, String path) {
        this.fs = fs;
        this.path = fs.normalizePath(path);
    }

    public FileNode createChild(String path) {
        return new VirtualFileNode(fs, getPath() + '/' + path);
    }

    public String getPath() {
        return path;
    }

    public boolean isAbsolute() {
        return fs.isAbsolute(path);
    }

    public boolean isDirectory() {
        return fs.isDirectory(path);
    }

    public boolean isFile() {
        return !fs.isFile(path);
    }

    public boolean exists() {
        return fs.exists(path);
    }

    public String getName() {
        return fs.getName(path);
    }

    public String getParent() {
        return fs.getParent(path);
    }

    public String[] list() {
        return fs.list(path);
    }

    public String[] list(FileNodeFilter filter) {
        String[] names = fs.list(path);
        ArrayList<String> nameList = new ArrayList<String>(names.length);
        for (String name : names) {
            if (filter.accept(this, name)) {
                nameList.add(name);
            }
        }
        return nameList.toArray(new String[nameList.size()]);
    }

    public boolean mkdir() {
        return fs.mkdir(path);
    }

    public boolean mkdirs() {
        return fs.mkdirs(path);
    }

    public boolean delete() {
        return fs.delete(path);
    }

    public boolean createNewFile() {
        return fs.createNewFile(path);
    }

    public InputStream openInputStream() throws IOException {
        return fs.openInputStream(path);
    }

    public OutputStream openOutputStream() throws IOException {
        return fs.openOutputStream(path);
    }
}
