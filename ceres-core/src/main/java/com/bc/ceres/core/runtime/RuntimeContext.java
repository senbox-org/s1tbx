package com.bc.ceres.core.runtime;

import com.bc.ceres.core.runtime.internal.RuntimeActivator;

/**
 * The Ceres {@code Runtime} class provides access to the system module, which
 * can be used to access the runtime's configuration and all of its
 * current modules.
 *
 * @author Norman Fomferra
 */
public final class RuntimeContext {

    /**
     * Checks the status of the runtime's one and only system module.
     * @return {@code true} if the runtime is available.
     */
     public static boolean isAvailable() {
        return getModuleContext() != null;
    }

    /**
     * Gets the runtime configuration. The method
     * returns {@code null} if the runtime is not available.
     * @return The runtime configuration, or {@code null}.
     */
    public static RuntimeConfig getConfig() {
        final ModuleContext moduleContext = getModuleContext();
        return moduleContext != null ? moduleContext.getRuntimeConfig() : null;
    }

    /**
     * Gets the context of the runtime's one and only system module. The method
     * returns {@code null} if the runtime is not available. This is the case
     * if the Ceres runtime is either not used at all or the system module
     * has not yet been started or it has already been stopped.
     *
     * @return The runtime's system module context, or {@code null}.
     */
    public static ModuleContext getModuleContext() {
        final RuntimeActivator instance = RuntimeActivator.getInstance();
        return instance != null ? instance.getModuleContext() : null;
    }
}
