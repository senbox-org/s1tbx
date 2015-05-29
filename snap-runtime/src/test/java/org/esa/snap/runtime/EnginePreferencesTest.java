package org.esa.snap.runtime;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * @author Norman
 */
public class EnginePreferencesTest {

    @Test
    public void testGetPutRemove() throws Exception {
        EnginePreferences test = new EnginePreferences("test");

        String KEY = "test.key";

        assertEquals(null, test.get(KEY, null));
        assertEquals("A", test.get(KEY, "A"));
        assertEquals(null, test.getProperties().getProperty(KEY));

        test.put(KEY, "B");

        assertEquals("B", test.get(KEY, null));
        assertEquals("B", test.get(KEY, "A"));
        assertEquals("B", test.getProperties().getProperty(KEY));

        test.remove(KEY);

        assertEquals(null, test.get(KEY, null));
        assertEquals("A", test.get(KEY, "A"));
        assertEquals(null, test.getProperties().getProperty(KEY));
    }


    @Test
    public void testRecognizedSystemProperties() throws Exception {
        EnginePreferences test = new EnginePreferences("test2");

        String KEY = "test2.key"; // Recognized key, because it starts with "test2."
        try {
            assertNull(System.getProperty(KEY));

            test.put(KEY, "B");
            assertEquals(null, System.getProperty(KEY));
            assertEquals("B", test.get(KEY, null));
            assertEquals("B", test.get(KEY, "A"));
            assertEquals("B", test.getProperties().getProperty(KEY));

            System.setProperty(KEY, "C");
            assertEquals("C", System.getProperty(KEY));
            assertEquals("C", test.get(KEY, null)); // System property has higher precedence
            assertEquals("C", test.get(KEY, "A"));  // System property has higher precedence
            assertEquals("B", test.getProperties().getProperty(KEY));

            test.put(KEY, "D"); // System property will change as well
            assertEquals("D", System.getProperty(KEY)); // System property changed!!!
            assertEquals("D", test.get(KEY, null));
            assertEquals("D", test.get(KEY, "A"));
            assertEquals("D", test.getProperties().getProperty(KEY));
        } finally {
            System.clearProperty(KEY);
        }
    }

    @Test
    public void testIgnoredSystemProperties() throws Exception {
        EnginePreferences test = new EnginePreferences("test3");

        String KEY = "org.esa.snap.test3.key"; // Ignored key, because it does not start with "test3."
        try {
            assertNull(System.getProperty(KEY));

            assertEquals(null, System.getProperty(KEY));
            assertEquals(null, test.get(KEY, null));
            assertEquals("A", test.get(KEY, "A"));
            assertEquals(null, test.getProperties().getProperty(KEY));

            System.setProperty(KEY, "E");
            assertEquals("E", System.getProperty(KEY)); // System property NOT changed!!!
            assertEquals(null, test.get(KEY, null));
            assertEquals("A", test.get(KEY, "A"));
            assertEquals(null, test.getProperties().getProperty(KEY));
        } finally {
            System.clearProperty(KEY);
        }
    }
}
