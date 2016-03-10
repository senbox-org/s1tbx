package org.esa.snap.jython;


import org.esa.snap.core.util.ServiceFinder;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.runtime.Activator;
import org.python.core.PyDictionary;
import org.python.core.PyObject;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;

import static org.esa.snap.core.util.SystemUtils.*;

/**
 * Activator for Jython plugin support.
 * <p/>
 * Jython plugins are ordinary Python files which are located in either ${user.home}/.snap/snap-jython
 * or one of the paths given by the {@code snap.jythonExtraPaths} configuration property.
 * <p/>
 * The value of the {@code snap.jythonExtraPaths} configuration property
 * is a list of directories separated by ':' (Unix) or ';' (Windows).
 * <p/>
 * A Jython plugin should expose a class with two methods which are called during the lifetime of a
 * SNAP Engine or the SNAP Desktop application:
 * <pre>
 * class Activator:
 *     def start(self):
 *         # ... perform any startup code here
 *
 *     def stop(self):
 *         # ... perform any shutdown code here
 * </pre>
 *
 * Activators are registered through a text file called
 * {@code META-INF/services/org.esa.snap.jython.PluginActivator}.
 * Each line of the fike contains the fully qualified name of an activator class.
 *
 * @author Norman Fomferra
 */
public class PluginActivator implements Activator {

    // see also org.esa.snap.python.PyBridge.SNAP_PYTHON_PATHS_PROPERTY
    public static final String JYTHON_EXTRA_PATHS_PROPERTY = "snap.jythonExtraPaths";

    // see also org.esa.snap.python.PyBridge.SNAP_PYTHON_DIRNAME
    public static final String SNAP_JYTHON_DIRNAME = "snap-jython";

    private PythonInterpreter jythonInterpreter;
    private List<PyObject> jythonActivators;

    PythonInterpreter getJythonInterpreter() {
        return jythonInterpreter;
    }

    List<PyObject> getJythonActivators() {
        return jythonActivators;
    }

    @Override
    public void start() {
        PySystemState pyState = new PySystemState();
        pyState.setClassLoader(Thread.currentThread().getContextClassLoader());
        jythonInterpreter = new PythonInterpreter(new PyDictionary(), pyState);
        jythonActivators = new ArrayList<>();
        initActivators();
        for (PyObject self : jythonActivators) {
            self.invoke("start");
        }
    }

    @Override
    public void stop() {
        if (jythonActivators != null) {
            for (PyObject self : jythonActivators) {
                self.invoke("stop");
            }
            jythonActivators = null;
        }
        if (jythonInterpreter != null) {
            jythonInterpreter.close();
            jythonInterpreter = null;
        }
    }


    private void initActivators() {
        Path jythonDir = SystemUtils.getApplicationDataDir(true).toPath().resolve(SNAP_JYTHON_DIRNAME);
        if (!Files.exists(jythonDir)) {
            try {
                Files.createDirectories(jythonDir);
            } catch (IOException e) {
                SystemUtils.LOG.log(Level.WARNING, "SNAP-Jython configuration error: failed to create " + jythonDir, e);
            }
        }

        ServiceFinder serviceFinder = new ServiceFinder(PluginActivator.class);
        serviceFinder.addSearchPath(jythonDir);
        serviceFinder.addSearchPathsFromPreferences(JYTHON_EXTRA_PATHS_PROPERTY);
        serviceFinder.setUseClassPath(true);
        List<ServiceFinder.Module> modules = serviceFinder.findServices();
        for (ServiceFinder.Module module : modules) {
            for (String pythonClassName : module.getServiceNames()) {
                loadActivator(module.getPath(), pythonClassName);
            }
        }
    }

    private void loadActivator(Path moduleRoot, String pythonClassName) {
        int lastDotPos = pythonClassName.lastIndexOf('.');
        String modulePath = "";
        String className = "";
        if (lastDotPos > 0) {
            modulePath = pythonClassName.substring(0, lastDotPos);
            className = pythonClassName.substring(lastDotPos + 1);
            loadActivator(moduleRoot, modulePath, className);
        }
        if (modulePath.isEmpty() || className.isEmpty()) {
            LOG.warning(String.format("Invalid Python module entry in %s: %s", moduleRoot, pythonClassName));
        }
    }

    private  void loadActivator(Path pythonModuleRoot, String pythonModuleName, final String pythonClassName) {
        File pythonModuleRootFile = FileUtils.toFile(pythonModuleRoot);
        try {
            jythonInterpreter.exec("import sys\n"
                                           + "sys.path += ['"+pythonModuleRootFile.getPath().replace("\\", "\\\\")+"']");
            jythonInterpreter.exec("from " + pythonModuleName + " import " + pythonClassName);
            PyObject activator = jythonInterpreter.eval(pythonClassName + "()");
            jythonActivators.add(activator);
            LOG.info(String.format("Jython activator %s.%s loaded from %s", pythonModuleName, pythonClassName, pythonModuleRoot));
        } catch (Exception e) {
            String msg = String.format("Failed to create Jython activator %s.%s defined in %s", pythonModuleName, pythonClassName, pythonModuleRoot);
            LOG.log(Level.SEVERE, msg, e);
        }
    }
}
