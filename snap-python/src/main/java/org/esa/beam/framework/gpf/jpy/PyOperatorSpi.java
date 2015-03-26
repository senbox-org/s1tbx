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

    public static final String PY_OP_RESOURCE_NAME = "META-INF/services/beampy-operators";

    public PyOperatorSpi() {
        super(PyOperator.class);
    }

    public PyOperatorSpi(OperatorDescriptor operatorDescriptor) {
        super(operatorDescriptor);
    }

    static final String EXT_PROPERTY_NAME = "snap.beampy.ext";

    static {
        scanDir(Paths.get(SystemUtils.getApplicationDataDir(true).getPath(), "beampy", "ext"));
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

    private static boolean registerModule(Path moduleRoot, String moduleRelPath, final String pythonClassName) {
        String pythonModuleRelSubPath;
        final String pythonModuleName;
        int i1 = moduleRelPath.lastIndexOf('/');
        if (i1 == 0) {
            pythonModuleRelSubPath = "";
            pythonModuleName = moduleRelPath.substring(1);
        } else if (i1 > 0) {
            pythonModuleRelSubPath = moduleRelPath.substring(0, i1);
            pythonModuleName = moduleRelPath.substring(i1 + 1);
        } else {
            pythonModuleRelSubPath = "";
            pythonModuleName = moduleRelPath;
        }

        Path pythonModuleDir = moduleRoot.resolve(pythonModuleRelSubPath);

        Path pythonModuleFile = pythonModuleDir.resolve(pythonModuleName + ".py");
        if (!Files.exists(pythonModuleFile)) {
            LOG.severe(String.format("Missing Python module '%s'", pythonModuleFile));
            return false;
        }

        Path pythonInfoXmlFile = pythonModuleDir.resolve(pythonModuleName + "-info.xml");
        DefaultOperatorDescriptor operatorDescriptor;
        if (Files.exists(pythonInfoXmlFile)) {
            try {
                try (BufferedReader reader = Files.newBufferedReader(pythonInfoXmlFile)) {
                    operatorDescriptor = DefaultOperatorDescriptor.fromXml(reader, pythonInfoXmlFile.toString(), PyOperatorSpi.class.getClassLoader());
                }
            } catch (IOException e) {
                LOG.severe(String.format("Failed to read from '%s'", pythonInfoXmlFile));
                return false;
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
        LOG.info(String.format("Python operator '%s' registered (Python module: '%s', class: '%s', URI: '%s')",
                               operatorName, pythonModuleName, pythonClassName, pythonModuleDir.toUri()));
        return true;
    }
}
