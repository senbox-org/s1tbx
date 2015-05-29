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
        Config.instance("bibo").clear();
        System.getProperties()
                .stringPropertyNames()
                .stream()
                .filter(name -> name.startsWith("bibo."))
                .forEach(System::clearProperty);
    }

    @Test
    public void testLoadStandard() throws Exception {
        assertFalse(Config.instance("bibo").loaded());
        Config.instance("bibo").load();
        assertTrue(Config.instance("bibo").loaded());
    }

    @Test
    public void testLoadCustom() throws Exception {

        Path configFile = Paths.get(EngineConfigTest.class.getResource("bibo-extra.properties").toURI());

        assertFalse(Config.instance("bibo").loaded());
        Config.instance("bibo").load(configFile);
        assertFalse(Config.instance("bibo").loaded());

        assertEquals(34, Config.instance("bibo").preferences().getInt("bibo.a", 0));
        assertEquals(0.41, Config.instance("bibo").preferences().getDouble("bibo.b", 0.0), 1e-10);
        assertEquals(true, Config.instance("bibo").preferences().getBoolean("bibo.c", false));
        assertEquals("ABC", Config.instance("bibo").preferences().get("bibo.d", null));
    }
}
