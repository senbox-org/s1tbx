package org.esa.snap.runtime;

import org.junit.Before;
import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.prefs.Preferences;

import static org.junit.Assert.*;

/**
 * @author Norman
 */
public class ConfigTest {

    @Before
    public void setUp() throws Exception {
        Config.newInstance();
        System.getProperties()
                .stringPropertyNames()
                .stream()
                .filter(name -> name.startsWith("snap."))
                .forEach(System::clearProperty);
    }

    @Test
    public void testDefaults() throws Exception {
        assertFalse(Config.instance().loaded());
        assertFalse(Config.instance().debug());
        assertFalse(Config.instance().ignoreUserConfig());
        assertFalse(Config.instance().ignoreDefaultConfig());
        assertArrayEquals(Config.DEFAULT_EXCLUDED_CLUSTER_NAMES, Config.instance().excludedClusterNames());
        assertArrayEquals(Config.DEFAULT_EXCLUDED_MODULE_NAMES, Config.instance().excludedModuleNames());
        assertEquals(Paths.get(""), Config.instance().installDir());
        assertEquals(Paths.get(System.getProperty("user.home"), ".snap"), Config.instance().userDir());
        assertNull(Config.instance().configFile());
    }

    @Test
    public void testSetters() throws Exception {
        Config.instance()
                .debug(true)
                .ignoreUserConfig(true)
                .ignoreDefaultConfig(true)
                .excludedClusterNames("s2tbx", "s3tbx", "smos")
                .excludedModuleNames("a", "b", "c")
                .installDir(Paths.get("/opt/snap2"))
                .userDir(Paths.get("/home/bar"))
                .configFile(Paths.get("./test.config"));
        assertTrue(Config.instance().debug());
        assertTrue(Config.instance().ignoreUserConfig());
        assertTrue(Config.instance().ignoreDefaultConfig());
        assertArrayEquals(new String[]{"s2tbx", "s3tbx", "smos"}, Config.instance().excludedClusterNames());
        assertArrayEquals(new String[]{"a", "b", "c"}, Config.instance().excludedModuleNames());
        assertEquals(Paths.get("/opt/snap2"), Config.instance().installDir());
        assertEquals(Paths.get("/home/bar"), Config.instance().userDir());
        assertEquals(Paths.get("./test.config"), Config.instance().configFile());
    }

    @Test
    public void testLoadWithDefaults() throws Exception {
        Config.instance().load();
        assertTrue(Config.instance().loaded());
        assertFalse(Config.instance().debug());
        assertFalse(Config.instance().ignoreUserConfig());
        assertFalse(Config.instance().ignoreDefaultConfig());
        assertArrayEquals(Config.DEFAULT_EXCLUDED_CLUSTER_NAMES, Config.instance().excludedClusterNames());
        assertArrayEquals(Config.DEFAULT_EXCLUDED_MODULE_NAMES, Config.instance().excludedModuleNames());
        assertEquals(Paths.get(""), Config.instance().installDir());
        assertEquals(Paths.get(System.getProperty("user.home"), ".snap"), Config.instance().userDir());
        assertNull(Config.instance().configFile());
    }

    @Test
    public void testLoadFromFile() throws Exception {
        Path configFile = Paths.get(ConfigTest.class.getResource("test.properties").toURI());
        Config.instance().configFile(configFile).load();
        assertTrue(Config.instance().debug());
        assertTrue(Config.instance().ignoreUserConfig());
        assertTrue(Config.instance().ignoreDefaultConfig());
        assertArrayEquals(new String[]{"s1tbx", "s2tbx", "s3tbx"}, Config.instance().excludedClusterNames());
        assertArrayEquals(new String[]{"snap-binning", "org.esa.snap:ceres-jai"}, Config.instance().excludedModuleNames());
        assertEquals(Paths.get("/opt/snap"), Config.instance().installDir());
        assertEquals(Paths.get("/home/foo"), Config.instance().userDir());
        assertEquals(configFile, Config.instance().configFile());
    }

    @Test
    public void testPreferencesResolution() throws Exception {
        Preferences preferences = Config.instance().preferences();

        preferences.put("snap.home", "/opt/snap");
        preferences.put("extra-cluster", "bibo");
        preferences.put("cluster-path", "${snap.home}/${extra-cluster}");

        assertEquals("/opt/snap/bibo", preferences.get("cluster-path", null));

        preferences.put("myarch", "${os.arch}");
        assertNotNull(preferences.get("myarch", null));
        assertFalse(preferences.get("myarch", null).contains("${"));
        assertFalse(preferences.get("myarch", null).contains("}"));
    }
}
