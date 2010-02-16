package com.bc.ceres.launcher;

import com.bc.ceres.core.runtime.RuntimeConfigException;
import com.bc.ceres.core.runtime.AbstractRuntimeTest;

import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

public class LauncherTest extends AbstractRuntimeTest {
    private static final boolean  YES = true;
    private static final boolean  NO_ = false;

    @Override
    protected void setUp() throws Exception {
        clearContextSystemProperties("x");
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        clearContextSystemProperties("x");
    }

    public void testBootstrapClasspathFactory() throws RuntimeConfigException, IOException {
        initContextHomeDir("x", "x-app", "");

        System.setProperty("ceres.context", "x");
        System.setProperty("x.home", getBaseDirPath() + "/x-app");
        System.setProperty("x.app", "bibo");
        System.setProperty("x.mainClass", "com.bc.ceres.core.runtime.RuntimeLauncher");
        Launcher defaulLauncher = Launcher.createDefaultLauncher();

        List<URL> classpath = Arrays.asList(defaulLauncher.createClasspath());
        testInClasspath(NO_, classpath, "x-app");
        testInClasspath(NO_, classpath, "x-app/config");
        testInClasspath(NO_, classpath, "x-app/lib");
        testInClasspath(NO_, classpath, "x-app/lib/ceres-launcher-0.5.jar");
        testInClasspath(YES, classpath, "x-app/lib/xstream-1.2.jar");
        testInClasspath(YES, classpath, "x-app/lib/xpp3-1.1.3.jar");
        testInClasspath(YES, classpath, "x-app/lib/jdom-1.0.jar");
        testInClasspath(YES, classpath, "x-app/lib/lib-jide-1.9");
        testInClasspath(NO_, classpath, "x-app/modules");
        testInClasspath(YES, classpath, "x-app/modules/ceres-core-0.5.jar");
        testInClasspath(NO_, classpath, "x-app/modules/ceres-ui-0.5.jar");
        testInClasspath(NO_, classpath, "x-app/modules/beam-core-4.0.jar");
        testInClasspath(NO_, classpath, "x-app/modules/beam-ui-4.0.jar");
        testInClasspath(NO_, classpath, "x-app/modules/lib-netcdf");
        testInClasspath(NO_, classpath, "x-app/modules/lib-netcdf/lib");
        testInClasspath(NO_, classpath, "x-app/modules/lib-netcdf/lib/nc-core.jar");
        testInClasspath(NO_, classpath, "x-app/modules/lib-hdf");
        testInClasspath(NO_, classpath, "x-app/modules/lib-hdf/lib");
        testInClasspath(NO_, classpath, "x-app/modules/lib-hdf/lib/jhdf.jar");

        clearContextSystemProperties("x");
    }

    public void testBruteForceClasspathFactory() throws RuntimeConfigException, IOException {
        clearContextSystemProperties("x");
        initContextHomeDir("x", "x-app", "");

        System.setProperty("ceres.context", "x");
        System.setProperty("x.home", getBaseDirPath() + "/x-app");
        System.setProperty("x.app", "bibo");
        System.setProperty("x.mainClass", LauncherTest.class.getName());
        Launcher defaulLauncher = Launcher.createDefaultLauncher();
        System.clearProperty("ceres.context");
        System.clearProperty("x.home");
        System.clearProperty("x.app");
        System.clearProperty("x.mainClass");

        List<URL> classpath = Arrays.asList(defaulLauncher.createClasspath());
        testInClasspath(NO_, classpath, "x-app");
        testInClasspath(NO_, classpath, "x-app/config");
        testInClasspath(NO_, classpath, "x-app/lib");
        testInClasspath(NO_, classpath, "x-app/lib/ceres-launcher-0.5.jar");
        testInClasspath(YES, classpath, "x-app/lib/xstream-1.2.jar");
        testInClasspath(YES, classpath, "x-app/lib/xpp3-1.1.3.jar");
        testInClasspath(YES, classpath, "x-app/lib/jdom-1.0.jar");
        testInClasspath(YES, classpath, "x-app/lib/lib-jide-1.9");
        testInClasspath(NO_, classpath, "x-app/modules");
        testInClasspath(YES, classpath, "x-app/modules/ceres-core-0.5.jar");
        testInClasspath(YES, classpath, "x-app/modules/ceres-ui-0.5.jar");
        testInClasspath(YES, classpath, "x-app/modules/beam-core-4.0.jar");
        testInClasspath(YES, classpath, "x-app/modules/beam-ui-4.0.jar");
        testInClasspath(YES, classpath, "x-app/modules/lib-netcdf");
        testInClasspath(NO_, classpath, "x-app/modules/lib-netcdf/lib");
        testInClasspath(YES, classpath, "x-app/modules/lib-netcdf/lib/nc-core.jar");
        testInClasspath(YES, classpath, "x-app/modules/lib-hdf");
        testInClasspath(NO_, classpath, "x-app/modules/lib-hdf/lib");
        testInClasspath(YES, classpath, "x-app/modules/lib-hdf/lib/jhdf.jar");
    }

    private void testInClasspath(boolean expected, List<URL> classpath, String filePath) throws IOException {
        URL url = toURL(filePath);
        boolean actual = classpath.contains(url);
        assertEquals("Is [" + url + "] a classpath entry ? :", expected, actual);
    }
}
