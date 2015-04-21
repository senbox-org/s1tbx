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

import java.util.logging.Logger;

/**
 * The configuration for a Ceres runtime.
 * The following system properties are recognized:
 * <ul>
 * <li><code>ceres.context</code> - the context ID used for the application, e.g. "beam". Mandatory.</li>
 * <li><code>${ceres.context}.home</code> - the application's home directory. If not given, the launcher will try to find one.</li>
 * <li><code>${ceres.context}.config</code> - path to a configuration file (Java properties file).</li>
 * <li><code>${ceres.context}.modules</code> - the application's modules directory, defaults to <code>modules</code>.</li>
 * <li><code>${ceres.context}.libDirs</code> - the application's library directories, defaults to <code>lib</code>.</li>
 * <li><code>${ceres.context}.mainClass</code> - the main class to be used, defaults to <code>com.bc.ceres.core.runtime.RuntimeLauncher</code> (which launches a Ceres module runtime).</li>
 * <li><code>${ceres.context}.classpath</code> - the main application classpath, defaults to {@code null}.</li>
 * <li><code>${ceres.context}.debug</code> - output extra debugging info.</li>
 * <li><code>${ceres.context}.logLevel</code> - the log level, for valid values see {@link java.util.logging.Level}.</li>
 * </ul>
 * With the exception of  <code>ceres.context</code> and <code>${ceres.context}.config</code> all properties can also be
 * specified in the configuration file. However, system properties always override configuration properties.
 */
public interface RuntimeConfig {
    /**
     * The Ceres context identifier, e.g. "beam".
     *
     * @return the context identifier.
     */
    String getContextId();

    /**
     * Gets the value of a configuration property with the name <code>${ceres.context}.<i>key</i></code>.
     * <p>
     * The method also substitues all occurences of <code>${<i>someKey</i>}</code> in the property value
     * with the value of <code><i>someKey</i></code>.
     *
     * @param key the context key
     * @return the property value or <code>null</code> if a configuration property with the name is undefined.
     */
    String getContextProperty(String key);

    /**
     * Gets the value of a configuration property with the name <code>${ceres.context}.<i>key</i></code>.
     * <p>
     * The method also substitues all occurences of <code>${<i>someKey</i>}</code> in the property value
     * with the value of <code><i>someKey</i></code>.
     *
     * @param key the context key
     * @param defaultValue the default value
     * @return the property value or the <code>defaultValue</code> if a configuration property with the name is undefined.
     */
    String getContextProperty(String key, String defaultValue);

    /**
     * The name of the class providing the main entry point.
     *
     * @return the name of the main class to be launched, may be {@code null}.
     * @see #getMainClassPath()
     */
    String getMainClassName();

    /**
     * An optional classpath containing the paths of additional application directories and ZIPs/JARs.
     * Path entries are separated by the system-specific path separator.
     * <p>
     * This classpath can be used to specify classes (e.g. the {@link #getMainClassName() main class}), that are neither contained in one of the {@link #getLibDirPaths() lib}
     * directories nor in the {@link #getModulesDirPath() modules} directory.
     * <p>
     * If specified, the main classpath will be the top-level classpath.
     *
     * @return the home directory path, may be {@code null}.
     * @since Ceres 0.11
     * @see #getMainClassName()
     */
    String getMainClassPath();

    /**
     * @return The identifier of the application or <code>null</code>
     *         if {@link #isUsingModuleRuntime() usingModuleRuntime} is <code>false</code>.
     */
    String getApplicationId();

    /**
     * @return <code>true</code> if a Ceres runtime will be started, <code>false</code> if a 'normal'
     *         <code>main</code> of the {@link #getMainClassName() main class} will be invoked.
     */
    boolean isUsingModuleRuntime();

    /**
     * The home directory path.
     *
     * @return home directory path.
     */
    String getHomeDirPath();

    /**
     * The configuration file path.
     *
     * @return configuration file path or <code>null</code> if not specified.
     */
    String getConfigFilePath();

    /**
     * The library directory paths.
     *
     * @return all library directory paths or an empty array if not specified.
     */
    String[] getLibDirPaths();

    /**
     * The modules directory path.
     *
     * @return modules directory path or <code>null</code> if not specified.
     */
    String getModulesDirPath();

    /**
     * Output debugging information?
     * @return <code>true</code> if so.
     */
    boolean isDebug();

    /**
     * Returns the logger.
     * @return the logger.
     */
    Logger getLogger();
}
