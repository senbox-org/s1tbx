package org.esa.beam.framework.gpf.jpy;


import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.Tile;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProduct;
import org.jpy.python.PyInterpreter;
import org.jpy.python.PyLib;
import org.jpy.python.PyModule;
import org.jpy.python.PyObject;

import java.awt.Rectangle;
import java.util.Map;

/**
 * @author Norman Fomferra
 */
@OperatorMetadata(alias = "PyOp")
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
    private transient TileComputer tileComputer;


    @Override
    public void initialize() throws OperatorException {
        PyInterpreter.initialize(null);
        PyModule pySysModule = PyInterpreter.importModule("sys");
        PyObject pyPathList = pySysModule.getAttributeObject("path");
        pyPathList.callMethod("append", ".\\examples");


        PyLib.Diag.setFlags(PyLib.Diag.F_EXEC);

        pyModule = PyInterpreter.importModule(pythonModuleName);
        PyObject pyTileComputer = pyModule.call(tileComputerClassName);
        tileComputer = pyTileComputer.cast(TileComputer.class);
        tileComputer.initialize(this);
    }

    @Override
    public void computeTile(Band targetBand, Tile targetTile, ProgressMonitor pm) throws OperatorException {
        tileComputer.computeTile(this, targetBand, targetTile);
    }

    @Override
    public void dispose() {
        tileComputer.dispose(this);
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

    public static class Spi extends OperatorSpi {
        public Spi() {
            super(PyOperator.class);
        }
    }
}
