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
public class EngineConfigTest {

    @Before
    public void setUp() throws Exception {
        EngineConfig.instance().clear();
        System.getProperties()
                .stringPropertyNames()
                .stream()
                .filter(name -> name.startsWith("snap."))
                .forEach(System::clearProperty);
    }

    @Test
    public void testDefaults() throws Exception {
        assertFalse(EngineConfig.instance().loaded());
        assertFalse(EngineConfig.instance().debug());
        assertFalse(EngineConfig.instance().ignoreUserConfig());
        assertFalse(EngineConfig.instance().ignoreDefaultConfig());
        assertArrayEquals(EngineConfig.DEFAULT_EXCLUDED_CLUSTER_NAMES, EngineConfig.instance().excludedClusterNames());
        assertArrayEquals(EngineConfig.DEFAULT_EXCLUDED_MODULE_NAMES, EngineConfig.instance().excludedModuleNames());
        assertEquals(Paths.get(System.getProperty("user.dir")), EngineConfig.instance().installDir());
        assertEquals(Paths.get(System.getProperty("user.home"), ".snap"), EngineConfig.instance().userDir());
        assertNull(EngineConfig.instance().configFile());
    }

    @Test
    public void testSetters() throws Exception {
        EngineConfig.instance()
                .debug(true)
                .ignoreUserConfig(true)
                .ignoreDefaultConfig(true)
                .excludedClusterNames("s2tbx", "s3tbx", "smos")
                .excludedModuleNames("a", "b", "c")
                .installDir(Paths.get("/opt/snap2"))
                .userDir(Paths.get("/home/bar"))
                .configFile(Paths.get("./test.properties"));
        assertTrue(EngineConfig.instance().debug());
        assertTrue(EngineConfig.instance().ignoreUserConfig());
        assertTrue(EngineConfig.instance().ignoreDefaultConfig());
        assertArrayEquals(new String[]{"s2tbx", "s3tbx", "smos"}, EngineConfig.instance().excludedClusterNames());
        assertArrayEquals(new String[]{"a", "b", "c"}, EngineConfig.instance().excludedModuleNames());
        assertEquals(Paths.get("/opt/snap2"), EngineConfig.instance().installDir());
        assertEquals(Paths.get("/home/bar"), EngineConfig.instance().userDir());
        assertEquals(Paths.get("./test.properties"), EngineConfig.instance().configFile());
    }

    @Test
    public void testLoadWithDefaults() throws Exception {
        EngineConfig.instance().load();
        assertTrue(EngineConfig.instance().loaded());
        assertFalse(EngineConfig.instance().debug());
        assertFalse(EngineConfig.instance().ignoreUserConfig());
        assertFalse(EngineConfig.instance().ignoreDefaultConfig());
        assertArrayEquals(EngineConfig.DEFAULT_EXCLUDED_CLUSTER_NAMES, EngineConfig.instance().excludedClusterNames());
        assertArrayEquals(EngineConfig.DEFAULT_EXCLUDED_MODULE_NAMES, EngineConfig.instance().excludedModuleNames());
        assertEquals(Paths.get(System.getProperty("user.dir")), EngineConfig.instance().installDir());
        assertEquals(Paths.get(System.getProperty("user.home"), ".snap"), EngineConfig.instance().userDir());
        assertNull(EngineConfig.instance().configFile());
    }

    @Test
    public void testLoadFromFile() throws Exception {
        Path configFile = Paths.get(EngineConfigTest.class.getResource("test.properties").toURI());
        EngineConfig.instance().configFile(configFile).load();
        assertTrue(EngineConfig.instance().debug());
        assertTrue(EngineConfig.instance().ignoreUserConfig());
        assertTrue(EngineConfig.instance().ignoreDefaultConfig());
        assertArrayEquals(new String[]{"s1tbx", "s2tbx", "s3tbx"}, EngineConfig.instance().excludedClusterNames());
        assertArrayEquals(new String[]{"snap-binning", "org.esa.snap:ceres-jai"}, EngineConfig.instance().excludedModuleNames());
        assertEquals(Paths.get("/opt/snap"), EngineConfig.instance().installDir());
        assertEquals(Paths.get("/home/foo"), EngineConfig.instance().userDir());
        assertEquals(configFile, EngineConfig.instance().configFile());
    }

    @Test
    public void testPreferencesResolution() throws Exception {
        Preferences preferences = EngineConfig.instance().preferences();

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
