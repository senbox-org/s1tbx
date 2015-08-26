package org.esa.snap.python.gpf;

import com.bc.ceres.core.ResourceLocator;
import org.esa.snap.framework.gpf.GPF;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.OperatorException;
import org.esa.snap.framework.gpf.OperatorSpi;
import org.esa.snap.framework.gpf.descriptor.DefaultOperatorDescriptor;
import org.esa.snap.framework.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.python.PyBridge;
import org.esa.snap.runtime.Config;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.logging.Level;

import static org.esa.snap.util.SystemUtils.LOG;

/**
 * The service provider interface (SPI) for the SNAP Python operator.
 *
 * @author Norman Fomferra
 * @since SNAP 2.0
 */
public class PyOperatorSpi extends OperatorSpi {

    public static final String PY_OP_SPI_RESOURCE_NAME = "META-INF/services/" + PyOperatorSpi.class.getName();

    public PyOperatorSpi() {
        super(PyOperator.class);
    }

    public PyOperatorSpi(OperatorDescriptor operatorDescriptor) {
        super(operatorDescriptor);
    }

    static {
        scanPathForPyOps(PyBridge.PYTHON_CONFIG_DIR);
        String extraPaths = Config.instance().preferences().get(PyBridge.PYTHON_EXTRA_PATHS_PROPERTY, null);
        if (extraPaths != null) {
            scanPathsForPyOps(extraPaths.split(File.pathSeparator));
        }
        scanClassPathForPyOps();
    }

    private static void scanPathsForPyOps(String... paths) {
        for (String path : paths) {
            scanPathForPyOps(Paths.get(path));
        }
    }

    private static void scanPathForPyOps(Path path) {
        // Note we may allow for zip files here later!
        if (Files.isDirectory(path)) {
            try {
                LOG.fine("Scanning for Python modules in " + path);
                Files.list(path).forEach(entry -> {
                    registerPyOpModule(entry.resolve(PY_OP_SPI_RESOURCE_NAME));
                });
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed scan for Python modules: " + e.getMessage(), e);
            }
        } else {
            LOG.warning("Ignoring non-existent Python module path: " + path);
        }
    }

    private static void scanClassPathForPyOps() {
        LOG.fine("Scanning for Python modules in Java class path...");
        Collection<Path> resources = ResourceLocator.getResources(PY_OP_SPI_RESOURCE_NAME);
        resources.forEach(PyOperatorSpi::registerPyOpModule);
    }

    private static void registerPyOpModule(Path resourcePath) {
        if (!Files.exists(resourcePath) || !resourcePath.endsWith(PY_OP_SPI_RESOURCE_NAME)) {
            return;
        }

        Path moduleRoot = subtract(resourcePath, Paths.get(PY_OP_SPI_RESOURCE_NAME).getNameCount());
        LOG.info("SNAP-Python module root found: " + moduleRoot.toUri());

        try {
            Files.lines(resourcePath).forEach(line -> {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    int lastDotPos = line.lastIndexOf('.');
                    String modulePath = "";
                    String className = "";
                    if (lastDotPos > 0) {
                        modulePath = line.substring(0, lastDotPos);
                        className = line.substring(lastDotPos + 1);
                        if (!registerModule(moduleRoot, modulePath, className)) {
                            LOG.warning(String.format("Python module not installed: invalid entry in %s: %s", resourcePath, line));
                        }
                    }
                    if (modulePath.isEmpty() || className.isEmpty()) {
                        LOG.warning(String.format("Invalid Python module entry in %s: %s", resourcePath, line));
                    }
                }
            });
        } catch (IOException e) {
            LOG.log(Level.SEVERE, "Python modules not installed: " + e.getMessage(), e);
        }
    }

    private static Path subtract(Path resourcePath, int nameCount) {
        Path moduleRoot = resourcePath;
        for (int i = 0; i < nameCount; i++) {
            moduleRoot = moduleRoot.resolve("..");
        }
        moduleRoot = moduleRoot.normalize();
        return moduleRoot;
    }

    private static boolean registerModule(Path pythonModuleRoot, String pythonModuleName, final String pythonClassName) {

        String pythonModuleRelPath = pythonModuleName.replace('.', '/');

        Path pythonModuleFile = pythonModuleRoot.resolve(pythonModuleRelPath + ".py");
        if (!Files.exists(pythonModuleFile)) {
            LOG.severe(String.format("Missing Python module '%s'", pythonModuleFile.toUri()));
            return false;
        }

        Path pythonInfoXmlFile = pythonModuleRoot.resolve(pythonModuleRelPath + "-info.xml");
        if (!Files.exists(pythonInfoXmlFile)) {
            LOG.warning(String.format("Missing operator metadata file '%s'. Using defaults.", pythonInfoXmlFile.toUri()));
        }

        DefaultOperatorDescriptor operatorDescriptor = createOperatorDescriptor(pythonInfoXmlFile, pythonModuleName);
        File pythonModuleRootFile = getPythonModuleRootFile(pythonModuleRoot);

        PyOperatorSpi operatorSpi = new PyOperatorSpi(operatorDescriptor) {

            @Override
            public Operator createOperator() throws OperatorException {
                PyOperator pyOperator = (PyOperator) super.createOperator();

                pyOperator.setParameterDefaultValues();
                pyOperator.setPythonModulePath(pythonModuleRootFile.getPath());
                pyOperator.setPythonModuleName(pythonModuleName);
                pyOperator.setPythonClassName(pythonClassName);
                return pyOperator;
            }
        };

        String operatorName = operatorDescriptor.getAlias() != null ? operatorDescriptor.getAlias() : operatorDescriptor.getName();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(operatorName, operatorSpi);
        LOG.info(String.format("Python operator '%s' registered (Python module: '%s', class: '%s', root: '%s')",
                               operatorName, pythonModuleName, pythonClassName, pythonModuleRootFile));
        return true;
    }

    private static DefaultOperatorDescriptor createOperatorDescriptor(Path pythonInfoXmlFile, String pythonModuleName) {
        DefaultOperatorDescriptor operatorDescriptor;
        if (Files.exists(pythonInfoXmlFile)) {
            try {
                try (BufferedReader reader = Files.newBufferedReader(pythonInfoXmlFile)) {
                    operatorDescriptor = DefaultOperatorDescriptor.fromXml(reader, pythonInfoXmlFile.toUri().toString(), PyOperatorSpi.class.getClassLoader());
                }
            } catch (IOException e) {
                LOG.severe(String.format("Failed to read from '%s'", pythonInfoXmlFile));
                operatorDescriptor = null;
            }
        } else {
            operatorDescriptor = new DefaultOperatorDescriptor(pythonModuleName, PyOperator.class);
        }
        return operatorDescriptor;
    }

    static File getPythonModuleRootFile(Path moduleRoot) {
        URI uri = moduleRoot.toUri();
        if ("jar".equals(uri.getScheme())) {
            // get the ZIP file from URI of form "jar:file:<path>!/"
            String schemeSpecificPart = uri.getSchemeSpecificPart();
            if (schemeSpecificPart != null && schemeSpecificPart.startsWith("file:")) {
                int pos = schemeSpecificPart.lastIndexOf('!');
                if (pos > 0) {
                    if ("/".equals(schemeSpecificPart.substring(pos + 1))) {
                        String fileUriString = schemeSpecificPart.substring(0, pos);
                        if (fileUriString.startsWith("file:")) {
                            return new File(URI.create(fileUriString));
                        }
                    }
                } else {
                    return new File(URI.create(schemeSpecificPart));
                }
            }
        }
        return moduleRoot.toFile();
    }
}
