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
