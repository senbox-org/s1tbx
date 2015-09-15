package org.esa.snap.jython;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.net.URI;
import java.net.URL;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Norman Fomferra
 * @see org.esa.snap.python.gpf.PyOperatorSpiTest
 */
public class PluginActivatorTest {

    @BeforeClass
    public static void setUp() throws Exception {
        File file = getResourceFile("/");
        assertTrue(file.isDirectory());
        System.setProperty(PluginActivator.JYTHON_EXTRA_PATHS_PROPERTY, file.getPath());
    }

    @AfterClass
    public static void tearDown() throws Exception {
        System.clearProperty(PluginActivator.JYTHON_EXTRA_PATHS_PROPERTY);
    }

    @Test
    public void testActivator() throws Exception {

        PluginActivator activator = new PluginActivator();

        activator.start();

        List<PyObject> jythonPlugins = activator.getJythonActivators();
        assertNotNull(jythonPlugins);
        assertEquals(2, jythonPlugins.size());

        PythonInterpreter interpreter = activator.getJythonInterpreter();
        assertNotNull(interpreter);

        // Import the already imported plugin modules to get them into the main namespace
        interpreter.exec("import plugin_1");
        interpreter.exec("import plugin_2");

        PyObject pi1Started = interpreter.eval("plugin_1.started");
        assertNotNull(pi1Started);
        assertEquals("True", pi1Started.toString());

        PyObject pi1Stopped = interpreter.eval("plugin_1.stopped");
        assertNotNull(pi1Stopped);
        assertEquals("False", pi1Stopped.toString());

        PyObject pi2Started = interpreter.eval("plugin_2.started");
        assertNotNull(pi2Started);
        assertEquals("True", pi2Started.toString());

        PyObject pi2Stopped = interpreter.eval("plugin_2.stopped");
        assertNotNull(pi2Stopped);
        assertEquals("False", pi2Stopped.toString());

        // tests importing a sub-module
        PyObject pi2Var = interpreter.eval("plugin_2.var");
        assertNotNull(pi2Var);
        assertEquals("42", pi2Var.toString());

        activator.stop();

        assertEquals("True", interpreter.eval("plugin_1.started").toString());
        assertEquals("True", interpreter.eval("plugin_1.started").toString());
        assertEquals("True", interpreter.eval("plugin_2.stopped").toString());
        assertEquals("True", interpreter.eval("plugin_2.stopped").toString());
    }

    public static File getResourceFile(String name) {
        URL resource = PluginActivatorTest.class.getResource(name);
        assertNotNull("missing resource '" + name + "'", resource);
        return new File(URI.create(resource.toString()));
    }
}
