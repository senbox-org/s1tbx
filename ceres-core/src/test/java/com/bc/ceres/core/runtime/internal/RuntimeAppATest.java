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
import com.bc.ceres.core.ProgressMonitor;
import com.bc.ceres.core.runtime.Constants;
import com.bc.ceres.core.runtime.Module;
import com.bc.ceres.core.runtime.RuntimeConfig;
import com.bc.ceres.core.runtime.RuntimeConfigException;
import junit.framework.TestCase;

import java.util.HashMap;


public class RuntimeAppATest extends TestCase {
    private RuntimeImpl runtime;

    @Override
    public void setUp() throws CoreException, RuntimeConfigException {
        System.setProperty("ceres.context", "appA");
        System.setProperty("appA.home", Config.getDirForAppA().toString());
        DefaultRuntimeConfig defaultRuntimeConfig = new DefaultRuntimeConfig();
        runtime = new RuntimeImpl(defaultRuntimeConfig, new String[0], ProgressMonitor.NULL);
        runtime.start();
    }

    @Override
    protected void tearDown() throws Exception {
        runtime.stop();
        runtime = null;
    }

    public void testLocations() {
        RuntimeConfig config = runtime.getRuntimeConfig();

        assertNull(config.getConfigFilePath());

        String homeDirPath = config.getHomeDirPath();
        assertNotNull(homeDirPath);
        assertTrue(homeDirPath.replace("\\", "/").endsWith("target/test-classes/testdirs/app-a"));

        String[] libDirPaths = config.getLibDirPaths();
        assertNotNull(libDirPaths);
        assertEquals(1, libDirPaths.length);
        assertTrue(libDirPaths[0].replace("\\", "/").endsWith("target/test-classes/testdirs/app-a/lib"));

        String modulesDirPath = config.getModulesDirPath();
        assertNotNull(modulesDirPath);
        assertTrue(modulesDirPath.replace("\\", "/").endsWith("target/test-classes/testdirs/app-a/modules"));
    }

    public void testAllExpectedModulesPresent() {

        Module[] modules = runtime.getModules();
        assertNotNull(modules);
        assertTrue(modules.length >= 6);

        HashMap<String, Module> map = new HashMap<String, Module>(modules.length);
        for (Module module : modules) {
            map.put(module.getSymbolicName(), module);
        }
        assertNotNull(map.get("module-a"));
        assertNotNull(map.get("module-b"));
        assertNotNull(map.get("module-c"));
        assertNotNull(map.get("module-d"));
        assertNotNull(map.get("module-e"));
        assertNotNull(map.get(Constants.SYSTEM_MODULE_NAME));
    }

    public void testSystemModule() {
        Module systemModule = runtime.getModule();
        assertNotNull(systemModule);
        assertEquals(0L, systemModule.getModuleId());
        assertEquals(Constants.SYSTEM_MODULE_NAME, systemModule.getSymbolicName());
    }
}
