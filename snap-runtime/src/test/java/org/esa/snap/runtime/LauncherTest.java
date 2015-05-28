package org.esa.snap.runtime;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Norman
 */
public class LauncherTest {

    @Before
    public void setUp() throws Exception {
        System.clearProperty(Launcher.PROPERTY_MAIN_CLASS_NAME);
    }

    @Test
    public void testSuccess() throws Exception {
        System.setProperty(Launcher.PROPERTY_MAIN_CLASS_NAME, App.class.getName());
        new Launcher().run(new String[]{"A", "B", "C"});
        assertArrayEquals(new String[]{"A", "B", "C"}, App.args);
    }

    @Test
    public void testFail() throws Exception {
        // No "snap.mainClass" set.
        try {
            new Launcher().run(new String[]{"A", "B", "C"});
            fail();
        } catch (RuntimeException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains(String.format("'%s'", Launcher.PROPERTY_MAIN_CLASS_NAME)));
        }
    }

    public static class App {
        static String[] args;

        public static void main(String[] args) {
            App.args = args.clone();
        }
    }
}
