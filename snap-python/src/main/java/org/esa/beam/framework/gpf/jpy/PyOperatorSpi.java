package org.esa.beam.framework.gpf.jpy;

import com.bc.ceres.core.runtime.RuntimeContext;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.descriptor.DefaultOperatorDescriptor;
import org.esa.beam.framework.gpf.descriptor.OperatorDescriptor;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URI;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;

import static org.esa.beam.util.SystemUtils.LOG;

/**
 * @author Norman Fomferra
 */
public class PyOperatorSpi extends OperatorSpi {

    public static final String PY_OP_RESOURCE_NAME = "META-INF/services/beampy-operators";
    public static boolean installed;

    public PyOperatorSpi() {
        super(PyOperator.class);
        install();
    }

    public PyOperatorSpi(OperatorDescriptor operatorDescriptor) {
        super(operatorDescriptor);
        install();
    }

    private static void install() {
        if (installed) {
            return;
        }
        scanDir(Paths.get(System.getProperty("user.home"), ".snap", "snappy", "ext"));
        scanDirs(System.getProperty("snap.snappy.ext", "").split(File.pathSeparator));
        scanClassPath();
        installed = true;
    }

    private static void scanDirs(String... paths) {
        for (String path : paths) {
            scanDir(Paths.get(path));
        }
    }

    private static void scanDir(Path dir) {
        try {
            LOG.fine("Scanning for Python modules in " + dir + "...");
            Files.list(dir).forEach(path -> {
                Path resolvedPath = path.resolve(PY_OP_RESOURCE_NAME);
                LOG.fine("Python module registration file found: " + resolvedPath);
                //registerPythonModule(resolvedPath);
                // todo
            });
        } catch (IOException e) {
            LOG.severe("Failed scan for Python modules: I/O problem: " + e.getMessage());
        }
    }

    private static void scanClassPath() {
        try {
            LOG.fine("Scanning for Python modules in Java class path...");
            Enumeration<URL> resources = RuntimeContext.getResources(PY_OP_RESOURCE_NAME);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try {
                    LOG.fine("Python module registration file found: " + url);
                    registerPythonModule(url);
                } catch (IOException e) {
                    LOG.warning("Failed to register Python modules seen in " + url + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            LOG.severe("Failed scan for Python modules: I/O problem: " + e.getMessage());
        }
    }

    private static void registerPythonModule(URL resourceUrl) throws IOException {
        String uriString = resourceUrl.toString();
        int pos = uriString.indexOf(PY_OP_RESOURCE_NAME);
        if (pos < 0) {
            return;
        }

        while (pos > 0 && uriString.charAt(pos - 1) == '/') {
            pos--;
        }
        if (pos > 0 && uriString.charAt(pos - 1) == '!') {
            pos--;
        }

        String moduleUriString = uriString.substring(0, pos);

        LineNumberReader reader = new LineNumberReader(new InputStreamReader(resourceUrl.openStream()));
        while (true) {
            String line = reader.readLine();
            if (line == null) {
                break;
            }
            line = line.trim();
            if (!line.isEmpty() && !line.startsWith("#")) {
                String[] split = line.split("\\s+");
                if (split.length == 2) {
                    try {
                        registerModule(moduleUriString, split[0].trim(), split[1].trim());
                    } catch (OperatorException e) {
                        LOG.warning(String.format("Invalid Python module entry in %s (line %d): %s", resourceUrl, reader.getLineNumber(), line));
                        LOG.warning(String.format("Caused by an I/O problem: %s", e.getMessage()));
                    }
                } else {
                    LOG.warning(String.format("Invalid Python module entry in %s (line %d): %s", resourceUrl, reader.getLineNumber(), line));
                }
            }
        }
    }

    private static void registerModule(String moduleDirUri, String pythonModuleRelPath, final String pythonClassName) throws OperatorException, IOException {
        String pythonModuleRelSubPath;
        final String pythonModuleName;
        int i1 = pythonModuleRelPath.lastIndexOf('/');
        if (i1 == 0) {
            pythonModuleRelSubPath = "";
            pythonModuleName = pythonModuleRelPath.substring(1);
        } else if (i1 > 0) {
            pythonModuleRelSubPath = pythonModuleRelPath.substring(0, i1);
            pythonModuleName = pythonModuleRelPath.substring(i1 + 1);
        } else {
            pythonModuleRelSubPath = "";
            pythonModuleName = pythonModuleRelPath;
        }

        FileSystem fs;
        try {
            URI uri = URI.create(moduleDirUri);
            fs = FileSystems.newFileSystem(uri, Collections.emptyMap());
        } catch (IOException e) {
            throw new OperatorException(e);
        }
        if (pythonModuleRelSubPath.isEmpty()) {
            pythonModuleRelSubPath = "/";
        }
        Path pythonModuleDir = fs.getPath(pythonModuleRelSubPath);

        Path pythonModuleFile = pythonModuleDir.resolve(pythonModuleName + ".py");
        if (!Files.exists(pythonModuleFile)) {
            throw new OperatorException("file not found: " + pythonModuleFile);
        }

        Path pythonInfoXmlFile = pythonModuleDir.resolve(pythonModuleName + "-info.xml");
        DefaultOperatorDescriptor operatorDescriptor;
        if (Files.exists(pythonInfoXmlFile)) {
            try (BufferedReader reader = Files.newBufferedReader(pythonInfoXmlFile)) {
                operatorDescriptor = DefaultOperatorDescriptor.fromXml(reader, pythonInfoXmlFile.toString(), PyOperatorSpi.class.getClassLoader());
            }
        } else {
            operatorDescriptor = new DefaultOperatorDescriptor(pythonModuleName, PyOperator.class);
            LOG.warning(String.format("Missing operator metadata file '%s'", pythonInfoXmlFile));
        }

        PyOperatorSpi operatorSpi = new PyOperatorSpi(operatorDescriptor) {

            @Override
            public Operator createOperator() throws OperatorException {
                PyOperator pyOperator = (PyOperator) super.createOperator();

                pyOperator.setParameterDefaultValues();
                pyOperator.setPythonModulePath(pythonModuleDir.toString());
                pyOperator.setPythonModuleName(pythonModuleName);
                pyOperator.setPythonClassName(pythonClassName);
                return pyOperator;
            }
        };

        String operatorName = operatorDescriptor.getAlias() != null ? operatorDescriptor.getAlias() : operatorDescriptor.getName();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(operatorName, operatorSpi);
        LOG.info(String.format("Python operator '%s' registered (class '%s' in file '%s')",
                               pythonModuleName, pythonClassName, pythonModuleFile));
    }
}
