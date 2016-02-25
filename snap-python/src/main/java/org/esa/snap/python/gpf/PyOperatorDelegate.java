package org.esa.snap.python.gpf;

import org.esa.snap.core.datamodel.Band;
import org.esa.snap.core.gpf.Operator;
import org.esa.snap.core.gpf.Tile;

import java.awt.Rectangle;
import java.util.HashMap;
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
     * Compute the tiles associated with the given bands.
     *
     * @param context         The GPF operator that provides the context for the Python implementation.
     * @param band            The target band.
     * @param targetTile      The band's target tile to be computed.
     */
    default void computeTile(Operator context, Band band, Tile targetTile) {
        final HashMap<Band, Tile> targetTiles = new HashMap<>();
        targetTiles.put(band, targetTile);
        compute(context, targetTiles, targetTile.getRectangle());
    }

    /**
     * Disposes the operator and all the resources associated with it.
     *
     * @param context The GPF operator that provides the context for the Python implementation.
     */
    void dispose(Operator context);
}
