package com.bc.ceres.launcher.internal;

import java.io.File;
import java.io.FileFilter;

public class ClasspathFileFilter implements FileFilter {

    public boolean accept(File file) {
        return isFileAccepted(file);
    }

    public static boolean isFileAccepted(File file) {
        if (file.isDirectory()) {
            return true;
        }
        String name = file.getName().toLowerCase();
        return name.endsWith(".jar") || name.endsWith(".zip");
    }
}
