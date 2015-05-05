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

import com.bc.ceres.core.runtime.AbstractRuntimeTest;
import com.bc.ceres.core.runtime.RuntimeConfigException;
import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

import java.io.File;
import java.io.IOException;

import static org.junit.Assert.*;


@Ignore
public class DefaultRuntimeConfigTest extends AbstractRuntimeTest {
    @Before
    public void setUp() throws Exception {
        clearContextSystemProperties("pacman");
        super.setUp();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
        clearContextSystemProperties("pacman");
    }

    @Ignore
    public void testWithNoProperties() throws RuntimeConfigException, IOException {
        DefaultRuntimeConfig config = new DefaultRuntimeConfig();
        assertEquals("ceres", config.getContextId());

        final File cwd = new File(".").getCanonicalFile();
        boolean hasConfig = new File(cwd, "config/ceres.config").exists();
        boolean hasLib = new File(cwd, "lib").exists();
        boolean hasModules = new File(cwd, "modules").exists();

        assertEquals(cwd, new File(config.getHomeDirPath()));
        if (hasConfig) {
            assertNotNull(config.getConfigFilePath());
        }else {
            assertNull(config.getConfigFilePath());
        }
        assertNotNull(config.getLibDirPaths());
        if (hasLib) {
            assertEquals(1, config.getLibDirPaths().length);
        }else {
            assertEquals(0, config.getLibDirPaths().length);
        }
        if (hasModules) {
            assertNotNull(config.getModulesDirPath());
        }else {
            assertNull(config.getModulesDirPath());
        }

        assertEquals("com.bc.ceres.core.runtime.RuntimeLauncher",config.getMainClassName());
        assertEquals(null, config.getMainClassPath());
    }

    @Test
    public void testWithSystemProperties() throws RuntimeConfigException, IOException {
        String configContent = "# Empty config file\n";
        initContextHomeDir("pacman", "pacman-1.3.6", configContent);

        System.setProperty("ceres.context", "pacman");
        System.setProperty("pacman.home", getBaseDirPath() + "/pacman-1.3.6");
        System.setProperty("pacman.app", "bibo");
        System.setProperty("pacman.grunt", "foobar");
        System.setProperty("pacman.classpath", "a:b:c");
        DefaultRuntimeConfig config = new DefaultRuntimeConfig();
        testConfigPaths(config);
        assertEquals("bibo", config.getApplicationId());
        assertEquals("foobar", config.getContextProperty("grunt"));
        assertEquals("a:b:c", config.getMainClassPath());
    }

    @Test
    public void testWithConfigProperties() throws RuntimeConfigException, IOException {
        String configContent = ""
                + "pacman.home  = " + getBaseDirPath() + "/pacman-1.3.6\n"
                + "pacman.app   = bibo\n"
                + "pacman.grunt = foobar\n"
        + "pacman.classpath = a:b:e\n";
        initContextHomeDir("pacman", "pacman-1.3.6", configContent);

        System.setProperty("ceres.context", "pacman");
        System.setProperty("pacman.config", getBaseDirPath() + "/pacman-1.3.6/config/pacman.config");
        DefaultRuntimeConfig config = new DefaultRuntimeConfig();
        testConfigPaths(config);
        assertEquals("bibo", config.getApplicationId());
        assertEquals("foobar", config.getContextProperty("grunt"));
        assertEquals("a:b:e", config.getMainClassPath());
    }


    @Test
    public void testWithNoHomeNoConfig() throws RuntimeConfigException, IOException {
        String configContent = ""
                + "pacman.app  = bibo\n"
                + "pacman.grunt = foobar\n"
                + "pacman.classpath = a:b\n";
        initContextHomeDir("pacman", "pacman-1.3.6", configContent);

        String oldUserDir = System.getProperty("user.dir", "");
        System.setProperty("user.dir", new File(getBaseDirPath(), "pacman-1.3.6").getPath());

        try {
            System.setProperty("ceres.context", "pacman");
            System.setProperty("pacman.debug", "true");
            DefaultRuntimeConfig config = new DefaultRuntimeConfig();
            testConfigPaths(config);
            assertEquals("bibo", config.getApplicationId());
            assertEquals("foobar", config.getContextProperty("grunt"));
            assertEquals("a:b", config.getMainClassPath());
        } finally {
            System.setProperty("user.dir", oldUserDir);
        }
    }

}
