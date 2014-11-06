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

import java.io.File;
import java.util.ArrayList;

public class BootstrapClasspathFactory extends AbstractClasspathFactory {

    private ArrayList<File> classpath;
    private File ceresCoreFile;

    public BootstrapClasspathFactory(RuntimeConfig config) {
        super(config);
        classpath = new ArrayList<File>(10);
    }

    @Override
    protected File[] getClasspathFiles() throws RuntimeConfigException {
        if (ceresCoreFile != null) {
            classpath.add(0, ceresCoreFile);
        }
        return classpath.toArray(new File[classpath.size()]);
    }

    @Override
    public void processClasspathFile(File file, LibType libType, int level) {
        if (level > 0) {
            return;
        }
        String name = file.getName().toLowerCase();

        if (name.startsWith(CERES_CORE_PREFIX)) {
            ceresCoreFile = getMax(ceresCoreFile, file);
        } else  if (name.startsWith(CERES_LAUNCHER_PREFIX)) {
            return;
        } else if (libType.equals(LibType.LIBRARY)) {
            classpath.add(file);
        }
    }

    static File getMax(File file1, File file2) {
        if (file1 == null) {
            return file2;
        }
        if (file2 == null) {
            return file1;
        }
        String nameNoExt1 = trimExt(file1.getName());
        String nameNoExt2 = trimExt(file2.getName());
        if (nameNoExt1.compareToIgnoreCase(nameNoExt2) > 0) {
            return file1;
        } else {
            return file2;
        }
    }

    private static String trimExt(String name) {
        int p = name.lastIndexOf((int) '.');
        String nameNoExt = name;
        if (p > 0) {
            nameNoExt = name.substring(0, p);
        }
        return nameNoExt;
    }
}
