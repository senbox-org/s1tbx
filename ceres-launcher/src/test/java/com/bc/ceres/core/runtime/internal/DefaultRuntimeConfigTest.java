package com.bc.ceres.core.runtime.internal;

import com.bc.ceres.core.runtime.AbstractRuntimeTest;
import com.bc.ceres.core.runtime.RuntimeConfigException;

import java.io.File;
import java.io.IOException;


public class DefaultRuntimeConfigTest extends AbstractRuntimeTest {
    @Override
    protected void setUp() throws Exception {
        clearContextSystemProperties("pacman");
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        clearContextSystemProperties("pacman");
    }

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
    }

    public void testWithSystemProperties() throws RuntimeConfigException, IOException {
        String configContent = "# Empty config file\n";
        initContextHomeDir("pacman", "pacman-1.3.6", configContent);

        System.setProperty("ceres.context", "pacman");
        System.setProperty("pacman.home", getBaseDirPath() + "/pacman-1.3.6");
        System.setProperty("pacman.app", "bibo");
        System.setProperty("pacman.grunt", "foobar");
        DefaultRuntimeConfig config = new DefaultRuntimeConfig();
        testConfigPaths(config);
        assertEquals("bibo", config.getApplicationId());
        assertEquals("foobar", config.getContextProperty("grunt"));
    }

    public void testWithConfigProperties() throws RuntimeConfigException, IOException {
        String configContent = ""
                + "pacman.home  = " + getBaseDirPath() + "/pacman-1.3.6\n"
                + "pacman.app   = bibo\n"
                + "pacman.grunt = foobar\n";
        initContextHomeDir("pacman", "pacman-1.3.6", configContent);

        System.setProperty("ceres.context", "pacman");
        System.setProperty("pacman.config", getBaseDirPath() + "/pacman-1.3.6/config/pacman.config");
        DefaultRuntimeConfig config = new DefaultRuntimeConfig();
        testConfigPaths(config);
        assertEquals("bibo", config.getApplicationId());
        assertEquals("foobar", config.getContextProperty("grunt"));
    }


    public void testWithNoHomeNoConfig() throws RuntimeConfigException, IOException {
        String configContent = ""
                + "pacman.app  = bibo\n"
                + "pacman.grunt = foobar\n";
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
        } finally {
            System.setProperty("user.dir", oldUserDir);
        }
    }

}
