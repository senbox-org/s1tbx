package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Activator;
import com.bc.ceres.core.runtime.ModuleContext;

public class DefaultActivator implements Activator {
    public void start(ModuleContext moduleContext) throws CoreException {
        moduleContext.getLogger().finest("DefaultActivator.start (module=" + moduleContext.getModule().getSymbolicName() + ")");
    }

    public void stop(ModuleContext moduleContext) throws CoreException {
        moduleContext.getLogger().finest("DefaultActivator.stop (module=" + moduleContext.getModule().getSymbolicName() + ")");
    }
}
