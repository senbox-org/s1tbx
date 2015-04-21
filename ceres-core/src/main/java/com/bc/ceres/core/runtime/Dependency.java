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

/**
 * An dependency declared in a module.
 * <p>
 *     If {@link #getDeclaringModule() declared} in a module manifest (module.xml), a dependency has the following syntax:
 * <pre>
 *    &lt;dependency&gt;
 *        &lt;module&gt;{@link #getModuleSymbolicName() moduleId}&lt;/module&gt;
 *        &lt;version&gt;{@link #getVersion() version}&lt;/version&gt;
 *    &lt;/dependency&gt;
 * </pre>
 * <p>
 * Or for libraries:
 * <pre>
 *    &lt;dependency&gt;
 *        &lt;lib&gt;{@link #getLibName() libName}&lt;/lib&gt;
 *        &lt;version&gt;{@link #getVersion() version}&lt;/version&gt;
 *    &lt;/dependency&gt;
 * </pre>
 * This interface is not intended to be implemented by clients.
 */
public interface Dependency {

    Module getDeclaringModule();

    String getLibName();

    String getModuleSymbolicName();

    String getVersion();

    boolean isOptional();
}
