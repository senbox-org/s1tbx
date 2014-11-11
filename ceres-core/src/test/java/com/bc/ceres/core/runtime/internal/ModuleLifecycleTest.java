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

import junit.framework.TestCase;
import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.logging.Logger;

public class ModuleLifecycleTest extends TestCase {

    private static final File MODULES_DIR = new File(Config.getDirForAppA(), "modules");
    private ModuleRegistry moduleRegistry;

    @Override
    protected void setUp() throws Exception {
        this.moduleRegistry = createModuleRegistry();
    }

    @Override
    protected void tearDown() throws Exception {
        moduleRegistry = null;
    }

    public void testNothing() {
    }

    public void testModuleLifecycle() throws CoreException {

        TestHelpers.assertModuleIsInstalled(getModule("module-a"));
        TestHelpers.assertModuleIsInstalled(getModule("module-b"));
        TestHelpers.assertModuleIsInstalled(getModule("module-c"));
        TestHelpers.assertModuleIsInstalled(getModule("module-d"));
        TestHelpers.assertModuleIsInstalled(getModule("module-e"));

        resolveModules();

        TestHelpers.assertModuleIsResolved(getModule("module-a"), 6, new ModuleImpl[] {});
        TestHelpers.assertModuleIsResolved(getModule("module-b"), 3, new ModuleImpl[] {getModule("module-a")});
        TestHelpers.assertModuleIsResolved(getModule("module-c"), 1, new ModuleImpl[] {getModule("module-b")});
        TestHelpers.assertModuleIsResolved(getModule("module-d"), 2, new ModuleImpl[] {getModule("module-a")});
        TestHelpers.assertModuleIsResolved(getModule("module-e"), 1, new ModuleImpl[] {getModule("module-b"), getModule("module-d")});

        startModules();

        TestHelpers.assertModuleIsActive(getModule("module-a"));
        TestHelpers.assertModuleIsActive(getModule("module-b"));
        TestHelpers.assertModuleIsActive(getModule("module-c"));
        TestHelpers.assertModuleIsActive(getModule("module-d"));
        TestHelpers.assertModuleIsActive(getModule("module-e"));

        stopModules();

        TestHelpers.assertModuleIsResolved(getModule("module-a"), 6, new ModuleImpl[] {});
        TestHelpers.assertModuleIsResolved(getModule("module-b"), 3, new ModuleImpl[] {getModule("module-a")});
        TestHelpers.assertModuleIsResolved(getModule("module-c"), 1, new ModuleImpl[] {getModule("module-b")});
        TestHelpers.assertModuleIsResolved(getModule("module-d"), 2, new ModuleImpl[] {getModule("module-a")});
        TestHelpers.assertModuleIsResolved(getModule("module-e"), 1, new ModuleImpl[] {getModule("module-b"), getModule("module-d")});
    }

    private ModuleImpl getModule(String symbolicName) {
        return TestHelpers.getSingleton(this.moduleRegistry, symbolicName);
    }

    private void resolveModules() throws CoreException {
        ModuleImpl[] modules = moduleRegistry.getModules();
        for (ModuleImpl module : modules) {
            final ModuleResolver moduleResolver = new ModuleResolver(ModuleResolver.class.getClassLoader(), false);
            moduleResolver.resolve(module);
        }
    }

    private void startModules() throws CoreException {
        ModuleImpl[] modules = moduleRegistry.getModules();
        Arrays.sort(modules, new Comparator<ModuleImpl>() {
            public int compare(ModuleImpl o1, ModuleImpl o2) {
                return o1.getRefCount() - o2.getRefCount();
            }
        });
        for (ModuleImpl module : modules) {
            module.start();
        }
    }

    private void stopModules() throws CoreException {
        ModuleImpl[] modules = moduleRegistry.getModules();
        for (ModuleImpl module : modules) {
            module.stop();
        }
    }

    private ModuleRegistry createModuleRegistry() throws CoreException, IOException {
        ModuleImpl[] modules = new ModuleLoader(Logger.getLogger("")).loadModules(MODULES_DIR, ProgressMonitor.NULL);
        ModuleRegistry moduleRegistry = new ModuleRegistry();
        for (int i = 0; i < modules.length; i++) {
            ModuleImpl module = modules[i];
            module.setModuleId(i + 1);
            moduleRegistry.registerModule(module);
        }
        return moduleRegistry;
    }
}
