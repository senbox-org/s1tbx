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

import com.bc.ceres.core.runtime.ModuleState;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.Version;
import com.bc.ceres.core.CoreException;
import junit.framework.TestCase;

public class ModuleSyncRunnerTest extends TestCase {

    public void testNullArgs() {
        try {
            ModuleSyncRunner.sync(null, new Module[0]);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            // ok
        }

        try {
            ModuleSyncRunner.sync(new ModuleItem[0], null);
            fail("NullPointerException expected");
        } catch (NullPointerException e) {
            // ok
        }
    }

    public void testEmptyModules() {
        ModuleItem[] moduleItems = sync(new ModuleItem[0], new Module[0]);
        testLength(moduleItems, 0);
    }

    public void testOneInstalledModule() throws CoreException {
        ModuleItem[] moduleItems = sync(new ModuleItem[]{
                TestHelpers.newModuleItemMock("module-a", "1.0", ModuleState.INSTALLED)
        }, new Module[]{
        });
        testLength(moduleItems, 0);
    }

    public void testOneInstalledAndOneAvailableModule_DifferentIdsSameVersions() throws CoreException {
        ModuleItem[] installedModuleItems = new ModuleItem[]{
                TestHelpers.newModuleItemMock("module-a", "1.0", ModuleState.INSTALLED)
        };
        Module[] repositoryModules = new Module[]{TestHelpers.newRepositoryModuleMock("module-b", "1.0",
                                                                                                          ModuleState.NULL)};
        ModuleItem[] moduleItems = sync(installedModuleItems, repositoryModules);
        testLength(moduleItems, 1);
        testModule(moduleItems[0], "module-b", "1.0", ModuleState.NULL, repositoryModules[0]);
    }

    public void testOneInstalledAndOneAvailableModule_SameIdsSameVersions() throws CoreException {
        ModuleItem[] moduleItems = sync(new ModuleItem[]{
                TestHelpers.newModuleItemMock("module-a", "1.0", ModuleState.INSTALLED)
        }, new Module[]{
                TestHelpers.newRepositoryModuleMock("module-a", "1.0", ModuleState.NULL)
        });
        testLength(moduleItems, 0);
    }

    public void testOneInstalledAndOneAvailableModule_SameIdsDiffVersions() throws CoreException {
        ModuleItem[] installedModuleItems = new ModuleItem[]{
                TestHelpers.newModuleItemMock("module-a", "1.0", ModuleState.INSTALLED)
        };
        Module[] availableModuleItems = new Module[]{
                TestHelpers.newRepositoryModuleMock("module-a", "1.1", ModuleState.NULL)
        };

        ModuleItem[] moduleItems = sync(installedModuleItems, availableModuleItems);
        testLength(moduleItems, 0);
        testModule(installedModuleItems[0], "module-a", "1.0", ModuleState.INSTALLED, availableModuleItems[0]);
    }

    private static ModuleItem[] sync(ModuleItem[] installedModuleItems, Module[] repositoryModules) {
        return ModuleSyncRunner.sync(
                installedModuleItems,
                repositoryModules
        );
    }

    public void testTwoInstalledAndTwoAvailableModules_OneCanBeUpdated() throws CoreException {
        ModuleItem[] installedModuleItems = new ModuleItem[]{
                TestHelpers.newModuleItemMock("module-a", "1.0", ModuleState.INSTALLED),
                TestHelpers.newModuleItemMock("module-c", "1.0", ModuleState.INSTALLED)
        };
        Module[] availableModuleItems = new Module[]{
                TestHelpers.newRepositoryModuleMock("module-b", "1.0", ModuleState.NULL),
                TestHelpers.newRepositoryModuleMock("module-a", "1.1", ModuleState.NULL)
        };
        ModuleItem[] moduleItems = sync(installedModuleItems, availableModuleItems);
        testLength(moduleItems, 1);
        testModule(moduleItems[0], "module-b", "1.0", ModuleState.NULL, availableModuleItems[0]);

        testModule(installedModuleItems[0], "module-a", "1.0", ModuleState.INSTALLED, availableModuleItems[1]);
        testModule(installedModuleItems[1], "module-c", "1.0", ModuleState.INSTALLED, null);

    }

    public void testTwoInstalledAndTwoAvailableModules_OneHasHigherOtherHasLowerVersion() throws CoreException {
        ModuleItem[] installedModuleItems = new ModuleItem[]{
                TestHelpers.newModuleItemMock("module-a", "1.0", ModuleState.INSTALLED),
                TestHelpers.newModuleItemMock("module-b", "1.0", ModuleState.INSTALLED)
        };
        Module[] availableModuleItems = new Module[]{
                TestHelpers.newRepositoryModuleMock("module-b", "0.9", ModuleState.NULL),
                TestHelpers.newRepositoryModuleMock("module-a", "1.1", ModuleState.NULL)
        };
        ModuleItem[] moduleItems = sync(installedModuleItems, availableModuleItems);
        testLength(moduleItems, 0);
        testModule(installedModuleItems[0], "module-a", "1.0", ModuleState.INSTALLED, availableModuleItems[1]);
        testModule(installedModuleItems[1], "module-b", "1.0", ModuleState.INSTALLED, null);
    }

    public void testThatUninstalledModulesAreShownAsAvailable() throws CoreException {
        ModuleItem[] installedModuleItems = new ModuleItem[]{
                TestHelpers.newModuleItemMock("module-a", "1.0", ModuleState.INSTALLED),
                TestHelpers.newModuleItemMock("module-b", "1.0", ModuleState.UNINSTALLED)
        };
        Module[] availableModuleItems = new Module[]{
                TestHelpers.newRepositoryModuleMock("module-b", "0.9", ModuleState.NULL),
                TestHelpers.newRepositoryModuleMock("module-a", "1.1", ModuleState.NULL)
        };
        ModuleItem[] moduleItems = sync(installedModuleItems, availableModuleItems);
        testLength(moduleItems, 0);
// todo - [UNINSTALL question!]
//        testLength(moduleItems, 1);
//        testModule(moduleItems[0], "module-b", "0.9", ModuleState.NULL, availableModuleItems[0]);
//
//        testModule(installedModuleItems[0], "module-a", "1.0", ModuleState.INSTALLED, availableModuleItems[1]);
//        testModule(installedModuleItems[1], "module-b", "1.0", ModuleState.UNINSTALLED, null);

    }

    public void testThatLatestUpdateIsSet() throws CoreException {
        ModuleItem[] installedModuleItems = new ModuleItem[]{
                TestHelpers.newModuleItemMock("module-a", "1.0", ModuleState.INSTALLED),
                TestHelpers.newModuleItemMock("module-b", "1.0", ModuleState.INSTALLED)
        };
        Module[] availableModuleItems = new Module[]{
                TestHelpers.newRepositoryModuleMock("module-b", "0.9", ModuleState.NULL),
                TestHelpers.newRepositoryModuleMock("module-b", "1.5", ModuleState.NULL),
                TestHelpers.newRepositoryModuleMock("module-b", "2.0", ModuleState.NULL),  // This is the one we want!
                TestHelpers.newRepositoryModuleMock("module-b", "1.2", ModuleState.NULL),
        };
        ModuleItem[] moduleItems = sync(installedModuleItems, availableModuleItems);
        testLength(moduleItems, 0);

        testModule(installedModuleItems[0], "module-a", "1.0", ModuleState.INSTALLED, null);
        testModule(installedModuleItems[1], "module-b", "1.0", ModuleState.INSTALLED, availableModuleItems[2]);
    }

    public void testTwoUninstalledAndTwoAvailables_SameIdsSameVersions() throws CoreException {
        ModuleItem[] installedModuleItems = new ModuleItem[]{
                TestHelpers.newModuleItemMock("module-a", "1.0", ModuleState.UNINSTALLED),
                TestHelpers.newModuleItemMock("module-b", "1.0", ModuleState.UNINSTALLED)
        };
        Module[] repositoryModules = new Module[]{
                TestHelpers.newRepositoryModuleMock("module-b", "1.0", ModuleState.NULL),
                TestHelpers.newRepositoryModuleMock("module-a", "1.0", ModuleState.NULL)
        };

        ModuleItem[] moduleItems = sync(installedModuleItems, repositoryModules);

        testLength(moduleItems, 0);
// todo - [UNINSTALL question!]
//        testLength(moduleItems, 2);
//        testModule(moduleItems[0], "module-b", "1.0", ModuleState.NULL, repositoryModules[0]);
//        testModule(moduleItems[1], "module-a", "1.0", ModuleState.NULL, repositoryModules[1]);
//
//        testModule(installedModuleItems[0], "module-a", "1.0", ModuleState.UNINSTALLED, null);
//        testModule(installedModuleItems[1], "module-b", "1.0", ModuleState.UNINSTALLED, null);
    }

    public void testTwoUninstalledAndTwoAvailables_SameIdsDiffVersions() throws CoreException {
        ModuleItem[] installedModuleItems = new ModuleItem[]{
                TestHelpers.newModuleItemMock("module-a", "1.0", ModuleState.UNINSTALLED),
                TestHelpers.newModuleItemMock("module-b", "1.0", ModuleState.UNINSTALLED)
        };
        Module[] repositoryModules = new Module[]{
                TestHelpers.newRepositoryModuleMock("module-b", "1.0", ModuleState.NULL),
                TestHelpers.newRepositoryModuleMock("module-a", "1.1", ModuleState.NULL)
        };
        ModuleItem[] moduleItems = sync(installedModuleItems, repositoryModules);
        testLength(moduleItems, 0);
// todo - [UNINSTALL question!]
//        testLength(moduleItems, 2);
//        testModule(moduleItems[0], "module-b", "1.0", ModuleState.NULL, repositoryModules[0]);
//        testModule(moduleItems[1], "module-a", "1.1", ModuleState.NULL, repositoryModules[1]);
//        testModule(installedModuleItems[0], "module-a", "1.0", ModuleState.UNINSTALLED, null);
//        testModule(installedModuleItems[1], "module-b", "1.0", ModuleState.UNINSTALLED, null);
    }

    private void testLength(ModuleItem[] moduleItems, int expectedLength) {
        assertNotNull(moduleItems);
        assertEquals(expectedLength, moduleItems.length);
    }

    private void testModule(ModuleItem moduleItem, String expectedId, String expectedVersion,
                            ModuleState expectedStatus, Module expectedUpdate) {
        assertEquals(expectedId, moduleItem.getModule().getSymbolicName());
        assertEquals(Version.parseVersion(expectedVersion), moduleItem.getModule().getVersion());
        assertEquals(expectedStatus, moduleItem.getModule().getState());
        assertSame(expectedUpdate, moduleItem.getRepositoryModule());
    }
}
