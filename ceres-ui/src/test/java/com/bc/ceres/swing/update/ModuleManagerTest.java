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
