package org.esa.snap.jython;


import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.python.core.PyObject;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.nio.file.Paths;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

/**
 * @author Norman Fomferra
 */
public class JythonPluginActivatorTest {

    @BeforeClass
    public static void setUp() throws Exception {
        File resourceRootDir = Paths.get(JythonPluginActivatorTest.class.getResource("/").toURI()).toFile();
        String extraPaths = String.join(File.pathSeparator,
                                        new File(resourceRootDir, "mod_parent_a").getPath(),
                                        new File(resourceRootDir, "mod_parent_b.zip").getPath());
        System.setProperty(JythonPluginActivator.JYTHON_EXTRA_PATHS_PROPERTY, extraPaths);
    }

    @AfterClass
    public static void tearDown() throws Exception {
        System.clearProperty(JythonPluginActivator.JYTHON_EXTRA_PATHS_PROPERTY);
    }

    @Test
    public void testActivator() throws Exception {

        JythonPluginActivator activator = new JythonPluginActivator();

        activator.start();

        List<JythonPluginActivator.JythonPlugin> jythonPlugins = activator.getJythonPlugins();
        assertNotNull(jythonPlugins);
        assertEquals(3, jythonPlugins.size());

        PythonInterpreter interpreter = activator.getJythonInterpreter();
        assertNotNull(interpreter);

        assertNotNull(interpreter.get("plugin_1"));
        assertNotNull(interpreter.get("plugin_2"));
        assertNotNull(interpreter.get("plugin_3"));

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

        PyObject pi2Var = interpreter.eval("plugin_2.var");
        assertNotNull(pi2Var);
        assertEquals("42", pi2Var.toString());

        activator.stop();

        assertEquals("True", interpreter.eval("plugin_1.started").toString());
        assertEquals("True", interpreter.eval("plugin_1.started").toString());
        assertEquals("True", interpreter.eval("plugin_2.stopped").toString());
        assertEquals("True", interpreter.eval("plugin_2.stopped").toString());
    }
}
