package org.esa.snap.core.engine;

import org.esa.snap.core.engine.Launcher;
import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.fail;

/**
 * Created by Norman on 20.05.2015.
 */
public class LauncherTest {

    @Test
    public void testSuccess() throws Exception {
        System.setProperty("snap.mainClass", App.class.getName());
        new Launcher().run(new String[]{"A"});
        assertArrayEquals(new String[]{"A"}, App.args);
    }

    @Test
    public void testFail() throws Exception {
        try {
            new Launcher().run(new String[]{"A"});
            fail();
        } catch (Exception e) {
            // ok
        }
    }


    public static class App {
        static String[] args;

        public static void main(String[] args) {
            App.args = args.clone();
        }
    }
}
