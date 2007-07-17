package org.esa.beam.framework.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Product;

import java.awt.*;

/**
 * The <code>Operator</code> interface defines the signature for processing
 * algorithms. Every <code>Node</code> of a processing graph uses an
 * <code>Operator</code> instance to compute its target Product. Implement
 * this interface to create new processing algorithms.
 * <p/>
 * <p><code>Operator</code>s shall exclusively be created by their {@link OperatorSpi}.</p>
 * <p/>
 * <p/>
 * <p>Clients shall not implement or extend the interface <code>Operator</code> directly. Instead
 * they should derive from {@link org.esa.beam.framework.gpf.AbstractOperator}.</p>
 *
 * @author Maximilian Aulinger
 * @author Norman Fomferra
 * @author Marco Zühlke
 * @since 4.1
 */
public interface Operator {

    /**
     * Gets the service provider interface (SPI) which was used to create this operator.
     *
     * @return the service provider interface.
     */
    OperatorSpi getSpi();

    /**
     * Initializes this operator and returns its target product.
     * The framework calls this method after it has created this operator.
     * this <code>Operator</code> have been set. Any client code that must be performed before
     * computation of raster data should be placed here.
     * <p/>
     * <p>Clients are adviced to use the progress monitor <code>pm</code> if the method may take
     * a while to finish. The progress monitor informs the framework about the progress being made.</p>
     *
     * @param context the operator's context
     * @param pm      a progress monitor. Can be used to signal progress.
     * @return the target product
     */
    Product initialize(OperatorContext context, ProgressMonitor pm) throws OperatorException;

    /**
     * todo - API doc
     *
     * @param targetTile
     * @param pm
     * @throws OperatorException
     */
    void computeBand(Raster targetRaster, ProgressMonitor pm) throws OperatorException;

    /**
     * todo - API doc
     *
     * @param targetTileRectangle
     * @param pm
     * @throws OperatorException
     */
    void computeAllBands(Rectangle targetTileRectangle, ProgressMonitor pm) throws OperatorException;

    /**
     * todo - API doc
     */
    void dispose();
}
