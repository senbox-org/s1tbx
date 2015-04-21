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

package com.bc.ceres.core.runtime;

import com.bc.ceres.core.CoreException;

/**
 * Provides a {@link #launch(com.bc.ceres.core.runtime.RuntimeConfig, ClassLoader, String[]) launch} method to launch a runtime.
 * This class provides a default application entry point
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
