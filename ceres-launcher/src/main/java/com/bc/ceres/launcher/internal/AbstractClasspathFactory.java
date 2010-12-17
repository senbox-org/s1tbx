/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package com.bc.ceres.launcher.internal;

import com.bc.ceres.core.runtime.RuntimeConfig;
import com.bc.ceres.core.runtime.RuntimeConfigException;
import com.bc.ceres.launcher.ClasspathFactory;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public abstract class AbstractClasspathFactory implements ClasspathFactory {
    protected static final String CERES_LAUNCHER_PREFIX = "ceres-launcher";
    protected static final String CERES_CORE_PREFIX = "ceres-core";

    protected enum LibType {
        LIBRARY,
        MODULE,
    }

    protected RuntimeConfig config;

    protected AbstractClasspathFactory(RuntimeConfig config) {
        this.config = config;
    }

    public RuntimeConfig getConfig() {
        return config;
    }

    /**
     * Creates the classpath entries. Calls the two template methods
     * @return
     * @throws RuntimeConfigException
     */
    public URL[] createClasspath() throws RuntimeConfigException {
        processClasspathEntries();
        File[] files = getClasspathFiles();
        ArrayList<URL> classpathUrls = new ArrayList<URL>(files.length);
        for (File file : files) {
            try {
                URL url = file.toURI().toURL();
                classpathUrls.add(url);
                trace(String.format("Library added to classpath: [%s]", file));
            } catch (MalformedURLException e) {
                trace(String.format("Failed to add library to classpath: malformed library path: [%s]", file));
            }
        }
        trace(String.format("%d library(s) added to classpath", classpathUrls.size()));
        return classpathUrls.toArray(new URL[classpathUrls.size()]);
    }

    protected abstract File[] getClasspathFiles() throws RuntimeConfigException;

    protected abstract void processClasspathFile(File file, LibType libType, int level);

    private void processClasspathEntries() {
        processLibs();
        processModules();
    }

    private void processLibs() {
        String[] libDirPaths = config.getLibDirPaths();
        for (String libDirPath : libDirPaths) {
            processClasspathEntries(libDirPath, LibType.LIBRARY);
        }
    }

    private void processModules() {
        String moduleDirPath = config.getModulesDirPath();
        if (moduleDirPath != null) {
            processClasspathEntries(moduleDirPath, LibType.MODULE);
        }
    }

    private void processClasspathEntries(String dirPath, LibType libType) {
        File dir = new File(dirPath);
        if (!dir.isAbsolute()) {
            dir = new File(config.getHomeDirPath(), dirPath);
        }
        if (dir.isDirectory()) {
            processClasspathEntries(dir.getAbsoluteFile(), libType, 0);
        }
    }

    private void processClasspathEntries(File libDir, LibType libType, int level) {
        File[] files = libDir.listFiles(new ClasspathFileFilter());
        if (files == null) {
            return;
        }
        for (File file : files) {
            processClasspathEntry(file, libType, level);
        }
    }

    private void processClasspathEntry(File file, LibType libType, int level) {
        if (libType.equals(LibType.MODULE)) {
            if (file.isDirectory()) {
                processClasspathEntries(file, LibType.MODULE, level + 1);
            } else if (isUninstalledFile(file)) {
                return;
            }
        }
        
        boolean consider = libType.equals(LibType.LIBRARY) ||
                libType.equals(LibType.MODULE) && (file.isDirectory() && level == 0 || !file.isDirectory());
        if (consider) {
            processClasspathFile(file, libType, level);
        }
    }

    private boolean isUninstalledFile(File file) {
        return new File(file.getParent(), file.getName() + ".uninstall").exists();
    }

    private void trace(String msg) {
        if (config.isDebug()) {
            config.getLogger().info(String.format("ceres-launcher: %s", msg));
        }
    }
}
