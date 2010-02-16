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
        if (!file.getName().toLowerCase().startsWith(CERES_LAUNCHER_PREFIX)) {
            classpath.add(file);
        }
    }

    @Override
    protected File[] getClasspathFiles() {
        return classpath.toArray(new File[classpath.size()]);
    }
}
