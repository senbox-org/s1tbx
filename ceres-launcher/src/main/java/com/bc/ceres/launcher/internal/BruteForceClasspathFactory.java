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

import java.io.File;
import java.util.ArrayList;


public class BruteForceClasspathFactory extends AbstractClasspathFactory {

    private ArrayList<File> classpath;

    public BruteForceClasspathFactory(RuntimeConfig config) {
        super(config);
        classpath = new ArrayList<File>(32);
    }

    @Override
    public void processClasspathFile(File file, LibType libType, int level) {
        if (level > 2) {
            return;
        }
        final String name = file.getName().toLowerCase();
        if (!name.startsWith(CERES_LAUNCHER_PREFIX)) {
            if (LibType.LIBRARY.equals(libType)) {
                classpath.add(file);
            } else if (LibType.MODULE.equals(libType)) {
                if (level == 0) {
                    classpath.add(file);
                } else if (level == 2 && "lib".equalsIgnoreCase(file.getParentFile().getName())) {
                    classpath.add(file);
                }
            }
        }
    }

    @Override
    protected File[] getClasspathFiles() {
        return classpath.toArray(new File[classpath.size()]);
    }
}
