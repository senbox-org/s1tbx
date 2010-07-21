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

package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.runtime.Dependency;
import com.bc.ceres.core.runtime.Module;

public class DependencyImpl implements Dependency {

    public static final Dependency[] EMPTY_ARRAY = new Dependency[0];

    private transient Module declaringModule;

    private String moduleSymbolicName; // IDE warning "private field never assigned" is ok
    private String libName; // IDE warning "private field never assigned" is ok
    private String version; // IDE warning "private field never assigned" is ok
    private boolean optional; // IDE warning "private field never assigned" is ok


    public Module getDeclaringModule() {
        return declaringModule;
    }

    public String getLibName() {
        return libName;
    }

    public String getModuleSymbolicName() {
        return moduleSymbolicName;
    }

    public String getVersion() {
        return version;
    }

    public boolean isOptional() {
        return optional;
    }

    void setDeclaringModule(Module declaringModule) {
        this.declaringModule = declaringModule;
    }

    // todo - test
    @Override
    public int hashCode() {
        if (libName != null) {
            return libName.hashCode();
        } else if (moduleSymbolicName != null) {
            return moduleSymbolicName.hashCode();
        }
        return super.hashCode();
    }

    // todo - test
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        } else if (obj == this) {
            return true;
        } else if (obj instanceof Dependency) {
            DependencyImpl other = (DependencyImpl) obj;
            if (libName != null) {
                return libName.equals(other.libName);
            } else if (moduleSymbolicName != null) {
                return moduleSymbolicName.equals(other.moduleSymbolicName);
            }
        }
        return false;
    }
}
