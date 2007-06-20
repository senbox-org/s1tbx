package com.bc.ceres.fs;

import java.io.*;

public class DefaultFileNode implements FileNode {
    private final File file;

    public DefaultFileNode(String path) {
        this.file = new File(path);
    }

    public boolean isAbsolute() {
        return file.isAbsolute();
    }

    public String getName() {
        return file.getName();
    }

    public boolean mkdir() {
        return file.mkdir();
    }

    public boolean mkdirs() {
        return file.mkdirs();
    }

    public boolean delete() {
        return file.delete();
    }

    public boolean createNewFile() throws IOException {
        return file.createNewFile();
    }

    public boolean isDirectory() {
        return file.isDirectory();
    }

    public boolean isFile() {
        return file.isFile();
    }

    public boolean exists() {
        return file.exists();
    }

    public String getParent() {
        return file.getParent();
    }

    public String getPath() {
        return file.getPath();
    }

    public String[] list() {
        String[] names = file.list();
        if (names == null) {
            names = new String[0];
        }
        return names;
    }

    public String[] list(final FileNodeFilter filter) {
        String[] names = file.list(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return filter.accept(DefaultFileNode.this, name);
            }
        });
        if (names == null) {
            names = new String[0];
        }
        return names;
    }

    public FileNode createChild(String path) {
        return new DefaultFileNode(new File(getPath(), path).getPath());
    }

    public InputStream openInputStream() throws IOException {
        return new FileInputStream(file);
    }

    public OutputStream openOutputStream() throws IOException {
        return new FileOutputStream(file);
    }
}
