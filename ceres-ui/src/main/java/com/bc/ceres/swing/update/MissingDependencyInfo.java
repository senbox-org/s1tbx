package com.bc.ceres.swing.update;

import com.bc.ceres.core.runtime.Dependency;
import com.bc.ceres.core.runtime.Module;

import java.util.ArrayList;
import java.util.List;

public class MissingDependencyInfo {

    private Dependency dependency;
    private List<Module> dependentModules;

    public MissingDependencyInfo(Dependency dependency) {
        this.dependency = dependency;
        dependentModules = new ArrayList<Module>(10);
    }

    public Dependency getDependency() {
        return dependency;
    }

    public void setDependency(Dependency dependency) {
        this.dependency = dependency;
    }

    public Module[] getDependentModules() {
        return dependentModules.toArray(new Module[dependentModules.size()]);
    }

    void addDependentModule(Module module) {
        dependentModules.add(module);
    }
}
