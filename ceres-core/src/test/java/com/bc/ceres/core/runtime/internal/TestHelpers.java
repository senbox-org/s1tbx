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

import junit.framework.Assert;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.ModuleState;

import java.io.IOException;
import java.net.URL;

import org.junit.Ignore;

@Ignore
public class TestHelpers {
    /**
     * Loads a module. Sets the location to the resiource URL and the ID to -1
     * @param resource the resource path
     * @return the module
     * @throws CoreException if the module could not be read
     */
    public static ModuleImpl parseModuleManifestWithDummyLocation(String resource) throws CoreException, IOException {
        URL url = TestHelpers.class.getResource(resource);
        ModuleImpl module = new ModuleManifestParser().parse(url.openStream());
        module.setLocation(url);
        return module;
    }

    /**
     * Loads a module. Sets the location to the resiource URL and the ID to -1
     * @param resource the resource path
     * @return the module
     * @throws CoreException if the module could not be read
     */
    public static ModuleImpl parseModuleManifest(String resource) throws CoreException, IOException {
        URL url = TestHelpers.class.getResource(resource);
        return new ModuleManifestParser().parse(url.openStream());
    }

    /**
     * Creates a registry form the given resources.
     * The module's IDs are subsequentially assigned starting from one (1).
     * All modules in the registry behave as if they where loaded by the {@link ModuleReader} or {@link ModuleLoader} class
     * but have not yet been resolved by the {@link ModuleResolver}.
     * @param resources
     * @return  the modules
     * @throws CoreException if the module registry could not be read
     */
    public static ModuleRegistry createModuleRegistry(String[] resources) throws CoreException, IOException {
        long moduleId = 1L;
        ModuleRegistry moduleRegistry = new ModuleRegistry();
        for (String resource : resources) {
            ModuleImpl module = parseModuleManifestWithDummyLocation(resource);
            module.setModuleId(moduleId);
            module.setState(ModuleState.INSTALLED);
            module.setImpliciteLibs(new String[0]);
            module.setImpliciteNativeLibs(new String[0]);
            moduleRegistry.registerModule(module);
            Assert.assertEquals(moduleId, module.getModuleId());
            Assert.assertSame(moduleRegistry, module.getRegistry());
            moduleId++;
        }
        Module[] modules = moduleRegistry.getModules();
        Assert.assertEquals(resources.length, modules.length);
        return moduleRegistry;
    }

    static void assertModuleIsInstalled(ModuleImpl module) {
        Assert.assertNotNull(module);
        Assert.assertEquals(ModuleState.INSTALLED, module.getState());
        Assert.assertNull(module.getClassLoader());
        Assert.assertNull(module.getActivator());
        Assert.assertEquals(0, module.getRefCount());
    }

    static void assertModuleIsResolved(ModuleImpl module, int expectedRefCount, ModuleImpl[] expectedModuleDependencies) {
        Assert.assertNotNull(module);
        Assert.assertEquals(ModuleState.RESOLVED, module.getState());
        Assert.assertNotNull(module.getClassLoader());
        Assert.assertNull(module.getActivator());
        Assert.assertNotNull(module.getModuleDependencies());
        Assert.assertEquals(expectedModuleDependencies.length, module.getModuleDependencies().length);
        for (int i = 0; i < expectedModuleDependencies.length; i++) {
            ModuleImpl expectedModuleDependency = expectedModuleDependencies[i];
            Assert.assertSame(expectedModuleDependency, module.getModuleDependencies()[i]);
        }
        Assert.assertEquals(expectedRefCount, module.getRefCount());
    }

    static void assertModuleIsActive(ModuleImpl module) {
        Assert.assertNotNull(module);
        Assert.assertEquals(ModuleState.ACTIVE, module.getState());
        Assert.assertNotNull(module.getClassLoader());
        Assert.assertNotNull(module.getActivator());
    }

    public static ModuleImpl getSingleton(ModuleRegistry moduleRegistry, String symbolicName) {
        ModuleImpl[] modules = moduleRegistry.getModules(symbolicName);
        Assert.assertNotNull(modules);
        Assert.assertEquals(1, modules.length);
        ModuleImpl module = modules[0];
        Assert.assertNotNull(module);
        return module;
    }
}
