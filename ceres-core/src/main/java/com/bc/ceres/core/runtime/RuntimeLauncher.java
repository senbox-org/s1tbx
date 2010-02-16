package com.bc.ceres.core.runtime;

import com.bc.ceres.core.CoreException;

/**
 * Provides a {@link #launch(com.bc.ceres.core.runtime.RuntimeConfig, ClassLoader, String[]) launch} method to launch a runtime.
 * This class provides a default application entry point and is used by the {@link com.bc.ceres.launcher.Launcher}.
 */
public final class RuntimeLauncher {

    /**
     * Launches a runtime with a context identifier given by the
     * system property <code>ceres.context</code>.
     *
     * @param config          the configuration
     * @param classLoader     the bootstrap class loader to be used
     * @param commandLineArgs the command line arguments passed to the application
     * @throws CoreException if an error occurs during launch
     * @see RuntimeFactory#createRuntime
     */
    public static void launch(RuntimeConfig config, ClassLoader classLoader, String[] commandLineArgs) throws CoreException {
        ClassLoader oldContextClassLoader = Thread.currentThread().getContextClassLoader();
        try {
            Thread.currentThread().setContextClassLoader(classLoader);
            ModuleRuntime runtime = RuntimeFactory.createRuntime(config, commandLineArgs);
            runtime.start();
        } finally {
            Thread.currentThread().setContextClassLoader(oldContextClassLoader);
        }
    }

}
