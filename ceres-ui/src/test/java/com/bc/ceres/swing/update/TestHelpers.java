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

import com.bc.ceres.core.*;
import com.bc.ceres.core.runtime.*;
import com.bc.ceres.core.runtime.internal.*;
import org.junit.Assert;
import org.junit.*;

import java.io.*;
import java.net.*;
import java.text.*;
import java.util.*;

@Ignore
public class TestHelpers {

    public static Module newRepositoryModuleMock(String name, String version, ModuleState state) throws CoreException {
        return newModuleImpl(name, version, state);
    }

    public static ModuleItem newModuleItemMock(String name, String version, ModuleState state) throws CoreException {
        return new ModuleItem(newModuleImpl(name, version, state));
    }

    private static ModuleImpl newModuleImpl(String name, String version, ModuleState state) throws CoreException {
        ModuleImpl module = null;
        String resource = "xml/" + name + ".xml";
        try {
            module = loadModule(resource);
        } catch (IOException e) {
            e.printStackTrace();
            Assert.fail(resource + ": " + e.getMessage());
        }
        module.setVersion(Version.parseVersion(version));
        module.setState(state);
        return module;
    }

    public static ModuleImpl loadModule(String resource) throws IOException, CoreException {
        URL url = ModuleSyncRunnerTest.class.getResource(resource);
        if (url == null) {
            Assert.fail("resource not found: " + resource);
        }
        ModuleImpl module = new ModuleManifestParser().parse(url.openStream());
        module.setLocation(url);
        return module;
    }

    static ModuleManager createModuleManager(final String[] installedResourcePaths,
                                              final String[] repositoryResourcePaths) {
        return new DefaultModuleManager((ModuleContext) null) {
            private ModuleImpl[] installedModules;
            private Module[] repositoryModules;


            @Override
            public Module[] getInstalledModules() {
                if (installedModules == null) {
                    installedModules = createModules(installedResourcePaths);
                    for (ModuleImpl installedModule : installedModules) {
                        installedModule.setState(ModuleState.ACTIVE);
                    }
                }
                return installedModules;
            }

            @Override
            public Module[] getRepositoryModules(ProgressMonitor pm) throws CoreException {
                if (repositoryModules == null) {
                    repositoryModules = createModules(repositoryResourcePaths);
                }
                return repositoryModules;
            }


            private ModuleImpl[] createModules(String[] moduleResourcePaths) {
                ArrayList<ModuleImpl> moduleList = new ArrayList<ModuleImpl>(moduleResourcePaths.length);
                for (String resourcePath : moduleResourcePaths) {
                    moduleList.add(createModule(resourcePath));
                }
                return moduleList.toArray(new ModuleImpl[moduleList.size()]);
            }

            private ModuleImpl createModule(String moduleResourcePath) {
                ModuleImpl module = null;
                try {
                    module = loadModule(moduleResourcePath);
                } catch (Exception e) {
                    String msgPattern = "Not able to load module descriptor for [{0}] - {1}";
                    Assert.fail(MessageFormat.format(msgPattern, moduleResourcePath, e.getMessage()));
                }
                return module;
            }

        };
    }
}
