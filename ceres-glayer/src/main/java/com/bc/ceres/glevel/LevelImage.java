package com.bc.ceres.glevel;

import javax.media.jai.PlanarImage;
import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * An image comprising multiple resolution levels.
 */
public interface LevelImage {

    /**
     * Gets the maximum number of resolution levels.
     *
     * @return the number of resolution levels
     */
    int getLevelCount();

    /**
     * Computes the resolution level from the given scaling factor. The scaling factor is the number of units in
     * the model CS per unit in the view CS.
     *
     * @param scale the model-to-view scaling factor.
     * @return the resolution level.
     */
    int computeLevel(double scale);

    /**
     * Gets the image for the given level.
     *
     * @param level the resolution level.
     * @return the image for the given level.
     * @see #regenerateLevels(boolean)
     */
    PlanarImage getPlanarImage(int level);

    /**
     * States an attempt to regenerate images at all levels and optionally remove all cached tiles.
     * After calling this method, {@link #getPlanarImage(int)} should return a newly created image the first
     * time it is called.
     * <p/>
     * This method is particularily useful if properties have changed that affect the appearance of the
     * returned images at all levels, e.g. after a new color palette has been assigned or the
     * contrast range has changed.
     *
     * @param removeCachedTiles if {@code true}, cached tiles will be removed from tile caches (if any)
     */
    void regenerateLevels(boolean removeCachedTiles);

    /**
     * Gets a copy (non-life object) of the affine transformation from image to model coordinates for the given level.
     *
     * @param level the resolution level
     * @return the affine transformation from image to model coordinates.
     */
    AffineTransform getImageToModelTransform(int level);

    /**
     * Gets a copy (non-life object) of the affine transformation from model to image coordinates for the given level.
     *
     * @param level the resolution level
     * @return the affine transformation from model to image coordinates.
     */
    AffineTransform getModelToImageTransform(int level);

    /**
     * Returns the bounding box in model coordinates for the given level.
     *
     * @param level the resolution level
     * @return the bounding box.
     */
    Rectangle2D getBounds(int level);
}
