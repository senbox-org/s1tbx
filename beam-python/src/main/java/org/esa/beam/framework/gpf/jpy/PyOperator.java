package org.esa.beam.framework.gpf.jpy;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.jpy.PyLib;
import org.jpy.PyModule;
import org.jpy.PyObject;

import java.awt.Rectangle;
import java.util.Map;

/**
 * @author Norman Fomferra
 */
@OperatorMetadata(alias = "PyOperator", internal=true)
public class PyOperator extends Operator {

    /**
     * The single source product.
     */
    @SourceProduct(optional = true)
    private Product sourceProduct;

    /**
     * Name of the Python module.
     */
    @Parameter
    private String pythonModuleName;

    /**
     * Name of the Python class.
     */
    @Parameter
    private String tileComputerClassName;

    @Parameter
    Map<String, Object> parameters;

    private transient PyModule pyModule;
    private transient TileStackComputer tileStackComputer;

    @Override
    public void initialize() throws OperatorException {

        System.out.println("initialize: thread = " + Thread.currentThread());
        PyLib.startPython(null);
        PyModule pySysModule = PyModule.importModule("sys");
        PyObject pyPathList = pySysModule.getAttribute("path");
        pyPathList.callMethod("append", ".\\examples");

        PyLib.Diag.setFlags(PyLib.Diag.F_EXEC);
        pyModule = PyModule.importModule(pythonModuleName);
        PyObject pyTileComputer = pyModule.call(tileComputerClassName);
        tileStackComputer = pyTileComputer.createProxy(TileStackComputer.class);
        tileStackComputer.initialize(this);
    }

    @Override
    public void computeTileStack(Map<Band, Tile> targetTiles, Rectangle targetRectangle, ProgressMonitor pm) throws OperatorException {
        System.out.println("computeTileStack: thread = " + Thread.currentThread());
        PyLib.Diag.setFlags(PyLib.Diag.F_EXEC);
        tileStackComputer.computeTileStack(this, targetTiles, targetRectangle);
        //PyLib.Diag.setFlags(PyLib.Diag.F_OFF);
    }

    @Override
    public void dispose() {
        System.out.println("dispose: thread = " + Thread.currentThread());
        tileStackComputer.dispose(this);
    }

    public interface TileComputer {
        void initialize(Operator operator);

        void computeTile(Operator operator, Band targetBand, Tile targetTile);

        void dispose(Operator operator);
    }

    public interface TileStackComputer {
        void initialize(Operator operator);

        void computeTileStack(Operator operator, Map<Band, Tile> targetTiles, Rectangle targetRectangle);

        void dispose(Operator operator);
    }

}
