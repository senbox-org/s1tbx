package org.esa.snap.python;

import org.esa.snap.util.Debug;
import org.esa.snap.util.SystemUtils;
import org.esa.snap.util.io.TreeCopier;
import org.jpy.PyLib;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.esa.snap.util.SystemUtils.LOG;

/**
 * This class is used to establish the bridge between Java and Python.
 * It unpacks the Python module 'snappy' to a user configurable location
 * and executes the script 'snappy/snappyutil.py' with appropriate parameters.
 * <p>
 * 'snappyutil.py' again configures 'jpy' by selecting and unpacking appropriate
 * jpy tools and binaries found as 'jpy.&lt;platform&gt;-&lt;python-version&gt;.zip' in the 'lib' resources folder
 * of this Java module.
 * 'snappyutil.py' will then call 'jpyutil.py' to write the Java- and Python-side configuration files
 * 'jpyutil.properties' and 'jpyconfig.py'.
 * <p>
 * Then, 'jpyutil.properties' will be used by jpy's {@code PyLib} class to identify its correct binaries.
 * {@code PyLib} is finally used to start an embedded Python interpreter using the shared Python library that belongs
 * to the Python interpreter that was used to execute 'snappyutil.py'.
 * <p>
 * The following system properties can be used to configure this class:
 * <p>
 * <ol>
 * <li>{@code snap.pythonModuleDir}: The directory in which the Python module 'snappy' will be installed. The default value is {@code "~/modules/snap-python"}.</li>
 * <li>{@code snap.pythonExecutable}: The Python executable to be used with SNAP. The default value is {@code "python"}.</li>
 * <li>{@code snap.forcePythonConfig}: Forces reconfiguration of the bridge for each SNAP run. The default value is {@code "true"}</li>
 * </ol>
 *
 * @author Norman Fomferra
 */
public class PyBridge {

    public static final String SNAPPYUTIL_PY_FILENAME = "snappyutil.py";
    public static final String SNAPPYUTIL_LOG_FILENAME = "snappyutil.log";
    public static final String FORCE_PYTHON_CONFIG_PROPERTY = "snap.forcePythonConfig";
    public static final String PYTHON_EXECUTABLE_PROPERTY = "snap.pythonExecutable";
    public static final String PYTHON_MODULE_INSTALL_DIR_PROPERTY = "snap.pythonModuleDir";
    public static final String JPY_JAVA_API_CONFIG_FILENAME = "jpyconfig.properties";
    public static final String JPY_DEBUG_PROPERTY = "jpy.debug";
    public static final String JPY_CONFIG_PROPERTY = "jpy.config";

    private static final Path MODULE_CODE_BASE_PATH = findModuleCodeBasePath();
    public static final String SNAPPY_DIR_NAME = "snappy";

    private static boolean established;

    /**
     * Establishes the SNAP-Python bridge.
     */
    public synchronized static void establish() throws IOException {
        if (established) {
            return;
        }

        Path pythonExecutable = getPythonExecutable();
        Path pythonModuleInstallDir = getPythonModuleInstallDir();
        boolean forcePythonConfig = isForcePythonConfig();

        installPythonModule(pythonExecutable,
                            pythonModuleInstallDir,
                            forcePythonConfig);

        synchronized (PyLib.class) {
            if (!established) {
                startPython(pythonModuleInstallDir);
                established = true;
            }
        }
    }

    /**
     * Installs the SNAP-Python interface.
     *
     * @param pythonExecutable The Python executable.
     * @param pythonModuleInstallDir The directory into which the 'snappy' Python module will be installed and configured.
     * @param forcePythonConfig If {@code true}, any existing installation / configuration will be overwritten.
     * @return The path to the configured 'snappy' Python module.
     * @throws IOException
     */
    public synchronized static Path installPythonModule(Path pythonExecutable,
                                                        Path pythonModuleInstallDir,
                                                        Boolean forcePythonConfig) throws IOException {
        if (pythonExecutable == null) {
            pythonExecutable = getPythonExecutable();
        }

        if (pythonModuleInstallDir == null) {
            pythonModuleInstallDir = getPythonModuleInstallDir();
        }

        if (forcePythonConfig == null) {
            forcePythonConfig = isForcePythonConfig();
        }

        Path snappyDir = pythonModuleInstallDir.resolve(SNAPPY_DIR_NAME);
        if (forcePythonConfig || !Files.isDirectory(snappyDir)) {
            unpackPythonModuleDir(snappyDir);
        }

        Path jpyConfigFile = snappyDir.resolve(JPY_JAVA_API_CONFIG_FILENAME);
        if (forcePythonConfig || !Files.exists(jpyConfigFile)) {
            // Configure jpy Python-side
            configureJpy(pythonExecutable, snappyDir);
        }
        if (!Files.exists(jpyConfigFile)) {
            throw new IOException(String.format("Python configuration incomplete.\n" +
                                                        "Missing file '%s'.\n" +
                                                        "Please check log file '%s'.",
                                                jpyConfigFile,
                                                snappyDir.resolve(SNAPPYUTIL_LOG_FILENAME)));
        }

        // Configure jpy Java-side
        System.setProperty(JPY_CONFIG_PROPERTY, jpyConfigFile.toString());
        if (Debug.isEnabled() && System.getProperty(JPY_DEBUG_PROPERTY) == null) {
            System.setProperty(JPY_DEBUG_PROPERTY, "true");
        }

        return snappyDir;
    }

    /**
     * Extends Python's system path (it's global {@code sys.path} variable) by the given path, if not already present.
     *
     * @param path The new module path.
     */
    public static void extendSysPath(String path) {
        if (path != null) {
            String code = String.format("" +
                                                "import sys;\n" +
                                                "p = '%s';\n" +
                                                "if not p in sys.path: sys.path.append(p)",
                                        path.replace("\\", "\\\\"));
            PyLib.execScript(code);
        }
    }

    private static void configureJpy(Path pythonExecutable, Path snappyDir) throws IOException {
        LOG.info("Configuring SNAP-Python bridge...");

        // "java.home" is always present
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable.toString());
        command.add(SNAPPYUTIL_PY_FILENAME);
        command.add("--snap_home");
        command.add(System.getProperty("snap.home", Paths.get(".").toAbsolutePath().normalize().toString()));
        //command.add(SystemUtils.getApplicationHomeDir().getPath());
        command.add("--java_module");
        command.add(toFilePath(MODULE_CODE_BASE_PATH).toString());
        command.add("--force");
        command.add("--log_file");
        command.add(SNAPPYUTIL_LOG_FILENAME);
        if (Debug.isEnabled()) {
            command.add("--log_level");
            command.add("DEBUG");
        }
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            command.add("--java_home");
            command.add(javaHome);
        }
        String osArch = System.getProperty("os.arch");  // "os.arch" is always present
        if (osArch != null) {
            command.add("--req_arch");
            command.add(osArch);
        }
        String commandLine = toCommandLine(command);
        LOG.info(String.format("Executing command: [%s]\n", commandLine));
        try {
            Process process = new ProcessBuilder()
                    .command(command)
                    .directory(snappyDir.toFile()).start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new IOException(String.format("Python configuration failed.\nCommand [%s]\nfailed with return code %s.", commandLine, exitCode));
            }
        } catch (InterruptedException e) {
            throw new IOException(String.format("Python configuration failed.\nCommand [%s]\nfailed with exception %s.", commandLine, e.getMessage()), e);
        }
    }

    private static String toCommandLine(List<String> command) {
        StringBuilder sb = new StringBuilder();
        for (String arg : command) {
            if (sb.length() > 0) {
                sb.append(" ");
            }
            sb.append(arg.contains(" ") ? String.format("\"%s\"", arg) : arg);
        }
        return sb.toString();
    }

    private static Path getResourcePath(String resource) {
        return MODULE_CODE_BASE_PATH.resolve(resource);
    }

    private static Path findModuleCodeBasePath() {
        try {
            URI uri = PyBridge.class.getProtectionDomain().getCodeSource().getLocation().toURI();
            try {
                return Paths.get(uri);
            } catch (FileSystemNotFoundException exp) {
                FileSystems.newFileSystem(uri, Collections.emptyMap());
                return Paths.get(uri);
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException("Failed to detect the module's code base path", e);
        }
    }

    private static Path toFilePath(Path path) {
        String prefix = "jar:";
        String suffix = "!/";
        String uriString = path.toUri().toString();
        if (uriString.startsWith(prefix)) {
            if (uriString.endsWith(suffix)) {
                uriString = uriString.substring(prefix.length(), uriString.length() - suffix.length());
            } else {
                int pos = uriString.indexOf(suffix);
                if (pos > 0) {
                    uriString = uriString.substring(prefix.length(), pos);
                } else {
                    uriString = uriString.substring(prefix.length());
                }
            }
            path = Paths.get(URI.create(uriString));
        }
        if (!(Files.isDirectory(path) || Files.isRegularFile(path))) {
            throw new IllegalArgumentException("path: " + path);
        }
        return path;
    }

    private static void unpackPythonModuleDir(Path pythonModuleDir) throws IOException {
        Files.createDirectories(pythonModuleDir);
        TreeCopier.copy(getResourcePath(SNAPPY_DIR_NAME), pythonModuleDir);
        LOG.info("SNAP-Python module directory: " + pythonModuleDir);
    }

    private static boolean isForcePythonConfig() {
        return System.getProperty(FORCE_PYTHON_CONFIG_PROPERTY, "true").equalsIgnoreCase("true");
    }

    private static Path getPythonExecutable() {
        return Paths.get(System.getProperty(PYTHON_EXECUTABLE_PROPERTY, "python"));
    }

    private static Path getPythonModuleInstallDir() {
        Path pythonModuleInstallDir;
        String pythonModuleDirStr = System.getProperty(PYTHON_MODULE_INSTALL_DIR_PROPERTY);
        if (pythonModuleDirStr != null) {
            pythonModuleInstallDir = Paths.get(pythonModuleDirStr);
        } else {
            pythonModuleInstallDir = Paths.get(SystemUtils.getApplicationDataDir(true).getPath(), "snap-python");
        }
        return pythonModuleInstallDir.toAbsolutePath().normalize();
    }


    private static void startPython(Path pythonModuleInstallDir) {
        //PyLib.Diag.setFlags(PyLib.Diag.F_ALL);
        String pythonVersion = PyLib.getPythonVersion();
        LOG.info("Starting Python " + pythonVersion);
        if (!PyLib.isPythonRunning()) {
            PyLib.startPython(pythonModuleInstallDir.toString());
        } else {
            extendSysPath(pythonModuleInstallDir.toString());
        }
    }

}
