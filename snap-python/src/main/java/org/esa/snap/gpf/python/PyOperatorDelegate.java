package org.esa.snap.gpf.python;

import org.esa.snap.framework.datamodel.Band;
import org.esa.snap.framework.gpf.Operator;
import org.esa.snap.framework.gpf.Tile;

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
     * Compute the tiles associated with the given bands.
     *
     * @param context         The GPF operator that provides the context for the Python implementation.
     * @param targetTiles     a mapping from {@link Band} objects to {@link Tile} objects.
     * @param targetRectangle the target rectangle to process in pixel coordinates.
     */
    void compute(Operator context, Map<Band, Tile> targetTiles, Rectangle targetRectangle);

    /**
     * Disposes the operator and all the resources associated with it.
     *
     * @param context The GPF operator that provides the context for the Python implementation.
     */
    void dispose(Operator context);
}
