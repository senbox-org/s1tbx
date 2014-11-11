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

package com.bc.ceres.swing.update;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ProxyConfig;

import java.net.URL;

public interface ModuleManager {

    /**
     * Retrieves the repository url.
     *
     * @return An url which points to the module repository, might be {@code null} if not already set.
     */
    URL getRepositoryUrl();

    /**
     * Gets the proxy configuration.
     *
     * @return A configuration used when the connection uses a proxy, might be {@code null} if not already set.
     */
    ProxyConfig getProxyConfig();

    /**
     * Retrieves the list of locally installed {@link Module modules}.
     *
     * @return An array of locally installed modules.
     */
    Module[] getInstalledModules();

    /**
     * Retrieves the list of {@link Module modules} on the repository.
     *
     * @param pm A monitor to inform the user about progress.
     *
     * @return An array of modules on the repository.
     *
     * @throws com.bc.ceres.core.CoreException
     *          if an error occures
     */
    Module[] getRepositoryModules(ProgressMonitor pm) throws CoreException;

    /**
     * Retrieves the list of locally installed {@link ModuleItem module items}.
     *
     * @return An array of locally installed modules.
     *
     * @since 0.6.1
     */
    ModuleItem[] getInstalledModuleItems();

    /**
     * Retrieves the list of local {@link ModuleItem module items} which can be updated by modules from the repository.
     * Will return an empty array until {@link #synchronizeWithRepository(ProgressMonitor)} is called.
     *
     * @return An array of updatable modules.
     *
     * @since 0.6.1
     */
    ModuleItem[] getUpdatableModuleItems();

    /**
     * Retrieves the list of {@link ModuleItem module items} which can be installed from the repository.
     * Will return an empty array until {@link #synchronizeWithRepository(ProgressMonitor)} is called.
     *
     * @return An array of available modules.
     *
     * @since 0.6.1
     */
    ModuleItem[] getAvailableModuleItems();

    /**
     * Synchronizes the this module manager with the specified repository.
     *
     * @param pm Can be used to indicate progress
     *
     * @throws CoreException It is thrown if the {@link #getRepositoryUrl() repository url} is not set
     *                       or an IO-Error occures.
     * @since 0.6.1
     */
    public void synchronizeWithRepository(ProgressMonitor pm) throws CoreException;

    /**
     * Performs an installation of a new module.
     *
     * @param newModule The module to be installed.
     * @param pm        A monitor to inform the user about progress.
     *
     * @return The installed module.
     *
     * @throws CoreException if an error occures
     * @see #startTransaction()
     * @see #endTransaction()
     * @see #rollbackTransaction()
     */
    Module installModule(Module newModule, ProgressMonitor pm) throws CoreException;

    /**
     * Performs an update to a higher version of an existing module.
     *
     * @param oldModule The module to be replaced by an newer one.
     * @param newModule The new module to be installed.
     * @param pm        A monitor to inform the user about progress.
     *
     * @return The installed module.
     *
     * @throws CoreException if an error occures
     * @see #startTransaction()
     * @see #endTransaction()
     * @see #rollbackTransaction()
     */
    Module updateModule(Module oldModule, Module newModule, ProgressMonitor pm) throws CoreException;

    /**
     * * Performs an uninstallation of an existing module.
     *
     * @param oldModule The module to be removed.
     * @param pm        A monitor to inform the user about progress.
     *
     * @throws CoreException if an error occures.
     * @see #startTransaction()
     * @see #endTransaction()
     * @see #rollbackTransaction()
     */
    void uninstallModule(Module oldModule, ProgressMonitor pm) throws CoreException;

    /**
     * After calling this method all calls to {@link #installModule(Module, ProgressMonitor) installModule()},
     * {@link #updateModule(Module, Module, ProgressMonitor) updateModule()} and {@link #uninstallModule(Module, ProgressMonitor) uninstallModule()}
     * are recorded.
     *
     * @see #endTransaction()
     * @see #rollbackTransaction()
     */
    void startTransaction();

    /**
     * Clears all recorded method all calls.
     *
     * @see #startTransaction()
     * @see #rollbackTransaction()
     */
    void endTransaction();

    /**
     * Makes all recorded method calls undone.
     *
     * @see #startTransaction()
     * @see #endTransaction()
     */
    void rollbackTransaction();
}
