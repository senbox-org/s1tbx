package org.esa.beam.framework.gpf.jpy;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.jpy.PyLib;
import org.jpy.PyModule;
import org.jpy.PyObject;

import java.awt.Rectangle;
import java.util.Map;

/**
 * An operator which uses Python code to process data products.
 *
 * @author Norman Fomferra
 * @since BEAM 5
 */
@OperatorMetadata(alias = "PyOp",
                  description = "Uses Python code to process data products",
                  version = "0.8",
                  authors = "N. Fomferra",
                  internal = true)
public class PyOperator extends Operator {

    @Parameter(description = "Path to the Python module(s). Can be either an absolute path or relative to the current working directory.", defaultValue = ".")
    private String pythonModulePath;

    @Parameter(description = "Name of the Python module.")
    private String pythonModuleName;

    /**
     * Name of the Python class which implements the {@link org.esa.beam.framework.gpf.jpy.PyOperator.PythonProcessor} interface.
     */
    @Parameter(description = "Name of the Python class which implements the operator. Please refer to the BEAM help for details.")
    private String pythonClassName;

    private transient PyModule pyModule;
    private transient PythonProcessor pythonProcessor;


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

        PyBridge.establish();

        synchronized (PyLib.class) {
            PyBridge.extendSysPath(pythonModulePath);

            String code = String.format("if '%s' in globals(): del %s", pythonModuleName, pythonModuleName);
            PyLib.execScript(code);

            pyModule = PyModule.importModule(pythonModuleName);
            PyObject pythonProcessorImpl = pyModule.call(pythonClassName);
            pythonProcessor = pythonProcessorImpl.createProxy(PythonProcessor.class);
            pythonProcessor.initialize(this);
        }
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        synchronized (PyLib.class) {
            //System.out.println("computeTileStack: thread = " + Thread.currentThread());
            //PyLib.Diag.setFlags(PyLib.Diag.F_EXEC);
            pythonProcessor.compute(this, targetTiles, targetRectangle);
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

    /**
     * The interface that the given Python class must implement.
     */
    public interface PythonProcessor {
        /**
         * Initialize the operator.
         *
         * @param operator The GPF operator which called the Python code.
         */
        void initialize(Operator operator);

        /**
         * Compute the tiles associated with the given bands.
         *
         * @param operator        The GPF operator which called the Python code.
         * @param targetTiles     a mapping from {@link Band} objects to {@link Tile} objects.
         * @param targetRectangle the target rectangle to process in pixel coordinates.
         */
        void compute(Operator operator, Map<Band, Tile> targetTiles, Rectangle targetRectangle);

        /**
         * Disposes the operator and all the resources associated with it.
         *
         * @param operator The GPF operator which called the Python code.
         */
        void dispose(Operator operator);
    }

}
