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
import org.junit.Test;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Logger;

import static org.junit.Assert.*;

/**
 * Unit test for {@link ModuleLoader}
 */
public class ModuleScannerTest {


    @Test(expected = NullPointerException.class)
    public void testLoggerIsNull() throws IOException, CoreException {
        new ModuleLoader(null);
    }

    @Test(expected = NullPointerException.class)
    public void testClassLoaderIsNull() throws IOException, CoreException {
        ModuleLoader moduleLoader = new ModuleLoader(Logger.getAnonymousLogger());

        moduleLoader.loadModules((ClassLoader) null, ProgressMonitor.NULL);
    }

    @Test(expected = NullPointerException.class)
    public void testModuleDirIsNull() throws IOException, CoreException {
        ModuleLoader moduleLoader = new ModuleLoader(Logger.getAnonymousLogger());

        moduleLoader.loadModules((File) null, ProgressMonitor.NULL);
    }

    @Test(expected = NullPointerException.class)
    public void testProgressMonitorIsNull() throws IOException, CoreException {
        ModuleLoader moduleLoader = new ModuleLoader(Logger.getAnonymousLogger());

        moduleLoader.loadModules(getClass().getClassLoader(), null);
    }

    @Test
    public void testClassPathScanner() throws IOException, CoreException {
        File modulesDir = new File(Config.getDirForAppB(), "modules");
        URL[] urls = getDirEntryUrls(modulesDir);
        URLClassLoader urlClassLoader = new URLClassLoader(urls, String.class.getClassLoader());

        ModuleImpl[] modules = new ModuleLoader(Logger.getAnonymousLogger()).loadModules(urlClassLoader,
                                                                                         ProgressMonitor.NULL);
        testAppBContents(modulesDir, modules);
    }

    @Test
    public void testDirectoryScanner() throws IOException, CoreException {
        File modulesDir = new File(Config.getDirForAppB(), "modules");

        ModuleImpl[] modules = new ModuleLoader(Logger.getAnonymousLogger()).loadModules(modulesDir,
                                                                                         ProgressMonitor.NULL);
        testAppBContents(modulesDir, modules);
    }

    private static void testAppBContents(File modulesDir, ModuleImpl[] modules) throws IOException {
        assertNotNull(modules);

        Map<String, ModuleImpl> map = toMap(modules);

        testModule(map, modulesDir,
                   "an-empty-module-dir",
                   "an-empty-module-dir",
                   new String[0],
                   new String[0]);
        testModule(map, modulesDir,
                   "a-module-dir-with-classes",
                   "a-module-dir-with-classes",
                   new String[0],
                   new String[0]);
        testModule(map, modulesDir,
                   "a-module-dir-with-jars",
                   "a-module-dir-with-jars",
                   new String[]{
                           "lib/lib-5.jar",
                           "lib/lib-6.jar",
                   },
                   new String[0]
        );
        testModule(map, modulesDir,
                   "a-module-dir-with-jars-and-classes",
                   "a-module-dir-with-jars-and-classes",
                   new String[]{
                           "lib/lib-3.jar",
                           "lib/lib-4.jar",
                   },
                   new String[0]
        );
        testModule(map, modulesDir,
                   "an-empty-module-jar",
                   "an-empty-module-jar.jar",
                   new String[0],
                   new String[0]);
        testModule(map, modulesDir,
                   "a-native-module",
                   "a-native-module",
                   new String[]{
                           "lib/jhdf.jar",
                           "lib/jhdf5.jar"
                   },
                   new String[]{
                           "lib/native/" + System.mapLibraryName("jhdf"),
                           "lib/native/" + System.mapLibraryName("jhdf5")
                   }
        );
    }

    private static void testModule(Map<String, ModuleImpl> map,
                                   File modulesDir,
                                   String moduleId,
                                   String fileName,
                                   String[] expectedImpliciteLibs,
                                   String[] expectedImpliciteNativeLibs) throws IOException {
        ModuleImpl module = map.get(moduleId);
        assertNotNull(module);

        URL expectedLocation = new File(modulesDir, fileName).toURI().toURL();
        assertEquals(expectedLocation, module.getLocation());


        HashSet<String> actualSet = new HashSet<>(10);

        String[] impliciteLibs = module.getImpliciteLibs();
        assertNotNull(impliciteLibs);
        Collections.addAll(actualSet, impliciteLibs);
        for (String expectedEntry : expectedImpliciteLibs) {
            assertTrue("missing implicit lib [" + expectedEntry + "]", actualSet.contains(expectedEntry));
        }

        actualSet.clear();
        String[] impliciteNativeLibs = module.getImpliciteNativeLibs();
        assertNotNull(impliciteNativeLibs);
        Collections.addAll(actualSet, impliciteNativeLibs);
        for (String expectedEntry : expectedImpliciteNativeLibs) {
            assertTrue("missing native lib [" + expectedEntry + "]", actualSet.contains(expectedEntry));
        }
    }

    private static Map<String, ModuleImpl> toMap(ModuleImpl[] modules) {
        HashMap<String, ModuleImpl> map = new HashMap<>();
        for (ModuleImpl module : modules) {
            map.put(module.getSymbolicName(), module);
        }
        return map;
    }


    private static URL[] getDirEntryUrls(File modulesDir) throws MalformedURLException {
        String[] dirs = new DirScanner(modulesDir).scan(new FilenameFilter() {
            public boolean accept(File dir, String name) {
                return true;
            }
        });
        URL[] urls = new URL[dirs.length];
        for (int i = 0; i < dirs.length; i++) {
            urls[i] = new File(modulesDir, dirs[i]).toURI().toURL();
        }
        return urls;
    }

}
