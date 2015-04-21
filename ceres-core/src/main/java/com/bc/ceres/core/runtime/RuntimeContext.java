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

import java.io.IOException;
import java.net.URL;
import java.util.Enumeration;

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
     *
     * @return {@code true} if the runtime is available.
     */
    public static boolean isAvailable() {
        return getModuleContext() != null;
    }

    /**
     * Gets the runtime configuration. The method
     * returns {@code null} if the runtime is not available.
     *
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
        return null;
    }

    /**
     * Finds all the resources within all runtime modules with the given name.
     * A resource is some data
     * (images, audio, text, etc) that can be accessed by class code in a way
     * that is independent of the location of the code.
     * <p>
     * The name of a resource is a <tt>/</tt>-separated path name that
     * identifies the resource.
     *
     * @param resourceName The resource name
     * @return An enumeration of {@link java.net.URL <tt>URL</tt>} objects for
     * the resource.  If no resources could  be found, the enumeration
     * will be empty.  Resources that the module runtime doesn't have
     * access to will not be in the enumeration.
     *
     * @throws java.io.IOException If I/O errors occur
     * @see ClassLoader#getResources(String)
     * @since 0.14
     */
    public static Enumeration<URL> getResources(String resourceName) throws IOException {
        return Thread.currentThread().getContextClassLoader().getResources(resourceName);
    }
}
