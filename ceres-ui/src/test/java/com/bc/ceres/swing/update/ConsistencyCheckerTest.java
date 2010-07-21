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

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.Dependency;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.Version;
import junit.framework.TestCase;

import java.text.MessageFormat;

public class ConsistencyCheckerTest extends TestCase {

    public void testAGoodSetup() throws CoreException {
        String[] installedModuleFileNames = {
                "xml/consistency/module-a-1.1.xml",
                "xml/consistency/module-b-2.3.1.xml",
        };
        String[] repositoryModuleFileNames = {
                "xml/consistency/module-a-1.0.1-SNAPSHOT.xml",
                "xml/consistency/module-b-2.3.2-needs-a-1.1.xml",
                "xml/consistency/module-c-1.0-needs-b-2.3.2.xml",
        };
        ModuleManager moduleManager = TestHelpers.createModuleManager(installedModuleFileNames,
                                                          repositoryModuleFileNames);
        moduleManager.synchronizeWithRepository(ProgressMonitor.NULL);

        assertEquals(2, moduleManager.getInstalledModuleItems().length);
        assertEquals(1, moduleManager.getUpdatableModuleItems().length);
        assertEquals(1, moduleManager.getAvailableModuleItems().length);

        ConsistencyChecker checker = new ConsistencyChecker(moduleManager);

        findModuleItem(moduleManager.getAvailableModuleItems(), "module-c").setAction(ModuleItem.Action.INSTALL);
        findModuleItem(moduleManager.getUpdatableModuleItems(), "module-b").setAction(ModuleItem.Action.UPDATE);
        assertTrue(checker.check());
    }

    public void testWithDependencyToLowerVersionAsPresent() throws CoreException {
            String[] installedModuleFileNames = {
                    "xml/consistency/module-a-1.0.xml",
                    "xml/consistency/module-b-2.4.xml",
            };
            String[] repositoryModuleFileNames = {
                    "xml/consistency/module-a-1.2-needs-b-2.3.2.xml",
                    "xml/consistency/module-c-1.0-needs-b-2.3.2.xml",
            };
            ModuleManager moduleManager = TestHelpers.createModuleManager(installedModuleFileNames,
                                                              repositoryModuleFileNames);
            moduleManager.synchronizeWithRepository(ProgressMonitor.NULL);

            assertEquals(2, moduleManager.getInstalledModuleItems().length);
            assertEquals(1, moduleManager.getUpdatableModuleItems().length);
            assertEquals(1, moduleManager.getAvailableModuleItems().length);

            ConsistencyChecker checker = new ConsistencyChecker(moduleManager);

            findModuleItem(moduleManager.getAvailableModuleItems(), "module-c").setAction(ModuleItem.Action.INSTALL);
            findModuleItem(moduleManager.getUpdatableModuleItems(), "module-a").setAction(ModuleItem.Action.UPDATE);
            assertTrue(checker.check());
        }


    public void testWithMissingOptionalDependencies() throws CoreException {
        String[] installedModuleFileNames = {
                "xml/consistency/module-a-1.0.xml",
                "xml/consistency/module-b-2.3.1.xml",
        };
        String[] repositoryModuleFileNames = {
                "xml/consistency/module-b-2.3.2-needs-a-1.1-optional.xml",
                "xml/consistency/module-c-1.0-needs-b-2.3.2.xml",
        };
        ModuleManager moduleManager = TestHelpers.createModuleManager(installedModuleFileNames,
                                                          repositoryModuleFileNames);
        moduleManager.synchronizeWithRepository(ProgressMonitor.NULL);

        assertEquals(2, moduleManager.getInstalledModuleItems().length);
        assertEquals(1, moduleManager.getUpdatableModuleItems().length);
        assertEquals(1, moduleManager.getAvailableModuleItems().length);

        ConsistencyChecker checker = new ConsistencyChecker(moduleManager);

        findModuleItem(moduleManager.getUpdatableModuleItems(), "module-b").setAction(ModuleItem.Action.UPDATE);
        assertTrue(checker.check());

        findModuleItem(moduleManager.getAvailableModuleItems(), "module-c").setAction(ModuleItem.Action.INSTALL);
        assertTrue(checker.check());

    }

    public void testUninstall() throws CoreException {
        String[] installedModuleFileNames = {
                "xml/consistency/module-a-1.2-needs-b-2.3.2.xml",
                "xml/consistency/module-c-1.0-needs-b-2.3.2.xml",
                "xml/consistency/module-b-2.3.2.xml",
        };
        ModuleManager moduleManager = TestHelpers.createModuleManager(installedModuleFileNames,
                                                          new String[0]);
        moduleManager.synchronizeWithRepository(ProgressMonitor.NULL);

        assertEquals(3, moduleManager.getInstalledModuleItems().length);
        assertEquals(0, moduleManager.getUpdatableModuleItems().length);
        assertEquals(0, moduleManager.getAvailableModuleItems().length);

        ConsistencyChecker checker = new ConsistencyChecker(moduleManager);

        findModuleItem(moduleManager.getInstalledModuleItems(), "module-b").setAction(ModuleItem.Action.UNINSTALL);
        assertFalse(checker.check());

        findModuleItem(moduleManager.getInstalledModuleItems(), "module-a").setAction(ModuleItem.Action.UNINSTALL);
        findModuleItem(moduleManager.getInstalledModuleItems(), "module-c").setAction(ModuleItem.Action.UNINSTALL);
        assertTrue(checker.check());
    }

    public void testWithMissingDependencies() throws CoreException {
        String[] installedModuleFileNames = {
                "xml/consistency/module-a-1.0.xml",
                "xml/consistency/module-b-2.3.1.xml",
        };
        String[] repositoryModuleFileNames = {
                "xml/consistency/module-a-1.0.1-SNAPSHOT.xml",
                "xml/consistency/module-b-2.3.2-needs-a-1.1.xml",
                "xml/consistency/module-c-1.0-needs-b-2.3.2.xml",
        };
        ModuleManager moduleManager = TestHelpers.createModuleManager(installedModuleFileNames,
                                                          repositoryModuleFileNames);
        moduleManager.synchronizeWithRepository(ProgressMonitor.NULL);

        assertEquals(2, moduleManager.getInstalledModuleItems().length);
        assertEquals(2, moduleManager.getUpdatableModuleItems().length);
        assertEquals(1, moduleManager.getAvailableModuleItems().length);

        ConsistencyChecker checker = new ConsistencyChecker(moduleManager);

        findModuleItem(moduleManager.getAvailableModuleItems(), "module-c").setAction(ModuleItem.Action.INSTALL);
        assertFalse(checker.check());
        assertMissingDependency(checker, "module-b", "2.3.2", "module-c","1.0");

        findModuleItem(moduleManager.getUpdatableModuleItems(), "module-b").setAction(ModuleItem.Action.UPDATE);
        assertFalse(checker.check());
        assertMissingDependency(checker, "module-a", "1.1", "module-b","2.3.2");

    }

    private void assertMissingDependency(ConsistencyChecker checker,
                                         String dependencyName, String dependencyVersion,
                                         String declaredByName,
                                         String declaredByVersion) {
        MissingDependencyInfo dependencyInfo = findDependency(checker.getMissingDependencies(),
                                                                                 dependencyName,
                                                                                 dependencyVersion);
        if (dependencyInfo == null) {
            String message = MessageFormat.format("Missing dependency to {0}-{1} by {2}-{3} not detected",
                                                  dependencyName,
                                                  dependencyVersion,
                                                  declaredByName,
                                                  declaredByVersion);
            fail(message);
        }
        Module[] neededbyModules = dependencyInfo.getDependentModules();
        if(findModule(neededbyModules, declaredByName, declaredByVersion) == null) {
            String message = MessageFormat.format("Missing dependency to {0}-{1} is not declared by {2}-{3}",
                                                  dependencyName,
                                                  dependencyVersion,
                                                  declaredByName,
                                                  declaredByVersion);
            fail(message);
        }
    }

    private MissingDependencyInfo findDependency(
            MissingDependencyInfo[] missingDependencies,
            String moduleName,
            String version) {
        for (MissingDependencyInfo missingDependency : missingDependencies) {
            Dependency dependency = missingDependency.getDependency();
            if (dependency.getModuleSymbolicName().equals(moduleName) &&
                dependency.getVersion().equals(version)) {
                return missingDependency;
            }
        }
        return null;
    }

    private ModuleItem findModuleItem(ModuleItem[] items, String symbolicName) {
        for (ModuleItem item : items) {
            if (item.getModule().getSymbolicName().equals(symbolicName)) {
                return item;
            }
        }
        return null;
    }

    private Module findModule(Module[] modules, String symbolicName, String version) {
        for (Module module : modules) {
            if (module.getSymbolicName().equals(symbolicName) &&
                    module.getVersion().equals(Version.parseVersion(version))) {
                return module;
            }
        }
        return null;
    }


}