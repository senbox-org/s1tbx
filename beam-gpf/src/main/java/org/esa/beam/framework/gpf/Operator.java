package org.esa.beam.framework.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.beam.framework.datamodel.Product;

import java.awt.Rectangle;

/**
 * The {@code Operator} interface defines the signature for processing
 * algorithms. Every {@code Node} of a processing graph uses an
 * {@code Operator} instance to compute its target Product. Implement
 * this interface to create new processing algorithms.
 * <p>{@code Operator}s shall exclusively be created by their {@link OperatorSpi}.</p>
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
     * Any client code that must be performed before computation of raster data should be placed here.
     * <p/>
     * <p>Clients are adviced to use the progress monitor <code>pm</code> if the method may take
     * a while to finish. The progress monitor informs the framework about the progress being made.</p>
     *
     * @param context the operator's context
     * @param pm      a progress monitor. Can be used to signal progress.
     *
     * @return the target product
     */
    Product initialize(OperatorContext context, ProgressMonitor pm) throws OperatorException;

    /**
     * <!-- todo - more API doc ->
     * This method is called by the framework for a specific target band and a specific rectangular area.
     * Which band and which area shall be computed can be retrived from the {@code targetRaster} parameter.
     *
     * @param targetRaster the raster to compute
     * @param pm           a progress monitor. Can be used to signal progress.
     *
     * @see Operator
     */
    void computeBand(Raster targetRaster, ProgressMonitor pm) throws OperatorException;

    /**
     * <!-- todo - more API doc ->
     * This method is called by the framework for a specific rectangular area.
     * The data for each target band shall be computed for the given area during this call.
     *
     * @param targetTileRectangle a rectangular area which shall be compmuted during this call.
     * @param pm                  a progress monitor. Can be used to signal progress.
     */
    void computeAllBands(Rectangle targetTileRectangle, ProgressMonitor pm) throws OperatorException;

    /**
     * Releases the resources the operator has acquired during its lifetime.
     */
    void dispose();
}
