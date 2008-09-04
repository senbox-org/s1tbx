package com.bc.ceres.glevel;

import com.bc.ceres.glevel.support.DefaultLayerImage;

import java.awt.geom.AffineTransform;
import java.awt.geom.Rectangle2D;

/**
 * An image comprising multiple resolution levels.
 */
public interface LayerImage {
    LayerImage NULL = DefaultLayerImage.NULL;

    /**
     * Gets the source for the level images used for different resolution levels.
     *
     * @return The level image source.
     */
    LevelImageSource getLevelImageSource();

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
     * Returns the bounding box in model coordinates.
     *
     * @return the bounding box.
     */
    Rectangle2D getModelBounds();
}
