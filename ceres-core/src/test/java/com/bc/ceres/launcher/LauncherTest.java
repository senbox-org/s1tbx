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

package com.bc.ceres.launcher;

import com.bc.ceres.core.runtime.AbstractRuntimeTest;
import com.bc.ceres.core.runtime.RuntimeConfigException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.*;

@Ignore
public class LauncherTest extends AbstractRuntimeTest {
    private static final boolean  YES = true;
    private static final boolean  NO_ = false;

    @Before
    public void setUp() throws Exception {
        clearContextSystemProperties("x");
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        clearContextSystemProperties("x");
    }

    @Test
    public void testBootstrapClasspathFactory() throws RuntimeConfigException, IOException {
        initContextHomeDir("x", "x-app", "");

        System.setProperty("ceres.context", "x");
        System.setProperty("x.home", getBaseDirPath() + "/x-app");
        System.setProperty("x.app", "bibo");
        System.setProperty("x.mainClass", "com.bc.ceres.core.runtime.RuntimeLauncher");
        System.setProperty("x.classpath", "." + File.pathSeparator + "x-app");
        Launcher defaultLauncher = Launcher.createDefaultLauncher();

        List<URL> cp1 = Arrays.asList(defaultLauncher.createMainClasspath());
        assertEquals(2, cp1.size());
        testInMainClasspath(YES, cp1, "x-app");
        testInMainClasspath(YES, cp1, ".");

        List<URL> cp2 = Arrays.asList(defaultLauncher.createDefaultClasspath());
        testInDefaultClasspath(NO_, cp2, "x-app");
        testInDefaultClasspath(NO_, cp2, "x-app/config");
        testInDefaultClasspath(NO_, cp2, "x-app/lib");
        testInDefaultClasspath(NO_, cp2, "x-app/lib/ceres-launcher-0.5.jar");
        testInDefaultClasspath(YES, cp2, "x-app/lib/xstream-1.2.jar");
        testInDefaultClasspath(YES, cp2, "x-app/lib/xpp3-1.1.3.jar");
        testInDefaultClasspath(YES, cp2, "x-app/lib/jdom-1.0.jar");
        testInDefaultClasspath(YES, cp2, "x-app/lib/lib-jide-1.9");
        testInDefaultClasspath(NO_, cp2, "x-app/modules");
        testInDefaultClasspath(YES, cp2, "x-app/modules/ceres-core-0.5.jar");
        testInDefaultClasspath(NO_, cp2, "x-app/modules/ceres-ui-0.5.jar");
        testInDefaultClasspath(NO_, cp2, "x-app/modules/beam-core-4.0.jar");
        testInDefaultClasspath(NO_, cp2, "x-app/modules/beam-ui-4.0.jar");
        testInDefaultClasspath(NO_, cp2, "x-app/modules/lib-netcdf");
        testInDefaultClasspath(NO_, cp2, "x-app/modules/lib-netcdf/lib");
        testInDefaultClasspath(NO_, cp2, "x-app/modules/lib-netcdf/lib/nc-core.jar");
        testInDefaultClasspath(NO_, cp2, "x-app/modules/lib-hdf");
        testInDefaultClasspath(NO_, cp2, "x-app/modules/lib-hdf/lib");
        testInDefaultClasspath(NO_, cp2, "x-app/modules/lib-hdf/lib/jhdf.jar");

        clearContextSystemProperties("x");
    }

    @Test
    public void testBruteForceClasspathFactory() throws RuntimeConfigException, IOException {
        clearContextSystemProperties("x");
        initContextHomeDir("x", "x-app", "");

        System.setProperty("ceres.context", "x");
        System.setProperty("x.home", getBaseDirPath() + "/x-app");
        System.setProperty("x.app", "bibo");
        System.setProperty("x.mainClass", LauncherTest.class.getName());
        System.setProperty("x.classpath", "." + File.pathSeparator + "x-app");
        Launcher defaultLauncher = Launcher.createDefaultLauncher();
        System.clearProperty("ceres.context");
        System.clearProperty("x.home");
        System.clearProperty("x.app");
        System.clearProperty("x.mainClass");
        System.clearProperty("x.classpath");

        List<URL> cp1 = Arrays.asList(defaultLauncher.createMainClasspath());
        assertEquals(2, cp1.size());
        testInMainClasspath(YES, cp1, "x-app");
        testInMainClasspath(YES, cp1, ".");

        List<URL> cp2 = Arrays.asList(defaultLauncher.createDefaultClasspath());
        testInDefaultClasspath(NO_, cp2, "x-app");
        testInDefaultClasspath(NO_, cp2, "x-app/config");
        testInDefaultClasspath(NO_, cp2, "x-app/lib");
        testInDefaultClasspath(NO_, cp2, "x-app/lib/ceres-launcher-0.5.jar");
        testInDefaultClasspath(YES, cp2, "x-app/lib/xstream-1.2.jar");
        testInDefaultClasspath(YES, cp2, "x-app/lib/xpp3-1.1.3.jar");
        testInDefaultClasspath(YES, cp2, "x-app/lib/jdom-1.0.jar");
        testInDefaultClasspath(YES, cp2, "x-app/lib/lib-jide-1.9");
        testInDefaultClasspath(NO_, cp2, "x-app/modules");
        testInDefaultClasspath(YES, cp2, "x-app/modules/ceres-core-0.5.jar");
        testInDefaultClasspath(YES, cp2, "x-app/modules/ceres-ui-0.5.jar");
        testInDefaultClasspath(YES, cp2, "x-app/modules/beam-core-4.0.jar");
        testInDefaultClasspath(YES, cp2, "x-app/modules/beam-ui-4.0.jar");
        testInDefaultClasspath(YES, cp2, "x-app/modules/lib-netcdf");
        testInDefaultClasspath(NO_, cp2, "x-app/modules/lib-netcdf/lib");
        testInDefaultClasspath(YES, cp2, "x-app/modules/lib-netcdf/lib/nc-core.jar");
        testInDefaultClasspath(YES, cp2, "x-app/modules/lib-hdf");
        testInDefaultClasspath(NO_, cp2, "x-app/modules/lib-hdf/lib");
        testInDefaultClasspath(YES, cp2, "x-app/modules/lib-hdf/lib/jhdf.jar");
    }

    private void testInMainClasspath(boolean expected, List<URL> classpath, String filePath) throws IOException {
        testInClasspath(expected, classpath, toMainURL(filePath));
    }

    private void testInDefaultClasspath(boolean expected, List<URL> classpath, String filePath) throws IOException {
        testInClasspath(expected, classpath, toDefaultURL(filePath));
    }

    private void testInClasspath(boolean expected, List<URL> classpath, URL url) {
        assertEquals("Is [" + url + "] a classpath entry ? :", expected, classpath.contains(url));
    }

}
