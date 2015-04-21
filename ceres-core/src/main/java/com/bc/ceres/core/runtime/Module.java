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

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Enumeration;

/**
 * Represents a module.
 * <p>
 * This interface is not intended to be implemented by clients.
 */
public interface Module extends Comparable<Module> {

    /**
     * @return The module's unique identifier.
     */
    long getModuleId();

    /**
     * @return The version of the module's manifest.
     */
    String getManifestVersion();

    /**
     * @return The module's symbolic name.
     */
    String getSymbolicName();

    /**
     * @return The module's version.
     */
    Version getVersion();

    /**
     * @return The current state of this module.
     */
    ModuleState getState();

    /**
     * @return The module's name, or <code>null</code> if not set.
     */
    String getName();

    /**
     * @return The name of the vendor of this module, or <code>null</code> if not set.
     */
    String getVendor();

    /**
     * @return The changelog information of this module, or <code>null</code> if not set.
     */
    String getChangelog();

    /**
     * @return The copyright notice of this module, or <code>null</code> if not set.
     */
    String getCopyright();

    /**
     * @return The contact address of the vendor of this mdule, or <code>null</code> if not set.
     */
    String getContactAddress();

    /**
     * @return The agency providing the funding for development, or <code>null</code> if not set.
     */
    String getFunding();

    /**
     * @return The module's description, or <code>null</code> if not set.
     */
    String getDescription();

    /**
     * @return The URL of the home page or documentation of this module, or <code>null</code> if not set.
     */
    String getUrl();

    /**
     * @return The location of the module's 'about.html' file.
     */
    String getAboutUrl();

    /**
     * @return The location of the module's license.
     */
    String getLicenseUrl();

    /**
     * @return The content length in bytes of the module's archive file (if any).
     */
    long getContentLength();

    /**
     * @return The date of the last modification expressed in milliseconds since 01.01.1970.
     */
    long getLastModified();

    /**
     * @return The module's categories.
     */
    String[] getCategories();

    /**
     * @return The module's packaging, e.g. "dir", "jar", "zip".
     */
    String getPackaging();

    /**
     * @return <code>true</code> if the module uses native libraries via JNI.
     */
    boolean isNative();

    /**
     * @return The name of this module's activator class.
     */
    String getActivatorClassName();

    /**
     * @return The module's (file) location.
     */
    URL getLocation();

    /**
     * @return The module's declared dependencies.
     */
    Dependency[] getDeclaredDependencies();

    /**
     * @return The module's declared extension points.
     */
    ExtensionPoint[] getExtensionPoints();

    /**
     * Gets the extension point for the given extension point identifier.
     *
     * @param extensionPointId The extension point identifier. Can by fully qualified
     *                         (<code>&lt;moduleId&gt;:&lt;extensionPointId&gt;</code>) or simple.
     *
     * @return The extension point.
     */
    ExtensionPoint getExtensionPoint(String extensionPointId);

    /**
     * Gets the extension with the specified identifier.
     *
     * @param extensionId The extension identifier.
     *
     * @return The extension or {@code null} if no such was found.
     */
    Extension getExtension(String extensionId);

    /**
     * @return The module's declared extensions.
     */
    Extension[] getExtensions();

    /**
     * Uninstalls this module from its runtime.
     *
     * @param pm the progress monitor
     *
     * @throws CoreException if an error occurred
     */
    void uninstall(ProgressMonitor pm) throws CoreException;

    /**
     * Gets the class loader used by this module.
     *
     * @return The class loader, or {@code null} if this module has not yet been resolved.
     */
    ClassLoader getClassLoader();

    /**
     * Loads the class with the specified name.
     *
     * @param name The <a href="#name">binary name</a> of the class
     *
     * @return The resulting <tt>Class</tt> object
     *
     * @throws ClassNotFoundException If the class was not found
     * @see ClassLoader#loadClass(String)
     */
    Class<?> loadClass(String name) throws ClassNotFoundException;

    /**
     * Finds the resource with the given name.  A resource is some data
     * (images, audio, text, etc) that can be accessed by class code in a way
     * that is independent of the location of the code.
     * <p>
     * The name of a resource is a '<tt>/</tt>'-separated path name that
     * identifies the resource.
     *
     * @param name The resource name
     *
     * @return A <tt>URL</tt> object for reading the resource, or
     *         <tt>null</tt> if the resource could not be found or the invoker
     *         doesn't have adequate privileges to get the resource.
     *
     * @see ClassLoader#getResource(String)
     */
    URL getResource(String name);

    /**
     * Returns an input stream for reading the specified resource.
     * <p>The name of a resource is a <tt>/</tt>-separated path name that
     * identifies the resource.
     *
     * @param name The resource name
     *
     * @return An input stream for reading the resource, or <tt>null</tt>
     *         if the resource could not be found
     *
     * @see ClassLoader#getResourceAsStream(String)
     */
    InputStream getResourceAsStream(String name);

    /**
     * Finds all the resources with the given name. A resource is some data
     * (images, audio, text, etc) that can be accessed by class code in a way
     * that is independent of the location of the code.
     * <p>
     * The name of a resource is a <tt>/</tt>-separated path name that
     * identifies the resource.
     *
     * @param name The resource name
     *
     * @return An enumeration of {@link java.net.URL <tt>URL</tt>} objects for
     *         the resource.  If no resources could  be found, the enumeration
     *         will be empty.  Resources that the module doesn't have
     *         access to will not be in the enumeration.
     *
     * @throws java.io.IOException If I/O errors occur
     * @see ClassLoader#getResources(String)
     */
    Enumeration<URL> getResources(String name) throws IOException;
}