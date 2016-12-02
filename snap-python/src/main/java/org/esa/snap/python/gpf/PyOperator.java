package org.esa.snap.python.gpf;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.OperatorException;
import org.esa.snap.core.gpf.Tile;
import org.esa.snap.core.gpf.annotations.OperatorMetadata;
import org.esa.snap.core.gpf.annotations.Parameter;
import org.esa.snap.core.util.SystemUtils;
import org.esa.snap.python.PyBridge;
import org.jpy.PyLib;
import org.jpy.PyModule;
import org.jpy.PyObject;

import java.awt.Rectangle;
import java.io.IOException;
import java.util.Map;

/**
 * An operator which uses Python code to process data products.
 *
 * @author Norman Fomferra
 * @since SNAP 2.0
 */
@OperatorMetadata(alias = "PyOp",
        description = "Uses Python code to process data products",
        version = "1.0",
        authors = "Norman Fomferra",
        internal = true)
public class PyOperator extends Operator {
    private final int COMPUTE_METHOD = 0x01;
    private final int COMPUTE_TILE_METHOD = 0x02;
    private final int COMPUTE_TILE_STACK_METHOD = 0x04;
    private final int DO_EXECUTE_METHOD = 0x08;

    @Parameter(description = "Path to the Python module(s). Can be either an absolute path or relative to the current working directory.", defaultValue = ".")
    private String pythonModulePath;

    @Parameter(description = "Name of the Python module.")
    private String pythonModuleName;

    /**
     * Name of the Python class which implements the {@link PyOperatorDelegate} interface.
     */
    @Parameter(description = "Name of the Python class which implements the operator. Please refer to the SNAP help for details.")
    private String pythonClassName;

    private transient PyModule pyModule;
    private transient PyOperatorDelegate pythonProcessor;

    private transient int computeMethodFlags;


    public String getPythonModulePath() {
        return pythonModulePath;
    }

    public void setPythonModulePath(String pythonModulePath) {
        this.pythonModulePath = pythonModulePath;
    }

    public String getPythonModuleName() {
        return pythonModuleName;
    }

    public void setPythonModuleName(String pythonModuleName) {
        this.pythonModuleName = pythonModuleName;
    }

    public String getPythonClassName() {
        return pythonClassName;
    }

    public void setPythonClassName(String pythonClassName) {
        this.pythonClassName = pythonClassName;
    }


    @Override
    public void initialize() throws OperatorException {
        if (pythonModuleName == null || pythonModuleName.isEmpty()) {
            throw new OperatorException("Missing parameter 'pythonModuleName'");
        }
        if (pythonClassName == null || pythonClassName.isEmpty()) {
            throw new OperatorException("Missing value for parameter 'pythonClassName'");
        }

        try {
            PyBridge.establish();
        } catch (IOException e) {
            throw new OperatorException("Failed to establish Python bridge", e);
        }

        synchronized (PyLib.class) {
            PyBridge.extendSysPath(pythonModulePath);

            String code = String.format("if '%s' in globals(): del %s", pythonModuleName, pythonModuleName);
            PyLib.execScript(code);

            pyModule = PyModule.importModule(pythonModuleName);
            PyObject pythonProcessorImpl = pyModule.call(pythonClassName);
            try {
                pythonProcessorImpl.getAttribute("compute");
                computeMethodFlags |= COMPUTE_METHOD;
                SystemUtils.LOG.warning(String.format("Python class %s.%s (path %s):\n"
                                                              + "The method compute(self, context, tiles, rectangle) is deprecated.\n"
                                                              + "Please replace it by computeTileStack(self, context, tiles, rectangle) or\n"
                                                              + "computeTile(self, context, band, tile) if your band's tiles can be\n"
                                                              + "computed independently.",
                                                      pythonModuleName, pythonClassName, pythonModulePath));
            } catch (RuntimeException e) {
                // attribute "compute" not found
            }
            try {
                pythonProcessorImpl.getAttribute("doExecute");
                computeMethodFlags |= DO_EXECUTE_METHOD;
            } catch (RuntimeException e) {
                // attribute "doExecute" not found
            }
            try {
                pythonProcessorImpl.getAttribute("computeTile");
                computeMethodFlags |= COMPUTE_TILE_METHOD;
            } catch (RuntimeException e) {
                // attribute "computeTile" not found
            }
            try {
                pythonProcessorImpl.getAttribute("computeTileStack");
                computeMethodFlags |= COMPUTE_TILE_STACK_METHOD;
            } catch (RuntimeException e) {
                // attribute "computeTileStack" not found
            }
            if (computeMethodFlags == 0) {
                throw new OperatorException("Neither doExecute(self, pm), computeTile(self, context, band, tile), " +
                                            "nor computeTileStack(self, context, tiles, rectangle) method found.");
            }
            pythonProcessor = pythonProcessorImpl.createProxy(PyOperatorDelegate.class);
            pythonProcessor.initialize(this);
        }
    }

    @Override
    public void doExecute(ProgressMonitor pm) {
        synchronized (PyLib.class) {
            if ((computeMethodFlags & DO_EXECUTE_METHOD) != 0) {
                pythonProcessor.doExecute(pm);
            }
        }
    }

    @Override
    public boolean canComputeTile() {
        return (computeMethodFlags & COMPUTE_TILE_METHOD) != 0;
    }

    @Override
    public boolean canComputeTileStack() {
        return (computeMethodFlags & COMPUTE_METHOD) != 0
                || (computeMethodFlags & COMPUTE_TILE_STACK_METHOD) != 0;
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        synchronized (PyLib.class) {
            //System.out.println("computeTileStack: thread = " + Thread.currentThread());
            //PyLib.Diag.setFlags(PyLib.Diag.F_EXEC);
            if ((computeMethodFlags & COMPUTE_TILE_METHOD) != 0) {
                pythonProcessor.computeTile(this, targetBand, targetTile);
            } else {
                throw new OperatorException("Missing computeTile(self, context, band, tile) method.");
            }
            //PyLib.Diag.setFlags(PyLib.Diag.F_OFF);
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        synchronized (PyLib.class) {
            //System.out.println("computeTileStack: thread = " + Thread.currentThread());
            //PyLib.Diag.setFlags(PyLib.Diag.F_EXEC);
            if ((computeMethodFlags & COMPUTE_METHOD) != 0) {
                pythonProcessor.compute(this, targetTiles, targetRectangle);
            } else if ((computeMethodFlags & COMPUTE_TILE_STACK_METHOD) != 0) {
                pythonProcessor.computeTileStack(this, targetTiles, targetRectangle);
            } else {
                throw new OperatorException("Missing computeTileStack(self, context, tiles, rectangle) method.");
            }
            //PyLib.Diag.setFlags(PyLib.Diag.F_OFF);
        }
    }

    @Override
    public void dispose() {
        synchronized (PyLib.class) {
            //System.out.println("dispose: thread = " + Thread.currentThread());
            pythonProcessor.dispose(this);
        }
    }

}
