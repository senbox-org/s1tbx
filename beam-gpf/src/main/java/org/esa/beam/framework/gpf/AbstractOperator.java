package org.esa.beam.framework.gpf;

/**
 * The abstract base class for all operators intended to be extended by clients.
 * <p>Four methods are intended to be implemented or overidden:
 * <ld>
 * <li>{@link #initialize() initialize()}: must be implemented in order to initialise the operator and create the target product.</li>
 * <li>{@link #computeTile(org.esa.beam.framework.datamodel.Band,Tile)   computeTile()}: implemented to compute the tile for a single band.</li>
 * <li>{@link #computeTileStack(java.util.Map,java.awt.Rectangle)  computeTileStack()}: implemented to compute the tiles for multiple bands.</li>
 * <li>{@link #dispose()}: can be overidden in order to free all resources previously allocated by the operator.</li>
 * </ld>
 * </p>
 * <p>Generally, only one {@code compute} method needs to be implemented. It depends on the type of algorithm which
 * of both operations is most advantageous to implement:
 * <ol>
 * <li>If bands can be computed independently of each other, then it is
 * beneficial to implement the {@code computeTile()} method. This is the case for sub-sampling, map-projections,
 * band arithmetic, band filtering and statistic analyses.</li>
 * <li>{@code computeTileStack()} should be overriden in cases where the bands of a product cannot be computed independly, e.g.
 * because they are a simultaneous output. This is often the case for algorithms based on neural network, cluster analyses,
 * model inversion methods or spectral unmixing.</li>
 * </ol>
 * </p>
 * <p>The framework execute either the {@code computeTile()} or the {@code computeTileStack()} method
 * based on the current use case or request.
 * If tiles for single bands are requested, e.g. for image display, it will always prefer an implementation of
 * the {@code computeTile()} method and call it.
 * If all tiles are requested at once, e.g. writing a product to disk, it will attempt to use the {@code computeTileStack()}
 * method. If the framework cannot use its preferred operation, it will use the one implemented by the operator.</p>
 */
public abstract class AbstractOperator extends Operator {

    /**
     * Constructs a new operator. Note that only SPIs should directly create operators.
     */
    protected AbstractOperator() {
    }
}
