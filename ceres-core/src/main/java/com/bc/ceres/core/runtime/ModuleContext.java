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
import com.bc.ceres.core.Extensible;
import com.bc.ceres.core.ProgressMonitor;

import java.net.URL;
import java.util.logging.Logger;

/**
 * The context in which a module lives.
 * <p>
 * This interface is not intended to be implemented by clients.
 */
public interface ModuleContext extends Extensible {
    /**
     * Gets the Ceres runtime configuration.
     * @return the Ceres runtime configuration.
     */
    RuntimeConfig getRuntimeConfig();

    /**
     * @return The module to which this context belongs to.
     */
    Module getModule();

    /**
     * Gets the module for the given module identifier.
     *
     * @param id The module identifier. If zero is passed, the
     *           system module is returned, which is always present.
     *
     * @return A module or <code>null</code> if no such exists.
     */
    Module getModule(long id);

    /**
     * Gets all modules in this context.
     *
     * @return All modules or an empty array.
     */
    Module[] getModules();

    /**
     * @return The context's logger.
     */
    Logger getLogger();

    /**
     * Installs the module from the given URL.
     *
     * @param url the URL
     * @param proxyConfig the proxy configuration, can be null
     * @param pm  the progress monitor
     *
     * @return the new module
     *
     * @throws CoreException if an error occurs
     */
    Module installModule(URL url, ProxyConfig proxyConfig, ProgressMonitor pm) throws CoreException;
}
