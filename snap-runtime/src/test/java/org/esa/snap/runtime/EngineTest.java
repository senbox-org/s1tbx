package org.esa.snap.runtime;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Norman
 */
public class EngineTest {

    @Test
    public void testIllegalStateException() throws Exception {
        Engine engine = Engine.start(false);
        engine.stop();

        try {
            engine.getClientClassLoader();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("start()"));
        }

        try {
            engine.setContextClassLoader();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("start()"));
        }

        try {
            engine.runClientCode(() -> {
            });
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("start()"));
        }

        try {
            engine.createClientRunnable(() -> {
            });
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("start()"));
        }
    }

    @Test
    public void testIllegalStateExceptionDeferred() throws Exception {
        Engine engine = Engine.start(false);
        Runnable clientRunnable = engine.createClientRunnable(() -> {
        });
        engine.stop();

        try {
            clientRunnable.run();
            fail("IllegalStateException expected");
        } catch (IllegalStateException e) {
            assertNotNull(e.getMessage());
            assertTrue(e.getMessage().contains("start()"));
        }
    }

}
