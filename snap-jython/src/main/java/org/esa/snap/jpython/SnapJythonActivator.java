package org.esa.snap.jpython;


import org.esa.snap.runtime.Activator;
import org.esa.snap.runtime.Config;
import org.esa.snap.util.SystemUtils;
import org.python.core.PyDictionary;
import org.python.core.PyList;
import org.python.core.PyString;
import org.python.core.PySystemState;
import org.python.util.PythonInterpreter;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author Norman Fomferra
 */
public class SnapJythonActivator implements Activator {

    public static final String SNAP_JYTHON_PATH_KEY = "snap.jython.path";
    public static final String SNAP_PY_FILENAME = "__snap__.py";
    public static final String SNAP_JYTHON_DEFAULT_DIRNAME = "snap-jython";

    private PythonInterpreter pythonInterpreter;

    public PythonInterpreter getPythonInterpreter() {
        return pythonInterpreter;
    }

    @Override
    public void start() {
        PySystemState pyState = new PySystemState();
        pyState.setClassLoader(Thread.currentThread().getContextClassLoader());
        pythonInterpreter = new PythonInterpreter(new PyDictionary(), pyState);

        List<Path> modulePaths = getJythonModulePaths();
        List<Path> pluginPaths = getJythonPluginPaths(modulePaths);
        extendJythonSysPath(modulePaths);
        executeJythonPluginScripts(pluginPaths);
    }

    @Override
    public void stop() {
        if (pythonInterpreter != null) {
            pythonInterpreter.close();
        }
    }

    private void extendJythonSysPath(List<Path> modulePaths) {
        PyString[] pyPaths = modulePaths.stream()
                .map(p -> new PyString(p.toString()))
                .toArray(PyString[]::new);

        pythonInterpreter.set("snap_module_paths", new PyList(pyPaths));
        pythonInterpreter.exec("import sys\n"
                                       + "sys.path += snap_module_paths");
    }

    private void executeJythonPluginScripts(List<Path> pluginPaths) {
        for (Path pluginPath : pluginPaths) {
            System.out.println("Executing Jython plugin script " + pluginPath);
            try {
                try (InputStream inputStream = Files.newInputStream(pluginPath)) {
                    pythonInterpreter.execfile(inputStream, pluginPath.toString());
                }
            } catch (IOException e) {
                // todo: log
                e.printStackTrace();
            }
        }
    }

    private List<Path> getJythonPluginPaths(List<Path> modulePaths) {
        List<Path> pluginPaths = new ArrayList<>();
        for (Path modulePath : modulePaths) {
            if (Files.isRegularFile(modulePath) && modulePath.getFileName().toString().endsWith(".zip")) {
                try {
                    FileSystem fileSystem = FileSystems.newFileSystem(modulePath, null);
                    modulePath = fileSystem.getPath("/");
                } catch (IOException e) {
                    // todo: log
                    e.printStackTrace();
                }
            }
            try {
                Stream<Path> pathStream = Files.list(modulePath);
                pluginPaths.addAll(pathStream.filter(p -> Files.exists(p.resolve(SNAP_PY_FILENAME))).collect(Collectors.toList()));
            } catch (IOException e) {
                // todo: log
                e.printStackTrace();
            }
        }
        return pluginPaths;
    }

    private List<Path> getJythonModulePaths() {
        String extraPaths = Config.instance().preferences().get(SNAP_JYTHON_PATH_KEY, null);
        List<Path> modulePaths = new ArrayList<>();
        modulePaths.add(SystemUtils.getApplicationDataDir().toPath().resolve(SNAP_JYTHON_DEFAULT_DIRNAME));
        if (extraPaths != null) {
            modulePaths.addAll(Stream.of(extraPaths.split(File.pathSeparator))
                                       .map(s -> Paths.get(s))
                                       .collect(Collectors.toList()));
        }
        return modulePaths;
    }

}
