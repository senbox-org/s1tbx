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
