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

package com.bc.ceres.launcher;

import com.bc.ceres.core.runtime.RuntimeConfigException;

import java.net.URL;

/**
 * Used to construct the classpath used by the launcher.
 * May be implemented by clients in order to construct a custom classpath.
 */
public interface ClasspathFactory {
    /**
     * Creates the classpath.
     *
     * @return the classpath which may be empty.
     * @throws com.bc.ceres.core.runtime.RuntimeConfigException
     *          If the configuration causes problems.
     */
    URL[] createClasspath() throws RuntimeConfigException;
}
