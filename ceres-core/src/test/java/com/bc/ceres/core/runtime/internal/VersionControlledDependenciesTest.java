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

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.runtime.Version;
import junit.framework.TestCase;

import java.io.IOException;

public class VersionControlledDependenciesTest extends TestCase {


    @Override
    protected void setUp() throws Exception {
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void testDependencyIsSmallerThanRequired() throws IOException, CoreException {
        ModuleRegistry moduleRegistry = TestHelpers.createModuleRegistry(new String[]{
                "xml/dependencies/versioned/module-base-1.0.xml",
                "xml/dependencies/versioned/module-a-needs-base-2.0.xml",
        });
        try {
            final ModuleResolver moduleResolver = new ModuleResolver(ModuleResolver.class.getClassLoader(), false);
            moduleResolver.resolve(moduleRegistry.getModules("module-a")[0]);
            fail("ResolveException expected, because module-a has dependency to module-base-2.0");
        } catch (ResolveException e) {
            // ignore
        }        
    }

    public void testDependencyIsBiggerThanRequired() throws IOException, CoreException {
        ModuleRegistry moduleRegistry = TestHelpers.createModuleRegistry(new String[]{
                "xml/dependencies/versioned/module-base-3.0.xml",
                "xml/dependencies/versioned/module-a-needs-base-2.0.xml",
        });
        try {
            final ModuleResolver moduleResolver = new ModuleResolver(ModuleResolver.class.getClassLoader(), false);
            ModuleImpl module = moduleRegistry.getModules("module-a")[0];
            moduleResolver.resolve(module);
            ModuleImpl dependency = module.getModuleDependencies()[0];
            assertEquals("module-base", dependency.getSymbolicName());
            assertEquals(new Version(3, 0, 0, ""), dependency.getVersion());
        } catch (ResolveException e) {
            fail("ResolveException not expected, because module has dependency to module-base-2.0 and 3.0 is available");
        }
    }

    public void testDependencyWithTwoModules() throws IOException, CoreException {
        ModuleRegistry moduleRegistry = TestHelpers.createModuleRegistry(new String[]{
                "xml/dependencies/versioned/module-base-3.0.xml",
                "xml/dependencies/versioned/module-a-needs-base-2.0.xml",
                "xml/dependencies/versioned/module-b-needs-base.xml",
        });
        final ModuleResolver moduleResolver = new ModuleResolver(ModuleResolver.class.getClassLoader(), false);
        try {
            ModuleImpl moduleA = moduleRegistry.getModules("module-a")[0];
            moduleResolver.resolve(moduleA);
            ModuleImpl dependencyOfA = moduleA.getModuleDependencies()[0];
            assertEquals("module-base", dependencyOfA.getSymbolicName());
            assertEquals(new Version(3, 0, 0, ""), dependencyOfA.getVersion());
        } catch (ResolveException e) {
            fail("ResolveException not expected, because " +
                 "module-a has dependency to module-base-2.0 and 3.0 is available ");
        }
        try {
            ModuleImpl moduleB = moduleRegistry.getModules("module-b")[0];
            moduleResolver.resolve(moduleB);
            ModuleImpl dependencyOfB = moduleB.getModuleDependencies()[0];
            assertEquals("module-base", dependencyOfB.getSymbolicName());
            assertEquals(new Version(3, 0, 0, ""), dependencyOfB.getVersion());
        } catch (ResolveException e) {
            fail("ResolveException not expected, because " +
                 "module-b has no version requirement.");
        }
    }

}
