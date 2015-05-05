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
import com.bc.ceres.core.runtime.RuntimeConfigException;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import static org.junit.Assert.*;


public class RuntimeAppBTest {

    private RuntimeImpl runtime;

    @Before
    public void setUp() throws CoreException, RuntimeConfigException {
        System.setProperty("ceres.context", "appB");
        System.setProperty("appB.home", Config.getDirForAppB().toString());
        DefaultRuntimeConfig defaultRuntimeConfig = new DefaultRuntimeConfig();
        runtime = new RuntimeImpl(defaultRuntimeConfig, new String[0], ProgressMonitor.NULL);
        runtime.start();
    }

    @After
    public void tearDown() throws Exception {
        runtime.stop();
        runtime = null;
    }

    // TODO rq/mp - this test fails on Mac OS (rq-20140116)
    @Test
    public void testAllExpectedModulesPresent() {

        Module[] modules = runtime.getModules();
        assertNotNull(modules);
        assertTrue(modules.length >= 6);

        HashMap<String, Module> map = new HashMap<>(modules.length);
        for (Module module : modules) {
            map.put(module.getSymbolicName(), module);
        }
        assertNotNull(map.get("a-module-dir-with-classes"));
        assertNotNull(map.get("a-module-dir-with-jars"));
        assertNotNull(map.get("a-module-dir-with-jars-and-classes"));
        assertNotNull(map.get("a-spi-host-module-jar"));
        assertNotNull(map.get("a-spi-client-module-jar"));
        assertNotNull(map.get("a-native-module"));
        assertNotNull(map.get("an-empty-module-dir"));
        assertNotNull(map.get("an-empty-module-jar"));
        assertNotNull(map.get(Constants.SYSTEM_MODULE_NAME));

        Module nativeModule = map.get("a-native-module");
        if (nativeModule instanceof ModuleImpl) {
            ModuleImpl nativeModuleImpl = (ModuleImpl) nativeModule;
            String[] impliciteNativeLibs = nativeModuleImpl.getImpliciteNativeLibs();
            List<String> libNames = new ArrayList<>();
            for (String libPath : impliciteNativeLibs) {
                libNames.add(new File(libPath).getName());
            }
            assertTrue(libNames.contains(System.mapLibraryName("jhdf")));
            assertTrue(libNames.contains(System.mapLibraryName("jhdf5")));
        }

    }
}
