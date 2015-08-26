package org.esa.snap.jython;


import org.esa.snap.runtime.Activator;
import org.esa.snap.runtime.Config;
import org.esa.snap.util.SystemUtils;
import org.python.core.PyDictionary;
import org.python.core.PyException;
import org.python.core.PyList;
import org.python.core.PyObject;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Activator for Jython plugin support.
 * <p/>
 * Jython plugins are ordinary Python files which are located in either ${user.home}/.snap/snap-jython
 * or one of the paths given by the {@code snap.jythonExtraPaths} configuration property.
 * <p/>
 * The value of the {@code snap.jythonExtraPaths} configuration property
 * is a list of directories and/or ZIP files separated by ':' (Unix) or ';' (Windows).
 * <p/>
 * A Python plugin should expose two methods which are called during the lifetime of a SNAP Engine application or of
 * the SNAP Desktop application:
 * <pre>
 * def on_snap_start():
 *     # ... perform any startup code here
 *
 * def on_snap_stop():
 *     # ... perform any shutdown code here
 * </pre>
 *
 * @author Norman Fomferra
 */
public class JythonPluginActivator implements Activator {

    // see also org.esa.snap.python.PyBridge.SNAP_PYTHON_PATHS_PROPERTY
    public static final String JYTHON_EXTRA_PATHS_PROPERTY = "snap.jythonExtraPaths";

    // see also org.esa.snap.python.PyBridge.SNAP_PYTHON_DIRNAME
    public static final String SNAP_JYTHON_DIRNAME = "snap-jython";

    private static final String PY_SUFFIX = ".py";
    private static final String ON_SNAP_START = "on_snap_start";
    private static final String ON_SNAP_STOP = "on_snap_stop";

    private PythonInterpreter jythonInterpreter;
    private List<JythonPlugin> jythonPlugins;

    PythonInterpreter getJythonInterpreter() {
        return jythonInterpreter;
    }

    List<JythonPlugin> getJythonPlugins() {
        return jythonPlugins;
    }

    @Override
    public void start() {
        List<Path> modulePaths = getJythonModulePaths();
        List<String> plugModules = getJythonPluginModules(modulePaths);
        if (plugModules.isEmpty()) {
            return;
        }

        PySystemState pyState = new PySystemState();
        pyState.setClassLoader(Thread.currentThread().getContextClassLoader());
        jythonInterpreter = new PythonInterpreter(new PyDictionary(), pyState);

        extendJythonSysPath(modulePaths);
        jythonPlugins = importJythonPluginModules(plugModules);
        for (JythonPlugin jythonPlugin : jythonPlugins) {
            invokePlugin(jythonPlugin, true);
        }
    }

    @Override
    public void stop() {
        if (jythonPlugins != null) {
            for (JythonPlugin jythonPlugin : jythonPlugins) {
                invokePlugin(jythonPlugin, false);
            }
            jythonPlugins = null;
        }
        if (jythonInterpreter != null) {
            jythonInterpreter.close();
            jythonInterpreter = null;
        }
    }

    private void invokePlugin(JythonPlugin jythonPlugin, boolean start) {
        PyObject callable = start ? jythonPlugin.onStart : jythonPlugin.onStop;
        if (callable != null) {
            try {
                SystemUtils.LOG.fine(String.format("Calling SNAP-Jython plugin module %s.%s()", jythonPlugin.moduleName, start ? ON_SNAP_START : ON_SNAP_STOP));
                callable.__call__();
            } catch (PyException e) {
                SystemUtils.LOG.log(Level.SEVERE, String.format("SNAP-Jython execution failure for plugin module '%s'", jythonPlugin.moduleName), e);
            }
        }
    }

    private void extendJythonSysPath(List<Path> modulePaths) {
        PyString[] pyPaths = modulePaths.stream()
                .map(p -> new PyString(p.toString()))
                .toArray(PyString[]::new);

        jythonInterpreter.set("snap_module_paths", new PyList(pyPaths));
        jythonInterpreter.exec("import sys\n"
                                        + "sys.path += snap_module_paths");
    }

    static class JythonPlugin {
        final String moduleName;
        final PyObject onStart;
        final PyObject onStop;

        JythonPlugin(String moduleName, PyObject onStart, PyObject onStop) {
            this.moduleName = moduleName;
            this.onStart = onStart;
            this.onStop = onStop;
        }
    }

    private List<JythonPlugin> importJythonPluginModules(List<String> moduleNames) {
        List<JythonPlugin> plugins = new ArrayList<>();
        for (String moduleName : moduleNames) {
            SystemUtils.LOG.info("Importing Jython plugin module " + moduleName);
            try {
                jythonInterpreter.exec("import " + moduleName);
                plugins.add(new JythonPlugin(moduleName,
                                             getCallable(moduleName, ON_SNAP_START),
                                             getCallable(moduleName, ON_SNAP_STOP)));
            } catch (PyException e) {
                SystemUtils.LOG.log(Level.SEVERE, String.format("SNAP-Jython import failure for plugin module '%s'", moduleName), e);
            }
        }
        return plugins;
    }

    private PyObject getCallable(String moduleName, String functionName) {
        try {
            PyObject callable = jythonInterpreter.eval(moduleName  + "." + functionName);
            if (callable.isCallable()) {
                SystemUtils.LOG.fine(String.format("SNAP-Jython callable '%s.%s' found", moduleName, functionName));
                return callable;
            }
        } catch (PyException ignored) {
        }
        SystemUtils.LOG.warning(String.format("SNAP-Jython plugin module '%s' should define a callable named '%s'", moduleName, functionName));
        return null;
    }

    private List<String> getJythonPluginModules(List<Path> moduleRootPaths) {
        //
        // Note that the current implementation just looks for *.py files in each module path' root and returns their file names.
        // We may be more selective in detecting actual SNAP plugin modules, e.g. forcing a
        // <module>/META-INF/services/org.esa.snap.jypson.SnapPlugIn file to exists and contain the plugin class names (nf)
        //
        List<String> pluginPaths = new ArrayList<>();
        for (Path moduleRootPath : moduleRootPaths) {
            if (Files.exists(moduleRootPath)) {
                FileSystem zipFS = null;
                if (Files.isRegularFile(moduleRootPath) && moduleRootPath.getFileName().toString().endsWith(".zip")) {
                    try {
                        zipFS = FileSystems.newFileSystem(moduleRootPath, null);
                        moduleRootPath = zipFS.getPath("/");
                    } catch (IOException e) {
                        SystemUtils.LOG.warning("SNAP-Jython configuration error: " + e.getMessage());
                    }
                }
                try {
                    pluginPaths.addAll(Files.list(moduleRootPath).filter(this::isPythonFile).map(this::getPyModuleName).collect(Collectors.toList()));
                } catch (IOException e) {
                    SystemUtils.LOG.warning("SNAP-Jython configuration error: " + e.getMessage());
                }
                if (zipFS != null) {
                    try {
                        zipFS.close();
                    } catch (IOException ignored) {
                    }
                }
            } else {
                SystemUtils.LOG.warning("SNAP-Jython module root path not found: " + moduleRootPath);
            }
        }
        return pluginPaths;
    }

    private boolean isPythonFile(Path path) {
        return Files.isRegularFile(path) && path.getFileName().toString().endsWith(".py");
    }

    private String getPyModuleName(Path path) {
        String fileName = path.getFileName().toString();
        if (fileName.endsWith(PY_SUFFIX)) {
            return fileName.substring(0, fileName.length() - PY_SUFFIX.length());
        }
        return fileName;
    }

    private List<Path> getJythonModulePaths() {
        List<Path> modulePaths = new ArrayList<>();
        Path jythonDir = SystemUtils.getApplicationDataDir(true).toPath().resolve(SNAP_JYTHON_DIRNAME);
        if (!Files.exists(jythonDir)) {
            try {
                Files.createDirectories(jythonDir);
                modulePaths.add(jythonDir);
            } catch (IOException e) {
                SystemUtils.LOG.log(Level.WARNING, "SNAP-Jython configuration error: failed to create " + jythonDir, e);
            }
        }
        String extraPaths = Config.instance().preferences().get(JYTHON_EXTRA_PATHS_PROPERTY, null);
        if (extraPaths != null) {
            modulePaths.addAll(Stream.of(extraPaths.split(File.pathSeparator))
                                       .map(s -> Paths.get(s).toAbsolutePath())
                                       .collect(Collectors.toList()));
        }
        return modulePaths;
    }

}
