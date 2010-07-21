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
import com.bc.ceres.core.runtime.Version;
import junit.framework.TestCase;

public class ModuleManagerTest extends TestCase {

    public void testInstallMultiVersionsOnRepo() throws CoreException {
        String[] installedModuleFileNames = {
                "xml/consistency/module-a-1.1.xml",
        };
        String[] repositoryModuleFileNames = {
                "xml/consistency/module-b-2.3.1.xml",
                "xml/consistency/module-b-2.3.2-needs-a-1.1.xml",
                "xml/consistency/module-b-2.4.xml",
        };
        ModuleManager moduleManager = TestHelpers.createModuleManager(installedModuleFileNames,
                                                          repositoryModuleFileNames);
        moduleManager.synchronizeWithRepository(ProgressMonitor.NULL);

        assertEquals(1, moduleManager.getInstalledModuleItems().length);
        assertEquals(0, moduleManager.getUpdatableModuleItems().length);
        assertEquals(1, moduleManager.getAvailableModuleItems().length);

        assertEquals("module-b",moduleManager.getAvailableModuleItems()[0].getModule().getSymbolicName());
        assertEquals(Version.parseVersion("2.4"),moduleManager.getAvailableModuleItems()[0].getModule().getVersion());

    }

    public void testUpdateMultiVersionsOnRepo() throws CoreException {
        String[] installedModuleFileNames = {
                "xml/consistency/module-a-1.0.xml",
        };
        String[] repositoryModuleFileNames = {
                "xml/consistency/module-a-1.0.1.xml",
                "xml/consistency/module-a-1.1.xml",
        };
        ModuleManager moduleManager = TestHelpers.createModuleManager(installedModuleFileNames,
                                                          repositoryModuleFileNames);
        moduleManager.synchronizeWithRepository(ProgressMonitor.NULL);

        assertEquals(1, moduleManager.getInstalledModuleItems().length);
        assertEquals(1, moduleManager.getUpdatableModuleItems().length);
        assertEquals(0, moduleManager.getAvailableModuleItems().length);

        assertEquals("module-a",moduleManager.getUpdatableModuleItems()[0].getModule().getSymbolicName());
        assertEquals(Version.parseVersion("1.0"),moduleManager.getUpdatableModuleItems()[0].getModule().getVersion());
        assertEquals(Version.parseVersion("1.1"),moduleManager.getUpdatableModuleItems()[0].getRepositoryModule().getVersion());
    }
}
