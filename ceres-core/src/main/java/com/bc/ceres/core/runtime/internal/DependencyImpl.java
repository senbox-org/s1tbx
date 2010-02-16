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
