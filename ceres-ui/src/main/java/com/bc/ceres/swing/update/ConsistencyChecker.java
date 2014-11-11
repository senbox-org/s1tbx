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

import com.bc.ceres.core.runtime.Dependency;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.Version;
import com.bc.ceres.core.runtime.internal.ModuleImpl;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Can be used to check a {@link ModuleManager} for consistency.
 *
 * @since 0.6.1
 */
class ConsistencyChecker {

    private HashMap<String, MissingDependencyInfo> missingDependencies;
    private ModuleManager moduleManager;

    public ConsistencyChecker(ModuleManager moduleManager) {
        this.moduleManager = moduleManager;
    }

    public boolean check() {
        reset();
        Map<String, Module> moduleMap = getCurrentModuleMap();
        Collection<Module> modules = moduleMap.values();
        for (Module module : modules) {
            Dependency[] declaredDependencies = module.getDeclaredDependencies();
            for (Dependency dependency : declaredDependencies) {
                if (isMandatoryModuleDependency(dependency)) {
                    String requiredVersionStr = dependency.getVersion();
                    Version requiredVersion = null;
                    if (requiredVersionStr != null) {
                        requiredVersion = Version.parseVersion(requiredVersionStr);
                    }
                    Module dependencyModule = findModule(moduleMap, dependency.getModuleSymbolicName(),
                                                         requiredVersion);
                    if (dependencyModule == null) {
                        addMissingDependency(dependency, module);
                    }
                }
            }
        }
        return missingDependencies == null;
    }

    private Map<String, Module> getCurrentModuleMap() {
        Map<String, Module> modules = new HashMap<String, Module>(37);
        for (ModuleItem installedModuleItem : moduleManager.getInstalledModuleItems()) {
            if (installedModuleItem.getAction().equals(ModuleItem.Action.NONE)
                    || installedModuleItem.getAction().equals(ModuleItem.Action.UPDATE)) {
                ModuleImpl module = installedModuleItem.getModule();
                modules.put(module.getSymbolicName(), module);
            }
            if (installedModuleItem.getAction().equals(ModuleItem.Action.UPDATE)) {
                Module repositoryModule = installedModuleItem.getRepositoryModule();
                if (repositoryModule != null) {
                    modules.put(repositoryModule.getSymbolicName(), repositoryModule);
                }
            }

        }
        for (ModuleItem availableModuleItem : moduleManager.getAvailableModuleItems()) {
            if (availableModuleItem.getAction().equals(ModuleItem.Action.INSTALL)) {
                ModuleImpl module = availableModuleItem.getModule();
                modules.put(module.getSymbolicName(), module);
            }
        }
        return modules;
    }


    private void reset() {
        if (missingDependencies != null) {
            missingDependencies.clear();
            missingDependencies = null;
        }
    }

    public void addMissingDependency(Dependency dependency, Module dependentModule) {
        if (missingDependencies == null) {
            missingDependencies = new HashMap<String, MissingDependencyInfo>(10);
        }
        String dependencyKey = dependency.getModuleSymbolicName() + dependency.getVersion();
        if (missingDependencies.get(dependencyKey) == null) {
            MissingDependencyInfo dependencyInfo = new MissingDependencyInfo(dependency);
            missingDependencies.put(dependencyKey, dependencyInfo);
        }
        missingDependencies.get(dependencyKey).addDependentModule(dependentModule);
    }

    public MissingDependencyInfo[] getMissingDependencies() {
        if (missingDependencies == null) {
            return new MissingDependencyInfo[0];
        }
        return missingDependencies.values().toArray(new MissingDependencyInfo[missingDependencies.size()]);
    }


    private static boolean isMandatoryModuleDependency(Dependency dependency) {
        return dependency.getModuleSymbolicName() != null && !dependency.isOptional();
    }

    private static Module findModule(Map<String, Module> modules, String symbolicName, Version requiredVersion) {
        Module module = modules.get(symbolicName);
        if (requiredVersion == null) {
            return module;
        }
        if (module != null && module.getVersion().compareTo(requiredVersion) >= 0) {
            return module;
        }
        return null;
    }

}
