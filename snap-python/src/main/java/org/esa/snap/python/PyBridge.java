package org.esa.snap.python;

import org.esa.snap.util.Debug;
import org.esa.snap.util.SystemUtils;
import org.esa.snap.util.io.TreeCopier;
import org.jpy.PyLib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import java.util.Set;

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

    public static final String PYTHON_EXECUTABLE_PROPERTY = "snap.pythonExecutable";
    public static final String PYTHON_MODULE_INSTALL_DIR_PROPERTY = "snap.pythonModuleDir";
    public static final String FORCE_PYTHON_CONFIG_PROPERTY = "snap.forcePythonConfig";

    private static final Path MODULE_CODE_BASE_PATH = findModuleCodeBasePath();
    private static final Path PYTHON_CONFIG_DIR = Paths.get(SystemUtils.getApplicationDataDir(true).getPath(), "snap-python");
    private static final String JPY_DEBUG_PROPERTY = "jpy.debug";
    private static final String JPY_CONFIG_PROPERTY = "jpy.config";
    private static final String SNAPPY_NAME = "snappy";
    private static final String SNAPPY_PROPERTIES_NAME = "snappy.properties";
    private static final String SNAPPYUTIL_PY_FILENAME = "snappyutil.py";
    private static final String SNAPPYUTIL_LOG_FILENAME = "snappyutil.log";
    private static final String JPY_JAVA_API_CONFIG_FILENAME = "jpyconfig.properties";

    private static boolean established;

    /**
     * Establishes the SNAP-Python bridge.
     */
    public synchronized static void establish() throws IOException {
        if (established) {
            return;
        }

        Path snappyPath = installPythonModule(null, null, null);

        synchronized (PyLib.class) {
            if (!established) {
                startPython(snappyPath.getParent());
                established = true;
            }
        }
    }

    /**
     * Installs the SNAP-Python interface.
     *
     * @param pythonExecutable       The Python executable.
     * @param pythonModuleInstallDir The directory into which the 'snappy' Python module will be installed and configured.
     * @param forcePythonConfig      If {@code true}, any existing installation / configuration will be overwritten.
     * @return The path to the configured 'snappy' Python module.
     * @throws IOException
     */
    public synchronized static Path installPythonModule(Path pythonExecutable,
                                                        Path pythonModuleInstallDir,
                                                        Boolean forcePythonConfig) throws IOException {

        loadPythonConfig();

        if (pythonExecutable == null) {
            pythonExecutable = getPythonExecutable();
        }

        if (pythonModuleInstallDir == null) {
            pythonModuleInstallDir = getPythonModuleInstallDir();
        }

        if (forcePythonConfig == null) {
            forcePythonConfig = isForcePythonConfig();
        }

        Path snappyPath = pythonModuleInstallDir.resolve(SNAPPY_NAME);
        if (forcePythonConfig || !Files.isDirectory(snappyPath)) {
            unpackPythonModuleDir(snappyPath);
            storePythonConfig(pythonExecutable, pythonModuleInstallDir);
        }

        Path jpyConfigFile = snappyPath.resolve(JPY_JAVA_API_CONFIG_FILENAME);
        if (forcePythonConfig || !Files.exists(jpyConfigFile)) {
            // Configure jpy Python-side
            configureJpy(pythonExecutable, snappyPath);
        }
        if (!Files.exists(jpyConfigFile)) {
            throw new IOException(String.format("SNAP-Python configuration incomplete.\n" +
                                                        "Missing file '%s'.\n" +
                                                        "Please check log file '%s'.",
                                                jpyConfigFile,
                                                snappyPath.resolve(SNAPPYUTIL_LOG_FILENAME)));
        }

        // Configure jpy Java-side
        System.setProperty(JPY_CONFIG_PROPERTY, jpyConfigFile.toString());
        if (Debug.isEnabled() && System.getProperty(JPY_DEBUG_PROPERTY) == null) {
            System.setProperty(JPY_DEBUG_PROPERTY, "true");
        }

        return snappyPath;
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
        LOG.info("Configuring SNAP-Python interface...");

        // "java.home" is always present
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable.toString());
        command.add(SNAPPYUTIL_PY_FILENAME);
        command.add("--snap_home");
        command.add(SystemUtils.getApplicationHomeDir().getPath());
        command.add("--java_module");
        command.add(stripJarScheme(MODULE_CODE_BASE_PATH).toString());
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

    private static Path stripJarScheme(Path path) {
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
            try {
                // We must decode the inner URI string first
                uriString = URLDecoder.decode(uriString, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                // ?
            }
            path = Paths.get(URI.create(uriString));
        }

        return path;
    }

    private static void unpackPythonModuleDir(Path pythonModuleDir) throws IOException {
        Files.createDirectories(pythonModuleDir);
        TreeCopier.copy(getResourcePath(SNAPPY_NAME), pythonModuleDir);
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
            pythonModuleInstallDir = PYTHON_CONFIG_DIR;
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


    private static boolean loadPythonConfig() {
        if (Files.isDirectory(PYTHON_CONFIG_DIR)) {
            Path pythonConfigFile = PYTHON_CONFIG_DIR.resolve(SNAPPY_PROPERTIES_NAME);
            if (Files.isRegularFile(pythonConfigFile)) {
                Properties properties = new Properties();
                try {
                    try (BufferedReader bufferedReader = Files.newBufferedReader(pythonConfigFile)) {
                        properties.load(bufferedReader);
                    }
                    Set<String> keys = properties.stringPropertyNames();
                    for (String key : keys) {
                        String value = properties.getProperty(key);
                        if (System.getProperty(key) == null) {
                            System.setProperty(key, value);
                        }
                    }
                    LOG.warning(String.format("SNAP-Python configuration loaded from '%s'", pythonConfigFile));
                    return true;
                } catch (IOException e) {
                    LOG.warning(String.format("Failed to load SNAP-Python configuration from '%s'", pythonConfigFile));
                }
            }
        }
        return false;
    }

    private static boolean storePythonConfig(Path pythonExecutable,
                                             Path pythonModuleInstallDir) {

        Path pythonConfigFile = PYTHON_CONFIG_DIR.resolve(SNAPPY_PROPERTIES_NAME);
        try {
            if (!Files.exists(pythonConfigFile.getParent())) {
                Files.createDirectories(pythonConfigFile.getParent());
            }
            Properties properties = new Properties();
            properties.setProperty(PYTHON_EXECUTABLE_PROPERTY, pythonExecutable.toString());
            properties.setProperty(PYTHON_MODULE_INSTALL_DIR_PROPERTY, pythonModuleInstallDir.toString());
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(pythonConfigFile)) {
                properties.store(bufferedWriter, "Created by " + PyBridge.class.getName());
            }
            LOG.warning(String.format("SNAP-Python configuration written to '%s'", pythonConfigFile));
            return true;
        } catch (IOException e) {
            LOG.warning(String.format("Failed to store SNAP-Python configuration to '%s'", pythonConfigFile));
            return false;
        }
    }
}
