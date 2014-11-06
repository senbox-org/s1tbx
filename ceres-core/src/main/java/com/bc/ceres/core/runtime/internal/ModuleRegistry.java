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

import com.bc.ceres.core.Assert;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Extension;
import com.bc.ceres.core.runtime.ExtensionPoint;
import com.bc.ceres.core.runtime.Module;

import java.net.URL;
import java.text.MessageFormat;
import java.util.*;

/**
 * A registry for {@link ModuleImpl}s and their extension points.
 * It defines a context for resolving module dependencies.
 */
public class ModuleRegistry {

    private Map<Long, ModuleImpl> idToModuleMap;
    private Map<URL, ModuleImpl> locationToModuleMap;
    private Map<String, Object> symbolicNameToModulesMap;  // maps to either a ModuleImpl or a List<ModuleImpl>
    private Map<String, ExtensionPoint> extensionPointMap;

    /**
     * Construct a new module registry.
     */
    public ModuleRegistry() {
        this.idToModuleMap = new HashMap<Long, ModuleImpl>(33);
        this.locationToModuleMap = new HashMap<URL, ModuleImpl>(33);
        this.symbolicNameToModulesMap = new HashMap<String, Object>(33);
        this.extensionPointMap = new HashMap<String, ExtensionPoint>(33);
    }

    /**
     * Registers the given module with this registry.
     *
     * @param module the module to register
     */
    public void registerModule(ModuleImpl module) throws CoreException {
        Assert.notNull(module, "module");
        Assert.notNull(module.getSymbolicName(), "module.getId()");
        Assert.notNull(module.getLocation(), "module.getLocation()");

        // Make sure that ID is not already used
        if (idToModuleMap.containsKey(module.getModuleId())) {
            throw new CoreException(MessageFormat.format("Duplicate module identifier [{0}]", module.getModuleId()));
        }
        // Make sure that symbolicName AND version does not already exist
        if (symbolicNameToModulesMap.containsKey(module.getSymbolicName())) {
            ModuleImpl[] modules = getModules(module.getSymbolicName());
            for (ModuleImpl mod : modules) {
                if (mod.getVersion().equals(module.getVersion())) {
                    throw new CoreException(
                            MessageFormat.format("Module with symbolic name [{0}] and version [{1}] already registered",
                                                 module.getSymbolicName(), module.getVersion()));
                }
            }
        }
        // Make sure that loocation is not already used
        if (locationToModuleMap.containsKey(module.getLocation())) {
            throw new CoreException(MessageFormat.format("Duplicate module location [{0}]", module.getLocation()));
        }

        // Make sure that extension points are not registered twice within same module
        // Note that it is possible that a module with same symbolicName but different version (module update)
        // may want to register existing extension points. This will not lead to an exception here, but such
        // extension points are not registered.
        ExtensionPoint[] extensionPoints = module.getExtensionPoints();
        for (ExtensionPoint extensionPoint : extensionPoints) {
            ExtensionPoint oldExtensionPoint = extensionPointMap.get(extensionPoint.getQualifiedId());
            if (oldExtensionPoint != null && oldExtensionPoint.getDeclaringModule() == module) {
                throw new CoreException(String.format("Module [%s]: Duplicate extension point identifier [%s]",
                                                      module.getSymbolicName(), extensionPoint.getId()));
            }
        }

        registerModuleId(module);
        registerLocation(module);
        registerSymbolicName(module);
        module.setRegistry(this);

        for (ExtensionPoint extensionPoint : extensionPoints) {
            ExtensionPoint oldExtensionPoint = extensionPointMap.get(extensionPoint.getQualifiedId());
            // Only register extension point if not already done, see comment above.
            if (oldExtensionPoint == null) {
                extensionPointMap.put(extensionPoint.getQualifiedId(), extensionPoint);
            }
        }
    }

    /**
     * Gets the module for the given module identifier.
     *
     * @param moduleId the module identifier
     * @return the module or <code>null</code> if not found
     */
    public ModuleImpl getModule(long moduleId) {
        Assert.argument(moduleId >= 0, "moduleId >= 0");
        return idToModuleMap.get(moduleId);
    }


    /**
     * Gets the module for the given module identifier.
     *
     * @param url the module location
     * @return the module or <code>null</code> if not found
     */
    public ModuleImpl getModule(URL url) {
        Assert.notNull(url, "url");
        return locationToModuleMap.get(url);
    }

    /**
     * Gets the module for the given module identifier.
     *
     * @param symbolicName the module's symbolic name
     * @return the module or <code>null</code> if not found
     */
    public ModuleImpl[] getModules(String symbolicName) {
        Assert.notNull(symbolicName, "symbolicName");
        Object o = symbolicNameToModulesMap.get(symbolicName);
        if (o == null) {
            return new ModuleImpl[0];
        } else if (o instanceof ModuleImpl) {
            ModuleImpl module = (ModuleImpl) o;
            return new ModuleImpl[]{module};
        } else if (o instanceof List) {
            List<ModuleImpl> modules = (List<ModuleImpl>) o;
            return modules.toArray(ModuleImpl.EMPTY_ARRAY);
        } else {
            throw new IllegalStateException();
        }
    }

    /**
     * Gets all modules registered so far.
     *
     * @return the modules registered so far.
     */
    public ModuleImpl[] getModules() {
        Collection<ModuleImpl> modules = idToModuleMap.values();
        return modules.toArray(ModuleImpl.EMPTY_ARRAY);
    }

    /**
     * Gets the extension point for the given extension point identifier.
     *
     * @param extensionPointId the qualified extension point identifier in the form <code>&lt;moduleId&gt;:&lt;extensionPointId&gt;</code>.
     * @return the extension point or <code>null</code> if not found
     */
    public ExtensionPoint getExtensionPoint(String extensionPointId) {
        if (extensionPointId == null) {
            throw new NullPointerException("extensionPointId");
        }
        return extensionPointMap.get(extensionPointId);
    }

    /**
     * Gets the extension point for the given extension point identifier. The  declaring module
     * is used to search the dependency tree for inherited extension points, if a point with the given fully qualified
     * name has not been registered directly.
     *
     * @param extensionPointId the qualified extension point identifier in the form <code>&lt;moduleId&gt;:&lt;extensionPointId&gt;</code>.
     * @param declaringModule the declaring module
     * @return the extension point or <code>null</code> if not found
     */
    public ExtensionPoint getExtensionPoint(String extensionPointId, ModuleImpl declaringModule) {
        ExtensionPoint point = getExtensionPoint(extensionPointId);
        if (point == null) {
            String extensionPointIdSimple = extensionPointId;
            final int index = extensionPointId.indexOf(':');
            if (index > 0) {
                extensionPointIdSimple = extensionPointId.substring(index + 1);
            }
            point = findExtensionPoint(declaringModule, extensionPointIdSimple);
        }
        return point;
    }


    /**
     * Gets all extension points defined so far.
     *
     * @return the extension points
     */
    public ExtensionPoint[] getExtensionPoints() {
        Collection<ExtensionPoint> extensionPoints = extensionPointMap.values();
        return extensionPoints.toArray(new ExtensionPoint[0]);
    }

    /**
     * Gets all extensions for the given extension point identifier.
     *
     * @param extensionPointId the qualified extension point identifier in the form <code>&lt;moduleId&gt;:&lt;extensionPointId&gt;</code>.
     * @return the extension points
     */
    public Extension[] getExtensions(String extensionPointId) {
        ExtensionPoint extensionPoint = getExtensionPoint(extensionPointId);
        ArrayList<Extension> list = new ArrayList<Extension>(16);
        if (extensionPoint != null) {
            // Declaring module shall contribute first, ...
            Module declaringModule = extensionPoint.getDeclaringModule();
            collectExtensions(declaringModule, extensionPoint, list);
            Module[] modules = getModules();
            for (Module module : modules) {
                // ... then all other modules contribute.
                if (module != declaringModule) {
                    collectExtensions(module, extensionPoint, list);
                }
            }
        }
        return list.toArray(new Extension[0]);
    }

    private void collectExtensions(Module module, ExtensionPoint extensionPoint, ArrayList<Extension> extensionList) {
        Extension[] extensions = module.getExtensions();
        for (Extension extension : extensions) {
            if (extension.getExtensionPoint() == extensionPoint) {
                extensionList.add(extension);
            }
        }
    }


    private void registerModuleId(ModuleImpl module) {
        idToModuleMap.put(module.getModuleId(), module);
    }

    private void registerLocation(ModuleImpl module) {
        locationToModuleMap.put(module.getLocation(), module);
    }

    private void registerSymbolicName(ModuleImpl module) {
        Object o = symbolicNameToModulesMap.get(module.getSymbolicName());
        if (o == null) {
            symbolicNameToModulesMap.put(module.getSymbolicName(), module);
        } else if (o instanceof ModuleImpl) {
            List<Module> arrayList = new ArrayList<Module>(3);
            arrayList.add((ModuleImpl) o);
            arrayList.add(module);
            symbolicNameToModulesMap.put(module.getSymbolicName(), arrayList);
        } else if (o instanceof List) {
            ((List<Module>) o).add(module);
        } else {
            throw new IllegalStateException();
        }
    }


    private static ExtensionPoint findExtensionPoint(ModuleImpl module, String extensionPointIdSimple) {
        final ModuleImpl[] dependencies = module.getModuleDependencies();
        if (dependencies == null) {
            return null;
        }
        for (ModuleImpl dependency : dependencies) {
            final ExtensionPoint point1 = dependency.getExtensionPoint(extensionPointIdSimple);
            if (point1 != null) {
                return point1;
            }
        }
        for (ModuleImpl dependency : dependencies) {
            final ExtensionPoint point1 = findExtensionPoint(dependency, extensionPointIdSimple);
            if (point1 != null) {
                return point1;
            }
        }
        return null;
    }
}
