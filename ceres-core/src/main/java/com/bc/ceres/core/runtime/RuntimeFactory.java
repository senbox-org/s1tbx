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
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.internal.DefaultRuntimeConfig;
import com.bc.ceres.core.runtime.internal.RuntimeImpl;
import com.bc.ceres.core.runtime.internal.SplashScreenProgressMonitor;

/**
 * A factory for runtime instances.
 * It provides the main entry point to the module runtime framework.
 */
public final class RuntimeFactory {

    /**
     * Creates a default configuration for an application composed of multiple modules.
     * <p>A module runtime is configured through the following system properties:
     * <ol>
     * <li><code>${ceres.context}.home</code> - the home directory. If not provided it will be derived from the current classloader's classpath.</li>
     * <li><code>${ceres.context}.config</code> - the directory for configuration files, such as <code>config.properties</code>.</li>
     * <li><code>${ceres.context}.libDirs</code> - the directory which is scanned for JARs. These are automatically added the runtime's classpath.</li>
     * <li><code>${ceres.context}.modules</code> - the directory which is scanned for modules.</li>
     * <li><code>${ceres.context}.app</code> - the identifier of an extension registered with the <code>ceres-core:applications</code> extension point.</li>
     * </ol>
     * where <code>${ceres.context}</code> is the value of the runtime's context identifier which defaults to "ceres".
     *
     * @return a default configuration
     *
     * @throws CoreException if a runtime core error occurs
     */
    public static RuntimeConfig createRuntimeConfig() throws CoreException {
        try {
            return new DefaultRuntimeConfig();
        } catch (RuntimeConfigException e) {
            throw new CoreException("Failed to set-up Ceres runtime configuration", e);
        }
    }

    /**
     * Creates a new module runtime. The supplied class loader is used for two purposes:
     * <ol>
     * <li>Its classpath is scanned for additional modules</li>
     * <li>It serves as parent class loader for all modules</li>
     * </ol>
     *
     * @param config          the runtime's configuration
     * @param commandLineArgs the command line arguments passed to the application
     * @return a module runtime instance
     */
    public static ModuleRuntime createRuntime(RuntimeConfig config, String[] commandLineArgs) {
        ProgressMonitor progressMonitor = createProgressMonitor(config);
        return createRuntime(config, commandLineArgs, progressMonitor);
    }

    /**
     * Creates a new module runtime with a client supplied progress monitor.
     *
     * @param config          the runtime's configuration
     * @param commandLineArgs the command line arguments passed to the application
     * @param progressMonitor the progress monitor which is informed about the launch sequence's progress
     * @return a module runtime instance
     *
     * @see #createRuntime(com.bc.ceres.core.runtime.RuntimeConfig, String[])
     */
    public static ModuleRuntime createRuntime(RuntimeConfig config, String[] commandLineArgs, ProgressMonitor progressMonitor) {
        return new RuntimeImpl(config, commandLineArgs, progressMonitor);
    }


    /**
     * Creates a default progress monitor for the runtime.
     * If a splash screen (java.awt.SplashScreen) is available
     * a progress monitor will be created which outputs its progress
     * messages to on the bottom of the splash screen image.
     *
     * @param config the runtime configuration
     * @return A progress monitor.
     */
    public static ProgressMonitor createProgressMonitor(RuntimeConfig config) {
        try {
            Class.forName("java.awt.SplashScreen");
            return SplashScreenProgressMonitor.createProgressMonitor(config);
        } catch (ClassNotFoundException e) {
            return ProgressMonitor.NULL;
        }
    }
}
