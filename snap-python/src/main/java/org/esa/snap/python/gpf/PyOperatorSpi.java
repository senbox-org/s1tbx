package org.esa.snap.python.gpf;

import org.esa.snap.core.gpf.GPF;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.OperatorSpi;
import org.esa.snap.core.gpf.descriptor.DefaultOperatorDescriptor;
import org.esa.snap.core.gpf.descriptor.OperatorDescriptor;
import org.esa.snap.core.util.ServiceFinder;
import org.esa.snap.core.util.io.FileUtils;
import org.esa.snap.python.PyBridge;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.esa.snap.core.util.SystemUtils.*;

/**
 * The service provider interface (SPI) for the SNAP Python operator.
 *
 * @author Norman Fomferra
 * @since SNAP 2.0
 */
public class PyOperatorSpi extends OperatorSpi {

    public PyOperatorSpi() {
        super(PyOperator.class);
    }

    public PyOperatorSpi(OperatorDescriptor operatorDescriptor) {
        super(operatorDescriptor);
    }

    static {
        ServiceFinder serviceFinder = new ServiceFinder(PyOperatorSpi.class);
        if (Files.isDirectory(PyBridge.PYTHON_CONFIG_DIR)) {
            serviceFinder.addSearchPath(PyBridge.PYTHON_CONFIG_DIR);
        }
        serviceFinder.addSearchPathsFromPreferences(PyBridge.PYTHON_EXTRA_PATHS_PROPERTY);
        serviceFinder.setUseClassPath(true);
        for (ServiceFinder.Module module : serviceFinder.findServices()) {
            for (String pythonClassName : module.getServiceNames()) {
                registerPythonOp(module.getPath(), pythonClassName);
            }
        }
    }

    private static void registerPythonOp( Path moduleRoot, String pythonClassName) {
        int lastDotPos = pythonClassName.lastIndexOf('.');
        String modulePath = "";
        String className = "";
        if (lastDotPos > 0) {
            modulePath = pythonClassName.substring(0, lastDotPos);
            className = pythonClassName.substring(lastDotPos + 1);
            if (!registerModule(moduleRoot, modulePath, className)) {
                LOG.warning(String.format("Python module not installed: invalid entry in %s: %s", moduleRoot, pythonClassName));
            }
        }
        if (modulePath.isEmpty() || className.isEmpty()) {
            LOG.warning(String.format("Invalid Python module entry in %s: %s", moduleRoot, pythonClassName));
        }
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
        File pythonModuleRootFile = FileUtils.toFile(pythonModuleRoot);

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

}
