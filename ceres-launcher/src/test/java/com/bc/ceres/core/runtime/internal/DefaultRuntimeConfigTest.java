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
        assertEquals(new File(".").getCanonicalFile(), new File(config.getHomeDirPath()));
        assertEquals(null, config.getConfigFilePath());
        assertEquals(null, config.getModulesDirPath());
        assertNotNull(config.getLibDirPaths());
        assertEquals(0, config.getLibDirPaths().length);
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
