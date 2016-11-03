package org.esa.snap.python.gpf;

import com.bc.ceres.core.ProgressMonitor;
import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.Tile;

import java.awt.Rectangle;
import java.util.Map;

/**
 * The interface that a given client Python class must implement.
 *
 * @author Norman Fomferra
 * @since SNAP 2.0
 */
public interface PyOperatorDelegate {

    /**
     * Initialize the operator.
     *
     * @param context The GPF operator that provides the context for the Python implementation.
     */
    void initialize(Operator context);

    /**
     * Executes the operator.
     * <p>
     * For operators that compute raster data tiles, the method is usually a no-op. Other operators might perform their
     * main work in this method, e.g. perform some image analysis such as extracting statistics and other features from
     * data products.
     * <p>
     * Don't call this method directly. The framework may call this method
     * <ol>
     * <li>once before the very first tile is computed, or</li>
     * <li>as a result of a call to {@link Operator#execute(ProgressMonitor)}.</li>
     * </ol>
     * <p>
     * The default implementation does nothing.
     *
     * @param pm A progress monitor to be notified for long-running tasks.
     */
    void doExecute(ProgressMonitor pm);

    /**
     * Compute the tiles associated with the given bands.
     *
     * @param context         The GPF operator that provides the context for the Python implementation.
     * @param targetTiles     A mapping from {@link Band} objects to target {@link Tile} objects.
     * @param targetRectangle The target rectangle to process in pixel coordinates.
     * @deprecated since SNAP 3.0. Use {@link #computeTileStack} instead.
     */
    @Deprecated
    void compute(Operator context, Map<Band, Tile> targetTiles, Rectangle targetRectangle);

    /**
     * Compute the tile associated with the given band.
     *
     * @param context    The GPF operator that provides the context for the Python implementation.
     * @param band       The band.
     * @param targetTile The band's target tile to be computed.
     * @since SNAP 3.0
     */
    void computeTile(Operator context, Band band, Tile targetTile);

    /**
     * Compute the tiles associated with the given bands.
     *
     * @param context         The GPF operator that provides the context for the Python implementation.
     * @param targetTiles     A mapping from {@link Band} objects to target {@link Tile} objects.
     * @param targetRectangle The target rectangle in pixel coordinates.
     * @since SNAP 3.0
     */
    void computeTileStack(Operator context, Map<Band, Tile> targetTiles, Rectangle targetRectangle);

    /**
     * Disposes the operator and all the resources associated with it.
     *
     * @param context The GPF operator that provides the context for the Python implementation.
     */
    void dispose(Operator context);
}
