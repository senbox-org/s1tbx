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
 * A runtime for applications composed of multiple modules.
 * <p>A module runtime is configured through a {@link RuntimeConfig}.
 * <p>The {@link #getModule()} method implemented of a <code>ModuleRuntime</code> returns the system module.
 * <p>This interface is not intended to be implemented by clients.
 */
public interface ModuleRuntime extends ModuleContext {

    /**
     * Gets the command line arguments passed to this runtime.
     *
     * @return the command line arguments
     */
    String[] getCommandLineArgs();

    /**
     * Starts the Ceres runtime.
     * This implies the following steps:
     * <ol>
     * <li>determine the runtime's <code>home</code> and <code>config</code> file locations,</li>
     * <li>if exists, load <code>config.properties</code> from the <code>config</code> location and add its properties to the system settings,</li>
     * <li>determine the runtime's <code>lib</code> and <code>modules</code> file locations,</li>
     * <li>scan the <code>lib</code> location for implicite JARs,</li>
     * <li>scan the <code>modules</code> location for modules,</li>
     * <li>initialize the system module,</li>
     * <li>resolve all modules,</li>
     * <li>start all modules,</li>
     * <li>register this runtime's {@link #stop()} method as a shutdown hook to the VM,</li>
     * <li>start an optional application, which is a {@link RuntimeRunnable} registered with the  <code>ceres-core:applications</code> extension point.</li>
     * </ol>
     *
     * @throws CoreException if an error in the runtime occurs
     * @see java.lang.Runtime#addShutdownHook(Thread)
     * @see #stop()
     */
    void start() throws CoreException;

    /**
     * Explicitely stops all modules and finally stops this Ceres runtime.
     * Note that this method is also registered as a shutdown hook to the VM during {@link #start()}.
     *
     * @throws CoreException if an error in the runtime occurs
     * @see #start()
     */
    void stop() throws CoreException;
}
