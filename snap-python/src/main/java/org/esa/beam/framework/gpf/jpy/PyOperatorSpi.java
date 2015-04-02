package org.esa.beam.framework.gpf.jpy;

import com.bc.ceres.core.ResourceLocator;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.descriptor.DefaultOperatorDescriptor;
import org.esa.beam.framework.gpf.descriptor.OperatorDescriptor;
import org.esa.beam.util.SystemUtils;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collection;
import java.util.logging.Level;

import static org.esa.beam.util.SystemUtils.LOG;

/**
 * @author Norman Fomferra
 */
public class PyOperatorSpi extends OperatorSpi {

    public static final String PY_OP_RESOURCE_NAME = "META-INF/services/snappy-operators";

    public PyOperatorSpi() {
        super(PyOperator.class);
    }

    public PyOperatorSpi(OperatorDescriptor operatorDescriptor) {
        super(operatorDescriptor);
    }

    static final String EXT_PROPERTY_NAME = "snap.snappy.ext";

    static {
        scanDir(Paths.get(SystemUtils.getApplicationDataDir(true).getPath(), "snappy", "ext"));
        scanDirs(System.getProperty(EXT_PROPERTY_NAME, "").split(File.pathSeparator));
        scanClassPath();
    }

    private static void scanDirs(String... paths) {
        for (String path : paths) {
            scanDir(Paths.get(path));
        }
    }

    private static void scanDir(Path dir) {
        if (Files.exists(dir)) {
            try {
                LOG.fine("Scanning for Python modules in directory " + dir);
                Files.list(dir).forEach(path -> {
                    registerPythonModule(path.resolve(PY_OP_RESOURCE_NAME));
                });
            } catch (IOException e) {
                LOG.log(Level.SEVERE, "Failed scan for Python modules: " + e.getMessage(), e);
            }
        } else {
            LOG.warning("Ignoring non-existent Python module path: " + dir);
        }
    }

    private static void scanClassPath() {
        LOG.fine("Scanning for Python modules in Java class path...");
        Collection<Path> resources = ResourceLocator.getResources(PY_OP_RESOURCE_NAME);
        resources.forEach(PyOperatorSpi::registerPythonModule);
    }

    private static void registerPythonModule(Path resourcePath) {
        if (!Files.exists(resourcePath) || !resourcePath.endsWith(PY_OP_RESOURCE_NAME)) {
            return;
        }

        Path moduleRoot = subtract(resourcePath, Paths.get(PY_OP_RESOURCE_NAME).getNameCount());
        LOG.info("Python module root found: " + moduleRoot.toUri());

        try {
            Files.lines(resourcePath).forEach(line -> {
                line = line.trim();
                if (!line.isEmpty() && !line.startsWith("#")) {
                    String[] split = line.split("\\s+");
                    if (split.length == 2) {
                        String modulePath = split[0].trim();
                        String className = split[1].trim();
                        if (!registerModule(moduleRoot, modulePath, className)) {
                            LOG.warning(String.format("Python module not installed: invalid entry in %s: %s", resourcePath, line));
                        }
                    } else {
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
