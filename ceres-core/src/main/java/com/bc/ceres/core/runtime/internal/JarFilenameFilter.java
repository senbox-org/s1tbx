package com.bc.ceres.core.runtime.internal;

import java.io.File;
import java.io.FilenameFilter;

public class JarFilenameFilter implements FilenameFilter {
    public boolean accept(File dir, String name) {
        return isJarName(name);
    }

    public static boolean isJarName(String name) {
        return name.length() > 4 && (
                isJarExtension(name) ||
                isZipExtension(name));
    }

    public static boolean isJarExtension(String name) {
        return name.endsWith(".jar") || name.endsWith(".JAR");
    }
    public static boolean isZipExtension(String name) {
        return name.endsWith(".zip") || name.endsWith(".ZIP");
    }
}
