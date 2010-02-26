package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.CoreException;
import com.bc.ceres.core.ProgressMonitor;
import junit.framework.TestCase;

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

/**
 * Unit test for {@link ModuleLoader}
 */
public class ModuleScannerTest
        extends TestCase {

    static {

    }

    public void testNullArgConvention() throws IOException, CoreException {
        try {
            new ModuleLoader(null);
            fail();
        } catch (NullPointerException e) {
        }

        try {
            new ModuleLoader(Logger.getAnonymousLogger()).loadModules((ClassLoader) null, ProgressMonitor.NULL);
            fail();
        } catch (NullPointerException e) {
        }

        try {
            new ModuleLoader(Logger.getAnonymousLogger()).loadModules((File) null, ProgressMonitor.NULL);
            fail();
        } catch (NullPointerException e) {
        }

        try {
            new ModuleLoader(Logger.getAnonymousLogger()).loadModules(getClass().getClassLoader(), null);
            fail();
        } catch (NullPointerException e) {
        }
    }

    public void testClassPathScanner() throws IOException, CoreException {
        File modulesDir = new File(Config.getDirForAppB(), "modules");
        URL[] urls = getDirEntryUrls(modulesDir);
        URLClassLoader urlClassLoader = new URLClassLoader(urls, String.class.getClassLoader());

        ModuleImpl[] modules = new ModuleLoader(Logger.getAnonymousLogger()).loadModules(urlClassLoader,
                                                                                         ProgressMonitor.NULL);
        testAppBContents(modulesDir, modules);
    }

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
                   new String[0]);
        testModule(map, modulesDir,
                   "a-module-dir-with-jars-and-classes",
                   "a-module-dir-with-jars-and-classes",
                   new String[]{
                           "lib/lib-3.jar",
                           "lib/lib-4.jar",
                   },
                   new String[0]);
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
                   });
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


        HashSet<String> actualSet = new HashSet<String>(10);

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
        HashMap<String, ModuleImpl> map = new HashMap<String, ModuleImpl>();
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
