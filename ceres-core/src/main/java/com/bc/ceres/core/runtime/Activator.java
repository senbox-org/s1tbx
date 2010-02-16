package com.bc.ceres.core.runtime;

import com.bc.ceres.core.CoreException;

public interface Activator {
    void start(ModuleContext moduleContext) throws CoreException;

    void stop(ModuleContext moduleContext) throws CoreException;
}
