/*
 * Copyright (C) 2015 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.snap.python;

import org.esa.snap.core.util.Debug;
import org.esa.snap.core.util.ResourceInstaller;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.core.util.io.TreeCopier;
import org.esa.snap.runtime.Config;
import org.jpy.PyLib;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;

import static org.esa.snap.core.util.SystemUtils.*;

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
 * <li>{@code snap.pythonExtraPaths}: Extra paths to be searched for SNAP Python extensions such as operators</li>
 * </ol>
 *
 * @author Norman Fomferra
 */
public class PyBridge {

    public static final String PYTHON_EXECUTABLE_PROPERTY = "snap.pythonExecutable";
    public static final String PYTHON_MODULE_DIR_PROPERTY = "snap.pythonModuleDir";
    public static final String FORCE_PYTHON_CONFIG_PROPERTY = "snap.forcePythonConfig";
    public static final String PYTHON_EXTRA_PATHS_PROPERTY = "snap.pythonExtraPaths";
    public static final Path PYTHON_CONFIG_DIR;

    private static final String SNAP_PYTHON_DIRNAME = "snap-python";
    private static final String JPY_DEBUG_PROPERTY = "jpy.debug";
    private static final String JPY_CONFIG_PROPERTY = "jpy.config";
    private static final String SNAPPY_NAME = "snappy";
    private static final String SNAPPY_PROPERTIES_NAME = "snappy.properties";
    private static final String SNAPPYUTIL_PY_FILENAME = "snappyutil.py";
    private static final String SNAPPYUTIL_LOG_FILENAME = "snappyutil.log";
    private static final String JPY_JAVA_API_CONFIG_FILENAME = "jpyconfig.properties";
    private static final Path MODULE_CODE_BASE_PATH;

    private static boolean established;

    static {
        MODULE_CODE_BASE_PATH = findModuleCodeBasePath();
        PYTHON_CONFIG_DIR = SystemUtils.getApplicationDataDir(true).toPath().resolve(SNAP_PYTHON_DIRNAME);
    }

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
     * @param pythonExecutable  The Python executable.
     * @param snappyParentDir   The directory into which the 'snappy' Python module will be installed and configured.
     * @param forcePythonConfig If {@code true}, any existing installation / configuration will be overwritten.
     * @return The path to the configured 'snappy' Python module.
     * @throws IOException if something went wrong during file access
     */
    public synchronized static Path installPythonModule(Path pythonExecutable,
                                                        Path snappyParentDir,
                                                        Boolean forcePythonConfig) throws IOException {

        loadPythonConfig();

        if (pythonExecutable == null) {
            pythonExecutable = getPythonExecutable();
        }

        if (snappyParentDir == null) {
            snappyParentDir = getSnappyParentDir();
        }

        if (forcePythonConfig == null) {
            forcePythonConfig = isForceGeneratingNewPythonConfig();
        }

        Path snappyPath = snappyParentDir.resolve(SNAPPY_NAME);
        if (forcePythonConfig || !Files.isDirectory(snappyPath)) {
            unpackPythonModuleDir(snappyPath);
            storePythonConfig(pythonExecutable, snappyParentDir);
        }

        Path jpyConfigFile = snappyPath.resolve(JPY_JAVA_API_CONFIG_FILENAME);
        if (forcePythonConfig || !Files.exists(jpyConfigFile)) {
            // Configure jpy Python-side
            configureJpy(pythonExecutable, snappyPath);
        }
        if (!Files.exists(jpyConfigFile)) {
            throw new IOException(String.format("SNAP-Python configuration incomplete.\n" +
                                                        "Missing file '%s'.\n" +
                                                        "Please check the log file '%s'.",
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
        command.add(Paths.get(".", SNAPPYUTIL_PY_FILENAME).toString());
        command.add("--snap_home");
        command.add(SystemUtils.getApplicationHomeDir().getPath());
        command.add("--java_module");
        command.add(stripJarScheme(MODULE_CODE_BASE_PATH).toString());
        command.add("--force");
        command.add("--log_file");
        command.add(Paths.get(".", SNAPPYUTIL_LOG_FILENAME).toString());
        if (Debug.isEnabled()) {
            command.add("--log_level");
            command.add("DEBUG");
        }

        String defaultJvmHeapSpace = getDefaultJvmHeapSpace();
        if (defaultJvmHeapSpace != null) {
            command.add("--jvm_max_mem");
            command.add(defaultJvmHeapSpace);
        }

        // "java.home" should always be present
        String javaHome = System.getProperty("java.home");
        if (javaHome != null) {
            command.add("--java_home");
            command.add(javaHome);
        }
        // "os.arch" should always be present
        String osArch = System.getProperty("os.arch");
        if (osArch != null) {
            // Note that we actually need the Java VM's architecture, and not the one of the host OS.
            // But there seems no way to retrieve it using Java JDK 1.8, so we stick to "os.arch".
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
                throw new IOException(
                        String.format("Python configuration failed.\n" +
                                              "Command [%s]\nfailed with return code %s.\n" +
                                              "Please check the log file '%s'.",
                                      commandLine, exitCode, snappyDir.resolve(SNAPPYUTIL_LOG_FILENAME)));
            }
        } catch (InterruptedException e) {
            throw new IOException(
                    String.format("Python configuration failed.\n" +
                                          "Command [%s]\nfailed with exception %s.\n" +
                                          "Please check the log file '%s'.",
                                  commandLine, e.getMessage(), snappyDir.resolve(SNAPPYUTIL_LOG_FILENAME)), e);
        }
    }

    private static String getDefaultJvmHeapSpace() {
        long totalMemory = getTotalPhysicalMemory();
        if (totalMemory > 0) {
            long memory = (long) (totalMemory * 0.7);
            long heapSpace = memory / (1024 * 1024 * 1024);
            return heapSpace+"G";
        } else {
            return null;
        }

    }

    private static long getTotalPhysicalMemory() {
        OperatingSystemMXBean operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        if (operatingSystemMXBean instanceof com.sun.management.OperatingSystemMXBean) {
            com.sun.management.OperatingSystemMXBean sunMXBean = (com.sun.management.OperatingSystemMXBean) operatingSystemMXBean;
            return sunMXBean.getTotalPhysicalMemorySize();
        }
        return -1L;
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
        return ResourceInstaller.findModuleCodeBasePath(PyBridge.class);
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
        LOG.info(String.format("SNAP-Python module '%s' located at %s", SNAPPY_NAME, pythonModuleDir));
    }

    private static boolean isForceGeneratingNewPythonConfig() {
        return Config.instance().preferences().getBoolean(FORCE_PYTHON_CONFIG_PROPERTY, false);
    }

    private static Path getPythonExecutable() {
        return Paths.get(Config.instance().preferences().get(PYTHON_EXECUTABLE_PROPERTY, "python"));
    }

    private static Path getSnappyParentDir() {
        Path pythonModuleInstallDir;
        String pythonModuleDirStr = Config.instance().preferences().get(PYTHON_MODULE_DIR_PROPERTY, null);
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
                        Config.instance().preferences().put(key, value);
                    }
                    LOG.info(String.format("SNAP-Python configuration loaded from '%s'", pythonConfigFile));
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
            properties.setProperty(PYTHON_MODULE_DIR_PROPERTY, pythonModuleInstallDir.toString());
            try (BufferedWriter bufferedWriter = Files.newBufferedWriter(pythonConfigFile)) {
                properties.store(bufferedWriter, "Created by " + PyBridge.class.getName());
            }
            LOG.info(String.format("SNAP-Python configuration written to '%s'", pythonConfigFile));
            return true;
        } catch (IOException e) {
            LOG.warning(String.format("Failed to store SNAP-Python configuration to '%s'", pythonConfigFile));
            return false;
        }
    }
}
