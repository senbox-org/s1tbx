package org.esa.beam.framework.gpf.jpy;

import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.util.Debug;
import org.esa.beam.util.logging.BeamLogManager;
import org.jpy.PyLib;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * This class is used to establish the bridge between Java and Python.
 * It basically let's a given Python interpreter execute the file 'beampyutil.py' found in the 'beampy'
 * folder of the unpacked BEAM-Python module.
 * <p/>
 * 'beampyutil.py' again configures 'jpy' by selecting and unpacking appropriate
 * jpy tools and binaries found as 'jpy.&lt;platform&gt;-&lt;python-version&gt;.zip' in the 'lib' folder of the unpacked BEAM-Python module.
 * 'beampyutil.py' will then call 'jpyutil.py' to write the Java- and Python-side configuration files 'jpyutil.properties'
 * and 'jpyconfig.py'.
  * <p/>
 * Then, 'jpyutil.properties' will be used by jpy's {@code PyLib} class to identify its correct binaries.
 * {@code PyLib} is finally used to start an embedded Python interpreter using the shared Python library that belongs to the Python
 * interpreter that was used to execute 'beampyutil.py'.
 * <p/>
 * The following system properties can be used to configure this class:
 * <p/>
 * <ol>
 * <li>{@code beam.pythonExecutable}: The python executable to be used with BEAM. The default value is {@code "python"}.</li>
 * <li>{@code beam.forcePythonConfig}: Forces reconfiguration of the bridge for each BEAM run. The default value is {@code "true"}</li>
 * </ol>
 *
 * @author Norman Fomferra
 */
public class PyBridge {

    public static final String BEAMPYUTIL_PY_FILENAME = "beampyutil.py";
    public static final String BEAMPYUTIL_LOG_FILENAME = "beampyutil.log";
    public static final String JPY_JAVA_API_CONFIG_FILENAME = "jpyconfig.properties";

    private static boolean established;
    private static File beampyDir;

    /**
     * Establishes the BEAM-Python bridge.
     */
    public synchronized static void establish() {

        if (established) {
            return;
        }

        beampyDir = getResourceFile("/beampy");
        if (beampyDir == null) {
            throw new OperatorException("Can't find BEAM-Python module directory.\n" +
                                        "(Make sure the BEAM-Python module is unpacked.)");
        }

        BeamLogManager.getSystemLogger().info("BEAM-Python module directory: " + beampyDir);

        boolean forcePythonConfig = System.getProperty("beam.forcePythonConfig", "true").equalsIgnoreCase("true");
        File jpyConfigFile = new File(beampyDir, JPY_JAVA_API_CONFIG_FILENAME);
        if (forcePythonConfig || !jpyConfigFile.exists()) {
            configureJpy();
        }
        if (!jpyConfigFile.exists()) {
            throw new OperatorException(String.format("Python configuration incomplete.\n" +
                                                      "Missing file '%s'.\n" +
                                                      "Please check '%s'.",
                                                      jpyConfigFile,
                                                      new File(beampyDir, BEAMPYUTIL_LOG_FILENAME)));
        }

        System.setProperty("jpy.config", jpyConfigFile.getPath());
        if (Debug.isEnabled() && System.getProperty("jpy.debug") == null) {
            System.setProperty("jpy.debug", "true");
        }

        synchronized (PyLib.class) {
            if (!established) {
                //PyLib.Diag.setFlags(PyLib.Diag.F_ALL);
                String pythonVersion = PyLib.getPythonVersion();
                BeamLogManager.getSystemLogger().info("Running Python " + pythonVersion);
                if (!PyLib.isPythonRunning()) {
                    PyLib.startPython(beampyDir.getPath());
                } else {
                    extendSysPath(beampyDir.getPath());
                }
                established = true;
            }
        }
    }

    /**
     * Extends Python's system path (it's global {@code sys.path} variable) by the given path, if not already present.
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

    private static void configureJpy() {
        BeamLogManager.getSystemLogger().info("Configuring BEAM-Python bridge...");

        String pythonExecutable = System.getProperty("beam.pythonExecutable", "python");

        // "java.home" is always present
        List<String> command = new ArrayList<>();
        command.add(pythonExecutable);
        command.add(BEAMPYUTIL_PY_FILENAME);
        command.add("--force");
        command.add("--log_file");
        command.add(BEAMPYUTIL_LOG_FILENAME);
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
        BeamLogManager.getSystemLogger().info(String.format("Executing command: [%s]\n", commandLine));
        try {
            Process process = new ProcessBuilder()
                    .command(command)
                    .directory(beampyDir).start();
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new OperatorException(String.format("Python configuration failed.\nCommand [%s]\nfailed with return code %s.", commandLine, exitCode));
            }
        } catch (IOException | InterruptedException e) {
            throw new OperatorException(String.format("Python configuration failed.\nCommand [%s]\nfailed with exception %s.", commandLine, e.getMessage()), e);
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

    private static File getResourceFile(String resourcePath) {
        URL resourceUrl = PyBridge.class.getResource(resourcePath);
        //System.out.println("resourceUrl = " + resourceUrl);
        if (resourceUrl != null) {
            try {
                File resourceFile = new File(resourceUrl.toURI());
                //System.out.println("resourceFile = " + resourceFile);
                if (resourceFile.exists()) {
                    return resourceFile;
                }
            } catch (URISyntaxException e) {
                // mmmmh
            }
        }
        return null;
    }

}
