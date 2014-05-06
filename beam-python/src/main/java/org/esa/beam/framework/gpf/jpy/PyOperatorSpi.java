package org.esa.beam.framework.gpf.jpy;

import com.bc.ceres.core.runtime.RuntimeContext;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.descriptor.DefaultOperatorDescriptor;
import org.esa.beam.framework.gpf.descriptor.OperatorDescriptor;
import org.esa.beam.util.logging.BeamLogManager;

import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;

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

    static {
        try {
            BeamLogManager.getSystemLogger().fine("Scanning for Python modules...");
            Enumeration<URL> resources = RuntimeContext.getResources(PY_OP_RESOURCE_NAME);
            while (resources.hasMoreElements()) {
                URL url = resources.nextElement();
                try {
                    BeamLogManager.getSystemLogger().fine("Python module registration file found: " + url);
                    registerPythonModule(url);
                } catch (IOException e) {
                    BeamLogManager.getSystemLogger().warning("Failed to register Python modules seen in " + url + ": " + e.getMessage());
                }
            }
        } catch (IOException e) {
            BeamLogManager.getSystemLogger().severe("Failed scan for Python modules: I/O problem: " + e.getMessage());
        }
    }

    private static void registerPythonModule(URL url) throws IOException {
        int i = url.toString().indexOf(PY_OP_RESOURCE_NAME);
        if (i < 0) {
            return;
        }

        final String moduleDirPath = url.toString().substring(0, i);

        LineNumberReader reader = new LineNumberReader(new InputStreamReader(url.openStream()));
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
                        registerModule(moduleDirPath, split[0].trim(), split[1].trim());
                    } catch (OperatorException e) {
                        BeamLogManager.getSystemLogger().warning(String.format("Invalid Python module entry in %s (line %d): %s", url, reader.getLineNumber(), line));
                        BeamLogManager.getSystemLogger().warning(String.format("Caused by an I/O problem: %s", e.getMessage()));
                    }
                } else {
                    BeamLogManager.getSystemLogger().warning(String.format("Invalid Python module entry in %s (line %d): %s", url, reader.getLineNumber(), line));
                }
            }
        }
    }

    private static void registerModule(String moduleDirUri, String pythonModuleRelPath, final String pythonClassName) throws OperatorException {
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

        final File pythonModuleDir;
        try {
            pythonModuleDir = new File(new URI(moduleDirUri + "/" + pythonModuleRelSubPath));
        } catch (URISyntaxException e) {
            throw new OperatorException(e.getMessage());
        }

        if (!pythonModuleDir.exists()) {
            throw new OperatorException("file not found: " + pythonModuleDir);
        }

        File pythonModuleFile = new File(pythonModuleDir, pythonModuleName + ".py");
        if (!pythonModuleFile.exists()) {
            throw new OperatorException("file not found: " + pythonModuleFile);
        }

        File pythonInfoXmlFile = new File(pythonModuleDir, pythonModuleName + "-info.xml");
        DefaultOperatorDescriptor operatorDescriptor;
        if (pythonInfoXmlFile.exists()) {
            operatorDescriptor = DefaultOperatorDescriptor.fromXml(pythonInfoXmlFile, PyOperatorSpi.class.getClassLoader());
        } else {
            operatorDescriptor = new DefaultOperatorDescriptor(pythonModuleName, PyOperator.class);
            BeamLogManager.getSystemLogger().warning(String.format("Missing operator metadata file '%s'", pythonInfoXmlFile));
        }

        PyOperatorSpi operatorSpi = new PyOperatorSpi(operatorDescriptor) {

            @Override
            public Operator createOperator() throws OperatorException {
                PyOperator pyOperator = (PyOperator) super.createOperator();

                pyOperator.setParameterDefaultValues();
                pyOperator.setPythonModulePath(pythonModuleDir.getPath());
                pyOperator.setPythonModuleName(pythonModuleName);
                pyOperator.setPythonClassName(pythonClassName);
                return pyOperator;
            }
        };

        String operatorName = operatorDescriptor.getAlias() != null ? operatorDescriptor.getAlias() : operatorDescriptor.getName();
        GPF.getDefaultInstance().getOperatorSpiRegistry().addOperatorSpi(operatorName, operatorSpi);
        BeamLogManager.getSystemLogger().info(String.format("Python operator '%s' registered (class '%s' in file '%s')",
                                                            pythonModuleName, pythonClassName, pythonModuleFile));
    }
}
